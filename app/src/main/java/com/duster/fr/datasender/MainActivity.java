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
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_DISC = 6;
    public static final int MESSAGE_TOAST = 5;

    //Key names received from BlutoothService handler
    //public static  String DEVICE_NAME = new String(insName);
    public static final String TOAST = "toast";

    //String buffer for outgoing messages
    private StringBuffer mOutStringBuffer = new StringBuffer();


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
    private byte[] version = new byte[]{1,0};
    //private String version= new String("0.1.6");


    //Timestamp and dataSpinner prefix
    private byte numData;
    private byte[] timeStamp = new byte[4];

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


    // Boolean to not allow abort button to do more than aborting
    private boolean isSendingData =false;


    //For determining the type of dataSpinner asked
    private int dataType;


    // Separate Thread for sending dataSpinner
    private Thread loop;


    // DataProvider
    private DataProvider dataProvider;


    //Number of dataSpinner package sent
    private  int d =0;

    //
    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private byte[] rBytes;

    private final Handler mHandler = new Handler() {



        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if(DEBUG) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:

                            /*----------------------------------------------------------*/
                            /* --- In case connection is established, send a message ---*/
                            /*----------------------------------------------------------*/

                            // Use StringBuilder for adding strings //

                            numData = (byte) 14;
                            byte[] responseBytes;
                            responseBytes = "madm".getBytes();

                            // Concatenation of the three arrays and sending dataSpinner
                            outputStream.reset();
                            try {
                                outputStream.write(numData);
                                outputStream.write(responseBytes);
                            } catch (IOException e) {
                                Log.w(TAG, "write failure");
                                break;
                            }
                            bluetoothService.write(outputStream.toByteArray());


                            //mConnectedDeviceName = msg.getData().getString(insName);
                            //Toast.makeText(getApplicationContext(), "Connected to "
                            //        + mConnectedDeviceName, Toast.LENGTH_SHORT).show();

                            break;
                        case BluetoothService.STATE_CONNECTING:
                            /***********/
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            /**********/
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);

                    if(bluetoothService.getState() != BluetoothService.STATE_CONNECTED){
                        numData = (byte) 14;
                        rBytes = "madm".getBytes();

                        try {
                            outputStream.write(numData);
                            outputStream.write(rBytes);
                        } catch (IOException e) {
                            Log.w(TAG, "write failure");
                            break;
                        }
                        bluetoothService.write(outputStream.toByteArray());

                    }
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);

                    //Deciding what to do with dataSpinner

                    /*-----------------------*/
                    /* --- If it's a ping ---*/
                    /*-----------------------*/

                    if(readMessage.equals("ping")) {
                        // Setting up the three byte arrays for the response
                        numData = (byte) 1;

                        rBytes = "pong".getBytes();

                        // Concatenation of the three arrays and sending dataSpinner
                        outputStream.reset();
                        try {
                            outputStream.write(numData);
                            outputStream.write(timeStamp);
                            outputStream.write(rBytes);
                        } catch (IOException e) {
                            Log.w(TAG, "write failure");
                            break;
                        }
                        bluetoothService.write(outputStream.toByteArray());

                        Toast.makeText(getApplicationContext()," The client app is requesting a "+readMessage,Toast.LENGTH_SHORT).show();
                    }

                    /*-----------------------------------------*/
                    /* --- If dataSpinner requested is the version ---*/
                    /*-----------------------------------------*/

                    else if(readMessage.equals("version")) {

                        numData = (byte) 3;
                        rBytes = version;

                        // Concatenation of the three arrays
                        outputStream.reset();
                        try {
                            outputStream.write(numData);
                            outputStream.write(timeStamp);
                            outputStream.write(rBytes);
                        } catch (IOException e) {
                            Log.w(TAG, "write failure");
                            break;
                        }
                        bluetoothService.write(outputStream.toByteArray());

                        Toast.makeText(getApplicationContext(),
                                String.valueOf(outputStream.toByteArray().length),
                                Toast.LENGTH_LONG)
                                .show();
                        Toast.makeText(getApplicationContext(),"the client app is requesting the "+readMessage+" of the insole",Toast.LENGTH_SHORT).show();
                    }

                    /*----------------------------------------------------*/
                    /* --- If dataSpinner requested is the side of the insole ---*/
                    /*----------------------------------------------------*/

                    else if(readMessage.equals("insole_side")) {
                        numData = (byte) 4;
                        rBytes = insoleSide.getBytes();

                        // Concatenation of the three arrays and sending response
                        outputStream.reset();
                        try {
                            outputStream.write(numData);
                            outputStream.write(timeStamp);
                            outputStream.write(rBytes);
                        } catch (IOException e) {
                            Log.w(TAG, "write failure");
                            break;
                        }
                        bluetoothService.write(outputStream.toByteArray());
                        Toast.makeText(getApplicationContext(), "the client app is requesting which " + readMessage+" is the insole",Toast.LENGTH_SHORT).show();
                    }

                    /*------------------------------------------*/
                    /* --- If dataSpinner requested is the footsize ---*/
                    /*------------------------------------------*/

                    else if(readMessage.equals("footsize")) {
                        numData = (byte) 8;
                        rBytes = new byte[1];
                        rBytes[0]= (byte) footSize;

                        // Concatenation of the three arrays
                        outputStream.reset();
                        try {
                            outputStream.write(numData);
                            outputStream.write(timeStamp);
                            outputStream.write(rBytes);
                        } catch (IOException e) {
                            Log.w(TAG, "write failure");
                            break;
                        }
                        bluetoothService.write(outputStream.toByteArray());

                        Toast.makeText(getApplicationContext(),"the client app is requesting the "+readMessage,Toast.LENGTH_SHORT).show();

                    }

                    /*--------------------------------------------------*/
                    /* --- If dataSpinner requested is the number of sensor ---*/
                    /*--------------------------------------------------*/

                    else if(readMessage.equals("sensors_number")) {


                        String sensor = sensorNumberView.getText().toString();

                        if(sensor.isEmpty() || sensor.equals("")){
                            Log.i(TAG,"sensor is empty");
                            Toast.makeText(getApplicationContext(),"The client app is requesting the number of sensors which is not yet specified",Toast.LENGTH_SHORT).show();
                        }
                        else{
                            numData = (byte) 2;
                            rBytes = new byte[1];
                            rBytes[0]= (byte) Integer.parseInt(sensor);

                            outputStream.reset();
                            try {
                                outputStream.write(numData);
                                outputStream.write(timeStamp);
                                outputStream.write(rBytes);
                            } catch (IOException e) {
                                Log.w(TAG, "write failure");
                                break;
                            }
                            bluetoothService.write(outputStream.toByteArray());

                            Toast.makeText(getApplicationContext(),"the client app is requesting the number of sensors of the insole",Toast.LENGTH_SHORT).show();

                        }

                    }


                    /*------------------------------------------------------*/
                    /* --- If the request is to change the insole's name--- */
                    /*------------------------------------------------------*/

                    else if (readMessage.length()==10) {
                        String t = "new_name";
                        if(readMessage.toLowerCase().contains(t.toLowerCase())){

                            if(readMessage.charAt(8) == (int) readMessage.charAt(8) && readMessage.charAt(9) == (int) readMessage.charAt(9)){
                                nameParam1 =  Integer.parseInt(String.valueOf(readMessage.charAt(8)));
                                nameParam2 = Integer.parseInt(String.valueOf(readMessage.charAt(9)));
                                insName = generateName(nameParam1, nameParam2, insoleSide, footSize);
                                bluetoothService.setBluetoothServiceName(insName);
                                //DEVICE_NAME=insName;
                                insoleNameView.setText(insName);


                                numData = (byte) 12;
                                try {
                                    outputStream.write(numData);
                                    outputStream.write(timeStamp);
                                    outputStream.write(rBytes);
                                } catch (IOException e) {
                                    Log.w(TAG, "write failure");
                                    break;
                                }
                                bluetoothService.write(outputStream.toByteArray());



                                Toast.makeText(getApplicationContext(),"The name of the insole has been changed to "+insName,Toast.LENGTH_SHORT).show();}
                        }else{
                            Toast.makeText(getApplicationContext(),"The client app sent an  unrecognized request",Toast.LENGTH_SHORT).show();
                        }


                    /*---------------------------------------------------------------*/
                    /* --- If it is a request to the insole to start sending dataSpinner--- */
                    /*---------------------------------------------------------------*/

                    }else if(readMessage.equals("start_sending")){

                        numData = (byte) 10;
                        rBytes = "start".getBytes();


                        // Concatenation of the three arrays and sending response
                        outputStream.reset();
                        try {
                            outputStream.write(numData);
                            outputStream.write(timeStamp);
                            outputStream.write(rBytes);
                        } catch (IOException e) {
                            Log.w(TAG, "write failure");
                            break;
                        }
                        bluetoothService.write(outputStream.toByteArray());

                        Toast.makeText(getApplicationContext()," The client app is requesting to "+readMessage+ " sending dataSpinner",Toast.LENGTH_SHORT).show();

                        //Sending the real dataSpinner
                        String messageS = sensorNumberView.getText().toString();
                        String messageF = frequencyView.getText().toString();
                        sendData(messageS, messageF);

                    }

                    /*---------------------------------------------------------------*/
                    /* --- If it is a request to the insole to stop sending dataSpinner--- */
                    /*---------------------------------------------------------------*/

                    else if(readMessage.equals("stop_sending")){

                        numData = (byte) 11;
                        rBytes = "stop".getBytes();

                        // Concatenation of the three arrays and sending response
                        outputStream.reset();try {
                            outputStream.write(numData);
                            outputStream.write(timeStamp);
                            outputStream.write(rBytes);
                        } catch (IOException e) {
                            Log.w(TAG, "write failure");
                            break;
                        }
                        bluetoothService.write(outputStream.toByteArray());


                        Toast.makeText(getApplicationContext()," The client app is requesting to "+readMessage+ " sending dataSpinner",Toast.LENGTH_SHORT).show();

                        //Sending the real dataSpinner
                        stopData();

                    }

                    /*----------------------------------------------------------------------*/
                    /* --- If it is a request is to make the insole enter in a deep sleep-- */
                    /*----------------------------------------------------------------------*/

                    else if(readMessage.equals("stop")){

                        if(DEBUG) Log.i(TAG,"attempting to sleep");

                        try {
                            Thread.currentThread().sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (DEBUG) Log.i(TAG,"Sleep succeeded");

                        Toast.makeText(getApplicationContext(),"The insole slept for 10 seconds",Toast.LENGTH_LONG).show();
                    }

                    /*----------------------------------------------------------------------*/
                    /* --- If it is a request is to inspect the batteryView-- */
                    /*----------------------------------------------------------------------*/

                    else if(readMessage.equals("battery_info")){

                        if(DEBUG) Log.i(TAG,"batteryView");
                        numData = (byte) 6;
                        rBytes = new byte[40];
                        rBytes[4] = (byte)((batInt*10)%256);
                        rBytes[5] = (byte)(batInt*10/256);

                        outputStream.reset();

                        try {
                            outputStream.write(numData);
                            outputStream.write(timeStamp);
                            outputStream.write(rBytes);
                        } catch (IOException e) {
                            Log.w(TAG, "write failure");
                            break;
                        }
                        bluetoothService.write(outputStream.toByteArray());





                    }

                   /*----------------------------------------------------------------------*/
                    /* --- If it is a request is to inspect the batteryView-- */
                    /*----------------------------------------------------------------------*/

                    else if(readMessage.equals("reset_timestamp")){

                        if(DEBUG) Log.i(TAG,"timestamp");
                        numData = (byte) 5;

                        outputStream.reset();
                        try {
                            outputStream.write(numData);
                            outputStream.write(timeStamp);
                        } catch (IOException e) {
                            Log.w(TAG, "write failure");
                            break;
                        }
                        bluetoothService.write(outputStream.toByteArray());
                        Toast.makeText(getApplicationContext(), "reset timestamp", Toast.LENGTH_SHORT).show();



                    }

                   /*------------------------------------------------------*/
                    /* --- If the request is to change the insole's name--- */
                    /*------------------------------------------------------*/

                    else if (readMessage.toLowerCase().contains("#")) {

                        //Toast.makeText(getApplicationContext(),"ignore",Toast.LENGTH_SHORT).show();
                    }

                    /*------------------------------*/
                    /* --- If none of the above --- */
                    /*------------------------------*/

                    else{
                        Toast.makeText(getApplicationContext(),"The client app rest an  unrecognized request",Toast.LENGTH_SHORT).show();
                    }

                    if(DEBUG)Log.d(TAG,"Message read");
                    Toast.makeText(getApplicationContext(),"message Read",Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    Log.i(TAG,insName);
                    mConnectedDeviceName = (String) msg.obj;
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_DISC:
                    dataProvider.abortSend();
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

        bluetoothService = new BluetoothService(mHandler,this);
        insName = new String(generateNameBis(nameNumber, insoleSide, footSize));
        bluetoothService.setBluetoothServiceName(insName);

        //side of the foot
        left_foot = (ImageView) findViewById(R.id.left_foot);
        right_foot = (ImageView) findViewById(R.id.right_foot);

        //EditTexts
        sensorNumberView = (EditText) findViewById(R.id.editSensorNumber);
        frequencyView = (EditText) findViewById(R.id.editFrequency);
        send = (Button) findViewById(R.id.sendBtn);

        // for new_design
        send.setBackgroundResource(R.drawable.start_selector);

        //set progressBar style
        // Get the Drawable custom_progressbar
        //Drawable draw= getResources().getDrawable(R.drawable.custom_progressbar);
        // set the drawable as progress drawable
        //progressBar.setProgressDrawable(ContextCompat.getDrawable(getApplicationContext(),R.drawable.custom_progressbar));

        //send.setBackgroundResource(R.drawable.send_selector);

        // Battery layouts
        batteryLevelView = (TextView) findViewById(R.id.batteryLevel);
        batteryView = (TextView) findViewById(R.id.battery);
        Typeface t = Typeface.createFromAsset(getAssets(),"fonts/Langdon.otf");
        batteryLevelView.setTypeface(t);
        batteryView.setTypeface(t);

        //Gettin bettery info
            this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));




        //Name of the insole
        insoleNameView = (TextView) findViewById(R.id.insoleName);
        insoleNameView.setText(generateNameBis(nameNumber, insoleSide, footSize));


        //Setting up the timestamp array
        String date = new SimpleDateFormat("dd,MM,yyyy,HH,mm,ss").format(new java.util.Date());

        String[] dateParts = date.split(",");

        byte t0 = (byte) Integer.parseInt(dateParts[0]);
        byte t1 = (byte) Integer.parseInt(dateParts[1]);
        byte t2 = (byte) Integer.parseInt(dateParts[2].substring(0,2));
        byte t3 = (byte) Integer.parseInt(dateParts[2].substring(2,4));
        timeStamp[0] = (byte) t0;
        timeStamp[1] = (byte) t1;
        timeStamp[2] = (byte) t2;
        timeStamp[3] = (byte) t3;

        Toast.makeText(getApplicationContext(),"Welcome to the insole simulator",Toast.LENGTH_SHORT).show();


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

                if (isChecked) {
                    if (bluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
                        Toast.makeText(getApplicationContext(), "You can't change the side of the insole while you're paired to a client app", Toast.LENGTH_SHORT).show();
                        leftRightButton.setChecked(false);
                    } else {
                        insoleSide = new String("R");
                        right_foot.setBackgroundResource(R.drawable.right_insolefoot);
                        left_foot.setBackgroundResource(R.drawable.left_insolefoot_uncliqued);
                        //insoleNameView.setTextColor(Color.parseColor("#fe6b83"));
                        insName = new String(generateNameBis(nameNumber, insoleSide, footSize));
                        bluetoothService.setBluetoothServiceName(insName);
                        //DEVICE_NAME=insName;
                        insoleNameView.setText(insName);

                        Toast.makeText(getApplicationContext(), "You're now a right side insole", Toast.LENGTH_SHORT).show();
                    }

                } else {

                    if (bluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
                        Toast.makeText(getApplicationContext(), "You can't change the side of the insole while you're paired to a client app", Toast.LENGTH_SHORT).show();
                        leftRightButton.setChecked(true);
                    } else {
                        insoleSide = new String("L");
                        right_foot.setBackgroundResource(R.drawable.right_insolefoot_uncliqued);
                        left_foot.setBackgroundResource(R.drawable.left_insolefoot);
                        //insoleNameView.setTextColor(Color.parseColor("#2fa8e0"));
                        insName = new String(generateNameBis(nameNumber, insoleSide, footSize));
                        bluetoothService.setBluetoothServiceName(insName);
                        //DEVICE_NAME=insName;
                        insoleNameView.setText(insName);
                        Toast.makeText(getApplicationContext(), "You're now a left side insole", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });





        //Setting up the spinner
        dataSpinner = (Spinner) findViewById(R.id.data);
        ArrayAdapter<CharSequence> dataAdapter = ArrayAdapter.createFromResource(this,R.array.data_type,android.R.layout.simple_spinner_item);
        dataAdapter.setDropDownViewResource(R.layout.spinner_textview);
        dataSpinner.setAdapter(dataAdapter);

        //Setting up the OnItemClickListener for the spinner

        dataSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String itemAtPosition = new String(parent.getItemAtPosition(position).toString());

                if (itemAtPosition.equals("Type 1")) {
                    //Toast.makeText(getApplicationContext(),"Type of dataSpinner chosen : 1",Toast.LENGTH_SHORT).show();
                    if (DEBUG) Log.i(TAG, "updating to type 1");
                    dataType = 1;
                    if (DEBUG) Log.i(TAG, "Update successful");
                } else if (itemAtPosition.equals("Type 2")) {
                    //Toast.makeText(getApplicationContext(),"Type of dataSpinner chosen : 2",Toast.LENGTH_SHORT).show();
                    if (DEBUG) Log.i(TAG, "updating to type 2");
                    dataType = 2;
                    if (DEBUG) Log.i(TAG, "Update successful");
                } else if (itemAtPosition.equals("Type 3")) {
                    //Toast.makeText(getApplicationContext(),"Type of dataSpinner chosen : 3",Toast.LENGTH_SHORT).show();
                    if (DEBUG) Log.i(TAG, "updating to type 3");
                    dataType = 3;
                    if (DEBUG) Log.i(TAG, "Update successful");

                } else if (itemAtPosition.equals("Type 4")) {
                    //Toast.makeText(getApplicationContext(),"Type of dataSpinner chosen : 3",Toast.LENGTH_SHORT).show();
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

                String messageS = sensorNumberView.getText().toString();
                String messageF = frequencyView.getText().toString();

                sendData(messageS,messageF);
            }
        });

        abort = (Button) findViewById(R.id.abortBtn);

        //for new design
        abort.setBackgroundResource(R.drawable.stop_selector);
        //abort.setBackgroundResource(R.drawable.abort_selector);

        abort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopData();
            }
        });

    }


    //Method for sending dataSpinner

    public void sendData(String message1, String message2){

        if(bluetoothService.getState() != BluetoothService.STATE_CONNECTED){
            Toast.makeText(getApplicationContext(),
                    "The client app needs to be connected first",
                    Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        if (message1.isEmpty() || message2.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Make sure you enter both the number of sensors and the frequencyView ", Toast.LENGTH_SHORT).show();
            return;
        }

        sensorNumberView.setFocusable(false);
        //sensorNumberView.setEnabled(false);

        frequencyView.setFocusable(false);
        //frequencyView.setEnabled(false);
        isSendingData = true;
        int sensorNbr = Integer.parseInt(message1);
        int frq = Integer.parseInt(message2);

        if(DEBUG) Log.i(TAG,"before updating dataProvider");
        dataProvider = new DataProvider(sensorNbr, frq, dataType);
        String s = String.valueOf(dataType);
        if (DEBUG) Log.i(TAG,"Update dataType to "+s);


        loop = new Thread(){
            @Override
            public void run() {
                while (dataProvider.getSend()) {
                    int f = dataProvider.getFrequency();
                    outputStream.reset();
                    try{
                        outputStream.write(dataProvider.getData());
                    }catch (IOException e){
                        Log.w(TAG, "failed to write dataSpinner");
                        continue;
                    }
                    /************************************/
                    //String message = Arrays.toString(dataProvider.getData());
                    String message = new String(outputStream.toByteArray());
                    //Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
                    //d++;
                    sendMessage(message);
                    bluetoothService.sleep(1000 / f);
                }
            }
        };
        loop.start();
        progressBar.setVisibility(View.VISIBLE);
        // clear EditTexts
        sensorNumberView.setText("");
        frequencyView.setText("");
    }



    //Method for stopping dataSpinner
    public void stopData(){

        if(isSendingData) {
            dataProvider.abortSend();

            sensorNumberView.setFocusableInTouchMode(true);
            //sensorNumberView.setEnabled(true);

            frequencyView.setFocusableInTouchMode(true);
            //frequencyView.setEnabled(true);
            d=0;
            if(DEBUG){Log.d(TAG,"Trying to interrupt the loop");}
            if(DEBUG){Log.d(TAG,"Trying Interruption successful");}

            progressBar.setVisibility(View.GONE);

            isSendingData =false;
        }else{
            Toast.makeText(getApplicationContext(),
                    "There is no stream of dataSpinner to be stopped",
                    Toast.LENGTH_SHORT).show();
        }

    }


    public void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (bluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            bluetoothService.write(send);
            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //sensorNumberView.setText(mOutStringBuffer);
        }
    }

    public String generateName(int p1,int p2, String inSide,int footS){

        nameNumber= p1 + (256*p2);
        stringBuilder.setLength(0);
        return stringBuilder.append("FeetMe ").append(String.valueOf(nameNumber)).append(inSide).append("-").append(footS).toString();
    }

    public String generateNameBis(int p1, String inSide,int footS){
        nameNumber = p1;
        stringBuilder.setLength(0);
        return stringBuilder.append("FeetMe ").append(String.valueOf(nameNumber)).append(inSide).append("-").append(footS).toString();
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
                bluetoothService.makeDiscoverable(this);
                bluetoothService.accept();
                return true;

        }

        return super.onOptionsItemSelected(item);
    }


    // Methods of communication with the SettingsFragment

    /*--- For receiving the new name ---*/
    @Override
    public void nameMessage(int nbr, String s, int foot) {

        if (bluetoothService.getState() == BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this,"You can't change the name of the insole simulator while you're paired with a client app", Toast.LENGTH_SHORT).show();
        }
        else{
            nameNumber=nbr;
            footSize=foot;
            stringBuilder.setLength(0);
            insName= stringBuilder.append("FeetMe ").append(String.valueOf(nameNumber)).append(s).append("-").append(footSize).toString();
            bluetoothService.setBluetoothServiceName(insName);


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
            Toast.makeText(getApplicationContext(),"The name of the insole has changed to "+insName,Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this,"You can't change the name of the foot size while you're paired with a client app", Toast.LENGTH_SHORT).show();
        }else{
            footSize = intFoot;
            stringBuilder.setLength(0);
            insName= stringBuilder.append("FeetMe ").append(String.valueOf(nameNumber)).append(insoleSide).append("-").append(footSize).toString();
            bluetoothService.setBluetoothServiceName(insName);
            //DEVICE_NAME=name;
            insoleNameView.setText(insName);
            Toast.makeText(getApplicationContext(),"footsize of the insole has changed to "+footSize,Toast.LENGTH_SHORT).show();
        }


    }
}