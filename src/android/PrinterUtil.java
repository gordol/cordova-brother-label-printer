package com.momzor.cordova.plugin.brotherPrinter;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import com.brother.ptouch.sdk.NetPrinter;
import com.brother.ptouch.sdk.Printer;
import com.brother.ptouch.sdk.PrinterInfo;
import com.brother.ptouch.sdk.connection.BluetoothConnectionSetting;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import java.io.IOException;


public class PrinterUtil {

    protected static final String LOG_TAG = "BrotherPrinter";
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 1;

    /**
     * @param base64EncodedImageOrPdf could be an encoded image (PNG, JPG, BMP) or PDF content
     * @param callbackctx
     * @return an instance of {@link Bitmap}
     * @throws IOException
     */
    protected static final Bitmap toBitmap(String base64EncodedImageOrPdf, final CallbackContext callbackctx) {
        try {

            int separatorIdx = base64EncodedImageOrPdf.indexOf(',');

            if (separatorIdx != -1) {
                base64EncodedImageOrPdf = base64EncodedImageOrPdf.substring(separatorIdx + 1);
            }

            byte[] binData = Base64.decode(base64EncodedImageOrPdf, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(binData, 0, binData.length);
            if (bitmap == null) {
                throw new RuntimeException("Unexpected bitmap generated: null");
            }

            return bitmap;

        } catch (Exception e) {
            Log.e(LOG_TAG, "!!!! Exception generating bitmap !!!!=== ", e);
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, " Bitmap creation failed: " + e);
            callbackctx.sendPluginResult(result);
            return null;
        }
    }


    protected static final NetPrinter[] enumerateNetPrinters(String modelName) {
        Printer myPrinter = new Printer();
        NetPrinter[] netPrinters = myPrinter.getNetPrinters(modelName);
        return netPrinters;
    }


    protected static final void requestStoragePermission(CordovaPlugin cordovaPlugin) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (cordovaPlugin.cordova.getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                cordovaPlugin.cordova.requestPermission(cordovaPlugin, PERMISSION_WRITE_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            }
        }

    }

    @NonNull
    protected static final Printer initPrinterProperties(com.momzor.cordova.plugin.brotherPrinter.PrintJobSetting printJobSetting) {
        Printer myPrinter = new Printer();
        PrinterInfo myPrinterInfo = myPrinter.getPrinterInfo();

        myPrinterInfo.printerModel = printJobSetting.model;
        myPrinterInfo.port = printJobSetting.port;
        myPrinterInfo.printMode = PrinterInfo.PrintMode.FIT_TO_PAPER;

        myPrinterInfo.orientation = printJobSetting.orientation;
        myPrinterInfo.paperSize = PrinterInfo.PaperSize.CUSTOM;

        myPrinterInfo.labelNameIndex = printJobSetting.labelNameIndex;

        myPrinterInfo.isAutoCut = true;
        myPrinterInfo.isCutAtEnd = true;
        myPrinterInfo.isHalfCut = true;
        myPrinterInfo.isSpecialTape = false;
        myPrinterInfo.numberOfCopies = printJobSetting.numberOfCopies;

        myPrinterInfo.macAddress = printJobSetting.macAddress;

        if (PrinterInfo.Port.NET.equals(printJobSetting.port)) {
            myPrinterInfo.ipAddress = printJobSetting.ipAddress;

        } else if (PrinterInfo.Port.BLUETOOTH.equals(printJobSetting.port)) {
            //TODO: Investigate: do we really need this?
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothConnectionSetting.setBluetoothAdapter(bluetoothAdapter);
        }

        myPrinter.setPrinterInfo(myPrinterInfo);
        return myPrinter;
    }

}
