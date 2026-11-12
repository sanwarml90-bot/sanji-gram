package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.LongSparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.R;
import org.telegram.messenger.SanjiGramConfig;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

/**
 * Sanji Gram — Owner Panel.
 * Only visible to the owner (see OWNER_USER_IDS). Gives the owner full
 * control over Sanji Gram: channel list + feature toggles.
 */
public class SanjiGramOwnerPanelActivity extends BaseFragment {

    // Sanji Gram: only the owner sees the panel.
    // 7966027018 = Sanji (main), 8657748041 = GHOST (his second account).
    public static final long[] OWNER_USER_IDS = {7966027018L, 8657748041L};

    /** True when the currently logged-in account belongs to the owner. */
    public static boolean isOwner(int currentAccount) {
        long id = UserConfig.getInstance(currentAccount).getClientUserId();
        for (long ownerId : OWNER_USER_IDS) {
            if (id == ownerId) {
                return true;
            }
        }
        return false;
    }

    // Sanji Gram: user mode — the same feature toggles, for every user
    // (opened from Settings as "Sanji Settings"). Owner-only rows stay hidden.
    private boolean userMode;

    /** Opens the panel as "Sanji Settings" for any user (toggles only). */
    public static SanjiGramOwnerPanelActivity asUserSettings() {
        Bundle args = new Bundle();
        args.putBoolean("userMode", true);
        return new SanjiGramOwnerPanelActivity(args);
    }

    public SanjiGramOwnerPanelActivity() {
        super();
    }

    public SanjiGramOwnerPanelActivity(Bundle args) {
        super(args);
    }

    private static final int ROW_OWNER_CARD = 1;
    private static final int ROW_CONTACT_OWNER = 2;
    private static final int ROW_ADD_CHANNEL = 50;
    private static final int ROW_VERSION = 60;
    private static final int ROW_ACCOUNTS = 61;
    private static final int ROW_TOGGLE_ANTI_DELETE = 70;
    private static final int ROW_TOGGLE_GHOST_MODE = 71;
    private static final int ROW_TOGGLE_SHOW_IDS = 72;
    private static final int ROW_TOGGLE_VIEW_ONCE = 73;
    private static final int ROW_TOGGLE_NO_ADS = 74;
    private static final int ROW_TOGGLE_AUTO_REPLY = 75;
    private static final int ROW_AUTO_REPLY_TEXT = 76;
    private static final int ROW_TOGGLE_STORY_SAVER = 77;
    private static final int ROW_TOGGLE_PIN_UNLIMITED = 78;
    private static final int ROW_TOGGLE_MSG_COUNTER = 79;
    private static final int ROW_TOGGLE_DOUBLE_TAP_LIKE = 80;
    private static final int ROW_TOGGLE_SHOW_HIDDEN = 81;
    private static final int ROW_TOGGLE_FORCE_JOIN = 84;
    private static final int ROW_FONT_PICKER = 82;
    private static final int ROW_ONLINE_STATUS = 83;
    private static final int ROW_AI_API_KEY = 85;
    private static final int ROW_TOGGLE_PHONE_EMOJIS = 86;
    private static final int ROW_ANALYSIS_CHATS = 62;
    private static final int ROW_ANALYSIS_GROUPS = 63;
    private static final int ROW_ANALYSIS_CHANNELS = 64;
    private static final int ROW_ANALYSIS_CONTACTS = 65;
    private static final int ROW_CHANNEL_BASE = 100;

    private UniversalRecyclerView listView;
    private ArrayList<String> channels = new ArrayList<>();

    @Override
    public View createView(Context context) {
        // Sanji Gram: user mode comes from the fragment arguments (passed
        // via the Bundle constructor by asUserSettings) so it survives
        // rotation. BaseFragment has no setArguments — arguments are set
        // only by the BaseFragment(Bundle) constructor.
        if (getArguments() != null) {
            userMode = getArguments().getBoolean("userMode", userMode);
        }
        // Sanji Gram: re-check ownership here, not only where the Settings
        // row is added. If a non-owner ever reaches this fragment, show an
        // empty view instead of the owner's controls. User mode (Sanji
        // Settings for everyone) skips the owner gate.
        if (!userMode && !isOwner(getCurrentAccount())) {
            return new FrameLayout(context);
        }
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });
        actionBar.setTitle(userMode ? "Sanji Settings" : "👑 Owner Panel");

        FrameLayout contentView = new FrameLayout(context);

        listView = new UniversalRecyclerView(this, this::fillItems, this::onItemClick, this::onItemLongClick);
        listView.setSections();
        listView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray, resourceProvider));
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));
        actionBar.setAdaptiveBackground(listView);

        return fragmentView = contentView;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        channels = new ArrayList<>(SanjiGramConfig.getChannels());

        // Sanji Gram: owner-only sections — hidden in user mode (Sanji Settings).
        if (!userMode) {
            items.add(UItem.asHeader("Owner"));
            items.add(UItem.asButton(ROW_OWNER_CARD, R.drawable.filled_crown_on, SanjiGramConfig.OWNER_NAME, "Owner of Sanji Gram"));
            items.add(UItem.asButton(ROW_CONTACT_OWNER, R.drawable.msg_recent, "Contact Owner", "@" + SanjiGramConfig.OWNER_USERNAME));
            items.add(UItem.asShadow("Only you can see this panel."));

            items.add(UItem.asHeader("Channels"));
            for (int i = 0; i < channels.size(); i++) {
                String username = channels.get(i);
                items.add(UItem.asButton(ROW_CHANNEL_BASE + i, R.drawable.msg_link, "@" + username, "Tap to open"));
            }
            items.add(UItem.asButton(ROW_ADD_CHANNEL, R.drawable.msg_add, "Add Channel", "Paste a t.me link or username"));
            items.add(UItem.asShadow("Tap a channel to open it. Long-press a channel to remove it from this list."));

            items.add(UItem.asHeader("Sanji Gram"));
            items.add(UItem.asButton(ROW_VERSION, R.drawable.msg_info, "Version", BuildVars.BUILD_VERSION_STRING));
            items.add(UItem.asButton(ROW_ACCOUNTS, R.drawable.settings_account, "Accounts", countActivatedAccounts() + " / " + UserConfig.MAX_ACCOUNT_COUNT + " logged in"));
            items.add(UItem.asShadow("Sanji Gram supports up to " + UserConfig.MAX_ACCOUNT_COUNT + " accounts."));

            // Sanji Gram: User Analysis — local only (this device/account).
            // Cross-device user counts need a server, which Sanji blocked.
            int[] dialogCounts = countDialogTypes();
            items.add(UItem.asHeader("User Analysis"));
            items.add(UItem.asButton(ROW_ANALYSIS_CHATS, R.drawable.msg_contact, "Private Chats", String.valueOf(dialogCounts[0])));
            items.add(UItem.asButton(ROW_ANALYSIS_GROUPS, R.drawable.filled_calls_users, "Groups", String.valueOf(dialogCounts[1])));
            items.add(UItem.asButton(ROW_ANALYSIS_CHANNELS, R.drawable.msg_channel, "Channels", String.valueOf(dialogCounts[2])));
            items.add(UItem.asButton(ROW_ANALYSIS_CONTACTS, R.drawable.settings_account, "Contacts", String.valueOf(getContactsController().contacts.size())));
            items.add(UItem.asShadow("Is device par ka data — sirf is phone/account ka. Dusre phones ke Sanji Gram users ginne ke liye server chahiye."));
        }

        items.add(UItem.asHeader("Features"));
        if (!userMode) {
            items.add(UItem.asCheck(ROW_TOGGLE_FORCE_JOIN, "Force Join Channels").setChecked(SanjiGramConfig.isForceJoin()));
        }
        items.add(UItem.asCheck(ROW_TOGGLE_ANTI_DELETE, "Anti-Delete").setChecked(SanjiGramConfig.isAntiDelete()));
        items.add(UItem.asCheck(ROW_TOGGLE_GHOST_MODE, "Ghost Mode").setChecked(SanjiGramConfig.isGhostMode()));
        items.add(UItem.asCheck(ROW_TOGGLE_SHOW_IDS, "Show IDs in Profile").setChecked(SanjiGramConfig.isShowIds()));
        items.add(UItem.asCheck(ROW_TOGGLE_VIEW_ONCE, "View Once Bypass").setChecked(SanjiGramConfig.isViewOnceBypass()));
        items.add(UItem.asCheck(ROW_TOGGLE_NO_ADS, "No Ads (hide sponsored)").setChecked(SanjiGramConfig.isNoAds()));
        items.add(UItem.asCheck(ROW_TOGGLE_AUTO_REPLY, "Auto-Reply").setChecked(SanjiGramConfig.isAutoReply()));
        items.add(UItem.asButton(ROW_AUTO_REPLY_TEXT, R.drawable.msg_edit, "Auto-Reply Text", SanjiGramConfig.getAutoReplyText()));
        // Sanji Gram: AI Writer key — never show the key itself, only Set / Not set.
        // Owner only — hidden in user mode.
        if (!userMode) {
            items.add(UItem.asButton(ROW_AI_API_KEY, R.drawable.msg_edit, "AI API Key", SanjiGramConfig.hasAiApiKey() ? "Set (tap to change)" : "Not set — tap to add"));
        }
        // Sanji Gram: phone emojis — stock engine (SharedConfig.useSystemEmoji)
        // already renders system emoji; this is only the toggle for it.
        items.add(UItem.asCheck(ROW_TOGGLE_PHONE_EMOJIS, "Phone Emojis").setChecked(SharedConfig.useSystemEmoji));
        items.add(UItem.asCheck(ROW_TOGGLE_STORY_SAVER, "Story Saver").setChecked(SanjiGramConfig.isStorySaver()));
        items.add(UItem.asCheck(ROW_TOGGLE_PIN_UNLIMITED, "Pin Unlimited").setChecked(SanjiGramConfig.isPinUnlimited()));
        items.add(UItem.asCheck(ROW_TOGGLE_MSG_COUNTER, "Message Counter").setChecked(SanjiGramConfig.isMessageCounter()));
        items.add(UItem.asCheck(ROW_TOGGLE_DOUBLE_TAP_LIKE, "Double-tap to Like").setChecked(SanjiGramConfig.isDoubleTapLike()));
        items.add(UItem.asCheck(ROW_TOGGLE_SHOW_HIDDEN, "Show Hidden Chats").setChecked(SanjiGramConfig.isShowHiddenChats()));
        items.add(UItem.asButton(ROW_FONT_PICKER, R.drawable.msg_text_outlined, "Custom Font", SanjiGramConfig.getCustomFontName()));
        items.add(UItem.asButton(ROW_ONLINE_STATUS, R.drawable.msg_online, "Online Status", SanjiGramConfig.getOnlineStatusName()));
        items.add(UItem.asShadow("Anti-Delete keeps messages others delete visible. Ghost Mode hides your typing and read receipts. Show IDs adds User / Chat / Channel ID rows to profiles. View Once Bypass keeps view-once media after viewing. No Ads hides sponsored messages in channels. Auto-Reply answers private messages once per hour. Story Saver adds a download button to stories. Pin Unlimited raises the pinned-chat limit. Double-tap to Like sends a heart reaction. Hidden chats reappear when Show Hidden Chats is on."));
    }

    private int countActivatedAccounts() {
        int count = 0;
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated()) {
                count++;
            }
        }
        return count;
    }

    /** Counts this account's dialogs by type: {private chats, groups, channels}. */
    private int[] countDialogTypes() {
        int users = 0, groups = 0, channels = 0;
        LongSparseArray<TLRPC.Dialog> dict = getMessagesController().dialogs_dict;
        for (int i = 0, n = dict.size(); i < n; i++) {
            TLRPC.Dialog dialog = dict.valueAt(i);
            if (dialog == null) {
                continue;
            }
            if (DialogObject.isChannel(dialog)) {
                channels++;
            } else if (DialogObject.isUserDialog(dialog.id)) {
                users++;
            } else {
                groups++;
            }
        }
        return new int[]{users, groups, channels};
    }

    private void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ROW_OWNER_CARD) {
            openChannel(SanjiGramConfig.CHANNEL_USERNAME);
        } else if (item.id == ROW_CONTACT_OWNER) {
            // Sanji Gram: opens a chat with the owner (@Sanjipro5).
            getMessagesController().openByUserName(SanjiGramConfig.OWNER_USERNAME, this, 0);
        } else if (item.id == ROW_ADD_CHANNEL) {
            showAddChannelDialog();
        } else if (item.id == ROW_VERSION) {
            AndroidUtilities.addToClipboard(BuildVars.BUILD_VERSION_STRING);
            BulletinFactory.of(this).createCopyBulletin("Version copied").show();
        } else if (item.id == ROW_AUTO_REPLY_TEXT) {
            showEditTextDialog("Auto-Reply Text", SanjiGramConfig.getAutoReplyText(), value -> {
                SanjiGramConfig.setAutoReplyText(value);
                listView.adapter.update(true);
            });
        } else if (item.id == ROW_AI_API_KEY) {
            // Sanji Gram: the dialog opens empty on purpose — the saved key
            // is never displayed on screen.
            showEditTextDialog("AI API Key", "", value -> {
                SanjiGramConfig.setAiApiKey(value);
                listView.adapter.update(true);
            });
        } else if (item.id == ROW_TOGGLE_PHONE_EMOJIS) {
            // Sanji Gram: phone (system) emojis instead of Telegram's own.
            toggleCheck(view, SharedConfig.useSystemEmoji, value -> {
                SharedConfig.useSystemEmoji = value;
                SharedConfig.saveConfig();
            });
            BulletinFactory.of(this).createCopyBulletin("Restart app to apply").show();
        } else if (item.id == ROW_FONT_PICKER) {
            int next = (SanjiGramConfig.getCustomFont() + 1) % 3;
            SanjiGramConfig.setCustomFont(next);
            listView.adapter.update(true);
            BulletinFactory.of(this).createCopyBulletin("Font: " + SanjiGramConfig.getCustomFontName() + " (restart app to apply)").show();
        } else if (item.id == ROW_ONLINE_STATUS) {
            int next = (SanjiGramConfig.getOnlineStatus() + 1) % 3;
            SanjiGramConfig.setOnlineStatus(next);
            listView.adapter.update(true);
        } else if (item.id >= ROW_CHANNEL_BASE && item.id < ROW_CHANNEL_BASE + channels.size()) {
            openChannel(channels.get(item.id - ROW_CHANNEL_BASE));
        } else if (item.id == ROW_TOGGLE_FORCE_JOIN) {
            toggleCheck(view, SanjiGramConfig.isForceJoin(), SanjiGramConfig::setForceJoin);
        } else if (item.id == ROW_TOGGLE_ANTI_DELETE) {
            toggleCheck(view, SanjiGramConfig.isAntiDelete(), SanjiGramConfig::setAntiDelete);
        } else if (item.id == ROW_TOGGLE_GHOST_MODE) {
            toggleCheck(view, SanjiGramConfig.isGhostMode(), SanjiGramConfig::setGhostMode);
        } else if (item.id == ROW_TOGGLE_SHOW_IDS) {
            toggleCheck(view, SanjiGramConfig.isShowIds(), SanjiGramConfig::setShowIds);
        } else if (item.id == ROW_TOGGLE_VIEW_ONCE) {
            toggleCheck(view, SanjiGramConfig.isViewOnceBypass(), SanjiGramConfig::setViewOnceBypass);
        } else if (item.id == ROW_TOGGLE_NO_ADS) {
            toggleCheck(view, SanjiGramConfig.isNoAds(), SanjiGramConfig::setNoAds);
        } else if (item.id == ROW_TOGGLE_AUTO_REPLY) {
            toggleCheck(view, SanjiGramConfig.isAutoReply(), SanjiGramConfig::setAutoReply);
        } else if (item.id == ROW_TOGGLE_STORY_SAVER) {
            toggleCheck(view, SanjiGramConfig.isStorySaver(), SanjiGramConfig::setStorySaver);
        } else if (item.id == ROW_TOGGLE_PIN_UNLIMITED) {
            toggleCheck(view, SanjiGramConfig.isPinUnlimited(), SanjiGramConfig::setPinUnlimited);
            getMessagesController().applySanjiGramPinLimit();
        } else if (item.id == ROW_TOGGLE_MSG_COUNTER) {
            toggleCheck(view, SanjiGramConfig.isMessageCounter(), SanjiGramConfig::setMessageCounter);
        } else if (item.id == ROW_TOGGLE_DOUBLE_TAP_LIKE) {
            toggleCheck(view, SanjiGramConfig.isDoubleTapLike(), SanjiGramConfig::setDoubleTapLike);
        } else if (item.id == ROW_TOGGLE_SHOW_HIDDEN) {
            toggleCheck(view, SanjiGramConfig.isShowHiddenChats(), SanjiGramConfig::setShowHiddenChats);
        }
    }

    private interface BoolSetter {
        void set(boolean value);
    }

    private void toggleCheck(View view, boolean current, BoolSetter setter) {
        boolean value = !current;
        setter.set(value);
        ((TextCheckCell) view).setChecked(value);
    }

    private boolean onItemLongClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= ROW_CHANNEL_BASE && item.id < ROW_CHANNEL_BASE + channels.size()) {
            String username = channels.get(item.id - ROW_CHANNEL_BASE);
            Context context = getParentActivity();
            if (context == null) {
                return true;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(context, getResourceProvider());
            builder.setTitle("Remove Channel");
            builder.setMessage("Remove @" + username + " from the Owner Panel list?");
            builder.setPositiveButton(getString(R.string.Delete), (dialog, which) -> {
                SanjiGramConfig.removeChannel(username);
                listView.adapter.update(true);
            });
            builder.setNegativeButton(getString(R.string.Cancel), null);
            showDialog(builder.create());
            return true;
        }
        return false;
    }

    /** Opens a channel through Telegram's normal resolve flow (loads its info + avatar). */
    private void openChannel(String username) {
        if (TextUtils.isEmpty(username)) {
            return;
        }
        getMessagesController().openByUserName(username, this, 0);
    }

    private void showAddChannelDialog() {
        showEditTextDialog("Add Channel", "", value -> {
            String username = normalizeChannelInput(value);
            if (!TextUtils.isEmpty(username)) {
                SanjiGramConfig.addChannel(username);
                listView.adapter.update(true);
            }
        });
    }

    /** Generic single-line text input dialog. */
    private void showEditTextDialog(String title, String initialText, java.util.function.Consumer<String> onSave) {
        Context context = getParentActivity();
        if (context == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context, getResourceProvider());
        builder.setTitle(title);
        final EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setHint(title);
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        editText.setText(initialText);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, getResourceProvider()));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogSearchHint, getResourceProvider()));
        FrameLayout container = new FrameLayout(context);
        container.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 24, 4, 24, 4));
        builder.setView(container);
        builder.setPositiveButton(getString(R.string.Save), (dialog, which) -> {
            String value = editText.getText().toString().trim();
            if (!TextUtils.isEmpty(value)) {
                onSave.accept(value);
            }
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        showDialog(builder.create());
        editText.requestFocus();
        AndroidUtilities.showKeyboard(editText);
    }

    /** Accepts "@name", "name", "t.me/name" or "https://t.me/name" and returns just "name". */
    private static String normalizeChannelInput(String input) {
        if (input == null) {
            return null;
        }
        String s = input.trim();
        if (s.startsWith("@")) {
            s = s.substring(1);
        }
        s = s.replace("https://t.me/", "").replace("http://t.me/", "").replace("t.me/", "");
        int slash = s.indexOf('/');
        if (slash >= 0) {
            s = s.substring(0, slash);
        }
        int q = s.indexOf('?');
        if (q >= 0) {
            s = s.substring(0, q);
        }
        s = s.trim();
        return s.isEmpty() ? null : s;
    }
}
