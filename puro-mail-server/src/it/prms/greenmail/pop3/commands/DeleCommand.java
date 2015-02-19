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

import java.util.List;

import javax.mail.Flags;


public class DeleCommand
        extends Pop3Command {
    public boolean isValidForState(Pop3State state) {

        return state.isAuthenticated();
    }
    
    public void execute(Pop3Connection conn, Pop3State state,
                        String cmd) {
        try {
            PuRoHierarchicalFolder inbox = state.getFolder();
            String[] cmdLine = cmd.split(" "); //(*) DELE 1

            String msgNumStr = cmdLine[1];
            boolean retrieveCompleteMsg = false; //sono interessato solo ai flag
            List<PuRoMessage> msgList = inbox.getMessages(new MsgRangeFilter(msgNumStr, false), retrieveCompleteMsg);
            if (msgList.size() != 1) {
                conn.println("-ERR no such message");

                return;
            }

            PuRoMessage msg = (PuRoMessage) msgList.get(0);
            Flags flags = msg.getFlags();

            if (flags.contains(Flags.Flag.DELETED)) {
                conn.println("-ERR message already deleted");

                return;
            }

            flags.add(Flags.Flag.DELETED);

            conn.println("+OK message scheduled for deletion");
        } catch (Exception e) {
            conn.println("-ERR " + e);
        }
    }
}