package org.project_orion.geonotifier;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends FragmentActivity
        implements View.OnClickListener, GpsListener, OnMapReadyCallback {

    private Button _buttonSend = null;
    private EditText _editLocation = null;
    private EditText _editServiceUrl = null;
    private TextView _titleLocation = null;
    private ProgressBar _progressBar = null;
    private GpsTracker _tracker = null;
    //private GpsTrackerEx _trackerEx = null;
    private NetWorker _network = null;
    private Location _location = null;
    private String _address = null;
    private GoogleMap _map = null;
    private Marker _gpsMarker = null;

    private static final String TAG = "GeoNotifier";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // lock screen for always Portrait mode
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        _progressBar = (ProgressBar) findViewById(R.id.progressBar);
        _editLocation = (EditText) findViewById(R.id.editLocation);
        _editServiceUrl = (EditText) findViewById(R.id.editServiceUrl);
        _titleLocation = (TextView) findViewById(R.id.titleLocation);

        _buttonSend = (Button) findViewById(R.id.buttonSend);
        _buttonSend.setOnClickListener(this);

        findViewById(R.id.buttonUpdate).setOnClickListener(this);
        findViewById(R.id.buttonUpdateEx).setOnClickListener(this);
        findViewById(R.id.buttonCenterMap).setOnClickListener(this);

        _tracker = new GpsTracker(getBaseContext());
        _tracker.setListener(this);

        //_trackerEx = new GpsTrackerEx(getBaseContext());
        //_trackerEx.setListener(this);

        _network = new NetWorker(getBaseContext());
        showNetStatus();

        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "Start");
        super.onStart();

        loadSettings();

        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler(this));
    }

    protected void onStop() {
        Log.d(TAG, "Stop");
        super.onStop();

        _tracker.stopTracking();
        //_trackerEx.disconnect();

        saveSettings();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "Pause");
        super.onPause();

        _tracker.stopTracking();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "Resume");
        super.onResume();

        _tracker.startTracking();
        _tracker.queryLocation();
        //_trackerEx.queryLocation();

        showNetStatus();
    }

    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.buttonSend:
                buttonSendClicked();
                break;

            case R.id.buttonUpdate:
                _tracker.queryLocation();
                break;

            case R.id.buttonUpdateEx:
                //_trackerEx.queryLocation();
                break;

            case R.id.buttonCenterMap:
                _tracker.queryLocation();
                //centerMapOnMarker();
                break;
        }
    }

    private void loadSettings() {
        // https://developer.android.com/guide/topics/data/data-storage.html
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        String url = prefs.getString("serviceUrl", "http://httpbin.org/post");
        _editServiceUrl.setText(url);
    }

    private void saveSettings() {
        SharedPreferences.Editor prefs = getPreferences(MODE_PRIVATE).edit();
        prefs.putString("serviceUrl", getServiceUrl());
        prefs.apply();
    }

    private void buttonSendClicked() {
        Log.d(TAG, "Send button click");

        if (_location == null) {
            showToast("Местоположение не определено");
            return;
        }
        String url = getServiceUrl();
        if (url.isEmpty()) {
            showToast("Не задан URL приемника");
            return;
        }
        try {
            sendLocation(url);
        } catch (Exception e) {
            e.printStackTrace();
            showToast(e.getMessage());
        }
    }

    private void showToast(String text) {
        Toast t = Toast.makeText(getBaseContext(), text, Toast.LENGTH_SHORT);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }

    private void showError(String title, String message) {
        AlertDialog d = new AlertDialog.Builder(MainActivity.this).create();
        d.setTitle(title);
        d.setMessage(message);
        d.setIcon(android.R.drawable.ic_dialog_alert);
        d.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {}
        });
        d.show();
    }

    private String getServiceUrl() {
        return _editServiceUrl.getText().toString().trim();
    }

    public void onLocationChanged(Location loc) {
        Log.d(TAG, "Location changed: " + GpsFormat.format(loc));
        showToast(loc == null? "Местоположение недоступно": "Местоположение принято");
        _location = loc;
        _tracker.queryAddress(loc);
        showLocation();
        updateLocationMarker();
    }

    @Override
    public void onAddressAcquired(String address) {
        _address = address;
        showLocation();
    }

    @Override
    public void onProviderUnavailable(String provider) {}

    @Override
    public void onLocationError(String message) {
        showError("Позиционирование", message);
    }

    private void showLocation() {
        _titleLocation.setText(String.format("Местоположение %s", GpsFormat.formatProvider(_location)));

        String location = GpsFormat.format(_location);
        if (_address != null && !_address.isEmpty())
            location = String.format("%s\n%s", location, _address);

        _editLocation.setText(location);
    }

    private void sendLocation(String url) {
        if (_location == null) return;

        String address = _address;
        if (address.equals(GpsTracker.Msg_NoGeocoder)) address = "";

        String json = GpsFormat.toJson(_location, address);
        if (json.isEmpty()) {
            showError("Сериализация", GpsFormat.getLastError());
            return;
        }

        new SendLocationTask().execute(url, json);
    }

    private void showNetStatus() {
        boolean conn = _network.isConnected();
        Log.d(TAG, "Network: " + conn);
        _buttonSend.setText(conn? "Отправить": "Нет сети");
        _buttonSend.setEnabled(conn);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        _map = googleMap;
        updateLocationMarker();
    }

    private void updateLocationMarker() {
        if (_map == null || _location == null) return;

        //LatLng pos = new LatLng(54.9439, 83.1936);
        LatLng pos = new LatLng(_location.getLatitude(), _location.getLongitude());
        if (_gpsMarker == null)
            _gpsMarker = _map.addMarker(new MarkerOptions().position(pos));
        else _gpsMarker.setPosition(pos);

        Log.d(TAG, "Marker position: " + _gpsMarker.getPosition().toString());

        centerMapOnMarker();
    }

    private void centerMapOnMarker() {
        if (_gpsMarker == null || _map == null) return;

        LatLng pos = _gpsMarker.getPosition();
        _map.animateCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder().target(pos).zoom(16).build()));
    }

    private class SendLocationTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            _progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            return _network.send(params[0], params[1]);
        }

        @Override
        protected void onCancelled() {
            _progressBar.setVisibility(View.GONE);
        }

        @Override
        protected void onPostExecute(String result) {
            _progressBar.setVisibility(View.GONE);

            if (result.isEmpty())
                showToast("Местоположение отправлено");
            else showError("Отправка", result);
        }
    }
}
