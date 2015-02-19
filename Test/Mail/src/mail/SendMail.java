package mail;

import java.util.Date;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SendMail 
{

	public static void main(String[] args)
	{
		try
		{
		  String host = "localhost";
		  String from = "test@localhost";
		  String to = "test_pup@tiscali.it";
		
		  // Get system properties
		    Properties properties = System.getProperties();
		
		  // Setup mail server
		   properties.setProperty("mail.smtp.host", host);
		   properties.setProperty("mail.smtp.port", "12365");
		
		  // Get the default Session object.
		  Session session = Session.getDefaultInstance(properties);
		
		    // Create a default MimeMessage object.
		  MimeMessage message = new MimeMessage(session);
		
		  // Set the RFC 822 "From" header field using the
		  // value of the InternetAddress.getLocalAddress method.
		  message.setFrom(new InternetAddress(from));
		
		  // Add the given addresses to the specified recipient type.
		  message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
		
		  // Set the "Subject" header field.
		  message.setSubject("hi..!");
		
		  // Sets the given String as this part's content,
		  // with a MIME type of "text/plain".
		  message.setText("Hi test pup......");
		
		  // Send message
		  Transport.send(message);
		
		   System.out.println("Message Send by fuck yeah.....");		
		}  
		catch(AddressException e)
		{}
		catch(MessagingException e) 
		{}
		
	   /* Properties props = new Properties();
	    //props.put("mail.smtp.host", "mail.libero.it");

	    props.put("mail.smtp.host", "localhost");
	    props.put("mail.from", "elmisterpup@gmail.com");
	    System.out.println("CAMBIA PORTA OGNI VOLTA!!!!");
	    props.put("mail.smtp.port", "18998");
	    Session session = Session.getInstance(props, null);

	    System.out.println("Trying to send an email");
	    
	    try 
	    {	    	
	        MimeMessage msg = new MimeMessage(session);
	        msg.setFrom();
	        msg.setRecipients(Message.RecipientType.TO, "clapuclapu@tiscali.it");
	        msg.setSubject("JavaMail hello world example");
	        msg.setSentDate(new Date());
	        msg.setText("Hello, world!\n");
	        Transport.send(msg);
	        System.out.println("OK\n");
	    }
	    catch (MessagingException mex)
	    {
	    	System.out.println("send failed, exception: " + mex);
	    }*/
		
		
	}

}
