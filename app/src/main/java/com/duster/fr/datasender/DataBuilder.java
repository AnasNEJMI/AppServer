package com.duster.fr.datasender;

import java.nio.ByteBuffer;

/**
 * Created by Anas on 15/09/2015.
 */
public class DataBuilder {

    private static final String TAG = "DataBuilder";
    private static  int sensorNumber= 91; // the number of sensors is to be dynamically setup
    private static final int OVER_HEAD = 7;

    private int type;
    private ByteBuffer buffer;
    private byte[] bytes;

    public  void setSensorNumber(int s){
        sensorNumber = s;

    }

    public  int getSenNumber(){
        return sensorNumber;

    }

    public DataBuilder(){
        type = 0;
        bytes = new byte[OVER_HEAD + sensorNumber];
    }

    public byte[] getData(){
        //buffer = ByteBuffer.allocate(sensorNumber + OVER_HEAD);
        if (type == 0) {
            for (int i = 0; i < sensorNumber + OVER_HEAD; i++) {
                //buffer.put((byte)5);
                bytes[i] = 5;
            }
        }else if(type == 1){
            for (int i = 0; i < sensorNumber + OVER_HEAD; i++) {
                //buffer.put((byte)250);
                bytes[i] = (byte) 250;
            }
        }

        return bytes;//buffer.array();
    }

    public void changeType(){
        type = (type+1) % 2;
    }
}
