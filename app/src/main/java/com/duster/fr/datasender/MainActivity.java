package com.duster.fr.datasender;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Message;
import android.os.Bundle;
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
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.ThreadPoolExecutor;

public class MainActivity extends ActionBarActivity {

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

    private DataBuilder dataBuilder = new DataBuilder();


    //Layouts
    private  Button send;
    private  Button abort;
    private EditText sensorNumber;
    private EditText frequency;
    private ProgressBar progressBar;
    private Spinner data;
    private TextView typeOfData;
    private ToggleButton leftRight;


    // Infos about the insole
    private String insoleSide;
    private int footSize = 42;
    private String version= new String("0.1.6");
    byte[] numData = new byte[1];
    byte[] timeStamp = new byte[3];




    // Boolean to not allow abort button to do more than aborting
    private boolean logic;

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
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);

                    //Deciding what to do with data

                    /* --- If it's a ping ---*/
                    if(readMessage.equals("ping")) {
                        // Setting up the three byte arrays for the response
                        numData[0]= (byte) 1;
                        String p = new String("pong");
                        byte[] pBytes = p.getBytes();

                        // Concatenation of the three arrays

                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
                        try {
                            outputStream.write( numData );
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            outputStream.write( timeStamp );
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            outputStream.write( pBytes );
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //Sending info to client app

                        byte[] c = outputStream.toByteArray();
                        String concat = Arrays.toString(c);
                        byte[] concatByte = concat.getBytes();
                        //this.sendMessage(concat);

                        //String pong = new String("pong");
                        //byte[] b = pong.getBytes();
                        bluetoothService.write(concatByte);

                        Toast.makeText(getApplicationContext()," The client app is requesting a "+readMessage,Toast.LENGTH_SHORT).show();
                    }


                    /* --- If data requested is the version ---*/
                    else if(readMessage.equals("version")) {

                        numData[0]= (byte) 3;
                        String p = new String(version);
                        byte[] pBytes = p.getBytes();

                        // Concatenation of the three arrays

                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
                        try {
                            outputStream.write( numData );
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            outputStream.write( timeStamp );
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            outputStream.write( pBytes );
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //Sending info to client app

                        byte[] c = outputStream.toByteArray();
                        String concat = Arrays.toString(c);
                        byte[] concatByte = concat.getBytes();
                        //this.sendMessage(concat);

                        //String pong = new String("pong");
                        //byte[] b = pong.getBytes();
                        bluetoothService.write(concatByte);
                        Toast.makeText(getApplicationContext(),"the client app is requesting "+readMessage+" of the insole",Toast.LENGTH_SHORT).show();
                    }


                    /* --- If data requested is the side ---*/
                    else if(readMessage.equals("side")) {
                        numData[0]= (byte) 4;
                        String p = new String(insoleSide);
                        byte[] pBytes = p.getBytes();

                        // Concatenation of the three arrays

                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
                        try {
                            outputStream.write( numData );
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            outputStream.write( timeStamp );
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            outputStream.write( pBytes );
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //Sending info to client app

                        byte[] c = outputStream.toByteArray();
                        String concat = Arrays.toString(c);
                        byte[] concatByte = concat.getBytes();
                        //this.sendMessage(concat);

                        //String pong = new String("pong");
                        //byte[] b = pong.getBytes();
                        bluetoothService.write(concatByte);

                        Toast.makeText(getApplicationContext(), "the client app is requesting which the " + readMessage+" is the insole",Toast.LENGTH_SHORT).show();
                    }

                    /* --- If data requested is the footsize ---*/
                    else if(readMessage.equals("footsize")) {
                        numData[0]= (byte) 1;
                        String p = String.valueOf(footSize);
                        byte[] pBytes = p.getBytes(Charset.forName("UTF-8"));

                        // Concatenation of the three arrays

                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
                        try {
                            outputStream.write( numData );
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            outputStream.write( timeStamp );
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            outputStream.write( pBytes );
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //Sending info to client app

                        byte[] c = outputStream.toByteArray();
                        String concat = Arrays.toString(c);
                        byte[] concatByte = concat.getBytes();
                        //this.sendMessage(concat);

                        //String pong = new String("pong");
                        //byte[] b = pong.getBytes();
                        bluetoothService.write(concatByte);

                        Toast.makeText(getApplicationContext(),"the client app is requesting the "+readMessage,Toast.LENGTH_SHORT).show();
                    }
                    else{
                        String unrec = new String("Request unrecognized");
                        byte[] b = unrec.getBytes();
                        bluetoothService.write(b);
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

        //Setting up the timestamp array
        timeStamp[0] = (byte) 1;
        timeStamp[1] = (byte) 2;
        timeStamp[2] = (byte) 3;



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
                    Toast.makeText(getApplicationContext(),"You're now a right side insole",Toast.LENGTH_SHORT).show();
                } else {
                    insoleSide = new String("L");
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
                    Toast.makeText(getApplicationContext(),"Type of data chosen : 1",Toast.LENGTH_SHORT).show();
                    if (DEBUG )Log.i(TAG,"updating to type 1");
                               dataType=1;
                    if (DEBUG) Log.i(TAG,"Update successful");
                }else if(itemAtPosition.equals("Type 2")){
                    Toast.makeText(getApplicationContext(),"Type of data chosen : 2",Toast.LENGTH_SHORT).show();
                    if (DEBUG )Log.i(TAG,"updating to type 2");
                    dataType=2;
                    if (DEBUG) Log.i(TAG,"Update successful");
                }else if(itemAtPosition.equals("Type 3")){
                    Toast.makeText(getApplicationContext(),"Type of data chosen : 3",Toast.LENGTH_SHORT).show();
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
                logic = true;
                String messageS = sensorNumber.getText().toString();
                String messageF = frequency.getText().toString();

                if (messageF.isEmpty() || messageS.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Make sure you enter both the number of sensors and the frequency ", Toast.LENGTH_SHORT).show();
                } else {
                    if(bluetoothService.getState() != BluetoothService.STATE_CONNECTED)
                    {Toast.makeText(getApplicationContext(), "The client app needs to be connected first", Toast.LENGTH_SHORT).show();}
                    else {
                        int sensorNbr = Integer.parseInt(messageS);
                        int frq = Integer.parseInt(messageF);

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

                        //if(dataProvider.getSend()==false){loop.interrupt();}


                        // check if the values entered are integers
                    /*try {
                        int num = Integer.parseInt(messageS);
                        Log.i("", num + " is a number");
                    } catch (NumberFormatException e) {
                        Log.i("", messageS + "is not a number");
                        Toast.makeText(getApplicationContext(), "the number of sensors should be a number", Toast.LENGTH_SHORT);
                    }

                    try {
                        int num = Integer.parseInt(messageF);
                        Log.i("", num + " is a number");
                    } catch (NumberFormatException e) {
                        Log.i("", messageF + "is not a number");
                        Toast.makeText(getApplicationContext(), "the number of sensors should be a number", Toast.LENGTH_SHORT);
                    }*/

                        // send data if connected


                        // clear EditTexts
                        sensorNumber.setText("");
                        frequency.setText("");
                        //sendMessage(messageS);
                        //sendMessage(messageF);
                    }
                }
            }
        });

        abort = (Button) findViewById(R.id.abortBtn);
        abort.setBackgroundResource(R.drawable.abort_selector);

        abort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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
        });

    }

    private void sendMessage(String message) {
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
                return true;

            case  R.id.action_make_discoverable:
                bluetoothService.makeDiscoverable(this);
                bluetoothService.accept();
                return true;

            case  R.id.action_send:
                bluetoothService.sendOrStop();
                return true;

            case  R.id.action_change:
                bluetoothService.change();
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

}