package com.duster.fr.datasender;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.UUID;

//import java.util.logging.Handler;

/**
 * Created by Anas on 15/09/2015.
 */
public class BluetoothService {

    private static final String TAG = "BluetoothService";
    protected static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private int mState;

    //To indicate the state of the current connection
    public static final int STATE_NONE = 0;       // nothing is done
    public static final int STATE_LISTEN = 1;     // currently listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device


    private final BluetoothAdapter mAdapter;
    private Handler mHandler;
    private Boolean send;
    private ConnectedThread mConnectedThread;
    private AcceptThread mAcceptThread;

    //data sent back to the apps
    private byte numData;
    private byte[] timeStamp = new byte[4];
    private byte[] version = new byte[]{1,0};

    //data about the insole
    private String side="L";
    private int sensorNb;
    private int frequency;
    private int dataType;
    private int footSize;
    private int nameParam1;
    private int nameParam2;
    private String btName;
    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private byte[] rBytes;
    private int batInt;
    private DataProvider dataProvider;

    //Secondary thread
    private Thread loop;

    //Setting up the timestamp array
    private String date = new SimpleDateFormat("dd,MM,yyyy,HH,mm,ss").format(new java.util.Date());
    private String[] dateParts = date.split(",");






    // Boolean to not allow abort button to do more than aborting
    private boolean isSendingData =false;


    private MainActivity activity;
    int testInt;

    public BluetoothService(String s,Handler handler, MainActivity activity) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
        this.activity = activity;
        side=s;
    }

    public synchronized void setSide(String s){
        side = s;
    }
    public synchronized void setSensorNb(int nb){
        sensorNb = nb;
    }

    public synchronized void setFrequency(int fr){
        frequency = fr;
    }

    public synchronized void setFootSize(int fs){
        footSize = fs;
    }
    public synchronized void setBatteryLevel(int batt){
        batInt = batt;
    }
    public synchronized void setType(int type){
        dataType = type;
    }

    public void setBluetoothServiceName(String s){
        mAdapter.setName(s);
    }

    public String getBluetoothServiceName(){
        return mAdapter.getName();
    }







    private synchronized void setState(int state){
        if(MainActivity.DEBUG) Log.d(TAG, "setState" + mState +"-->"+state);
        mState=state;
    }

    public synchronized int getState(){
        return mState;
    }

    private synchronized void stop() {
        Log.d(TAG, "stop");
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

    }
    // To write to the ConnectedThread in an unsynchronized manner

    public void write(byte[] outMsg){
        ConnectedThread r;

        synchronized (this){
            if (mState!=STATE_CONNECTED) return;
            r=mConnectedThread;
        }

        //unsynchronized wrinting

        r.write(outMsg);


    }
    public synchronized void disconnect() {
        stop();
        setState(STATE_NONE);
    }

    /* Starting the thread to enable connection to device*/
    public synchronized void accept() {
        Log.d(TAG, "accept");
        stop();
        mAcceptThread = new AcceptThread();
        mAcceptThread.start();
    }

    /*Starting the tread to manage the connection and enable transmissions*/

    public synchronized void Connected(BluetoothSocket socket,BluetoothDevice device) {
        Log.d(TAG, "connected");
        stop();

        mConnectedThread = new ConnectedThread(socket, mHandler);
        mConnectedThread.start();
        mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME,"Connected to -> "+device.getName()).sendToTarget();
        //Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME, device.getName());
        //mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);

    }

    public boolean sendOrStop(){
        if(mConnectedThread!=null){
            send = !send;
        }
        return send;
    }

    public void sleep(int t){
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class AcceptThread extends Thread {
        private static final String TAG = "AcceptThread";

        private final BluetoothServerSocket mmServerSocket;
        private BluetoothSocket mbtSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;

            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord("Data Sender for android", MY_UUID); // Unsed to get a BluetoothServerSocket running
            } catch (IOException e) {
                if(MainActivity.DEBUG) Log.e(TAG, e.toString());
            }
            mmServerSocket = tmp;

        }

        public void run() {
            /* Connection through a socket*/
                try {
                    mbtSocket = mmServerSocket.accept(); // listening to connection requests
                    // not to be used in the main thread ( because it's a blocking call)
                    Log.i(TAG, "connection accepted");
                    mmServerSocket.close(); // Used to not accept additional connections

                } catch (IOException connectException) {
                /*enabling the connection and closing the socket*/
                    if (MainActivity.DEBUG) Log.e(TAG, "close socket");
                    try {
                        mmServerSocket.close();
                    } catch (IOException closeException) {
                        if (MainActivity.DEBUG) Log.e(TAG, closeException.toString());
                    }

                }
                if (mbtSocket != null) {
                    BluetoothService.this.Connected(mbtSocket, mbtSocket.getRemoteDevice());
                    //if (MainActivity.DEBUG) Log.d(TAG, mbtSocket.getRemoteDevice().getName());
                }

        }

        public void cancel(){
            try {
                mmServerSocket.close();
                mbtSocket=null;
            } catch (IOException e) {
                Log.i(TAG, "The socket couldn't be closed", e);
            }
        }
    }

    private void connectionLost(){
        setState(STATE_LISTEN);

        // send failure message back to the UI Activity

        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST,"The connection with the device was last");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }
    private class ConnectedThread extends Thread {

        private static final String TAG = "ConnectedThread";

        private volatile boolean running;

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, Handler handler) {
            mmSocket = socket;
            mHandler = handler;


            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                if(MainActivity.DEBUG) Log.e(TAG, "Could not get any data from the socket");
                BluetoothService.this.disconnect();
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte t0 = (byte) Integer.parseInt(dateParts[0]);
            byte t1 = (byte) Integer.parseInt(dateParts[1]);
            byte t2 = (byte) Integer.parseInt(dateParts[2].substring(0,2));
            byte t3 = (byte) Integer.parseInt(dateParts[2].substring(2,4));
            timeStamp[0] = (byte) t0;
            timeStamp[1] = (byte) t1;
            timeStamp[2] = (byte) t2;
            timeStamp[3] = (byte) t3;

            /*----------------------------------------------------------*/
            /* --- once connection is established, send a message ------*/
            /*----------------------------------------------------------*/

            numData = (byte) 14;
            rBytes= "madm".getBytes();

            // Concatenation of the three arrays and sending dataSpinner
            outputStream.reset();
            try {
                outputStream.write(numData);
                outputStream.write(rBytes);
            } catch (IOException e) {
                Log.w(TAG, "write failure");
            }
            write(outputStream.toByteArray());

            running = true;
            if(MainActivity.DEBUG) Log.i(TAG,"Begin mConnected");
            int bytes =0;
            byte[] buffer = new byte[1024];
            send = false;
            testInt = 0;
            String message;
            while(running) {
                try {

                    if(mmInStream.available() > 0) {
                        bytes = mmInStream.read(buffer);

                        message = new String(buffer,0,bytes);

                        //Deciding what to do with data

                        /*-----------------------*/
                        /* --- If it's a ping ---*/
                        /*-----------------------*/

                        if(message.equals("ping")) {
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

                            write(outputStream.toByteArray());
                        }
                        /*------------------------------------------------*/
                        /* --- If data requested is the version ---*/
                        /*------------------------------------------------*/

                        else if(message.equals("version")){

                            numData = (byte) 3;

                            // Concatenation of the three arrays
                            outputStream.reset();
                            try {
                                outputStream.write(numData);
                                outputStream.write(timeStamp);
                                outputStream.write(version);
                            } catch (IOException e) {
                                Log.w(TAG, "write failure");
                                break;
                            }
                            write(outputStream.toByteArray());
                            if(MainActivity.DEBUG) Log.d(TAG,"version of the insole is sent");
                            mHandler.obtainMessage(MainActivity.MESSAGE_TOAST,"The version of the" +
                                    "app has been requested")
                                    .sendToTarget();

                        }

                        /*----------------------------------------------------*/
                        /* --- If data requested is the side of the insole ---*/
                        /*----------------------------------------------------*/
                        else if(message.equals("insole_side")){
                            numData = (byte) 4;
                            rBytes = side.getBytes();

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
                            write(outputStream.toByteArray());
                            mHandler.obtainMessage(MainActivity.MESSAGE_TOAST,"the client " +
                                    "app is requesting which side is the insole")
                                    .sendToTarget();
                        }

                        /*--------------------------------------------------*/
                        /* --- If data requested is the number of sensor ---*/
                        /*--------------------------------------------------*/

                        else if(message.equals("sensors_number")){

                            numData = (byte) 2;
                            rBytes = new byte[]{(byte) sensorNb};
                            outputStream.reset();
                            try {
                                outputStream.write(numData);
                                outputStream.write(timeStamp);
                                outputStream.write(rBytes);
                            } catch (IOException e) {
                                Log.w(TAG, "write failure");
                                break;
                            }
                            write(outputStream.toByteArray());

                            mHandler.obtainMessage(MainActivity.MESSAGE_TOAST,"the client" +
                                    " app is requesting the number of sensors of the insole")
                                    .sendToTarget();
                        }

                        /*--------------------------------------------------*/
                        /* --- If data requested is the footsize ---*/
                        /*--------------------------------------------------*/

                        else if(message.equals("footsize")){

                            numData = (byte) 8;
                            rBytes = new byte[]{(byte) footSize};

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
                            write(outputStream.toByteArray());

                            mHandler.obtainMessage(MainActivity.MESSAGE_TOAST,"the client" +
                                    " app is requesting the size of the foot")
                                    .sendToTarget();


                        }

                        /*------------------------------------------------------*/
                        /* --- If the request is to change the insole's name--- */
                        /*------------------------------------------------------*/

                        else if(message.length()==10 &&message.toLowerCase().contains("new_name".
                                toLowerCase()) ){

                            if(message.charAt(8) == (int) message.charAt(8) &&
                                    message.charAt(9) == (int) message.charAt(9)){

                                nameParam1 =  Integer.parseInt(String.valueOf(message.charAt(8)));
                                nameParam2 = Integer.parseInt(String.valueOf(message.charAt(9)));

                                activity.runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        btName=activity.generateName(nameParam1,
                                                nameParam2, side, footSize);
                                        activity.setName(btName);
                                        setBluetoothServiceName(btName);
                                    }
                                });

                                numData = (byte) 12;
                                rBytes = "name".getBytes();
                                try {
                                    outputStream.write(numData);
                                    outputStream.write(timeStamp);
                                    outputStream.write(rBytes);
                                } catch (IOException e) {
                                    Log.w(TAG, "write failure");
                                    break;
                                }
                                write(outputStream.toByteArray());
                                mHandler.obtainMessage(MainActivity.MESSAGE_TOAST,"The " +
                                        "name has been changed")
                                        .sendToTarget();
                            }
                        }

                        /*--------------------------------------------------------*/
                        /* --- If it is a request is to inspect the battery level-- */
                        /*--------------------------------------------------------*/

                        else if(message.equals("battery_info")){
                            if(MainActivity.DEBUG) Log.i(TAG,"battery");
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
                            write(outputStream.toByteArray());
                            mHandler.obtainMessage(MainActivity.MESSAGE_TOAST,"The " +
                                    "battery level has been requested")
                                    .sendToTarget();
                        }

                        else if(message.equals("start_sending")){
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
                            write(outputStream.toByteArray());
                            mHandler.obtainMessage(MainActivity.MESSAGE_TOAST,"the request to " +
                                    "start sending is received")
                                    .sendToTarget();

                            sendData();

                        }

                        else if(message.equals("stop_sending")){
                            numData = (byte) 11;
                            rBytes = "stop".getBytes();

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
                            write(outputStream.toByteArray());
                            mHandler.obtainMessage(MainActivity.MESSAGE_TOAST,"The client " +
                                    "app is requesting to stop sending data")
                                    .sendToTarget();

                            BluetoothService.this.stopData();

                        }

                        else if(message.equals("stop")){
                            if(MainActivity.DEBUG) Log.d(TAG,"The BT service enters a sleep mode");
                            try {
                                Thread.currentThread().sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (MainActivity.DEBUG) Log.i(TAG, "The BT service slept well");
                            mHandler.obtainMessage(MainActivity.MESSAGE_TOAST,"The BT service" +
                                    " slept well !")
                                    .sendToTarget();

                        }
                        else if(message.equals("reset_timestamp")){
                            if(MainActivity.DEBUG) Log.i(TAG,"timestamp");
                            numData = (byte) 5;

                            outputStream.reset();
                            try {
                                outputStream.write(numData);
                                outputStream.write(timeStamp);
                            } catch (IOException e) {
                                Log.w(TAG, "write failure");
                                break;
                            }
                            write(outputStream.toByteArray());
                            if (MainActivity.DEBUG) Log.i(TAG, "The BT service slept well");
                            mHandler.obtainMessage(MainActivity.MESSAGE_TOAST, "Timestamp reset")
                                    .sendToTarget();

                        }
                        else if(message.contains("#")){
                            if (MainActivity.DEBUG)Log.d(TAG, "Hello message received");
                        }


                        else{
                            mHandler.obtainMessage(MainActivity.MESSAGE_TOAST, "Request unrecognized")
                                    .sendToTarget();
                        }





                    }else{
                        try {
                            Thread.sleep(50);
                        }catch (InterruptedException e){
                            Log.w(TAG, Log.getStackTraceString(e));
                        }
                    }

                } catch (IOException e) {
                    if (MainActivity.DEBUG) Log.e(TAG, "disconnected", e);
                    BluetoothService.this.disconnect();
                    Log.d(TAG, "send Message disconnection");
                }
            }
        }

        public void write(byte[] buffer){

            try {
                mmOutStream.write(buffer);
                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e("writing", "unable to write data in the device", e);
                //mHandler.obtainMessage(MainActivity.MESSAGE_DISC).sendToTarget();
                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        activity.EditorsEnabled(true);
                    }
                });
                dataProvider.abortSend();
                BluetoothService.this.disconnect();
                //added in 30th
                //makeDiscoverable(activity);
                BluetoothService.this.accept();
            }

        }

        public void read(byte[] bytes){
            try {
                mmInStream.read(bytes);
            }catch (IOException e){
                Log.e("Reading", "Unable to read data on the stream", e);
            }
        }

        public void cancel() {
            Log.i(TAG, "cancel Thread");
            running = false;

            try {
                Thread.sleep(100);
            } catch (InterruptedException e){
                Log.e(TAG, "interrupter");
            }
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close socket", e);
            }
        }
    }

    protected void checkBluetoothEnabled(Activity activity){
        if(!mAdapter.isEnabled()){
            Intent enableBtIntent= new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent,1);
        }
    }

    protected void makeDiscoverable(Activity activity){
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
        activity.startActivity(discoverableIntent);
    }

    protected void sendData(){
        if(this.getState() != BluetoothService.STATE_CONNECTED){
            mHandler.obtainMessage(MainActivity.MESSAGE_TOAST,"you need to be connected first")
                    .sendToTarget();
            return;
        }

        isSendingData = true;

        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                activity.EditorsEnabled(false);
            }
        });

        if(MainActivity.DEBUG) Log.i(TAG,"before updating dataProvider");
        dataProvider = new DataProvider(sensorNb, frequency, dataType);

        if(MainActivity.DEBUG) {;
            Log.d(TAG, String.valueOf(sensorNb));
            Log.d(TAG, String.valueOf(frequency));
            Log.d(TAG, String.valueOf(dataType));
        }
        //String s = String.valueOf(dataType);
        //if (MainActivity.DEBUG) Log.i(TAG,"Update dataType to "+s);

        loop = new Thread() {
            @Override
            public void run() {
                while (dataProvider.getSend()) {
                    outputStream.reset();
                    try {
                        outputStream.write(dataProvider.getData());
                        if(MainActivity.DEBUG){
                            Log.d(TAG,String.valueOf(dataProvider.getData().length));
                        }
                    } catch (IOException e) {
                        Log.w(TAG, "failed to write dataSpinner");
                        continue;
                    }
                    write(outputStream.toByteArray());
                    try {
                        sleep(1000 / frequency);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        loop.start();
    }


    protected void stopData(){
        if(isSendingData) {

            activity.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    activity.EditorsEnabled(true);
                }
            });
            dataProvider.abortSend();
            if(MainActivity.DEBUG){Log.d(TAG,"Trying to interrupt the loop");}
            if(MainActivity.DEBUG){Log.d(TAG,"Trying Interruption successful");}
            isSendingData =false;
        }else{

            mHandler.obtainMessage(MainActivity.MESSAGE_TOAST, "There is no stream of data" +
                    "to be stopped")
                    .sendToTarget();
        }
    }


}