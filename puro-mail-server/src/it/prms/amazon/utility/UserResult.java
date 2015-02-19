package it.prms.amazon.utility;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import com.amazonaws.services.dynamodb.model.AttributeValue;

public class UserResult 
{
	private String email;
	private String password;
	private String firstname;
	private String lastname;
	private Date lastUpdate;
	private ArrayList<String> folder;
	//private ArrayList<String> region;
	
	/**
	 * Costruisce un oggeto User a partire da hash map letta da Dynamo
	 * @param userItem
	 */
	public UserResult(Map<String, AttributeValue> userItem)
	{
		if(userItem != null)
		{
			AttributeValue attrEmail = userItem.get(TableInfo.TUserEmailHash.toString());
			AttributeValue attrPassword = userItem.get(TableInfo.TUserPassword.toString());
			AttributeValue attrFirstName = userItem.get(TableInfo.TUserFirstName.toString());
			AttributeValue attrLastName = userItem.get(TableInfo.TUserLastName.toString());	
			AttributeValue attrFolder = userItem.get(TableInfo.TUserFolder.toString());
			AttributeValue attrLastUpdate = userItem.get(TableInfo.TUserLastUpdate.toString());
			//AttributeValue attrRegion = userItem.get(TableInfo.TUserRegion.toString());
					
			if(attrEmail != null)		
				this.email = attrEmail.getS();		
			else
				this.email = "";
			
			if(attrPassword != null)
				this.password = attrPassword.getS();
			else
				this.password = "";
			
			if(attrFirstName != null)
				this.firstname = attrFirstName.getS();
			else
				this.firstname = "";
			
			if(attrLastName != null)
				this.lastname = attrLastName.getS();
			else
				this.lastname = "";
			
			if(attrFolder != null)
			{
				this.folder = new ArrayList<String>();			
				Iterator<String> iterator = attrFolder.getSS().iterator();
				
				while(iterator.hasNext())			
					folder.add(iterator.next());
			}
			else
				this.folder = new ArrayList<String>();
			
			if(attrLastUpdate != null)
			{
				try
				{
					long lLastUpdate = Long.valueOf(attrLastUpdate.getN());
					this.lastUpdate = new Date(lLastUpdate);
				}
				catch(NumberFormatException e)
				{
					lastUpdate = new Date();
				}
			}
			else
				lastUpdate = new Date();
			
			/*if(attrRegion != null)
			{
				this.region = new ArrayList<String>();
				Iterator<String> iterator = attrRegion.getSS().iterator();
				
				while(iterator.hasNext())			
					region.add(iterator.next());
			}
			else
				this.region = new ArrayList<String>();*/
		}
		else
		{
			this.email = "";
			this.password = "";
			this.firstname = "";
			this.lastname = "";
			this.folder = new ArrayList<String>();
			//this.region = new ArrayList<String>();
		}
	}

	public String getEmail() 
	{
		return email;
	}

	public String getPassword() 
	{
		return password;
	}
	
	public String getFirstname() 
	{
		return firstname;
	}
	
	public String getLastname() 
	{
		return lastname;
	}
	
	public ArrayList<String> getFolder()
	{
		return folder;
	}

	public Date getLastUpdate()
	{
		return lastUpdate;
	}
	
	/*public ArrayList<String> getRegion()
	{
		return region;
	}*/
	
	public void setEmail(String email) 
	{
		this.email = email;
	}

	public void setPassword(String password) 
	{
		this.password = password;
	}

	public void setFirstname(String firstname) 
	{
		this.firstname = firstname;
	}

	public void setLastname(String lastname) 
	{
		this.lastname = lastname;
	}

	public void setLastUpdate(Date lastUpdate)
	{
		this.lastUpdate = new Date(lastUpdate.getTime());
	}
	
}
