/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.util;

import java.sql.Connection;
import java.util.Date;
import java.util.HashMap;
import com.xanboo.core.model.device.XanbooDevice;
import com.xanboo.core.sdk.account.XanbooAccount;
import java.sql.Types;
import java.util.Set;
/**
 * Singleton to load the push message configuration parameters and queue the 
 * push notification messages in ACTION_QUEUE table
 * @author lm899p
 */
public class PushMessageQueue
{
    private static String SEPARATOR_CHAR = "~:~";
    private Logger logger = null;
    private static PushMessageQueue instance = null;
    private BaseDAO dao;
    private HashMap<Integer,Boolean> eventCategoryMap = new HashMap<Integer,Boolean>();
    //private HashMap<Integer,Integer> eventCategoryMap = new HashMap<Integer,Integer>();
    //private HashMap<Integer,CategoryStatusType> categoryStatusMap = new HashMap<Integer,CategoryStatusType>();
    /**
     * Private constructor / singleton pattern
     */
    private PushMessageQueue()
    {
        try 
        {
            // obtain a Logger instance
            logger=LoggerFactory.getLogger(this.getClass().getName());
            
            // create implementation Class for Oracle, Sybase, etc.
            dao = (BaseDAO) DAOFactory.getDAO();
            
            initialize();
        }
        catch(Exception ne) 
        {
            if(logger.isDebugEnabled()) 
                logger.error("[SysAdminManagerDAO()]: " + ne.getMessage(), ne);
           logger.error("[SysAdminManagerDAO()]: " + ne.getMessage());
        }
        logger.info("[PushMessageQueue()] - create new instance of push queue singleton");
    }
    /**
     * Returns the existing instance of <code>PushMessageQueue</code>
     * @return 
     */
    public static PushMessageQueue getInstance()
    {
        Logger log = LoggerFactory.getLogger("PushMessageQueue");
        if ( log.isDebugEnabled() ) log.debug("[getInstance()] - ");
        
        if ( instance == null )
            instance = new PushMessageQueue();
        return instance;
    }
    /**
     * Private method to initialize the singleton
     */
    private void initialize()
    {
        logger.info("[initialize()] - load push notification configuration parameters");
        logger.info("[initialize()] - Push notification events&categories : "+GlobalNames.PUSH_CATEGORY_EVENTS);
        //logger.info("[initialize()] - Push notification category status : "+GlobalNames.PUSH_CATEGORY_STATUS);
        
        if ( GlobalNames.PUSH_CATEGORY_EVENTS != null && !GlobalNames.PUSH_CATEGORY_EVENTS.equalsIgnoreCase(""))
        {
            
            String[] pushEvents = GlobalNames.PUSH_CATEGORY_EVENTS.split(",");
            for ( int i = 0; i < pushEvents.length; i++ )
            {
                //Integer eventId = new Integer(pushEvents[i].substring(0,pushEvents[i].indexOf(":")));
                //Integer categoryBit = new Integer(pushEvents[i].substring((pushEvents[i].indexOf(":")+1)));
                //eventCategoryMap.put(eventId,categoryBit);
                eventCategoryMap.put(new Integer(pushEvents[i]),true);
            }
        }
        else
            logger.error("[intialize()] - missing config, no events configured for push notification");
        
        /*if ( GlobalNames.PUSH_CATEGORY_STATUS != null && !GlobalNames.PUSH_CATEGORY_STATUS.equalsIgnoreCase("") )
        {
            String[] catStatus = GlobalNames.PUSH_CATEGORY_STATUS.split(",");
            for ( int i = 0 ; i < catStatus.length; i++ )
            {
                Integer catId = new Integer(catStatus[i].substring(0,catStatus[i].indexOf(":")));
                String status = catStatus[i].substring((catStatus[i].indexOf(":")+1));
                categoryStatusMap.put(catId,CategoryStatusType.getStatusType(status));
            }
        }
        else
            logger.error("[initialize()] - missing config, no push categories enabled");
        */
    }
    
    public boolean isEventPush(Integer eventId)
    {
        return eventCategoryMap.containsKey(eventId);
    }
    
    /**
     * 
     * @param conn - The database connection for use when queueing the push notification
     * @param device - an instance of <code>XanbooDevice</code>
     * @param deviceGuid - the GUID of the device which generated the notification. If the XanbooDevice.deviceGuid is null or '0', the value of this field is used. 
     * @param eventId - the numeric event identifier for the push notification
     * @param message - the text of the push notification
     * @param pushUrl - an optional url to be sent with the push notification
     * @param pushBtnName - an optional button label to be sent with the push notification
     * @param eGroupId - 
     * @param timestamp - the date and time of the alert, stored in ACTION_QUEUE using GMT
     * @param accessKey - the access key associated with an event, NULL for solution center
     * @param locale - the locale (language_id) for the push message
     * @param timeZoneId - identifies the time zone used when converting the timestamp from GMT to local time
     * @param profileAddress - an optional destination for the push message. 
     */
    public void queuePushMessage(Connection conn,XanbooDevice device,String deviceGuid, String srcDeviceGuid, 
                                 Integer eventId,String message, String pushUrl, String pushBtnName, String eGroupId,String timeStr,
                                 String accessKey, String locale, String timeZoneId, String profileAddress) throws XanbooException
    {
        HashMap<String,String> srcLabelMap = new HashMap<String,String>();
        srcLabelMap.put("BTN",(pushBtnName != null ? pushBtnName : ""));
        srcLabelMap.put("URL",(pushUrl != null ? pushUrl : ""));
        queuePushMessage(conn, device, deviceGuid, srcDeviceGuid, eventId, message, eGroupId, timeStr, accessKey, locale, timeZoneId, profileAddress, srcLabelMap);
    }
    public void queueEventPushMessage(Connection conn,XanbooDevice device,String deviceGuid, String deviceCatalogId, String srcDeviceGuid, 
                                      Integer eventId,String message, String eGroupId,String timeStr,String accessKey, String locale, 
                                      String timeZoneId, String profileAddress, String customDLA) throws XanbooException
    {
        HashMap<String,String> srcLabelMap = new HashMap<String,String>();
        srcLabelMap.put("CATID",(deviceCatalogId != null ? deviceCatalogId : ""));
        srcLabelMap.put("CUST",(customDLA != null ? customDLA : ""));
        queuePushMessage(conn, device, deviceGuid, srcDeviceGuid, eventId, message, eGroupId, timeStr, accessKey, locale, timeZoneId, profileAddress, srcLabelMap);
    }
    /**
     * 
     * @param conn
     * @param device
     * @param deviceGuid
     * @param srcDeviceGuid
     * @param eventId
     * @param message
     * @param eGroupId
     * @param timeStr
     * @param accessKey
     * @param locale
     * @param timeZoneId
     * @param profileAddress
     * @param srcLabelMap
     * @throws XanbooException 
     */
    protected void queuePushMessage(Connection conn,XanbooDevice device,String deviceGuid, String srcDeviceGuid, Integer eventId, String message,
                                 String eGroupId,String timeStr, String accessKey, String locale, String timeZoneId, String profileAddress,HashMap<String,String> srcLabelMap) 
                throws XanbooException
    {
        //validation
        if ( device == null )
            throw new XanbooException(40005,"Missing required parameter: device");
        
        if ( device.getDomain() == null || device.getDomain().equals(""))
            throw new XanbooException(40005,"Missing required parameter, domain in XanbooDevice");
        
        if ( device.getAccountId() <= 0 ) 
            throw new XanbooException(40005,"Missing required parameter, accountId in XanbooDevice");
                
        if ( eventId == null)
            throw new XanbooException(40005,"The eventId parameter is missing");
        
        if ( message == null || message.isEmpty())
            throw new XanbooException(40005,"Missing required parameter, message is NULL or empty string");
        
        //determine if the category is enabled and if account subscribes
        if ( !eventCategoryMap.containsKey(eventId)) return; //the event is not supported
        
        /*
        Integer category = eventCategoryMap.get(eventId);
               
        if ( !categoryStatusMap.containsKey(category))
        {
            logger.warn("[queuePushMessage()] - A category "+category+" was found for "+eventId+" and the category status is missing");
            return; //missing category status
        }
        //if the category status is disabled OR the category status is "beta" and the accountType for the account is not TYPE_BETA, reject
        if ( categoryStatusMap.get(category) == CategoryStatusType.DISABLED || 
             ( categoryStatusMap.get(category) == CategoryStatusType.BETA_BUDDY && (device.getAccountType() & XanbooAccount.TYPE_BETA) != XanbooAccount.TYPE_BETA ) )
        {
            logger.info("[queuePushMessage()] - push category="+category+" status is "+categoryStatusMap.get(category)+" and accountType="+device.getAccountType());
            return; //do not queue this push message
        }
        
        //check if the category bit is disabled 
        if ( !XanbooUtil.isBitOn(device.getPushPreferences(), category) )
        {
            //user does not subscribe to push for the category
            logger.info("[queuePushMessage()] notifications for "+category+" turned off for accountId="+device.getAccountId()+" pushPrefs="+device.getPushPreferences());
            return ;
        }*/
               
        if ( logger.isDebugEnabled()) {
            logger.debug("[queuePushMessage()]: queuing push notification for account/device="+device+", deviceGuid="+deviceGuid+
                         ",  eventId="+eventId+", message="+message+", eGroupId="+eGroupId+", timestamp="+timeStr+", accessKey="+accessKey+
                         ", locale="+locale+", timeZoneId="+timeZoneId+", profileAddress="+profileAddress+", srcLabelMap="+srcLabelMap);
        }
        
        //category enabled and customer account prefs enabled
        SQLParam[] args = new SQLParam[15+2];
        args[0] = new SQLParam(device.getDomain(),Types.VARCHAR);       //i_domain_id        IN  ACTION_QUEUE.domain_id%TYPE,
        args[1] = new SQLParam(device.getAccountId(),Types.NUMERIC);    //i_account_id       IN  ACCOUNT.account_id%TYPE,
        args[2] = new SQLParam(device.getExtAccId(),Types.VARCHAR);     //i_extacc_id        IN  ACTION_QUEUE.EXT_ACCOUNT_ID%TYPE,
        
        args[3] = new SQLParam("0",Types.VARCHAR);                      //i_gateway_guid     IN  DEVICE.gateway_guid%TYPE,
        if ( device.getGatewayGUID() != null && !device.getGatewayGUID().equalsIgnoreCase(""))
            args[3] = new SQLParam(device.getGatewayGUID(),Types.VARCHAR);
        
        args[4] = new SQLParam("0",Types.VARCHAR);                      //i_device_guid      IN  DEVICE.device_guid%TYPE,
        if ( device.getDeviceGUID() != null && !device.getDeviceGUID().equalsIgnoreCase("") )
            args[4] = new SQLParam(device.getDeviceGUID(),Types.VARCHAR);
        if ( deviceGuid != null && !deviceGuid.equalsIgnoreCase(""))
            args[4] = new SQLParam(deviceGuid,Types.VARCHAR);
        
        args[5] = srcDeviceGuid != null ? new SQLParam(srcDeviceGuid,Types.VARCHAR) : new SQLParam(null,Types.NULL); //i_src_dguid        in  ACTION_QUEUE.SRC_DEVICE_GUID%TYPE,
        args[6] = accessKey != null ? new SQLParam(accessKey,Types.VARCHAR) : new SQLParam(null,Types.NULL); //i_accessKey_id     in  ACTION_QUEUE.ACCESSKEY_ID%type,
        args[7] = new SQLParam(eventId,Types.NUMERIC);  //i_event_id         IN  EVENTLOG.event_id%TYPE,
        args[8] = eGroupId != null ? new SQLParam(eGroupId,Types.VARCHAR) : new SQLParam(null,Types.NULL);
        args[9] = new SQLParam(message,Types.VARCHAR);  //i_label            IN  ACTION_QUEUE.label%TYPE,
        
        //i_source_label     IN  ACTION_QUEUE.source_label%TYPE,
        args[10] = new SQLParam(null,Types.NULL);
        
        StringBuilder bldr = new StringBuilder();
        if ( srcLabelMap.containsKey("BTN"))
        {
            bldr.append("BTN{").append(srcLabelMap.get("BTN")).append("}");
            bldr.append(SEPARATOR_CHAR);
            bldr.append("URL{").append(srcLabelMap.get("URL")).append("}");
        }
        if ( srcLabelMap.containsKey("CATID"))
        {
            bldr.append("CATID{").append(srcLabelMap.get("CATID")).append("}");
            bldr.append(SEPARATOR_CHAR);
            bldr.append("CUST{").append(srcLabelMap.get("CUST")).append("}");
        }
        if ( bldr.length() > 0 )
        {
            if ( logger.isDebugEnabled() ) logger.debug("[queuePushMessage()] - passing "+bldr+" as i_source_label");
            args[10] = new SQLParam(bldr.toString(),Types.VARCHAR);
        }
                
        args[11] = new SQLParam(locale, Types.VARCHAR);     //i_language_id      IN  ACTION_QUEUE.language_id%TYPE,
        args[12] = new SQLParam(timeZoneId,Types.VARCHAR);  //i_tzname           IN  ACTION_QUEUE.tzname%TYPE,
        args[13] = new SQLParam(new Integer(GlobalNames.PUSH_PROFILE_TYPE),Types.NUMERIC);  //i_profiletype_id   IN  NOTIFICATION_PROFILE.profiletype_id%TYPE,
        //i_profile_address  IN  NOTIFICATION_PROFILE.profile_address%TYPE,
        args[14] = new SQLParam(null,Types.NULL); 
        if ( profileAddress != null && !profileAddress.equalsIgnoreCase(""))
            args[14] = new SQLParam(profileAddress, Types.VARCHAR);
        else if ( device.getGatewayGUID() != null && !device.getGatewayGUID().equalsIgnoreCase("") && !device.getGatewayGUID().equals("0"))
            args[14] = new SQLParam(device.getGatewayGUID(),Types.VARCHAR);
        else
            args[14] = new SQLParam(new Long(device.getAccountId()).toString(),Types.VARCHAR);
        
        //if ( logger.isDebugEnabled() )
        //{
        //    for ( int i = 0; i < args.length; i++)
        //    {
        //        if ( args[i] != null ) 
        //            logger.debug("[queuePushMessage] - args["+i+"]="+args[i].toString());
         //   }
       // }
        
        dao.callSP(conn, "XC_ACCOUNT_PKG.queueAction", args,true);
    }
    
    enum CategoryStatusType
    {
        ENABLED("enabled"),DISABLED("disabled"),BETA_BUDDY("beta");
        private String type = "";
        CategoryStatusType(String typ)
        {
            type = typ;
        }
        public String getType(){return type;}
        public static CategoryStatusType getStatusType(String status)
        {
            if ( ENABLED.type.equals(status)) return ENABLED;
            if ( DISABLED.type.equals(status)) return DISABLED;
            if ( BETA_BUDDY.type.equals(status)) return BETA_BUDDY;
            return null;
        }
    }
}
