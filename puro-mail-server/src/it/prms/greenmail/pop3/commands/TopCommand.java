/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package it.prms.greenmail.pop3.commands;


import it.prms.greenmail.foedus.util.MsgRangeFilter;
import it.prms.greenmail.pop3.Pop3Connection;
import it.prms.greenmail.pop3.Pop3State;
import it.prms.greenmail.store.PuRoHierarchicalFolder;
import it.prms.greenmail.store.PuRoMessage;
import it.prms.greenmail.util.GreenMailUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;



public class TopCommand
        extends Pop3Command {
    public boolean isValidForState(Pop3State state) {

        return state.isAuthenticated();
    }
    //TODO DA FARE
    //(*)the POP3 server sends the headers of the message, the blank line separating the headers from the body, and then the
    //(*) number of lines of the indicated message's body, being careful to byte-stuff the termination character
    public void execute(Pop3Connection conn, Pop3State state,
                        String cmd) {
        try {
            PuRoHierarchicalFolder inbox = state.getFolder();
            String[] cmdLine = cmd.split(" ");
            if (cmdLine.length < 3)
                throw new IllegalArgumentException("range and line count required");

            String msgNumStr = cmdLine[1];
            boolean retrieveCompleteMsg = true; //voglio tutto il messaggio (di cui poi prendo solo tot linee di body)
            List<PuRoMessage> msgList = inbox.getMessages(new MsgRangeFilter(msgNumStr, false), retrieveCompleteMsg);
            if (msgList.size() != 1) {
                conn.println("-ERR no such message");

                return;
            }

            PuRoMessage msg = (PuRoMessage) msgList.get(0);

            int numLines = Integer.parseInt(cmdLine[2]);

            BufferedReader in = new BufferedReader(new StringReader(GreenMailUtil.getWholeMessage(msg.getMimeMessage())));

            conn.println("+OK");

            copyHeaders(in, conn);
            copyLines(in, conn, numLines);
            in.close();
            conn.println(".");
        } catch (Exception e) {
            conn.println("-ERR " + e);
        }
    }

    void copyHeaders(BufferedReader in, Pop3Connection conn)
            throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            conn.println(line);
            if ("".equals(line))

                break;
        }
    }

    void copyLines(BufferedReader in, Pop3Connection conn,
                   int numLines)
            throws IOException {
        int count = 0;
        String line;
        while ((line = in.readLine()) != null && count < numLines) {
            conn.println(line);
            count++;
        }
    }
}