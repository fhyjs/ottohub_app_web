package org.eu.hanana.reimu.ottohub_app_web;

import android.webkit.MimeTypeMap;

public class Utils {
    public static String getMimeTypeFromExtension(String path) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        if (extension == null) return "text/plain";
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        return mime != null ? mime : "application/octet-stream";
    }
}
