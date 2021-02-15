package com.example.magic8ball;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.Set;

public class MainActivity
        extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {


    private BluetoothAdapter mBtAdapter = null;
    private Set<BluetoothDevice> pairedDevices;
    private ArrayAdapter<String> mBTArrayAdapter;

    public static String EXTRA_ADDRESS = "device_address";
    public static String EXTRA_NAME = "device_name";

    private static final int ENABLE_BT = 1;

    private TextView connectView;
    private Button mBtOnBtn;
    private Button mBtOffBtn;
    private Button mRefreshBtn;
    private ListView mPairedLv;
    private Button mGo2MagiclessBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set up find view by id
        connectView = findViewById(R.id.connectView);
        mBtOnBtn = findViewById(R.id.mainBluetoothOnBtn);
        mBtOffBtn = findViewById(R.id.mainBluetoothOffBtn);
        mRefreshBtn = findViewById(R.id.mainShowPairedBtn);
        mPairedLv = findViewById(R.id.mainShowPairedLv);
        mGo2MagiclessBtn = findViewById(R.id.mainGo2Magicless);

        //set up array adapter
        mBTArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        //list view setup
        mPairedLv.setAdapter(mBTArrayAdapter);
        mPairedLv.setOnItemClickListener(mDeviceClickListener);

        //check for bluetooth
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBtAdapter == null) {
            //no bluetooth ability
            sendToast("Bluetooth not available");

            //finish apk?
            this.finish();
            System.exit(0);
        }

        mBtOnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothOn();
            }
        });

        mBtOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothOff();
            }
        });

        mRefreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listPairedDevices();
            }
        });

        mGo2MagiclessBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToMagicless();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.settings_menu,menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.settings) {
            Intent intent = new Intent(MainActivity.this,SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.help){
            Uri uri = Uri.parse("https://bluedot.readthedocs.io");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
            Lazy function to create toasts instead of writing them out every time
             */
    private void sendToast(String msg) {
        Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_SHORT).show();
    }


    /*
    Bluetooth on/off functions
     */
    private void bluetoothOn() {
        if (!mBtAdapter.isEnabled()) {
            Intent turnBtOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBtOn,ENABLE_BT);
        } else {
            sendToast("Bluetooth already on");
        }
    }

    private void bluetoothOff() {
        mBtAdapter.disable();
        sendToast("Bluetooth disabled");
    }


    /*
    Go to magicless activity
     */
    private void goToMagicless() {
        Intent intent = new Intent(MainActivity.this,MagicNoBtActivity.class);
        startActivity(intent);
    }


    /*
    Port check function
    If the preference for port has changed, tell the user
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("port")) setConnectMsg(sharedPreferences);
    }

    /*
    Port check function
    If the task is resumed, tell user port
     */
    @Override
    protected void onResume() {
        super.onResume();
        setConnectMsg();
    }

    /*
    call bigger setConnectMsg() if shared preferences not given
    */
    private void setConnectMsg() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setConnectMsg(sharedPreferences);
    }

    /*
    tell user what port is in use if it's not the default
     */
    private void setConnectMsg(SharedPreferences sharedPreferences) {
        String msg = "Connect on port: ";
        Boolean default_port = sharedPreferences.getBoolean("default_port",true);
        String port_value = sharedPreferences.getString("port","1");

        if (!default_port) {
            msg = msg + port_value;
        } else {
            msg = msg + "1";
        }

        connectView.setText(msg);
    }


    /*
    display a list of paired bluetooth devices using an array adapter
     */
    private void listPairedDevices() {
        mBTArrayAdapter.clear();
        pairedDevices = mBtAdapter.getBondedDevices();

        //check bluetooth enabled before doing anything
        if (mBtAdapter.isEnabled()) {
            if (pairedDevices.size()>0) {
                for (BluetoothDevice device: pairedDevices) {
                    mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                sendToast("Showing paired devices");
            } else {
                sendToast("No paired devices found");
            }

        } else {
            sendToast("Bluetooth not on");
        }
    }


    /*
    on click of device in list, connect to device and go to magic activity
     */
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //get MAC address and device name
            String info = ((TextView) view).getText().toString();
            String deviceName = info.split("\n")[0];
            String deviceAddress = info.split("\n")[1];

            //go to magic activity
            Intent i = new Intent(MainActivity.this,MagicActivity.class);
            i.putExtra(EXTRA_NAME,deviceName);
            i.putExtra(EXTRA_ADDRESS,deviceAddress);
            startActivity(i);
        }
    };

    /*
    give user feedback from app when it tries to enable bluetooth
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                //user enabled bluetooth
                sendToast("Bluetooth Enabled");
            } else if (resultCode == RESULT_CANCELED) {
                //user cancelled
                sendToast("Bluetooth Cancelled");
            } else
                sendToast("Something went wrong :(");
        }
    }
}