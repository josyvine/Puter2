package com.puter.unofficial;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Utility class to handle general file operations.
 * Converts documents, text files, and other non-image attachments 
 * into Base64 strings for direct injection into the Puter.js chat logic.
 */
public class FileUtils {

    private static final String TAG = "PuterFileUtils";

    /**
     * Reads a file from a given Uri and encodes its entire content to a Base64 string.
     * 
     * @param context App context to access ContentResolver.
     * @param fileUri The Uri of the file selected via the native file picker.
     * @return Base64 encoded string of the file content, or null if an error occurs.
     */
    public static String fileToBase64(Context context, Uri fileUri) {
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(fileUri);
            byte[] buffer = new byte[8192];
            int bytesRead;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }

            byte[] fileBytes = output.toByteArray();
            
            // Encode bytes to Base64
            return Base64.encodeToString(fileBytes, Base64.NO_WRAP);

        } catch (Exception e) {
            Log.e(TAG, "Failed to convert file to Base64: " + e.getMessage());
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing stream: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Extracts the file name from a Uri (if needed for logging or UI).
     */
    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}