package com.hakaselabs.speech2emoji;

import android.os.AsyncTask;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

/**
 * Created by wei on 30/10/15.
 */
public class TFIDFEmojiModel extends AsyncTask<InputStream, Integer, Void> implements PredictionModel{
    private HashMap<Integer, List<Candidate> > tfidf_matrix;
    private VoiceService vs;
    private Map<String, Integer> wordMap;
    private String[] emojis;

    public TFIDFEmojiModel(VoiceService vs){
        this.vs = vs;
        this.wordMap = new HashMap<>();
        tfidf_matrix = new HashMap<>();
    }

    private void loadModel(InputStream is) throws IOException{
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[8*1024];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        byte[] byteArray = buffer.toByteArray();
        int offset=0;

        //read first 2 byte indicating vocab string length
        int vocabBytes = (byteArray[offset++]&0xff) + ((byteArray[offset++]&0xff) << 8);
        //read vocab string
        String vocabStr = new String(Arrays.copyOfRange(byteArray, offset, offset+vocabBytes), "UTF-8");
        int k=0;
        for(String s : vocabStr.split(" ")){
            this.wordMap.put(s,k++);
        }
        offset += vocabBytes;

        //read 2 bytes indicating emoji string length
        int emojiBytes = (byteArray[offset++]&0xff) + ((byteArray[offset++]&0xff) << 8);

        //read emoji string
        emojis = new String(Arrays.copyOfRange(byteArray, offset, offset+emojiBytes), "UTF-8").split(" ");
        offset += emojiBytes;

        //read the mappings
        int n;
        int wid;
        while( offset< byteArray.length){
            wid = (byteArray[offset++]&0xff) + ((byteArray[offset++]&0xff) << 8);
            n = (byteArray[offset++]&0xff) + ((byteArray[offset++]&0xff) << 8);
            List<Candidate> candidates = new ArrayList<>();
            for(int num=0; num < n; num++){
                int eid = (byteArray[offset++]&0xff) + ((byteArray[offset++]&0xff) << 8);
                byte[] v = Arrays.copyOfRange(byteArray, offset, offset + 4);
                float f = ByteBuffer.wrap(v).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                offset+=4;
                //values.put(eid, f);
                Candidate c = new Candidate(eid,f);
                candidates.add(c);
            }
            this.tfidf_matrix.put(wid, candidates);
        }
    }

    //@Override
    protected Void doInBackground(InputStream... is) {
        try {
            this.loadModel(is[0]);
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
        return null;
    }

    //@Override
    protected void onPostExecute (Void result){
        //super.onPostExecute(result);
        vs.startListening();
    }

    public String predict(String[] context){
        TreeSet<Candidate> candidateSet = new TreeSet<>();
        int chooseFrom = 5;
        float total=0f;
        Random r = new Random();
        Integer bestEmojiID;
        for(String w : context){
            if(!this.wordMap.containsKey(w.toLowerCase(Locale.getDefault())))
                continue;

            int wid = this.wordMap.get(w.toLowerCase(Locale.getDefault()));
            if(tfidf_matrix.containsKey(wid)){
                candidateSet.addAll(tfidf_matrix.get(wid));
            }
        }
        Iterator<Candidate> it = candidateSet.descendingIterator();
        int current = 0;
        List<Candidate> result = new ArrayList<>();
        float[] cdf = new float[chooseFrom];
        while(it.hasNext() && (current < chooseFrom)){
            Candidate c = it.next();
            result.add(c);
            total+=c.getProb();
            cdf[current] = total;
            current++;
        }

        if(result.size()>0) {
            for (int i = 0; i < cdf.length; i++) {
                cdf[i] /= total;
            }

            float rd = r.nextFloat();
            int idx = 0;
            for (; idx < cdf.length; idx++) {
                if (cdf[idx] > rd) break;
            }
            //choose emoji with probability
            bestEmojiID = result.get(idx).getId();
        }else{
            bestEmojiID = r.nextInt(this.emojis.length);
        }
        return this.emojis[bestEmojiID];
    }

    public static void main(String[] args){
        TFIDFEmojiModel model = new TFIDFEmojiModel(null);
        try {
            model.loadModel(new FileInputStream("/Users/wei/dev/speech2emoji/mobile/src/main/assets/model.m"));
            String p = model.predict(new String[] {"I", "love", "canada"});
            System.out.println(p);

        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }
}
