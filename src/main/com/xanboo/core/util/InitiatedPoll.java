/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/util/InitiatedPoll.java,v $
 * $Id: InitiatedPoll.java,v 1.8 2007/02/15 10:31:46 levent Exp $
 * 
 * Copyright 2002-2007 Xanboo, Inc.
 *
 */

package com.xanboo.core.util;

import java.net.*;
import java.io.*;


/**
 * Utility class to perform initiated-poll to the appliance gateways
 */
public class InitiatedPoll implements Runnable {
    //private static final int DEFAULT_TIMEOUT = 30000;

    private static final String SERVICE_PAGE = "/appliance.htm";
    private static final String SERVICE_TYPE = "poll";

    private static Logger logger = LoggerFactory.getLogger(InitiatedPoll.class.getName());
    
    private String gGuid;
    private String sIP;
    private int sPort;
    private String sToken;

    
    /**
     *  Constructor: Also starts the thread to perform the check
     */
    public InitiatedPoll(String gguid, String ip, int port, String token) {
        this.gGuid = gguid;
        setIP(ip);
        setPort(port);
        setToken(token);

        Thread th=new Thread(this);
        th.setDaemon(true);
        th.start();
    }
    
    
    // thread code 
    public void run() {
        try {
            String reqPage = SERVICE_PAGE + "?service=" + SERVICE_TYPE + "&token=" + this.sToken;

            if(logger.isDebugEnabled()) {                
                logger.debug("Performing Direct SIP --> IP=" + this.sIP + ":" + this.sPort + reqPage);
            }
            int rc = SimpleHttpClient.sendRequest(this.sIP, this.sPort, reqPage);
            if(logger.isDebugEnabled()) {                
                logger.debug("Direct SIP DONE --> RC=" + rc);
            }
            
            
        }catch(Exception e) {
            if(logger.isDebugEnabled()) {                
                logger.debug("Direct SIP Exception: ", e);
            }else {
                logger.warn("Direct SIP Exception: " + e.getMessage());
            }
        }
    }

 
    // setters
    public void setIP(String ip) { this.sIP=ip; }
    public void setPort(int port) { this.sPort=port; }
    public void setToken(String token) { this.sToken=token; }

/*
    // test main
    public static void main(String[] args) {

        InitiatedPoll rt = new InitiatedPoll("core", "tin.corecam.com", 8080, "");
        rt.run();
        System.err.println("OUT");;
    }
*/
}
