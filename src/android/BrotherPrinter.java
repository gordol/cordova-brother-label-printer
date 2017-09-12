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
import org.json.JSONObject;
import org.json.JSONException;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telecom.Call;
import android.util.Base64;
import android.util.Log;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.app.PendingIntent;

import com.brother.ptouch.sdk.LabelInfo;
import com.brother.ptouch.sdk.NetPrinter;
import com.brother.ptouch.sdk.Printer;
import com.brother.ptouch.sdk.PrinterInfo;
import com.brother.ptouch.sdk.PrinterStatus;
import com.brother.ptouch.sdk.printdemo.common.MsgHandle;
import com.brother.ptouch.sdk.printdemo.printprocess.ImageBitmapPrint;
import com.brother.ptouch.sdk.printdemo.printprocess.ImageFilePrint;

public class BrotherPrinter extends CordovaPlugin {
    private static PrinterInfo.Model[] supportedModels = {
            PrinterInfo.Model.QL_720NW,
            PrinterInfo.Model.QL_820NWB,
    };

    private MsgHandle mHandle;
    private ImageBitmapPrint mBitmapPrint;
    private ImageFilePrint mFilePrint;

    private final static int PERMISSION_WRITE_EXTERNAL_STORAGE = 1;

    private boolean isPermitWriteStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (cordova.getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        mHandle = new MsgHandle(null);
        mBitmapPrint = new ImageBitmapPrint(cordova.getActivity(), mHandle);
        mFilePrint = new ImageFilePrint(cordova.getActivity(), mHandle);

        if (!isPermitWriteStorage()) {
            cordova.requestPermission(this, PERMISSION_WRITE_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    //token to make it easy to grep logcat
    private static final String TAG = "BrotherPrinter";

    @Override
    public boolean execute (String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if ("pairBluetoothPrinters".equals(action)) {
            pairBluetoothPrinters(callbackContext);
            return true;
        }

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
                if (deviceName.startsWith(modelName)) {
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
                if (printer.modelName.endsWith(modelName)) {
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
            Printer myPrinter = new Printer();
            PrinterInfo myPrinterInfo = new PrinterInfo();

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

    private static final int OPEN_BLUETOOTH_SETTINGS_REQUEST = 0x2345;
    private void pairBluetoothPrinters(final CallbackContext callbackctx) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                final Intent openPairSection = new Intent();
                openPairSection.setAction(Settings.ACTION_BLUETOOTH_SETTINGS);
                cordova.startActivityForResult(BrotherPrinter.this, openPairSection, OPEN_BLUETOOTH_SETTINGS_REQUEST);

                callbackctx.success();
            }
        });
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

            SharedPreferences sharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(cordova.getActivity());

            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putString("printerModel", printer.model.toString());
            editor.putString("port", printer.port.toString());
            editor.putString("address", printer.ipAddress);
            editor.putString("macAddress", printer.macAddress);
            editor.putString("paperSize", LabelInfo.QL700.W62.toString());
            editor.commit();

            PluginResult result = new PluginResult(PluginResult.Status.OK, args);
            callbackctx.sendPluginResult(result);
        } catch (JSONException e) {
            e.printStackTrace();
            PluginResult result = pluginErrorResult("setPrinter", 2, "an error occurred while trying to set the printer: " + e.getLocalizedMessage());
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
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(cordova.getActivity());

        String port = sharedPreferences.getString("port", "");
        if ("".equals(port)) {
            PluginResult result = pluginErrorResult("printViaSDK", 4, "no printer has been set.");
            callbackctx.sendPluginResult(result);
            return;
        }

        if (PrinterInfo.Port.BLUETOOTH.toString().equals(port)) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                PluginResult result = pluginErrorResult("printViaSDK", 3, "this device does not have a bluetooth adapter.");
                callbackctx.sendPluginResult(result);
                return;
            }

            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                cordova.getActivity().startActivity(enableBtIntent);
            }

            mBitmapPrint.setBluetoothAdapter(bluetoothAdapter);
            mFilePrint.setBluetoothAdapter(bluetoothAdapter);
        }

        Bitmap bitmap = null;
        try {
            String encodedImg = args.getString(0);
            bitmap = bmpFromBase64(encodedImg);
        } catch (JSONException e) {
            e.printStackTrace();
            PluginResult result = pluginErrorResult("printViaSDK", 1, "an error occurred while trying to retrieve the image passed in: " + e.getLocalizedMessage());
            callbackctx.sendPluginResult(result);
            return;
        }

        if (bitmap == null) {
            PluginResult result = pluginErrorResult("printViaSDK", 1, "the passed in data did not seemd to be a decoable image. Please ensure it is a base64 encoded stirng of a supported Android format.");
            callbackctx.sendPluginResult(result);
            return;
        }

        mHandle.setCallbackContext(callbackctx);

        List<Bitmap> bitmaps = new ArrayList<Bitmap>();
        bitmaps.add(bitmap);

        mBitmapPrint.setBitmaps(bitmaps);
        mBitmapPrint.print();
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

    private PluginResult pluginErrorResult(String namespace, int code, String message) {
        JSONObject result = new JSONObject();
        try {
            result.put("namespace", namespace);
            result.put("code", code);
            result.put("message", message);
        } catch (JSONException e) {
            Log.d(TAG, "unable to encode error as json object: ", e);
        }

        return new PluginResult(PluginResult.Status.ERROR, result);
    }

}
