/*
 * $Source:  $
 * $Id:  $
 *
 * Copyright 2013 AT&T Digital Life
 *
 */

package com.xanboo.core.sdk.sysadmin;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import org.apache.commons.lang.StringUtils;

import com.xanboo.core.common.LicenseControl;
import com.xanboo.core.mbus.MBusSynchronizer;
import com.xanboo.core.mbus.domain.DLCoreMBusAccountChange;
import com.xanboo.core.mbus.domain.DLCoreMBusEntityChange;
import com.xanboo.core.mbus.domain.DLCoreMBusStateChange;
import com.xanboo.core.mbus.domain.DLCoreMBusSubscriptionChange;
import com.xanboo.core.model.XanbooGateway;
import com.xanboo.core.model.device.XanbooCatalog;
import com.xanboo.core.model.device.XanbooDevice;
import com.xanboo.core.model.device.XanbooDeviceCatalog;
import com.xanboo.core.model.device.XanbooEventCatalog;
import com.xanboo.core.model.device.XanbooMobjectCatalog;
import com.xanboo.core.sdk.AbstractSDKManagerEJB;
import com.xanboo.core.sdk.account.DlaAuthServiceClient;
import com.xanboo.core.sdk.account.SubscriptionFeature;
import com.xanboo.core.sdk.account.XanbooAccount;
import com.xanboo.core.sdk.account.XanbooNotificationProfile;
import com.xanboo.core.sdk.account.XanbooSubscription;
import com.xanboo.core.sdk.contact.XanbooContact;
import com.xanboo.core.sdk.services.ServiceManager;
import com.xanboo.core.sdk.services.model.ServiceSubscription;
import com.xanboo.core.sdk.util.SubscriptionMonitorStatus;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooAdminPrincipal;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.EServicePusher;
import com.xanboo.core.util.GlobalNames;
import com.xanboo.core.util.RMSClient;
import com.xanboo.core.util.SBNSynchronizer;
import com.xanboo.core.util.SimpleACSIClient;
import com.xanboo.core.util.XanbooException;
import com.xanboo.core.util.XanbooUtil;
import com.xanboo.core.util.fs.AbstractFSProvider;
import com.xanboo.core.util.fs.XanbooFSProviderProxy;

/**
 * Session Bean implementation of <code>SysAdminManager</code>.
 * <p>
 * This bean acts as a wrapper class for all Xanboo system administration related Core SDK methods.
 * </p>
*/
@Remote (SysAdminManager.class)
@Stateless (name="SysAdminManager")
@TransactionManagement( TransactionManagementType.BEAN )
public class SysAdminManagerEJB  extends AbstractSDKManagerEJB {

    // related DAO class
    private SysAdminManagerDAO dao=null;

   
    /** Status ID for active devices  */
    public static final int DEVICE_STATUS_ACTIVE=2;
    
    /** Status ID for inactive device*/
    public static final int DEVICE_STATUS_INACTIVE=3;
    
    /** Market Area argument for RMS Call*/
    public static final String RMS_MARKET_AREA="Market_Area";
    
    /** Market Area column name*/
    public static final String SUBFLAGS_COL_NAME="SUBS_FLAGS";       
    public static final String MARKET_AREA_COL_NAME="BMARKET";       
    
    private HashMap<String,HashMap> domainTable = null; 
    private long   lastCacheUpdate=0;    
    
    long tmplReloadInterval = 60*24*10*60*1000; //default 10 days
    private static XanbooResultSet domainTemplates = null;
    private static long domainTmplLastTm = 0l;
    private static boolean resetTemplateCache = false;
    
    private long providerReloadInterval = 60*24*10*60*1000; //default 10 days
    private static XanbooResultSet providerRef = null;
    private static long providerLastTm = 0l;
    private static boolean resetProviderCache = false;
    
    private com.xanboo.core.extservices.outbound.ServiceOutboundProxy soProxy = null;
    
    // Subscription Features DLDP 4207
    private Map<String,  List<SubscriptionFeature>> domainSubscriptions = null;
    private long   lastDomainFeatureCacheUpdate=0;
    
    @PostConstruct
    public void init() throws Exception 
    {
        dao = new SysAdminManagerDAO(); 
        soProxy = (com.xanboo.core.extservices.outbound.ServiceOutboundProxy)super.getEJB(GlobalNames.EJB_OUTBOUND_PROXY);
        
       
            tmplReloadInterval = GlobalNames.NOTIF_TEMPL_RELOAD_INTERVAL * 60 * 1000;
      
            
            providerReloadInterval = GlobalNames.NOTIF_PROVIDER_RELD_INTERVAL * 60 * 1000;
       
    }


    //--------------- Business methods ------------------------------------------------------

    /**
     * Checks if a given caller principal is valid or not.
     * @param xCaller A XanbooAdminPrincipal object that should contain valid information about an authenticated user
     *
     * @throws XanbooException if xCaller doesn't contain valid information
     */
    private static void checkCallerPrivilege(XanbooAdminPrincipal xCaller) throws XanbooException {
        //Username must not be null, role must be greather than zero, level must be zero or greater
        if(xCaller==null || xCaller.getUsername()==null || xCaller.getRole()<1 || xCaller.getLevel()<0) {
            throw new XanbooException(10005);   // invalid caller
        }
    }
    
    public XanbooResultSet getAccountList(XanbooAdminPrincipal xCaller, String username, int startRow, int numRows) throws XanbooException{
        if (logger.isDebugEnabled()) {
            logger.debug("[getAccountList()]:");
        }
        
        if(startRow<0 || numRows<-1) {
            throw new XanbooException(10050);
        }

        Connection conn=null;
        try {
            // validate the caller and privileges
            checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            return dao.getAccount(conn, xCaller, -1, username, startRow, numRows);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getAccountList()]" + e.getMessage(), e);
            }else {
              logger.error("[getAccountList()]" + e.getMessage());
            }
            throw new XanbooException(10030, "[getAccountList]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }          
        
    }    

    public XanbooResultSet getAccount(XanbooAdminPrincipal xCaller, String extAccountId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getAccount()]:");
        }
        
        if(extAccountId == null || extAccountId.length()==0) {
            throw new XanbooException(10050);
        }

        Connection conn=null;
        try {
            // validate the caller and privileges
            checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            return dao.getAccount(conn, xCaller, 0, extAccountId, 0, -1); // -1 account id indicates query by external account id
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getAccount()]" + e.getMessage(), e);
            }else {
              logger.error("[getAccount()]" + e.getMessage());
            }            
            throw new XanbooException(10030, "[getAccount]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }          
    }    
   
    public XanbooResultSet getAccount(XanbooAdminPrincipal xCaller, long accountId) throws XanbooException{
        if (logger.isDebugEnabled()) {
            logger.debug("[getAccount()]:");
        }
        
        if(accountId<=0) {
            throw new XanbooException(10050);
        }

        Connection conn=null;
        try {
            // validate the caller and privileges
            checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            return dao.getAccount(conn, xCaller, accountId, null, 0, -1);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getAccount()]" + e.getMessage(), e);
            }else {
              logger.error("[getAccount()]" + e.getMessage());
            }            
            throw new XanbooException(10030, "[getAccount]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }          
    }    
    

   
    public void updateAccount(XanbooAdminPrincipal xCaller, long accountId, int status, String regToken) throws XanbooException {
        XanbooAccount acc = new XanbooAccount();
        acc.setAccountId(accountId);
        acc.setStatus(status);
        acc.setToken(regToken);
        acc.setExtAccountId(null);  //no update
        acc.setFifoPurgingFlag(-1); //no update

        updateAccount(xCaller, acc);    
    }


    public void updateAccount(XanbooAdminPrincipal xCaller, XanbooAccount xAccount) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateAccount()]:");
        }
        
        String domainId = null;
     
        boolean doMBusSync = true; //MBus 

        if(xAccount==null || xAccount.getAccountId()<0) {
            throw new XanbooException(10050);
        }

        if(xAccount.getStatus()!=XanbooAccount.STATUS_INACTIVE && xAccount.getStatus()!=XanbooAccount.STATUS_ACTIVE &&
             xAccount.getStatus()!=XanbooAccount.STATUS_DISABLED && xAccount.getStatus()!=XanbooAccount.STATUS_CANCELLED &&
                 xAccount.getStatus()!=XanbooAccount.STATUS_UNCHANGED) {
            throw new XanbooException(10050);
        }

        Connection conn=null;
        boolean rollback=false;
        try {
            // validate the caller and privileges
            checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            domainId = dao.updateAccount(conn, xCaller, xAccount);
            if(xAccount.getStatus()!=XanbooAccount.STATUS_UNCHANGED) {
                logger.info("[updateAccount()]: STATUS CHANGED account:" + xAccount.getAccountId() + "@" + domainId + ", status:" + xAccount.getStatus());
            }
        }catch (XanbooException xe) {
            rollback=true;
            doMBusSync = false; // don't send message in case of exception
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[updateAccount()]" + e.getMessage(), e);
            }else {
              logger.error("[updateAccount()]" + e.getMessage());
            }
            rollback=true;
            doMBusSync = false; // don't send message in case of exception
            throw new XanbooException(10030, "[updateAccount]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
             //MBus - Send account.update message
            if(GlobalNames.MBUS_SYNC_ENABLED && doMBusSync){
            	
            	String	userId =  xAccount.getUser() != null ? xAccount.getUser().getUserId() +"" : xCaller.getUsername() ; 	
            	if(logger.isDebugEnabled()) {
    		 
    	
    		 
            		StringBuffer sb = new StringBuffer("[MBus : publish messages : SysAdminManagerEJB.updateAccount()]:");
            		sb.append("\n Domain : " + domainId );
            		sb.append("\n AccountId : " + xAccount.getAccountId() );
            		sb.append("\n ExtAccountId : " + xAccount.getExtAccountId() );
            		sb.append("\n Status : " + xAccount.getStatus() );
            		sb.append("\n UserId : " +userId );
    		
    		 
            		logger.debug( sb.toString() );
           }
                   MBusSynchronizer.updateAccount(domainId, xAccount.getAccountId(), xAccount.getExtAccountId(), xAccount.getStatus(), 
                		   userId , "SysAdminManagerEJB.updateAccount");
           
                    }		
            		
            if (GlobalNames.DLA_AUTHENTICATE_VIA_SERVICE && !rollback) {
            	invalidateDlaAuthServiceCache(xAccount.getAccountId());
            }
        }
    }

    

    public XanbooResultSet getUserList(XanbooAdminPrincipal xCaller, long accountId) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[getUserList()]:");
        }
        return getUserList(xCaller, accountId, 0, -1);
        
    }
    
    

    public XanbooResultSet getUserList(XanbooAdminPrincipal xCaller, long accountId, int startRow, int numRows) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[getUserList()]:");
        }

        if(startRow<0 || numRows<-1 || accountId<-1) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;       
        try {
            // validate the caller and privileges
            checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            return dao.getUserList(conn, xCaller, accountId, startRow, numRows);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getUserList()]" + e.getMessage(), e);
            }else {
              logger.error("[getUserList()]" + e.getMessage());
            }
            throw new XanbooException(10030, "[getUserList]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
    }
    

    public void updateDeviceStatus(XanbooAdminPrincipal xCaller, long accountId, String gatewayGUID, String deviceGUID,
                    int status ) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[updateDeviceStatus()]: ");
        }

        // validate the input parameters
        try {
            if(accountId<0 || gatewayGUID==null || deviceGUID==null
            || gatewayGUID.trim().equals("") || deviceGUID.trim().equals("")
            || ( status != DEVICE_STATUS_ACTIVE && status != DEVICE_STATUS_INACTIVE ) ) {
                throw new XanbooException(10050);
            }
        } catch (Exception e) {
            throw new XanbooException(10050);
        }
            
        
        /* CODE TO ENABLE BACKWARD COMPATIBILITY WITH THE NOTION THAT A GATEWAY'S DEVICE GUID IS THE SAME AS IT'S GATEWAY_GUID */
        if (deviceGUID != null && gatewayGUID != null && deviceGUID.equals(gatewayGUID)) {
            deviceGUID = "0";
        }
        /* END OF BACKWARD COMPATIBILITY CODE */

        Connection conn=null;
        boolean rollback=false;
        
        try {
            // validate the caller and privileges
            checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            dao.updateDeviceStatus(conn, xCaller, accountId, gatewayGUID, deviceGUID, status);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[updateDeviceStatus()]" + e.getMessage(), e);
            }else {
              logger.error("[updateDeviceStatus()]" + e.getMessage());
            }            
            rollback=true;
            throw new XanbooException(10030,  "[updateDeviceStatus]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
    }
    
    public void updateDevice(XanbooAdminPrincipal xCaller, long accountId, XanbooDevice xanbooDevice)
            throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[updateDevice()]: ");
        }

        // validate the input parameters
        try {
            if( accountId < 0 || xanbooDevice == null || !xanbooDevice.validForUpdate() ) {
                throw new XanbooException(10050);
            }
        } catch (Exception e) {
            throw new XanbooException(10050);
        }


        /* CODE TO ENABLE BACKWARD COMPATIBILITY WITH THE NOTION THAT A GATEWAY'S DEVICE GUID IS THE SAME AS IT'S GATEWAY_GUID */
        if (xanbooDevice.getDeviceGUID() != null && xanbooDevice.getGatewayGUID() != null
        &&  xanbooDevice.getDeviceGUID().equals(xanbooDevice.getGatewayGUID())) {
            xanbooDevice.setDeviceGUID("0");
        }
        /* END OF BACKWARD COMPATIBILITY CODE */

        Connection conn=null;
        boolean rollback=false;
        boolean doMBusSync = true; //MBus 

        try {
            // validate the caller and privileges
            checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            dao.updateDevice(conn, xCaller, accountId, xanbooDevice);
        }catch (XanbooException xe) {
            rollback=true;
            doMBusSync = false;
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[updateDevice()]" + e.getMessage(), e);
            }else {
              logger.error("[updateDevice()]" + e.getMessage());
            }
            rollback=true;
            doMBusSync = false;
            throw new XanbooException(10030,  "[updateDevice]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        //sending oracle CSI notification when self istalled via DLD
        if(GlobalNames.MODULE_CSI_ENABLED && (xanbooDevice.getInstallerId() != null && xanbooDevice.getInstallerId().endsWith(",C"))) {
            SimpleACSIClient acsi = new SimpleACSIClient();
            acsi.newDeviceRegistered(xanbooDevice.getExtAccId(),
            		xanbooDevice.getSubId(), 
            		xanbooDevice.getGatewayGUID(),
            		xanbooDevice.getDeviceGUID(),
            		xanbooDevice.getCatalogId(),
                    null,
                    xanbooDevice.getInstallerId(),
                    xanbooDevice.getHwId(),
                    xanbooDevice.getHwId(), xanbooDevice.getSourceId() );
        }
        
        if(GlobalNames.MBUS_SYNC_ENABLED && doMBusSync){
        	
	        DLCoreMBusEntityChange data = new DLCoreMBusEntityChange();
	        
	        data.setCatId(xanbooDevice.getCatalogId());
	        data.setDeviceGUID(xanbooDevice.getDeviceGUID());
	        data.setGatewayGUID(xanbooDevice.getGatewayGUID());
	        
	        if(accountId != 0 )	data.setAccId(accountId);
	        if(xanbooDevice.getDomain() != null )	data.setDomain(xanbooDevice.getDomain() );
	        if(xanbooDevice.getExtAccId() != null )	data.setExtAccId(xanbooDevice.getExtAccId() );	        
	        if(xanbooDevice.getHwId() != null )	data.setHwId(xanbooDevice.getHwId() );
	        if(xanbooDevice.getInstallerId() != null )	data.setInsId(xanbooDevice.getInstallerId() );
        	
	        data.setSrcAppId("SysAdminManager.UpdateDevice");
	        if(xCaller.getUsername() != null) data.setSrcOriginatorId(xCaller.getUsername());	        
	        if(xanbooDevice.getSubId() != null )	data.setSubsId(xanbooDevice.getSubId() );        	
	        if(xanbooDevice.getSourceId() != null )	data.setSourceId(xanbooDevice.getSourceId() );        	
        	String 	date = XanbooUtil.getGMTDateTime(null);
        
        	data.setCreationDate(date);
        	data.setTs(date);
	
	        if(logger.isDebugEnabled()) {
	            StringBuffer sb = new StringBuffer("[MBus : publish messages : SysAdminManagerEJB.updateDevice :");
	            sb.append((data.toString()));    
	            logger.debug(sb.toString());             		
	        }
		
	        MBusSynchronizer.updateDevice(data);
        }
    }
     
    public XanbooResultSet getDeviceClassList(String lang) throws XanbooException {
        // deprecating the authenticated version
        return getDeviceClassList( null, lang );
    }

    // This method is deprecated - eventually move code into unauthenticated method.
    public XanbooResultSet getDeviceClassList(XanbooAdminPrincipal xCaller, String lang) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getDeviceClassList()]:");
        }

        if(lang==null || lang.trim().equals("")) lang="en";
        
        Connection conn=null;
        try {
            // first validate the caller and privileges
            if ( xCaller != null ) checkCallerPrivilege(xCaller);

            conn=dao.getConnection();
            return dao.getDeviceClassList(conn, xCaller, lang);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getDeviceClassList()]" + e.getMessage(), e);
            }else {
              logger.error("[getDeviceClassList()]" + e.getMessage());
            }            
            throw new XanbooException(10030, "[getDeviceClassList]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
    }
    
    

    public XanbooResultSet getDeviceListByClass(XanbooAdminPrincipal xCaller, String dClass) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[getDeviceListByClass()]:");
        }
        return getDeviceListByClass(xCaller, dClass, 0, -1);
    }
    

    

    public XanbooResultSet getDeviceListByClass(XanbooAdminPrincipal xCaller, String dClass, int startRow, int numRows) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[getDeviceListByClass()]:");
        }
        
        if(startRow<0 || numRows<-1) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        try {
            // first validate the caller and privileges
            checkCallerPrivilege(xCaller);

            conn=dao.getConnection();
            return dao.getDeviceListByClass(conn, xCaller, dClass, startRow, numRows);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getDeviceListByClass()]" + e.getMessage(), e);
            }else {
              logger.error("[getDeviceListByClass()]" + e.getMessage());
            }
            throw new XanbooException(10030, "[getDeviceListByClass]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
    }
    
    public XanbooResultSet getGateway(XanbooAdminPrincipal xCaller, String gatewayGuid) throws XanbooException{
        if (logger.isDebugEnabled()) {
            logger.debug("[getGateway()]:");
        }
        Connection conn=null;
        try {
            // first validate the caller and privileges
            checkCallerPrivilege(xCaller);

            conn=dao.getConnection();
            return dao.getGateway(conn, xCaller, null, null, null, gatewayGuid);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getGateway()]" + e.getMessage(), e);
            }else {
              logger.error("[getGateway()]" + e.getMessage());
            }
            throw new XanbooException(10030, "[getGateway]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        } 
    }    

    public XanbooResultSet getGateway(XanbooAdminPrincipal xCaller, String subsId, String imei) throws XanbooException{
        return getGateway(xCaller, subsId, imei, null);   
    }  

      
    public XanbooResultSet getGateway(XanbooAdminPrincipal xCaller, String subsId, String imei, String serialNo) throws XanbooException{
        if (logger.isDebugEnabled()) {
            logger.debug("[getGateway()]:");
        }
        Connection conn=null;
        try {
            // first validate the caller and privileges
            checkCallerPrivilege(xCaller);

            conn=dao.getConnection();
            return dao.getGateway(conn, xCaller, subsId, imei, serialNo, null);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getGateway()]" + e.getMessage(), e);
            }else {
              logger.error("[getGateway()]" + e.getMessage());
            }
            throw new XanbooException(10030, "[getGateway]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }  
        
    }    
    public long newBroadcastMessage(XanbooAdminPrincipal xCaller, String message, String lang, long[] accountId) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[newBroadcastMessage()]:");
        }

        if(message==null || message.trim().equals("")) {
            throw new XanbooException(10050);
        }
        
        // todo: default language get as an environment
        if(lang==null || lang.trim().equals("")) lang="en";

        if(accountId != null) {
            // validate the input parameters
            for(int i=0; i<accountId.length; i++) {
                if(accountId[i]<0) {
                    throw new XanbooException(10050);
                }
            }
        }        
        
        Connection conn=null;
        boolean rollback=false;
        try {
            // validate the caller and privileges
            checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            return dao.newBroadcastMessage(conn, xCaller, message, lang, accountId);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[newBroadcastMessage]:" + e.getMessage(), e);
            }else {
              logger.error("[newBroadcastMessage]:" + e.getMessage());
            }
            throw new XanbooException(10030, "[newBroadcastMessage]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
    }
    
    

    public void deleteBroadcastMessage(XanbooAdminPrincipal xCaller, long[] messageId) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteBroadcastMessage()]:");
        }
        
        if(messageId==null) {
            throw new XanbooException(10050);
        }
        
        for(int i=0;i<messageId.length;i++) {
            if(messageId[i]<0) throw new XanbooException(10050);
        }

        
        Connection conn=null;
        boolean rollback=false;
        try {
            // validate the caller and privileges
            checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            dao.deleteBroadcastMessage(conn, xCaller, messageId);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[deleteBroadcastMessage]:" + e.getMessage(), e);
            }else {
              logger.error("[deleteBroadcastMessage]:" + e.getMessage());
            }           
            throw new XanbooException(10030, "[deleteBroadcastMessage]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
    }
    
    


    public XanbooResultSet getMObjectHistoryTable(XanbooAdminPrincipal xCaller) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[getMObjectHistoryTable()]:");
        }
        
        Connection conn=null;
        try {
            // first validate the caller and privileges
            checkCallerPrivilege(xCaller);

            conn=dao.getConnection();
            return dao.getMObjectHistoryTable(conn, xCaller);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getMObjectHistoryTable]:" + e.getMessage(), e);
            }else {
              logger.error("[getMObjectHistoryTable]:" + e.getMessage());
            }                       
            throw new XanbooException(10030, "[getMObjectHistoryTable]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
    }
    
    

    public void enableMObjectHistory(XanbooAdminPrincipal xCaller, String catalogId, String[] mobjectId) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[enableMObjectHistory()]:");
        }
        
        try {
            if(catalogId==null || catalogId.trim().equals("") || mobjectId==null ) {
                throw new XanbooException(10050);
            } else {
                //Catalog IDs in the system have a prepended zero. To avoid user confusion, we will automatically prepend this zero if the length of the
                //supplied catalog ID is 14 characters. This way, we can also accept catalogIds as they appear in the descriptor.
                catalogId = (catalogId.length() == 14) ? "0" + catalogId : catalogId ;
            }
            
            for(int i=0;i<mobjectId.length;i++) {
                if(mobjectId[i]==null || mobjectId[i].trim().equals("")) throw new XanbooException(10050);
            }
            
            
        } catch ( Exception e ) {
            throw new XanbooException(10050);
        }
            
        
        
        Connection conn=null;
        boolean rollback=false;
        try {
            // validate the caller and privileges
            checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            dao.enableMObjectHistory(conn, xCaller, catalogId, mobjectId);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[enableMObjectHistory]:" + e.getMessage(), e);
            }else {
              logger.error("[enableMObjectHistory]:" + e.getMessage());
            }             
            rollback=true;
            throw new XanbooException(10030, "[enableMObjectHistory]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
    }
    
    

    public void disableMObjectHistory(XanbooAdminPrincipal xCaller, String catalogId, String[] mobjectId) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[disableMObjectHistory()]:");
        }

        try {
            if(catalogId==null || catalogId.trim().equals("") || mobjectId==null) {
                throw new XanbooException(10050);
            }

            for(int i=0;i<mobjectId.length;i++) {
                if(mobjectId[i]==null || mobjectId[i].trim().equals("")) throw new XanbooException(10050);
            }
        } catch ( Exception e ) {
            throw new XanbooException(10050);
        }
                
        Connection conn=null;
        boolean rollback=false;
        
        try {
            // validate the caller and privileges
            checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            dao.disableMObjectHistory(conn, xCaller, catalogId, mobjectId);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[disableMObjectHistory]:" + e.getMessage(), e);
            }else {
              logger.error("[disableMObjectHistory]:" + e.getMessage());
            }             
            throw new XanbooException(10030, "[disableMObjectHistory]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
    }

    

    public void deleteMObjectHistory(XanbooAdminPrincipal xCaller, String catalogId, String mobjectId, Date fromDate, Date toDate) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteMObjectHistory()]: ");
        }
    
        Connection conn=null;
        boolean rollback=false;

        try {
            // validate the caller and privileges
            checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            dao.deleteMObjectHistory(conn, xCaller, catalogId, mobjectId, fromDate, toDate);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[deleteMObjectHistory]:" + e.getMessage(), e);
            }else {
              logger.error("[deleteMObjectHistory]:" + e.getMessage());
            }                         
            rollback=true;
            throw new XanbooException(10030, "[deleteMObjectHistory]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
    
    }    
    
    

    public XanbooResultSet getMObjectHistory(XanbooAdminPrincipal xCaller, String catalogId, String mobjectId, Date fromDate, Date toDate) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getMObjectHistory()]: ");
        }

        Connection conn=null;
        try {
            // validate the caller and privileges
            checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            return dao.getMObjectHistory(conn, xCaller, catalogId, mobjectId, fromDate, toDate);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getMObjectHistory]:" + e.getMessage(), e);
            }else {
              logger.error("[getMObjectHistory]:" + e.getMessage());
            }                  
            throw new XanbooException(10030, "[getMObjectHistory]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
    
    }

    

    public XanbooResultSet getSystemParam(XanbooAdminPrincipal xCaller, String param) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getSystemParam()]:");
        }
        
        Connection conn=null;
        try {
            conn=dao.getConnection();
            return dao.getSystemParam(conn, xCaller, param);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getSystemParam]:" + e.getMessage(), e);
            }else {
              logger.error("[getSystemParam]:" + e.getMessage());
            }             
            throw new XanbooException(10030, "[getSystemParam]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
    }
    


    public XanbooPrincipal getXanbooPrincipal(XanbooAdminPrincipal xCaller, String domainId, String extUserId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getXanbooPrincipal()]:");
        }
        
        // first validate the input parameters
        try {
            if(domainId==null || extUserId==null || domainId.trim().equals("") || extUserId.trim().equals("") ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        // now check the validity of the user
        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();
            return dao.getXanbooPrincipal(conn, xCaller, domainId, extUserId);
        }catch (XanbooException xe) {
            rollback=true;            
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[getXanbooPrincipal]:" + e.getMessage(), e);
            }else {
              logger.error("[getXanbooPrincipal]:" + e.getMessage());
            }                         
            throw new XanbooException(10030);
        }finally {
            dao.closeConnection(conn, rollback);
        }
    }    
    


    public void deleteDevice(XanbooAdminPrincipal xCaller, long accountId, String gatewayGUID, String deviceGUID) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteDevice()]: ");
        }

        // validate the input parameters
        if(gatewayGUID==null || gatewayGUID.trim().length()==0 || (deviceGUID!=null && deviceGUID.trim().length()==0)) {
            throw new XanbooException(10050);
        }
        
        /* CODE TO ENABLE BACKWARD COMPATIBILITY WITH THE NOTION THAT A GATEWAY'S DEVICE GUID IS THE SAME AS IT'S GATEWAY_GUID */
        if (deviceGUID != null && gatewayGUID != null && deviceGUID.equals(gatewayGUID)) {
            deviceGUID = "0";
        }
        /* END OF BACKWARD COMPATIBILITY CODE */
        
        // route to ServiceManager if this is a deleteDevice call for a service device
        if(XanbooUtil.isExternalServiceDevice(deviceGUID)) {
        	try {
				Object obj = getEJB(GlobalNames.EJB_EXTSERVICE_MANAGER);
				if(obj!=null && obj instanceof ServiceManager) {
					ServiceManager ejb = (ServiceManager)obj;
					ejb.deleteDevice(xCaller.getAccountPrincipal(), gatewayGUID, deviceGUID);
		            return;
				}else {
					 throw new XanbooException(60002, "[deleteDevice]: Unable to lookup" + GlobalNames.EJB_EXTSERVICE_MANAGER);
				}
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
        boolean rollback = false;
        try {
            // validate the caller and privileges
            checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            catId = dao.deleteDevice(conn, xCaller, accountId, gatewayGUID, deviceGUID);
            
            //sync with ACSI, if enabled
            //if NOT a zwave device (end-devices only!)
            if(!deviceGUID.equals("0") && catId!=null && !catId.substring(0, 4).equals("1005")) {
                if(GlobalNames.MODULE_CSI_ENABLED) {
                    XanbooResultSet xrs = dao.getSubscription(conn, xCaller, accountId, null, null);
                    for(int i=0; xrs!=null && i<xrs.size(); i++) {
                        if(xrs.getElementString(i, "GATEWAY_GUID").equalsIgnoreCase(gatewayGUID)) {
                            SimpleACSIClient acsi = new SimpleACSIClient();
                            
                            subsId = xrs.getElementString(0, "SUBS_ID");//DF#21450
                            
                            int rc = acsi.deviceRemoved(xrs.getElementString(0, ""), 
                            						subsId , 
                                                    gatewayGUID, 
                                                    deviceGUID,
                                                    null );
                            break;
                        }
                    }
                }
            }
            
            String mobjectDir = AbstractFSProvider.getBaseDir(null, xCaller.getDomain(), accountId, AbstractFSProvider.DIR_ACCOUNT_MOBJECT);
            
            //If deleting a gateway, remove it's MOBJECT directories
            if(deviceGUID.equals("0")) {
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

            }else {     // it is an end-device, send SIP to the gwy
                //If anything goes wrong here, we don't want to roll back, so use a separate try block.
                try {
                    try {
                        XanbooGateway gwyInfo = dao.getGatewayInfo(conn, accountId, gatewayGUID);
                        pollGateway(gwyInfo); 
                    }catch(Exception ee) {} //ignore sip polling exceptions

                } catch (Exception e) {
                    if (logger.isDebugEnabled()) {
                        logger.error("[deleteDevice]:" + e.getMessage(), e);
                    } else {
                        logger.error("[deleteDevice]:" + e.getMessage() );
                    }
                }
                    
            }
            
        }catch (XanbooException xe) {
            rollback = true;
            throw xe;
        }catch (Exception e) {
            rollback = true;
            if(logger.isDebugEnabled()) {
              logger.error("[deleteDevice]:" + e.getMessage(), e);
            }else {
              logger.error("[deleteDevice]:" + e.getMessage());
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
                EServicePusher.post2EntityListener(accountId, gatewayGUID, deviceGUID, null, null, null, true);
            }
        }
        
        //DF#21450
        //sync with MBUS, if enabled -  start            
        if(GlobalNames.MBUS_SYNC_ENABLED){
        	
        	String date = XanbooUtil.getCurrentGMTDateTime();
			

        	if(logger.isDebugEnabled()) {
                StringBuffer sb = new StringBuffer("[MBus : publish messages : DeviceManagerEJB.deleteDevice()]:");
                sb.append("DomainId : " + xCaller.getDomain());
                sb.append("\n AccountId : " +accountId);
                sb.append("\n catId : " + catId  );
                sb.append("\n ExtAccountId : " + null );
                sb.append("\n SubId : " + subsId );
                sb.append("\n deviceGUID : " + deviceGUID);
                sb.append("\n GatewayGUID : " + gatewayGUID);
                sb.append("\n TS : " + date );
                sb.append("\n UserName  : " + xCaller.getUsername()   );

                logger.debug(sb.toString());            		
        	}
        	
        
        	
        	DLCoreMBusEntityChange data = new DLCoreMBusEntityChange();

			data.setDomain(xCaller.getDomain());

			data.setAccId(accountId);

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
			data.setSrcOriginatorId(xCaller.getUsername() );
			// data.setStatus(-1);

			data.setSw(null);

			
			data.setTs(date);

			MBusSynchronizer.deleteDevice(data);
			
			
  
        }//MBus -  end
        
//DF#21450
                
        
    }    
    
 

    public void deleteAccount(XanbooAdminPrincipal xCaller, long accountId) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteAccount()]:");
        }
        boolean doMBusSync = true;//MBus
        if(accountId <= 0) {
            throw new XanbooException(10050);
        }
        String domainId = null;
        Connection conn=null;
        boolean rollback=false;
        try {
            // validate the caller and privileges
            checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            
            XanbooResultSet account = getAccount(xCaller, accountId);
            domainId = account.getElementString(0, "DOMAIN_ID");
            //First, do the deletion on the database side of things.
            dao.deleteAccount(conn, xCaller, accountId);
            
        }catch (XanbooException xe) {
            rollback=true;
            doMBusSync = false; // don't send message in case of exception
            throw xe;
        }catch (Exception e) {
            rollback=true;
            doMBusSync = false; // don't send message in case of exception
            if(logger.isDebugEnabled()) {
              logger.error("[deleteAccount]:" + e.getMessage(), e);
            }else {
              logger.error("[deleteAccount]:" + e.getMessage());
                }                         
            throw new XanbooException(10030, "[deleteAccount]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
          //MBus - Send account.delete message
            if(GlobalNames.MBUS_SYNC_ENABLED && doMBusSync){
            	
            	if(logger.isDebugEnabled()) {
            		 
            		 StringBuffer sb = new StringBuffer("[MBus : publish messages : SysAdminManagerEJB.deleteAccount()]:");
            		 sb.append("\n Domain : " + domainId );
            		 sb.append("\n AccountId : " + accountId );
            		 sb.append("\n ExtAccountId : " + xCaller.getUsername() );
            		 sb.append("\n UserId : " + xCaller.getUsername() );
            		            		 
                     logger.debug( sb.toString() );
                   }
            	
            	MBusSynchronizer.deleteAccount(domainId, accountId, xCaller.getUsername(), xCaller.getUsername(),  "SysAdminManagerEJB.deleteAccount");
            	
            }
        }
    }
    

    public XanbooResultSet getDeviceDescriptor( XanbooAdminPrincipal xCaller, String catalogId ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getDeviceDescriptor()]:");
        }
        
        Connection conn=null;
        try {
            // validate the caller and privileges
            checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            return dao.getDeviceDescriptor(conn, catalogId );
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getDeviceDescriptor]:" + e.getMessage(), e);
            }else {
              logger.error("[getDeviceDescriptor]:" + e.getMessage());
            }                         
            throw new XanbooException(10030, "[getDeviceDescriptor]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
    }

     
    
    public XanbooResultSet getDeviceEventLog(XanbooAdminPrincipal xCaller, long accountId, String gatewayGUID, String deviceGUID, String eventId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getDeviceEventLog()]:");
        }
        
        Connection conn=null;
        try {
            // validate the caller and privileges
            checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            return dao.getDeviceEventLog(conn,  xCaller, accountId, gatewayGUID, deviceGUID, eventId );
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getDeviceEventLog]:" + e.getMessage(), e);
            }else {
              logger.error("[getDeviceEventLog]:" + e.getMessage());
            }                                     
            throw new XanbooException(10030, "[getDeviceEventLog]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }    
    }
    
       
    public void clearDeviceEventLog(XanbooAdminPrincipal xCaller, long accountId, String gatewayGUID, String deviceGUID, String eventId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[clearDeviceEventLog()]: ");
        }
    
        Connection conn=null;
        boolean rollback=false;

        try {
            // validate the caller and privileges
            checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            dao.clearDeviceEventLog(conn, xCaller, accountId, gatewayGUID, deviceGUID, eventId);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[clearDeviceEventLog]:" + e.getMessage(), e);
            }else {
              logger.error("[clearDeviceEventLog]:" + e.getMessage());
            }               
            throw new XanbooException(10030, "[clearDeviceEventLog]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
    }
    
    public XanbooAdminPrincipal authenticateAdmin(String adminId, String password) throws XanbooException {
        
    	if (logger.isDebugEnabled()) {
            logger.debug("[authenticateAdmin()]:");
        }
    	XanbooAdminPrincipal xap = null;
        // first validate the input parameters
        try {
            if(adminId==null || password==null || adminId.trim().equals("") || password.trim().equals("")) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        // now check the validity of the admin user
        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();            
            xap = dao.authenticateAdmin(conn, adminId, password);
            return xap;
        }catch (XanbooException xe) {
        	 logger.error("[authenticateAdmin()]: Exception:" + xe.getMessage(), xe);
            rollback=true;
            throw xe;
        }catch (Exception e) {        	
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[authenticateAdmin()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[authenticateAdmin()]: Exception:" + e.getMessage());
            }
            throw new XanbooException(10030, "[authenticateAdmin()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        } 
    }
    
        public XanbooAdminPrincipal authenticateAdmin(String adminId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[authenticateAdmin()]:");
        }
        
        // first validate the input parameters
        try {
            if(adminId==null  || adminId.trim().equals("") ) {            	
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        XanbooAdminPrincipal xap = null;
        // now check the validity of the admin user
        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();
            xap = dao.authenticateAdmin(conn, adminId);
            return xap;            
        }catch (XanbooException xe) {
        	logger.error("[authenticateAdmin()]: Exception:" + xe.getMessage(), xe);
            rollback=true;
            throw xe;
        }catch (Exception e) {
        	
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[authenticateAdmin()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[authenticateAdmin()]: Exception:" + e.getMessage());
            }
            throw new XanbooException(10030, "[authenticateAdmin()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        } 
    }
    public void updateAdmin(XanbooAdminPrincipal xCaller, String adminId, String password) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[updateAdmin()]: ");
        }

        Connection conn=null;
        boolean rollback=false;

        try {
            checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            dao.updateAdmin(conn, xCaller, adminId, password);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[updateAdmin()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[updateAdmin()]: Exception:" + e.getMessage());
            }            
            throw new XanbooException(10030, "[updateAdmin]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
    }
    
    public void updateAdmin(XanbooAdminPrincipal xCaller, String adminId, String password, int roleId, int adminLevel, String domainId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[updateAdmin()]: ");
        }

        Connection conn=null;
        boolean rollback=false;

        try {
            checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            dao.updateAdmin(conn, xCaller, adminId, password, roleId, adminLevel, domainId);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;        
            if(logger.isDebugEnabled()) {
              logger.error("[updateAdmin()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[updateAdmin()]: Exception:" + e.getMessage());
            }            
            throw new XanbooException(10030, "[updateAdmin]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
    }    

    public XanbooResultSet getProfileTypeList(XanbooAdminPrincipal xCaller) throws XanbooException {
        return getProfileTypeList();
    }    
    
    public XanbooResultSet getProfileTypeList() throws XanbooException {
        if(logger.isDebugEnabled()){
            logger.debug("[getProfileTypeList]");
        }
        
        
        boolean execDBCall= false;
        
        
        
     
          
          if( providerRef == null ){
          	if( GlobalNames.providerDBLock.isWriteLocked()){
          		//wait for readlock
          		GlobalNames.providerDBLock.readLock().lock();
          	}else {
          		
          		// get write lock ...
              	GlobalNames.providerDBLock.writeLock().lock();
              	execDBCall = true;
          	}
          	
          }else{
          	resetProviderCache = ( this.providerReloadInterval > 0 && (System.currentTimeMillis() > (this.providerLastTm+this.providerReloadInterval)));
          	if(  resetProviderCache ) {
          		if( ! GlobalNames.providerDBLock.isWriteLocked()){
              	/*	//wait for readlock
              		GlobalNames.templateDBLock.readLock().lock();
              	}else {*/
              		// get write lock ...
                  	GlobalNames.providerDBLock.writeLock().lock();
                  	
                 		execDBCall = true;
              	}
          	}
          }
          
         
          
          
          Connection conn=null;  
         
        // try{ 
          if(execDBCall){
          	
          	if( providerRef != null  && GlobalNames.providerDBLock.isWriteLocked()) {
          		
          		if(!resetProviderCache) {
          			return 	providerRef;
          		}
          		
          		providerLastTm = System.currentTimeMillis();
          		resetProviderCache = false;
          	}
          	
       /* if ( this.providerReloadInterval > 0 && System.currentTimeMillis() > (this.providerLastTm+this.providerReloadInterval))
            this.providerRef = null;
        
        if ( this.providerRef != null ) return this.providerRef;
        
        Connection conn=null;*/
        
        try{
               //checkCallerPrivilege(xCaller);
               conn=dao.getConnection();
               this.providerRef = dao.getProfileTypeList(conn);
             //  this.providerLastTm = System.currentTimeMillis();
              
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e){
            if(logger.isDebugEnabled()) {
              logger.error("[getProfileTypeList()]: " + e.getMessage(), e);
            }else {
              logger.error("[getProfileTypeList()]: " + e.getMessage());
            }                        
            throw new XanbooException(10030, "[getProfileTypeList]:" + e.getMessage());
        }finally{
            dao.closeConnection(conn);
           
            if(GlobalNames.providerDBLock.isWriteLocked()) GlobalNames.providerDBLock.writeLock().unlock();
        }
          }
          return providerRef;
    }
    
    /**
     * Retrieves a catalog of all device descriptors loaded in the system.
     * <br>
     * The application should cache the returned XanbooCatalog entry to avoid multiple calls to this method.
     *
     * @param xCaller A XanbooAdminPrincipal object that identifies the caller
     *
     * @return a XanbooCatalog which contains object representations of all loaded device descriptors.
     */
    public XanbooCatalog getXanbooCatalog() throws XanbooException {
    	return getXanbooCatalog(null);
    }
    
    
    /**
     * @param specificCatalogId - null --> return all catalogs else return specific catalog details 
     * @return
     * @throws XanbooException
     */
    private XanbooCatalog getXanbooCatalog(String specificCatalogId) throws XanbooException {
        if(logger.isDebugEnabled()){
            logger.debug("[getXanbooCatalog()]");
        }

        Connection conn=null;
        
        XanbooCatalog xanbooCatalog = null;
        
        try{
            //checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            XanbooResultSet deviceCatalogs = dao.getDeviceDescriptor( conn, null );

            xanbooCatalog = new XanbooCatalog();

            for ( int i=0; i<deviceCatalogs.size(); i++ ) {
               String catalogId = deviceCatalogs.getElementString( i, "CATALOG_ID");
               
               if(specificCatalogId!=null && !(specificCatalogId.equalsIgnoreCase(catalogId))){ 
            	   continue;
               }

               // Create device catalog entry
               XanbooDeviceCatalog deviceCatalog = new XanbooDeviceCatalog( catalogId, deviceCatalogs.getElementString( i, "LABEL" ) );

               // Create managed object entries and add to this device catalog
               XanbooResultSet mobjects = dao.getDeviceDescriptor( conn, catalogId );
               for ( int j=0; j<mobjects.size(); j++ ) {
                   XanbooMobjectCatalog m = new XanbooMobjectCatalog( mobjects.getElementString( j, "MOBJECT_ID" ), mobjects.getElementString( j, "LABEL" ), mobjects.getElementString( j, "TYPE" ), mobjects.getElementString( j, "DEFAULT_VALUE" ), Byte.parseByte( mobjects.getElementString( j, "ACCESS_ID" ) ), Byte.parseByte( mobjects.getElementString( j, "ISPUBLIC" )) );
                   deviceCatalog.addMobjectCatalog( m );
               }

               // Create event entries and add to this device catalog
               XanbooResultSet events = dao.getEventCatalog( conn, catalogId );
               for ( int j=0; j<events.size(); j++ ) {
                   int eventLevel = 0;
                   try {
                       eventLevel = events.getElementInteger( j, "ELEVEL" );
                   } catch ( Exception e ) {
                       eventLevel = -1;
                   }
                   if ( eventLevel >= 0 ) {
                       XanbooEventCatalog e = new XanbooEventCatalog( events.getElementString( j, "EVENT_ID" ), events.getElementString( j, "LABEL" ) );
                       deviceCatalog.addEventCatalog( e );
                   }
               }

               // Add device catalog to catalogs container
               xanbooCatalog.addDeviceCatalog( deviceCatalog );

            }
            
            deviceCatalogs = this.getDeviceClassList( "en" );
            for ( int j=0; j<deviceCatalogs.size(); j++ ) {
                xanbooCatalog.addDeviceClassName( deviceCatalogs.getElementString( j, "CLASS_ID" ), deviceCatalogs.getElementString( j, "NAME" ) );
            }
               
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e){
            if(logger.isDebugEnabled()) {
              logger.error("[getProfileTypeList()]: " + e.getMessage(), e);
            }else {
              logger.error("[getProfileTypeList()]: " + e.getMessage());
            }                        
            throw new XanbooException(10030, "[getProfileTypeList]:" + e.getMessage());
        }finally{
            dao.closeConnection(conn);
        }
        
        return xanbooCatalog;
        
    }
    
    public XanbooCatalog getXanbooDictionaryCatalog() throws XanbooException {
        if(logger.isDebugEnabled()){
            logger.debug("[getXanbooDictionaryCatalog()]");
        }
        
        XanbooCatalog xanbooCatalog = getXanbooCatalog(GlobalNames.DICTIONARY_CATALOG_ID);
        
        
        return xanbooCatalog;
    }

    public HashMap getDomain(String domainId)throws XanbooException
    {
        logger.debug("[getDomain()]: retrieve domain info for domainId="+domainId);
        if( domainId == null ) return null;
        
        //expiring cache, if GlobalNames.SERVICE_CACHE_EXPIRY_MINUTES minutes passed
        long currTime = System.currentTimeMillis();
        if( (( currTime-lastCacheUpdate)/60000 ) > GlobalNames.SERVICE_CACHE_EXPIRY_MINUTES ) 
        {
            synchronized(this) 
            {
                domainTable=null;
            }
        }
        
        //cache domain table records, if not done before
        if(domainTable==null) 
        {
            try
            {
                XanbooResultSet domainList = getDomainList();
                domainTable = new HashMap<String,HashMap>();
                for(int i=0; i<domainList.size(); i++) 
                {
                    HashMap domainRec = (HashMap) domainList.get(i);
                    domainTable.put((String) domainRec.get("DOMAIN_ID"), domainRec);
                }
                lastCacheUpdate = System.currentTimeMillis();
            }
            catch(Exception xe) 
            {
                if(logger.isDebugEnabled()) 
                {
                    logger.warn("[getDomain()]: ", xe);
                }
                else 
                {
                    logger.warn("[getDomain()]: " + xe.getMessage());
                }
                return null;
            }
        }
        return domainTable.get(domainId);
    }
    
    public XanbooResultSet getDomainList(XanbooAdminPrincipal xCaller) throws XanbooException{
        return getDomainList();
    }
    
    
    public XanbooResultSet getDomainList() throws XanbooException{
        if (logger.isDebugEnabled()) {
            logger.debug("[getDomainList()]:");
        }
        
        Connection conn=null;
        try {
            // validate the caller and privileges
            //checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            return dao.getDomainList(conn);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getDomainList()]" + e.getMessage(), e);
            }else {
              logger.error("[getDomainList()]" + e.getMessage());
            }
            throw new XanbooException(10030, "[getDomainList]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }          
        
    }    
    

 public XanbooResultSet getDomainTemplateList() throws XanbooException{
        if (logger.isDebugEnabled()) {
            logger.debug("[getDomainTemplateList()]:");
        }
        
        boolean execDBCall= false;
        
      
        
      //  if ( System.currentTimeMillis() > (tmplReloadInterval+this.domainTmplLastTm) )
        //    this.domainTemplates = null;
        
       // if ( domainTemplates != null ) return domainTemplates;
        
        if( domainTemplates == null ){
        	if( GlobalNames.templateDBLock.isWriteLocked()){
        		//wait for readlock
        		GlobalNames.templateDBLock.readLock().lock();
        	}else {
        		
        		// get write lock ...
            	GlobalNames.templateDBLock.writeLock().lock();
            	execDBCall = true;
        	}
        	
        }else{
        	resetTemplateCache = ((System.currentTimeMillis() > (tmplReloadInterval+this.domainTmplLastTm)));
        	if(  resetTemplateCache ) {
        		if( ! GlobalNames.templateDBLock.isWriteLocked()){
            	/*	//wait for readlock
            		GlobalNames.templateDBLock.readLock().lock();
            	}else {*/
            		// get write lock ...
                	GlobalNames.templateDBLock.writeLock().lock();
                	
               		execDBCall = true;
            	}
        	}
        }
        
       
        
        
        Connection conn=null;  
       
      // try{ 
        if(execDBCall){
        	
        	if( domainTemplates != null  && GlobalNames.templateDBLock.isWriteLocked()) {
        		
        		if(!resetTemplateCache) {
        			return 	domainTemplates;
        		}
        		resetTemplateCache = false;
        		domainTmplLastTm = System.currentTimeMillis();
        	}
        	
       
        try {
            // validate the caller and privileges
            //checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            domainTemplates = dao.getDomainTemplateList(conn);
           // domainTmplLastTm = System.currentTimeMillis();
           
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getDomainTemplateList()]" + e.getMessage(), e);
            }else {
              logger.error("[getDomainTemplateList()]" + e.getMessage());
            }
            throw new XanbooException(10030, "[getDomainTemplateList]:" + e.getMessage());
        }finally {
        	if (GlobalNames.templateDBLock.isWriteLocked())
				GlobalNames.templateDBLock.writeLock().unlock();
            dao.closeConnection(conn);
            
        }  
        
        }
        return domainTemplates;
        
     /*  }catch (XanbooException xe) {
           throw xe;
       }catch (Exception e) {
           if(logger.isDebugEnabled()) {
             logger.error("[getDomainTemplateList()]" + e.getMessage(), e);
           }else {
             logger.error("[getDomainTemplateList()]" + e.getMessage());
           }
           throw new XanbooException(10030, "[getDomainTemplateList]:" + e.getMessage());
       }finally {
       	if (GlobalNames.templateDBLock.isWriteLocked())
				GlobalNames.templateDBLock.writeLock().unlock();
       	
      
           dao.closeConnection(conn);
           
       }  */
        
    }      

    public void newSubscription(XanbooAdminPrincipal xCaller, XanbooSubscription xsub, boolean verifySubs) throws XanbooException {
        
        if(logger.isDebugEnabled()) {
            logger.debug("[newSubscription()]: ");
        }
        LicenseControl licenseControl = LicenseControl.getInstance();
        if(licenseControl != null && licenseControl.isSubsCountExceeded(xCaller.getDomain())){
        	logger.error("[newSubscription()] License usage exceeded license limit for domainId : "+ xCaller.getDomain());
        	throw new XanbooException(21422,"License usage exceeded license limit for domainId: " + xCaller.getDomain());
        }
        
        if(licenseControl != null && licenseControl.isLicenseRevokedOrExpired(xCaller.getDomain())){
        	logger.error("[newSubscription()] Domain License exprired/revoked for domainId : "+ xCaller.getDomain());
        	throw new XanbooException(21422,"License revoked or expired for domainId: " +xCaller.getDomain());
        }
        
        boolean doMBusSync = true;//MBus
        boolean validateFeatures = true;
        // first validate the input parameters
        try {
            if(!xsub.isValidSubscriptionToCreate()) {
            	 doMBusSync = false; 
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
        	 doMBusSync = false; 
            throw new XanbooException(10050);
        }

        Connection conn=null;
        boolean rollback=false;

        String tmpGGUID=null; //temporary gguid from imei for now, as the controller is not installed/instantiated yet

        try {
            checkCallerPrivilege(xCaller);

            //default subs flags, for now!
            int defaultFlags = GlobalNames.MODULE_SBN_ENABLED ? XanbooSubscription.DEFAULT_SUBS_FLAGS_DOMESTIC : XanbooSubscription.DEFAULT_SUBS_FLAGS;
            
            int dlFlags = defaultFlags;
            
            //if a subs flag is passed, use that
            if(xsub.isSubsStatusToBeUpdated()) {
                if(xsub.getSubsFlagsMask()<=0) {    //no mask, take flags as is
                    dlFlags = xsub.getSubsFlags();
                }else {
                    //final flag value: for 1 bits im mask, get the value from passed flags. For 0 bits, get the flag vals from default
                    dlFlags = (xsub.getSubsFlags() & xsub.getSubsFlagsMask()) | (defaultFlags & (~xsub.getSubsFlagsMask()));   
                }
            }
            
            
            if(verifySubs && GlobalNames.MODULE_CSI_ENABLED) 
            {                  
                HashMap<String,Object> subsDataMap = new HashMap();
                dlFlags = verifySubscription(xsub.getSubsId(), xsub.getHwId(), subsDataMap, dlFlags);
                
                
                String subClass = (String)subsDataMap.get("SubscriptionClass");
                if ( subClass.equalsIgnoreCase("D"))
                    xsub.setSubscriptionClass(GlobalNames.DLLITE);
                else
                    xsub.setSubscriptionClass("DLSEC");

                
                List<String> socCodeList = (List<String>)subsDataMap.get("SOC_CODES");
                setSubscriptionFeatures(xCaller, xsub, socCodeList);
                
                validateFeatures = false;
            }
            else
            {
            	if(xsub.getSubscriptionClass() == null || xsub.getSubscriptionClass().equalsIgnoreCase(""))
                {
            		xsub.setSubscriptionClass("DLSEC");
            	}
                else if ( !xsub.getSubscriptionClass().equalsIgnoreCase("DLSEC") && !xsub.getSubscriptionClass().equalsIgnoreCase(GlobalNames.DLLITE))
                {
            		throw new XanbooException(21420, "Incorrect subscription class " + xsub.getSubscriptionClass());
            	}
            }
            xsub.setSubsFlags(dlFlags);
        
            
            //lookup timezone by zip from SBN, if SBN enabled and SBN zip2timezone lookup (thru app.timezone.lookup="SBN")
            if(GlobalNames.MODULE_SBN_ENABLED && GlobalNames.APP_TIMEZONE_LOOKUP!=null && GlobalNames.APP_TIMEZONE_LOOKUP.equalsIgnoreCase("SBN")) {
                if(xsub.getSubsInfo().getZip()!=null && xsub.getSubsInfo().getZip().length()>0) {
                    SBNSynchronizer sbn = new SBNSynchronizer();
                    String olson = sbn.getTimezoneByZip(xsub.getSubsInfo().getZip());
                    if(olson!=null && olson.length()>0) xsub.setTzone(olson); //replace timezone retrieved, if not null
                }
            }
            
            
            // DLDP 4207 Validate sub features based on domain     
            //TODO: Should we log the error and continue or throw exception?
            if ( validateFeatures  )
                validateSubsFeature(xCaller.getDomain(), xsub.getSubsFeatures());
			// End of DLDP 4207
            conn=dao.getConnection();
            try {
                tmpGGUID = dao.newSubscription(conn, xCaller, xsub);
            }catch (XanbooException xe) {
            	 doMBusSync = false; 
                //if(xe.getCode()!=21401) throw xe; //ignore already existing subscription exceptions!
                throw xe;
            }
            
            //sync with SBN, if enabled
            if(GlobalNames.MODULE_SBN_ENABLED) {
                SBNSynchronizer sbn = new SBNSynchronizer();
                boolean sbnOK = sbn.newSubscription(xsub, tmpGGUID);
                if(!sbnOK) {    //SBN sync failed!
                    throw new XanbooException(21420, "Failed to add/update subscription. SBN synchronization failed.");
                }
            }
            /*//Signal RMS on subscription creation
            if(GlobalNames.MODULE_PH_ENABLED || GlobalNames.MODULE_RMS_ENABLED) {
            	String rmsphEnabled = "RMS";            	
            	if(GlobalNames.MODULE_PH_ENABLED){
            		rmsphEnabled = "PH";
            	}
                try{
                    HashMap groups = new HashMap<String, String>();
                    groups.put(RMS_MARKET_AREA, xsub.getbMarket());
                    RMSClient.registerDLC(xsub.getHwId(), groups, xCaller.getDomain(), rmsphEnabled);                    
                }
                catch(XanbooException rmse){
                    if(logger.isDebugEnabled())
                        logger.warn("[newSubscription()]: Failed to add subscription hardware in RMS. CTN:" + xsub.getSubsId() + ", IMEI:"+ xsub.getHwId() + ", MARKETAREACODE:"+xsub.getbMarket()+ ", Exception:" + rmse.getErrorMessage(), rmse);
                    else
                        logger.warn("[newSubscription()]: Failed to add subscription hardware in RMS. CTN:" + xsub.getSubsId() + ", IMEI:"+ xsub.getHwId() + ", MARKETAREACODE:"+xsub.getbMarket()+ ", Exception:" + rmse.getErrorMessage());
                }catch (Exception rmse) {
                    if(logger.isDebugEnabled())
                        logger.warn("[newSubscription()]: Failed to add subscription hardware in RMS. CTN:" + xsub.getSubsId() + ", IMEI:"+ xsub.getHwId() + ", MARKETAREACODE:"+xsub.getbMarket()+ ", Exception:" + rmse.getMessage(), rmse);
                    else
                        logger.warn("[newSubscription()]: Failed to add subscription hardware in RMS. CTN:" + xsub.getSubsId() + ", IMEI:"+ xsub.getHwId() + ", MARKETAREACODE:"+xsub.getbMarket()+ ", Exception:" + rmse.getMessage());
                }
            }*/                

        }catch (XanbooException xe) {
            rollback=true;
            doMBusSync = false; 
            throw xe;
        }catch (Exception e) {
            rollback=true;
            doMBusSync = false; 
            if(logger.isDebugEnabled()) {
              logger.error("[newSubscription()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[newSubscription()]: Exception:" + e.getMessage());
            }
            throw new XanbooException(10030, e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
            
          //Signal RMS on subscription creation. moved to finally block not to block db for any RMS delay
            if(doMBusSync && (GlobalNames.MODULE_PH_ENABLED || GlobalNames.MODULE_RMS_ENABLED)) {
            	String rmsphEnabled = "RMS";            	
            	if(GlobalNames.MODULE_PH_ENABLED){
            		rmsphEnabled = "PH";
            	}
                try{
                    HashMap groups = new HashMap<String, String>();
                    groups.put(RMS_MARKET_AREA, xsub.getbMarket());
                    RMSClient.registerDLC(xsub.getHwId(), groups, xCaller.getDomain(), rmsphEnabled);                    
                }
                catch(XanbooException rmse){
                    if(logger.isDebugEnabled())
                        logger.warn("[newSubscription()]: Failed to add subscription hardware in RMS. CTN:" + xsub.getSubsId() + ", IMEI:"+ xsub.getHwId() + ", MARKETAREACODE:"+xsub.getbMarket()+ ", Exception:" + rmse.getErrorMessage(), rmse);
                    else
                        logger.warn("[newSubscription()]: Failed to add subscription hardware in RMS. CTN:" + xsub.getSubsId() + ", IMEI:"+ xsub.getHwId() + ", MARKETAREACODE:"+xsub.getbMarket()+ ", Exception:" + rmse.getErrorMessage());
                }catch (Exception rmse) {
                    if(logger.isDebugEnabled())
                        logger.warn("[newSubscription()]: Failed to add subscription hardware in RMS. CTN:" + xsub.getSubsId() + ", IMEI:"+ xsub.getHwId() + ", MARKETAREACODE:"+xsub.getbMarket()+ ", Exception:" + rmse.getMessage(), rmse);
                    else
                        logger.warn("[newSubscription()]: Failed to add subscription hardware in RMS. CTN:" + xsub.getSubsId() + ", IMEI:"+ xsub.getHwId() + ", MARKETAREACODE:"+xsub.getbMarket()+ ", Exception:" + rmse.getMessage());
                }
            }
            
			// MBus Start-
			if (GlobalNames.MBUS_SYNC_ENABLED && doMBusSync) {
				String subsLName = null;
				String subsFName = null;
				String street1 = null;
				String street2 = null;
				String city = null;
				String state = null;
				String zip = null;
				String zip4 = null;
				String country = null;
				long userId = 0;

				if (xsub.getSubsInfo() != null) {
					subsLName = xsub.getSubsInfo().getLastName();
					subsFName = xsub.getSubsInfo().getFirstName();
					street1 = xsub.getSubsInfo().getAddress1();
					street2 = xsub.getSubsInfo().getAddress2();
					city = xsub.getSubsInfo().getCity();
					state = xsub.getSubsInfo().getState();
					zip = xsub.getSubsInfo().getZip();
					zip4 = xsub.getSubsInfo().getZip4();
					country = xsub.getSubsInfo().getCountry();
					userId = xsub.getSubsInfo().getUserId();
				}

				DLCoreMBusSubscriptionChange data = new DLCoreMBusSubscriptionChange();
				data.setAccId(xsub.getAccountId());
				data.setbMarket(xsub.getbMarket());
				data.setbSubMarket(xsub.getbSubMarket());
				data.setCity(city);
				data.setCountry(country);
				
				String 	date = XanbooUtil.getGMTDateTime(xsub.getDateCreated());
				data.setDateCreated(date);
				
				data.setDomain(xCaller.getDomain());
				data.setExtAccId(xsub.getExtAccountId());
				data.setGatewayGUID(tmpGGUID);
				data.setHwId(xsub.getHwId());
				// data.setHwIdNew(hwIdNew);
				data.setSrcAppId("SysAdminManagerEJB.newSubscription");
				data.setSrcOriginatorId(userId + "");
				data.setState(state);
				data.setStreet1(street1);
				data.setStreet2(street2);
				data.setSubsFeatures(xsub.getSubsFeatures());
				data.setSubsFirstName(subsFName);
				data.setSubsFlags(xsub.getSubsFlags());
				data.setSubsId(xsub.getSubsId());
				data.setSubsLastName(subsLName);
				data.setTimeZone(xsub.getTzone());
                data.setSubsClass(xsub.getSubscriptionClass());
				data.setZip(zip);
				data.setZip4(zip4);
				
				data.setInstallType(xsub.getInstallType());//DLDP 2679

				if (logger.isDebugEnabled()) {

					StringBuffer sb = new StringBuffer(
							"[MBus : publish messages : SysAdminManagerEJB.newSubscription()]:");
					sb.append("\n  : " + data.toString());

					logger.debug(sb.toString());
				}
				MBusSynchronizer.newSubscription(data);

                if ( xsub.getSubscriptionClass().equalsIgnoreCase(GlobalNames.DLLITE))
                {
                    //publish device create
                    /*DLCoreMBusEntityChange data = new DLCoreMBusEntityChange();
                    data.setAccId(device.getAccount());
                    data.setCatId(xc.getAttribute(kw.XKEY_CATALOG_ID));
                    data.setDeviceGUID(xc.getAttribute(kw.XKEY_DEVI));
                    data.setDomain(xc.getAttribute(kw.XKEY_DOMAIN) );
                    data.setExtAccId(device.getExtAccountId());
                    data.setFw(xc.getAttribute(kw.XKEY_FWVERSION));
                    data.setGatewayGUID(device.getGatewayGUID());
                    data.setHwId(xc.getAttribute(kw.XKEY_HWID));
                    data.setInsId(xc.getAttribute(kw.XKEY_IID));
                    data.setLabel(	xc.getAttribute(kw.XKEY_LABEL));
                    data.setMake(null);
                    data.setModel(xc.getAttribute(kw.XKEY_MODEL));
                    data.setSerial(xc.getAttribute(kw.XKEY_SERIALNO));
                    data.setSrcAppId("AbstractMessageHandlerEJB.processEntitySynchronization");
                    data.setSrcOriginatorId(userId+"");
                    data.setStatus(status);
                    data.setSubsId(device.getSubId());
                    data.setSw(xc.getAttribute(kw.XKEY_SWVERSION));

                    String 	date = XanbooUtil.getGMTDateTime(xc.getAttribute(kw.XKEY_TIME));

                    data.setCreationDate(date);
                    data.setTs(date);
                    data.setSourceId(dSourceId);

                    MBusSynchronizer.newDevice(data);
                    
                    //publish oids
                    DLCoreMBusStateChange state = new DLCoreMBusStateChange();

                    state.setAccId(device.getAccount());
                    String 	date = XanbooUtil.getGMTDateTime(xc.getAttribute(kw.XKEY_TIME));
                    state.setDateCreated(date);
                    state.setDomain(device.getDomain());
                    state.setExtAccId(device.getExtAccountId());
                    state.setGatewayGUID(device.getGatewayGUID());
                    state.setSrcAppId("AbstractMessageHandlerEJB.processEntitySynchronization" );
                    state.setSrcOriginatorId(userid);
                    state.setSubsId(device.getSubId());
                    state.setDeviceGUID(deviceGUID);
                    state.setCatId(catId);
                    state.setOid(mobjectId);
                    state.setCqId(xc.getAttribute(kw.XKEY_CQID));
                    state.setAck(xc.getAttribute(kw.XKEY_ACK));
                    state.setVal(value);

                    MBusSynchronizer.updateObject(state);
                    * */
                }
                
			}
			// MBus end
            
            
        }
        
        
    }

    
    public void newSubscription(XanbooAdminPrincipal xCaller, long accountId, String extAccountId, String cLastname, String cFirstname, String subsId, int subsFlags,
            String hwId, String label, String tzName, String masterPin, String masterDuress, String alarmPass,
             XanbooContact subsInfo, XanbooNotificationProfile[] emergencyContacts, boolean verifySubs) throws XanbooException {

        XanbooSubscription xsub = new XanbooSubscription(accountId, extAccountId);
        xsub.setSubsId(subsId);
        xsub.setSubsFlags(subsFlags);
        xsub.setHwId(hwId);
        xsub.setLabel(label);
        xsub.setTzone(tzName);
        xsub.setDisarmPin(masterPin);
        xsub.setDuressPin(masterDuress);
        xsub.setAlarmCode(alarmPass);
        xsub.setSubsInfo(subsInfo);
        xsub.setNotificationProfiles(emergencyContacts);
        xsub.setSubsFeatures(null);
        
        newSubscription(xCaller, xsub, verifySubs);
   }

    /*  As part of this DLDP, bit-4 and bit-5 will be combined and renamed as 'Monitoring Type' mode/option for the subscription:

    Bit 4-5: Monitoring Type 

    00: No Monitoring (NM) // 0

    01: Professional Monitoring (PM) // 16 

    10: Self-Monitoring (SM) // 32

    11: Professional Monitoring-Pending (PP)  // 48 */
    private SubscriptionMonitorStatus checkForSubMonitorTypeFlagChange(int nSubsFlagsOld, int nSubsFlagsNew) {
    	
    	 if(logger.isDebugEnabled()) {
             logger.debug("[checkForSubMonitorTypeFlagChange()]: nSubsFlagsOld : " + nSubsFlagsOld + " \n : nSubsFlagsNew : " + nSubsFlagsNew );
         }

        // if mask is specified but not touching subscription monitoring type, return null
        if ((nSubsFlagsOld & 48) == (nSubsFlagsNew & 48)) return null;

        return getSubMonitorType(nSubsFlagsNew);
	}


private SubscriptionMonitorStatus getSubMonitorType(int nSubsFlags) {
	int nSMC = nSubsFlags & 48;
	
	if(nSMC == 0 ) return SubscriptionMonitorStatus.NM;
	if(nSMC == 16 ) return SubscriptionMonitorStatus.PM;
	if(nSMC == 32 ) return SubscriptionMonitorStatus.SM;
	if(nSMC == 48 ) return SubscriptionMonitorStatus.PP;
	
	

	return null;
}

    public void updateSubscription(XanbooAdminPrincipal xCaller, XanbooSubscription xsub, String hwIdNew, int alarmDelay, int tcFlag) throws XanbooException    {
    	updateSubscription( xCaller, xsub, hwIdNew, alarmDelay, tcFlag, null, null, false);
    }
    
    private  void updateSubscription(XanbooAdminPrincipal xCaller, XanbooSubscription xsub, String hwIdNew, int alarmDelay, int tcFlag, String feature_csv, XanbooSubscription xanSubs, Boolean addKeyHolder) throws XanbooException    {
        if(logger.isDebugEnabled()) {
        	logger.debug("[updateSubscription()]: XanbooSubscription :  " + xsub +" \n" + " XanbooAdminPrincipal : " +xCaller );
        }
    boolean doMBusSync = true; //MBus 
    //special subsFlags support
    //      -9999: restore service
    //      -9998: suspend service
    //      -9997: cancel service
    //      -9995: set 3g-only profile
    //      -9994: set 3g+bb profile
    //???SHOULD WE ALLOW direct sets thru >=0 values? allowed for now!

    // first validate the input parameters
    try {
        if(!xsub.isValidSubscriptionToUpdate()) {
        	 doMBusSync = false;
            throw new XanbooException(10050);
        }
        
        if(hwIdNew!=null && xsub.getHwId().equalsIgnoreCase(hwIdNew)) hwIdNew=null; //null newhwid param, if equal to current subs hwid.

    }catch(Exception e) {
    	doMBusSync = false;
        throw new XanbooException(10050);
    }
    
    /* if cancellation request, use the related call - no other subscription updates allowed */
    if(xsub.isCancelled()) {
        cancelSubscription(xCaller, xsub.getAccountId(), null, xsub.getSubsId(), xsub.getHwId());
        return;
    }
    
  
    
  //  XanbooSubscription xsub2=xsub;
    
    XanbooSubscription xsubOld= null;
    
    XanbooSubscription xsubNew=null;
    boolean isSubsMonTypeDiffFromDB =  true;
   
    
    Connection conn=null;
    boolean rollback=false;
    SBNSynchronizer sbn = null;
    try {
        checkCallerPrivilege(xCaller);
        
      
            //read current subs flags to compare after changes
            if(xanSubs == null){ // added for DLDP 4207 to avoid repeated getsubscription method invocation from addRemove
	        	XanbooResultSet  subsList = this.getSubscription(xCaller, xsub.getAccountId(), xsub.getSubsId(), xsub.getHwId());  
	        	if (subsList == null || (subsList.size() == 0)) {
	        	    throw new XanbooException(21405, "Failed to add/update subscription. Invalid account or subscription id, or not found.");
	        	}
	        	xsubOld = new XanbooSubscription(subsList, 0);
            }else{
            	xsubOld = xanSubs;
            }
               
			//////////////////////////////////////////////////////////////////////////
			//Van fulfillment - If this is an IMEI update for a van fulfillment order, 
			//update subsFlags with the "3G Only" indicator and update subscription features from the CSI verifySubscription call
			// invoke CSI if enabled
			if (GlobalNames.MODULE_CSI_ENABLED && (hwIdNew != null && !hwIdNew.trim().equals("") && xsubOld.getSubsId().equalsIgnoreCase(xsubOld.getHwId()))) {
				HashMap<String,Object> subsDataMap = new HashMap();
				int dlFlags = verifySubscription(xsub.getSubsId(), hwIdNew, subsDataMap, xsubOld.getSubsFlags());
				xsub.setSubsFlags(dlFlags);
			
				List<String> socCodeList = (List<String>)subsDataMap.get("SOC_CODES");
				setSubscriptionFeatures(xCaller, xsub, socCodeList);
			}      
			//End Van fulfillment IMIE update check
			/////////////////////////////////////////////////////////////////////////

        //lookup timezone by zip from SBN, if SBN enabled and SBN zip2timezone lookup (thru app.timezone.lookup="SBN")
        if(GlobalNames.MODULE_SBN_ENABLED && GlobalNames.APP_TIMEZONE_LOOKUP!=null && GlobalNames.APP_TIMEZONE_LOOKUP.equalsIgnoreCase("SBN")) {
            if(xsub.getSubsInfo()!=null && xsub.getSubsInfo().getZip()!=null && xsub.getSubsInfo().getZip().length()>0) {
                if(sbn == null) sbn = new SBNSynchronizer();
                String olson = sbn.getTimezoneByZip(xsub.getSubsInfo().getZip());
                if(olson!=null && olson.length()>0) xsub.setTzone(olson); //replace timezone retrieved, if not null
            }
        }
        
        conn=dao.getConnection();
        
        if(feature_csv != null){
     	   xsub.setSubsFeatures(feature_csv);
        }
        if(xsub.getSubsFeatures() != null && xsub.getSubsFeatures().trim().equalsIgnoreCase(",")){
     	   xsub.setSubsFeatures(" ");
        }
        XanbooNotificationProfile xnpArray[] = dao.getNotificationProfile(conn,xsub.getAccountId(), 0,true);
        
        // DLDP 4207 check if subscription has emergency contact defined
       
		if (xsub.getSubsFeatures() != null) {
				String[] strSubsFea = xsub.getSubsFeatures().split(",");
				for (int i = 0; i < strSubsFea.length; i++) {
					if (checkKeyHolderList(strSubsFea[i])) {
						addKeyHolder = true;
						break;
					}
				}
			}
        
        
			if(addKeyHolder){
				Boolean atleastOneEmergencyContact = false;
				for (XanbooNotificationProfile xanbooNotificationProfile : xnpArray) {
					if(xanbooNotificationProfile.isValidEmergencyContact()){
						atleastOneEmergencyContact = true;
						break;
					}
				}
	        
				if(!atleastOneEmergencyContact){        
					throw new XanbooException(27206, "Atleast one emergency contact information needed.");
				}
			}
		
        // DLDP 4207 ends for emergency contact information

		//update subscription and see if a gguid exists for the subscription
        xsub = dao.updateSubscription(conn, xCaller, xsub, hwIdNew, alarmDelay, tcFlag);
     
        
        // read subscription to get details after changes (using new hwid if necessary)
        XanbooResultSet subsList = dao.getSubscription(conn, xCaller, xsub.getAccountId(), xsub.getSubsId(), (hwIdNew!=null && hwIdNew.trim().length()>0) ? hwIdNew.trim() : xsub.getHwId());
       xsubNew = new XanbooSubscription(subsList, 0);
            
       if(xsubNew.getSubsFeatures()!=null && xsubNew.getSubsFeatures().trim().equalsIgnoreCase(""))
       {
    	   xsubNew.setSubsFeatures(" ");
       }
        isSubsMonTypeDiffFromDB =  checkIfSubsMonTypeIsDIffFromDB(xsubOld,xsubNew);
        
        //if service status is being changed
            /*if(isSubsStatusChangeToActiveFromCancel(xsubOld,xsubNew)) { //   
            	   //dl service status bits to 11 --> activate/restore service
          	
            // sync with ACSI when the gateway activated again
            if(GlobalNames.MODULE_CSI_ENABLED && xsubNew.getGguid().indexOf("DL-")==-1) {
                SimpleACSIClient acsi = new SimpleACSIClient();
                acsi.newDeviceRegistered(xsubNew.getExtAccountId(),
						xsubNew.getSubsId(), 
						xsubNew.getGguid(),
                        "0",
                        GlobalNames.GWAY_CATALOG_ID,
                        null,
                        GlobalNames.DEFAULT_APP_USER,
                        xsubNew.getHwId(),
                        xsubNew.getHwId(),null );
            }
            
            
            //Signal RMS on subscription activate/restore service - only if there is a real gwy!!!
            if( (GlobalNames.MODULE_PH_ENABLED || GlobalNames.MODULE_RMS_ENABLED) && xsubNew.getGguid().indexOf("DL-")==-1) {
            	String rmsphEnabled = "RMS";            	
            	if(GlobalNames.MODULE_PH_ENABLED){
            		rmsphEnabled = "PH";
            	}
                String marketAreaCode="";
                try{
                    //First get the market area code
                    marketAreaCode=xsubNew.getbMarket();
                    if(marketAreaCode != null && marketAreaCode.length() > 0 ) {
                        HashMap groups = new HashMap<String, String>();
                        groups.put(RMS_MARKET_AREA, marketAreaCode);
                        RMSClient.registerOrUpdateDLC(xsubNew.getHwId(), groups, xCaller.getDomain(), rmsphEnabled);
                    }
                }
                catch(XanbooException rmse){
                    if(logger.isDebugEnabled())
                        logger.warn("[updateSubscription()]: Failed to add subscription hardware in RMS. CTN:" + xsubNew.getSubsId() + ", IMEI:"+ xsubNew.getHwId() + ", MARKETAREACODE:"+marketAreaCode+ ", Exception:" + rmse.getErrorMessage(), rmse);
                    else
                        logger.warn("[updateSubscription()]: Failed to add subscription hardware in RMS. CTN:" + xsubNew.getSubsId() + ", IMEI:"+ xsubNew.getHwId() + ", MARKETAREACODE:"+marketAreaCode+ ", Exception:" + rmse.getErrorMessage());
                }catch (Exception rmse) {
                    if(logger.isDebugEnabled())
                        logger.warn("[updateSubscription()]: Failed to add subscription hardware in RMS. CTN:" + xsubNew.getSubsId() + ", IMEI:"+ xsubNew.getHwId() + ", MARKETAREACODE:"+marketAreaCode+ ", Exception:" + rmse.getMessage(), rmse);
                    else
                        logger.warn("[updateSubscription()]: Failed to add subscription hardware in RMS. CTN:" + xsubNew.getSubsId() + ", IMEI:"+ xsubNew.getHwId() + ", Exception:" + rmse.getMessage());
                }
            }
            
            
            
            }
            //signal RMS on IMEI change !
            if((GlobalNames.MODULE_PH_ENABLED || GlobalNames.MODULE_RMS_ENABLED ) && hwIdNew!=null && hwIdNew.length()>0) {
            	String rmsphEnabled = "RMS";            	
            	if(GlobalNames.MODULE_PH_ENABLED){
            		rmsphEnabled = "PH";
            	}
                String marketAreaCode="";
                try{
                    //First get the market area code
                    marketAreaCode=xsubNew.getbMarket();
                    if(marketAreaCode != null && marketAreaCode.length() > 0 ) {
                        RMSClient.replaceDLC(xsubOld.getHwId(),hwIdNew,marketAreaCode,RMS_MARKET_AREA, xCaller.getDomain(),rmsphEnabled);
                    }
                }
                catch(XanbooException rmse){
                    if(logger.isDebugEnabled())
                        logger.warn("[updateSubscription()]: Failed to swap subscription hardware in RMS. CTN:" + xsubNew.getSubsId() + ", IMEI:"+ xsubOld.getHwId() + ", NEWIMEI:"+ hwIdNew + ", MARKETAREACODE:"+marketAreaCode+", Exception:" + rmse.getErrorMessage(), rmse);
                    else
                        logger.warn("[updateSubscription()]: Failed to swap subscription hardware in RMS. CTN:" + xsubNew.getSubsId() + ", IMEI:"+ xsubOld.getHwId() + ", NEWIMEI:"+ hwIdNew + ", MARKETAREACODE:"+marketAreaCode+ ", Exception:" + rmse.getErrorMessage(), rmse);
                }catch (Exception rmse) {
                    if(logger.isDebugEnabled())
                        logger.warn("[updateSubscription()]: Failed to swap subscription hardware in RMS. CTN:" + xsubNew.getSubsId() + ", IMEI:"+ xsubOld.getHwId() + ", NEWIMEI:"+ hwIdNew + ", MARKETAREACODE:"+marketAreaCode+ ", Exception:" + rmse.getMessage(), rmse);
                    else
                        logger.warn("[updateSubscription()]: Failed to swap subscription hardware in RMS. CTN:" + xsubNew.getSubsId() + ", IMEI:"+ xsubOld.getHwId() + ", NEWIMEI:"+ hwIdNew + ", MARKETAREACODE:"+marketAreaCode+ ", Exception:" + rmse.getMessage(), rmse);
                }
            }
            
            
                
              make a call to CSI 
            if(hwIdNew!=null && !xsubOld.getHwId().equalsIgnoreCase(hwIdNew)){
            	 /* sync with ACSI 
                if(GlobalNames.MODULE_CSI_ENABLED) {
	                SimpleACSIClient acsi = new SimpleACSIClient();
	                int rc = acsi.deviceRemoved(xsubNew.getExtAccountId(), 
	                						xsubNew.getSubsId(), 
	                						xsubNew.getGguid(), 
	                                        "0",
	                                        null );
                }
            }  
            
          */
        
        
			// DLDP 3043 -start -vp889x        	
			XanbooResultSet rsDeviceList = null;
			if (GlobalNames.MODULE_SBN_ENABLED) {

				if (sbn == null)
					sbn = new SBNSynchronizer();
		
				// Check for the subscription monitoring type flags change,
				// including pending-PM
				SubscriptionMonitorStatus sms = checkForSubMonitorTypeFlagChange(
						xsubOld.getSubsFlags(), xsubNew.getSubsFlags());

				if (logger.isDebugEnabled()) {
					logger.debug("SubscriptionMonitorStatus : " + sms);
				}

				// if (sms != null) {
				//XanbooNotificationProfile[] xnpArray = null; commented out for 4207 as notification profile retrieved above for same account number.
				

				if ( (isSubsMonTypeDiffFromDB && isEligibleForSBNFullSync(sms))  || addKeyHolder) { 

					XanbooPrincipal xPrincipal = getXanbooPrincipal(xCaller,
							xsubNew.getAccountId(), xsubNew.getSubsId(),
							xsubNew.getHwId());

					if (logger.isDebugEnabled()) {
						logger.debug("[dao.getDeviceList()]: " + xPrincipal
								+ " , " + xsubNew.getGguid() + " , null");
					}

					rsDeviceList = dao.getDeviceList(conn, xPrincipal,
							xsubNew.getGguid(), null);

					/*
					 *  commented out for 4207 as notification profile retrieved above for same account number.
					 *  invoking getNotificationProfile only once.
					 
					XanbooNotificationProfile[] xnpArray1 = dao.getNotificationProfile(conn,
							xPrincipal.getAccountId(), xPrincipal.getUserId(),
							true);
					*/
					
					fullSyncWithSBN(xsubNew, sbn, rsDeviceList, sms,
							hwIdNew,   xnpArray,xsubOld );


				} else{
					// check if both are same set null and -1 to features and subs flag
					if ( (xsubOld.getSubsFlags() == xsubNew.getSubsFlags()) && ((xsubOld.getSubsFeatures()!= null) && (xsubOld.getSubsFeatures().equalsIgnoreCase(xsubNew.getSubsFeatures())) ) ) {
						xsub.setSubsFeatures(null);
						xsub.setSubsFlags(-1);
					}else{
						xsub.setSubsFeatures(xsubNew.getSubsFeatures());
						xsub.setSubsFlags(xsubNew.getSubsFlags());
					}
					sbnInstalationAlarmDelay(alarmDelay, xsubNew, sbn);
					sbnUpdateInstallation(xsub, sbn, hwIdNew);
					sbnUpdateInstallationMonitorStatus(xsubNew, sbn, sms ,  xsubOld);
				}
				
			}

        //DLDP 3043 -end -vp889x
		
		if(xsubOld.getSubsFeatures()!=null && xsubNew.getSubsFeatures()!= null){
			String []origSubsFeature = xsubOld.getSubsFeatures().split(",");
	    	String	[]finalSubsFeature = xsubNew.getSubsFeatures().split(",");                	
			List<String> lstOrigSubFeature = new ArrayList<String>(Arrays.asList(origSubsFeature));
			List<String> lstFinalSubFeature = new ArrayList<String>(Arrays.asList(finalSubsFeature));
			List<String> featuresRemoved = null;
			featuresRemoved = getRemovedSubFeature(lstOrigSubFeature, lstFinalSubFeature);
						
			cancelServiceSubscription(xCaller, featuresRemoved, xsubNew);
		}
    }catch (XanbooException xe) {
        rollback=true;
        doMBusSync = false;
        throw xe;
    }catch (Exception e) {
        rollback=true;
        doMBusSync = false;
        if(logger.isDebugEnabled()) {
          logger.error("[updateSubscription()]: Exception:" + e.getMessage(), e);
        }else {
          logger.error("[updateSubscription()]: Exception:" + e.getMessage());
        }
        throw new XanbooException(10030, e.getMessage());
    }finally {
        dao.closeConnection(conn, rollback);
        //if service status is being changed
        if(doMBusSync && isSubsStatusChangeToActiveFromCancel(xsubOld,xsubNew)) { //   
        	   //dl service status bits to 11 --> activate/restore service
      	
        /* sync with ACSI when the gateway activated again*/
         if(GlobalNames.MODULE_CSI_ENABLED  && xsubNew.getGguid().indexOf("DL-")==-1) {
            SimpleACSIClient acsi = new SimpleACSIClient();
            acsi.newDeviceRegistered(xsubNew.getExtAccountId(),
					xsubNew.getSubsId(), 
					xsubNew.getGguid(),
                    "0",
                    GlobalNames.GWAY_CATALOG_ID,
                    null,
                    GlobalNames.DEFAULT_APP_USER,
                    xsubNew.getHwId(),
                    xsubNew.getHwId(),null );
        }
        
        //Signal RMS on subscription activate/restore service - only if there is a real gwy!!!
        if(doMBusSync && (GlobalNames.MODULE_PH_ENABLED || GlobalNames.MODULE_RMS_ENABLED)  && xsubNew.getGguid().indexOf("DL-")==-1) {
        	String rmsphEnabled = "RMS";            	
        	if(GlobalNames.MODULE_PH_ENABLED){
        		rmsphEnabled = "PH";
        	}
            String marketAreaCode="";
            try{
                //First get the market area code
                marketAreaCode=xsubNew.getbMarket();
                if(marketAreaCode != null && marketAreaCode.length() > 0 ) {
                    HashMap groups = new HashMap<String, String>();
                    groups.put(RMS_MARKET_AREA, marketAreaCode);
                    RMSClient.registerOrUpdateDLC(xsubNew.getHwId(), groups, xCaller.getDomain(), rmsphEnabled);
                }
            }
            catch(XanbooException rmse){
                if(logger.isDebugEnabled())
                    logger.warn("[updateSubscription()]: Failed to add subscription hardware in RMS. CTN:" + xsubNew.getSubsId() + ", IMEI:"+ xsubNew.getHwId() + ", MARKETAREACODE:"+marketAreaCode+ ", Exception:" + rmse.getErrorMessage(), rmse);
                else
                    logger.warn("[updateSubscription()]: Failed to add subscription hardware in RMS. CTN:" + xsubNew.getSubsId() + ", IMEI:"+ xsubNew.getHwId() + ", MARKETAREACODE:"+marketAreaCode+ ", Exception:" + rmse.getErrorMessage());
            }catch (Exception rmse) {
                if(logger.isDebugEnabled())
                    logger.warn("[updateSubscription()]: Failed to add subscription hardware in RMS. CTN:" + xsubNew.getSubsId() + ", IMEI:"+ xsubNew.getHwId() + ", MARKETAREACODE:"+marketAreaCode+ ", Exception:" + rmse.getMessage(), rmse);
                else
                    logger.warn("[updateSubscription()]: Failed to add subscription hardware in RMS. CTN:" + xsubNew.getSubsId() + ", IMEI:"+ xsubNew.getHwId() + ", Exception:" + rmse.getMessage());
            }
        }
        
        }
        
        //signal RMS on IMEI change !
        if(doMBusSync && (GlobalNames.MODULE_PH_ENABLED || GlobalNames.MODULE_RMS_ENABLED ) && hwIdNew!=null && hwIdNew.length()>0) {
        	String rmsphEnabled = "RMS";            	
        	if(GlobalNames.MODULE_PH_ENABLED){
        		rmsphEnabled = "PH";
        	}
            String marketAreaCode="";
            try{
                //First get the market area code
                marketAreaCode=xsubNew.getbMarket();
                if(marketAreaCode != null && marketAreaCode.length() > 0 ) {
                    RMSClient.replaceDLC(xsubOld.getHwId(),hwIdNew,marketAreaCode,RMS_MARKET_AREA, xCaller.getDomain(),rmsphEnabled);
                }
            }
            catch(XanbooException rmse){
                if(logger.isDebugEnabled())
                    logger.warn("[updateSubscription()]: Failed to swap subscription hardware in RMS. CTN:" + xsubNew.getSubsId() + ", IMEI:"+ xsubOld.getHwId() + ", NEWIMEI:"+ hwIdNew + ", MARKETAREACODE:"+marketAreaCode+", Exception:" + rmse.getErrorMessage(), rmse);
                else
                    logger.warn("[updateSubscription()]: Failed to swap subscription hardware in RMS. CTN:" + xsubNew.getSubsId() + ", IMEI:"+ xsubOld.getHwId() + ", NEWIMEI:"+ hwIdNew + ", MARKETAREACODE:"+marketAreaCode+ ", Exception:" + rmse.getErrorMessage(), rmse);
            }catch (Exception rmse) {
                if(logger.isDebugEnabled())
                    logger.warn("[updateSubscription()]: Failed to swap subscription hardware in RMS. CTN:" + xsubNew.getSubsId() + ", IMEI:"+ xsubOld.getHwId() + ", NEWIMEI:"+ hwIdNew + ", MARKETAREACODE:"+marketAreaCode+ ", Exception:" + rmse.getMessage(), rmse);
                else
                    logger.warn("[updateSubscription()]: Failed to swap subscription hardware in RMS. CTN:" + xsubNew.getSubsId() + ", IMEI:"+ xsubOld.getHwId() + ", NEWIMEI:"+ hwIdNew + ", MARKETAREACODE:"+marketAreaCode+ ", Exception:" + rmse.getMessage(), rmse);
            }
        }
        
        /* make a call to CSI */
        if(doMBusSync && (hwIdNew!=null && !xsubOld.getHwId().equalsIgnoreCase(hwIdNew))){
        	/* sync with ACSI */
            if(GlobalNames.MODULE_CSI_ENABLED) {
                SimpleACSIClient acsi = new SimpleACSIClient();
                int rc = acsi.deviceRemoved(xsub.getExtAccountId(), 
                						xsubNew.getSubsId(), 
                						xsubNew.getGguid(), 
                                        "0",
                                        null );
            }
        }  
        
        //MBus Start- 
        if(GlobalNames.MBUS_SYNC_ENABLED && doMBusSync){
        	
        
        	String subsLName = null;
        	String subsFName = null;
        	String street1 = null;
        	String street2 = null;
        	String city = null;
        	String state = null;
        	String zip = null;
        	String zip4 = null;
        	String country = null;
        	long userId = 0;

        	if(xsubNew.getSubsInfo()  != null){
        		subsLName = xsubNew.getSubsInfo().getLastName();
        		subsFName = xsubNew.getSubsInfo().getFirstName();
        		street1 = xsubNew.getSubsInfo().getAddress1();
        		street2 = xsubNew.getSubsInfo().getAddress2();
        		city = xsubNew.getSubsInfo().getCity();
        		state = xsubNew.getSubsInfo().getState();
        		zip = xsubNew.getSubsInfo().getZip();
        		zip4 = xsubNew.getSubsInfo().getZip4();
        		country = xsubNew.getSubsInfo().getCountry();
        		userId = xsubNew.getSubsInfo().getUserId();
        	}
        	
        	
        	
        	DLCoreMBusSubscriptionChange data = new DLCoreMBusSubscriptionChange();
            data.setAccId(xsubNew.getAccountId());
            data.setbMarket(xsubNew.getbMarket());
            data.setbSubMarket(xsubNew.getbSubMarket());
            data.setCity(city);
            data.setCountry(country);

            String 	date = XanbooUtil.getGMTDateTime(xsubNew.getDateCreated());
            data.setDateCreated(date);

            data.setDomain(xCaller.getDomain());
            data.setExtAccId(xsub.getExtAccountId());
            data.setGatewayGUID(xsubNew.getGguid());
            logger.info("Subs update - existing hwid ="+ xsubOld.getHwId() + " new hwid="+ xsubNew.getHwId());
            data.setHwId(xsubOld.getHwId());     // old hwid to fetch subscription
            data.setHwIdNew(xsubNew.getHwId());  // new hwid to update if changed from old hwid
            
            data.setHwIdNew(hwIdNew);
            data.setSrcAppId("SysAdminManagerEJB.updateSubscription");
            data.setSrcOriginatorId(userId + "");
            data.setState(state);
            data.setStreet1(street1);
            data.setStreet2(street2);
            data.setSubsFeatures(xsubNew.getSubsFeatures());
            data.setSubsFirstName(subsFName);

            data.setSubsId(xsubNew.getSubsId());
            data.setSubsLastName(subsLName);
            data.setTimeZone(xsubNew.getTzone());
            data.setZip(zip);
            data.setZip4(zip4);

            data.setSubsFlags(xsubNew.getSubsFlags());
            
        	data.setInstallType(xsubNew.getInstallType());//DLDP 2679
        	data.setSubsClass(xsub.getSubscriptionClass());//DLDP 3056
        	
        	if(logger.isDebugEnabled()) {
                StringBuffer sb = new StringBuffer("[MBus : publish messages : SysAdminManagerEJB.updateSubscription()]:");
                sb.append("\n  : " + data.toString() );
                logger.debug( sb.toString() );
            }
        	 
        	MBusSynchronizer.updateSubscription(data);

        }
        // MBus end
    }
    
}

    private Boolean checkNotEqual(List<String> lstOrigSubFeature, List<String> lstFinalSubFeature){
    	Boolean result = false;
    	for (String string : lstOrigSubFeature) {
			if(!lstFinalSubFeature.contains(string)){
				result = true;
				break;
			}
		}
    	
    	return result;
    }
    
    private List<String> getRemovedSubFeature(List<String> lstOrigSubFeature, List<String> lstFinalSubFeature){
    	List<String> featureRemoved = new ArrayList<String>();
    	for (String string : lstOrigSubFeature) {
			if(!lstFinalSubFeature.contains(string)){
				featureRemoved.add(string);
			}
		}
    	
    	return featureRemoved;
    }

private void sbnInstalationAlarmDelay(int alarmDelay, XanbooSubscription xsub2,
		SBNSynchronizer sbn) throws XanbooException {
	
	if (alarmDelay >= 0) {
		if (logger.isDebugEnabled())

			logger.debug("DLDP3043 : setInstallationAlarmDelay : ");
		boolean sbnOK = sbn.setInstallationAlarmDelay(
				xsub2.getSubsId(), xsub2.getHwId(), alarmDelay);
		if (!sbnOK) { // SBN sync failed!
			throw new XanbooException(21420,
					"Failed to set subscription alarm delay timer. SBN synchronization failed.");
		}
	}
}


private boolean isNull(String text) {
	if(text == null || text.trim().length() == 0 ) return true;
	return false;
}


private boolean isCancelled(int subsFlags ) {
    if(subsFlags==-9997) return true;
    if(subsFlags>0 &&  (subsFlags & 3)==0) return true;     //check low 2 bits, ignore mask

            
    return false;
}


private boolean isSubsStatusChange(XanbooSubscription xsubOld,
		XanbooSubscription xsubNew) {

	
			int subsFlagOld = xsubOld.getSubsFlags();
			
			int subsFlagNew = xsubNew.getSubsFlags();

			if(subsFlagNew>0 && subsFlagOld >0 && (subsFlagNew & 3)==(subsFlagOld & 3)) 

				return false;

	
	return true;
}


	private boolean isSubsStatusChangeToActiveFromCancel(XanbooSubscription xsubOld,
			XanbooSubscription xsubNew) {

		if (xsubNew != null && xsubNew.isActive()) {
			if (xsubOld != null) {
				int subsFlagOld =  xsubOld.getSubsFlags();

				if (isCancelled(subsFlagOld))

					return true;

			}
		}
		return false;
	}


private boolean checkIfSubsMonTypeIsDIffFromDB(XanbooSubscription xsubOld ,
		XanbooSubscription xsubNew) {
	if(xsubOld != null  && xsubNew != null)
	{
		int subsFlagOld =     xsubOld.getSubsFlags();
     
		int subsFlagNew =   xsubNew.getSubsFlags();
   
		SubscriptionMonitorStatus oldStatus =	getSubMonitorType(subsFlagOld);
		SubscriptionMonitorStatus newStatus =	getSubMonitorType(subsFlagNew);
		
		if(oldStatus != null && newStatus != null && newStatus.equals(oldStatus)){
			return false;
		}
         
	}
	return true;
}


	private void fullSyncWithSBN(XanbooSubscription newxsub,
			SBNSynchronizer sbn, XanbooResultSet deviceList,
			SubscriptionMonitorStatus sms, String hwIdNew,
		 XanbooNotificationProfile[] xnpArray,XanbooSubscription xsubold) throws XanbooException {
		try {
			if (logger.isDebugEnabled()) {

				logger.debug("DLDP3043 : in synhronizeSubscriptionWithSBN : newsub : "
						+ newxsub.toString() 
						+ " : \n deviceListSize : "
						+ deviceList.size()
						+ " : \n : SubscriptionMonitorStatus : "
						+ sms
						+ " : \n : hwIdNew : "
						+ hwIdNew
					
						+ " : \n : xnpArray : " + xnpArray);
			}

			// update

		

				if (!sbn.checkIfSubscriptionExists(newxsub.getSubsId(),
						newxsub.getHwId(), newxsub.getGguid())) {

					// add subscription

					sbnNewSubscription(newxsub, sbn);

				}

				String gguid = newxsub.getGguid();

				sbnSyncEmergencyContacts(sbn, xnpArray, gguid);

				sbnNewDeviceRegistered(newxsub, sbn, deviceList);

			

				if(sms != null){
					boolean sbnOK = sbn.updateInstallationMonStatus(newxsub.getGguid(), sms.getSBNStatusValue());

					if (logger.isDebugEnabled()) {
		
						logger.debug(sms.getSBNStatusValue()
								+ " : installationMonStatus :  DLDP3043 : updateInstallationMonStatus in SBN : result : "
								+ sbnOK);
					}
					if (!sbnOK) { // SBN sync failed!
						throw new XanbooException(21420,
								"Failed to updateInstallationMonStatus. SBN synchronization failed.");
					}
				}	

			sbnUpdateInstallation(newxsub, sbn, hwIdNew);
			sbnUpdateInstallationMonitorStatus(newxsub, sbn, sms, xsubold);
			sbnInstalationAlarmDelay(newxsub.getAlarmDelay(), newxsub, sbn);

		} catch (Exception e) {
			logger.error("DLDP3043 : Error addUpdateSubscription in SBN ", e);
			throw new XanbooException(21420,
					"Failed to addUpdateSubscription. SBN synchronization failed.");
		}
	}


	private void sbnUpdateInstallationMonitorStatus(XanbooSubscription newxsub,
			SBNSynchronizer sbn,
			SubscriptionMonitorStatus sms, XanbooSubscription xsubOld) throws XanbooException {
		String installationMonStatus = null;
		String subsClass = newxsub.getSubscriptionClass();
		// service is being suspended/restored (not syncing SBN on when
		// subsFlags>0 values!!! - not a valid scenario, but problem!)
	

			// if service status is being changed
		// different from DB
		Boolean subFeatureChange = false;
		
		String subFeatureOld = xsubOld.getSubsFeatures();
		String subFeatureNew = newxsub.getSubsFeatures();
		
		if(subFeatureOld == null && subFeatureNew==null){
			subFeatureChange = false;
		}
		
		
		if(((subFeatureOld!=null && subFeatureOld.trim().length()>0) && subFeatureNew==null) || 
				(subFeatureOld == null && subFeatureNew != null && subFeatureNew.trim().length()>0) ){
			subFeatureChange = true;
		}
		
		
		if(subFeatureOld!=null && subFeatureNew!=null){
			if(subFeatureOld.trim().equalsIgnoreCase(subFeatureNew.trim())){
				subFeatureChange = false;
			}
			else			
				subFeatureChange = true;					
		}
			
		logger.info("subFeatureChange="+ subFeatureChange);
		
		if(isSubsStatusChange(xsubOld,newxsub) || (subFeatureChange) ||  (sms != null && sms.getSBNStatusValue() != null)){

				if (logger.isDebugEnabled())

					logger.debug("DLDP3043 : updateInstallationMonStatus : ");

				if (newxsub.isSuspended()) { // dl service status bits: 01
												// or 10 --> suspend service
					if (newxsub.getGguid() != null) {
						// read current subs flags and check if subs is
						// cancelled

						int currSubsFlags = -1;
						currSubsFlags = xsubOld.getSubsFlags();

						if (currSubsFlags > 0 && (currSubsFlags & 3) != 0) { // subs
																				// not
																				// cancelled,
																				// allow
																				// suspension
																				// in
																				// SBN
							if (newxsub.getGguid().indexOf("DL-") == -1) { // if
																			// there
																			// is
																			// a
																			// real
																			// gateway!,
																			// set
																			// to
																			// suspended
								// sbn.updateInstallationMonStatus(newxsub.getGguid(),
								// SBNSynchronizer.MONSTATUS_SUSPENDED);
								installationMonStatus = SBNSynchronizer.MONSTATUS_SUSPENDED;
							} else { // if there is NO real gateway!, set to
										// pending state
								// sbn.updateInstallationMonStatus(newxsub.getGguid(),
								// SBNSynchronizer.MONSTATUS_PENDING);
								installationMonStatus = SBNSynchronizer.MONSTATUS_PENDING;
							}
						}
					}
				} else if (newxsub.isActive()) { // dl service status bits
													// to 11 -->
													// activate/restore
													// service
					// allow SBN restore, only if there is a real gateway!
					if (newxsub.getGguid() != null) {
						if (newxsub.getGguid().indexOf("DL-") == -1) { // if
																		// there
																		// is
																		// a
																		// real
																		// gateway!,
																		// set
																		// to
																		// activate
							// sbn.updateInstallationMonStatus(newxsub.getGguid(),
							// SBNSynchronizer.MONSTATUS_ACTIVE);

							// should be based on monitoring type
							// installationMonStatus = SBNSynchronizer.MONSTATUS_ACTIVE;
							
							installationMonStatus = getSubMonitorType(newxsub.getSubsFlags()).getSBNStatusValue();
							
							Boolean addKeyHolder = false;
					    	if (newxsub.getSubsFeatures() != null) {
					    		addKeyHolder = newxsub.getSubsFeatures().contains(GlobalNames.FEATURE_KEY_HOLDER);			
							}
					    	boolean pos4 = XanbooUtil.isBitOn(newxsub.getSubsFlags(), 4);
							boolean pos5 = XanbooUtil.isBitOn(newxsub.getSubsFlags(),5);
							// For SM and NM if no keyholder then set NMON
					    	if(!addKeyHolder && ((pos5 && !pos4 ) || (!pos5 && !pos4 )) ){
					    		installationMonStatus =  SBNSynchronizer.MONSTATUS_NO_MONITORED;
						    }
					    	
					    	if(subsClass !=null && subsClass.equalsIgnoreCase(GlobalNames.DLLITE)){
					    		installationMonStatus =  SBNSynchronizer.MONSTATUS_MONITORED;
					    	}

						} else { // if there is NO real gateway!, set to
									// pending state
							// sbn.updateInstallationMonStatus(newxsub.getGguid(),
							// SBNSynchronizer.MONSTATUS_PENDING);

							installationMonStatus = SBNSynchronizer.MONSTATUS_PENDING;
						}

					}

				}
			
				// override if Monitoring Type is being changed, but only if gateway is already installed - status is not pending
				if(subsClass !=null && subsClass.equalsIgnoreCase(GlobalNames.DLSEC)){
					if (( (installationMonStatus != SBNSynchronizer.MONSTATUS_PENDING) && (installationMonStatus != SBNSynchronizer.MONSTATUS_NO_MONITORED)) && sms != null && sms.getSBNStatusValue() != null) {
						 installationMonStatus = sms.getSBNStatusValue();
					}
				}
		

		if (installationMonStatus != null) {
			boolean sbnOK = sbn.updateInstallationMonStatus(
					newxsub.getGguid(), installationMonStatus);

			if (logger.isDebugEnabled()) {

				logger.debug(installationMonStatus
						+ " : installationMonStatus :  DLDP3043 : updateInstallationMonStatus in SBN : result : "
						+ sbnOK);
			}
			if (!sbnOK) { // SBN sync failed!
				throw new XanbooException(21420,
						"Failed to updateInstallationMonStatus. SBN synchronization failed.");
			}
		}
		
		}
	}


	private void sbnUpdateInstallation(XanbooSubscription newxsub,
			SBNSynchronizer sbn, String hwIdNew) throws XanbooException {
		// if hwid, name/address or alarmpass is being updated
		if (hwIdNew != null
				|| newxsub.getTzone() != null
				|| newxsub.getAlarmCode() != null
				|| (newxsub.getSubsInfo() != null && newxsub.getSubsInfo()
						.hasAnyData()) || newxsub.getSubsFeatures()!=null) {
			if (logger.isDebugEnabled())

				logger.debug("DLDP3043 : updateInstallation : ");
			boolean sbnOK = sbn.updateInstallation(newxsub.getSubsId(),
					newxsub.getHwId(), newxsub.getGguid(), hwIdNew, null,
					newxsub.getTzone(), newxsub.getAlarmCode(),
					newxsub.getSubsInfo(), newxsub.getSubsFlags(), newxsub.getSubsFeatures(),newxsub.getSubscriptionClass());
			// boolean sbnOK = sbn.updateInstallation(xsub.getGguid(),
			// hwIdNew, null, tzName, alarmPass, subsInfo);
			if (!sbnOK) { // SBN sync failed!
				throw new XanbooException(21420,
						"Failed to add/update subscription. SBN synchronization failed.");
			}

		}
	}


	private void sbnNewSubscription(XanbooSubscription newxsub,
			SBNSynchronizer sbn) throws XanbooException {
		
		
		
		boolean sbnOK = sbn.newSubscription(newxsub,
				newxsub.getGguid());

		if (logger.isDebugEnabled()) {

			logger.debug("DLDP3043 : Adding New Subscription in SBN : result : "
					+ sbnOK);
		}
		if (!sbnOK) { // SBN sync failed!
			throw new XanbooException(21420,
					"Failed to add/update subscription. SBN synchronization failed.");
		}
	}


	private void sbnSyncEmergencyContacts(SBNSynchronizer sbn,
			XanbooNotificationProfile[] xnpArray, String gguid)
			throws XanbooException {
		//delete existing 
		for(int i=1;i<6;i++) {
			try {
				boolean sbnOK = sbn.deleteEmergencyContact(gguid, i);
				
				if (!sbnOK) { // SBN sync failed!
					logger.error("Failed to deleteEmergencyContact . SBN synchronization  : gguid : " +gguid + " : Seq : " + i);
				}

			} catch (Exception e) {
				logger.error("Failed to deleteEmergencyContact . SBN synchronization  : gguid : " +gguid + " : Seq : " + i);
				
			}
		}
		
		
		//add
		if (xnpArray != null && xnpArray.length > 0) {

		
			for (XanbooNotificationProfile xnp : xnpArray) {

				if (xnp != null && xnp.isValidEmergencyContact()
						&& xnp.getGguid() != null && gguid != null
						&& gguid.equals(xnp.getGguid().trim())) { // add
																	// the
																	// check
																	// to
																	// see
																	// if
																	// the
																	// GGUID's
																	// are
																	// same
																	// and
																	// addUpdateEmergencyContact
					if (logger.isDebugEnabled())

						logger.debug("DLDP3043 : addUpdateEmergencyContact : "
								+ xnp);
					boolean sbnOK = sbn.addUpdateEmergencyContact(xnp);
					if (!sbnOK) { // SBN sync failed!
						logger.error(
								"Failed to update notification profile. SBN synchronization failed : xnp : " +xnp);
					}

				}
			}
		}
	}


	private void sbnNewDeviceRegistered(XanbooSubscription newxsub,
			SBNSynchronizer sbn, XanbooResultSet deviceList)
			throws XanbooException {
		if (deviceList != null && deviceList.size() > 0) {
			for (int i = 0; deviceList != null && i < deviceList.size(); i++) {
				String dguid = null;

				dguid = (String) deviceList.getElementString(i,
						"DEVICE_GUID");
				String catalogId = (String) deviceList
						.getElementString(i, "CATALOG_ID");
				String dLabel = (String) deviceList.getElementString(i,
						"LABEL");

				boolean sbnOK = sbn.newDeviceRegistered(
						newxsub.getGguid(), dguid, catalogId, dLabel);

				if (logger.isDebugEnabled()) {

					logger.debug("DLDP3043 : Add/Update Device in SBN : dguid : "
							+ dguid);
				}

				if (!sbnOK) { // SBN sync failed!
					logger.error(
							"Failed to Add/Update Device. SBN synchronization for device failed : dguid : " + dguid );
				}

			}
		}
	}

private boolean isEligibleForSBNFullSync(SubscriptionMonitorStatus sms) {
	if (sms != null  && SubscriptionMonitorStatus.PM.equals(sms) ) {
		return true;
	}
	return false;
}
	

	public void updateSubscription(XanbooAdminPrincipal xCaller, long accountId, String subsId, String hwId, int subsFlags, String hwIdNew, String label, String tzName,
            String masterPin, String masterDuress, String alarmPass, XanbooContact subsInfo, int alarmDelay, int tcFlag) throws XanbooException {

        XanbooSubscription xsub = new XanbooSubscription();
        xsub.setAccountId(accountId);
        xsub.setSubsId(subsId);
        xsub.setHwId(hwId);
        xsub.setSubsFlags(subsFlags);
        xsub.setLabel(label);
        xsub.setTzone(tzName);
        xsub.setDisarmPin(masterPin);
        xsub.setDuressPin(masterDuress);
        xsub.setAlarmCode(alarmPass);
        xsub.setSubsInfo(subsInfo);
        xsub.setSubsFeatures(null);
        
        updateSubscription(xCaller, xsub, hwIdNew, alarmDelay, tcFlag);
        
    }


	public void cancelSubscription(XanbooAdminPrincipal xCaller, long accountId, String extAccountId, String subsId, String hwId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[cancelSubscription()]: accountId="+accountId+", extAccountId="+extAccountId+", subsId="+subsId+", hwId="+hwId);
        }

        // first validate the input parameters
        if((accountId<=0 && extAccountId==null) || subsId==null || hwId==null || subsId.trim().length()==0 || hwId.trim().length()==0) {
            throw new XanbooException(10050);
        }

        boolean doMBusSync = true; //MBus 
        XanbooSubscription xsub =null;
        XanbooAdminPrincipal xap=new XanbooAdminPrincipal(GlobalNames.DEFAULT_APP_USER, 0, 0); 
        Connection conn=null;
        boolean rollback=false;
        Boolean accountDelete = false;
        try {
            checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            
            if(accountId<=0) {
                try {
                    XanbooResultSet rs = getAccount( xCaller, extAccountId );
                    if( rs == null || rs.size() > 0 ) {
                        accountId = rs.getElementLong(0, "ACCOUNT_ID");    //getting existing account id
                    }
                }catch(Exception ee) {
                    accountId = -1;
                } //ignore exception by lookup account id by extAcctId
            }
            
            if(accountId<=0) {  //could not locate account by extAcctId
            	doMBusSync = false;
                throw new XanbooException(21405);
            }
            
            //cancel subscription and get the gguid for the subsciption
            xsub = new XanbooSubscription(accountId,extAccountId);
            xsub.setSubsId(subsId);
            xsub.setHwId(hwId);
            
            xsub = dao.cancelSubscription(conn, xCaller, xsub);
            
         /*   Boolean accountDelete = false;
            // Added for Vanfullfilment 
            if(subsId.equalsIgnoreCase(hwId)){ 
            	XanbooResultSet xrs= dao.getSubscription(conn, xCaller, accountId, null, null);            	
            	dao.deleteSubscription(conn, xCaller, xsub);
            	
            	if(xrs.size() ==1){
            		accountDelete=true;
            		XanbooAccount xAccount = new XanbooAccount();
            		xAccount.setAccountId(accountId);
            		xAccount.setStatus(XanbooAccount.STATUS_CANCELLED);
            		xAccount.setDomain(xCaller.getDomain());
            		updateAccount(xCaller, xAccount);
            		dao.deleteAccount(conn, xCaller, accountId);
            	}
            }
           
            
            if ( logger.isDebugEnabled() )
                logger.debug("[cancelSubscription()]: - canceling subsId="+subsId+", hwId="+hwId+", subsClass="+xsub.getSubscriptionClass()+", features="+xsub.getSubsFeatures());
            */
            
            //sync with SBN and send cancellation email
            if(GlobalNames.MODULE_SBN_ENABLED) {
                SBNSynchronizer sbn = new SBNSynchronizer();
                // Added for Vanfullfilment  -- Update the IMEI in SBN with BAN# and [ then send the Cancellation to SBN]  
                if(subsId.equalsIgnoreCase(hwId)){ 
                	sbn.updateInstallation(subsId, hwId, xsub.getGguid(), xsub.getExtAccountId(), null, null, null, null, -1, null, xsub.getSubscriptionClass());
                }
                
                boolean sbnOK = sbn.updateInstallationMonStatus(xsub.getGguid(), SBNSynchronizer.MONSTATUS_CANCELLED);
                
                if(!sbnOK) {    //SBN sync failed!
                	doMBusSync = false;
                    throw new XanbooException(21420, "Failed to cancel subscription. SBN synchronization failed.");
                }

                //send cancellation email for DL domain - better way?
                if(GlobalNames.MODULE_CSI_ENABLED && xCaller.getDomain()!=null && xCaller.getDomain().equals("DL")) {
                    //first retrieve account users and locate master user
                    XanbooResultSet users = getUserList(xCaller, accountId);    //!!!!change SP to do join and return user fname,lname and email!!!
                    if(users!=null && users.size()>0) {
                        for(int i=0; i<users.size(); i++) {
                            String isMaster = users.getElementString(i, "IS_MASTER");
                            if(isMaster!=null && isMaster.equals("1")) {    //send notif to master user email
                                String em = users.getElementString(i, "EMAIL");
                                if(em!=null) {  //cant check (em.endsWith("@CSI")) !
                                    String fn = users.getElementString(i, "FIRSTNAME");
                                    String ln = users.getElementString(i, "LASTNAME");
                                    int rc = 1;
                                    SimpleACSIClient acsi = new SimpleACSIClient();
                                    if(GlobalNames.DLLITE.equalsIgnoreCase(xsub.getSubscriptionClass())) {
                                    	rc = acsi.sendCancelNotification(extAccountId, subsId, ((ln!=null && ln.length()>0) ? fn+" "+ln : null), em, null, GlobalNames.DLLITE);
                                    } else {
                                    	rc = acsi.sendCancelNotification(extAccountId, subsId, ((ln!=null && ln.length()>0) ? fn+" "+ln : null), em, null);
                                    }
                                    if(rc==0) {
                                       logger.info("[cancelSubscription()]: ACSI CANCEL NOTIFICATION SENT to:" + em + ", acc:" + accountId + ", subid:" + subsId + ", subsType=" + xsub.getSubscriptionClass());
                                    } else {
                                       logger.warn("[cancelSubscription()]: ACSI CANCEL NOTIFICATION FAILED to:" + em + ", acc:" + accountId + ", subid:" + subsId + ", subsType=" + xsub.getSubscriptionClass() + ", RC:" + rc);
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
            
          /*  //MBus Start- 
            if(GlobalNames.MBUS_SYNC_ENABLED && doMBusSync) {
            	DLCoreMBusSubscriptionChange data = new DLCoreMBusSubscriptionChange();
                data.setAccId(xsub.getAccountId());
                String 	date = XanbooUtil.getGMTDateTime(xsub.getDateCreated());
                data.setDateCreated(date);
                data.setDomain(xCaller.getDomain());
                data.setExtAccId(xsub.getExtAccountId());
                data.setGatewayGUID(xsub.getGguid());
                data.setSubsId(xsub.getSubsId());
                data.setHwId(xsub.getHwId());
                data.setSrcAppId("SysAdminManagerEJB.cancelSubscription");
                data.setSrcOriginatorId(xCaller.getUsername());
                data.setSubsFlags(xsub.getSubsFlags());
                
            	data.setInstallType(xsub.getInstallType());//DLDP 2679

          	if(logger.isDebugEnabled()) {
                    StringBuffer sb = new StringBuffer("[MBus : publish messages : SysAdminManagerEJB.cancelSubscription()]:");
                    sb.append("\n  : " + data.toString() );
                    logger.debug( sb.toString() );
                }
          	logger.info("cancel subscription - "+ xsub.getSubsId() + " hwid = "+ xsub.getHwId());
          	if(xsub.getSubsId() != null && xsub.getHwId() != null){
            	 
            	MBusSynchronizer.updateSubscription(data);
          	}
            	
            	if(accountDelete){
            		DLCoreMBusAccountChange data1 = new DLCoreMBusAccountChange();
            		data1.setAccId(xsub.getAccountId());
            		data1.setSrcAppId("SysAdminManagerEJB.AccountDelete");            		
            		data1.setDomain(xCaller.getDomain());
            		data1.setSrcOriginatorId(xCaller.getUsername());            		
            		data1.setExtAccId(xsub.getExtAccountId());      
            		MBusSynchronizer.deleteAccount(data1);
            	}
            }*/
            
            /*//sending cancellation signal to RMS. Just log errors/warnings and continue.
            if(GlobalNames.MODULE_PH_ENABLED || GlobalNames.MODULE_RMS_ENABLED) {
            	String rmsphEnabled = "RMS";            	
            	if(GlobalNames.MODULE_PH_ENABLED){
            		rmsphEnabled = "PH";
            	}
            	
                try{
                    RMSClient.deleteDLC(hwId, xCaller.getDomain(),rmsphEnabled);
                }
                catch(XanbooException rmse){
                    if(logger.isDebugEnabled())
                        logger.warn("[cancelSubscription()]: Failed to remove subscription hardware in RMS. CTN:" + subsId + ", IMEI:"+ hwId + ", Exception:" + rmse.getErrorMessage(), rmse);
                    else
                        logger.warn("[cancelSubscription()]: Failed to remove subscription hardware in RMS. CTN:" + subsId + ", IMEI:"+ hwId + ", Exception:" + rmse.getErrorMessage());
                }catch (Exception rmse) {
                    if(logger.isDebugEnabled())
                        logger.warn("[cancelSubscription()]: Failed to remove subscription hardware in RMS. CTN:" + subsId + ", IMEI:"+ hwId + ", Exception:" + rmse.getMessage(), rmse);
                    else
                        logger.warn("[cancelSubscription()]: Failed to remove subscription hardware in RMS. CTN:" + subsId + ", IMEI:"+ hwId + ", Exception:" + rmse.getMessage());
                }
            } 
             sync with ACSI 
            if(GlobalNames.MODULE_CSI_ENABLED) {
	            SimpleACSIClient acsi = new SimpleACSIClient();
	            int rc = acsi.deviceRemoved(extAccountId, 
	            							subsId, 
	            							xsub.getGguid(), 
	            							"0",
	            							null );
            }*/
        }catch (XanbooException xe) {
        	doMBusSync = false;
            rollback=true;
            throw xe;
        }catch (Exception e) {
        	doMBusSync = false;
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[cancelSubscription()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[cancelSubscription()]: Exception:" + e.getMessage());
            }
            throw new XanbooException(10030, e.getMessage());
        }finally {            
            if (GlobalNames.DLA_AUTHENTICATE_VIA_SERVICE && !rollback) {
            	invalidateDlaAuthServiceCache(xsub.getAccountId());
            }
            
            if(!doMBusSync) {
            	dao.closeConnection(conn, rollback);
            }
            if(doMBusSync){
            	
	            // Added for Vanfullfilment 
	            if(subsId.equalsIgnoreCase(hwId)){ 
	            	XanbooResultSet xrs= dao.getSubscription(conn, xCaller, accountId, null, null);            	
	            	dao.deleteSubscription(conn, xCaller, xsub);
	            	
	            	if(xrs.size() ==1){
	            		accountDelete=true;
	            		XanbooAccount xAccount = new XanbooAccount();
	            		xAccount.setAccountId(accountId);
	            		xAccount.setStatus(XanbooAccount.STATUS_CANCELLED);
	            		xAccount.setDomain(xCaller.getDomain());
	            		updateAccount(xCaller, xAccount);
	            		dao.deleteAccount(conn, xCaller, accountId);
	            	}
	            }
           
	            dao.closeConnection(conn, rollback);
	            if ( logger.isDebugEnabled() )
	                logger.debug("[cancelSubscription()]: - canceling subsId="+subsId+", hwId="+hwId+", subsClass="+xsub.getSubscriptionClass()+", features="+xsub.getSubsFeatures());
            
            
	            //MBus Start- 
	            if(GlobalNames.MBUS_SYNC_ENABLED ) {
	            	DLCoreMBusSubscriptionChange data = new DLCoreMBusSubscriptionChange();
	                data.setAccId(xsub.getAccountId());
	                String 	date = XanbooUtil.getGMTDateTime(xsub.getDateCreated());
	                data.setDateCreated(date);
	                data.setDomain(xCaller.getDomain());
	                data.setExtAccId(xsub.getExtAccountId());
	                data.setGatewayGUID(xsub.getGguid());
	                data.setSubsId(xsub.getSubsId());
	                data.setHwId(xsub.getHwId());
	                data.setSrcAppId("SysAdminManagerEJB.cancelSubscription");
	                data.setSrcOriginatorId(xCaller.getUsername());
	                data.setSubsFlags(xsub.getSubsFlags());
	                
	            	data.setInstallType(xsub.getInstallType());//DLDP 2679
	
	          	if(logger.isDebugEnabled()) {
	                    StringBuffer sb = new StringBuffer("[MBus : publish messages : SysAdminManagerEJB.cancelSubscription()]:");
	                    sb.append("\n  : " + data.toString() );
	                    logger.debug( sb.toString() );
	                }
	          	logger.info("cancel subscription - "+ xsub.getSubsId() + " hwid = "+ xsub.getHwId());
	          	if(xsub.getSubsId() != null && xsub.getHwId() != null){	            	 
	            	MBusSynchronizer.updateSubscription(data);
	          	}
	            	
	            	if(accountDelete){
	            		DLCoreMBusAccountChange data1 = new DLCoreMBusAccountChange();
	            		data1.setAccId(xsub.getAccountId());
	            		data1.setSrcAppId("SysAdminManagerEJB.AccountDelete");            		
	            		data1.setDomain(xCaller.getDomain());
	            		data1.setSrcOriginatorId(xCaller.getUsername());            		
	            		data1.setExtAccId(xsub.getExtAccountId());      
	            		MBusSynchronizer.deleteAccount(data1);
	            	}
	            }
	          //sending cancellation signal to RMS. Just log errors/warnings and continue.
	            if(GlobalNames.MODULE_PH_ENABLED || GlobalNames.MODULE_RMS_ENABLED) {
	            	String rmsphEnabled = "RMS";            	
	            	if(GlobalNames.MODULE_PH_ENABLED){
	            		rmsphEnabled = "PH";
	            	}
	            	
	                try{
	                    RMSClient.deleteDLC(hwId, xCaller.getDomain(),rmsphEnabled);
	                }
	                catch(XanbooException rmse){
	                    if(logger.isDebugEnabled())
	                        logger.warn("[cancelSubscription()]: Failed to remove subscription hardware in RMS. CTN:" + subsId + ", IMEI:"+ hwId + ", Exception:" + rmse.getErrorMessage(), rmse);
	                    else
	                        logger.warn("[cancelSubscription()]: Failed to remove subscription hardware in RMS. CTN:" + subsId + ", IMEI:"+ hwId + ", Exception:" + rmse.getErrorMessage());
	                }catch (Exception rmse) {
	                    if(logger.isDebugEnabled())
	                        logger.warn("[cancelSubscription()]: Failed to remove subscription hardware in RMS. CTN:" + subsId + ", IMEI:"+ hwId + ", Exception:" + rmse.getMessage(), rmse);
	                    else
	                        logger.warn("[cancelSubscription()]: Failed to remove subscription hardware in RMS. CTN:" + subsId + ", IMEI:"+ hwId + ", Exception:" + rmse.getMessage());
	                }
	            } 
	            /* sync with ACSI */
	            if(GlobalNames.MODULE_CSI_ENABLED) {
		            SimpleACSIClient acsi = new SimpleACSIClient();
		            int rc = acsi.deviceRemoved(extAccountId, 
		            							subsId, 
		            							xsub.getGguid(), 
		            							"0",
		            							null );
	            }
            }
        }
        //delete the external service subscriptions
        try {
        	if ( xsub.getSubscriptionClass() != null && xsub.getSubscriptionClass().equals(GlobalNames.DLLITE) 
                    && xsub.getSubsFeatures() != null && !xsub.getSubsFeatures().equalsIgnoreCase(""))
               {
                  
                      soProxy = (com.xanboo.core.extservices.outbound.ServiceOutboundProxy)getEJB(GlobalNames.EJB_OUTBOUND_PROXY);
                   //List<ServiceSubscription> serviceSubscriptions = dao.getAccountSubscriptionSvcs(conn, accountId, null);
               	Object obj = getEJB(GlobalNames.EJB_EXTSERVICE_MANAGER);
               	if(obj!=null && obj instanceof ServiceManager) {
    					ServiceManager ejb = (ServiceManager)obj;
    					if(xap.getAccountPrincipal() == null) {
    						// set account principal to pass caller account info.
    						XanbooPrincipal xp = new XanbooPrincipal(GlobalNames.DEFAULT_APP_USER, accountId, 0);
    						xp.setDomain(xap.getDomain());
        					xap.setAccountPrincipal(xp); 
    					} else {
    						xap.getAccountPrincipal().setAccountId(accountId);
    					    if (StringUtils.isBlank(xap.getAccountPrincipal().getUsername())) xap.getAccountPrincipal().setUsername(GlobalNames.DEFAULT_APP_USER); 
    					    if (xap.getAccountPrincipal().getUserId() < 0) xap.getAccountPrincipal().setUserId(0); 
    					}
    					
    					List <ServiceSubscription> servicesubs = ejb.getServiceSubscription(xap.getAccountPrincipal(), null);
    					if(servicesubs != null && servicesubs.size() > 0 ){
    						for ( ServiceSubscription svcSubscription : servicesubs )
    		                {
    		                    if(svcSubscription.getGguid() != null && xsub.getGguid() != null && (svcSubscription.getGguid().equals(xsub.getGguid()))){
    								if ( logger.isDebugEnabled() )
    			                        logger.debug("[cancelSubscription()]: - xCaller="+xCaller+", subsId="+subsId+" cancel service subscription "+svcSubscription);
    			                    try
    			                    {
    			                    	soProxy.cancelServiceSubscription(xap, svcSubscription);
    			                    }
    			                    catch(XanbooException xe)
    			                    {
    			                        if ( xe.getCode() == 10005 )
    			                        {
    			                            logger.warn("[cancelSubscription()] - Critial error when attempting to cancel a subscription, code=10005",xe);
    			                            throw xe;
    			                        }
    			                    }
    			                    catch(Exception ex)
    			                    {
    			                       logger.warn("[cancelSubscription()]: - Exception canceling external service subscription for "+svcSubscription.getServiceId()+" on account="+accountId, ex);
    			                    }
    			                }
    		                }
    					}
    					
    				}
               }
        	   //delete the virtual gateway even if features are null
        	   if ( !accountDelete && xsub.getSubscriptionClass() != null && xsub.getSubscriptionClass().equals(GlobalNames.DLLITE) ){
		        	 //delete the virtual gateway if it is a DLITE subscription and sync with SBN and notify mbus.
		            logger.debug("Accont id:"+ accountId+"Subs ID : "+subsId+"HW ID : "+hwId);
		            deleteVirtualGateway(xCaller,accountId,xsub.getGguid(),"0");
		            //once gateway is successfully deleted ,delete the subscription.
		            deleteSubscription(xCaller, accountId,subsId, hwId);
		            logger.debug("Sucessfully deleted subscription with Account id:"+accountId+"Subs ID : "+subsId+"HW ID : "+hwId);
        	   }
        	
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[cancelSubscription()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[cancelSubscription()]: Exception:" + e.getMessage());
            }
            throw new XanbooException(10030, e.getMessage());
        }
        

    }
    
    public void deleteSubscription(XanbooAdminPrincipal xCaller, long accountId, String subsId, String hwId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteSubscription()]: accountId="+accountId+", subsId="+subsId+", hwId="+hwId);
        }

        // first validate the input parameters
        if((accountId<=0 ) || subsId==null || hwId==null || subsId.trim().length()==0 || hwId.trim().length()==0) {
            throw new XanbooException(10050);
        }

        boolean doMBusSync = true; //MBus 
        
        Connection conn=null;
        boolean rollback=false;

        try {
            checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            //cancel subscription and get the gguid for the subsciption
            XanbooSubscription xsub = new XanbooSubscription(accountId,"");
            xsub.setSubsId(subsId);
            xsub.setHwId(hwId);
            XanbooResultSet xrs = dao.getSubscription(conn, xCaller, accountId, subsId, hwId);
            xsub = dao.deleteSubscription(conn, xCaller, xsub);
            if ( logger.isDebugEnabled() )
                logger.debug("[deleteSubscription()]: - deleting subsId="+subsId+", hwId="+hwId+", subsClass="+xsub.getSubscriptionClass()+", features="+xsub.getSubsFeatures());
            
            //MBus Start- 
            if(GlobalNames.MBUS_SYNC_ENABLED && doMBusSync && xrs.size()>0) {
            	
            	xsub = new XanbooSubscription(xrs,0);	
            	
            	DLCoreMBusSubscriptionChange data = new DLCoreMBusSubscriptionChange();
                data.setAccId(xsub.getAccountId());
                String 	date = XanbooUtil.getGMTDateTime(xsub.getDateCreated());
                data.setDateCreated(date);
                data.setDomain(xCaller.getDomain());
                data.setExtAccId(xsub.getExtAccountId());
                data.setGatewayGUID(xsub.getGguid());
                data.setSubsId(xsub.getSubsId());
                data.setHwId(xsub.getHwId());
                data.setSrcAppId("SysAdminManagerEJB.deleteSubscription");
                data.setSrcOriginatorId(xCaller.getUsername());
                data.setSubsFlags(xsub.getSubsFlags());
                
            	data.setInstallType(xsub.getInstallType());//DLDP 2679

          	if(logger.isDebugEnabled()) {
                    StringBuffer sb = new StringBuffer("[MBus : publish messages : SysAdminManagerEJB.deleteSubscription()]:");
                    sb.append("\n  : " + data.toString() );
                    logger.debug( sb.toString() );
                }
            	 
            	MBusSynchronizer.updateSubscription(data);                
            }
            //sync with SBN and send cancellation email many not be needed.
            /*
            if(GlobalNames.MODULE_SBN_ENABLED) {
                SBNSynchronizer sbn = new SBNSynchronizer();
                boolean sbnOK = sbn.updateInstallationMonStatus(xsub.getGguid(), SBNSynchronizer.MONSTATUS_CANCELLED);
                if(!sbnOK) {    //SBN sync failed!
                    throw new XanbooException(21420, "Failed to cancel subscription. SBN synchronization failed.");
                }
            }
            */
  
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[deleteSubscription()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[deleteSubscription()]: Exception:" + e.getMessage());
            }
            throw new XanbooException(10030, e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }


    }

    public void deleteVirtualGateway(XanbooAdminPrincipal xCaller, long accountId, String gatewayGUID, String deviceGUID) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteVirtualGateway()]: ");
        }

        // validate the input parameters
        if(gatewayGUID==null || gatewayGUID.trim().length()==0 || (deviceGUID!=null && deviceGUID.trim().length()==0)) {
            throw new XanbooException(10050);
        }
        
        /* CODE TO ENABLE BACKWARD COMPATIBILITY WITH THE NOTION THAT A GATEWAY'S DEVICE GUID IS THE SAME AS IT'S GATEWAY_GUID */
        if (deviceGUID != null && gatewayGUID != null && deviceGUID.equals(gatewayGUID)) {
            deviceGUID = "0";
        }
        /* END OF BACKWARD COMPATIBILITY CODE */
        
        String subsId = null;//DF#21450
        String catId = null;
        Connection conn=null;
        boolean rollback = false;
        try {
            // validate the caller and privileges
            checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            catId = dao.deleteDevice(conn, xCaller, accountId, gatewayGUID, deviceGUID);
            
        }catch (XanbooException xe) {
            rollback = true;
            throw xe;
        }catch (Exception e) {
            rollback = true;
            if(logger.isDebugEnabled()) {
              logger.error("[deleteDevice]:" + e.getMessage(), e);
            }else {
              logger.error("[deleteDevice]:" + e.getMessage());
            }             
            throw new XanbooException(10030, "[deleteDevice]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        //sync with other external entity listeners - SBN.This may not be needed.
        /*if(GlobalNames.MODULE_SBN_ENABLED) {
             SBNSynchronizer sbn = new SBNSynchronizer();
             if(!sbn.removeDevice(gatewayGUID, deviceGUID, catId, false)) {    //rollback, if it fails
                 throw new XanbooException(10030, "[deleteDevice]: Failed to remove device/zone from SBN.");
              }
        }
        */
        //DF#21450
        //sync with MBUS, if enabled -  start            
        if(GlobalNames.MBUS_SYNC_ENABLED){
        	
        	String date = XanbooUtil.getCurrentGMTDateTime();
        	if(logger.isDebugEnabled()) {
                StringBuffer sb = new StringBuffer("[MBus : publish messages : DeviceManagerEJB.deleteDevice()]:");
                sb.append("DomainId : " + xCaller.getDomain());
                sb.append("\n AccountId : " +accountId);
                sb.append("\n catId : " + catId  );
                sb.append("\n ExtAccountId : " + null );
                sb.append("\n SubId : " + subsId );
                sb.append("\n deviceGUID : " + deviceGUID);
                sb.append("\n GatewayGUID : " + gatewayGUID);
                sb.append("\n TS : " + date );
                sb.append("\n UserName  : " + xCaller.getUsername()   );

                logger.debug(sb.toString());            		
        	}
        	DLCoreMBusEntityChange data = new DLCoreMBusEntityChange();

			data.setDomain(xCaller.getDomain());

			data.setAccId(accountId);

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
			data.setSrcOriginatorId(xCaller.getUsername() );
			// data.setStatus(-1);

			data.setSw(null);

			
			data.setTs(date);

			MBusSynchronizer.deleteDevice(data);
        }//MBus -  end
        
    }  

    public XanbooResultSet getSubscription(XanbooAdminPrincipal xCaller, long accountId, String subsId, String hwId) throws XanbooException{
        if (logger.isDebugEnabled()) {
            logger.debug("[getSubscription()]:");
        }
        // first validate the input parameters. at least an account id or subsId must be provided
        if(accountId<=0 && (subsId==null || subsId.trim().length()==0) && (hwId==null || hwId.trim().length()==0)) {
            throw new XanbooException(10050);
        }

        Connection conn=null;
        try {
            // validate the caller and privileges
            //checkCallerPrivilege(xCaller);
                         
            conn=dao.getConnection();
            return dao.getSubscription(conn, xCaller, accountId, subsId, hwId);

        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.warn("[getSubscription()]" + e.getMessage(), e);
            }else {
              logger.warn("[getSubscription()]" + e.getMessage());
            }
            throw new XanbooException(10030, "[getSubscription]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
    }

    public XanbooResultSet getSubscription(XanbooAdminPrincipal xCaller, String bMarket, String state, String city, String postalCode)throws XanbooException
    {
        return null;
    }

    public XanbooSubscription[] getSubscription(XanbooAdminPrincipal xCaller, long accountId, String subsId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getSubscription()]:");
        }
        // first validate the input parameters. at least an account id or subsId must be provided
        if(accountId<=0 && (subsId==null || subsId.trim().length()==0)) {
            throw new XanbooException(10050);
        }

        Connection conn=null;
        try {
            // validate the caller and privileges
            //checkCallerPrivilege(xCaller);
                         
            conn=dao.getConnection();
            XanbooResultSet xrs = dao.getSubscription(conn, xCaller, accountId, subsId, null);
            
            XanbooSubscription[] subs = null;
            
            //convert resultset to subs objects
            if(xrs!=null && xrs.size()>0) {
                subs = new XanbooSubscription[xrs.size()];
                for(int i=0; i<xrs.size(); i++) {
                    subs[i] = new XanbooSubscription(xrs, i);
                }
            }
            
            return subs;
            

        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.warn("[getSubscription()]" + e.getMessage(), e);
            }else {
              logger.warn("[getSubscription()]" + e.getMessage());
            }
            throw new XanbooException(10030, "[getSubscription]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
        
    }
    
    
    
    public XanbooPrincipal getXanbooPrincipal(XanbooAdminPrincipal xCaller, long accountId, String subsId, String hwId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getXanbooPrincipal()]:");
        }

        // first validate the input parameters
        if(accountId<1 && subsId==null) {
            throw new XanbooException(10050);
        }

        // now check the validity of the user
        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();

            if(accountId>0) {   //an account id is provided
                return dao.getXanbooPrincipal(conn, xCaller, null, String.valueOf(accountId));

            }else {     //no account id, find the account id first
                XanbooResultSet subs = dao.getSubscription(conn, xCaller, accountId, subsId, hwId);
                if(subs==null || subs.size()==0)  { //not found
                    throw new XanbooException(27060);
                }
                HashMap sub = (HashMap) subs.get(0);
                return dao.getXanbooPrincipal(conn, xCaller, null, (String) sub.get("ACCOUNT_ID"));
            }

        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[getXanbooPrincipal]:" + e.getMessage(), e);
            }else {
              logger.error("[getXanbooPrincipal]:" + e.getMessage());
            }
            throw new XanbooException(10030);
        }finally {
            dao.closeConnection(conn, rollback);
        }
    }
    
    
    
    public void auditLog(XanbooAdminPrincipal xCaller, long accountId, String gatewayGUID, String deviceGUID, String actionSource, String actionDesc, String actionValue) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[auditLog()]: ");
        }
        
        // first validate the input parameters
        if(accountId<0) {
            throw new XanbooException(10050);
        }

        
        Connection conn=null;
        boolean rollback=false;       
        try {
            checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            dao.auditLog(conn, xCaller, accountId, gatewayGUID, deviceGUID, actionSource, actionDesc, actionValue);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[auditLog()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[auditLog()]: Exception:" + e.getMessage());
            }            
            throw new XanbooException(10030, "[auditLog]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
    }
    

    public XanbooResultSet getLocationCodeList(String domainId, String lang, int level) throws XanbooException {
        if(logger.isDebugEnabled()){
            logger.debug("[getLocationCodeList]");
        }
        
        Connection conn=null;
        
        try{
            //checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            XanbooResultSet xrs = dao.getLocationCodeList(conn, domainId, lang, level);
            if((xrs==null || xrs.size()==0) && !domainId.equals("default")) xrs = dao.getLocationCodeList(conn, "default", lang, level);
            return xrs;
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e){
            if(logger.isDebugEnabled()) {
              logger.error("[getLocationCodeList()]: " + e.getMessage(), e);
            }else {
              logger.error("[getLocationCodeList()]: " + e.getMessage());
            }                        
            throw new XanbooException(10030, "[getLocationCodeList]:" + e.getMessage());
        }finally{
            dao.closeConnection(conn);
        }
    }
    
    public void addProvisionedDevice(XanbooAdminPrincipal xCaller, long accountId, String subsId, String hwId, String classId, String subclassId, String installType, String swapGuid, int count) throws XanbooException  {
        if(logger.isDebugEnabled()){
            logger.debug("[addProvisionedDevice]");
        }
        
        // first validate the input parameters. An account id, subsId and hdId must all be provided
        if(accountId<=0 || (subsId==null || subsId.trim().length()==0) || (hwId==null || hwId.trim().length()==0)) {
            throw new XanbooException(10050);
        }
        //validate the other params
        if((classId==null || classId.trim().length()!=4) || (subclassId==null || subclassId.trim().length()!=2) || installType==null || installType.length()!=1 || (installType.charAt(0)!='A' && installType.charAt(0)!='S' && installType.charAt(0)!='P') || count<=0) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback=false;

        try {
            checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            dao.addDeleteProvisionedDevice(conn, xCaller, accountId, subsId, hwId, classId, subclassId, installType, swapGuid, count, false);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[addProvisionedDevice()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[addProvisionedDevice()]: Exception:" + e.getMessage());
            }            
            throw new XanbooException(10030, "[addProvisionedDevice]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
    }
    
    public void deleteProvisionedDevice(XanbooAdminPrincipal xCaller, long accountId, String subsId, String hwId, String classId, String subclassId, String installType, String swapGuid, int count) throws XanbooException   {
        if(logger.isDebugEnabled()){
            logger.debug("[deleteProvisionedDevice]");
        }

        // first validate the input parameters. An account id, subsId and hdId must all be provided
        if(accountId<=0 || (subsId==null || subsId.trim().length()==0) || (hwId==null || hwId.trim().length()==0)) {
            throw new XanbooException(10050);
        }
        //validate the other params
        if((classId==null || classId.trim().length()!=4) || subclassId==null || subclassId.trim().length()!=2 || installType==null || installType.length()!=1 || (installType.charAt(0)!='A' && installType.charAt(0)!='S' && installType.charAt(0)!='P') || count<=0) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback=false;

        try {
            checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            dao.addDeleteProvisionedDevice(conn, xCaller, accountId, subsId, hwId, classId, subclassId, installType, swapGuid, count, true);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[addProvisionedDevice()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[addProvisionedDevice()]: Exception:" + e.getMessage());
            }            
            throw new XanbooException(10030, "[addProvisionedDevice]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
    }
    
    public XanbooResultSet getProvisionedDeviceList(XanbooAdminPrincipal xCaller, long accountId, String subsId, String hwId, String classId, String subclassId, String installType) throws XanbooException   {
        if(logger.isDebugEnabled()){
            logger.debug("[getProvisionedDeviceList]");
        }
        
        // first validate the input parameters. at least an account id  must be provided
        if(accountId<=0 || (subsId!=null && hwId==null)) {
            throw new XanbooException(10050);
        }
        
        //validate the other params
        if(subsId==null || subsId.trim().length()==0) { //null other params if subsId is null
            subsId=null;
            hwId=null;
            classId=null;
            subclassId=null;
        }
        
        if(classId!=null && subclassId==null) { //subclass required, if class is specified
            throw new XanbooException(10050);
        }
        
        if(installType!=null && !installType.equals("A") && !installType.equals("S") && !installType.equals("P")) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        
        try{
            //checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            XanbooResultSet xrs = dao.getProvisionedDeviceList(conn, xCaller, accountId, subsId, hwId, classId, subclassId, installType);
            return xrs;
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e){
            if(logger.isDebugEnabled()) {
              logger.error("[getProvisionedDeviceList()]: " + e.getMessage(), e);
            }else {
              logger.error("[getProvisionedDeviceList()]: " + e.getMessage());
            }                        
            throw new XanbooException(10030, "[getProvisionedDeviceList]:" + e.getMessage());
        }finally{
            dao.closeConnection(conn);
        }
        
    }

    public XanbooResultSet getSupportedDeviceList(String domainId, String installType, String monType) throws XanbooException {
    	
    	 if(logger.isDebugEnabled()){
             logger.debug("[getSupportedDeviceList]");
         }
         
         // first validate the input parameters. domain id  must be provided
         if(domainId==null || domainId.trim().length()==0) {
             throw new XanbooException(10050);
         }
         
         if(installType==null) {
             installType="P";
         }else if(installType.charAt(0)!='S' && installType.charAt(0)!='P') {
             throw new XanbooException(10050);
         }
         
         Connection conn=null;
         
         try{
             //checkCallerPrivilege(xCaller);
             conn=dao.getConnection();
             XanbooResultSet xrs = dao.getSupportedDeviceList(conn, domainId, installType, monType);
             return xrs;
         }catch (XanbooException xe) {
             throw xe;
         }catch (Exception e){
             if(logger.isDebugEnabled()) {
               logger.error("[getSupportedDeviceList()]: " + e.getMessage(), e);
             }else {
               logger.error("[getSupportedDeviceList()]: " + e.getMessage());
             }                        
             throw new XanbooException(10030, "[getSupportedDeviceList]:" + e.getMessage());
         }finally{
             dao.closeConnection(conn);
         }
    }
    
     public int getAlertCount(XanbooAdminPrincipal xCaller, long accountId, String gatewayGUID) throws XanbooException {
        //if (logger.isDebugEnabled()) {
            logger.debug("[getAlertCount()]: acc=" + accountId + ", gguid=" + gatewayGUID);
        //}

        if(accountId<=0 || gatewayGUID==null || gatewayGUID.length()==0) { //subclass required, if class is specified
            throw new XanbooException(10050);
        }

        
        Connection conn=null;
        try{
            //checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            return dao.getAlertCount(conn, xCaller, accountId, gatewayGUID);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e){
            if(logger.isDebugEnabled()) {
              logger.error("[getAlertCount()]: " + e.getMessage(), e);
            }else {
              logger.error("[getAlertCount()]: " + e.getMessage());
            }                        
            throw new XanbooException(10030, "[getAlertCount]:" + e.getMessage());
        }finally{
            dao.closeConnection(conn);
        }

     }

     public XanbooResultSet getAlarmArchiveItem(XanbooAdminPrincipal xCaller, long accountId, long archiveId, String extAccountId, String subsIdOrGguId, String egrpId, String startTS, String endTS) throws XanbooException {
        if(logger.isDebugEnabled()){
            logger.debug("[getAlarmArchiveItem]");
        }

        Connection conn=null;

        try{
               // validate the caller and privileges
               checkCallerPrivilege(xCaller);

               conn=dao.getConnection();
               return dao.getAlarmArchiveItem(conn, xCaller, accountId, archiveId, extAccountId, subsIdOrGguId, egrpId, startTS, endTS);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e){
            if(logger.isDebugEnabled()) {
              logger.error("[getAlarmArchiveItem()]: " + e.getMessage(), e);
            }else {
              logger.error("[getAlarmArchiveItem()]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[getAlarmArchiveItem]:" + e.getMessage());
        }finally{
            dao.closeConnection(conn);
        }
     }

     public void updateAlarmArchiveItem(XanbooAdminPrincipal xCaller, long archiveId) throws XanbooException {
        if(logger.isDebugEnabled()){
            logger.debug("[updateAlarmArchiveItem]");
        }

        Connection conn=null;

        try{
               // validate the caller and privileges
               checkCallerPrivilege(xCaller);

               conn=dao.getConnection();
               dao.updateAlarmArchiveItem(conn, xCaller, archiveId);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e){
            if(logger.isDebugEnabled()) {
              logger.error("[updateAlarmArchiveItem()]: " + e.getMessage(), e);
            }else {
              logger.error("[updateAlarmArchiveItem()]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[updateAlarmArchiveItem]:" + e.getMessage());
        }finally{
            dao.closeConnection(conn);
        }
     }

     public Integer getNotificationOptInStatus(XanbooAdminPrincipal xCaller, String notificationAddress, String token) throws XanbooException {

        if( (notificationAddress == null || notificationAddress.trim().length()==0) &&
             (token == null || token.trim().length()==0) ) {
            throw new XanbooException(10050);
        }

        if (notificationAddress != null && notificationAddress.trim().length()>0 &&
                !XanbooUtil.isValidEmail(notificationAddress) &&
                 !XanbooUtil.isValidPhone(notificationAddress)) {
                 throw new XanbooException(10050);
        }

        Connection conn=null;

        try{
               // validate the caller and privileges
               checkCallerPrivilege(xCaller);

               conn=dao.getConnection();
               return dao.getNotificationOptInStatus(conn, xCaller.getDomain(), notificationAddress, token);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e){
            if(logger.isDebugEnabled()) {
              logger.error("[getNotificationOptInStatus()]: " + e.getMessage(), e);
            }else {
              logger.error("[getNotificationOptInStatus()]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[getNotificationOptInStatus]:" + e.getMessage());
        }finally{
            dao.closeConnection(conn);
        }
     }

     public Map<String, Integer> getNotificationOptInStatus(XanbooAdminPrincipal xCaller, List<String> notificationAddresses) throws XanbooException {
         if (notificationAddresses == null || notificationAddresses.size() == 0) {
             throw new XanbooException(10050);
         }

         for (String notificationAddress : notificationAddresses) {
             if (!XanbooUtil.isValidEmail(notificationAddress) && !XanbooUtil.isValidPhone(notificationAddress)) {
                 throw new XanbooException(10050);
             }
         }
         
        Connection conn=null;

        try{
               // validate the caller and privileges
               checkCallerPrivilege(xCaller);

               conn=dao.getConnection();             
               return dao.getNotificationOptInStatus(conn, xCaller.getDomain(), notificationAddresses);

        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e){
            if(logger.isDebugEnabled()) {
              logger.error("[getNotificationOptInStatusList()]: " + e.getMessage(), e);
            }else {
              logger.error("[getNotificationOptInStatusList()]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[getNotificationOptInStatusList]:" + e.getMessage());
        }finally{
            dao.closeConnection(conn);
        }
     }

     public void setNotificationOptInStatus(XanbooAdminPrincipal xCaller, String notificationAddress, String token, int status) throws XanbooException {

        if (!validSetNotificationOptInStatusInput(notificationAddress, token, status)) {
            throw new XanbooException(10050);
        }

        boolean rollback=false;
        Connection conn = null;

        try {
               Integer profileType = null;
               String profileAddress = null;

               if (notificationAddress != null && notificationAddress.length() > 0) {
                   if ( notificationAddress.indexOf("%") > 0) {
                       profileType = new Integer(notificationAddress.substring(0,notificationAddress.indexOf("%")));
                       profileAddress = notificationAddress.substring((notificationAddress.indexOf("%")+1));
                   } else {
                       if (XanbooUtil.isValidPhone(notificationAddress))
                           profileType = GlobalNames.SMS_PROFILE_TYPE;
                       else if (XanbooUtil.isValidEmail(notificationAddress))
                           profileType = GlobalNames.EMAIL_PROFILE_TYPE;
                       else
                           throw new XanbooException(10050);

                       profileAddress = notificationAddress;
                   }
               }
                // validate the caller and privileges
               checkCallerPrivilege(xCaller);
               long accountId = 0l;
               if (xCaller.getAccountPrincipal() != null && xCaller.getAccountPrincipal().getAccountId() > 0l) {
                    accountId = xCaller.getAccountPrincipal().getAccountId();
               }
               conn=dao.getConnection();
               dao.setNotificationOptInStatus(conn, accountId, xCaller.getDomain(), profileAddress, token, status, "", "", profileType == null ? null : profileType.toString());
        } catch (XanbooException xe) {
            rollback = true;
            throw xe;
        }catch (Exception e){
            rollback = true;
            if(logger.isDebugEnabled()) {
              logger.error("[setNotificationOptInStatus()]: " + e.getMessage(), e);
            }else {
              logger.error("[setNotificationOptInStatus()]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[setNotificationOptInStatus]:" + e.getMessage());
        }finally{
            dao.closeConnection(conn, rollback);
        }
     }

     public void setNotificationOptInStatus(XanbooAdminPrincipal xCaller, Map<String, Integer> notificationAddresses) throws XanbooException {
         if (notificationAddresses == null || notificationAddresses.isEmpty()) {
             throw new XanbooException(10050);
         }

        boolean rollback=false;
        Connection conn = null;

         try {
              // validate the caller and privileges
              checkCallerPrivilege(xCaller);
              conn=dao.getConnection();
              long accountId = 0l;

              if (xCaller.getAccountPrincipal() != null && xCaller.getAccountPrincipal().getAccountId() > 0l) {
                  accountId = xCaller.getAccountPrincipal().getAccountId();
              }
                    
               for (String notificationAddress : notificationAddresses.keySet()) {
                   Integer status = notificationAddresses.get(notificationAddress);
                   if (!validSetNotificationOptInStatusInput(notificationAddress, null, status)) {
                        throw new XanbooException(10050);
                   }

                   Integer profileType = null;
                   String profileAddress = null;

                   if ( notificationAddress.indexOf("%") > 0) {
                        profileType = new Integer(notificationAddress.substring(0,notificationAddress.indexOf("%")));
                        profileAddress = notificationAddress.substring((notificationAddress.indexOf("%")+1));
                   } else {
                        if (XanbooUtil.isValidPhone(notificationAddress))
                            profileType = GlobalNames.SMS_PROFILE_TYPE;
                        else if (XanbooUtil.isValidEmail(notificationAddress))
                            profileType = GlobalNames.EMAIL_PROFILE_TYPE;
                        else
                            throw new XanbooException(10050);
                        profileAddress = notificationAddress;
                   }                   
                   
                   dao.setNotificationOptInStatus(conn, accountId, xCaller.getDomain(), profileAddress, null,
                           notificationAddresses.get(notificationAddress), "", "", profileType == null ? null : profileType.toString());
               }

         } catch (XanbooException xe) {
            rollback = true;
            throw xe;
        }catch (Exception e){
            rollback = true;
            if(logger.isDebugEnabled()) {
              logger.error("[setNotificationOptInStatus()]: " + e.getMessage(), e);
            }else {
              logger.error("[setNotificationOptInStatus()]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[setNotificationOptInStatus]:" + e.getMessage());
        }finally{
            dao.closeConnection(conn, rollback);
        }

     }

     private boolean validSetNotificationOptInStatusInput(String notificationAddress, String token, int status) {
         if( (notificationAddress == null || notificationAddress.trim().length()==0) &&
             (token == null || token.trim().length()==0) ) {
            return false;
        }
         
        if (status < 0 || status > 2) {
            return false;
        }

        return true;
     }
     
  // get Domain Licenses
     public XanbooResultSet getDomainLicenses(XanbooAdminPrincipal xCaller, String domainId) throws XanbooException {
     	
     	 if (logger.isDebugEnabled()) {
              logger.debug("[getDomainLicenses()]:");
          }
          
          Connection conn=null;
          try {
              // validate the caller and privileges
         	 checkCallerPrivilege(xCaller);

              conn=dao.getConnection();
              return dao.getDomainLicenses(conn, domainId);
              
          }catch (XanbooException xe) {
              throw xe;
          }catch (Exception e) {
              if(logger.isDebugEnabled()) {
                logger.error("[getDomainLicenses()]" + e.getMessage(), e);
              }else {
                logger.error("[getDomainLicenses()]" + e.getMessage());
              }
              throw new XanbooException(60000, "[getDomainLicenses()]:" + e.getMessage());
          }finally {
              dao.closeConnection(conn);
          }     
     }
     
     
      // Added for DLDP 1510 3.3.1.1
     public void logoutAdmin(XanbooAdminPrincipal xap, long accountId, String gatewayGUID, String deviceGUID, String actionSource, String actionDesc, String actionValue){    	 
         if(xap!=null && xap.isLogAudit()) {               
             try {
            	 auditLog(xap, accountId, gatewayGUID, deviceGUID, actionSource, actionDesc, actionValue );
             }catch(Exception ee) {
                 if(logger.isDebugEnabled()) {
                     logger.warn("Failed to log audit activity. Exception: " + ee.getMessage() );
                 }                 
             }
         }  	
     }
     
     public void newAdmin(XanbooAdminPrincipal xap, long accountId, String gatewayGUID, String deviceGUID, String actionSource, String actionDesc, String actionValue){
    	 if(xap!=null && xap.isLogAudit()) {               
             try {
                 auditLog(xap, accountId, gatewayGUID, deviceGUID, actionSource, actionDesc, actionValue );             
             }catch(Exception ee) {
                 if(logger.isDebugEnabled()) {
                     logger.warn("Failed to log audit activity. Exception: " + ee.getMessage() );
                 }                 
             }
         }   
     }
     
     public void deleteAdmin(XanbooAdminPrincipal xap, long accountId, String gatewayGUID, String deviceGUID, String actionSource, String actionDesc, String actionValue) {
    	 if(xap!=null && xap.isLogAudit()) {               
             try {
                 auditLog(xap, accountId, gatewayGUID, deviceGUID, actionSource, actionDesc, actionValue );                
             }catch(Exception ee) {
                 if(logger.isDebugEnabled()) {
                     logger.warn("Failed to log audit activity. Exception: " + ee.getMessage() );
                 }
             }
         }     
     }
     
     // 4207 validate subs feature
     public void validateSubsFeature(String domain, String CSVfeatures) throws XanbooException{
         if ( logger.isDebugEnabled() )
             logger.debug("[validateSubsFeature()] - domain="+domain+", CSVfeatures="+CSVfeatures);
    	 if (CSVfeatures != null && CSVfeatures.trim().length()>0 ) {
				List<SubscriptionFeature> lstSubsFeature = getDomainFeatureList(domain, null);				
				if(lstSubsFeature.size() ==0){
					throw new XanbooException(21540, "Invalid subs features.");
				}
				List<String> lstString = Arrays.asList(CSVfeatures.split(","));
				List<String> actualSubsFeatures = Arrays.asList(XanbooSubscription.toFeature(lstSubsFeature).split(","));
				
				for (String updateFeatures : lstString) {
					if (!actualSubsFeatures.contains(updateFeatures)) {
						throw new XanbooException(21540, "Invalid subs features.");
					}
				}
			}    	
     }
     // End of DLDP 4207
     public List<SubscriptionFeature> getDomainFeatureList(XanbooAdminPrincipal xCaller, List<String> mappingCodes) throws  XanbooException
     {
         if ( logger.isDebugEnabled())
             logger.debug("[getDomainFeatureList] xCaller="+xCaller+" mappingCodes="+mappingCodes);
         
         if ( xCaller == null || xCaller.getDomain() == null || xCaller.getDomain().equalsIgnoreCase(""))
             throw new XanbooException(10030,"Missing credentials or domain");
         
         return getDomainFeatureList(xCaller.getDomain(),mappingCodes);
     }
    /**
     * Method to retrieve feature list based on domain and mapping code.
     * @param domainId
     * @param mappingCode
     * @return list of subscription features
     * @throws XanbooException
     */
    public List<SubscriptionFeature> getDomainFeatureList(String domainId, List<String> mappingCode) throws  XanbooException{
    	 if (logger.isDebugEnabled()) {
             logger.debug("[getDomainFeatureList()]:");
         }
    	 
    	 //expiring cache, if GlobalNames.SERVICE_CACHE_EXPIRY_MINUTES minutes passed
         long currTime = System.currentTimeMillis();
         if( (( currTime-lastDomainFeatureCacheUpdate)/60000 ) > GlobalNames.SERVICE_CACHE_EXPIRY_MINUTES ) 
         {
             synchronized(this) 
             {
            	 domainSubscriptions=null;
             }
         }         
         
         Connection conn=null;
         try {           
        	 if(domainSubscriptions == null){
        		 domainSubscriptions = new HashMap<String,  List<SubscriptionFeature>>();
        	 }

        	 if(!domainSubscriptions.containsKey(domainId)){
        		 conn=dao.getConnection();
        		 XanbooResultSet domainSubs = dao.getDomainFeatureList(conn, domainId);
        		 List<SubscriptionFeature> lstSubsFeature = new ArrayList<SubscriptionFeature>();
        		 for(int i=0; i<domainSubs.size(); i++){        			
					 HashMap<String, String> row = (HashMap<String, String>)domainSubs.get(i);        			 
        			 SubscriptionFeature subFeature = new SubscriptionFeature();
        			 subFeature.setDescription((String)row.get("DESCRIPTION"));
        			 subFeature.setFeatureId((String)row.get("FEATURE_ID"));
        			 subFeature.setMapping((String)row.get("MAPPING"));
        			 subFeature.setTc_acceptance((String)row.get("TC_ACCEPTANCE"));
        			 lstSubsFeature.add(subFeature);
        		 }
        		 domainSubscriptions.put(domainId, lstSubsFeature);
        	 }    
        	 lastDomainFeatureCacheUpdate = System.currentTimeMillis();
        	 
        	 
         }catch (XanbooException xe) {
             throw xe;
         }catch (Exception e) {
             if(logger.isDebugEnabled()) {
               logger.error("[getDomainFeatureList()]" + e.getMessage(), e);
             }else {
               logger.error("[getDomainFeatureList()]" + e.getMessage());
             }
            throw new XanbooException(60000, "[getDomainFeatureList()]:" + e.getMessage());
         }finally {
             dao.closeConnection(conn);
         }      
         
         
         // make a copy of original domain feature list
         List<SubscriptionFeature> lstSubsciptionFeature = new ArrayList<SubscriptionFeature>();
         lstSubsciptionFeature.addAll(domainSubscriptions.get(domainId));
         // filter subscription feature based on mapping code list
         List<SubscriptionFeature> toBeRemove = new ArrayList<SubscriptionFeature>();
         if(mappingCode != null){
        	 for (SubscriptionFeature subscriptionFeature : lstSubsciptionFeature) {
        		 if(!mappingCode.contains(subscriptionFeature.getMapping())){
        			 toBeRemove.add(subscriptionFeature);
        		 }
			}
         }
         
         if(toBeRemove.size() >0){
        	 lstSubsciptionFeature.removeAll(toBeRemove);
         }
        	 
         return lstSubsciptionFeature;
     }
    
	/**
	 * Method to add subscription features to existing subscription features.
	 * @param xsub - instance of Xanboo Subscription
	 * @param lstSubsFeature - list of subscription features
	 * @return - CSV string of newly added subscription features
	 * @throws RemoteException
	 * @throws XanbooException
	 */
	private String getUniqueAddFeatures( XanbooSubscription xsub, List<SubscriptionFeature> lstSubsFeature)throws RemoteException,XanbooException{
		    	 
    	Set<String> setSubFeature = new HashSet<String>();		
		String totalSubFeature = ( (xsub.getSubsFeatures()!=null && xsub.getSubsFeatures().trim().length()>0) ?xsub.getSubsFeatures()+",":"") + XanbooSubscription.toFeature(lstSubsFeature);
		String[] arrString = totalSubFeature.split(",");
		for (String string : arrString) {
			if(string!=null && string.length()>0){
				setSubFeature.add(string);			
			}
		}
		StringBuffer strbuffer = new StringBuffer();
		for (String string : setSubFeature) {
			strbuffer.append(string + ",");
		}
		
		strbuffer.setLength(strbuffer.length()-1);
		return strbuffer.toString();
	}
	
	
		
	/**
	 * @param xsub - instance of subscription feature
	 * @param lstSubsFeature - list of subscription features
	 * @return - CSV string of features 
	 * @throws RemoteException
	 * @throws XanbooException
	 */
	private String getUniqueRemoveFeatures(  XanbooSubscription xsub, List<SubscriptionFeature>lstSubsFeature, List<String> featuresRemoved )throws RemoteException,XanbooException{
		    	 
		String[] removeSubFeature = XanbooSubscription.toFeature(lstSubsFeature).split(",");
		String[] actualSubFeature = xsub.getSubsFeatures().split(",");
		final List<String> lstActualSubFeature =  new ArrayList<String>();
		Collections.addAll(lstActualSubFeature, actualSubFeature);
		
			      
		for (String subscriptionFeature : removeSubFeature) {
			if(lstActualSubFeature.contains(subscriptionFeature)){
				lstActualSubFeature.remove(subscriptionFeature);
				featuresRemoved.add(subscriptionFeature);
			}
		}
		
		StringBuffer strBuffer = new StringBuffer();
		for (String string : lstActualSubFeature) {
			strBuffer.append(string +",");
		}
		
		if(strBuffer.toString().trim().length() > 0){
			strBuffer.setLength(strBuffer.length()-1);
		}
		if(strBuffer.toString().trim().length() == 0){
			strBuffer.append(" ");
		}
		return strBuffer.toString();
	}

	
	
	
	public String addRemoveSubscriptionFeature(XanbooAdminPrincipal xCaller, XanbooSubscription xsub, List<SubscriptionFeature> featuresToAdd,List<SubscriptionFeature> featuresToRemove)throws RemoteException,XanbooException{
		
		if(xsub == null){
			throw new XanbooException(21405, "Failed to add/update subscription. Invalid account or subscription id, or not found.");
		}
		
		XanbooResultSet subsList = getSubscription(xCaller, xsub.getAccountId(), xsub.getSubsId(), xsub.getHwId());
		
		if (subsList == null || (subsList.size() == 0)) {
    	    throw new XanbooException(21405, "Failed to add/update subscription. Invalid account or subscription id, or not found.");
    	}
		String strUniqueFeatures = null;
		String strUniqueAddFeatures = null;
		String strUniqueRemoveFeatures = null;
		
		for(int i=0; i<subsList.size(); i++){
			
			try {
				XanbooSubscription xSubOrig = new XanbooSubscription(subsList, i);			
				List<String> strOrigFeatures = Arrays.asList(xSubOrig.getSubsFeatures().split(","));
				
				Boolean isSbnSyncNeeded = false;
				List<String> featuresRemoved= new ArrayList<String>();
				
				// add features
				if(featuresToAdd != null && featuresToAdd.size()>0){
					// check if full sbn sync needed				
					for (SubscriptionFeature subscriptionFeature : featuresToAdd) {
						String featureId = subscriptionFeature.getFeatureId();
						if( (!strOrigFeatures.contains(featureId)) &&  (checkKeyHolderList(featureId) ) ){
							isSbnSyncNeeded = true;
							break;
						}
					}				
					// get unique list of features
					strUniqueAddFeatures = getUniqueAddFeatures( xSubOrig,  featuresToAdd);	
					strUniqueFeatures = strUniqueAddFeatures;
					
				}
				
				if((featuresToRemove!=null && featuresToRemove.size() >0) && (strUniqueAddFeatures == null || (strUniqueAddFeatures!=null && strUniqueAddFeatures.trim().length()==0))){
					// check if full sbn sync needed				
					for (SubscriptionFeature subscriptionFeature : featuresToRemove) {
						String featureId = subscriptionFeature.getFeatureId();
						// check if feature id exists in subscription and it is one of the key holders
						if( (strOrigFeatures.contains(featureId)) &&  (checkKeyHolderList(featureId) ) ){
							isSbnSyncNeeded = true;
							break;
						}
					}					
					// get unique list of features
					
					strUniqueRemoveFeatures = getUniqueRemoveFeatures( xSubOrig,  featuresToRemove, featuresRemoved);	
					strUniqueFeatures = strUniqueRemoveFeatures;
				}else{
					if((featuresToRemove!=null && featuresToRemove.size() >0) && strUniqueAddFeatures!=null ){
						List<String> strActualFeatures = Arrays.asList(strUniqueAddFeatures.split(","));
						List<String> strCopyActualFeatures = new ArrayList<String>();
						strCopyActualFeatures.addAll(strActualFeatures);
						for (SubscriptionFeature subscriptionFeature : featuresToRemove) {
							String featureId = subscriptionFeature.getFeatureId();
							// check if feature id exists in subscription and it is one of the key holders
							if( (strCopyActualFeatures.contains(featureId))  ){
								strCopyActualFeatures.remove(strCopyActualFeatures.indexOf(featureId));			
								featuresRemoved.add(featureId);
							}
						}	
						
						StringBuffer strFinal = new StringBuffer();
						for (String string : strCopyActualFeatures) {
							strFinal.append(string).append(",");
						}
						if(strFinal.length()>0){
							strFinal.setLength(strFinal.length()-1);
						}
						strUniqueFeatures = strFinal.toString();
						
					}
				}
				
				 if(!StringUtils.isEmpty(strUniqueFeatures)){
			        	int index1 = strUniqueFeatures.indexOf(FEATURE_KEY_HOLDER+"1");
			        	int index2 = strUniqueFeatures.indexOf(FEATURE_KEY_HOLDER+"2");
			        	
			        	if(index1 >= 0 && index2 >=0){
			        		logger.warn("More than one KEYHOLDERLVL cannot be added." );
			        		throw new XanbooException(10031, "More than one KEYHOLDERLVL cannot be added." );
			        	}
			        }
				 
				if(xsub.getHwId() == null || xsub.getHwId().trim().length()==0){
					xsub.setHwId(xSubOrig.getHwId());
				}
				int tc_flag = -1;
				// check tc_flag only when new soc code added as per DLDP 4350
				if(featuresToAdd!= null && featuresToRemove == null && xsub.getTcFlag() == 1){
					tc_flag = 1;
				}				
				updateSubscription(xCaller, xsub, null, -1, tc_flag, strUniqueFeatures, xSubOrig, isSbnSyncNeeded);
				
			}
			catch(XanbooException xe){
				if(xe.getCode()==10031){
					throw new XanbooException(10031, "More than one KEYHOLDERLVL cannot be added.");
				}
			}
			catch (Exception e) {
		        if(logger.isDebugEnabled()) {
		          logger.error("[addRemoveSubscriptionFeature()]: Exception:" + e.getMessage(), e);
		        }else {
		          logger.error("[addRemoveSubscriptionFeature()]: Exception:" + e.getMessage());
		        }		       
		        throw new XanbooException(10030, e.getMessage());
		    }
		}
		
		return strUniqueFeatures;
	}
	private void cancelServiceSubscription(XanbooAdminPrincipal xCaller,List<String> featuresRemoved, XanbooSubscription xSubOrig) throws XanbooException{
		Connection conn = null;
		if( (featuresRemoved!=null && featuresRemoved.size() >0) && ( xSubOrig.getSubscriptionClass() != null && xSubOrig.getSubscriptionClass().equals(GlobalNames.DLLITE))) {
          
                soProxy = (com.xanboo.core.extservices.outbound.ServiceOutboundProxy)getEJB(GlobalNames.EJB_OUTBOUND_PROXY);
           try{
        	   conn = dao.getConnection();
            for ( String feature : featuresRemoved )
            {
                List<ServiceSubscription> serviceSubscriptions = dao.getAccountSubscriptionSvcs(conn, xSubOrig.getAccountId(), feature);
                if(serviceSubscriptions != null && serviceSubscriptions.size() > 0 ){
                	for ( ServiceSubscription svcSubscription : serviceSubscriptions )
                    {
                        soProxy.cancelServiceSubscription(xCaller, svcSubscription);
                    }		
                }
                
            }
           
           }catch (Exception e) {
   	        if(logger.isDebugEnabled()) {
  	          logger.error("[cancelServiceSubscription()]: Exception:" + e.getMessage(), e);
  	        }else {
  	          logger.error("[cancelServiceSubscription()]: Exception:" + e.getMessage());
  	        }
  	        throw new XanbooException(10030, e.getMessage());
  	    }finally {
  	       dao.closeConnection(conn);
  	    }
        }
	}
	
	/**
	 * @param conn - database connection object 
	 * @param bMarket - business market
	 * @param state - state
	 * @param city - city
	 * @param zip - zip
	 * @param prefBit - prefBit
	 * @return - instance of XanbooResultSet 
	 * @throws XanbooException
	 */
	public XanbooResultSet getSubscriptions(XanbooAdminPrincipal xCaller, String bMarket, String state, String city, String postalCode, Integer prefBit)throws XanbooException{
		 if(logger.isDebugEnabled()){
	            logger.debug("[getSubscriptions]");
	        }
		 
		 Connection conn=null;
	        try {
	            // validate the caller and privileges
	            checkCallerPrivilege(xCaller);
	            conn=dao.getConnection();
	            return dao.getSubscriptions(conn, bMarket, state, city, postalCode, prefBit);
	        }catch (XanbooException xe) {
	            throw xe;
	        }catch (Exception e) {
	            if(logger.isDebugEnabled()) {
	              logger.error("[getSubscriptions()]" + e.getMessage(), e);
	            }else {
	              logger.error("[getSubscriptions()]" + e.getMessage());
	            }
	            throw new XanbooException(10030, "[getSubscriptions]:" + e.getMessage());
	        }finally {
	            dao.closeConnection(conn);
	        }         
	        
	 }

    public XanbooResultSet getDeviceModel(XanbooAdminPrincipal xap, String languageId, String modelId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getDeviceModel()]:");
        }

        Connection conn = null;
        try {
            // validate the caller and privileges
            checkCallerPrivilege(xap);
            conn = dao.getConnection();

            return dao.getDeviceModel(conn, languageId, modelId);

        } catch (XanbooException xe) {
            throw xe;
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.error("[getDeviceModel] Calling DAO.getDeviceModel failed", e);
            } else {
                logger.error("[getDeviceModel]: " + e.getMessage());
            }
            throw new XanbooException(10030, "Exception while executing DAO method");
        } finally {
            dao.closeConnection(conn);
        }
    }

        
 	private boolean checkKeyHolderList(String featureId){
		if(featureId.length() > 11 && FEATURE_KEY_HOLDER.equalsIgnoreCase(featureId.substring(0,12))){
			return true;
		}
		else
		return false;
	}

        /**
         * Verifys the subscription in CSI
         * @param subsId
         * @param hwId
         * @param subsDataMap  The CSI call sets subscripton data in this HaskMap
         * @param inputDFlags
         * @return inputDFlags with bitwise operation of the 3G only value retreived from the CSI call
         * @throws XanbooException
         */
        private int verifySubscription(String subsId, String hwId, HashMap subsDataMap, int inputDFlags) throws XanbooException {

            // check subscription thru ACSI, and get 3Gonly flag value
            SimpleACSIClient acsi = new SimpleACSIClient();
            
            // DLDP 2728 Changes -vp889x --start
            int[] vSubResponse = acsi.verifySubscription(subsId, hwId, subsDataMap);
            int is3Gonly = vSubResponse[0];
            // DLDP 2728 TC_FLAG setting disabled - st024y - 2015-07-07
            //int termsConditionStatus = vSubResponse[1];
            //xsub.setTcFlag(termsConditionStatus);
            // DLDP 2728 Changes -vp889x --end

            if (is3Gonly < 0) {

                if (is3Gonly == -3) {
                    throw new XanbooException(21407, "Failed to verify subscription. No hardware information or IMEI found.");
                } else if (is3Gonly == -2) {
                    throw new XanbooException(21408, "Failed to verify subscription. Hardware information or IMEI does not match.");
                } else if (is3Gonly == -9) {
                    throw new XanbooException(21409, "Failed to verify subscription. ACSI call failed.");
                } else {
                    throw new XanbooException(21410, "Failed to verify subscription. Invalid subscription or hardware id.");
                }
            }

            if (is3Gonly == 0) {
                return inputDFlags | 4;       //set bit2     (bit2-3 ==> 01:3g+bb)
            } else {
                return inputDFlags & (~4);    //reset bit2   (bit2-3 ==> 00:3g-only)
            }
        }

        /**
         * Sets the subscription featues based on what is in socCodeList
         * @param xCaller
         * @param xsub
         * @param socCodeList
         * @throws XanbooException
         */
        private void setSubscriptionFeatures(XanbooAdminPrincipal xCaller, XanbooSubscription xsub, List<String> socCodeList) throws XanbooException {
            
            if (socCodeList == null) return;

            List<SubscriptionFeature> subFeatures = this.getDomainFeatureList(xCaller.getDomain(), null);
                xsub.setSubsFeatures(""); //override this column with features returned from CSI call
                for ( String socCd : socCodeList )
                {
                    boolean mapped = false;
                    for ( SubscriptionFeature feature : subFeatures )
                    {
                        if (feature.getMapping().indexOf(socCd) > -1 )
                        {
                            xsub.appendFeatureId(feature.getFeatureId());
                            mapped = true;
                            break;
                        }
                    }
                    if ( !mapped )
                        logger.warn("[newSubscription()] - did not find a matching feature for SOC code="+socCd);
                }
        }
        
	private static String FEATURE_KEY_HOLDER= "KEYHOLDERLVL";
	 
    private void invalidateDlaAuthServiceCache(long accountId) {
    	
   	 	if(logger.isDebugEnabled()){
   	 		logger.debug("[invalidateDlaAuthServiceCache]");
   	 		
   	 	}

    	try {
    		XanbooResultSet gguidList = getGatewayGuids(accountId);
    		
    		if (gguidList != null && gguidList.size() > 0) {
    			
    			DlaAuthServiceClient dlaAuthServiceClient = new DlaAuthServiceClient();
    			dlaAuthServiceClient.invalidateGatewayGuids(accountId, gguidList);
    		}

    	} catch (Exception e) {
            if(logger.isDebugEnabled()) {
        		logger.error("[invalidateDlaAuthServiceCache()]: caught exception invalidating Dla Auth Service Cache: " + e.getMessage(), e);
            } else {
        		logger.error("[invalidateDlaAuthServiceCache()]: caught exception invalidating Dla Auth Service Cache: " + e.getMessage());
            }             
    	}
    	
    }
    
    private XanbooResultSet getGatewayGuids(long accountId) throws XanbooException {
	   	 if(logger.isDebugEnabled()){
	         logger.debug("[getGatewayGuids] for accountId=" + accountId);
	     }
	     
	     Connection conn=null;
	     try {
	         conn=dao.getConnection();
	         XanbooResultSet xrs = dao.getGatewayGuids(conn, Long.valueOf(accountId));
	         return xrs;
	         
	     } finally {
	         dao.closeConnection(conn);
	     }
    }

    public void syncSbn(XanbooAdminPrincipal xCaller, XanbooSubscription xSub) throws RemoteException, XanbooException{
    	SBNSynchronizer sbn = new SBNSynchronizer();
    	String installationMonStatus = null;
    	Connection conn=null;
    	XanbooResultSet  subsList = this.getSubscription(xCaller, xSub.getAccountId(), xSub.getSubsId(), xSub.getHwId());
    	if (subsList == null || (subsList.size() == 0)) {
        	    throw new XanbooException(21405, "Failed to add/update subscription. Invalid account or subscription id, or not found.");
        	}
    	XanbooSubscription xanbooSubscription = null;
		try {
			xanbooSubscription = new XanbooSubscription(subsList, 0);
		} catch (InstantiationException e) {
			throw new XanbooException(21405, "Failed to add/update subscription. Invalid account or subscription id, or not found.");
		}
			
    	XanbooPrincipal xPrincipal = getXanbooPrincipal(xCaller,xanbooSubscription.getAccountId(), xanbooSubscription.getSubsId(),xanbooSubscription.getHwId());
    	String gguid = null;
    	if(xanbooSubscription.getSubscriptionClass()!=null && xanbooSubscription.getSubscriptionClass().equalsIgnoreCase(GlobalNames.DLLITE)){
    		gguid = xanbooSubscription.getGguid();
    	}
    	
    	if (!sbn.checkIfSubscriptionExists(xanbooSubscription.getSubsId(),xanbooSubscription.getHwId(), gguid)) {
			
			try{
			conn=dao.getConnection();
			XanbooNotificationProfile xnpArray[] = dao.getNotificationProfile(conn,xanbooSubscription.getAccountId(), 0,true);
			xanbooSubscription.setNotificationProfiles(xnpArray);
			sbnNewSubscription(xanbooSubscription, sbn);
			sbnSyncEmergencyContacts(sbn, xnpArray, xanbooSubscription.getGguid());

			XanbooResultSet deviceList = dao.getDeviceList(conn, xPrincipal,xanbooSubscription.getGguid(), null);
			sbnNewDeviceRegistered(xanbooSubscription, sbn, deviceList);
			}catch(Exception excep){
				throw new XanbooException(21420,"Failed to updateInstallationMonStatus. SBN synchronization failed.");
			}
			finally{
				dao.closeConnection(conn, false);
			}
			
		}
    	
    	try{
			sbnInstalationAlarmDelay(xanbooSubscription.getAlarmDelay(), xanbooSubscription, sbn);
    	}
    	catch(Exception xe){
    		logger.info("Alarm update fail ..");
    	}
    	try{
			
			sbnUpdateInstallation(xanbooSubscription, sbn, xanbooSubscription.getHwId());
    	}
			catch(Exception xe){
	    		logger.info("sbnUpdateInstallation update fail ..");
	    	}
			//sbnUpdateInstallationMonitorStatus(xsubNew, sbn, sms ,  xsubOld);
			if (xanbooSubscription.isSuspended()) { // dl service status bits: 01 // or 10 --> suspend service				
				if (xanbooSubscription.getGguid() != null) {// 	read current subs flags and check if subs is cancelled																					
					int currSubsFlags = -1;
					currSubsFlags = xanbooSubscription.getSubsFlags();
					
					if (currSubsFlags > 0 && (currSubsFlags & 3) != 0) { // subs not cancelled, allow suspension in SBN
							if (xanbooSubscription.getGguid().indexOf("DL-") == -1) { // if there is a real gateway!, set to suspended
								installationMonStatus = SBNSynchronizer.MONSTATUS_SUSPENDED;
							} else { // if there is NO real gateway!, set to pending state
								installationMonStatus = SBNSynchronizer.MONSTATUS_PENDING;
							}
						}
					}
				}
			else if (xanbooSubscription.isActive()) { // dl service status bits to 11 --> activate/restore service  allow SBN restore, only if there is a real gateway!
				if (xanbooSubscription.getGguid() != null) {
					if (xanbooSubscription.getGguid().indexOf("DL-") == -1) { // if
							installationMonStatus = getSubMonitorType(xanbooSubscription.getSubsFlags()).getSBNStatusValue();
							Boolean addKeyHolder = false;
							if (xanbooSubscription.getSubsFeatures() != null) {
									addKeyHolder = xanbooSubscription.getSubsFeatures().contains(GlobalNames.FEATURE_KEY_HOLDER);			
							}
							boolean pos4 = XanbooUtil.isBitOn(xanbooSubscription.getSubsFlags(), 4);
							boolean pos5 = XanbooUtil.isBitOn(xanbooSubscription.getSubsFlags(),5);
							// For SM and NM if no keyholder then set NMON
							if(!addKeyHolder && ((pos5 && !pos4 ) || (!pos5 && !pos4 )) ){
								installationMonStatus =  SBNSynchronizer.MONSTATUS_NO_MONITORED;
							}
							
							if(xanbooSubscription.getSubscriptionClass() !=null && xanbooSubscription.getSubscriptionClass().equalsIgnoreCase(GlobalNames.DLLITE)){
								installationMonStatus =  SBNSynchronizer.MONSTATUS_MONITORED;
							}

					} else { // if there is NO real gateway!, set to pending state sbn.updateInstallationMonStatus(newxsub.getGguid(), SBNSynchronizer.MONSTATUS_PENDING);
						installationMonStatus = SBNSynchronizer.MONSTATUS_PENDING;
					}
				}
			}
			SubscriptionMonitorStatus sms = getSubMonitorType(xanbooSubscription.getSubsFlags());
			
			// override if Monitoring Type is being changed, but only if gateway is already installed - status is not pending
			if(xanbooSubscription.getSubscriptionClass() !=null && xanbooSubscription.getSubscriptionClass().equalsIgnoreCase(GlobalNames.DLSEC)){
				if (( (installationMonStatus != SBNSynchronizer.MONSTATUS_PENDING) && (installationMonStatus != SBNSynchronizer.MONSTATUS_NO_MONITORED)) && sms != null && sms.getSBNStatusValue() != null) {
					 installationMonStatus = sms.getSBNStatusValue();
				}
			}
			if (installationMonStatus != null) {
				boolean sbnOK = sbn.updateInstallationMonStatus(xanbooSubscription.getGguid(), installationMonStatus);

				if (logger.isDebugEnabled()) {
					logger.debug(installationMonStatus + " : installationMonStatus :  DLDP3043 : updateInstallationMonStatus in SBN : result : "+ sbnOK);
				}
				if (!sbnOK) { // SBN sync failed!
					throw new XanbooException(21420,"Failed to updateInstallationMonStatus. SBN synchronization failed.");
				}
			}
    }
    
    public XanbooResultSet getDeviceListByGGuid(XanbooAdminPrincipal xCaller, String gguid, String dguid)throws XanbooException
    {
        if (logger.isDebugEnabled()) 
        {
            logger.debug("[getDevice()]:");
        }
       
        if ( xCaller == null )
        {
            logger.warn("[getDevice()] - Security principal not specified");
            throw new XanbooException(61001,"Security principal not provided");
        }
        
        if ( gguid == null || gguid.equalsIgnoreCase(""))
        {
            logger.warn("[getDevice()] - gateway guid not specified");
            throw new XanbooException(61001,"Gateway guid was not provided");
        }
        
        if ( dguid == null || dguid.equalsIgnoreCase(""))
        {
            logger.warn("[getDevice()] - device guid was not specified");
            throw new XanbooException(61001,"Device guid was not provided");
        }
                
        Connection conn=null;
        boolean rollback=false;
        //first step ... insert the record into the notification_event_queue table and commit
        try 
        {
            //validate the caller and privileges
            //XanbooUtil.checkCallerPrivilege(xCaller);
            conn = dao.getConnection();
            return dao.getDeviceListByGGuid(conn,null,gguid,dguid);
        }
        catch (XanbooException xe) 
        {
            rollback=true;            
            throw xe;
        }
        catch (Exception e) 
        {
            rollback=true;
            if(logger.isDebugEnabled()) 
            {
              logger.error("[getDevice()]: Exception:" + e.getMessage(), e);
            }
            else 
            {
              logger.error("[getDevice()]: Exception:" + e.getMessage());
            }                                        
            throw new XanbooException(61000, "[getDevice()]: Exception:" + e.getMessage());
        }
        finally 
        {
            dao.closeConnection(conn, rollback);
        }
    }
}

