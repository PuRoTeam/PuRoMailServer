package it.prms.greenmail;

import it.prms.amazon.services.DynamoDB;
import it.prms.amazon.utility.AttributeType;
import it.prms.amazon.utility.TableInfo;
import it.prms.amazon.utility.WrongTypeException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.InternalServerErrorException;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodb.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodb.model.UpdateTableRequest;

public class PuRoConfig 
{
	private DynamoDB dynamoDB;
	private HashMap<String, String> parameters; //hashmap contenente tutti i parametri con rispettivi valori
	
	public ArrayList<String> paramNames; //lista nome dei parametri di configurazione
	
	//TODO se aggiungi nuovi campi, aggiungili nell'array del costruttore
	public static final String configHashKey = "puro-config"; //valore della chiave dell'item di configurazione nella tabella User
	public static final String domainName = "domain";
	public static final String rtMetamail = "rtmetamail";
	public static final String wtMetamail = "wtmetamail";
	public static final String rtUser = "rtuser";
	public static final String wtUser = "wtuser";
	public static final String rtFolder = "rtfolder";
	public static final String wtFolder = "wtfolder";
	
	public PuRoConfig(DynamoDB dynamoDB)
	{
		this.dynamoDB = dynamoDB;
		parameters = new HashMap<String, String>();
		
		//TODO se aggiungi nuovi campi, aggiungili nell'array
		paramNames = new ArrayList<String>();
		paramNames.add(domainName);
		paramNames.add(rtFolder);
		paramNames.add(wtFolder);
		paramNames.add(rtUser);
		paramNames.add(wtUser);
		paramNames.add(rtMetamail);
		paramNames.add(wtMetamail);
		
		loadConfigurationAndSetParameters();
	}
	
	public void loadConfigurationAndSetParameters()
	{
		loadConfiguration();
		setThroughputParameters();
	}
	
	/**
	 * Carica la configurazione salvata nella entry con chiave configHashKey della tabella User su DynamoDB
	 */
	public void loadConfiguration()
	{			
		System.out.println("Loading Configuration\n");
		
		try 
		{
			Map<String, AttributeValue> item = dynamoDB.retrieveItem(TableInfo.TableUser.toString(), configHashKey, AttributeType.StringType, null, null);
			
			for(int i = 0; i < paramNames.size(); i++)
			{
				String curParamName = paramNames.get(i);
				
				AttributeValue curAttr = item.get(curParamName);
				
				if(curAttr != null)
					parameters.put(curParamName, curAttr.getS());
			}
		} 
		catch (ProvisionedThroughputExceededException e) 
		{ e.printStackTrace(); }
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
	}
	
	public String getDomainName()
	{
		return parameters.get(domainName);
	}
	
	public void setThroughputParameters() {
		try {
			Long lRtFolder = Long.valueOf(parameters.get(rtFolder));
			Long lWtFolder = Long.valueOf(parameters.get(wtFolder));
			
			Long lRtUser = Long.valueOf(parameters.get(rtUser));
			Long lWtUser = Long.valueOf(parameters.get(wtUser));
			
			Long lRtMetamail = Long.valueOf(parameters.get(rtMetamail));
			Long lWtMetamail = Long.valueOf(parameters.get(wtMetamail));
			
			if(lRtFolder != null && lWtFolder != null)
				dynamoDB.updateTableThroughput(TableInfo.TableFolder.toString(), lRtFolder, lWtFolder);
			
			if(lRtUser != null && lWtUser != null)
				dynamoDB.updateTableThroughput(TableInfo.TableUser.toString(), lRtUser, lWtUser);
			
			if(lRtMetamail != null && lWtMetamail != null)
				dynamoDB.updateTableThroughput(TableInfo.TableMetaMail.toString(), lRtMetamail, lWtMetamail);
		} catch(NumberFormatException e) {
			e.printStackTrace();
		}
	}
}
