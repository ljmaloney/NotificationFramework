/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/util/SimpleHttpClient.java,v $
 * $Id: SimpleHttpClient.java,v 1.2 2008/04/02 13:48:57 levent Exp $
 * 
 * Copyright 2002-2007 Xanboo, Inc.
 *
 */

package com.xanboo.core.util;

import java.net.*;
import java.io.*;
import java.util.*;


public class SimpleHttpClient {
    
    private static final int SOCKET_TIMEOUT = 20000;    //20 secs for connection and read timeouts


    /** private constructor */
    private SimpleHttpClient() {}

    
    public static int sendRequest(String host, int port, String requestPage) throws IOException {
        int rc=0;
        Socket sock = null;
        try {
            sock = openConnection(host, port);
            sendGetRequest(sock, requestPage);
            rc = getResponseCode(sock);
        }catch(IOException ioe) {
            throw ioe;
        }finally {
            closeConnection(sock);
        }
        
        return rc;
    }
    

    public static int sendRequest(String host, int port, String requestPage, String postData, int timeout) throws IOException {

        int rc=0;
        Socket sock = null;

        try {
            sock = openConnection(host, port, timeout);
            if(postData==null) {
                sendGetRequest(sock, requestPage);
            }else {
                sendPostRequest(sock, requestPage, postData);
            }
            rc = getResponseCode(sock);
        }catch(IOException ioe) {
            throw ioe;
        }finally {
            closeConnection(sock);
        }
        
        return rc;
    }

    
    private static Socket openConnection(String host, int port) throws IOException {
        return openConnection(host, port, SOCKET_TIMEOUT);
    }
    
    
    private static Socket openConnection(String host, int port, int timeout) throws IOException {
        ////System.out.println("Connecting: " + host + ":" + port);

        InetAddress addr = InetAddress.getByName(host);

        //in order to support connect timeouts, will require min jdk 1.4 though
        //worth jdk 1.4 sdk requirement just for this? I think it does.
        //prev jdks cannot specify connect timeouts - the call would be:
        //Socket sock = new Socket(host, port);
        
        Socket sock = new Socket();
        sock.connect(new InetSocketAddress(host, port), SOCKET_TIMEOUT);        
        
        //now set the read timeout
        sock.setSoTimeout(SOCKET_TIMEOUT);

        return sock;
    }
    
    
    private static void closeConnection(Socket sock) throws IOException {
        if(sock!=null) sock.close();
    }

    
    
    private static void sendGetRequest(Socket sock, String requestPage) throws IOException {
        if(sock!=null) {
            ////System.out.println("Sending HTTP request: " + requestPage);
            
            // send the request
            OutputStreamWriter out = new OutputStreamWriter(sock.getOutputStream());
            out.write("GET " + requestPage + " HTTP/1.0\r\n");
            
            out.write("\r\n");
            out.flush();
            //out.close();
        }
    }


    private static void sendPostRequest(Socket sock, String requestPage, String postData) throws IOException {
        if(sock!=null) {
            ////System.out.println("Sending HTTP request: " + requestPage);

            // send the request
            OutputStreamWriter out = new OutputStreamWriter(sock.getOutputStream());
            out.write("POST " + requestPage + " HTTP/1.0\r\n");
            
            out.write("Content-Length: " + postData.length() + "\r\n");
            out.write("\r\n");
            out.write(postData);
            out.write("\r\n");
            out.flush();
            //out.close();
        }
    }
    
    
    private static int getResponseCode(Socket sock) throws IOException {
        int respCode = -1;

        if(sock!=null) {
            InputStream rawin = sock.getInputStream();

            //loop for all headers, but will read only response code header !!!
            while(true) {
                String header = readHeaderLine(rawin);
                if(header==null || header.length()==0 ) break;

                int ix=header.indexOf(' ');
                if(ix>0) {
                    header = header.substring(ix+1);
                    int ix2=header.indexOf(' ');
                    if(ix2>0) {
                        try {
                            respCode = Integer.parseInt(header.substring(0, ix2));
                            break;
                        }catch(Exception e) {
                            ////System.out.println("******HEADER=" + header);
                            e.printStackTrace();
                        }
                    }
                }
            }//end while
                
        }
        
        return respCode;
    }
    
    

    private static String readHeaderLine(InputStream rawin) throws IOException {
        StringBuffer headerb = new StringBuffer("");
        while(true) {
            int ch = rawin.read();
            if(ch==-1) break;
            if(ch==0xd) continue;
            if(ch==0xa) break;
            headerb.append((char) ch);
        }
        return headerb.toString();
    }
    
    public static void main(String[] args) {
        
        if(args.length != 3) {
            System.err.println("Usage: SimpleHttpClient <host> <port> <page> [postData]");
            System.exit(1);
        }
        
        try {
            int rc;
            if(args.length>3)
                rc = SimpleHttpClient.sendRequest(args[0], Integer.parseInt(args[1]), args[2], args[3], 5000);
            else
                rc = SimpleHttpClient.sendRequest(args[0], Integer.parseInt(args[1]), args[2]);

            System.out.println("\nRC=" + rc);
        }catch(IOException ioe) {
            System.err.println("\nException: " + ioe.getMessage());
        }
    }
    
    
}
    
