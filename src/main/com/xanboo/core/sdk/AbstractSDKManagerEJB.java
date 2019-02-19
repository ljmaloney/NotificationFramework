/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/AbstractSDKManagerEJB.java,v $
 * $Id: AbstractSDKManagerEJB.java,v 1.1 2011/07/01 16:11:31 levent Exp $
 *
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk;

import com.xanboo.core.model.XanbooGateway;
import com.xanboo.core.util.EjbProxy;
import com.xanboo.core.util.GlobalNames;
import com.xanboo.core.util.InitiatedPoll;
import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;
import com.xanboo.core.util.ProxyInitiatedPoll;


/**
 * <p>
 * Abstract Session Bean class for all SDK Manager EJB implementations
 * </p>
 */

public abstract class AbstractSDKManagerEJB  {
  
	private static final long serialVersionUID = 7257352515023975530L;

	protected Logger logger = LoggerFactory.getLogger(getClass().getName());
    
	private static EjbProxy proxy;
     //--------------- Common SDK EJB methods ------------------------------------------------------

    public static void pollGateway(XanbooGateway gwy) {

        if(gwy.isBBonline() && gwy.getInbound()>0) {          //else if inbound>0 --> direct SIP, if BB interface is online
            String natIP = GlobalNames.lookupNatIp(null);
            if(gwy.getInbound() == 1) {
                if(natIP==null) {
                    InitiatedPoll ipoll = new InitiatedPoll(gwy.getGatewayGUID(), gwy.getHeaderIP(), gwy.getInboundPort(), gwy.getToken());
                }else {
                    ProxyInitiatedPoll ipoll = new ProxyInitiatedPoll(gwy.getGatewayGUID(), natIP, -999, gwy.getHeaderIP(), gwy.getInboundPort(), -999, gwy.getToken());
                }
            }else if(gwy.getInbound()==2) {
                if(natIP==null) {
                    InitiatedPoll ipoll = new InitiatedPoll(gwy.getGatewayGUID(), gwy.getApplianceIP(), gwy.getInboundPort(), gwy.getToken());
                }else {
                    ProxyInitiatedPoll ipoll = new ProxyInitiatedPoll(gwy.getGatewayGUID(), natIP, -999, gwy.getApplianceIP(), gwy.getInboundPort(), -999, gwy.getToken());
                }    
            }
        }else if(GlobalNames.SIP_PREFER_NAT && gwy.isBBonline() && gwy.getCaps().indexOf("N=1") != -1 && gwy.getNATInbound()>0) {         // ifinbound=0, natInbound>0 and supports NAT --> NAT SIP (in preference to 3G)
            //lookup internal NAT proxy hostname/ip while calling
            ProxyInitiatedPoll ipoll = new ProxyInitiatedPoll(gwy.getGatewayGUID(), GlobalNames.lookupNatIp(gwy.getProxyIP()), gwy.getProxyPort(), gwy.getHeaderIP(), gwy.getInboundPort(), gwy.getInboundPortUDP(), gwy.getToken());
        }else if(gwy.is3Gonline() && gwy.getWirelessIP() != null && gwy.getWirelessIP().length() > 0) {   // if 3G ip is set, SIP thru 3G APN if online
            String natIP = GlobalNames.lookupNatIp(null);
            if(natIP==null) {
                InitiatedPoll ipoll = new InitiatedPoll(gwy.getGatewayGUID(), gwy.getWirelessIP(), gwy.getInboundPort(), gwy.getToken());
            }else {
                ProxyInitiatedPoll ipoll = new ProxyInitiatedPoll(gwy.getGatewayGUID(), natIP, -999, gwy.getWirelessIP(), gwy.getInboundPort(), -999, gwy.getToken());
            }
        }else if(gwy.isBBonline() && gwy.getCaps().indexOf("N=1") != -1 && gwy.getNATInbound()>0) {         // ifinbound=0, natInbound>0 and supports NAT --> NAT SIP
            //lookup internal NAT proxy hostname/ip while calling
            ProxyInitiatedPoll ipoll = new ProxyInitiatedPoll(gwy.getGatewayGUID(), GlobalNames.lookupNatIp(gwy.getProxyIP()), gwy.getProxyPort(), gwy.getHeaderIP(), gwy.getInboundPort(), gwy.getInboundPortUDP(), gwy.getToken());
        }
    }

	
    
    /**
     * Look up ejb interface 
     */
    public Object getEJB(String jndiName) {
        Object ejbInterface = null;
    	if(logger.isDebugEnabled()) {
            logger.debug("[getEJB(jndiName)]:");
        } 
        if(proxy == null) proxy = new EjbProxy(GlobalNames.SDK_JNDI_CONTEXT_FACTORY, GlobalNames.SDK_JNDI_PROVIDER_URL);
        ejbInterface =  proxy.getObj(jndiName);
        if(logger.isDebugEnabled()) {
            logger.debug("[getEJB(jndiName)]:  EJB ref :" + ejbInterface);
        } 
        return ejbInterface;
    }
}
