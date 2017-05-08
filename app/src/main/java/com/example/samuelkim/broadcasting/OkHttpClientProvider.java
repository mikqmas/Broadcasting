package com.example.samuelkim.broadcasting;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

/**
 * Created by samuel.kim on 5/3/17.
 */

public class OkHttpClientProvider {

    private static KeyManager[] keyManagers;
    private static OkHttpClient _client;
    private static Object _lockClient = new Object();

    protected static OkHttpClient getClient() {
        synchronized (_lockClient) {
            return _client;
        }
    }

    private static OkHttpClient provideOkHttpClient(final InputStream certP12, final String secret) {
        X509TrustManager trustManager = trustManagerForCertificates(certP12, secret);
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(keyManagers, new TrustManager[]{trustManager}, null);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }

        return new OkHttpClient
                .Builder()
                .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                .build();
    }

    public static void initialize(final InputStream certP12, final String secret) {
        if (_client == null) {
            synchronized (_lockClient) {
                if (_client == null) {
                    _client = provideOkHttpClient(certP12, secret);
                }
            }
        }
    }

    private static X509TrustManager trustManagerForCertificates(final InputStream instream, final String secret) {

        try {
            char[] tableauCertPassword = secret.toCharArray();
            KeyStore appKeyStore = KeyStore.getInstance("BKS");
            appKeyStore.load(instream, tableauCertPassword);
            instream.close();

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
            keyManagerFactory.init(appKeyStore, tableauCertPassword);
            keyManagers = keyManagerFactory.getKeyManagers();

            TrustManagerFactory tmf =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(appKeyStore);

            TrustManager[] trustManagers = tmf.getTrustManagers();

            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException(
                        "Unexpected default trust managers:" + Arrays.toString(trustManagers));
            }
            return (X509TrustManager) trustManagers[0];
        } catch (Exception e) {
            //Timber.e(e, "SSLSocket creation exception %s", e.getMessage());
            //throw new AssertionError(e);
            return null;
        }
    }

}
