package com.threescreens.cordova.plugin.brotherPrinter;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import com.brother.ptouch.sdk.LabelInfo;
import com.brother.ptouch.sdk.NetPrinter;
import com.brother.ptouch.sdk.Printer;
import com.brother.ptouch.sdk.PrinterInfo;
import com.brother.ptouch.sdk.PrinterInfo.ErrorCode;
import com.brother.ptouch.sdk.PrinterStatus;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 *   BrotherPrinter - cordova plugin providing access to networked printers
 *
 *   - updated March 24, 2018
 *     > added method for searching printers
 *     > implemented PDF printing
 *     > supplied updated printer list based upon latest Brother SDK (3.0.9)
 *     > introduced Javadoc
 */
public class BrotherPrinter extends CordovaPlugin {

    //Holds the name of the valid network printers to discover during search
    private static final String[] modelNames = {"710W","720NW","800","810W","820NWB","QL-710W","QL-720NW","QL-800","QL-800W","QL-820NWB"};

    //Holds valid paper sizes for the QL series printers
    private static final String[] PS_QL = {"W17H54", "W17H87", "W23H23", "W29H42", "W29H90", "W38H90", "W39H48", "W52H29", "W54H29", "W62H29", "W62H100", "W60H86", "W12", "W29", "W38", "W50", "W54", "W62", "W62RB"};

    //Holds the map of NetPrinter objects available based upon the search operation, keyed by their serial number
    private Map<String,Map<String,String>> discoveredNetworkPrinters;

    //Holds the NetPrinter (converted to a Map object) selected during search
    private Map<String,String> selectedPrinter;

    //Holds the token to make it easy to grep logcat
    private static final String TAG = "[BrotherPrinter Plugin]";

    //the callback context through which responses to plugin requests should be sent
    private CallbackContext callbackctx;

    /**
     * entry point to the plugin from the cordova context
     *
     * @param action the method to trigger
     * @param args the JSON array of arguments to pass to the requested method
     * @param callbackContext the context through which results from the requested method should be passed
     *
     * @throws JSONException if the arguments are not compatible (TODO: clarify this doc entry)
     *
     */
    @Override
    public boolean execute (String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if ("findNetworkPrinters".equals(action)) {
            findNetworkPrinters(args, callbackContext);
            return true;
        }

        if ("setSessionPrinter".equals(action)) {
            setSessionPrinter(args, callbackContext);
            return true;
        }

        if ("printViaSDK".equals(action)) {
            printBitmapImage(args, callbackContext);
            return true;
        }

        if ("printBitmapImage".equals(action)) {
            printBitmapImage(args, callbackContext);
            return true;
        }

        if ("printPdf".equals(action)) {
            printPdf(args, callbackContext);
            return true;
        }

        if ("sendUSBConfig".equals(action)) {
            sendUSBConfig(args, callbackContext);
            return true;
        }

        return false;
    }

    /**
     *  utility method used to process printer search using @link #modelNames provided
     *
     *  @return an array of printers that were found
     */
    private NetPrinter[] enumerateNetPrinters() {
        Printer myPrinter = new Printer();
        PrinterInfo myPrinterInfo = new PrinterInfo();
        NetPrinter[] _netPrinters = myPrinter.getNetPrinters(modelNames);
        return _netPrinters;
    }

    /**
     *  uses the provided serial number to set the session printer from list of NetPrinter objects discovered
     *
     *  @param args JSONArray containing the serial number of the printer to select
     *  @param callbackctx the context provided by the method invoking this request
     *
     */
    private void setSessionPrinter(final JSONArray args, final CallbackContext callbackctx){

        final String serialnumber = args.optString(0,null);

        if( serialnumber != null && discoveredNetworkPrinters != null ){
            //check for printer serial number in the list of found printers
            this.selectedPrinter = discoveredNetworkPrinters.get(serialnumber);
        }

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try{
                    PluginResult result;
                    if( selectedPrinter == null ){
                        String message = "No printers found with the serial number provided.";
                        if( discoveredNetworkPrinters == null || discoveredNetworkPrinters.size() == 0 ){
                            message = "No printers were discovered with findNetworkPrinters() method.";
                        }
                        result = new PluginResult(PluginResult.Status.ERROR, message);
                    }
                    else{
                        result = new PluginResult(PluginResult.Status.OK, "printer set");
                    }
                    callbackctx.sendPluginResult(result);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });

    }

    /**
     * searches the network for a printer matching those deemed valid for the plugin (contained in {@link #modelNames})
     *
     * @param callbackctx the context provided by the method invoking this request
     */
    private void findNetworkPrinters(final JSONArray args, final CallbackContext callbackctx) {

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try{

                    NetPrinter[] _netPrinters = enumerateNetPrinters();
                    int totalPrinters = 0;

                    if( _netPrinters != null && _netPrinters.length > 0 ){

                        Log.d(TAG, "---- network printers found! ----");

                        totalPrinters = _netPrinters.length;
                        discoveredNetworkPrinters = new HashMap<String,Map<String,String>>();
                        ArrayList<NetPrinter> netPrintersList = new ArrayList<NetPrinter>(Arrays.asList(_netPrinters));
                        for( NetPrinter np : netPrintersList ){

                            Map<String,String> _printer = new HashMap<String,String>();
                            _printer.put("ipAddress", np.ipAddress);
                            _printer.put("macAddress", np.macAddress);
                            _printer.put("serNo", np.serNo);
                            _printer.put("nodeName", np.nodeName);
                            _printer.put("paperName","W62H100");
                            _printer.put("paperNameArray",Arrays.toString(PS_QL));
                            discoveredNetworkPrinters.put(np.serNo,_printer);

                            Log.d(TAG, "model:  " + np.modelName
                                        + "\n ip:     " + np.ipAddress
                                        + "\n mac:    " + np.macAddress
                                        + "\n serial: " + np.serNo
                                        + "\n name:   " + np.nodeName
                            );

                        };

                        Log.d(TAG, "---- /network printers found! ----");
                    }
                    else{
                        //no network printers could be found based upon the modelNames search parameter supplied
                        Log.d(TAG, "!!!! No compatible network printers found !!!!");
                    }

                    args.put(totalPrinters);
                    args.put((new JSONObject(discoveredNetworkPrinters)).toString());
                    PluginResult result = new PluginResult(PluginResult.Status.OK, args);
                    callbackctx.sendPluginResult(result);

                }catch(Exception e){
                    e.printStackTrace();
                }

            }

        });

    }

    /**
     *  utility method to create a bitmap from a base64 string
     *
     *  @param base64 the string to convert to an image
     *  @param callbackctx the context provided by the method invoking this request
     *  @return a bitmap {@see android.graphics.Bitmap} image
     */
    public static Bitmap bmpFromBase64(String base64, final CallbackContext callbackctx){
        try{
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     *  prints a PDF using a file path provided and defined printer
     *
     *  @param args JSONArray containing the filepath string and printer object to use
     *  @param callbackctx the context provided by the method invoking this request
     */
    private void printPdf(final JSONArray args, final CallbackContext callbackctx) {

        final String filepath = args.optString(0,null);
        final String param1 = args.optString(1,null);
        final String param2 = args.optString(2,null);

        if( param1 != null && param1.startsWith("printer")
                                    && discoveredNetworkPrinters != null ){
            //check for printer serial number in the list of found printers
            //this assumes the user is overriding any prior session printer that may have been selected
            this.selectedPrinter = discoveredNetworkPrinters.get(param1.split(":")[1].trim());
        }

        //user may have supplied a paper name manually
        if( param1 != null && param1.startsWith("paper") ){
            this.selectedPrinter.put("paperName",param1.split(":")[1].trim());
        }
        else if( param2 != null ){
            this.selectedPrinter.put("paperName",param2.split(":")[1].trim());
        }

        if(this.selectedPrinter == null){
            PluginResult result;
            result = new PluginResult(PluginResult.Status.ERROR, "No printers have been selected. You must first run findNetworkPrinters() to search the network.");
            callbackctx.sendPluginResult(result);
        }

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try{

                    Printer myPrinter = new Printer();

                    PrinterInfo myPrinterInfo = myPrinter.getPrinterInfo();

                    myPrinterInfo.printerModel  = PrinterInfo.Model.QL_720NW;
                    myPrinterInfo.port          = PrinterInfo.Port.NET;
                    myPrinterInfo.printMode     = PrinterInfo.PrintMode.ORIGINAL;
                    myPrinterInfo.orientation   = PrinterInfo.Orientation.PORTRAIT;
                    myPrinterInfo.paperSize     = PrinterInfo.PaperSize.CUSTOM;
                    myPrinterInfo.ipAddress     = selectedPrinter.get("ipAddress");
                    myPrinterInfo.macAddress    = selectedPrinter.get("macAddress");
                    myPrinterInfo.isAutoCut     = true;
                    myPrinterInfo.isCutAtEnd    = true;

                    myPrinterInfo.labelNameIndex = LabelInfo.QL700.valueOf(selectedPrinter.get("paperName")).ordinal();

                    myPrinter.setPrinterInfo(myPrinterInfo);

                    PrinterStatus status = new PrinterStatus();

                    //get the total number of pages in the PDF
                    int totalpages = 0;
                    if (Build.VERSION.SDK_INT < 21) {
                        totalpages = myPrinter.getPDFPages(filepath);
                    } else {
                        totalpages = myPrinter.getPDFFilePages(filepath);
                    }

                    for (int i = 0; i < totalpages; i++) {
                        if (Build.VERSION.SDK_INT < 21) {
                            status = myPrinter.printPDF(filepath, i+1);
                        } else {
                            status = myPrinter.printPdfFile(filepath, i+1);
                        }
                        if (status.errorCode != ErrorCode.ERROR_NONE) {
                            break;
                        }
                    }

                    //converting the enum ErrorCode object to a string for debugging
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

    /**
     *  prints a BMP image using the base64 string representing the file contents
     *
     *  @param args JSONArray containing the filepath string and printer object to use
     *  @param callbackctx the context provided by the method invoking this request
     *
     */
    private void printBitmapImage(final JSONArray args, final CallbackContext callbackctx) {

        final Bitmap bitmap = bmpFromBase64(args.optString(0, null), callbackctx);
        final String param1 = args.optString(1,null);
        final String param2 = args.optString(2,null);

        if( param1 != null && param1.startsWith("printer")
                                    && discoveredNetworkPrinters != null ){
            //check for printer serial number in the list of found printers
            //this assumes the user is overriding any prior session printer that may have been selected
            this.selectedPrinter = discoveredNetworkPrinters.get(param1.split(":")[1].trim());
        }

        //user may have supplied a paper name manually
        if( param1 != null && param1.startsWith("paper") ){
            this.selectedPrinter.put("paperName",param1.split(":")[1].trim());
        }
        else if( param2 != null ){
            this.selectedPrinter.put("paperName",param2.split(":")[1].trim());
        }

        if(this.selectedPrinter == null){
            PluginResult result;
            result = new PluginResult(PluginResult.Status.ERROR, "No printers have been selected. You must first run findNetworkPrinters() to search the network.");
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
                    myPrinterInfo.ipAddress     = selectedPrinter.get("ipAddress");
                    myPrinterInfo.macAddress    = selectedPrinter.get("macAddress");
                    myPrinterInfo.isAutoCut     = true;
                    myPrinterInfo.isCutAtEnd    = true;

                    //this may need to be parameterized via options arguments
                    myPrinterInfo.labelNameIndex = LabelInfo.QL700.valueOf(selectedPrinter.get("paperName")).ordinal();

                    myPrinter.setPrinterInfo(myPrinterInfo);

                    PrinterStatus status = myPrinter.printImage(bitmap);

                    //converting the enum ErrorCode object to a string for debugging
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


    /**
     *  captures the USB configuration settings for the attached printers
     *
     *  TODO provide clarification as to what this method does
     *  @param args JSONArray containing parameters
     *  @param callbackctx the context provided by the method invoking this request
     */
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

                LabelInfo myLabelInfo = myPrinter.getLabelInfo();

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
