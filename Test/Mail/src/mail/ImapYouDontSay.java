package mail;

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

public class ImapYouDontSay 
{
	public static void main(String[] args) 
	{
		String host = "127.0.0.1";
		String user = "login_imap";
		String password = "pwd_imap";
		
		
		Properties props = System.getProperties();
		props.setProperty("mail.store.protocol", "imaps");
		props.setProperty("mail.imap.port", "3891");
				
		System.out.println("Starting IMAP Client");
		
		try 
		{
			Session session = Session.getDefaultInstance(props, null);
			Store store = session.getStore("imap");
			//store.connect("imap.gmail.com", "<username>", "password");
			store.connect(host, user, password);
			
			System.out.println(store);
		
			Folder inbox = store.getFolder("Inbox");
			inbox.open(Folder.READ_ONLY);
			Message messages[] = inbox.getMessages();
			for(Message message:messages) 
			{
				System.out.println(message);
			}
		}
		catch (NoSuchProviderException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
		catch (MessagingException e)
		{
			e.printStackTrace();
			System.exit(2);
		}

	}

}
