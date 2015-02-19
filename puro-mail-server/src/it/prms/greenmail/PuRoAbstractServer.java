package it.prms.greenmail;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Vector;

import it.prms.greenmail.util.DummySSLServerSocketFactory;
import it.prms.greenmail.util.ServerSetup;
import it.prms.greenmail.util.Service;

public abstract class PuRoAbstractServer extends Service {
    protected final InetAddress bindTo;
    protected ServerSocket serverSocket = null;
    protected PuRoManagers puroManager;
    protected Vector handlers = null;

    protected ServerSetup setup;

    protected PuRoAbstractServer(ServerSetup setup, PuRoManagers dynamoDBManager) {
        try {
            this.setup = setup;
            bindTo = (setup.getBindAddress() == null) ? InetAddress.getByName("0.0.0.0") : InetAddress.getByName(setup.getBindAddress());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        this.puroManager = dynamoDBManager;
        handlers = new Vector();
    }

    protected synchronized ServerSocket openServerSocket() throws IOException {
        ServerSocket ret = null;
        IOException retEx = null;
        for (int i=0;i<25 && (null == ret);i++) {
            try {
                if (setup.isSecure()) {
                    ret = DummySSLServerSocketFactory.getDefault().createServerSocket(setup.getPort(), 0, bindTo);
                } else {
                	/* Valore di default per il backlog: 50
                	 * Valore massimo per il backlog: 128 [cat /proc/sys/net/core/somaxconn]
                	 * Il valore 0 come secondo argomento, lo imposta al valore di default
                	 */
                    ret = new ServerSocket(setup.getPort(), 0, bindTo); 
                }
            } catch (BindException e) {
                try {
                    retEx = e;
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {}
            }
        }
        if (null == ret && null != retEx) {
            throw retEx;
        }
        return ret;
    }

    public String getBindTo() {
        return bindTo.getHostAddress();
    }

    public int getPort() {
        return setup.getPort();
    }

    public String getProtocol() {
        return setup.getProtocol();
    }

    public ServerSetup getServerSetup() {
        return setup;
    }

    public String toString() {
        return null!=setup? setup.getProtocol()+':'+setup.getPort() : super.toString();
    }

}