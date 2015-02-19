package it.prms.greenmail.store;

import it.prms.amazon.services.DynamoDB;
import it.prms.amazon.services.S3;
import it.prms.greenmail.imap.ImapConstants;

public class PuRoRootFolder extends PuRoHierarchicalFolder 
{
    public PuRoRootFolder(DynamoDB dynamoDB, S3 s3) 
    {
        super(null, ImapConstants.USER_NAMESPACE, dynamoDB, s3);
    }

    public String getFullName() 
    {
        return getName();
    }
}
