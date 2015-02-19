/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package it.prms.greenmail.smtp.commands;


import it.prms.greenmail.smtp.PuRoSmtpManager;
import it.prms.greenmail.smtp.SmtpConnection;
import it.prms.greenmail.smtp.SmtpManager;
import it.prms.greenmail.smtp.SmtpState;

import java.io.IOException;

public abstract class SmtpCommand {

    public abstract void execute(SmtpConnection conn,
                                 SmtpState state,
                                 PuRoSmtpManager manager,
                                 String commandLine)
            throws IOException;
}
