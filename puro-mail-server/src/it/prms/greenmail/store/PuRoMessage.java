package it.prms.greenmail.store;

import it.prms.amazon.utility.MetaMail;

import java.util.Date;

import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

public class PuRoMessage {
    private MimeMessage mimeMessage;
    private String folder;
    private long uid;
    private String bucket;
    private String objectname;
    private String header;
    private int size;
    private Date timeStamp;
    private Flags flags;
    private SimpleMessageAttributes attributes;
    private boolean isSet; //se false, sono stati recuperati soltanto l'uid e la size, altrimenti sono stati recuperati tutti i campi, più l'oggetto su S3
	
    public PuRoMessage(long uid, int size) {
    	this.uid = uid;
    	this.size = size;
    	//timeStamp = new Date();
    	flags = new Flags(); //necessario per aggiungere flag DELETED a messaggi parzialmente recuperati da db
    	isSet = false;
    }
    
    public PuRoMessage(MimeMessage mimeMessage, MetaMail metaMail) {
    	setMailFields(mimeMessage, metaMail);
	}
    
    public void setMailFields(MimeMessage mimeMessage, MetaMail metaMail) {
    	this.mimeMessage = mimeMessage;
    	folder = metaMail.getFolder();
    	uid = metaMail.getUid();
    	bucket = metaMail.getBucket();
    	objectname = metaMail.getFilename();
    	header = metaMail.getHeader();
    	size = metaMail.getSize();
    	timeStamp = metaMail.getDate();
    	mergeOldAndNewFlags(metaMail.getFlags());
    	isSet = true;
    }
    
    /**
     * Se avevo aggiunto flag prima di recuperare tutto il messaggio, è necessario non sovrascriverli, ma unire nuovi con vecchi flag    
     * @param newFlags
     */
    public void mergeOldAndNewFlags(Flags newFlags) {
    	if(flags != null) {
			Flags oldFlags = flags;
			flags = newFlags;
			Flag[] systemOld = oldFlags.getSystemFlags();
			String[] userOld = oldFlags.getUserFlags();
			
			for(int i = 0; i < systemOld.length; i++)
				flags.add(systemOld[i]);
			
			for(int i = 0; i < userOld.length; i++)
				flags.add(userOld[i]);
    	}
    	else
    		flags = newFlags;
    }   
    
    public MimeMessage getMimeMessage() {
        return mimeMessage;
    }

    public String getFolder() {
    	return folder;
    }
    
    public long getUid() {
        return uid;
    }
    
    public String getBucket() {
    	return bucket;
    }
    
    public String getObjectName() {
    	return objectname;
    }
    
    public String getHeader() {
    	return header;
    }
    
	public int getSize() {
		return size;
	} 

	public void setSize(int size) {
		this.size = size;
	}
	
    public Date getTimeStamp() {
        return timeStamp;
    }
    
    public Flags getFlags() {
        return flags;
    }
        
    public MailMessageAttributes getAttributes() throws FolderException {
        if (attributes == null) {
            attributes = new SimpleMessageAttributes();
            try {
                attributes.setAttributesFor(mimeMessage);
            } catch (MessagingException e) {
                throw new FolderException("Could not parse mime message." + e.getMessage());
            }
        }
        return attributes;
    }
    
    public boolean isSet() {
    	return isSet;
    }

}
