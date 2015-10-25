package com.hakaselabs.speech2emoji;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;


/**
 * Created by wei on 22/10/15.
 */
public class VoiceService extends Service {
    public static String TAG = VoiceService.class.getName();
    public static final String VOICE_MESSAGE = VoiceService.class.getName();
    protected static AudioManager mAudioManager;
    protected SpeechRecognizer mSpeechRecognizer;
    protected Intent mSpeechRecognizerIntent;
    protected final Messenger mServerMessenger = new Messenger(new IncomingHandler(this));

    protected boolean mIsListening;
    protected volatile boolean mIsCountDownOn;
    private static boolean mIsStreamSolo;

    static final int MSG_RECOGNIZER_START_LISTENING = 1;
    static final int MSG_RECOGNIZER_CANCEL = 2;

    // Binder given to clients
    //private final IBinder mBinder = new LocalBinder();
    private LocalBroadcastManager mLocalBroadcaster ;
    private String mSpeechResultString ;

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.

    public class LocalBinder extends Binder {
        VoiceService getService() {
            // Return this instance of LocalService so clients can call public methods
            return VoiceService.this;
        }
    }
     */
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
                            mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, true);
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
                        mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, false);
                        mIsStreamSolo = false;
                    }
                    target.mSpeechRecognizer.cancel();
                    target.mIsListening = false;
                    Log.d(TAG, "handle message: canceled recognizer");
                    break;
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
            Message message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
            try {
                mServerMessenger.send(message);
                Log.d(TAG, "sent message: MSG_RECOGNIZER_CANCEL");
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
            mIsListening = false;
            Message message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
            try {
                mServerMessenger.send(message);
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
            Log.d(TAG, "ASR result = " + resultList.get(0));
            sendResult(resultList.get(0));

            Message message;
            try {
                if (mIsCountDownOn){
                    mIsCountDownOn = false;
                    mNoSpeechCountDown.cancel();
                }

                message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
                mServerMessenger.send(message);
                Log.d(TAG, "sent message: MSG_RECOGNIZER_CANCEL");
                message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
                mServerMessenger.send(message);
                Log.d(TAG, "sent message: MSG_RECOGNIZER_START_LISTENING");

            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onRmsChanged(float rmsdB) {
            if(rmsdB>2.0)
              Log.d(TAG, "onRmsChanged="+rmsdB);
        }
    } //SpeechRecognitionListener

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate(): " + VoiceService.class.getName());
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());
        mSpeechResultString = "hello";
        mLocalBroadcaster = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        //Log.d(TAG, "onBind(): " + VoiceService.class.getName());
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand(): " + VoiceService.class.getName());
        try
        {
            Message msg = new Message();
            msg.what = MSG_RECOGNIZER_START_LISTENING;
            mServerMessenger.send(msg);
        }
        catch (RemoteException e)
        {

        }
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

    public void sendResult(String message) {
        Intent intent = new Intent(VOICE_MESSAGE);
        if(message != null)
            intent.putExtra(VOICE_MESSAGE, message);
        mLocalBroadcaster.sendBroadcast(intent);
    }

}
