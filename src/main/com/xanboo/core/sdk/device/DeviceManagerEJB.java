/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/device/DeviceManagerEJB.java,v $
 * $Id: DeviceManagerEJB.java,v 1.88 2011/07/07 21:36:40 levent Exp $
 *
 * Copyright 2002 Xanboo, Inc.
 *
 */


package com.xanboo.core.sdk.device;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import org.apache.commons.lang.StringUtils;
import com.xanboo.core.extservices.gateway.ServiceGatewayFactory;
import com.xanboo.core.extservices.outbound.ServiceOutboundProxy;
import com.xanboo.core.extservices.rules.RuleManager;
import com.xanboo.core.mbus.MBusSynchronizer;
import com.xanboo.core.mbus.domain.DLCoreMBusEntityChange;
import com.xanboo.core.model.XanbooBinaryContent;
import com.xanboo.core.model.XanbooGateway;
import com.xanboo.core.model.XanbooMobject;
import com.xanboo.core.sdk.AbstractSDKManagerEJB;
import com.xanboo.core.sdk.pai.PAICommandREST;
import com.xanboo.core.sdk.services.ServiceManager;
import com.xanboo.core.sdk.services.model.ServiceObject;
import com.xanboo.core.sdk.services.model.ServiceSubscription;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooAdminPrincipal;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.EServicePusher;
import com.xanboo.core.util.EjbProxy;
import com.xanboo.core.util.GlobalNames;
import com.xanboo.core.util.SBNSynchronizer;
import com.xanboo.core.util.SimpleACSIClient;
import com.xanboo.core.util.TraceLogger;
import com.xanboo.core.util.XanbooException;
import com.xanboo.core.util.XanbooUtil;
import com.xanboo.core.util.fs.AbstractFSProvider;
import com.xanboo.core.util.fs.XanbooFSProviderProxy;

/**
 * <p>
 * Session Bean implementation of <code>DeviceManager</code>. This bean acts as a wrapper class for
 * all device, device query/controls, event and notification setup related Core SDK methods.
 * </p>
 */
@Stateless (name="DeviceManager")
@TransactionManagement( TransactionManagementType.BEAN )
@Remote(DeviceManager.class)

public class DeviceManagerEJB extends AbstractSDKManagerEJB  {
    
    // related DAO class
    private DeviceManagerDAO dao=null;
    
    //@EJB(beanName = "ServiceManager")
    private ServiceManager sManager = null;
    //@EJB(beanName = "RuleManager"
    private RuleManager ruleManager = null;
    
    //@EJB(beanName = "PAICommandREST")
    private PAICommandREST paiCmdRest = null;
    
    /** Status ID for active devices  */
    public static final int DEVICE_STATUS_ACTIVE=2;
    
    /** Status ID for inactive device*/
    public static final int DEVICE_STATUS_INACTIVE=3;
    
    public static final String OID_TOBE_FILTERED="#99@";

    @PostConstruct
    public void init() throws Exception 
    {
    	
 
        EjbProxy proxy = new EjbProxy();
        try 
        {
            dao = new DeviceManagerDAO();

            paiCmdRest = (PAICommandREST) proxy.getObj(GlobalNames.EJB_PAI_REST_SERVICE);
            sManager = (ServiceManager) proxy.getObj(GlobalNames.EJB_EXTSERVICE_MANAGER);
        } 
        catch (Exception e) 
        {
				 logger.error("[getServiceManagerEJB()]:" + e.getMessage());
        }
        
        try 
        {
            ruleManager = (RuleManager) proxy.getObj(GlobalNames.EJB_RULE_MANAGER);
        } 
        catch (Exception e) 
        {
				 logger.error("[getServiceManagerEJB()]: Error getting reference to RuleManager EJB" + e.getMessage());
        }
    }
    
    
    //--------------- Business methods ------------------------------------------------------
    /* gets a reference to the DeviceManager EJB, if necessary */

    
    /**
     * Generic reference data query procewdure (device class list, action types, etc.)
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param spName stored procedure name to call for reference data
     * @param lang the language in which the reference data will be returned.
     *
     * @return a XanbooResultSet which contains a HashMap list of device classes
     * @throws XanbooException
     *
     */
    private XanbooResultSet getReferenceData(XanbooPrincipal xCaller, String spName, String lang) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getReferenceData()]:");
        }
        
        Connection conn=null;
        try {
            // first validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            return dao.getReferenceData(conn, xCaller.getAccountId(), xCaller.getUserId(), spName, lang);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getReferenceData()]: " + e.getMessage(), e);
            }else {
              logger.error("[getReferenceData()]: " + e.getMessage());
            }                            
            throw new XanbooException(10030, "[getReferenceData]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
        
    }
    
    public String getUserLanguage(XanbooPrincipal xCaller,String gatewayGuid,boolean includeMasterUser) throws XanbooException
    {
    	long startMS = System.currentTimeMillis();
        if (logger.isDebugEnabled()) {
            logger.debug("[getUserLanguage()]: account="+xCaller.getAccountId()+", gatewayGuid="+gatewayGuid+", includeMasterUser="+includeMasterUser);
        }
        
        //validate input parameters, account and gateway are required
        if ( xCaller == null || gatewayGuid == null || gatewayGuid.equalsIgnoreCase("") )
            throw new XanbooException(10050);
        
        Connection conn=null;
        try 
        {
            // first validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            conn = dao.getConnection();
            String lang = dao.getUserLanguage(conn, xCaller, gatewayGuid,includeMasterUser);
            if ( logger.isDebugEnabled() )
                logger.debug("[getUserLanguage()] - language/locale for account/gateway is :"+lang);
            
            
            long endMS = System.currentTimeMillis();
            
            long trace = endMS-startMS;
            
            if(trace >GlobalNames.TRACE_TIME || logger.isDebugEnabled() || (gatewayGuid!=null && GlobalNames.GoldenGGUIDs.contains(gatewayGuid))  ){
            	
            	logger.info("ALERT : getUserLanguage : "+ gatewayGuid +" : " + trace);
            }
            
            return lang;
        }
        catch (XanbooException xe) 
        {
            throw xe;
        }
        catch (Exception e) 
        {
            if(logger.isDebugEnabled()) 
            {
              logger.error("[getUserLanguage()]: " + e.getMessage(), e);
            }
            else 
            {
              logger.error("[getUserLanguage()]: " + e.getMessage());
            }                            
            throw new XanbooException(10030, "[getUserLanguage]:" + e.getMessage());
        }
        finally 
        {
            dao.closeConnection(conn);
        }
    }
    
    
    public XanbooResultSet getDeviceClassList(XanbooPrincipal xCaller) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getDeviceClassList()]:");
        }
        
        try {
            return getDeviceClassList(xCaller, null);
        }catch(XanbooException xe) {
            throw xe;
        }
    }
    
    
    

    public XanbooResultSet getDeviceClassList(XanbooPrincipal xCaller, String lang) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getDeviceClassList()]:");
        }
        
        try {
            return getReferenceData(xCaller, dao.SP_DEVICE_CLASS_LIST, lang);
        }catch(XanbooException xe) {
            throw xe;
        }
        
    }
    
    
    

    public XanbooResultSet getDeviceListByClass(XanbooPrincipal xCaller, String dClass) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getDeviceListByClass()]:");
        }
        
        if ( dClass == null ) {
            //If we are listing all devices, use getDeviceList call.
            return this.getDeviceList( xCaller, null, null );
        } else {
            
            Connection conn=null;
            try {
                // first validate the caller and privileges
                XanbooUtil.checkCallerPrivilege(xCaller);
                
                conn=dao.getConnection();
                return dao.getDeviceListByClass(conn, xCaller, dClass);
            }catch (XanbooException xe) {
                throw xe;
            }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getDeviceListByClass()]: " + e.getMessage(), e);
            }else {
              logger.error("[getDeviceListByClass()]: " + e.getMessage());
            }                                            
                throw new XanbooException(10030, "[getDeviceListByClass]:" + e.getMessage());
            }finally {
                dao.closeConnection(conn);
            }
        }
    }
    

    public XanbooResultSet getDeviceList(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getDeviceList()]: ");
        }
        
        // validate the input parameters
        try {
            if(gatewayGUID!=null && gatewayGUID.trim().equals("") ||
            (deviceGUID!=null && deviceGUID.trim().equals(""))) {
                throw new XanbooException(10050);
            }
        } catch (Exception e) {
            throw new XanbooException(10050);
        }
        
        
        /* ENABLE BACKWARD COMPATIBILITY WITH THE NOTION THAT A GATEWAY'S DEVICE GUID IS THE SAME AS IT'S GATEWAY_GUID */
        deviceGUID = this.checkForGatewayGUID( gatewayGUID, deviceGUID );
        
        Connection conn=null;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            return dao.getDeviceList(conn, xCaller, gatewayGUID, deviceGUID);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getDeviceList()]: " + e.getMessage(), e);
            }else {
              logger.error("[getDeviceList()]: " + e.getMessage());
            }                  
            throw new XanbooException(10030, "[getDeviceList]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
        
    }
    
    

    public void updateDeviceStatus(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, int status ) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[updateDeviceStatus()]: ");
        }
        
        // validate the input parameters
        if(gatewayGUID==null || deviceGUID==null
        || gatewayGUID.trim().equals("") || deviceGUID.trim().equals("")
        || ( status != DEVICE_STATUS_ACTIVE && status != DEVICE_STATUS_INACTIVE ) ) {
            throw new XanbooException(10050);
        }
        
        /* ENABLE BACKWARD COMPATIBILITY WITH THE NOTION THAT A GATEWAY'S DEVICE GUID IS THE SAME AS IT'S GATEWAY_GUID */
        deviceGUID = this.checkForGatewayGUID( gatewayGUID, deviceGUID );
        
        Connection conn=null;
        boolean rollback=false;
        
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            dao.updateDeviceStatus(conn, xCaller.getAccountId(), xCaller.getUserId(), gatewayGUID, deviceGUID, status);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[updateDeviceStatus()]: " + e.getMessage(), e);
            }else {
              logger.error("[updateDeviceStatus()]: " + e.getMessage());
            }                            
            throw new XanbooException(10030, "[updateDeviceStatus]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
    }
    

    public XanbooResultSet getMObject(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String mobjectId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getMObject()]: " + gatewayGUID + ":" + deviceGUID + ":" + mobjectId );
        }
        XanbooResultSet rs=null;
        // validate the input parameters
        if(gatewayGUID==null || gatewayGUID.trim().equals("")) {
            throw new XanbooException(10050);
        }
        
        /* ENABLE BACKWARD COMPATIBILITY WITH THE NOTION THAT A GATEWAY'S DEVICE GUID IS THE SAME AS IT'S GATEWAY_GUID */
        deviceGUID = this.checkForGatewayGUID( gatewayGUID, deviceGUID );
        
        Connection conn=null;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            rs=dao.getMObject(conn, xCaller.getAccountId(), xCaller.getUserId(), gatewayGUID, deviceGUID, mobjectId);
            
            //filter oids if their value is "#99@".  
            Iterator<HashMap> iter = rs.iterator();
            while (iter.hasNext()) {            	
            	 HashMap row = (HashMap) iter.next();
                if (row.get("VALUE").toString().equalsIgnoreCase(OID_TOBE_FILTERED)) {
                    iter.remove();
                }
            }
            
            // for AccuWeather device, invoke ServiceOutboundProxy getMobject to retrieve value.
            if(XanbooUtil.isExternalServiceDevice(deviceGUID) && ServiceGatewayFactory.ACCUWEATHER_DGUID.equalsIgnoreCase(deviceGUID)) {
            	try {
    				Object obj = getEJB(GlobalNames.EJB_OUTBOUND_PROXY);
    				if(obj!=null && obj instanceof ServiceOutboundProxy) {
    					XanbooAdminPrincipal xap=new XanbooAdminPrincipal(GlobalNames.DEFAULT_APP_USER, 0, 0);   
    					xap.setAccountPrincipal(xCaller);   //set account principal to pass caller account info.
    					ServiceOutboundProxy ejb = (ServiceOutboundProxy)obj;
    					List<String> mobjectIds = null;
    					if(StringUtils.isNotBlank(mobjectId)) {
    						mobjectIds = new ArrayList<String>();
    				    	mobjectIds.add(mobjectId);
    					}
    					List<XanbooMobject> xsMobjects = ejb.getMObject(xap, gatewayGUID, deviceGUID, mobjectIds);
    					return getExtServiceMobjectResultSet(xsMobjects, rs);
    				}else {
    					 throw new XanbooException(60002, "[setMObject]: Unable to lookup" + GlobalNames.EJB_OUTBOUND_PROXY);
    				}
    			}catch(XanbooException xe) {
    	             throw xe;
    	        }catch(Exception e) {
    	             if(logger.isDebugEnabled()) {
    	               logger.error("[getMObject()]" + e.getMessage(), e);
    	             }else {
    	               logger.error("[getMObject()]" + e.getMessage());
    	             }
    	             throw new XanbooException(60000, "[getMObject]:" + e.getMessage());
    	         }
            }
            return rs;	
            
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getMObject()]: " + e.getMessage(), e);
            }else {
              logger.error("[getMObject()]: " + e.getMessage());
            }                              
            throw new XanbooException(10030, "[getMObject]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
    }

    public long setMObject(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID,
    String mobjectId, String mobjectValue) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[setMObject()]: ");
        }
        
        /* ENABLE BACKWARD COMPATIBILITY WITH THE NOTION THAT A GATEWAY'S DEVICE GUID IS THE SAME AS IT'S GATEWAY_GUID */
        deviceGUID = this.checkForGatewayGUID( gatewayGUID, deviceGUID );
        
        String[] ids = new String[1];
        String[] values = new String[1];
        
        ids[0] = mobjectId;
        values[0] = mobjectValue;
        
        try {
            return setMObject(xCaller, gatewayGUID, deviceGUID, ids, values);
        }catch (XanbooException xe) {
            throw xe;
        }
    }
    
    

    public long setMObject(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID,
                           String[] mobjectId, String[] mobjectValue) throws XanbooException 
    {
    	long commandQueueId=-1;
    	if(logger.isDebugEnabled()) 
        {
            logger.debug("[setMObject()]: ");
        }
        
        // validate the input parameters
        if( gatewayGUID==null || deviceGUID==null || gatewayGUID.trim().equals("") || 
            deviceGUID.trim().equals("") || mobjectId.length==0 || mobjectValue.length==0) 
        {
            throw new XanbooException(10050);
        }
        
        /* ENABLE BACKWARD COMPATIBILITY WITH THE NOTION THAT A GATEWAY'S DEVICE GUID IS THE SAME AS IT'S GATEWAY_GUID */
        deviceGUID = this.checkForGatewayGUID( gatewayGUID, deviceGUID );
        
        // validate mobjectid/value pairs
        for(int i=0; i<mobjectId.length; i++) 
        {
            if(mobjectId[i]==null || mobjectValue[i]==null
            || mobjectId[i].trim().length()==0 || mobjectId[i].length()>16
            || mobjectValue[i].length()>GlobalNames.MOBJECT_VALUE_MAXLEN) 
            {
                throw new XanbooException(10050);
            }
            
            if(mobjectValue[i].length()==0) 
            {
                mobjectValue[i] = "null";
            }
        }
        
        // for external service device, invoke ServiceOutBoundProxyEJB's setMobject.
        if(XanbooUtil.isExternalServiceDevice(deviceGUID)) {
        	List<XanbooMobject> mobjects = new ArrayList<XanbooMobject>();
	        for(int i=0; i < mobjectId.length; i++) { 
	        	mobjects.add(new XanbooMobject(mobjectId[i], mobjectValue[i]));
	        }
        	
        	try {
				Object obj = getEJB(GlobalNames.EJB_OUTBOUND_PROXY);
				if(obj!=null && obj instanceof ServiceOutboundProxy) {
					XanbooAdminPrincipal xap=new XanbooAdminPrincipal(GlobalNames.DEFAULT_APP_USER, 0, 0);   
					xap.setAccountPrincipal(xCaller);   //set account principal to pass caller account info.
					ServiceOutboundProxy ejb = (ServiceOutboundProxy)obj;
					commandQueueId = ejb.setMobject(xap, gatewayGUID, deviceGUID, mobjects);
					if(logger.isDebugEnabled()) {
						logger.debug("service device mobject updated:" +commandQueueId);
					}
				}else {
					 throw new XanbooException(60002, "[setMObject]: Unable to lookup" + GlobalNames.EJB_OUTBOUND_PROXY);
				}
			}catch(XanbooException xe) {
	             throw xe;
	        }catch(Exception e) {
	             if(logger.isDebugEnabled()) {
	               logger.error("[setMObject()]" + e.getMessage(), e);
	             }else {
	               logger.error("[setMObject()]" + e.getMessage());
	             }
	             throw new XanbooException(60000, "[setMObject]:" + e.getMessage());
	         }
        } else {
        	Connection conn=null;
            boolean rollback=false;
            boolean paiSuccess = false;
            XanbooGateway gwyInfo=null;
         
            try 
            {
                // validate the caller and privileges
                XanbooUtil.checkCallerPrivilege(xCaller);
                
                conn=dao.getConnection();
                commandQueueId = dao.setMObject(conn, xCaller.getAccountId(), xCaller.getUserId(), gatewayGUID, deviceGUID, mobjectId, mobjectValue, null);

                //special handling for testMode OID 9 !!!
                if(GlobalNames.MODULE_SBN_ENABLED) 
                {  //apply, if SBN enabled
                    for(int i=0; i<mobjectId.length; i++) 
                    { // loop thru oids to see if OID 9 is references
                        if(mobjectId[i].equals(GlobalNames.OID_WELLKNOWN_TESTMODE)) 
                        {
                            //get gateway's timezone to pass to SBN call
                            XanbooResultSet mobjects = getMObject(xCaller, gatewayGUID,"0", GlobalNames.OID_WELLKNOWN_TIMEZONE);
                            if(mobjects != null && mobjects.size() >0) 
                            {
                                SBNSynchronizer sbn = new SBNSynchronizer();
                                //testmode oid value format:
                                //    start test : "<timestamp>,<userid>" or "<timestamp>"
                                //    stop test  : "" or ",<userid>"
                                String ts = null;
                                String userid = null;
                                mobjectValue[i] = mobjectValue[i].trim();
                                int ix = mobjectValue[i].indexOf(",");
                                if(ix==0) 
                                {         //ix=0  --> this is a ",<userid" value
                                    userid = mobjectValue[i].substring(1);
                                }
                                else if(ix==-1) 
                                {   //ix=-1 --> this is either a "" or "<timestamp>" value
                                    ts = mobjectValue[i];  //"<19char timestamp>"
                                }
                                else 
                                {             //ix>0  --> this is a "<timestamp>,<userid>" value
                                    ts = mobjectValue[i].substring(0, ix);
                                    userid = mobjectValue[i].substring(ix+1);
                                }

                                if(ts != null && ts.length()!= 19) ts=null;   //ts must be 19chars!

                                int rc = sbn.setTestMode(gatewayGUID, deviceGUID, ts, mobjects.getElementString(0, "VALUE"), userid, false);
                                if(rc!=0) 
                                {
                                    throw new XanbooException(20090, "Failed to set test mode in SBN");
                                }
                            }
                            break;  //break out of the loop
                        }
                    }
                }
                
                //get gwy info for polling
                gwyInfo = dao.getGatewayInfo(conn, xCaller.getAccountId(), gatewayGUID);   
                if(gwyInfo!=null && gwyInfo.isTraceLogging()) {
                    TraceLogger.log(Long.toString(commandQueueId), Long.toString(xCaller.getAccountId()), gatewayGUID, deviceGUID, this.getClass().getSimpleName(), "setMobject", mobjectId[0], mobjectValue[0]);
                }
                
            }
            catch (XanbooException xe) 
            {
                rollback=true;
                if(xe.getCode() != 26134) 
                {  // do not fail, if no command was inserted, since there was already a command in the queue
                    throw xe;
                }
            }
            catch (Exception e) 
            {
                if(logger.isDebugEnabled()) 
                {
                  logger.error("[setMObject()]: " + e.getMessage(), e);
                }
                else 
                {
                  logger.error("[setMObject()]: " + e.getMessage());
                }                                          
                rollback=true;
                throw new XanbooException(10030, "[setMObject]:" + e.getMessage());
            }
            finally 
            {
                dao.closeConnection(conn, rollback);
            }
            
            try
            {  
            	
            	 if(logger.isDebugEnabled()) {
            		 logger.debug("******************************************");
            		 
            		 logger.debug(" gwyInfo : " + gwyInfo);
            		 logger.debug(" GlobalNames.MODULE_PAI_ENABLED : " + GlobalNames.MODULE_PAI_ENABLED);
            		 logger.debug(" GlobalNames : " + GlobalNames.class);
            		 logger.debug(" mobjectId : " + mobjectId);
            		
            	 }
            	
            	// do not send OID1=0 commands (user logged in) through PAI to improve login times
                if ( gwyInfo != null && GlobalNames.MODULE_PAI_ENABLED && gwyInfo.getPAIServerURI()!= null && 
                     !gwyInfo.getPAIServerURI().equalsIgnoreCase("") &&
                     !(mobjectId.length == 1 && mobjectId[0].equals("1") && mobjectValue[0].equals("0")) )
                {
                   // this.getPAIManagerEJB();
                              List<HashMap> params = new ArrayList<HashMap>();
                    for ( int i = 0; i < mobjectId.length; i++)
                    {
                        HashMap paramMap = new HashMap<String,String>();
                        params.add(paramMap);
                        paramMap.put("PARAM", mobjectId[i]);
                        paramMap.put("VALUE", mobjectValue[i]);
                    }
                    
               	 if(logger.isDebugEnabled()) {
               		 logger.debug("params : " + params);
               		 
               		 logger.debug("calling paiCmdRest.sendSetObject");
               	 }
                    //GregorianCalendar gcal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
                    String status = paiCmdRest.sendSetObject(new XanbooAdminPrincipal("admin",99,99),gatewayGUID,deviceGUID,new java.util.Date(),
                                                             commandQueueId,gwyInfo.getPAIServerURI(),gwyInfo.getPAIAccessToken(),params);
                	 if(logger.isDebugEnabled()) {
                   		 logger.debug("status : " + status);
                	 }
                    
                    if ( status.equalsIgnoreCase("no_registry_entry"))
                    {
                        logger.debug("[setMObject] - gateway not connected / no registry entry for gateway " +gatewayGUID);
                        paiSuccess = false;
                    }
                    else if ( status.equalsIgnoreCase("success"))
                    {
                        paiSuccess = true;
                        paiCmdRest.updateCommandStatus(new XanbooAdminPrincipal("admin",99,99), commandQueueId);
                    }
                    else //an error was returned for the command from PAI server
                    {
                        logger.info("[setMObject] - an error returned from PAI server, "+status);
                    }
                }
                
                if(logger.isDebugEnabled()) {
           		 logger.debug("******************************************");
           	 }
            }
            catch(XanbooException xe)
            {
                logger.warn("[setMObject] - error sending command via PAI server",xe);
            }
            catch(Exception ex)
            {
                logger.warn("[setMObject] - error sending command via PAI sever",ex);
            }
            
            if ( !paiSuccess )  //if command was not send to the pai server and returned a good response, poll
            {
                //send poll request to DLC
                if(!rollback && gwyInfo!=null) 
                {
                    pollGateway(gwyInfo);
                }
            }
        }
        
        return commandQueueId;
    }
    
    

    public XanbooResultSet getDeviceEvent(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String eventId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getDeviceEvent()]: ");
        }
        
        // validate the input parameters
        if(gatewayGUID==null || deviceGUID==null || gatewayGUID.trim().equals("") || deviceGUID.trim().equals("")) {
            throw new XanbooException(10050);
        }
        
        /* ENABLE BACKWARD COMPATIBILITY WITH THE NOTION THAT A GATEWAY'S DEVICE GUID IS THE SAME AS IT'S GATEWAY_GUID */
        deviceGUID = this.checkForGatewayGUID( gatewayGUID, deviceGUID );
        
        Connection conn=null;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            return dao.getDeviceEvent(conn, xCaller.getAccountId(), xCaller.getUserId(), gatewayGUID, deviceGUID, eventId);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getDeviceEvent()]: " + e.getMessage(), e);
            }else {
              logger.error("[getDeviceEvent()]: " + e.getMessage());
            }                     
            throw new XanbooException(10030, "[getDeviceEvent]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
        
    }  
    

    public XanbooResultSet getDeviceEventLog(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String eventId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getDeviceEventLog()]: ");
        }
        
        // validate the input parameters
        if(gatewayGUID==null || gatewayGUID.trim().equals("")) {
            throw new XanbooException(10050);
        }
        
        /* ENABLE BACKWARD COMPATIBILITY WITH THE NOTION THAT A GATEWAY'S DEVICE GUID IS THE SAME AS IT'S GATEWAY_GUID */
        deviceGUID = this.checkForGatewayGUID( gatewayGUID, deviceGUID );
        
        Connection conn=null;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            return dao.getDeviceEventLog(conn, xCaller.getAccountId(), xCaller.getUserId(), gatewayGUID, deviceGUID, eventId);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getDeviceEventLog()]: " + e.getMessage(), e);
            }else {
              logger.error("[getDeviceEventLog()]: " + e.getMessage());
            }                     
            throw new XanbooException(10030, "[getDeviceEventLog]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
        
    }
    
    

    public void clearDeviceEventLog(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String eventId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[clearDeviceEventLog()]: ");
        }
        
        // validate the input parameters
        if(gatewayGUID==null || gatewayGUID.trim().equals("")) {
            throw new XanbooException(10050);
        }
        
        /* ENABLE BACKWARD COMPATIBILITY WITH THE NOTION THAT A GATEWAY'S DEVICE GUID IS THE SAME AS IT'S GATEWAY_GUID */
        deviceGUID = this.checkForGatewayGUID( gatewayGUID, deviceGUID );
        
        Connection conn=null;
        boolean rollback = true;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            dao.clearDeviceEventLog(conn, xCaller.getAccountId(), xCaller.getUserId(), gatewayGUID, deviceGUID, eventId);
            rollback = false;
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[clearDeviceEventLog()]: " + e.getMessage(), e);
            }else {
              logger.error("[clearDeviceEventLog()]: " + e.getMessage());
            }            
            throw new XanbooException(10030, "[clearDeviceEventLog]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
    }
    

    public XanbooResultSet getCommandQueueItem(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getCommandQueueItem()]: ");
        }
        
        // validate the input parameters
        if(gatewayGUID==null || gatewayGUID.trim().equals("")) {
            throw new XanbooException(10050);
        }
        
        /* ENABLE BACKWARD COMPATIBILITY WITH THE NOTION THAT A GATEWAY'S DEVICE GUID IS THE SAME AS IT'S GATEWAY_GUID */
        deviceGUID = this.checkForGatewayGUID( gatewayGUID, deviceGUID );
        
        Connection conn=null;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            return dao.getCommandQueueItem(conn, xCaller.getAccountId(), xCaller.getUserId(), gatewayGUID, deviceGUID);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getCommandQueueItem()]: " + e.getMessage(), e);
            }else {
              logger.error("[getCommandQueueItem()]: " + e.getMessage());
            }            
            throw new XanbooException(10030, "[getCommandQueueItem]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
        
    }
    

    public void deleteCommandQueueItem(XanbooPrincipal xCaller, String gatewayGUID, long[] queueId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteCommandQueueItem()]: ");
        }
        
        // validate the input parameters
        if(gatewayGUID==null || gatewayGUID.trim().equals("")) {
            throw new XanbooException(10050);
        }
        
        // validate command queue ids
        for(int i=0; i<queueId.length; i++) {
            if(queueId[i]<1) {
                throw new XanbooException(10050);
            }
        }
        
        Connection conn=null;
        boolean rollback=false;
        
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            dao.deleteCommandQueueItem(conn, xCaller.getAccountId(), xCaller.getUserId(), gatewayGUID, queueId);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[deleteCommandQueueItem()]: " + e.getMessage(), e);
            }else {
              logger.error("[deleteCommandQueueItem()]: " + e.getMessage());
            }                        
            rollback=true;
            throw new XanbooException(10030, "[deleteCommandQueueItem]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
    }
        

    public void emptyCommandQueue(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[emptyCommandQueue()]: ");
        }
        
        // validate the input parameters
        if(gatewayGUID==null || gatewayGUID.trim().equals("")) {
            throw new XanbooException(10050);
        }
        
        /* ENABLE BACKWARD COMPATIBILITY WITH THE NOTION THAT A GATEWAY'S DEVICE GUID IS THE SAME AS IT'S GATEWAY_GUID */
        deviceGUID = this.checkForGatewayGUID( gatewayGUID, deviceGUID );
        
        Connection conn=null;
        boolean rollback=false;
        
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            dao.emptyCommandQueue(conn, xCaller.getAccountId(), xCaller.getUserId(), gatewayGUID, deviceGUID);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[emptyCommandQueue()]: " + e.getMessage(), e);
            }else {
              logger.error("[emptyCommandQueue()]: " + e.getMessage());
            }
            rollback=true;
            throw new XanbooException(10030, "[emptyCommandQueue]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
    }
    
    
    public XanbooResultSet getNotificationActionList(XanbooPrincipal xCaller, String gatewayGUID,
    String deviceGUID, String eventId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getNotificationActionList()]: ");
        }

        /*
        // validate the input parameters
        if(gatewayGUID==null || gatewayGUID.trim().equals("")) {
            throw new XanbooException(10050);
        }
        */

        /* ENABLE BACKWARD COMPATIBILITY WITH THE NOTION THAT A GATEWAY'S DEVICE GUID IS THE SAME AS IT'S GATEWAY_GUID */
        deviceGUID = this.checkForGatewayGUID( gatewayGUID, deviceGUID );

        Connection conn=null;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();

            return dao.getNotificationActionList(conn, xCaller.getAccountId(), xCaller.getUserId(), gatewayGUID, deviceGUID, eventId);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getNotificationActionList()]: " + e.getMessage(), e);
            }else {
              logger.error("[getNotificationActionList()]: " + e.getMessage());
            }            
            throw new XanbooException(10030, "[getNotificationActionList]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
    }
    
    public void newNotificationAction(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID,
    String eventId, long[] profileId ) throws XanbooException {
        newNotificationAction( xCaller, gatewayGUID, deviceGUID, eventId, profileId, -1 );
    }    

    public void newNotificationAction(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID,
    String eventId, long[] profileId, int quietTime) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[newNotificationAction()]: ");
        }
        
        // validate the input parameters
        if(gatewayGUID==null || deviceGUID==null || eventId==null || profileId.length<1
        || gatewayGUID.trim().equals("") || deviceGUID.trim().equals("") || eventId.trim().equals("")) {
            throw new XanbooException(10050);
        }
        
        /* ENABLE BACKWARD COMPATIBILITY WITH THE NOTION THAT A GATEWAY'S DEVICE GUID IS THE SAME AS IT'S GATEWAY_GUID */
        deviceGUID = this.checkForGatewayGUID( gatewayGUID, deviceGUID );
        
        // validate profile ids
        for(int i=0; i<profileId.length; i++) {
            if(profileId[i]<-1) {
                throw new XanbooException(10050);
            }
        }
        
        Connection conn=null;
        boolean rollback=false;
        
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            dao.newNotificationAction(conn, xCaller.getAccountId(), xCaller.getUserId(), gatewayGUID, deviceGUID, eventId, profileId, quietTime);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[newNotificationAction()]: " + e.getMessage(), e);
            }else {
              logger.error("[newNotificationAction()]: " + e.getMessage());
            }                        
            throw new XanbooException(10030, "[newNotificationAction]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
    }
       

    public void deleteNotificationAction(XanbooPrincipal xCaller, String gatewayGUID,
    String deviceGUID, String eventId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteNotificationAction()]: ");
        }
        
        long[] actionId = new long[1];
        actionId[0]=-1;  // special action id value to delete all actions for the event
        
        /* ENABLE BACKWARD COMPATIBILITY WITH THE NOTION THAT A GATEWAY'S DEVICE GUID IS THE SAME AS IT'S GATEWAY_GUID */
        deviceGUID = this.checkForGatewayGUID( gatewayGUID, deviceGUID );
        
        deleteNotificationAction(xCaller, gatewayGUID, deviceGUID, eventId, actionId);
    }
    
    

    public void deleteNotificationAction(XanbooPrincipal xCaller, String gatewayGUID,
    String deviceGUID, String eventId, long[] actionId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteNotificationAction()]: ");
        }
        
        // validate the input parameters
        if(gatewayGUID==null || deviceGUID==null || eventId==null
        || gatewayGUID.trim().equals("") || deviceGUID.trim().equals("") || eventId.trim().equals("")) {
            throw new XanbooException(10050);
        }
        
        /* ENABLE BACKWARD COMPATIBILITY WITH THE NOTION THAT A GATEWAY'S DEVICE GUID IS THE SAME AS IT'S GATEWAY_GUID */
        deviceGUID = this.checkForGatewayGUID( gatewayGUID, deviceGUID );
        
        // validate the action ids
        for(int i=0; i<actionId.length; i++) {
            if(actionId[i]<-1) {
                throw new XanbooException(10050);
            }
        }
        
        Connection conn=null;
        boolean rollback=false;
        
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            dao.deleteNotificationAction(conn, xCaller.getAccountId(), xCaller.getUserId(), gatewayGUID, deviceGUID, eventId, actionId);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[deleteNotificationAction()]: " + e.getMessage(), e);
            }else {
              logger.error("[deleteNotificationAction()]: " + e.getMessage());
            }                        
            throw new XanbooException(10030, "[deleteNotificationAction]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
    }
    
    
    
    public XanbooGateway getGatewayInfo(XanbooPrincipal xCaller, String gatewayGUID) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getGatewayInfo()]: ");
        }
        
        // validate the input parameters
        if(gatewayGUID==null || gatewayGUID.trim().equals("")) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback=false;
        XanbooGateway gatewayInfo=new XanbooGateway(gatewayGUID, xCaller.getAccountId());
        
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            return  dao.getGatewayInfo(conn, xCaller.getAccountId(), gatewayGUID);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getGatewayInfo()]: " + e.getMessage(), e);
            }else {
              logger.error("[getGatewayInfo()]: " + e.getMessage());
            }                                    
            rollback=true;
            throw new XanbooException(10030, "[getGatewayInfo]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
    }
    

    public void deleteDevice(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteDevice()]: ");
        }
        
        // validate the input parameters
        if(gatewayGUID==null || gatewayGUID.length()==0 || deviceGUID==null || deviceGUID.length()==0) {
            throw new XanbooException(10050, "Invalid gateway/device guid.");
        }
        
        /* ENABLE BACKWARD COMPATIBILITY WITH THE NOTION THAT A GATEWAY'S DEVICE GUID IS THE SAME AS IT'S GATEWAY_GUID */
        deviceGUID = this.checkForGatewayGUID( gatewayGUID, deviceGUID );
        
        //route to ServiceManager if this is a newDevice call for a service device
        if(XanbooUtil.isExternalServiceDevice(deviceGUID)) {
            try {
              //  getServiceManagerEJB();
               // gatewayGUID = GlobalNames.DLLITE_GATEWAY_PREFIX+xCaller.getAccountId();
                sManager.deleteDevice(xCaller, gatewayGUID, deviceGUID);
                return;
                
            }catch(XanbooException xe) {
                throw xe;
            }catch(Exception e) {
                if(logger.isDebugEnabled()) {
                  logger.error("[deleteDevice()]" + e.getMessage(), e);
                }else {
                  logger.error("[deleteDevice()]" + e.getMessage());
                }
                throw new XanbooException(10050, "[deleteDevice]:" + e.getMessage());
            }
            
        }
        String subsId = null;//DF#21450
        String catId = null;
        Connection conn=null;
        boolean rollback = true;
       
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            catId = dao.deleteDevice(conn, xCaller.getAccountId(), xCaller.getUserId(), gatewayGUID, deviceGUID);
            
            //sync with ACSI, if enabled
            //if NOT a zwave device (end-devices only!)
            if(!deviceGUID.equals("0") && catId!=null && !catId.substring(0, 4).equals("1005")) {
                if(GlobalNames.MODULE_CSI_ENABLED) {
                    XanbooResultSet xrs = dao.getSubscription(conn, xCaller, null, null);
                    for(int i=0; xrs!=null && i<xrs.size(); i++) {
                        if(xrs.getElementString(i, "GATEWAY_GUID").equalsIgnoreCase(gatewayGUID)) {
                            SimpleACSIClient acsi = new SimpleACSIClient();
                            
                            subsId = xrs.getElementString(0, "SUBS_ID");//DF#21450
                            
                            
                            int rc = acsi.deviceRemoved(xrs.getElementString(0, ""), 
                            						subsId  , 
                                                    gatewayGUID, 
                                                    deviceGUID,
                                                    null );
                            break;
                        }
                    }
                }
            }
            
            String mobjectDir = AbstractFSProvider.getBaseDir(null, xCaller.getDomain(), xCaller.getAccountId(), AbstractFSProvider.DIR_ACCOUNT_MOBJECT);
            
            //If deleting a gateway, remove it's MOBJECT directories
            if(deviceGUID.equals(gatewayGUID) || deviceGUID.equals("0")) {
                //If anything goes wrong here, we don't want to roll back, so use a separate try block.
                try {
                    XanbooFSProviderProxy.getInstance().rmDir(null, mobjectDir + "/" + gatewayGUID, true);
                            
                } catch (Exception e) {
                    if (logger.isDebugEnabled()) {
                        logger.error( "[deleteDevice]:" + e.getMessage(), e);
                    } else {
                        logger.error( "[deleteDevice]:" + e.getMessage() );
                    }
                }
                
                //TODO: delete all zones from SBN here!!!
                /*
                if(GlobalNames.MODULE_SBN_ENABLED) {
                    SBNSynchronizer sbn = new SBNSynchronizer();

                    //first get all devices for this gateway
                    XanbooResultSet xset = dao.getDeviceList(conn, xCaller, gatewayGUID, null);
                    for(int i=0; i<xset.size(); i++) {
                        HashMap dev = (HashMap) xset.get(i);
                        if(!sbn.removeDevice(gatewayGUID, (String)dev.get("DEVICE_GUID"), ((String)dev.get("CATALOG_ID")).substring(1), false)) {    //rollback, if it fails
                            throw new XanbooException(10030, "[deleteDevice]: Failed to remove device/zone from SBN.");
                        }
                    }
                }
                */
                
            }else {     // it is am end-device deletion, send SIP to gwy

                //If anything goes wrong here, we don't want to roll back, so use a separate try block.
                try {
                    
                    try {
                        XanbooGateway gwyInfo = dao.getGatewayInfo(conn, xCaller.getAccountId(), gatewayGUID);
                        pollGateway(gwyInfo);
                    }catch(Exception ee) {} //ignore sip polling exceptions

                }catch (Exception e) {
                    if (logger.isDebugEnabled()) {
                        logger.error("[deleteDevice]:" + e.getMessage(), e);
                    } else {
                        logger.error("[deleteDevice]:" + e.getMessage() );
                    }
                }
                
            }
            
            rollback = false;
            
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[deleteDevice()]: " + e.getMessage(), e);
            }else {
              logger.error("[deleteDevice()]: " + e.getMessage());
            }                                                
            throw new XanbooException(10030, "[deleteDevice]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
        
        //sync with other external entity listeners - SBN and eService
        //if NOT a zwave device (end-devices only!)
        if(!deviceGUID.equals("0") && catId!=null && !catId.substring(0, 4).equals("1005")) {
            //now sync with SBN, if enabled
            if(GlobalNames.MODULE_SBN_ENABLED) {
                SBNSynchronizer sbn = new SBNSynchronizer();
                if(!sbn.removeDevice(gatewayGUID, deviceGUID, catId, false)) {    //rollback, if it fails
                    throw new XanbooException(10030, "[deleteDevice]: Failed to remove device/zone from SBN.");
                }
            }
            
            // sync with eService, if enabled
            if(GlobalNames.ESERVICE_URL !=null) {
                EServicePusher.post2EntityListener(xCaller.getAccountId(), gatewayGUID, deviceGUID, null, null, null, true);
            }
        }
        
      //DF#21450
        //sync with MBUS, if enabled -  start            
        if(GlobalNames.MBUS_SYNC_ENABLED){
        	
        	String date = XanbooUtil.getCurrentGMTDateTime();
			

        	if(logger.isDebugEnabled()) {
                StringBuffer sb = new StringBuffer("[MBus : publish messages : DeviceManagerEJB.deleteDevice()]:");
                sb.append("DomainId : " + xCaller.getDomain());
                sb.append("\n AccountId : " + xCaller.getAccountId());
                sb.append("\n catId : " + catId  );
                sb.append("\n ExtAccountId : " + null );
                sb.append("\n SubId : " + subsId );
                sb.append("\n deviceGUID : " + deviceGUID);
                sb.append("\n GatewayGUID : " + gatewayGUID);
                sb.append("\n TS : " + date );
                sb.append("\n UserId  : " + xCaller.getUserId()   );

                logger.debug(sb.toString());            		
        	}
        	
        
        	
        	DLCoreMBusEntityChange data = new DLCoreMBusEntityChange();

			data.setDomain(xCaller.getDomain());

			data.setAccId(xCaller.getAccountId());

			data.setCatId(catId);
			data.setDeviceGUID(deviceGUID);

			data.setExtAccId( null);
			data.setSubsId(subsId);

			data.setFw(null);
			data.setGatewayGUID(gatewayGUID);

			data.setHwId(null);
			data.setInsId(null);
			data.setLabel(null);
			data.setMake(null);
			data.setModel(null);
			data.setSerial(null);
			data.setSrcAppId("DeviceManagerEJB.deleteDevice" );
			data.setSrcOriginatorId(xCaller.getUserId()+"");
			// data.setStatus(-1);

			data.setSw(null);

			
			data.setTs(date);

			MBusSynchronizer.deleteDevice(data);
			
			
  
        }//MBus -  end
        
//DF#21450
    }
    
    /*
     * This method is a temporary fix to allow backward compatibility with the notion that a gateway's device guid is the same as it's gateway guid.
     * <br>
     * The new system instead has the character 0 (zero) for a gateway's device guid.
     */
    private String checkForGatewayGUID(String gatewayGUID, String deviceGUID) {
        if (deviceGUID != null && gatewayGUID != null && deviceGUID.equals(gatewayGUID)) {
            return "0";
        } else {
            return deviceGUID;
        }
        
    }



    public XanbooBinaryContent getMObjectBinary(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String mobjectId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getMObjectBinary()]: " + gatewayGUID + ":" + deviceGUID + ":" + mobjectId );
        }
        
        // validate the input parameters
        if(gatewayGUID==null || gatewayGUID.trim().equals("") || deviceGUID==null || deviceGUID.trim().equals("") || mobjectId==null || mobjectId.trim().equals("")) {
            throw new XanbooException(10050);
        }
        
        
        Connection conn=null;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            
            //first get content type from mobject_value record
            XanbooResultSet rs = dao.getMObject(conn, xCaller.getAccountId(), xCaller.getUserId(), gatewayGUID, deviceGUID, mobjectId);
            if(rs!=null && rs.size()>0) {   //mobject record found in mobject_value, get content type and timestamp
                HashMap row = (HashMap) rs.get(0);

                XanbooBinaryContent xbin = null;  
                String contentType = (String) row.get("VALUE");
                String ts = (String) row.get("TIMESTAMP");

                //now get binary content from mobject_binary table
                rs = dao.getMObjectBinary(conn, xCaller.getAccountId(), xCaller.getUserId(), gatewayGUID, deviceGUID, mobjectId);                
                if(rs!=null && rs.size()>0) {   //binary found in DB mobject_binary table, return it!
                    row = (HashMap) rs.get(0);
                    xbin = new XanbooBinaryContent(contentType, (byte[]) row.get("CONTENT"));
                    xbin.setLastModified(ts);
                    return xbin;
                }

                //not found in DB mobject_binary -> get it from File system as legacy BAU - TO bE DEPRECATED!!!
                String filePath = AbstractFSProvider.getBaseDir(null, xCaller.getDomain(), xCaller.getAccountId(), AbstractFSProvider.DIR_ACCOUNT_MOBJECT) + "/" + gatewayGUID + "/" + deviceGUID + "." + mobjectId + ".0" ;
                try {
                    byte[] bytes = XanbooFSProviderProxy.getInstance().getFileBytes(null, filePath);
                    
                    //if file found on FS, migrate to DB
                    if(bytes!=null) {
                        dao.setMObjectBinary(conn, xCaller.getAccountId(), xCaller.getUserId(), gatewayGUID, deviceGUID, mobjectId, bytes);
                        
                        //remove binary from NAS too
                        XanbooFSProviderProxy.getInstance().removeFile(null, filePath);
                    }
                    
                    xbin = new XanbooBinaryContent(contentType, bytes);
                    xbin.setLastModified(ts);
                    return xbin;
                }catch(Exception e) {
                    throw new XanbooException(22120, "Failed to get item binary from filesystem. File not found: " + filePath);
                }
            }
            
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getMObjectBinary()]: " + e.getMessage(), e);
            }else {
              logger.error("[getMObjectBinary()]: " + e.getMessage());
            }                              
            throw new XanbooException(10030, "[getMObjectBinary]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }

        return null;    //mobject not found
    }

    

    public long setMObjectBinary(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID,
            String mobjectId, XanbooBinaryContent mobjectBinary) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[setMObjectBinary()]: ");
        }
        
        // validate the input parameters
        if(gatewayGUID==null || deviceGUID==null || mobjectId == null || mobjectBinary == null
        || gatewayGUID.trim().equals("") || deviceGUID.trim().equals("")
        || mobjectId.trim().equals("")) {
            throw new XanbooException(10050);
        }
        
        /* ENABLE BACKWARD COMPATIBILITY WITH THE NOTION THAT A GATEWAY'S DEVICE GUID IS THE SAME AS IT'S GATEWAY_GUID */
        deviceGUID = this.checkForGatewayGUID( gatewayGUID, deviceGUID );
        
        Connection conn=null;
        boolean rollback=false;
        
        long commandQueueId=-1;
              
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            
            /* for binary mobjects with >4K length, use content type as value in the DB command queue */
            /* for binary mobjects with <=4K length, store binary content converted to string as value in the DB command queue */
            //if(stringContent == null) {
            //    stringContent = (cType==null ? "null" : cType);
            //}
            String stringContent = null;
            String cType = mobjectBinary.getContentType();
            if(cType!=null && (cType.indexOf("text")!=-1 || cType.indexOf("json")!=-1)) {   //for text content, check 4K length limit for special logic!
                if(mobjectBinary.getBinaryContent().length <= 4000) {
                    try {
                        stringContent = new String(mobjectBinary.getBinaryContent(), "UTF-8");
                    }catch(Exception e) {
                        stringContent = null;
                    }
                }
            }
            
            String[] objectId = {mobjectId};        //oid to set in the DB
            String[] objectVal = { stringContent };
            String[] valContentType = { cType == null ? "null" : cType };
            
            commandQueueId = dao.setMObject(conn, xCaller.getAccountId(), xCaller.getUserId(), gatewayGUID, deviceGUID, objectId, objectVal, valContentType);

            if(commandQueueId >0) {
            
                //fave object binary to mobject_binary table
                dao.setMObjectBinary(conn, xCaller.getAccountId(), xCaller.getUserId(), gatewayGUID, deviceGUID, mobjectId, mobjectBinary.getBinaryContent());
                
                XanbooGateway gwyInfo = dao.getGatewayInfo(conn, xCaller.getAccountId(), gatewayGUID);
                if(gwyInfo!=null && gwyInfo.isTraceLogging()) {
                    TraceLogger.log(Long.toString(commandQueueId), Long.toString(xCaller.getAccountId()), gatewayGUID, deviceGUID, this.getClass().getSimpleName(), "setMobjectBinary", objectId[0], objectVal[0]);
                }
                pollGateway(gwyInfo);
            }
            
        }catch (XanbooException xe) {
            rollback=true;
            if(xe.getCode() != 26134) {  // do not fail, if no command was inserted, since there was already a command in the queue
                throw xe;
            }
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[setMObject()]: " + e.getMessage(), e);
            }else {
              logger.error("[setMObject()]: " + e.getMessage());
            }                                                
            rollback=true;
            throw new XanbooException(10030, "[setMObject]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
        if ( !mobjectId.equals("519")) return commandQueueId; //if the binary object Id is not 519 (rule manipulation OID), return
        
        //if the oid is 519 (rule modification OID)
        try
        {
            XanbooAdminPrincipal adminPrincipal = new XanbooAdminPrincipal();
            String jsonString = new String(mobjectBinary.getBinaryContent(), "UTF-8");
            logger.debug("[setMObjectBinary()] : calling ruleManager.updateRule with accountId="+xCaller.getAccountId()+", gguid="+gatewayGUID+", json="+jsonString);
            ruleManager.updateRule(adminPrincipal, xCaller.getAccountId(), xCaller.getUserId(), gatewayGUID,commandQueueId,null, jsonString);
        }
        catch(Exception ex)
        {
            if(logger.isDebugEnabled()) 
                logger.error("[setMObject()]: " + ex.getMessage(), ex);
            else 
                logger.error("[setMObject()]: " + ex.getMessage());     
            throw new XanbooException(10030, "[setMObject]:" + ex.getMessage());
        }
        return commandQueueId;
        
    }
    
    
    public String newDevice( XanbooPrincipal xCaller, String gatewayGUID, String catalogId, String label ) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[newDevice()]:");
        }
        
        // validate the input parameters
        if(gatewayGUID==null || gatewayGUID.length()==0 || catalogId==null || catalogId.length()<14) {
            throw new XanbooException(10050);
        }
         
        if(catalogId.length()==14) catalogId="0"+catalogId;
        
        //Only allow newDevice call for IP Cameras
        String vendorId = catalogId.substring( 1, 5 );
        String classId  = catalogId.substring( 5, 9 );
        
        //route to ServiceManager if this is a newDevice call for a service device
        if(XanbooUtil.isValidServiceId(vendorId)) {
            try {
               //   getServiceManagerEJB();
               // gatewayGUID = GlobalNames.DLLITE_GATEWAY_PREFIX+xCaller.getAccountId(); //method can only create service devices. 
                ServiceSubscription subs = new ServiceSubscription();
                subs.setAccountId(xCaller.getAccountId());
                subs.setServiceId(vendorId);    //vendorId is service Id
                subs.setSgGuid(gatewayGUID);
                subs.setGguid(gatewayGUID);
                ServiceObject sobj = sManager.newDevice(xCaller, gatewayGUID, catalogId, label, null);
                if(sobj!=null) {
                	return sobj.getDeviceGuid();
                }
            }catch(XanbooException xe) {
                throw xe;
            }catch(Exception e) {
                if(logger.isDebugEnabled()) {
                  logger.error("[newDevice()]" + e.getMessage(), e);
                }else {
                  logger.error("[newDevice()]" + e.getMessage());
                }
                throw new XanbooException(10050, "[newDevice]:" + e.getMessage());
            }
        }

        //native devices not allowed. FOR NOW!
        throw new XanbooException(10050, "newDevice NOT allowed for native devices.");
        
/*        
        Connection conn = null;
        boolean rollback = true;
        String deviceId = null;
        
        try {
            
            // first validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            deviceId = dao.newDevice( conn, xCaller, gatewayGUID, catalogId, label );
            rollback = false;
            
            XanbooGateway gwyInfo = dao.getGatewayInfo(conn, xCaller.getAccountId(), gatewayGUID);
            pollGateway(gwyInfo);
            
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[newDevice()]: " + e.getMessage(), e);
            }else {
                logger.error("[newDevice()]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[newDevice]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
        return deviceId;    
*/                
     }
     
     
    public HashMap<String,String> getCameraURL( XanbooPrincipal xCaller, String catalogId ) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getCameraURL()]:");
        }

        if(GlobalNames.CAMERA_STREAM_URLS.size() == 0) {
            Connection conn=null;
            try {
                // validate the caller and privileges
                //checkCallerPrivilege(xCaller);
                conn=dao.getConnection();
                XanbooResultSet xrs = dao.getCameraURL(conn);

                if(xrs!=null && xrs.size()>0) {
                    for(int i=0; i<xrs.size(); i++) {
                        //e.g.   "10030206000004-m3u8-0",  "/img/stream.m3u8"
                        String key = xrs.getElementString(i, "CATALOG_ID") + "-" + xrs.getElementString(i, "STREAM_TYPE") + "-" + xrs.getElementString(i, "BANDWIDTH");
                        GlobalNames.CAMERA_STREAM_URLS.put(key, xrs.getElementString(i, "STREAM_URL"));
                    }
                }

            }catch (XanbooException xe) {
                throw xe;
            }catch (Exception e) {
                if(logger.isDebugEnabled()) {
                logger.error("[getCameraURL()]" + e.getMessage(), e);
                }else {
                logger.error("[getCameraURL()]" + e.getMessage());
                }
                throw new XanbooException(10030, "[getCameraURL]:" + e.getMessage());
            }finally {
                dao.closeConnection(conn);
            }          
        }

        
        if(catalogId==null || catalogId.trim().length()==0) return GlobalNames.CAMERA_STREAM_URLS;
        catalogId = catalogId.trim();

        HashMap<String,String> urls = null;
        Set keys = GlobalNames.CAMERA_STREAM_URLS.keySet();
        
        for (Iterator it = keys.iterator(); it.hasNext();) {
            String key = (String) it.next();
            if(key.startsWith(catalogId)) {
                if(urls == null) urls = new HashMap<String,String>();
                urls.put(key, GlobalNames.CAMERA_STREAM_URLS.get(key));
            }
        }
        
        return urls;
    }
        
    /**
     * Returns Domain Ref by domainId     
     * @param xCaller XanbooPrincipal object with which to authenticate
     * @param domainId The domain id of the domain ref
     * @return A XanbooResultSet of system parameters.
     * @throws XanbooException
     */
    public XanbooResultSet getDomainRefByDomainId( XanbooPrincipal xCaller, String domainId ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getDomainRefByDomainId()]:");
        }
        
        Connection conn=null;
        try {
        	// validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
                       
            return dao.getDomainRefByDomainId(conn, xCaller, domainId);
            
            
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error("[getDomainRefByDomainId] Calling DAO.getDomainByDomainId failed", e);
            }else {
                logger.error("[getDomainRefByDomainId]: " + e.getMessage());
            }
            throw new XanbooException(10030, "Exception while executing DAO method");
        }finally {
            dao.closeConnection(conn);
        }
    }

    /**
     *
     * @param xCaller
     * @param languageId Language Id to query.  If null, default will be english
     * @param modelId Model Id to query.  If null, all device models will be returned
     * @return The device model list
     * @throws XanbooException
     */
    public XanbooResultSet getDeviceModel(XanbooPrincipal xCaller, String languageId, String modelId) throws XanbooException {
         if (logger.isDebugEnabled()) {
            logger.debug("[getDeviceModel()]:");
        }

        Connection conn=null;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();

            return dao.getDeviceModel(conn, languageId, modelId);

        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error("[getDeviceModel] Calling DAO.getDeviceModel failed", e);
            }else {
                logger.error("[getDeviceModel]: " + e.getMessage());
            }
            throw new XanbooException(10030, "Exception while executing DAO method");
        }finally {
            dao.closeConnection(conn);
        }
    }
   
    private XanbooResultSet getExtServiceMobjectResultSet(List<XanbooMobject> xsMobjects, XanbooResultSet dbMobjects) {
    	XanbooResultSet newMobjectRs = new XanbooResultSet();
    	if(xsMobjects != null && xsMobjects.size() > 0) {
			HashMap mObjectMap = null;
			for (int i = 0; dbMobjects != null && i < dbMobjects.size(); i++) {
				for(XanbooMobject xm : XanbooUtil.emptyIfNull(xsMobjects)) {
		    		if(xm.getOid().equals(dbMobjects.getElementString(i, "MOBJECT_ID"))) {
		    			mObjectMap = (HashMap) dbMobjects.get(i);
						mObjectMap.put("PENDING_VALUE", xm.getStringValue());
						mObjectMap.put("VALUE", xm.getStringValue());
						newMobjectRs.add(mObjectMap);
		    		}
		        }
	        }
		}
    	return newMobjectRs!=null && newMobjectRs.size() > 0 ? newMobjectRs : dbMobjects;
    }
}
