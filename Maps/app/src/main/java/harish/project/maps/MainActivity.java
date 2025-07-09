package harish.project.maps;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;
import harish.project.maps.models.TrafficJunction;
import harish.project.maps.services.FirebaseService;
import harish.project.maps.services.GeminiService;
import harish.project.maps.services.TrafficService;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.model.*;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
//import com.google.android.libraries.places.api.model.PlaceLikelihoodBufferResponse;
//import com.google.android.libraries.places.api.net.NearbySearchRequest;
//import com.google.android.libraries.places.api.net.NearbySearchResponse;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import android.location.Location;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import android.os.Handler;
import android.os.Looper;
import android.animation.ValueAnimator;
import android.animation.ArgbEvaluator;

//import com.google.android.libraries.places.api.model.PlaceField;

//import com.google.android.libraries.places.api.places.PlacesClient;

public class MainActivity extends BaseActivity implements OnMapReadyCallback,
        FirebaseService.TrafficDataListener, TextToSpeech.OnInitListener {

    private GoogleMap mMap;
    private FirebaseService firebaseService;
    private GeminiService geminiService;
    private TextToSpeech textToSpeech;
    private Map<String, Circle> trafficMarkers;
    private Marker sourceMarker;
    private Marker destinationMarker;
    private GeoApiContext geoApiContext;
    private ExecutorService executorService;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private TrafficService trafficService;
    private Polyline routePolyline;
    private FloatingActionButton nearestHospitalFab;
    private View hospitalInfoCard;
    private TextView hospitalNameText;
    private TextView hospitalAddressText;
    private Button navigateButton;
    private List<Marker> hospitalMarkers = new ArrayList<>();
    private List<Hospital> hospitals = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    private PlacesClient placesClient;
    private LatLng lastKnownLocation;
    private Hospital nearestHospital;
    private double nearestHospitalDistance;
    private List<Circle> corridorJunctionMarkers = new ArrayList<>();
    private List<TrafficJunction> latestJunctionsCache;
    private List<ValueAnimator> corridorBlinkAnimators = new ArrayList<>();
    private Marker userAmbulanceMarker;

    private static class Hospital {
        LatLng location;
        String name;
        String address;

        Hospital(String name, String address, LatLng location) {
            this.name = name;
            this.address = address;
            this.location = location;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize services
        firebaseService = new FirebaseService();
        geminiService = new GeminiService();
        trafficMarkers = new HashMap<>();
        textToSpeech = new TextToSpeech(this, this);
        executorService = Executors.newSingleThreadExecutor();
        trafficService = new TrafficService();

        // Initialize GeoApiContext for Directions API
        geoApiContext = new GeoApiContext.Builder()
                .apiKey(getString(R.string.google_maps_key))
                .build();

        // Initialize Places
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }
        placesClient = Places.createClient(this);

        // Initialize views
        nearestHospitalFab = findViewById(R.id.nearestHospitalFab);
        hospitalInfoCard = findViewById(R.id.hospitalInfoCard);
        hospitalNameText = findViewById(R.id.hospitalNameText);
        hospitalAddressText = findViewById(R.id.hospitalAddressText);
        navigateButton = findViewById(R.id.navigateButton);
        nearestHospitalFab.setOnClickListener(v -> findAndRouteToNearestHospital());

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkLocationPermission();
        loadHospitalsFromAssets();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Set default location (e.g., city center)
        LatLng defaultLocation = new LatLng(12.9716, 77.5946); // Bangalore coordinates
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));

        // Enable location if permission is granted
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        // Add hospital markers
        addHospitalMarkers();

        // Start listening to traffic updates
        startTrafficUpdates();
    }

    @Override
    public void onTrafficDataUpdated(List<TrafficJunction> junctions) {
        latestJunctionsCache = junctions;
        runOnUiThread(() -> {
            updateTrafficMarkers(junctions);
            analyzeAndPredictTraffic(junctions);
        });
    }

    @Override
    public void onEmergencyVehicleDetected(TrafficJunction junction) {
        runOnUiThread(() -> {
            // Update map marker for emergency vehicle
            Circle circle = trafficMarkers.get(junction.getJunctionId());
            if (circle != null) {
                circle.setFillColor(Color.RED);
            }

            // Generate and speak emergency alert
            geminiService.generateVoiceAlert(junction, new GeminiService.GeminiCallback() {
                @Override
                public void onSuccess(String alert) {
                    runOnUiThread(() -> textToSpeech.speak(alert, TextToSpeech.QUEUE_FLUSH, null, null));
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                            "Error: " + error, Toast.LENGTH_SHORT).show());
                }
            });
        });
    }

    private void updateTrafficMarkers(List<TrafficJunction> junctions) {
        for (TrafficJunction junction : junctions) {
            Circle circle = trafficMarkers.get(junction.getJunctionId());
            LatLng position = new LatLng(junction.getLatitude(), junction.getLongitude());

            if (circle == null) {
                // Create new marker
                CircleOptions circleOptions = new CircleOptions()
                        .center(position)
                        .radius(100) // meters
                        .strokeWidth(2)
                        .strokeColor(Color.BLACK);
                circle = mMap.addCircle(circleOptions);
                trafficMarkers.put(junction.getJunctionId(), circle);
            }

            // Update circle color based on traffic density using TrafficService
            int color = trafficService.getTrafficColor(junction.getVehicleDensity());
            circle.setFillColor(color);
        }
    }

    private void analyzeAndPredictTraffic(List<TrafficJunction> junctions) {
        // Analyze traffic pattern
        geminiService.analyzeTrafficPattern(junctions, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(String analysis) {
                runOnUiThread(() -> {
                    TextView statusText = findViewById(R.id.trafficStatusText);
                    statusText.setText(analysis);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "Analysis Error: " + error, Toast.LENGTH_SHORT).show());
            }
        });

        // Predict future traffic
        geminiService.predictFutureTraffic(junctions, new GeminiService.GeminiCallback() {
            @Override
            public void onSuccess(String prediction) {
                runOnUiThread(() -> {
                    TextView predictionText = findViewById(R.id.predictionText);
                    predictionText.setText(prediction);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "Prediction Error: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                if (mMap != null) {
                    if (ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mMap.setMyLocationEnabled(true);
                    }
                }
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.setLanguage(Locale.US);
        }
    }

    private void startTrafficUpdates() {
        try {
            FirebaseDatabase.getInstance().getReference("traffic_data")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            try {
                                List<TrafficJunction> junctions = new ArrayList<>();
                                for (DataSnapshot junctionSnapshot : snapshot.getChildren()) {
                                    TrafficJunction junction = junctionSnapshot.getValue(TrafficJunction.class);
                                    if (junction != null) {
                                        junctions.add(junction);
                                    }
                                }
                                if (!junctions.isEmpty()) {
                                    updateTrafficMarkers(junctions);
                                    analyzeAndPredictTraffic(junctions);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                    "Error loading traffic data: " + error.getMessage(),
                                    Toast.LENGTH_SHORT).show());
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopTrafficUpdates() {
        try {
            // Clear all traffic markers
            for (Circle circle : trafficMarkers.values()) {
                if (circle != null) {
                    circle.remove();
                }
            }
            trafficMarkers.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null) {
            startTrafficUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTrafficUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        firebaseService.removeTrafficDataListener(this);
        executorService.shutdown();
        if (geoApiContext != null) {
            geoApiContext.shutdown();
        }
        stopTrafficUpdates();
    }

    // Helper to get current location (if available)
    private LatLng getCurrentLocation() {
        if (mMap != null && mMap.getMyLocation() != null) {
            android.location.Location loc = mMap.getMyLocation();
            return new LatLng(loc.getLatitude(), loc.getLongitude());
        }
        Toast.makeText(this, "Current location not available", Toast.LENGTH_SHORT).show();
        return null;
    }

    private void loadHospitalsFromAssets() {
        try {
            InputStream is = getAssets().open("hospitals.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONArray arr = new JSONArray(json);
            hospitals.clear();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String name = obj.getString("name");
                String address = obj.getString("address");
                double lat = obj.getDouble("lat");
                double lng = obj.getDouble("lng");
                hospitals.add(new Hospital(name, address, new LatLng(lat, lng)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addHospitalMarkers() {
        for (Marker marker : hospitalMarkers) {
            marker.remove();
        }
        hospitalMarkers.clear();
        for (Hospital hospital : hospitals) {
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(hospital.location)
                    .title(hospital.name)
                    .snippet(hospital.address)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_hospital)));
            hospitalMarkers.add(marker);
        }
    }

    private Hospital findNearestHospital(LatLng current) {
        Hospital nearest = null;
        double minDist = Double.MAX_VALUE;
        for (Hospital hospital : hospitals) {
            double dist = distanceBetween(current, hospital.location);
            if (dist < minDist) {
                minDist = dist;
                nearest = hospital;
            }
        }
        return nearest;
    }

    // Haversine formula for distance between two LatLngs
    private double distanceBetween(LatLng a, LatLng b) {
        double earthRadius = 6371e3; // meters
        double dLat = Math.toRadians(b.latitude - a.latitude);
        double dLng = Math.toRadians(b.longitude - a.longitude);
        double lat1 = Math.toRadians(a.latitude);
        double lat2 = Math.toRadians(b.latitude);
        double aVal = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(aVal), Math.sqrt(1 - aVal));
        return earthRadius * c;
    }

    private void findAndRouteToNearestHospital() {
        if (!checkLocationPermission()) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                lastKnownLocation = new LatLng(location.getLatitude(), location.getLongitude());
                searchNearbyHospitals(lastKnownLocation);
            } else {
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void searchNearbyHospitals(LatLng userLocation) {
        String apiKey = getString(R.string.google_maps_key);
        double lat = userLocation.latitude;
        double lng = userLocation.longitude;
        double radius = 5000; // 5km in meters
        String urlString = String.format(
                "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=%f,%f&radius=%f&type=hospital&key=%s",
                lat, lng, radius, apiKey);
        executorService.execute(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    org.json.JSONObject json = new org.json.JSONObject(response.toString());
                    org.json.JSONArray results = json.getJSONArray("results");
                    if (results.length() == 0) {
                        new Handler(Looper.getMainLooper()).post(
                                () -> Toast.makeText(this, "No hospitals found nearby", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    // Find the nearest hospital
                    double minDist = Double.MAX_VALUE;
                    org.json.JSONObject nearest = null;
                    for (int i = 0; i < results.length(); i++) {
                        org.json.JSONObject obj = results.getJSONObject(i);
                        org.json.JSONObject loc = obj.getJSONObject("geometry").getJSONObject("location");
                        double hLat = loc.getDouble("lat");
                        double hLng = loc.getDouble("lng");
                        double dist = distanceBetween(userLocation, new LatLng(hLat, hLng));
                        if (dist < minDist) {
                            minDist = dist;
                            nearest = obj;
                        }
                    }
                    if (nearest != null) {
                        String name = nearest.getString("name");
                        String address = nearest.optString("vicinity", "");
                        org.json.JSONObject loc = nearest.getJSONObject("geometry").getJSONObject("location");
                        double hLat = loc.getDouble("lat");
                        double hLng = loc.getDouble("lng");
                        LatLng hospitalLatLng = new LatLng(hLat, hLng);
                        new Handler(Looper.getMainLooper()).post(() -> routeToHospital(hospitalLatLng, name, address));
                    } else {
                        new Handler(Looper.getMainLooper()).post(
                                () -> Toast.makeText(this, "No hospitals found nearby", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> Toast
                            .makeText(this, "Places API HTTP error: " + responseCode, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> Toast
                        .makeText(this, "Error searching hospitals: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void routeToHospital(LatLng hospitalLatLng, String hospitalName, String hospitalAddress) {
        if (lastKnownLocation == null) {
            Toast.makeText(this, "Current location not available", Toast.LENGTH_SHORT).show();
            return;
        }
        // Remove previous markers and polylines
        if (routePolyline != null)
            routePolyline.remove();
        if (destinationMarker != null)
            destinationMarker.remove();
        if (userAmbulanceMarker != null)
            userAmbulanceMarker.remove();
        for (Circle c : corridorJunctionMarkers)
            c.remove();
        corridorJunctionMarkers.clear();
        // Add ambulance marker at user's current location
        try {
            userAmbulanceMarker = mMap.addMarker(new MarkerOptions()
                    .position(lastKnownLocation)
                    .title("You (Ambulance)")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_ambulance)) // TODO: Ensure
                                                                                         // ic_ambulance.png exists in
                                                                                         // drawable
                    .zIndex(20f));
        } catch (Exception e) {
            // fallback: default marker if ambulance icon missing
            userAmbulanceMarker = mMap.addMarker(new MarkerOptions()
                    .position(lastKnownLocation)
                    .title("You (Ambulance)")
                    .zIndex(20f));
        }
        destinationMarker = mMap.addMarker(new MarkerOptions()
                .position(hospitalLatLng)
                .title(hospitalName)
                .snippet(hospitalAddress)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_hospital)));
        executorService.execute(() -> {
            try {
                DirectionsResult result = DirectionsApi.newRequest(geoApiContext)
                        .origin(new com.google.maps.model.LatLng(lastKnownLocation.latitude,
                                lastKnownLocation.longitude))
                        .destination(
                                new com.google.maps.model.LatLng(hospitalLatLng.latitude, hospitalLatLng.longitude))
                        .mode(TravelMode.DRIVING)
                        .await();
                if (result.routes != null && result.routes.length > 0) {
                    final List<LatLng> path = decodePolyline(result.routes[0].overviewPolyline.getEncodedPath());
                    runOnUiThread(() -> {
                        animateGreenCorridor(path);
                        markJunctionsOnRoute(path);
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        for (LatLng point : path)
                            builder.include(point);
                        LatLngBounds bounds = builder.build();
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                        showHospitalInfo(hospitalName, hospitalAddress, hospitalLatLng);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "No route found", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error fetching route: " + e.getMessage(), Toast.LENGTH_SHORT)
                        .show());
            }
        });
    }

    // Animate a green polyline along the path
    private void animateGreenCorridor(List<LatLng> path) {
        if (routePolyline != null)
            routePolyline.remove();
        PolylineOptions polylineOptions = new PolylineOptions()
                .color(Color.GREEN)
                .width(24f)
                .geodesic(true);
        routePolyline = mMap.addPolyline(polylineOptions);
        ValueAnimator animator = ValueAnimator.ofInt(0, path.size());
        animator.setDuration(1200 + path.size() * 8); // Duration based on path length
        animator.addUpdateListener(animation -> {
            int points = (int) animation.getAnimatedValue();
            if (points > 1 && points <= path.size()) {
                routePolyline.setPoints(path.subList(0, points));
            }
        });
        animator.start();
    }

    // Mark green circles at traffic junctions along the route
    private void markJunctionsOnRoute(List<LatLng> path) {
        // Stop any previous blink animators
        for (ValueAnimator animator : corridorBlinkAnimators)
            animator.cancel();
        corridorBlinkAnimators.clear();
        for (Circle c : corridorJunctionMarkers)
            c.remove();
        corridorJunctionMarkers.clear();
        List<TrafficJunction> allJunctions = getLatestJunctions();
        if (allJunctions == null)
            return;
        double threshold = 30.0; // meters
        for (TrafficJunction junction : allJunctions) {
            LatLng junctionLatLng = new LatLng(junction.getLatitude(), junction.getLongitude());
            for (LatLng routePoint : path) {
                if (distanceBetween(junctionLatLng, routePoint) < threshold) {
                    Circle circle = mMap.addCircle(new CircleOptions()
                            .center(junctionLatLng)
                            .radius(50)
                            .strokeWidth(16f)
                            .strokeColor(0xFF00FF00)
                            .fillColor(0x9900FF00)
                            .zIndex(10f));
                    corridorJunctionMarkers.add(circle);
                    // Start blinking animation for this circle
                    ValueAnimator blink = ValueAnimator.ofObject(new ArgbEvaluator(), 0x9900FF00, 0xFF00FF00,
                            0x9900FF00);
                    blink.setDuration(1000);
                    blink.setRepeatCount(ValueAnimator.INFINITE);
                    blink.setRepeatMode(ValueAnimator.RESTART);
                    blink.addUpdateListener(animation -> {
                        int color = (int) animation.getAnimatedValue();
                        circle.setFillColor(color);
                    });
                    blink.start();
                    corridorBlinkAnimators.add(blink);
                    break;
                }
            }
        }
    }

    // Helper to get the latest list of junctions (cache or static)
    private List<TrafficJunction> getLatestJunctions() {
        return latestJunctionsCache;
    }

    private void showHospitalInfo(String name, String address, LatLng hospitalLatLng) {
        hospitalNameText.setText(name);
        hospitalAddressText.setText(address);
        double distance = 0;
        if (lastKnownLocation != null) {
            float[] results = new float[1];
            Location.distanceBetween(lastKnownLocation.latitude, lastKnownLocation.longitude, hospitalLatLng.latitude,
                    hospitalLatLng.longitude, results);
            distance = results[0] / 1000.0; // in km
        }
        hospitalInfoCard.setVisibility(View.VISIBLE);
        hospitalAddressText.setText(String.format("%s\n%.2f km away", address, distance));
        navigateButton.setOnClickListener(v -> openGoogleMapsNavigation(hospitalLatLng));
    }

    private void openGoogleMapsNavigation(LatLng dest) {
        String uri = "google.navigation:q=" + dest.latitude + "," + dest.longitude;
        Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        startActivity(intent);
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            LatLng p = new LatLng((lat / 1E5), (lng / 1E5));
            poly.add(p);
        }
        return poly;
    }
}