/**
 * Message Handle
 *
 * @author Brother Industries, Ltd.
 * @version 2.2
 */

package com.brother.ptouch.sdk.printdemo.common;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.brother.ptouch.sdk.PrinterInfo;
//import com.brother.ptouch.sdk.printdemo.R;
//
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

public class MsgHandle extends Handler {
    private CallbackContext mCallback;

    public final static int FUNC_OTHER = 1;
    public final static int FUNC_SETTING = 2;
    public final static int FUNC_TRANSFER = 3;

    private String mResult;
    private String mBattery;
    private boolean isCancelled = false;
    private int funcID = FUNC_OTHER;

    private static final String TAG = "Brother/SDKEvent";

    public MsgHandle(final CallbackContext callbackctx) {
        funcID = FUNC_OTHER;
        mCallback = callbackctx;
    }

    /**
     * set the function id
     */
    public void setFunction(int funcID) {

        this.funcID = funcID;
    }

    /**
     * set the printing result
     */
    public void setResult(String results) {

        mResult = results;
    }

    /**
     * set the Battery info.
     */
    public void setBattery(String battery) {
        mBattery = battery;
    }

    /**
     * setCallbackContext sets the callback context to use.
     *
     * @param callbackctx [description]
     */
    public void setCallbackContext(CallbackContext callbackctx) {
        mCallback = callbackctx;
    }

    /**
     * Message Handler which deal with the messages from UI thread or print
     * thread START: start message SDK_EVENT: message from SDK UPDATE: end
     * message
     */
    @Override
    public void handleMessage(Message msg) {

        switch (msg.what) {
            case Common.MSG_PRINT_START:
            case Common.MSG_DATA_SEND_START:
                Log.d(TAG, "Start Communication...");
                break;
            case Common.MSG_TRANSFER_START:
                Log.d(TAG, "Transfer Start...");
                break;
            case Common.MSG_GET_FIRM:
                Log.d(TAG, "Version = " + mResult);

                break;
            case Common.MSG_SDK_EVENT:
                Log.d(TAG, msg.obj.toString());
                String strMsg = msg.obj.toString();
                if (strMsg.equals(PrinterInfo.Msg.MESSAGE_START_COMMUNICATION
                        .toString())) {
                    Log.d(TAG, "Preparing the connection...");
                    break;
                } else if (strMsg.equals(PrinterInfo.Msg.MESSAGE_START_CREATE_DATA
                        .toString())) {
                    Log.d(TAG, "Creating the data");
                    break;
                } else if (strMsg.equals(PrinterInfo.Msg.MESSAGE_START_SEND_DATA
                        .toString())) {
                    if (funcID != FUNC_OTHER) {
                        Log.d(TAG, "Sending the data...");
                    } else {
                        Log.d(TAG, "Sending the data...");
                    }
                    break;

                } else if (strMsg.equals(PrinterInfo.Msg.MESSAGE_END_SEND_DATA
                        .toString())) {
                    if (funcID == FUNC_OTHER) {
                        Log.d(TAG, "Printing the data");
                    } else if (funcID == FUNC_TRANSFER) {
                        Log.d(TAG, "The data was sent");
                    }
                    break;
                } else if (strMsg.equals(PrinterInfo.Msg.MESSAGE_END_SEND_TEMPLATE
                        .toString())) {
                    Log.d(TAG, "The data was sent");
                    break;
                } else if (strMsg.equals(PrinterInfo.Msg.MESSAGE_PRINT_COMPLETE
                        .toString())) {
                    Log.d(TAG, "The printing process was completed");
                    break;
                } else if (strMsg.equals(PrinterInfo.Msg.MESSAGE_PRINT_ERROR
                        .toString())) {
                    Log.d(TAG, "Runtime Error Occurred");
                    break;
                } else if (strMsg.equals(PrinterInfo.Msg.MESSAGE_END_COMMUNICATION
                        .toString())) {
                    Log.d(TAG, "Closing the Connection");
                    break;
                } else if (strMsg.equals(PrinterInfo.Msg.MESSAGE_PAPER_EMPTY
                        .toString())) {
                    if (!isCancelled)
                        Log.d(TAG, "Please Set Paper");
                    break;
                } else if (strMsg.equals(PrinterInfo.Msg.MESSAGE_START_COOLING
                        .toString())) {
                    Log.d(TAG, "Printer cooling start");
                    break;
                } else if (strMsg.equals(PrinterInfo.Msg.MESSAGE_END_COOLING
                        .toString())) {
                    Log.d(TAG, "Printer cooling end");
                    break;
                } else if (strMsg.equals(PrinterInfo.Msg.MESSAGE_WAIT_PEEL
                        .toString())) {
                    Log.d(TAG, "Waiting for peeling label");
                    break;
                } else if (strMsg
                        .equals(PrinterInfo.Msg.MESSAGE_START_SEND_TEMPLATE
                                .toString())) {
                    Log.d(TAG, "File transferring");
                    break;
                } else if (strMsg
                        .equals(PrinterInfo.Msg.MESSAGE_START_UPDATE_BLUETOOTH_SETTING
                                .toString())) {
                    Log.d(TAG, "Transferring Bluetooth settings");
                    break;
                } else if (strMsg
                        .equals(PrinterInfo.Msg.MESSAGE_START_GET_BLUETOOTH_SETTING
                                .toString())) {
                    Log.d(TAG, "Getting Bluetooth settings");
                    break;
                } else if (strMsg
                        .equals(PrinterInfo.Msg.MESSAGE_START_GET_TEMPLATE_LIST
                                .toString())) {
                    Log.d(TAG, "Retrieving template list");
                    break;
                } else if (strMsg
                        .equals(PrinterInfo.Msg.MESSAGE_START_REMOVE_TEMPLATE_LIST
                                .toString())) {
                    Log.d(TAG, "Removing templates");
                    break;
                } else {
                    break;
                }
            case Common.MSG_PRINT_END:
            case Common.MSG_DATA_SEND_END:
                isCancelled = false;

                if (mCallback == null) {
                    break;
                }

                JSONObject resultObject = new JSONObject();
                try {
                    resultObject.put("result", mResult);

                    if (!mBattery.equals("")) {
                        resultObject.put("battery", mBattery);
                    }

                    final PluginResult sendEndResult = new PluginResult(PluginResult.Status.OK, resultObject);
                    mCallback.sendPluginResult(sendEndResult);
                    mCallback = null;
                } catch (JSONException e) {

                }
                break;
            case Common.MSG_PRINT_CANCEL:
                isCancelled = true;

                if (mCallback == null) {
                    break;
                }

                final PluginResult cancelResult = pluginErrorResult("printCancelled", 1, "cancelled.");
                mCallback.sendPluginResult(cancelResult);
                mCallback = null;
                break;
            case Common.MSG_WRONG_OS:
                if (mCallback == null) {
                    break;
                }

                final PluginResult wrongOSResult = pluginErrorResult("wrongOS", 1, "Android OS is not supported");
                mCallback.sendPluginResult(wrongOSResult);
                mCallback = null;
                break;
            case Common.MSG_NO_USB:
                if (mCallback == null) {
                    break;
                }

                final PluginResult noUSBResult = pluginErrorResult("noUSB", 1, "USB device is not found.");
                mCallback.sendPluginResult(noUSBResult);
                mCallback = null;
                break;
            default:
                break;
        }
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
