package harish.project.maps.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtils {

  public static final int REQUEST_FILE_PERMISSIONS = 1001;
  public static final int REQUEST_MANAGE_STORAGE = 1002;

  /**
   * Check if file access permissions are granted
   */
  public static boolean hasFilePermissions(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      // Android 13+ uses READ_MEDIA_IMAGES
      return ContextCompat.checkSelfPermission(context,
          Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      // Android 11+ uses scoped storage, check if we can access files
      return Environment.isExternalStorageManager();
    } else {
      // Android 10 and below use READ_EXTERNAL_STORAGE
      return ContextCompat.checkSelfPermission(context,
          Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
  }

  /**
   * Request file access permissions
   */
  public static void requestFilePermissions(Activity activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      // Android 13+ - request READ_MEDIA_IMAGES
      if (ContextCompat.checkSelfPermission(activity,
          Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(activity,
            new String[] { Manifest.permission.READ_MEDIA_IMAGES },
            REQUEST_FILE_PERMISSIONS);
      }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      // Android 11+ - request MANAGE_EXTERNAL_STORAGE
      if (!Environment.isExternalStorageManager()) {
        try {
          Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
          intent.addCategory("android.intent.category.DEFAULT");
          intent.setData(Uri.parse(String.format("package:%s", activity.getPackageName())));
          activity.startActivityForResult(intent, REQUEST_MANAGE_STORAGE);
        } catch (Exception e) {
          Intent intent = new Intent();
          intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
          activity.startActivityForResult(intent, REQUEST_MANAGE_STORAGE);
        }
      }
    } else {
      // Android 10 and below - request READ_EXTERNAL_STORAGE
      if (ContextCompat.checkSelfPermission(activity,
          Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(activity,
            new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
            REQUEST_FILE_PERMISSIONS);
      }
    }
  }

  /**
   * Handle permission request results
   */
  public static boolean handlePermissionResult(Activity activity, int requestCode,
      @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    if (requestCode == REQUEST_FILE_PERMISSIONS) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(activity, "File access permission granted", Toast.LENGTH_SHORT).show();
        return true;
      } else {
        Toast.makeText(activity, "File access permission denied", Toast.LENGTH_SHORT).show();
        return false;
      }
    } else if (requestCode == REQUEST_MANAGE_STORAGE) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
        Toast.makeText(activity, "File access permission granted", Toast.LENGTH_SHORT).show();
        return true;
      } else {
        Toast.makeText(activity, "File access permission denied", Toast.LENGTH_SHORT).show();
        return false;
      }
    }
    return false;
  }

  /**
   * Get list of required permissions for current Android version
   */
  public static List<String> getRequiredFilePermissions() {
    List<String> permissions = new ArrayList<>();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
    } else {
      permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    return permissions;
  }

  /**
   * Show permission explanation dialog
   */
  public static void showPermissionExplanation(Activity activity, String message) {
    Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
  }
}