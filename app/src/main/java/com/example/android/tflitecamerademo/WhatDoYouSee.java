package com.example.android.tflitecamerademo;

import android.app.Application;

import com.softbankrobotics.sample.whatdoyousee.R;

import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;

public class WhatDoYouSee extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ViewPump.init(ViewPump.builder()
                .addInterceptor(new CalligraphyInterceptor(
                        new CalligraphyConfig.Builder()
                                .setDefaultFontPath("fonts/Dosis-Bold.ttf")
                                .setFontAttrId(R.attr.fontPath)
                                .build()))
                .build());

    }
}
