/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/util/TimedSocket.java,v $
 * $Id: TimedSocket.java,v 1.1 2003/05/06 23:35:53 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.util;

import java.net.*;
import java.io.*;


public class TimedSocket {

    public static final int TIMEOUT_CONNECTION = 10000;
    
    // Polling delay in msecs to check the connection
    private static final int POLL_DELAY = 200;

    /**
      * Attempts to connect to a service at the specified address
      * and port, for a specified maximum amount of time.
      *
      * @param	addr	Address of host
      * @param	port	Port of service
      * @param	timeout	timeout in milliseconds
      */
    public static Socket getSocket(InetAddress addr, int port, int timeout) throws InterruptedIOException, IOException {
            // Create a new socket thread, and start it running
            SocketThread st = new SocketThread( addr, port );
            st.start();

            int timer = 0;
            Socket sock = null;

            while(true) {
                // connected yet ?
                if(st.isConnected()) {
                    // success - out of here
                    sock = st.getSocket();
                    break;
                }else {
                    // any exception
                    if (st.isError()) {
                        throw (st.getException());
                    }

                    try {
                        // Sleep for a short period of time
                        Thread.sleep ( POLL_DELAY );
                    }catch (InterruptedException ie) {}

                    // Increment timer
                    timer += POLL_DELAY;

                    // Check to see if time limit exceeded
                    if(timer > timeout) {
                        // Can't connect to server
                        throw new InterruptedIOException("Could not connect for " + timeout + " milliseconds");
                    }
                }
            }

            return sock;
    }

    
    /**
      * Attempts to connect to a service at the specified address
      * and port, for a specified maximum amount of time.
      *
      *	@param	host	Hostname of machine
      *	@param	port	Port of service
      * @param	timeout	Timeout in milliseconds
      */
    public static Socket getSocket(String host, int port, int timeout) throws InterruptedIOException, IOException  {
            // Convert host into an InetAddress, and call getSocket method
            InetAddress inetAddr = InetAddress.getByName(host);

            return getSocket(inetAddr, port, timeout);
    }
    
    

    // Inner class for establishing a socket thread within another thread, to prevent blocking.
    static class SocketThread extends Thread  {
        
        volatile private Socket m_connection = null;    // Socket connection to remote host
        private String          m_host       = null;    // Hostname to connect to
        private InetAddress     m_inet        = null;   // Internet Address to connect to
        private int             m_port        = 0;      // Port number to connect to

        private IOException     m_exception   = null;   // Exception in the event a connection error occurs


        // Connect to the specified host and port number
        public SocketThread (String host, int port) {
            m_host = host;
            m_port = port;
        }

        // Connect to the specified host IP and port number
        public SocketThread(InetAddress inetAddr, int port) {
            m_inet = inetAddr;
            m_port = port;
        }

        public void run() {
            // Socket used for establishing a connection
            Socket sock = null;

            try {
                // Connect now - BLOCKING
                if(m_host != null) {
                    sock = new Socket (m_host, m_port);
                }else {
                    sock = new Socket (m_inet, m_port);
                }
            }catch(IOException ioe) {
                // Assign to our exception member variable
                m_exception = ioe;
                return;
            }

            // no error, connection established
            m_connection = sock;
        }

        
        public boolean isConnected() {
            if (m_connection == null)
                return false;
            else
                return true;
        }

        
        public boolean isError() {
            if (m_exception == null)
                return false;
            else
                return true;
        }

        
        public Socket getSocket()  {
            return m_connection;
        }

        public IOException getException()  {
            return m_exception;
        }
    }

    
}
