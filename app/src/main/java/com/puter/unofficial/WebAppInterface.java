package com.puter.unofficial;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import java.util.Locale;

/**
 * The refined bridge class between the HTML JavaScript and Native Android code.
 * Implements Text-To-Speech (TTS), Speech-To-Text (STT) triggers, 
 * authentication persistence, and native file/camera pickers.
 */
public class WebAppInterface {

    private final Context context;
    private final WebView webView;
    private TextToSpeech tts;
    private final SharedPreferences prefs;
    private VoiceManager voiceManager;
    private boolean isTtsInitialized = false;

    /**
     * Constructor for the interface.
     * @param context Application or Activity context.
     * @param webView Reference to the WebView for running JavaScript callbacks.
     */
    public WebAppInterface(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
        this.prefs = context.getSharedPreferences("PuterPrefs", Context.MODE_PRIVATE);

        // Initialize Native Android Text-To-Speech Engine
        this.tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsInitialized = true;
                }
            }
        });
    }

    /**
     * Setter to link the VoiceManager.
     * Required so the startListening trigger knows which manager to use.
     */
    public void setVoiceManager(VoiceManager voiceManager) {
        this.voiceManager = voiceManager;
    }

    // 1. NATIVE TEXT-TO-SPEECH (TTS)
    // Supports Barge-in: stops current speech and starts new text immediately.
    @JavascriptInterface
    public void speak(String text) {
        if (isTtsInitialized && tts != null) {
            // Barge-in logic: QUEUE_FLUSH clears previous speech and interrupts immediately
            tts.stop();
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "PuterTTS_ID");
        }
    }

    // 2. STOP TTS
    // Triggered manually from UI or when user starts speaking (interruption).
    @JavascriptInterface
    public void stopSpeaking() {
        if (tts != null) {
            tts.stop();
        }
    }

    // 3. NATIVE SPEECH RECOGNITION TRIGGER
    // Linked to the mic icon in the HTML search bar.
    @JavascriptInterface
    public void startListening() {
        if (voiceManager != null) {
            // Stop TTS before starting the microphone to prevent the AI from hearing itself
            stopSpeaking();
            
            // Run on UI thread to ensure SpeechRecognizer stability
            ((Activity) context).runOnUiThread(() -> voiceManager.startListening());
        }
    }

    // 4. NATIVE FILE / CAMERA PICKER
    // Linked to the "+" icon in the HTML search bar.
    @JavascriptInterface
    public void openFilePicker() {
        ((Activity) context).runOnUiThread(() -> {
            // This triggers the onShowFileChooser logic defined in MainActivity's WebChromeClient
            // By simulating a click or invoking a custom Intent.
            // For Puter.js Base64 requirements, we trigger the system chooser.
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            ((Activity) context).startActivityForResult(Intent.createChooser(intent, "Select File"), 1);
        });
    }

    // 5. SIGN IN
    // Redirects to browser for Puter authentication.
    @JavascriptInterface
    public void signIn() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://puter.com/login"));
        context.startActivity(intent);
    }

    // 6. SIGN OUT
    // Clears persistence and updates local state.
    @JavascriptInterface
    public void signOut() {
        prefs.edit().putBoolean("is_logged_in", false).apply();
        ((Activity) context).runOnUiThread(() -> {
            Toast.makeText(context, "Signed out of Puter", Toast.LENGTH_SHORT).show();
            webView.reload(); // Refresh HTML to show "Sign In" option
        });
    }

    // 7. LOGIN PERSISTENCE CHECK
    // Returns true if the user has already signed in previously.
    @JavascriptInterface
    public boolean isLoggedIn() {
        return prefs.getBoolean("is_logged_in", false);
    }

    /**
     * Internal Java method to set login status from MainActivity 
     * when the auth callback is intercepted.
     */
    public void setLoggedIn(boolean status) {
        prefs.edit().putBoolean("is_logged_in", status).apply();
    }

    /**
     * Cleanup resources. 
     * Called when MainActivity is destroyed.
     */
    public void destroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}