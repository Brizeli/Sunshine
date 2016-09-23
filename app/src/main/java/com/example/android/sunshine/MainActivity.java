package com.example.android.sunshine;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private ArrayAdapter<String> mForecastAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        String[] array = {"Mon 23/6 - Sunny - 31/17",
//                "Tue 24/6 - Foggy - 21/8",
//                "Wed 25/6 - Cloudy - 22/17",
//                "Thurs 26/6 - Rainy - 18/11",
//                "Fri 27/6 - Foggy - 21/10",
//                "Sat 28/6 - TRAPPED IN WEATHERSTATION - 23/18",
//                "Sun 29/6 - Sunny - 20/7",
//                "Tue 24/6 - Foggy - 21/8",
//                "Wed 25/6 - Cloudy - 22/17",
//                "Thurs 26/6 - Rainy - 18/11",
//                "Fri 27/6 - Foggy - 21/10",
//                "Sat 28/6 - TRAPPED IN WEATHERSTATION - 23/18",
//                "Sun 29/6 - Sunny - 20/7",
//        };
//        List<String> weekForecast = new ArrayList<>(Arrays.asList(array));
        mForecastAdapter = new ArrayAdapter<>(this,
                                              R.layout.list_item_forecast,
                                              R.id.txtVw_listItemForecast,
                                              new ArrayList());
        ListView lv = (ListView) findViewById(R.id.lstVw_forecast);
        if (lv != null) {
            lv.setAdapter(mForecastAdapter);
            lv.setOnItemClickListener(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateWeather(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.action_refresh:
                return updateWeather(this);
            case R.id.action_settings:
                return SettingsActivity.launch(this);
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean updateWeather(Context context) {
        try {
            String location = SettingsActivity.getLocation(context);
            if (location == null || TextUtils.isEmpty(location)) return false;
            new FetchWeatherTask().execute(location);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this, DetailActivity.class)
                .putExtra(
                        Intent.EXTRA_TEXT,
                        mForecastAdapter.getItem(position));
        startActivity(intent);
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = getClass().getSimpleName();

        @Override
        protected String[] doInBackground(String... params) {
            if (params.length == 0) return null;
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String forecastJsonStr = null;
            String format = "json";//redundant
            String units = "metric";
            int numDays = 16;
            String apiKey = BuildConfig.OPEN_WEATHER_MAP_API_KEY;
            try {
                final String FORECAST_BASE_URL =
                        "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String APPID_PARAM = "APPID";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .appendQueryParameter(APPID_PARAM, apiKey)
                        .build();

                URL url = new URL(builtUri.toString());
                Log.v(LOG_TAG, "Built URI " + builtUri.toString());
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) return null; // Nothing to do.
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) buffer.append(line + "\n");
                if (buffer.length() == 0) return null; // Stream was empty.  No point in parsing.
                forecastJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                if (forecastJsonStr != null)
                    return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
            }
            return null;
        }

        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays) throws JSONException {
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            String[] resultStrs = new String[numDays];
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            boolean isImperial = SettingsActivity.isImperial(getApplicationContext());

            for (int i = 0; i < weatherArray.length(); i++, cal.add(Calendar.DATE, 1)) {
                String day = getReadableDateString(cal.getTimeInMillis());

                JSONObject dayForecast = weatherArray.getJSONObject(i);
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                String description = weatherObject.getString(OWM_DESCRIPTION);

                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);
                String highAndLow = formatHighLows(high, low, isImperial);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }
            for (String s : resultStrs) Log.v(LOG_TAG, "Forecast entry: " + s);
            return resultStrs;
        }

        private String formatHighLows(double high, double low, boolean isImperial) {
            if (isImperial) {
                high = (high * 1.8) + 32;
                low = (low * 1.8) + 32;
            }
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);
            return roundedHigh + "/" + roundedLow;
        }

        private String getReadableDateString(long time) {
            return new SimpleDateFormat("EEE MMM dd", Locale.getDefault()).format(time);
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mForecastAdapter.clear();
                mForecastAdapter.addAll(result);
            }
        }
    }

    public static class WeatherDataParser {
        public static double getMaxTemperatureForDay(String weatherJsonStr, int dayIndex) throws JSONException {
            JSONObject o = new JSONObject(weatherJsonStr);
            JSONArray list = o.getJSONArray("list");
            JSONObject day = list.getJSONObject(dayIndex);
            JSONObject temp = day.getJSONObject("temp");
            return temp.getDouble("max");
        }
    }
}
