package it.prms.greenmail.smtp;

import it.prms.greenmail.PuRoAbstractServer;
import it.prms.greenmail.PuRoManagers;
import it.prms.greenmail.foedus.util.InMemoryWorkspace;
import it.prms.greenmail.smtp.commands.SmtpCommandRegistry;
import it.prms.greenmail.util.ServerSetup;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;

//(*) forse inutile
public class PuRoSmtpServer extends PuRoAbstractServer {

	private String credentials_path = "";
	
    public PuRoSmtpServer(ServerSetup setup, PuRoManagers managers) {
        super(setup, managers);
    }
	
    public PuRoSmtpServer(ServerSetup setup, PuRoManagers puroManager, String credentials_path) {
        super(setup, puroManager);
        this.credentials_path = credentials_path; 
    }

    public synchronized void quit() {
        try {
            for (Iterator iterator = handlers.iterator(); iterator.hasNext();) {
                SmtpHandler smtpHandler = (SmtpHandler) iterator.next();
                smtpHandler.quit();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            if (null != serverSocket && !serverSocket.isClosed()) {
                serverSocket.close();
                serverSocket = null;
            }
        } catch (Exception e) {
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
                	//(*) Gestisce richieste concorrenti
                    Socket clientSocket = serverSocket.accept(); //(*) bloccante fino all'arrivo di una connessione, crea un handler per ogni connessione in arrivo
                                      
                    SmtpHandler smtpHandler = new SmtpHandler(new SmtpCommandRegistry(), puroManager.getSmtpManager(), new InMemoryWorkspace(), clientSocket);
                    handlers.add(smtpHandler); 
                    smtpHandler.start(); //(*) ogni handler ha il suo oggetto SmtpState, quindi per ogni richiesta viene gestito un messaggio a parte senza interferenze
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
                catch (IOException e) //(*) eccezione dovuta a DynamoDBManagers(credentials_path) e serverSocket.accept() 
                { 
                	//ignored
                }
            }
        } finally{
            quit();
        }
    }

}
