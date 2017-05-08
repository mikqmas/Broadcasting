package com.example.samuelkim.broadcasting;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.icu.util.Output;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IInterface;
import android.os.Looper;
import android.os.RemoteException;
import android.print.PrintJobId;
import android.print.PrintManager;
import android.printservice.PrintService;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Config;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.clover.connector.sdk.v3.PaymentConnector;
import com.clover.sdk.util.CloverAccount;
import com.clover.sdk.util.CloverAuth;
import com.clover.sdk.util.CustomerMode;
import com.clover.sdk.v1.BindingException;
import com.clover.sdk.v1.ClientException;
import com.clover.sdk.v1.Intents;
import com.clover.sdk.v1.ServiceConnector;
import com.clover.sdk.v1.ServiceException;
import com.clover.sdk.v1.app.AppConnector;
import com.clover.sdk.v1.customer.CustomerConnector;
import com.clover.sdk.v1.merchant.MerchantConnector;
import com.clover.sdk.v1.printer.Printer;
import com.clover.sdk.v1.printer.PrinterConnector;
import com.clover.sdk.v1.printer.ReceiptRegistrationConnector;
import com.clover.sdk.v1.printer.job.ImagePrintJob;
import com.clover.sdk.v1.printer.job.PrintJob;
import com.clover.sdk.v1.printer.job.PrintJobsConnector;
import com.clover.sdk.v1.printer.job.PrintJobsContract;
import com.clover.sdk.v1.printer.job.StaticGiftReceiptPrintJob;
import com.clover.sdk.v1.printer.job.StaticOrderPrintJob;
import com.clover.sdk.v1.printer.job.StaticPaymentPrintJob;
import com.clover.sdk.v1.printer.job.StaticReceiptPrintJob;
import com.clover.sdk.v1.tender.Tender;
import com.clover.sdk.v1.tender.TenderConnector;
import com.clover.sdk.v3.apps.AppsConnector;
import com.clover.sdk.v3.customers.Customer;
import com.clover.sdk.v3.employees.Employee;
import com.clover.sdk.v3.employees.EmployeeConnector;
import com.clover.sdk.v3.employees.Role;
import com.clover.sdk.v3.inventory.InventoryConnector;
import com.clover.sdk.v3.inventory.Item;
import com.clover.sdk.v1.merchant.Merchant;
import com.clover.sdk.v3.inventory.TaxRate;
import com.clover.sdk.v3.order.Discount;
import com.clover.sdk.v3.order.LineItem;
import com.clover.sdk.v3.order.Order;
import com.clover.sdk.v3.order.OrderConnector;
import com.clover.sdk.v3.order.OrderType;
import com.clover.sdk.v3.order.OrderV31Connector;
import com.clover.sdk.v3.pay.PaymentRequest;
import com.clover.sdk.v3.pay.PaymentRequestCardDetails;
import com.clover.sdk.v3.payments.Payment;
import com.clover.sdk.v3.payments.Refund;
import com.clover.sdk.v3.payments.TipMode;
import com.clover.sdk.v3.payments.TransactionSettings;
import com.clover.sdk.v3.payments.VaultedCard;
import com.clover.sdk.v3.scanner.BarcodeResult;
import com.clover.sdk.v3.scanner.BarcodeScanner;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.example.samuelkim.broadcasting.PrintService.FIRE_ORDER;
import static com.example.samuelkim.broadcasting.PrintService.ORDER_ID;
import static com.example.samuelkim.broadcasting.PrintService.PRINT_RECEIPT;
import static com.example.samuelkim.broadcasting.PrintService.PRINT_TYPE;

public class MainActivity extends AppCompatActivity {
    public static Integer count = 0;
    protected static final String START_KIOSK = "com.clover.remote.terminal.action.V1_START_REMOTE_TERMINAL_KIOSK";
    private static final int SECURE_PAYMENT_APP_KIOSK = 5;
    private InventoryConnector mInventoryConnector;
    private MerchantConnector mMerchantConnector;
    private CustomerConnector mCustomerConnector;
    private OrderConnector mOrderConnector;
    private EmployeeConnector mEmployeeConnector;
    private TenderConnector tenderConnector;
    private Merchant mMerchant;
    private Account mAccount;
    private PrintJob pj;
    private static final int CARD_DATA_REQUEST_CODE = 1;
    private static final int MANUAL_PAY_REQUEST_CODE = 2;
    private static final int REFUND_REQUEST = 3;
    private static final int TIP_REQUEST = 4;
    private static final int REQUEST_PAY = 5;
    private static final int REQUEST_ANY_EMPLOYEE = 6;
    String token = "";


    private BarcodeScanner bs;

    private KeyStore createTrustStore() {
        try {

            String STORETYPE = "PKCS12";
            KeyStore trustStore = KeyStore.getInstance(STORETYPE);
            InputStream trustStoreStream = getClass().getResourceAsStream("/certs/clover_cacerts.p12");
            String TRUST_STORE_PASSWORD = "clover";

            trustStore.load(trustStoreStream, TRUST_STORE_PASSWORD.toCharArray());

            return trustStore;
        } catch (Throwable t) {
            Log.e(getClass().getSimpleName(), "Error loading trust store", t);
            t.printStackTrace();
            return null;
        }

    }


    public void createPayment(View view) {
        Toast.makeText(MainActivity.this, "thisisatest", Toast.LENGTH_LONG).show();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                JSONObject paymentParamsBody = null;
                try {
                    paymentParamsBody = new JSONObject();
                    paymentParamsBody.put("tender", new JSONObject());
                    paymentParamsBody.getJSONObject("tender").put("id", "N7JJHPSFP087A");
                    paymentParamsBody.put("amount", 544);
                } catch (Exception e) {
                    e.printStackTrace();
                }


                OkHttpClient client = new OkHttpClient();
                final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                String url = "https://api.clover.com/v3/merchants/YTEJ7Y271CZAG/orders/QGR3DX27XG4HP/payments/?access_token=25ddd51b-7ad1-2ea5-63b8-45709b675afa";
                String json = paymentParamsBody.toString();

                RequestBody body = RequestBody.create(JSON, json);

                Request request = new Request.Builder().url(url).post(body).build();
                try {
                    Response response = client.newCall(request).execute();
                    Log.i("response", response.toString());
                    Toast.makeText(MainActivity.this, response.toString(), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }


    public void getIsRevenue(View view) {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected final Void doInBackground(Void... params) {
                try {
                    LineItem li = mOrderConnector.getOrder("XQJB22GC6AZ6W").getLineItems().get(0);
                    Log.i("HASREVENUE", Boolean.toString(li.hasIsRevenue()));
                    Log.i("GETREVENUE", Boolean.toString(li.getIsRevenue()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

            }
        }.execute();
    }


    public void launchClear(View view) {
        // Load CAs from an InputStream
// (could be from a resource or ByteArrayInputStream or ...)
        try{
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
// From https://www.washington.edu/itconnect/security/ca/load-der.crt
            InputStream caInput = this.getResources().openRawResource(R.raw.client); // p12 file inside res/raw directory
            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
                System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
            } finally {
                caInput.close();
            }

// Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

// Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

// Create an SSLContext that uses our TrustManager
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);

            HashMap<String, String> formData = new HashMap<>();
                formData.put("grant_type", "password");
                formData.put("username", "clover_sam");
                formData.put("password", "fXhtVsb3");

                FormBody.Builder ret = new FormBody.Builder();
                for(String key : formData.keySet()) {
                    ret.add(key, formData.get(key));
                }

// Tell the URLConnection to use a SocketFactory from our SSLContext
            URL url = new URL("https://dev-capi.clearme.com/auth/v1/realms/capi/protocol/openid-connect/token");
            HttpsURLConnection urlConnection =
                    (HttpsURLConnection)url.openConnection();
            urlConnection.setSSLSocketFactory(context.getSocketFactory());
            InputStream in = urlConnection.getInputStream();
            Log.i("log", in.toString());
        }catch (Exception e) {
            e.printStackTrace();
        }


//        final InputStream instream = this.getResources().openRawResource(R.raw.client); // p12 file inside res/raw directory
//        OkHttpClientProvider.initialize(instream, "thisisatest");
//        final OkHttpClient cli = OkHttpClientProvider.getClient();
//
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Request.Builder requestBuilder = new Request.Builder();
//
//                HashMap<String, String> formData = new HashMap<>();
//                formData.put("grant_type", "password");
//                formData.put("username", "clover_sam");
//                formData.put("password", "fXhtVsb3");
//
//                FormBody.Builder ret = new FormBody.Builder();
//                for(String key : formData.keySet()) {
//                    ret.add(key, formData.get(key));
//                }
//
//                Request request = requestBuilder
//                        .url("https://dev-capi.clearme.com/auth/v1/realms/capi/protocol/openid-connect/token")
//                        .get()
//                        .build();
//
//                try {
//                    Response response = cli.newCall(request).execute();
//                    if (!response.isSuccessful()) {
//                        Exception ex = new Exception("POST failed with code: " + response.code());
//                    }
//                } catch (Exception ex) {
//                    Exception ee = ex;
//                    ex.printStackTrace();
//                }
//            }
//        }).start();
}

//        KeyStore trustStore = createTrustStore();

    public void addTax(View view) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    List<TaxRate> taxRates = new ArrayList<TaxRate>();
                    taxRates.add(mInventoryConnector.getTaxRates().get(0));
                    Item item = new Item().setName("thisisatest123").setPrice(1000l).setDefaultTaxRates(false);
                    item.setTaxRates(taxRates);
                    mInventoryConnector.createItem(item);
                    Log.i("test", item.toString());
                } catch (Exception exception) {
                    Log.e("exception log", exception.getMessage(), exception.getCause());
                }
                return null;
            }
        }.execute();
    }

    public void getDeletedCustomer(View view) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    com.clover.sdk.v1.customer.Customer c = mCustomerConnector.getCustomer("54EPY1XGP5YDA");
                    Log.i("Customer", c.toString());
                } catch (Exception exception) {
                    Log.e("exception log", exception.getMessage(), exception.getCause());
                }
                return null;
            }
        }.execute();
    }

    // Your setting (true or false) for on / off.
    private static Bundle getBarcodeSetting(final boolean enabled) {
        final Bundle extras = new Bundle();
        extras.putBoolean(Intents.EXTRA_START_SCAN, enabled);
        extras.putBoolean(Intents.EXTRA_SHOW_PREVIEW, enabled);
        extras.putBoolean(Intents.EXTRA_SHOW_MERCHANT_PREVIEW, enabled);
        extras.putBoolean(Intents.EXTRA_LED_ON, false);
        return extras;
    }

    // Receiving barcode scan
    private final BroadcastReceiver mBarcodeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            BarcodeResult br = new BarcodeResult(intent);
            Log.d("SCANNER", "Received scan with result" + br.getBarcode());
            Toast.makeText(context, br.getBarcode(), Toast.LENGTH_SHORT).show();
            bs.stopScan(getBarcodeSetting(false));
        }
    };

    // Starting, receiving and stopping scanner service.
    public void useScanner(View view){
        bs = new BarcodeScanner(this);
        bs.startScan(getBarcodeSetting(true));
        IntentFilter filter = new IntentFilter(BarcodeResult.INTENT_ACTION);
        registerReceiver(mBarcodeReceiver, filter);
    }



    public void requestRole(View view) {
        Intent i = new Intent(Intents.ACTION_REQUEST_ROLE);
        i.putExtra(Intents.EXTRA_ROLE, "employee");
        startActivity(i);
    }

    public void cloverPay(View view) {
        Intent i = new Intent(Intents.ACTION_CLOVER_PAY);
        i.putExtra(Intents.EXTRA_CLOVER_ORDER_ID, "96CQJAT27Y082");
        startActivity(i);
    }

    public void getPIN(View view) {
        Intent i = new Intent(Intents.ACTION_AUTHENTICATE_EMPLOYEE);
        i.putExtra(Intents.EXTRA_REASON, "Enter PIN");
        startActivityForResult(i, REQUEST_ANY_EMPLOYEE);
    }

    public void createTenderType(View view) {
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    tenderConnector.checkAndCreateTender("thisisatesttender", getPackageName(), true, false);
                    tenderConnector.getTenders();
                } catch (Exception exception) {
                    Log.e("exception log", exception.getMessage(), exception.getCause());
                    return exception;
                }
                return null;
            }
        }.execute();
    }

    public void deleteCustomTender(View view) {
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    tenderConnector.deleteTender(tenderConnector.getTenders().get(14).getId());
                } catch (Exception exception) {
                    Log.e("exception log", exception.getMessage(), exception.getCause());
                    return exception;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception e) {
                super.onPostExecute(e);
                Toast.makeText(getBaseContext(), "deleted", Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    public void clearCustomerFromOrder(View view) {

        AppsConnector apc = new AppsConnector(this, mAccount);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected final Void doInBackground(Void... params) {
                try {
                    Order order = mOrderConnector.getOrder("Y8TBS5V2ZBTVM");
                    order.clearCustomers();
                    OrderType ot = order.getOrderType();
                    ot.setIsHidden(true);
                    mOrderConnector.updateOrder(order);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    public void printJobExample(View view) {
        Intent i = new Intent(MainActivity.this, PrintJobsTestActivity.class);
        startActivity(i);
    }

    public void doRefund(View view){
        Intent i = new Intent(Intents.ACTION_START_ORDER_MANAGE);
        i.putExtra(Intents.EXTRA_ORDER_ID, "E7EV3KW4H4MGM");
        startActivityForResult(i, REFUND_REQUEST);
    }

    public void webView(View view) {
        WebView webview = new WebView(this);
        setContentView(webview);
//        webview.loadUrl("https://sandbox.dev.clover.com/oauth/authorize?client_id=B170Z3P9MDGPW");
        webview.loadUrl("http://example.com");
    }

    private void getCloverAuth() {
        // This needs to be done on a background thread
        new AsyncTask<Void, Void, CloverAuth.AuthResult>() {
            @Override
            protected CloverAuth.AuthResult doInBackground(Void... params) {
                try {
                    return CloverAuth.authenticate(MainActivity.this, mAccount);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }



    public void noTaxOrder (View view) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected final Void doInBackground(Void... params){
                Order order = null;
                try{
//                    OrderType type = new OrderType().setId("83P2FABM4TRQ6").setLabel("notax").setTaxable(false).setLabelKey("com.clover.order.type.dine_in");
                    OrderType notax = mOrderConnector.getOrder("A26MKQ29Q71P2").getOrderType();
                    order = mOrderConnector.createOrder(new Order().setOrderType(notax));
                }catch (Exception e){
                    e.printStackTrace();
                }

                Intent i = new Intent(Intents.ACTION_START_REGISTER);
                i.putExtra(Intents.EXTRA_ORDER_ID, order.getId());
                startActivity(i);
                return null;
            }
        }.execute();
    }

    public void manualPay(View view){
        Intent intent = new Intent(Intents.ACTION_MANUAL_PAY);
        intent.putExtra(Intents.EXTRA_AMOUNT, 2000);
        startActivityForResult(intent, MANUAL_PAY_REQUEST_CODE);
    }

    public void addTaxRate(View view) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected final Void doInBackground(Void... params) {
                try {
//                    Item i = mInventoryConnector.getItem("8BGFJ8KR813SJ").setDefaultTaxRates(false);
//                    i.clearDefaultTaxRates();
//                    List<TaxRate> taxRates = new ArrayList<TaxRate>();
//                    taxRates.add(mInventoryConnector.getTaxRate("R97VTY4E9G7DR"));
//                    i.setTaxRates(taxRates);
//                    mInventoryConnector.updateItem(i);
//                    Log.i("test", i.toString());
                    List<String> taxRates = new ArrayList<String>();
                    taxRates.add("R97VTY4E9G7DR");
                    mInventoryConnector.assignTaxRatesToItem("QM21DFMW1ACJJ", taxRates);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    public void addPayment(View view){
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    Payment p = new Payment().setAmount(600l).setTaxAmount(100l);
                    List<LineItem> i = mOrderConnector.getOrder("GPB6BJQ5VR7X4").getLineItems();
                    mOrderConnector.addPayment2("GPB6BJQ5VR7X4", p, i);
                    Log.i("test", i.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }


    public void deleteOrder(View view) {
        new AsyncTask<Void, Void, Void>(){
            OrderConnector OC = new OrderConnector(getBaseContext(), mAccount, null);
            Order order = null;

            @Override
            protected Void doInBackground(Void... voids) {
                OC.connect();

                try {
                    OC.deleteOrder2("S4BE4B0G6PH0T", true);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }
        }.execute();
    }

    public void readCard(View view) {
        final Intent intent = new Intent(Intents.ACTION_SECURE_CARD_DATA);
        intent.putExtra(Intents.EXTRA_TRANSACTION_TYPE, Intents.TRANSACTION_TYPE_CARD_DATA);
        startActivityForResult(intent, CARD_DATA_REQUEST_CODE);
    }


    public void pay(View view) {
        Intent intent = new Intent(Intents.ACTION_SECURE_PAY);
        intent.putExtra(Intents.EXTRA_ORDER_ID, "G575J0TFXEHWP");
        intent.putExtra(Intents.EXTRA_AMOUNT, 1000l);
//        TransactionSettings transactionSettings = new TransactionSettings();
//        transactionSettings.setDisableCashBack(true);
//        transactionSettings.setAllowOfflinePayment(false);
//        transactionSettings.setAutoAcceptPaymentConfirmations(true);
//        transactionSettings.setAutoAcceptSignature(true);
//        transactionSettings.setTipMode(TipMode.ON_SCREEN_BEFORE_PAYMENT);
//        intent.putExtra(Intents.EXTRA_TRANSACTION_SETTINGS, transactionSettings);
        intent.putExtra(Intents.EXTRA_DISABLE_CASHBACK, true);
        intent.putExtra(Intents.EXTRA_TIP_AMOUNT, 30);
        startActivityForResult(intent, REQUEST_PAY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to

        if (requestCode == REQUEST_ANY_EMPLOYEE) {
            String stringExtra;
            if (data != null && resultCode == RESULT_OK) {
                stringExtra = data.getStringExtra(Intents.EXTRA_EMPLOYEE_ID);
                if (stringExtra != null) {
                    Toast.makeText(this, stringExtra, Toast.LENGTH_SHORT).show();
                }
            }
        }
        if (requestCode == CARD_DATA_REQUEST_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.

                // Do something with the contact here (bigger example below)

                Toast.makeText(getBaseContext(), data.getExtras().getParcelable(Intents.EXTRA_CARD_DATA).toString(), Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == CARD_DATA_REQUEST_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.

                // Do something with the contact here (bigger example below)
                Log.i("card data", data.getExtras().getParcelable(Intents.EXTRA_CARD_DATA).toString());
                Toast.makeText(getBaseContext(), data.getExtras().getParcelable(Intents.EXTRA_CARD_DATA).toString(), Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == REQUEST_PAY) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Payment pay = data.getExtras().getParcelable(Intents.EXTRA_PAYMENT);
                Log.i("card data", pay.toString());
//                VaultedCard vc = pay.getCardTransaction().getVaultedCard();
//                if(vc != null){
//                    Toast.makeText(getBaseContext(), vc.toString(), Toast.LENGTH_LONG).show();
//                }else {
//                    Toast.makeText(getBaseContext(), "vaultedCard: null", Toast.LENGTH_LONG).show();
//                }
            }
        }

        //REFUND check
        if(requestCode == REFUND_REQUEST) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params){
                    try{
                        Order o = mOrderConnector.getOrder("E7EV3KW4H4MGM");

                        List<Refund> refunds = o.getRefunds();
                        Boolean hasRefunds = o.hasRefunds();

                        mOrderConnector.addOnOrderChangedListener(new OrderV31Connector.OnOrderUpdateListener2() {
                            @Override
                            public void onRefundProcessed(String orderId, String refundId) {
                                Toast.makeText(MainActivity.this, refundId, Toast.LENGTH_SHORT).show();
                            }
                            @Override
                            public void onOrderUpdated(String orderId, boolean selfChange) {

                            }

                            @Override
                            public void onOrderCreated(String orderId) {

                            }

                            @Override
                            public void onOrderDeleted(String orderId) {

                            }

                            @Override
                            public void onOrderDiscountAdded(String orderId, String discountId) {

                            }

                            @Override
                            public void onOrderDiscountsDeleted(String orderId, List<String> discountIds) {

                            }

                            @Override
                            public void onLineItemsAdded(String orderId, List<String> lineItemIds) {

                            }

                            @Override
                            public void onLineItemsUpdated(String orderId, List<String> lineItemIds) {

                            }

                            @Override
                            public void onLineItemsDeleted(String orderId, List<String> lineItemIds) {

                            }

                            @Override
                            public void onLineItemModificationsAdded(String orderId, List<String> lineItemIds, List<String> modificationIds) {

                            }

                            @Override
                            public void onLineItemDiscountsAdded(String orderId, List<String> lineItemIds, List<String> discountIds) {

                            }

                            @Override
                            public void onLineItemExchanged(String orderId, String oldLineItemId, String newLineItemId) {

                            }

                            @Override
                            public void onPaymentProcessed(String orderId, String paymentId) {

                            }



                            @Override
                            public void onCreditProcessed(String orderId, String creditId) {

                            }
                        });
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute();
        }

        // Check which request we're responding to
        if (requestCode == MANUAL_PAY_REQUEST_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                final Payment payment = data.getExtras().getParcelable(Intents.EXTRA_PAYMENT);
                Toast.makeText(this, payment.toString(), Toast.LENGTH_LONG).show();
                try {
                    new AsyncTask<Void, Void, Void>() {
                        OrderConnector OC = new OrderConnector(getBaseContext(), mAccount, null);

                        @Override
                        protected Void doInBackground(Void... voids) {
                            try{
                                List<Payment> payments = OC.getOrder(payment.getOrder().getId()).getPayments();
                                if(payments.size() < 1){
                                    Log.i("payments", "payment was voided");
                                }else {
                                    Log.i("payments", "payment was successful");
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                            }

                            return null;
                        }
                    }.execute();

                } catch (Exception e) {
                    e.printStackTrace();
                }
//                new AsyncTask<Void, Void, Void>() {
//                    @Override
//                    protected Void doInBackground(Void... voids) {
//
//                        return null;
//                    }
//                }.execute();
            }else{
                Log.i("Manual", "RESULT CANCELLED");
            }
        }

        if(requestCode == TIP_REQUEST) {
            Log.i("TEST", Long.toString(data.getExtras().getLong(Intents.EXTRA_TIP_AMOUNT)));
        }
    }

    public void createDiscount(View view) {
        new AsyncTask<Void, Void, Void>(){
            InventoryConnector IC = new InventoryConnector(getBaseContext(), mAccount, null);
            com.clover.sdk.v3.inventory.Discount discount = null;
            List<com.clover.sdk.v3.inventory.Discount> discounts = null;

            @Override
            protected Void doInBackground(Void... voids) {
                IC.connect();

                try {
                    discount = new com.clover.sdk.v3.inventory.Discount();
                    discount.setAmount(1000L);
                    discount.setName("this is a test discount");
                    discount.setId("test");
                    IC.createDiscount(discount);
                    Log.i("test", IC.getDiscount("test").toString());
                    Log.i("TAG", IC.getDiscounts().get(0).toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }
        }.execute();
    }

    public void getTender(View view) {
        new AsyncTask<Void, Void, Void>(){
            TenderConnector TC = new TenderConnector(getBaseContext(), mAccount, null);
            Tender tender = null;

            @Override
            protected Void doInBackground(Void... voids) {
                TC.connect();

                try {
                    tender = TC.getTenders().get(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.i("Tender", tender.toString());
                return null;
            }
        }.execute();
    }

    private Employee employee;

    public void setValue(Employee e) {
        employee = e;
    }

    public void getEmployee(View view){
        new AsyncTask<Void, Void, Employee>(){
            Employee employee = null;

            @Override
            protected Employee doInBackground(Void... voids) {
                try {
                    employee = mEmployeeConnector.getEmployee();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return employee;
            }

            @Override
            protected void onPostExecute(Employee employee) {
                super.onPostExecute(employee);
                setValue(employee);
                Toast.makeText(MainActivity.this, employee.toString(), Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }


    public void addDiscount(View view) {
        new AsyncTask<Void, Void, Void>() {
            OrderConnector OC = new OrderConnector (getBaseContext(), mAccount, null);
            Order order = null;

            @Override
            protected Void doInBackground(Void... voids) {
                OC.connect();

                List<String> test = null;
                try {
                    order = OC.getOrder("Z3SDX5QMNPRVM");
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (ClientException e) {
                    e.printStackTrace();
                } catch (ServiceException e) {
                    e.printStackTrace();
                } catch (BindingException e) {
                    e.printStackTrace();
                }
//                Discount discount = new Discount().setPercentage(50l).setName("FiveStars");
                order.getLineItems().get(0).setPrice(100L);

//                try {
//                    OC.addDiscount2("adskjflasdf", discount);
//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                } catch (ClientException e) {
//                    e.printStackTrace();
//                } catch (ServiceException e) {
//                    e.printStackTrace();
//                } catch (BindingException e) {
//                    e.printStackTrace();
//                }

                return null;
            }
        }.execute();
    }

    public void addOrder(View view) {
        new AsyncTask<Void, Void, Void>() {
            OrderConnector OC = new OrderConnector (getBaseContext(), mAccount, null);
            Order order = null;

            @Override
            protected Void doInBackground(Void... voids) {
                OC.connect();

                List<String> test = null;
                try {
                    order = OC.createOrder(new Order());
                    Log.i("createdorder", order.toString());
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (ClientException e) {
                    e.printStackTrace();
                } catch (ServiceException e) {
                    e.printStackTrace();
                } catch (BindingException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    public void GetMerchantEmail(View view) {
        new AsyncTask<Void, Void, Account>() {

            @Override
            protected Account doInBackground(Void... voids) {
                mMerchantConnector = new MerchantConnector(getBaseContext(), mAccount, null);
                try {
                    mMerchant = mMerchantConnector.getMerchant();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (ClientException e) {
                    e.printStackTrace();
                } catch (ServiceException e) {
                    e.printStackTrace();
                } catch (BindingException e) {
                    e.printStackTrace();
                }

                return mMerchant.getAccount();
            }

            @Override
            protected void onPostExecute(Account a) {
                super.onPostExecute(a);
                String email = a.name.split("\\s+")[2];
                Toast.makeText(getBaseContext(), a.toString(), Toast.LENGTH_LONG).show();

            }
        }.execute();
    }



    public void CreateAuthToken(View view){
        new AsyncTask<Void, Void, String>() {

            OkHttpClient client = new OkHttpClient();
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    token = CloverAuth.authenticate(MainActivity.this, mAccount).authToken;
                    token = CloverAuth.authenticate(getBaseContext(), CloverAccount.getAccount(getBaseContext()), true, 1000l, TimeUnit.MILLISECONDS).authToken;
                } catch (OperationCanceledException e) {
                    e.printStackTrace();
                } catch (AuthenticatorException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.i("TAG", token);

                String url = "https://apisandbox.dev.clover.com/v3/merchants/EJE2ZH35JJAG2";

                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + token)
                        .get()
                        .build();

                Response response = null;

                try {
                    response = client.newCall(request).execute();
                }catch (Exception e){
                    e.printStackTrace();
                }

                Log.i("RESPONSE", token.toString() + response.toString());


                try {
                    return response.body().string();
                } catch (IOException e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                TextView result = (TextView) findViewById(R.id.result);
                result.append(s + "\n");
            }
        }.execute();
    }

    public void deleteAuthToken(View view) {
        new AsyncTask<Void, Void, String>() {
            String token = "";
            OkHttpClient client = new OkHttpClient();
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    token = CloverAuth.authenticate(getBaseContext(), CloverAccount.getAccount(getBaseContext()), true).authToken;
                } catch (OperationCanceledException e) {
                    e.printStackTrace();
                } catch (AuthenticatorException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.i("TAG", token);

                String url = "https://apisandbox.dev.clover.com/v3/access_tokens/" + token;
                Request request = new Request.Builder()
                        .url(url)
                        .delete()
                        .build();

                Response response = null;

                try {
                    response = client.newCall(request).execute();
                }catch (Exception e){
                    e.printStackTrace();
                }

                Log.i("RESPONSE", response.toString());

                return Integer.toString(response.code());
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                TextView result = (TextView) findViewById(R.id.result);
                result.append(s + "\n");
            }
        }.execute();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void PrintSomething(View view){
        new AsyncTask<Void, Void, Void>() {
            Order o = null;
            Payment p = null;
            Printer printer = null;
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    o = mOrderConnector.getOrder("PXJ40VCV1N84W");
                    p = o.getPayments().get(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                PrinterConnector pc = new PrinterConnector(getBaseContext(), mAccount, null){

                };
                try {
                    printer = pc.getPrinters().get(0);
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (ClientException e) {
                    e.printStackTrace();
                } catch (ServiceException e) {
                    e.printStackTrace();
                } catch (BindingException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                if(o != null) {
                    Order o2 = new Order();
                    List<LineItem> lineitems = o.getLineItems();
                    o2.setLineItems(lineitems);
//                    pj = new StaticOrderPrintJob.Builder().order(o).build();
//                    pj = new StaticPaymentPrintJob.Builder().payment(p).build();
                    pj = new StaticReceiptPrintJob.Builder().order(o).build();
//                    pj = new StaticGiftReceiptPrintJob.Builder().order(o).build();

//                    PrintJob imagePrint = new ImagePrintJob.Builder().urlString("https://upload.wikimedia.org/wikipedia/commons/2/22/Wikipedia-Logo-black-and-white.png").build();
//                    imagePrint.print(getApplicationContext(), mAccount);

                    PrintJobsConnector pjc = new PrintJobsConnector(getBaseContext());
//
                    String printid = pjc.print(printer, pj);

//                    Log.i("PRINTID", printid);
//                    Log.i("PRINTJOBS", pjc.getPrintJobIds(PrintJobsContract.STATE_DONE).toString());
                }
            }
        }.execute();
    }


    public void printPaymentReceipt(View view){
        new AsyncTask<Void, Void, Void>() {
            Order o = null;
            Payment p = null;
            Printer printer = null;
            @Override
            protected Void doInBackground(Void... voids) {
                PrinterConnector pc = new PrinterConnector(getBaseContext(), mAccount, null);
                try {
                    printer = pc.getPrinters().get(0);
                    o = mOrderConnector.getOrder("PXJ40VCV1N84W");
//                  Get Payment associated with the order
                    p = o.getPayments().get(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                if(p != null && o != null) {
//                    pj = new StaticPaymentPrintJob.Builder().payment(p).build();
                    pj = new StaticReceiptPrintJob.Builder().order(o).build();

                    PrintJobsConnector pjc = new PrintJobsConnector(getBaseContext());

                    String printid = pjc.print(printer, pj);
                }else {
                    Log.i("O/P is null", "payment: " + p.toString() + "order: " + o.toString());
                }
            }
        }.execute();
    }

    public void print(View view){
        Intent i = new Intent(getBaseContext(), com.example.samuelkim.broadcasting.PrintService.class);
        i.putExtra(PRINT_TYPE, PRINT_RECEIPT);
        i.putExtra(ORDER_ID, "GPB6BJQ5VR7X4");
        startService(i);
//        service.startService(i);

//        new AsyncTask<Void, Void, Void>() {
//            Order o = null;
//            Printer printer = null;
//            PrinterConnector pc = new PrinterConnector(getBaseContext(), mAccount, null);
//            PrintJobsConnector pjc = new PrintJobsConnector(getBaseContext());
//
//            @Override
//            protected Void doInBackground(Void... voids) {
//                try {
//                    o = mOrderConnector.getOrder("K3411HR8J9MKW"); //Get Order
//                    printer = pc.getPrinters().get(0);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                return null;
//            }
//
//            @Override
//            protected void onPostExecute(Void aVoid) {
//                super.onPostExecute(aVoid);
//
//                if(o != null) {
//                    pj = new StaticReceiptPrintJob.Builder().order(o).build();
//                    String printid = pjc.print(printer, pj);
//                }
//            }
//        }.execute();
    }

    public void cancelPrint(View view) {
        new AsyncTask<Void, Void, Void>() {
            Printer printer = null;
            @Override
            protected Void doInBackground(Void... voids) {
                PrinterConnector pc = new PrinterConnector(getBaseContext(), mAccount, null);
                try {
                    printer = pc.getPrinters().get(0);
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (ClientException e) {
                    e.printStackTrace();
                } catch (ServiceException e) {
                    e.printStackTrace();
                } catch (BindingException e) {
                    e.printStackTrace();
                }
                PrintJobsConnector pjc = new PrintJobsConnector(getBaseContext());
//                String printId = pjc.getPrintJobIds(PrintJobsContract.STATE_ERROR).get(0);

                pj.cancel();

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

            }
        }.execute();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAccount == null) {
            mAccount = CloverAccount.getAccount(this);

            if (mAccount == null) {
                return;
            }
        }

        // Create Receivers without having to put it in the manifest.
//        getApplicationContext().registerReceiver(new ConsumReceiver(), new IntentFilter(Intents.ACTION_ACTIVE_REGISTER_ORDER));

        connect();
//        getCloverAuth();
    }

    private void connect() {
        if (mAccount != null) {
            mInventoryConnector = new InventoryConnector(this, mAccount, null);
            mOrderConnector = new OrderConnector(this, mAccount, null);
            mMerchantConnector = new MerchantConnector(this, mAccount, null);
            tenderConnector = new TenderConnector(this, mAccount, null);
            mEmployeeConnector = new EmployeeConnector(this, mAccount, null);
            mCustomerConnector = new CustomerConnector(this, mAccount, null);
            mCustomerConnector.connect();
            mEmployeeConnector.connect();
            mInventoryConnector.connect();
            mOrderConnector.connect();
            mMerchantConnector.connect();
            tenderConnector.connect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();
    }

    private void disconnect() {
        if (mAccount != null) {
            mInventoryConnector.disconnect();
            mMerchantConnector.disconnect();
            mOrderConnector.disconnect();
            tenderConnector.disconnect();
            mEmployeeConnector.disconnect();
            mCustomerConnector.disconnect();
            mCustomerConnector = null;
            mEmployeeConnector = null;
            mInventoryConnector = null;
            mOrderConnector = null;
            mMerchantConnector = null;
            tenderConnector = null;
        }
    }

    public void networkTest(View view) {
        getBaseContext().sendOrderedBroadcast(new Intent(Intents.ACTION_ACTIVE_REGISTER_ORDER), null);

//        new NetworkAysncTask().execute();

//        new AsyncTask<Void, Void, Void>() {
//            @Override
//            protected final Void doInBackground(Void... params) {
//                try {
//                    mInventoryConnector.updateItemStockQuantity("2YJSH2SQ96G02", 2L);
//                    Item mItem = new Item();
//                    mItem.setName("hellllllo");
//                    mItem.setPrice(100l);
//                    mInventoryConnector.createItem(mItem);
//
//
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                return null;
//            }
//        }.execute();
    }



    private class NetworkAysncTask extends AsyncTask<Void, Void, String> {
        String response = "";

        @Override
        protected String doInBackground(Void... params){
            HttpsURLConnection client = null;

            try {
                System.setProperty("https.protocols", "TLSv1.2");
                URL url = new URL("https://sandbox.dev.clover.com/v3/merchants/EJE2ZH35JJAG2/orders");
                client = (HttpsURLConnection) url.openConnection();

                //POST
                client.setRequestProperty("Authorization", "Bearer " + token);
                client.setRequestProperty("Content-Type", "application/json");
                String input = "{\"state\": \"open\"}";
                client.setDoOutput( true );
                client.setDoInput( true );
                client.setChunkedStreamingMode(0);

                OutputStream out = client.getOutputStream();
                OutputStreamWriter wr = new OutputStreamWriter(out);

                wr.write(input);
                wr.flush();
                wr.close();

                String line = "";
                InputStreamReader isr = new InputStreamReader(client.getInputStream());
                BufferedReader reader = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();

                while((line = reader.readLine()) != null){
                    sb.append(line + "\n");
                }

                response = sb.toString();

                Log.i("TAG", response);

                isr.close();
                reader.close();

////GET 
//
// client.setRequestProperty("Authorization", "Bearer e462d53f-0ef5-c500-65e9-350e7129ab73"); 
//
// client.setRequestProperty("Content-Type", "application/json"); 
//
// InputStream in = client.getInputStream(); 
//
// InputStreamReader isw = new InputStreamReader(in); 
//
// int data = isw.read(); 
//
// while (data != -1){ 
//                    char current = (char) data; 
//                    data = isw.read(); 
//
// System.out.print(current); 
//
// response += current; 
//
// }



            }catch (Exception e) {
                e.printStackTrace();
            }finally {
                if(client != null){
                    client.disconnect();
                }
            }

            return response;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Toast.makeText(getBaseContext(), ""+s, Toast.LENGTH_LONG).show();
        }
    }


    public void sendBroadcast(View view){
//        Intent intent = new Intent();
//        intent.setAction("com.clover.intent.action.APP_NOTIFICATION");
//        intent.putExtra("EXTRA_CLOVER_ORDER_ID", "ABC123");
//        sendBroadcast(intent);

//        CustomerMode.enable(this);


        Intent intent = new Intent(Intents.ACTION_CUSTOMER_ADD_TIP);
        intent.putExtra(Intents.EXTRA_ORDER_ID, "E2ECC215HRXHP");
        intent.putExtra(Intents.EXTRA_AMOUNT, 1234l);
//        intent.setExtrasClassLoader(getClass().getClassLoader());
//        intent.setAction(START_KIOSK);
        startActivityForResult(intent, TIP_REQUEST);


    }
}
