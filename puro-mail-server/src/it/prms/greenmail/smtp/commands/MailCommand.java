/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package it.prms.greenmail.smtp.commands;


import it.prms.greenmail.mail.MailAddress;
import it.prms.greenmail.smtp.PuRoSmtpManager;
import it.prms.greenmail.smtp.SmtpConnection;
import it.prms.greenmail.smtp.SmtpState;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.AddressException;


/**
 * MAIL command.
 * <p/>
 * <p/>
 * The spec is at <a
 * href="http://asg.web.cmu.edu/rfc/rfc2821.html#sec-4.1.1.2">
 * http://asg.web.cmu.edu/rfc/rfc2821.html#sec-4.1.1.2</a>.
 * </p>
 */
public class MailCommand
        extends SmtpCommand {
    static final Pattern param = Pattern.compile("MAIL FROM:\\s?<(.*)>",
            Pattern.CASE_INSENSITIVE);
	
    public void execute(SmtpConnection conn, SmtpState state,
    					PuRoSmtpManager manager, String commandLine) {    	
    	
    	int index = commandLine.lastIndexOf(">");
    	
    	String commandLineCopy = commandLine;
    	   
    	if(index != -1)
    		commandLineCopy = commandLine.substring(0, index + 1); //non considero la parte relativa alla SIZE del messaggio
    	
        Matcher m = param.matcher(commandLineCopy);
        try {
            if (m.matches()) {
                String from = m.group(1);

                MailAddress fromAddr = new MailAddress(from);

                //(*) da implementare (non credo)
                String err = manager.checkSender(state, fromAddr); //(*) restituisce sempre un errore "null" per segnalare che è tutto ok. Da modificare!
                if (err != null) {
                    conn.println(err);

                    return;
                }

                state.clearMessage(); //(*) cancello il messaggio precedente e creo il nuovo (viene gestito in memoria chiaramente)
                state.getMessage().setReturnPath(fromAddr);
               
                conn.println("250 OK");
            } else {
                conn.println("501 Required syntax: 'MAIL FROM:<email@host>'");
            }
        } catch (AddressException e) {
            conn.println("501 Malformed email address. Use form email@host");
        }
    }
}