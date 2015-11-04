package com.hakaselabs.speech2emoji;

/**
 * Created by wei on 30/10/15.
 */
public interface PredictionModel {

    //public void readModel(String path);
    public  String predict(String[] context);

}
