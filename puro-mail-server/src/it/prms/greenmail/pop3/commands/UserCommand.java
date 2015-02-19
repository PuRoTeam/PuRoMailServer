/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package it.prms.greenmail.pop3.commands;

import it.prms.greenmail.pop3.Pop3Connection;
import it.prms.greenmail.pop3.Pop3State;
import it.prms.greenmail.user.PuRoMailUser;
import it.prms.greenmail.user.UserException;



public class UserCommand
        extends Pop3Command {
    public boolean isValidForState(Pop3State state) {

        return !state.isAuthenticated();
    }

    public void execute(Pop3Connection conn, Pop3State state,
                        String cmd) {
        try {
            String[] args = cmd.split(" ");
            if (args.length < 2) {
                conn.println("-ERR Required syntax: USER <username>");

                return;
            }

            String username = args[1];

            if(state.getUser() != null) //evita errori del tipo USER A(ok), USER B(ok), PASS B(ok) -> User A in memoria ma ineliminabile
            	state.getUserManager().removeConnectedUser(state.getUser());
            
            PuRoMailUser user = state.getUser(username);            
            state.setUser(user);
                        
            conn.println("+OK");
        } catch (UserException nsue) {
            conn.println("-ERR " + nsue);
        }
    }
}