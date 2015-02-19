package it.prms.greenmail.smtp;

import it.prms.greenmail.PuRoConfig;
import it.prms.greenmail.imap.PuRoImapHostManager;
import it.prms.greenmail.mail.MailAddress;
import it.prms.greenmail.mail.MovingMessage;
import it.prms.greenmail.user.PuRoMailUser;
import it.prms.greenmail.user.PuRoUserManager;
import it.prms.greenmail.user.UserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Iterator;

import javax.mail.Message;
import javax.mail.MessagingException;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

public class PuRoSmtpManager {
    private Incoming _incomingQueue;
    private PuRoUserManager userManager;
    private PuRoImapHostManager imapHostManager;
    private PuRoConfig config;
    
    public PuRoSmtpManager(PuRoImapHostManager imapHostManager, PuRoUserManager userManager, PuRoConfig config) {
    	_incomingQueue = new Incoming();    	
    	this.imapHostManager = imapHostManager;
        this.userManager = userManager;        
        this.config = config;
    }

	public String checkSender(SmtpState state, MailAddress sender) {
        //always ok
        return null;
    }

    public String checkRecipient(SmtpState state, MailAddress rcpt) {
    	//always ok
    	String err = "550 No such user here";
    	
    	if(true)
    		return null;
    	else    	
    		return err; 
    }
  
	public String checkData(SmtpState state) {

        return null;
    }
        
	public synchronized void send(SmtpState state) throws Exception {
        _incomingQueue.enqueue(state.getMessage());
    }

	public PuRoImapHostManager getImapHostManager() {
		return imapHostManager;
	}
	
	public PuRoUserManager getUserManager() {
		return userManager;
	}
	
	public PuRoConfig getConfig() {
		return config;
	}
	
    private class Incoming {

        public void enqueue(MovingMessage msg) 
        		throws MessagingException, UserException, SocketTimeoutException, TextParseException, UnknownHostException, IOException, ExtSmtpConnException {
            Iterator iterator = msg.getRecipientIterator();
            String tos = ""; //(*) insieme degli utenti destinatari
            while (iterator.hasNext()) {
                MailAddress username = (MailAddress) iterator.next();
                
                if (tos.length()>0) {
                    tos+=",";
                }
                tos+=username; //destinatario_1,destinatario_2,...,destinatario_n
            }
            try {
                msg.getMessage().addRecipients(Message.RecipientType.TO,tos); //aggiungo tutti i destinatario al messaggio
            } catch (MessagingException e) {
                throw new MessagingException("451 Server Error: local error in processing, requested action aborted");
            }
            iterator = msg.getRecipientIterator();
            while (iterator.hasNext()) {
                MailAddress username = (MailAddress) iterator.next();
                if(username.getHost().equals(config.getDomainName())){
                	handle(msg, username);
                }else{
                	handleExt(msg, username);
                }
            }
        }

        private void handle(MovingMessage msg, MailAddress mailAddress) throws UserException {
        	PuRoMailUser user = null;
            try {
                    user = userManager.getUser(mailAddress.getEmail());
                    if (null != user) {                    	
                    	user.deliver(msg);
                    }
                    else
                    	throw new UserException(); 
            } catch (Exception e) {
                throw new UserException("451 Server Error: local error in processing, requested action aborted");
            } finally {
            	if(user != null)
            		userManager.removeConnectedUser(user);
            }

            msg.releaseContent();
        }
        
        private void handleExt(MovingMessage msg, MailAddress mailAddress) 
        		throws TextParseException, SocketTimeoutException, UnknownHostException, IOException, MessagingException, ExtSmtpConnException {
        	/*
        	 * MAIL FROM:<mittente>
        	 * RCPT TO:<destinatario1>
        	 * RCPT TO:<destinatario2>
        	 * ....
        	 * DATA 
        	 * QUIT
        	 */
        	
    		Lookup lookup;
    		Record [] records;
    		
    		Socket socket = null;
	        PrintWriter out = null;
	        OutputStream outData = null;
	        BufferedReader in = null;
	        
			try {
				lookup = new Lookup(mailAddress.getHost(), Type.MX);
				records = lookup.run();    		
				
	    		if(records != null) {
	    			MXRecord mx = (MXRecord) records[0];
	    			    			
    	            socket = new Socket(mx.getTarget().toString(), 25);	//potrebbe non restituire un target giusto!!!
    	                	            
    	            out = new PrintWriter(socket.getOutputStream(), true);
    	            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    	            outData = socket.getOutputStream();
    	            checkReply(in.readLine());
    	                	            
    	            out.println("HELO misterpup.dyndns-wiki.com");
    	            checkReply(in.readLine());
    	            
    	            out.println("MAIL FROM:<"+mailAddress.getEmail()+">");
    	            checkReply(in.readLine());
    	            
    	        	for(int j=0; j<msg.getToAddresses().size(); ++j){
    	        		
    	        		out.println("RCPT TO:<"+msg.getToAddresses().get(j)+">");
    	        		checkReply(in.readLine());
    	        	}
    	        	
    	        	out.println("DATA");
    	        	checkReply(in.readLine());
    	        	    	        	
    			    msg.getMessage().writeTo(outData); //Forse problemi con outputstream
    			    
    			       			    
    	        	out.println(".");
    	        	checkReply(in.readLine());
    	        	
    	        	out.println("QUIT");
    	        	checkReply(in.readLine());
	    	
	    			out.close();
	    			outData.close();
	    			in.close();	    			
	    			socket.close();
	    		}
	    		else
	    			throw new ExtSmtpConnException("451 Server Error: error during address lookup, requested action aborted");
	    		
			} catch (TextParseException e) {
				throw new TextParseException("451 Server Error: error during address lookup, requested action aborted");
			} catch (SocketTimeoutException ste) {
				throw new SocketTimeoutException("451 Server Error: connection timeout, requested action aborted");
	        } catch (UnknownHostException e) {
	        	throw new UnknownHostException("451 Server Error: error during address lookup, requested action aborted");
	        } catch (IOException e) {
	        	throw new IOException("451 Server Error: local error in processing, requested action aborted");	    
	        } catch (MessagingException e) {
	        	throw new MessagingException("451 Server Error: local error in processing, requested action aborted");
			} catch (ExtSmtpConnException e) {
				throw e;
			}
			
            msg.releaseContent();
        }
        
        private void checkReply(String line) throws ExtSmtpConnException{
        	
        	if(line == null)
        		throw new ExtSmtpConnException("451 Server Error: error during communication with external SMTP server");
        	String c = line.substring(0, 1);
        	if(!c.equals("2") && !c.equals("3"))
        		throw new ExtSmtpConnException("451 Server Error: error during communication with external SMTP server");
        }
    }
}
