package it.prms.greenmail.store;

import it.prms.amazon.services.DynamoDB;
import it.prms.amazon.services.S3;
import it.prms.amazon.utility.FolderResult;
import it.prms.amazon.utility.TableInfo;
import it.prms.amazon.utility.WrongTypeException;
import it.prms.greenmail.imap.ImapConstants;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.StringTokenizer;

import javax.mail.Flags;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.InternalServerErrorException;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodb.model.ResourceNotFoundException;

public class PuRoStore implements ImapConstants { 

    private PuRoRootFolder rootMailbox; //(*) un oggetto PuRoRootFolder con nome "#mail" e senza genitore (lo teniamo solo in memoria e non su DynamoDB)
    private DynamoDB dynamoDB;
    private S3 s3;
    
    private static final Flags PERMANENT_FLAGS = new Flags();
    
    static {
        PERMANENT_FLAGS.add(Flags.Flag.ANSWERED);
        PERMANENT_FLAGS.add(Flags.Flag.DELETED);
        PERMANENT_FLAGS.add(Flags.Flag.DRAFT);
        PERMANENT_FLAGS.add(Flags.Flag.FLAGGED);
        PERMANENT_FLAGS.add(Flags.Flag.SEEN);
    }
    
    /**
     * Costruttore
     * @param dynamoDB
     * @param s3
     */
    public PuRoStore(DynamoDB dynamoDB, S3 s3)
    {
    	rootMailbox = new PuRoRootFolder(dynamoDB, s3);
    	this.dynamoDB = dynamoDB;
    	this.s3 = s3;
    }
    
    /**
     * Aumenta il numero di viewer per la cartella folder e tutte le cartelle superiori, esclusa la root (#mail)
     * @param folder
     */
    public synchronized void addFolderAndParentViewer(PuRoHierarchicalFolder folder) {
    	PuRoHierarchicalFolder parent = folder;
    	
    	while(parent != null && parent != rootMailbox)
    	{
    		parent.addViewer();
    		parent = parent.getParent();
    	} 	
    }
     
    /**
     * Decrementa il numero di viewer per la cartella folder e tutte le cartelle superiori, esclusa la root (#mail).
     * Se il contatore arriva a zero, elimina la cartella (ed eventualmente i genitori fino al namespace incluso)
     * @param folder
     */
    public synchronized void removeFolderAndParentViewer(PuRoHierarchicalFolder folder) {
    	PuRoHierarchicalFolder parent = folder;
    	
    	while(parent != null && parent != rootMailbox)
    	{
    		int curViewer = parent.removeViewer();
    		String toRemove = parent.getName();
    		parent = parent.getParent();
    		
    		if(curViewer == 0)
    			parent.deleteChild(toRemove);
    	}
    }
    
    /**
     * Recupera dalla memoria l'oggetto folder avente nome indicato come argomento 
     * @param absoluteMailboxName
     * @return l'oggetto PuRoHierarchicalFolder associato al parametro
     */
    public PuRoHierarchicalFolder getInMemoryFolder(String absoluteMailboxName){
    	StringTokenizer tokens = new StringTokenizer(absoluteMailboxName, HIERARCHY_DELIMITER); //(*) delimitatore token: "."

        // The first token must be "#mail"
        // Si prende già il primo token "#mail"
        if (!tokens.hasMoreTokens() || !tokens.nextToken().equalsIgnoreCase(USER_NAMESPACE)) {
            return null;
        }

        PuRoHierarchicalFolder parent = rootMailbox;
        while (parent != null && tokens.hasMoreTokens()) {
            String childName = tokens.nextToken(); //(*) token corrente: "<HASH-EMAIL>"
            parent = parent.getChild(childName); //(*) restituisce oggetto PuRoHierarchicalFolder dal nome childName = "INBOX" (per SMTP e POP3)
        }
        
        return parent;
    }
    
    /**
     * Recupera la Inbox se già presente in memoria, altrimenti la carica dai riferimenti in DynamoDB
     * @param qualifiedMailboxName
     * @return la Inbox della casella mail
     * @throws FolderException
     */
    public PuRoHierarchicalFolder getPop3Inbox(String qualifiedMailboxName) throws FolderException
    {    	
    	PuRoHierarchicalFolder inbox = getInMemoryFolder(qualifiedMailboxName);
    	
    	//Controllo se è presente in memoria
    	if(inbox != null) 
    	{    		  		
    		return inbox;
    	}
    	else
    	{        	
        	int index = qualifiedMailboxName.lastIndexOf(".");
        	
        	if(index == -1)
        		return null;
        	
        	String parentPath = qualifiedMailboxName.substring(0, index);    	
        	PuRoHierarchicalFolder parent = rootMailbox;
        	
        	StringTokenizer tokens = new StringTokenizer(parentPath, HIERARCHY_DELIMITER); //(*) delimitatore token: "."
        	    	
            if (!tokens.hasMoreTokens() || !tokens.nextToken().equalsIgnoreCase(USER_NAMESPACE))        
                return null;
        	
        	while (parent != null && tokens.hasMoreTokens())
        	{
        		String childName = tokens.nextToken();
        		parent = parent.getChild(childName);
        	} 	
        	
        	if(parent == null)
        		return null;
        			
    		
    		Map<String, AttributeValue> folderItem;
    		try 
    		{			
    			folderItem = dynamoDB.retrieveFolderItem(qualifiedMailboxName);
    		
    			//Se la cartella esiste già in DynamoDB
    			if(folderItem != null)
    			{
    				FolderResult folderResult = new FolderResult(folderItem); //oggetto con dentro i campi della relativa riga in DynamoDB
    			
    				inbox = new PuRoHierarchicalFolder(parent, folderResult.getName(), folderResult.getNextUID(), folderResult.isSelectable(), dynamoDB, s3); 
    				parent.getChildren().add(inbox);
    			}
    		
    		} 
        	catch (ProvisionedThroughputExceededException e) 
        	{ throw new FolderException("Server Error: throughput exceeded"); } 
        	catch (InternalServerErrorException e) 
    		{ throw new FolderException("Server Error: service has a problem when trying to process the request"); }
        	catch (ResourceNotFoundException e)
    		{ throw new FolderException("Server Error: referencing a resource that does not exist"); }
        	catch (AmazonServiceException e) 
    		{ throw new FolderException("Server Error: service was not able to process the request"); }
        	catch (AmazonClientException e) 
    		{ throw new FolderException("Server Error: PuRo unable to get a response from a service"); }
        	catch (WrongTypeException e) 
    		{ throw new FolderException("Server Error: wrong error type"); }		
    	}
    	if(inbox != null)
    		inbox.addViewer(); //aggiunge viewer alla cartella #mail.hash.INBOX  
		
		return inbox;
    }
    
    /**
     * Aggiunge solo in memoria cartelle del tipo #mail.hash-mail
     * @param inMemoryFolderName, è il nome finale, non il path assoluto
     * @return un oggetto tipo PuroHierarchicalFolder
     */
    public PuRoHierarchicalFolder addInMemoryFolderToRoot(String inMemoryFolderName)
    {    	
    	ArrayList<PuRoHierarchicalFolder> rootChildren = rootMailbox.getChildren();
    	
    	PuRoHierarchicalFolder inMemoryFolder = null;
    	
    	for(int i = 0; i < rootChildren.size(); i++)
    	{
    		PuRoHierarchicalFolder curChild = rootChildren.get(i);
    		if(curChild.getName().equals(inMemoryFolderName))
    		{    			
    			inMemoryFolder = curChild;
    			continue;
    		}
    	}
    	
    	//Se figlio non è già presente in memoria
    	if(inMemoryFolder == null) 
    	{
    		inMemoryFolder = new PuRoHierarchicalFolder(rootMailbox, inMemoryFolderName, dynamoDB, s3);
    		rootChildren.add(inMemoryFolder);
    	}
    	
    	inMemoryFolder.addViewer();  	
    	
    	return inMemoryFolder;
    }
    
    /**
     * Recupera la versione più aggiornata della folder con nome indicato nell'argomento
     *  
     * @param qualifiedMailboxName
     * @return l'oggetto PuRoHierarchicalFolder aggiornato
     * @throws FolderException 
     */
	public PuRoHierarchicalFolder getMailbox(String qualifiedMailboxName) throws FolderException
	{			
        StringTokenizer tokens = new StringTokenizer(qualifiedMailboxName, HIERARCHY_DELIMITER); //(*) delimitatore token: "."

        //consuma il primo token, "#mail"
        if (!tokens.hasMoreTokens() || !tokens.nextToken().equalsIgnoreCase(USER_NAMESPACE))        
            return null;
        
        PuRoHierarchicalFolder lastParent = null;
        PuRoHierarchicalFolder updatedFolder = rootMailbox;        
        
        boolean isUpdated = true; //se false devo aggiornare la folder dalla memoria     
        String nameToUpdate = ""; //nome prima cartella da aggiornare
        
        while (tokens.hasMoreTokens()) 
        {       	
        	lastParent = updatedFolder;        		
        	String childName = tokens.nextToken();
        	updatedFolder = updatedFolder.getChild(childName); //(*) restituisce oggetto HierarchicalFolder dal nome childName = "INBOX" (per SMTP e POP3)         
            
        	if(updatedFolder == null || !folderIsUpdated(updatedFolder.getFullName())) //visto che è un OR, se parent è nullo non viene valutata la seconda condizione e quindi nessun "segmentation fault"
        	{
        		isUpdated = false;
        		nameToUpdate = childName;
        		break;
        	}            
        }
        
        //Se la cartella è da aggiornare (o perchè non esiste in memoria o perchè scaduta)
        if(!isUpdated)
        {
    		String pathToFolderToUpdate = lastParent.getFullName() + HIERARCHY_DELIMITER + nameToUpdate;
    		
    		updatedFolder = updateFolder(lastParent, pathToFolderToUpdate);      		
    		
    		while(updatedFolder != null && tokens.hasMoreTokens()) //se volevo la cartella #mail.hash.A.B e già la A non era aggiornata, ora che ho caricato A, devo caricare anche B
    		{
    			lastParent = updatedFolder;
    			String childName = tokens.nextToken();
    			pathToFolderToUpdate += HIERARCHY_DELIMITER + childName;
    			updatedFolder = updateFolder(lastParent, pathToFolderToUpdate);
    		}
        }
        
        return updatedFolder;    
	}
	
	/**
	 * Aggiorna la cartella col path "folderToUpdateCompletePath" in memoria, e la aggiunge fra i figli di "parent"	 * 
	 * 
	 * @param parent
	 * @param folderToUpdateCompletePath
	 * @return l'oggetto PuRoHierarchicalFolder aggiornato
	 * @throws FolderException
	 */
	public PuRoHierarchicalFolder updateFolder(PuRoHierarchicalFolder parent, String folderToUpdateCompletePath) throws FolderException
	{
		PuRoHierarchicalFolder updatedFolder = null;
		
		Map<String, AttributeValue> folderItem;
		try {
			
			folderItem = dynamoDB.retrieveFolderItem(folderToUpdateCompletePath);
			if(folderItem != null) //la cartella esisteva in DynamoDB
			{
				FolderResult folderResult = new FolderResult(folderItem); //oggetto con dentro i campi della relativa riga in DynamoDB
								
				updatedFolder = new PuRoHierarchicalFolder(parent, folderResult.getName(), folderResult.getNextUID(), folderResult.isSelectable(), dynamoDB, s3/*, dynamoDB.getTimer()*/); 
				parent.getChildren().add(updatedFolder);
			}
			
		}catch (ProvisionedThroughputExceededException e) { 
			throw new FolderException("Server Error: throughput exceeded"); 
		}catch (InternalServerErrorException e){ 
			throw new FolderException("Server Error: service has a problem when trying to process the request"); 
		}catch (ResourceNotFoundException e){
    		throw new FolderException("Server Error: referencing a resource that does not exist"); 
    	}catch (AmazonServiceException e){
    		throw new FolderException("Server Error: service was not able to process the request");
    	}catch (AmazonClientException e){
    		throw new FolderException("Server Error: PuRo unable to get a response from a service");
    	}catch (WrongTypeException e) {
    		throw new FolderException("Server Error: wrong error type");
    	}
		
		return updatedFolder;
	}
	
	
	/**
	 * Controlla se la cartella in memoria è aggiornata rispetto al quella salvata in dynamo
	 * @param folderCompletePath
	 * @return true se aggiornata, false altrimenti
	 * @throws FolderException 
	 */
	public boolean folderIsUpdated(String folderCompletePath) throws FolderException 
	{
		boolean isUpdated = false;
		PuRoHierarchicalFolder inMemFolder;
		try {
			inMemFolder = getMailbox(folderCompletePath);
			
			//Se la cartella è presente nell'albero in memoria
			if(inMemFolder != null){
				
				//Recupero il campo "lastUpdate" dalla tabella su Dynamo
				String attributeToGet[] = {TableInfo.TFolderLastUpdate.toString()};
				Map<String, AttributeValue> folderItem = getFolderAttributesFromDB(folderCompletePath, attributeToGet);
				AttributeValue lastUpdateFolder = folderItem.get(TableInfo.TFolderLastUpdate.toString());
				
				//Se il campo "lastUpdate" è valorizzato
				if(lastUpdateFolder != null){
					try{
						long lastUpdate = Long.valueOf(lastUpdateFolder.getN());
						Date dateLastUpdate = new Date(lastUpdate);
						
						//Se il valore in memoria è >= di quello recuperato dal DB allora la cartella è aggiornata
						if(inMemFolder.getLastUpdate().compareTo(dateLastUpdate) >= 0)
							isUpdated = true;
					
					}catch(NumberFormatException e){
						e.printStackTrace();
					}
				}
			}
		}catch (ProvisionedThroughputExceededException e) { 
			throw new FolderException("Server Error: throughput exceeded"); 
		}catch (InternalServerErrorException e){ 
			throw new FolderException("Server Error: service has a problem when trying to process the request"); 
		}catch (ResourceNotFoundException e){
    		throw new FolderException("Server Error: referencing a resource that does not exist"); 
    	}catch (AmazonServiceException e){
    		throw new FolderException("Server Error: service was not able to process the request");
    	}catch (AmazonClientException e){
    		throw new FolderException("Server Error: PuRo unable to get a response from a service");
    	}
        	
		return isUpdated;
	}
	
	/**
	 * Recupera dalla tabella "Folder" in DynamoDB gli attributi specificati per una data entry 
	 * @param folder, entry che si vuole recuperare
	 * @param attributesToGet, colonne della entry specificata
	 * @return HashMap con il risultato
	 */
	private Map<String, AttributeValue> getFolderAttributesFromDB(String folder, String attributesToGet[]) throws FolderException
	{
		Map<String, AttributeValue> folderItem = null;
		
		try	{
			
			if(attributesToGet.length > 0)
				folderItem = dynamoDB.retrieveFolderItem(folder, attributesToGet);
			else
				folderItem = dynamoDB.retrieveFolderItem(folder);
		}catch (ProvisionedThroughputExceededException e) { 
			throw new FolderException("Server Error: throughput exceeded"); 
		}catch (InternalServerErrorException e){ 
			throw new FolderException("Server Error: service has a problem when trying to process the request"); 
		}catch (ResourceNotFoundException e){
    		throw new FolderException("Server Error: referencing a resource that does not exist"); 
    	}catch (AmazonServiceException e){
    		throw new FolderException("Server Error: service was not able to process the request");
    	}catch (AmazonClientException e){
    		throw new FolderException("Server Error: PuRo unable to get a response from a service");
    	}catch (WrongTypeException e) {
    		throw new FolderException("Server Error: wrong error type");
    	}
	
		return folderItem;
	}
	
	
	/**
	 * Aggiorna i campi della cartella in memoria con le informazioni recuparate da DynamoDB
	 * 
	 * @param folder cartella da aggiornare
	 * @return f aggiornata, o null se c'è stato qualche errore
	 */
	public PuRoHierarchicalFolder upgradePuRoFolder(PuRoHierarchicalFolder folder) throws FolderException{
		
		try	{
			Map<String, AttributeValue> item = getFolderAttributesFromDB(folder.getFullName(), null);
			FolderResult folderResult = new FolderResult(item);
			
			if(folderResult != null){				
				//folder.setName(folderResult.getName()); 					
				folder.setNextUid(folderResult.getNextUID());				
				//folder.getParent();
				//for(int i=0; i<folder.getChildren().size(); ++i)
					//folder.getChildren().get(i).setName(folderResult.getChildren().get(i));
				
				folder.setSelectable(folderResult.isSelectable());
				folder.setLastUpdate(folderResult.getLastUpdate());
			}
			
		}catch (ProvisionedThroughputExceededException e) { 
			throw new FolderException("Server Error: throughput exceeded"); 
		}catch (InternalServerErrorException e){ 
			throw new FolderException("Server Error: service has a problem when trying to process the request"); 
		}catch (ResourceNotFoundException e){
    		throw new FolderException("Server Error: referencing a resource that does not exist"); 
    	}catch (AmazonServiceException e){
    		throw new FolderException("Server Error: service was not able to process the request");
    	}catch (AmazonClientException e){
    		throw new FolderException("Server Error: PuRo unable to get a response from a service");
    	}
		
		return folder;
	}
	
	public PuRoHierarchicalFolder getMailbox(MailFolder parent, String mailboxName) {
		return ((PuRoHierarchicalFolder) parent).getChild(mailboxName);
	}

	/**
	 * Salva nella tabella Folder su dynamoDB, un item rappresentante la folder la cui chiave è il percorso completo fullNewMailboxName
	 * 
	 * @param parent
	 * @param mailboxName
	 * @param selectable
	 * @return l'oggetto PuRoHierarchicalFolder appena creato
	 * @throws FolderException
	 */
	public PuRoHierarchicalFolder createMailbox(PuRoHierarchicalFolder parent, String mailboxName, boolean selectable) throws FolderException
	{		
        if (mailboxName.indexOf(HIERARCHY_DELIMITER_CHAR) != -1) //il nome della mailbox non può essere composto da "."
        	throw new FolderException("Invalid mailbox name.");
               	
        	String fullNewMailboxName = parent.getFullName() + HIERARCHY_DELIMITER_CHAR + mailboxName;
      
			Map<String, AttributeValue> folderItem;
			try 
			{
				folderItem = dynamoDB.retrieveFolderItem(fullNewMailboxName, new String[0]);
				
				//Se la folder già esiste
		        if(folderItem != null) 		        
		        {	
		        	throw new FolderException("Mailbox already exists.");
		        }
		        else
		        {
		        	PuRoHierarchicalFolder castParent = (PuRoHierarchicalFolder) parent;
		        	PuRoHierarchicalFolder child = new PuRoHierarchicalFolder(castParent, mailboxName, dynamoDB, s3); 
		            child.setSelectable(selectable);
		         		           
		           dynamoDB.createAndAddFolderItem(child);
		            		            
		            castParent.getChildren().add(child); // in questa posizione perchè così se si verifica un'eccezione non viene aggiunto child fra i figli di parent
		            
		            return child;
		        }
		        
			}catch (AmazonClientException e){
				throw new FolderException("Server Error: PuRo unable to get a response from a service");
			}catch (WrongTypeException e) {
	    		throw new FolderException("Server Error: wrong error type");
	    	}
	}

    public PuRoRootFolder getRootFolder(){
    	return rootMailbox;
    }
    
    public DynamoDB getDynamoDB() {
		return dynamoDB;
	}

	public S3 getS3() {
		return s3;
	}

}
