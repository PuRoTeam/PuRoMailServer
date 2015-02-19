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
 * NOOP command.
 * <p/>
 * <p/>
 * The spec is at <a
 * href="http://asg.web.cmu.edu/rfc/rfc2821.html#sec-4.1.1.9">
 * http://asg.web.cmu.edu/rfc/rfc2821.html#sec-4.1.1.9</a>.
 * </p>
 */
public class NoopCommand
        extends SmtpCommand {
    public void execute(SmtpConnection conn, SmtpState state,
    					PuRoSmtpManager manager, String commandLine) {
        conn.println("250 Is that all?");
    }
}