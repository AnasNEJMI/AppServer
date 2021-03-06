package com.duster.fr.datasender;

import android.util.Log;

import java.util.Random;

/**
 * Created by Anas on 21/09/2015.
 */
public class DataProvider {


    // for Debugging purposes
    private static final String TAG = "DataProvider";
    private static boolean DEBUG = false;

    //Number of sensors and frequency of transfer
    private final int sensorNumber;
    private int frequency;

    //Type of data
    private int dataType;

    // for sending or aborting
    private volatile boolean send=true;

    //for data (in bytes)
    byte[] data;

    // Random variable



    public DataProvider(int sensorNbr, int frq, int type) {
        this.frequency = frq;
        this.sensorNumber = sensorNbr;
        this.dataType = type;
    }

    // Setters and getters

    public  int getSensorNumber(){
        return sensorNumber;

    }

    /*--For frequency--*/
    public  void setFrequency(int frq){
        frequency = frq;

    }

    public  int getFrequency(){
        return frequency;

    }

    /*--For DataType--*/
    public  void setDataType(int type){
        dataType = type;

    }

    public  int getDataType(){
        return dataType;

    }

    /*--For Send--*/
    public  void abortSend(){
        send =!send;
    }

    public  boolean getSend(){
        return send;

    }


    public byte[] getData(){

        int sensorNumberDividable = sensorNumber-(sensorNumber%3);
        int frontSensors=sensorNumberDividable/3 +(sensorNumber%3);
        int middleSensors = sensorNumberDividable/3;
        int backSensors = sensorNumberDividable/3;
        data = new byte[sensorNumber];
        if(dataType == 1) {
            Log.i(TAG,"data type ==1");

            for (int j = 0; j < frontSensors; j++) {
                Random rand = new Random();
                data[j] = (byte) (100 + rand.nextInt((10 - 1) + 1));
            }
            for (int j = 0; j < middleSensors; j++) {
                Random rand = new Random();
                data[j+frontSensors] = (byte) (40+ rand.nextInt((20 - 1) + 1));
            }
            for (int j = 0; j < backSensors; j++) {
                Random rand = new Random();
                data[j+frontSensors+middleSensors] = (byte) (0+ rand.nextInt((10 - 1) + 1));
            }
        }else if(dataType == 2) {
            Log.i(TAG,"data type ==2");
            for (int j = 0; j < frontSensors; j++) {
                Random rand = new Random();
                data[j] = (byte) (70 + rand.nextInt((10 - 1) + 1));
            }
            for (int j = 0; j < middleSensors; j++) {
                Random rand = new Random();
                data[j+frontSensors] = (byte) (60+ rand.nextInt((20 -(-5)) + (-5)));
            }
            for (int j = 0; j < backSensors; j++) {
                Random rand = new Random();
                data[j+frontSensors+middleSensors] = (byte) (65+ rand.nextInt((10 - 1) + 1));
            }

        }else if(dataType==3){
            Log.i(TAG,"data type ==3");
            for (int j = 0; j < frontSensors; j++) {
                Random rand = new Random();
                data[j] = (byte) (0 + rand.nextInt((10 - 1) + 1));
            }
            for (int j = 0; j < middleSensors; j++) {
                Random rand = new Random();
                data[j+frontSensors] = (byte) (50+ rand.nextInt((20 - 1) + 1));
            }
            for (int j = 0; j < backSensors; j++) {
                Random rand = new Random();
                data[j+frontSensors+middleSensors] = (byte) (100+ rand.nextInt((10 - 1) + 1));
            }
        }

        else if(dataType==4){
            Log.i(TAG,"data type ==4");
            for (int j = 0; j<sensorNumber; j++) {

                data[j] = (byte) 0;
            }

        }
        return data;
    }


}
