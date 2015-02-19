/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package it.prms.greenmail.imap.commands;


import it.prms.greenmail.imap.ImapSession;
import it.prms.greenmail.store.FolderException;

import java.util.Collection;

/**
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
class LsubCommand extends ListCommand {
    public static final String NAME = "LSUB";

    protected Collection doList(ImapSession session, String searchPattern)
            throws FolderException {
        return session.getHost().listSubscribedMailboxes(session.getUser(), searchPattern);
    }

    /**
     * @see ImapCommand#getName
     */
    public String getName() {
        return NAME;
    }
}
