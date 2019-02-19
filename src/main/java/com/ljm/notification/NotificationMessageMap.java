/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ljm.notification;

import java.util.HashMap;

/**
 * Class to contain any specific logic for handling message information. 
 * 
 * @author Luther Maloney
 * @since October 2013
 */
public class NotificationMessageMap extends HashMap
{
    //generic parameters
    /** Define constants used as keys */
    /** The id of the access key used with the system**/
    public static final String ACCESSKEY = "ACCESSKEY";
    /** The Xanboo local account id **/
    public static final String ACCOUNTID = "ACCOUNTID";
    /** The "brand" name used in the template **/
    public static final String BRAND = "BRAND";
    /** Name of a button for the template**/
    public static final String BUTTON_NAME = "ACTION_BTN_NAME";
    /** The URL passed into the template **/
    public static final String BUTTON_LINK = "ACTION_URL";
    /** Id number of the contact**/
    public static final String CONTACT_ID = "CONTACTID";
    /** corresponds to the "_custom" xail attribute**/
    public static final String CUSTOM = "CUSTOM";
    /** the name of the device/gateway generating the notification **/
    public static final String DEVICE = "DEVICE";
    /** Used when the system sends a message for device creation / deletion**/
    public static final String DEVICE_ACTION_TYPE = "DEVICE_ACTION_TYP";
    /** The device GUID of the device, not the gateway **/
    public static final String DGUID = "DGUID";
    public static final String CATALOG_ID = "DGUID_CATALOG_ID";
    /** The domain id / name used in the template **/
    public static final String DOMAINID = "DOMAINID";
    /** An id used to group events together. For example front door
    * is broken in and cameras are triggered. **/
    public static final String EGROUP_ID = "EGROUP_ID";
    /** The text description of the event **/
    public static final String EVENT = "EVENT";
    /** the log id of the event **/
    public static final String EVENTLOGID = "EVENTLOGID"; 
    /** Email address for xanboo account for invitations **/
    public static final String FROM_INVITE_EMAIL = "FROM_EMAIL";
    /** The name of the gateway **/
    public static final String GATEWAY = "GATEWAY";
    /** the GGUID of the gateway causing the event **/
    public static final String GGUID = "GGUID";
    /** the ID number of the event. Defaults to "0" for generic notifications. 
    *  This can be used with DOMAIN_TEMPLATES.EVENT_ID to designate 
    *  a specific template
    */
    public static final String ID = "ID";
    /** Invitation id **/
    public static final String INVITATION_ID = "INVITEE_ID";
    /** invitation user message **/
    public static final String INVITATION_MSG = "USERMESSAGE";
    /** invitation user subject **/
    public static final String INVITATION_SUBJ = "USERSUBJECT";
    /** invitation url params **/
    public static final String INVITATION_URL_PARAMS = "URLPARAM";
    /** Template parameter for the username column on the profiletype_ref table **/
    public static final String PTYPE_USER = "PTYPE_USER";
    /** template parameter for the userpass column on the profiletype_ref table **/
    public static final String PTYPE_PASS = "PTYPE_PASS";
    /** the name of the rule causing the notification **/
    public static final String RULE_NAME = "RULE";
    public static final String RULE_ID = "RULE_ID";
    public static final String SOURCE = "SOURCE";
    /** the name of the source device for the notification rule (if not the gateway)**/
    public static final String SRCDEVICE = "SRCDEVICE";
    /** the GUID of the source device triggering the rule, if not the gateway **/
    public static final String SRCGUID = "SRCGUID";

    /** destination email address for invitation **/
    public static final String TO_INVITE_EMAIL = "TO_EMAIL";

    public static final String TOADDRESS = "TOADDRESS";

    /** Unique token for each TOADDRESS **/
    public static final String OPTINOUT_TOKEN = "OPTINOUT_TOKEN";

    /** Will contain the notification OPT-IN or OPT-OUT link **/
    public static final String OPTINOUT_URL = "OPTINOUT_URL";
    
    /**Timestamp of the event **/
    public static final String TIMESTAMP = "TIMESTAMP";
    public static final String USERINFO = "USERINFO";
    public static final String USERMESSAGE1 = "USERMESSAGE1";
    public static final String USERNAME = "USERNAME";
    public static final String VIEW_KEY = "VIEW_KEY";

    //parameters from the NotificationMeassage
    /** alaram code **/
    public static final String ALARM_CODE = "ALARMCODE";
    /** external account id **/
    public static final String EXTERNAL_ACCT_ID = "EXT_ACCOUNTID";
    /** subscriber id **/
    public static final String SUBSCRIBER_ID = "SUBID";
    /** subscriber name **/
    public static final String SUBSCRIBER_NAME = "SUBNAME";
    
    public static final String MESSAGE_KEY="MESSAGEKEY";
    public static final String QUEUEID = "QUEUEID";
    
    public static final String SRC_GATEWAY_NAME = "SRCGATEWAY";
    public static final String SRC_DEVICE_NAME = "SRCDEVICE";
    
    public static final String DOORBELL_CAMERA_URL="DOORBELL_URLS";
    
    public static final String NOTIF_TYPE="NOTIFICATION_TYPE";
    
    public static final String OBJECT_VALUE = "OBJECT_VALUE";
    
    /**
    * Constructor
    */
    public NotificationMessageMap()
    {
        super();
        put("ACCESSKEY","");
        put("ACCOUNTID","");
        put("BRAND","");
        put("DEVICE","");
        put("DGUID","");
        put("DOMAINID","");
        put("EGROUP_ID","");
        put("EVENT","");
        put("EVENTLOGID","");
        put("GATEWAY","");
        put("GGUID","");
        put("ID","0");
        put("PTYPE_USER","");
        put("PTYPE_PASS","");
        put("RULE","");
        put("SOURCE","");
        put("SRCDEVICE","");
        put("SRCGUID","");
        put("TOADDRESS","");
        put("OPTINOUT_TOKEN", "");
        put("OPTINOUT_URL", "");
        put("TIMESTAMP","");
        put("USERINFO","");
        put("USERMESSAGE1","");
        put("USERNAME","");
        put("URLPARAM","");
        put("ALARMCD","");
        put("EXT_ACCOUNTID","");
        put("SUBID","");
        put("SUBNAME","");

        put("INVITEE_ID","");
        put("USERMESSAGE","");
        put("USERSUBJECT","");
        put("VIEW_KEY","");
        put("TO_EMAIL","");
        put("FROM_EMAIL","");
        
        put("MESSAGEKEY","");
        put("QUEUEID","");
        super.put("NOTIFICATION_TYPE",NotificationMessageInterface.MessageType.EVENT_MESSAGE.type);
        put("OBJECT_VALUE","");
        put("DGUID_CATALOG_ID","000000000000000");
    }
        /**
        * Constructor
        * Initialize all of the properties in the map to a default value of an empty string ("").
        * @param initialCapacity
        * @param loadFactor 
        */
    public NotificationMessageMap(int initialCapacity, float loadFactor)
    {
        super(initialCapacity,loadFactor);
        put("ACCESSKEY","");
        put("ACCOUNTID","");
        put("BRAND","");
        put("DEVICE","");
        put("DGUID","");
        put("DOMAINID","");
        put("EGROUP_ID","");
        put("EVENT","");
        put("EVENTLOGID","");
        put("GATEWAY","");
        put("GGUID","");
        put("ID",0);
        put("PTYPE_USER","");
        put("PTYPE_PASS","");
        put("RULE_NAME","");
        put("SOURCE","");
        put("SRCDEVICE","");
        put("SRCGUID","");
        put("TOADDRESS","");
        put("OPTINOUT_TOKEN", "");
        put("OPTINOUT_URL", "");
        put("TIMESTAMP","");
        put("USERINFO","");
        put("USERMESSAGE1","");
        put("USERNAME","");
        put("ALARMCD","");
        put("EXT_ACCOUNTID","");
        put("SUBSCRIBER_ID","");
        put("SUBSCRIBER_NAME","");
        super.put("NOTIFICATION_TYPE",NotificationMessageInterface.MessageType.EVENT_MESSAGE.type);
        put("OBJECT_VALUE","");
        put("DGUID_CATALOG_ID","000000000000000");
    }
    public void setDeviceAction(DeviceActionType actionType)
    {
        super.put("NOTIFICATION_TYPE",NotificationMessageInterface.MessageType.DEVICE_ACTION);
        put("DEVICE_ACTION_TYP",actionType);
    }
    //common accessed properties
    public String getDomainId()
    {
        return (String)get(DOMAINID);
    }
    public Integer getEventId()
    {
        return new Integer(get(ID).toString());
    }
    public String getGatewayUID()
    {
        return (String)get(GGUID);
    }
    public String getCameraUrlsJson()
    {
        return (String)get(DOORBELL_CAMERA_URL);
    }
    public void setCameraUrlsJson(String urlJson)
    {
        put(this.DOORBELL_CAMERA_URL,urlJson);
    }
    
    public Object put(String key, Object value)
    {
        if ( key.equals("NOTIFICATION_TYPE")) throw new IllegalArgumentException("Setting NOTIF_TYPE not allowed. Call either setObjectId() or setDeviceAction()");
        
        if ( key.equalsIgnoreCase("ID"))
        {
            Integer eid = 0;
            if ( value instanceof Integer )
                eid = (Integer)value;
            else
            {
                eid = new Integer(value.toString());
            }
            
            if ( eid > 10000 ) 
                throw new IllegalArgumentException("Invalid value for eventId (ID), eventId must be less than 10,000. To specify and oid, call setObjectId()");
            super.put("NOTIFICATION_TYPE",NotificationMessageInterface.MessageType.EVENT_MESSAGE.type);
        }
        
        return super.put(key,value);
    }
    
    public void setObjectId(Integer oid,String value)
    {
        super.put("NOTIFICATION_TYPE",NotificationMessageInterface.MessageType.OID_MESSAGE.type);
        super.put(this.ID,oid+10000+"");
        if ( value != null ) put(this.OBJECT_VALUE,value);
    }
    
    public enum DeviceActionType
    {
        CREATE_DEVICE("create"),DELETE_DEVICE("delete");
        String type = "";
        DeviceActionType(String type){this.type=type;}
        public String getType(){return this.type;}
    }
}
