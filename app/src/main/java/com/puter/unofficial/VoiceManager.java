package com.puter.unofficial;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.webkit.WebView;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Refined Native Android Speech-to-Text Manager.
 * Optimized for Puter Unofficial to handle barge-in interruptions
 * and reliable communication with the HTML frontend.
 */
public class VoiceManager {

    private static final String TAG = "PuterVoiceManager";
    private final Context context;
    private final WebView webView;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private WebAppInterface bridge;

    public VoiceManager(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
        initializeRecognizer();
    }

    /**
     * Link the bridge to allow VoiceManager to trigger TTS stops 
     * when it detects the user has started speaking.
     */
    public void setBridge(WebAppInterface bridge) {
        this.bridge = bridge;
    }

    private void initializeRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            // Allow partial results for faster perceived response if needed
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    Log.d(TAG, "Microphone Ready");
                }

                @Override
                public void onBeginningOfSpeech() {
                    Log.d(TAG, "User started speaking");
                    // BARGE-IN: If the AI is currently speaking, stop it immediately
                    if (bridge != null) {
                        bridge.stopSpeaking();
                    }
                }

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    Log.d(TAG, "User finished speaking");
                }

                @Override
                public void onError(int error) {
                    String message;
                    switch (error) {
                        case SpeechRecognizer.ERROR_AUDIO: message = "Audio recording error"; break;
                        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: message = "Permission denied"; break;
                        case SpeechRecognizer.ERROR_NETWORK: message = "Network error"; break;
                        case SpeechRecognizer.ERROR_NO_MATCH: message = "No speech recognized"; break;
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: message = "No speech input"; break;
                        default: message = "Recognition error"; break;
                    }
                    Log.e(TAG, "Speech Error: " + message);
                    // Notify frontend of the error
                    sendResultToWeb("Error: " + message);
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String recognizedText = matches.get(0);
                        Log.d(TAG, "Final Result: " + recognizedText);
                        sendResultToWeb(recognizedText);
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {}

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        }
    }

    /**
     * Safely injects the recognized text into the WebView.
     * Replaces single quotes with escaped quotes to prevent JS injection errors.
     */
    private void sendResultToWeb(String text) {
        String safeText = text.replace("'", "\\'");
        webView.post(() -> webView.evaluateJavascript("if(window.onSpeechResult) { window.onSpeechResult('" + safeText + "'); }", null));
    }

    public void startListening() {
        if (speechRecognizer != null) {
            // Cancel any current recognition before starting a new one to prevent hangs
            speechRecognizer.cancel();
            speechRecognizer.startListening(recognizerIntent);
        }
    }

    public void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
            speechRecognizer.destroy();
        }
    }
}