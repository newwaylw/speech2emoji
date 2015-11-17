package com.hakaselabs.speech2emoji;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;


/**
 * Created by wei on 22/10/15.
 */
public class VoiceService extends Service {
    public static String TAG = VoiceService.class.getName();
    public static final String VOICE_RESULT_READY = "0";
    public static final String MODEL_READY ="1";
    public static final String RECOGNIZER_ERROR = "2";
    protected static AudioManager mAudioManager;
    protected SpeechRecognizer mSpeechRecognizer;
    protected Intent mSpeechRecognizerIntent;
    protected final Messenger mServerMessenger = new Messenger(new IncomingHandler(this));

    protected boolean mIsListening;
    protected volatile boolean mIsCountDownOn;
    private static boolean mIsStreamSolo;
    private static int mStreamVolume = 0;

    static final int MSG_RECOGNIZER_START_LISTENING = 1;
    static final int MSG_RECOGNIZER_CANCEL = 2;
    static final int MSG_RECOGNIZER_RESTART = 3;

    private LocalBroadcastManager mLocalBroadcaster ;
    private TFIDFEmojiModel emojiModel;

    protected static class IncomingHandler extends Handler {
        private WeakReference<VoiceService> mtarget;

        IncomingHandler(VoiceService target) {
            mtarget = new WeakReference<VoiceService>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            final VoiceService target = mtarget.get();

            switch (msg.what) {
                case MSG_RECOGNIZER_START_LISTENING:

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        // turn off beep sound
                        if (!mIsStreamSolo) {
                            //mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, true);
                            //mAudioManager.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                            //mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
                            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0); // setting system volume to zero, muting
                            mIsStreamSolo = true;
                        }
                    }
                    if (!target.mIsListening) {
                        target.mSpeechRecognizer.startListening(target.mSpeechRecognizerIntent);
                        target.mIsListening = true;
                        Log.d(TAG, "handle message: start listening");
                    }
                    break;

                case MSG_RECOGNIZER_CANCEL:
                    if (mIsStreamSolo) {
                        //mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, false);
                        //mAudioManager.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                        //mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mStreamVolume, 0);
                        mIsStreamSolo = false;
                    }
                    target.mSpeechRecognizer.cancel();
                    target.mIsListening = false;
                    Log.d(TAG, "handle message: canceled recognizer");
                    break;

                case  MSG_RECOGNIZER_RESTART:
                    Log.d(TAG, "handle message: restart recognizer");
                    target.mSpeechRecognizer.destroy();
                    target.mIsListening = false;
                    target.initSpeechRecognizer();

            }
        }
    }

    // Count down timer for Jelly Bean work around
    protected CountDownTimer mNoSpeechCountDown = new CountDownTimer(5000, 1000) {

        @Override
        public void onTick(long millisUntilFinished) {
            Log.d(TAG, "onTick(), time up in "+millisUntilFinished+ "ms");
        }

        @Override
        public void onFinish() {
            Log.d(TAG, "CountDownTimer onFinish()");
            mIsCountDownOn = false;
            //Message message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
            Message message = Message.obtain(null, MSG_RECOGNIZER_RESTART);
            try {
                mServerMessenger.send(message);
                Log.d(TAG, "sent message: MSG_RECOGNIZER_RESTART");
                message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
                mServerMessenger.send(message);
                Log.d(TAG, "sent message: MSG_RECOGNIZER_START_LISTENING");

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    protected class SpeechRecognitionListener implements RecognitionListener {

        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech()");
            // speech input will be processed, so there is no need for count down anymore
            if (mIsCountDownOn) {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }
        }

        @Override
        public void onBufferReceived(byte[] buffer) {

        }

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech");

        }

        @Override
        public void onError(int error) {
            String errMsgStr;
            switch(error) {
                case SpeechRecognizer.ERROR_CLIENT:
                    errMsgStr = "Other client side errors";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    errMsgStr = "Insufficient permissions";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    errMsgStr = "No recognition result matched";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    errMsgStr = "No speech input";
                    break;
                case SpeechRecognizer.ERROR_AUDIO:
                    errMsgStr = "Audio recording error";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    errMsgStr = "Other network related errors";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    errMsgStr = "RecognitionService busy";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    errMsgStr = "Server sends error status";
                    break;
                default:
                    errMsgStr = "error code:"+error;

            }

            Log.d(TAG, "onError():"+errMsgStr);
            if (mIsCountDownOn) {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }

            //when there is an error, also sent a message to activity
            sendResult(RECOGNIZER_ERROR, "oov");
            mIsListening = false;
            //Message message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
            Message message = Message.obtain(null, MSG_RECOGNIZER_RESTART);
            try {
                mServerMessenger.send(message);
                Log.d(TAG, "sent message: MSG_RECOGNIZER_RESTART");
                message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
                mServerMessenger.send(message);
                Log.d(TAG, "sent message: MSG_RECOGNIZER_START_LISTENING");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }

        @Override
        public void onPartialResults(Bundle partialResults) {

        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mIsCountDownOn = true;
                mNoSpeechCountDown.start();

            }
            Log.d(TAG, "onReadyForSpeech(), new timer started.");
        }

        @Override
        public void onResults(Bundle results) {
            Log.d(TAG, "onResults()");
            ArrayList<String> resultList = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            String [] speechWordArray = resultList.get(0).toLowerCase().split("\\s+");
            String emoji = emojiModel.predict(speechWordArray);
            Log.d(TAG, "ASR result = " + resultList.get(0) + " , predicted emoji="+emoji);
            sendResult(VOICE_RESULT_READY, emoji);

            Message message;
            try {
                if (mIsCountDownOn){
                    mIsCountDownOn = false;
                    mNoSpeechCountDown.cancel();
                }

                message = Message.obtain(null, MSG_RECOGNIZER_RESTART);
                mServerMessenger.send(message);
                Log.d(TAG, "sent message: MSG_RECOGNIZER_RESTART");
                message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
                mServerMessenger.send(message);
                Log.d(TAG, "sent message: MSG_RECOGNIZER_START_LISTENING");

            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onRmsChanged(float rmsdB) {
            //if(rmsdB>2.0)
              //Log.d(TAG, "onRmsChanged="+rmsdB);
        }
    } //SpeechRecognitionListener

    private void initSpeechRecognizer(){
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());
    }

    public void startListening(){
        try
        {
            Message msg = new Message();
            msg.what = MSG_RECOGNIZER_START_LISTENING;
            mServerMessenger.send(msg);

            sendResult(MODEL_READY, null);
        }
        catch (RemoteException e)
        {

        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate(): " + VoiceService.class.getName());
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mStreamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC); // getting system volume into var for later un-muting
        initSpeechRecognizer();
        mLocalBroadcaster = LocalBroadcastManager.getInstance(this);
        try {
            InputStream is = getAssets().open("model.m");
            emojiModel = new TFIDFEmojiModel(this);
            Log.d(TAG, "spawn async task to load model...");
            emojiModel.execute(is);
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        //Log.d(TAG, "onBind(): " + VoiceService.class.getName());
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand(): " + VoiceService.class.getName());
        //startListening();
        return  START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mIsCountDownOn) {
            mNoSpeechCountDown.cancel();
        }
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.destroy();
        }
    }

    public void sendResult(String type, String msg) {
        Intent intent = new Intent(type);
        if(msg != null)
            intent.putExtra(type, msg);
        mLocalBroadcaster.sendBroadcast(intent);
    }

}
