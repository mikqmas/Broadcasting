package com.example.samuelkim.broadcasting;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.clover.sdk.util.CloverAccount;
import com.clover.sdk.v1.BindingException;
import com.clover.sdk.v1.ClientException;
import com.clover.sdk.v1.Intents;
import com.clover.sdk.v1.ServiceException;
import com.clover.sdk.v3.order.LineItem;
import com.clover.sdk.v3.order.Order;
import com.clover.sdk.v3.order.OrderConnector;

public class ChangePrice extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final String orderId = intent.getExtras().getString(Intents.EXTRA_ORDER_ID);
        final Context context2 = context;
//        new AsyncTask<Void, Void, Void>() {
//            @Override
//            protected Void doInBackground(Void... voids) {
//                Order order;
//                final OrderConnector OC = new OrderConnector(context2, CloverAccount.getAccount(context2), null);
//                try {
//                    order = OC.getOrder(orderId);
//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                } catch (ClientException e) {
//                    e.printStackTrace();
//                } catch (ServiceException e) {
//                    e.printStackTrace();
//                } catch (BindingException e) {
//                    e.printStackTrace();
//                }
//
//                LineItem li = order.getLineItems().get(0);
//                li.setPrice(100L);
//                return null;
//            }
//        }.execute();
    }
}
