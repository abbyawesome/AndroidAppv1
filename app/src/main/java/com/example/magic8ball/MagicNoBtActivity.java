package com.example.magic8ball;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;
import java.util.Random;

public class MagicNoBtActivity extends AppCompatActivity {


    //shake sensors
    private SensorManager sensorManager;
    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;

    //views
    private TextView mUserText;
    private Button mShakeBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_magicless);

        //sensor stuff
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Objects.requireNonNull(sensorManager).registerListener(
                shakeListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        mAccel = 10f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

        //id stuff
        mUserText = findViewById(R.id.magicless8BallTv);
        mShakeBtn = findViewById(R.id.magiclessShakeBtn);

        mShakeBtn.setOnClickListener(v -> shakeString());
    }

    /*
    detect if phone was shaken
    implement send w/ bluetooth to pi on this later
     */
    private final SensorEventListener shakeListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt(x * x + y * y + z * z);
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * .9f + delta;

            if (mAccel > 12) {
                Toast.makeText(getApplicationContext(),"Shake detected",Toast.LENGTH_LONG).show();
                shakeString();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(
                shakeListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(shakeListener);
    }

    /*
    shake to get string
     */
    public void shakeString() {
        Random random = new Random();
        int randomNumber = random.nextInt(12);
        String msg;
        switch(randomNumber) {
            case 0:
               msg = "It is certain";
               break;
            case 1:
                msg = "As I see it, yes";
                break;
            case 2:
                msg = "Ask again later";
                break;
            case 3:
                msg = "Don't count on it";
                break;
            case 4:
                msg = "It is decidedly so";
                break;
            case 5:
                msg = "Most likely";
                break;
            case 6:
                msg = "Reply hazy, try again";
                break;
            case 7:
                msg = "My reply is no";
                break;
            case 8:
                msg = "Without a doubt";
                break;
            case 9:
                msg = "Outlook good";
                break;
            case 10:
                msg = "Better not tell you now";
                break;
            default:
                msg = "My sources say no";
                break;
        }

        mUserText.setText(msg);
    }
}
