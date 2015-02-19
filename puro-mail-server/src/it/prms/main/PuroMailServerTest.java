package it.prms.main;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import it.prms.amazon.utility.AmazonEndPoint;
import it.prms.greenmail.util.PuRoMail;
import it.prms.greenmail.util.ServerSetup;

public class PuroMailServerTest 
{
	public static void main(String[] args) 
	{	
		String credentials_path = "";
		
		if(args.length == 0)
		{
			System.out.println("Please specify location of Credentials file");
			
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			
		    try 
		    {
		    	credentials_path = br.readLine();
		    } 
		    catch (IOException ioe) 
		    {
		         System.out.println("IO error");
		         System.exit(-1);
		    }			
		}
		else
			credentials_path = args[0];
				
		//String credentials_path = "/home/tank/Scrivania/AwsCredentials.properties";
		//String credentials_path = "/home/misterpup/Scrivania/Uni/SD/Progetto/AWS/AccountProf/AwsCredentials.properties";
				
		System.out.println("PuRo mail server\n");
		
		int portOffset_SMTP = 12340;
		int portOffset_POP3 = 12340;		
		//int portOffset_IMAP = 3748;
			
		ServerSetup SMTP_test = new ServerSetup(25  + portOffset_SMTP, null, ServerSetup.PROTOCOL_SMTP);
		ServerSetup POP3_test = new ServerSetup(110 + portOffset_POP3, null, ServerSetup.PROTOCOL_POP3);
		//ServerSetup IMAP_test = new ServerSetup(143 + portOffset_IMAP, null, ServerSetup.PROTOCOL_IMAP);
		
		ServerSetup SMTPS_test = new ServerSetup(25  + portOffset_SMTP + 1, null, ServerSetup.PROTOCOL_SMTPS);
		ServerSetup POP3S_test = new ServerSetup(110 + portOffset_POP3 + 1, null, ServerSetup.PROTOCOL_POP3S);
		//ServerSetup IMAPS_test = new ServerSetup(143 + portOffset_IMAP + 1, null, ServerSetup.PROTOCOL_IMAPS);
		
		ServerSetup[] config = {SMTP_test, POP3_test/*, IMAP_test*/, SMTPS_test, POP3S_test/*, IMAPS_test*/};
				
		AmazonEndPoint dynamoDBEndPoint = AmazonEndPoint.DDBEUIreland;
		AmazonEndPoint s3EndPoint = AmazonEndPoint.S3EUIreland;
		
		PuRoMail puroMail = null;
		try 
		{
			puroMail = new PuRoMail(config, credentials_path, dynamoDBEndPoint, s3EndPoint);			
			puroMail.start();
			
		    System.out.println("SMTP Server listening on port: " + SMTP_test.getPort());
		    System.out.println("POP3 Server listening on port: " + POP3_test.getPort());
		    //System.out.println("IMAP Server listening on port: " + IMAP_test.getPort());	
		    System.out.println("SMTPS Server listening on port: " + SMTPS_test.getPort());
		    System.out.println("POP3S Server listening on port: " + POP3S_test.getPort());
		    //System.out.println("IMAPS Server listening on port: " + IMAPS_test.getPort());	
			System.out.println("\nServer online\n");
		} 
		catch (FileNotFoundException e) 
		{ e.printStackTrace(); }
		catch (IllegalArgumentException e) 
		{ e.printStackTrace(); } 
		catch (IOException e)
		{ e.printStackTrace(); }	
	}
}
