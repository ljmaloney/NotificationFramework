/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/util/GlobalNames.java,v $
 * $Id: GlobalNames.java,v 1.77 2011/07/13 12:51:41 stu Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.util;

import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Class to hold global names/variables. Any change must be reflected in the
 * deployment descriptors.
 *
 */
public class GlobalNames {
    // Global system parameters
   	private static Logger log = LoggerFactory.getLogger(GlobalNames.class.getName());
    private static boolean isInitialized=false;      // is sdk initialized or not
    
    public static String DEFAULT_DOMAIN = "xanboo";   
    public static String DAO_CLASS      = "com.xanboo.core.util.OracleDAO";      // default DAO implementation classname (OracleDAO, etc.)

    public static String LOG4J_CONFIG   = "WEB-INF/resources/xanboo-log4j.properties";
    
    // Enabled/Disabled status of Core Application modules. All enabled by default.
    public static boolean MODULE_WASTEBASKET_ENABLED     = false;
    public static boolean MODULE_EVENTLOG_ENABLED        = false;

    // SDK App tier JNDI properties
    public static String  SDK_JNDI_CONTEXT_FACTORY = null;
    public static String  SDK_JNDI_PROVIDER_URL    = null;

    // ADS and DMP enablement App tier JNDI properties
    public static boolean MODULE_ADS_ENABLED       = true;        //enable/disable ADS Receiver logic/module
    public static boolean MODULE_DMP_ENABLED       = true;         //enable/disable DMP Receiver logic/module
    
    // Enabled/Disabled status of Core Services. All enabled by default.
    public static boolean       IS_AI_SERVER          = true;
    public static boolean       IS_AIA_SERVER         = true;
    
    public static final String  SERVICE_USER = "_xaninit_";
    public static final String  DEFAULT_APP_USER = "dlcore";
    public static int           SERVICE_CACHE_EXPIRY_MINUTES = 6*60;   // 6 hours default for service handler cache expiries

    public static int           CACHE_TTL_BROADCAST_COMMAND = 15;   // cache expiry for broadcast commands in mins. Default 15mins, <=0 to diable caching.

    
    // Enabled/Disabled status of Core Scheduled Services. All disabled by default.
    //public static boolean    SERVICE_INVITATION_ENABLED     = false;
    public static boolean    SERVICE_NOTIFICATION_ENABLED   = false; // Is notification service running on any server

    // Enable/Disable SBN professional monitoring (disabled by default)
    public static boolean MODULE_SBN_ENABLED        = false;
    public static String  APP_TIMEZONE_LOOKUP    = "SBN";     //enable SBN lookups by default

    
    // Enable/Disable RMS (enabled by default)
    public static boolean    MODULE_RMS_ENABLED        = true;
    // Enable/Disable PH (disabled by default)    
    public static boolean MODULE_PH_ENABLED           = false;
    
    // Enable/Disable CSI (enabled by default)
    public static boolean    MODULE_CSI_ENABLED        = true;
    
    public static boolean MODULE_PAI_ENABLED           = false;

    public static int APP_HASHING_ALGORITHM = 2;    //0:MD5, 1:SHA-1, 2:SHA-256
    
    // Resource JNDI REFERENCES --------------------
    public static String COREDS         = "java:/xanboo/jdbc/XanbooCoreDS";         // Main Core Datasource JNDI/env ref
    public static String COREDS_CHARSET = "US7ASCII";               // Database Charset
    
    
 
    // EJB JNDI references
    public static final String EJB_XAIL_MAIN_HANDLER          = "xanboo/xail/XailMainHandler!com.xanboo.core.xail.ejb.XailMainHandler";
    public static final String EJB_XAIL_DEVICE_HANDLER        = "xanboo/xail/DeviceMessageHandlerEJB!com.xanboo.core.xail.ejb.handler.XailMessageHandler";
    public static final String EJB_XAIL_MOBJECT_HANDLER       = "xanboo/xail/MObjectMessageHandlerEJB!com.xanboo.core.xail.ejb.handler.XailMessageHandler";
    public static final String EJB_XAIL_EVENT_HANDLER         = "xanboo/xail/EventMessageHandlerEJB!com.xanboo.core.xail.ejb.handler.XailMessageHandler";
    public static final String EJB_CORESERVICE_MANAGER        = "xanboo/xail/ServiceManager!com.xanboo.core.service.ejb.ServiceManager";
    public static final String EJB_COREADMIN_MANAGER          = "xanboo/xail/CoreAdminManager!com.xanboo.core.admin.ejb.CoreAdminManager";
    public static final String EJB_XAIL_PAI_MANAGER           = "xanboo/xail/PAIManager!com.xanboo.core.xail.ejb.pai.PAIManager";
    
    public static final String EJB_ACCOUNT_MANAGER            = "xanboo/sdk/AccountManager!com.xanboo.core.sdk.account.AccountManager";
    public static final String EJB_ACCT_ALERT_MANAGER         = "xanboo/sdk/AccountAlertManager!com.xanboo.core.sdk.account.alert.AccountAlertManager";
    public static final String EJB_INBOX_MANAGER              = "xanboo/sdk/InboxManager!com.xanboo.core.sdk.inbox.InboxManager";
    public static final String EJB_FOLDER_MANAGER             = "xanboo/sdk/FolderManager!com.xanboo.core.sdk.folder.FolderManager";
    public static final String EJB_INVITATION_MANAGER         = "xanboo/sdk/InvitationManager!com.xanboo.core.sdk.invitation.InvitationManager";
    public static final String EJB_SYSADMIN_MANAGER           = "xanboo/sdk/SysAdminManager!com.xanboo.core.sdk.sysadmin.SysAdminManager";
    public static final String EJB_MAIL_MANAGER               = "xanboo/sdk/MailManager!com.xanboo.core.sdk.util.mail.MailManager";
    public static final String EJB_DEVICE_MANAGER             = "xanboo/sdk/DeviceManager!com.xanboo.core.sdk.device.DeviceManager";
    public static final String EJB_DEVICEGROUP_MANAGER        = "xanboo/sdk/DeviceGroupManager!com.xanboo.core.sdk.devicegroup.DeviceGroupManager";
    public static final String EJB_GATEWAY_MANAGER            = "xanboo/sdk/GatewayManager!com.xanboo.core.sdk.gateway.GatewayManager";
    public static final String EJB_WASTEBASKET_MANAGER        = "xanboo/sdk/WastebasketManager!com.xanboo.core.sdk.wastebasket.WastebasketManager";
    public static final String EJB_CONTACT_MANAGER            = "xanboo/sdk/ContactManager!com.xanboo.core.sdk.contact.ContactManager";
    public static final String EJB_ENERGY_MANAGER             = "xanboo/sdk/EnergyManager!com.xanboo.core.sdk.energy.EnergyManager";
    public static final String EJB_EXTSERVICE_MANAGER         = "xanboo/sdk/ServiceManager!com.xanboo.core.sdk.services.ServiceManager";
    public static final String EJB_PAI_REST_SERVICE           = "xanboo/sdk/PAICommandREST!com.xanboo.core.sdk.pai.PAICommandREST";
    public static final String EJB_NOTIFICATION_MANAGER       = "xanboo/sdk/NotificationManager!com.xanboo.core.sdk.notification.NotificationManager";
    
    // External Service EJB JNDO refs
    public static final String EJB_INBOUND_PROXY   = "xanboo/extservices/ServiceInboundProxy!com.xanboo.core.extservices.inbound.ServiceInboundProxy";
    public static final String EJB_OUTBOUND_PROXY  = "xanboo/extservices/ServiceOutboundProxy!com.xanboo.core.extservices.outbound.ServiceOutboundProxy";
    public static final String EJB_RULE_MANAGER    = "xanboo/extservices/RuleManager!com.xanboo.core.extservices.rules.RuleManager";
    
    
    
    
    
    // system data store mount point related params
    public static String APP_MOUNT_ROOT    = "/export/app/xapp/xancore/";    
    public static String APP_MOUNT_DIR     = "data00";
    public static String SYSTEM_MOUNT_DIR  = APP_MOUNT_DIR;

    public static String APP_FS_PROVIDERS       = "0=com.xanboo.core.util.fs.DefaultNFSProvider,1=com.xanboo.core.util.fs.ATTCloudFSProvider";
    public static String APP_FS_MOUNTPOINTS     = APP_MOUNT_DIR;
    public static String APP_FS_MOUNTPOINTS_RO  = "";
    public static String MEDIA_SERVICE_MOUNTPOINT    = "2@MEDIASVC";
    public static String MEDIA_SERVICE_ENABLED    = "0";
    public static String MEDIA_SERVICE_ENABLED_FOR_ALL = "1";
    public static String MEDIA_SERVICE_DISABLED_FOR_ALL = "0";
    public static String MEDIA_SERVICE_ENABLED_BY_ACCOUNT_ID = "2";

    public static final String ACCOUNT_DIRECTORY       = "/account";      //parent directory to store account specific files
    public static final String ITEM_DIRECTORY          = "/item";         //sub-directory to store item files per account
    public static final String ITEM_THUMB_DIRECTORY    = "/thumb";        //sub-directory to store item thumbnail files per account
    public static final String MOBJECT_DIRECTORY       = "/mobject";      //sub-directory to store binary mobject values per account
    
    public static final String SYSTEM_DIRECTORY         = "/system";      //parent directory to store system files
    public static final String TEMP_DIRECTORY           = "/tmp";         //sub-directory to store system tmp files
    public static final String LOG_DIRECTORY            = "/log";         //sub-directory to store system log files
    public static final String DOWNLOAD_DIRECTORY       = "/download";    //sub-directory to store system download files
    
  
    public static String SYSTEM_LOG_DIRECTORY      = APP_MOUNT_ROOT + APP_MOUNT_DIR + SYSTEM_DIRECTORY + LOG_DIRECTORY;
    public static String SYSTEM_DOWNLOAD_DIRECTORY = APP_MOUNT_ROOT + APP_MOUNT_DIR + SYSTEM_DIRECTORY + DOWNLOAD_DIRECTORY;
    public static String SYSTEM_MOBJECT_DIRECTORY  = SYSTEM_DOWNLOAD_DIRECTORY + MOBJECT_DIRECTORY;

    
    public static final String DLLITE_GATEWAY_PREFIX    = "XV";           //predefined prefix for the DL-Light gateway

    public static String ACCTALERT_CREATE_EVENT="";
    public static String ACCTALERT_CLEAR_EVENT="";
    
    public static String PUSH_CATEGORY_EVENTS = ""; /*event to category correspondence in the format of [eventId]:[bit],[eventId]:[bit]*/
    //public static String PUSH_CATEGORY_STATUS = ""; /*enable/disable push category systemwide*/
    
    // other item related parameters
    public static String ITEM_UPLOAD_DIRECTORY = "tmp";
    public static String ITEM_MAX_SIZE         = "10M";
    public static boolean ITEM_THUMB_ENABLED   = true;
    public static int    ITEM_THUMB_WIDTH      = 75;
    public static int    ITEM_THUMB_HEIGHT     = 56;
    public static String ITEM_IMAGE_TYPES      = "image/jpeg,image/jpg,image/gif,image/pjpeg";
    public static String ITEM_IMAGE_PROCESSOR  = "/usr/X11R6/bin/convert";

    public static int   ITEM_QUOTA_POLICY = 1; // When event takes account over quota: 1=Attempt to reject only attachment. 2=Always reject entire event.
    public static long  ITEM_MIN_SIZE = 1024;  //The minimum size that an item may be.
    public static boolean ITEM_ENCRYPTION_ENABLED = true;         //enable/disable file storage encryption for customer items

    public static String RULE_FACTORY_IMPL=null;
    
    //Video Verification related params
    public static boolean VVS_ENABLED      = false;
    public static String VVS_MOUNT_ROOT    = "/export/app/xapp/xancore/";
    public static String VVS_DATA_DIR      = "vvs";
    public static String VVS_EVENT_ID      = "601";
    
    
    
    

    
    public static String GoldenGGUIDs = "07845152212840B889F8E8B62F0B95ED,CBF118D7DBB54B00B7CEE80B47E1E8AD";
    public static  long TRACE_TIME = 50;
    
    public static int     XAIL_SESSION_TTL          = 900; // xail session timeout in seconds before gateway authentication from database is forced // change to 15 mins
    
   // public static boolean isFIFOThreadPoolEnabled = false;
    public static int fifoThreadPool = 0;
    
    public static int roundTripThreadPoolSize = 0;
    public  static ExecutorService notificationService = Executors.newFixedThreadPool(5);
    public  static ExecutorService execnotificationService = Executors.newFixedThreadPool(5);
    
    public static int ACSI_TIMEOUT  = 5;//10 sec
    public static int ROUNDTRIP_TIMEOUT  = 5;//10 sec
    
    static{
    	
    	try {
			String rtTimeout = System.getProperty("roundtrip.timeout.sec");
			
			if(rtTimeout != null){
			GlobalNames.ROUNDTRIP_TIMEOUT =	Integer.parseInt(rtTimeout);
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			ROUNDTRIP_TIMEOUT = 5;
		}
    	
    	try {
			String acsiTimeout = System.getProperty("acsi.timeout.sec");
			
			if(acsiTimeout != null){
			GlobalNames.ACSI_TIMEOUT =	Integer.parseInt(acsiTimeout);
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			ACSI_TIMEOUT = 10;
		}
    	
    	try {
			String notifPoolSize = System.getProperty("notif.pool.size");
			int notifSize = 0;
			if(notifPoolSize != null){
				 try {
					notifSize =	Integer.parseInt(notifPoolSize);
				} catch (Exception e) {
					notifSize = 0;
				}
			
				
			}
			
			if(notifSize > 0){
				notificationService = Executors.newFixedThreadPool(notifSize);
				execnotificationService=Executors.newFixedThreadPool(notifSize);
			}
			 log.info("notificationService enabled : notif.pool.size :" + notifSize);  
		} catch (Exception e1) {
			log.error(e1.getMessage());
		}
    	
    	try {
			String traceTS = System.getProperty("trace.ms");
			
			if(traceTS != null){
			GlobalNames.TRACE_TIME =	Integer.parseInt(traceTS);
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			TRACE_TIME = 50;
		}
    	 log.info("TRACE_TIME :" + TRACE_TIME);  
    	try {
			String goldenGGUIDStr = System.getProperty("golden.gguids");
			
			if(goldenGGUIDStr != null){
			GlobalNames.GoldenGGUIDs =	goldenGGUIDStr;
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			GoldenGGUIDs = "07845152212840B889F8E8B62F0B95ED,CBF118D7DBB54B00B7CEE80B47E1E8AD";
		}
    	 log.info("GoldenGGUIDs :" + GoldenGGUIDs); 
    	try {
			String sessionttl = System.getProperty("xail.session.ttl");
			
			if(sessionttl != null){
			GlobalNames.XAIL_SESSION_TTL =	Integer.parseInt(sessionttl);
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			XAIL_SESSION_TTL          = 900;
		} 
    	
    	 log.info("XAIL_SESSION_TTL :" + XAIL_SESSION_TTL); 
    	try {
			String fifoPool = System.getProperty("fifo.pool.size");
			
			if(fifoPool != null){
			GlobalNames.fifoThreadPool =	Integer.parseInt(fifoPool);
			
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			fifoThreadPool = 0;
		} 
    	
    	 log.info("fifoThreadPool :" + fifoThreadPool); 
    	
      	try {
    			String roundtripPool = System.getProperty("roundtrip.pool.size");
    			
    			if(roundtripPool != null){
    			GlobalNames.roundTripThreadPoolSize =	Integer.parseInt(roundtripPool);
    			
    			}
    		} catch (Exception e1) {
    			// TODO Auto-generated catch block
    			roundTripThreadPoolSize = 0;
    		} 
      	
      	log.info("roundTripThreadPoolSize :" + roundTripThreadPoolSize); 
    	
    }
    
    public  static ExecutorService fifoService;
 
 	 public static final ReentrantReadWriteLock fifoLock = new ReentrantReadWriteLock();
 	
 	 public static boolean isFIFOThreadPoolEnabled(){
 		 
 		if(fifoService != null && GlobalNames.fifoThreadPool > 0) return true;
 		
 		if(  GlobalNames.fifoLock.isWriteLocked())  GlobalNames.fifoLock.readLock();
 		
 		 if (GlobalNames.fifoThreadPool > 0 && fifoService == null ) {
 			 
 			try {
				GlobalNames.fifoLock.writeLock().lock();
				 
				 if ( fifoService == null ) {
					 fifoService = Executors.newFixedThreadPool(GlobalNames.fifoThreadPool);
					
					 log.info("isFIFOThreadPoolEnabled : GlobalNames.fifoThreadPoolSize :" + GlobalNames.fifoThreadPool);  
					 
				 }

				GlobalNames.fifoLock.writeLock().unlock();
				if(fifoService != null) return true;
			} catch (Exception e) {
				log.error(e.getMessage());
			}finally{
				if(  GlobalNames.fifoLock.isWriteLocked()) GlobalNames.fifoLock.writeLock().unlock();
			}
 		 }
 	
 		
 		return false;
 	 }
 	 
 	 public  static ExecutorService roundTripService;
 	 public  static ExecutorService execRoundTripService;
 	 
 	 public static final ReentrantReadWriteLock roundTripLock = new ReentrantReadWriteLock();
 	 
 	 
 	 public static boolean isRoundTripThreadPoolEnabled(){
 		 
  		if(roundTripService != null && GlobalNames.roundTripThreadPoolSize>0) return true;
  		
  		if(  GlobalNames.roundTripLock.isWriteLocked())  GlobalNames.roundTripLock.readLock();
  		
  		 if (GlobalNames.roundTripThreadPoolSize > 0 && roundTripService == null ) {
  			 
  			try {
 				GlobalNames.roundTripLock.writeLock().lock();
 				 
 				 if ( roundTripService == null ){
 					 roundTripService = Executors.newFixedThreadPool(GlobalNames.roundTripThreadPoolSize);
 					execRoundTripService = Executors.newFixedThreadPool(GlobalNames.roundTripThreadPoolSize);
 					 log.info("isRoundTripThreadPoolEnabled : GlobalNames.roundTripThreadPoolSize :" + GlobalNames.roundTripThreadPoolSize);
 					 log.info("isRoundTripThreadPoolEnabled : GlobalNames.execRoundTripService :" + GlobalNames.execRoundTripService);
 				 }

 				GlobalNames.roundTripLock.writeLock().unlock();
 				if(roundTripService != null) return true;
 			} catch (Exception e) {
 				log.error(e.getMessage());
 			}finally{
 				if(  GlobalNames.roundTripLock.isWriteLocked()) GlobalNames.roundTripLock.writeLock().unlock();
 			}
  		 }
  	
  	
  		return false;
  	 }
    
    public static String MAIL_HOST            = "localhost";
    public static String MAIL_DEBUG           = "false";
    public static int EMAIL_PROFILE_TYPE = 0;
    public static int SMS_PROFILE_TYPE = 12;
    public static int PUSH_PROFILE_TYPE = 420;
     public static int NOTIF_TEMPL_RELOAD_INTERVAL = 60*24*10;//10 days
    public static int NOTIF_PROVIDER_RELD_INTERVAL = 60*24*10;//10 days
        
    public static int    DEVICE_INVITATION_QUOTA = 5; // invitation quota is currently static (not per-account, like disk space and device quota)

    public static int    NOTIFICATION_PROFILE_MAX = 5;
    public static int    NOTIFICATION_QUIET_TIME  = 60;
    public static int    NOTIFICATION_CHAIN_MAX   = 10;
    public static int    NOTIFICATION_MAX_EVENTS  = 10;

    // Parameters for quota exceeded notification emails (note that %%EVENTLOGID%% contains percentage usage)
    public static int    QUOTA_WARN_LEVEL       = 90; // send warning at 90% usage

    // Parameters for FIFO-PURGING processes
    public static int    FIFO_PURGING_LEVEL_START = 95; // start purging at 95%
    public static int    FIFO_PURGING_LEVEL_END   = 70; // end purging at 70%
    public static int    FIFO_PURGING_TTL         = 60; // 60secs max lifetime for purging threads

    //enable/disable moving of mas inbox items to purge_inbox_mas_item table during fifo purge
    public static boolean FIFO_PURGING_MAS_PURGE   = false;


    public static String   NAT_HOST_CSV          = "";             //nat host server name suffix csv
    public static HashMap  NAT_HOST_HASHMAP      = null;            //nat host server name to internal IP suffix lookup hashmap
    public static HashMap  NAT_HOST_LB_HASHMAP   = null;            //nat host server name to internal LB VIP suffix lookup hashmap
    public static Object[] NAT_HOST_HASHMAP_KEYS = null;            //nat host server name hashmap keys array for random selection
        
    public static boolean SIP_PREFER_NAT       = false;          // prefer NAT SIP over 3G SIP even if 3G is online
    
    public static String CSI_URL              = null;         //Central Station Interface URLs to post alarms
    public static String ESERVICE_URL         = null;         //Listener URL for state updates (posted as <url>?key=gguid&dev=dguid&MOID=oid&value=val)

    public static String ESERVICE_FILTER_EID         = "1030,1035,1036,1050,660,661,2090,2091,2092,2093,1060,1061,1062,1064,1065";    
    public static String ESERVICE_FILTER_OID_BINARY  = "518,519,1258,1259";    
    public static int    ESERVICE_MAX_SIZE_BINARY    = 200;     //in Kbytes
    
    
    public static int MAXAGE_EVENTLOG        = 30;         // max age (in days) of entries in eventlogs
    public static int MAXAGE_COMMANDQUEUE    =  1;         // max age (in days) of entries in commandqueue
    public static int MAXAGE_WASTEBASKET     =  0;         // Max age in days for account wastebasket items (0=no aging).
    
    public static int COMMANDQUEUE_EXPIRE_THRESHOLD    =  30;         // Threshold (in minutes) to drop commands before sending to DLA


    public static String PROVIDER_ENCRYPTION = null;

    
    // xail codes
    public static int     XAIL_CODE_ACK             = 0;
    public static int     XAIL_CODE_NAK             = 2;
    public static int     XAIL_CODE_NOTPROCESSED    = 200;
    
    
    
    public static String  XAIL_3G_IP                = null;         //3G network IP list specified as a regular expression
    
    public static int     XAIL_CLIENT_CERTS         = 0;            //XAIL Client certs: 0=optional, 1=required
    public static boolean XAIL_CERT_AUTH_ENABLED     = false;

    public static final int MOBJECT_VALUE_MAXLEN    = 1500;         //maximum value length allowed for mobjects
    
    public static final String DICTIONARY_CATALOG_ID = "00019999999999";

    
    public static final String OID_WELLKNOWN_TESTMODE   = "9";          //Test Mode OID
    public static final String OID_WELLKNOWN_TIMEZONE   = "1026";       //TimeZone OID
    
    // External Service API timeout configuration.
    public static final boolean API_TIMEOUT_ENABLE = true;
    public static final int API_TIMEOUT = 30; // value in a seconds.
    
    //-------------- FOR NOW !!!!!
    
    public static final String CAMERA_STREAM_SUPPORTED_TYPES =  ".m3u8.flv.swf.mjpeg.ts.sav.rtsp.cgi";
    public static final String CAMERA_STREAM_SUPPORTED_BW   =  ".0.1";
    
    //---MBus-----
    public static boolean MBUS_SYNC_ENABLED = false ; // default is false
    
    public static String MBUS_SYNC_MSG_TYPES = null ; // default is null
    
    public static int MBUS_THREAD_POOL_SIZE  =0; // default the size is 0- not use multi-threading
    
   // public static String MBUS_BINARY_FILTER_EID_MODE = "include";
  //  public static String MBUS_BINARY_FILTER_EID         = "na"; 
    public static String MBUS_BINARY_FILTER_OID_MODE = "include";
    public static String MBUS_BINARY_FILTER_OID  = "518,519,1258,1259";    
    public static int    MBUS_BINARY_MAX_SIZE    = 200;     //in Kbytes
    
    ///public static String SUBS_FEATURES = "Subscription Features";  // Added for DLDP 4207
    
    //---MBus-----
    
    //static Camera page hashmap, keyed by <cat id>-<typ>-<bw>
    public static HashMap<String,String> CAMERA_STREAM_URLS = new HashMap<String,String>();

    //Notification opt-in
    public static boolean NOTIFICATION_OPT_IN_ENABLED      = false;
    public static final int NOTIFICATION_OPT_OUT = 0;
    public static final int NOTIFICATION_OPT_IN = 1;
    public static final int NOTIFICATION_OPT_IN_REQUESTED = 2;
    
    public static String SERVER_CAPS = "none";

    // Environment ID.
    public static String ENVIRONEMENT_ID = "Dev";
    public static String LICENSE_SERVICE_URL = "localhost:8080";
    
    public static String FEATURE_KEY_HOLDER= "KEYHOLDERLVL";
    
    public static String DLSEC= "DLSEC";
    public static String DLLITE="DLLITE";           //DLITE Subscription.

    public static boolean DLC_IIWC_SESSIONS_ENABLED = true;
    public static String IIWC_SERVER_SESSION_URL = null;
    public static final int IIWC_SESSION_TIMEOUT = 8; // value in a seconds.

    //rate-limit/flow control params
    public static  long RATECONTROL_CHECK_TIME = 5 ;
   // public static  boolean IS_FLOWCONTROL_AI_ENABLED = false;
    public static  boolean IS_FLOWCONTROL_ENABLED = false;
    public static  String RATECONTROL_FILE_LOCATION = "";
   // public static ConcurrentHashMap<String,String> entryGroupMap = new ConcurrentHashMap<String,String>();
   // public static ConcurrentHashMap<String,String> rateEntryMap = new ConcurrentHashMap<String,String>();
   public static ExecutorService rateControlService =  Executors.newFixedThreadPool(1);
   // public static int dlcGroupCount = 32;
    
       public static ConcurrentHashMap<String,RateControlFile> rcFileMap = new ConcurrentHashMap<String,RateControlFile>();
    
    //Cache Template
    public static final ReentrantReadWriteLock templateCacheLock = new ReentrantReadWriteLock();
    public static final ReentrantReadWriteLock templateDBLock = new ReentrantReadWriteLock();
    
    public static final ReentrantReadWriteLock providerDBLock = new ReentrantReadWriteLock();

    //Config Manager Service Processing Test
    public static boolean CONFIG_FACILITY_TEST_ENABLED =false;

    //DLA Authentication Service params
    public static boolean DLA_AUTHENTICATE_VIA_SERVICE  = false;
    public static String DLA_AUTHENTICATE_SERVICE_HOST = "localhost:8095";
    public static int DLA_AUTHENTICATE_SERVICE_CONNECTION_TIMEOUT = 5000;
    
   // External service configuration file key parameter in jvm.
 	public static final String EXTERNAL_SERVICE_CONFIG_FILE = "external.service.config";
 	
 	public final static String GWAY_CATALOG_ID = "00010000010004";
 	public final static String VGWAY_CATALOG_ID = "00010000090004";

 	
 	

    public static synchronized boolean isInitialized() { 
            if(!isInitialized) {
                isInitialized = true;
                return false;
            }else {
                return true;
            }
    }
    
    public static String getCharSet() {
        if(COREDS_CHARSET==null || COREDS_CHARSET.equals("US7ASCII")) {
            //return null;
            return "UTF-8";     //changed to return UTF8 always
        }else {
            return "UTF-8";
        }
    }

    public static String lookupNatIp(String hostname) {
        if(NAT_HOST_HASHMAP==null) return hostname;     //no nat hosts defined, return hostname as is

        String ip=null;

        if(hostname!=null) { //a nat hostname specified, locate its ip address
            ip=(String) NAT_HOST_HASHMAP.get(hostname);
        }else {              //no nat hostname, pick one randomly
            Random generator = new Random(); 
            String host=null;
            if(NAT_HOST_HASHMAP_KEYS==null) NAT_HOST_HASHMAP_KEYS = NAT_HOST_HASHMAP.keySet().toArray();
                host = (String) NAT_HOST_HASHMAP_KEYS[generator.nextInt(NAT_HOST_HASHMAP_KEYS.length)];
                
            if (NAT_HOST_LB_HASHMAP != null) {
                ip = (String) NAT_HOST_LB_HASHMAP.get(host);
            }
            if (ip==null) {
                ip = (String) NAT_HOST_HASHMAP.get(host);
            }
        }

        return (ip==null) ? hostname : ip;
    }  
    
    
    
    public static boolean isMbusBinary(String oid){
    	if(MBUS_BINARY_FILTER_OID_MODE.equalsIgnoreCase("include")){
            if((","+MBUS_BINARY_FILTER_OID+",").indexOf(","+oid+",") != -1) {
                    return true;
            }else{
                    return false;
            }
    	}else{
            if((","+MBUS_BINARY_FILTER_OID+",").indexOf(","+oid+",") != -1) {
                    return false;
            }else{
                    return true;
            }
    	}
    }
    
     //4182 
    
        public static String getRateControlValue(String gguid){
    	
    	 String value= null;
    	
    	for(RateControlFile rcFile : rcFileMap.values()){
    		
    		value =	rcFile.getRate(gguid);
    	if(value != null) {
    		 if(log.isDebugEnabled()){
     	    	 log.debug(rcFile.getFileName() + " : GGUID : " +gguid + "  : Rate : " + value);
     	     }
    		return value;
    	}
    		
    	}
    	
  
    	  if(log.isDebugEnabled()){
 	    	 log.debug("GGUID : " +gguid + "  : Rate : " + value);
 	     }
		return value;
    	
    }
    
    /**
    // base directory to store account specific data
    // e.g. <mount_root>/<mount_point>/account/<domain>/<accountDir>/
    //      where accountDir is typically "<segment>/<accountId>"
    public static String getBaseAccountDir(String mount, String domain, String accountDir) {
        if(mount==null)
            return APP_MOUNT_ROOT + APP_MOUNT_DIR + ACCOUNT_DIRECTORY + "/" + domain + "/" + accountDir ;        
        else
            return APP_MOUNT_ROOT + mount + ACCOUNT_DIRECTORY + "/" + domain + "/" + accountDir ;        
    }
    **/
 
}
