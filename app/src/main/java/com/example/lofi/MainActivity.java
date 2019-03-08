package com.example.lofi;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.tensorflow.lite.Interpreter;


import android.content.res.AssetManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;

import java.lang.Math;
import android.view.View.OnClickListener;




public class MainActivity extends AppCompatActivity{

    private SensorManager sensorManager;
    private Sensor sensor;

    private SensorManager sensorManagerStep;
    private Sensor sensorStep;
    private long lastStepTime = 0;

    private final int SAMPLE_RATE_US = 50000; // Sample every 50ms for 20Hz

    private final int STRIDES_PER_CALC = 16;
    private double[] strides = new double[STRIDES_PER_CALC];
    // make effective stride rate average of last STRIDES_PER_CALC stride rates
    private int numStrides = 0; // increment on each new step

    public double getMean(double[] vals) {
        int num = vals.length;
        double sum = 0;

        for (int i = 0; i < num; i++) {
            sum += vals[i];
        }

        return sum/num;
    }

    public void zeroStrideArray() {
        for (int i = 0; i < STRIDES_PER_CALC; i++) {
            strides[i] = 0.0;
        }
    }

    SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                activityRecognizer.setSample(inputWindow, event.values[0], event.values[1], event.values[2]);

                if (activityRecognizer.num_samples_in_this_window == NUM_SAMPLES_PER_WINDOW) {
                    float[][] standardizedWindow = activityRecognizer.standardize(inputWindow);

                /*
                Log.i("INPUT_DEBUG", "Before standardization");
                Log.i("INPUT_DEBUG", Float.toString(inputWindow[0][0]) + " " +
                        Float.toString(inputWindow[0][1]) + " " + Float.toString(inputWindow[0][2]));
                Log.i("INPUT_DEBUG", "After standardization");
                Log.i("INPUT_DEBUG", Float.toString(activityRecognizer.inputWindow[0][0]) + " " +
                        Float.toString(activityRecognizer.inputWindow[0][1]) + " " +
                        Float.toString(activityRecognizer.inputWindow[0][2]));
                */

                    activityRecognizer.setInputWindow(standardizedWindow);

                /*
                for (int i = 0; i < NUM_SAMPLES_PER_WINDOW; i ++) {
                    Log.i("INPUT_DEBUG", Integer.toString(i));
                    for (int j = 0; j < NUM_AXES; j++) {
                        Log.i("INPUT_DEBUG", Float.toString(inputWindow[i][j]));
                    }
                }
                */


                    //Log.i("DEBUG", "About to run inference");
                    activityRecognizer.runInference();
                    //Log.i("DEBUG", "Ran inference, getting probabilities");
                    outputProbabilities = activityRecognizer.getOutputProbabilities();
                    //Log.i("DEBUG", "Got probabilities, displaying");


                /*
                for (int i = 0; i < NUM_OUTPUTS; i++) {
                    Log.i("OUTPUT_DEBUG", Float.toString(outputProbabilities[0][i]));
                }
                */

                    TextView activity1p_tv = findViewById(R.id.activity1p);
                    TextView activity2p_tv = findViewById(R.id.activity2p);
                    TextView activity3p_tv = findViewById(R.id.activity3p);
                    TextView activity4p_tv = findViewById(R.id.activity4p);
                    activity1p_tv.setText(String.format("%.2f", outputProbabilities[0][0]));
                    activity2p_tv.setText(String.format("%.2f", outputProbabilities[0][1]));
                    activity3p_tv.setText(String.format("%.2f", outputProbabilities[0][2]));
                    activity4p_tv.setText(String.format("%.2f", outputProbabilities[0][3]));

                    if (outputProbabilities[0][0] >= outputProbabilities[0][1] &&
                            outputProbabilities[0][0] >= outputProbabilities[0][2] &&
                            outputProbabilities[0][0] >= outputProbabilities[0][3]) {
                        activity1p_tv.setTextColor(Color.RED);
                        activity2p_tv.setTextColor(Color.BLACK);
                        activity3p_tv.setTextColor(Color.BLACK);
                        activity4p_tv.setTextColor(Color.BLACK);
                    } else if (outputProbabilities[0][1] >= outputProbabilities[0][0] &&
                            outputProbabilities[0][1] >= outputProbabilities[0][2] &&
                            outputProbabilities[0][1] >= outputProbabilities[0][3]) {
                        activity1p_tv.setTextColor(Color.BLACK);
                        activity2p_tv.setTextColor(Color.RED);
                        activity3p_tv.setTextColor(Color.BLACK);
                        activity4p_tv.setTextColor(Color.BLACK);
                    } else if (outputProbabilities[0][2] >= outputProbabilities[0][0] &&
                            outputProbabilities[0][2] >= outputProbabilities[0][1] &&
                            outputProbabilities[0][2] >= outputProbabilities[0][3]) {
                        activity1p_tv.setTextColor(Color.BLACK);
                        activity2p_tv.setTextColor(Color.BLACK);
                        activity3p_tv.setTextColor(Color.RED);
                        activity4p_tv.setTextColor(Color.BLACK);
                    } else {
                        activity1p_tv.setTextColor(Color.BLACK);
                        activity2p_tv.setTextColor(Color.BLACK);
                        activity3p_tv.setTextColor(Color.BLACK);
                        activity4p_tv.setTextColor(Color.RED);
                    }
                }
            }

            else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
                long timestamp = event.timestamp;
                long delta = timestamp - lastStepTime;
                double strideRate = 0.0;

                if (lastStepTime == 0) {
                    lastStepTime = timestamp;
                }
                else {
                    strideRate = 60.0 / (delta / 1000000000.0);
                    lastStepTime = timestamp;
                }

                TextView stride_tv = findViewById(R.id.stride);
                String toStride_tv = "Running stride rate: " + String.format("%.2f", strideRate) + " s/m";
                stride_tv.setText(toStride_tv);

                strides[numStrides++] = strideRate;

                if (numStrides == STRIDES_PER_CALC) {
                    double effectiveStrideRate = getMean(strides);
                    TextView eff_stride_tv = findViewById(R.id.eff_stride);
                    String toEff_stride_tv = "Effective stride rate: " + String.format("%.2f", effectiveStrideRate) + " s/m";
                    eff_stride_tv.setText(toEff_stride_tv);
                    numStrides = 0;
                }

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            /* TODO:
            Implement?
             */
        }
    };
    private final static int NUM_OUTPUTS = 4;
    private final static int NUM_SAMPLES_PER_WINDOW = 60;
    private final static int NUM_AXES = 3;

    private float[][] inputWindow = new float[NUM_SAMPLES_PER_WINDOW][NUM_AXES];
    private float[][] outputProbabilities = new float[1][NUM_OUTPUTS];

    private ActivityRecognizer activityRecognizer;
    private MusicRecommender musicRecommender;

    private final static int K = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activityRecognizer = new ActivityRecognizer(this);

        TextView activity1_tv = findViewById(R.id.activity1);
        activity1_tv.setText(R.string.activity1str); //set text for text view

        TextView activity2_tv = findViewById(R.id.activity2);
        activity2_tv.setText(R.string.activity2str); //set text for text view

        TextView activity3_tv = findViewById(R.id.activity3);
        activity3_tv.setText(R.string.activity3str); //set text for text view

        TextView activity4_tv = findViewById(R.id.activity4);
        activity4_tv.setText(R.string.activity4str); //set text for text view



        try {
            musicRecommender = new MusicRecommender(this);

            /* Testing KNN
            Song[] songs = musicRecommender.knn(5, -1.0f, 1.0f);
            for (Song s : songs) {
                Log.i("KNN", s.artist + " , " + s.title);
            }
            */


            TextView rec1 = findViewById(R.id.rec1);
            TextView rec2 = findViewById(R.id.rec2);
            TextView rec3 = findViewById(R.id.rec3);
            TextView rec4 = findViewById(R.id.rec4);
            TextView rec5 = findViewById(R.id.rec5);

            TextView[] recs = {rec1, rec2, rec3, rec4, rec5};

            Song[] songs = musicRecommender.knn(5, -1.3f, 1.3f);
            for (int i = 0; i < 5; i++) {
                String toTextView = "";
                toTextView += songs[i].artist + " - " + songs[i].title + " (V = " +
                        String.format("%.2f", songs[i].v) + ", A = " +
                        String.format("%.2f", songs[i].a) + ")";
                recs[i].setText(toTextView);
            }
        }
        catch (IOException e) {
            Log.e("OnCreate", "Music recommender instantiation failed");
        }

        // Manual input for song recommendation
        final EditText input_valence = findViewById(R.id.input_valence);
        final EditText input_arousal = findViewById(R.id.input_arousal);
        final EditText input_stride = findViewById(R.id.input_stride);
        Button get_songs = findViewById(R.id.get_songs);

        input_valence.setHint("val");
        input_arousal.setHint("ar");
        input_stride.setHint("str");

        get_songs.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                boolean valenceGiven = input_valence.getText().toString().trim().length() > 0;
                boolean arousalGiven = input_arousal.getText().toString().trim().length() > 0;
                boolean strideGiven = input_stride.getText().toString().trim().length() > 0;
                float thisValence = 0.0f;
                float thisArousal = 0.0f;
                float thisStride = 0.0f;

                if (valenceGiven) thisValence = Float.parseFloat(input_valence.getText().toString());
                if (arousalGiven) thisArousal = Float.parseFloat(input_arousal.getText().toString());
                if (strideGiven)  thisStride = Float.parseFloat(input_stride.getText().toString());


                TextView rec1 = findViewById(R.id.rec1);
                TextView rec2 = findViewById(R.id.rec2);
                TextView rec3 = findViewById(R.id.rec3);
                TextView rec4 = findViewById(R.id.rec4);
                TextView rec5 = findViewById(R.id.rec5);

                TextView[] recs = {rec1, rec2, rec3, rec4, rec5};

                // If no stride rate given, search based on VA
                if (!strideGiven) {
                    Song[] recommendations = musicRecommender.knn(K, thisValence, thisArousal);
                    for (int i = 0; i < K; i++) {
                        String toTextView = "";
                        toTextView += recommendations[i].artist + " - " + recommendations[i].title + " (V = " +
                                String.format("%.2f", recommendations[i].v) + ", A = " +
                                String.format("%.2f", recommendations[i].a) + ")";
                        recs[i].setText(toTextView);
                    }
                }

                // If stride rate given, search based on stride only (for now)
                else {
                    Song[] recommendations = musicRecommender.knn(K, thisStride);
                    for (int i = 0; i < K; i++) {
                        String toTextView = "";
                        toTextView += recommendations[i].artist + " - " + recommendations[i].title + " (T = " +
                                String.format("%.2f", recommendations[i].t) + ")";
                        recs[i].setText(toTextView);
                    }
                }
            }
                                     });


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {

            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(sensorEventListener, sensor, SAMPLE_RATE_US);

        } else {
            Log.e("ACC", "No accelerometer found");
        }

        sensorManagerStep = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (sensorManagerStep.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null) {

            sensorStep = sensorManagerStep.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            sensorManagerStep.registerListener(sensorEventListener, sensorStep, SensorManager.SENSOR_DELAY_FASTEST);

        } else {
            Log.e("ACC", "No step detector found");
        }

        zeroStrideArray();

    }


    /* TODO:
    implement active/non-active mode detection
    implement different recommendation functions depending on mode
     */

}


