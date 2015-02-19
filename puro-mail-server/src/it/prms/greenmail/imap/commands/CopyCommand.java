/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package it.prms.greenmail.imap.commands;

import it.prms.greenmail.imap.*;
import it.prms.greenmail.store.FolderException;
import it.prms.greenmail.store.MailFolder;


/**
 * Handles processeing for the COPY imap command.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
class CopyCommand extends SelectedStateCommand implements UidEnabledCommand {
    public static final String NAME = "COPY";
    public static final String ARGS = "<message-set> <mailbox>";

    /**
     * @see CommandTemplate#doProcess
     */
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session)
            throws ProtocolException, FolderException {
        doProcess(request, response, session, false); //(*) Visto che useUids stà sempre a false, utilizza i Message Sequence Number (MSN) anzichè gli UID
    }

    public void doProcess(ImapRequestLineReader request,
                          ImapResponse response,
                          ImapSession session,
                          boolean useUids)
            throws ProtocolException, FolderException {
        IdRange[] idSet = parser.parseIdRange(request); //(*) range di valori rappresentanti i Message Sequence Number dei messaggi da copiare
        String mailboxName = parser.mailbox(request);
        parser.endLine(request);

        ImapSessionFolder currentMailbox = session.getSelected();
        MailFolder toFolder;
        try {
            toFolder = getMailbox(mailboxName, session, true);
        } catch (FolderException e) {
            e.setResponseCode("TRYCREATE"); //(*) se la mailbox di destinazione non esisteva, segnalo un errore
            throw e;
        }

//        if (! useUids) {
//            idSet = currentMailbox.toUidSet(idSet);
//        }
//        currentMailbox.copyMessages(toMailbox, idSet);
        long[] uids = currentMailbox.getMessageUids();
        for (int i = 0; i < uids.length; i++) {
            long uid = uids[i];
            boolean inSet;
            if (useUids) { //(*) sempre false
                inSet = includes(idSet, uid);
            } else {
                int msn = currentMailbox.getMsn(uid); //(*) restituisce indice all'interno dell'ArrayList + 1 (perchè deve partire da 1 e non da sero)
                inSet = includes(idSet, msn); //(*) verifica che il Message Sequence Number sia compreso nel range specificato nella richiesta
            }

            if (inSet) {
                currentMailbox.copyMessage(uid, toFolder);
            }
        }

        session.unsolicitedResponses(response);
        response.commandComplete(this);
    }

    /**
     * @see ImapCommand#getName
     */
    public String getName() {
        return NAME;
    }

    /**
     * @see CommandTemplate#getArgSyntax
     */
    public String getArgSyntax() {
        return ARGS;
    }
}
