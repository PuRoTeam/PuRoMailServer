/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package it.prms.greenmail.smtp.commands;


import it.prms.greenmail.foedus.util.StreamUtils;
import it.prms.greenmail.mail.MovingMessage;
import it.prms.greenmail.smtp.PuRoSmtpManager;
import it.prms.greenmail.smtp.SmtpConnection;
import it.prms.greenmail.smtp.SmtpManager;
import it.prms.greenmail.smtp.SmtpState;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;

import javax.mail.MessagingException;


/**
 * DATA command.
 * <p/>
 * <p/>
 * The spec is at <a
 * href="http://asg.web.cmu.edu/rfc/rfc2821.html#sec-4.1.1.4">
 * http://asg.web.cmu.edu/rfc/rfc2821.html#sec-4.1.1.4 </a>.
 * </p>
 */
public class DataCommand extends SmtpCommand {
    public void execute(SmtpConnection conn, SmtpState state,
                        PuRoSmtpManager manager, String commandLine)
            throws IOException {
        MovingMessage msg = state.getMessage();

        if (msg.getReturnPath() == null) {
            conn.println("503 MAIL command required");

            return;
        }

        if (!msg.getRecipientIterator().hasNext()) {
            conn.println("503 RCPT command(s) required");

            return;
        }

        conn.println("354 Start mail input; end with <CRLF>.<CRLF>");

        String value = "Return-Path: <" + msg.getReturnPath() +
                ">\r\n" + "Received: from " +
                conn.getClientAddress() + " (HELO " +
                conn.getHeloName() + "); " +
                new java.util.Date() + "\r\n";

        msg.readDotTerminatedContent(new BufferedReader(StreamUtils.splice(new StringReader(value),
                conn.getReader())));
        
        String err = manager.checkData(state); //restituisce sempre errore nullo
        if (err != null) {
            conn.println(err);

            return;
        }

        try {
            manager.send(state);
            conn.println("250 OK");
        } catch (Exception je) {
            conn.println("" + je);
        }

        state.clearMessage();
    }
}