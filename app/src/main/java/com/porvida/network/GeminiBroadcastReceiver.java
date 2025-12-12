package com.porvida.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeminiBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION_REQUEST = "com.porvida.GEMINI_REQUEST";
    public static final String ACTION_RESPONSE = "com.porvida.GEMINI_RESPONSE";
    public static final String EXTRA_PROMPT = "prompt";
    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_ERROR = "error";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_REQUEST.equals(intent.getAction())) return;
        final String prompt = intent.getStringExtra(EXTRA_PROMPT);
        EXECUTOR.execute(() -> {
            String text = null;
            String error = null;
            try {
                if (prompt == null || prompt.isEmpty()) {
                    error = "Empty prompt";
                } else {
                    GeminiService.ChatResponse resp = GeminiService.chatSimpleJava(prompt);
                    text = resp.getText();
                    error = resp.getError();
                }
            } catch (Exception e) {
                Log.e("GeminiReceiver", "error", e);
                error = e.getMessage();
            }

            Intent out = new Intent(ACTION_RESPONSE);
            if (error != null) out.putExtra(EXTRA_ERROR, error);
            if (text != null) out.putExtra(EXTRA_TEXT, text);
            try { context.sendBroadcast(out); } catch (Exception ignored) {}
        });
    }
}
