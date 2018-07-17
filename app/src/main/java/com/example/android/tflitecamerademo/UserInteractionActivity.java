package com.example.android.tflitecamerademo;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.aldebaran.qi.Consumer;
import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.Qi;
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
                setupRobotToBriefing();
            }
        };

    }

    @Override
    protected void onDestroy() {
        QiSDK.unregister(this);
        super.onDestroy();
    }
    //endregion

    private void setupRobotToBriefing() {
        if (this.qiContext == null)
            return;

        Say say = SayBuilder.with(this.qiContext)
                .withResource(R.string.briefing_speak_text)
                .build();

        say.run();

        setupRobotToCallToAction();
    }

    private void setupRobotToCallToAction() {
        if (qiContext == null)
            return;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnSee.setVisibility(View.VISIBLE);
            }
        });

        Say say = SayBuilder.with(this.qiContext)
                .withResource(R.string.call_to_action_speak_text)
                .build();
        say.run();
        player.start();
    }

    private void scanObject() {
    }

    private void catchInput() {
    }

    //region onClick
    @OnClick(R.id.img_cross)
    public void onImgCrossClicked() {
        this.finish();
    }

    @OnClick(R.id.btn_see)
    public void onBtnSeeClicked() {

    }

    @OnClick(R.id.img_home)
    public void onViewClicked() {
        QiSDK.register(this, this);
    }
    //endregion

    //region Robot Callback
    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        this.qiContext = qiContext;
        setupRobotToBriefing();
    }

    @Override
    public void onRobotFocusLost() {
        this.qiContext = null;
    }

    @Override
    public void onRobotFocusRefused(String reason) {
        //NOT USED IN THIS CASE
        Log.e(TAG, "onRobotFocusRefused: " + reason);
    }
    //endregion
}
