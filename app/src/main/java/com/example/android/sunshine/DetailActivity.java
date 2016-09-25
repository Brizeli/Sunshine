package com.example.android.sunshine;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class DetailActivity extends AppCompatActivity {

    private static final String LOG_TAG = DetailActivity.class.getSimpleName();
    private static final String SHARE_HASHTAG = " #SunshineApp";
    private String mForecastString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
            mForecastString = intent.getStringExtra(Intent.EXTRA_TEXT);
            TextView txtVw_detail = (TextView) findViewById(R.id.txtVw_detail);
            if (txtVw_detail != null) txtVw_detail.setText(mForecastString);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail, menu);
        MenuItem item = menu.findItem(R.id.action_share);
        ShareActionProvider mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        if (mShareActionProvider != null) mShareActionProvider.setShareIntent(createShareForecastIntent());
        else Log.d(LOG_TAG, "Share Action Provider is null?");
        return super.onCreateOptionsMenu(menu);
    }

    private Intent createShareForecastIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mForecastString + SHARE_HASHTAG);
        return shareIntent;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.action_settings:
                return SettingsActivity.launch(this);
            case R.id.action_map:
                return SettingsActivity.openPreferredLocationInMap(this);
        }
        return super.onOptionsItemSelected(item);
    }

}
