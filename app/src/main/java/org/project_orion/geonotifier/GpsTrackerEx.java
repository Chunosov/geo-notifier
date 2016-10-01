package org.project_orion.geonotifier;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

public class GpsTrackerEx implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "GeoNot.GpsTrackerEx";

    private final Context _context;

    private GoogleApiClient _apiClient = null;
    private GpsListener _listener = null;

    public GpsTrackerEx(Context context) {
        _context = context;
    }

    public void queryLocation() {
        if (_apiClient != null)
            queryLocationInternal();
        else
            connect();
    }

    private void queryLocationInternal() {
        Log.d(TAG, "Query location via Google Play API");
        new QueryLocationTask().execute();
    }

    public void setListener(GpsListener listener) {
        _listener = listener;
    }

    private void raiseLocationChanged(Location location) {
        if (_listener != null)
            _listener.onLocationChanged(location);
    }

    private void raiseLocationError(String message) {
        if (_listener != null)
            _listener.onLocationError(message);
    }

    private void connect() {
        Log.d(TAG, "Connect to Google Play API");
        if (!checkGooglePlayApi()) return;

        _apiClient = new GoogleApiClient.Builder(_context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        Log.d(TAG, "Google Play API: " + _apiClient.isConnected());
    }

    public void disconnect() {
        if (_apiClient != null)
            _apiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        queryLocationInternal();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        String errorMessage = connectionResult.getErrorMessage();
        Log.e(TAG, "Google Play API Client connection failed: " + errorMessage);
        raiseLocationError(errorMessage);
    }

    public String testGooglePlayApi() {
        GoogleApiAvailability apiCheck = GoogleApiAvailability.getInstance();
        int res = apiCheck.isGooglePlayServicesAvailable(_context);
        if (res == ConnectionResult.SUCCESS) {
            Log.d(TAG, "Google Play API available");
            return "";
        }
        String errorMessage = apiCheck.getErrorString(res);
        Log.e(TAG, "Google Play API is NOT available: " + errorMessage);
        return errorMessage;
    }

    private boolean checkGooglePlayApi() {
        String message = testGooglePlayApi();
        if (!message.isEmpty()) {
            raiseLocationError(message);
            return false;
        }
        return true;
    }

    private class QueryLocationTask extends AsyncTask<Void, Void, Location> {
        @Override
        protected Location doInBackground(Void... params) {
            return LocationServices.FusedLocationApi.getLastLocation(_apiClient);
        }

        @Override
        protected void onPostExecute(Location result) {
            raiseLocationChanged(result);
        }
    }
}
