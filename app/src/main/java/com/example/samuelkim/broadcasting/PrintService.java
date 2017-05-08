package com.example.samuelkim.broadcasting;

import android.accounts.Account;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import com.clover.sdk.util.CloverAccount;
import com.clover.sdk.v1.BindingException;
import com.clover.sdk.v1.ClientException;
import com.clover.sdk.v1.ServiceException;
import com.clover.sdk.v1.printer.Category;
import com.clover.sdk.v1.printer.Printer;
import com.clover.sdk.v1.printer.PrinterConnector;
import com.clover.sdk.v1.printer.job.PrintJob;
import com.clover.sdk.v1.printer.job.StaticBillPrintJob;
import com.clover.sdk.v1.printer.job.StaticOrderPrintJob;
import com.clover.sdk.v1.printer.job.StaticPaymentPrintJob;
import com.clover.sdk.v3.order.Order;
import com.clover.sdk.v3.order.OrderConnector;

import java.util.List;

/**
 * Created by samuel.kim on 5/1/17.
 */

public class PrintService extends Service {

    private Context mContext;
    private Account mAccount;

    public static final String PRINT_TYPE = "PRINT_TYPE";
    public static final String ORDER_ID = "ORDER_ID";
    public static final String PRINT_ORDER = "PRINT_ORDER";
    public static final String FIRE_ORDER = "FIRE_ORDER";
    public static final String PRINT_BILL = "PRINT_BILL";
    public static final String PRINT_RECEIPT = "PRINT_RECEIPT";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mContext = getApplicationContext();
        mAccount = CloverAccount.getAccount(mContext);

        if(intent == null){
            stopSelf();
            return START_NOT_STICKY;
        }

        String printType = intent.getStringExtra(PRINT_TYPE);
        String orderID = intent.getStringExtra(ORDER_ID);

        if(!orderID.equals("")) {
            switch (printType) {
                case PRINT_ORDER:
                    PrintOrder(orderID);
                    break;
                case FIRE_ORDER:
                    FireOrder(orderID);
                    break;
                case PRINT_BILL:
                    PrintBill(orderID);
                    break;
                case PRINT_RECEIPT:
                    PrintReceipt(orderID);
                    break;
            }
        }

        return START_NOT_STICKY;
    }

    private List<Printer> GetPrinters(Category printerCategory) {
        PrinterConnector printerConnector = new PrinterConnector(mContext, mAccount, null);
        printerConnector.connect();

        List<Printer> result = null;
        try {
            result = printerConnector.getPrinters(printerCategory);
        } catch (RemoteException | ClientException | BindingException | ServiceException e) {
            e.printStackTrace();
        }

        printerConnector.disconnect();

        return result;
    }

    public void PrintOrder(String orderID){
        Runnable runner = new PrintOrderRunnable(orderID);
        Thread runIt = new Thread(runner);
        runIt.start();
    }

    private class PrintOrderRunnable implements Runnable {

        private String orderID;

        PrintOrderRunnable(String id){
            orderID = id;
        }

        public void run() {
            OrderConnector orderConn = new OrderConnector(mContext, mAccount, null);
            orderConn.connect();

            Order newOrder = null;
            try {
                newOrder = orderConn.getOrder(orderID);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(newOrder != null) {
                boolean useBackup = true;
                PrintJob pj = new StaticOrderPrintJob.Builder().markPrinted(true).order(newOrder).build();

                List<Printer> printerList = GetPrinters(Category.ORDER);
                if(printerList != null && !printerList.isEmpty()) {
                    for (Printer printer : printerList) {
                        try {
                            pj.print(mContext, mAccount, printer);
                            useBackup = false;
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (useBackup) {
                    try {
                        pj.print(mContext, mAccount);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            orderConn.disconnect();
        }
    }

    public void FireOrder(String orderID){
        Runnable runner = new FireOrderRunnable(orderID);
        Thread runIt = new Thread(runner);
        runIt.start();
    }

    private class FireOrderRunnable implements Runnable {

        private String orderID;

        FireOrderRunnable(String id){
            orderID = id;
        }

        public void run() {
            OrderConnector orderConn = new OrderConnector(mContext, mAccount, null);
            orderConn.connect();

            try {
                orderConn.fire(orderID);
            } catch (RemoteException | ClientException | ServiceException | BindingException e) {
                e.printStackTrace();
            }

            orderConn.disconnect();
        }
    }

    public void PrintReceipt(String orderID){
        Runnable runner = new PrintReceiptRunnable(orderID);
        Thread runIt = new Thread(runner);
        runIt.start();
    }

    private class PrintReceiptRunnable implements Runnable {

        private String orderID;

        PrintReceiptRunnable(String id){
            orderID = id;
        }

        public void run() {
            OrderConnector orderConn = new OrderConnector(mContext, mAccount, null);
            orderConn.connect();

            Order newOrder = null;
            try {
                newOrder = orderConn.getOrder(orderID);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(newOrder != null) {
                //If the order is open, then we need to print the Bill instead
                if(newOrder.getState().toLowerCase().equals("open")){
                    PrintBill(orderID);
                }else {
                    boolean useBackup = true;
                    PrintJob pj = new StaticPaymentPrintJob.Builder().order(newOrder).build();

                    List<Printer> printerList = GetPrinters(Category.RECEIPT);
                    if (printerList != null && !printerList.isEmpty()) {
                        for (Printer printer : printerList) {
                            try {
                                pj.print(mContext, mAccount, printer);
                                useBackup = false;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (useBackup) {
                        try {
                            pj.print(mContext, mAccount);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            orderConn.disconnect();
        }
    }

    public void PrintBill(String orderID){
        Runnable runner = new PrintBillRunnable(orderID);
        Thread runIt = new Thread(runner);
        runIt.start();
    }

    private class PrintBillRunnable implements Runnable {

        private String orderID;

        PrintBillRunnable(String id){
            orderID = id;
        }

        public void run() {
            OrderConnector orderConn = new OrderConnector(mContext, mAccount, null);
            orderConn.connect();

            Order newOrder = null;
            try {
                newOrder = orderConn.getOrder(orderID);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(newOrder != null) {
                boolean useBackup = true;
                PrintJob pj = new StaticBillPrintJob.Builder().markPrinted(true).order(newOrder).build();

                List<Printer> printerList = GetPrinters(Category.RECEIPT);
                if(printerList != null && !printerList.isEmpty()) {
                    for (Printer printer : printerList) {
                        try {
                            pj.print(mContext, mAccount, printer);
                            useBackup = false;
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (useBackup) {
                    try {
                        pj.print(mContext, mAccount);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            orderConn.disconnect();
        }
    }
}
