package com.puter.unofficial;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Full-screen Native Voice Agent Activity.
 * Implements a "Live" conversation mode similar to Gemini Live.
 * Features:
 * 1. Native STT for user input.
 * 2. Native TTS for AI responses.
 * 3. Barge-in: User speaking immediately interrupts AI speech.
 * 4. Continuous flow: Automatically listens after AI finishes speaking.
 */
public class VoiceAgentActivity extends AppCompatActivity {

    private static final String TAG = "PuterVoiceAgent";
    
    private TextView tvStatus, tvTranscript;
    private FloatingActionButton fabMic;
    private ImageButton btnClose;
    
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech tts;
    private Intent recognizerIntent;
    
    private boolean isListening = false;
    private boolean isAIspeaking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_agent);

        // Initialize UI
        tvStatus = findViewById(R.id.tvVoiceStatus);
        tvTranscript = findViewById(R.id.tvTranscript);
        fabMic = findViewById(R.id.fabMicControl);
        btnClose = findViewById(R.id.btnCloseVoice);

        // Initialize TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                // Start the conversation by listening or greeting
                startListening();
            }
        });

        // Initialize STT
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        setupSTTListener();

        // Listeners
        fabMic.setOnClickListener(v -> toggleListening());
        btnClose.setOnClickListener(v -> finish());
    }

    private void setupSTTListener() {
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                tvStatus.setText("Puter is listening...");
                isListening = true;
            }

            @Override
            public void onBeginningOfSpeech() {
                // BARGE-IN LOGIC: If AI is speaking and user starts talking, stop AI
                if (tts.isSpeaking()) {
                    tts.stop();
                    isAIspeaking = false;
                }
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // Used for visualizer scaling if needed
            }

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                isListening = false;
            }

            @Override
            public void onError(int error) {
                tvStatus.setText("Try speaking again...");
                isListening = false;
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String userText = matches.get(0);
                    tvTranscript.setText(userText);
                    
                    // Send recognized text to MainActivity to process with Puter AI
                    processUserQuery(userText);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    tvTranscript.setText(matches.get(0));
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    /**
     * Sends the recognized text back to the MainActivity.
     * MainActivity will call Puter.js and then call this activity's speakAIResponse.
     */
    private void processUserQuery(String text) {
        tvStatus.setText("Puter is thinking...");
        
        // Broadcast or Result to MainActivity
        Intent intent = new Intent("PUTER_VOICE_INPUT");
        intent.putExtra("QUERY", text);
        sendBroadcast(intent);
    }

    /**
     * Called when Puter AI returns a response string.
     * Speaks the response and then restarts the mic for continuous flow.
     */
    public void speakAIResponse(String response) {
        isAIspeaking = true;
        tvStatus.setText("Puter is speaking...");
        
        // QUEUE_FLUSH ensures any previous speech is cleared
        tts.speak(response, TextToSpeech.QUEUE_FLUSH, null, "VOICE_AGENT_ID");

        // Use UtteranceProgressListener to detect when AI finishes to resume listening
        tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(() -> {
                    isAIspeaking = false;
                    startListening(); // Continuous conversation loop
                });
            }

            @Override
            public void onError(String utteranceId) {
                isAIspeaking = false;
            }
        });
    }

    private void startListening() {
        if (!isListening) {
            speechRecognizer.startListening(recognizerIntent);
        }
    }

    private void toggleListening() {
        if (isListening) {
            speechRecognizer.stopListening();
        } else {
            if (tts.isSpeaking()) tts.stop();
            startListening();
        }
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}