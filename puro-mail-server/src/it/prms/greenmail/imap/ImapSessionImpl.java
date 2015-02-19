/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package it.prms.greenmail.imap;


import it.prms.greenmail.store.FolderException;
import it.prms.greenmail.store.MailFolder;
import it.prms.greenmail.store.MessageFlags;
import it.prms.greenmail.user.GreenMailUser;
import it.prms.greenmail.user.PuRoMailUser;
import it.prms.greenmail.user.PuRoUserManager;
import it.prms.greenmail.user.UserManager;

import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;

/**
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public final class ImapSessionImpl implements ImapSession {
    private ImapSessionState state = ImapSessionState.NON_AUTHENTICATED;
    private PuRoMailUser user = null;
    private ImapSessionFolder selectedMailbox = null;

    private String clientHostName;
    private String clientAddress;

    // TODO these shouldn't be in here - they can be provided directly to command components.
    private ImapHandler handler;
    private PuRoImapHostManager imapHost;
    private PuRoUserManager users;

    public ImapSessionImpl(PuRoImapHostManager imapHost,
                           PuRoUserManager users,
                           ImapHandler handler,
                           String clientHostName,
                           String clientAddress) {
        this.imapHost = imapHost;
        this.users = users;
        this.handler = handler;
        this.clientHostName = clientHostName;
        this.clientAddress = clientAddress;
    }

    public PuRoImapHostManager getHost() {
        return imapHost;
    }

    public void unsolicitedResponses(ImapResponse request) throws FolderException {
        unsolicitedResponses(request, false);
    }

    public void unsolicitedResponses(ImapResponse response, boolean omitExpunged) throws FolderException {
        ImapSessionFolder selected = getSelected();
        if (selected != null) {
            // New message response
            if (selected.isSizeChanged()) {
                response.existsResponse(selected.getMessageCount());
                response.recentResponse(selected.getRecentCount(true));
                selected.setSizeChanged(false);
            }

            // Message updates
            List flagUpdates = selected.getFlagUpdates();
            Iterator iter = flagUpdates.iterator();
            while (iter.hasNext()) {
                ImapSessionFolder.FlagUpdate entry =
                        (ImapSessionFolder.FlagUpdate) iter.next();
                int msn = entry.getMsn();
                Flags updatedFlags = entry.getFlags();
                StringBuffer out = new StringBuffer("FLAGS ");
                out.append(MessageFlags.format(updatedFlags));
                if (entry.getUid() != null) {
                    out.append(" UID ");
                    out.append(entry.getUid());
                }
                response.fetchResponse(msn, out.toString());

            }

            // Expunged messages
            if (!omitExpunged) {
                int[] expunged = selected.getExpunged();
                for (int i = 0; i < expunged.length; i++) {
                    int msn = expunged[i];
                    response.expungeResponse(msn);
                }
            }
        }
    }

    public void closeConnection(String byeMessage) {
        handler.forceConnectionClose(byeMessage);
    }

    public void closeConnection() {
        handler.resetHandler();
    }

    public PuRoUserManager getUserManager() {
        return users;
    }

    public String getClientHostname() {
        return clientHostName;
    }

    public String getClientIP() {
        return clientAddress;
    }

    public void setAuthenticated(PuRoMailUser user) {
        this.state = ImapSessionState.AUTHENTICATED;
        this.user = user;
    }

    public PuRoMailUser getUser() {
        return this.user;
    }

    public void deselect() {
        this.state = ImapSessionState.AUTHENTICATED;
        if (selectedMailbox != null) {
            // TODO is there more to do here, to cleanup the mailbox.
            selectedMailbox.removeListener(selectedMailbox);
            this.selectedMailbox = null;
        }
    }

    public void setSelected(MailFolder folder, boolean readOnly) {
        ImapSessionFolder sessionMailbox = new ImapSessionFolder(folder, this, readOnly);
        this.state = ImapSessionState.SELECTED;
        this.selectedMailbox = sessionMailbox;
    }

    public ImapSessionFolder getSelected() {
        return this.selectedMailbox;
    }

    public boolean selectedIsReadOnly() {
        return selectedMailbox.isReadonly();
    }

    public ImapSessionState getState() {
        return this.state;
    }

}
