package com.example.android.tflitecamerademo.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
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
import com.aldebaran.qi.sdk.builder.TakePictureBuilder;
import com.aldebaran.qi.sdk.builder.TopicBuilder;
import com.aldebaran.qi.sdk.core.QiThreadPool;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.camera.TakePicture;
import com.aldebaran.qi.sdk.object.conversation.AutonomousReactionImportance;
import com.aldebaran.qi.sdk.object.conversation.AutonomousReactionValidity;
import com.aldebaran.qi.sdk.object.conversation.BodyLanguageOption;
import com.aldebaran.qi.sdk.object.conversation.Bookmark;
import com.aldebaran.qi.sdk.object.conversation.Chat;
import com.aldebaran.qi.sdk.object.conversation.Phrase;
import com.aldebaran.qi.sdk.object.conversation.QiChatbot;
import com.aldebaran.qi.sdk.object.conversation.Topic;
import com.aldebaran.qi.sdk.object.holder.AutonomousAbilitiesType;
import com.aldebaran.qi.sdk.object.holder.Holder;
import com.aldebaran.qi.sdk.object.image.EncodedImage;
import com.aldebaran.qi.sdk.object.image.EncodedImageHandle;
import com.aldebaran.qi.sdk.object.image.TimestampedImageHandle;
import com.example.android.tflitecamerademo.tf.Classifier;
import com.example.android.tflitecamerademo.tf.TensorFlowImageClassifier;
import com.example.android.tflitecamerademo.tf.Utils;
import com.softbankrobotics.sample.whatdoyousee.R;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
    @BindView(R.id.btn_again)
    TextView btnAgain;
    @BindView(R.id.img_home)
    ImageView imgHome;
    @BindView(R.id.img_warning)
    ImageView imgWarning;
    @BindView(R.id.img_tick)
    ImageView imgTick;
    @BindView(R.id.flash_ctn)
    ImageView flashCtn;
    @BindView(R.id.img_result)
    ImageView imgResult;
    @BindView(R.id.txt_reco)
    TextView txtReco;

    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 117;
    private static final float IMAGE_STD = 1;
    private static final String INPUT_NAME = "input";
    private static final String OUTPUT_NAME = "output";
    private static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
    private static final String LABEL_FILE = "file:///android_asset/imagenet_comp_graph_label_strings.txt";


    int countError = 0;
    AtomicBoolean isScanning = new AtomicBoolean(false);
    CountDownTimer timer;

    MediaPlayer playerStart;
    MediaPlayer playerFlash;

    QiContext qiContext;
    Holder pepperHolder;

    QiChatbot qiChatbot;
    Chat chat;
    Topic topic;
    Map<String, Bookmark> bookmarks;
    Future<Void> futureChat;
    Future<TakePicture> futurePicture;


    //region Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_user_interaction);

        ButterKnife.bind(this);
        QiSDK.register(this, this);

        playerStart = MediaPlayer.create(this, R.raw.mariocoin);
        playerFlash = MediaPlayer.create(this, R.raw.automatic_camera);

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
        QiThreadPool.run(this::releaseRobot);
        super.onDestroy();
    }
    //endregion

    //region Flow
    private void setupRobot() {
        createChatBot();

        freezeRobot();

        futurePicture = TakePictureBuilder.with(qiContext).buildAsync();

        runBriefing();
    }

    private void runBriefing() {
        if (this.qiContext == null)
            return;

        goToBookmark("briefing");

        if (futureChat == null
                || futureChat.isCancelled())
            futureChat = chat.async().run();
    }

    private void runCallToAction() {
        if (qiContext == null)
            return;

        runOnUiThread(() -> {
            playerStart.start();
            btnSee.setVisibility(View.VISIBLE);
            timer.start();
        });
    }


    //endregion

    //region Robot
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

        chat.addOnHeardListener(heardPhrase -> runOnUiThread(() -> timer.cancel()));

        qiChatbot.addOnBookmarkReachedListener(bookmark -> {
            if ("endAction".equals(bookmark.getName())) {
                runCallToAction();
            } else if ("endMove".equals(bookmark.getName())) {
                if (!isScanning.get()) {
                    scanObject();
                }
            } else if ("endAgain".equals(bookmark.getName())) {
                runOnUiThread(() -> btnAgain.setVisibility(View.VISIBLE));
            }
        });

        chat.addOnFallbackReplyFoundForListener(input -> {
            timer.cancel();
            countError++;
            if (countError > 3) {
                countError = 0;
                goToBookmark("briefing");
            }
        });
    }

    private Future<Void> goToBookmark(String bookmarkKey) {
        return qiChatbot.async().goToBookmark(bookmarks.get(bookmarkKey),
                AutonomousReactionImportance.HIGH,
                AutonomousReactionValidity.IMMEDIATE);
    }

    private void freezeRobot() {
        //TODO set animation to pepper arms

        pepperHolder = HolderBuilder.with(qiContext)
                .withAutonomousAbilities(AutonomousAbilitiesType.BACKGROUND_MOVEMENT, AutonomousAbilitiesType.BASIC_AWARENESS)
                .build();

        pepperHolder.hold();

        qiChatbot.setSpeakingBodyLanguage(BodyLanguageOption.DISABLED);
        chat.setListeningBodyLanguage(BodyLanguageOption.DISABLED);
    }

    private void releaseRobot() {
        if (pepperHolder != null) {
            pepperHolder.async().release();
        }

        if (chat != null)
            chat.setListeningBodyLanguage(BodyLanguageOption.NEUTRAL);

        if (qiChatbot != null)
            qiChatbot.setSpeakingBodyLanguage(BodyLanguageOption.NEUTRAL);

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
        }
        if (futurePicture != null
                && !futurePicture.isCancelled()
                && !futurePicture.isDone()) {
            futurePicture.thenConsume(voidFuture -> {
                if (voidFuture.isCancelled()
                        || voidFuture.hasError())
                    if (runnable != null)
                        runnable.run();
            });
            futurePicture.requestCancellation();
        } else {
            if (runnable != null)
                runnable.run();
        }
    }

    private void unregisterListener() {
        if (chat != null) {
            chat.async().removeAllOnFallbackReplyFoundForListeners();
        }
        if (qiChatbot != null) {
            qiChatbot.async().removeAllOnBookmarkReachedListeners();
            qiChatbot.async().removeAllOnEndedListeners();
        }
    }
    //endregion

    //region TensorFlow
    private void prepareToScan() {
        isScanning.set(true);

        pepperHolder.hold();

        runOnUiThread(() -> {
            imgWarning.setVisibility(View.VISIBLE);
            txtQuestionMark.setVisibility(View.GONE);
        });

        runOnUiThread(() -> {
            flashCtn.setVisibility(View.VISIBLE);
            imgWarning.setVisibility(View.GONE);
            playerFlash.start();
            flashCtn.setVisibility(View.GONE);
            imgTick.setVisibility(View.VISIBLE);
            btnAgain.setVisibility(View.GONE);
            imgResult.setVisibility(View.GONE);
            btnSee.setVisibility(View.GONE);
        });
    }

    private void scanObject() {
        runOnUiThread(() -> btnSee.setEnabled(false));

        prepareToScan();

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
        Bitmap resizedBitmap = Utils.getResizedBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);

        Classifier classifier =
                TensorFlowImageClassifier.create(
                        getAssets(),
                        MODEL_FILE,
                        LABEL_FILE,
                        INPUT_SIZE,
                        IMAGE_MEAN,
                        IMAGE_STD,
                        INPUT_NAME,
                        OUTPUT_NAME);

        final List<Classifier.Recognition> results = classifier.recognizeImage(resizedBitmap);

        Classifier.Recognition highReco = new Classifier.Recognition("test", "testObject", 0.0f, null);

        QiThreadPool.run(() -> pepperHolder.release());

        showResultScreen(Utils.getResizedBitmap(bitmap, 1400, 900, true));

        for (Classifier.Recognition recognition :
                results) {

            if (recognition.getConfidence() > highReco.getConfidence())
                highReco = recognition;

            txtReco.setText(highReco.getTitle() + " : " + highReco.getConfidence() * 100);

            if (highReco.getConfidence() * 100 > 20) {

                String recoName = highReco.getTitle();

                QiThreadPool.run(() -> {
                    qiChatbot.variable("object").setValue(recoName);
                    goToBookmark("classify");
                });
            }
        }

        QiThreadPool.run(() -> {
            goToBookmark("again");
        });
    }

    private void showResultScreen(Bitmap bitmap) {
        imgResult.setImageBitmap(bitmap);
        imgResult.setVisibility(View.VISIBLE);
        imgCross.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_IN);
        imgHome.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_IN);
    }

    private void readyToScanAgain() {
        runOnUiThread(() -> {
            prepareLayoutForScan();
            btnAgain.setVisibility(View.VISIBLE);
            QiThreadPool.run(() -> goToBookmark("tryAgain"));
        });
    }

    private void prepareLayoutForScan() {
        isScanning.set(false);
        btnSee.setEnabled(true);
        btnAgain.setEnabled(true);
        imgResult.setVisibility(View.GONE);
        imgTick.setVisibility(View.GONE);
        imgWarning.setVisibility(View.GONE);
        txtQuestionMark.setVisibility(View.VISIBLE);
        txtReco.setText("");
    }

    //endregion

    //region onClick
    @OnClick(R.id.img_cross)
    public void onImgCrossClicked() {
        QiThreadPool.run(this::releaseRobot);
        finishAffinity();
    }

    @OnClick(R.id.btn_see)
    public void onBtnSeeClicked() {
        if (!isScanning.get()) {
            timer.cancel();
            runOnUiThread(() -> btnSee.setEnabled(false));
            QiThreadPool.run(() -> goToBookmark("dontMove"));
        }
    }

    @OnClick(R.id.btn_again)
    public void onBtnAgainClicked() {
        isScanning.set(false);
        readyToScanAgain();
    }

    @OnClick(R.id.img_home)
    public void onViewClicked() {
        prepareLayoutForScan();
        QiThreadPool.run(() -> goToBookmark("briefing"));
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
        releaseRobot();
        this.qiContext = null;
    }

    @Override
    public void onRobotFocusRefused(String reason) {
        //NOT USED IN THIS CASE
        Log.e(TAG, "onRobotFocusRefused: " + reason);
    }
    //endregion
}