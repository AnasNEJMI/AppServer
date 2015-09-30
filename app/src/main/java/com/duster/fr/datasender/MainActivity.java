package com.duster.fr.datasender;

import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class MainActivity extends ActionBarActivity implements SettingsFragment.Communicator {

    // for Debuging purposes
    private static final String TAG = "MainActivity";
    protected static final boolean DEBUG = true;

    // Message types sent from BlutoothService handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    //Key names received from BlutoothService handler
    public static final String TOAST = "toast";

    //for adding strings
    private StringBuilder stringBuilder = new StringBuilder();

    private String mConnectedDeviceName = null;
    private BluetoothService bluetoothService;


    //Layouts
    private  Button send;
    private  Button abort;
    private EditText sensorNumberView;
    private EditText frequencyView;
    private ProgressBar progressBar;
    private Spinner dataSpinner;
    private TextView typeOfDataView;
    private ToggleButton leftRightButton;

    //Name of the Insole
    private TextView insoleNameView;
    private int nameParam1=1;
    private int nameParam2=1;
    private int nameNumber=128;
    public static String insName="device_name";


    // Infos about the insole
    private String insoleSide="L";
    private int footSize = 43;

    //Battery level
    private TextView batteryLevelView;
    private TextView batteryView;
    private int batInt;

    // side of the foot
    private ImageView left_foot;
    private ImageView right_foot;

    //Receiver of batteryView info
    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            batInt=level;
            batteryView.setText(String.valueOf(level) + "%");
        }
    };

    //For determining the type of dataSpinner asked
    private int dataType;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if(DEBUG) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            /***********/
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            /***********/
                            break;
                        case BluetoothService.STATE_LISTEN:
                            /***********/
                            break;
                        case BluetoothService.STATE_NONE:
                            /**********/
                            break;
                    }
                    break;

                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    Log.i(TAG,(String)msg.obj);
                    mConnectedDeviceName = (String) msg.obj;
                    Toast.makeText(getApplicationContext(), "Connected to : "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(),(String) msg.obj,
                            Toast.LENGTH_SHORT).show();
                    break;

            }
        }
    };





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_design);
        //ActionBar actionBar = getActionBar();
        //actionBar.setBackgroundDrawable(new ColorDrawable(Color.RED)); // set your desired color

        bluetoothService = new BluetoothService(insoleSide,mHandler,this);
        insName = new String(generateNameBis(nameNumber, insoleSide, footSize));
        bluetoothService.setBluetoothServiceName(insName);
        if(DEBUG) Log.d(TAG,"This is "+bluetoothService.getBluetoothServiceName());

        //side of the foot
        left_foot = (ImageView) findViewById(R.id.left_foot);
        right_foot = (ImageView) findViewById(R.id.right_foot);

        //EditTexts
        sensorNumberView = (EditText) findViewById(R.id.editSensorNumber);
        frequencyView = (EditText) findViewById(R.id.editFrequency);
        send = (Button) findViewById(R.id.sendBtn);

        // for new_design
        send.setBackgroundResource(R.drawable.start_selector);

        // Battery layouts
        batteryLevelView = (TextView) findViewById(R.id.batteryLevel);
        batteryView = (TextView) findViewById(R.id.battery);
        Typeface t = Typeface.createFromAsset(getAssets(),"fonts/Langdon.otf");
        batteryLevelView.setTypeface(t);
        batteryView.setTypeface(t);

        //Gettin bettery info
            this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent
                    .ACTION_BATTERY_CHANGED));

        //Name of the insole
        insoleNameView = (TextView) findViewById(R.id.insoleName);
        insoleNameView.setText(generateNameBis(nameNumber, insoleSide, footSize));



        Toast.makeText(getApplicationContext(),"Welcome to the insole simulator"
                ,Toast.LENGTH_SHORT).
                show();


        // Setting up the police of "Type of dataSpinner"
        typeOfDataView = (TextView) findViewById(R.id.type_of_data);
        Typeface typeface = Typeface.createFromAsset(getAssets(),"fonts/Ailerons.ttf");
        typeOfDataView.setTypeface(typeface);

        //Setting up the circular progress bar
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        //Setting up the switch that determines which side of the insole
        leftRightButton = (ToggleButton) findViewById(R.id.leftRight);

        leftRightButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (bluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
                    Toast.makeText(getApplicationContext(), "You can't change " +
                            "the side of the insole while you're paired to a client app"
                            , Toast.LENGTH_SHORT)
                            .show();
                    leftRightButton.setChecked(false);
                    return;}
                else if (isChecked) {
                    insoleSide = new String("R");
                    insName = new String(generateNameBis(nameNumber, insoleSide, footSize));
                    bluetoothService.setBluetoothServiceName(insName);
                    insoleNameView.setText(insName);

                    right_foot.setBackgroundResource(R.drawable.right_insolefoot);
                    left_foot.setBackgroundResource(R.drawable.left_insolefoot_uncliqued);

                    Toast.makeText(getApplicationContext(), "You're now a right side insole", Toast
                            .LENGTH_SHORT).show();
                }

                else {
                    insoleSide = new String("L");
                    insName = new String(generateNameBis(nameNumber, insoleSide, footSize));
                    bluetoothService.setBluetoothServiceName(insName);
                    insoleNameView.setText(insName);

                    right_foot.setBackgroundResource(R.drawable.right_insolefoot_uncliqued);
                    left_foot.setBackgroundResource(R.drawable.left_insolefoot);

                    Toast.makeText(getApplicationContext(), "You're now a left side insole", Toast.
                            LENGTH_SHORT).show();

                }
            }
        });





        //Setting up the spinner
        dataSpinner = (Spinner) findViewById(R.id.data);
        ArrayAdapter<CharSequence> dataAdapter = ArrayAdapter.createFromResource(this,R.array
                .data_type,android.R.layout.simple_spinner_item);

        dataAdapter.setDropDownViewResource(R.layout.spinner_textview);
        dataSpinner.setAdapter(dataAdapter);

        //Setting up the OnItemClickListener for the spinner

        dataSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String itemAtPosition = new String(parent.getItemAtPosition(position).toString());

                if (itemAtPosition.equals("Type 1")) {
                    if (DEBUG) Log.i(TAG, "updating to type 1");
                    dataType = 1;
                    if (DEBUG) Log.i(TAG, "Update successful");
                } else if (itemAtPosition.equals("Type 2")) {
                    if (DEBUG) Log.i(TAG, "updating to type 2");
                    dataType = 2;
                    if (DEBUG) Log.i(TAG, "Update successful");
                } else if (itemAtPosition.equals("Type 3")) {
                    if (DEBUG) Log.i(TAG, "updating to type 3");
                    dataType = 3;
                    if (DEBUG) Log.i(TAG, "Update successful");

                } else if (itemAtPosition.equals("Type 4")) {
                    if (DEBUG) Log.i(TAG, "updating to type 4");
                    dataType = 4;
                    if (DEBUG) Log.i(TAG, "Update successful");

                }


            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int messageSInt = Integer.parseInt(sensorNumberView.getText().toString());
                int messageFInt = Integer.parseInt(frequencyView.getText().toString());
                bluetoothService.setSensorNb(messageSInt);
                bluetoothService.setFrequency(messageFInt);
                bluetoothService.setType(dataType);
                bluetoothService.sendData();
            }
        });

        abort = (Button) findViewById(R.id.abortBtn);

        //for new design
        abort.setBackgroundResource(R.drawable.stop_selector);
        //abort.setBackgroundResource(R.drawable.abort_selector);

        abort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothService.stopData();
            }
        });

    }



    public String generateName(int p1,int p2, String inSide,int footS){

        nameNumber= p1 + (256*p2);
        stringBuilder.setLength(0);
        return stringBuilder.append("FeetMe ").append(String.valueOf(nameNumber))
                .append(inSide).append("-").append(footS).toString();
    }

    public String generateNameBis(int p1, String inSide,int footS){
        stringBuilder.setLength(0);
        return stringBuilder.append("FeetMe ").append(String.valueOf(p1))
                .append(inSide).append("-").append(footS).toString();
    }

    public void showDialog(){
        FragmentManager manager = getFragmentManager();
        SettingsFragment settings = new SettingsFragment();
        settings.show(manager, "SettingsFragment");
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (item.getItemId()){
            case  R.id.action_settings:
                showDialog();
                return true;

            case  R.id.action_make_discoverable:
                String messageS = sensorNumberView.getText().toString();
                String messageF = frequencyView.getText().toString();
                if(messageF.isEmpty()||messageS.isEmpty()){
                    Toast.makeText(getApplicationContext(),"You can't activate BT before " +
                            "entering all the values", Toast.LENGTH_SHORT).show();
                }
                else{
                    bluetoothService.makeDiscoverable(this);
                    bluetoothService.accept();

                    //disabling data enteries
                    sensorNumberView.setEnabled(false);
                    frequencyView.setEnabled(false);
                    dataSpinner.getSelectedView().setEnabled(false);
                    dataSpinner.setEnabled(false);

                    //Setting the frequency and the number of sensors
                    bluetoothService.setSensorNb(Integer.parseInt(messageS));
                    bluetoothService.setFrequency(Integer.parseInt(messageF));
                    bluetoothService.setFootSize(footSize);
                    bluetoothService.setSide(insoleSide);
                    bluetoothService.setBatteryLevel(batInt);
                    bluetoothService.setType(dataType);

                    if(DEBUG){
                    Log.d(TAG,String.valueOf(Integer.parseInt(messageS)));
                    Log.d(TAG,String.valueOf(Integer.parseInt(messageF)));
                    Log.d(TAG,String.valueOf(footSize));
                    Log.d(TAG,insoleSide);
                    Log.d(TAG,String.valueOf(batInt));}

                    return true;
                }
            case R.id.action_disconnect:


                bluetoothService.disconnect();
                EditorsEnabled(true);

            case R.id.action_restart_app:
                Intent i = getBaseContext().getPackageManager()
                        .getLaunchIntentForPackage( getBaseContext().getPackageName() );
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
        }

        return super.onOptionsItemSelected(item);
    }


    // Methods of communication with the SettingsFragment

    /*--- For receiving the new name ---*/
    @Override
    public void nameMessage(int nbr, String s, int foot) {

        if (bluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this,"You can't change the name of the insole simulator" +
                    " while you're paired with a client app", Toast.LENGTH_SHORT)
                    .show();
        } else {
            footSize=foot;
            nameNumber=nbr;
            insoleSide=s;
            stringBuilder.setLength(0);
            insName= generateNameBis(nameNumber,insoleSide,footSize);
            bluetoothService.setBluetoothServiceName(insName);
            if(DEBUG)Log.d(TAG,"This that :"+bluetoothService.getBluetoothServiceName());


            if(s.equals("R"))
            {
                if(DEBUG) Log.i(TAG, "right");
                leftRightButton.setChecked(true);
            }else if(s.equals("L"))
            {
                if(DEBUG) Log.i(TAG, "left");
                leftRightButton.setChecked(false);
            }
            //DEVICE_NAME=n;
            insoleNameView.setText(insName);
            Toast.makeText(getApplicationContext(),"The name of the insole has " +
                    "changed to "+insName,Toast.LENGTH_SHORT)
                    .show();
        }

    }

    /*--- For receiving the new version ---*/
    @Override
    public void versionMessage(String message) {

    }

    /*--- For receiving the new footsize ---*/
    @Override
    public void footsizeMessage(int intFoot) {

        if (bluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this,"You can't change the name of the foot size " +
                    "while you're paired with a client app", Toast.LENGTH_SHORT)
                    .show();
        }else{
            footSize = intFoot;
            insName= generateNameBis(nameNumber,insoleSide,footSize);
            bluetoothService.setBluetoothServiceName(insName);
            //DEVICE_NAME=name;
            insoleNameView.setText(insName);
            Toast.makeText(getApplicationContext(),"footsize of the insole has " +
                    "changed to "+footSize,Toast.LENGTH_SHORT)
                    .show();
        }


    }

    // Setters

    public void setName(String s){
        insName=s;
        insoleNameView.setText(insName);
    }

    public void EditorsEnabled(boolean b){
        if(b==true){
            sensorNumberView.setEnabled(true);
            frequencyView.setEnabled(true);
            progressBar.setVisibility(View.GONE);
            dataSpinner.getSelectedView().setEnabled(true);
            dataSpinner.setEnabled(true);
        } else{
            sensorNumberView.setEnabled(false);
            frequencyView.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            dataSpinner.getSelectedView().setEnabled(false);
            dataSpinner.setEnabled(false);

        }
    }



}