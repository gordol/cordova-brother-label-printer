/**
 * ImagePrint for printing
 *
 * @author Brother Industries, Ltd.
 * @version 2.2
 */
package com.brother.ptouch.sdk.printdemo.printprocess;

import android.content.Context;
import android.graphics.Bitmap;

import com.brother.ptouch.sdk.PrinterInfo.ErrorCode;
import com.brother.ptouch.sdk.printdemo.common.MsgHandle;

import java.util.ArrayList;

public class ImageBitmapPrint extends BasePrint {

    private ArrayList<Bitmap> mBitmaps;

    public ImageBitmapPrint(Context context, MsgHandle mHandle) {
        super(context, mHandle);
    }

    /**
     * set print data
     */
    public ArrayList<Bitmap> getBitmaps() {
        return mBitmaps;
    }

    /**
     * set print data
     */
    public void setBitmaps(ArrayList<Bitmap> bitmaps) {
        mBitmaps = bitmaps;
    }

    /**
     * do the particular print
     */
    @Override
    protected void doPrint() {

        int count = mBitmaps.size();

        for (int i = 0; i < count; i++) {

            Bitmap bitmap = mBitmaps.get(i);

            mPrintResult = mPrinter.printImage(bitmap);

            // if error, stop print next files
            if (mPrintResult.errorCode != ErrorCode.ERROR_NONE) {
                break;
            }
        }
    }



}