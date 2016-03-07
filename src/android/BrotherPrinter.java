package com.threescreens.cordova.plugin.brotherPrinter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.io.InputStream;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
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
                    //myPrinterInfo.printMode     = PrinterInfo.PrintMode.FIT_TO_PAGE;
                    myPrinterInfo.printMode     = PrinterInfo.PrintMode.ORIGINAL;
                    //myPrinterInfo.orientation   = PrinterInfo.Orientation.LANDSCAPE;
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
}
