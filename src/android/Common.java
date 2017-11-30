/**
 * Common define and functions
 *
 * @author Brother Industries, Ltd.
 * @version 2.2
 */

package com.brother.ptouch.sdk.printdemo.common;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;

public class Common {

    public static final String INTENT_TYPE_FLAG = "typeFlag";
    public static final String INTENT_FILE_NAME = "fileName";
    public static final String PREFES_SAVE_FOLDER = "saveFolderPath";

    public static final String PREFES_IMAGE_PRN_PATH = "prnAndImagePath";

    public static final String PREFES_PDZ_PATH = "pdzPath";
    public static final String PREFES_PDF_PATH = "pdfPath";

    // Activity_PrintTemplate
    public static final String TEMPLATE_REPLACE_TYPE = "Type";
    public static final String TEMPLATE_OBJECTNAME_INDEX = "Index";
    public static final String TEMPLATE_REPLACE_TEXT = "Text";
    public static final String TEMPLATE_KEY = "key";

    public static final String ENCODING_ENG = "ENG";
    public static final String ENCODING_JPN = "JPN";
    public static final String ENCODING_CHN = "CHN";

    public static final String SETTINGS_PORT = "port";
    public static final String SETTINGS_PAPERSIZE = "papersize";

    public static final String BLUETOOTH = "BLUETOOTH";
    public static final String NET = "NET";
    public static final String USB = "USB";

    public static final int TEMPLATE_REPLACE_TYPE_START = 10020;
    public static final int TEMPLATE_REPLACE_TYPE_END = 10021;
    public static final int TEMPLATE_REPLACE_TYPE_TEXT = 10022;
    public static final int TEMPLATE_REPLACE_TYPE_INDEX = 10023;
    public static final int TEMPLATE_REPLACE_TYPE_NAME = 10024;

    // Activity_NetPrinterList
    public static final String MODEL_NAME = "modelName";

    // MsgHandle
    public static final int MSG_SDK_EVENT = 10001;
    public static final int MSG_PRINT_START = 10002;
    public static final int MSG_PRINT_END = 10003;
    public static final int MSG_PRINT_CANCEL = 10004;
    public static final int MSG_TRANSFER_START = 10005;
    public static final int MSG_WRONG_OS = 10006;
    public static final int MSG_NO_USB = 10007;
    public static final int MSG_DATA_SEND_START = 10030;
    public static final int MSG_DATA_SEND_END = 10031;
    public static final int MSG_GET_FIRM = 10099;

    // Activity_NetPrinterList
    public static final int SEARCH_TIMES = 10;
    public static final int ACTION_WIFI_SETTINGS = 10007;
    public static final int ACTION_BLUETOOTH_SETTINGS = 10008;

    // Activity_PrintImage
    public static final int FILE_SELECT_PRN_IMAGE = 10010;

    // Activity_PrintPdf
    public static final int FILE_SELECT_PDF = 10011;

    // Activity_PrintPdz
    public static final int FILE_SELECT_PDZ = 10012;

    // Activity_Settings
    public static final int PRINTER_SEARCH = 10014;

    // Activity_Settings
    public static final int SAVE_PATH = 10015;

    // Activity_Settings
    public static final int FOLDER_SELECT = 10016;

    public static final String CUSTOM_PAPER_FOLDER = Environment
            .getExternalStorageDirectory().toString() + "/customPaperFileSet/";

    public static int mUsbRequest;

    public enum BatteryStatus {
        FULL("PRINTER_BATTERY_FULL"),
        MIDDLE("PRINTER_BATTERY_MIDDLE"),
        WEAK("PRINTER_BATTERY_WEAK"),
        CHARGE("PRINTER_BATTERY_CHARGE"),
        ACADAPTER("PRINTER_BATTERY_AC_ADAPTER");

        private final String message;
        BatteryStatus(String message) {
            this.message = message;
        }

        public String toString() {
            return this.message;
        }

    }


    /**
     * judge whether is a image file.
     *
     * @param path the file path
     * @return true if it is a image file, otherwise false
     */
    public static boolean isImageFile(final String path) {

        if (path == null || "".equals(path)) {
            return false;
        }

        final String extention = path.substring(
                path.lastIndexOf(".", path.length()) + 1, path.length());
        return (extention.equalsIgnoreCase("jpg"))
                || (extention.equalsIgnoreCase("jpeg"))
                || (extention.equalsIgnoreCase("bmp"))
                || (extention.equalsIgnoreCase("png"))
                || (extention.equalsIgnoreCase("gif"));

    }
    /**
     * judge whether is a prn file.
     *
     * @param path the file path
     * @return true if it is a image file, otherwise false
     */
    public static boolean isPrnFile(String path) {

        String extention = path.substring(
                path.lastIndexOf(".", path.length()) + 1, path.length());
        return extention.equalsIgnoreCase("prn");
    }

    /**
     * judge whether is a prn file.
     *
     * @param path the file path
     * @return true if it is a image file, otherwise false
     */
    public static boolean isBinFile(String path) {

        String extention = path.substring(
                path.lastIndexOf(".", path.length()) + 1, path.length());
        return extention.equalsIgnoreCase("bin");
    }

    /**
     * judge whether is a pdz file.
     *
     * @param path the file path
     * @return true if it is a image file, otherwise false
     */
    @SuppressWarnings("UnusedAssignment")
    public static boolean isTemplateFile(String path) {

        String extention = path.substring(
                path.lastIndexOf(".", path.length()) + 1, path.length());
        if (extention.equalsIgnoreCase("pdz")
                || extention.equalsIgnoreCase("blf")
                || extention.equalsIgnoreCase("pd3")) {
        return true;
    }
        return false;
    }

    /**
     * judge whether is a pdf file.
     *
     * @param path the file path
     * @return true if it is a image file, otherwise false
     */
    public static boolean isPdfFile(String path) {

        String extention = path.substring(
                path.lastIndexOf(".", path.length()) + 1, path.length());
        return extention.equalsIgnoreCase("pdf");
    }


    /**
     * fileToBitymap takes a file at a given path and creates a Bitmap for it.
     * It will create the resulting bitmap attempting to target the dimensions
     * provided.
     *
     * @param  filePath The path to the file on the filesystem.
     * @param  width    The target width.
     * @param  length   The target height.
     * @return          The resulting bitmap after being resampled and loaded,
     *                      provided no issues occurred.
     */
    public static Bitmap fileToBitmap(String filePath, int width, int length) {

        final long imageView01Resolution = (long) (width * length);

        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(filePath, options);

        int imgSize = options.outWidth * options.outHeight;

        if (imgSize < imageView01Resolution) {
            options.inSampleSize = 1;
        } else if (imgSize < imageView01Resolution * 2 * 2) {
            options.inSampleSize = 2;
        } else {
            options.inSampleSize = 4;
        }

        float resizeScaleWidth;
        float resizeScaleHeight;
        Matrix matrix = new Matrix();
        resizeScaleWidth = (float) width / options.outWidth;
        resizeScaleHeight = (float) length / options.outHeight;
        float scale = Math.min(resizeScaleWidth, resizeScaleHeight);

        options.inJustDecodeBounds = false;

        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);

        if (bitmap != null && scale < 1.0) {
            matrix.postScale(scale * options.inSampleSize, scale
                    * options.inSampleSize);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), matrix, true);
        }
        return bitmap;

    }

}
