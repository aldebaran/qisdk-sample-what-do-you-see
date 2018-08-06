package com.example.android.tflitecamerademo;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.builder.HolderBuilder;
import com.aldebaran.qi.sdk.object.camera.TakePicture;
import com.aldebaran.qi.sdk.object.conversation.AutonomousReactionImportance;
import com.aldebaran.qi.sdk.object.conversation.AutonomousReactionValidity;
import com.aldebaran.qi.sdk.object.conversation.BodyLanguageOption;
import com.aldebaran.qi.sdk.object.conversation.Bookmark;
import com.aldebaran.qi.sdk.object.conversation.Chat;
import com.aldebaran.qi.sdk.object.conversation.QiChatbot;
import com.aldebaran.qi.sdk.object.holder.AutonomousAbilitiesType;
import com.aldebaran.qi.sdk.object.holder.Holder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RobotUtils {

    /**
     * Freeze the movement of the robot
     *
     * @param qiContext the current {@link QiContext}
     * @param holder    the robot {@link Holder}
     * @param chatbot   the current {@link QiChatbot}
     * @param chat      the current {@link Chat}
     * @return
     */
    public static Holder freezeRobot(@NonNull QiContext qiContext, Holder holder, QiChatbot chatbot, Chat chat) {
        //TODO set animation to pepper arms

        holder = HolderBuilder.with(qiContext)
                .withAutonomousAbilities(AutonomousAbilitiesType.BACKGROUND_MOVEMENT, AutonomousAbilitiesType.BASIC_AWARENESS)
                .build();

        holder.hold();

        if (chat != null) {
            chat.setListeningBodyLanguage(BodyLanguageOption.DISABLED);
        }

        if (chatbot != null) {
            chatbot.setSpeakingBodyLanguage(BodyLanguageOption.DISABLED);
        }

        return holder;
    }

    /**
     * Release the robot's movement
     *
     * @param activity      the current {@link Activity}
     * @param holder        the current robot {@link Holder}
     * @param chatBot       the current {@link QiChatbot}
     * @param chat          the current {@link Chat}
     * @param futureChat    the {@link Future} of the {@link Chat}
     * @param futurePicture the {@link Future} of the taking picture function
     * @param runnable      a {@link Runnable} to run after the cancellation
     */
    public static void releaseRobot(@NonNull Activity activity,
                                    Holder holder,
                                    QiChatbot chatBot,
                                    Chat chat,
                                    Future<Void> futureChat,
                                    Future<TakePicture> futurePicture,
                                    Runnable runnable) {
        if (holder != null) {
            holder.async().release();
        }

        if (chat != null)
            chat.setListeningBodyLanguage(BodyLanguageOption.NEUTRAL);

        if (chatBot != null)
            chatBot.setSpeakingBodyLanguage(BodyLanguageOption.NEUTRAL);

        stopChat(futureChat, futurePicture, runnable);

        unregisterListener(chatBot, chat);
    }

    /**
     * Stop the current chat of the robot
     * @param futureChat the current {@link Future} of the chat
     * @param futurePicture
     * @param runnable
     */
    public static void stopChat(Future<Void> futureChat, Future<TakePicture> futurePicture, Runnable runnable) {

        cancelAllFutures(futureChat, futurePicture).andThenConsume(aVoid -> {
            if (runnable != null)
                runnable.run();
        });
    }

    /**
     * Unregister all the listener who subscribed to the {@link QiChatbot} and {@link Chat}
     *
     * @param chatbot the current {@link QiChatbot}
     * @param chat    the current {@link Chat}
     */
    public static void unregisterListener(QiChatbot chatbot, Chat chat) {
        if (chat != null) {
            chat.async().removeAllOnFallbackReplyFoundForListeners();
        }
        if (chatbot != null) {
            chatbot.async().removeAllOnBookmarkReachedListeners();
            chatbot.async().removeAllOnEndedListeners();
        }
    }

    /**
     * Go to the topic {@link Bookmark}
     *
     * @param chatbot     the current {@link QiChatbot}
     * @param bookmarks   the list of chatbot {@link Bookmark}
     * @param bookmarkKey the key of the {@link Bookmark} in the list
     * @return a {@link Future<Void>} of the goTo in the topic
     */
    public static Future<Void> goToBookmark(QiChatbot chatbot, Map<String, Bookmark> bookmarks, String bookmarkKey) {
        return goToBookmark(chatbot, bookmarks, bookmarkKey, null);
    }

    /**
     * Go to the topic {@link Bookmark}
     *
     * @param chatbot     the current {@link QiChatbot}
     * @param bookmarks   the list of chatbot {@link Bookmark}
     * @param bookmarkKey the key of the {@link Bookmark} in the list
     * @param objectName  the value of the object scanned (optional)
     * @return a {@link Future<Void>} of the goTo in the topic
     */
    public static Future<Void> goToBookmark(QiChatbot chatbot, Map<String, Bookmark> bookmarks, String bookmarkKey, @Nullable String objectName) {

        if (chatbot == null
                || bookmarks == null
                || bookmarks.isEmpty()
                || bookmarkKey.isEmpty())
            return null;

        if (objectName != null) {
            chatbot.variable("object").setValue(objectName);
        }

        return chatbot.async().goToBookmark(bookmarks.get(bookmarkKey),
                AutonomousReactionImportance.HIGH,
                AutonomousReactionValidity.IMMEDIATE);
    }


    /**
     * Cancel the provided {@link Future}.
     *
     * @param futureToCancel the {@link Future} to cancelFuture
     * @return A {@link Future} that can only end in a success state, when the provided {@link Future} is cancelled.
     * If the {@link Future} to cancelFuture is already done, this method returns immediately.
     */
    @NonNull
    public static Future<Void> cancelFuture(@Nullable Future<?> futureToCancel) {
        if (futureToCancel == null) {
            return Future.of(null);
        }

        futureToCancel.requestCancellation();
        return futureToCancel.thenConsume(future -> {
        });
    }

    /**
     * Cancel all the provided futures.
     *
     * @param futuresToCancel the futures to cancelFuture
     * @return A {@link Future} that can only end in a success state, when all the provided futures are cancelled.
     * If the futures to cancelFuture are already done, this method returns immediately.
     */
    @NonNull
    public static Future<Void> cancelAllFutures(@Nullable Future<?>... futuresToCancel) {
        if (futuresToCancel == null) {
            return Future.of(null);
        }

        List<Future<?>> cancellations = new ArrayList<>();

        for (Future<?> futureToCancel : futuresToCancel) {
            Future<Void> cancellation = cancelFuture(futureToCancel);
            cancellations.add(cancellation);
        }

        Future<?>[] cancellationsArray = new Future<?>[cancellations.size()];
        return Future.waitAll(cancellations.toArray(cancellationsArray));
    }

}
