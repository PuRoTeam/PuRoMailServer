package it.prms.greenmail.user;

import it.prms.amazon.utility.UserResult;
import it.prms.greenmail.imap.PuRoImapHostManager;
import it.prms.greenmail.mail.MovingMessage;
import it.prms.greenmail.store.PuRoHierarchicalFolder;
import it.prms.greenmail.store.PuRoStore;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;

import com.amazonaws.services.s3.model.Region;

public class PuRoMailUser {
	
	private String email;	//KEY
	private String password;
	private String firstname;
	private String lastname;
	private ArrayList<String> folder;
	private Date lastupdate;
    //private ArrayList<Region> region;
	
	private int countConnectedUser;
    
    private PuRoImapHostManager imapHostManager;

   
   public PuRoMailUser(String email, String password, String firstname, String lastname, Date creationDate, PuRoImapHostManager imapHostManager) {
    	this.email = email;
        this.password = password;
		this.firstname = firstname;
		this.lastname = lastname;
        this.folder = new ArrayList<String>();
        this.lastupdate = new Date(creationDate.getTime());
        //this.region = new ArrayList<Region>();
        this.imapHostManager = imapHostManager;
        
        this.countConnectedUser = 0;
   }
   
   public PuRoMailUser(UserResult userResult, PuRoImapHostManager imapHostManager) {
	   email = userResult.getEmail();
	   password = userResult.getPassword();
	   firstname = userResult.getFirstname();
	   lastname = userResult.getLastname();    		
	   lastupdate = userResult.getLastUpdate();				
	   folder = new ArrayList<String>();
	   
	   ArrayList<String> userResultFolder = userResult.getFolder();		
		for(int i = 0; i < userResultFolder.size(); i++)
			folder.add(userResultFolder.get(i));
		
		this.imapHostManager = imapHostManager;
		
		countConnectedUser = 0;
   }
   
   /**
    * Incrementa il contatore del numero di connessioni utilizzanti l'user corrente
    */
   public synchronized int addConnectedUser() {
	   countConnectedUser++;
	   return countConnectedUser;
   }
   
   /**
    * Decrementa il contatore del numero di connessioni utilizzanti l'user corrente
    */
   public synchronized int removeConnectedUser() {
	   if(countConnectedUser > 0)
		   countConnectedUser--;
	   return countConnectedUser;
   }
   
   public int getConnectedUser() {
	   return countConnectedUser;
   }
   
   public String getEmail() {
	   return email;
   }
   
   public void setEmail(String email) {
	   this.email = email;
   }
   
	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Date getLastUpdate() {
		return lastupdate;
	}
	
	public void setLastUpdate(Date lastupdate) {
		this.lastupdate = new Date(lastupdate.getTime());
	}
	
	public ArrayList<String> getFolder() {
		return folder;
	}
	
	/*public ArrayList<Region> getRegion() {
	return region;
	}
	
	//Restituisce un Array contenente il nome di ogni regione sotto forma di Stringa (Region.US_Standard ha valore null)
	public String[] getStringRegion(){
		String[] user_region = new String[region.size()];
		
		for(int i = 0; i < region.size(); i++)
			user_region[i] = region.get(i).toString();
					
		return user_region;		
	}	
	
	public void setRegion(ArrayList<Region> region) {
		this.region = region;
	}*/

	/**
	 * Richiamata dal comando DATA di SMTP, salva un messaggio nella INBOX dell'utente
	 */	
    public void deliver(MovingMessage msg) //usato SOLO in SMTP, quindi posso richiamare la funzione folder.addViewer (tanto l'unico caso d'uso è questo)
            throws UserException {
    	PuRoStore store = imapHostManager.getStore();
    	PuRoHierarchicalFolder inbox = null;
        try {
        	inbox = imapHostManager.getInbox(this);
        	inbox.store(msg);        	
        } catch (Exception me) {
            throw new UserException(me);
            //se si è verificato un errore, le cartelle sono già state rimosse in getInbox
        } finally {
        	if(inbox != null)
        		store.removeFolderAndParentViewer(inbox);
        }
    }

    public void authenticate(String pass)
            throws UserException {
    	String md5pass = md5sum(pass);
        if (!password.equals(md5pass))
            throw new UserException("Invalid password");
    }
	
    public String getQualifiedMailboxName() {
        return String.valueOf(email.hashCode());
    }

    public int hashCode() {
        return email.hashCode();
    }
	
    /**
     * Crea un hash md5sum della stringa passata per parametro     
     * 
     */
    public  String md5sum(String s){
		
		StringBuffer sb = new StringBuffer();
		
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
	        byte[] mdbytes = md.digest(s.getBytes("UTF-8"));
	 
	        //convert the byte to hex format method 1
	        for (int i = 0; i < mdbytes.length; i++) {
	          sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
	        }
	 	 
	        //convert the byte to hex format method 2
	        StringBuffer hexString = new StringBuffer();
	    	for (int i=0;i<mdbytes.length;i++) {
	    		String hex=Integer.toHexString(0xff & mdbytes[i]);
	   	     	if(hex.length()==1) hexString.append('0');
	   	     	hexString.append(hex);
	    	}
	    	
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		return sb.toString();
	}
}