package com.xanboo.core.notification;

import java.util.HashMap;
import java.util.List;
/**
 * Interface to define methods used to send a notification. The concrete implementation
 * contains all the details to connect and send the notification via a provider. 
 * The provider can be email, internal systems (CSI), or external/3rd party systems. This
 * interface creates an abstraction where the XAIL processing code does not "know" about the 
 * details for sending the notification.
 * 
 * @author Luther J Maloney
 * @since October 2013
 */
public interface XanbooNotificationProviderInterface
{
    //keys used in ConfigMap
    /**profile type id**/
    public static String PROFILE_ID	= "PROFILETYPE_ID";
    /**Description of the profile **/
    public static String PROFILE_DESCR = "DESCRIPTION";
    /**The suffix field contains the HTTP URL or the email host name**/
    public static String PROFILE_SUFFIX = "SUFFIX";
    /**Max len (for SMS messages) **/
    public static String PROFILE_MAXLEN = "MAXLEN";
    /**Flag used to determine if provider can send attachments **/
    public static String PROFILE_ATTACH = "ATTACHMENT";
    /**Flag for tracking usage of the provider **/
    public static String PROFILE_TRK_USAGE = "TRACK_USAGE";
    /**the user name for the profile **/
    public static String PROFILE_USERNM = "USERNAME";
    /**the password associate with the username **/
    public static String PROFILE_USERPASS = "USERPASS";
    /**the profile category**/
    public static String PROFILE_CATEGORY = "PROFILE_CATEGORY";
    /** profile properties **/
    public static final String PROPERTIES = "PROVIDER_PROPERTIES";
    /** profile classname **/
    public static final String CLASSNAME = "PROVIDER_CLASSNAME";
    /**
    * Method to perform initialization of the provider
    * @param configMap 
    */
    public void initialize(HashMap configMap);
    /**
    * 
    * @param contentType
    * @return 
    */
    public boolean canAccept(String contentType);
    /**
    * Method to return true if the provider can accept a list of destinations.
    * @return 
    */
    public boolean canAcceptDestinationList();
    /*
    * Returns the HashMap instance containing the configuration parameters
    */
    public HashMap getConfigMap();
    /**
    * Setter for the configuration parameters hash map
    * @param configMap 
    */
    public void setConfigMap(HashMap configMap);
    /**
    * Encodes the message in a manner specific to the provider
    * @param message
    * @return 
    */
    public String encodeMessage(String message);
    /**
    * Returns true if the message parameters should be encoded prior to applying the template. 
    * This method is ONLY applicable when the _text attribute is not present in the XAIL command
    * @return 
    */
    public boolean encodeParametersBeforeTemplate();
    /**
    * Method to log notification errors to the database
    * @param error
    * @param message 
    */
    public void logNotificationError(NotificationDestination destination,NotificationMessageInterface message,String errorStr,Throwable stackTrace);
    /**
    * The profile suffix (useful for legacy DLA XAIL 
    * @return 
    */
    public String getProfileSuffix();
    /**
    * The profileTypeId
    * @return 
    */
    public Integer getProfileTypeId();
    /**
    * 
    * @param destination
    * @param message 
    */
    public void sendMessage(NotificationDestination destinations,NotificationMessageInterface message) throws XanbooNotificationException;
    /**
    * 
    * @param destinations
    * @param message 
    */
    public void sendMessage(List<NotificationDestination> destinations,NotificationMessageInterface message) throws XanbooNotificationException;
    /**
    * Validate the notification being sent. Most SMS providers have a max number of characters they will support
    * @param message 
    */
    public void validate(NotificationMessageInterface message);
    /**
    * Method to validate destinations
    * @param destinations
    * @return 
    */
    public boolean validateDestinations(List<NotificationDestination> destinations);

}