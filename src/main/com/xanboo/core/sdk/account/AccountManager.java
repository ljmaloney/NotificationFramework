/*

 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/account/AccountManager.java,v $
 * $Id: AccountManager.java,v 1.33 2010/10/08 18:24:59 levent Exp $
 *
 * Copyright 2011 AT&T Digital Life
 *
 */

package com.xanboo.core.sdk.account;
 
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import com.xanboo.core.sdk.contact.XanbooContact;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooAdminPrincipal;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.XanbooException;


/**
 *<p>
 * Business Interface for the AccountManagerEJB
 *</p>
 *
 * <b> Glossary </b>
 * <ul>
 *  <li>
 *  <b>Notification profile</b> <br>
 * Represents a notification contact record. Notification Profiles are managed
 * by using the profile related SDK methods within AccountManager, but are assigned to specific events
 * by using the notification related SDK methods in DeviceManager.<br>
 * Notification profiles may include at least one, at most 3 notification addresses/contacts. Each address/contact must have
 * a valid profile type (e.g. email).
 *</li>
 *<li>
 *  <b>Notification profile type</b> <br>
 * Notification profiles may be one of a number of types. The most common type is email, other types are configured in the Xanboo system for various transports/protocols.
 * The type must be a valid PROFILETYPE_ID returned by {@link com.xanboo.core.sdk.sysadmin.SysAdminManager#getProfileTypeList(XanbooAdminPrincipal xCaller)} 
 *</li>
 * <li>
 *  <b>Domain</b> <br>
 *  Xanboo accounts have an associated with them a domain parameter. This is a feature that could be used for different purposes, but is basically
 *  just a way of segregating groups of accounts if required. Domains must be pre-configured in the xanboo system and cannot be specified dynamically.<br>
 *  If in doubt, or if you do not have a specific purpose for this feature, use the constant XanbooAccount.DEFAULT_DOMAIN.
 * </li>
 *</ul>
 *
 * @see com.xanboo.core.sdk.device.DeviceManagerEJB
 * @see com.xanboo.core.sdk.account.XanbooAccount
 * @see com.xanboo.core.sdk.account.XanbooUser
 * @see com.xanboo.core.sdk.account.XanbooQuota
 * @see com.xanboo.core.sdk.account.XanbooNotificationProfile
 *
 */
//TODO - add XanbooQuota to glossary

public interface AccountManager //extends EJBObject 
{ 
    
    /**
     * Registers a new account with the Xanboo system
     * <br>
     * <p>
     *  The supplied XanbooAccount object must contain a valid XanbooUser object (XanbooAccount.setUser() ). This user becomes the account's
     *  master user, which has full control over the account, and cannot be deleted (without deleting the entire account). <br>
     *  On successful account creation, a XanbooPrincipal object is returned for the created user and account. This can be stored and used on all
     *  subsequent SDK calls for this user, or alternatively, a new XanbooPrincipal can be created by calling authenticateUser().<br>
     *  Disk, User and Gateway quotas must be supplied to create an account.
     * </p>
     * @param xAccount A XanbooAccount object from which the new account information is extracted.
     * @param xQuotas[] An array of quota objects to be applied to the new account.
     *
     * @return the account id associated with the newly created account.
     * @throws XanbooException if the account failed to be created.
     *
     * @see XanbooQuota
     * @see XanbooUser
     *
     */ 
    public XanbooPrincipal newAccount(XanbooAccount xAccount, XanbooQuota xQuotas[] ) throws RemoteException, XanbooException;
    
    
    /**
     * Authenticates a Xanboo user for the supplied domain with the xanboo username and password.
     * On successful authentication, a XanbooPrincipal object is returned for the user who matches the supplied credentials.
     * Depending on domain configuration, validated username may be locked after N consecutive failed authentication attempts.
     * 
     * @param domainId the domain in which the user resides (see glossary above)
     * @param userName username to authenticate
     * @param password user password
     *
     * @return A XanbooPrincipal object for the authenticated user
     * @throws XanbooException if authentication fails.
     *
     */    
    public XanbooPrincipal authenticateUser(String domainId, String userName, String password) throws RemoteException, XanbooException;
    
    
    /**
     * Authenticates a Xanboo user for the supplied domain with the xanboo username and password.
     * On successful authentication, a XanbooPrincipal object is returned for the user who matches the supplied credentials.
     * An optional loginAttempts parameter can specify that the username will become locked after that number of consecutive login failures.
     * Once a user id is locked, it can be unlocked by resetting it's password ( via either forgot password link, or xancore-support )
     *
     * @param domainId the domain in which the user resides (see glossary above)
     * @param userName username to authenticate
     * @param password user password
     * @param loginAttempts the max number of consecutive login failure attempts.
     *
     * @return A XanbooPrincipal object for the authenticated user
     * @throws XanbooException if authentication fails.
     *
     * @deprecated replaced by {@link #authenticateUser(String, String, String)}
     */    
    public XanbooPrincipal authenticateUser(String domainId, String username, String password, int maxLoginAttempts) throws RemoteException, XanbooException;
        
        
    /**
     * Authenticates a Xanboo user for the supplied domain with the xanboo username and a security question/answer.
     * On successful authentication, a XanbooPrincipal object is returned for the user who matches the supplied credentials.
     * Depending on domain configuration, validated username may be locked after N consecutive failed authentication attempts.
     *
     * @param domainId the domain in which the user resides (see glossary above)
     * @param userName username to authenticate
     * @param sq a XanbooSecurityQuestion object with a valid security question id and answer to match
     *
     * @return A XanbooPrincipal object for the authenticated user
     * @throws XanbooException if authentication fails.
     * 
     * @deprecated replace by {@link #authenticateUser(String, String, XanbooSecurityQuestion [])}
     */    
    public XanbooPrincipal authenticateUser(String domainId, String username, XanbooSecurityQuestion sq) throws RemoteException, XanbooException;

    /**
     * Authenticates a Xanboo user for the supplied domain with the xanboo username and a security question/answer.
     * On successful authentication, a XanbooPrincipal object is returned for the user who matches the supplied credentials.
     * Depending on domain configuration, validated username may be locked after N consecutive failed authentication attempts.
     *
     * @param domainId the domain in which the user resides (see glossary above)
     * @param userName username to authenticate
     * @param sqs an array of XanbooSecurityQuestion object with a valid security question id and answer to match
     *
     * @return A XanbooPrincipal object for the authenticated user
     * @throws XanbooException if authentication fails.
     *
     */    
    public XanbooPrincipal authenticateUser(String domainId, String username, XanbooSecurityQuestion [] sqs) throws RemoteException, XanbooException;
    
    /**
     * Authenticates a Xanboo user by external domain and user ID.
     * External user IDs are set at user creation time, and can be used by an external system as a convenient way to link user accounts to xanboo.
     *
     * @param domainId domain identifier for the user
     * @param extUserId The external user Id to authenticate
     *
     * @return A XanbooPrincipal object for the authenticated user
     * @throws XanbooException if authentication fails.
     *
     */    
    public XanbooPrincipal authenticateUser(String domainId, String extUserId) throws RemoteException, XanbooException;
    
       
     /**
      * Adds a new user to an already existing Xanboo account.
      * Only a master is permitted to add users to an account, so the user identified by the XanbooPrincipal object must be master.<br>
      * The user to be added:<br>
      *  - May not be a master user, as there may be only one master user per account
      *  - Must have a valid username (xUser.getUsername())
      *  - Must have a valid password (xUser.getPassword())
      *  - Must have a correctly formatted email address (xUser.getEmail())
      * @param xCaller a XanbooPrincipal object that identifies the caller
      * @param xUser a XanbooUser object from which the new user information is extracted. Security questions on the XanbooUser object are ignored in this call.
      *              Security questions can be added/updated thru updateUser calls.
      *
      * @return a user id associated with the newly created user.
      * @throws XanbooException if the user failed to be added to the account.
      *
      */
    public long newUser(XanbooPrincipal xCaller, XanbooUser xUser) throws RemoteException, XanbooException;
        
    
     /**
      * Adds a new user to an existing Xanboo account with contact information. <br>
      * Similar to the overloaded newUser method, but storing additional contact information related to the user.
      * Note that the email address that is part of the XanbooContact will overwrite the email address of XanbooUser.
      * 
      * @param xCaller a XanbooPrincipal object that identifies the caller
      * @param xUser a XanbooUser object from which the new user information is extracted.
      * @param xContact a XanbooContact object from which the contact information will be extracted.
      *
      * @return a user id of the newly created user.
      * @throws XanbooException if the user failed to be added
      */    
    public long newUser(XanbooPrincipal xCaller, XanbooUser xUser, XanbooContact xContact) throws RemoteException, XanbooException;
    
        
     /**
      * Deletes a Xanboo user from a Xanboo account. <br>
      * Master users may not be deleted, and only the master user has the ability to delete users.
      *
      * @param xCaller a XanbooPrincipal object that identifies the caller
      * @param userId  the user id to delete. The specified id must belong to the caller account.
      *
      * @throws XanbooException if the user failed to be deleted.
      * 
      */    
    public void deleteUser(XanbooPrincipal xCaller, long userId) throws RemoteException, XanbooException;
    
        
     /**
     * Updates details stored about a XanbooUser.
     * <br>
     * xUser must have valid accountId and userId. Other parameters (extUserId, username, password, tzname & languageId) will only be updated if their values
     * are not null. This makes it simpler to update a userInformation without the need to first retrieve a XanbooUser object (although that is still a simple way of
     * updating a user). For master users only, this call allows users to update account's fifo purging preference as well.<br>
     * User status will be updated (0:active or 1:inactive/locked status) if the status id is NOT set to XanbooUser.STATUS_UNCHANGED.
     * User security questions will be updated if the security questions array for the user is not null. Only non-null array members will be updated (see XanbooUser javadocs).
     * Note that the password and security question answer fields will be stored encrypted/hashed and not human readable when queried back.
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param xUser a XanbooUser object from which the new user information is extracted.
     *
     * @throws XanbooException if the user failed to be updated.
     * 
     * @see com.xanboo.core.sdk.account.XanbooUser
     */    
    public void updateUser(XanbooPrincipal xCaller, XanbooUser xUser) throws RemoteException, XanbooException;
    
        
     /**
      * Gets a list of users assicated with the callers account.
      *<br>
      * <b> Columns Returned: </b>
      * <UL>
      *  <li> USER_ID </li>
      *  <li> EMAIL </li>
      *  <li> STATUS_ID </li>
      *  <li> IS_MASTER </li>
      *  <li> USERNAME </li>
      *  <li> IS_TEMP_PASSWORD </li>
      *  <li> SECQ_Q1 </li>
      *  <li> SECQ_Q2 </li>
      * </UL>
      * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
      *
      * @param xCaller a XanbooPrincipal object that identifies the caller
      *
      * @return a XanbooResultSet which contains a HashMap list of user records
      * @throws XanbooException if retrieving the list failed.
      */    
    public XanbooResultSet getUserList(XanbooPrincipal xCaller) throws RemoteException, XanbooException;
        
    
    /**
     * @deprecated  replaced by {@link #getAccountQuota(XanbooPrincipal, int, String)}
     */
    public XanbooQuota getAccountQuota(XanbooPrincipal xCaller, int quotaId) throws RemoteException, XanbooException;
    
    
     /**
     * Gets specific account quota values (current value, max value etc) for a given account and quota id.
     * Choices for quotaID are XanbooQuota constants: <br>
      <ol>
        <li> XanbooQuota.DISK </li>
        <li> XanbooQuota.USER</li>
        <li> XanbooQuota.GATEWAY</li>
        <li> XanbooQuota.DEVICE</li>
        <li> XanbooQuota.NOTIFICATION</li>
      </ol>
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param quotaId The ID to specify which quota to retrieve
     * @param quotaClass For device or notification quotas, a device class must be specified. Otherwise null.
     *
     * @return a XanbooQuota object containing the quota values
     * @throws XanbooException if the query failed.
     *
     * @see com.xanboo.core.sdk.account.XanbooQuota
     */    
    public XanbooQuota getAccountQuota(XanbooPrincipal xCaller, int quotaId, String quotaClass) throws RemoteException, XanbooException;    
    
    
    /**
     * Gets all account quota values for the caller's account.
     * An account will have multiple quotas assigned to it, affecting different properties. This will retrieve a list of all quotas that relate to the caller's account.
     *
     * <br>
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> DESCRIPTION </li>
     *  <li> QUOTAREF_ID </li>
     *  <li> CURRENT_VALUE </li>
     *  <li> QUOTA_VALUE </li>
     *  <li> QUOTACLASS_ID </li>
     *  <li> RESET_PERIOD </li>
     * </UL>
     *
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * The quotaclass_id is currently only used for device and notification quotas. It specifies the specific device class or notification profile type 
     * for which the quota applies.
     * Note that a device quota may be set for a major device class, eg. 0200, to limit the total number of cameras regardless of sub-type.
     * It may also be set for a minor class, eg. 0202 to place a quota on the number of wireless cameras.
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     *
     * @return a XanbooResultSet which contains a HashMap list of account quotas
     * @throws XanbooException if the query failed.
     *
     * @see com.xanboo.core.sdk.account.XanbooQuota
     */    
    public XanbooResultSet getAccountQuotaList(XanbooPrincipal xCaller) throws RemoteException, XanbooException;
    
    
    
    /**
     * Updates specific account quota limits for a given account and quota id. <br>
     * See XanbooQuota constants for available quota ids. <br>
     * The force flag allows the quota to be set lower than the current value.
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param xQuota a XanbooQuota object for requested quota update.
     * @param forceFlag If false, the quota may not be set to less that the current value.
     *
     * @throws XanbooException if the update failed.
     */    
    public void updateAccountQuota(XanbooPrincipal xCaller, XanbooQuota xQuota, boolean forceFlag) throws RemoteException, XanbooException;
    

    
     /**
     * Updates contact information for a Xanboo user belonging to the caller.
     * One way to update a user's information it to first retrieve a XanbooContact object using getUserInformation, updating the attributes of that object, 
       and then pass it into this method.
     * The supplied XanbooContact must have accountId and userId.
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param xContact a XanbooContact object from which the user contact information will be extracted.
     *
     * @throws XanbooException if the update failed.
     */    
    public void updateUserInformation(XanbooPrincipal xcaller, XanbooContact xContact) throws RemoteException, XanbooException;  
    
    
    
    /**
     * Gets contact information for a xanboo user.
     * Note that the supplying of user contact information is optional, so a user may not have any valid contact information.
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param userId the user id to get the information for. The specified
     *               id must belong to the caller account.
     *
     * @return a XanbooContact object
     * @throws XanbooException if the information could not be retrieved.
     */    
    public XanbooContact getUserInformation(XanbooPrincipal xcaller, long userId) throws RemoteException, XanbooException;     
    
    
    
    /**
     * Returns all notification profile records for the caller's account.
     * A notification profile is a destination address stored in the system to which a message may be sent upon the occurance of an event.
     * Methods in the DeviceManager allow the assigning of notification profiles to device instance events for notification purposes.<br>
     * See constants in this class for definitions of profile types.
     *
     * <br>
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> PROFILE_ID </li>
     *  <li> PROFILETYPE_ID </li>
     *  <li> PROFILE_ADDRESS </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     *
     * @return a XanbooResultSet which contains a HashMap list of account notification profiles
     * @throws XanbooException
     *
     * @see com.xanboo.core.sdk.device.DeviceManager
     * @see com.xanboo.core.sdk.account.XanbooNotificationProfile
     * 
     * @deprecated  replaced by {@link #getNotificationProfile(XanbooPrincipal, boolean)}
     */    
    public XanbooResultSet getNotificationProfile(XanbooPrincipal xCaller) throws RemoteException, XanbooException;
    

    /**
     * Returns all or requested notification profiles for the caller's account, including emergency contact records.
     * A notification profile is a destination address stored in the system to which a message may be sent upon the occurance of an event.
     * Methods in the DeviceManager allow the assigning of notification profiles to device instance events for notification purposes.<br>
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param getEmergencyContacts boolean to indicate if emergency contact records will also be returned or not
     *
     * @return an array of XanbooNotificationProfile objects or null
     * @throws XanbooException
     *
     * @see com.xanboo.core.sdk.account.XanbooNotificationProfile
     */
    public XanbooNotificationProfile[] getNotificationProfile(XanbooPrincipal xCaller, boolean getEmergencyContacts) throws RemoteException, XanbooException;
    
    
    /**
     * Creates a new notification profile for the caller's account.
     * A notification profile is a destination address to which a message may be sent upon the occurance of an event.
     * Methods in the DeviceManager allow the assigning of notification profiles to device instance events for notification purposes.<br>
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param profileType the type of profile address/id to be created. Type must be a valid PROFILETYPE_ID<br>
     *  returned by {@link com.xanboo.core.sdk.sysadmin.SysAdminManager#getProfileTypeList(XanbooAdminPrincipal xCaller)} 
     *
     * @param profileAddress the address/id (email address, pager pin number, phone number, etc.) for the profile to be created.
     *
     * @return new profile id 
     * @throws XanbooException if the profile was not created.
     *
     * @deprecated  replaced by {@link #newNotificationProfile(XanbooPrincipal, XanbooNotificationProfile)}
     */    
    public long newNotificationProfile(XanbooPrincipal xCaller, int profileType, String profileAddress) throws RemoteException, XanbooException;

    
    /**
     * Creates a new notification profile for the caller's account.
     * A notification profile is a destination address to which a message may be sent upon the occurance of an event.
     * Methods in the DeviceManager allow the assigning of notification profiles to device instance events for notification purposes.<br>
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param xnp a valid XanbooNotificationProfile object to be created
     *
     * @return new profile id
     * @throws XanbooException if the profile was not created.
     *
     * @see com.xanboo.core.sdk.account.XanbooNotificationProfile
     */
    public long newNotificationProfile(XanbooPrincipal xCaller, XanbooNotificationProfile xnp) throws RemoteException, XanbooException;

    /**
     * Creates a set of new notification profiles for the caller's account.
     * A notification profile is a destination address to which a message may be sent upon the occurance of an event.
     * Methods in the DeviceManager allow the assigning of notification profiles to device instance events for notification purposes.<br>
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param xnp[] an array of valid XanbooNotificationProfile objects to be created. All objects in the array must have the same emergency contact flag value.
     *
     * @return new profile id[] an array of profile ids created
     * @throws XanbooException if the profile was not created.
     *
     * @see com.xanboo.core.sdk.account.XanbooNotificationProfile
     */
    public long[] newNotificationProfile(XanbooPrincipal xCaller, XanbooNotificationProfile[] xnp) throws RemoteException, XanbooException;
    
    
    /**
     * Removes a number of notification profiles from the caller's account.
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param profileId[] an array of profile ids to be removed. If null, all notification profiles
     *        from the caller's account will be removed. Emergency Contact records should be removed as an array of ids, not by passing a null profileId!
     *
     * @throws XanbooException
     */    
    public void deleteNotificationProfile(XanbooPrincipal xCaller, long[] profileId) throws RemoteException,XanbooException;
    /**
     * Removes multiple notification profiles from the callers account
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param xnp and array of XanbooNotificationProfile(s) that identify the profiles to be removed.
     * @throws RemoteException
     * @throws XanbooException 
     */
    public void deleteNotificationProfile(XanbooPrincipal xCaller,XanbooNotificationProfile[] xnp)throws RemoteException,XanbooException;
    
     /**
     * Updates an existing notification profile.
     * <br>
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param profileId notification profile id
     * @param profileType the type of profile address/id to be created. Type must be a valid PROFILETYPE_ID<br>
     *   returned by {@link com.xanboo.core.sdk.sysadmin.SysAdminManager#getProfileTypeList(XanbooAdminPrincipal xCaller)} 
     * @param profileAddress the address/id (email address, pager pin number, phone number, etc.) for the profile to be updated.
     *
     * @throws XanbooException if the user failed to be updated.
     *
     * @deprecated  replaced by {@link #updateNotificationProfile(XanbooPrincipal, XanbooNotificationProfile)}
     */    
    public void updateNotificationProfile(XanbooPrincipal xCaller, long profileId, int profileType, String profileAddress) throws RemoteException, XanbooException;
    

     /**
     * Updates an existing notification profile.
     * <br>
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param xnp a valid XanbooNotificationProfile object to be updated<br>. Only non-null contact addresses and >=0 profile-types will be updated
     *            for the profile id specified. If a profile id is not specified, the call will add a new profile record and work similar to the newNotificationProfile call.
     *
     * @throws XanbooException if the user failed to be updated.
     */
    public void updateNotificationProfile(XanbooPrincipal xCaller, XanbooNotificationProfile xnp) throws RemoteException, XanbooException;

     /**
     * Updates a set of existing notification profile.
     * <br>
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param xnp[] an array of valid XanbooNotificationProfile objects to be updated<br>. Only non-null contact addresses and >=0 profile-types will be updated
     *            for the profile ids specified. If a profile id is not specified for a profile record, the call will add a new profile record and work similar to 
     *            the newNotificationProfile call. All objects in the array must have the same emergency contact flag value!
     *
     * @throws XanbooException if the user failed to be updated.
     */
    public void updateNotificationProfile(XanbooPrincipal xCaller, XanbooNotificationProfile[] xnp) throws RemoteException, XanbooException;
    
    
    /**
     * Sends test messages to the notification profiles specified. Only first contact address(es) is tested.
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param profileId an array of profile ids to be tested
     *
     * @throws XanbooException
     */    
    public void testNotificationProfile(XanbooPrincipal xCaller, long[] profileId) throws RemoteException,XanbooException;
    
    
    /**
     * Returns a list of broadcast messages (if any) waiting for an account.
     * Broadcast messages are usually configured by an administator, or automated process, and are displayed to the user somewhere in the UI.
     * Note that if a message is broadcast across all accounts, it will have a null account id (it may be preferable to distinguish in the UI between
     * global messages, and messages destined for the caller)
     * <br>
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> MESSAGE </li>
     *  <li> ACCOUNT_ID </li>
     *  <li> MESSAGE_ID </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param lang Language id of the message to be retrieved (e.g. "fr"). If null, "en" is assumed.
     *
     * @return a XanbooResultSet which contains a HashMap list of broadcast messages
     * @throws XanbooException
     */    
    public XanbooResultSet getBroadcastMessage(XanbooPrincipal xCaller, String lang) throws RemoteException, XanbooException;   
    

    
     /**
     * Retrieves a specific XanbooUser object.
     *
     * @param xCaller The accountId and userId properties are used to authenticate this call
     * @param userId The ID of the user to retrieve
     *
     * @return A XanbooUser object of the requested ID.
     *         Returned XanbooUSer object will have the password and security question answer (if applicable) 
     *         attributes as null since they are hashed and not readable.
     * @throws XanbooException if the operation failed.
     *
    */      
    public XanbooUser getUser(XanbooPrincipal xCaller, long userId) throws RemoteException, XanbooException ;
    

    
     /**
     * Locates a specific XanbooUser record by a given username or email address.
     *
     * @param domainId the domain the user record will be searched
     * @param username login username for the user to be located. Either a username or an email address must be specified.
     * @param email email address for the user to be located. Either a username or an email address must be specified. If username is not null, this parameter is ignored.
     * @param sendUsernameReminder boolean to send an email notification to the user for forgotten username scenarios.  
     *
     * @return An array of XanbooUser objects found.
     *         Returned XanbooUser object will have the password and security question answer (if applicable) 
     *         attributes as null since they are hashed and not readable.
     * @throws XanbooException if the operation failed.
     *
    */      
    public XanbooUser[] locateUser(String domainId, String username, String email, boolean sendUsernameReminder) throws RemoteException, XanbooException ;
    
    
    
    
     /**
     * Resets password for a given user.
     *
     * @param user a XanbooUser object to identify the user to reset the password for. At least the domain, account and userid attributes must be populated to
     *             make this call and match an existing user record. Typically the locateUser() is called first (by username or email address) to obtain the user object to pass into this call.
     *             This call also generates a temporary password email to the user's email address.
     * 
     * @return temporary password string set.
     * 
     * @throws XanbooException if the operation failed.
     *
    */      
    public String resetPassword(XanbooUser user) throws RemoteException, XanbooException ;
    
    
    
     /**
     * Updates user access control for a device.
     * <br>
     * Currently only gateway level access control is supported, so the deviceGUID passed to this call must be '0'.<br>
     * The user identified by the supplied XanbooPrincipal must be the master user for this account, as non-master users
     * do not have privilege to update access control directives. <br>
     * By default, a newly created user has no access to it's account gateways. <BR>
     * <br>
     * Valid values for accessId are:
     * <ul>
     *  <li> AccountManagerEJB.USER_ACCESS_NONE </li>
     *  <li> AccountManagerEJB.USER_ACCESS_READ </li>
     *  <li> AccountManagerEJB.USER_ACCESS_READWRITE </li>
     * </ul>
     *
     * @param xCaller Identifies the master user for this account.
     * @param userId The ID of the non-master user to update the access level for.
     * @param gatewayGUID The GUID of the gateway for which to update the access level.
     * @param deviceGUID The GUID of the device for which to update the access. (see note above)
     * @param accessId The access level to assign for this user/device combination
     *
     * @return A XanbooUser of the requested ID
     * @throws XanbooException if the operation failed.
     *
    */  
    public void updateUserACL( XanbooPrincipal xCaller, long userId, String gatewayGUID, String deviceGUID, int accessId ) throws RemoteException, XanbooException ;

    
    
     /**
      * Retrieves one or more ACL directives for a user.
      * <br>
      *Currently, only gateway level access control is permitted, so device_guid must always be zero.
      *<br>
      * <b> Columns Returned: </b>
      * <UL>
      *  <li> OBJECTTYPE_ID </li>
      *  <li> OBJECT_ID </li>
      *  <li> ACCESSTYPE_ID </li>
      * </UL>
      * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
      *
      * @param xCaller a XanbooPrincipal object that identifies the caller
      * @param userId The ID of the user for which to retrieve access control directives
      * @param gatewayGUID The guid identifying the gateway for which to retrieve access directive. If null, all locations returned.
      * @param deviceGUID The guid identifying the device for which to retrieve access directive. If null, all devices returned.
      *
      *
      * @return a XanbooResultSet which contains a HashMap list of user records
      * @throws XanbooException if retrieving the list failed.
      */    
    public XanbooResultSet getUserACL(XanbooPrincipal xCaller, long userId, String gatewayGUID, String deviceGUID ) throws RemoteException, XanbooException;
 
    
    /**
     * Returns information for a specific account in the Xanboo system.
     * <br>
     *
     * <br>
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> ACCOUNT_ID </li>
     *  <li> DOMAIN_ID </li>
     *  <li> EXT_ACCOUNT_ID </li>
     *  <li> INBOX_COUNT </li>
     *  <li> STATUS_ID </li>
     *  <li> TRASH_COUNT </li>
     *  <li> TOKEN </li>
     *  <li> ENABLE_FIFO_PURGE </li>
     *  <li> TYPE </li>
     *  <li> SELF_PROVISIONING_OVERRIDE </li>
     *  <li> PUSH_PREFS</li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @return a XanbooResultSet which contains a HashMap list of Xanboo accounts
     * @throws XanbooException
     *
     * @see XanbooPrincipal
     * @see XanbooResultSet
     *
     */     
    public XanbooResultSet getAccount(XanbooPrincipal xCaller) throws RemoteException, XanbooException;
    
    
    /**
     * Updates the status of an account to INACTIVE, ACTIVE, DISABLED or CANCELLED, or the registration token for an account.
     * <br>
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param status the new status ID. The status value can be one of <code>XanbooAccount.STATUS_INACTIVE</code>, 
     *        <code>XanbooAccount.STATUS_ACTIVE</code>, <code>XanbooAccount.STATUS_DISABLED</code>, or 
     *        <code>XanbooAccount.STATUS_CANCELLED</code>.
     * @param regToken the registration token to be updated
     * @throws XanbooException if the operation failed.
     *
     * @see XanbooPrincipal
     * @see XanbooAccount
     *
     * @deprecated  replaced by {@link #updateAccount(XanbooPrincipal xCaller, XanbooAccount xAccount)}
    */     
    public void updateAccount(XanbooPrincipal xCaller, int status, String regToken) throws RemoteException, XanbooException;
 

    /**
     * Updates the status, registration token, external account id and fifo purging preference flag for an account
     *    - status can be updated to XanbooAccount.STATUS_INACTIVE, XanbooAccount.STATUS_ACTIVE, XanbooAccount.STATUS_DISABLED or XanbooAccount.STATUS_CANCELLED
     *    - if XanbooAccount parameter's status is set to XanbooAccount.STATUS_UNCHANGED (-1), the status value will not be updated
     *    - if XanbooAccount parameter's token is set to null, the token value will not be updated
     *    - if XanbooAccount parameter's external account id is set to null, the external account id value will not be updated
     *    - if XanbooAccount parameter's fifoPurging flag is set to -1, the fifoPurging flag value will not be updated
     *    - if XanbooAccount parameter's pushPreferences is set to NULL, the ACCOUNT.PUSH_PREFS column will not be updated
     * <br>
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param xAccount A XanbooAccount object to get updated account attributes from.
     * @throws XanbooException if the operation failed.
     *
     * @see XanbooPrincipal
     * @see XanbooAccount
     *
     */
    public void updateAccount(XanbooPrincipal xCaller, XanbooAccount xAccount) throws RemoteException, XanbooException;



    /**
     * Completely deletes an account from the Xanboo system including the removal of the account's data directory.
     *
     * @param xCaller a XanbooPrincipal object that identifies the callers whose account will be deleted
     *
     * @throws XanbooException if the operation failed.
     *
     */    
    public void deleteAccount(XanbooPrincipal xCaller) throws RemoteException, XanbooException;


    
    /**
     * Updates an existing gateway subscription for a given account.
     * <p>
     * This method is used to update an existing gateway subscription record, including email and security pins for the master user, and alarm password for the subscription
     * </p>
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param subsId unique subscription identifier to be updated (e.g. a CTN). Must be specified.
     * @param hwId a unique gateway hardware identifier to to be updated (e.g. IMEI or MAC).  Must be specified.
     * @param masterPin optional security disarm pin for the controller (master user). Will be updated, if not null.
     * @param masterDuress optional security duress pin for the controller (master user). Will be updated, if not null.
     * @param alarmPass optional security alarm password/code for verbal professional monitoring verifications. Will be updated, if not null.
     *
     * @throws XanbooException
     * 
     * @deprecated  replaced by {@link #updateSubscription(XanbooPrincipal, String, String, String, String, String, int)}
     *
     */
   public void updateSubscription(XanbooPrincipal xCaller, String subsId, String hwId, String masterPin, String masterDuress, String alarmPass) throws RemoteException, XanbooException;
    

    /**
     * Updates an existing gateway subscription for a given account.
     * <p>
     * This method is used to update an existing gateway subscription record, including email and security pins for the master user, and alarm password for the subscription
     * </p>
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param subsId unique subscription identifier to be updated (e.g. a CTN). Must be specified.
     * @param hwId a unique gateway hardware identifier to to be updated (e.g. IMEI or MAC).  Must be specified.
     * @param masterPin optional security disarm pin for the controller (master user). Will be updated, if not null.
     * @param masterDuress optional security duress pin for the controller (master user). Will be updated, if not null.
     * @param alarmPass optional security alarm password/code for verbal professional monitoring verifications. Will be updated, if not null.
     * @param alarmDelay to enable/disable SBN alarm delay timer for the installation. 0 will disable, 1 will enable alarm delay timer. Timer will not be updated if -1 passed.
     *
     * @throws XanbooException
     * 
     * @deprecated  replaced by {@link #updateSubscription(XanbooPrincipal, String, String, String, String, String, int, int)}
     */
   public void updateSubscription(XanbooPrincipal xCaller, String subsId, String hwId, String masterPin, String masterDuress, String alarmPass, int alarmDelay) throws RemoteException, XanbooException;


    /**
     * Updates an existing gateway subscription for the cxaller account.
     * <p>
     * This method is used to update an existing gateway subscription record, including email and security pins for the master user, and alarm password for the subscription
     * </p>
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param subsId unique subscription identifier to be updated (e.g. a CTN). Must be specified.
     * @param hwId a unique gateway hardware identifier to to be updated (e.g. IMEI or MAC).  Must be specified.
     * @param masterPin optional security disarm pin for the controller (master user). Will be updated, if not null.
     * @param masterDuress optional security duress pin for the controller (master user). Will be updated, if not null.
     * @param alarmPass optional security alarm password/code for verbal professional monitoring verifications. Will be updated, if not null.
     * @param alarmDelay to enable/disable professional monitoring alarm delay timer for the installation. 0 will disable, 1 will enable alarm delay timer. Timer will not be updated if -1 passed.
     * @param tcFlag to set the terms and conditions flag. A zero value indicates the flag is not set, a positive value indicates that it is. Will not be updated if negative value passed. A zero value indicates the flag is not set, a positive value indicates that it is.
     * 
     * @throws XanbooException
     * 
     * @deprecated  replaced by {@link #updateSubscription(XanbooPrincipal, XanbooSubscription, String, int, int)}
     */
   public void updateSubscription(XanbooPrincipal xCaller, String subsId, String hwId, String masterPin, String masterDuress, String alarmPass, int alarmDelay, int tcFlag) throws RemoteException, XanbooException;

   
    /**
     * Updates an existing gateway subscription for the caller account.
     * <p>
     * This method is used to update an existing gateway subscription record, including security disarm/duress pins, alarm password, alarm delay and T&C flag subscription properties, and also
     * change the existing hardware id (e.g. DLC IMEI or serial number) for the subscription. All other subscription properties are ignored during update!
     * </p>
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param xSub a valid XanbooSubscription object with subscription details to be updated. The subscription id and hardware id attributes are all required to identify a particular subscription to update, and
     *             the account id must match the caller account id. Only disarmPin, duressPins and alarmCode attributes with non-null values will be updated.
     * @param hwIdNew a new gateway hardware identifier (e.g. IMEI, serial#) to update the subscription with. Will be updated, if not null.
     * @param alarmDelay to enable/disable professional monitoring alarm delay timer for the installation. 0 will disable, 1 will enable alarm delay timer. Timer will not be updated if -1 passed.
     * @param tcFlag to set the terms and conditions flag. A zero value indicates the flag is not set, a positive value indicates that it is. Will not be updated if negative value passed. A zero value indicates the flag is not set, a positive value indicates that it is.
     *
     * @throws XanbooException
     * 
     */
    public void updateSubscription(XanbooPrincipal xCaller, XanbooSubscription xsub, String hwIdNew, int alarmDelay, int tcFlag) throws RemoteException, XanbooException;
   
   
    /**
     * Retrieve existing gateway subscription(s) for the account.
     * <p>
     * This method is used to retrieve existing gateway subscription records for the calling account, and optionally for a given subscription id and status
     * </p>
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param subsId unique subscription identifier to be retrieved. If null, all subscriptions for the account will be returned
     * @param hwId a unique gateway hardware identifier for the subscription (e.g. IMEI or MAC). If null, all subscriptions will be returned
     * 
     * <br>
     * <b> Some of the columns returned: </b>
     * <UL>
     *  <li> SUBS_ID</li>
     *  <li> GATEWAY_GUID</li>
     *  <li> SUBS_FLAGS</li>
     *  <li> HWID</li>
     *  <li> FIRSTNAME</li>
     *  <li> LASTNAME </li>
     *  <li> ZIP</li>
     *  <li> TC_FLAG</li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     * 
     * GATEWAY_GUID column provides the physical gateway instance the subscription is associated with. A GATEWAY_GUID of null indicates that the subscription
     * is pending gateway activation/installation.
     * <br>
     * 
     * @deprecated  replaced by {@link #getSubscription(XanbooPrincipal xCaller, String subsId)}
    */
    public XanbooResultSet getSubscription(XanbooPrincipal xCaller, String subsId, String hwId) throws RemoteException,XanbooException;

    
    
    /**
     * Retrieve existing subscription(s) for the caller account.
     * <p>
     * This method is used to retrieve existing gateway subscription records for the calling account, and optionally for a given subscription id
     * 
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param subsId unique subscription identifier to be retrieved. If null, all subscriptions for the account will be returned
     * 
     * @return an array of XanbooSubscription objects or null
     * @throws XanbooException
     *
     * @see com.xanboo.core.sdk.account.XanbooSubscription
     * <br>
    */
    public XanbooSubscription[] getSubscription(XanbooPrincipal xCaller, String subsId) throws RemoteException,XanbooException;
    
    
     /**
     * Retrieves the list of provisioned/pending device(s) for the caller account or subscription.
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param subsId subscription identifier (e.g. CTN) to query the provisioned device record(s) for. If null, all records for the given account will be returned.
     * @param hwId subscription hardware identifier (e.g. IMEI or Serial) to query the provisioned device record(s) for. If subId is not null, hwId must also be specified to identify the subscription. If subId is null, this parameter is assumed null.
     * @param classId optional 4-digit device class-id to specify the type/class of provisioned device to be queried. If null, all classes will be returned. If subId is null, this parameter is assumed null.
     * @param subclassId optional 2-digit device subclass-id to identify the type/class of provisioned device to be queried. If null, all subclasses will be returned. If subId is null, this parameter is assumed null.
     * @param installType 1-char installation type to query for. Must be one of 'S' (Self-Install), 'P' (Professional-install), or 'A' (Al). If null, all installation type records will be returned.
     * 
     * <br>
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> SUBS_ID</li>
     *  <li> HWID</li>
     *  <li> CLASS_ID</li>
     *  <li> SUBCLASS_ID</li>
     *  <li> INSTALL_TYPE</li>
     *  <li> SWAP_GUID </li>
     *  <li> PROVISION_COUNT</li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @return a XanbooResultSet containing a list of provisioned/pending device record(s) for the specified account or subscription.
     */    
    public XanbooResultSet getProvisionedDeviceList(XanbooPrincipal xCaller, String subsId, String hwId, String classId, String subclassId, String installType) throws RemoteException,XanbooException;
    

    
     /**
     * Returns the number of alarms for the account gateway specified.
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID gateway GUID to query the alarm count for.
     *
     * @return count total number of alarms 
     */    
    public int getAlertCount(XanbooPrincipal xCaller, String gatewayGUID) throws RemoteException, XanbooException;
    
    /**
     * Method to be called by the UI to when a user logs out of the system. Releases any resources
     * that have been specifically allocated for the user. 
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param reconnect - a flag used to specify if resources should be released and reallocated or just released
     * @throws RemoteException
     * @throws XanbooException 
     */
    public void logoutUser(XanbooPrincipal xCaller,Boolean reconnect)throws RemoteException, XanbooException;

       /**
    * Returns the notification opt-in status of the specified input.  If both profileAddress and token
    * are provided, both will be used in the query.  If only one of the inputs is provided, then
    * that input will be used in the query
    * @param xCaller
    * @param notificationAddress
    * @param token
    * @return the notification opt-in status or null if not found
    * @throws RemoteException
    * @throws XanbooException
    */
   public Integer getNotificationOptInStatus(XanbooPrincipal xCaller, String notificationAddress, String token) throws RemoteException, XanbooException;

   /**
    * Returns a Map of notification addresss and associated opt in status value for the specified input List.
    * @param xCaller
    * @param notificationAddress
    * @return
    * @throws RemoteException
    * @throws XanbooException
    */
   public Map<String, Integer> getNotificationOptInStatus(XanbooPrincipal xCaller, List<String> notificationAddress) throws RemoteException, XanbooException;

   /**
    * Sets the opt-in status with the specified value.  Either the token or (notification address and domain id of
    * the xCaller) are used to query the opt-in status.  If the opt-in status is not found, it will be created.
    * The profile type ID can be included in the notification address
    * For example 0%4045551212 (0 is the profile type ID, % is the separator
    * @param xCaller
    * @param notificationAddress
    * @param token
    * @param status
    * @throws RemoteException
    * @throws XanbooException
    */
   public void setNotificationOptInStatus(XanbooPrincipal xCaller, String notificationAddress, String token, int status) throws RemoteException, XanbooException;

   /**
    * Sets the opt-in status for each notification address in notificationAddresses.  The profile type ID can be included in the notification address
    * For example 0%4045551212 (0 is the profile type ID, % is the separator
    * @param xCaller
    * @param notificationAddresses  Map containing notification address - value pairs
    * @throws RemoteException
    * @throws XanbooException
    */
   public void setNotificationOptInStatus(XanbooPrincipal xCaller, Map<String, Integer> notificationAddresses) throws RemoteException, XanbooException;

   
   /**
    * Retrieves the list of "self-installable"  supported device classes (class+subclass combinations) for a particular account (domain), and monitoring type.   
    * and/or monitoring type (i.e professional/self/non monitoring). 
    *
    * @param domainId the domain identifier to query for
    * @param monType 1-char monitoring type to query for. Must be one of 'S' (Self-Monitoring), 'P' (Professional-Monitoring), 'N' (Non-Monitoring). If null, all supported devices for the domain will be returned.
    * 
    * <br>
    * <b> Columns Returned: </b>
    * <UL>
    *  <li> VENDOR_ID</li>
    *  <li> CLASS_ID</li>
    *  <li> SUBCLASS_ID</li>
    *  <li> ENABLED_PM</li>
    *  <li> ENABLED_SM</li>
    *  <li> ENABLED_NM</li>
    * </UL>
    * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
    * 
    * @return a XanbooResultSet containing a list of supported device classes (class_id and subclass_id combinations)
  */  
   public XanbooResultSet getSupportedDeviceList(XanbooPrincipal xp, String monType) throws RemoteException, XanbooException;
   
   
   /**
    * Retrieves the list of "self-installable" supported device classes (class+subclass combinations) for a particular account/subscription. 
    * If the includeProvisioned flag is true, the call will also include the provisioning/pending records for the subscription in the returned resultset.
    *
    * @param xCaller a XanbooPrincipal object that identifies the caller
    * @param subsId the subscription identifier to query for INSTALL_TYPE
    * @param hwId hardware identifier .
    * @param includeProvisioned, if true provisioning/pending records for the subscription.
    * 
    * <br>
    * <b> Columns Returned: </b>
    * <UL>
    *  <li> VENDOR_ID</li>
    *  <li> CLASS_ID</li>
    *  <li> SUBCLASS_ID</li>
    *  <li> INSTALL_TYPE</li>
    *  <li> SWAP_GUID </li>
    *  <li> PROVISION_COUNT</li>
    *  <li> ENABLED_PM</li>
    *  <li> ENABLED_SM</li>
    *  <li> ENABLED_NM</li>
    * </UL>
    * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
    * 
    * @return a XanbooResultSet containing a list of supported devices for the account, plus provisioned/pending device record(s) if requested for the specified subscription.
    * 
  */  
   public XanbooResultSet getSupportedDeviceList(XanbooPrincipal xp, String subsId, String hwId,  boolean includeProvisioned) throws RemoteException, XanbooException;
   
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
   public XanbooResultSet getSupportedClassList( XanbooPrincipal xCaller, String subsId, String hwid) throws RemoteException,XanbooException;
   
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
   public XanbooResultSet getSupportedClassList( XanbooPrincipal xCaller,String gateway) throws RemoteException,XanbooException;
   
}
