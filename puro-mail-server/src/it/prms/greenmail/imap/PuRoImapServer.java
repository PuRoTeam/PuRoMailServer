package it.prms.greenmail.imap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;

import it.prms.greenmail.PuRoAbstractServer;
import it.prms.greenmail.PuRoManagers;
import it.prms.greenmail.store.PuRoStore;
import it.prms.greenmail.user.PuRoUserManager;
import it.prms.greenmail.user.UserManager;
import it.prms.greenmail.util.ServerSetup;

//(*) forse inutile
public class PuRoImapServer extends PuRoAbstractServer {

	private String credentials_path = "";
	
    public PuRoImapServer(ServerSetup setup, PuRoManagers managers) {
        super(setup, managers);
    }
	
    public PuRoImapServer(ServerSetup setup, PuRoManagers puroManager, String credentials_path) {
        super(setup, puroManager);
        this.credentials_path = credentials_path; 
    }
    
    public synchronized void quit() {
        try {
            for (Iterator iterator = handlers.iterator(); iterator.hasNext();) {
                ImapHandler imapHandler = (ImapHandler) iterator.next();
                imapHandler.resetHandler();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            if (null != serverSocket && !serverSocket.isClosed()) {
                serverSocket.close();
                serverSocket = null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        try {

            try {
                serverSocket = openServerSocket();
                setRunning(true);
                synchronized (this) {
                    this.notifyAll();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            while (keepOn()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                   
                    
                    ImapHandler imapHandler = new ImapHandler(puroManager.getUserManager(), puroManager.getImapHostManager(), clientSocket);
                    handlers.add(imapHandler);
                    imapHandler.start();
                }
                catch(FileNotFoundException e) //file credenziali non trovato, interrompi il thread
                {
                	e.printStackTrace();
                	break;
                }
                catch(IllegalArgumentException e)
                {
                	e.printStackTrace();
                	break;
                }
                catch (IOException ignored)//(*) eccezione dovuta a DynamoDBManagers(credentials_path) e serverSocket.accept() 
                {
                    //ignored
                }
            }
        } finally{
            quit();
        }
    }

}
