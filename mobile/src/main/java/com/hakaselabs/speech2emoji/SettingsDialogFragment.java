package com.hakaselabs.speech2emoji;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import java.util.Locale;


public class SettingsDialogFragment extends DialogFragment {

    private Locale selectedLocale = Locale.getDefault();
    private int selectedIndex = 0;
    private Locale[] locales = {Locale.US, Locale.UK, Locale.CHINA};

    OnLocaleChangedListener mCallback;
    // Container Activity must implement this interface
    public interface OnLocaleChangedListener {
        public void onLocaleChanged(Locale locale , int which);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.language_setting);
        int style = DialogFragment.STYLE_NORMAL, theme = 0;
        theme = android.R.style.Theme_DeviceDefault_Dialog;
        setStyle(style, theme);

        String[] languages = new String[locales.length];
        for (int i=0 ; i < locales.length; i++){
            languages[i] = locales[i].getDisplayLanguage(locales[i])+"("+ locales[i].getDisplayCountry(locales[i])+")";
            if (locales[i].equals(selectedLocale)){
                selectedIndex = i;
            }
        }

        builder.setSingleChoiceItems(languages,selectedIndex, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedLocale = locales[which];
            }
        });

        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCallback.onLocaleChanged(selectedLocale, which);
                dismiss();
            }
        });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnLocaleChangedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnLocaleChangedListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public void setSelectedIndex(int i){
        this.selectedIndex = i;
    }
}
