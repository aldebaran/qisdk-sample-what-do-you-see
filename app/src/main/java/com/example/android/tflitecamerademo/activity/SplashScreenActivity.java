/*
 * Copyright (C) 2018 SoftBank Robotics Europe
 * See COPYING for the license
 */
package com.example.android.tflitecamerademo.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.ImageView;

import com.airbnb.lottie.LottieAnimationView;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.design.activity.conversationstatus.SpeechBarDisplayStrategy;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.softbankrobotics.sample.whatdoyousee.R;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SplashScreenActivity extends RobotActivity implements RobotLifecycleCallbacks {
    private static final String TAG = "SplashScreenActivity";

    @BindView(R.id.img_cross)
    ImageView imgCross;
    @BindView(R.id.lottie_loader)
    LottieAnimationView animationView;

    //region Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setSpeechBarDisplayStrategy(SpeechBarDisplayStrategy.OVERLAY);

        setContentView(R.layout.activity_splashscreen);
        ButterKnife.bind(this);

        checkLanguage();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        checkLanguage();
    }

    @Override
    protected void onDestroy() {
        QiSDK.unregister(this);
        super.onDestroy();
    }
    //endregion

    /**
     * Check the language, if it's english go to Introduction Activity if
     * not show the language settings to switch
     */
    private void checkLanguage() {
        if (!Locale.getDefault().getDisplayLanguage().equals(getString(R.string.language))) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.wrong_language_title_dialog)
                    .setMessage(R.string.wrong_language_message_dialog)
                    .setPositiveButton(R.string.setting_button_dialog, (dialog, which) -> {
                        startActivityForResult(new Intent(Settings.ACTION_LOCALE_SETTINGS), 0);
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {
            QiSDK.register(this, this);
        }
    }


    /**
     * Show the Introduction's Activity and finish this one
     */
    private void goToIntroduction() {
        Intent mainIntent = new Intent(SplashScreenActivity.this, IntroductionActivity.class);
        SplashScreenActivity.this.startActivity(mainIntent);
        SplashScreenActivity.this.finish();
    }

    /**
     * Close the app
     */
    @OnClick(R.id.img_cross)
    public void onViewClicked() {
        this.finish();
    }

    //region RobotCallback

    /**
     * Say when it's ready and show the introduction's Activity
     *
     * @param qiContext the current {@link QiContext} of the robot
     */
    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        Say say = SayBuilder.with(qiContext)
                .withResource(R.string.ready_speak_text)
                .build();

        say.run();

        runOnUiThread(() -> YoYo.with(Techniques.FadeOut).onEnd(animator -> goToIntroduction()).playOn(animationView));
    }

    @Override
    public void onRobotFocusLost() {
        //NOT USED IN THIS CASE
    }

    @Override
    public void onRobotFocusRefused(String reason) {
        //NOT USED IN THIS CASE
        Log.e(TAG, "onRobotFocusRefused: " + reason);
    }
    //endregion
}
