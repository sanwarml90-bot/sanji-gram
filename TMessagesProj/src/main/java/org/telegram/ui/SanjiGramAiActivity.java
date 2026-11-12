package org.telegram.ui;

import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.SanjiGramConfig;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Sanji Gram — AI Writer page. Opened from the AI button in a chat header.
 * Two modes: "Sahi karo" (fix the user's text) and "Likho" (write text from
 * a prompt). The result is editable and can be sent to the same chat.
 * Uses the user's own OpenRouter key (entered in the Owner Panel) and a
 * free model. The key is never baked into the app.
 */
public class SanjiGramAiActivity extends BaseFragment {

    private final long dialogId;
    private boolean fixMode = true;
    private EditText inputEdit;
    private EditText resultEdit;
    private Button fixButton;
    private Button writeButton;
    private Button generateButton;
    private boolean generating;

    public SanjiGramAiActivity(long dialogId) {
        this.dialogId = dialogId;
    }

    @Override
    public View createView(Context context) {
        actionBar.setTitle("AI Writer");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(16);
        content.setPadding(pad, pad, pad, pad);
        content.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray, resourceProvider));

        LinearLayout modeRow = new LinearLayout(context);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        fixButton = new Button(context);
        fixButton.setText("Sahi karo");
        fixButton.setOnClickListener(v -> setMode(true));
        writeButton = new Button(context);
        writeButton.setText("Likho");
        writeButton.setOnClickListener(v -> setMode(false));
        modeRow.addView(fixButton, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
        modeRow.addView(writeButton, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
        content.addView(modeRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        inputEdit = new EditText(context);
        inputEdit.setMinLines(4);
        inputEdit.setGravity(Gravity.TOP);
        inputEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        inputEdit.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourceProvider));
        inputEdit.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText, resourceProvider));
        content.addView(inputEdit, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        generateButton = new Button(context);
        generateButton.setText("AI se banao");
        generateButton.setOnClickListener(v -> generate());
        content.addView(generateButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        resultEdit = new EditText(context);
        resultEdit.setMinLines(6);
        resultEdit.setGravity(Gravity.TOP);
        resultEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        resultEdit.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourceProvider));
        resultEdit.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText, resourceProvider));
        resultEdit.setHint("Yahan result aayega — edit kar sakte ho");
        content.addView(resultEdit, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        Button sendButton = new Button(context);
        sendButton.setText("Chat me bhejo");
        sendButton.setOnClickListener(v -> sendToChat());
        content.addView(sendButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(content);
        setMode(true);
        return fragmentView = scrollView;
    }

    private void setMode(boolean fix) {
        fixMode = fix;
        if (fixButton != null) {
            fixButton.setAlpha(fix ? 1f : 0.5f);
        }
        if (writeButton != null) {
            writeButton.setAlpha(fix ? 0.5f : 1f);
        }
        if (inputEdit != null) {
            inputEdit.setHint(fix ? "Apna text yahan likho, AI sahi karke dega" : "Prompt likho, AI text banayega");
        }
    }

    private void generate() {
        if (generating) {
            return;
        }
        if (!SanjiGramConfig.hasAiApiKey()) {
            BulletinFactory.of(this).createErrorBulletin("Pehle Owner Panel me AI API Key daalo.").show();
            return;
        }
        String input = inputEdit == null ? "" : inputEdit.getText().toString().trim();
        if (input.isEmpty()) {
            BulletinFactory.of(this).createErrorBulletin("Pehle kuch likho.").show();
            return;
        }
        generating = true;
        generateButton.setEnabled(false);
        generateButton.setText("Ban raha hai...");
        String apiKey = SanjiGramConfig.getAiApiKey();
        String systemPrompt = fixMode ? SanjiGramConfig.AI_SYSTEM_FIX : SanjiGramConfig.AI_SYSTEM_WRITE;
        new Thread(() -> {
            String result = callAi(apiKey, systemPrompt, input);
            AndroidUtilities.runOnUIThread(() -> {
                generating = false;
                if (generateButton != null) {
                    generateButton.setEnabled(true);
                    generateButton.setText("AI se banao");
                }
                if (resultEdit != null) {
                    resultEdit.setText(result);
                }
            });
        }).start();
    }

    private String callAi(String apiKey, String systemPrompt, String input) {
        HttpURLConnection connection = null;
        try {
            JSONObject body = new JSONObject();
            body.put("model", SanjiGramConfig.AI_MODEL);
            JSONArray messages = new JSONArray();
            JSONObject system = new JSONObject();
            system.put("role", "system");
            system.put("content", systemPrompt);
            messages.put(system);
            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("content", input);
            messages.put(user);
            body.put("messages", messages);
            body.put("max_tokens", 800);

            URL url = new URL(SanjiGramConfig.AI_API_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);
            connection.setDoOutput(true);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
            int code = connection.getResponseCode();
            InputStream stream = code == 200 ? connection.getInputStream() : connection.getErrorStream();
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            if (code != 200) {
                return "AI error (" + code + "). Dobara try karo.";
            }
            JSONObject json = new JSONObject(response.toString());
            JSONArray choices = json.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                return "AI se jawab nahi mila. Dobara try karo.";
            }
            JSONObject first = choices.optJSONObject(0);
            JSONObject message = first == null ? null : first.optJSONObject("message");
            String text = message == null ? "" : message.optString("content", "");
            if (text == null || text.trim().isEmpty()) {
                return "AI se khali jawab mila. Dobara try karo.";
            }
            return text.trim();
        } catch (Exception e) {
            return "AI error: " + e.getMessage();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void sendToChat() {
        String text = resultEdit == null ? "" : resultEdit.getText().toString().trim();
        if (text.isEmpty()) {
            BulletinFactory.of(this).createErrorBulletin("Pehle AI se text banao.").show();
            return;
        }
        SendMessagesHelper.SendMessageParams params = new SendMessagesHelper.SendMessageParams();
        params.message = text;
        params.peer = dialogId;
        SendMessagesHelper.getInstance(getCurrentAccount()).sendMessage(params);
        finishFragment();
    }
}
