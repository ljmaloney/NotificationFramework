package com.ljm.notification;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
/**
 * Interface to specify the information required for sending a notification. Any fields not 
 * directly specified in the interface should be accessable via the <code>getMessageProperties</code>
 * method. 
 * @author Luther J Maloney
 * @since October 2013
 */
public interface NotificationMessageInterface extends java.io.Serializable
{
    /**
    * The Xanboo account id
    * @return 
    */
    public Integer getAccountId();
    /**
    * The Alarm code from the XAIL command (if present)
    * @return 
    */
    public String getAlarmCode();
    /**
    * Attachment file names
    * @return 
    */
    public List<String> getAttachments();
    /**
    * The list of BCC notification destinations. This would be email specific, 
    * as SMS does not have this concept
    * @return 
    */
    public List<NotificationDestination> getBCCAddrList();
    /**
    * The list of CC notification destinations. Email specific
    * @return 
    */
    public List<NotificationDestination> getCCAddrList();
    /**
    * The content type of the message. Used when sending emails, defaults to "text/plain"
    * @return 
    */
    public String getContentType();
    public Object getCustomProperty(String key);
    public String getCustomMapJSON();
    /**
     * Sets the value of a custom property
     * @param key
     * @param value
     * @param type
     * @return 
     */
    public Object putCustomProperty(String key, Object value, NotificationCustomMap.DataType type);
    /**
    * The account id of the external system(s). 
    * @return 
    */
    public String getExternalAccountId();
    /**
    * The address used as the "from" field when sending email notifications.
    * @return 
    */
    public String getFromAddress();
    /**
    * The language used for the notification
    * @return 
    */
    public String getLanguage();
    /**
    * The text of the message. Can be populated from XAIL or from template processing
    * @return 
    */
    public String getMessage();
    /**
    * 
    * @param message
    * @param contentType 
    */
    public void setMessage(String message,String contentType);
    /**
    * Sets the message to be sent
    * @param msgStr 
    */
    public void setMessage(String msgStr);
    /**
    * Returns the message parameters / properties not specified as methods in this interface.
    * @return 
    */
    public NotificationMessageMap getMessageProperties();
    /**
     * Returns the type of notification message represented by the interface. 
     * @return 
     */
    public MessageType getMessageType();
    /**
    * Returns true if the value returned by <code>getSubject()</code> is used to
    * override the SUBJECT_TMPL column on the DOMAIN_TEMPLATES	table. If <code>getSubject()</code>
    * returns either NULL or an empty string, the SUBJECT_TMPL column is used.
    * @return 
    */
    public boolean overrideSubjectTemplate();
    /**
     * Returns the new value for an oid change
     * @return 
     */
    public String getObjectValue();
    /**
     * 
     * @return 
     */
    public ResponseMap getResponseMap();
    /**
     * Returns true if the notification is "silent", false otherwise
     * @return 
     */
    public Boolean getSilentNotification();    
    /**
    * Returns the list of destinations sorted. The default implementation sorts based on profile type.
    * @return 
    */
    public List<NotificationDestination> getSortedToDestinations();
    /**
    * The subject (for emails)
    * @return 
    */
    public String getSubject();
    /**
    * Sets the subject
    * @param subjStr 
    */
    public void setSubject(String subjStr);
    /**
    * The subscriber id
    * @return 
    */
    public String getSubscriberId();
    /**
    * The name of the subscriber
    * @return 
    */
    public String getSubscriberName();
    /**
    * The template type Id. For XAIL generated notifications, this will be "0".
    * @return 
    */
    public Integer getTemplateTypeId();
    /**
    * Timestamp of the event generating the notification.
    * @return 
    */
    public String getTimestamp();
    /**
    * List of notification destinations for the message. Should always be present.
    * @return 
    */
    public List<NotificationDestination> getToAddrList();
    
    public enum MessageType
    {
        EVENT_MESSAGE("event"),
        OID_MESSAGE("oid"),
        DEVICE_ACTION("device");
        
        String type = "";
        MessageType(String type){this.type=type;}
        public String getType(){return this.type;}
    }
}