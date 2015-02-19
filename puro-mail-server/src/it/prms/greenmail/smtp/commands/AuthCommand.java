package it.prms.greenmail.smtp.commands;

import it.prms.greenmail.smtp.PuRoSmtpManager;
import it.prms.greenmail.smtp.SmtpConnection;
import it.prms.greenmail.smtp.SmtpState;
import it.prms.greenmail.user.PuRoMailUser;
import it.prms.greenmail.user.PuRoUserManager;

import java.io.BufferedReader;
import java.io.IOException;

import org.xbill.DNS.utils.base64;

//Supporta solo AUTH PLAIN

public class AuthCommand extends SmtpCommand {

	public void execute(SmtpConnection conn, SmtpState state,
			PuRoSmtpManager smtpManager, String commandLine) throws IOException {
				
		/*DUMMY AUTH*/		
		try
		{			
			String trimmedCommandLine = commandLine.trim();
			
			String auth = trimmedCommandLine.substring(0, 4); //sicuramente valido perchè è stato contattato comando AUTH
			if(trimmedCommandLine.equals(auth))
				throw new Exception("Missing Argument: AUTH TYPE");
			
			String authPlain = "AUTH PLAIN";
			
			String toDecode = "";
			
			if(trimmedCommandLine.length() > authPlain.length())
			{
				String commandAuthPlain = trimmedCommandLine.substring(0, authPlain.length());
				
				if(!commandAuthPlain.equals(authPlain))
					throw new Exception("Only AUTH PLAIN supported");
				
				toDecode = trimmedCommandLine.substring(authPlain.length() + 1).trim();
			}
			else if(trimmedCommandLine.length() == authPlain.length()) //autenticazione su due righe
			{
				BufferedReader in = conn.getReader(); //TODO sbagliato? Guarda comando DATA
				
				String line = in.readLine();
				
				toDecode = line;
			}				
			else if(trimmedCommandLine.length() < authPlain.length())
				throw new Exception("Only AUTH PLAIN supported");
			
						
			byte[] decodedBytes = base64.fromString(toDecode);
			
			if(decodedBytes != null)
			{
				int i = 0;
				
				while(decodedBytes[i] != 0)
					i++;
					
				int j = i + 1;
				
				if(i < decodedBytes.length)
					while(decodedBytes[j] != 0)
						j++;
				
				if(i >= decodedBytes.length || j >= decodedBytes.length)
					throw new Exception("Invalid command argument, does not contain NUL");
				else
				{
					String authorizationId = new String(decodedBytes, 0, i);
			        String authenticationId = new String(decodedBytes, i + 1, j - i - 1);
			        String passwd = new String(decodedBytes, j + 1, decodedBytes.length - j - 1);
			        
			        PuRoUserManager userManager = smtpManager.getUserManager();			      
			        PuRoMailUser user = userManager.test(authenticationId, passwd);
			        
			        if(user != null)
			        {
			        	conn.println("250 OK");
			        	userManager.removeConnectedUser(user);
			        }
			        else 
			        	throw new Exception("Invalid userID or password");
				}
			}
			else
				throw new Exception("Invalid command argument");			
		}
		catch(Exception e)
		{
			conn.println("-ERR " + e); //TODO codice errore?
		}
		
		conn.println("250 OK"); //TODO TEMPORANEO
	}
}
