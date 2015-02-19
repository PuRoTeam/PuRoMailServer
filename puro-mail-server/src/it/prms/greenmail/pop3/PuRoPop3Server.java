package it.prms.greenmail.pop3;

import it.prms.greenmail.PuRoAbstractServer;
import it.prms.greenmail.PuRoManagers;
import it.prms.greenmail.pop3.commands.Pop3CommandRegistry;
import it.prms.greenmail.util.ServerSetup;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;

//(*) forse inutile
public class PuRoPop3Server extends PuRoAbstractServer {

	private String credentials_path = "";
	
    public PuRoPop3Server(ServerSetup setup, PuRoManagers puroManager) {
        super(setup, puroManager);
    }
	
    public PuRoPop3Server(ServerSetup setup, PuRoManagers puroManager, String credentials_path) {
        super(setup, puroManager);
        this.credentials_path = credentials_path;
    }

    public synchronized void quit() {

        try {
            for (Iterator iterator = handlers.iterator(); iterator.hasNext();) {
                Pop3Handler pop3Handler = (Pop3Handler) iterator.next();
                pop3Handler.quit();
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
                    
                    Pop3Handler pop3Handler = new Pop3Handler(new Pop3CommandRegistry(), puroManager.getUserManager(), clientSocket);
                    handlers.add(pop3Handler);
                    pop3Handler.start();
                }
                //Se i file credenziali non vengono trovati, il thread viene interrotto
                catch(FileNotFoundException e)
                {
                	e.printStackTrace();
                	break;
                }
                catch(IllegalArgumentException e)
                {
                	e.printStackTrace();
                	break;
                }
                catch (IOException ignored)//Eccezione dovuta a DynamoDBManagers(credentials_path) e serverSocket.accept() 
                { 
                    //ignored
                }
                
            }
        } finally{
            quit();
        }
    }
}
