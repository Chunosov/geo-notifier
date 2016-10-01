package org.project_orion.geonotifier;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.util.List;
import java.util.Locale;

public class GpsTracker implements LocationListener {

    private final Context _context;
    private final LocationManager _locationManager;

    public static final String Msg_NoGeocoder = "(no geocoder)";

    private GpsListener _listener = null;
    private Geocoder _geocoder = null;
    private int _initCounter = 0;
    private int _initIterations = 10;
    private boolean _initialization = false;
    private double _initialLongitude = 0;
    private double _initialLatitude = 0;

    private static final String TAG = "GeoNotifier.GpsTracker";

    private static final long MIN_DIST_FOR_GPS_UPDATE_FIRST = 1; // meters
    private static final long MIN_TIME_FOR_GPS_UPDATE_FIRST = 200; // milliseconds
    private static final long MIN_DIST_FOR_GPS_UPDATE = 10; // meters
    private static final long MIN_TIME_FOR_GPS_UPDATE = 1000 * 60; // milliseconds

    public GpsTracker(Context context) {
        _context = context;
        _locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void startTracking() {
        _initCounter = 0;
        _initialization = true;
        _initialLongitude = 0;
        _initialLatitude = 0;
        startTrackingInternal(true);
    }

    public void restartTracking() {
        _initialization = false;
        stopTracking();
        startTrackingInternal(false);
    }

    private void startTrackingInternal(boolean first) {

        String provider = getLocationProvider();
        if (provider != null) {
            long interval = first? MIN_TIME_FOR_GPS_UPDATE_FIRST: MIN_TIME_FOR_GPS_UPDATE;
            long distance = first? MIN_DIST_FOR_GPS_UPDATE_FIRST: MIN_DIST_FOR_GPS_UPDATE;
            _locationManager.requestLocationUpdates(provider, interval, distance, this);
        }
    }

    public void stopTracking() {
        _locationManager.removeUpdates(this);
    }

    public void queryLocation() {
        Log.d(TAG, "Query location via LocationManager");
        new QueryLocationTask().execute();
    }

    public void queryAddress(Location loc) {
        if (loc == null) raiseAddressAcquired("");
        else new AcquireAddressTask().execute(loc);
    }

    private String getLocationProvider() {
        if (_locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            return LocationManager.GPS_PROVIDER;

        if (_locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
            return LocationManager.NETWORK_PROVIDER;

        return null;
    }

    public void setListener(GpsListener listener) {
        _listener = listener;
    }

    @Override
    public void onLocationChanged(Location location) {

        if (_initialization) {
            if (location != null) {
                _initCounter++;
                _initialLongitude = location.getLongitude();
                _initialLatitude = location.getLatitude();
                if (_initCounter == _initIterations) {
                    double count = _initIterations;
                    location.setLongitude(_initialLongitude / count);
                    location.setLatitude(_initialLatitude / count);
                    raiseLocationChanged(location);
                    restartTracking();
                }
            }
            return;
        }

        raiseLocationChanged(location);
    }

    private void raiseLocationChanged(Location location) {
        if (_listener != null)
            _listener.onLocationChanged(location);
    }

    private void raiseProviderUnavailable(String provider) {
        if (_listener != null)
            _listener.onProviderUnavailable(provider);
    }

    private void raiseAddressAcquired(String address) {
        if (_listener != null)
            _listener.onAddressAcquired(address);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        Log.d(TAG, String.format("Provider changed: %s (%s)", s, formatProviderStatus(i)));

        if (i == LocationProvider.AVAILABLE)
            queryLocation();
        else raiseProviderUnavailable(s);
    }

    @Override
    public void onProviderEnabled(String s) {
        Log.d(TAG, "Provider enabled: " + s);

        queryLocation();
    }

    @Override
    public void onProviderDisabled(String s) {
        Log.d(TAG, "Provider disabled: " + s);

        queryLocation();
    }

    private static String formatProviderStatus(int s) {
        switch (s) {
            case LocationProvider.AVAILABLE: return "AVAILABLE";
            case LocationProvider.OUT_OF_SERVICE: return "OUT_OF_SERVICE";
            case LocationProvider.TEMPORARILY_UNAVAILABLE: return "TEMPORARILY_UNAVAILABLE";
        }
        return "";
    }

    private class QueryLocationTask extends AsyncTask<Void, Void, Location> {
        @Override
        protected Location doInBackground(Void... params) {
            String provider = getLocationProvider();
            return provider != null? _locationManager.getLastKnownLocation(provider): null;
        }

        @Override
        protected void onPostExecute(Location result) {
            raiseLocationChanged(result);
        }
    }

    private class AcquireAddressTask extends AsyncTask<Location, Void, String> {
        @Override
        protected String doInBackground(Location... params) {
            return queryAddressInternal(params[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            raiseAddressAcquired(result);
        }
    }

    // https://developer.android.com/training/location/display-address.html
    private String queryAddressInternal(Location loc) {
        if (loc == null)
            return "";

        if (!Geocoder.isPresent())
            return Msg_NoGeocoder;

        if (_geocoder == null)
            _geocoder = new Geocoder(_context, Locale.getDefault());

        String address = "";
        try {
            List<Address> addresses = _geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
            for (Address adr: addresses)
                address += adr.getLocality() + "\n";
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (address.equals("null"))
            return "";

        return address.trim();
    }
}
