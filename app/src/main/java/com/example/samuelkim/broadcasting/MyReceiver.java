package com.example.samuelkim.broadcasting;

import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.clover.sdk.util.CloverAccount;
import com.clover.sdk.util.CustomerMode;
import com.clover.sdk.v1.Intents;
import com.clover.sdk.v1.app.AppNotification;
import com.clover.sdk.v1.app.AppNotificationReceiver;
import com.clover.sdk.v1.printer.Category;
import com.clover.sdk.v1.printer.job.PrintJob;
import com.clover.sdk.v1.printer.job.StaticOrderPrintJob;
import com.clover.sdk.v1.printer.job.StaticReceiptPrintJob;
import com.clover.sdk.v3.order.Order;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import static android.R.attr.order;

/**
 * Created by samuel.kim on 1/18/17.
 */

public class MyReceiver extends AppNotificationReceiver {
    @Override
    public void onReceive(Context context, AppNotification notification) {

        MainActivity.count++;
        Toast.makeText(context, MainActivity.count.toString(), Toast.LENGTH_SHORT).show();

        new NetworkAysncTask().execute(context);

    }

    private class NetworkAysncTask extends AsyncTask<Context, Void, String> {
        String response = "";
        Context context;

        @Override
        protected String doInBackground(Context... params){
            HttpsURLConnection client = null;
            context = params[0];

            try {
                URL url = new URL("https://sandbox.dev.clover.com/v3/merchants/EJE2ZH35JJAG2/orders");
                client = (HttpsURLConnection) url.openConnection();

                //POST
                client.setRequestProperty("Authorization", "Bearer e462d53f-0ef5-c500-65e9-350e7129ab73");
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
            Toast.makeText(context, ""+s, Toast.LENGTH_LONG).show();
        }
    }
}


//        Account account = CloverAccount.getAccount(context);
//
//        Order order = new Order().setId(notification.payload);
//        new StaticOrderPrintJob.Builder().order(order).build().print(context, account);

//public class MyReceiver extends BroadcastReceiver {
//    private Account account;
//    private int count;
//
//    @Override
//    public void onReceive(Context context, Intent intent) {
//
////        count++;
////        Toast.makeText(context, count, Toast.LENGTH_SHORT).show();
//
//        String orderId = intent.getDataString();
////        Bundle extras = intent.getExtras();
////        String orderId = extras.getString(Intents.EXTRA_CLOVER_ORDER_ID);
////        String test = "ABC1AFDFS23";
////
//        Toast.makeText(context, intent.getDataString(), Toast.LENGTH_LONG).show();
////
//        Order order = new Order().setId(orderId);
////
////        if(CustomerMode.getState(context) == CustomerMode.State.ENABLED){
////            CustomerMode.disable(context);
////        }else {
////            CustomerMode.enable(context);
////        }
////
////        account = CloverAccount.getAccount(context);
////
//        new StaticOrderPrintJob.Builder().order(order).build().print(context, account);
//
//    }
//}
