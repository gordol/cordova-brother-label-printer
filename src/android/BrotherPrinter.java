package com.threescreens.cordova.plugin.brotherPrinter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

//these were for hidden webview to transform HTML into a bitmap for label generation, not used at the moment, but may bring it back
//import android.graphics.Canvas;
//import android.graphics.Picture;
//import android.os.Handler;
//import android.view.View;
//import android.view.ViewGroup;
//import android.webkit.WebView;
//import android.webkit.WebViewClient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.app.PendingIntent;

import com.brother.ptouch.sdk.NetPrinter;
import com.brother.ptouch.sdk.Printer;
import com.brother.ptouch.sdk.PrinterInfo;
import com.brother.ptouch.sdk.LabelInfo;
import com.brother.ptouch.sdk.PrinterStatus;

public class BrotherPrinter extends CordovaPlugin {

    private Printer myPrinter = new Printer();
    private PrinterInfo myPrinterInfo = new PrinterInfo();

    private static PrinterInfo.Model[] supportedModels = {
        PrinterInfo.Model.QL_720NW,
        PrinterInfo.Model.QL_820NWB,
        PrinterInfo.Model.TD_2120N,
    };

    private final static int PERMISSION_WRITE_EXTERNAL_STORAGE = 1;

    private boolean isPermitWriteStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (cordova.getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        if (!isPermitWriteStorage()) {
            cordova.requestPermission(this, PERMISSION_WRITE_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }


    //token to make it easy to grep logcat
    private static final String TAG = "BrotherPrinter";


    @Override
    public boolean execute (String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if ("findNetworkPrinters".equals(action)) {
            findNetworkPrinters(callbackContext);
            return true;
        }

        if ("findBluetoothPrinters".equals(action)) {
            findBluetoothPrinters(callbackContext);
            return true;
        }

        if ("findPrinters".equals(action)) {
            findPrinters(callbackContext);
            return true;
        }

        if ("setPrinter".equals(action)) {
            setPrinter(args, callbackContext);
            return true;
        }

        if ("setLabelInfo".equals(action)) {
            setLabelInfo(args, callbackContext);
            return true;
        }

        if ("printViaSDK".equals(action)) {
            printViaSDK(args, callbackContext);
            return true;
        }

        if ("printerStatus".equals(action)) {
            printerStatus(callbackContext);
            return true;
        }

        if ("sendUSBConfig".equals(action)) {
            sendUSBConfig(args, callbackContext);
            return true;
        }

        return false;
    }


    private class DiscoveredPrinter {
        public PrinterInfo.Model model;
        public PrinterInfo.Port port;
        public String modelName;
        public String serNo;
        public String ipAddress;
        public String macAddress;
        public String nodeName;
        public String location;

        public DiscoveredPrinter(BluetoothDevice device) {
            port = PrinterInfo.Port.BLUETOOTH;
            ipAddress = null;
            serNo = null;
            nodeName = null;
            location = null;
            macAddress = device.getAddress();
            modelName = device.getName();

            String deviceName = device.getName();
            PrinterInfo.Model[] models = PrinterInfo.Model.values();
            for (PrinterInfo.Model model : models) {
                String modelName = model.toString().replaceAll("_", "-");
                if (deviceName.contains(modelName)) {
                    this.model = model;
                    break;
                }
            }
        }


        public DiscoveredPrinter(NetPrinter printer) {
            port = PrinterInfo.Port.NET;
            modelName = printer.modelName;
            ipAddress = printer.ipAddress;
            macAddress = printer.macAddress;
            nodeName = printer.nodeName;
            location = printer.location;

            PrinterInfo.Model[] models = PrinterInfo.Model.values();
            for (PrinterInfo.Model model : models) {
                String modelName = model.toString().replaceAll("_", "-");
                if (printer.modelName.contains(modelName)) {
                    this.model = model;
                    break;
                }
            }
        }


        public DiscoveredPrinter(JSONObject object) throws JSONException {
            model = PrinterInfo.Model.valueOf(object.getString("model"));
            port = PrinterInfo.Port.valueOf(object.getString("port"));

            if (object.has("modelName")) {
                modelName = object.getString("modelName");
            }

            if (object.has("ipAddress")) {
                ipAddress = object.getString("ipAddress");
            }

            if (object.has("macAddress")) {
                macAddress = object.getString("macAddress");
            }

            if (object.has("serialNumber")) {
                serNo = object.getString("serialNumber");
            }

            if (object.has("nodeName")) {
                nodeName = object.getString("nodeName");
            }

            if (object.has("location")) {
                location = object.getString("location");
            }
        }


        public JSONObject toJSON() throws JSONException {
            JSONObject result = new JSONObject();
            result.put("model", model.toString());
            result.put("port", port.toString());
            result.put("modelName", modelName);
            result.put("ipAddress", ipAddress);
            result.put("macAddress", macAddress);
            result.put("serialNumber", serNo);
            result.put("nodeName", nodeName);
            result.put("location", location);

            return result;
        }
    }


    private List<DiscoveredPrinter> enumerateNetPrinters() {
        ArrayList<DiscoveredPrinter> results = new ArrayList<DiscoveredPrinter>();
        try {
            String[] models = new String[supportedModels.length];
            for (int i = 0; i < supportedModels.length; i++) {
                models[i] = supportedModels[i].toString().replaceAll("_", "-");
            }

            NetPrinter[] printers = myPrinter.getNetPrinters(models);
            for (int i = 0; i < printers.length; i++) {
                results.add(new DiscoveredPrinter(printers[i]));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }


    private List<DiscoveredPrinter> enumerateBluetoothPrinters() {
        ArrayList<DiscoveredPrinter> results = new ArrayList<DiscoveredPrinter>();
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                return results;
            }

            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                cordova.getActivity().startActivity(enableBtIntent);
            }

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices == null || pairedDevices.size() == 0) {
                return results;
            }

            for (BluetoothDevice device : pairedDevices) {
                DiscoveredPrinter printer = new DiscoveredPrinter(device);

                if (printer.model == null) {
                    continue;
                }
                results.add(printer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }


    private void sendDiscoveredPrinters(final CallbackContext callbackctx, List<DiscoveredPrinter> discoveredPrinters) {
        JSONArray args = new JSONArray();

        for (DiscoveredPrinter p : discoveredPrinters) {
            try{
                args.put(p.toJSON());
            } catch(JSONException e) {
                // ignore this exception for now.
                e.printStackTrace();
            }
        }

        PluginResult result = new PluginResult(PluginResult.Status.OK, args);
        callbackctx.sendPluginResult(result);
    }

    private void findNetworkPrinters(final CallbackContext callbackctx) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                List<DiscoveredPrinter> discoveredPrinters = enumerateNetPrinters();
                sendDiscoveredPrinters(callbackctx, discoveredPrinters);
            }

        });
    }

    private void findBluetoothPrinters(final CallbackContext callbackctx) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                List<DiscoveredPrinter> discoveredPrinters = enumerateBluetoothPrinters();
                sendDiscoveredPrinters(callbackctx, discoveredPrinters);
            }
        });
    }

    private void findPrinters(final CallbackContext callbackctx) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                List<DiscoveredPrinter> allPrinters = enumerateNetPrinters();
                allPrinters.addAll(enumerateBluetoothPrinters());
                sendDiscoveredPrinters(callbackctx, allPrinters);
            }
        });
    }

    private void setPrinter(JSONArray args, final CallbackContext callbackctx) {
        try {
            JSONObject obj = args.getJSONObject(0);
            DiscoveredPrinter printer = new DiscoveredPrinter(obj);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());

            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putString("printerModel", printer.model.toString());
            editor.putString("port", printer.port.toString());
            editor.putString("ipAddress", printer.ipAddress);
            editor.putString("macAddress", printer.macAddress);
            editor.commit();

            PluginResult result = new PluginResult(PluginResult.Status.OK, args);
            callbackctx.sendPluginResult(result);
        } catch (JSONException e) {
            e.printStackTrace();
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, "An error occurred while trying to set the printer.");
            callbackctx.sendPluginResult(result);
        }
    }

    private void setLabelInfo(JSONArray args, final CallbackContext callbackctx) {
        try {
            JSONObject obj = args.getJSONObject(0);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());

            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putBoolean("useCustomLabel", obj.optBoolean("useCustomLabel", false));
            editor.putString("labelModel", obj.optString("labelModel", ""));
            editor.putString("labelName", obj.optString("labelName", ""));
            editor.putString("customPaper", obj.optString("customPaper", ""));
            editor.commit();

            PluginResult result = new PluginResult(PluginResult.Status.OK, args);
            callbackctx.sendPluginResult(result);
        } catch (JSONException e) {
            e.printStackTrace();
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, "An error occurred while trying to set the printer.");
            callbackctx.sendPluginResult(result);
        }
    }

    public static Bitmap bmpFromBase64(String base64){
        try{
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            InputStream stream = new ByteArrayInputStream(bytes);

            return BitmapFactory.decodeStream(stream);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private void printViaSDK(final JSONArray args, final CallbackContext callbackctx) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try{
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());

                    String port          = sharedPreferences.getString("port", "");
                    String printerModel  = sharedPreferences.getString("printerModel", "");
                    String ipAddress     = sharedPreferences.getString("ipAddress", "");

                    if ("".equals(port)) {
                        PluginResult result = new PluginResult(PluginResult.Status.ERROR, "No printer has been set.");
                        callbackctx.sendPluginResult(result);
                        return;
                    }

                    if (PrinterInfo.Port.BLUETOOTH.toString().equals(port)) {
                        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        if (bluetoothAdapter == null) {
                            PluginResult result = new PluginResult(PluginResult.Status.ERROR, "This device does not have a bluetooth adapter.");
                            callbackctx.sendPluginResult(result);
                            return;
                        }

                        if (!bluetoothAdapter.isEnabled()) {
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            cordova.getActivity().startActivity(enableBtIntent);
                        }

                        myPrinter.setBluetooth(bluetoothAdapter);
                    }

                    Bitmap bitmap = null;
                    try {
                        String encodedImg = args.getString(0);
                        bitmap = bmpFromBase64(encodedImg);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        PluginResult result = new PluginResult(PluginResult.Status.ERROR, "An error occurred while trying to retrieve the image passed in.");
                        callbackctx.sendPluginResult(result);
                        return;
                    }

                    if (bitmap == null) {
                        PluginResult result = new PluginResult(PluginResult.Status.ERROR, "The passed in data did not seem to be a decodable image. Please ensure it is a base64 encoded string of a supported Android format");
                        callbackctx.sendPluginResult(result);
                        return;
                    }

                    myPrinterInfo = myPrinter.getPrinterInfo();

                    myPrinterInfo.printerModel  = PrinterInfo.Model.valueOf(printerModel);
                    myPrinterInfo.port          = PrinterInfo.Port.valueOf(port);

                    if (PrinterInfo.Port.NET.toString().equals(port)) {
                        myPrinterInfo.ipAddress = ipAddress;
                    } else {
                        myPrinterInfo.macAddress = sharedPreferences.getString("macAddress", "");
                    }

                    myPrinter.setPrinterInfo(myPrinterInfo);

                    setPrinterLabelInfo(sharedPreferences);

                    myPrinter.startCommunication();
                    PrinterStatus status = myPrinter.printImage(bitmap);
                    myPrinter.endCommunication();

                    //casting to string doesn't work, but this does... wtf Brother
                    String status_code = ""+status.errorCode;

                    Log.d(TAG, "PrinterStatus: "+status_code);

                    PluginResult result = new PluginResult(PluginResult.Status.OK, status_code);
                    callbackctx.sendPluginResult(result);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    private int getLabelNameId(String modelName, String labelName) throws Exception {
        switch (modelName) {
            case "PT3":
                return LabelInfo.PT3.valueOf(labelName).ordinal();
            case "PT":
                return LabelInfo.PT.valueOf(labelName).ordinal();
            case "QL700":
                return LabelInfo.QL700.valueOf(labelName).ordinal();
            case "QL1115":
                return LabelInfo.QL1115.valueOf(labelName).ordinal();
            case "QL1100":
                return LabelInfo.QL1100.valueOf(labelName).ordinal();
            default:
                throw new Exception("Unrecognized model name for label");
        }
    }

    private void setPrinterLabelInfo(SharedPreferences sharedPreferences) throws Exception {
        Boolean useCustomLabel = sharedPreferences.getBoolean("useCustomLabel", false);
        String labelModel = sharedPreferences.getString("labelModel", "");
        String labelName = sharedPreferences.getString("labelName", "");
        String customPaper = sharedPreferences.getString("customPaper", "");

        if (useCustomLabel) {
            myPrinterInfo.labelNameIndex = getLabelNameId(labelModel, labelName);
        } else {
            myPrinter.startCommunication();
            myPrinterInfo.labelNameIndex = myPrinter.getLabelInfo().labelNameIndex;
            myPrinter.endCommunication();
        }

        myPrinterInfo.customPaper = customPaper;

        myPrinterInfo.isAutoCut = true;
        myPrinterInfo.isCutAtEnd = true;
        myPrinterInfo.isHalfCut = false;
        myPrinterInfo.isSpecialTape = false;

        myPrinter.setPrinterInfo(myPrinterInfo);
    }


    private void sendUSBConfig(final JSONArray args, final CallbackContext callbackctx){

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                Context context = cordova.getActivity().getApplicationContext();
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());
                String port = sharedPreferences.getString("port", "");

                if ("".equals(port)) {
                    PluginResult result = new PluginResult(PluginResult.Status.ERROR, "No printer has been set.");
                    callbackctx.sendPluginResult(result);
                    return;
                }

                Printer myPrinter = new Printer();

                if (PrinterInfo.Port.USB.toString().equals(port)) {

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
                }

                String printerModel  = sharedPreferences.getString("printerModel", "");
                String printerPort   = sharedPreferences.getString("port", "");
                String ipAddress     = sharedPreferences.getString("ipAddress", "");

                myPrinterInfo = myPrinter.getPrinterInfo();

                myPrinterInfo.printerModel  = PrinterInfo.Model.valueOf(printerModel);
                myPrinterInfo.port          = PrinterInfo.Port.valueOf(printerPort);

                if (PrinterInfo.Port.NET.toString().equals(port)) {
                    myPrinterInfo.ipAddress = PrinterInfo.Port.NET.valueOf(ipAddress).toString();
                }

                myPrinter.setPrinterInfo(myPrinterInfo);

                try {
                    File outputDir = context.getCacheDir();
                    File outputFile = new File(outputDir.getPath() + "configure.prn");

                    FileWriter writer = new FileWriter(outputFile);
                    writer.write(args.optString(0, null));
                    writer.close();


                    myPrinter.startCommunication();
                    PrinterStatus status = myPrinter.printFile(outputFile.toString());
                    myPrinter.endCommunication();
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


    private void printerStatus(final CallbackContext callbackctx) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try{

                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());

                    String port = sharedPreferences.getString("port", "");
                    if ("".equals(port)) {
                        PluginResult result = new PluginResult(PluginResult.Status.ERROR, "No printer has been set.");
                        callbackctx.sendPluginResult(result);
                        return;
                    }

                    if (PrinterInfo.Port.BLUETOOTH.toString().equals(port)) {
                        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        if (bluetoothAdapter == null) {
                            PluginResult result = new PluginResult(PluginResult.Status.ERROR, "This device does not have a bluetooth adapter.");
                            callbackctx.sendPluginResult(result);
                            return;
                        }

                        if (!bluetoothAdapter.isEnabled()) {
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            cordova.getActivity().startActivity(enableBtIntent);
                        }

                        myPrinter.setBluetooth(bluetoothAdapter);
                    }

                    myPrinterInfo = myPrinter.getPrinterInfo();

                    String printerModel  = sharedPreferences.getString("printerModel", "");
                    String printerPort   = sharedPreferences.getString("port", "");
                    String ipAddress     = sharedPreferences.getString("ipAddress", "");

                    myPrinterInfo.printerModel  = PrinterInfo.Model.valueOf(printerModel);
                    myPrinterInfo.port          = PrinterInfo.Port.valueOf(printerPort);

                    if (PrinterInfo.Port.NET.toString().equals(port)) {
                        myPrinterInfo.ipAddress = PrinterInfo.Port.NET.valueOf(ipAddress).toString();
                    }

                    myPrinter.setPrinterInfo(myPrinterInfo);

                    setPrinterLabelInfo(sharedPreferences);

                    myPrinter.startCommunication();
                    PrinterStatus status = myPrinter.getPrinterStatus();
                    myPrinter.endCommunication();

                    //casting to string doesn't work, but this does... wtf Brother
                    String status_code = ""+status.errorCode;

                    Log.d(TAG, "PrinterStatus: "+status_code);

                    PluginResult result = new PluginResult(PluginResult.Status.OK, status_code);
                    callbackctx.sendPluginResult(result);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
}
