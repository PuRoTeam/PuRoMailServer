package it.prms.greenmail.store;

import it.prms.amazon.services.DynamoDB;
import it.prms.amazon.services.S3;
import it.prms.amazon.update.attribute.SingleUpdateAttribute;
import it.prms.amazon.update.attribute.UpdateAttributeList;
import it.prms.amazon.update.attribute.WrongActionException;
import it.prms.amazon.utility.AttributeType;
import it.prms.amazon.utility.MetaMail;
import it.prms.amazon.utility.TableInfo;
import it.prms.amazon.utility.WrongTypeException;
import it.prms.greenmail.foedus.util.MsgRangeFilter;
import it.prms.greenmail.imap.ImapConstants;
import it.prms.greenmail.mail.MovingMessage;
import it.prms.greenmail.util.GreenMailUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodb.model.AttributeAction;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodb.model.InternalServerErrorException;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodb.model.ResourceNotFoundException;

public class PuRoHierarchicalFolder {
	
	private DynamoDB dynamoDB;
	private S3 s3;
	
	private String name;
	private long nextUid; 								// Ogni messaggio all'interno della STESSA Folder ha un identificativo univoco (ma non fra Folder differenti)
	private PuRoHierarchicalFolder parent; 				// Genitore cartella
	private ArrayList<PuRoHierarchicalFolder> children; // ArrayList di figli	
    private boolean isSelectable = false; 
    
    private List<PuRoMessage> puroMessages = Collections.synchronizedList(new LinkedList<PuRoMessage>());
    private long uidValidity;

    private Date lastUpdate;
    
    private int countViewer;
    
    private static final Flags PERMANENT_FLAGS = new Flags(); //lista Flag IMAP

    static {
        PERMANENT_FLAGS.add(Flags.Flag.ANSWERED);
        PERMANENT_FLAGS.add(Flags.Flag.DELETED);
        PERMANENT_FLAGS.add(Flags.Flag.DRAFT);
        PERMANENT_FLAGS.add(Flags.Flag.FLAGGED);
        PERMANENT_FLAGS.add(Flags.Flag.SEEN);
    }
    
    /**
     * Costruttore PuRoHierarchicalFolder
     * @param parent
     * @param name
     * @param dynamoDB
     * @param s3
     */
    public PuRoHierarchicalFolder(PuRoHierarchicalFolder parent, String name, DynamoDB dynamoDB, S3 s3)
    {
    	this.dynamoDB = dynamoDB;
    	this.s3 = s3;
        this.children = new ArrayList<PuRoHierarchicalFolder>();
        this.parent = parent;
        this.name = name;
        this.nextUid = 1;
        this.uidValidity = System.currentTimeMillis(); 	
        
        this.lastUpdate = new Date();
        
        this.countViewer = 0;
    }
    
    /**
     * Costruttore PuRoHierarchicalFolder
     * @param parent
     * @param name
     * @param nextUid
     * @param selectable
     * @param dynamoDB
     * @param s3
     */
    public PuRoHierarchicalFolder(PuRoHierarchicalFolder parent, String name, long nextUid, boolean selectable, DynamoDB dynamoDB, S3 s3)
    {    	
    	this.parent = parent;
    	this.s3 = s3;    	
    	this.name = name;
    	this.nextUid = nextUid;
    	this.children = new ArrayList<PuRoHierarchicalFolder>();
    	this.isSelectable = selectable;
    	this.dynamoDB = dynamoDB;
    	this.uidValidity = System.currentTimeMillis(); 
    	
        this.lastUpdate = new Date();
        
        this.countViewer = 0;
    }
    
    /**
     * Incrementa numero di connessioni interessata alla cartella corrente
     * @return
     */
    public synchronized int addViewer(){
    	countViewer++;
    	return countViewer;
    }
    
    /**
     * Decrementa numero di connessioni interessate alla cartella corrente
     */
    public synchronized int removeViewer(){
    	if(countViewer > 0)
    		countViewer--;
    	return countViewer;
    }
    
    public DynamoDB getDynamoDB(){    
    	return dynamoDB;
    }
    
	
	public String getName() {
		return name;
	}

	/**
	 * Restituisce path completo cartella
	 */
    public String getFullName() {
        return parent.getFullName() + ImapConstants.HIERARCHY_DELIMITER_CHAR + name;
    }

	
	public Flags getPermanentFlags() {
		return PERMANENT_FLAGS;
	}
	
	public long getUidValidity() {
		return uidValidity;
	}
	
	public boolean isSelectable() {
		return isSelectable;
	}

    public long getUidNext() {
        return nextUid;
    }
    
    public void deleteChild(String relativeName){
    	PuRoHierarchicalFolder child = getChild(relativeName);
    	if(child != null)
    		this.children.remove(child);
    }
    
    /**
     * Richiamata dal comando POP3 RSET. Elimina i flag DELETE dalle cartelle contrassegnate come da cancellare
     * 
     * @return
     */
    public int resetDeletedFlag() 
    {    	
        int count = 0;
        for(int i = 0; i < puroMessages.size(); i++) 
        {
        	PuRoMessage msg = (PuRoMessage) puroMessages.get(i);
            Flags flags = msg.getFlags();
            if (flags.contains(Flags.Flag.DELETED)) 
            {
                count++;
                flags.remove(Flags.Flag.DELETED);
            }
        }
        return count;
    }
    
    /**
     * La lista dei messaggi deve essere già caricata in memoria, altrimenti non ha senso specificare message number
     * se retrieveCompleteMsg è true, allora recupero per ogni messaggio selezionato, tutti i suoi campi da dynamo e s3
     * POP3: DELE, RETR, TOP
     * 
     * @param range
     * @param retrieveCompleteMsg
     * @return
     * @throws FolderException
     */
	public List<PuRoMessage> getMessages(MsgRangeFilter range, boolean retrieveCompleteMsg) throws FolderException 
	{
		List<PuRoMessage> ret = new ArrayList<PuRoMessage>();
		
        for (int i = 0; i < puroMessages.size(); i++) 
        {
            if (range.includes(i+1)) //i + 1 in quanto i message number partono da 1 e non da 0 come negli array  
            {           	
				PuRoMessage puroMsg = puroMessages.get(i);
				
				Flags flags = puroMsg.getFlags();				
				if (flags.contains(Flags.Flag.DELETED))
					continue;
				
				if(retrieveCompleteMsg && !puroMsg.isSet()) //messaggio caricato parzialmente, ho solo uid e size
				{
					PuRoMessage newPuroMsg = getSingleMessageFromDB(puroMsg.getUid());
									
					puroMessages.set(i, newPuroMsg);					
				}
            	
            	ret.add(puroMessages.get(i));    		            	
            }
                          
        }
        return ret;
	}

	/**
	 * Recupera un messaggio da DynamoDB
	 * @param uid
	 * @return
	 * @throws FolderException
	 */
	public PuRoMessage getSingleMessageFromDB(long uid) throws FolderException{
		
		PuRoMessage msg = null;		
		Map<String, AttributeValue> item;
		
		try {
			item = dynamoDB.retrieveSingleMetaMailItem(this.getFullName(), uid);
			
		}catch (ProvisionedThroughputExceededException e) 
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
		
		try {		
			MetaMail metaMail = new MetaMail(item);
		
			MimeMessage mimeMsg = s3.downloadObjectFromBucket(metaMail.getBucket(), metaMail.getFilename());
			msg = new PuRoMessage(mimeMsg, metaMail);		
			
		} catch (AmazonServiceException e) {			
			deleteMessageInDB(this.getFullName(), uid);
			throw new FolderException("Server Error: PuRo unable to retrieve message");
		} catch (AmazonClientException e) {
			throw new FolderException("Server Error: PuRo unable to get a response from a service");
		} catch (MessagingException e) {
			throw new FolderException("Server Error: corrupted message");
		} catch (IOException e) {
			throw new FolderException("Server Error: PuRo unable to retrieve message");
		}
		
		return msg;
	}
    
	/**
	 * Elimina il riferimento di una mail da DynamoDB, perchè è stato rilevata una inconsistenza su S3. 
	 * Viene utilizzata dai metodi getSingleMessageFromDB() e append()
	 * 
	 * @param folder
	 * @param uid
	 * @throws FolderException
	 */
	private void deleteMessageInDB(String folder, long uid) throws FolderException
	{
		try	{
			dynamoDB.deleteMetaMailItem(this.getFullName(), uid);
		} catch(WrongTypeException e) {
			throw new FolderException("Server Error: wrong error type");			
		} catch(ProvisionedThroughputExceededException e) {
			throw new FolderException("Server Error: throughput exceeded");
		} catch(ConditionalCheckFailedException e) {
			throw new FolderException("Server error: expected value does not match what was found in the syste"); 
		}catch (InternalServerErrorException e){
			throw new FolderException("Server Error: service has a problem when trying to process the request");
		}catch (ResourceNotFoundException e){
			throw new FolderException("Server Error: referencing a resource that does not exist"); 
		}catch (AmazonServiceException e){
			throw new FolderException("Server Error: service was not able to process the request");
		}catch (AmazonClientException e){
			throw new FolderException("Server Error: PuRo unable to get a response from a service");
		}
	}
	
	/**
	 * Richiamata dai comandi POP3 LIST, UIDL. Recupera UID e SIZE dei messaggi specificati
	 * 
	 * @param range
	 * @return
	 */
	public List<PuRoMessage> getUidAndSizeOnly(MsgRangeFilter range)
    {        
		List<PuRoMessage> ret = new ArrayList<PuRoMessage>();
		
        for (int i = 0; i < puroMessages.size(); i++) 
        {
        	PuRoMessage puroMsg = puroMessages.get(i);
        	
			Flags flags = puroMsg.getFlags();				
			if (flags.contains(Flags.Flag.DELETED))
				continue;        	
        	
            if (range.includes(i+1))       
                ret.add(puroMsg);            
        }
        return ret; 	
    }
    
	/**
	 * Richiamata dai comandi POP3 STAT, LIST, UIDL
	 * 
	 * @return
	 * @throws FolderException
	 */
    public List<PuRoMessage> getUidAndSizeOnly() throws FolderException
    {    	
    	try 
    	{   
    		if(puroMessages.size() > 0) 
    		{
				List<PuRoMessage> nonDeletedMsg = new ArrayList<PuRoMessage>();
				
				for (int i = 0; i < puroMessages.size(); i++) 
				{					
					PuRoMessage puroMsg = puroMessages.get(i);
					
					Flags flags = puroMsg.getFlags();				
					if (flags.contains(Flags.Flag.DELETED))
						continue;
					
					nonDeletedMsg.add(puroMsg);
				}
				
				return nonDeletedMsg;
    		}    			   		
    		
    		String folderName = this.getFullName();
    	
    		String[] attributeToGet = new String[]{TableInfo.TMetaUidRange.toString(), TableInfo.TMetaSize.toString()};    	
    	
    		List<Map<String, AttributeValue>> items = dynamoDB.retrieveMultipleMetaMailItem(folderName, attributeToGet);
			
    		Iterator<Map<String, AttributeValue>> iterator = items.iterator();
    		while(iterator.hasNext())
    		{
    			Map<String, AttributeValue> curMap = iterator.next();
    			
    			AttributeValue attrUid = curMap.get(TableInfo.TMetaUidRange.toString());
    			AttributeValue attrSize = curMap.get(TableInfo.TMetaSize.toString());
    			
    			if(attrUid != null && attrSize != null)
    			{
    				try
    				{
    					long uid =  Long.parseLong(attrUid.getN());
    					int size = Integer.parseInt(attrSize.getN());
    					
    					PuRoMessage msg = new PuRoMessage(uid, size);
    					puroMessages.add(msg);
    				}
    				catch(NumberFormatException e)
    				{ continue; } //prossima iterazione
    			}    			
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
    	
    	return puroMessages;
    }
    
    public void setNextUid(long nextUid) {
		this.nextUid = nextUid;
	}

	public void setParent(PuRoHierarchicalFolder parent) {
		this.parent = parent;
	}

	public void setChildren(ArrayList<PuRoHierarchicalFolder> children) {
		this.children = children;
	}

	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	/**
	 * Richiamata dal comando SMTP DATA
	 * 
	 * @param mail
	 * @throws FolderException
	 */
    public void store(MovingMessage mail) throws FolderException { //(*) richiamato da deliver(MovingMessage msg)
        store(mail.getMessage());
    }
    
    public void store(MimeMessage message) throws FolderException { //(*) richiamato da deliver(MimeMessage msg) e da HierarchicalFolder.store(MovineMessage) (funzione sopra)
        Date internalDate = new Date();
        Flags flags = new Flags();
        appendMessage(message, flags, internalDate); //(*) crea un nuovo messaggio di tipo StoredMessage con flag "RECENT" e lo salva nella Folder segnalando l'evento a tutti i listener registrati
    }
    
    /**
     *  Richiamata dal comando SMTP DATA
     *  Ordine chiamate necessario per resistenza alle failure:
     *  - Aggiorno nextUID
     *  - Aggiungo item in dynamo
     *  - Aggiungo oggetto in S3
     */
	public long appendMessage(MimeMessage message, Flags flags, Date internalDate) throws FolderException {
			
		long folderNextUID = -1;
		MetaMail metaMail = new MetaMail();
		
		try	{
			//Recupero UID ed incremento
			String[] attributeToGet = {TableInfo.TFolderNextUID.toString()}; //visto che ho bisogno solo del campo nextUID, nella query richiedo solo quello
			Map<String, AttributeValue> folderItem = dynamoDB.retrieveFolderItem(this.getFullName(), attributeToGet);
			
			if(folderItem.get(TableInfo.TFolderNextUID.toString()) == null) {
				throw new FolderException("Server Error");
			}
			else {
				folderNextUID = Long.parseLong(folderItem.get(TableInfo.TFolderNextUID.toString()).getN());
				
				nextUid = folderNextUID; //giusto eseguire due volte
								
				metaMail.setUid(folderNextUID); //questa operazione devoe essere eseguita prima di incrementare il nextUID, perchè nextUID non deve puntare a nessun messaggio corrente
				metaMail.setFolder(this.getFullName());
				metaMail.setHeader(GreenMailUtil.getHeaders(message));
				metaMail.setDate(internalDate);
				metaMail.setFlags(flags);
				metaMail.setBucket(s3.getCurrentBucket());
				metaMail.setSize(message.getSize());
				metaMail.setFilename(s3.generateFileKey(metaMail));
								
				folderNextUID++;
				UpdateAttributeList updateAttributeList = new UpdateAttributeList();
				updateAttributeList.put("nextUID", new SingleUpdateAttribute(folderNextUID, AttributeAction.PUT));
				dynamoDB.updateAttributeList("Folder", this.getFullName(), AttributeType.StringType, null, null, updateAttributeList);
				
				nextUid = folderNextUID; //giusto eseguire due volte
								
				//Scrittura entry nella tabella "MetaMail"
				dynamoDB.addItem(TableInfo.TableMetaMail.toString(), dynamoDB.newMailItem(metaMail));
				
				PuRoMessage newMsg = new PuRoMessage(message, metaMail);				
				puroMessages.add(newMsg);
			}
			
		//se si verificasse un'eccezione in updateAttributeList, non è un problema se c'è un buco nei valori di nextUID, quindi possiamo non disfare l'ultima operazione
		//se si verificasse un'eccezione in addItem, allora l'item non è stato creato, quindi nessun problema
		} catch(WrongTypeException e) {
			throw new FolderException("Server Error: wrong error type");			
		} catch(ProvisionedThroughputExceededException e) {
			throw new FolderException("Server Error: throughput exceeded");
		} catch(ConditionalCheckFailedException e) {
			throw new FolderException("Server error: expected value does not match what was found in the syste"); 
		}catch (InternalServerErrorException e){
			throw new FolderException("Server Error: service has a problem when trying to process the request");
		}catch (ResourceNotFoundException e){
			throw new FolderException("Server Error: referencing a resource that does not exist"); 
		}catch (AmazonServiceException e){
			throw new FolderException("Server Error: service was not able to process the request");
		}catch (AmazonClientException e){
			throw new FolderException("Server Error: PuRo unable to get a response from a service");
		} catch (WrongActionException e) {
			throw new FolderException("Server Error: wrong action type error");
		} catch (NumberFormatException e) {
			throw new FolderException("Server Error: wrong number format error");
		} catch (MessagingException e) {
			throw new FolderException("Server Error: corrupted message");
		}
		
		try {
			s3.uploadFileToBucket(message,metaMail.getBucket(),metaMail.getFilename());
			
		//se fallisco nell'aggiungere il messaggio a S3, lo elimino da dynamo
		} catch (AmazonClientException e) {
			deleteMessageInDB(metaMail.getFolder(), metaMail.getUid());
			throw new FolderException("Server Error: PuRo unable to get a response from a service");
		} catch (IOException e) {
			deleteMessageInDB(metaMail.getFolder(), metaMail.getUid());
			throw new FolderException("Server error: PuRo unable to send message");
		} catch (MessagingException e) {
			deleteMessageInDB(metaMail.getFolder(), metaMail.getUid());
			throw new FolderException("Server error: corrupted message");
		}
		
		return folderNextUID;
	}

	/**
	 * Richiamata dal comando POP3 QUIT
	 * Deve essere richiamata solo con QUIT (a cui segue la chiusura della connessione), perchè altrimenti i message number verrebbero confusi
	 * 
	 * Ordine di eliminazione necessario per resistenza alle failure:
	 * - leggo bucketname e filename da dynamo
	 * - elimino oggetto da s3
	 * - elimino item da dynamo
	 * 
	 * @throws FolderException
	 */
	public void expunge() throws FolderException {
		
		for (int i = 0; i < puroMessages.size(); i++) {
			PuRoMessage message = (PuRoMessage) puroMessages.get(i);
			
			if (message.getFlags().contains(Flags.Flag.DELETED)) {
				
				try {
				
				Map<String, AttributeValue> item = null;
								
				String attributeToGet[] = {TableInfo.TMetaBucket.toString(), TableInfo.TMetaFileName.toString()};
				
				item = dynamoDB.retrieveSingleMetaMailItem(this.getFullName(), message.getUid(), attributeToGet);
				
				AttributeValue attrBucket = item.get(TableInfo.TMetaBucket.toString());
				AttributeValue attrFileName = item.get(TableInfo.TMetaFileName.toString());
				
				if(attrBucket != null && attrFileName != null){					
					s3.deleteObjectFromBuckets(attrBucket.getS(), attrFileName.getS());
				}
				//else: esisteva l'oggetto in dynamoDB ma non in S3. In Tal caso si procede dopo con l'eliminazione da dynamo
								
				} catch(WrongTypeException e) {
					throw new FolderException("Server Error: wrong error type");			
				} catch(ProvisionedThroughputExceededException e) {
					throw new FolderException("Server Error: throughput exceeded");
				} catch (InternalServerErrorException e){
					throw new FolderException("Server Error: service has a problem when trying to process the request");
				}catch (ResourceNotFoundException e){
					throw new FolderException("Server Error: referencing a resource that does not exist"); 
				}catch (AmazonServiceException e){
					throw new FolderException("Server Error: service was not able to process the request");
				}catch (AmazonClientException e){
					throw new FolderException("Server Error: PuRo unable to get a response from a service");
				} 
				
				try {					
            		String delFolder = this.getFullName();
            		long delUid = message.getUid();
            		
            		dynamoDB.deleteMetaMailItem(delFolder, delUid);
            		    				
    				puroMessages.remove(i);
    				i--;
            		
				//se si fosse verificata un'eccezione in deleteMetaMailItem, l'elemento non è stato cancellato
            	//se l'item non esiste, NON viene generata un'eccezione	
				} catch(WrongTypeException e) {
					throw new FolderException("Server Error: wrong error type");			
				} catch(ProvisionedThroughputExceededException e) {
					throw new FolderException("Server Error: throughput exceeded");
				} catch(ConditionalCheckFailedException e) {
					throw new FolderException("Server error: expected value does not match what was found in the syste"); 
				}catch (InternalServerErrorException e){
					throw new FolderException("Server Error: service has a problem when trying to process the request");
				}catch (ResourceNotFoundException e){
					throw new FolderException("Server Error: referencing a resource that does not exist"); 
				}catch (AmazonServiceException e){
					throw new FolderException("Server Error: service was not able to process the request");
				}catch (AmazonClientException e){
					throw new FolderException("Server Error: PuRo unable to get a response from a service");
				}
			}
		}
	}	

	public int getMsn(long uid) throws FolderException 
	{
        for (int i = 0; i < puroMessages.size(); i++) 
        {
            PuRoMessage message = (PuRoMessage) puroMessages.get(i);
            
            if (message.getUid() == uid)            
                return i + 1;
            
        }
        throw new FolderException("No such message.");
	}

	public PuRoHierarchicalFolder getChild(String childName) {
        Iterator<PuRoHierarchicalFolder> iterator = children.iterator();
        while (iterator.hasNext()) { //(*) si scorre tutti i figli
        	PuRoHierarchicalFolder child = iterator.next();
            if (child.getName().equalsIgnoreCase(childName)) {
                return child;
            }
        }
        return null;
	}


	public ArrayList<PuRoHierarchicalFolder> getChildren() {
		return children;
	}


	public void setSelectable(boolean selectable) {
		this.isSelectable = selectable;
	}


	public PuRoHierarchicalFolder getParent() {
		return parent;
	}

	public Date getLastUpdate()
	{
		return lastUpdate;
	}	
	
}
