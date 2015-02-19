/*
 * Copyright (c) 2006 Wael Chatila / Icegreen Technologies. All Rights Reserved.
 * This software is released under the LGPL which is available at http://www.gnu.org/copyleft/lesser.html
 * This file has been used and modified. Original file can be found on http://foedus.sourceforge.net
 */
package it.prms.greenmail.smtp;

import it.prms.greenmail.foedus.util.Workspace;
import it.prms.greenmail.mail.MovingMessage;



public class SmtpState {
    MovingMessage currentMessage;
    Workspace _workspace;

    public SmtpState(Workspace workspace) {
        _workspace = workspace;
        clearMessage();
    }

    public MovingMessage getMessage() {

        return currentMessage;
    }

    /**
     * To destroy a half-contructed message.
     */
    public void clearMessage() {
        if (currentMessage != null)
            currentMessage.releaseContent();

        currentMessage = new MovingMessage(_workspace);
    }
}