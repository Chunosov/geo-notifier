package org.project_orion.geonotifier;

import android.location.Location;
import android.location.LocationManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class GpsFormat {

    private static String _lastError;

    public static String getLastError() { return _lastError; }

    public static String formatProvider(Location loc) {
        if (loc == null) return "";
        String provider = loc.getProvider();
        if (provider.contentEquals(LocationManager.GPS_PROVIDER))
            return "(GPS)";
        if (provider.contentEquals(LocationManager.NETWORK_PROVIDER))
            return "(Сеть)";
        return "(" + provider + ")";
    }

    public static String format(Location loc) {
        if (loc == null) return "N/A";

        double lat = loc.getLatitude();
        String latMark = lat < 0? "ю.ш.": "с.ш.";
        String latStr = formatCoordinate(lat);

        double lon = loc.getLongitude();
        String lonMark = lon < 0? "з.д.": "в.д.";
        String lonStr = formatCoordinate(lon);

        return String.format("%s%s %s%s", latStr, latMark, lonStr, lonMark);
    }

    private static String formatCoordinate(double fractionalDeg) {
        double deg = Math.abs(fractionalDeg);
        double min = (deg - (int) deg) * 60;
        double sec = (min - (int) min) * 60;

        int d = (int) deg;
        int m = (int) min;
        int s = (int) Math.round(sec);

        if (s == 60) {
            s = 0;
            m++;
        }
        if (m == 60) {
            m = 0;
            d++;
        }

        return String.format(Locale.getDefault(), "%02d°%02d′%02d″", d, m, s);
    }

    private static final String JSON_KEY_LONGITUDE ="longitude";
    private static final String JSON_KEY_LATITUDE = "latitude";
    private static final String JSON_KEY_PROVIDER = "provider";
    private static final String JSON_KEY_ADDRESS = "address";
    private static final String JSON_KEY_HASH = "hash";
    private static final String JSON_HASH_SALT = "2c94919e-c92f-4b15-983f-9474b26d2663";

    public static String toJson(Location loc, String address) {
        JSONObject json = new JSONObject();
        try {
            json.put(JSON_KEY_LONGITUDE, loc.getLongitude());
            json.put(JSON_KEY_LATITUDE, loc.getLatitude());
            json.put(JSON_KEY_PROVIDER, loc.getProvider());
            json.put(JSON_KEY_ADDRESS, address);

            String longitude = json.getString(JSON_KEY_LONGITUDE);
            String latitude = json.getString(JSON_KEY_LATITUDE);
            String hash = md5(JSON_HASH_SALT + longitude + latitude);
            if (hash.isEmpty()) return "";

            json.put(JSON_KEY_HASH, hash);

            return json.toString();

        } catch (JSONException e) {
            _lastError = e.getLocalizedMessage();
            e.printStackTrace();
        }
        return "";
    }

    private static final String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            _lastError = e.getLocalizedMessage();
            e.printStackTrace();
        }
        return "";
    }
}
