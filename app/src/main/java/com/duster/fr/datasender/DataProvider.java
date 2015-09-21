package com.duster.fr.datasender;

import java.util.Random;

/**
 * Created by Anas on 21/09/2015.
 */
public class DataProvider {


    // for Debugging purposes
    private static final String TAG = "DataProvider";
    private static boolean DEBUG = true;

    //Number of sensors and frequency of transfer
    private int sensorNumber;
    private int frequency;

    //Type of data
    private int dataType;

    // for sending or aborting
    private volatile boolean send=true;

    //for data (in bytes)
    byte[] data;

    // Random variable

    Random rand = new Random();

    public DataProvider(int sensorNbr, int frq, int type) {
        this.frequency = frq;
        this.sensorNumber = sensorNbr;
        this.dataType = type;
    }

    // Setters and getters

    /*--For sensorNumber--*/
    public  void setSensorNumber(int sensorNbr){
        sensorNumber = sensorNbr;

    }

    public  int getSenNumber(){
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

            data = new byte[sensorNumber];
            switch(dataType){
                case 0:
                    for (int j = 0; j< sensorNumber; j++) {
                        data[j] = 5;
                    }
                case 1:
                    for (int j = 0; j< sensorNumber; j++) {
                        data[j] = 4;
                    }
                case 2:
                    for (int j = 0; j< sensorNumber; j++) {
                        data[j] = 3;
                    }
            }
        return data;
    }

    public void changeDataType(int type){
        dataType=type;
    }


}
