/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package it.prms.greenmail.pop3;



import it.prms.greenmail.pop3.commands.Pop3Command;
import it.prms.greenmail.pop3.commands.Pop3CommandRegistry;
import it.prms.greenmail.user.PuRoUserManager;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.StringTokenizer;


public class Pop3Handler extends Thread {
    Pop3CommandRegistry _registry;
    Pop3Connection _conn;
    PuRoUserManager _manager;
    Pop3State _state;
    boolean _quitting;
    String _currentLine;
    private Socket _socket;

    public Pop3Handler(Pop3CommandRegistry registry,
                       PuRoUserManager manager, Socket socket) {
        _registry = registry;
        _manager = manager;
        _socket = socket;
    }

    public void run() {
        try {
        	System.gc();
        	
            _conn = new Pop3Connection(this, _socket);
            _state = new Pop3State(_manager);

            _quitting = false;

            sendGreetings();

            while (!_quitting) {
                handleCommand();
            }

            _conn.close();
        } catch (SocketTimeoutException ste) {
            _conn.println("421 Service shutting down and closing transmission channel");

        } catch (Exception e) {
        } finally { //eseguito anche in caso di eccezione
        	
        	if(_state.getUser() != null)
        	_state.getUserManager().removeConnectedUser(_state.getUser());
        	
        	if(_state.getFolder() != null) //ero autenticato
        		_state.getStore().removeFolderAndParentViewer(_state.getFolder());
        		
            try {
                _socket.close();
            } catch (IOException ioe) {
            }
            
            quit();
            
            System.gc();
        }

    }

    void sendGreetings() {
        _conn.println("+OK POP3 GreenMail Server ready");
    }

    void handleCommand()
            throws IOException {
        _currentLine = _conn.readLine();

        if (_currentLine == null) {
            quit();

            return;
        }

        String commandName = new StringTokenizer(_currentLine, " ").nextToken()
                .toUpperCase();

        Pop3Command command = _registry.getCommand(commandName);

        if (command == null) {
            _conn.println("-ERR Command not recognized");

            return;
        }

        if (!command.isValidForState(_state)) {
            _conn.println("-ERR Command not valid for this state");

            return;
        }

        command.execute(_conn, _state, _currentLine);
    }

    public void quit() {
         _quitting = true;
        try {
            if (_socket != null && !_socket.isClosed()) {
                _socket.close();
            }
        } catch(IOException ignored) {
            //empty
        } 
    }
}