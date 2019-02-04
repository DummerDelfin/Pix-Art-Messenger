package de.pixart.messenger.ui;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

import de.pixart.messenger.R;
import de.pixart.messenger.databinding.ActivityMucUsersBinding;
import de.pixart.messenger.entities.Conversation;
import de.pixart.messenger.entities.MucOptions;
import de.pixart.messenger.services.XmppConnectionService;
import de.pixart.messenger.ui.adapter.UserAdapter;
import de.pixart.messenger.ui.util.MucDetailsContextMenuHelper;
import rocks.xmpp.addr.Jid;

public class MucUsersActivity extends XmppActivity implements XmppConnectionService.OnMucRosterUpdate, XmppConnectionService.OnAffiliationChanged {

    private UserAdapter userAdapter;

    private Conversation mConversation = null;

    @Override
    protected void refreshUiReal() {
    }

    @Override
    void onBackendConnected() {
        final Intent intent = getIntent();
        final String uuid = intent == null ? null : intent.getStringExtra("uuid");
        if (uuid != null) {
            mConversation = xmppConnectionService.findConversationByUuid(uuid);
        }
        loadAndSubmitUsers();
    }

    private void loadAndSubmitUsers() {
        if (mConversation != null) {
            ArrayList<MucOptions.User> users = mConversation.getMucOptions().getUsers();
            Collections.sort(users);
            userAdapter.submitList(users);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (!MucDetailsContextMenuHelper.onContextItemSelected(item, userAdapter.getSelectedUser(), this)) {
            return super.onContextItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMucUsersBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_muc_users);
        setSupportActionBar((Toolbar) binding.toolbar);
        configureActionBar(getSupportActionBar(), true);
        this.userAdapter = new UserAdapter(getPreferences().getBoolean("advanced_muc_mode", false));
        binding.list.setAdapter(this.userAdapter);
    }


    @Override
    public void onMucRosterUpdate() {
        loadAndSubmitUsers();
    }

    private void displayToast(final String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onAffiliationChangedSuccessful(Jid jid) {

    }

    @Override
    public void onAffiliationChangeFailed(Jid jid, int resId) {
        displayToast(getString(resId, jid.asBareJid().toString()));
    }
}