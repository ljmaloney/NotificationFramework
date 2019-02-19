/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.notification;


import com.xanboo.core.util.Logger;
import java.util.HashMap;
import java.util.List;
import com.xanboo.core.util.LoggerFactory;
/**
 * Abstract class to provide default implementations to commonly used methods. 
 * 
 * @author Luther J Maloney
 * @since October 2013
 */
public abstract class AbstractNotificationProvider implements XanbooNotificationProviderInterface
{
    /**variable for the configuration parameters **/
    private HashMap configMap = null;
    /** the profile type id **/
    private Integer profileTypeId = null;
    /**the profile suffix **/
    private String profileSuffix = null;

    /**
    * Initializes the instance of the notification provider. 
    * @param configMap 
    */
    @Override
    public void initialize(HashMap configMap) 
    {
        this.setConfigMap(configMap);
        this.profileSuffix = (String)configMap.get(PROFILE_SUFFIX);
        this.profileTypeId = new Integer(configMap.get(PROFILE_ID).toString());
        LoggerFactory.getLogger(getClass().getName()).info("[initialize()] Initializing notification provider [profileTypeId="+profileTypeId+"]");
        //System.out.println("Initialize called - AbstractNotificationProvider profileSuffix="+profileSuffix+" profileTypeId="+profileTypeId);
    }
    /**
    * SMS notifications can only accept text messages. If the contentType is anything other 
    * than null or "text/plain", this method will return false. 
    * @param contentType
    * @return 
    */
    @Override
    public boolean canAccept(String contentType)
    {
        if ( contentType == null || contentType.equals("text/plain"))
            return true;
        return false;
    }
    /**
    * By default, all providers can accept multiple destinations
    * @return 
    */
    @Override
    public boolean canAcceptDestinationList()
    {
        return false;
    }
    /**
    * Returns the config parameter map for the provider
    * @return 
    */
    @Override
    public HashMap getConfigMap() 
    {
        return this.configMap;
    }
    /**
    * Sets the config parameters for the provider
    * @param map 
    */
    @Override
    public void setConfigMap(HashMap map) 
    {
        this.configMap = map;
    }
    /**
    * Default implementation to encode the message string. 
    * @param message
    * @return 
    */
    @Override
    public String encodeMessage(String message)
    {
        return message;	//default
    }

    @Override
    public boolean encodeParametersBeforeTemplate()
    {
        return false;
    }

    @Override
    public void logNotificationError(NotificationDestination destination,NotificationMessageInterface message,String errorStr,Throwable stackTrace)
    {
        Logger log = LoggerFactory.getLogger(getClass().getName());
        if ( message != null)
            log.warn("[AbstractNotificationProvider.logNotificationError()] Message - "+message);
        if ( destination != null)
            log.warn("[AbstractNotificationProvider.logNotificationError()] Destination - "+destination);
        if ( log.isDebugEnabled() )
           log.warn("[AbstractNotificationProvider.logNotificationError()] "+errorStr, stackTrace);
        else
            log.warn("[AbstractNotificationProvider.logNotificationError()] "+errorStr);
    }
    /**
    * Returns the profile suffix
    * @return 
    */
    @Override
    public String getProfileSuffix()
    {
        return this.profileSuffix;
    }
    /**
    * Sets the profile suffix for the provider
    * @param suffix 
    */
    public void setProfileSuffix(String suffix)
    {
        this.profileSuffix = suffix;
    }
    /**
    * Returns the profile type id
    * @return 
    */
    @Override
    public Integer getProfileTypeId() 
    {
        return profileTypeId;
    }
    /**
    * Sets the profile type id
    * @param typeId 
    */
    public void setProfileTypeId(Integer typeId)
    {
        this.profileTypeId = typeId;
    }

    /**
    * Default implementation for sending to a list. Most notification providers (SMS) can only sent to one destination 
    * per call. 
    * @param destinations
    * @param message 
    */
    @Override
    public void sendMessage(List<NotificationDestination> destinations,NotificationMessageInterface message)throws XanbooNotificationException
    {
        for ( NotificationDestination destination : destinations )
            sendMessage(destination,message);
    }

    /**
    * Default validation routine. <br/>
    * If the MAX_LEN of the PROFILETYPE_REF record is either null or -1 or 0, the length is not checked.<br/>
    * Otherwise the length is checked and an exception thrown if message is greater than the max length.
    * @param message 
    */
    @Override
    public void validate(NotificationMessageInterface message)
    {
        //if MAX_LEN is defined, check the message length
        String maxLenStr = configMap.get(PROFILE_MAXLEN).toString();
        if ( maxLenStr != null && !maxLenStr.equalsIgnoreCase(""))
        {
            Integer maxLen = new Integer(configMap.get(PROFILE_MAXLEN).toString());
            if (  !maxLen.equals(-1) && !maxLen.equals(0))
            {
                //a valid maxLen field 
                int maxLength = maxLen.intValue();
                if ( message.getMessage().length() > maxLength )
                {
                    LoggerFactory.getLogger(getClass().getName()).warn("[AbstractNotificationProvider.validate()] - Message length of "+message.getMessage().length()+" is more than the max length "+maxLen);
                    throw new RuntimeException("Message length of "+message.getMessage().length()+" is more than the max length "+maxLen);
                }
            }
        }
        else 
        {
            if ( LoggerFactory.getLogger(getClass().getName()).isDebugEnabled())
                LoggerFactory.getLogger(getClass().getName()).debug("[validate()] maxlength not configured for provider");
        }
    }
    /**
    * Default implementation to validate the list of destinations. The default implementation always returns true.
    * @param destinations
    * @return 
    */
    @Override
    public boolean validateDestinations(List<NotificationDestination> destinations)
    {
        return true;
    }

    @Override
    public boolean equals(Object provider2)
    {
        if ( !(provider2 instanceof XanbooNotificationProviderInterface) )
            return false;

        XanbooNotificationProviderInterface pi = (XanbooNotificationProviderInterface)provider2;
        if ( pi.getProfileTypeId() == null && this.profileTypeId == null )
            return pi.getProfileSuffix().equals(this.profileSuffix);
        else if ( pi.getProfileTypeId() != null && this.profileTypeId != null )
            return (pi.getProfileTypeId().intValue() == this.profileTypeId.intValue());
        return false;
    }
}
