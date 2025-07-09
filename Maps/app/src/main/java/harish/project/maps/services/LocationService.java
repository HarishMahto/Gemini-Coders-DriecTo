package harish.project.maps.services;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class LocationService {
  private static final String TAG = "LocationService";
  private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
  private static final long MIN_TIME_BETWEEN_UPDATES = 30000; // 30 seconds
  private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

  private final Context context;
  private final LocationManager locationManager;
  private Location currentLocation;
  private LocationListener currentLocationListener;

  public interface LocationCallback {
    void onLocationReceived(Location location);

    void onLocationError(String error);

    void onPermissionDenied();
  }

  public LocationService(Context context) {
    this.context = context;
    this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
  }

  public boolean hasLocationPermission() {
    return ContextCompat.checkSelfPermission(context,
        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context,
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
  }

  public void requestLocationPermission(Activity activity) {
    ActivityCompat.requestPermissions(activity,
        new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION },
        LOCATION_PERMISSION_REQUEST_CODE);
  }

  public void getCurrentLocation(LocationCallback callback) {
    if (!hasLocationPermission()) {
      callback.onPermissionDenied();
      return;
    }

    if (ActivityCompat.checkSelfPermission(context,
        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(context,
            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      callback.onLocationError("Location permission not granted");
      return;
    }

    // Try to get last known location first
    Location lastKnownLocation = getLastKnownLocation();
    if (lastKnownLocation != null) {
      currentLocation = lastKnownLocation;
      callback.onLocationReceived(currentLocation);
    }

    // Create location listener
    currentLocationListener = new LocationListener() {
      @Override
      public void onLocationChanged(Location location) {
        currentLocation = location;
        callback.onLocationReceived(location);
        // Stop listening after getting the first location
        stopLocationUpdates();
      }

      @Override
      public void onStatusChanged(String provider, int status, Bundle extras) {
      }

      @Override
      public void onProviderEnabled(String provider) {
      }

      @Override
      public void onProviderDisabled(String provider) {
        callback.onLocationError("Location provider disabled");
      }
    };

    try {
      // Try GPS first
      if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            MIN_TIME_BETWEEN_UPDATES,
            MIN_DISTANCE_CHANGE_FOR_UPDATES,
            currentLocationListener);
      }
      // Try network provider as backup
      else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            MIN_TIME_BETWEEN_UPDATES,
            MIN_DISTANCE_CHANGE_FOR_UPDATES,
            currentLocationListener);
      } else {
        callback.onLocationError("No location providers available");
      }
    } catch (SecurityException e) {
      Log.e(TAG, "Security exception while requesting location updates", e);
      callback.onLocationError("Location permission denied");
    }
  }

  private Location getLastKnownLocation() {
    Location bestLocation = null;
    long bestTime = 0;

    try {
      if (ActivityCompat.checkSelfPermission(context,
          Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
          ActivityCompat.checkSelfPermission(context,
              Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

        // Try GPS provider
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
          Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
          if (gpsLocation != null && gpsLocation.getTime() > bestTime) {
            bestLocation = gpsLocation;
            bestTime = gpsLocation.getTime();
          }
        }

        // Try network provider
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
          Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
          if (networkLocation != null && networkLocation.getTime() > bestTime) {
            bestLocation = networkLocation;
            bestTime = networkLocation.getTime();
          }
        }
      }
    } catch (SecurityException e) {
      Log.e(TAG, "Security exception while getting last known location", e);
    }

    return bestLocation;
  }

  public void stopLocationUpdates() {
    try {
      if (currentLocationListener != null) {
        if (ActivityCompat.checkSelfPermission(context,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
          locationManager.removeUpdates(currentLocationListener);
          currentLocationListener = null;
        }
      }
    } catch (SecurityException e) {
      Log.e(TAG, "Security exception while stopping location updates", e);
    }
  }

  public Location getCurrentLocation() {
    return currentLocation;
  }

  public boolean isLocationEnabled() {
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
  }
}