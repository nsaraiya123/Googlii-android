package com.googlii.application;

import android.app.Application;

import com.facebook.FacebookSdk;
import com.parse.Parse;
import com.parse.ParseFacebookUtils;

/**
 * Created by niravsaraiya on 10/12/15.
 */
public class GoogliiAplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Required - Initialize the Parse SDK
        Parse.initialize(this);

        Parse.setLogLevel(Parse.LOG_LEVEL_DEBUG);

        FacebookSdk.sdkInitialize(getApplicationContext());

        ParseFacebookUtils.initialize(this);
    }
}
