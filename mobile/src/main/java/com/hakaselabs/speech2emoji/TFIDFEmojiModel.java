package com.hakaselabs.speech2emoji;

import android.content.Context;
import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.IOUtils;

/**
 * Created by wei on 30/10/15.
 */
public class TFIDFEmojiModel extends AsyncTask<InputStream, Integer, Void> implements PredictionModel{
    private HashMap<String, LinkedTreeMap<String,Double> > tfidf_matrix;
    private Gson gson;
    private InputStream is;
    private VoiceService vs;

    public TFIDFEmojiModel(VoiceService vs){
        gson = new Gson();
        this.vs = vs;
    }

    @Override
    protected Void doInBackground(InputStream... is) {
        try {
            String jsonTxt = IOUtils.toString(is[0]);
            tfidf_matrix = gson.fromJson(jsonTxt, HashMap.class);
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute (Void result){
        super.onPostExecute(result);
        vs.startListening();
    }

    public String predict(String[] context){
        LinkedTreeMap<String, Double> tmpMap;
        Double maxScore = 0.0;
        String bestEmoji=new String(Character.toChars(128512));
        for(String term : context){
            if(tfidf_matrix.containsKey(term)){
                tmpMap = tfidf_matrix.get(term);
                for(String key : tmpMap.keySet()) {
                    if (tmpMap.get(key) > maxScore) {
                        bestEmoji = key;
                        maxScore = tmpMap.get(key);
                    }
                }
            }
        }
        return bestEmoji;
    }

}
