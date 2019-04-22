package com.momzor.cordova.plugin.brotherPrinter;


import android.support.annotation.NonNull;

import com.brother.ptouch.sdk.LabelInfo;
import com.brother.ptouch.sdk.PrinterInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PrintJobSetting {
    String modelName;
    PrinterInfo.Model model;
    int numberOfCopies;

    PrinterInfo.Port port;

    String ipAddress = null;
    String macAddress = null;

    int labelNameIndex;
    PrinterInfo.Orientation orientation;

    boolean printerFound;

    protected void init(final JSONArray args) throws JSONException {
        JSONObject options = args.getJSONObject(1);

        String tmpModelName = options.getString("modelName").toUpperCase();
        String tmpModelEnumName = tmpModelName.replace("-", "_");
        String tmpLabelName = options.getString("labelName").toUpperCase();
        String tmpOrientation = options.getString("orientation").toUpperCase();
        String tmpPort = options.getString("port").toUpperCase();

        this.numberOfCopies = toNumberOfCopies(options);
        this.orientation = PrinterInfo.Orientation.valueOf(tmpOrientation);
        this.modelName = tmpModelName;
        this.model = PrinterInfo.Model.valueOf(tmpModelEnumName);
        this.labelNameIndex = toLabelIndexName(tmpLabelName);
        this.port = toPort(tmpPort);

        this.printerFound = false;
    }

    private int toNumberOfCopies(JSONObject options) throws JSONException {
        int n = options.getInt("numberOfCopies");
        if (n <= 0) {
            throw new IllegalArgumentException("Invalid numberOfCopies: " + n);
        }

        return n;
    }

    @NonNull
    private PrinterInfo.Port toPort(String port) {
        PrinterInfo.Port p = PrinterInfo.Port.valueOf(port);
        if (PrinterInfo.Port.NET.equals(p) || PrinterInfo.Port.BLUETOOTH.equals(port)) {
            return p;
        }

        throw new IllegalArgumentException("Unsupported port " + port);
    }

    private int toLabelIndexName(String labelName) {
        if (this.modelName.startsWith("QL")) {
            LabelInfo.QL700 label = LabelInfo.QL700.valueOf(labelName);
            return label.ordinal();
        }

        if (this.modelName.startsWith("PT")) {
            LabelInfo.PT label = LabelInfo.PT.valueOf(labelName);
            return label.ordinal();
        }

        throw new IllegalArgumentException("Unsupported printer model " + labelName);
    }
}
