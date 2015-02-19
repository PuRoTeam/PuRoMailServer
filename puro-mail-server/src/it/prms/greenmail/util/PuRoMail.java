package it.prms.greenmail.util;

import it.prms.amazon.utility.AmazonEndPoint;
import it.prms.greenmail.PuRoAbstractServer;
import it.prms.greenmail.PuRoManagers;
import it.prms.greenmail.imap.PuRoImapServer;
import it.prms.greenmail.pop3.PuRoPop3Server;
import it.prms.greenmail.smtp.PuRoSmtpServer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

public class PuRoMail {

	PuRoManagers managers;
    HashMap<String, PuRoAbstractServer> services; // (chiave, valore) = (nome protocollo, server protocollo)  (per ogni protocollo posso lanciare in locale UN solo server)
        
    /**
     * Call this constructor if you want to run more than one of the email servers
     * 
     * @param config
     * @param credentials_path
     * @param dynamoDBEndPoint
     * @param s3EndPoint
     * @throws FileNotFoundException
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public PuRoMail(ServerSetup[] config, String credentials_path, AmazonEndPoint dynamoDBEndPoint, AmazonEndPoint s3EndPoint) throws FileNotFoundException, IllegalArgumentException, IOException {//(*) meglio lanciare questa funzione che quella sopra perchè così i tre server condividono la stessa inbox senza dover mettere le variabili a static in Managers
        
        managers = new PuRoManagers(credentials_path, dynamoDBEndPoint, s3EndPoint);
        	
        services = new HashMap<String, PuRoAbstractServer>();
        for (int i = 0; i < config.length; i++) {
            ServerSetup setup = config[i];
            if (services.containsKey(setup.getProtocol())) {
                throw new IllegalArgumentException("Server '" + setup.getProtocol() + "' was found at least twice in the array");
            }
            final String protocol = setup.getProtocol();
            if (protocol.startsWith(ServerSetup.PROTOCOL_SMTP)) {
                services.put(protocol, new PuRoSmtpServer(setup, managers));
            } else if (protocol.startsWith(ServerSetup.PROTOCOL_POP3)) {
                services.put(protocol, new PuRoPop3Server(setup, managers));
            } else if (protocol.startsWith(ServerSetup.PROTOCOL_IMAP)) {
                services.put(protocol, new PuRoImapServer(setup, managers));
            }
        }
    }

    /**
     * Avvia tutti i server lanciando il corrispondente metodo run
     */
    public synchronized void start() { 
    	
        for (Iterator<PuRoAbstractServer> it = services.values().iterator(); it.hasNext();) {
            Service service = (Service) it.next();
            service.startService(null);            
        }
        //quick hack for now, will change eventually
        boolean allup = false;
        for (int i=0;i<200 && !allup;i++) { // errore se non riesce ad avviare tutti i server dentro alla hashmap entro un certo tempo limite
            allup = true;
            for (Iterator<PuRoAbstractServer> it = services.values().iterator(); it.hasNext();) {
                Service service = (Service) it.next();
                allup = allup && service.isRunning();
            }
            if (!allup) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                }
            }
        }
        if (!allup) {
            throw new RuntimeException("Couldnt start at least one of the mail services.");
        }
    }

    public synchronized void stop() {
        for (Iterator<PuRoAbstractServer> it = services.values().iterator(); it.hasNext();) {
            Service service = (Service) it.next();
            service.stopService(null);
        }
    }

    public PuRoSmtpServer getSmtp() {
        return (PuRoSmtpServer) services.get(ServerSetup.PROTOCOL_SMTP);
    }

    public PuRoImapServer getImap() {
        return (PuRoImapServer) services.get(ServerSetup.PROTOCOL_IMAP);

    }

    public PuRoPop3Server getPop3() {
        return (PuRoPop3Server) services.get(ServerSetup.PROTOCOL_POP3);
    }

    public PuRoSmtpServer getSmtps() {
        return (PuRoSmtpServer) services.get(ServerSetup.PROTOCOL_SMTPS);
    }

    public PuRoImapServer getImaps() {
        return (PuRoImapServer) services.get(ServerSetup.PROTOCOL_IMAPS);

    }

    public PuRoPop3Server getPop3s() {
        return (PuRoPop3Server) services.get(ServerSetup.PROTOCOL_POP3S);
    }

    public PuRoManagers getManagers() {
        return managers;
    }   

	
}
