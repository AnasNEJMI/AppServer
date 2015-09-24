package com.duster.fr.datasender;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Message;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import android.os.Handler;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.security.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ThreadPoolExecutor;

public class MainActivity extends ActionBarActivity implements SettingsFragment.Communicator {

    // for Debuging purposes
    private static final String TAG = "MainActivity";
    private static final boolean DEBUG = true;

    // Message types sent from BlutoothService handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    //Key names received from BlutoothService handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    //String buffer for outgoing messages
    private StringBuffer mOutStringBuffer = new StringBuffer();

    private String mConnectedDeviceName = null;
    private BluetoothService bluetoothService;


    //Layouts
    private  Button send;
    private  Button abort;
    private EditText sensorNumber;
    private EditText frequency;
    private ProgressBar progressBar;
    private Spinner data;
    private TextView typeOfData;
    private ToggleButton leftRight;

    //Name of the Insole
    private TextView NAME;
    private TextView insoleName;
    private int nameParam1=1;
    private int nameParam2=1;
    private int nameNumber=128;


    // Infos about the insole
    private String insoleSide="L";
    private int footSize = 42;
    private String version= new String("0.1.6");


    //Timestamp and data prefix
    private byte[] numData = new byte[1];
    private byte[] timeStamp = new byte[4];


    //For the outputStream and concatenation of output data
    private OutPutStream outPutStream = new OutPutStream();


    // Boolean to not allow abort button to do more than aborting
    private boolean logic=false;


    //For determining the type of data asked
    private int dataType;


    // Separate Thread for sending data
    private Thread loop;


    // DataProvider
    private DataProvider dataProvider;


    //Number of data package sent
    private  int d =0;








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

                            numData[0]= (byte) 14;
                            String response = new String("madm");
                            byte[] responseBytes = response.getBytes();

                            // Concatenation of the three arrays and sending data
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            byte[] concatByte = outPutStream.concatenateTwoBytes(outputStream,numData,responseBytes);
                            bluetoothService.write(concatByte);

                            mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                            Toast.makeText(getApplicationContext(), "Connected to "
                                    + mConnectedDeviceName, Toast.LENGTH_SHORT).show();

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
                        numData[0]= (byte) 14;
                        String response = new String("madm");
                        byte[] responseBytes = response.getBytes();

                        // Concatenation of the three arrays and sending data
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        byte[] concatByte = outPutStream.concatenateTwoBytes(outputStream, numData, responseBytes);
                        bluetoothService.write(concatByte);
                    }
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);

                    //Deciding what to do with data

                    /*-----------------------*/
                    /* --- If it's a ping ---*/
                    /*-----------------------*/

                    if(readMessage.equals("ping")) {
                        // Setting up the three byte arrays for the response
                        numData[0]= (byte) 1;
                        String response = new String("pong");
                        byte[] responseBytes = response.getBytes();

                        // Concatenation of the three arrays and sending data
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        byte[] concatByte = outPutStream.concatenateData(outputStream,numData,timeStamp,responseBytes);
                        bluetoothService.write(concatByte);
                        Toast.makeText(getApplicationContext()," The client app is requesting a "+readMessage,Toast.LENGTH_SHORT).show();
                    }

                    /*-----------------------------------------*/
                    /* --- If data requested is the version ---*/
                    /*-----------------------------------------*/

                    else if(readMessage.equals("version")) {

                        numData[0]= (byte) 3;
                        String p = new String(version);
                        byte[] responseBytes = p.getBytes();

                        // Concatenation of the three arrays
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        byte[] concatByte = outPutStream.concatenateData(outputStream, numData, timeStamp, responseBytes);
                        bluetoothService.write(concatByte);
                        Toast.makeText(getApplicationContext(),"the client app is requesting the "+readMessage+" of the insole",Toast.LENGTH_SHORT).show();
                    }

                    /*----------------------------------------------------*/
                    /* --- If data requested is the side of the insole ---*/
                    /*----------------------------------------------------*/

                    else if(readMessage.equals("side")) {
                        numData[0]= (byte) 4;
                        String p = new String(insoleSide);
                        byte[] responseBytes = p.getBytes();

                        // Concatenation of the three arrays and sending response

                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        byte[] concatByte = outPutStream.concatenateData(outputStream, numData, timeStamp, responseBytes);
                        bluetoothService.write(concatByte);

                        Toast.makeText(getApplicationContext(), "the client app is requesting which " + readMessage+" is the insole",Toast.LENGTH_SHORT).show();
                    }

                    /*------------------------------------------*/
                    /* --- If data requested is the footsize ---*/
                    /*------------------------------------------*/

                    else if(readMessage.equals("footsize")) {
                        numData[0]= (byte) 8;
                        byte[] responseBytes = new byte[1];
                        responseBytes[0]= (byte) footSize;

                        // Concatenation of the three arrays

                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        byte[] concatByte = outPutStream.concatenateData(outputStream, numData, timeStamp, responseBytes);
                        bluetoothService.write(concatByte);

                        Toast.makeText(getApplicationContext(),"the client app is requesting the "+readMessage,Toast.LENGTH_SHORT).show();

                    }

                    /*--------------------------------------------------*/
                    /* --- If data requested is the number of sensor ---*/
                    /*--------------------------------------------------*/

                    else if(readMessage.equals("sensor_number")) {


                        String sensor = sensorNumber.getText().toString();

                        if(sensor.isEmpty() || sensor.equals("")){
                            String error = new String("You must specify the number of sensors in the server app");
                            byte[] er = error.getBytes();
                            bluetoothService.write(er);
                        }
                        else{
                            int sensorNb = Integer.parseInt(sensor);
                            numData[0]= (byte) 2;
                            byte[] responseBytes = new byte[1];
                            responseBytes[0]= (byte) sensorNb;

                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            byte[] concatByte = outPutStream.concatenateData(outputStream, numData, timeStamp, responseBytes);
                            bluetoothService.write(concatByte);
                            Toast.makeText(getApplicationContext(),"the client app is requesting the number of sensors of the insole",Toast.LENGTH_SHORT).show();

                        }

                    }


                    /*------------------------------------------------------*/
                    /* --- If the request is to change the insole's name--- */
                    /*------------------------------------------------------*/

                    else if (readMessage.length()==10) {
                        String t = new String("new_name");
                        if(readMessage.toLowerCase().contains(t.toLowerCase())){

                            if(readMessage.charAt(8) == (int) readMessage.charAt(8) && readMessage.charAt(9) == (int) readMessage.charAt(9)){
                                nameParam1 = (int) readMessage.charAt(8);
                                nameParam1 = (int) readMessage.charAt(9);
                                String newName = generateName(nameParam1,nameParam2,insoleSide,footSize);
                                insoleName.setText(newName);


                                numData[0]= (byte) 12;
                                String p = new String("name");
                                byte[] responseBytes = p.getBytes();

                                // Concatenation of the three arrays
                                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                byte[] concatByte = outPutStream.concatenateData(outputStream, numData, timeStamp, responseBytes);
                                bluetoothService.write(concatByte);

                                Toast.makeText(getApplicationContext(),"The name of the insole has been changed to "+newName,Toast.LENGTH_SHORT).show();}
                        }else{
                            String unrec = new String("Request unrecognized");
                            byte[] b = unrec.getBytes();
                            bluetoothService.write(b);
                        }


                    /*---------------------------------------------------------------*/
                    /* --- If it is a request to the insole to start sending data--- */
                    /*---------------------------------------------------------------*/

                    }else if(readMessage.equals("start_sending")){

                        numData[0]= (byte) 10;
                        String response = new String("start");
                        byte[] responseBytes = response.getBytes();

                        // Concatenation of the three arrays and sending response
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        byte[] concatByte = outPutStream.concatenateData(outputStream,numData,timeStamp,responseBytes);
                        bluetoothService.write(concatByte);
                        Toast.makeText(getApplicationContext()," The client app is requesting to "+readMessage+ " sending data",Toast.LENGTH_SHORT).show();

                        //Sending the real data
                        String messageS = sensorNumber.getText().toString();
                        String messageF = frequency.getText().toString();
                        sendData(messageS, messageF);

                    }

                    /*---------------------------------------------------------------*/
                    /* --- If it is a request to the insole to stop sending data--- */
                    /*---------------------------------------------------------------*/

                    else if(readMessage.equals("stop_sending")){

                        numData[0]= (byte) 11;
                        String response = new String("stop");
                        byte[] responseBytes = response.getBytes();

                        // Concatenation of the three arrays and sending response
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        byte[] concatByte = outPutStream.concatenateData(outputStream, numData, timeStamp, responseBytes);
                        bluetoothService.write(concatByte);
                        Toast.makeText(getApplicationContext()," The client app is requesting to "+readMessage+ " sending data",Toast.LENGTH_SHORT).show();

                        //Sending the real data
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
                    /* --- If it is a request is to inspect the battery-- */
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

                    /*------------------------------*/
                    /* --- If none of the above --- */
                    /*------------------------------*/

                    else{
                        Toast.makeText(getApplicationContext(),"The client app rest an  unrecognized request",Toast.LENGTH_SHORT).show();
                    }







                    if(DEBUG)Log.d(TAG,"Message read");
                    //Toast.makeText(getApplicationContext(),"message Read",Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.insole_simulator);
        //ActionBar actionBar = getActionBar();
        //actionBar.setBackgroundDrawable(new ColorDrawable(Color.RED)); // set your desired color

        bluetoothService = new BluetoothService(mHandler,this);
        bluetoothService.accept();
        sensorNumber = (EditText) findViewById(R.id.editSensorNumber);
        frequency = (EditText) findViewById(R.id.editFrequency);
        send = (Button) findViewById(R.id.sendBtn);
        send.setBackgroundResource(R.drawable.send_selector);

        // Name of the insole
        //NAME = (TextView) findViewById(R.id.NAME);
        Typeface t = Typeface.createFromAsset(getAssets(),"fonts/Langdon.otf");
        //NAME.setTypeface(t);

        insoleName = (TextView) findViewById(R.id.insoleName);

        insoleName.setText(generateName(nameParam1,nameParam2,insoleSide,footSize));

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


        // Setting up the police of "Type of data"
        typeOfData = (TextView) findViewById(R.id.type_of_data);
        Typeface typeface = Typeface.createFromAsset(getAssets(),"fonts/Ailerons.ttf");
        typeOfData.setTypeface(typeface);

        //Setting up the circular progress bar
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        //Setting up the switch that determines which side of the insole
        leftRight = (ToggleButton) findViewById(R.id.leftRight);

        leftRight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    insoleSide = new String("R");
                    insoleName.setText(generateNameBis(nameNumber, insoleSide, footSize));
                    Toast.makeText(getApplicationContext(),"You're now a right side insole",Toast.LENGTH_SHORT).show();

                } else {
                    insoleSide = new String("L");
                    insoleName.setText(generateNameBis(nameNumber, insoleSide, footSize));
                    Toast.makeText(getApplicationContext(),"You're now a left side insole",Toast.LENGTH_SHORT).show();
                }
            }
        });





        //Setting up the spinner
        data = (Spinner) findViewById(R.id.data);
        ArrayAdapter<CharSequence> dataAdapter = ArrayAdapter.createFromResource(this,R.array.data_type,android.R.layout.simple_spinner_item);
        dataAdapter.setDropDownViewResource(R.layout.spinner_textview);
        data.setAdapter(dataAdapter);

        //Setting up the OnItemClickListener for the spinner

        data.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String itemAtPosition = new String(parent.getItemAtPosition(position).toString());

                if(itemAtPosition.equals("Type 1")){
                    //Toast.makeText(getApplicationContext(),"Type of data chosen : 1",Toast.LENGTH_SHORT).show();
                    if (DEBUG )Log.i(TAG,"updating to type 1");
                               dataType=1;
                    if (DEBUG) Log.i(TAG,"Update successful");
                }else if(itemAtPosition.equals("Type 2")){
                    //Toast.makeText(getApplicationContext(),"Type of data chosen : 2",Toast.LENGTH_SHORT).show();
                    if (DEBUG )Log.i(TAG,"updating to type 2");
                    dataType=2;
                    if (DEBUG) Log.i(TAG,"Update successful");
                }else if(itemAtPosition.equals("Type 3")){
                    //Toast.makeText(getApplicationContext(),"Type of data chosen : 3",Toast.LENGTH_SHORT).show();
                    if (DEBUG )Log.i(TAG,"updating to type 3");
                    dataType=3;
                    if (DEBUG) Log.i(TAG,"Update successful");

                }



            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String messageS = sensorNumber.getText().toString();
                String messageF = frequency.getText().toString();

                sendData(messageS,messageF);
            }
        });

        abort = (Button) findViewById(R.id.abortBtn);
        abort.setBackgroundResource(R.drawable.abort_selector);

        abort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopData();
            }
        });

    }


    //Method for sending data

    public void sendData(String message1, String message2){

        if (message1.isEmpty() || message2.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Make sure you enter both the number of sensors and the frequency ", Toast.LENGTH_SHORT).show();
        }else {
            if(bluetoothService.getState() != BluetoothService.STATE_CONNECTED)
            {Toast.makeText(getApplicationContext(), "The client app needs to be connected first", Toast.LENGTH_SHORT).show();}
            else{

                logic = true;
                int sensorNbr = Integer.parseInt(message1);
                int frq = Integer.parseInt(message2);

                if(DEBUG) Log.i(TAG,"before updating dataProvider");
                dataProvider = new DataProvider(sensorNbr, frq, dataType);
                String s = String.valueOf(dataType);
                if (DEBUG) Log.i(TAG,"Update dataType to "+s);


                loop = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {

                            while (dataProvider.getSend() == true) {
                                int f = dataProvider.getFrequency();
                                String message = Arrays.toString(dataProvider.getData());
                                //Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
                                message += d;
                                d++;
                                sendMessage(message);
                                bluetoothService.sleep(1000 / f);
                                //int dt = dataProvider.getDataType();
                                // if(dt ==3){ dataProvider.setDataType(0);}
                                //else{dataProvider.setDataType(dt+1);}

                            }
                        }
                    }
                });
                loop.start();
                progressBar.setVisibility(View.VISIBLE);
                // clear EditTexts
                sensorNumber.setText("");
                frequency.setText("");

            }
        }




    }



    //Method for stopping data
    public void stopData(){

        if(logic == true)

        {dataProvider.abortSend();
            d=0;
            if(DEBUG){Log.d(TAG,"Trying to interrupt the loop");}
            loop.interrupt();
            if(DEBUG){Log.d(TAG,"Trying Interruption successful");}

            progressBar.setVisibility(View.GONE);

            logic=!logic;
        }else{
            Toast.makeText(getApplicationContext(),"There is no stream of data to be stopped",Toast.LENGTH_SHORT).show();
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
            //sensorNumber.setText(mOutStringBuffer);
        }
    }

    public String generateName(int p1,int p2, String inSide,int footS){

        int n = p1 + 256*p2;
        String newName = "FeetMe "+n+inSide+"-"+footS;
        return newName;
    }

    public String generateNameBis(int p1, String inSide,int footS){

        String newName = "FeetMe "+p1+inSide+"-"+footS;
        return newName;
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
    public void NameMessage(int nbr,String s,int foot) {

        String n = nbr+s+"-"+foot;
        footSize=foot;
        nameNumber=nbr;
        if(s.equals("R"))
        {
            if(DEBUG) Log.i(TAG, "right");
            leftRight.setChecked(true);
        }else if(s.equals("L"))
        {
            if(DEBUG) Log.i(TAG, "left");
            leftRight.setChecked(false);
        }
        insoleName.setText(n);
        Toast.makeText(getApplicationContext(),"The name of the insole has changed to "+n,Toast.LENGTH_SHORT).show();
    }

    /*--- For receiving the new version ---*/
    @Override
    public void VersionMessage(String message) {

    }

    /*--- For receiving the new footsize ---*/
    @Override
    public void FootsizeMessage(int intFoot) {

        footSize = intFoot;
        String name = nameNumber+insoleSide+"-"+footSize;
        insoleName.setText(name);
        Toast.makeText(getApplicationContext(),"footsize of the insole has changed to "+footSize,Toast.LENGTH_SHORT).show();
    }
}