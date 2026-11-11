package org.telegram.messenger;

import android.app.Activity;
import android.content.SharedPreferences;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Sanji Gram — owner-controlled feature toggles and per-chat sets.
 * Stored in a separate "sanji_gram" SharedPreferences file so the
 * mod's settings never clash with Telegram's own "mainconfig".
 */
public class SanjiGramConfig {

    /** Channel promoted in the Owner Panel. Change this once Sanji gives the real channel username. */
    public static final String CHANNEL_USERNAME = "sanjibomber";

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("sanji_gram", Activity.MODE_PRIVATE);
    }

    // ---------- Feature toggles ----------

    /** Anti-Delete: when ON, messages deleted by others stay visible locally. Default OFF. */
    public static boolean isAntiDelete() {
        return prefs().getBoolean("anti_delete", false);
    }

    public static void setAntiDelete(boolean value) {
        prefs().edit().putBoolean("anti_delete", value).apply();
    }

    /** Ghost Mode: when ON, no typing indicators and no read receipts are sent. Default OFF. */
    public static boolean isGhostMode() {
        return prefs().getBoolean("ghost_mode", false);
    }

    public static void setGhostMode(boolean value) {
        prefs().edit().putBoolean("ghost_mode", value).apply();
    }

    /** Show IDs in Profile: when ON, User / Chat / Channel ID rows appear in profiles. Default ON. */
    public static boolean isShowIds() {
        return prefs().getBoolean("show_ids", true);
    }

    public static void setShowIds(boolean value) {
        prefs().edit().putBoolean("show_ids", value).apply();
    }

    /** View Once Bypass: when ON, view-once media is not consumed/deleted after viewing. Default OFF. */
    public static boolean isViewOnceBypass() {
        return prefs().getBoolean("view_once_bypass", false);
    }

    public static void setViewOnceBypass(boolean value) {
        prefs().edit().putBoolean("view_once_bypass", value).apply();
    }

    /** No Ads: when ON, sponsored messages are hidden from channel feeds. Default OFF. */
    public static boolean isNoAds() {
        return prefs().getBoolean("no_ads", false);
    }

    public static void setNoAds(boolean value) {
        prefs().edit().putBoolean("no_ads", value).apply();
    }

    /** Auto-Reply: when ON, incoming private messages get an automatic reply. Default OFF. */
    public static boolean isAutoReply() {
        return prefs().getBoolean("auto_reply", false);
    }

    public static void setAutoReply(boolean value) {
        prefs().edit().putBoolean("auto_reply", value).apply();
    }

    /** The text sent by Auto-Reply. Editable in the Owner Panel. */
    public static String getAutoReplyText() {
        return prefs().getString("auto_reply_text", "Hello! I am busy right now, will reply soon.");
    }

    public static void setAutoReplyText(String value) {
        prefs().edit().putString("auto_reply_text", value).apply();
    }

    /** Story Saver: when ON, a save button appears in the story viewer. Default OFF. */
    public static boolean isStorySaver() {
        return prefs().getBoolean("story_saver", false);
    }

    public static void setStorySaver(boolean value) {
        prefs().edit().putBoolean("story_saver", value).apply();
    }

    /** Pin Unlimited: when ON, the pinned-chat limit is raised. Default OFF. */
    public static boolean isPinUnlimited() {
        return prefs().getBoolean("pin_unlimited", false);
    }

    public static void setPinUnlimited(boolean value) {
        prefs().edit().putBoolean("pin_unlimited", value).apply();
    }

    /** Message Counter: when ON, a total message count row shows in chat profiles. Default ON. */
    public static boolean isMessageCounter() {
        return prefs().getBoolean("message_counter", true);
    }

    public static void setMessageCounter(boolean value) {
        prefs().edit().putBoolean("message_counter", value).apply();
    }

    /** Double-tap to Like: when ON, double-tapping a message sends a heart reaction. Default OFF. */
    public static boolean isDoubleTapLike() {
        return prefs().getBoolean("double_tap_like", false);
    }

    public static void setDoubleTapLike(boolean value) {
        prefs().edit().putBoolean("double_tap_like", value).apply();
    }

    /** Show Hidden Chats: when ON, hidden chats are temporarily visible in the chat list. Default OFF. */
    public static boolean isShowHiddenChats() {
        return prefs().getBoolean("show_hidden_chats", false);
    }

    public static void setShowHiddenChats(boolean value) {
        prefs().edit().putBoolean("show_hidden_chats", value).apply();
    }

    // ---------- Custom font: 0 = Default, 1 = Bold, 2 = Monospace ----------

    public static int getCustomFont() {
        return prefs().getInt("custom_font", 0);
    }

    public static void setCustomFont(int value) {
        prefs().edit().putInt("custom_font", value).apply();
    }

    public static String getCustomFontName() {
        switch (getCustomFont()) {
            case 1: return "Bold";
            case 2: return "Monospace";
            default: return "Default";
        }
    }

    /** Returns the Android Typeface for the chosen custom font, or null for Default. */
    public static android.graphics.Typeface getCustomTypeface() {
        switch (getCustomFont()) {
            case 1: return android.graphics.Typeface.DEFAULT_BOLD;
            case 2: return android.graphics.Typeface.MONOSPACE;
            default: return null;
        }
    }

    // ---------- Online status: 0 = Default, 1 = Always Online, 2 = Always Recently ----------

    public static int getOnlineStatus() {
        return prefs().getInt("online_status", 0);
    }

    public static void setOnlineStatus(int value) {
        prefs().edit().putInt("online_status", value).apply();
    }

    public static String getOnlineStatusName() {
        switch (getOnlineStatus()) {
            case 1: return "Always Online";
            case 2: return "Always Recently";
            default: return "Default";
        }
    }

    // ---------- Channel list (Owner Panel) ----------

    /** Ordered set of channel usernames shown in the Owner Panel. Seeded with CHANNEL_USERNAME on first run. */
    public static LinkedHashSet<String> getChannels() {
        Set<String> stored = prefs().getStringSet("channels", null);
        if (stored == null) {
            LinkedHashSet<String> seed = new LinkedHashSet<>();
            seed.add(CHANNEL_USERNAME);
            prefs().edit().putStringSet("channels", seed).apply();
            return seed;
        }
        return new LinkedHashSet<>(stored);
    }

    public static void addChannel(String username) {
        if (username == null || username.isEmpty()) {
            return;
        }
        LinkedHashSet<String> set = getChannels();
        set.add(username);
        prefs().edit().putStringSet("channels", set).apply();
    }

    public static void removeChannel(String username) {
        LinkedHashSet<String> set = getChannels();
        set.remove(username);
        prefs().edit().putStringSet("channels", set).apply();
    }

    // ---------- Per-chat sets (dialog ids stored as strings) ----------

    private static LinkedHashSet<String> getDialogIdSet(String key) {
        Set<String> stored = prefs().getStringSet(key, null);
        return stored == null ? new LinkedHashSet<>() : new LinkedHashSet<>(stored);
    }

    private static void putDialogIdSet(String key, LinkedHashSet<String> set) {
        prefs().edit().putStringSet(key, set).apply();
    }

    /** Locked chats (Chat Lock). */
    public static boolean isChatLocked(long dialogId) {
        return getDialogIdSet("locked_chats").contains(String.valueOf(dialogId));
    }

    public static void setChatLocked(long dialogId, boolean locked) {
        LinkedHashSet<String> set = getDialogIdSet("locked_chats");
        if (locked) {
            set.add(String.valueOf(dialogId));
        } else {
            set.remove(String.valueOf(dialogId));
        }
        putDialogIdSet("locked_chats", set);
    }

    /** Hidden chats (Hidden Chats). */
    public static boolean isChatHidden(long dialogId) {
        return getDialogIdSet("hidden_chats").contains(String.valueOf(dialogId));
    }

    public static void setChatHidden(long dialogId, boolean hidden) {
        LinkedHashSet<String> set = getDialogIdSet("hidden_chats");
        if (hidden) {
            set.add(String.valueOf(dialogId));
        } else {
            set.remove(String.valueOf(dialogId));
        }
        putDialogIdSet("hidden_chats", set);
    }

    /** Auto-Reply rate limit: last reply time (ms) per dialog id, kept in-memory only. */
    private static final java.util.HashMap<Long, Long> autoReplyLastTime = new java.util.HashMap<>();

    public static boolean canAutoReplyNow(long dialogId) {
        Long last = autoReplyLastTime.get(dialogId);
        long now = System.currentTimeMillis();
        if (last != null && now - last < 3600_000L) {
            return false;
        }
        autoReplyLastTime.put(dialogId, now);
        return true;
    }
}
