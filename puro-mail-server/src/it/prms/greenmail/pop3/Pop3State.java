/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package it.prms.greenmail.pop3;

import it.prms.greenmail.imap.PuRoImapHostManager;
import it.prms.greenmail.store.FolderException;
import it.prms.greenmail.store.PuRoHierarchicalFolder;
import it.prms.greenmail.store.PuRoStore;
import it.prms.greenmail.user.NoSuchUserException;
import it.prms.greenmail.user.PuRoMailUser;
import it.prms.greenmail.user.PuRoUserManager;
import it.prms.greenmail.user.UserException;



public class Pop3State {
	PuRoUserManager _manager;
    PuRoMailUser _user;
    PuRoStore store;
    PuRoHierarchicalFolder _inbox;
    private PuRoImapHostManager imapHostManager;

    public Pop3State(PuRoUserManager manager) {
        _manager = manager;
        imapHostManager = manager.getImapHostManager();
        store = imapHostManager.getStore();
    }

    public PuRoMailUser getUser() {

        return _user;
    }

    public PuRoMailUser getUser(String username) throws UserException {
        PuRoMailUser user = _manager.getUser(username);
        if (null == user) {
            throw new NoSuchUserException(username + " doesn't exist");
        }
        return user;
    }

    public void setUser(PuRoMailUser user) {
        _user = user;
    }

    public boolean isAuthenticated() {
        return _inbox != null;
    }

    public void authenticate(String pass) //richiamato solo da comando PASS
            throws UserException, FolderException {
        if (_user == null)
            throw new UserException("No user selected");

        _user.authenticate(pass); //(*) confronta la password con quella dell'utente
        _inbox = imapHostManager.getInbox(_user); //(*) restituisce oggetto HierarchicalFolder dal nome "INBOX" (per SMTP e POP3)               
    }

    public PuRoHierarchicalFolder getFolder() {
        return _inbox;
    }
    
    public PuRoStore getStore() {
    	return store;
    }
    
    public PuRoUserManager getUserManager() {
    	return _manager;
    }
    
    public void resetState() {
    	_user = null;
    	store = null;
        _inbox = null;    
    }
}