package it.prms.greenmail.user;

import it.prms.greenmail.imap.ImapHostManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class UserManagerImpl implements UserManager {
	
    Map _users = Collections.synchronizedMap(new HashMap());
    private ImapHostManager imapHostManager;
    
    public UserManagerImpl(ImapHostManager imapHostManager) {
        this.imapHostManager = imapHostManager;
    }

    public GreenMailUser getUser(String login) {
    	    	
    	//if(_users.get(login) != null)
    	//	System.out.println("getUser: " + ((GreenMailUser)_users.get(login)).getLogin());
    	
        return (GreenMailUser) _users.get(login);
    }

    public GreenMailUser getUserByEmail(String email) {
        GreenMailUser ret = getUser(email);
        if (null == ret) {
            for (Iterator it = _users.values().iterator(); it.hasNext();) {
                GreenMailUser u = (GreenMailUser) it.next();
                if (u.getEmail().trim().equalsIgnoreCase(email.trim())) {
                    return u;
                }
            }
        }
        return ret;
    }

    public GreenMailUser createUser(String name, String login, String password) throws UserException {
        GreenMailUser user = new UserImpl(name, login, password, imapHostManager);
        user.create();
        addUser(user);
                
        //System.out.println("createUser: creato " + this.getUser(user.getLogin()).getLogin());
        
        return user;
    }

    private void addUser(GreenMailUser user) {
        _users.put(user.getLogin(), user);
    }

    public void deleteUser(GreenMailUser user)
            throws UserException {
        user = (GreenMailUser) _users.remove(user.getLogin());
        if (user != null)
            user.delete();
    }

    public boolean test(String userid, String password) {
        GreenMailUser u = getUser(userid);
        if (null == u) {
            return false;
        }

        return u.getPassword().equals(password);
    }

    public ImapHostManager getImapHostManager() {
        return imapHostManager;
    }

}
