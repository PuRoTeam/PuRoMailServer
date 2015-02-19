package it.prms.amazon.services;

import it.prms.amazon.update.attribute.MultiUpdateAttribute;
import it.prms.amazon.update.attribute.SingleUpdateAttribute;
import it.prms.amazon.update.attribute.UpdateAttribute;
import it.prms.amazon.update.attribute.UpdateAttributeList;
import it.prms.amazon.utility.AmazonEndPoint;
import it.prms.amazon.utility.AttributeType;
import it.prms.amazon.utility.FlagConverter;
import it.prms.amazon.utility.MetaMail;
import it.prms.amazon.utility.TableInfo;
import it.prms.amazon.utility.WrongTypeException;
import it.prms.greenmail.store.PuRoHierarchicalFolder;
import it.prms.greenmail.user.PuRoMailUser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodb.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodb.model.CreateTableRequest;
import com.amazonaws.services.dynamodb.model.DeleteItemRequest;
import com.amazonaws.services.dynamodb.model.DeleteItemResult;
import com.amazonaws.services.dynamodb.model.DeleteTableRequest;
import com.amazonaws.services.dynamodb.model.DeleteTableResult;
import com.amazonaws.services.dynamodb.model.DescribeTableRequest;
import com.amazonaws.services.dynamodb.model.DescribeTableResult;
import com.amazonaws.services.dynamodb.model.GetItemRequest;
import com.amazonaws.services.dynamodb.model.GetItemResult;
import com.amazonaws.services.dynamodb.model.InternalServerErrorException;
import com.amazonaws.services.dynamodb.model.Key;
import com.amazonaws.services.dynamodb.model.KeySchema;
import com.amazonaws.services.dynamodb.model.KeySchemaElement;
import com.amazonaws.services.dynamodb.model.LimitExceededException;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodb.model.PutItemRequest;
import com.amazonaws.services.dynamodb.model.PutItemResult;
import com.amazonaws.services.dynamodb.model.QueryRequest;
import com.amazonaws.services.dynamodb.model.QueryResult;
import com.amazonaws.services.dynamodb.model.ResourceInUseException;
import com.amazonaws.services.dynamodb.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodb.model.ReturnValue;
import com.amazonaws.services.dynamodb.model.TableDescription;
import com.amazonaws.services.dynamodb.model.TableStatus;
import com.amazonaws.services.dynamodb.model.UpdateItemRequest;
import com.amazonaws.services.dynamodb.model.UpdateItemResult;
import com.amazonaws.services.dynamodb.model.UpdateTableRequest;
import com.amazonaws.services.dynamodb.model.UpdateTableResult;

public class DynamoDB {
	   
	private AmazonDynamoDBClient dynamoDB;
	private AmazonEndPoint currentEndPoint;
		
	
    /*****************************Costruttori*****************************/
	
	public DynamoDB() throws FileNotFoundException, IllegalArgumentException, IOException {
		this("", null);
    }
	
	public DynamoDB(AmazonEndPoint endpoint) throws FileNotFoundException, IllegalArgumentException, IOException  {
		this("", endpoint);
    }
	
	public DynamoDB(String credentials_path) throws FileNotFoundException, IllegalArgumentException, IOException {
		this(credentials_path, null);
    }
	
	/**
	 * Costruttore generico per la classe, che si occupa di creare il client verso il servizio DynamoDB
	 * @param credentials_path, il percorso assoluto al file delle credenziali
	 * @param endpoint
	 * @throws FileNotFoundException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public DynamoDB(String credentials_path, AmazonEndPoint endpoint)
			throws FileNotFoundException, IllegalArgumentException, IOException {
	
		AWSCredentials credentials = null;
		
		if(credentials_path.equals(""))
			credentials = new PropertiesCredentials(DynamoDB.class.getResourceAsStream("AwsCredentials.properties"));
		else
			credentials = new PropertiesCredentials(new File(credentials_path));
		
        dynamoDB = new AmazonDynamoDBClient(credentials);
        
        this.setCurrentEndPoint(endpoint);	
	}
	
	/*****************************Costruttori*****************************/	
	
	public void setCurrentEndPoint(AmazonEndPoint currentEndPoint) throws IllegalArgumentException{
		
		if(currentEndPoint != null) {
			dynamoDB.setEndpoint(currentEndPoint.toString());
			this.currentEndPoint = currentEndPoint;			
		}
		else
			this.currentEndPoint = null;
	
	}
	
	public AmazonEndPoint getCurrentEndPoint() {
		return currentEndPoint;
	}
	
	/*****************************Creazione Tabella*****************************/
	
	/**
	 * Funzione che crea un tabella in DynamoDB, con hashKey con previsione di throughput 10L, 5L
	 * @param tableName
	 * @param hashKeyString
	 * @param hashKeyStringType
	 * @return
	 * @throws ResourceInUseException, in caso di tabella già esistente
	 * @throws LimitExceededException
	 * @throws InternalServerErrorException
	 * @throws AmazonServiceException
	 * @throws AmazonClientException
	 */
	public boolean createTableDynamoDB(String tableName, String hashKeyString, String hashKeyStringType)
			throws ResourceInUseException, LimitExceededException, InternalServerErrorException, AmazonServiceException, AmazonClientException{
		
		return createTableDynamoDB(tableName, hashKeyString, hashKeyStringType, null, null, 10L, 5L);
	}
	
	/**
	 * Funzione che crea un tabella in DynamoDB, con hashKey con previsione di throughput passate come argomento
	 * 
	 * @param tableName
	 * @param hashKeyString
	 * @param hashKeyStringType
	 * @param readCapacityUnits
	 * @param writeCapacityUnits
	 * @return
	 * @throws ResourceInUseException, in caso di tabella già esistente
	 * @throws LimitExceededException
	 * @throws InternalServerErrorException
	 * @throws AmazonServiceException
	 * @throws AmazonClientException
	 */
	public boolean createTableDynamoDB(String tableName, String hashKeyString, String hashKeyStringType, Long readCapacityUnits, Long writeCapacityUnits)
			throws ResourceInUseException, LimitExceededException, InternalServerErrorException, AmazonServiceException, AmazonClientException{
		
		return createTableDynamoDB(tableName, hashKeyString, hashKeyStringType, null, null, readCapacityUnits, writeCapacityUnits);
	}
	
	/**
	 * Funzione che crea un tabella in DynamoDB, con hashKey e rangeKey con previsione di throughput 10L, 5L
	 * 
	 * @param tableName
	 * @param hashKeyString
	 * @param hashKeyStringType
	 * @param rangeKeyString
	 * @param rangeKeyStringType
	 * @return
	 * @throws ResourceInUseException
	 * @throws LimitExceededException
	 * @throws InternalServerErrorException
	 * @throws AmazonServiceException
	 * @throws AmazonClientException
	 */
	public boolean createTableDynamoDB(String tableName, String hashKeyString, String hashKeyStringType, String rangeKeyString, String rangeKeyStringType)
			throws ResourceInUseException, LimitExceededException, InternalServerErrorException, AmazonServiceException, AmazonClientException{
		
		return createTableDynamoDB(tableName, hashKeyString, hashKeyStringType, rangeKeyString, rangeKeyStringType, 10L, 5L);
	}
	
	/**
	 * Funzione che crea un tabella in DynamoDB, con hashKey e rangeKey con previsione di throughput passate come argomento
	 * 
	 * @param tableName
	 * @param hashKeyString
	 * @param hashKeyStringType
	 * @param rangeKeyString
	 * @param rangeKeyStringType
	 * @param readCapacityUnits
	 * @param writeCapacityUnits
	 * @return
	 * @throws ResourceInUseException
	 * @throws LimitExceededException
	 * @throws InternalServerErrorException
	 * @throws AmazonServiceException
	 * @throws AmazonClientException
	 */
	public boolean createTableDynamoDB(String tableName, String hashKeyString, String hashKeyStringType, String rangeKeyString, String rangeKeyStringType,
			 Long readCapacityUnits, Long writeCapacityUnits) 
					 throws ResourceInUseException, LimitExceededException, InternalServerErrorException, AmazonServiceException, AmazonClientException{
		
		ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput()
		  .withReadCapacityUnits(readCapacityUnits)
		  .withWriteCapacityUnits(writeCapacityUnits);
		
		CreateTableRequest createTableRequest = null; 
		
		// Create a table with a primary key named 'primaryKey', which holds a string
		if(hashKeyString != null){
			if(rangeKeyString != null){
				KeySchemaElement hashKey = new KeySchemaElement().withAttributeName(hashKeyString).withAttributeType(hashKeyStringType);
				KeySchemaElement rangeKey = new KeySchemaElement().withAttributeName(rangeKeyString).withAttributeType(rangeKeyStringType);
				KeySchema ksHash = new KeySchema().withHashKeyElement(hashKey);
				KeySchema ksRange = new KeySchema().withRangeKeyElement(rangeKey);
				
				createTableRequest = new CreateTableRequest()
				  .withTableName(tableName)
				  .withKeySchema(ksHash).withKeySchema(ksRange)
				  .withProvisionedThroughput(provisionedThroughput);
			}else{
				KeySchemaElement hashKey = new KeySchemaElement().withAttributeName(hashKeyString).withAttributeType(hashKeyStringType);
				KeySchema ksHash = new KeySchema().withHashKeyElement(hashKey);
				
				createTableRequest = new CreateTableRequest()
				  .withTableName(tableName)
				  .withKeySchema(ksHash)
				  .withProvisionedThroughput(provisionedThroughput);
			}
		}else{
			return false;
		}
        
        dynamoDB.createTable(createTableRequest).getTableDescription();

        // Wait for it to become active
        return waitForTableToBecomeAvailable(tableName);
	}
	
	/*****************************Creazione Tabella*****************************/
	
	public boolean deleteTableDynamoDB(String tableName) 
			throws ResourceInUseException, LimitExceededException, InternalServerErrorException, 
			ResourceNotFoundException, AmazonServiceException, AmazonClientException {
		
		DeleteTableRequest deleteTableRequest = new DeleteTableRequest() .withTableName(tableName);
		DeleteTableResult result = dynamoDB.deleteTable(deleteTableRequest);
		if(result != null)
			return true;
		else
			return false;
	}
	
	//Se esiste già un item con la chiave primaria specificata, dynamoDB.putItem lo rimpiazza con il nuovo item
	public PutItemResult addItem(String tableName, Map<String, AttributeValue> item) 
			throws ProvisionedThroughputExceededException, ConditionalCheckFailedException,
			InternalServerErrorException, ResourceNotFoundException, AmazonServiceException, AmazonClientException{
		
		PutItemRequest putItemRequest = new PutItemRequest(tableName, item);
		PutItemResult putItemResult = dynamoDB.putItem(putItemRequest);

		return putItemResult;
	}
	
	/*****************************MetaMail*****************************/
	
	public Map<String, AttributeValue> newMailItem(MetaMail metaMail) {
		
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        
        item.put(TableInfo.TMetaFolderHash.toString(), new AttributeValue().withS(metaMail.getFolder()));
        item.put(TableInfo.TMetaUidRange.toString(), new AttributeValue().	withN(Long.toString(metaMail.getUid())));
        item.put(TableInfo.TMetaHeader.toString(), new AttributeValue().	withS(metaMail.getHeader()));
        item.put(TableInfo.TMetaBucket.toString(), new AttributeValue().	withS(metaMail.getBucket()));
        item.put(TableInfo.TMetaFileName.toString(), new AttributeValue().	withS(metaMail.getFilename()));
        item.put(TableInfo.TMetaTimestamp.toString(), new AttributeValue().	withN(Long.toString(metaMail.getDate().getTime())));
        item.put(TableInfo.TMetaSize.toString(), new AttributeValue().		withN(Integer.toString(metaMail.getSize())));
        
        String[] allFlags = FlagConverter.flagsToString(metaMail.getFlags());

        if(allFlags.length > 0)
        	item.put(TableInfo.TMetaFlags.toString(), new AttributeValue().withSS(allFlags));
                
        return item;
    }
		
	public PutItemResult createAndAddMailItems(MetaMail metaMail) throws AmazonServiceException, AmazonClientException{
		
		String tableName = "Metamail";	
		
		Map<String, AttributeValue> item = newMailItem(metaMail);		
		PutItemResult result = addItem(tableName, item);
		
        return result;
    }
	
	public Map<String, AttributeValue> retrieveSingleMetaMailItem(String folder, long uid) 
			throws WrongTypeException, ProvisionedThroughputExceededException, 
			InternalServerErrorException, ResourceNotFoundException, AmazonServiceException, AmazonClientException{
				
		String[] attributeToGet = {TableInfo.TMetaFolderHash.toString(),
									TableInfo.TMetaUidRange.toString(),
									TableInfo.TMetaHeader.toString(),
									TableInfo.TMetaBucket.toString(),
									TableInfo.TMetaFileName.toString(),
									TableInfo.TMetaTimestamp.toString(),
									TableInfo.TMetaSize.toString(),
									TableInfo.TMetaFlags.toString()};
				
		return retrieveSingleMetaMailItem(folder, uid, attributeToGet);
	}
	
	public Map<String, AttributeValue> retrieveSingleMetaMailItem(String folder, long uid, String[] attributeToGet)
				throws WrongTypeException, ProvisionedThroughputExceededException, 
				InternalServerErrorException, ResourceNotFoundException, AmazonServiceException, AmazonClientException{
				
		String tableName = TableInfo.TableMetaMail.toString();
		String searchHashKey = folder;
		AttributeType attributeTypeHash = AttributeType.StringType;
		String searchRangeKey = Long.toString(uid);
		AttributeType attributeTypeRange = AttributeType.NumberType;
		
		return retrieveItem(tableName, searchHashKey, attributeTypeHash, searchRangeKey, attributeTypeRange, attributeToGet);					
	}
	
	public  List<Map<String,AttributeValue>> retrieveMultipleMetaMailItem(String folder)
			throws WrongTypeException, ProvisionedThroughputExceededException, 
			InternalServerErrorException, ResourceNotFoundException, AmazonServiceException, AmazonClientException{
		
		String[] attributeToGet = {TableInfo.TMetaFolderHash.toString(),
									TableInfo.TMetaUidRange.toString(),
									TableInfo.TMetaHeader.toString(),
									TableInfo.TMetaBucket.toString(),
									TableInfo.TMetaFileName.toString(),
									TableInfo.TMetaTimestamp.toString(),
									TableInfo.TMetaSize.toString(),
									TableInfo.TMetaFlags.toString()};
		
		return retrieveMultipleMetaMailItem(folder, attributeToGet);
	}
	
	public  List<Map<String,AttributeValue>> retrieveMultipleMetaMailItem(String folder, String[] attributeToGet)
			throws WrongTypeException, ProvisionedThroughputExceededException, 
			InternalServerErrorException, ResourceNotFoundException, AmazonServiceException, AmazonClientException{
							
		String tableName = TableInfo.TableMetaMail.toString();
		String searchHashKey = folder;
		AttributeType attributeTypeHash = AttributeType.StringType;
		
		return query(tableName, searchHashKey, attributeTypeHash, attributeToGet);
	}
	
	public Map<String, AttributeValue> deleteMetaMailItem(String folder, long uid)
	    		throws WrongTypeException, ProvisionedThroughputExceededException, ConditionalCheckFailedException,	InternalServerErrorException, ResourceNotFoundException,
	    		AmazonServiceException, AmazonClientException {
		
		String tableName = TableInfo.TableMetaMail.toString();
		String deleteHashKey = folder;
		AttributeType deleteHashKeyType = AttributeType.StringType;
		String deleteRangeKey = Long.toString(uid);
		AttributeType deleteRangeKeyType = AttributeType.NumberType;
		
		DeleteItemResult item = deleteItem(tableName, deleteHashKey, deleteHashKeyType, deleteRangeKey, deleteRangeKeyType);
		
		return item.getAttributes();
	}
	
	/*****************************MetaMail*****************************/
	
	/*****************************User*****************************/
	
	public Map<String, AttributeValue> newUserItem(PuRoMailUser user) {
        
		Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        		
        item.put(TableInfo.TUserEmailHash.toString(), new AttributeValue().withS(user.getEmail()));
        item.put(TableInfo.TUserPassword.toString(), new AttributeValue().withS(user.getPassword()));
        item.put(TableInfo.TUserFirstName.toString(), new AttributeValue().withS(user.getFirstname()));
        item.put(TableInfo.TUserLastName.toString(), new AttributeValue().withS(user.getLastname()));
        
        long lastUpdate = user.getLastUpdate().getTime();
        item.put(TableInfo.TUserLastUpdate.toString(), new AttributeValue().withN(Long.toString(lastUpdate)));
        
        String[] user_folder = new String[user.getFolder().size()];
        user.getFolder().toArray(user_folder);        
        if(user_folder.length > 0)
        	item.put(TableInfo.TUserFolder.toString(), new AttributeValue().withSS(user_folder));
        
        return item;
    }
	
	public PutItemResult createAndAddUserItems(PuRoMailUser user)
			throws ProvisionedThroughputExceededException, ConditionalCheckFailedException,
			InternalServerErrorException, ResourceNotFoundException, AmazonServiceException, AmazonClientException{
		
		String tableName = TableInfo.TableUser.toString();	
		
		Map<String, AttributeValue> item = newUserItem(user);		
		PutItemResult result = addItem(tableName, item);
		
        return result;
    }
	
	public Map<String, AttributeValue> retrieveUserItem(String email) 
			throws WrongTypeException, ProvisionedThroughputExceededException, 
			InternalServerErrorException, ResourceNotFoundException, AmazonServiceException, AmazonClientException{
				
		String[] attributeToGet = {TableInfo.TUserEmailHash.toString(),
									TableInfo.TUserPassword.toString(),
									TableInfo.TUserFirstName.toString(),
									TableInfo.TUserLastName.toString(),
									TableInfo.TUserFolder.toString(),
									TableInfo.TUserLastUpdate.toString()/*,
									TableInfo.TUserRegion.toString()*/};
				
		return retrieveUserItem(email, attributeToGet);
	}
	
	public Map<String, AttributeValue> retrieveUserItem(String email, String[] attributeToGet)
				throws WrongTypeException, ProvisionedThroughputExceededException, 
				InternalServerErrorException, ResourceNotFoundException, AmazonServiceException, AmazonClientException{
		
		String tableName = TableInfo.TableUser.toString();
		String searchHashKey = email;
		AttributeType attributeTypeHash = AttributeType.StringType;
		
		return retrieveItem(tableName, searchHashKey, attributeTypeHash, null, null, attributeToGet);					
	}
	
	/*****************************User*****************************/
	
	/*****************************Folder*****************************/

	public Map<String, AttributeValue> newFolderItem(PuRoHierarchicalFolder folder) {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        
        String fullName = folder.getFullName();        
        long nextUID = folder.getUidNext();        
        String parentFullName = folder.getParent().getFullName();    
        
        String[] children = new String[folder.getChildren().size()];       
        folder.getChildren().toArray(children);
        String selectable = new Boolean(folder.isSelectable()).toString();
        Date lastUpdate = folder.getLastUpdate();
                
        item.put(TableInfo.TFolderNameHash.toString(), new AttributeValue().withS(fullName));
        item.put(TableInfo.TFolderNextUID.toString(), new AttributeValue().withN(Long.toString(nextUID)));
        item.put(TableInfo.TFolderParent.toString(), new AttributeValue().withS(parentFullName));
        if(children.length > 0)
        	item.put(TableInfo.TFolderChildren.toString(), new AttributeValue().withSS(children));
        item.put(TableInfo.TFolderSelectable.toString(), new AttributeValue().withS(selectable));
        item.put(TableInfo.TFolderLastUpdate.toString(), new AttributeValue().withS(lastUpdate.toString()));
        
        return item;
    }
	
	public PutItemResult createAndAddFolderItem(PuRoHierarchicalFolder folder)
			throws ProvisionedThroughputExceededException, ConditionalCheckFailedException,
			InternalServerErrorException, ResourceNotFoundException, AmazonServiceException, AmazonClientException{
		
        String tableName = TableInfo.TableFolder.toString();
		
		Map<String, AttributeValue> item = newFolderItem(folder);
		PutItemResult result = addItem(tableName, item);
		
        return result;
    }
		
	public Map<String, AttributeValue> retrieveFolderItem(String completeMailboxName)
			throws WrongTypeException, ProvisionedThroughputExceededException, 
			InternalServerErrorException, ResourceNotFoundException, AmazonServiceException, AmazonClientException{
	
		String[] attributeToGet = {TableInfo.TFolderNameHash.toString(),
									TableInfo.TFolderNextUID.toString(),
									TableInfo.TFolderParent.toString(), 
									TableInfo.TFolderChildren.toString(),
									TableInfo.TFolderSelectable.toString(),
									TableInfo.TFolderLastUpdate.toString()};
			
		return retrieveFolderItem(completeMailboxName, attributeToGet);
	}
	
	public Map<String, AttributeValue> retrieveFolderItem(String completeMailboxName, String[] attributeToGet)
			throws WrongTypeException, ProvisionedThroughputExceededException, 
			InternalServerErrorException, ResourceNotFoundException, AmazonServiceException, AmazonClientException{
		
		String tableName = TableInfo.TableFolder.toString();
		String searchHashKey = completeMailboxName;
		AttributeType attributeTypeHash = AttributeType.StringType;
				
		return retrieveItem(tableName, searchHashKey, attributeTypeHash, null, null, attributeToGet);
	}
	
	
	/*****************************Folder*****************************/
	
	public Map<String, AttributeValue> retrieveItem(String tableName, String searchHashKey, AttributeType attributeTypeHash, String searchRangeKey, 
			AttributeType attributeTypeRange) 
					throws WrongTypeException, ProvisionedThroughputExceededException, 
					InternalServerErrorException, ResourceNotFoundException, AmazonServiceException, AmazonClientException{
		
		String[] attributeToGet = new String[0]; //array vuoto di attributi da prelevare -> preleva tutti gli attributi
		return retrieveItem(tableName, searchHashKey, attributeTypeHash, searchRangeKey, attributeTypeRange, attributeToGet);
	}	
	
	public Map<String, AttributeValue> retrieveItem(String tableName, String searchHashKey, AttributeType attributeTypeHash, String searchRangeKey, 
			AttributeType attributeTypeRange, String[] attributeToGet) 
					throws WrongTypeException, ProvisionedThroughputExceededException, 
					InternalServerErrorException, ResourceNotFoundException, AmazonServiceException, AmazonClientException{
                	
    	GetItemRequest getItemRequest = null;
    	GetItemResult result = null;
    	
    	Key key = new Key();
    	
    	if(searchHashKey != null && tableName != null){
        	if(attributeTypeHash.equals(AttributeType.StringType)){ //La Key della tabella è una Stringa
        		
           		key.setHashKeyElement(new AttributeValue().withS(searchHashKey));
        		
    			if(searchRangeKey != null){ //Tabella con Range
	    			if(attributeTypeRange.equals(AttributeType.StringType)){ //Il Range della tabella è una Stringa
	    				key.setRangeKeyElement(new AttributeValue().withS(searchRangeKey));
	    				getItemRequest = new GetItemRequest().withTableName(tableName)
	        					.withKey(key)
	        					.withAttributesToGet(attributeToGet);
	    			}else if(attributeTypeRange.equals(AttributeType.NumberType)){ //Il Range della tabella è un Numero
	    				key.setRangeKeyElement(new AttributeValue().withN(searchRangeKey));
	    				getItemRequest = new GetItemRequest().withTableName(tableName)
	        					.withKey(key)
	        					.withAttributesToGet(attributeToGet);
	    			}else{
	    				throw new WrongTypeException("Range Key must be of type String or Number");
	    			}
    			}else{ //Tabella senza Range
    				getItemRequest = new GetItemRequest().withTableName(tableName)
        					.withKey(key)
        					.withAttributesToGet(attributeToGet);
    			}
    		}else if(attributeTypeHash.equals(AttributeType.NumberType)){ //La Key della tabella è un Numero
    			key.setHashKeyElement(new AttributeValue().withN(searchHashKey));
    			
    			if(searchRangeKey != null){ //Tabella con Range
	    			if(attributeTypeRange.equals(AttributeType.StringType)){ //Il Range della tabella è una Stringa
	    				key.setRangeKeyElement(new AttributeValue().withS(searchRangeKey));
	    				getItemRequest = new GetItemRequest().withTableName(tableName)
	        					.withKey(key);
	    			}else if(attributeTypeRange.equals(AttributeType.NumberType)){ //Il Range della tabella è un Numero
	    				key.setRangeKeyElement(new AttributeValue().withN(searchRangeKey));
	    				getItemRequest = new GetItemRequest().withTableName(tableName)
	        					.withKey(key)
	        					.withAttributesToGet(attributeToGet);
	    			}else{
	    				throw new WrongTypeException("Range Key must be of type String or Number");
	    			}
    			}else{ //Tabella senza Range
    				getItemRequest = new GetItemRequest().withTableName(tableName)
        					.withKey(key)
        					.withAttributesToGet(attributeToGet);
    			}
    		}else{
    			throw new WrongTypeException("Hash Key must be of type String or Number");
    		}        	
    	}
    		
		if(!getItemRequest.equals(null)){
			result = dynamoDB.getItem(getItemRequest); //anche se il risultato fosse vuoto, l'oggetto GetItemResult è comunque non nullo

			if(result != null)
				return result.getItem(); //nel caso in cui si fosse richiesto un oggetto inesistente nella tabella, result.getItem restituisce null
			else
				return null;
		}    
		
		return null;  
	}
	
	/**
	 * Recupera tutti gli item con stessa hash in una tabella con chiave (hash, range). Per ogni item vengono recuperati tutti gli attributi 
	 * @param tableName
	 * @param searchHashKey
	 * @param attributeTypeHash
	 * @return
	 * @throws WrongTypeException
	 * @throws ProvisionedThroughputExceededException
	 * @throws InternalServerErrorException
	 * @throws ResourceNotFoundException
	 * @throws AmazonServiceException
	 * @throws AmazonClientException
	 */
	public List<Map<String, AttributeValue>> query(String tableName, String searchHashKey, AttributeType attributeTypeHash)
			throws WrongTypeException, ProvisionedThroughputExceededException, 
			InternalServerErrorException, ResourceNotFoundException, AmazonServiceException, AmazonClientException {
		
		String[] attributeToGet = new String[0]; //array vuoto di attributi da prelevare -> preleva tutti gli attributi
		return query(tableName, searchHashKey, attributeTypeHash, attributeToGet);
	}
	
	public List<Map<String, AttributeValue>> query(String tableName, String searchHashKey, AttributeType attributeTypeHash, String[] attributesToGet)
			throws WrongTypeException, ProvisionedThroughputExceededException, 
			InternalServerErrorException, ResourceNotFoundException, AmazonServiceException, AmazonClientException 
	{
		QueryRequest queryRequest = null;
		QueryResult result = null;
		
		if(searchHashKey != null && tableName != null)
		{			
			queryRequest = new QueryRequest().withTableName(tableName).withAttributesToGet(attributesToGet);
			
			if(attributeTypeHash.equals(AttributeType.StringType))
			{
				queryRequest.setHashKeyValue(new AttributeValue().withS(searchHashKey));
			}
			else if(attributeTypeHash.equals(AttributeType.NumberType))
			{
				queryRequest.setHashKeyValue(new AttributeValue().withN(searchHashKey));
			}
			else
				throw new WrongTypeException("Hash Key must be of type String or Number");
			
			result = dynamoDB.query(queryRequest);
		}
		
		if(result != null)
		{
			return result.getItems();
		}
		
		return null;
	}
	
	
	/**
	 * Esegue l'update di uno o più attributi di un item della tabella. Per ogni attributo è necessario specificare
	 * se sia un campo a valore singolo o un set e l'azione da eseguire (ADD, PUT, DELETE)
	 * @param tableName
	 * @param updateHashKey
	 * @param attributeTypeHash
	 * @param updateRangeKey
	 * @param attributeTypeRange
	 * @param updateAttributeList
	 * @return
	 * @throws ProvisionedThroughputExceededException
	 * @throws ConditionalCheckFailedException
	 * @throws InternalServerErrorException
	 * @throws ResourceNotFoundException
	 * @throws AmazonServiceException
	 * @throws AmazonClientException
	 * @throws WrongTypeException
	 */
	public Map<String, AttributeValue> updateAttributeList(String tableName, String updateHashKey, AttributeType attributeTypeHash, String updateRangeKey, AttributeType attributeTypeRange,
			UpdateAttributeList updateAttributeList) 
					throws ProvisionedThroughputExceededException, ConditionalCheckFailedException,
					InternalServerErrorException, ResourceNotFoundException, AmazonServiceException, AmazonClientException, WrongTypeException
	{
        Map<String, AttributeValueUpdate> updateItems = new HashMap<String, AttributeValueUpdate>();

        if(updateHashKey != null)
        {
        	Key key = new Key();
        	
        	if(attributeTypeHash.equals(AttributeType.StringType))
        		key.setHashKeyElement(new AttributeValue().withS(updateHashKey));
        	else if(attributeTypeHash.equals(AttributeType.NumberType))
        		key.setHashKeyElement(new AttributeValue().withN(updateHashKey));
        	else
        		throw new WrongTypeException("Hash Key must be of type String or Number.");        	       	
        	
        	if(updateRangeKey != null)
        	{        		      
        		if(attributeTypeRange.equals(AttributeType.StringType))
        			key.setRangeKeyElement(new AttributeValue().withS(updateRangeKey));
        		else if(attributeTypeRange.equals(AttributeType.NumberType))
        			key.setRangeKeyElement(new AttributeValue().withN(updateRangeKey));
        		else
        			throw new WrongTypeException("Range Key must be of type String or Number.");        		
        	}        	           
            
        	//Aggiungo tutti gli attributi alla map updateItems (per ogni attributo viene stabilito se bisogna eseguire ADD, PUT o DELETE)
            ArrayList<String> listAttributeNames = updateAttributeList.getAttributeNames(); //gli attributi che voglio modificare
            for(int i = 0; i < listAttributeNames.size(); i++)
            {
            	String currAttributeName = listAttributeNames.get(i); //nome attributo da modificare
            	
            	UpdateAttribute temp = updateAttributeList.getAttributeObject(listAttributeNames.get(i));
            	if(temp.getClass().equals(SingleUpdateAttribute.class)) //l'attributo è una stringa o un numero
            	{
            		SingleUpdateAttribute single = (SingleUpdateAttribute)temp; 
            		
            		if(single.getType().equals(AttributeType.StringType))            		
            			updateItems.put(currAttributeName, new AttributeValueUpdate().withAction(single.getAttributeAction()).withValue(new AttributeValue().withS(single.getValue())));
            		else //può essere solo di due tipi, quindi se è un numero
            			updateItems.put(currAttributeName, new AttributeValueUpdate().withAction(single.getAttributeAction()).withValue(new AttributeValue().withN(single.getValue())));            			
            	}
            	else if(temp.getClass().equals(MultiUpdateAttribute.class)) //l'attributo è un insieme di stringhe o un insieme di numeri
            	{
            		MultiUpdateAttribute multi = (MultiUpdateAttribute)temp;
            		
            		if(multi.getType().equals(AttributeType.StringSetType) && multi.getValues().size() > 0)
            			updateItems.put(currAttributeName, new AttributeValueUpdate().withAction(multi.getAttributeAction()).withValue(new AttributeValue().withSS(multi.getArrayOfValues())));            	
            		else if(multi.getType().equals(AttributeType.NumberSetType) && multi.getValues().size() > 0)//può essere solo di due tipi, quindi se è un set di numeri
            			updateItems.put(currAttributeName, new AttributeValueUpdate().withAction(multi.getAttributeAction()).withValue(new AttributeValue().withNS(multi.getArrayOfValues())));
            	}
            	else
            	{
            		System.out.println("ERRORE - Debug"); //errore, oggetto di altra classe, non si può verificare
            	}        	
            }    
            
            ReturnValue returnValues = ReturnValue.ALL_NEW; //restituisce tutti gli attributi (con l'eventuale valore aggiornato) dell'item di cui si è appena fatto l'update.
            
            UpdateItemRequest updateItemRequest = new UpdateItemRequest().withTableName(tableName).withKey(key).withAttributeUpdates(updateItems).withReturnValues(returnValues);
    		
            UpdateItemResult result = dynamoDB.updateItem(updateItemRequest);
   
            if(result != null)
            	return result.getAttributes();
            else 
            	return null;
        }
        return null;
	}
	
	
	public void updateAddNewAttribute(String tableName, String updateKey, String newAttribute, String newValue) 
			throws ProvisionedThroughputExceededException, ConditionalCheckFailedException, InternalServerErrorException,
			ResourceNotFoundException, AmazonClientException, AmazonServiceException{
		
        try {
            Map<String, AttributeValueUpdate> updateItems = new HashMap<String, AttributeValueUpdate>();

            Key key = new Key().withHashKeyElement(new AttributeValue().withN(updateKey));
            updateItems.put(newAttribute, new AttributeValueUpdate().withValue(new AttributeValue().withS(newValue)));
            
            ReturnValue returnValues = ReturnValue.ALL_NEW;
            
            UpdateItemRequest updateItemRequest = new UpdateItemRequest().withTableName(tableName).withKey(key).withAttributeUpdates(updateItems).withReturnValues(returnValues);
            
            UpdateItemResult result = dynamoDB.updateItem(updateItemRequest);
            
            // Check the response.
            System.out.println("Printing item after adding new attribute...");
            printItem(result.getAttributes());            
                            
        } catch(AmazonServiceException ase){
        	System.err.println("Failed to add new attribute in " + tableName);
        }
    }
    
	public UpdateTableResult updateTable(UpdateTableRequest r) throws AmazonServiceException, AmazonClientException {
		return dynamoDB.updateTable(r);
	}
	
	public DescribeTableResult describeTable (DescribeTableRequest describeTableRequest) throws AmazonServiceException, AmazonClientException {
		return dynamoDB.describeTable(describeTableRequest);
	}
	
	public UpdateTableResult updateTableThroughput(String tableName, long newReadUnits, long newWriteUnits) {
		UpdateTableResult updateTableResult = null;
		
		DescribeTableRequest describeTableRequest = new DescribeTableRequest();
		describeTableRequest.setTableName(tableName);
		
		DescribeTableResult describeTableResult = describeTable(describeTableRequest);
		
		Long currentReadU = describeTableResult.getTable().getProvisionedThroughput().getReadCapacityUnits();
		Long currentWriteU = describeTableResult.getTable().getProvisionedThroughput().getWriteCapacityUnits();
		
		UpdateTableRequest updateTableRequest = new UpdateTableRequest().withTableName(tableName);
		
		ProvisionedThroughput throughput = new ProvisionedThroughput();
		
		if(newReadUnits != currentReadU.longValue() || newWriteUnits != currentWriteU) //se almeno uno dei due valori è diverso da quello corrente, procedo all'update
		{
			throughput.setReadCapacityUnits(newReadUnits);
			throughput.setWriteCapacityUnits(newWriteUnits);
		}
		
		if(throughput.getReadCapacityUnits() != null || throughput.getWriteCapacityUnits() != null) //se almeno uno dei due valori è diverso da quello corrente, procedo all'update
		{
			updateTableRequest.setProvisionedThroughput(throughput);
			
			updateTableResult = updateTable(updateTableRequest);
		}
		
		return updateTableResult;
	}
	
    public DeleteItemResult deleteItem(String tableName, String deleteKey, AttributeType deleteKeyType) 
    		throws WrongTypeException, ProvisionedThroughputExceededException, ConditionalCheckFailedException,	InternalServerErrorException, ResourceNotFoundException,
    		AmazonServiceException, AmazonClientException{
                 
        	Key key = new Key();
            if(deleteKeyType.equals(AttributeType.StringType)){
            	key = new Key().withHashKeyElement(new AttributeValue().withS(deleteKey));
            }else if(deleteKeyType.equals(AttributeType.NumberType)){
            	key = new Key().withHashKeyElement(new AttributeValue().withN(deleteKey));
            }else{
				throw new WrongTypeException("Delete key must be of type String or Number");
            }
                 
            //definisce il valore di ritorno della richiesta di cancellazione
            ReturnValue returnValues = ReturnValue.ALL_OLD;

            DeleteItemRequest deleteItemRequest = new DeleteItemRequest()
                .withTableName(tableName)
                .withKey(key)
                .withReturnValues(returnValues);

            DeleteItemResult result = dynamoDB.deleteItem(deleteItemRequest);
            
            return result;
    }
    
    public DeleteItemResult deleteItem(String tableName, String deleteHashKey, AttributeType deleteHashKeyType, String deleteRangeKey, AttributeType deleteRangeKeyType) 
    		throws WrongTypeException, ProvisionedThroughputExceededException, ConditionalCheckFailedException,	InternalServerErrorException, ResourceNotFoundException,
    		AmazonServiceException, AmazonClientException{
    	
    	DeleteItemResult result = null;
    	
        if(deleteHashKey != null)
        {        	
	        	Key key = new Key();
	        	
	            if(deleteHashKeyType.equals(AttributeType.StringType)){
	            	key = new Key().withHashKeyElement(new AttributeValue().withS(deleteHashKey));
	            }else if(deleteHashKeyType.equals(AttributeType.NumberType)){
	            	key = new Key().withHashKeyElement(new AttributeValue().withN(deleteHashKey));
	            }else{
					throw new WrongTypeException("Delete key must be of type String or Number");
	            }
	        	
	        	if(deleteRangeKey != null){
	        		
	        		if(deleteRangeKeyType.equals(AttributeType.StringType))
	        			key.setRangeKeyElement(new AttributeValue().withS(deleteRangeKey));
	        		else if(deleteRangeKeyType.equals(AttributeType.NumberType))
	        			key.setRangeKeyElement(new AttributeValue().withN(deleteRangeKey));
	        		else
	        			throw new WrongTypeException("Range Key must be of type String or Number.");        		
	        	}
	        	
	        	  //definisce il valore di ritorno della richiesta di cancellazione
	            ReturnValue returnValues = ReturnValue.ALL_OLD;
	
	            DeleteItemRequest deleteItemRequest = new DeleteItemRequest()
	                .withTableName(tableName)
	                .withKey(key)
	                .withReturnValues(returnValues);
	
	            result = dynamoDB.deleteItem(deleteItemRequest);           
        
        }
        return result;
    }

    public void printItem(Map<String, AttributeValue> attributeList) throws IllegalStateException{
    	
    	if(attributeList != null){
	        for (Map.Entry<String, AttributeValue> item : attributeList.entrySet()) {
	            String attributeName = item.getKey();
	            AttributeValue value = item.getValue();            
	         
	            System.out.println(attributeName + " "
	                    + (value.getS() == null ? "" : "S=[" + value.getS() + "]")
	                    + (value.getN() == null ? "" : "N=[" + value.getN() + "]")
	                    + (value.getB() == null ? "" : "B=[" + value.getB() + "]")
	                    + (value.getSS() == null ? "" : "SS=[" + value.getSS() + "]")
	                    + (value.getNS() == null ? "" : "NS=[" + value.getNS() + "]")
	                    + (value.getBS() == null ? "" : "BS=[" + value.getBS() + "] \n"));     
	        }
    	}
    }
    
    public boolean waitForTableToBecomeAvailable(String tableName) 
    		throws InternalServerErrorException, ResourceNotFoundException, AmazonServiceException, AmazonClientException{
    	
        System.out.println("Waiting for " + tableName + " to become ACTIVE...");

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (10 * 60 * 1000);
        while (System.currentTimeMillis() < endTime) {
            try {
            	Thread.sleep(1000 * 20);
            } catch (Exception e) {}
            
            try {
                DescribeTableRequest request = new DescribeTableRequest().withTableName(tableName);
                TableDescription tableDescription = dynamoDB.describeTable(request).getTable();
                String tableStatus = tableDescription.getTableStatus();
                System.out.println(" - current state: " + tableStatus);
                if (tableStatus.equals(TableStatus.ACTIVE.toString())) return true;
            } catch (AmazonServiceException ase) {
                if (ase.getErrorCode().equalsIgnoreCase("ResourceNotFoundException") == false) throw ase;
            }
        }

        throw new RuntimeException("Table " + tableName + " never went active");
    }
}
