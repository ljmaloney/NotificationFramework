/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.sdk.account.alert;

import java.util.Date;
import java.util.HashMap;

/**
 * Class containing information for an account level alert. 
 * <br/>
 * <br/>Valid values for alertBehavior are: 
 * <ul>
 *  <li><code>AlertBehaviorType.CLEAR_ON_VIEW</code>
 *  <li><code>AlertBehaviorType.HOLD_ON_VIEW</code>
 * </ul>
 * Valid values for alertEvent are:
 * <ul>
 *  <li><code>EventType.NEW_ALERT</code> - creates a new alert / identifies the alert as a new alert
 *  <li><code>EventType.CLEAR_ALERT</code> - identifies the alert as a cleared alert
 * </ul>
 * Valid values for messageType are:
 * <ul>
 *  <li><code>MessageTypeType.PUSH</code> - instructs the system to send the alert via push notification. The alert is not persisted for later retrieval
 *  <li><code>MessageTypeType.ALERT</code> - instructs the system to persist the alert for later retrieval, no push notification is sent
 *  <li><code>MessageTypeType.PUSH_AND_ALERT</code> - instructs the system to both send a push notification and persist the alert for later retrieval
 * </ul>
 * @author Luther J Maloney / lm899p
 * @since March 2015
 */
public class XanbooAlert implements java.io.Serializable
{
    /** Digital Life internal account id **/
    private Long accountId = -1l;
    private Integer pushPreferences = 0;
    /** uniquely identifies the alert **/
    private Long alertId = -1l;
    /** domain id */
    private String domainId = "";
    /** external subscriber id **/
    private String subsId = null;
    /** integer status code **/
    private Integer statusInd = null;
    /** the source system of the alert**/
    private String alertSource = null;
    /** uniquely identifies the type of alert, used when clearing the alert**/
    private String alertCode = null;
    /** the text of the alert **/
    private String alertText = null;
    /** the date the alert was created **/
    private Date createDate = null;
    /** the date the alert expires **/
    private Date expirationDate = null;
    /** language / locale **/
    private String languageId = "en";   //default is "en"
    /** any additional info for the alert, name value pairs **/
    private HashMap<String,String> alertInfoMap = new HashMap<String,String>();
    /** specifies the type of alert**/
    private EventType alertEvent = EventType.NEW_ALERT;
    private Integer eventId = null;
    /**how the alert is to be handled by the ui **/
    private AlertBehaviorType alertBehavior = AlertBehaviorType.CLEAR_ON_VIEW;
    /** how the system processes the new alert **/
    private MessageTypeType messageType = null;
    /** alert push url **/
    private String alertPushUrl;
    /** alert push label **/
    private String alertPushLabel;
    /** populated when source system requests a response via CSI**/
    private String responseTemplateId = null;
    /**
     * Default, no arg constructor
     */
    public XanbooAlert()
    {
        
    }
    /**
     * Creates a new alert. By default, the alert is created with <code>EventType.NEW_ALERT</code>
     * @param alertSrc - The source system for the alert
     * @param alertCode - A unique code for the type of alert, used when the alert is cleared
     * @param expireDate - The expiration date of the alert, optional 
     * @param alertBehaviorStr - defines how the alert is handled when displayed to the user, valid values are
     * <ul>
     *  <li>clearOnView - Instructs the UI to clear the alert when the alert is viewed (<code>AlertBehaviorType.CLEAR_ON_VIEW</code>)
     *  <li>holdOnView - Instructs the UI to hold the alert until cleared by the source system (<code>AlertBehaviorType.HOLD_ON_VIEW</code>)
     *  <li>null - If this parameter is NULL, the system defaults to clearOnView
     * </ul>
     * @param msgTypeStr - The type of message, specifies how the new alert is handled, can be one of
     * <ul>
     *  <li>push - instructs the system to send the alert via push notification. The alert is not persisted for later retrieval
     *  <li>alert - instructs the system to persist the alert for later retrieval, no push notification is sent
     *  <li>pushAndAlert - instructs the system to both send a push notification and persist the alert for later retrieval
     * </ul>
     * @param alertText - The text of the alert
     * @param alertInfoMap - A <code>HashMap<String,String></code> containing any extra parameters for the alert
     */
    public XanbooAlert(String alertSrc,String alertCode,Date expireDate,String alertBehaviorStr,
                       String msgTypeStr,String alertText,HashMap<String,String> alertInfoMap)
    {
        this.alertSource = alertSrc;
        this.alertCode = alertCode;
        this.expirationDate = expireDate;
        this.alertText = alertText;
        this.alertInfoMap = alertInfoMap;
        this.alertEvent = EventType.NEW_ALERT;
        
        if ( alertBehaviorStr == null || alertBehaviorStr.equalsIgnoreCase("")) this.alertBehavior = AlertBehaviorType.CLEAR_ON_VIEW;
        else this.alertBehavior = AlertBehaviorType.getType(alertBehaviorStr);
        
        if ( alertBehavior == null )
            throw new NullPointerException("Invalid Parameter, invalid value for alert behavior");
        
        if ( msgTypeStr == null || msgTypeStr.equalsIgnoreCase(""))
            throw new NullPointerException("Invalid Parameter, message type is not provided");
        
        this.messageType = MessageTypeType.getType(msgTypeStr);
        if ( messageType == null )
            throw new NullPointerException("Invalid Parameter, invalid value for message type");
        
    }
    /**
     * Constructs an instance of <code>XanbooAlert</code>. This constructor is for use in the Digital Life SDK only. 
     * @param acctId - the local account id for digital life
     * @param alertId - the unique identifier for the alert
     * @param subsId - the subscriber number
     * @param src - the source system for the alert
     * @param code - A unique code for the type of alert, used when the alert is cleared 
     * @param txt - The alert text
     * @param createDate - the date the alert was created, auto generated by the database
     * @param expireDate - The expiration date of the alert
     * @param behavior - defines how the alert is handled when displayed to the user, valid values are 
     * <ul>
     *  <li><code>AlertBehaviorType.CLEAR_ON_VIEW</code>
     *  <li><code>AlertBehaviorType.HOLD_ON_VIEW</code>
     * </ul>
     * @param event - Identifies if the alert is cleared or not. Valid values are:
     * <ul>
     *  <li><code>EventType.NEW_ALERT</code> - creates a new alert / identifies the alert as a new alert
     *  <li><code>EventType.CLEAR_ALERT</code> - identifies the alert as a cleared alert
     * </ul>
     * @param msgTyp - the type of message, specifies how the new alert is handled, can be one of
     * <ul>
     *  <li><code>MessageTypeType.PUSH</code> - instructs the system to send the alert via push notification. The alert is not persisted for later retrieval
     *  <li><code>MessageTypeType.ALERT</code> - instructs the system to persist the alert for later retrieval, no push notification is sent
     *  <li><code>MessageTypeType.PUSH_AND_ALERT</code> - instructs the system to both send a push notification and persist the alert for later retrieval
     * </ul>
     * @param alertInfo -A <code>HashMap<String,String></code> containing any extra parameters for the alert
     */
    protected XanbooAlert(Long acctId,Long alertId,String subsId,String src,String code,
                       String txt,Date createDate,Date expireDate,AlertBehaviorType behavior,
                       EventType event,MessageTypeType msgTyp,HashMap<String,String> alertInfo)
    {
        this.accountId = acctId;
        this.alertId = alertId;
        this.subsId = subsId;
        this.alertSource = src;
        this.alertCode = code;
        this.alertText = txt;
        this.alertInfoMap = alertInfo;
        this.createDate = createDate;
        this.expirationDate = expireDate;
        this.alertBehavior = behavior;
        this.alertEvent = event;
        this.messageType = msgTyp;
    }
    /**
     * Gets the Digital Life internal account id
     * @return 
     */
    public Long getAccountId()
    {
        return this.accountId;
    }
    /**
     * Sets the digital life internal account id
     * @param acctId 
     */
    public void setAccountId(Long acctId)
    {
        this.accountId = acctId;
    }
    public AlertBehaviorType getAlertBehavior()
    {
        return this.alertBehavior;
    }
    public void setAlertBehavior(AlertBehaviorType type)
    {
        this.alertBehavior = type;
    }
    /**
     * Gets the unique type of alert
     * @return 
     */
    public String getAlertCode()
    {
        return this.alertCode;
    }
    /**
     * Sets the type of alert
     * @param code 
     */
    public void setAlertCode(String code)
    {
        this.alertCode = code;
    }
    /**
     * Gets the event associated with the alert, Can be one of 
     * <code>EventType.NEW_ALERT</code> or <code>EventType.CLEAR_ALERT</code>
     * @return 
     */
    public EventType getAlertEvent()
    {
        return this.alertEvent;
    }
    /**
     * Sets the alert type. can be one of 
     *  <code>EventType.NEW_ALERT</code> or <code>EventType.CLEAR_ALERT</code>
     * @param type 
     */
    protected void setAlertEvent(EventType type)
    {
        this.alertEvent = type;
    }
    /**
     * Gets the unique id number for the alert
     * @return 
     */
    public Long getAlertId()
    {
        return this.alertId;
    }
    /**
     * Sets the unique id number for the alert. Only populated by DL-Core
     * @param alertId 
     */
    protected void setAlertId(Long alertId)
    {
        this.alertId = alertId;
    }
    /**
     * Gets additional info associated with the alert (name = value pairs)
     * @return 
     */
    public HashMap<String,String> getAlertInfo()
    {
        return this.alertInfoMap;
    }
    /** 
     * Sets the additional info associated with the alert (name = value) pairs
     * @param alertInfo 
     */
    public void setAlertInfo(HashMap<String,String> alertInfo)
    {
        this.alertInfoMap = alertInfo;
    }
    /**
     * Returns a specific value from the additional info
     * @param key
     * @return 
     */
    public String getAlertInfoValue(String key)
    {
        return alertInfoMap.get(key);
    }
    /** 
     * Puts a specific value for the key into the additional alert parameters
     * @param key
     * @param val 
     */
    public void putAlertInfo(String key,String val)
    {
        alertInfoMap.put(key, val);
    }
    protected Integer getPushPrefs()
    {
        return this.pushPreferences;
    }
    protected void setPushPrefs(Integer pushPrefs)
    {
        this.pushPreferences = pushPrefs;
    }
    /**
     * Returns the source system of the alert
     * @return 
     */
    public String getAlertSource()
    {
        return this.alertSource;
    }
    /**
     * Sets the source system of the alert
     * @param source 
     */
    public void setAlertSource(String source)
    {
        this.alertSource = source;
    }
    /**
     * The text of the alert / notification. 
     * @return 
     */
    public String getAlertText()
    {
        return this.alertText;
    }
    /**
     * Sets the text of the alert / notification
     * @param txt 
     */
    public void setAlertText(String txt)
    {
        this.alertText = txt;
    }
    /** 
     * The date the alert was created. Autogenerated when the alert is created.
     * @return 
     */
    public Date getCreateDate()
    {
        return this.createDate;
    }
    /**
     * Sets the date the alert was created. Only populate by DL-Core
     * @param cd 
     */
    protected void setCreateDate(Date cd)
    {
        this.createDate = cd;
    }
    public String getDomainId()
    {
        return this.domainId;
    }
    public void setDomainId(String domain)
    {
        this.domainId = domain;
    }
    protected Integer getEventId()
    {
        return this.eventId;
    }
    protected void setEventId(Integer eventId)
    {
        this.eventId = eventId;
    }
    /**
     * The expiration date of the alert
     * @return 
     */
    public Date getExpirationDate()
    {
        return this.expirationDate;
    }
    /** 
     * Sets the expiration date of the alert
     * @param expDate 
     */
    public void setExpirationDate(Date expDate)
    {
        this.expirationDate = expDate;
    }
    
    public String getLanguageId()
    {
        return this.languageId;
    }
    
    public void setLanguageId(String locale)
    {
        this.languageId = locale;
    }
    
    public MessageTypeType getMessageType()
    {
        return this.messageType;
    }
    public void setMessageType(MessageTypeType type)
    {
        this.messageType = type;
    }
    public void setMessageType(String type)
    {
        this.messageType = MessageTypeType.getType(type);
        if ( messageType == null )
            throw new NullPointerException("Invalid Parameter, invalid value for message type");
    }
    public String getResponseTemplateId()
    {
        return this.responseTemplateId;
    }
    public void setResponseTemplateId(String id)
    {
        this.responseTemplateId = id;
    }
    public Integer getStatusInd()
    {
        return this.statusInd;
    }
    protected void setStatusInd(Integer ind)
    {
        this.statusInd  =ind;
    }
    /**
     * The external subscriber id number/string
     * @return 
     */
    public String getSubscriberId()
    {
        return this.subsId;
    }
    /**
     * Sets the external subscriber id number/String
     * @param subsId 
     */
    public void setSubscriberId(String subsId)
    {
        this.subsId = subsId;
    }
    /**
     * Defines the valid event types supported by this class. Valid event types are
     * <ul>
     *  <li>NEW_ALERT - event_id = 510 / Create new Alert
     *  <li>CLEAR_ALERT - event_id = 511 / Clear alert
     * <ul>
     */
    public enum EventType
    {   
        /* A new alert is mapped to event_id 510  */
        NEW_ALERT("Create new Alert",10),
        /* A clear alert is mapped to event_id 511 */
        CLEAR_ALERT("Clear Alert",11);
        private String eventText;
        private Integer eventId;
        EventType(String txt,Integer eventId){eventText=txt;this.eventId=eventId;}
        public String getEventText(){return this.eventText;}
        public Integer getEventId(){return this.eventId;}
        /**
         * Return the correct <code>EventType</code> for the specific eventId
         * @param eventId
         * @return 
         */
        public static EventType getEvent(Integer eventId)
        {
            if ( eventId.equals(NEW_ALERT.eventId)) return NEW_ALERT;
            if ( eventId.equals(CLEAR_ALERT.eventId))return CLEAR_ALERT;
            return null;
        }
    }
    /**
     * Defines the valid MessageTypes for alerts. valid values are:
     * <ul>
     *  <li>PUSH_ONLY - push / ACCOUNT_ALERT.MSG_TYPE=0 - send push notification only
     *  <li>ALERT_ONLY - alert / ACCOUNT_ALERT.MSG_TYPE=1 - persist alert for later retrieval only
     *  <li>PUSH_AND_ALERT - pushAndAlert / ACCOUNT_ALERT.MSG_TYPE=2 - both persist the alert and send push notification
     * <ul>
     */
    public enum MessageTypeType
    {
        /* push only messages*/
        PUSH_ONLY("push",0),
        /* alert only messages*/
        ALERT_ONLY("alert",1),
        /* push and alert messages*/
        PUSH_AND_ALERT("pushAndAlert",2);
        private String text = "";
        private Integer dbValue = null;
        MessageTypeType(String txt,Integer db){this.text=txt;this.dbValue=db;}
        public String getText(){return this.text;}
        public Integer getDbValue(){return this.dbValue;}
        /**
         * Return the correct MessageTypeType for the specified text value
         * @param text
         * @return 
         */
        public static MessageTypeType getType(String text)
        {
            if ( PUSH_ONLY.text.equals(text)) return PUSH_ONLY;
            if ( ALERT_ONLY.text.equals(text)) return ALERT_ONLY;
            if ( PUSH_AND_ALERT.text.equals(text)) return PUSH_AND_ALERT;
            return null;
        }
        /**
         * Return the correct MessageTypeType for the specified dbValue
         * @param dbValue
         * @return 
         */
        public static MessageTypeType getType(Integer dbValue)
        {
            if ( dbValue.equals(PUSH_ONLY.dbValue)) return PUSH_ONLY;
            if ( dbValue.equals(ALERT_ONLY.dbValue)) return ALERT_ONLY;
            if ( dbValue.equals(PUSH_AND_ALERT.dbValue))return PUSH_AND_ALERT;
            return null;
        }
            
    }
    /**
     * Defines the valid alert behavior types
     * <ul>
     *  <li>CLEAR_ON_VIEW - clearOnView/ACCOUNT_ALERT.ALERT_BEHAVIOR='C' - clear the alert when viewed by the user
     *  <li>HOLD_ON_VIEW - holdOnView/ACCOUNT_ALERT.ALERT_BEHAVIOR='H' -  hold the alert until notified by origin system
     * </ul>
     */
    public enum AlertBehaviorType 
    {
        /** indicates the alert should be cleared when the alert is viewed by the user**/
        CLEAR_ON_VIEW("clearOnView","C"),
        /** indicates the alert should be held until cleared by the source system**/
        HOLD_ON_VIEW("holdOnView","H");
        
        private String type = "";
        private String dbValue = "";
        
        AlertBehaviorType(String type,String dbVal){this.type = type;dbValue=dbVal;}
        
        public String type(){return this.type;}
        public String getDBValue(){return this.dbValue;}
        /**
         * Returns the correct enum value based on the type string. 
         * @param type - A string that represents a valid AlertBehavior
         * @return - An instance of the AlertBehaviorType corresponding to the string, or null if invalid
         */
        public static AlertBehaviorType getType(String type)
        {
            if ( CLEAR_ON_VIEW.type.equals(type)) return CLEAR_ON_VIEW;
            if ( HOLD_ON_VIEW.type.equals(type)) return HOLD_ON_VIEW;
            return null;
        }
        public static AlertBehaviorType getTypeDB(String dbValue)
        {
            if ( CLEAR_ON_VIEW.dbValue.equals(dbValue)) return CLEAR_ON_VIEW;
            if ( HOLD_ON_VIEW.dbValue.equals(dbValue)) return HOLD_ON_VIEW;
            return null;
        }
    };

    @Override
    public String toString()
    {
        StringBuilder bldr = new StringBuilder();
        bldr.append("XanbooAlert{accountId=").append(accountId);
        bldr.append(", alertId=").append(alertId);
        bldr.append(", subsId=").append(subsId);
        bldr.append(", statusInd=").append(statusInd);
        bldr.append(", alertSource=").append(alertSource);
        bldr.append(", alertCode=").append(alertCode);
        bldr.append(", alertText=").append(alertText);
        bldr.append(", createDate=").append(createDate);
        bldr.append(", expirationDate=").append(expirationDate);
        bldr.append(", alertInfoMap=").append(alertInfoMap);
        bldr.append(", alertEvent=").append(alertEvent);
        bldr.append(", alertBehavior=").append(alertBehavior);
        bldr.append(", alertPushLabel=").append(alertPushLabel);
        bldr.append(", alertPushUrl=").append(alertPushUrl);
        bldr.append(", messageType=").append(messageType);
        bldr.append(", responseTemplateId=").append(responseTemplateId).append('}');
        return bldr.toString();
    }
	/**
	 * @return the alertPushUrl
	 */
	public String getAlertPushUrl() {
		return alertPushUrl;
	}
	/**
	 * @param alertPushUrl the alertPushUrl to set
	 */
	public void setAlertPushUrl(String alertPushUrl) {
		this.alertPushUrl = alertPushUrl;
	}
	/**
	 * @return the alertPushLabel
	 */
	public String getAlertPushLabel() {
		return alertPushLabel;
	}
	/**
	 * @param alertPushLabel the alertPushLabel to set
	 */
	public void setAlertPushLabel(String alertPushLabel) {
		this.alertPushLabel = alertPushLabel;
	}
}
