package com.momzor.cordova.plugin.brotherPrinter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;

import com.brother.ptouch.sdk.NetPrinter;
import com.brother.ptouch.sdk.Printer;
import com.brother.ptouch.sdk.PrinterStatus;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Set;

import static com.momzor.cordova.plugin.brotherPrinter.PrinterUtil.LOG_TAG;

public class BrotherPrinter extends CordovaPlugin {

    @Override
    public void pluginInitialize() {
        Log.d(LOG_TAG, "Initializing " + getClass().getSimpleName());
        super.pluginInitialize();
        PrinterUtil.requestStoragePermission(this);
    }


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if ("printLabel".equals(action)) {
            printLabel(args, callbackContext);
            return true;
        }

        throw new UnsupportedOperationException("Unsupported action " + action);
    }

    private void printLabel(JSONArray args, CallbackContext callbackContext) throws JSONException {
        String base64DataToPrint = args.optString(0, null);
        PrintJobSetting printJobSetting = new PrintJobSetting();
        printJobSetting.init(args);

        doPrint(base64DataToPrint, printJobSetting, callbackContext);
    }


    private void findNetworkPrinters(PrintJobSetting printJobSetting) {

        NetPrinter[] netPrinters = PrinterUtil.enumerateNetPrinters(printJobSetting.modelName);

        int netPrinterCount = netPrinters.length;

        if (netPrinterCount > 0) {

            Log.d(LOG_TAG, "---- network printers found! ----");

            for (int i = 0; i < netPrinterCount; i++) {

                printJobSetting.ipAddress = netPrinters[i].ipAddress;
                printJobSetting.macAddress = netPrinters[i].macAddress;


                Log.d(LOG_TAG,
                        " idx:    " + Integer.toString(i)
                                + "\n model:  " + netPrinters[i].modelName
                                + "\n ip:     " + netPrinters[i].ipAddress
                                + "\n mac:    " + netPrinters[i].macAddress
                                + "\n serial: " + netPrinters[i].serNo
                                + "\n name:   " + netPrinters[i].nodeName
                );
            }

            printJobSetting.printerFound = true;
            Log.d(LOG_TAG, "---- /network printers found! ----");

        } else if (netPrinterCount == 0) {
            printJobSetting.printerFound = false;
            Log.d(LOG_TAG, "!!!! No network printers found !!!!");
        }
    }

    /**
     * get paired printers
     */
    private void findBluetoothPairedPrinters(PrintJobSetting printJobSetting) {

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            printJobSetting.printerFound = false;
            Log.e(LOG_TAG, "No Bluetooth Adapter was found");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {

            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            /** startActivity(enableBtIntent);**/
            Log.e(LOG_TAG, "Bluetooth Adapter not enabled. Please enable it and try again");
            printJobSetting.printerFound = false;
            return;
        }


        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices == null || pairedDevices.isEmpty()) {
            printJobSetting.printerFound = false;
            Log.d(LOG_TAG, "---- /NO bluetooth printers found! ----");
            return;
        }


        printJobSetting.printerFound = true;

        for (BluetoothDevice device : pairedDevices) {
            printJobSetting.macAddress = device.getAddress();
            Log.d(LOG_TAG, device.getAddress());

        }

        // Hem ... how do we know this is THE printer???
        Log.d(LOG_TAG, "---- /bluetooth printers found! ----");
    }


    private void doPrint(final String base64DataToPrint, final PrintJobSetting printJobSetting, final CallbackContext callbackctx) {

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {

                try {

                    final Bitmap bitmap = PrinterUtil.toBitmap(base64DataToPrint, callbackctx);
                    if (bitmap == null) {
                        return;
                    }

                    //TODO: integrate retry N times(configurable) before giving up
                    findPrinter(printJobSetting);

                    if (!printJobSetting.printerFound) {
                        callbackctx.error("No Network printer was found. Aborting.");
                        return;
                    }

                    Printer myPrinter = PrinterUtil.initPrinterProperties(printJobSetting);

                    PrinterStatus status = myPrinter.printImage(bitmap);
                    callbackctx.success(String.valueOf(status.errorCode));

                } catch (Exception e) {
                    Log.e(LOG_TAG, "!!!! Exception while printing !!!!=== ", e);
                    callbackctx.error("FAILED to print: " + e);

                }
            }
        });
    }

    private void findPrinter(final PrintJobSetting printJobSetting) {
        switch (printJobSetting.port) {
            case BLUETOOTH:
                findBluetoothPairedPrinters(printJobSetting);
                break;
            case NET:
                findNetworkPrinters(printJobSetting);
                break;
            default:
                // we should never ever get here
        }
    }

}
