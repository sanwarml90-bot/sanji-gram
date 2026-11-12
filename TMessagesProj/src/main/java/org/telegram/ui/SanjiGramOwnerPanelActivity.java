package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.R;
import org.telegram.messenger.SanjiGramConfig;
import org.telegram.messenger.UserConfig;
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

    private static final int ROW_OWNER_CARD = 1;
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
    private static final int ROW_FONT_PICKER = 82;
    private static final int ROW_ONLINE_STATUS = 83;
    private static final int ROW_CHANNEL_BASE = 100;

    private UniversalRecyclerView listView;
    private ArrayList<String> channels = new ArrayList<>();

    @Override
    public View createView(Context context) {
        // Sanji Gram: re-check ownership here, not only where the Settings
        // row is added. If a non-owner ever reaches this fragment, show an
        // empty view instead of the owner's controls.
        if (!isOwner(getCurrentAccount())) {
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
        actionBar.setTitle("👑 Owner Panel");

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

        items.add(UItem.asHeader("Owner"));
        items.add(UItem.asButton(ROW_OWNER_CARD, R.drawable.filled_crown_on, "Sanji", "Owner of Sanji Gram"));
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

        items.add(UItem.asHeader("Features"));
        items.add(UItem.asCheck(ROW_TOGGLE_ANTI_DELETE, "Anti-Delete").setChecked(SanjiGramConfig.isAntiDelete()));
        items.add(UItem.asCheck(ROW_TOGGLE_GHOST_MODE, "Ghost Mode").setChecked(SanjiGramConfig.isGhostMode()));
        items.add(UItem.asCheck(ROW_TOGGLE_SHOW_IDS, "Show IDs in Profile").setChecked(SanjiGramConfig.isShowIds()));
        items.add(UItem.asCheck(ROW_TOGGLE_VIEW_ONCE, "View Once Bypass").setChecked(SanjiGramConfig.isViewOnceBypass()));
        items.add(UItem.asCheck(ROW_TOGGLE_NO_ADS, "No Ads (hide sponsored)").setChecked(SanjiGramConfig.isNoAds()));
        items.add(UItem.asCheck(ROW_TOGGLE_AUTO_REPLY, "Auto-Reply").setChecked(SanjiGramConfig.isAutoReply()));
        items.add(UItem.asButton(ROW_AUTO_REPLY_TEXT, R.drawable.msg_edit, "Auto-Reply Text", SanjiGramConfig.getAutoReplyText()));
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

    private void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ROW_OWNER_CARD) {
            openChannel(SanjiGramConfig.CHANNEL_USERNAME);
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
