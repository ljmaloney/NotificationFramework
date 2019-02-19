/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/util/ProxyInitiatedPoll.java,v $
 * $Id: ProxyInitiatedPoll.java,v 1.5 2006/11/07 16:42:41 levent Exp $
 *
 * Copyright 2002-2007 Xanboo, Inc.
 *
 */

package com.xanboo.core.util;

import java.net.*;
import java.io.*;

import com.xanboo.core.util.Logger;

/**
 * Utility class to perform to trigger a proxy initiated server poll to the appliance gateways
 */
public class ProxyInitiatedPoll implements Runnable {
    //private static final int DEFAULT_TIMEOUT = 30000;
    public static final int DEFAULT_PROXY_LISTENING_PORT = 4999;   

    private static final String SERVICE_PAGE = "/appliance.htm";
    private static final String SERVICE_TYPE = "poll";
    
    private static Logger logger = LoggerFactory.getLogger(ProxyInitiatedPoll.class.getName());
    
    private String gGuid;
    private String proxyHost;
    private int proxyPort;

    private int proxySourcePort;    // proxy source port to initiate the poll from
    private String gwyIP;           // gateway ip to poll
    private int gwyPort;            // gateway port to poll
    private String gwyToken;        // current gateway token
    private int gwyPortUDP;         // gateway UDP port to poll


    /**
     *  Constructor: Also starts the thread to perform the check
     */
    public ProxyInitiatedPoll(String gguid, String pHost, int pSourcePort, String gHost, int gPort, int gPortUDP, String gToken) {
        
        this.gGuid = gguid;
        this.proxyHost = pHost;
        this.proxyPort = DEFAULT_PROXY_LISTENING_PORT;
        
        this.proxySourcePort = pSourcePort;
        this.gwyIP = gHost;
        this.gwyPort = gPort;
        this.gwyToken = gToken;
        this.gwyPortUDP = gPortUDP;
        
        if(proxyHost!=null && !proxyHost.equals("null")) {
            Thread th=new Thread(this);
            th.setDaemon(true);
            th.start();
        }
    }

    
    // thread code 
    public void run() {
        try {

            String reqPage = null;
            
            if(proxySourcePort==-999) {     //-999 indicates, Direct SIP signalling thru NAT servers for web/app tier seperated setups
                reqPage = "/sip.htm?";
                reqPage = reqPage +   "gwIP=" + this.gwyIP;
                reqPage = reqPage +   "&gwPort=" + this.gwyPort;
                reqPage = reqPage +   "&token=" + this.gwyToken;
                
                if(logger.isDebugEnabled()) {                
                    logger.debug("Performing Direct SIP thru NAT proxy: " + this.proxyHost + ":" + this.proxyPort + " for gguid=" + this.gGuid + ", gwIP=" + this.gwyIP + ", req:" + reqPage);
                }
                
            }else {     //NAT SIP signalling
                reqPage = SERVICE_PAGE + "?service=" + SERVICE_TYPE;
                reqPage = reqPage +   "&token=" + this.gwyToken;
                reqPage = reqPage +   "&gwIP=" + this.gwyIP;
                reqPage = reqPage +   "&gwPort=" + this.gwyPort;
                reqPage = reqPage +   "&proxyPort=" + this.proxySourcePort;
                if(this.gwyPortUDP != -999) {
                    reqPage = reqPage +   "&udpPort=" + this.gwyPortUDP;
                }else {
                    reqPage = reqPage +   "&udpPort=";                
                }
                
                if(logger.isDebugEnabled()) {                
                    logger.debug("Performing NAT SIP thru NAT proxy: " + this.proxyHost + ":" + this.proxyPort + " for gguid=" + this.gGuid + ", gwIP=" + this.gwyIP + ", req:" + reqPage);
                }
            }

            int rc = SimpleHttpClient.sendRequest(this.proxyHost, this.proxyPort, reqPage);
            if(logger.isDebugEnabled()) {                
                logger.debug("NAT SIP DONE --> RC=" + rc);
            }
            
        }catch(Exception e) {
            if(logger.isDebugEnabled()) {                
                logger.debug((proxySourcePort==-999 ? "Direct" : "NAT") + " SIP Failed thru NAT proxy: " + this.proxyHost + ":" + this.proxyPort + " for gguid=" + this.gGuid + ":"+ this.gwyPort + ", gwIP=" + this.gwyIP + ", exception: ", e);
            }else {
                logger.warn((proxySourcePort==-999 ?  "Direct" : "NAT") + " SIP Failed thru NAT proxy: " + this.proxyHost + ":" + this.proxyPort + " for gguid=" + this.gGuid + ":"+ this.gwyPort + ", gwIP=" + this.gwyIP + ", exception: " + e.getMessage());
            }
        }
    }

 
/*
    // test main
    public static void main(String[] args) {

        InitiatedPoll rt = new InitiatedPoll("core", "tin.corecam.com", 8080, "");
        rt.run();
        System.err.println("OUT");;
    }
*/
}
