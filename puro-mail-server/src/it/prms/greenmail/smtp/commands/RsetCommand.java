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
 * RSET command.
 * <p/>
 * <p/>
 * The spec is at <a
 * href="http://asg.web.cmu.edu/rfc/rfc2821.html#sec-4.1.1.5">
 * http://asg.web.cmu.edu/rfc/rfc2821.html#sec-4.1.1.5</a>.
 * </p>
 */
public class RsetCommand //(*) dopo RSET lo stato viene azzerato e si riparte dal comando MAIL
        extends SmtpCommand {
    public void execute(SmtpConnection conn, SmtpState state,
    					PuRoSmtpManager manager, String commandLine) {
        state.clearMessage();
        conn.println("250 OK");
    }//(*) SE INVIO RSET DOPO IL COMANDO DATA, IN OGNI CASO IL MESSAGGIO E' STATO GIA' MANDATO (funziona così anche su libero)
}