/* -------------------------------------------------------------------
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been modified by the copyright holder. Original file can be found at http://james.apache.org
 * -------------------------------------------------------------------
 */
package it.prms.greenmail.imap.commands;

import it.prms.greenmail.imap.ImapRequestLineReader;
import it.prms.greenmail.imap.ImapResponse;
import it.prms.greenmail.imap.ImapSession;
import it.prms.greenmail.imap.ProtocolException;
import it.prms.greenmail.store.FolderException;


/**
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
public interface UidEnabledCommand {
    void doProcess(ImapRequestLineReader request,
                   ImapResponse response,
                   ImapSession session,
                   boolean useUids)
            throws ProtocolException, FolderException;
}
