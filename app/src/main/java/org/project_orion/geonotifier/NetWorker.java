package org.project_orion.geonotifier;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class NetWorker {

    private final Context _context;

    private static final String TAG = "GeoNotifier.NetWorker";

    public NetWorker(Context context) {
        _context = context;
    }

    public boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager)
                _context.getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo net = cm.getActiveNetworkInfo();
        return net != null && net.isConnected();
    }

    public String send(String url, String json)  {
        Log.d(TAG, "URL: " + url);
        Log.d(TAG, "JSON: " + json);
        try {
            HttpPost post = new HttpPost(url);
            post.setEntity(new StringEntity(json));
            post.setHeader("Accept", "application/json");
            post.setHeader("Content-type", "application/json");

            HttpResponse response = new DefaultHttpClient().execute(post);
            if (response == null) return "Не получен ответ от сервера";

            StatusLine status = response.getStatusLine();
            if (status == null) return "Получен неверный ответ от сервера (1)";
            int statusCode = status.getStatusCode();
            if (statusCode != 200)
                return String.format("Сервер вернул код ошибки %d (%s)", statusCode, status.getReasonPhrase());

            InputStream stream = response.getEntity().getContent();
            if (stream == null) return "Получен неверный ответ от сервера (2)";

            Log.d(TAG, "Server response: " + readStream(stream));

        } catch (Exception e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }
        return "";
    }

    private static String readStream(InputStream inputStream) throws IOException {
        String line;
        String result = "";
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        while ((line = bufferedReader.readLine()) != null)
            result += line;
        inputStream.close();
        return result;
    }
}
