/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/account/AccountManagerEJB.java,v $
 * $Id: AccountManagerEJB.java,v 1.82 2011/07/01 16:11:30 levent Exp $
 * 
 * Copyright 2011 AT&T Digital Life
 *
 */

package com.xanboo.core.sdk.account;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.CreateException;
import javax.ejb.EJB;
import javax.ejb.Init;
import javax.ejb.Local;
import javax.ejb.LocalHome;
import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.Remove;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import com.xanboo.core.common.LicenseControl;
import com.xanboo.core.mbus.MBusSynchronizer;
import com.xanboo.core.mbus.domain.DLCoreMBusSubscriptionChange;
import com.xanboo.core.mbus.domain.DLCoreMBusUserChange;
import com.xanboo.core.sdk.contact.XanbooContact;
import com.xanboo.core.sdk.device.DeviceManager;
import com.xanboo.core.sdk.pai.PAICommandREST;
import com.xanboo.core.sdk.sysadmin.SysAdminManager;
//import com.xanboo.core.xail.ejb.pai.PAIManager;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooEncryptionProvider;
import com.xanboo.core.security.XanbooEncryptionProviderFactory;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.EjbProxy;
import com.xanboo.core.util.GlobalNames;
import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;
import com.xanboo.core.util.SBNSynchronizer;
import com.xanboo.core.util.XanbooException;
import com.xanboo.core.util.XanbooUtil;
import com.xanboo.core.util.fs.AbstractFSProvider;

import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
/**
 *
 * Session Bean implementation of <code>AccountManager</code>. This bean acts as a wrapper class for
 * all account/user management, notification profile related Core SDK methods.<br>
 *
 *
 */

@Remote(AccountManager.class)
@Stateless(name="AccountManager")
@TransactionManagement( TransactionManagementType.BEAN )

public class AccountManagerEJB  {

  // private SessionContext context;
   private Logger logger;
    
   // related DAO class
   private AccountManagerDAO dao=null;

   private DeviceManager dManager ;
  
   private SysAdminManager sManager;
   //private PAIManager paiManager = null;
  
   private PAICommandREST paiCmdRest ;
   
   /**
    * Profile type id used for Email notifications    */
   public static final int PROFILE_TYPE_EMAIL=0;
   
   /** Access level specifying no user access to an object **/
   public static final int USER_ACCESS_NONE = 0;
   
   /** Access level specifying read only user access to an object **/
   public static final int USER_ACCESS_READ = 1;

   /** Access level specifying full (read/write) user access to an object **/
   public static final int USER_ACCESS_READWRITE = 2;
   
   
 
   
    @PostConstruct
   public void init() throws CreateException {
        try {
            // create a logger instance
            logger=LoggerFactory.getLogger(this.getClass().getName());
            if(logger.isDebugEnabled()) {
                logger.debug("[ejbCreate()]:");
            }
            
            dao = new AccountManagerDAO();
            EjbProxy proxy = new EjbProxy();
            dManager = (DeviceManager) proxy.getObj(GlobalNames.EJB_DEVICE_MANAGER);
            sManager = (SysAdminManager) proxy.getObj(GlobalNames.EJB_SYSADMIN_MANAGER);
            paiCmdRest = (PAICommandREST) proxy.getObj(GlobalNames.EJB_PAI_REST_SERVICE);
          //  getDeviceManagerEJB();
         //   getSysAdminManagerEJB();

        }catch (Exception se) {
            throw new CreateException("Failed to create AccountManager:" + se.getMessage());
        }
        
    }
    

    
    /* gets a reference to the DeviceManager EJB, if necessary */
   /* private void getDeviceManagerEJB() {
        if(dManager==null) {
            EjbProxy proxy = new EjbProxy();
            try {
                dManager = (DeviceManager) proxy.getObj(GlobalNames.EJB_DEVICE_MANAGER);
            }catch(Exception e) {
                if(logger.isDebugEnabled()) {
                    logger.error("[getDeviceManagerEJB()]:" + e.getMessage(), e);
                }else {
                    logger.error("[getDeviceManagerEJB()]:" + e.getMessage());
                }
            }
        }
    }*/

    /* gets an EJB reference to the SysAdminManagerEJB, if necessary */
  /*  public void getSysAdminManagerEJB() {
        if(sManager==null) {
            EjbProxy proxy = new EjbProxy();
            try {
                sManager = (SysAdminManager) proxy.getObj(GlobalNames.EJB_SYSADMIN_MANAGER);
                
            }catch(Exception e) {
                if(logger.isDebugEnabled()) {
                    logger.error("[getSysAdminManagerEJB()]: Exception " + e.getMessage(), e);
                }else {
                    logger.error("[getSysAdminManagerEJB()]: Exception " + e.getMessage());
                }
            }
        }
    }    */
    
  /*  public void getPAIManagerEJB() {
        if(paiCmdRest==null) {
            EjbProxy proxy = new EjbProxy();
            try {
                paiCmdRest = (PAICommandREST) proxy.getObj(GlobalNames.EJB_PAI_REST_SERVICE);
                
            }catch(Exception e) {
                if(logger.isDebugEnabled()) {
                    logger.error("[getPAIManagerEJB()]: Exception " + e.getMessage(), e);
                }else {
                    logger.error("[getPAIManagerEJB()]: Exception " + e.getMessage());
                }
            }
        }
    }*/
    
    public XanbooPrincipal newAccount(XanbooAccount xAccount, XanbooQuota[] xQuotas) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[newAccount()]:");
        }
         
        boolean doMBusSync = true;//MBus
       
        XanbooPrincipal xp = null;
        long userQuota = -1;
        long diskQuota = -1;
        long gatewayQuota = -1;
        
        // first validate the input parameters
        try {
            if(xAccount==null || xAccount.getUser()==null || xAccount.getUser().getUsername().length()==0 || (xAccount.getUser().getUsername().indexOf(" ") != -1) //don't allow white spaces in usernames
                    || xAccount.getUser().getPassword().length()==0 ||
                    !xAccount.getUser().isMaster() || !XanbooUtil.isValidEmail(xAccount.getUser().getEmail()) ) {
                throw new XanbooException(10050, "Missing parameters to newAccount");
            }
            
            /* make sure username and password are not same */
            if(xAccount.getUser().getUsername().equalsIgnoreCase(xAccount.getUser().getPassword())) {
                throw new XanbooException(21011, "Failed to create account. Username and password cannot be same.");
            }
            
            //if the langage value set on the XanbooUser instance is null, use the default setting from 
            //the domain. If the LANGUAGE_ID on the domain is missing (should not happen!!!), use "en"
            if ( xAccount.getUser().getLanguage() == null || xAccount.getUser().getLanguage().equalsIgnoreCase("") )
            {
                
                if ( logger.isDebugEnabled() )
                    logger.debug("[newAccount()] - domain for account is "+xAccount.getDomain());
                
                HashMap domainMap = this.sManager.getDomain(xAccount.getDomain());
                
                if ( logger.isDebugEnabled() )
                    logger.debug("[newAccount()] - domain values = "+domainMap);
                xAccount.getUser().setLanguage( domainMap.get("LANGUAGE_ID") != null ?(String)domainMap.get("LANGUAGE_ID") : "en");
                
                if ( logger.isDebugEnabled() )
                    logger.debug("[newAccount()] - language for user is "+xAccount.getUser().getLanguage());
            }
            
            //Populate quota vars as integers for easy reference later on.
            for (int i=0; i<xQuotas.length; i++) {
                if(xQuotas[i].getResetPeriod()<0 || xQuotas[i].getResetPeriod()>31) xQuotas[i].setResetPeriod(0);
                switch ( xQuotas[i].getQuotaId() ) {
                    case XanbooQuota.DISK : 
                        diskQuota = xQuotas[i].getQuotaValueLong();
                        break;
                    case XanbooQuota.USER :
                        userQuota = xQuotas[i].getQuotaValueLong();
                        break;
                    case XanbooQuota.GATEWAY :
                        gatewayQuota = xQuotas[i].getQuotaValueLong();
                        break;
                    case XanbooQuota.DEVICE :
                        if(xQuotas[i].getQuotaClass()==null ||  xQuotas[i].getQuotaClass().length()==0) {
                            throw new XanbooException( 21035, "Failed to create account. Unsupported device quota.");
                        }
                        if(xQuotas[i].getQuotaClass().equals("0000")) {
                            xQuotas[i].setQuotaId(XanbooQuota.GATEWAY);
                        }
                        break;
                    case XanbooQuota.NOTIFICATION:
                        //must supply a class (notification profile type
                        if(xQuotas[i].getQuotaClass()==null ||  xQuotas[i].getQuotaClass().length()==0) {
                            throw new XanbooException( 21035, "Failed to create account. Unsupported notification quota.");
                        }
                        
                        break;
                    default:
                        throw new XanbooException( 21035, "Failed to create account. Unsupported quota.");
                }
            }
            //Check for DISK & USER quota IDs in the hashmap. Both must be supplied to create an account.
            if ( userQuota == -1 || diskQuota == -1 || gatewayQuota == -1 ) {
                throw new XanbooException( 21043 , "Failed to create account. User, disk & gateway quotas must be specified." );
            }
        }catch(Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[newAccount()]: " + e.getMessage(), e);
            } else {
                logger.error("[newAccount()]: " + e.getMessage());
            }
            doMBusSync = false; // don't send message in case of exception
            throw new XanbooException(10050);
        }

        //if no ext userid is specified, generate a unique one from username
        if(xAccount.getExtAccountId()==null || xAccount.getExtAccountId().length()==0) {
            xAccount.setExtAccountId(xAccount.getUser().getUsername());
        }
        
        //if no ext userid is specified, generate a unique one from username
        if(xAccount.getUser().getExtUserId()==null || xAccount.getUser().getExtUserId().length()==0) {
            xAccount.getUser().setExtUserId(xAccount.getUser().getUsername());
        }
        
        //generate gateway token, if not generated already
        if(xAccount.getToken()==null || xAccount.getToken().length()==0) {
            xAccount.setToken(XanbooUtil.generateRegistrationToken(xAccount.getDomain(), xAccount.getUser().getUsername()));
        }
        
        
        // create the account Database records
        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();
            xp = dao.newAccount(conn, xAccount, userQuota, diskQuota, gatewayQuota);
            //Loop through quotas and apply them to the account
            for ( int i=0; i<xQuotas.length; i++ ) {
                //Gateway, Disk and User quotas already appled during newAccount DAO call, so only deal with devices:
                if ( xQuotas[i].getQuotaId() == XanbooQuota.DEVICE ) {
                    if ( !xQuotas[i].getQuotaClass().equals( "0000" ) ) {   //gateway quota already applied
                        xQuotas[i].setAccountId( xp.getAccountId() );
                        dao.updateAccountQuota( conn, xQuotas[i], true );
                    }
                }else if ( xQuotas[i].getQuotaId() == XanbooQuota.NOTIFICATION ) {
                    xQuotas[i].setAccountId( xp.getAccountId() );
                    dao.updateAccountQuota( conn, xQuotas[i], true );
                }
            }
            
            logger.info("[newAccount()]: ACCOUNT REGISTERED account:" + xp.getAccountId() + "@" + xp.getDomain() + ", status:" + xAccount.getStatus());
            
        }catch (XanbooException xe) {
            rollback = true;
            doMBusSync = false; // don't send message in case of exception
            throw xe;
        }catch (Exception e) {
            rollback = true; 
            doMBusSync = false; // don't send message in case of exception
            if(logger.isDebugEnabled()) {
              logger.error("[newAccount()]: " + e.getMessage(), e);
            }else {
              logger.error("[newAccount()]: " + e.getMessage());
            }
            
            throw new XanbooException(10030, "[newAccount()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
            
            //MBus - Send account.create message
            if(GlobalNames.MBUS_SYNC_ENABLED && doMBusSync){
            	
            	String email = xAccount.getUser()!= null ? xAccount.getUser().getEmail() : null ;
            	String extUserId = xAccount.getUser()!= null ? xAccount.getUser().getExtUserId() : null ;
            	
            	 if(logger.isDebugEnabled()) {
            		 
            		 StringBuffer sb = new StringBuffer("[MBus : publish messages : AccountManagerEJB.newAccount()]:");
            		 sb.append("\n Domain : " + xp.getDomain() );
            		 sb.append("\n AccountId : " + xp.getAccountId() );
            		 sb.append("\n ExtAccountId : " + xAccount.getExtAccountId() );
            		 sb.append("\n Status : " + xAccount.getStatus() );
            		 sb.append("\n UserId : " + xp.getUserId() );
            		 sb.append("\n Username : " + xp.getUsername() );
            		 sb.append("\n Email : " + email );
            		 sb.append("\n ExtUserId : " + extUserId );
            		 
                     logger.debug( sb.toString() );
                   }
            	 
            	MBusSynchronizer.newAccount(xp.getDomain(), xp.getAccountId(), xAccount.getExtAccountId(), xAccount.getStatus(), xp.getUserId(),
            			xp.getUsername(),email,extUserId , "AccountManagerEJB.newAccount");
            	
            	if (logger.isDebugEnabled()) {

					StringBuffer sb = new StringBuffer(
							"[MBus : publish messages user.create: AccountManagerEJB.newAccount()]:");
					sb.append("\n Domain : " + xp.getDomain());
					sb.append("\n AccountId : " + xp.getAccountId());
					sb.append("\n ExtAccountId : " + xAccount.getExtAccountId());
					sb.append("\n UserId : " + xp.getUserId());
					sb.append("\n Username : " + xp.getUsername());
					sb.append("\n Email : " + email);
					sb.append("\n ExtUserId : " + extUserId);

					logger.debug(sb.toString());
				}

				DLCoreMBusUserChange user = new DLCoreMBusUserChange();

				user.setAccId(xp.getAccountId());
				user.setDomain(xp.getDomain());
				user.setEmail(email);
				user.setExtAccId(xAccount.getExtAccountId());
				user.setExtUserid(extUserId);
				user.setFirstName(null);
				user.setLastName(null);
				user.setLogin(xp.getUsername());
				user.setPh1(null);
				user.setPh2(null);
				user.setSrcAppId("AccountManagerEJB.newAccount");
				user.setSrcOriginatorId(xp.getUserId() + "");
				user.setUserId(xp.getUserId() + "");

				MBusSynchronizer.newUser(user);
			
            }
            
        }

        //Return a valid XanbooPrincipal for this newly created account.
        return xp;
    }
    
    
    public XanbooPrincipal authenticateUser(String domainId, String username, String password, int maxLoginAttempts) throws XanbooException {
        return authenticateUser(domainId, username, password);
    }

    public XanbooPrincipal authenticateUser(String domainId, String username, String password) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[authenticateUser()]: domain=" + domainId + ", username=" + username);
        }
        
        // first validate the input parameters
        if(domainId==null || username==null || domainId.trim().length()==0 || username.trim().length()==0
                || password==null || password.trim().length()==0) {
            throw new XanbooException(10050);
        }
        LicenseControl licenseControl = LicenseControl.getInstance();
        if(licenseControl != null && licenseControl.isLicenseRevokedOrExpired(domainId)){
        	logger.error("[authenticateUser()] Domain License exprired/revoked for domainId : "+ domainId);
        	throw new XanbooException(21422,"License revoked or expired for domainId: " +domainId);
        }
        
        // now check the validity of the user
        XanbooPrincipal xp = null;
        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();
            xp = dao.authenticateUser(conn, domainId, username, password);
        }catch (XanbooException xe) {
            if( xe.getCode() == 210502 ) { // no rollback - login fail count changed (special nak do not change)
                rollback = false;
                throw new XanbooException( 21050, xe );
            } else {
                rollback=true;
                throw xe;
            }
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[authenticateUser()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[authenticateUser()]: Exception:" + e.getMessage());
            }            
            throw new XanbooException(10030, "[authenticateUser()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
        //locate all gateways belonging to this account and send server/nat proxy polls to wake them up
        if(dManager!=null && !rollback) {
            String gguid = null;
            try {
                XanbooResultSet xres = dManager.getDeviceListByClass(xp, "0000");
                for(int i=0; i<xres.size(); i++) {
                    gguid = xres.getElementString(i, "GATEWAY_GUID");
                    if(gguid!=null && gguid.length()>0 && !XanbooUtil.isExternalServiceDevice(gguid)) {  //check valid ggguid and not an external service gateway
                        //set reset oid value to 0 (noop) to wake up device
                        dManager.setMObject(xp, gguid, "0", "1", "0");
                    }
                }
            }catch(Exception eee) { //log exception and continue
                if(logger.isDebugEnabled()) {
                    logger.warn("[authenticateUser()]: Exception waking gateway:" + gguid + ", EXCEPTION:" + eee.getMessage(), eee);
                }else {
                    logger.warn("[authenticateUser()]: Exception waking gateway:" + gguid + ", EXCEPTION:" + eee.getMessage());
                }
            }
        }

        return xp;
    }

    public XanbooPrincipal authenticateUser(String domainId, String username, XanbooSecurityQuestion sq) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[authenticateUser()]: domain=" + domainId + ", username=" + username + ", secQ=" + sq.getSecQuestionId());
        }
        
        // first validate the input parameters
        if(domainId==null || username==null || domainId.trim().length()==0 || username.trim().length()==0 || !sq.isValid()) {
            throw new XanbooException(10050);
        }
        
        // now check the validity of the user
        XanbooPrincipal xp = null;
        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();
            xp = dao.authenticateUser(conn, domainId, username, sq);
        }catch (XanbooException xe) {
            if( xe.getCode() == 210502 ) { // no rollback - login fail count changed (special nak do not change)
                rollback = false;
                throw new XanbooException( 21061, xe );
            } else {
                rollback=true;
                throw xe;
            }
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[authenticateUser()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[authenticateUser()]: Exception:" + e.getMessage());
            }            
            throw new XanbooException(10030, "[authenticateUser()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }

        return xp;
    }    
    
    
    
    public XanbooPrincipal authenticateUser(String domainId, String username, XanbooSecurityQuestion[] sqs) throws XanbooException {
    	
        
        // first validate the input parameters
        if(domainId==null || username==null || domainId.trim().length()==0 || username.trim().length()==0 || sqs.length != 2 || !sqs[0].isValid() || !sqs[1].isValid()) {
            throw new XanbooException(10050);
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("[authenticateUser()]: domain=" + domainId + ", username=" + username + ", secQ1=" + sqs[0].getSecQuestionId() + ", secQ2=" + sqs[1].getSecQuestionId());
        }        
        
        // now check the validity of the user
        XanbooPrincipal xp = null;
        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();
            xp = dao.authenticateUser(conn, domainId, username, sqs);
        }catch (XanbooException xe) {
            if( xe.getCode() == 210502 ) { // no rollback - login fail count changed (special nak do not change)
                rollback = false;
                throw new XanbooException( 21061, xe );
            } else {
                rollback=true;
                throw xe;
            }
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[authenticateUser()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[authenticateUser()]: Exception:" + e.getMessage());
            }            
            throw new XanbooException(10030, "[authenticateUser()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }

        return xp;
    }


    public XanbooPrincipal authenticateUser(String domainId, String extUserId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[authenticateUser()]: domain=" + domainId + ", extUserId=" + extUserId);
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
        XanbooPrincipal xp = null;
        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();
            xp = dao.authenticateUser(conn, domainId, extUserId);
        }catch (XanbooException xe) {
            //if( xe.getCode() != )
            rollback=true;            
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[authenticateUser()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[authenticateUser()]: Exception:" + e.getMessage());
            }                        
            throw new XanbooException(10030, "[authenticateUser()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
        
        //locate all gateways belonging to this account and send server/nat proxy polls to wake them up
        if(dManager!=null && !rollback) {
            try {
                XanbooResultSet xres = dManager.getDeviceListByClass(xp, "0000");
                for(int i=0; i<xres.size(); i++) {
                    String gguid = xres.getElementString(i, "GATEWAY_GUID");
                    if(gguid!=null && gguid.length()>0) {
                        //set reset oid value to 0 (noop) to wake up device
                        dManager.setMObject(xp, gguid, "0", "1", "0");
                    }
                }
            }catch(Exception eee) { //log exception and continue
                if(logger.isDebugEnabled()) {
                    logger.error("[authenticateUser()]: Exception waking gateway:" + eee.getMessage(), eee);
                }else {
                    logger.error("[authenticateUser()]: Exception waking gateway:" + eee.getMessage());
                }
            }
        }

        return xp;
    }    
    
    

     public long newUser(XanbooPrincipal xCaller, XanbooUser xUser) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[newUser()]:");
        }
        boolean doMBusSync = true;//MBus 
        // access allowed, proceed and validate input params
        if(xUser==null || xUser.getAccountId()<1 || xUser.getUsername().length()==0 || (xUser.getUsername().indexOf(" ") != -1) ||
                xUser.getPassword().length()==0 || !XanbooUtil.isValidEmail(xUser.getEmail()) || xUser.isMaster()) {
            throw new XanbooException(10050);
        }

        /* make sure username and password are not same */
        if(xUser.getUsername().equalsIgnoreCase(xUser.getPassword())) {
            throw new XanbooException(21011, "Failed to create user. Username and password cannot be same.");
        }
        
        //if the langage value set on the XanbooUser instance is null, use the default setting from 
        //the domain. If the LANGUAGE_ID on the domain is missing (should not happen!!!), use "en"
        try
        {
            if ( xUser.getLanguage() == null || xUser.getLanguage().equalsIgnoreCase("") )
            {
                HashMap domainMap = this.sManager.getDomain(xCaller.getDomain());
                xUser.setLanguage( domainMap.get("LANGUAGE_ID") != null ?(String)domainMap.get("LANGUAGE_ID") : "en");
            }
        }
        catch(RemoteException re)
        {
            logger.warn("[newUser()] - problem retreiving domain information, using \"en\" for language", re);
            xUser.setLanguage("en");
        }
        
        //encrypt the profileData if it's set
        try {
            if (xUser.getProfileData() != null && xUser.getProfileData().length() > 0) {
                xUser.setProfileData(encrypt(xUser.getProfileData()));
            }
        }
        catch (Exception ee)
        {
            logger.error("[newUser()] - problem encrypting profile data", ee);
            throw new XanbooException(21421);
        }

        //if no ext userid is specified, generate a unique one from username
        if(xUser.getExtUserId()==null || xUser.getExtUserId().length()==0) {
            xUser.setExtUserId(xUser.getUsername());
        }

        Connection conn=null;
        boolean rollback=false;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            dao.newUser(conn, xUser);
        }catch (XanbooException xe) {
        	  doMBusSync = false; // don't send message in case of exception
            rollback=true;
            throw xe;
        }catch (Exception e) {
        	  doMBusSync = false; // don't send message in case of exception
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[newUser()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[newUser()]: Exception:" + e.getMessage());
            }                        
            throw new XanbooException(10030, "[newUser()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
            
            //MBus Start- Send user.create message
            if(GlobalNames.MBUS_SYNC_ENABLED && doMBusSync){
            	

                       	
            	 if(logger.isDebugEnabled()) {
            		 
            		 StringBuffer sb = new StringBuffer("[MBus : publish messages : AccountManagerEJB.newUser()]:");
            		 sb.append("\n Domain : " + xCaller.getDomain() );
            		 sb.append("\n AccountId : " + xCaller.getAccountId());
            		 sb.append("\n ExtAccountId : " + xUser.getExtUserId() );
            		 sb.append("\n Status : " + xUser.getStatus() );
            		 sb.append("\n Email : " + xUser.getEmail() );
            		 sb.append("\n UserId : " + xCaller.getUserId() );
            		
            		 
                     logger.debug( sb.toString() );
                   }
            	 
            	MBusSynchronizer.newUser(xCaller.getDomain(), xCaller.getAccountId(), null, null, xUser.getEmail(), xUser.getExtUserId(), null, null, null, null, xUser.getUserId() ,  "AccountManagerEJB.newUser" );
   
            }
            // MBus end
            
        }

        return xUser.getUserId();   // return the user ID

     }
     


     public long newUser(XanbooPrincipal xCaller, XanbooUser xUser, XanbooContact xContact) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[newUser()]:");
        }
        boolean doMBusSync = false;//MBus 
        try {
            this.newUser(xCaller, xUser);
            doMBusSync = true;//MBus 
            this.updateUserInformation(xCaller, xContact);
        }catch(XanbooException xe) {
        	doMBusSync = false;
            throw xe;
        }finally{
        	   //MBus Start- Send user.update message
            if(GlobalNames.MBUS_SYNC_ENABLED && doMBusSync){
            	

                       	
            	 if(logger.isDebugEnabled()) {
            		 
            		 StringBuffer sb = new StringBuffer("[MBus : publish messages : AccountManagerEJB.newUser()]:");
            		 sb.append("\n Domain : " + xCaller.getDomain() );
            		 sb.append("\n AccountId : " + xCaller.getAccountId());
            		 sb.append("\n ExtAccountId : " + xUser.getExtUserId() );
            		 sb.append("\n Status : " + xUser.getStatus() );
            		 sb.append("\n Email : " + xUser.getEmail() );
            		 
            		 sb.append("\n login : " + xContact.getUsername() );
            		 sb.append("\n ph1 - phone : " + xContact.getPhone());
            		 sb.append("\n ph2 - cellphone : " + xContact.getCellPhone() );
            		 sb.append("\n fname : " + xContact.getFirstName() );
            		 sb.append("\n lname : " + xContact.getLastName() );
            		 
            		 sb.append("\n UserId : " + xCaller.getUserId() );
            		
            		 
                     logger.debug( sb.toString() );
                   }
            	 
            	MBusSynchronizer.updateUser( xCaller.getDomain(), xCaller.getAccountId(), null, xContact.getUsername(), xUser.getEmail(), xUser.getExtUserId(), xContact.getPhone(), xContact.getCellPhone(),
            			xContact.getFirstName(), xContact.getLastName(), xUser.getUserId() ,  "AccountManagerEJB.newUser.updateUserInformation" );
   
            }
            // MBus end
        }
        
        return xUser.getUserId();   // return the user ID

     }
     
     
     public void deleteUser(XanbooPrincipal xCaller, long userId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteUser()]:");
        }
        boolean doMBusSync = true;//MBus 
        // validate input params
        if(userId<1) {
            throw new XanbooException(10050);
        }

        Connection conn=null;
        boolean rollback=false;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            dao.deleteUser(conn, xCaller.getAccountId(), userId);
        }catch (XanbooException xe) {
        	doMBusSync = false;//MBus 
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            doMBusSync = false;//MBus 
            if(logger.isDebugEnabled()) {
              logger.error("[deleteUser()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[deleteUser()]: Exception:" + e.getMessage());
            }                                    
            throw new XanbooException(10030, "[deleteUser()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
            //MBus Start- Send user.update message
            if(GlobalNames.MBUS_SYNC_ENABLED && doMBusSync){
            	

                       	
            	 if(logger.isDebugEnabled()) {
            		 
            		 StringBuffer sb = new StringBuffer("[MBus : publish messages : AccountManagerEJB.deleteUser()]:");
            		 sb.append("\n Domain : " + xCaller.getDomain() );
            		 sb.append("\n AccountId : " + xCaller.getAccountId());
            		 sb.append("\n login : " + xCaller.getUsername());
            		 sb.append("\n UserId : " + userId );
            		
            		 
                     logger.debug( sb.toString() );
                   }
            	 
            	MBusSynchronizer.deleteUser( xCaller.getDomain(), xCaller.getAccountId(),null, xCaller.getUsername(), userId,  "AccountManagerEJB.deleteUser");
            	
            			
   
            }
            // MBus end
            
        }

     }


     public void updateUser(XanbooPrincipal xCaller, XanbooUser xUser) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateUser()]: acc=" + xUser.getAccountId() + ", usrid=" + xUser.getUserId());
        }
        boolean doMBusSync = true;//MBus 
        // validate the input parameters
        if(xUser==null || xUser.getAccountId()<1 || xUser.getUserId()<1 ) {
            throw new XanbooException(10050);
        }

        Connection conn=null;
        boolean rollback=false;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);

            //non-master users cannot update other users
            if(!xCaller.isMaster() && xCaller.getUserId()!=xUser.getUserId()) {
                throw new XanbooException(10005);   // invalid caller
            }
            
            //for non-master users, prevent fifo purging preference sets
            if(!xCaller.isMaster()) xUser.setFifoPurgingFlag(-1);

            //encrypt the profileData if it's set
            try {
                if (xUser.getProfileData() != null && xUser.getProfileData().length() > 0) {
                    xUser.setProfileData(encrypt(xUser.getProfileData()));
                }
            } catch (Exception ee) {
                logger.error("[updateUser()] - problem encrypting profile data", ee);
                throw new XanbooException(21421);
            }
            
            conn=dao.getConnection();
            dao.updateUser(conn, xUser);
            
        }catch (XanbooException xe) {
        	doMBusSync = false;//MBus 
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            doMBusSync = false;//MBus 
            if(logger.isDebugEnabled()) {
              logger.error("[updateUser()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[updateUser()]: Exception:" + e.getMessage());
            }              
            throw new XanbooException(10030, "[updateUser()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
            
            //MBus Start- Send user.update message
            if(GlobalNames.MBUS_SYNC_ENABLED && doMBusSync){
            	

                       	
            	 if(logger.isDebugEnabled()) {
            		 
            		 StringBuffer sb = new StringBuffer("[MBus : publish messages : AccountManagerEJB.updateUser()]:");
            		 sb.append("\n Domain : " + xCaller.getDomain() );
            		 sb.append("\n AccountId : " + xCaller.getAccountId());
            		 sb.append("\n ExtAccountId : " + xUser.getExtUserId() );
            		 sb.append("\n Status : " + xUser.getStatus() );
            		 sb.append("\n Email : " + xUser.getEmail() );
            		 sb.append("\n UserId : " + xCaller.getUserId() );
            		
            		 
                     logger.debug( sb.toString() );
                   }
            	 
            	MBusSynchronizer.updateUser( xCaller.getDomain(), xCaller.getAccountId(), null, null, xUser.getEmail(), xUser.getExtUserId(), null, null,
            			null, null, xUser.getUserId() ,  "AccountManagerEJB.updateUser" );
   
            }
            // MBus end
        }

     }
 

     public XanbooResultSet getUserList(XanbooPrincipal xCaller) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getUserList()]: acc=" + xCaller.getAccountId());
        }
        
        Connection conn=null;       
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);

            conn=dao.getConnection();
            return dao.getUserList(conn, xCaller.getAccountId());
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getUserList()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[getUserList()]: Exception:" + e.getMessage());
            }
            throw new XanbooException(10030, "[getUserList()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
     }

     

     public XanbooContact getUserInformation(XanbooPrincipal xCaller, long userId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getUserInformation()]:");
        }

        // first validate the input parameters
        if(userId<1) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;      
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);

            conn=dao.getConnection();
            return dao.getUserInformation(conn, xCaller.getAccountId(), userId);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getUserInformation()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[getUserInformation()]: Exception:" + e.getMessage());
            }            
            throw new XanbooException(10030, "[getUserInformation()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
     }
     

     public void updateUserInformation(XanbooPrincipal xCaller, XanbooContact xContact) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateUserInformation()]:");
        }
        boolean doMBusSync = true;//MBus 
        // validate the input parameters
        try {
            if(xContact==null || xContact.getAccountId()<1 || xContact.getUserId()<1 ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
        	doMBusSync = false;//MBus 
            throw new XanbooException(10050);
        }
        
        if ( xContact.getEmailProfileType() == -1 )
            xContact.setEmailProfileType(GlobalNames.EMAIL_PROFILE_TYPE);
        //profile type for phone 
        if ( xContact.getPhone1ProfileType() == -1 && xContact.getPhone1SMS() )
            xContact.setPhone1ProfileType(GlobalNames.SMS_PROFILE_TYPE);
        //profile type for phone_cell
        if ( xContact.getPhone2ProfileType() == -1 && xContact.getPhone2SMS() )
            xContact.setPhone2ProfileType(GlobalNames.SMS_PROFILE_TYPE);
        //profile type for fax
        if ( xContact.getPhone3ProfileType() == -1 && xContact.getPhone3SMS() )
            xContact.setPhone3ProfileType(GlobalNames.SMS_PROFILE_TYPE);
        
        Connection conn=null;
        boolean rollback=false;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);

            conn=dao.getConnection();
            dao.updateUserInformation(conn, xContact);
        }catch (XanbooException xe) {
            rollback=true;
            doMBusSync = false;//MBus 
            throw xe;
        }catch (Exception e) {
            rollback=true;
            doMBusSync = false;//MBus 
            if(logger.isDebugEnabled()) {
              logger.error("[updateUserInformation()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[updateUserInformation()]: Exception:" + e.getMessage());
            }            
            throw new XanbooException(10030, "[updateUserInformation()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
            
            //MBus Start- Send user.update message
            if(GlobalNames.MBUS_SYNC_ENABLED && doMBusSync){
            	

                       	
            	 if(logger.isDebugEnabled()) {
            		 
            		 StringBuffer sb = new StringBuffer("[MBus : publish messages : AccountManagerEJB.updateUserInformation()]:");
            		 sb.append("\n Domain : " + xCaller.getDomain() );
            		 sb.append("\n AccountId : " + xCaller.getAccountId());
            		 sb.append("\n Email : " + xContact.getEmail() );
            		 
            		 sb.append("\n login : " + xContact.getUsername() );
            		 sb.append("\n ph1 - phone : " + xContact.getPhone());
            		 sb.append("\n ph2 - cellphone : " + xContact.getCellPhone() );
            		 sb.append("\n fname : " + xContact.getFirstName() );
            		 sb.append("\n lname : " + xContact.getLastName() );
            		 
            		 sb.append("\n UserId : " + xCaller.getUserId() );
            		
            		 
                     logger.debug( sb.toString() );
                   }
            	 
            	MBusSynchronizer.updateUser( xCaller.getDomain(), xCaller.getAccountId(), null, xContact.getUsername(), xContact.getEmail(), null, xContact.getPhone(), xContact.getCellPhone(),
            			xContact.getFirstName(), xContact.getLastName(), xCaller.getUserId() ,  "AccountManagerEJB.updateUserInformation" );
   
            }
            // MBus end
        }
     }
     
     public XanbooQuota getAccountQuota(XanbooPrincipal xCaller, int quotaId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getAccountQuota(2)]:");
        }
        
        return getAccountQuota( xCaller, quotaId, null );

     }
     
     public XanbooQuota getAccountQuota(XanbooPrincipal xCaller, int quotaId, String quotaClassId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getAccountQuota(1)]:");
        }
        
        XanbooQuota xQuota = new XanbooQuota( xCaller.getAccountId(), quotaId, 0L, 0L);
        xQuota.setQuotaClass( quotaClassId );
        
        // first validate the input parameters
        try {
            if(xQuota==null || xQuota.getAccountId()<1 || xQuota.getQuotaId()<0) {
                throw new XanbooException(10050);
            }
            
            /* Gateway quotas always represented as XanbooQuota.GATEWAY / classId '0000' */
            if ( xQuota.getQuotaId() == XanbooQuota.DEVICE && xQuota.getQuotaClass()!=null && xQuota.getQuotaClass().equals( "0000" ) ) {
                xQuota.setQuotaId( XanbooQuota.GATEWAY );
            } else if ( xQuota.getQuotaId() == XanbooQuota.GATEWAY ) {
                xQuota.setQuotaClass( "0000" );
            }
            
        }catch(Exception e) {
            throw new XanbooException(10050);
        }

        Connection conn=null;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);

            conn=dao.getConnection();
            dao.getAccountQuota(conn, xQuota);
            
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getAccountQuota()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[getAccountQuota()]: Exception:" + e.getMessage());
            }              
            throw new XanbooException(10030, "[getAccountQuota()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
        
        return xQuota;
     }

     
     public XanbooResultSet getAccountQuotaList(XanbooPrincipal xCaller) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getAccountQuotaList()]:");
        }

        Connection conn=null;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);

            conn=dao.getConnection();
            return dao.getAccountQuotaList(conn, xCaller.getAccountId());
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getAccountQuotaList()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[getAccountQuotaList()]: Exception:" + e.getMessage());
            }                          
            throw new XanbooException(10030, "[getAccountQuotaList()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
     }
     
     
     public void updateAccountQuota(XanbooPrincipal xCaller, XanbooQuota xQuota, boolean forceFlag) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateAccountQuota()]:");
        }
         
        // validate the input parameters
        try {
            if(xQuota==null || xQuota.getAccountId()<1 || xQuota.getQuotaId()<0 ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }

        if(xQuota.getResetPeriod()<0 || xQuota.getResetPeriod()>31) xQuota.setResetPeriod(0);
        
        if(xQuota.getQuotaId() == XanbooQuota.DEVICE) {
            //must supply a class (notification profile type
            if(xQuota.getQuotaClass()==null ||  xQuota.getQuotaClass().length()==0) {
                throw new XanbooException( 21090, "Failed to update account quota. Unsupported device quota.");
            }
            
            if(xQuota.getQuotaClass().equals("0000")) {
                xQuota.setQuotaId(XanbooQuota.GATEWAY);
            }
        }

        if(xQuota.getQuotaId() == XanbooQuota.NOTIFICATION) {
            //must supply a class (notification profile type
            if(xQuota.getQuotaClass()==null ||  xQuota.getQuotaClass().length()==0) {
                throw new XanbooException( 21090, "Failed to create account quota. Unsupported notification quota.");
            }
        }
        
        
        Connection conn=null;
        boolean rollback=false;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);

            conn=dao.getConnection();
            dao.updateAccountQuota(conn, xQuota, forceFlag);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[updateAccountQuota()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[updateAccountQuota()]: Exception:" + e.getMessage());
            }                          
            throw new XanbooException(10030, "[updateAccountQuota()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
     }

     
     public XanbooResultSet getNotificationProfile(XanbooPrincipal xCaller) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getNotificationProfile()]:");
        }

        Connection conn=null;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);

            conn=dao.getConnection();
            return dao.getNotificationProfile(conn, xCaller.getAccountId(), xCaller.getUserId());
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getNotificationProfile()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[getNotificationProfile()]: Exception:" + e.getMessage());
            }                
            throw new XanbooException(10030, "[getNotificationProfile()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
        
     }     



     public XanbooNotificationProfile[] getNotificationProfile(XanbooPrincipal xCaller, boolean getEmergencyContacts) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getNotificationProfile()]:");
        }

        Connection conn=null;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);

            conn=dao.getConnection();
            return dao.getNotificationProfile(conn, xCaller.getAccountId(), xCaller.getUserId(), getEmergencyContacts);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getNotificationProfile()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[getNotificationProfile()]: Exception:" + e.getMessage());
            }
            throw new XanbooException(10030, "[getNotificationProfile()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }

     }


     /* deprecated */
     public long newNotificationProfile(XanbooPrincipal xCaller, int profileType, String profileAddress) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[newNotificationProfile()]:");
        }

        // validate the input parameters
        XanbooNotificationProfile xnp = new XanbooNotificationProfile(false, null, profileType, profileAddress);

        //verify email / pager address
        if ( profileType == this.PROFILE_TYPE_EMAIL && !XanbooUtil.isValidEmail(profileAddress)) {
            throw new XanbooException(21230, "Failed to create notification profile. Invalid email address");
        //FOR NOW!} else if (profileType != this.PROFILE_TYPE_EMAIL && !XanbooUtil.isValidString(profileAddress, "0123456789")) {
        //    throw new XanbooException(21225, "Failed to create notification profile. Invalid phone/pager number");
        } 

        return newNotificationProfile(xCaller, xnp);
     }



     public long newNotificationProfile(XanbooPrincipal xCaller, XanbooNotificationProfile xnp) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[newNotificationProfile()]:");
        }

        // validate the input parameters
        if((xnp.isEmergencyContact() && !xnp.isValidEmergencyContact()) || (!xnp.isEmergencyContact() && !xnp.isValidNotificationProfile())) {
            throw new XanbooException(10050);
        }

        Connection conn=null;
        boolean rollback=false;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            long npId = dao.newNotificationProfile(conn, xCaller.getAccountId(), xCaller.getUserId(), xnp);

            //sync with SBN, if a real gguid is associated with the emergency contact!
            if(GlobalNames.MODULE_SBN_ENABLED && xnp.isValidEmergencyContact() && xnp.getGguid()!=null) {
                SBNSynchronizer sbn = new SBNSynchronizer();
                boolean sbnOK = sbn.addUpdateEmergencyContact(xnp);
                if(!sbnOK) {    //SBN sync failed!
                    throw new XanbooException(21232, "Failed to create notification profile. SBN synchronization failed.");
                }
            }
            
            if ( xnp.isEmergencyContact() && xnp.isSendNotification() )
            {
                XanbooUser xUser = dao.getUser(conn,  xCaller, xCaller.getUserId());
                dao.queueAction(conn, xUser.getDomain(), xUser.getAccountId(), xUser.getExtUserId(), "0", "0", "1075", "", "Auto-notify", xUser.getLanguage(), xUser.getTimezone(), "", xUser.getEmail() );
            }

            return npId;
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[newNotificationProfile()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[newNotificationProfile()]: Exception:" + e.getMessage());
            }
            throw new XanbooException(10030, "[newNotificationProfile()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
     }

     
     
     public long[] newNotificationProfile(XanbooPrincipal xCaller, XanbooNotificationProfile[] xnp) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[newNotificationProfile()]: BULK");
        }
        
        boolean isAllEmergencyContact = true;
        boolean isAllNotificationProfile = true;
        boolean isSendNotification = false;
        
        // validate the input parameters
        for(int i=0; i<xnp.length; i++) {
            if(!xnp[i].isEmergencyContact()) isAllEmergencyContact=false;
            if(xnp[i].isEmergencyContact()) isAllNotificationProfile=false;
            if((xnp[i].isEmergencyContact() && !xnp[i].isValidEmergencyContact()) || (!xnp[i].isEmergencyContact() && !xnp[i].isValidNotificationProfile())) {
                throw new XanbooException(10050);
            }
            if ( xnp[i].isSendNotification()) isSendNotification = true;
        }
        
        if(!isAllEmergencyContact && !isAllNotificationProfile) {
            throw new XanbooException(10050, "Profiles must be of the same type!");
        }
        
        
        long[] npId = new long[xnp.length];
        
        Connection conn=null;
        boolean rollback=false;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            
            //sync with SBN one at a time, if all are NOT emergency contacts with real gguids
            for(int i=0; i<xnp.length; i++) {
                npId[i] = dao.newNotificationProfile(conn, xCaller.getAccountId(), xCaller.getUserId(), xnp[i]);
            }
            
            //BULK sync with SBN, if all are emergency contacts
            if(GlobalNames.MODULE_SBN_ENABLED && isAllEmergencyContact) {
                SBNSynchronizer sbn = new SBNSynchronizer();
                boolean sbnOK = sbn.addUpdateEmergencyContact(xnp);
                if(!sbnOK) {    //SBN sync failed!
                    throw new XanbooException(21232, "Failed to create notification profile. SBN synchronization failed.");
                }
            }
            
            if ( isAllEmergencyContact && isSendNotification )
            {
                XanbooUser xUser = dao.getUser(conn,  xCaller, xCaller.getUserId());
                dao.queueAction(conn, xUser.getDomain(), xUser.getAccountId(), xUser.getExtUserId(), "0", "0", "1075", "", "Auto-notify", xUser.getLanguage(), xUser.getTimezone(), "", xUser.getEmail() );
            }
            
            return npId;
            
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[newNotificationProfile()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[newNotificationProfile()]: Exception:" + e.getMessage());
            }
            throw new XanbooException(10030, "[newNotificationProfile()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
        
     }
     
     
     public void deleteNotificationProfile(XanbooPrincipal xCaller, long[] profileId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteNotificationProfile()]:");
        }

        if(profileId != null) {
            // validate the input parameters
            for(int i=0; i<profileId.length; i++) {
                if(profileId[i]<0) {
                    throw new XanbooException(10050);
                }
            }
        }

        Connection conn=null;
        boolean rollback=false;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);

            conn=dao.getConnection();
            dao.deleteNotificationProfile(conn, xCaller.getAccountId(), xCaller.getUserId(), profileId);
        }catch (XanbooException xe) {
            rollback=true;            
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[deleteNotificationProfile()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[deleteNotificationProfile()]: Exception:" + e.getMessage());
            }                                        
            throw new XanbooException(10030, "[deleteNotificationProfile()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
     }
     
     
     public void deleteNotificationProfile(XanbooPrincipal xCaller,XanbooNotificationProfile[] xnp)throws XanbooException
     {
         if ( logger.isDebugEnabled())
             logger.debug("[deleteNotificationProfile() - profileArray]");
         boolean sendNotification = false;
         boolean emergencyContact = false;
         
         if ( xnp != null )
         {
             for ( XanbooNotificationProfile profile : xnp )
             {
                 if ( profile == null || profile.getProfileId() < 0 )
                     throw new XanbooException(10050);
             }
         }
         
        long[] profileIdArray = new long[xnp.length];
        int i = 0;
        for ( XanbooNotificationProfile p : xnp)
        {
            profileIdArray[i] = p.getProfileId();
            i++;
            if ( p.isSendNotification() ) sendNotification = true;
            if ( p.isEmergencyContact() ) emergencyContact = true;
        }
         
        Connection conn=null;
        boolean rollback=false;
        try 
        {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);

            conn=dao.getConnection();
            dao.deleteNotificationProfile(conn, xCaller.getAccountId(), xCaller.getUserId(), profileIdArray);
            
            if ( sendNotification && emergencyContact)  //at least one of the notification profiles was an emergencyContact and had sendNotification=true
            {
                XanbooUser xUser = dao.getUser(conn,  xCaller, xCaller.getUserId());
                dao.queueAction(conn, xUser.getDomain(), xUser.getAccountId(), xUser.getExtUserId(), "0", "0", "1075", "", "Auto-notify", xUser.getLanguage(), xUser.getTimezone(), "", xUser.getEmail() );
            }
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
              logger.error("[deleteNotificationProfile() - profileArray]: Exception:" + e.getMessage(), e);
            }
            else 
            {
              logger.error("[deleteNotificationProfile() - profileArray]: Exception:" + e.getMessage());
            }                                        
            throw new XanbooException(10030, "[deleteNotificationProfile() - profileArray]: Exception:" + e.getMessage());
        }
        finally 
        {
            dao.closeConnection(conn, rollback);
        }
     }

     public void testNotificationProfile(XanbooPrincipal xCaller, long[] profileId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[testNotificationProfile()]:");
        }

        if(profileId != null) {
            // validate the input parameters
            for(int i=0; i<profileId.length; i++) {
                if(profileId[i]<0) {
                    throw new XanbooException(10050);
                }
            }
        }else {
            throw new XanbooException(10050);
        }

        Connection conn=null;
        boolean rollback=false;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);

            conn=dao.getConnection();
            dao.testNotificationProfile(conn, xCaller.getAccountId(), xCaller.getUserId(), profileId);
        }catch (XanbooException xe) {
            rollback=true;            
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[testNotificationProfile()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[testNotificationProfile()]: Exception:" + e.getMessage());
            }                                        
            throw new XanbooException(10030, "[testNotificationProfile()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
     }


     /* deprecated */
     public void updateNotificationProfile(XanbooPrincipal xCaller, long profileId, int profileType, String profileAddress) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateNotificationProfile()]:");
        }
        

        // validate the input parameters
        XanbooNotificationProfile xnp = new XanbooNotificationProfile(false, null, profileType, profileAddress);
        xnp.setProfileId(profileId);
        
        
        //verify email / pager address
        if ( profileType == this.PROFILE_TYPE_EMAIL && !XanbooUtil.isValidEmail(profileAddress)) {
            throw new XanbooException(21240, "Failed to update notification profile. Invalid email address");
        ///} else if (profileType != this.PROFILE_TYPE_EMAIL && !XanbooUtil.isValidPager(profileAddress)) {
        ///    throw new XanbooException(21235, "Failed to update notification profile. Invalid pager number");
        } 

        updateNotificationProfile(xCaller, xnp);

     }     


     public void updateNotificationProfile(XanbooPrincipal xCaller, XanbooNotificationProfile xnp) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateNotificationProfile()]:");
        }


        // validate the input parameters
        if((xnp.isEmergencyContact() && !xnp.isValidEmergencyContact()) || (!xnp.isEmergencyContact() && !xnp.isValidNotificationProfile())) {
            throw new XanbooException(10050);
        }

        Connection conn=null;
        boolean rollback=false;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);

            conn=dao.getConnection();
            if(xnp.getProfileId()<=0) {  //add a new profile
                long npId = dao.newNotificationProfile(conn, xCaller.getAccountId(), xCaller.getUserId(), xnp);
                xnp.setProfileId(npId);
            }else {                         // update existing profile
                dao.updateNotificationProfile(conn, xCaller.getAccountId(), xCaller.getUserId(), xnp);
            }


            //sync with SBN
            if(GlobalNames.MODULE_SBN_ENABLED && xnp.isValidEmergencyContact() && xnp.getGguid()!=null) {
                SBNSynchronizer sbn = new SBNSynchronizer();
                boolean sbnOK = sbn.addUpdateEmergencyContact(xnp);
                if(!sbnOK) {    //SBN sync failed!
                    throw new XanbooException(21254, "Failed to update notification profile. SBN synchronization failed.");
                }
            }
            
            if ( xnp.isEmergencyContact() && xnp.isSendNotification() )
            {
                XanbooUser xUser = dao.getUser(conn,  xCaller, xCaller.getUserId());
                dao.queueAction(conn, xUser.getDomain(), xUser.getAccountId(), xUser.getExtUserId(), "0", "0", "1075", "", "Auto-notify", xUser.getLanguage(), xUser.getTimezone(), "", xUser.getEmail() );
            }

        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[updateNotificationProfile()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[updateNotificationProfile()]: Exception:" + e.getMessage());
            }
            rollback=true;
            throw new XanbooException(10030);
        }finally {
            dao.closeConnection(conn, rollback);
        }

     }

     
     
     public void updateNotificationProfile(XanbooPrincipal xCaller, XanbooNotificationProfile[] xnp) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateNotificationProfile()]: BULK");
        }

        
        boolean isAllEmergencyContact = true;
        boolean isAllNotificationProfile = true;
        boolean isSendNotification = false;
        
        // validate the input parameters
        for(int i=0; i<xnp.length; i++) {
            if(!xnp[i].isEmergencyContact()) isAllEmergencyContact=false;
            if(xnp[i].isEmergencyContact()) isAllNotificationProfile=false;
            if((xnp[i].isEmergencyContact() && !xnp[i].isValidEmergencyContact()) || (!xnp[i].isEmergencyContact() && !xnp[i].isValidNotificationProfile()) ) {
                throw new XanbooException(10050);
            }
            if ( xnp[i].isSendNotification()) isSendNotification = true;
        }
        
        if(!isAllEmergencyContact && !isAllNotificationProfile) {
            throw new XanbooException(10050, "Profiles must be of the same type!");
        }
        
        Connection conn=null;
        boolean rollback=false;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            
            //sync with SBN one at a time, if all are NOT emergency contacts with real gguids
            for(int i=0; i<xnp.length; i++) {
                if(xnp[i].getProfileId()<=0) {  //add a new profile
                    long npId = dao.newNotificationProfile(conn, xCaller.getAccountId(), xCaller.getUserId(), xnp[i]);
                    xnp[i].setProfileId(npId);
                }else {                         // update existing profile
                    dao.updateNotificationProfile(conn, xCaller.getAccountId(), xCaller.getUserId(), xnp[i]);
                }
            }
            
            //BULK sync with SBN, if all are emergency contacts
            if(GlobalNames.MODULE_SBN_ENABLED && isAllEmergencyContact) {
                SBNSynchronizer sbn = new SBNSynchronizer();
                boolean sbnOK = sbn.addUpdateEmergencyContact(xnp);
                if(!sbnOK) {    //SBN sync failed!
                    throw new XanbooException(21254, "Failed to update notification profile. SBN synchronization failed.");
                }
            }
            
            if ( isAllEmergencyContact && isSendNotification )
            {
                XanbooUser xUser = dao.getUser(conn,  xCaller, xCaller.getUserId());
                dao.queueAction(conn, xUser.getDomain(), xUser.getAccountId(), xUser.getExtUserId(), "0", "0", "1075", "", "Auto-notify", xUser.getLanguage(), xUser.getTimezone(), "", xUser.getEmail() );
            }

        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[updateNotificationProfile()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[updateNotificationProfile()]: Exception:" + e.getMessage());
            }
            rollback=true;
            throw new XanbooException(10030);
        }finally {
            dao.closeConnection(conn, rollback);
        }

     }
     

     public XanbooResultSet getBroadcastMessage(XanbooPrincipal xCaller, String lang) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getBroadcastMessage()]:");
        }

        if(lang==null || lang.trim().equals("")) lang="en";        
        
        Connection conn=null;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);

            conn=dao.getConnection();
            return dao.getBroadcastMessage(conn, xCaller.getAccountId(), xCaller.getUserId(), lang);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getBroadcastMessage()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[getBroadcastMessage()]: Exception:" + e.getMessage());
            }            
            throw new XanbooException(10030, "[getBroadcastMessage()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
        
     }          
     
  
    public XanbooUser getUser(XanbooPrincipal xCaller, long userId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getUser()]: usrid=" + userId);
        }

        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0 || userId < 1 ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        XanbooUser user = new XanbooUser();
        Connection conn=null;
        try {
            XanbooUtil.checkCallerPrivilege(xCaller);
        
            conn=dao.getConnection();
            user = dao.getUser(conn, xCaller, userId);

            //decrypt the profile data
            String profileData = user.getProfileData();
            if (profileData != null && profileData.trim().length() > 0) {
                try {
                    user.setProfileData(decrypt(profileData));
                } catch (Exception ee) {
                    logger.error("[getUser()] - problem dencrypting profile data", ee);
                    user.setProfileData("Decryption Error");
                }
            }
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getUser()] Exception: " + e.getMessage(), e);
            }else {
              logger.error("[getUser()] Exception: " + e.getMessage());
            }                        
            throw new XanbooException(10030, "[getUser()] Exception: " + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
        
        return user;
    }
    
    
    
    /* Locates a specific XanbooUser record by a given username or email address. */      
    public XanbooUser[] locateUser(String domainId, String username, String email, boolean sendUsernameReminder) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[locateUser()]: username=" + (username==null ? "" : username) + ", email=" + (email==null ? "" : email));
        }

        if(username!=null && username.trim().length()==0) username=null;
        if(email!=null && email.trim().length()==0) email=null;
        
        
        if(domainId==null || domainId.length()==0 || (username==null && (email==null || email.indexOf("@")==-1)) ) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        try {
            conn=dao.getConnection();
            return dao.locateUser(conn, domainId, username, email, sendUsernameReminder);
            
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[locateUser()] Exception: " + e.getMessage(), e);
            }else {
              logger.error("[locateUser()] Exception: " + e.getMessage());
            }                        
            throw new XanbooException(10030, "[locateUser()] Exception: " + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
        
    }
    
    
    public String resetPassword(XanbooUser user) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[resetPassword()]: acc=" + user.getAccountId() + ", usrid=" + user.getUserId());
        }
        
        if(user.getDomain()==null || user.getAccountId()<0 || user.getUserId()<0) {
            throw new XanbooException(10050);
        }

        Connection conn=null;
        try {
            conn=dao.getConnection();
            XanbooPrincipal xp = new XanbooPrincipal(user.getDomain(), user.getUsername(), user.getAccountId(), user.getUserId());
            
            XanbooUser usr = dao.getUser(conn, xp, user.getUserId());
            usr.setPasswordnotificationProfile(user.getPasswordnotificationProfile());
            
            //check if returned user account id match the one in passed XP
            if(usr.getAccountId()!=user.getAccountId() || (usr.getDomain()!=null && !usr.getDomain().equals(user.getDomain()))) {
                throw new XanbooException(10050, "Account/Domain ID mismatch");
            }
            
            usr.setTemporaryPassword();
            
            //unlock user account
            usr.setStatus(0);
            
            updateUser( xp, usr );
            
            return usr.getPassword();
            
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[resetPassword()] Exception: " + e.getMessage(), e);
            }else {
              logger.error("[resetPassword()] Exception: " + e.getMessage());
            }                        
            throw new XanbooException(10030, "[resetPassword()] Exception: " + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
        
    }   
    
    
   
    public void updateUserACL( XanbooPrincipal xCaller, long userId, String gatewayGUID, String deviceGUID, int accessId ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateUserACL()]:");
        }
 
        // validate the input parameters
        try {
            //Allow for gateways only at this time.
            if( userId == 0 || gatewayGUID.trim().equals("") || !deviceGUID.equals("0") ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        updateUserACL( xCaller, userId, 3, gatewayGUID, accessId );
        
    }
    
    
    private void updateUserACL( XanbooPrincipal xCaller, long userId, int objectTypeId, String objectId, int accessId ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateUserACL()]:");
        }
 
        // validate the input parameters
        try {
            if( userId == 0 || objectTypeId == 0 || objectId.trim().equals("") ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback=false;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            dao.updateUserACL(conn, xCaller, userId, objectTypeId, objectId.trim(), accessId );
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[updateUserACL()] Exception: " + e.getMessage(), e);
            }else {
              logger.error("[updateUserACL()] Exception: " + e.getMessage());
            }                                    
            throw new XanbooException(10030, "[updateUserACL()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
    }
 

     public XanbooResultSet getUserACL(XanbooPrincipal xCaller, long userId, String gatewayGUID, String deviceGUID ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getUserACL()]:");
        }

        // validate the input parameters
        try {
            //Allow for gateways only at this time.
            if( userId == 0 || ( deviceGUID == null || !deviceGUID.equals("0") ) ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;       
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            return dao.getUserACL(conn, xCaller, userId, gatewayGUID, deviceGUID);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getUserACL()] Exception: " + e.getMessage(), e);
            }else {
              logger.error("[getUserACL()] Exception: " + e.getMessage());
            }                                                
            throw new XanbooException(10030, "[getUserACL()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
     }
     
    public XanbooResultSet getAccount(XanbooPrincipal xCaller) throws XanbooException{
        if (logger.isDebugEnabled()) {
            logger.debug("[getAccount()]:");
        }
        
        Connection conn=null;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            return dao.getAccount(conn, xCaller);
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

    
    public void updateAccount(XanbooPrincipal xCaller, int status, String regToken) throws XanbooException {

        XanbooAccount acc = new XanbooAccount();
        acc.setAccountId(xCaller.getAccountId());
        acc.setStatus(status);
        acc.setToken(regToken);
        acc.setExtAccountId(null);  //no update
        acc.setFifoPurgingFlag(-1); //no update

        updateAccount(xCaller, acc);
    }


    
    public void updateAccount(XanbooPrincipal xCaller, XanbooAccount xAccount) throws XanbooException {
          	if (logger.isDebugEnabled()) {
            logger.debug("[updateAccount()]:");
        }
    	        
        boolean doMBusSync = true;//MBus 

        if(xAccount==null) {
            throw new XanbooException(10050);
        }

        if(xAccount.getStatus()!=XanbooAccount.STATUS_INACTIVE && xAccount.getStatus()!=XanbooAccount.STATUS_ACTIVE &&
             xAccount.getStatus()!=XanbooAccount.STATUS_DISABLED && xAccount.getStatus()!=XanbooAccount.STATUS_CANCELLED &&
                 xAccount.getStatus()!=XanbooAccount.STATUS_UNCHANGED) {
        	doMBusSync = false; // don't send message in case of exception
        	throw new XanbooException(10050);
        }


        Connection conn=null;
        boolean rollback=false;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            dao.updateAccount(conn, xCaller, xAccount);
            /*if(xAccount.getStatus()!=XanbooAccount.STATUS_UNCHANGED) {
                logger.info("[updateAccount()]: STATUS CHANGED account:" + xCaller.getAccountId() + "@" + xCaller.getDomain() + ", status:" + xAccount.getStatus());
            }*/
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
            	

                       	
            	 if(logger.isDebugEnabled()) {
            		 
            		 StringBuffer sb = new StringBuffer("[MBus : publish messages : AccountManagerEJB.updateAccount()]:");
            		 sb.append("\n Domain : " + xAccount.getDomain() );
            		 sb.append("\n AccountId : " + xAccount.getAccountId() );
            		 sb.append("\n ExtAccountId : " + xAccount.getExtAccountId() );
            		 sb.append("\n Status : " + xAccount.getStatus() );
            		 sb.append("\n UserId : " + xCaller.getUserId() );
            		
            		 
                     logger.debug( sb.toString() );
                   }
            	 
            	MBusSynchronizer.updateAccount(xAccount.getDomain(), xAccount.getAccountId(), xAccount.getExtAccountId(), xAccount.getStatus(), 
            			xCaller.getUserId()+"", "AccountManagerEJB.updateAccount");
   
            }
            
            if (GlobalNames.DLA_AUTHENTICATE_VIA_SERVICE && !rollback) {
            	invalidateDlaAuthServiceCache(xCaller.getAccountId());
            }
        }
    }


    public void deleteAccount(XanbooPrincipal xCaller) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteAccount()]:");
        }
        boolean doMBusSync = true;//MBus
        Connection conn=null;
        boolean rollback=false;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            //First, do the deletion on the database side of things.
            dao.deleteAccount(conn, xCaller);
            
            //removed account folder move to system trash dir as account data may span over multiple mounts now
            //!!!!!TODO: rather delete all account entities in SP and just set account record to deleted. Then purging
            //service to determine accounts to be purged, determine mounts for that account and purge folders
            
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
            		 
            		 StringBuffer sb = new StringBuffer("[MBus : publish messages : AccountManagerEJB.deleteAccount()]:");
            		 sb.append("\n Domain : " + xCaller.getDomain() );
            		 sb.append("\n AccountId : " + xCaller.getAccountId() );
            		 sb.append("\n ExtAccountId : " + xCaller.getUsername() );
            		 sb.append("\n UserId : " + xCaller.getUserId() );
            		            		 
                     logger.debug( sb.toString() );
                   }
 
            	MBusSynchronizer.deleteAccount(xCaller.getDomain(), xCaller.getAccountId(), xCaller.getUsername(), xCaller.getUserId()+"",  "AccountManagerEJB.deleteAccount");
            	
            }
        }
    }

     
/*     
     public void addUserACL() {
     }
     
     public void deleteUserACL() {
     }
     
     public void updateUserACL() {
     }
*/

   /* for backwards compatibility - will be removed in 1209 code release */
   public void updateSubscription(XanbooPrincipal xCaller, String subsId, String hwId, String masterPin, String masterDuress, String alarmPass) throws XanbooException {
       updateSubscription(xCaller, subsId, hwId, masterPin, masterDuress, alarmPass, -1);
   }

   /* for backwards compatibility - will be removed in 1303 code release */
   public void updateSubscription(XanbooPrincipal xCaller, String subsId, String hwId, String masterPin, String masterDuress, String alarmPass, int alarmDelay) throws XanbooException {
       updateSubscription(xCaller, subsId, hwId, masterPin, masterDuress, alarmPass, alarmDelay, -1);
   }
   
   public void updateSubscription(XanbooPrincipal xCaller, String subsId, String hwId, String masterPin, String masterDuress, String alarmPass, int alarmDelay, int tcFlag) throws XanbooException {
        XanbooSubscription xsub = new XanbooSubscription();
        xsub.setAccountId(xCaller.getAccountId());
        xsub.setSubsId(subsId);
        xsub.setHwId(hwId);
        xsub.setDisarmPin(masterPin);
        xsub.setDuressPin(masterDuress);
        xsub.setAlarmCode(alarmPass);

        updateSubscription(xCaller, xsub, null, alarmDelay, tcFlag);
   }


    public void updateSubscription(XanbooPrincipal xCaller, XanbooSubscription xsub, String hwIdNew, int alarmDelay, int tcFlag) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[updateSubscription()]: ");
        }
        boolean doMBusSync = true; //MBus 
        // first validate the input parameters
        try {
            if(xCaller.getAccountId()!=xsub.getAccountId() || !xsub.isValidSubscriptionToUpdate()) {
            	throw new XanbooException(10050);
            }

            //nulling attributes to be ignored for this call!
            xsub.setSubsFlags(-1);
            xsub.setSubsInfo(null);
            xsub.setNotificationProfiles(null);
            xsub.setLabel(null);
            xsub.setTzone(null);
            xsub.setbMarket(null);
            xsub.setbSubMarket(null);
            if(hwIdNew!=null && xsub.getHwId().equalsIgnoreCase(hwIdNew)) hwIdNew=null; //null newhwid param, if equal to current subs hwid.
            
        }catch(Exception e) {
        	doMBusSync = false;
            throw new XanbooException(10050);
        }
        
        XanbooSubscription xsub2=xsub;

        Connection conn=null;
        boolean rollback=false;

        try {
            XanbooUtil.checkCallerPrivilege(xCaller);

            
            conn=dao.getConnection();
            
            //update subscription and see if a gguid exists for the subsciption
            xsub2 = dao.updateSubscription(conn, xCaller, xsub.getSubsId(), xsub.getHwId(), xsub.getDisarmPin(), xsub.getDuressPin(), xsub.getAlarmCode(), xsub.getSubsInfo(), hwIdNew, alarmDelay, tcFlag);
            // read subscription to get details after changes (using new hwid if necessary)            
            XanbooResultSet subsList = dao.getSubscription(conn, xCaller,  xsub2.getSubsId(),  (hwIdNew!=null && hwIdNew.trim().length()>0) ? hwIdNew.trim() : xsub.getHwId());  // Fixed for 30349 
            xsub2 = new XanbooSubscription(subsList, 0);
            xsub.setGguid(xsub2.getGguid()); 
            
            //sync with SBN, if enabled and any relevant param is not null (new hwid or name/address or alarmpass changed)
            //               or service is being suspended/restored (not syncing SBN on when subsFlags>0 values!!! - not a valid scenario, but problem!)
            if(GlobalNames.MODULE_SBN_ENABLED && (hwIdNew!=null || xsub.getAlarmCode()!=null || alarmDelay>=0)) {
                SBNSynchronizer sbn = new SBNSynchronizer();

                //if hwid, name/address or alarmpass is being updated
                if(hwIdNew!=null || xsub.getAlarmCode()!=null) {
                    boolean sbnOK = sbn.updateInstallation(xsub.getSubsId(), xsub.getHwId(), xsub.getGguid(), hwIdNew,  null, null, xsub.getAlarmCode(), null,-1, null,xsub2.getSubscriptionClass());
                    if(!sbnOK) {    //SBN sync failed!
                        throw new XanbooException(21420, "Failed to update subscription. SBN synchronization failed.");
                    }
                    
                    /*** disable for now - no IMEI change by customers themselves for domestic !!!
                    //signal RMS on IMEI change !
                    if(GlobalNames.MODULE_RMS_ENABLED && hwIdNew!=null && hwIdNew.length()>0) {
                        String marketAreaCode="";
                        try{
                            //First get the market area code
                            XanbooResultSet subsList = this.getSubscription(xCaller, xsub.getAccountId(), xsub.getSubsId(), xsub.getHwId());
                            if(subsList != null && subsList.size() > 0){
                                marketAreaCode=subsList.getElementString(0,MARKET_AREA_COL_NAME);
                                if(marketAreaCode != null && marketAreaCode.length() > 0 ) {
                                    RMSClient.replaceDLC(xsub.getHwId(),hwIdNew,marketAreaCode,RMS_MARKET_AREA);
                                }
                            }
                        }
                        catch(XanbooException rmse){
                            if(logger.isDebugEnabled())
                                logger.warn("[updateSubscription()]: Failed to swap subscription hardware in RMS. CTN:" + xsub.getSubsId() + ", IMEI:"+ xsub.getHwId() + ", NEWIMEI:"+ hwIdNew + ", MARKETAREACODE:"+marketAreaCode+", Exception:" + rmse.getErrorMessage(), rmse);
                            else
                                logger.warn("[updateSubscription()]: Failed to swap subscription hardware in RMS. CTN:" + xsub.getSubsId() + ", IMEI:"+ xsub.getHwId() + ", NEWIMEI:"+ hwIdNew + ", MARKETAREACODE:"+marketAreaCode+ ", Exception:" + rmse.getErrorMessage(), rmse);
                        }catch (Exception rmse) {
                            if(logger.isDebugEnabled())
                                logger.warn("[updateSubscription()]: Failed to swap subscription hardware in RMS. CTN:" + xsub.getSubsId() + ", IMEI:"+ xsub.getHwId() + ", NEWIMEI:"+ hwIdNew + ", MARKETAREACODE:"+marketAreaCode+ ", Exception:" + rmse.getMessage(), rmse);
                            else
                                logger.warn("[updateSubscription()]: Failed to swap subscription hardware in RMS. CTN:" + xsub.getSubsId() + ", IMEI:"+ xsub.getHwId() + ", NEWIMEI:"+ hwIdNew + ", MARKETAREACODE:"+marketAreaCode+ ", Exception:" + rmse.getMessage(), rmse);
                        }
                    }
                    ***/ 
                    
                }
                
                if(alarmDelay >= 0) {
                    boolean sbnOK = sbn.setInstallationAlarmDelay(xsub.getSubsId(), xsub.getHwId(), alarmDelay);
                    if(!sbnOK) {    //SBN sync failed!
                        throw new XanbooException(21420, "Failed to set subscription alarm delay timer. SBN synchronization failed.");
                    }
                }
                
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

            	if(xsub.getSubsInfo()  != null){
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
                data.setAccId(xsub2.getAccountId());
                data.setbMarket(xsub2.getbMarket());
                data.setbSubMarket(xsub2.getbSubMarket());
                data.setCity(city);
                data.setCountry(country);

                String 	date = XanbooUtil.getGMTDateTime(xsub2.getDateCreated());
                data.setDateCreated(date);

                data.setDomain(xCaller.getDomain());
                data.setExtAccId(xsub.getExtAccountId());
                data.setGatewayGUID(xsub2.getGguid());
                data.setHwId(xsub.getHwId()); // old hwid to retrieve subscription record in mongo
                data.setHwIdNew(hwIdNew);     // update new hwid in monogo
                data.setSrcAppId("AccountManagerEJB.updateSubscription");
                data.setSrcOriginatorId(userId + "");
                data.setState(state);
                data.setStreet1(street1);
                data.setStreet2(street2);
                data.setSubsFeatures(xsub2.getSubsFeatures());
                data.setSubsFirstName(subsFName);

                data.setSubsId(xsub2.getSubsId());
                data.setSubsLastName(subsLName);
                data.setTimeZone(xsub2.getTzone());
                data.setZip(zip);
                data.setZip4(zip4);

                data.setSubsFlags(xsub2.getSubsFlags());
                
            	data.setInstallType(xsub2.getInstallType());//DLDP 2679
            	data.setSubsClass(xsub2.getSubscriptionClass());//DLDP 3056
            	
                /* --disabled, since latest flags value is now returned by the SP
                try {
                    int mSubFlag = XanbooUtil.getSubsFlag(xsub.getSubsFlags(), xsub.getGguid(), xsub.getSubsFlagsMask());
                     if(logger.isDebugEnabled()) {
                             logger.debug( "XanbooUtil.getSubsFlag(" + xsub.getSubsFlags() + "," + xsub.getGguid()+ "," + xsub.getSubsFlagsMask() + " )" ); 
                     }
                    data.setSubsFlags(mSubFlag);
                } catch (Exception e) {
                    data.setSubsFlags(xsub.getSubsFlags());
                }
                */
                if(logger.isDebugEnabled()) {
                    StringBuffer sb = new StringBuffer("[MBus : publish messages : AccountManagerEJB.updateSubscription()]:");
                    sb.append("\n  : " + data.toString() );
                    logger.debug( sb.toString() );
                }
            	 
            	MBusSynchronizer.updateSubscription(data);
   
            }
            // MBus end
        }
        
    }
   
   

    public XanbooResultSet getSubscription(XanbooPrincipal xCaller, String subsId, String hwId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getSubscription()]:");
        }

        Connection conn=null;
        try {
            // validate the caller and privileges
            //checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            XanbooResultSet xrs = dao.getSubscription(conn, xCaller, subsId, hwId);
            
            return xrs;

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
    
    
    public XanbooSubscription[] getSubscription(XanbooPrincipal xCaller, String subsId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getSubscription()]:");
        }

        Connection conn=null;
        try {
            // validate the caller and privileges
            //checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            XanbooResultSet xrs = dao.getSubscription(conn, xCaller, subsId, null);
            
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

    
    public XanbooResultSet getProvisionedDeviceList(XanbooPrincipal xCaller, String subsId, String hwId, String classId, String subclassId, String installType) throws XanbooException   {
        if(logger.isDebugEnabled()){
            logger.debug("[getProvisionedDeviceList]");
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
            XanbooResultSet xrs = dao.getProvisionedDeviceList(conn, xCaller, subsId, hwId, classId, subclassId, installType);
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

    
     public int getAlertCount(XanbooPrincipal xCaller, String gatewayGUID) throws XanbooException {
        //if (logger.isDebugEnabled()) {
            logger.debug("[getAlertCount()]: acc=" + xCaller.getAccountId() + ", gguid=" + gatewayGUID);
        //}

        if(gatewayGUID==null || gatewayGUID.length()==0) { //subclass required, if class is specified
            throw new XanbooException(10050);
        }

        
        Connection conn=null;
        try{
            //checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            return dao.getAlertCount(conn, xCaller, gatewayGUID);
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
     
     public void logoutUser(XanbooPrincipal xCaller, Boolean reconnect) throws XanbooException
     {
         logger.info("[logoutUser] logging out accountid="+xCaller.getAccountId());
         try
         {
            XanbooResultSet xres = dManager.getDeviceListByClass(xCaller, "0000");
            for(int i=0; i<xres.size(); i++) 
            {
                String gguid = xres.getElementString(i, "GATEWAY_GUID");
                if(gguid!=null && gguid.length()>0) 
                {
                    paiCmdRest.sendDisconnect(null, gguid, reconnect);
                }
            }
         }
         catch(Exception ex)
         {
             logger.warn("[logoutUser] Exception sending disconnect command",ex);
         }
     }

     public Integer getNotificationOptInStatus(XanbooPrincipal xCaller, String notificationAddress, String token) throws XanbooException {

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

     public Map<String, Integer> getNotificationOptInStatus(XanbooPrincipal xCaller, List<String> notificationAddresses) throws XanbooException {
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

     public void setNotificationOptInStatus(XanbooPrincipal xCaller, String notificationAddress, String token, int status) throws XanbooException {

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

               conn=dao.getConnection();
               dao.setNotificationOptInStatus(conn, xCaller.getDomain(), xCaller.getAccountId(), profileAddress, token, status, "", "", profileType == null ? null : profileType.toString());
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

     public void setNotificationOptInStatus(XanbooPrincipal xCaller, Map<String, Integer> notificationAddresses) throws XanbooException {
         if (notificationAddresses == null || notificationAddresses.isEmpty()) {
             throw new XanbooException(10050);
         }

        boolean rollback=false;
        Connection conn = null;

         try {
              conn=dao.getConnection();

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

                   dao.setNotificationOptInStatus(conn, xCaller.getDomain(), xCaller.getAccountId(), profileAddress, null,
                           notificationAddresses.get(notificationAddress), xCaller.getLanguage(), xCaller.getTimezone(), profileType == null ? null : profileType.toString());
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
     
     public XanbooResultSet getSupportedDeviceList(XanbooPrincipal xp, String monType) throws XanbooException {
    	
    	 if(logger.isDebugEnabled()){
             logger.debug("[getSupportedDeviceList]");
         }
         
         // first validate the input parameters. domain id  must be provided
         if(xp!=null &&  xp.getDomain()==null || xp.getDomain().trim().length()==0) {
             throw new XanbooException(10050);
         }
         
         Connection conn=null;
         try{
             //checkCallerPrivilege(xCaller);
             conn=dao.getConnection();
             XanbooResultSet xrs = dao.getSupportedDeviceList(conn, xp.getDomain(), "S", monType);
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
     
     public XanbooResultSet getSupportedDeviceList(XanbooPrincipal xp, String subsId, String hwId,  boolean includeProvisioned) throws XanbooException {
    	 if(logger.isDebugEnabled()){
             logger.debug("[getSupportedDeviceList]");
         }
         
         // first validate the input parameters. domain id  must be provided
         if(xp!=null &&  xp.getDomain()==null || xp.getDomain().trim().length()==0  ||
        		 subsId==null || subsId.trim().length()==0 || hwId==null || hwId.trim().length()==0) {
             throw new XanbooException(10050);
         }
         
         Connection conn=null;
         try{
             //checkCallerPrivilege(xCaller);
             conn=dao.getConnection();
             XanbooResultSet xrs = dao.getSupportedDeviceList(conn, xp.getDomain(), subsId, hwId, includeProvisioned);
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

     private String encrypt(String unsecureString) throws Exception {
        XanbooEncryptionProvider encProvider = XanbooEncryptionProviderFactory.getProvider();
        if (encProvider == null) {
        	throw new Exception("Failed to get encryption provider");
        }
        return encProvider.encrypt(unsecureString);
     }

     private String decrypt(String encryptedString) throws Exception {
        XanbooEncryptionProvider encProvider = XanbooEncryptionProviderFactory.getProvider();
        if (encProvider == null) {
        	throw new Exception("Failed to get encryption provider");
        }
        return encProvider.decrypt(encryptedString);
     }
     
     /**
      * Retrieves list of domain feature class for a given subscription id and hardware id
      * 
      * @param xCaller a XanbooPrincipal object that identifies the caller 
      * @param subsId the subscription identifier to query for INSTALL_TYPE
      * @param hwId hardware identifier 
      * @return
      * 
      * <br>
      * <b> Columns Returned: </b>
      * <UL>
      *  <li> VENDOR_ID</li>
      *  <li> CLASS_ID</li>
      *  <li> SUBCLASS_ID</li>
      * </UL
      * 
      * @throws XanbooException
      */
     public XanbooResultSet getSupportedClassList( XanbooPrincipal xCaller, String subsId, String hwid) throws XanbooException{
    	return getSupportedClassList(xCaller, null, subsId, hwid);
     }
     /**
      * Retrieves list of domain feature class for a gatewayd guid 
      * 
      * @param xCaller a XanbooPrincipal object that identifies the caller
      * @param gateway_guid The GUID of the gateway for which to update the access level
      * @return
      * 
      * <br>
      * <b> Columns Returned: </b>
      * <UL>
      *  <li> VENDOR_ID</li>
      *  <li> CLASS_ID</li>
      *  <li> SUBCLASS_ID</li>
      * </UL
      * 
      * @throws XanbooException
      */
     public XanbooResultSet getSupportedClassList( XanbooPrincipal xCaller,String gatewayGuid) throws XanbooException{
    	 return getSupportedClassList(xCaller, gatewayGuid, null, null);
     }
     
     /**
      * @param xCaller a XanbooPrincipal object that identifies the caller
      * @param gateway_guid The GUID of the gateway for which to update the access level
      * @param subsId the subscription identifier to query for INSTALL_TYPE
      * @param hwId hardware identifier 
      * @return
      * @throws XanbooException
      */
    private XanbooResultSet getSupportedClassList(XanbooPrincipal xCaller, String gateway_guid,String subsId, String hwId) throws XanbooException {
    	 if(logger.isDebugEnabled()){
             logger.debug("[getSupportedDeviceList]");
         }
         
         // first validate the input parameters. domain id  must be provided
         if(xCaller!=null &&  xCaller.getDomain()==null || xCaller.getDomain().trim().length()==0 ) {
             throw new XanbooException(10050);
         }
         
         Connection conn=null;
         try{             
             conn=dao.getConnection();
             XanbooResultSet xrs = dao.getSupportedClassList(conn, xCaller.getDomain(),xCaller.getAccountId(), gateway_guid, subsId, hwId);
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
}
