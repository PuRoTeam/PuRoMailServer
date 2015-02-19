package it.prms.amazon.utility;

import java.util.Date;
import java.util.Map;

import javax.mail.Flags;

import com.amazonaws.services.dynamodb.model.AttributeValue;

public class MetaMail {
	
	private String folder; 	//HashKey
	private long uid;		//RangeKey
	private String bucket;
	private String objectname;
	private String header;
	private int size;
	private Date date;
	private Flags flags;
	
	public MetaMail(){}
	
	/**
	 * Costruttore in una entry della tabella Metamail
	 * @param metaMailItem, risultato di una ricerca su DynamoDB
	 */
	public MetaMail(Map<String, AttributeValue> metaMailItem)
	{
		if(metaMailItem != null)
		{
			AttributeValue attrFolder = metaMailItem.get(TableInfo.TMetaFolderHash.toString());
			AttributeValue attrUID = metaMailItem.get(TableInfo.TMetaUidRange.toString());
			AttributeValue attrBucket = metaMailItem.get(TableInfo.TMetaBucket.toString());
			AttributeValue attrObjectName = metaMailItem.get(TableInfo.TMetaFileName.toString());	
			AttributeValue attrHeader = metaMailItem.get(TableInfo.TMetaHeader.toString());
			AttributeValue attrSize = metaMailItem.get(TableInfo.TMetaSize.toString());
			AttributeValue attrDate = metaMailItem.get(TableInfo.TMetaTimestamp.toString());
			AttributeValue attrFlags = metaMailItem.get(TableInfo.TMetaFlags.toString());
			
			if(attrFolder != null)
				this.folder = attrFolder.getS();
			else
				this.folder = "";
			
			if(attrUID != null)
				this.uid = Long.parseLong(attrUID.getN());
			else
				this.uid = -1;
			
			if(attrBucket != null)
				this.bucket = attrBucket.getS();
			else
				this.bucket = "";
			
			if(attrObjectName != null)
				this.objectname = attrObjectName.getS();
			else
				this.objectname = "";
			
			if(attrHeader != null)
				this.header = attrHeader.getS();
			else
				this.header = "";
			
			if(attrSize != null)
				this.size = Integer.parseInt(attrSize.getN());
			else
				this.size = -1;
			
			if(attrDate != null) 
				this.date = new Date(new Long(attrDate.getN())); //La data salvata in Dynamo è un long
			else
				this.date = new Date();
			
			if(attrFlags != null)						
				flags = FlagConverter.stringsToFlags(attrFlags.getSS());			
			else
				flags = new Flags();
		}
		else
		{
			this.folder = "";
			this.uid = -1;
			this.bucket = "";
			this.objectname = "";
			this.header = "";
			this.size = -1;
			this.date = new Date();
			flags = new Flags();
		}
	}
	
	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public String getFilename() {
		return objectname;
	}

	public void setFilename(String objectname) {
		this.objectname = objectname;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public Flags getFlags() {
		return flags;
	}

	public void setFlags(Flags flags) {
		this.flags = flags;
	}
}