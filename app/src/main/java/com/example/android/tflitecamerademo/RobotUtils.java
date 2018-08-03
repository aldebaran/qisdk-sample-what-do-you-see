package com.example.android.tflitecamerademo;

import android.app.Activity;
import android.support.annotation.NonNull;

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

import java.util.List;
import java.util.Map;

public class RobotUtils {

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

        QiSDK.unregister(activity);
    }

    public static void stopChat(Future<Void> futureChat, Future<TakePicture> futurePicture, Runnable runnable) {
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

    public static void unregisterListener(QiChatbot chatbot, Chat chat) {
        if (chat != null) {
            chat.async().removeAllOnFallbackReplyFoundForListeners();
        }
        if (chatbot != null) {
            chatbot.async().removeAllOnBookmarkReachedListeners();
            chatbot.async().removeAllOnEndedListeners();
        }
    }

    public static Future<Void> goToBookmark(QiChatbot chatbot, Map<String, Bookmark> bookmarks, String bookmarkKey) {
        return goToBookmark(chatbot, bookmarks, bookmarkKey, null);
    }

    public static Future<Void> goToBookmark(QiChatbot chatbot, Map<String, Bookmark> bookmarks, String bookmarkKey, String objectName) {

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
}
