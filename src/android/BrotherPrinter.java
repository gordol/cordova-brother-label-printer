package com.threescreens.cordova.plugin.brotherPrinter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.app.PendingIntent;

import com.brother.ptouch.sdk.LabelInfo;
import com.brother.ptouch.sdk.NetPrinter;
import com.brother.ptouch.sdk.Printer;
import com.brother.ptouch.sdk.PrinterInfo;
import com.brother.ptouch.sdk.PrinterStatus;

public class BrotherPrinter extends CordovaPlugin {

    String modelName = "QL-720NW";
    private NetPrinter[] netPrinters;

    private String ipAddress   = null;
    private String macAddress  = null;
    private Boolean searched   = false;
    private Boolean found      = false;

    //token to make it easy to grep logcat
    private static final String TAG = "print";

    private CallbackContext callbackctx;

    @Override
    public boolean execute (String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if ("findNetworkPrinters".equals(action)) {
            findNetworkPrinters(callbackContext);
            return true;
        }

        if ("printViaSDK".equals(action)) {
            printViaSDK(args, callbackContext);
            return true;
        }

        if ("sendUSBConfig".equals(action)) {
            sendUSBConfig(args, callbackContext);
            return true;
        }

        return false;
    }

    private NetPrinter[] enumerateNetPrinters() {
        Printer myPrinter = new Printer();
        PrinterInfo myPrinterInfo = new PrinterInfo();
        netPrinters = myPrinter.getNetPrinters(modelName);
        return netPrinters;
    }

    private void findNetworkPrinters(final CallbackContext callbackctx) {

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try{

                    searched = true;

                    NetPrinter[] netPrinters = enumerateNetPrinters();
                    int netPrinterCount = netPrinters.length;

                    ArrayList<Map> netPrintersList = null;
                    if(netPrintersList != null) netPrintersList.clear();
                    netPrintersList = new ArrayList<Map>();

                    if (netPrinterCount > 0) {
                        found = true;
                        Log.d(TAG, "---- network printers found! ----");

                        for (int i = 0; i < netPrinterCount; i++) {
                            Map<String, String> netPrinter = new HashMap<String, String>();

                            ipAddress = netPrinters[i].ipAddress;
                            macAddress = netPrinters[i].macAddress;

                            netPrinter.put("ipAddress", netPrinters[i].ipAddress);
                            netPrinter.put("macAddress", netPrinters[i].macAddress);
                            netPrinter.put("serNo", netPrinters[i].serNo);
                            netPrinter.put("nodeName", netPrinters[i].nodeName);

                            netPrintersList.add(netPrinter);

                            Log.d(TAG, 
                                        " idx:    " + Integer.toString(i)
                                    + "\n model:  " + netPrinters[i].modelName
                                    + "\n ip:     " + netPrinters[i].ipAddress
                                    + "\n mac:    " + netPrinters[i].macAddress
                                    + "\n serial: " + netPrinters[i].serNo
                                    + "\n name:   " + netPrinters[i].nodeName
                                 );
                        }

                        Log.d(TAG, "---- /network printers found! ----");

                    }else if (netPrinterCount == 0 ) { 
                        found = false;
                        Log.d(TAG, "!!!! No network printers found !!!!");
                    }

                    JSONArray args = new JSONArray();
                    PluginResult result;

                    Boolean available = netPrinterCount > 0;

                    args.put(available);
                    args.put(netPrintersList);

                    result = new PluginResult(PluginResult.Status.OK, args);

                    callbackctx.sendPluginResult(result);

                }catch(Exception e){    
                    e.printStackTrace();
                }

            }

        });

    }

    public static Bitmap bmpFromBase64(String base64, final CallbackContext callbackctx){
        try{
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }catch(Exception e){    
            e.printStackTrace();
            return null;
        }
    }

    private void printViaSDK(final JSONArray args, final CallbackContext callbackctx) {

        final Bitmap bitmap = bmpFromBase64(args.optString(0, null), callbackctx);

        if(!searched){
            PluginResult result;
            result = new PluginResult(PluginResult.Status.ERROR, "You must first run findNetworkPrinters() to search the network.");
            callbackctx.sendPluginResult(result);
        }

        if(!found){
            PluginResult result;
            result = new PluginResult(PluginResult.Status.ERROR, "No printer was found. Aborting.");
            callbackctx.sendPluginResult(result);
        }

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try{

                    Printer myPrinter = new Printer();
                    PrinterInfo myPrinterInfo = new PrinterInfo();

                    myPrinterInfo = myPrinter.getPrinterInfo();

                    myPrinterInfo.printerModel  = PrinterInfo.Model.QL_720NW;
                    myPrinterInfo.port          = PrinterInfo.Port.NET;
                    myPrinterInfo.printMode     = PrinterInfo.PrintMode.ORIGINAL;
                    myPrinterInfo.orientation   = PrinterInfo.Orientation.PORTRAIT;
                    myPrinterInfo.paperSize     = PrinterInfo.PaperSize.CUSTOM;
                    myPrinterInfo.ipAddress     = ipAddress;
                    myPrinterInfo.macAddress    = macAddress;

                    myPrinter.setPrinterInfo(myPrinterInfo);

                    LabelInfo myLabelInfo = new LabelInfo();

                    myLabelInfo.labelNameIndex  = myPrinter.checkLabelInPrinter();
                    myLabelInfo.isAutoCut       = true;
                    myLabelInfo.isEndCut        = true;
                    myLabelInfo.isHalfCut       = false;
                    myLabelInfo.isSpecialTape   = false;

                    //label info must be set after setPrinterInfo, it's not in the docs
                    myPrinter.setLabelInfo(myLabelInfo);

                    String labelWidth = ""+myPrinter.getLabelParam().labelWidth;
                    String paperWidth = ""+myPrinter.getLabelParam().paperWidth;
                    Log.d(TAG, "paperWidth = " + paperWidth);
                    Log.d(TAG, "labelWidth = " + labelWidth);
                    
                    PrinterStatus status = myPrinter.printImage(bitmap);

                    //casting to string doesn't work, but this does... wtf Brother
                    String status_code = ""+status.errorCode;

                    Log.d(TAG, "PrinterStatus: "+status_code);

                    PluginResult result;
                    result = new PluginResult(PluginResult.Status.OK, status_code);
                    callbackctx.sendPluginResult(result);

                }catch(Exception e){    
                    e.printStackTrace();
                }
            }
        });
    }


    private void sendUSBConfig(final JSONArray args, final CallbackContext callbackctx){

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {

                Printer myPrinter = new Printer();

                Context context = cordova.getActivity().getApplicationContext();

                UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                UsbDevice usbDevice = myPrinter.getUsbDevice(usbManager);
                if (usbDevice == null) {
                    Log.d(TAG, "USB device not found");
                    return;
                }

                final String ACTION_USB_PERMISSION = "com.threescreens.cordova.plugin.brotherPrinter.USB_PERMISSION";

                PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
                usbManager.requestPermission(usbDevice, permissionIntent);

                final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        if (ACTION_USB_PERMISSION.equals(action)) {
                            synchronized (this) {
                                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                                    Log.d(TAG, "USB permission granted");
                                else
                                    Log.d(TAG, "USB permission rejected");
                            }
                        }
                    }
                };

                context.registerReceiver(mUsbReceiver, new IntentFilter(ACTION_USB_PERMISSION));

                while (true) {
                    if (!usbManager.hasPermission(usbDevice)) {
                        usbManager.requestPermission(usbDevice, permissionIntent);
                    } else {
                        break; 
                    }

                    try { 
                        Thread.sleep(1000);
                    } catch (InterruptedException e) { 
                        e.printStackTrace();
                    }
                }

                PrinterInfo myPrinterInfo = new PrinterInfo();

                myPrinterInfo = myPrinter.getPrinterInfo();

                myPrinterInfo.printerModel  = PrinterInfo.Model.QL_720NW;
                myPrinterInfo.port          = PrinterInfo.Port.USB;
                myPrinterInfo.paperSize     = PrinterInfo.PaperSize.CUSTOM;

                myPrinter.setPrinterInfo(myPrinterInfo);

                LabelInfo myLabelInfo = new LabelInfo();

                myLabelInfo.labelNameIndex  = myPrinter.checkLabelInPrinter();
                myLabelInfo.isAutoCut       = true;
                myLabelInfo.isEndCut        = true;
                myLabelInfo.isHalfCut       = false;
                myLabelInfo.isSpecialTape   = false;

                //label info must be set after setPrinterInfo, it's not in the docs
                myPrinter.setLabelInfo(myLabelInfo);


                try {
                    File outputDir = context.getCacheDir();
                    File outputFile = new File(outputDir.getPath() + "configure.prn");

                    FileWriter writer = new FileWriter(outputFile);
                    writer.write(args.optString(0, null));
                    writer.close();

                    PrinterStatus status = myPrinter.printFile(outputFile.toString());
                    outputFile.delete();

                    String status_code = ""+status.errorCode;

                    Log.d(TAG, "PrinterStatus: "+status_code);

                    PluginResult result;
                    result = new PluginResult(PluginResult.Status.OK, status_code);
                    callbackctx.sendPluginResult(result);

                } catch (IOException e) {
                    Log.d(TAG, "Temp file action failed: " + e.toString());
                } 

            }
        });
    }

}
