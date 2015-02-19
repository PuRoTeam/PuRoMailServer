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

import java.io.StringReader;
import java.util.List;

import javax.mail.Flags;


public class RetrCommand
        extends Pop3Command {
    public boolean isValidForState(Pop3State state) 
    {
        return state.isAuthenticated();
    }

    public void execute(Pop3Connection conn, Pop3State state,
    String cmd) 
    {
		try
		{
			PuRoHierarchicalFolder inbox = state.getFolder();
			String[] cmdLine = cmd.split(" "); //(*) RETR 1 (recupera il messaggio col message number pari a 1)
			
			String msgNumStr = cmdLine[1];
			boolean retrieveCompleteMsg = true; //voglio tutto il messaggio
			List<PuRoMessage> msgList = inbox.getMessages(new MsgRangeFilter(msgNumStr, false), retrieveCompleteMsg); //recupera il singolo messaggio dalla inbox
			
			if (msgList.size() != 1) 
			{
				conn.println("-ERR no such message");			
				return;
			}
			
			PuRoMessage msg = (PuRoMessage) msgList.get(0);
			String email = GreenMailUtil.getWholeMessage(msg.getMimeMessage());
			conn.println("+OK");
			conn.print(new StringReader(email)); //scrive il messaggio sulla connessione
			conn.println();
			conn.println(".");
			msg.getFlags().add(Flags.Flag.SEEN); //segna il messaggio SimpleStoredMessage come già letto
		} catch (Exception e) {
		conn.println("-ERR " + e);
		}
    }	    
}