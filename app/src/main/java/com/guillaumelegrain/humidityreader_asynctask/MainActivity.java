package com.guillaumelegrain.humidityreader_asynctask;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.txusballesteros.SnakeView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import fr.cnam.smb116.thread.asynctask.HTTPHumiditySensor;


public class MainActivity extends AppCompatActivity {

    private static final String URL_SENSOR = "http://lmi92.cnam.fr/ds2438/ds2438/";
    private TextView mainTextView;
    private Button startButton;
    private Button stopButton;
    private Button setButton;
    private EditText urlEditText;
    private ProgressBar humidityProgressBar;
    private SnakeView snakeView;
    private int humidityPercentage;
    private String url;

    private UpdateHumidityAsyncTask updateHumidityAsyncTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Access view elements defined in layout XML
        mainTextView = (TextView) findViewById(R.id.textView);
        startButton = (Button) findViewById(R.id.startButton);
        stopButton = (Button) findViewById(R.id.stopButton);
        setButton = (Button) findViewById(R.id.setButton);
        urlEditText = (EditText) findViewById(R.id.urlEditText);
        humidityProgressBar = (ProgressBar) findViewById(R.id.humidityProgressBar);

        // configure graph
        snakeView = (SnakeView) findViewById(R.id.snake);
        snakeView.setMinValue(0);
        snakeView.setMaxValue(100);

        // get saved sensor url from preferences
        url = loadURL_SENSOR(getApplication());
        urlEditText.setText(url);

        //humidityProgressBar.setProgress(humidityPercentage);
    }

    private class UpdateHumidityAsyncTask extends AsyncTask<Void, Integer, Void> {

        private DateFormat dateFormat;
        private boolean isRunning;
        private static final long UPDATE_PERIOD_MS = 1 * 1000;

        @Override
        protected void onPreExecute() {
            Log.i("UpdateHumidityAsyncTask", "onPreExecute");
            isRunning = false;
            // create a date formatter to display dates into Strings
            dateFormat = new SimpleDateFormat();

            // Disable start button
            startButton.setClickable(false);
            startButton.setEnabled(false);

            // Enable stop button
            stopButton.setClickable(true);
            stopButton.setEnabled(true);
        }

        @Override
        protected Void doInBackground(Void...v) {
            Log.i("UpdateHumidityAsyncTask", "doInBackground");
            isRunning = true;
            HTTPHumiditySensor humiditySensor = new HTTPHumiditySensor(url);
            // Request every UPDATE_PERIOD_MS until the task is killed
            Float response = new Float(0);
            while (isRunning) {
                long startRequestTime = System.currentTimeMillis();
                try {
                    response = humiditySensor.value();
                    publishProgress(response.intValue());
                } catch (Exception e) {
                    e.printStackTrace();
                    isRunning = false;
                }
                long endRequestTime = System.currentTimeMillis();
                long responseDuration = startRequestTime - endRequestTime;
                // subtract response duration to account for variable response times
                SystemClock.sleep(UPDATE_PERIOD_MS - responseDuration);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Log.i("UpadteHumidityAsyncTask", "onProgressUpdate: " + values[0]);
            // humidityPercentage = progress[0];
            // Update UI
            humidityProgressBar.setProgress(values[0]);
            snakeView.addValue(values[0]);
            // create a calendar initialized to the current date and time
            Date currentDate = new Date();
            String dateString = dateFormat.format(currentDate);
            mainTextView.setText("[" + dateString + "] Humidity: " + values[0]);

            // warn user when there is no internet connection
            if (!isInternetOn()) {
                Toast.makeText(getApplication(), "No internet connection", Toast.LENGTH_SHORT);
            }
        }

        public void stop() {
            isRunning = false;
            // Enable start button
            startButton.setClickable(true);
            startButton.setEnabled(true);

            // Disable stop button
            stopButton.setClickable(false);
            stopButton.setEnabled(false);
        }
    }

    public void onClickStartButton(View v) {
        Log.i("MainActivity", "onClickStartButton");

        // Start updating
        // Create a new task for each start. Task can only be executed once
        updateHumidityAsyncTask = new UpdateHumidityAsyncTask();
        updateHumidityAsyncTask.execute();
    }

    public void onClickStopButton(View v) {
        // Stop updating
        updateHumidityAsyncTask.stop();
    }

    public void onClickEditText(View v) {
        Log.v("MainActivity", "onClickTextEdit");
        // Make Set button visible
        setButton.setVisibility(View.VISIBLE);

        // Stop updating
        if (updateHumidityAsyncTask != null) {
            updateHumidityAsyncTask.stop();
        }

        // Disable start button
        startButton.setClickable(false);
        startButton.setEnabled(false);
    }

    public void onClickSetButton(View v) {
        url = urlEditText.getText().toString();
        saveURL_SENSOR(v.getContext(), url);
        Log.v("MainActivity", url);
        // Enable start button
        startButton.setClickable(true);
        startButton.setEnabled(true);
        // Make Set button invisible
        setButton.setVisibility(View.INVISIBLE);
    }

    /**
     * Make sensor url persistent. Save url to preferences.
     * @param context
     * @param url
     */
    private static void saveURL_SENSOR(Context context, String url) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("url_sensor", url);
        edit.commit();
    }

    /**
     * load saved sensor url from preferences
     * @param context
     * @return String
     */
    private static String loadURL_SENSOR(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("url_sensor", URL_SENSOR);
    }

    /**
     * check if device is connected to the internet
     * @return boolean
     */
    public  final boolean isInternetOn() {
        final ConnectivityManager conMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            return true;
        }
        return false;
    }
}
