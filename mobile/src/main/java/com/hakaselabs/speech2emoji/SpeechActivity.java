package com.hakaselabs.speech2emoji;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.app.ActionBarActivity;
import com.hakaselabs.speech2emoji.util.SystemUiHider;
import java.util.Locale;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class SpeechActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
//    private SystemUiHider mSystemUiHider;
    private Intent serviceIntent;
    private BroadcastReceiver mLocalBroadcastReceiver;
    private int width;
    private int height;
    private String result;
    private TextView emojiTextView;
    private View controlsView;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private Button gotoSettingButton;
    private RadioButton languageCNButton;
    private RadioButton languageENButton;
    private int drawableRes=0;
    private Locale selectedLocale = Locale.getDefault();

    public static String TAG = SpeechActivity.class.getSimpleName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        drawerLayout = (DrawerLayout)findViewById(R.id.drawerLayout);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                        R.string.drawer_open, R.string.drawer_close) {
            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                //getActionBar().setTitle("setting?");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //getActionBar().setTitle("setting2?");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        this.languageCNButton = (RadioButton)findViewById(R.id.button_language_cn);
        this.languageENButton = (RadioButton)findViewById(R.id.button_language_en);
        RadioGroup rGroup = (RadioGroup)findViewById(R.id.radio_group);



        if(Locale.US.equals(Locale.getDefault())) {
            drawableRes = R.drawable.us;
            this.languageENButton.setChecked(true);
        }else if(Locale.UK.equals(Locale.getDefault())) {
            drawableRes = R.drawable.gb;
            this.languageENButton.setChecked(true);
        }else if (Locale.CHINA.equals(Locale.getDefault())) {
            this.languageCNButton.setChecked(true);

        }

        this.languageCNButton.setText(Locale.CHINA.getDisplayLanguage(Locale.CHINA));
        this.languageCNButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.cn, 0);

        this.languageENButton.setText(Locale.getDefault().getDisplayLanguage());
        this.languageENButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, drawableRes, 0);

        rGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            public void onCheckedChanged(RadioGroup rGroup, int checkedId)
            {
                //rGroup.getCheckedRadioButtonId();
                switch(checkedId){
                    case R.id.button_language_cn: selectedLocale = Locale.CHINA;break;
                    case R.id.button_language_en:
                        selectedLocale = (drawableRes == R.drawable.gb) ?  Locale.UK : Locale.US;
                        break;
                }
                //restart service?
                stopService(serviceIntent);
                serviceIntent.putExtra("LANG", selectedLocale.getLanguage());
                startService(serviceIntent);
            }
        });

        // Set the drawer toggle as the DrawerListener
        drawerLayout.setDrawerListener(mDrawerToggle);

        //controlsView = findViewById(R.id.fullscreen_content_controls);
        emojiTextView = (TextView) findViewById(R.id.emoji_textview);
        final Button exitButton = (Button) findViewById(R.id.exit_button);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        this.width = size.x;
        this.height = size.y;

        serviceIntent = new Intent(this.getApplicationContext(), VoiceService.class);

        emojiTextView.setText("Summoning emoji lord...");
        emojiTextView.setTextSize(18);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        /*
        mSystemUiHider = SystemUiHider.getInstance(this, emojiTextView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });
        */
        // Set up the user interaction to manually show or hide the system UI.
        /*
        emojiTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick(), view="+view);
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });
        */
        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.

        //exitButton.setOnTouchListener(mDelayHideTouchListener);

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "exit button onClicked,");
                Toast.makeText(SpeechActivity.this, "exit", Toast.LENGTH_SHORT).show();
                stopService(serviceIntent);
                finish();
            }

        });
        mLocalBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == VoiceService.MODEL_READY){
                    emojiTextView.setText("Speak now");
                }else if (action == VoiceService.VOICE_RESULT_READY) {
                    result = intent.getStringExtra(VoiceService.VOICE_RESULT_READY);
                    int fontSize = Math.min(width, height) /10;
                    emojiTextView.setTextSize(fontSize);
                    emojiTextView.setText(result);
                }

            }
        };
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        //delayedHide(100);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "On Start .....");
        if (this.hasInternetConnection()) {

            IntentFilter filter = new IntentFilter(VoiceService.VOICE_RESULT_READY);
            filter.addAction(VoiceService.MODEL_READY);
            LocalBroadcastManager.getInstance(this).registerReceiver((mLocalBroadcastReceiver),
                    filter);
            serviceIntent.putExtra("LANG", this.selectedLocale.getLanguage());
            startService(serviceIntent);
        }else{
            this.redirectToSettings();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //this.systemLocale = newConfig.locale;
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    /*
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };
    */
    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.

    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalBroadcastReceiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle){
        super.onSaveInstanceState(bundle);

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(serviceIntent);
    }
    */
    private boolean hasInternetConnection() {
        //check if device has internet access
        ConnectivityManager cm =
                (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return  activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    private void redirectToSettings(){
        this.emojiTextView.setText("\u26A0\n Network unavailable, have you enabled network access?");
        LinearLayout layout = (LinearLayout) findViewById(R.id.main_listview);
        if(gotoSettingButton != null){
            layout.removeView(gotoSettingButton);
        }else {
            gotoSettingButton = new Button(this);
        }

        gotoSettingButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        gotoSettingButton.setText("Go to settings");
        layout.addView(gotoSettingButton);
        gotoSettingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "goto setting button clicked.");
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)); //ACTION_WIRELESS_SETTINGS
            }
        });
    }
}
