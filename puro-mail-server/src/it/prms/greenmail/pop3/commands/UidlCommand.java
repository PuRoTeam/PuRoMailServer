/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package it.prms.greenmail.pop3.commands;


import it.prms.greenmail.foedus.util.MsgRangeFilter;
import it.prms.greenmail.pop3.Pop3Connection;
import it.prms.greenmail.pop3.Pop3State;
import it.prms.greenmail.store.FolderException;
import it.prms.greenmail.store.PuRoHierarchicalFolder;
import it.prms.greenmail.store.PuRoMessage;

import java.util.Iterator;
import java.util.List;



public class UidlCommand
        extends Pop3Command {
    public boolean isValidForState(Pop3State state) {

        return state.isAuthenticated();
    }

    /*
      (*)
      If an argument was given and the POP3 server issues a positive
          response with a line containing information for that message.
          This line is called a "unique-id listing" for that message.

          If no argument was given and the POP3 server issues a positive
          response, then the response given is multi-line.  After the
          initial +OK, for each message in the maildrop, the POP3 server
          responds with a line containing information for that message.
          This line is called a "unique-id listing" for that message.

          In order to simplify parsing, all POP3 servers are required to
          use a certain format for unique-id listings.  A unique-id
          listing consists of the message-number of the message,
          followed by a single space and the unique-id of the message.
    
    */
    
    public void execute(Pop3Connection conn, Pop3State state, String cmd) 
    {
		try 
		{
			PuRoHierarchicalFolder inbox = state.getFolder();
			String[] cmdLine = cmd.split(" ");
			
			if (cmdLine.length > 1) 
			{
				String msgNumStr = cmdLine[1];
				List<PuRoMessage> msgList = inbox.getUidAndSizeOnly(new MsgRangeFilter(msgNumStr, false));
				
				if (msgList.size() != 1) 
				{
					conn.println("-ERR no such message");					
					return;
				}
				
				PuRoMessage msg = (PuRoMessage) msgList.get(0);
				conn.println("+OK " + msgNumStr + " " + msg.getUid());
			}
			else 
			{
				List<PuRoMessage> messages = inbox.getUidAndSizeOnly();
				
				conn.println("+OK");
				for (Iterator<PuRoMessage> i = messages.iterator(); i.hasNext();) 
				{
					PuRoMessage msg = (PuRoMessage) i.next();
					conn.println(inbox.getMsn(msg.getUid()) + " " + msg.getUid());
				}				
				conn.println(".");
			}
		} 
		catch (FolderException me) 
		{ conn.println("-ERR " + me); }
	}
    
}