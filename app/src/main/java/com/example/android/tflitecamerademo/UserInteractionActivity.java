package com.example.android.tflitecamerademo;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.aldebaran.qi.Consumer;
import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.ChatBuilder;
import com.aldebaran.qi.sdk.builder.HolderBuilder;
import com.aldebaran.qi.sdk.builder.QiChatbotBuilder;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.builder.TopicBuilder;
import com.aldebaran.qi.sdk.core.QiThreadPool;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.conversation.BodyLanguageOption;
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

    int countError = 0;

    CountDownTimer timer;
    MediaPlayer player;

    QiContext qiContext;
    Holder pepperHolder;

    QiChatbot qiChatbot;
    Chat pepperChat;
    Future<Void> futureChat;

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
                stopChat(() -> goToBriefing());
            }
        };
    }

    @Override
    protected void onDestroy() {
        releaseRobot();
        super.onDestroy();
    }
    //endregion

    //region RobotCleaning
    private void releaseRobot() {
        timer.cancel();

        if (pepperHolder != null) {
            pepperHolder.async().release();
        }

        stopChat(null);

        unregisterListener();

        QiSDK.unregister(this);
    }

    private void stopChat(Runnable runnable) {
        if (futureChat != null
                && !futureChat.isCancelled()
                && !futureChat.isDone()) {
            futureChat.thenConsume(voidFuture -> {
                if (voidFuture.isCancelled()
                        || voidFuture.hasError())
                    if (runnable != null)
                        runnable.run();
            });
            futureChat.requestCancellation();
        } else {
            if (runnable != null)
                runnable.run();
        }
    }

    private void unregisterListener() {
        if (pepperChat != null) {
            pepperChat.removeAllOnStartedListeners();
            pepperChat.removeAllOnHeardListeners();
            pepperChat.removeAllOnFallbackReplyFoundForListeners();
        }
        if (qiChatbot != null) {
            qiChatbot.removeAllOnEndedListeners();
        }
    }
    //endregion

    //region SetupRobot
    private void freezeRobot() {
        //TODO set animation to pepper arms

        pepperHolder = HolderBuilder.with(qiContext)
                .withAutonomousAbilities(AutonomousAbilitiesType.BACKGROUND_MOVEMENT)
                .build();

        pepperHolder.hold();
        qiChatbot.setSpeakingBodyLanguage(BodyLanguageOption.DISABLED);
    }

    private void prepareChatBot() {
        qiChatbot = QiChatbotBuilder
                .with(qiContext)
                .withTopic(TopicBuilder
                        .with(qiContext)
                        .withResource(R.raw.see)
                        .build())
                .build();
    }

    private void setupRobotToBriefing() {
        if (this.qiContext == null)
            return;

        prepareChatBot();

        freezeRobot();

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

        setupChatBot();
    }

    private void setupChatBot() {
        if (qiContext == null)
            return;

        if (qiChatbot == null)
            prepareChatBot();

        pepperChat.addOnHeardListener(heardPhrase -> timer.cancel());

        pepperChat.addOnFallbackReplyFoundForListener(input -> {
            countError++;
            if (countError > 3) {
                countError = 0;
                stopChat(this::goToBriefing);
            }
        });

        qiChatbot.addOnEndedListener(endReason -> stopChat(this::scanObject));
        qiChatbot.addOnEndedListener(endReason -> futureChat.requestCancellation());

        pepperChat = ChatBuilder.with(qiContext)
                .withChatbot(qiChatbot)
                .build();

        futureChat = pepperChat.async().run();

    }
    //endregion

    //region TensorFlow
    private void scanObject() {

    }
    //endregion

    //region onClick
    @OnClick(R.id.img_cross)
    public void onImgCrossClicked() {
        timer.cancel();
        releaseRobot();
        finish();
    }

    @OnClick(R.id.btn_see)
    public void onBtnSeeClicked() {
        timer.cancel();
    }

    @OnClick(R.id.img_home)
    public void onViewClicked() {
        goToBriefing();
    }

    private void goToBriefing() {
        QiThreadPool.run(() -> {
            timer.cancel();
            setupRobotToBriefing();
        });
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