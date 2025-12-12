package com.porvida.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpenAIBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION_REQUEST = "com.porvida.OPENAI_REQUEST";
    public static final String ACTION_RESPONSE = "com.porvida.OPENAI_RESPONSE";

    public static final String EXTRA_HISTORY_JSON = "history_json"; // JSON array of {role, content}
    public static final String EXTRA_ENABLE_WEB = "enable_web";
    public static final String EXTRA_ALLOWED_DOMAINS = "allowed_domains"; // String[]
    public static final String EXTRA_MODEL = "model";
    public static final String EXTRA_COUNTRY = "country";
    public static final String EXTRA_CITY = "city";
    public static final String EXTRA_REGION = "region";
    public static final String EXTRA_TIMEZONE = "timezone";

    public static final String EXTRA_TEXT = "text"; // response text
    public static final String EXTRA_ERROR = "error"; // error message
    public static final String EXTRA_CITATIONS = "citations"; // String[] of URLs

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_REQUEST.equals(intent.getAction())) return;
        final String historyJson = intent.getStringExtra(EXTRA_HISTORY_JSON);
        final boolean enableWeb = intent.getBooleanExtra(EXTRA_ENABLE_WEB, false);
        final String[] domainsArr = intent.getStringArrayExtra(EXTRA_ALLOWED_DOMAINS);
    final String model = intent.getStringExtra(EXTRA_MODEL) != null ? intent.getStringExtra(EXTRA_MODEL) : "gpt-5-nano";
    final String country = intent.getStringExtra(EXTRA_COUNTRY);
    final String city = intent.getStringExtra(EXTRA_CITY);
    final String region = intent.getStringExtra(EXTRA_REGION);
    final String timezone = intent.getStringExtra(EXTRA_TIMEZONE);

        EXECUTOR.execute(() -> {
            String text = null;
            String error = null;
            ArrayList<String> citations = new ArrayList<>();
            try {
                List<OpenAIService.MessageItem> history;
                if (historyJson == null || historyJson.isEmpty()) {
                    history = new ArrayList<>();
                } else {
                    history = OpenAIInterop.decodeHistory(historyJson);
                }
                List<String> allowed = null;
                if (domainsArr != null && domainsArr.length > 0) {
                    allowed = new ArrayList<>();
                    for (String d : domainsArr) if (d != null && !d.isEmpty()) allowed.add(d);
                }
                OpenAIService.ChatResult result = OpenAIInterop.chatBlocking(history, enableWeb, allowed, model, country, city, region, timezone);
                if (result == null || result.getText() == null) {
                    error = "Empty response";
                } else {
                    text = result.getText();
                    if (result.getCitations() != null) {
                        for (OpenAIService.UrlCitation c : result.getCitations()) {
                            if (c.getUrl() != null) citations.add(c.getUrl());
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("OpenAIReceiver", "error", e);
                error = e.getMessage();
            }

            Intent out = new Intent(ACTION_RESPONSE);
            if (error != null) out.putExtra(EXTRA_ERROR, error);
            if (text != null) out.putExtra(EXTRA_TEXT, text);
            if (!citations.isEmpty()) out.putStringArrayListExtra(EXTRA_CITATIONS, citations);
            try { context.sendBroadcast(out); } catch (Exception ignored) {}
        });
    }
}
