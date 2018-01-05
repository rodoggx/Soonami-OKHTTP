package com.example.rodoggx.soonamiokhttp;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.text.SimpleDateFormat;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    public static final String USG_URL = "https://earthquake.usgs.gov/fdsnws/event/1/";
    private static final String TAG = "TAG_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new OkhttpTask().execute();
    }

    public class OkhttpTask extends AsyncTask<URL, Void, Event> {
        @Override
        protected Event doInBackground(URL... urls) {
            OkHttpClient client = new OkHttpClient();

            HttpUrl.Builder urlBuilder = HttpUrl.parse(USG_URL).newBuilder();
            urlBuilder.addPathSegment("query");
            urlBuilder.addQueryParameter("format", "geojson");
            urlBuilder.addQueryParameter("starttime", "2014-01-01");
            urlBuilder.addQueryParameter("endtime", "2014-12-01");
            urlBuilder.addQueryParameter("minmagnitude", "8.2");
            String url = urlBuilder.build().toString();

            Request request = new Request.Builder().url(url).build();
            String jsonResponse = "";
            try {
                Response response = client.newCall(request).execute();
                jsonResponse = response.body().string();
            } catch (Exception e) {
                Log.e(TAG, "doInBackground: ", e);
            }
            Event earthquake = extractFeatureFromJson(jsonResponse);
            return earthquake;
        }

        @Override
        protected void onPostExecute(Event event) {
            if (event == null) {
                return;
            }
            updateUi(event);
        }
    }

    private void updateUi(Event earthquake) {
        TextView titleView = (TextView) findViewById(R.id.title);
        titleView.setText(earthquake.title);

        TextView dateView = (TextView) findViewById(R.id.date);
        dateView.setText(getDateString(earthquake.time));

        TextView tsunamiView = (TextView) findViewById(R.id.tsunami_alert);
        tsunamiView.setText(getTsunamiAlertString(earthquake.tsunamiAlert));
    }

    private String getDateString(long time) {
        SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy 'at' HH:mm:ss z");
        return formatter.format(time);
    }

    private String getTsunamiAlertString(int tsunamiAlert) {
        switch (tsunamiAlert) {
            case 0:
                return getString(R.string.alert_no);
            case 1:
                return getString(R.string.alert_yes);
            default:
                return getString(R.string.alert_not_available);
        }
    }

    private Event extractFeatureFromJson(String jsonResponse) {
        if (TextUtils.isEmpty(jsonResponse)) {
            return null;
        }
        try {
            JSONObject baseJsonResponse = new JSONObject(jsonResponse);
            JSONArray featureArray = baseJsonResponse.getJSONArray("features");

            if (featureArray.length() > 0) {
                JSONObject firstFeature = featureArray.getJSONObject(0);
                JSONObject properties = firstFeature.getJSONObject("properties");

                String title = properties.getString("title");
                long time = properties.getLong("time");
                int tsunamiAlert = properties.getInt("tsunami");

                return new Event(title, time, tsunamiAlert);
            }
        } catch (JSONException e) {
            Log.e(TAG, "extractFeatureFromJson: ", e);
        }
        return null;
    }
}
