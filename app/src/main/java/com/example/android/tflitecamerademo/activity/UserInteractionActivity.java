package com.example.android.tflitecamerademo.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.ColorRes;
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
import com.aldebaran.qi.sdk.builder.QiChatbotBuilder;
import com.aldebaran.qi.sdk.builder.TakePictureBuilder;
import com.aldebaran.qi.sdk.builder.TopicBuilder;
import com.aldebaran.qi.sdk.core.QiThreadPool;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.camera.TakePicture;
import com.aldebaran.qi.sdk.object.conversation.Bookmark;
import com.aldebaran.qi.sdk.object.conversation.Chat;
import com.aldebaran.qi.sdk.object.conversation.Phrase;
import com.aldebaran.qi.sdk.object.conversation.QiChatbot;
import com.aldebaran.qi.sdk.object.conversation.Topic;
import com.aldebaran.qi.sdk.object.holder.Holder;
import com.aldebaran.qi.sdk.object.image.EncodedImage;
import com.aldebaran.qi.sdk.object.image.EncodedImageHandle;
import com.aldebaran.qi.sdk.object.image.TimestampedImageHandle;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.example.android.tflitecamerademo.RobotUtils;
import com.example.android.tflitecamerademo.tf.Classifier;
import com.example.android.tflitecamerademo.tf.ImageClassifier;
import com.example.android.tflitecamerademo.tf.Utils;
import com.softbankrobotics.sample.whatdoyousee.R;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.example.android.tflitecamerademo.tf.Constant.IMAGE_MEAN;
import static com.example.android.tflitecamerademo.tf.Constant.IMAGE_STD;
import static com.example.android.tflitecamerademo.tf.Constant.INPUT_NAME;
import static com.example.android.tflitecamerademo.tf.Constant.INPUT_SIZE;
import static com.example.android.tflitecamerademo.tf.Constant.LABEL_FILE;
import static com.example.android.tflitecamerademo.tf.Constant.MODEL_FILE;
import static com.example.android.tflitecamerademo.tf.Constant.OUTPUT_NAME;

public class UserInteractionActivity extends RobotActivity implements RobotLifecycleCallbacks, Chat.OnHeardListener, QiChatbot.OnBookmarkReachedListener {
    private static final String TAG = "UserInteractionActivity";

    @BindView(R.id.img_question)
    ImageView imgQuestion;
    @BindView(R.id.img_pepper)
    ImageView imgPepper;
    @BindView(R.id.img_warning)
    ImageView imgWarning;
    @BindView(R.id.img_valid)
    ImageView imgValid;
    @BindView(R.id.btn_see)
    TextView btnSee;
    @BindView(R.id.flash_ctn)
    ImageView flashCtn;
    @BindView(R.id.img_result)
    ImageView imgResult;
    @BindView(R.id.btn_again)
    TextView btnAgain;
    @BindView(R.id.img_cross)
    ImageView imgCross;
    @BindView(R.id.img_home)
    ImageView imgHome;

    int countError = 0;
    AtomicBoolean isScanning = new AtomicBoolean(false);
    CountDownTimer timer;

    MediaPlayer playerStart;
    MediaPlayer playerFlash;
    MediaPlayer playerSuccess;

    QiContext qiContext;
    Holder pepperHolder;

    QiChatbot qiChatbot;
    Chat chat;
    Topic topic;
    Map<String, Bookmark> bookmarks;
    Future<Void> futureChat;
    Future<TakePicture> futurePicture;
    Classifier.Recognition heightRecognition;

    //region Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_user_interaction);

        ButterKnife.bind(this);
        QiSDK.register(this, this);

        playerStart = MediaPlayer.create(this, R.raw.ready_sound);
        playerFlash = MediaPlayer.create(this, R.raw.automatic_camera);
        playerSuccess = MediaPlayer.create(this, R.raw.success_sound);

        timer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                runBriefing();
            }
        };
    }

    @Override
    protected void onDestroy() {
        QiThreadPool.run(() ->
                RobotUtils.releaseRobot(
                        getParent(),
                        pepperHolder,
                        qiChatbot,
                        chat,
                        futureChat,
                        futurePicture,
                        null));
        super.onDestroy();
    }
    //endregion

    //region Flow
    private void setupRobot() {
        createChatBot();

        pepperHolder = RobotUtils.freezeRobot(qiContext, pepperHolder, qiChatbot, chat);

        futurePicture = TakePictureBuilder.with(qiContext).buildAsync();

        runBriefing();
    }

    private void createChatBot() {
        if (qiContext == null)
            return;

        topic = TopicBuilder
                .with(qiContext)
                .withResource(R.raw.see)
                .build();

        bookmarks = topic.getBookmarks();

        qiChatbot = QiChatbotBuilder
                .with(qiContext)
                .withTopic(topic)
                .build();

        chat = ChatBuilder.with(qiContext)
                .withChatbot(qiChatbot)
                .build();

        chat.addOnHeardListener(this);
        qiChatbot.addOnBookmarkReachedListener(this);
    }

    private void runBriefing() {
        if (this.qiContext == null)
            return;

        runOnUiThread(this::layoutBriefing);

        RobotUtils.goToBookmark(qiChatbot, bookmarks, "briefing");

        if (futureChat == null
                || futureChat.isCancelled())
            futureChat = chat.async().run();
    }

    private void runCallToAction() {
        if (qiContext == null)
            return;

        RobotUtils.goToBookmark(qiChatbot, bookmarks, "callToAction").andThenConsume(aVoid -> runOnUiThread(() -> {
            playerStart.start();
            timer.start();
            layoutCallToAction();
        }));
    }
    //endregion

    //regionLayout
    private void layoutBriefing() {
        isScanning.set(false);

        colorTopButtons(android.R.color.black);
        imgQuestion.setVisibility(View.VISIBLE);
        imgPepper.setVisibility(View.VISIBLE);
        imgWarning.setVisibility(View.GONE);
        imgValid.setVisibility(View.GONE);
        btnSee.setVisibility(View.GONE);
        flashCtn.setVisibility(View.GONE);
        imgResult.setVisibility(View.GONE);
        btnAgain.setVisibility(View.GONE);
        imgHome.setVisibility(View.GONE);

        btnSee.setEnabled(true);
        btnAgain.setEnabled(true);
    }

    private void layoutCallToAction() {
        isScanning.set(false);

        colorTopButtons(android.R.color.black);
        imgQuestion.setVisibility(View.VISIBLE);
        imgPepper.setVisibility(View.VISIBLE);
        imgWarning.setVisibility(View.GONE);
        imgValid.setVisibility(View.GONE);
        flashCtn.setVisibility(View.GONE);
        imgResult.setVisibility(View.GONE);
        btnAgain.setVisibility(View.GONE);
        imgHome.setVisibility(View.VISIBLE);
        YoYo.with(Techniques.SlideInUp)
                .duration(500)
                .onStart(animator -> btnSee.setVisibility(View.VISIBLE))
                .playOn(btnSee);

        btnSee.setEnabled(true);
        btnAgain.setEnabled(true);
    }

    private void layoutScan() {
        isScanning.set(true);
        pepperHolder.hold();

        runOnUiThread(() -> {
            layoutCaptureMove(false);
            playerFlash.setOnCompletionListener(mp -> {
                layoutCaptureMove(true);
                YoYo.with(Techniques.Flash)
                        .duration(50)
                        .onStart(animator -> flashCtn.setVisibility(View.VISIBLE))
                        .onEnd(animator -> flashCtn.setVisibility(View.GONE))
                        .playOn(flashCtn);
            });
            playerFlash.start();
        });
    }

    private void layoutCaptureMove(boolean showValid) {
        colorTopButtons(android.R.color.black);
        imgQuestion.setVisibility(View.INVISIBLE);
        imgPepper.setVisibility(View.VISIBLE);
        btnSee.setVisibility(View.GONE);
        flashCtn.setVisibility(View.GONE);
        imgResult.setVisibility(View.GONE);
        btnAgain.setVisibility(View.GONE);
        imgHome.setVisibility(View.VISIBLE);
        imgWarning.setVisibility((showValid) ? View.GONE : View.VISIBLE);
        imgValid.setVisibility((showValid) ? View.VISIBLE : View.GONE);

        btnSee.setEnabled(false);
    }

    private void layoutResult(Bitmap bitmap) {
        isScanning.set(false);

        colorTopButtons(android.R.color.white);
        imgResult.setImageBitmap(bitmap);
        YoYo.with(Techniques.FadeIn)
                .duration(500)
                .onStart(animator -> imgResult.setVisibility(View.VISIBLE))
                .onEnd(animator -> {
                    imgQuestion.setVisibility(View.INVISIBLE);
                    imgPepper.setVisibility(View.GONE);
                    imgWarning.setVisibility(View.GONE);
                    imgValid.setVisibility(View.GONE);
                    btnSee.setVisibility(View.GONE);
                    flashCtn.setVisibility(View.GONE);
                    imgHome.setVisibility(View.VISIBLE);
                    btnAgain.setVisibility(View.VISIBLE);
                    YoYo.with(Techniques.SlideInUp)
                            .duration(500)
                            .onEnd(animator1 -> QiThreadPool.run(() -> {
                                pepperHolder.release();
                            }))
                            .playOn(btnAgain);
                })
                .playOn(imgResult);

        btnSee.setEnabled(false);
    }

    private void colorTopButtons(@ColorRes int color) {
        imgCross.setColorFilter(getResources().getColor(color), PorterDuff.Mode.SRC_IN);
        imgHome.setColorFilter(getResources().getColor(color), PorterDuff.Mode.SRC_IN);
    }
    //endregion

    //region TensorFlow
    private void scanObject() {
        layoutScan();

        Future<TimestampedImageHandle> timestampedImageHandleFuture = futurePicture.andThenCompose(takePicture -> {
            Log.i(TAG, "take picture launched!");
            return takePicture.async().run();
        });

        timestampedImageHandleFuture.andThenConsume(timestampedImageHandle -> {
            Log.i(TAG, "Picture taken");
            // get picture
            EncodedImageHandle encodedImageHandle = timestampedImageHandle.getImage();

            EncodedImage encodedImage = encodedImageHandle.getValue();
            Log.i(TAG, "PICTURE RECEIVED!");

            // get the byte buffer and cast it to byte array
            ByteBuffer buffer = encodedImage.getData();
            buffer.rewind();
            final int pictureBufferSize = buffer.remaining();
            final byte[] pictureArray = new byte[pictureBufferSize];
            buffer.get(pictureArray);

            Log.i(TAG, "PICTURE RECEIVED! (" + pictureBufferSize + " Bytes)");
            // display picture
            Bitmap pictureBitmap = BitmapFactory.decodeByteArray(pictureArray, 0, pictureBufferSize);

            runOnUiThread(() -> classifyImage(pictureBitmap));
        });

    }

    public void classifyImage(Bitmap bitmap) {
        Classifier classifier =
                ImageClassifier.create(
                        getAssets(),
                        MODEL_FILE,
                        LABEL_FILE,
                        INPUT_SIZE,
                        IMAGE_MEAN,
                        IMAGE_STD,
                        INPUT_NAME,
                        OUTPUT_NAME);

        Bitmap resizedBitmap = Utils.getResizedBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
        List<Classifier.Recognition> results = classifier.recognizeImage(resizedBitmap);
        heightRecognition = new Classifier.Recognition("test", "testObject", 0.0f, null);

        layoutResult(Utils.getResizedBitmap(bitmap, 1400, 900, true));

        for (Classifier.Recognition recognition :
                results) {

            if (recognition.getConfidence() > heightRecognition.getConfidence())
                heightRecognition = recognition;

            if (heightRecognition.getConfidence() * 100 > 20) {

                String name = heightRecognition.getTitle();

                QiThreadPool.run(() -> RobotUtils.goToBookmark(qiChatbot, bookmarks, "classify", name).getValue());
                Log.d(TAG, "classifyImage: " + name);
            } else {
                QiThreadPool.run(() -> RobotUtils.goToBookmark(qiChatbot, bookmarks, "again", null).getValue());
            }
        playerSuccess.start();
        }
        isScanning.set(false);
    }
    //endregion

    //region onClick
    @OnClick(R.id.img_cross)
    public void onImgCrossClicked() {
        QiThreadPool.run(() ->
                RobotUtils.releaseRobot(
                        getParent(),
                        pepperHolder,
                        qiChatbot,
                        chat,
                        futureChat,
                        futurePicture,
                        this::finishAffinity));
    }

    @OnClick(R.id.btn_see)
    public void onBtnSeeClicked() {
        if (!isScanning.get()) {
            timer.cancel();
            runOnUiThread(() -> btnSee.setEnabled(false));
            QiThreadPool.run(() -> RobotUtils.goToBookmark(qiChatbot, bookmarks, "dontMove").getValue());
        }
    }

    @OnClick(R.id.btn_again)
    public void onBtnAgainClicked() {
        QiThreadPool.run(() -> RobotUtils.goToBookmark(qiChatbot, bookmarks, "tryAgain").getValue());
    }

    @OnClick(R.id.img_home)
    public void onViewClicked() {
        layoutBriefing();
        QiThreadPool.run(() -> RobotUtils.goToBookmark(qiChatbot, bookmarks, "briefing"));
    }
    //endregion

    //region Robot Callback
    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        this.qiContext = qiContext;
        setupRobot();
    }

    @Override
    public void onRobotFocusLost() {
        RobotUtils.releaseRobot(getParent(), pepperHolder, qiChatbot, chat, futureChat, futurePicture, null);
        this.qiContext = null;
    }

    @Override
    public void onRobotFocusRefused(String reason) {
        //NOT USED IN THIS CASE
        Log.e(TAG, "onRobotFocusRefused: " + reason);
    }
    //endregion

    //region Chat Callback
    @Override
    public void onHeard(Phrase heardPhrase) {
        runOnUiThread(() -> timer.cancel());
    }

    @Override
    public void onBookmarkReached(Bookmark bookmark) {
        if ("endBriefing".equals(bookmark.getName())) {
            runCallToAction();
        } else if ("dontMove".equals(bookmark.getName())) {
            runOnUiThread(() -> layoutCaptureMove(false));
        } else if ("endMove".equals(bookmark.getName())) {
            if (!isScanning.get()) {
                scanObject();
            }
        } else if ("endAgain".equals(bookmark.getName())) {
            runCallToAction();
        }
    }
    //endregion
}