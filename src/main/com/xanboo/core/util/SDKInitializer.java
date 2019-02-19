/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/util/SDKInitializer.java,v $
 * $Id: SDKInitializer.java,v 1.24 2011/07/01 16:11:30 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.util;

import com.att.dlife.dlcore.mbus.client.publisher.PublisherUtil;
import com.xanboo.core.sdk.sysadmin.SysAdminManager;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooAdminPrincipal;
import com.xanboo.core.util.fs.FSProviderCache;
import com.xanboo.core.util.fs.XanbooFSProviderProxy;
import org.apache.log4j.PropertyConfigurator;

import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * Class to initialize Core SDK
 */
public class SDKInitializer {
    private HashMap sysParams = null;
    private Logger logger=null;

    /** 
     * Initializes the SDK for local SDK/ejb invocations/deployments
     */
    public SDKInitializer(String contextPath) throws Exception {
        this(contextPath, null, null);
    }

    /**
     * Initializes the SDK for remote SDK/ejb invocations/deployments
     * @param contextPath optional application context string
     * @param sdkJndiContextFactory optional jndi factory class impl. If passed null, "org.jnp.interfaces.NamingContextFactory" will be defaulted
     * @param sdkJndiUrl url for sdk jndi lookups (e.g. jnp://192.168.1.131:1099)
     */
    
    public SDKInitializer(String contextPath, String sdkJndiContextFactory, String sdkJndiUrl) throws Exception {
        if(logger==null) logger=LoggerFactory.getLogger(this.getClass().getName());

        if(GlobalNames.isInitialized()) return;

        try {
            EjbProxy proxy = null;

            // get SDK EJB/APP tier jndi props
            sdkJndiUrl = System.getProperty("sdk.naming.provider.url", sdkJndiUrl);
            sdkJndiContextFactory = System.getProperty("sdk.naming.factory.initial", sdkJndiContextFactory);

            if(sdkJndiUrl!=null && sdkJndiUrl.length()>0) {
               if(sdkJndiContextFactory==null || sdkJndiContextFactory.length()==0) {
                   sdkJndiContextFactory = "org.jnp.interfaces.NamingContextFactory";
               }
               GlobalNames.SDK_JNDI_PROVIDER_URL = sdkJndiUrl;
               GlobalNames.SDK_JNDI_CONTEXT_FACTORY = sdkJndiContextFactory;

               proxy = new EjbProxy(GlobalNames.SDK_JNDI_CONTEXT_FACTORY, GlobalNames.SDK_JNDI_PROVIDER_URL);

            }else {
                proxy = new EjbProxy();
            }

            SysAdminManager sManager = (SysAdminManager) proxy.getObj(GlobalNames.EJB_SYSADMIN_MANAGER);
            XanbooResultSet xs = sManager.getSystemParam(new XanbooAdminPrincipal(GlobalNames.SERVICE_USER, 1, 2), null);

            sysParams = new HashMap();

            for(int i=0; i<xs.size();i++) {
                HashMap param = (HashMap) xs.get(i);
                sysParams.put(param.get("PARAM"), param.get("VALUE"));
            }

            readCoreParameters(contextPath);
       }catch(Exception e) {
           throw e;
       }
    }

   private void checkCreatDir(String dirPath) {
        XanbooFSProviderProxy.getInstance().checkDir(null, dirPath, true);
   }
    
    
    // reads Core system parameters from DB or property file
   public void readCoreParameters(String contextPath) {

        // read system data store settings
        GlobalNames.APP_MOUNT_ROOT       = getCoreParameter("app.mount.root", GlobalNames.APP_MOUNT_ROOT);
        GlobalNames.APP_MOUNT_DIR        = getCoreParameter("app.mount.dir", GlobalNames.APP_MOUNT_DIR);
        GlobalNames.SYSTEM_MOUNT_DIR     = getCoreParameter("app.mount.system", GlobalNames.APP_MOUNT_DIR);
        
        GlobalNames.APP_FS_PROVIDERS     = getCoreParameter("app.fs.providers", GlobalNames.APP_FS_PROVIDERS);
        GlobalNames.APP_FS_MOUNTPOINTS   = getCoreParameter("app.fs.mountpoints", GlobalNames.APP_MOUNT_DIR);
        
        GlobalNames.APP_FS_MOUNTPOINTS_RO   = getCoreParameter("app.fs.mountpoints.readonly", GlobalNames.APP_FS_MOUNTPOINTS_RO);
        GlobalNames.MEDIA_SERVICE_MOUNTPOINT   = getCoreParameter("app.fs.mediasvc.mountpoints", GlobalNames.MEDIA_SERVICE_MOUNTPOINT);
        GlobalNames.MEDIA_SERVICE_ENABLED   = getCoreParameter("mediasvc.enabled", GlobalNames.MEDIA_SERVICE_ENABLED);

        FSProviderCache.getInstance().initialize(GlobalNames.APP_FS_PROVIDERS, GlobalNames.APP_FS_MOUNTPOINTS, GlobalNames.APP_FS_MOUNTPOINTS_RO);
        
        // Added for DPDP 2816 License management.
        GlobalNames.ENVIRONEMENT_ID      = getCoreParameter("license.environment.id", GlobalNames.ENVIRONEMENT_ID);
        GlobalNames.LICENSE_SERVICE_URL  = getCoreParameter("license.server.url", GlobalNames.LICENSE_SERVICE_URL);
        
        // create sys directories, if necessary
        String systemDir = GlobalNames.APP_MOUNT_ROOT + GlobalNames.SYSTEM_MOUNT_DIR + GlobalNames.SYSTEM_DIRECTORY;

        GlobalNames.SYSTEM_LOG_DIRECTORY =  systemDir + GlobalNames.LOG_DIRECTORY;
        checkCreatDir(GlobalNames.SYSTEM_LOG_DIRECTORY);
        
        GlobalNames.SYSTEM_DOWNLOAD_DIRECTORY = systemDir + GlobalNames.DOWNLOAD_DIRECTORY;
        GlobalNames.SYSTEM_MOBJECT_DIRECTORY = GlobalNames.SYSTEM_DOWNLOAD_DIRECTORY + GlobalNames.MOBJECT_DIRECTORY;
        checkCreatDir(GlobalNames.SYSTEM_MOBJECT_DIRECTORY);


        //temporary location for file uploads. If not specified or not an absolute path, use relative path to system folder
        GlobalNames.ITEM_UPLOAD_DIRECTORY = getCoreParameter("app.tmp.dir", GlobalNames.ITEM_UPLOAD_DIRECTORY);
        if(GlobalNames.ITEM_UPLOAD_DIRECTORY==null || GlobalNames.ITEM_UPLOAD_DIRECTORY.length()==0)
            GlobalNames.ITEM_UPLOAD_DIRECTORY  = systemDir + GlobalNames.TEMP_DIRECTORY;
        else if(!GlobalNames.ITEM_UPLOAD_DIRECTORY.startsWith("/"))
            GlobalNames.ITEM_UPLOAD_DIRECTORY  = systemDir + "/" + GlobalNames.ITEM_UPLOAD_DIRECTORY;

        checkCreatDir(GlobalNames.ITEM_UPLOAD_DIRECTORY);
        

        // read and setup LOG4J properties config
        String log4jConfig = System.getProperty("app.log.config", null);
        // if specified as a system property use that as an override to xancore config param
        if(log4jConfig != null && log4jConfig.length()>0) {
           GlobalNames.LOG4J_CONFIG = log4jConfig;
        }else {
           GlobalNames.LOG4J_CONFIG = getCoreParameter("app.log.config", GlobalNames.LOG4J_CONFIG);
        }
        if(GlobalNames.LOG4J_CONFIG != null) {
            if(GlobalNames.LOG4J_CONFIG.startsWith("/")) {  //absolute path
                PropertyConfigurator.configure(GlobalNames.LOG4J_CONFIG);
            }else { //relative path to jboss home environment
                String jBossHome = System.getenv("JBOSS_HOME");
                if(jBossHome!=null) {
                    PropertyConfigurator.configure(jBossHome + "/" + GlobalNames.LOG4J_CONFIG);
                }
            }
        }
        
	logger=LoggerFactory.getLogger(this.getClass().getName());
        logger.info("[SDKInitializer()]: Initializing Xanboo Core SDK parameters");
        
        // load device parameters
        GlobalNames.DEVICE_INVITATION_QUOTA    = getCoreParameter("quota.invitation.device", GlobalNames.DEVICE_INVITATION_QUOTA);
        
	// load item parameters
        GlobalNames.DEFAULT_DOMAIN    = getCoreParameter("app.domain.default", GlobalNames.DEFAULT_DOMAIN);
        GlobalNames.DAO_CLASS         = getCoreParameter("app.dao.class", GlobalNames.DAO_CLASS);
        GlobalNames.MAXAGE_EVENTLOG   = getCoreParameter("app.maxage.eventlog", GlobalNames.MAXAGE_EVENTLOG);
        GlobalNames.MAXAGE_COMMANDQUEUE   = getCoreParameter("app.maxage.commandqueue", GlobalNames.MAXAGE_COMMANDQUEUE);
        GlobalNames.MAXAGE_WASTEBASKET    = getCoreParameter("app.maxage.wastebasket", GlobalNames.MAXAGE_WASTEBASKET);
        GlobalNames.COREDS_CHARSET        = getCoreParameter("db.charset", GlobalNames.COREDS_CHARSET);
        if(GlobalNames.MAXAGE_EVENTLOG<1) GlobalNames.MAXAGE_EVENTLOG=1;
        if(GlobalNames.MAXAGE_COMMANDQUEUE<1) GlobalNames.MAXAGE_COMMANDQUEUE=1;
        GlobalNames.COMMANDQUEUE_EXPIRE_THRESHOLD   = getCoreParameter("commandqueue.expire.threshold", GlobalNames.COMMANDQUEUE_EXPIRE_THRESHOLD);

        //hashing algorith used for passwords: 0:MD5, 1:SHA-1 or 2:SHA-256
        GlobalNames.APP_HASHING_ALGORITHM        = getCoreParameter("app.password.hashing", GlobalNames.APP_HASHING_ALGORITHM);
              
        // enable/disable modules
        GlobalNames.MODULE_WASTEBASKET_ENABLED = getCoreParameter("module.wastebasket.enabled", GlobalNames.MODULE_WASTEBASKET_ENABLED);
        GlobalNames.MODULE_EVENTLOG_ENABLED = getCoreParameter("module.eventlog.enabled", GlobalNames.MODULE_EVENTLOG_ENABLED);

        //enable/disable SBN professional monitoring
        GlobalNames.MODULE_SBN_ENABLED = getCoreParameter("module.sbn.enabled", GlobalNames.MODULE_SBN_ENABLED);
        //enable/disable zip2timezone lookups
        GlobalNames.APP_TIMEZONE_LOOKUP = getCoreParameter("app.timezone.lookup", GlobalNames.APP_TIMEZONE_LOOKUP);
        
        //enable/disable RMS
        GlobalNames.MODULE_RMS_ENABLED = getCoreParameter("module.rms.enabled", GlobalNames.MODULE_RMS_ENABLED);
        //enable/disable PrimeHome
        GlobalNames.MODULE_PH_ENABLED = getCoreParameter("module.ph.enabled", GlobalNames.MODULE_PH_ENABLED);
        
        //enable/disable CSI
        GlobalNames.MODULE_CSI_ENABLED = getCoreParameter("module.csi.enabled", GlobalNames.MODULE_CSI_ENABLED);
        
        GlobalNames.MODULE_PAI_ENABLED = getCoreParameter("module.pai.enabled", GlobalNames.MODULE_PAI_ENABLED);
        
        logger.info("[SDKInitializer()]: RMS Integration:" +  (GlobalNames.MODULE_RMS_ENABLED ? "ENABLED" : "DISABLED") );            
        logger.info("[SDKInitializer()]: SBN Integration:" +  (GlobalNames.MODULE_SBN_ENABLED ? "ENABLED" : "DISABLED") );                    
        logger.info("[SDKInitializer()]: CSI Integration:" +  (GlobalNames.MODULE_CSI_ENABLED ? "ENABLED" : "DISABLED") );  
        logger.info("[SDKInitializer()]: PH Integration:" +  (GlobalNames.MODULE_PH_ENABLED ? "ENABLED" : "DISABLED") );  
        
        GlobalNames.ITEM_MAX_SIZE           = getCoreParameter("item.size.max", GlobalNames.ITEM_MAX_SIZE);
        GlobalNames.ITEM_IMAGE_TYPES        = getCoreParameter("item.image.types", GlobalNames.ITEM_IMAGE_TYPES);
        GlobalNames.ITEM_IMAGE_PROCESSOR    = getCoreParameter("item.image.processor", GlobalNames.ITEM_IMAGE_PROCESSOR);
        GlobalNames.ITEM_THUMB_ENABLED      = getCoreParameter("item.thumb.enabled", GlobalNames.ITEM_THUMB_ENABLED);
        GlobalNames.ITEM_THUMB_WIDTH        = getCoreParameter("item.thumb.width", GlobalNames.ITEM_THUMB_WIDTH);
        GlobalNames.ITEM_THUMB_HEIGHT       = getCoreParameter("item.thumb.height", GlobalNames.ITEM_THUMB_HEIGHT);
        GlobalNames.ITEM_QUOTA_POLICY       = getCoreParameter("item.quota.policy", GlobalNames.ITEM_QUOTA_POLICY);
        GlobalNames.ITEM_ENCRYPTION_ENABLED = getCoreParameter("item.encryption.enabled", GlobalNames.ITEM_ENCRYPTION_ENABLED);

        GlobalNames.XAIL_CODE_ACK           = getCoreParameter("xail.code.ack", GlobalNames.XAIL_CODE_ACK);
        GlobalNames.XAIL_CODE_NAK           = getCoreParameter("xail.code.nak", GlobalNames.XAIL_CODE_NAK);
        GlobalNames.XAIL_CODE_NOTPROCESSED  = getCoreParameter("xail.code.notprocessed", GlobalNames.XAIL_CODE_NOTPROCESSED);

        GlobalNames.XAIL_3G_IP              = getCoreParameter("xail.3G.ip", GlobalNames.XAIL_3G_IP);
        GlobalNames.XAIL_CLIENT_CERTS       = getCoreParameter("xail.certs", GlobalNames.XAIL_CLIENT_CERTS);
        GlobalNames.XAIL_CERT_AUTH_ENABLED  = getCoreParameter("xail.cert.auth.enabled", GlobalNames.XAIL_CERT_AUTH_ENABLED);
        
        GlobalNames.RULE_FACTORY_IMPL       = getCoreParameter("system.rule.factory.classname",GlobalNames.RULE_FACTORY_IMPL);
        
        // read notification parameters
        GlobalNames.NOTIFICATION_QUIET_TIME   = getCoreParameter("notification.quiettime",GlobalNames.NOTIFICATION_QUIET_TIME);
        GlobalNames.NOTIFICATION_PROFILE_MAX  = getCoreParameter("notification.profile.max", GlobalNames.NOTIFICATION_PROFILE_MAX);
        GlobalNames.NOTIFICATION_CHAIN_MAX   = getCoreParameter("notification.chain.max",GlobalNames.NOTIFICATION_CHAIN_MAX);
        
        GlobalNames.ACCTALERT_CREATE_EVENT = getCoreParameter("app.account.alert.create",GlobalNames.ACCTALERT_CREATE_EVENT);
        GlobalNames.ACCTALERT_CLEAR_EVENT = getCoreParameter("app.account.alert.clear",GlobalNames.ACCTALERT_CLEAR_EVENT);
        
        //push parameters
        GlobalNames.PUSH_CATEGORY_EVENTS = getCoreParameter("system.push.eid",null);
        //GlobalNames.PUSH_CATEGORY_STATUS = getCoreParameter("system.push.category.status",null);
        
        // quota warning notification configuration parameters
        GlobalNames.QUOTA_WARN_LEVEL          = getCoreParameter("quota.warning.level", GlobalNames.QUOTA_WARN_LEVEL);

        // fifo purging configuration parameters
        GlobalNames.FIFO_PURGING_LEVEL_START  = getCoreParameter("fifo.purging.level.start", GlobalNames.FIFO_PURGING_LEVEL_START);
        GlobalNames.FIFO_PURGING_LEVEL_END    = getCoreParameter("fifo.purging.level.end", GlobalNames.FIFO_PURGING_LEVEL_END);
        GlobalNames.FIFO_PURGING_TTL          = getCoreParameter("fifo.purging.ttl", GlobalNames.FIFO_PURGING_TTL);
        GlobalNames.FIFO_PURGING_MAS_PURGE = getCoreParameter("fifo.purging.maspurge", GlobalNames.FIFO_PURGING_MAS_PURGE);
        if(GlobalNames.FIFO_PURGING_LEVEL_START<80) GlobalNames.FIFO_PURGING_LEVEL_START=80;
        if(GlobalNames.FIFO_PURGING_LEVEL_END<60) GlobalNames.FIFO_PURGING_LEVEL_END=60;

        // reading mail properties
        GlobalNames.MAIL_HOST              = getCoreParameter("mail.smtp.host", GlobalNames.MAIL_HOST);
        GlobalNames.MAIL_DEBUG             = getCoreParameter("mail.debug", GlobalNames.MAIL_DEBUG);
        GlobalNames.EMAIL_PROFILE_TYPE     = getCoreParameter("system.email.profiletype",GlobalNames.EMAIL_PROFILE_TYPE);
        GlobalNames.SMS_PROFILE_TYPE       = getCoreParameter("system.sms.profiletype",GlobalNames.SMS_PROFILE_TYPE);
        GlobalNames.PUSH_PROFILE_TYPE      = getCoreParameter("system.push.profiletype",GlobalNames.PUSH_PROFILE_TYPE);
        GlobalNames.NOTIF_TEMPL_RELOAD_INTERVAL = getCoreParameter("system.template.cache.reload",GlobalNames.NOTIF_TEMPL_RELOAD_INTERVAL);
        GlobalNames.NOTIF_PROVIDER_RELD_INTERVAL = getCoreParameter("system.profiletype.cache.reload",GlobalNames.NOTIF_PROVIDER_RELD_INTERVAL);
        
        GlobalNames.NAT_HOST_CSV           = getCoreParameter("nat.host", GlobalNames.NAT_HOST_CSV);
        
        GlobalNames.SIP_PREFER_NAT         = getCoreParameter("sip.prefer.nat", GlobalNames.SIP_PREFER_NAT);

        GlobalNames.SERVER_CAPS            = getCoreParameter("app.server.capabilities", GlobalNames.SERVER_CAPS);
        
        // get eService listener URL from system props.
        GlobalNames.ESERVICE_URL = System.getProperty("sdk.eservice.url", null);
        if(GlobalNames.ESERVICE_URL==null) {
            GlobalNames.ESERVICE_URL = getCoreParameter("eservice.url", GlobalNames.ESERVICE_URL);
        }
        if(GlobalNames.ESERVICE_URL!=null && GlobalNames.ESERVICE_URL.trim().length()==0) GlobalNames.ESERVICE_URL=null;
        
        //eservice eid and binary oid filter defs (csv list of allowed eids and binary oids)
        GlobalNames.ESERVICE_FILTER_EID = getCoreParameter("eservice.filter.eid", GlobalNames.ESERVICE_FILTER_EID);
        GlobalNames.ESERVICE_FILTER_OID_BINARY = getCoreParameter("eservice.filter.oid.binary", GlobalNames.ESERVICE_FILTER_OID_BINARY);
        GlobalNames.ESERVICE_MAX_SIZE_BINARY = getCoreParameter("eservice.binary.maxsize", GlobalNames.ESERVICE_MAX_SIZE_BINARY);
        
        //encryption provider class
        GlobalNames.PROVIDER_ENCRYPTION  = getCoreParameter("provider.encryption.class", GlobalNames.PROVIDER_ENCRYPTION);
        if(GlobalNames.PROVIDER_ENCRYPTION==null || GlobalNames.PROVIDER_ENCRYPTION.trim().length()==0) GlobalNames.PROVIDER_ENCRYPTION=null;
        
    	// --MBus-- start

		GlobalNames.MBUS_SYNC_MSG_TYPES = getCoreParameter("mbus.sync.enabled",
				GlobalNames.MBUS_SYNC_MSG_TYPES);

		if (GlobalNames.MBUS_SYNC_MSG_TYPES != null
				&& !GlobalNames.MBUS_SYNC_MSG_TYPES.toLowerCase().equals(
						"false")) {
			GlobalNames.MBUS_SYNC_ENABLED = true;

			try {
				PublisherUtil.initialize();
			} catch (Throwable e) {
				// e.printStackTrace();

				logger.warn("[SDKInitialier()]: MBUS Integration failed :" + e.getMessage());
				GlobalNames.MBUS_SYNC_ENABLED = false;

			}

			if (GlobalNames.MBUS_SYNC_ENABLED) {
				GlobalNames.MBUS_THREAD_POOL_SIZE = getCoreParameter(
						"mbus.threadpool.size",
						GlobalNames.MBUS_THREAD_POOL_SIZE);

			}

		}

		logger.info("[SDKInitializer()]: MBUS Integration:"
				+ (GlobalNames.MBUS_SYNC_ENABLED ? "ENABLED" : "DISABLED"));
		
		
/*		GlobalNames.MBUS_BINARY_FILTER_EID_MODE = getCoreParameter("mbus.binary.eid.mode",
				GlobalNames.MBUS_BINARY_FILTER_EID_MODE);
		
		GlobalNames.MBUS_BINARY_FILTER_EID = getCoreParameter("mbus.binary.eid",
				GlobalNames.MBUS_BINARY_FILTER_EID);
		
		logger.info("[SDKInitializer()]: "
				+ "\n MBUS_BINARY_FILTER_EID_MODE : " + GlobalNames.MBUS_BINARY_FILTER_EID_MODE 
				+ "\n MBUS_BINARY_FILTER_EID : " + GlobalNames.MBUS_BINARY_FILTER_EID );*/
		
		GlobalNames.MBUS_BINARY_FILTER_OID_MODE = getCoreParameter("mbus.binary.oid.mode",
				GlobalNames.MBUS_BINARY_FILTER_OID_MODE);
		
		GlobalNames.MBUS_BINARY_FILTER_OID = getCoreParameter("mbus.binary.oid",
				GlobalNames.MBUS_BINARY_FILTER_OID);
		
		 GlobalNames.MBUS_BINARY_MAX_SIZE = getCoreParameter("mbus.binary.maxsize", 
				 GlobalNames.MBUS_BINARY_MAX_SIZE);
		 
		
			
                logger.info("[SDKInitializer()]: "
                                + "\n MBUS_BINARY_FILTER_OID_MODE : " + GlobalNames.MBUS_BINARY_FILTER_OID_MODE 
                                + "\n MBUS_BINARY_FILTER_OID : " + GlobalNames.MBUS_BINARY_FILTER_OID );

                logger.info("[SDKInitializer()]: "
                                + "\n MBUS_BINARY_MAX_SIZE : " + GlobalNames.MBUS_BINARY_MAX_SIZE );
		
		// ---MBus --end
       
 //4182 flow control
                
                // RateControlCheck in GlobalNamesInitializer (called from XancoreInitializer) should suffice 
                // SDKInitializer call should not be needed, since that is class is typically used by other applications to initialize the SDK, not xancore.  
/*        GlobalNames.RATECONTROL_CHECK_TIME = getCoreParameter("app.ratecontrol.checkinterval", GlobalNames.RATECONTROL_CHECK_TIME);
        GlobalNames.RATECONTROL_CHECK_TIME = GlobalNames.RATECONTROL_CHECK_TIME * 60 *1000;
        
        GlobalNames.RATECONTROL_FILE_LOCATION = getCoreParameter("app.ratecontrol.file", GlobalNames.RATECONTROL_FILE_LOCATION);
        
        if(GlobalNames.IS_AI_SERVER || GlobalNames.IS_AIA_SERVER){
        	//start a rate control check thread
        	GlobalNames.rateControlService.execute(new RateControlCheckTask());
        
        }

        logger.info("[SDKInitializer()]: RATECONTROL_FILE_LOCATION:" + GlobalNames.RATECONTROL_FILE_LOCATION + 
                                             ", RATECONTROL_CHECK_TIME(mins):" + GlobalNames.RATECONTROL_CHECK_TIME/60000);*/
        //4182 
                        

        
        //Video Verification
        GlobalNames.VVS_ENABLED = getCoreParameter("module.vvs.enabled", GlobalNames.VVS_ENABLED);
        GlobalNames.VVS_MOUNT_ROOT = getCoreParameter("vvs.mount.root", GlobalNames.VVS_MOUNT_ROOT);
        GlobalNames.VVS_DATA_DIR = getCoreParameter("vvs.dir", GlobalNames.VVS_DATA_DIR);
        GlobalNames.VVS_EVENT_ID = getCoreParameter("vvs.eventid", GlobalNames.VVS_EVENT_ID);

        //Notfification opt-in
        GlobalNames.NOTIFICATION_OPT_IN_ENABLED = getCoreParameter("notification.optin.enabled", GlobalNames.NOTIFICATION_OPT_IN_ENABLED);

        //IIWC configurations
        GlobalNames.IIWC_SERVER_SESSION_URL = getCoreParameter("iiwc.sessions.url", GlobalNames.IIWC_SERVER_SESSION_URL);
        
        if (GlobalNames.SERVER_CAPS.indexOf("DLAIIWC") != -1)
            GlobalNames.DLC_IIWC_SESSIONS_ENABLED = true;
        else
            GlobalNames.DLC_IIWC_SESSIONS_ENABLED = false;

       //DLA Authentication params
       GlobalNames.DLA_AUTHENTICATE_VIA_SERVICE = getCoreParameter("dla.authenticate.via.service", GlobalNames.DLA_AUTHENTICATE_VIA_SERVICE);
       GlobalNames.DLA_AUTHENTICATE_SERVICE_HOST = getCoreParameter("dla.authenticate.service.host", GlobalNames.DLA_AUTHENTICATE_SERVICE_HOST);
       GlobalNames.DLA_AUTHENTICATE_SERVICE_CONNECTION_TIMEOUT = getCoreParameter("dla.authenticate.service.connection.timeout", GlobalNames.DLA_AUTHENTICATE_SERVICE_CONNECTION_TIMEOUT);

       //check if NAT hostname 2 ip address lookup csv is specified or not
        //csv is in host:ip[:lbvip],host:ip[:lbvip].... form
        String natLookupCSV = getCoreParameter("nat.host.lookup", null);
        if(natLookupCSV!=null) {
            /* parse a list of hostname:ip[:lbvip] tokens separated by , ; or space */
            StringTokenizer st = new StringTokenizer(natLookupCSV,", ;");
            while (st.hasMoreTokens()) {
                String foundHost = st.nextToken();
                StringTokenizer st2 = new StringTokenizer(foundHost,":");
                String hName = st2.nextToken();
                String hIp = st2.nextToken();
                String hLbIp = null;
                if (st2.hasMoreTokens()) hLbIp = st2.nextToken();

                //lazy init if necessary, and add ip to the hashmap
                if(GlobalNames.NAT_HOST_HASHMAP==null) {
                    GlobalNames.NAT_HOST_HASHMAP=new HashMap();
                }
                GlobalNames.NAT_HOST_HASHMAP.put(hName, hIp);

                if(hLbIp != null) {
                    if(GlobalNames.NAT_HOST_LB_HASHMAP==null) {
                        GlobalNames.NAT_HOST_LB_HASHMAP=new HashMap();
                    }
                    GlobalNames.NAT_HOST_LB_HASHMAP.put(hName, hLbIp);
                }
            }
        }
        
        logger.info("[SDKInitializer()]: Initializing File System Providers/Mount Points" + FSProviderCache.getInstance().dump());            

        logger.info("[SDKInitializer()]: Initialization completed.");
   }
   
   

   // gets a string property
   public String getCoreParameter(String propName, String propDefault) {
       if(sysParams!=null && sysParams.containsKey(propName)) {
        logger.info("READING name=" + propName + ", val=" + (String) sysParams.get(propName));       
           return (String) sysParams.get(propName);
       }else {
        //System.err.println("READING name=" + propName + ", default=" + propDefault);       
           return propDefault;
       }
   }

   
   // gets an int property
   public int getCoreParameter(String propName, int propDefault) {
        int propValue;

        try {
            propValue=Integer.parseInt(getCoreParameter(propName, Integer.toString(propDefault)));
        }catch(NumberFormatException ne) { return propDefault; }

        return propValue;
   }


   // gets a long property
   public long getCoreParameter(String propName, long propDefault) {
        long propValue;

        try {
            propValue=Long.parseLong(getCoreParameter(propName, Long.toString(propDefault)));
        }catch(NumberFormatException ne) { return propDefault; }

        return propValue;
   }


   // gets a byte property
   public byte getCoreParameter(String propName, byte propDefault) {
        byte propValue;

        try {
            propValue=Byte.parseByte(getCoreParameter(propName, Byte.toString(propDefault)));
        }catch(NumberFormatException ne) { return propDefault; }

        return propValue;
   }
   
   // gets a boolean property
   public boolean getCoreParameter(String propName, boolean propDefault) {
        Boolean propValue;

        try {
            propValue=Boolean.valueOf(getCoreParameter(propName, (propDefault ? "true" : "false")));
        }catch(NumberFormatException ne) { return propDefault; }

        return propValue.booleanValue();
   }
    
    
    
}
