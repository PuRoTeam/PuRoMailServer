/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package it.prms.greenmail.smtp;



import it.prms.greenmail.imap.ImapHostManager;
import it.prms.greenmail.mail.MailAddress;
import it.prms.greenmail.mail.MovingMessage;
import it.prms.greenmail.user.GreenMailUser;
import it.prms.greenmail.user.UserException;
import it.prms.greenmail.user.UserManager;

import javax.mail.Message;
import javax.mail.MessagingException;


import java.util.*;


public class SmtpManager {
    private Incoming _incomingQueue;
    private UserManager userManager;
    private ImapHostManager imapHostManager;
    private Vector notifyList;

    public SmtpManager(ImapHostManager imapHostManager, UserManager userManager) {
        this.imapHostManager = imapHostManager;
        this.userManager = userManager;
        _incomingQueue = new Incoming();
        notifyList = new Vector();
    }

   //(*) smtp.libero.it non effettua mai un controllo sul sender
	public String checkSender(SmtpState state, MailAddress sender) {
        //always ok
        return null;
    }

	//(*) smtp.libero.it verifica che il destinatario esista unicamente se esso appartiene al dominio libero.it
	//(*) se non appartiene al dominio e successivamente non riesce ad inviare il messaggio, allora dovrebbe mandare una mail indietro al mittente
    public String checkRecipient(SmtpState state, MailAddress rcpt) {
    	
        //MailAddress sender = state.getMessage().getReturnPath();
        //return null;
    	
    	String err = "550 No such user here";
    	
    	/*String domain = rcpt.getHost();    	
    	String server_domain = "puro-mail-server.tk"; //leggilo da file di configurazione
    	    	
    	if(domain.equalsIgnoreCase(server_domain))
    	{
    		User rcpt_user = userManager.getUserByEmail(rcpt.getEmail());
    		
    		if(rcpt_user != null) //user esistente
    			return null;
    		else
    			return err;
    	}
    	else
    	{
    		return null;
    	}*/
    	
    	if(true)
    		return null;
    	else    	
    		return err; 
    }
  
	public String checkData(SmtpState state) {

        return null;
    }
        
	public synchronized void send(SmtpState state) {
        _incomingQueue.enqueue(state.getMessage());
        for (int i = 0; i < notifyList.size(); i++) {
            WaitObject o = (WaitObject) notifyList.get(i);
            synchronized (o) {
                o.emailReceived();
            }
        }
    }

    /**
     * @return null if no need to wait. Otherwise caller must call wait() on the returned object
     */
    public synchronized WaitObject createAndAddNewWaitObject(int emailCount) {
        final int existingCount = imapHostManager.getAllMessages().size();
        if (existingCount >= emailCount) {
            return null;
        }
        WaitObject ret = new WaitObject(emailCount - existingCount);
        notifyList.add(ret);
        return ret;
    }

    //~----------------------------------------------------------------------------------------------------------------
    /**
     * This Object is used by a thread to wait until a number of emails have arrived.
     * (for example Server's waitForIncomingEmail method)
     *
     * Every time an email has arrived, the method emailReceived() must be called.
     *
     * The notify() or notifyALL() methods should not be called on this object unless
     * you want to notify waiting threads even if not all the required emails have arrived.
     *
     */
    public static class WaitObject {
        private boolean arrived = false;
        private int emailCount;

        public WaitObject(int emailCount) {
            this.emailCount = emailCount;
        }

        public int getEmailCount() {
            return emailCount;
        }

        public boolean isArrived() {
            return arrived;
        }

        private void setArrived() {
            arrived = true;
        }

        public void emailReceived()
        {
            emailCount--;
            if (emailCount<=0) {
                setArrived();
                this.notifyAll();
            }
        }
    }

    private class Incoming {
        boolean _stopping;


        public void enqueue(MovingMessage msg) {
            Iterator iterator = msg.getRecipientIterator();
            String tos = ""; //(*) insieme degli utenti destinatari
            while (iterator.hasNext()) {
                MailAddress username = (MailAddress) iterator.next();
                if (tos.length()>0) {
                    tos+=",";
                }
                tos+=username; //(*) destinatario_1,destinatario_2,...,destinatario_n
            }
            try {
                msg.getMessage().addRecipients(Message.RecipientType.TO,tos); //(*) aggiungo tutti i destinatario al messaggio
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
            iterator = msg.getRecipientIterator();
            while (iterator.hasNext()) {
                MailAddress username = (MailAddress) iterator.next();
                handle(msg, username); //(*) consegna il messaggio ad ogni utente destinatario
            }

        }


        private void handle(MovingMessage msg, MailAddress mailAddress) {
            try {
                try {
                    GreenMailUser user = userManager.getUserByEmail(mailAddress.getEmail());
                    if (null == user) { //(*) serve solo nel caso di store in memoria

                        user = userManager.createUser(mailAddress.getEmail(),mailAddress.getEmail(), mailAddress.getEmail());
                    }

                    user.deliver(msg);

                } catch (UserException e) {
                    throw new RuntimeException(e);
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            msg.releaseContent();
        }
    }
}