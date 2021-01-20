
package com.capriza.reactlibrary;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import androidx.core.content.FileProvider;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.List;

public class RNCOpenDocModule extends ReactContextBaseJavaModule implements ActivityEventListener {
  private static final String LOG_TAG = "RNCOpenDoc";
  private static final String CONTENT_URI_TYPE_PREFIX = "content://";
  private static final String FILE_PATH_TYPE_PREFIX = "file://";
  private static final String NO_MIME_TYPE = "";
  private static final int PICK_REQUEST_CODE = 1978;

  private static class Fields {
    private static final String FILE_NAME = "fileName";
    private static final String TYPE = "type";
  }

  private Callback callback;

  private final ReactApplicationContext reactContext;

  public RNCOpenDocModule(ReactApplicationContext reactContext) {
    super(reactContext);
    reactContext.addActivityEventListener(this);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNCOpenDoc";
  }

  private String getMimeTypeByFileExtensionFallbackToSuggestion(String filePath, String suggestedMimeType) {
    String ext = "";
    int nameEndIndex = filePath.lastIndexOf('.');
    if (nameEndIndex > 0) {
      ext = filePath.substring(nameEndIndex + 1);
    }
    Log.d(LOG_TAG, ext);
    MimeTypeMap mime = MimeTypeMap.getSingleton();
    String type = mime.getMimeTypeFromExtension(ext.toLowerCase());

    if (type == null) {
      if (suggestedMimeType != null && !suggestedMimeType.isEmpty()) {
        return suggestedMimeType;
      }
    }

    if (type == null) {
      type = HttpURLConnection.guessContentTypeFromName(filePath);
    }

    if (type == null) {
      type = "application/" + ext;
    }
    return type;
  }

  private boolean isContentUri(String path) {
    return path != null && path.startsWith(CONTENT_URI_TYPE_PREFIX);
  }

  private boolean isFilePath(String path) {
    return path != null && path.startsWith(FILE_PATH_TYPE_PREFIX);
  }

  private String getMimeType(String path, String suggestedMimeType) {
    if (isContentUri(path) && suggestedMimeType != null && !suggestedMimeType.isEmpty()) {
      return suggestedMimeType;
    }
    return getMimeTypeByFileExtensionFallbackToSuggestion(path, suggestedMimeType);
  }

  private Uri GetUriFromPath(String path) {
    Uri uri = null;

    if (isContentUri(path)) {
      uri = Uri.parse(path);
    } else {
      if (isFilePath(path)) {
        path = path.replace(FILE_PATH_TYPE_PREFIX, "");
      }
      File file = new File(path);
      if (!file.exists()) {
        Log.e(LOG_TAG, "File does not exist");
        return null;
      }
      uri = FileProvider.getUriForFile(reactContext.getApplicationContext(),reactContext.getApplicationContext().getPackageName() + ".provider", file);
    }

    return uri;
  }

  @ReactMethod
  public void open(String path, String suggestedMimeType, final Promise promise) {
    internalOpen(path, NO_MIME_TYPE, promise);
  }

  @ReactMethod
  public void openWithSuggestedMime(String path, String suggestedMimeType, final Promise promise) {
    internalOpen(path, suggestedMimeType, promise);
  }

  public void internalOpen(String path, String suggestedMimeType, final Promise promise) {
    try {
      Uri uri = GetUriFromPath(path);
      if (uri == null) {
        return;
      }

      String type = this.getMimeType(uri.toString(), suggestedMimeType);

      Intent intent = new Intent(Intent.ACTION_VIEW, uri);

      if (type != null && uri != null) {
        intent.setDataAndType(uri, type);
      } else if (type != null) {
        intent.setType(type);
      }

      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

      //Verify There is an App to Receive the Intent
      PackageManager packageManager = getReactApplicationContext().getPackageManager();
      List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
      boolean isIntentSafe = activities.size() > 0;
      if (isIntentSafe) {
        getReactApplicationContext().startActivity(intent);
        promise.resolve(true);
      } else {
        promise.reject(new Error("There is no App installed that can open this document"));
      }
    } catch(ActivityNotFoundException ex) {
      Log.e(LOG_TAG, "can't open document", ex);
      promise.reject(new Error("can't open document"));
    }
  }

  @ReactMethod
  public void share(String path, String suggestedMimeType) {
    internalShare(path, NO_MIME_TYPE);
  }

  @ReactMethod
  public void shareWithSuggestedMime(String path, String suggestedMimeType) {
    internalShare(path, suggestedMimeType);
  }

  public void internalShare(String path, String suggestedMimeType) {
    try {
      Uri uri = GetUriFromPath(path);
      if (uri == null) {
        return;
      }

      String type = this.getMimeType(uri.toString(), suggestedMimeType);

      Intent shareIntent = new Intent();
      shareIntent.setAction(Intent.ACTION_SEND);
      shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
      shareIntent.setType(type);
      shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

      Intent i = Intent.createChooser(shareIntent, "Share");
      i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

      getReactApplicationContext().startActivity(i);
    } catch(ActivityNotFoundException ex) {
      Log.e(LOG_TAG, "can't share document", ex);
    }
  }

  @ReactMethod
  public void pick(ReadableMap args, Callback callback) {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("*/*");

    if (args != null && !args.isNull("fileTypes")) {
      intent.putExtra(Intent.EXTRA_MIME_TYPES, args.getArray("fileTypes").toArrayList().toArray(new String[0]));
    }

    this.callback = callback;

    getReactApplicationContext().startActivityForResult(intent, PICK_REQUEST_CODE, Bundle.EMPTY);
  }

  @Override
  public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
    onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public void onNewIntent(Intent intent) {

  }

  private void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode != PICK_REQUEST_CODE || callback == null) {
      return;
    }
    if (resultCode == Activity.RESULT_CANCELED) {
      callback.invoke(null, null);
      return;
    }

    if (resultCode != Activity.RESULT_OK) {
      callback.invoke("Bad result code: " + resultCode, null);
      return;
    }

    if (data == null) {
      callback.invoke("No data", null);
      return;
    }

    try {
      Uri uri = data.getData();
      final WritableNativeArray res = new WritableNativeArray();
      res.pushMap(toMapWithMetadata(uri));
      callback.invoke(null, res);
    } catch (Exception e) {
      Log.e(LOG_TAG, "Failed to read", e);
      callback.invoke(e.getMessage(), null);
    }
  }

  private WritableMap toMapWithMetadata(Uri uri) {
    WritableMap map;
    if(uri.toString().startsWith("/")) {
      map = metaDataFromFile(new File(uri.toString()));
    } else {
      map = metaDataFromContentResolver(uri);
    }

    map.putString("uri", uri.toString());

    return map;
  }

  private WritableMap metaDataFromFile(File file) {
    WritableMap map = Arguments.createMap();

    if(!file.exists())
      return map;

    map.putString(Fields.FILE_NAME, file.getName());
    map.putString(Fields.TYPE, mimeTypeFromName(file.getAbsolutePath()));

    return map;
  }

  private WritableMap metaDataFromContentResolver(Uri uri) {
    WritableMap map = Arguments.createMap();

    ContentResolver contentResolver = getReactApplicationContext().getContentResolver();

    map.putString(Fields.TYPE, contentResolver.getType(uri));

    Cursor cursor = contentResolver.query(uri, null, null, null, null, null);

    try {
      if (cursor != null && cursor.moveToFirst()) {
        map.putString(Fields.FILE_NAME, cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)));
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }

    return map;
  }

  private static String mimeTypeFromName(String absolutePath) {
    String extension = MimeTypeMap.getFileExtensionFromUrl(absolutePath);
    if (extension != null) {
      return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    } else {
      return null;
    }
  }
}
