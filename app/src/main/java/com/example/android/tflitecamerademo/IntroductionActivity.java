package com.example.android.tflitecamerademo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

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

public class IntroductionActivity extends RobotActivity implements RobotLifecycleCallbacks {
    private static final String TAG = "IntroductionActivity";

    @BindView(R.id.img_cross)
    ImageView imgCross;

    //region Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_intro);
        ButterKnife.bind(this);

        QiSDK.register(this, this);
    }

    @Override
    protected void onDestroy() {
        QiSDK.unregister(this);
        super.onDestroy();
    }
    //endregion

    @OnClick(R.id.img_cross)
    public void onViewClicked() {
        this.finish();
    }

    private void goToUserInteraction() {
        Intent mainIntent = new Intent(IntroductionActivity.this, UserInteractionActivity.class);
        IntroductionActivity.this.startActivity(mainIntent);
        IntroductionActivity.this.finish();
    }

    //region RobotCallback
    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        Say say = SayBuilder.with(qiContext)
                .withResource(R.string.intro_speak_text)
                .build();
        say.run();

        goToUserInteraction();
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
