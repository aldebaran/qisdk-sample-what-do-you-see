package com.example.android.tflitecamerademo;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.softbankrobotics.sample.whatdoyousee.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class UserInteractionActivity extends RobotActivity implements RobotLifecycleCallbacks {
    private static final String TAG = "UserInteractionActivity";

    @BindView(R.id.img_cross)
    ImageView imgCross;
    @BindView(R.id.txt_question_mark)
    TextView txtQuestionMark;
    @BindView(R.id.img_pepper)
    ImageView imgPepper;
    @BindView(R.id.btn_see)
    TextView btnSee;
    @BindView(R.id.img_home)
    ImageView imgHome;

    CountDownTimer timer;
    MediaPlayer player;
    QiContext qiContext;

    //region Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_user_interaction);
        ButterKnife.bind(this);
        QiSDK.register(this, this);

        player = MediaPlayer.create(this, R.raw.mariocoin);
        timer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                onViewClicked();
            }
        };

    }

    @Override
    protected void onDestroy() {
        QiSDK.unregister(this);
        timer.cancel();
        super.onDestroy();
    }
    //endregion

    private void setupRobotToBriefing() {
        if (this.qiContext == null)
            return;

        runOnUiThread(() -> btnSee.setVisibility(View.GONE));

        Say say = SayBuilder.with(this.qiContext)
                .withResource(R.string.briefing_speak_text)
                .build();
        say.run();

        setupRobotToCallToAction();
    }

    private void setupRobotToCallToAction() {
        if (qiContext == null)
            return;

        runOnUiThread(() -> btnSee.setVisibility(View.VISIBLE));

        Say say = SayBuilder.with(this.qiContext)
                .withResource(R.string.call_to_action_speak_text)
                .build();
        say.run();

        player.start();
        timer.start();
    }

    private void scanObject() {
        timer.cancel();

    }

    private void catchInput() {
    }

    //region onClick
    @OnClick(R.id.img_cross)
    public void onImgCrossClicked() {
        timer.cancel();
        finish();
    }

    @OnClick(R.id.btn_see)
    public void onBtnSeeClicked() {
        timer.cancel();
    }

    @OnClick(R.id.img_home)
    public void onViewClicked() {
        timer.cancel();
        QiSDK.register(this, this);
    }
    //endregion

    //region Robot Callback
    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        timer.cancel();
        this.qiContext = qiContext;
        setupRobotToBriefing();
    }

    @Override
    public void onRobotFocusLost() {
        timer.cancel();
        qiContext = null;
    }

    @Override
    public void onRobotFocusRefused(String reason) {
        //NOT USED IN THIS CASE
        Log.e(TAG, "onRobotFocusRefused: " + reason);
    }
    //endregion
}
