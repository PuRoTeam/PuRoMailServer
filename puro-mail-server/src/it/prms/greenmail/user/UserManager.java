package it.prms.greenmail.user;

import it.prms.greenmail.imap.ImapHostManager;


public interface UserManager 
{		
    public abstract GreenMailUser getUser(String login);

    public abstract GreenMailUser getUserByEmail(String email);

    public abstract GreenMailUser createUser(String name, String login, String password) throws UserException;

    public abstract void deleteUser(GreenMailUser user) throws UserException;

    public abstract boolean test(String userid, String password);
    
    public abstract ImapHostManager getImapHostManager();
}
