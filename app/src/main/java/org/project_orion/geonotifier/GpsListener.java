package org.project_orion.geonotifier;

import android.location.Location;

public interface GpsListener {
    void onLocationChanged(Location location);
    void onLocationError(String message);
    void onAddressAcquired(String address);
    void onProviderUnavailable(String provider);
}
