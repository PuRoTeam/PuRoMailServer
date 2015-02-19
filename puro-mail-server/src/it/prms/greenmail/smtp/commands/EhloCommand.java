package it.prms.greenmail.smtp.commands;

import it.prms.greenmail.smtp.PuRoSmtpManager;
import it.prms.greenmail.smtp.SmtpConnection;
import it.prms.greenmail.smtp.SmtpState;

import java.io.IOException;

//250-DSN. Delivery Status Notification: messaggio torna indietro se impossibilitati ad inviarlo. NON SUPPORTATO
//250-8BITMIME. Messaggi a 8 bit. SUPPORTATO (CREDO)
//250-HELP
//250-PIPELINING. Client può inviare un comando senza attendere risposta da quello precedente. SUPPORTATO (CREDO)
//250-AUTH=LOGIN
//250-AUTH LOGIN CRAM-MD5 DIGEST-MD5 PLAIN. SUPPORTATO SOLO PLAIN
//250-DELIVERBY 300. Consegna entro questo tempo. NON SUPPORTATO.
//250 SIZE 15728640. Massima dimensione messaggio. Se non specifico il valore, significa che supporto qualunque dimensione. SUPPORTATO

public class EhloCommand extends SmtpCommand {

	public void execute(SmtpConnection conn, SmtpState state,
			PuRoSmtpManager manager, String commandLine) throws IOException {

		extractEhloName(conn, commandLine);
        state.clearMessage();
        conn.println("250-" + conn.getServerGreetingsName() + "\r\n" + "250-AUTH=LOGIN" + "\r\n" + "250-AUTH PLAIN" + "\r\n" + "250 SIZE");
	}	

    private void extractEhloName(SmtpConnection conn, String commandLine) {
		String ehloName;
		
		if (commandLine.length() > 5)
			ehloName = commandLine.substring(5);
		else
			ehloName = null;
		
		conn.setHeloName(ehloName);
	}
	
}
