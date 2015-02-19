package it.prms.greenmail.user;

import it.prms.amazon.services.DynamoDB;
import it.prms.amazon.utility.TableInfo;
import it.prms.amazon.utility.UserResult;
import it.prms.amazon.utility.WrongTypeException;
import it.prms.greenmail.PuRoConfig;
import it.prms.greenmail.imap.PuRoImapHostManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodb.model.InternalServerErrorException;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodb.model.ResourceNotFoundException;

public class PuRoUserManager {

	private PuRoImapHostManager imapHostManager;
	private PuRoConfig config;
	private DynamoDB dynamoDB;
	Map<String, PuRoMailUser> usersMap = Collections.synchronizedMap(new HashMap<String, PuRoMailUser>());
	
	public PuRoUserManager(PuRoImapHostManager imapHostManager, PuRoConfig config, DynamoDB dynamoDB)
	{
		this.imapHostManager = imapHostManager;
		this.config = config;
		this.dynamoDB = dynamoDB;
	}
	
	/**
	 * Restituisce utente da DynamoDB. Nel caso di IMAP, controlla prima se l'utente in memoria è aggiornato, e in caso restituisce quello
	 * @param login
	 * @return
	 */
	public PuRoMailUser getUser(String login)
	{    	
    	String email = getEmailFromLoginString(login);
    	
		PuRoMailUser updatedUser = null;
    	
    	boolean isUpdated = false; //userIsUpdated(email); scommentare nel caso di IMAP
    	
		if(!isUpdated)
		{
			updatedUser = getUserFromDB(email);
			
			if(updatedUser != null)
			{
				usersMap.put(email, updatedUser);
			}
		}
		else
			updatedUser = usersMap.get(email);
				
		if(updatedUser != null)
			updatedUser.addConnectedUser();
		
		return updatedUser;
	}
	
	public String getEmailFromLoginString(String login)
	{
    	int index = login.indexOf("@");
    	
    	String email = "";
    	
    	if(index == -1)
    		email = login + "@" + config.getDomainName();
    	else
    		email = login;
    	
    	return email;
	}
	
	/**
	 * Controlla se l'utente in memoria con la mail specificata è aggiornato
	 * @param email
	 * @return
	 */
	private boolean userIsUpdated(String email)
	{
		PuRoMailUser inMemUser = usersMap.get(email);
		
		boolean isUpdated = false; 
		
		if(inMemUser != null)
		{
			String attributeToGet[] = {TableInfo.TUserLastUpdate.toString()};
			Map<String, AttributeValue> userItem = getUserAttributesFromDB(email, attributeToGet);
			
			AttributeValue lastUpdateDB = userItem.get(TableInfo.TUserLastUpdate.toString());
			
			if(lastUpdateDB != null) //utente in db, confrontare data in memoria con data in db
			{
				try
				{
					long lLastUpdateDB = Long.valueOf(lastUpdateDB.getN());
					Date dateLastUpdateDB = new Date(lLastUpdateDB);
					
					if(inMemUser.getLastUpdate().compareTo(dateLastUpdateDB) >= 0)
						isUpdated = true;
				}
				catch(NumberFormatException e)
				{ e.printStackTrace(); }
			}
		} 
		
		return isUpdated;
	}
		
	/**
	 * Recupera un utente da DynamoDB
	 * @param email
	 * @return
	 */
	private PuRoMailUser getUserFromDB(String email)
	{    	
    	PuRoMailUser user = null;
    	
    	String attributesToGet[] = new String[0];
    	Map<String, AttributeValue> userItem = getUserAttributesFromDB(email, attributesToGet);
    	
    	if(userItem != null)
    	{
    		UserResult userResult = new UserResult(userItem);
    		user = new PuRoMailUser(userResult, imapHostManager);
    	}
    	
    	return user;
	}
	
	/**
	 * Restituisce attributi utente da DynamoDB
	 * @param email
	 * @param attributesToGet
	 * @return
	 */	
	private Map<String, AttributeValue> getUserAttributesFromDB(String email, String attributesToGet[])
	{
		Map<String, AttributeValue> userItem = null;
		
		try
		{
			if(attributesToGet.length > 0)
				userItem = dynamoDB.retrieveUserItem(email, attributesToGet);
			else
				userItem = dynamoDB.retrieveUserItem(email);
		}
    	catch (ProvisionedThroughputExceededException e){
    		e.printStackTrace(); } 
    	catch (InternalServerErrorException e) 
    	{ e.printStackTrace(); } 
    	catch (ResourceNotFoundException e) 
    	{ e.printStackTrace(); } 
    	catch (AmazonServiceException e) 
    	{ e.printStackTrace(); } 
    	catch (AmazonClientException e) 
    	{ e.printStackTrace(); } 
    	catch (WrongTypeException e) 
    	{ e.printStackTrace(); }
		
		return userItem;
	}
	
	/**
	 * Utilzzato dal comando IMAP LOGIN, controlla che i dati login e password siano relativi ad un utente esistente
	 * @param login
	 * @param password
	 * @return l'utente autenticato
	 */
    public PuRoMailUser test(String login, String password) 
    {
    	String email = getEmailFromLoginString(login);
    	
    	PuRoMailUser authenticatedUser = null;    	
    	boolean isUpdated = false;//userIsUpdated(email); //scommentare nel caso di IMAP
    	
    	if(isUpdated)
    	{
    		PuRoMailUser inMemUser = usersMap.get(email);
    		
    		if(inMemUser.getPassword().equals(password))
    			authenticatedUser = inMemUser;
    	}
    	else
    	{
    		String attributeToGet[] = {TableInfo.TUserPassword.toString()};
    		Map<String, AttributeValue> item = getUserAttributesFromDB(email, attributeToGet);
    		
    		if(item != null)
    		{
    			AttributeValue attrPassword = item.get(TableInfo.TUserPassword.toString());
    			
    			if(attrPassword != null)
    			{
    				String dbPassword = attrPassword.getS();
    				
    				if(dbPassword.equals(password))
    				{
    					//email e password posso non leggerli da db, perchè ce li ho già
        				String otherAttributeToGet[] = {TableInfo.TUserFirstName.toString(), TableInfo.TUserLastName.toString(), TableInfo.TUserFolder.toString(), TableInfo.TUserLastUpdate.toString()};
        				
        				Map<String, AttributeValue> otherItem = getUserAttributesFromDB(email, otherAttributeToGet);	
        				
        				UserResult result = new UserResult(otherItem);	    				
        				result.setEmail(email);
        				result.setPassword(dbPassword);
        				
        				authenticatedUser = new PuRoMailUser(result, imapHostManager);
        				
        				usersMap.put(email, authenticatedUser); 
    				}
    			}
    		}
    	}
	    	
    	return authenticatedUser;
    }
	
    /**
     * Incrementa il contatore del numero di connessioni utilizzanti l'user corrente
     */
    public synchronized void addConnectedUser(PuRoMailUser user) 
    {
    	if(user != null)
    		user.addConnectedUser();
    }
    
    /**
     * Decrementa il contatore del numero di connessioni utilizzanti l'user corrente.
     * Nel caso raggiunga valore zero, l'utente viene eliminato dalla memoria.
     */
    public synchronized void removeConnectedUser(PuRoMailUser user)
    {
    	if(user != null)
    	{
    		user.removeConnectedUser();
    	
    		if(user.getConnectedUser() == 0)
    			usersMap.remove(user.getEmail());
    	}
    }
    
    /**
     * Crea utente in database
     * 
     */
    public void createUserInDb(String email, String password, String firstname, String lastname) throws UserException 
    {  
    	Date creationDate = new Date();
    	PuRoMailUser newUser = new PuRoMailUser(email, password, firstname, lastname, creationDate, imapHostManager);    	
    	ArrayList<String> userFolder = newUser.getFolder();
    	
    	String inbox = PuRoImapHostManager.USER_NAMESPACE + PuRoImapHostManager.HIERARCHY_DELIMITER +  newUser.getQualifiedMailboxName() + PuRoImapHostManager.HIERARCHY_DELIMITER + PuRoImapHostManager.INBOX_NAME;
    	userFolder.add(inbox);    	
    	
    	try
    	{
    		dynamoDB.createAndAddUserItems(newUser);
    	}
    	catch(ProvisionedThroughputExceededException e)
    	{ 
    		e.printStackTrace();
    		throw new UserException(); 
    	}
    	catch(ConditionalCheckFailedException e)
    	{ 
    		e.printStackTrace();
    		throw new UserException(); 
    	}
    	catch(InternalServerErrorException e)
    	{ 
    		e.printStackTrace();
    		throw new UserException(); 
    	}
    	catch(ResourceNotFoundException e)
    	{ 
    		e.printStackTrace();
    		throw new UserException(); 
    	}
    	catch(AmazonServiceException e)
    	{ 
    		e.printStackTrace();
    		throw new UserException(); 
    	}
    	catch(AmazonClientException e)
    	{ 
    		e.printStackTrace();
    		throw new UserException(); 
    	}
    }

    public PuRoImapHostManager getImapHostManager() {
        return imapHostManager;
    }
	
    public DynamoDB getDynamoDB() {
        return dynamoDB;
    }
    
    public PuRoConfig getConfig() {
    	return config;
    }
}
