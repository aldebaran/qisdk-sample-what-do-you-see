package com.example.android.tflitecamerademo;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.ChatBuilder;
import com.aldebaran.qi.sdk.builder.HolderBuilder;
import com.aldebaran.qi.sdk.builder.QiChatbotBuilder;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.builder.TopicBuilder;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.conversation.Chat;
import com.aldebaran.qi.sdk.object.conversation.QiChatbot;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.aldebaran.qi.sdk.object.holder.AutonomousAbilitiesType;
import com.aldebaran.qi.sdk.object.holder.Holder;
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
    int countError = 0;
    MediaPlayer player;
    Chat pepperChat;
    Future<Void> futureChat;
    QiContext qiContext;
    Holder pepperHolder;

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
        releaseRobot();
        super.onDestroy();
    }
    //endregion

    private void setupRobotToBriefing() {
        if (this.qiContext == null)
            return;

        pepperHolder = HolderBuilder.with(qiContext)
                .withAutonomousAbilities(AutonomousAbilitiesType.BACKGROUND_MOVEMENT)
                .build();

        pepperHolder.hold();

        runOnUiThread(() -> btnSee.setVisibility(View.GONE));

        Say say = SayBuilder.with(this.qiContext)
                .withResource(R.string.briefing_speak_text)
                .build();
        say.run();

        //TODO set animation to pepper arms

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

        prepareChatBot();

    }

    private void prepareChatBot() {
        if (qiContext == null)
            return;

        QiChatbot qiChatbot =
                QiChatbotBuilder
                        .with(qiContext)
                        .withTopic(TopicBuilder
                                .with(qiContext)
                                .withResource(R.raw.see)
                                .build())
                        .build();

        pepperChat = ChatBuilder.with(qiContext)
                .withChatbot(qiChatbot)
                .build();


        futureChat = pepperChat.async().run();

        pepperChat.addOnHeardListener(heardPhrase -> {
            timer.cancel();
        });

        pepperChat.addOnFallbackReplyFoundForListener(input -> {
            countError++;
            if (countError > 3) {
                futureChat.requestCancellation();
                onViewClicked();
            }
        });

        qiChatbot.addOnEndedListener(endReason -> futureChat.requestCancellation());
    }

    private void scanObject() {
        timer.cancel();

    }

    private void catchInput() {
    }

    private void releaseRobot() {
        if (futureChat != null
                && !futureChat.isCancelled()) {
            futureChat.requestCancellation();
        }
        if (pepperChat != null) {
            pepperChat.removeAllOnStartedListeners();
        }
        if (pepperHolder != null) {
            pepperHolder.async().release();
        }
        timer.cancel();
        QiSDK.unregister(this);
    }

    //region onClick
    @OnClick(R.id.img_cross)
    public void onImgCrossClicked() {
        pepperHolder.async().release();
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
        releaseRobot();
        qiContext = null;
    }

    @Override
    public void onRobotFocusRefused(String reason) {
        //NOT USED IN THIS CASE
        Log.e(TAG, "onRobotFocusRefused: " + reason);
    }
    //endregion
}
