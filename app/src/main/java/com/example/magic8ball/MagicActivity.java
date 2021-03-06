package com.example.magic8ball;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import java.util.Objects;

public class MagicActivity extends AppCompatActivity {

//    //shake sensors
//    private SensorManager sensorManager;
//    private float mAccel;
//    private float mAccelCurrent;
//    private float mAccelLast;

    //from main
    String deviceAddress = null;
    String deviceName = null;

    //handler and thread
    private BtManagerThread mBtThread = null;
    private StringBuffer mInStringBuffer;
    private StringBuffer mOutStringBuffer;

    //bluetooth
    private BluetoothAdapter mBtAdapter = null;

    //views
    private ImageView mHeartRateImg;
    private Button mBadBtn;
    private TextView mResponseTv;
    private Button mOverrideBtn;
    private TextView mCounterTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_magic);

        //view ids
        //mUserText = findViewById(R.id.magic8BallTv);
        mHeartRateImg = findViewById(R.id.magicHeartRateIv);
        mResponseTv = findViewById(R.id.magicDataTv);
        mCounterTv = findViewById(R.id.magicUserTv);
        mBadBtn = findViewById(R.id.magicHiBPMBtn);
        mBadBtn.setOnClickListener(v -> highBPMButton());
        mOverrideBtn = findViewById(R.id.magicOverrideBtn);
        mOverrideBtn.setOnClickListener(v -> highBPMButton());

        //initialize BtManagerThread
        mBtThread = new BtManagerThread(this, mHandler);
        mInStringBuffer = new StringBuffer();
        mOutStringBuffer = new StringBuffer();

        //bluetooth setup
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            sendToast("Bluetooth not available");
            this.finish();
        } else if (!mBtAdapter.isEnabled()) {
            sendToast("Bluetooth not on");
            this.finish();
        }

        //get bluetooth extras
        Intent fromMainIntent = getIntent();
        deviceName = fromMainIntent.getStringExtra(MainActivity.EXTRA_NAME);
        deviceAddress = fromMainIntent.getStringExtra(MainActivity.EXTRA_ADDRESS);

        //bluetooth port number
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int port_number = 1;
        if (!sharedPreferences.getBoolean("default_port",true)) {
            String port_value = sharedPreferences.getString("port","0");
            port_number = Integer.parseInt(port_value);
        }

        //connect to device
        BluetoothDevice device = mBtAdapter.getRemoteDevice(deviceAddress);
        mBtThread.connect(device,port_number,true);
    }


    /*
    Lazy function to create toasts instead of writing them out every time
     */
    private void sendToast(String msg) {
        Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_SHORT).show();
    }

    /*
    high BPM button sends off that we're ok now
     */
    private void highBPMButton() {
        vibrate(false);

        //send button pressed
        send("1,0,0,0.0,1.0\n");

        //send button released
        send("0,0,0,0.0,1.0\n");

        mBadBtn.setVisibility(View.INVISIBLE);
    }

    /*
   vibrate the phone
    */
    private void vibrate(boolean on) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if(on) {
            //start with no delay
            //vibrate for 100ms, wait 1000ms, and repeat
            long[] pattern = {0, 100, 1000};
            v.vibrate(pattern,0);
        } else {
            v.cancel();
        }

    }


    /*
    lazy function to count how many bad heart rates we've gotten
     */
    private void newBPM(Boolean good) {
        String string = "bad";

        if (good) {
            string = "good";
        }

        String msg = "Received " + string + " BPM";
        mCounterTv.setText(msg);
    }


    /*
    Set up handler
    Handles messages between magic activity and BT manager thread
     */
    public Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BtManagerThread.STATE_CONNECTED:
                            Log.d("status","connected");
                            msg("Connected to " + deviceName);
                            //matrix: set visibility

                            //send protocol version to server
                            send("3," + Constants.PROTOCOL_VERSION + "," + Constants.CLIENT_NAME + "\n");
                            break;

                        case BtManagerThread.STATE_CONNECTING:
                            Log.d("status","connecting");
                            msg("Connecting to " + deviceName);
                            //matrix.setVisibility(View.INVISIBLE);
                            break;

                        case BtManagerThread.STATE_LISTEN:
                        case BtManagerThread.STATE_NONE:
                            Log.d("status","not connected");
                            msg("Not connected");
                            disconnect();
                            break;
                    }
                    break;

                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMsg = new String(writeBuf);
                    break;

                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;

                    //construct string from valid bytes
                    String readData = new String(readBuf, 0,msg.arg1);

                    //message received
                    parseData(readData);
                    break;

                case Constants.MESSAGE_TOAST:
                    sendToast(msg.getData().getString(Constants.TOAST));
                    break;
            }
        }
    };


    /**
     * functions used by handler
    **/

    /*
    display message from handler in text view
     */
    private void msg(String message) {
        mResponseTv.setText(message);
    }

    /*
    send version to handler
     */
    public void send(String message) {
        // Check that we're actually connected before trying anything
        if (mBtThread.getState() != BtManagerThread.STATE_CONNECTED) {
            Toast.makeText(this, "cant send message - not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mBtThread.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
        }
    }

    /*
    read data to device
     */
    private void parseData(String data) {
        //add message to buffer
        mInStringBuffer.append(data);

        //find any complete messages
        String[] messages = mInStringBuffer.toString().split("\\n");
        int numbMessages = messages.length;

        //check for \n at end of message
        //if not there, message incomplete, ignore it
        if (!mInStringBuffer.toString().endsWith("\n")) {
            numbMessages = numbMessages - 1;
        }

        //clean of any processed messages
        if (mInStringBuffer.lastIndexOf("\n") > -1) {
            mInStringBuffer.delete(0, mInStringBuffer.lastIndexOf("\n") + 1);
        }

        //process messages
        for (int messageNumb = 0; messageNumb < numbMessages; messageNumb++) {
            processMessage(messages[messageNumb]);
        }
    }

    /*
    using message from pi, decide what to do on the app end
     */
    private void processMessage(String message) {
        //split message string into its pieces
        String[] parameters = message.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        boolean invalid = false;

        //check the message type to make sure it's valid
        if (parameters.length > 0) {
            switch (parameters[0]) {
                case "4":
                case "5":
                    invalid = processSetMatrixMessage(parameters);
                    break;
                default:
                    invalid = true;
            }
        }

        if (invalid) {
            msg("Error: Invalid message: " + message + "");
        }
    }

    /*
    display the heart rate
     */
    private void heartRate(String r) {
        int val = Integer.parseInt(r,16);

        if (val > 5) {
            //high heart rate, turn on button
            mBadBtn.setVisibility(View.VISIBLE);
            vibrate(true);
            mHeartRateImg.setImageResource(R.drawable.bad_foreground);
            newBPM(false);
        } else {
            mHeartRateImg.setImageResource(R.drawable.good_foreground);
            vibrate(false);
            newBPM(true);
        }
    }



    /*
    basically hijack the color part here to use for layout string
     */
    private boolean processSetMatrixMessage(String[] parameters) {
        // "4,[color],[square],[border],[visible],[cols],[rows]"
        // "5,[color],[square],[border],[visible],[col],[row]"

        boolean invalid = false;

        //check length
        if (parameters.length == 7) {
            String color = parameters[1];
            // 0 = #
            // 1-3 = r
            // 3-5 = g
            // 5-7 = b
            // 7-9 = alpha

            //color = color.substring(3,5);
            //shakeString(color);

            String red = color.substring(1,3);

            heartRate(red);

        } else {
            invalid = true;
        }

        return invalid;
    }


    /*
    disconnect from bt thread
     */
    private void disconnect() {
        if (mBtThread != null) {
            mBtThread.stop();
        }

        finish();
    }
}
