package com.alsa.picturecut.utils;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

/**
 * AlbumUtil [ 系统相册相关的方法 ]
 * created by alsa on 2019/12/11
 */
public class AlbumUtil {
    /**
     * [ 打开系统相册 ]
     *
     * @param activity activity
     */
    public static void openPhotoAlbum(Activity activity, int requestCode) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("image/*");
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * [ 根据URI获取图片绝对路径 ]
     *
     * @param context context
     * @param uri     图片的URI
     * @return 图片的绝对路径|null
     */
    public static String getRealPathFromUri(Context context, Uri uri) {
        // 4.4以下版本和4.4及以上版本获取路径的方式不同
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return getRealPathFromUriAboveApi19(context, uri);
        } else {
            return getRealPathFromUriBelowApi19(context, uri);
        }
    }

    /**
     * [ 根据URI获取图片绝对路径|4.4及以上系统 ]
     *
     * @param context context
     * @param uri     图片的URI
     * @return 图片的绝对路径|null
     */
    private static String getRealPathFromUriAboveApi19(Context context, Uri uri) {
        String filePath = null;
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // 如果是document类型的uri，则通过documentID来处理
            String documentId = DocumentsContract.getDocumentId(uri);
            if (isMediaDocument(uri)) {
                // 使用':'分割
                String id = documentId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=?";
                String[] selectionArgs = {id};
                filePath = getDataColumn(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection, selectionArgs);
            } else if (isDownloadDocument(uri)) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(documentId));
                filePath = getDataColumn(context, contentUri, null, null);
            }
        } else if (uri.getScheme().equalsIgnoreCase("content")) {
            // 如果是content类型的Uri
            filePath = getDataColumn(context, uri, null, null);
        } else if (uri.getScheme().equalsIgnoreCase("file")) {
            // 如果是file类型的Uri，直接获取图片对应的路径
            filePath = uri.getPath();
        }
        return filePath;
    }

    /**
     * [ 根据URI获取图片绝对路径|4.4以下系统 ]
     *
     * @param context context
     * @param uri     图片的URI
     * @return 图片的绝对路径|null
     */
    private static String getRealPathFromUriBelowApi19(Context context, Uri uri) {
        return getDataColumn(context, uri, null, null);
    }

    /**
     * 判断是否为媒体文件
     *
     * @param uri uri
     * @return true|false
     */
    private static boolean isMediaDocument(Uri uri) {
        return uri.getAuthority().equals("com.android.providers.media.documents");
    }

    /**
     * 判断是否为下载文件
     *
     * @param uri uri
     * @return true|false
     */
    private static boolean isDownloadDocument(Uri uri) {
        return uri.getAuthority().equals("com.android.providers.downloads.documents");
    }

    /**
     * [ 获取数据库表中的_data列，返回Uri对应的文件路径 ]
     *
     * @param context       context
     * @param uri           uri
     * @param selection     筛选列名称
     * @param selectionArgs 筛选列参数值
     * @return uri对应的文件路径|null
     */
    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        String path = null;
        String[] projection = new String[]{MediaStore.Images.Media.DATA};
        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(projection[0]);
                path = cursor.getString(columnIndex);
            }
        }
        return path;
    }
}
