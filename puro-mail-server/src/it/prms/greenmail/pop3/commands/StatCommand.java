/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package it.prms.greenmail.pop3.commands;


import it.prms.greenmail.pop3.Pop3Connection;
import it.prms.greenmail.pop3.Pop3State;
import it.prms.greenmail.store.PuRoHierarchicalFolder;
import it.prms.greenmail.store.PuRoMessage;

import java.util.Iterator;
import java.util.List;



public class StatCommand extends Pop3Command 
{
    public boolean isValidForState(Pop3State state) 
    {
        return state.isAuthenticated();
    }

    public void execute(Pop3Connection conn, Pop3State state, String cmd) 
    {
		try 
		{
			PuRoHierarchicalFolder inbox = state.getFolder();
			
			List<PuRoMessage> messages = inbox.getUidAndSizeOnly();
			long size = sumMessageSizes(messages);
			
			conn.println("+OK " + messages.size() + " " + size);
		} 
		catch (Exception me) 
		{ conn.println("-ERR " + me); }
	}
    
    long sumMessageSizes(List<PuRoMessage> messages) 
    {
        long total = 0;

        for (Iterator<PuRoMessage> i = messages.iterator(); i.hasNext();) 
        {
        	PuRoMessage msg = (PuRoMessage) i.next();            
            total += msg.getSize();
        }

        return total;
    }
    
}