package it.prms.greenmail;

import it.prms.amazon.services.DynamoDB;
import it.prms.amazon.services.S3;
import it.prms.amazon.utility.AmazonEndPoint;
import it.prms.greenmail.imap.PuRoImapHostManager;
import it.prms.greenmail.smtp.PuRoSmtpManager;
import it.prms.greenmail.store.PuRoStore;
import it.prms.greenmail.user.PuRoUserManager;

import java.io.FileNotFoundException;
import java.io.IOException;

public class PuRoManagers 
{
	private PuRoStore store;
	
	private PuRoConfig config;
	
    private PuRoImapHostManager imapHostManager;
    private PuRoUserManager userManager;
    private PuRoSmtpManager smtpManager;
    
    /**
     * Costruisce i gestori per i protocolli POP3, SMTP, IMAP
     * 
     * @param credentials_path
     * @param dynamoDBEndPoint
     * @param s3EndPoint
     * @throws FileNotFoundException
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public PuRoManagers(String credentials_path, AmazonEndPoint dynamoDBEndPoint, AmazonEndPoint s3EndPoint) throws FileNotFoundException, IllegalArgumentException, IOException
    {
    	store = new PuRoStore(new DynamoDB(credentials_path, dynamoDBEndPoint), new S3(credentials_path, s3EndPoint));
    
    	config = new PuRoConfig(store.getDynamoDB());
    	
    	imapHostManager = new PuRoImapHostManager(store);
    	userManager = new PuRoUserManager(imapHostManager, config, store.getDynamoDB());
        smtpManager = new PuRoSmtpManager(imapHostManager, userManager, config);
    }
    
    public PuRoConfig getConfig()
    {
    	return config;
    }

    public PuRoStore getStore()
    {
    	return store;
    }
    
    public PuRoSmtpManager getSmtpManager() 
    {
        return smtpManager;
    }

    public PuRoUserManager getUserManager() 
    {
        return userManager;
    }

    public PuRoImapHostManager getImapHostManager() 
    {
        return imapHostManager;
    }
}
