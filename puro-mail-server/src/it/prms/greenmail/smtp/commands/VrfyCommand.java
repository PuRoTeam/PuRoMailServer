/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package it.prms.greenmail.smtp.commands;

import it.prms.greenmail.smtp.PuRoSmtpManager;
import it.prms.greenmail.smtp.SmtpConnection;
import it.prms.greenmail.smtp.SmtpState;


/**
 * VRFY command.
 * <p/>
 * <p/>
 * The spec is at <a
 * href="http://asg.web.cmu.edu/rfc/rfc2821.html#sec-4.1.1.6">
 * http://asg.web.cmu.edu/rfc/rfc2821.html#sec-4.1.1.6</a>.
 * </p>
 */
public class VrfyCommand //(*) generalmente non è più supportato (perchè usato dagli spammer)
        extends SmtpCommand {
    public void execute(SmtpConnection conn, SmtpState state,
    					PuRoSmtpManager manager, String commandLine) {
        conn.println("252 Cannot VRFY user, but will accept message and attempt delivery");
    }
}