package com.duster.fr.datasender;

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
    private boolean send;

    //for data (in bytes)
    byte[] data;



    // Constructor

    public void DataProvider(int sensorNbr,int frq, int type){
        this.frequency=frq;
        this.sensorNumber=sensorNbr;
        this.dataType=type;

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


    public byte[] getData(int sensorNumber, int frequency, int dataType){

        for(int i =0; i<frequency;i++){
            data = new byte[sensorNumber];
            switch(dataType){
                case 0:
                    for (int j = 0; i < sensorNumber; j++) {
                        data[i] = 5;
                    }
                case 1:
                case 2:
                case 3:
            }

        }
        return data;
    }

    public void changeDataType(int type){
        dataType=type;
    }


}
