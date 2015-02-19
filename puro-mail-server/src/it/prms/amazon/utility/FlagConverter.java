package it.prms.amazon.utility;

import java.util.List;

import javax.mail.Flags;
import javax.mail.Flags.Flag;

public class FlagConverter 
{		
	/**
	 * Metodo di conversione dei flag in array di String
	 * @param flags
	 * @return array di flag da poter inserire in DynamoDB
	 */
	public static String[] flagsToString(Flags flags)
	{	
		String[] userFlags = flags.getUserFlags();
		Flag[] systemFlags = flags.getSystemFlags();
		
		int allFlagsSize = userFlags.length + systemFlags.length;
		String[] allFlags = new String[allFlagsSize];
		int curSize = 0;

		for(int i = 0; i < systemFlags.length; i++)
		{
			if(systemFlags[i].equals(Flags.Flag.DELETED)) 
			{
				allFlags[curSize] = "DELETED";
				curSize++;
			}
			if(systemFlags[i].equals(Flags.Flag.ANSWERED)) 
			{
				allFlags[curSize] = "ANSWERED";
				curSize++;
			}
			if(systemFlags[i].equals(Flags.Flag.DRAFT)) 
			{
				allFlags[curSize] = "DRAFT";
				curSize++;
			}
			if(systemFlags[i].equals(Flags.Flag.FLAGGED)) 
			{
				allFlags[curSize] = "FLAGGED";
				curSize++;
			}
			if(systemFlags[i].equals(Flags.Flag.RECENT)) 
			{
				allFlags[curSize] = "RECENT";
				curSize++;
			}
			if(systemFlags[i].equals(Flags.Flag.SEEN)) 
			{
				allFlags[curSize] = "SEEN";
				curSize++;
			}	
			if(systemFlags[i].equals(Flags.Flag.USER))
			{
				allFlags[curSize] = "USER";
				curSize++;				
			}
		}
		
        for(int i = 0; i < userFlags.length; i++)
        {
        	allFlags[curSize] = userFlags[i];
        	curSize++;
        }
		
        return allFlags;
	}
	
	/**
	 * Recupera un oggetto Flags da un array di String di un item, restituito da una query a DynamoDB
	 * @param flagsFromDB
	 * @return oggetto Flags 
	 */
	//
	public static Flags stringsToFlags(List<String> flagsFromDB)
	{
		Flags flags = new Flags();
		
		for(int i = 0; i < flagsFromDB.size(); i++)
		{
			if(flagsFromDB.get(i).equals("DELETED")) 
			{
				flags.add(Flags.Flag.DELETED); //aggiunge ai system flag
			}
			else if(flagsFromDB.get(i).equals("ANSWERED"))
			{
				flags.add(Flags.Flag.ANSWERED);
			}
			else if(flagsFromDB.get(i).equals("DRAFT"))
			{
				flags.add(Flags.Flag.DRAFT);
			}
			else if(flagsFromDB.get(i).equals("FLAGGED"))
			{
				flags.add(Flags.Flag.FLAGGED);
			}
			else if(flagsFromDB.get(i).equals("RECENT"))
			{
				flags.add(Flags.Flag.RECENT);
			}
			else if(flagsFromDB.get(i).equals("SEEN"))
			{
				flags.add(Flags.Flag.SEEN);
			}
			else if(flagsFromDB.get(i).equals("USER"))
			{
				flags.add(Flags.Flag.USER);
			}
			else //user defined flag
			{
				flags.add(flagsFromDB.get(i)); //aggiunge agli user flag
			}
		}
		
		return flags;
	}
}

