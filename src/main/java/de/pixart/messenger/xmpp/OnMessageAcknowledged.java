package de.pixart.messenger.xmpp;

import de.pixart.messenger.entities.Account;

public interface OnMessageAcknowledged {
    boolean onMessageAcknowledged(Account account, String id);
}
