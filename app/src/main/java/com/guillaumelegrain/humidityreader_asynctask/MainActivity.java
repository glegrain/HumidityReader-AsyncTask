package com.guillaumelegrain.humidityreader_asynctask;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import fr.cnam.smb116.thread.asynctask.HTTPHumiditySensor;


public class MainActivity extends AppCompatActivity {

    private TextView mainTextView;
    private Button startButton;
    private Button stopButton;
    private ProgressBar humidityProgressBar;
    private int humidityPercentage;

    private UpdateHumidityAsyncTask updateHumidityAsyncTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Access view elements defined in layout XML
        mainTextView = (TextView) findViewById(R.id.textView);
        startButton = (Button) findViewById(R.id.startButton);
        stopButton = (Button) findViewById(R.id.stopButton);
        humidityProgressBar = (ProgressBar) findViewById(R.id.humidityProgressBar);

        //humidityProgressBar.setProgress(humidityPercentage);
    }

    private class UpdateHumidityAsyncTask extends AsyncTask<Void, Integer, Float> {

        private DateFormat dateFormat;
        private static final long UPDATE_PERIOD_MS = 5 * 1000;

        @Override
        protected void onPreExecute() {
            // create a date formatter to display dates into Strings
            dateFormat = new SimpleDateFormat();
        }

        @Override
        protected Float doInBackground(Void...v) {
            Log.i("UpdateHumidityAsyncTask", "doInBackground");
            Float response = new Float(0);
            HTTPHumiditySensor humiditySensor = new HTTPHumiditySensor(
                    "http://lmi92.cnam.fr/ds2438/ds2438/");
            // Request every UPDATE_PERIOD_MS until the task is killed
            while (true) {
                long startRequestTime = System.currentTimeMillis();
                try {
                    response = humiditySensor.value();
                    publishProgress(response.intValue());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                long endRequestTime = System.currentTimeMillis();
                long responseDuration = startRequestTime - endRequestTime;
                // subtract response duration to account for variable response times
                SystemClock.sleep(UPDATE_PERIOD_MS - responseDuration);
            }
            //return response;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Log.i("UpadteHumidityAsyncTask", "onProgressUpdate: " + values[0]);
            // humidityPercentage = progress[0];
            // Update UI
            humidityProgressBar.setProgress(values[0]);
            // create a calendar initialized to the current date and time
            Date currentDate = new Date();
            String dateString = dateFormat.format(currentDate);
            mainTextView.setText("[" + dateString + "] Humidity: " + values[0]);
        }
    }

    public void onClickStartButton(View v) {
        Log.i("MainActivity", "onClickStartButton");

        updateHumidityAsyncTask = new UpdateHumidityAsyncTask();
        updateHumidityAsyncTask.execute();

        // Disable start button
        //Button startButton = (Button) findViewById(R.id.startButton);
        startButton.setClickable(false);
        //startButton.setBackgroundColor(Color.DKGRAY);

        // Enable stop button
        //Button stopButton = (Button) findViewById(R.id.stopButton);
        stopButton.setClickable(true);
        //stopButton.setBackgroundColor(Color.LTGRAY);
    }

    public void onClickStopButton(View v) {
        // Enable start button
        //Button startButton = (Button) findViewById(R.id.startButton);
        startButton.setClickable(true);
        //startButton.setBackgroundColor(Color.LTGRAY);

        // Disable stop button
        //Button stopButton = (Button) findViewById(R.id.stopButton);
        stopButton.setClickable(false);
        //stopButton.setBackgroundColor(Color.DKGRAY);
    }
}
