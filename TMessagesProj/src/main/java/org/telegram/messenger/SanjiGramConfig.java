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

    /** Owner of Sanji Gram — shown in the Owner Panel with a Contact Owner row. */
    public static final String OWNER_NAME = "Sanji";
    public static final String OWNER_USERNAME = "Sanjipro5";

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

    /** Force Join: when ON, non-owner users must join every channel in the
     * Owner Panel list before they can use the app (a page opens on login
     * and closes once all channels are joined). The owner never sees it.
     * Default ON. */
    public static boolean isForceJoin() {
        return prefs().getBoolean("force_join", true);
    }

    public static void setForceJoin(boolean value) {
        prefs().edit().putBoolean("force_join", value).apply();
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
        // Sanji Gram: cap the text — an unbounded reply text can fail to send
        // or flood a chat if the owner pastes something huge by accident.
        if (value == null) {
            value = "";
        }
        if (value.length() > 500) {
            value = value.substring(0, 500);
        }
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

    // ---------- Deleted-message badge ----------

    private static volatile java.util.Set<String> deletedMarksCache;

    /** Messages deleted by others, stored as "dialogId:msgId". Shown with a
     * "Deleted" badge instead of vanishing (works with Anti-Delete). */
    public static boolean isMarkedDeleted(long dialogId, int messageId) {
        java.util.Set<String> cached = deletedMarksCache;
        if (cached == null) {
            cached = getDialogIdSet("deleted_marks");
            deletedMarksCache = cached;
        }
        return cached.contains(dialogId + ":" + messageId);
    }

    public static void markDeleted(long dialogId, int messageId) {
        synchronized (SETS_LOCK) {
            LinkedHashSet<String> set = getDialogIdSet("deleted_marks");
            if (set.size() > 2000) {
                set.clear();
            }
            set.add(dialogId + ":" + messageId);
            putDialogIdSet("deleted_marks", set);
            deletedMarksCache = set;
        }
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
        // Sanji Gram: Telegram usernames are 5-32 chars of letters, digits
        // and underscore. Reject anything else so a bad paste can't save a
        // broken channel entry that fails to resolve later.
        if (!username.matches("[A-Za-z0-9_]{5,32}")) {
            return;
        }
        synchronized (SETS_LOCK) {
            LinkedHashSet<String> set = getChannels();
            set.add(username);
            putChannelSet(set);
        }
    }

    public static void removeChannel(String username) {
        synchronized (SETS_LOCK) {
            LinkedHashSet<String> set = getChannels();
            set.remove(username);
            putChannelSet(set);
        }
    }

    private static void putChannelSet(LinkedHashSet<String> set) {
        prefs().edit().putStringSet("channels", set).apply();
    }

    // ---------- Per-chat sets (dialog ids stored as strings) ----------

    // Sanji Gram: getStringSet() + a full set copy per dialog on the UI
    // thread made the chat list lag (getDialogsArray / tab badges run on
    // every refresh). Cache the sets in memory; writers replace the cached
    // reference after persisting, so readers never touch SharedPreferences.
    private static volatile java.util.Set<String> hiddenChatsCache;
    private static volatile java.util.Set<String> lockedChatsCache;

    // Sanji Gram: all read-modify-write updates of the sets above (and the
    // channel set) synchronize on this lock. Without it, two concurrent
    // toggles can both read the same set and the second save silently drops
    // the first change. Readers use the volatile caches and need no lock.
    private static final Object SETS_LOCK = new Object();

    private static LinkedHashSet<String> getDialogIdSet(String key) {
        Set<String> stored = prefs().getStringSet(key, null);
        return stored == null ? new LinkedHashSet<>() : new LinkedHashSet<>(stored);
    }

    private static void putDialogIdSet(String key, LinkedHashSet<String> set) {
        prefs().edit().putStringSet(key, set).apply();
    }

    /** Locked chats (Chat Lock). */
    public static boolean isChatLocked(long dialogId) {
        java.util.Set<String> cached = lockedChatsCache;
        if (cached == null) {
            cached = getDialogIdSet("locked_chats");
            lockedChatsCache = cached;
        }
        return cached.contains(String.valueOf(dialogId));
    }

    public static void setChatLocked(long dialogId, boolean locked) {
        synchronized (SETS_LOCK) {
            LinkedHashSet<String> set = getDialogIdSet("locked_chats");
            if (locked) {
                set.add(String.valueOf(dialogId));
            } else {
                set.remove(String.valueOf(dialogId));
            }
            putDialogIdSet("locked_chats", set);
            lockedChatsCache = set;
        }
    }

    /** Hidden chats (Hidden Chats). */
    public static boolean isChatHidden(long dialogId) {
        java.util.Set<String> cached = hiddenChatsCache;
        if (cached == null) {
            cached = getDialogIdSet("hidden_chats");
            hiddenChatsCache = cached;
        }
        return cached.contains(String.valueOf(dialogId));
    }

    public static void setChatHidden(long dialogId, boolean hidden) {
        synchronized (SETS_LOCK) {
            LinkedHashSet<String> set = getDialogIdSet("hidden_chats");
            if (hidden) {
                set.add(String.valueOf(dialogId));
            } else {
                set.remove(String.valueOf(dialogId));
            }
            putDialogIdSet("hidden_chats", set);
            hiddenChatsCache = set;
        }
    }

    /** Auto-Reply rate limit: last reply time (ms) per dialog id, kept in-memory only. */
    private static final java.util.concurrent.ConcurrentHashMap<Long, Long> autoReplyLastTime = new java.util.concurrent.ConcurrentHashMap<>();

    public static boolean canAutoReplyNow(long dialogId) {
        // Sanji Gram: the check and the timestamp update must be atomic.
        // get() + put() lets two threads both pass the check and send two
        // replies. compute() runs atomically per key, so only the first
        // caller inside the hour window gets true.
        long now = System.currentTimeMillis();
        boolean[] allowed = new boolean[1];
        autoReplyLastTime.compute(dialogId, (key, last) -> {
            if (last != null && now - last < 3600_000L) {
                return last;
            }
            allowed[0] = true;
            return now;
        });
        return allowed[0];
    }
}
