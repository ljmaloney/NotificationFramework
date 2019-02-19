package com.ljm.notification;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
/**
 * Default implementation of <code>NotificationMessageInterface</code>.
 * 
 * @author Luther J Maloney
 * @since October 2013
 */
public class NotificationMessage implements NotificationMessageInterface 
{
    /**account id**/
    private Integer accountId = null;
    /** the alarm code, if present **/
    private String alarmCode = null;
    /* List of file names of any attachments**/
    private List<String> attachments = new ArrayList<String>();
    private String contentType = null;
    /**External account id**/
    private String externalAccountId = null;
    /** List of destination for the notification **/
    private List<NotificationDestination> toAddressList = new ArrayList<NotificationDestination>();
    private List<NotificationDestination> ccAddressList = new ArrayList<NotificationDestination>();
    private List<NotificationDestination> bccAddressList = new ArrayList<NotificationDestination>();
    /** the "From" address for email notifications **/
    private String fromAddress = null;
    /**the text / template of the notification message**/
    private String message = "";
    /**Message properties **/
    private NotificationMessageMap messageProperties = new NotificationMessageMap();
    private NotificationCustomMap customMap = null;
    private ResponseMap responseMap = null;
    private boolean overrideSubjectTmpl;
    /**Subject for the notification **/
    private String subject = "";
    /**the ID number of the subscriber **/
    private String subscriberId = null;
    /**The name of the xanboo subscriber**/
    private String subscriberName = null;
    /**Template type for the message **/
    private Integer templateTypeId = null;
    /**Timestamp notification was generated**/
    private String messageTimestamp = null;	//default value
    /** instance variable for the notification language **/
    private String language = "en";
    
    private Boolean silent = false;
    /**
    * Constructor
    */
    public NotificationMessage()
    {
        Date currDate = new Date();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        messageProperties.put(NotificationMessageMap.TIMESTAMP, df.format(currDate));
        overrideSubjectTmpl = false;
    }
    /**
    * Constructor.
    * @param subject
    * @param message
    * @param fromAddress
    * @param toAddresses
    * @param messageMap 
    */
    public NotificationMessage(String subject,String message,String fromAddress,String[] toAddresses,NotificationMessageMap messageMap)
    {
        this();
        this.subject = subject;
        this.message = message;
        this.fromAddress = fromAddress;
        this.messageProperties = messageMap;
        if ( toAddresses != null )
        {
            for ( String addr : toAddresses )
                toAddressList.add(new NotificationDestination(addr));
        }
    }
    /**
    * Constructor
    * @param subject
    * @param message
    * @param fromAddr
    * @param toAddress
    * @param messageMap 
    */
    public NotificationMessage(String subject,String message,String fromAddr,String toAddress,NotificationMessageMap messageMap)
    {
        this();
        this.subject = subject;
        this.message = message;
        this.fromAddress = fromAddr;
        this.addToAddress(toAddress);
        this.messageProperties = messageMap;
    }
    /**
    * Returns the local (xanboo) account id
    * @return 
    */
    @Override
    public Integer getAccountId()
    {
        return this.accountId;
    }
    public void setAccountId(Integer id)
    {
        this.accountId = id;
        getMessageProperties().put(NotificationMessageMap.ACCOUNTID, id);
    }
    /**
    * The alarm code (from xail command)
    * @return 
    */
    @Override
    public String getAlarmCode()
    {
        return this.alarmCode;
    }
    public void setAlarmCode(String code)
    {
        this.alarmCode = code;
        getMessageProperties().put(NotificationMessageMap.ALARM_CODE, code);
        putCustomProperty(NotificationCustomMap.ALARM_CODE,code,NotificationCustomMap.DataType.STRING);
    }

    public void addAttachment(String fileName)
    {
        this.attachments.add(fileName);
    }
    /**
    * The list of attachements (file names)
    * @return 
    */
    @Override
    public List<String> getAttachments()
    {
        return attachments;
    } 
    /**
    * Adds an address to the BCC (Blind Carbon Copy) list
    * @param emailAddress 
    */
    public void addBCCAddress(String emailAddress) 
    {
        this.bccAddressList.add(new NotificationDestination(emailAddress)); 
    }
    /**
    * The list of destinations for BCC
    * @return 
    */
    @Override
    public List<NotificationDestination> getBCCAddrList()
    {
        return this.bccAddressList;
    }
    public void addCCAddress(String emailAddress)
    {
        this.ccAddressList.add(new NotificationDestination(emailAddress)); 
    }
    @Override
    public List<NotificationDestination> getCCAddrList()
    {
        return this.ccAddressList;
    }
    @Override
    public String getContentType()
    {
        return this.contentType;
    }
    public NotificationCustomMap getCustomMap()
    {
        if ( customMap == null ) customMap = new NotificationCustomMap();
        return customMap;
    }
    public Object getCustomProperty(String key)
    {
        if ( this.customMap == null || !customMap.containsKey(key) )
            return null;
        return customMap.get(key);
    }
    public Object putCustomProperty(String key, Object value, NotificationCustomMap.DataType type)
    {
        if ( customMap == null ) customMap = new NotificationCustomMap();
        return customMap.put(key, value, type);
    }
    public String getCustomMapJSON()
    {
        if ( customMap == null ) return null;
        return customMap.toJSON();
    }
    /**
    * The external system account id
    * @return 
    */
    @Override
    public String getExternalAccountId()
    {
        return this.externalAccountId;
    }
    public void setExternalAccountId(String id)
    {
        this.externalAccountId = id;
        getMessageProperties().put(NotificationMessageMap.EXTERNAL_ACCT_ID, id);
    }
    /** 
    * The email address to be used in the "from" field when sending notificaiton via email. 
    * @return 
    */
    @Override
    public String getFromAddress()
    {
    return this.fromAddress;
    }
    public void setFromAddress(String addr)
    {
        this.fromAddress = addr;
    }
    public String getLanguage()
    {
        return this.language;
    }
    public void setLanguage(String language)
    {
        this.language = language;
    }
    /**
    * The notification message
    * @return 
    */
    @Override
    public String getMessage()
    {
        return message;
    }
    @Override
    public void setMessage(String message)
    {
        setMessage(message,"text/plain");
    }
    /**
    * 
    * @param message
    * @param contentType 
    */
    @Override
    public void setMessage(String message,String contentType)
    {
        this.message = message;
        this.contentType = contentType;
    }

    @Override
    public NotificationMessageMap getMessageProperties()
    {
        return this.messageProperties;
    }
    @Override
    public NotificationMessageInterface.MessageType getMessageType()
    {
    	if(this.messageProperties.get(NotificationMessageMap.NOTIF_TYPE) instanceof String) {
    		String type =(String)this.messageProperties.get(NotificationMessageMap.NOTIF_TYPE);
            if ( NotificationMessageInterface.MessageType.EVENT_MESSAGE.type.equals(type)) return NotificationMessageInterface.MessageType.EVENT_MESSAGE;
            if ( NotificationMessageInterface.MessageType.OID_MESSAGE.type.equals(type)) return NotificationMessageInterface.MessageType.OID_MESSAGE;
            if ( NotificationMessageInterface.MessageType.DEVICE_ACTION.type.equals(type)) return NotificationMessageInterface.MessageType.DEVICE_ACTION;
    	} else if (this.messageProperties.get(NotificationMessageMap.NOTIF_TYPE) instanceof NotificationMessageInterface.MessageType){
    		return (NotificationMessageInterface.MessageType)this.messageProperties.get(NotificationMessageMap.NOTIF_TYPE);
    	}
        return null;    //should never return null
    }
    public void setMessageType(NotificationMessageInterface.MessageType messageType)
    {
        this.messageProperties.put(NotificationMessageMap.NOTIF_TYPE, messageType.type);
    }
    @Override
    public String getObjectValue()
    {
        return (String)this.messageProperties.get(NotificationMessageMap.OBJECT_VALUE);
    }
    public void setObjectValue(Integer oid,String val)
    {
        this.messageProperties.setObjectId(oid, val);
    }
    /**
    * Returns true if the value returned by <code>getSubject()</code> is used to
    * override the SUBJECT_TMPL column on the DOMAIN_TEMPLATES	table. If <code>getSubject()</code>
    * returns either NULL or an empty string, the SUBJECT_TMPL column is used.
    * @return 
    */
    @Override
    public boolean overrideSubjectTemplate()
    {
        return overrideSubjectTmpl;
    }
    public void setOverrideSubjectTemplate(boolean override)
    {
        this.overrideSubjectTmpl = override;
    }
    public ResponseMap getResponseMap()
    {
        if ( responseMap == null ) responseMap = new ResponseMap();
        return responseMap;
    }
    public Boolean getSilentNotification()
    {
        return this.silent;
    }
    public void setSilentNotification(Boolean silent)
    {
        this.silent = silent;
    }
    /**
    * Returns the list of "to" destinations sorted by profile type. 
    * @return 
    */
    @Override
    public List<NotificationDestination> getSortedToDestinations()
    {
        List<NotificationDestination> sortedList = new ArrayList<NotificationDestination>(toAddressList);
        Collections.sort(sortedList,new Comparator<NotificationDestination>()
        {
            @Override
            public int compare(NotificationDestination o1, NotificationDestination o2)
            {
                //if the profile type is not specified, compare using the suffix
                if ( o1.getProfileTypeId().equals(NotificationDestination.PROFILE_NOT_SPECIFIED) 
                     && o2.getProfileTypeId().equals(NotificationDestination.PROFILE_NOT_SPECIFIED))
                {
                    return o1.getSuffix().compareTo(o2.getSuffix());
                }
                //compare the profile type id
                return o1.getProfileTypeId().compareTo(o2.getProfileTypeId());
            }

        });
        return sortedList;
    }
    @Override
    public String getSubject()
    {
        return subject;
    }
    @Override
    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    @Override
    public String getSubscriberId()
    {
        return this.subscriberId;
    }
    public void setSubscriberId(String id)
    {
        this.subscriberId = id;
        getMessageProperties().put(NotificationMessageMap.SUBSCRIBER_ID, id);
    }
    @Override
    public String getSubscriberName()
    {
        return this.subscriberName;
    }
    public void setSubscriberName(String name)
    {
        this.subscriberName = name;
        getMessageProperties().put(NotificationMessageMap.SUBSCRIBER_NAME, name);
    }
    /**
    * The template type id. Template type "0" is used for notifications from DLC
    * @return 
    */
    @Override
    public Integer getTemplateTypeId()
    {
        return this.templateTypeId;
    }
    public void setTemplateTypeId(Integer id)
    {
        this.templateTypeId = id;
    }
    /**
    * The timestamp the notification was generated / sent
    * @return 
    */
    @Override
    public String getTimestamp()
    {
        //return this.messageTimestamp;
        return (String)getMessageProperties().get(NotificationMessageMap.TIMESTAMP);
    }
    public void setTimestamp(Date currDate)
    {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        setTimestamp(df.format(currDate));
    }
    public void setTimestamp(String timestamp)
    {
        messageTimestamp = timestamp;
        getMessageProperties().put(NotificationMessageMap.TIMESTAMP,timestamp);
    }

    /**
    * the "to" addresses
    * @param emailAddress 
    */
    public void addToAddress(String emailAddress)
    {
        addToAddress(new NotificationDestination(emailAddress)); 
    }
    public void addToAddress(NotificationDestination destination)
    {
        this.toAddressList.add(destination);
    }
    @Override
    public List<NotificationDestination> getToAddrList()
    {
        return this.toAddressList;
    }
    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        str.append("NotificationMessage [ accountId=");
        str.append(accountId);
        str.append(",alarmCode=");
        str.append(alarmCode);
        str.append(", attachments=");
        str.append(attachments.size());
        str.append(",contentType=");
        str.append(contentType);
        str.append(",externalAccountId=");
        str.append(externalAccountId);
        str.append(",fromAddress=");
        str.append(fromAddress);
        str.append(",subscriberId=");
        str.append(subscriberId);
        str.append(",subscriberName=");
        str.append(subscriberName);
        str.append(",templateTypeId=");
        str.append(templateTypeId);
        str.append(",messageTimestamp=");
        str.append(messageTimestamp);
        str.append("]");
        str.append("\r\n");
        str.append("[MessageProperties = ");
        str.append(messageProperties);
        str.append(" ]");
        return str.toString();
    }
    
    public enum MessageType
    {
        EVENT_MESSAGE("event"),
        OID_MESSAGE("oid"),
        DEVICE_ACTION("device");
        
        String type = "";
        MessageType(String type){this.type=type;}
    }
}