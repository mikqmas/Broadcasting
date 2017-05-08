package com.example.samuelkim.broadcasting;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.clover.sdk.v1.Intents;

/**
 * Created by samuel.kim on 2/21/17.
 */

public class ConsumReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, intent.toString(), Toast.LENGTH_LONG).show();
//        if(intent.getAction().equals(Intents.ACTION_ACTIVE_REGISTER_ORDER)){
//            Toast.makeText(context, "test!@#@!", Toast.LENGTH_LONG).show();
//            abortBroadcast();
//        }
    }
}
