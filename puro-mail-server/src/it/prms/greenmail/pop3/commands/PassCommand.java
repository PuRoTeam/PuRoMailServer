/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package it.prms.greenmail.pop3.commands;

import it.prms.greenmail.pop3.Pop3Connection;
import it.prms.greenmail.pop3.Pop3State;
import it.prms.greenmail.user.PuRoMailUser;



public class PassCommand //(*) ogni volta qui viene settato la folder (rileggendola da DynamoDB)
        extends Pop3Command {
    public boolean isValidForState(Pop3State state) {

        return !state.isAuthenticated();
    }

    public void execute(Pop3Connection conn, Pop3State state,
                        String cmd) {
    	PuRoMailUser user = state.getUser();
        if (user == null) {
            conn.println("-ERR USER required");

            return;
        }

        String[] args = cmd.split(" ");
        if (args.length < 2) {
            conn.println("-ERR Required syntax: PASS <username>");

            return;
        }

        try {
            String pass = args[1];             
            state.authenticate(pass); //(*) verifica la correttezza della password e l'esistenza della inbox e setta, se andato a buon fine, i parametri di Pop3State (la folder)
            
            conn.println("+OK");
        } catch (Exception e) {
            conn.println("-ERR Authentication failed: " + e);
                                 
            state.setUser(null);  //riparto dal comando USER, evita errori del tipo USER A(ok), PASS A(no), USER B(ok), PASS B(ok) -> USER A in memoria ma ineliminabile
            state.getUserManager().removeConnectedUser(user);
        }
    }
}