package org.telegram.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ConnectionsManager;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SanjiGramConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Sanji Gram — forced join page.
 *
 * Shown once per app launch (from the chat list) to non-owner accounts when
 * Force Join is ON and the user hasn't joined every channel in the Owner
 * Panel list. The user joins the channels one by one; the page closes
 * automatically once all are joined. The owner never sees this page.
 *
 * Channels that fail to resolve (bad username, no network) are skipped
 * instead of blocking the user. A channel whose join request fails stays
 * joinable so the user can retry; the owner can remove it from the list
 * in the Owner Panel.
 */
public class SanjiGramForceJoinActivity extends BaseFragment {

    private static final int ROW_CHANNEL_BASE = 100;

    /** Set the first time maybeShow() runs, so the check happens once per launch. */
    private static boolean checkedThisLaunch = false;

    /**
     * Checks membership and shows the page when needed. Call from the chat
     * list (DialogsActivity.onResume). Does nothing for the owner, for
     * logged-out accounts, when Force Join is off, or when there is nothing
     * left to join.
     */
    public static void maybeShow(BaseFragment parent) {
        if (checkedThisLaunch) {
            return;
        }
        checkedThisLaunch = true;
        int account = parent.getCurrentAccount();
        if (!UserConfig.getInstance(account).isClientActivated()
                || SanjiGramOwnerPanelActivity.isOwner(account)
                || !SanjiGramConfig.isForceJoin()) {
            return;
        }
        ArrayList<String> channels = SanjiGramConfig.getChannels();
        if (channels.isEmpty()) {
            return;
        }
        ArrayList<String> unjoined = new ArrayList<>();
        int[] pending = {channels.size()};
        for (String username : channels) {
            TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
            req.username = username;
            ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                if (error == null && response instanceof TLRPC.TL_contacts_resolvedPeer) {
                    TLRPC.Chat chat = ((TLRPC.TL_contacts_resolvedPeer) response).chat;
                    if (chat != null && chat.left) {
                        unjoined.add(username);
                    }
                }
                // On resolve error (bad username, no network): skip the
                // channel instead of blocking the user.
                if (--pending[0] == 0 && !unjoined.isEmpty()) {
                    if (parent.getParentActivity() == null || LaunchActivity.getLastFragment() != parent) {
                        return;
                    }
                    parent.presentFragment(new SanjiGramForceJoinActivity(unjoined));
                }
            }));
        }
    }

    private final ArrayList<String> usernames;
    private final Map<String, TLRPC.Chat> chats = new HashMap<>();
    private final Set<String> joined = new HashSet<>();
    private final Set<String> joining = new HashSet<>();
    private UniversalRecyclerView listView;

    public SanjiGramForceJoinActivity(ArrayList<String> usernames) {
        this.usernames = usernames;
    }

    @Override
    public View createView(Context context) {
        // No back button on purpose: the page only closes once every
        // channel is joined (see onBackPressed).
        actionBar.setTitle("Join Channels");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout contentView = new FrameLayout(context);
        listView = new UniversalRecyclerView(this, this::fillItems, this::onItemClick, this::onItemLongClick);
        listView.setSections();
        listView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray, resourceProvider));
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));
        actionBar.setAdaptiveBackground(listView);

        resolveChats();
        return fragmentView = contentView;
    }

    /** Resolves each username to its channel object (needed to join it). */
    private void resolveChats() {
        int account = getCurrentAccount();
        for (String username : usernames) {
            TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
            req.username = username;
            ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                if (error == null && response instanceof TLRPC.TL_contacts_resolvedPeer) {
                    TLRPC.Chat chat = ((TLRPC.TL_contacts_resolvedPeer) response).chat;
                    if (chat != null) {
                        // Cache it so addUserToChat finds it (getChat) and
                        // getInputChannel has the access hash.
                        getMessagesController().putChat(chat, false);
                        chats.put(username, chat);
                        if (!chat.left) {
                            joined.add(username);
                        }
                        if (listView != null && listView.adapter != null) {
                            listView.adapter.update(true);
                        }
                        checkAllJoined();
                    }
                }
            }));
        }
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader("Sanji Gram"));
        for (int i = 0; i < usernames.size(); i++) {
            String username = usernames.get(i);
            String state;
            if (joined.contains(username)) {
                state = "Joined ✓";
            } else if (joining.contains(username)) {
                state = "Joining…";
            } else if (!chats.containsKey(username)) {
                state = "Loading…";
            } else {
                state = "Tap to join";
            }
            items.add(UItem.asButton(ROW_CHANNEL_BASE + i, R.drawable.msg_link, "@" + username, state));
        }
        items.add(UItem.asShadow("Join all channels above to continue using Sanji Gram. The page closes automatically once you have joined them all."));
    }

    private void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id < ROW_CHANNEL_BASE || item.id >= ROW_CHANNEL_BASE + usernames.size()) {
            return;
        }
        String username = usernames.get(item.id - ROW_CHANNEL_BASE);
        if (joined.contains(username) || joining.contains(username)) {
            return;
        }
        TLRPC.Chat chat = chats.get(username);
        if (chat == null) {
            return;
        }
        joining.add(username);
        if (listView.adapter != null) {
            listView.adapter.update(true);
        }
        TLRPC.User self = UserConfig.getInstance(getCurrentAccount()).getCurrentUser();
        getMessagesController().addUserToChat(chat.id, self, 0, null, this, false, () -> {
            joining.remove(username);
            joined.add(username);
            if (listView.adapter != null) {
                listView.adapter.update(true);
            }
            checkAllJoined();
        }, error -> {
            joining.remove(username);
            if (listView.adapter != null) {
                listView.adapter.update(true);
            }
            if (getParentActivity() != null) {
                BulletinFactory.of(this).createErrorBulletin("Could not join @" + username + (error != null && !TextUtils.isEmpty(error.text) ? ": " + error.text : "")).show();
            }
            return false;
        });
    }

    private boolean onItemLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    private boolean allJoined() {
        for (String username : usernames) {
            if (!joined.contains(username)) {
                return false;
            }
        }
        return true;
    }

    private void checkAllJoined() {
        if (!usernames.isEmpty() && allJoined()) {
            finishFragment();
        }
    }

    @Override
    public boolean onBackPressed(boolean invoked) {
        // Block the back button until every channel is joined. Returning
        // true lets the fragment close; false keeps this page on screen.
        return allJoined();
    }
}
