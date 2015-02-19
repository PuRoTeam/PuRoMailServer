package it.prms.amazon.utility;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import com.amazonaws.services.dynamodb.model.AttributeValue;

public class FolderResult
{
	private String completePath; //path completo: #mail.hash-email.INBOX
	private String singleName; //INBOX
	private long nextUID;
	private String parent;
	private ArrayList<String> children;
	private boolean selectable;
	private Date lastUpdate;
	
	/**
	 * Costruisce un oggetto Folder a partire dalla hasm map prelevata su DynamoDB
	 * @param folderItem
	 */
	public FolderResult(Map<String, AttributeValue> folderItem)
	{
		if(folderItem != null)
		{
			AttributeValue attrCompletePath = folderItem.get(TableInfo.TFolderNameHash.toString());
			AttributeValue attrNextUID = folderItem.get(TableInfo.TFolderNextUID.toString());
			AttributeValue attrParent = folderItem.get(TableInfo.TFolderParent.toString());
			AttributeValue attrChildren = folderItem.get(TableInfo.TFolderChildren.toString());	
			AttributeValue attrSelectable = folderItem.get(TableInfo.TFolderSelectable.toString());
			AttributeValue attrLastUpdate = folderItem.get(TableInfo.TFolderLastUpdate.toString());
			
			if(attrCompletePath != null)
			{
				this.completePath = attrCompletePath.getS();
				this.singleName = completePath.substring(this.completePath.lastIndexOf(".") + 1);
			}
			else
			{
				this.completePath = "";
				this.singleName = "";
			}
			
			if(attrNextUID != null)
			{
				try
				{
					this.nextUID = Long.parseLong(attrNextUID.getN());
				}
				catch(NumberFormatException e)
				{
					e.printStackTrace();
					this.nextUID = -1;
				}
			}
			else
				this.nextUID = -1;
					
			if(attrParent != null)
				this.parent = attrParent.getS();
			else
				this.parent = "";
			
			if(attrChildren != null)
			{
				this.children = new ArrayList<String>();
				
				Iterator<String> iterator = attrChildren.getSS().iterator();
				
				while(iterator.hasNext())			
					children.add(iterator.next());			
			}
			else
				this.children = new ArrayList<String>();
			
			if(attrSelectable != null)
				this.selectable = Boolean.parseBoolean(attrSelectable.getS());
			else
				this.selectable = false;
			
			if(attrLastUpdate != null)
				this.lastUpdate = new Date(Long.valueOf(attrLastUpdate.getS()));
			else
				this.lastUpdate = null;
		}
		else
		{
			this.completePath = "";
			this.singleName = "";
			this.nextUID = -1;
			this.parent = "";
			this.children = new ArrayList<String>();
			this.selectable = false;
			this.setLastUpdate(null);
		}
	}

	public String getCompletePath() 
	{
		return completePath;
	}

	public String getName()
	{
		return singleName;
	}
	
	public long getNextUID() 
	{
		return nextUID;
	}
	
	public String getParent() 
	{
		return parent;
	}
	
	public ArrayList<String> getChildren()
	{
		return children;
	}
	
	public boolean isSelectable() 
	{
		return selectable;
	}
	
	public void setCompletePath(String completePath) 
	{
		this.completePath = completePath;
	}

	public void setName(String name)
	{
		this.singleName = name;
	}
	
	public void setNextUID(long nextUID) 
	{
		this.nextUID = nextUID;
	}

	public void setParent(String parent) 
	{
		this.parent = parent;
	}

	public void setSelectable(boolean selectable) 
	{
		this.selectable = selectable;
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
}
