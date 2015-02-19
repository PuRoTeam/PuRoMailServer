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
import it.prms.greenmail.user.PuRoMailUser;


/**
 * Handles processeing for the LOGIN imap command.
 *
 * @author Darrell DeBoer <darrell@apache.org>
 * @version $Revision: 109034 $
 */
class LoginCommand extends NonAuthenticatedStateCommand {
    public static final String NAME = "LOGIN";
    public static final String ARGS = "<userid> <password>";

    /**
     * @see CommandTemplate#doProcess
     */
    protected void doProcess(ImapRequestLineReader request,
                             ImapResponse response,
                             ImapSession session)
            throws ProtocolException {
        String userid = parser.astring(request);
        String password = parser.astring(request);
        parser.endLine(request);

        PuRoMailUser user = session.getUserManager().test(userid, password);
        
        if(user != null)
        {
            session.setAuthenticated(user);
            response.commandComplete(this);
        }
        else
        	response.commandFailed(this, "Invalid login/password");
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

/*
6.2.2.  LOGIN Command

   Arguments:  user name
               password

   Responses:  no specific responses for this command

   Result:     OK - login completed, now in authenticated state
               NO - login failure: user name or password rejected
               BAD - command unknown or arguments invalid

      The LOGIN command identifies the client to the server and carries
      the plaintext password authenticating this user.

   Example:    C: a001 LOGIN SMITH SESAME
               S: a001 OK LOGIN completed
*/
