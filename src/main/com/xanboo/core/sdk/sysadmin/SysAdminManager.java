/*
 * $Source:  $
 * $Id:  $
 *
 * Copyright 2013 AT&T Digital Life
 *
 */

package com.xanboo.core.sdk.sysadmin;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.xanboo.core.model.device.XanbooCatalog;
import com.xanboo.core.model.device.XanbooDevice;
import com.xanboo.core.sdk.account.SubscriptionFeature;
import com.xanboo.core.sdk.account.XanbooAccount;
import com.xanboo.core.sdk.account.XanbooNotificationProfile;
import com.xanboo.core.sdk.account.XanbooSubscription;
import com.xanboo.core.sdk.contact.XanbooContact;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooAdminPrincipal;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.XanbooException;
/**
 * Remote Interface for the SysAdminManagerEJB
 * <p>
 * Methods in this manager are for administration of accounts, gateways and associated devices within the Xanboo core system. Methods are provided for
 * basic administration, such as listing, activating and deactivating accounts. In addition, some more advanced features such as data collection
 * are available.
 * </p>
 * <p>
 * All authenticated calls to SysAdminManager methods require a valid XanbooAdminPrincipal object as their first parameter.<br>
 * The XanbooAdminPrincipal can be obtained by calling {@link com.xanboo.core.sdk.sysadmin.SysAdminManager#authenticateAdmin}.
 * 
 * </p>
 * <p>
 *  <b>GLOSSARY</b>
 *  <ol>
 *      <li>
 *          <b>Broadcast Message</b> <br>
 *          Broadcast messages are stored in the system and are retrieved using AccountManager.getBroadcastMessage(). 
 *          They may be global (retrieved by all accounts), or specific to one account.
 *      </li>
 *      <li>
 *          <b>Managed Object History</b> (Data Collection)<br>
 *          Sometimes called data collection, this system works by creating a filter to specify which data to record. A filter simply consists of a 
 *          catalog Id, and a managed object ID (OID), and when set, all changes to that OID value for all devices of that catalog ID will be recorded.
 *          Recorded data may be retrieved globally, or for a particular filter.<br>
 *          The data collected by this mechanism should be periodically read from the SDK, stored in an external system, and the erased from the xanboo system.
 *      </li>
 *  </ol>
 *
 *
 */
public interface SysAdminManager  {
    
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
     *  <li> DATE_CREATED </li>
     *  <li> DATE_MODIFIED </li>
     *  <li> TRASH_COUNT </li>
     *  <li> TOKEN </li>
     *  <li> USER_ID </li>
     *  <li> ENABLE_FIFO_PURGE </li>
     *  <li> TYPE </li>
     *  <li> SELF_PROVISIONING_OVERRIDE </li>     
     *  <li> SELF_PROVISIONING </li>     
     *  <li> TECH_PROVISIONING </li>     
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param accountId the accountId to get the details for. 
     * @return a XanbooResultSet which contains a HashMap list of Xanboo accounts
     * @throws XanbooException
     *
     * @see XanbooAdminPrincipal
     * @see XanbooResultSet
     *
     */     
    public XanbooResultSet getAccount(XanbooAdminPrincipal xCaller, long accountId) throws RemoteException, XanbooException;
    

    /**
     * Queries a Xanboo account by External Account Id.
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
     *  <li> DATE_CREATED </li>
     *  <li> DATE_MODIFIED </li>
     *  <li> TRASH_COUNT </li>
     *  <li> TOKEN </li>
     *  <li> USER_ID </li>
     *  <li> ENABLE_FIFO_PURGE </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param extAccountId the external accountId to get the details for.
     * @return a XanbooResultSet which contains a HashMap list of Xanboo accounts
     * @throws XanbooException
     *
     * @see XanbooAdminPrincipal
     * @see XanbooResultSet
     *
     */     
    public XanbooResultSet getAccount(XanbooAdminPrincipal xCaller, String extAccountId) throws RemoteException, XanbooException;
    
    
    /**
     * Returns the list of all accounts in the system with pagination controls.
     *
     * <br>
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> ACCOUNT_ID </li>
     *  <li> DOMAIN_ID </li>
     *  <li> EXT_ACCOUNT_ID </li>
     *  <li> INBOX_COUNT </li>
     *  <li> STATUS_ID </li>
     *  <li> DATE_CREATED </li>
     *  <li> DATE_MODIFIED </li>
     *  <li> TRASH_COUNT </li>
     *  <li> TOKEN </li>
     *  <li> ENABLE_FIFO_PURGE </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param startRow the starting row number for the account list
     * @param username an optional username string to search for. If null, username is ignored.
     * @param numRows the number of account records to be returned
     *
     * @return a XanbooResultSet which contains a HashMap list of Xanboo accounts
     * @throws XanbooException
     *
     * @see XanbooAdminPrincipal
     * @see XanbooResultSet
     *
    */      
    public XanbooResultSet getAccountList(XanbooAdminPrincipal xCaller, String username, int startRow, int numRows) throws RemoteException, XanbooException;
    
    
    
    /**
     * Updates the status of an account to INACTIVE, ACTIVE, DISABLED or CANCELLED, or the registration token for an account.
     * <br>
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param accountId to update the status for
     * @param status the new status ID. The status value can be one of <code>XanbooAccount.STATUS_INACTIVE</code>, 
     *        <code>XanbooAccount.STATUS_ACTIVE</code>, <code>XanbooAccount.STATUS_DISABLED</code>, or 
     *        <code>XanbooAccount.STATUS_CANCELLED</code>.
     * @param regToken the registration token to be updated
     * @throws XanbooException if the operation failed.
     *
     * @see XanbooAdminPrincipal
     * @see XanbooAccount
     *
     * @deprecated  replaced by {@link #updateAccount(XanbooAdminPrincipal xCaller, XanbooAccount xAccount)}
     */     
    public void updateAccount(XanbooAdminPrincipal xCaller, long accountId, int status, String regToken) throws RemoteException, XanbooException;


    /**
     * Updates the type, status, registration token, external account id, fifo purging preference flag and device provisioning override flag for an account
     *    - account type can be set to once of XanbooAccount.TYPE_UNCHANGED, XanbooAccount.TYPE_REGULAR, XanbooAccount.TYPE_DEMO, XanbooAccount.TYPE_BETA or XanbooAccount.TYPE_DEMO|XanbooAccount.TYPE_BETA
     *    - account status can be set to one of XanbooAccount.STATUS_UNCHANGED, XanbooAccount.STATUS_INACTIVE, XanbooAccount.STATUS_ACTIVE, XanbooAccount.STATUS_DISABLED or XanbooAccount.STATUS_CANCELLED
     *    - account selfInstallProvisioningFlag can be set to one of XanbooAccount.PROVISIONING_UNCHANGED, XanbooAccount.PROVISIONING_DISABLED, XanbooAccount.PROVISIONING_ENABLED, XanbooAccount.PROVISIONING_NOOVERRIDE (no override,use domain default)
     *    - if XanbooAccount type is set to XanbooAccount.TYPE_UNCHANGED, the account type value will not be updated
     *    - if XanbooAccount status is set to XanbooAccount.STATUS_UNCHANGED, the status value will not be updated
     *    - if XanbooAccount token is set to null, the token value will not be updated
     *    - if XanbooAccount external account id is set to null, the external account id value will not be updated
     *    - if XanbooAccount fifoPurging flag is set to -1, the fifoPurging flag value will not be updated
     *    - if XanbooAccount selfInstallProvisioningFlag is set to XanbooAccount.PROVISIONING_UNCHANGED, the flag value will not be updated
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
    public void updateAccount(XanbooAdminPrincipal xCaller, XanbooAccount xAccount) throws RemoteException, XanbooException;


    
    /**
     * Returns the list of users associated within the specified account.
     * <br>
     * An account always has one master user, and may have a number of regular users. The maximum number of users that may be associated with an account is defined
     * by the account's user quota.
     *
     * <br>
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> ACCOUNT_ID </li>
     *  <li> IS_MASTER </li>
     *  <li> LANGUAGE_ID </li>
     *  <li> LAST_LOGIN </li>
     *  <li> TZNAME </li>
     *  <li> USERNAME </li>
     *  <li> USER_ID </li>
     *  <li> DOMAIN_ID </li>
     *  <li> STATUS_ID </li>
     *  <li> EXT_USER_ID </li>
     *  <li> PREFS </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param accountId to get the user list for. If -1, all user records will be returned.
     *
     * @return a XanbooResultSet which contains a HashMap list of the accounts users.
     * @throws XanbooException if the operation fails.
     *
     */    
    public XanbooResultSet getUserList(XanbooAdminPrincipal xCaller, long accountId) throws RemoteException, XanbooException;
    
    
    
    /**
     * Returns the list of users associated within the specified account with pagination controls.
     * <br>
     * An account always has one master user, and may have a number of regular users. The maximum number of users that may be associated with an account is defined
     * by the account's user quota.
     *
     * <br>
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> ACCOUNT_ID </li>
     *  <li> IS_MASTER </li>
     *  <li> LANGUAGE_ID </li>
     *  <li> LAST_LOGIN </li>
     *  <li> TZNAME </li>
     *  <li> USERNAME </li>
     *  <li> USER_ID </li>
     *  <li> DOMAIN_ID </li>
     *  <li> STATUS_ID </li>
     *  <li> EXT_USER_ID </li>
     *  <li> PREFS </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param accountId to get the user list for. If -1, all user records will be returned.
     * @param startRow the starting row number for the user list
     * @param numRows the number of user records to be returned.
     *
     * @return a XanbooResultSet which contains a HashMap list of the account's users.
     * @throws XanbooException if the operation fails.
     *
     * @see XanbooAdminPrincipal
     * @see XanbooResultSet
     */    
    public XanbooResultSet getUserList(XanbooAdminPrincipal xCaller, long accountId, int startRow, int numRows) throws RemoteException, XanbooException;
    
    
    
    /**
     * Updates status for a specific device instance to ACTIVE or INACTIVE.
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param accountId the owner account id for the device
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier to update.
     * @param status The new status for the device - must be one of SysAdminManagerEJB.DEVICE_STATUS_ACTIVE
     *        or SysAdminManagerEJB.DEVICE_STATUS_INACTIVE
     *
     * @throws XanbooException if the operation failed.
     *
     * @deprecated replaced by {@link #updateDevice(XanbooAdminPrincipal xCaller, long accountId, XanbooDevice xanbooDevice)}
     */    
    public void updateDeviceStatus(XanbooAdminPrincipal xCaller, long accountId, String gatewayGUID, String deviceGUID, int status) throws RemoteException, XanbooException;
    

    /**
     * Updates status, installerId or sourceId (customer brought-in flag) of an an existing device instance 
     * @param xCaller
     * @param accountId
     * @param xanbooDevice
     * @throws XanbooException
     */
    public void updateDevice(XanbooAdminPrincipal xCaller, long accountId, XanbooDevice xanbooDevice) throws RemoteException, XanbooException;
    
    /**
     * Returns a global device class reference list for devices in the system.
     *
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> CLASS_ID </li>
     *  <li> NAME </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param lang the language identifier string in which the class list will be returned. If null, "en" is assumed to be the default.
     *
     * @return a XanbooResultSet which contains a HashMap list of device classes
     * @throws XanbooException if the operation failed.
     *
     * @deprecated replaced by {@link #getDeviceClassList(String)}
     */    
    public XanbooResultSet getDeviceClassList(XanbooAdminPrincipal xCaller, String lang) throws RemoteException, XanbooException;
    
    
    /**
     * Returns a global device class reference list definitions for devices in the system.
     *
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> CLASS_ID </li>
     *  <li> NAME </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param lang the language identifier string in which the class list will be returned. If null, "en" is assumed to be the default.
     *
     * @return a XanbooResultSet which contains a HashMap list of device classes
     * @throws XanbooException if the operation failed.
     *
     */    
    public XanbooResultSet getDeviceClassList(String lang) throws RemoteException, XanbooException;
    
    
    
    /**
     * Returns list of all devices of specified class in the system.
     * <br>
     * Valid classes can be obtained by calling <code>SysAdminManager.getDeviceClassList()</code>
     *
     * <p>
     * The class can be specified as either a 2 character major class or 4 character specific class Id.
     * For example, requesting class '0200' would list managed objects for devices instanciated with class '0200', whereas requesting '02' would list for all devices of major class '02' (0200, 0201....02XX)
     * </p>
     *
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> ACCOUNT_ID </li>
     *  <li> CATALOG_ID </li>
     *  <li> CLASS_ID </li>
     *  <li> DEVICE_GUID </li>
     *  <li> GATEWAY_GUID </li>
     *  <li> LABEL </li>
     *  <li> LAST_CONTACT </li>
     *  <li> STATUS_ID </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param dClass device class id for the query. If null, all devices are returned. Valid class ids
     *        can be obtained via <code>getDeviceClassList</code> method.
     *
     * @return a XanbooResultSet which contains a HashMap list of devices
     * @throws XanbooException if the operation failed.
     *
     */    
    public XanbooResultSet getDeviceListByClass(XanbooAdminPrincipal xCaller, String dClass) throws RemoteException, XanbooException;
    
    
    
    /**
     * Returns list of all devices of specified class in the system with pagination controls.
     * <br>
     * Valid classes can be obtained by calling <code>SysAdminManager.getDeviceClassList()</code>
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> ACCOUNT_ID </li>
     *  <li> CATALOG_ID </li>
     *  <li> CLASS_ID </li>
     *  <li> DEVICE_GUID </li>
     *  <li> GATEWAY_GUID </li>
     *  <li> LABEL </li>
     *  <li> LAST_CONTACT </li>
     *  <li> STATUS_ID </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param dClass device class id for the query. If null, all devices are returned. Valid class ids
     *        can be obtained via <code>getDeviceClassList</code> method.
     * @param startRow the starting row number for the account list
     * @param numRows the number of account records to be returned
     *
     * @return a XanbooResultSet which contains a HashMap list of devices
     * @throws XanbooException if the operation failed.
     *
     */    
    public XanbooResultSet getDeviceListByClass(XanbooAdminPrincipal xCaller, String dClass, int startRow, int numRows) throws RemoteException, XanbooException;
    
   /**
     * Returns controller based on CTN or IMEI passed 
     *
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> ACCOUNT_ID </li>
     *  <li> CATALOG_ID </li>
     *  <li> CLASS_ID </li>
     *  <li> DEVICE_GUID </li>
     *  <li> GATEWAY_GUID </li>
     *  <li> LABEL </li>
     *  <li> LAST_CONTACT </li>
     *  <li> STATUS_ID </li>
     *  <li> DATE_CREATED </li>
     *  <li> SUBS_ID </li>
     *  <li> SUBS_FLAGS </li>
     *  <li> IMEI </li>
     *  <li> LOC_CODE </li>
     *  <li> INS_ID </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param subsId
     * @param imei
     * @return a XanbooResultSet which contains a HashMap list of controllers
     * @throws XanbooException if the operation failed.
     *
     * @deprecated replaced by {@link #getGateway(XanbooAdminPrincipal, String, String, String)}
     */       
    
    public XanbooResultSet getGateway(XanbooAdminPrincipal xCaller, String subsId, String imei) throws RemoteException, XanbooException;
    
    /**
     * Returns controller based on gatewayGuid passed 
     *
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> ACCOUNT_ID </li>
     *  <li> CATALOG_ID </li>
     *  <li> CLASS_ID </li>
     *  <li> DEVICE_GUID </li>
     *  <li> GATEWAY_GUID </li>
     *  <li> LABEL </li>
     *  <li> LAST_CONTACT </li>
     *  <li> STATUS_ID </li>
     *  <li> DATE_CREATED </li>
     *  <li> SUBS_ID </li>
     *  <li> SUBS_FLAGS </li>
     *  <li> IMEI </li>
     *  <li> LOC_CODE </li>
     *  <li> INS_ID </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param gatewayGuid
     * @return a XanbooResultSet which contains a HashMap list of controllers
     * @throws XanbooException if the operation failed.
     *
     * @deprecated replaced by {@link #getGateway(XanbooAdminPrincipal, String)}
     */       
    
    public XanbooResultSet getGateway(XanbooAdminPrincipal xCaller, String gatewayGuid) throws RemoteException, XanbooException;    
    
   /**
     * Returns controller based on CTN, IMEI or Serial Number passed 
     *
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> ACCOUNT_ID </li>
     *  <li> CATALOG_ID </li>
     *  <li> CLASS_ID </li>
     *  <li> DEVICE_GUID </li>
     *  <li> GATEWAY_GUID </li>
     *  <li> LABEL </li>
     *  <li> LAST_CONTACT </li>
     *  <li> STATUS_ID </li>
     *  <li> DATE_CREATED </li>
     *  <li> SUBS_ID </li>
     *  <li> SUBS_FLAGS </li>
     *  <li> IMEI </li>
     *  <li> LOC_CODE </li>
     *  <li> INS_ID </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param subsId
     * @param imei
     * @param serialNo
     * @return a XanbooResultSet which contains a HashMap list of controllers
     * @throws XanbooException if the operation failed.
     *
     */       
    
    public XanbooResultSet getGateway(XanbooAdminPrincipal xCaller, String subsId, String imei, String serialNo) throws RemoteException, XanbooException;
    
    /**
     * Creates a broadcast message to all accounts in the system.
     * Broadcast messages are a way of posting a message that may be shown to users at any time.
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param message the broadcast message (max 512 characters)
     * @param lang the language identifier string for the message. If null, "en" is assumed to be the default.
     * @param accountId the account to send the broadcast message to. If -1, message will be broadcasted to all accounts.
     *
     * @return the broadcast message id assigned
     * @throws XanbooException if the operation failed.
     *
     */     
    public long newBroadcastMessage(XanbooAdminPrincipal xCaller, String message, String lang, long[] accountId) throws RemoteException, XanbooException;   
    
    
    
    /**
     * Deletes one or more broadcast messages from the system.
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param messageId an array of message ids to be removed. 
     *
     * @throws XanbooException if the operation failed.
     *
     */    
    public void deleteBroadcastMessage(XanbooAdminPrincipal xCaller, long[] messageId) throws RemoteException, XanbooException;     
    
    
    
    /**
     * Enables value history recording for a list of managed objects in a specific device type.
     * <br>
     * 
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param catalogId the catalog id for the managed objects
     * @param mobjectId array of managed object ids to start recording history of
     *
     * @throws XanbooException if the operation failed.
     *
     */
    public void enableMObjectHistory(XanbooAdminPrincipal xCaller, String catalogId, String[] mobjectId) throws RemoteException, XanbooException;     
    
    
    
    /**
     * Disables value history recording for a list of managed objects.
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param catalogId the catalog id for the managed objects
     * @param mobjectId array of managed object ids of which to cease recording history
     *
     * @return a XanbooResultSet which contains a HashMap list of
     * @throws XanbooException if the operation failed.
     *
     */    
    public void disableMObjectHistory(XanbooAdminPrincipal xCaller, String catalogId, String[] mobjectId) throws RemoteException, XanbooException;     
    
    
    
    /**
     * Clears managed object history values for a specified date interval.
     * This method is typically used for cleaning up recorded values of managed objects enabled for data collection.
     * History values, between fromDate(inclusive) and toDate(exclusive) will be deleted.
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param catalogId the catalog id for the managed object. If null, all managed object history
     *        values will be returned regardless of the managed object id specified.
     * @param mobjectId managed object id to retrieve history values for. If null, all managed object
     *        history values for the specified catalog id will be returned.
     * @param fromDate value history interval start date.
     * @param toDate value history interval end date.
     *
     * @throws XanbooException if the operation failed.
     */    
    public void deleteMObjectHistory(XanbooAdminPrincipal xCaller, String catalogId, String mobjectId, Date fromDate, Date toDate) throws RemoteException, XanbooException;
    
    
    
    /**
     * Returns the list of managed objects that are enabled for history recording.
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> CATALOG_ID </li>
     *  <li> MOBJECT_ID </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     *
     * @return a XanbooResultSet which contains a HashMap list of history table entries.
     * @throws XanbooException if the operation failed.
     *
     */    
    public XanbooResultSet getMObjectHistoryTable(XanbooAdminPrincipal xCaller) throws RemoteException, XanbooException;
    
    
    
    /**
     * Returns managed object history values for a specified date interval. This method is 
     * typically used for retrieving recorded values of managed objects enabled for data collection.
     *
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> CATALOG_ID </li>
     *  <li> DEVICE_GUID </li>
     *  <li> GATEWAY_GUID </li>
     *  <li> MOBJECT_ID </li>
     *  <li> TIMESTAMP </li>
     *  <li> VALUE </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *  History values, between fromDate(inclusive) and toDate(exclusive) will be returned.
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param catalogId the catalog id for the managed object. If null, all managed object history
     *        values will be returned regardless of the managed object id specified.
     * @param mobjectId managed object id to retrieve history values for. If null, all managed object
     *        history values for the specified catalog id will be returned.
     * @param fromDate value history interval start date.
     * @param toDate value history interval end date.
     *
     * @return a XanbooResultSet containg managed object historical data.
     * @throws XanbooException if the operation failed.
     */    
    public XanbooResultSet getMObjectHistory(XanbooAdminPrincipal xCaller, String catalogId, String mobjectId, Date fromDate, Date toDate) throws RemoteException, XanbooException;
    
    
    
    /**
     *  Retuns list of all or specified system parameter(s).
     * <b> Columns Returned: </b>
     * <UL>
     *  <li>PARAM</li>
     *  <li>VALUE</li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param param The parameter to retrieve, or null to return all system parameters
     *
     * @return A XanbooResultSet of system parameters.
     * @throws XanbooException if the operation failed.
     */    
    public XanbooResultSet getSystemParam(XanbooAdminPrincipal xCaller, String param) throws RemoteException, XanbooException;
    
    
    
    /**
     * Creates a XanbooPrincipal object for a user by external user ID.
     * External user IDs are set at user creation time, and can be used by an external system as a convenient way to link user accounts to xanboo.<br>
     * Note that no exception will be thrown if the user is deactivated/disabled. If the user exists, a principal will be returned.
     *
     * @param domainId domain identifier for the user
     * @param extUserId The external user Id to authenticate
     *
     * @return A XanbooPrincipal object for the authenticated user
     * @throws XanbooException if the external userID is invalid.
     *
     */    
    public XanbooPrincipal getXanbooPrincipal(XanbooAdminPrincipal xCaller, String domainId, String extUserId) throws RemoteException, XanbooException;
    

    
    /**
     * Permanently deletes a device from a customer's account.
     * <p>
     * This method is used to completely remove a device, and all of it's associated data (including event logs) from the system. Once a device is deleted, it cannot be restored - it does not
     * appear in the accounts wastebasket section.
     * <br>
     * The deletion of a gateway also involves the deletion of all of its sub devices, and their associated data.
     * </p>
     * <p>
     * <b>Note. The SDK only currently allows the deletion of a gateway and all of it's associated devices, so all calls to this method must have a device GUID of 0 (zero).</b>
     * </p>
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param accountId The account of the gateway to be deleted
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier to get the device information for.
     *
     * @throws XanbooException
     *
     */    
    public void deleteDevice(XanbooAdminPrincipal xCaller, long accountId, String gatewayGUID, String deviceGUID) throws RemoteException, XanbooException;
    
    
    
    /**
     * Completely deletes an account from the Xanboo system including the removal of the account's data directory.
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param accountId The ID of the account to delete.
     *
     * @throws XanbooException if the operation failed.
     *
     */    
    public void deleteAccount(XanbooAdminPrincipal xCaller, long accountId) throws RemoteException, XanbooException;
    
    
    
    /**
     *  Retuns either a list of all loaded device descriptors, or managed objects of a particular descriptor
     *  @param xCaller a XanbooAdminPrincipal object that identifies the caller
     *  @param catalogId the catalog id for the descriptor. If null, list of all loaded descriptors will
     *      be returned.
     *  @return A XanbooResultSet of system parameters.
     *
     * @deprecated  replaced by {@link #getXanbooCatalog()}
     */    
    public XanbooResultSet getDeviceDescriptor(XanbooAdminPrincipal xCaller, String catalogId) throws RemoteException, XanbooException;
    
    
    
   /**
     * Returns event log entries for a gateway or device instance of an account.
     * Every time an event occurs, the occurance is logged. These logs remain accessible until cleared, either through an SDK call, or some external maintenance script.
     * <br>
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> CATALOG_ID </li>
     *  <li> DATE_RECEIVED </li>
     *  <li> SOURCE_LABEL </li>
     *  <li> CATEGORY </li>
     *  <li> GATEWAY_GUID </li>
     *  <li> LABEL </li>
     *  <li> DEVICE_GUID </li>
     *  <li> DATE_OCCURED </li>
     *  <li> EVENTLOG_ID </li>
     *  <li> EVENT_ID </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param accountId The account id of the eventlogs to be retrieved
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier of the managed object. If null,
     *                   all event log entries for the entire gateway and all of it's assocaited devices is returned.
     * @param eventId the event identifier to retrieve the log entries for. If null,
     *                all events log records for the specified gateway/device are returned.
     *
     * @return an XanbooResultSet which contains a HashMap list of event logs
     * @throws XanbooException
     *
     */     
    public XanbooResultSet getDeviceEventLog(XanbooAdminPrincipal xCaller, long accountId, String gatewayGUID, String deviceGUID, String eventId) throws RemoteException, XanbooException;
    
    
    /**
     * Clears event log entries for a gateway or device instance of an account
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param accountId The account id of the eventlogs to be deleted
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier of the managed object. If null,
     *                   all event log entries for the whole gateway are cleared.
     * @param eventId the event identifier to delete the log entries for. If null,
     *                all events log records for the specified gateway/device are cleared.
     *
     * @throws XanbooException
     *
     */     
    public void clearDeviceEventLog(XanbooAdminPrincipal xCaller, long accountId,	String gatewayGUID,	String deviceGUID, String eventId) throws RemoteException, XanbooException;

    
    
    /**
     * Authenticates an admin user with given admin id and password.<br>
     * @param adminId Username of the admin user
     * @param password Password of the admin user
     *
     * @return A XanbooAdminPrincipal object for the authenticated admin user.
     * @throws XanbooException
     *         
     */       
    public XanbooAdminPrincipal authenticateAdmin(String adminId, String password) throws RemoteException, XanbooException;

      /**
     * Authenticates an admin user with given admin id <br>
     * @param adminId Username of the admin user
     * @return A XanbooAdminPrincipal object for the authenticated admin user.
     * @throws XanbooException
     *         
     */       
    public XanbooAdminPrincipal authenticateAdmin(String adminId) throws RemoteException, XanbooException;

    
    
    /**
     * Updates/resets admin user passwords
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param adminId Id of the admin user
     * @param password New password for the admin user
     *
     * @throws XanbooException
     */     
    public void updateAdmin(XanbooAdminPrincipal xCaller, String adminId, String password) throws RemoteException, XanbooException;
    
    /**
     * Updates/resets admin user passwords
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param adminId Id of the admin user
     * @param password New password for the admin user
     *
     * @throws XanbooException
     */     
    public void updateAdmin(XanbooAdminPrincipal xCaller, String adminId, String password, int roleId, int adminLevel, String domain) throws RemoteException, XanbooException;    
    
    
   /**
     * @deprecated  replaced by {@link #getProfileTypeList()}
     */
    public XanbooResultSet getProfileTypeList(XanbooAdminPrincipal xCaller) throws RemoteException, XanbooException;

   /**
     * Retrieves a list of profile types.<br>
     * <p> 
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> PROFILETYPE_ID </li>
     *  <li> DESCRIPTION </li>
     *  <li> SUFFIX </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     * </p>
     * @return an XanbooResultSet which contains a HashMap list of profile types
     * @throws XanbooException
     *
     */
    public XanbooResultSet getProfileTypeList() throws RemoteException, XanbooException;

    
    /**
     * Retrieves a catalog of all device descriptors loaded in the system.
     * <br>
     * The application should cache the returned XanbooCatalog entry to avoid multiple calls to this method.
     *
     * @param xCaller A XanbooAdminPrincipal object that identifies the caller
     *
     * @return a XanbooCatalog which contains object representations of all loaded device descriptors.
     * @throws XanbooException
     */
    public XanbooCatalog getXanbooCatalog() throws RemoteException, XanbooException;

    /**
     * Retrieves the requested domain (specified by the domainId parameter). 
     * @param domainId
     * @return : The same columns returned by {@link getDomainList()} for the requested domainId
     * @throws XanbooException 
     */
    public java.util.HashMap getDomain(String domainId)throws RemoteException,XanbooException;
    
   /**
     * @deprecated  replaced by {@link #getDomainList()}
     */
    public XanbooResultSet getDomainList(XanbooAdminPrincipal xCaller) throws RemoteException, XanbooException;

    
    /**
     * Retrieves a list of domains configured for this SDK installation.
     *
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> DOMAIN_ID</li>
     *  <li> DESCRIPTION </li>
     *  <li> BRAND </li>
     *  <li> NOTFROM </li>
     *  <li> DEFAULT_TZ </li>
     *  <li> DEFAULT_COUNTRY </li>
     *  <li> DEFAULT_SUBS_FLAGS </li>
     *  <li> DEFAULT_DISK_QUOTA </li>
     *  <li> DEFAULT_USER_QUOTA </li>
     *  <li> DEFAULT_GWY_QUOTA </li>
     *  <li> SELFINSTALL_ISPROVISIONED </li>
     *  <li> TECHINSTALL_ISPROVISIONED </li>
     *  <li> MAX_LOGIN_ATTEMPTS</li>
     *  <li> CURRENCY</li>
     *  <li> ENERGY_RATE</li>
     *  <li> LANGUAGE_ID</li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @return a XanbooResultSet containing a list of unique domains
     */    
    public XanbooResultSet getDomainList() throws RemoteException, XanbooException;

    
    /**
     * Retrieves a list of domain message templates configured for this SDK installation.<br>
     * <p>
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> DOMAIN_ID</li>
     *  <li> TEMPLATETYPE_ID</li>
     *  <li> PROFILETYPE_ID</li>
     *  <li> LANGUAGE_ID</li>
     *  <li> MESSAGE_TMPL</li>
     *  <li> SUBJECT_TMPL </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     * </p>
     * <br>
     * The TEMPLATETYPE_ID identifies what kind of message template the current record is. Valid template type ids are
     * 0(Device Notification Messages), 1(Invitation Messages), 2(Disk Quota warning messages), 3(Notification Quota warning messages)
     * 4(Account notification Messages), 5(Test Notification Messages)
     * @return a XanbooResultSet containing a list of unique domains
     */    
    public XanbooResultSet getDomainTemplateList() throws RemoteException, XanbooException;

    /**
     * Retrieves the list of supported device classes (class+subclass combinations) for a domain and of given installation type
     * and/or monitoring type (i.e professional/self/non monitoring). 
     *
     * @param domainId the domain identifier to query for
     * @param installType 1-char installation type to query for. Must be one of 'S' (Self-Install), 'P' (Professional-install). If null, all supported devices for the domain will be returned.
     * @param monType 1-char monitoring type to query for. Must be one of 'S' (Self-Monitoring), 'P' (Professional-Monitoring), 'N' (Non-Monitoring). If null, all supported devices for the domain will be returned.
     * <p>
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> VENDOR_ID</li>
     *  <li> CLASS_ID</li>
     *  <li> SUBCLASS_ID</li>
     *  <li> ENABLED_SELFINSTALL</li>
     *  <li> ENABLED_PM</li>
     *  <li> ENABLED_SM</li>
     *  <li> ENABLED_NM</li>
     * </UL>
     * </p>
     * <br>
     * @return a XanbooResultSet containing a list of supported device classes (class_id and subclass_id combinations)
   */  
    
    public XanbooResultSet getSupportedDeviceList(String domainId, String installType, String monType) throws RemoteException, XanbooException;
    
    
    /**
     * Creates a new gateway subscription for a given account.
     * <p>
     * This method is used to add a new gateway subscription record with all relevant subscription information, including
     * subscription id, status, associated gateway identifiers, gateway label, timezone, security pins for the master user,
     * subscription address and emergency contacts
     * </p>
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param accountId The account to associate the subscription with
     * @param subsId a unique subscription identifier (e.g. a CTN)
     * @param subsFlags a positive integer value for service subscription flags (see controller descriptor oid 40 definition). Will be set, if >0.
     * @param hwId a unique gateway hardware identifier to be used with the subscription (e.g. IMEI or MAC)
     * @param label default name to be given to the associated gateway
     * @param tzName default t?mezone name (Olson) to be assigned to the associated gateway
     * @param masterPin optional security disarm pin for the controller (master user)
     * @param masterDuress optional security duress pin for the controller (master user)
     * @param alarmPass optional security alarm password/code for verbal professional monitoring verifications
     * @param subsInfo a XanbooContact object to pass subscription address information (only name and address fields are used)
     * @param emergencyContacts an array of XanbooNotificationProfile objects to be used as emergency contacts for the subscription. Max 5 entries allowed.
     * @param verifySubs a boolean to enable/disable subscription verifications, if applicable
     *
     * @throws XanbooException
     *
     * @deprecated  replaced by {@link #newSubscription(XanbooAdminPrincipal xCaller, XanbooSubscription xSub, boolean verifySubs)}
     */
    public void newSubscription(XanbooAdminPrincipal xCaller, long accountId, String extAccountId, String cLastname, String cFirstname, String subsId, int subsFlags,
            String hwId, String label, String tzName, String masterPin, String masterDuress, String alarmPass,
             XanbooContact subsInfo, XanbooNotificationProfile[] emergencyContacts, boolean verifySubs) throws RemoteException, XanbooException;

   
    /**
     * Creates a new subscription for a given account.
     * <p>
     * This method is used to add a new subscription record with all relevant subscription information, including
     * subscription id, feature flags, status, associated gateway identifiers, gateway label, timezone, security pins for the master user,
     * subscription address and emergency contacts
     * </p>
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param xSub a valid XanbooSubscription object with subscription details to be created
     * @param verifySubs a boolean to enable/disable subscription verifications, if applicable
     *
     * @throws XanbooException
     *
     */
    public void newSubscription(XanbooAdminPrincipal xCaller, XanbooSubscription xSub, boolean verifySubs) throws RemoteException, XanbooException;
   

    /**
     * Updates an existing gateway subscription for a given account.
     * <p>
     * This method is used to update an existing gateway subscription record, including subscription status, associated gateway identifiers,
     * gateway label, timezone, security pins for the master user, subscription address and emergency contacts
     * </p>
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param accountId The owner account for the subscription to be updated
     * @param subsId unique subscription identifier to be updated (e.g. a CTN). Must be specified.
     * @param hwId a unique gateway hardware identifier to to be updated (e.g. IMEI or MAC).  Must be specified.
     * @param subsFlags a positive integer value for service subscription flags (see controller descriptor oid 40 definition).
     * @param hwIdNew a new gateway hardware identifier to update the subscription with. Will be updated, if not null.
     * @param label default name to be given to the associated gateway. Will be updated, if not null.
     * @param tzName default t?mezone name (Olson) to be assigned to the associated gateway. Will be updated, if not null.
     * @param masterPin optional security disarm pin for the controller (master user). Will be updated, if not null.
     * @param masterDuress optional security duress pin for the controller (master user). Will be updated, if not null.
     * @param alarmPass optional security alarm password/code for verbal professional monitoring verifications. Will be updated, if not null.
     * @param subsInfo a XanbooContact object to pass subscription address information (only name and address fields are used). For name updates, both first and lastname needs to be non-null.
     *                 For address updates, either address1 or zip field must be non-null. All address fields including null values will be updated.
     * @param alarmDelay to enable/disable SBN alarm delay timer for the installation. 0 will disable, 1 will enable alarm delay timer. Timer will not be updated if -1 passed.
     * @param tcFlag to set the terms and conditions flag. A zero value indicates the flag is not set, a positive value indicates that it is. Will not be updated if negative value passed. A zero value indicates the flag is not set, a positive value indicates that it is.
     *
     * @throws XanbooException
     *
     * @deprecated  replaced by {@link #newSubscription(XanbooAdminPrincipal xCaller, XanbooSubscription xSub, boolean verifySubs)}
     */
   public void updateSubscription(XanbooAdminPrincipal xCaller, long accountId, String subsId, String hwId, int subsFlags, String hwIdNew, String label, String tzName,
           String masterPin, String masterDuress, String alarmPass, XanbooContact subsInfo, int alarmDelay, int tcFlag) throws RemoteException, XanbooException;


    /**
     * Updates an existing gateway subscription for a given account.
     * <p>
     * This method is used to update an existing gateway subscription record, including subscription status, feature flags, associated gateway identifiers,
     * gateway label, timezone, security pins for the master user, subscription address and emergency contacts
     * </p>
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param xSub a valid XanbooSubscription object with subscription details to be updated. The account id, subscription id and hardware id attributes are all required to identify a particular subscription to update.
     *             Only subsFlags, label, tzone, pin, alarm passcode, contact attributes with non-null values will be updated
     * @param hwIdNew a new gateway hardware identifier to update the subscription with. Will be updated, if not null.
     * @param alarmDelay to enable/disable SBN alarm delay timer for the installation. 0 will disable, 1 will enable alarm delay timer. Timer will not be updated if -1 passed.
     * @param tcFlag to set the terms and conditions flag. A zero value indicates the flag is not set, a positive value indicates that it is. Will not be updated if negative value passed. A zero value indicates the flag is not set, a positive value indicates that it is.
     *
     * @throws XanbooException
     */
    public void updateSubscription(XanbooAdminPrincipal xCaller, XanbooSubscription xSub, String hwIdNew, int alarmDelay, int tcFlag) throws RemoteException, XanbooException;
    
    /**
     * Cancels an existing gateway subscription for a given account.
     * <p>
     * This method is used to cancel an existing gateway subscription record
     * </p>
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param accountId The owner account for the subscription to be cancelled. A value of -1 will cancel a subscription by extAccountId specified.
     * @param extAccountId the external accountId for the subscription to be cancelled. Either accountId or extAccountId must be specified!
     * @param subsId unique subscription identifier to be cancelled (e.g. a CTN). Must be specified.
     * @param hwId a unique gateway hardware identifier to to be cancelled (e.g. IMEI or MAC).  Must be specified.
     *
     * @throws XanbooException
     *
     */
    public void cancelSubscription(XanbooAdminPrincipal xCaller, long accountId, String extAccountId, String subsId, String hwId) throws RemoteException, XanbooException;


    /**
     * Retrieve existing gateway subscriptions for a given account.
     * <p>
     * This method is used to retrieve existing gateway subscription records for an account, and optionally for a given subscription and hardware id
     * </p>
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param accountId The owner account for the subscription to be retrieved
     * @param subsId unique subscription identifier to be retrieved (e.g. CTN). If null, all subscriptions for the account will be returned
     * @param hwId a unique gateway hardware identifier for the subscription (e.g. IMEI or MAC). If null, all subscriptions will be returned
     * <p>
     * <b> Some of the columns returned: </b>
     * <UL>
     *  <li> SUBS_ID</li>
     *  <li> HWID</li>
     *  <li> GATEWAY_GUID</li>
     *  <li> SUBS_FLAGS</li>
     *  <li> SUBS_FEATURES</li>
     *  <li> FIRSTNAME</li>
     *  <li> LASTNAME </li>
     *  <li> ZIP</li>
     *  <li> TC_FLAG</li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     * </p>
     * GATEWAY_GUID column provides the physical gateway instance the subscription is associated with. A GATEWAY_GUID of null indicates that the subscription
     * is pending gateway activation/installation.
     * <br>
     * 
     * @deprecated  replaced by {@link #getSubscription(XanbooAdminPrincipal xCaller, long accountId, String subsId)}
     */
    public XanbooResultSet getSubscription(XanbooAdminPrincipal xCaller, long accountId, String subsId, String hwId) throws RemoteException,XanbooException;


    
    /**
     * Retrieve existing subscription(s) for a given account.
     * <p>
     * This method is used to retrieve existing gateway subscription records for the calling account, and optionally for a given subscription id
     * 
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param accountId The owner account for the subscription to be retrieved
     * @param subsId unique subscription identifier to be retrieved. If null, all subscriptions for the account will be returned
     * 
     * @return an array of XanbooSubscription objects or null
     * @throws XanbooException
     *
     * @see com.xanboo.core.sdk.account.XanbooSubscription
     * <br>
    */
    public XanbooSubscription[] getSubscription(XanbooAdminPrincipal xCaller, long accountId, String subsId) throws RemoteException, XanbooException;
    
    /**
     * Returns information for subscriptions/accounts matching the search criteria. 
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param bMarket the business market to which the subscription/account belongs
     * @param state the state where the customer (subscription) is located
     * @param city the city where the customer (subscription) is located
     * @param postalCode identifies the zip code for the customer 
     * @return an instance of XanbooResultSet containing the following fields<br/>
     * <ul>
     *  <li>ACCOUNT_ID</li>
     *  <li>GATEWAY_GUID</li>
     *  <li>SUBS_ID</li>
     *  <li>LASTNAME</li>
     *  <li>FIRSTNAME</li>
     *  <li>BMARKET</li>
     *  <li>CITY</li>
     *  <li>STATE</li>
     *  <li>ZIP</li>
     * </ul>
     * @throws XanbooException 
     */
    public XanbooResultSet getSubscription(XanbooAdminPrincipal xCaller, String bMarket, String state, String city, String postalCode)throws RemoteException,XanbooException;
     /**
     * Creates a XanbooPrincipal object by account or subscription id.
     * Either an account ID or a subsId/hwId pair must be used for the call
     * Note that no exception will be thrown if the user is deactivated/disabled. If the user exists, a principal will be returned.
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param accountId for the principal to be created. If -1 specified, a subsId/hwId must be specified.
     * @param subsId subscription identifier (e.g. CTN). If null, accountId parameter must be specified.
     * @param hwId subscription hardware identifier (e.g. IMEI or MAC).
     *
     * @return A XanbooPrincipal object for account/subscription master user
     *
     * @throws XanbooException if the account or subscription ID is invalid.
     *
     */
     public XanbooPrincipal getXanbooPrincipal(XanbooAdminPrincipal xCaller, long accountId, String subsId, String hwId) throws RemoteException,XanbooException;

     
     
     /**
     * Creates an audit log entry for administrative actions on accounts and children gateways/devices
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param accountId Required account id the audit action is executed on.
     * @param gatewayGUID Gateway GUID the audit action is executed on. Null, if no gateway guid is applicable.
     * @param deviceGUID Device identifier the audit action is executed on. Null, if no device guid is applicable.
     * 
     * @param actionSource Audit source string that identifies the application generating the audit log. Null, if not applicable.
     * @param actionDesc A required audit action description text to be logged.
     *
     * @throws XanbooException
     *
     */
    public void auditLog(XanbooAdminPrincipal xCaller, long accountId, String gatewayGUID, String deviceGUID, String actionSource, String actionDesc, String actionValue) throws RemoteException, XanbooException;

    
     /**
     * Retrieves a list of location codes for a given domain. Location codes are maintained as a multi-level list of alphanumeric codes (up to 4-chars) with 
     * corresponding location descriptions. Levels are defined as positive integers, lower numbers representing higher levels. A location code entry may optionally
     * have a parent location code assigned to make it applicable to that parent location code only. Typically, level 1 is used for codes for 'Floor' codes, level 2 is for 'Room' 
     * codes and level 3 is used for egress/device code entries. The special location code value "0" is defined as "unassigned" locations.
     *
     * @param domainId domain identifier for the location code list
     * @param lang language identifier for the location code descriptions. If null, default 'en' descriptions will be returned
     * @param level location level for the list to be returned. If -1, all levels will be returned
     * 
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> LOC_LEVEL</li>
     *  <li> LOC_CODE</li>
     *  <li> DESCRIPTION </li>
     *  <li> PARENT_LOC_CODE</li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @return a XanbooResultSet containing a list of location code definitions for the specified domain and optionally level.
     */    
   public XanbooResultSet getLocationCodeList(String domainId, String lang, int level) throws RemoteException, XanbooException;
   
   
     /**
     * Adds one or more provisioned/pending device(a) to a particular subscription.
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param accountId parent account to add the provisioned device record to.
     * @param subsId subscription identifier (e.g. CTN) to add the provisioned device record to.
     * @param hwId subscription hardware identifier (e.g. IMEI or Serial) to add the provisioned device record to.
     * @param classId 4-digit deviceclass-id to identify the type/class of device to be provisioned.
     * @param subclassId 2-digit device subclass-id to identify the type/class of device to be provisioned. 
     * @param installType 1-char installation type. Must be one of 'S' (Self-Install), 'P' (Professional-install), or 'A' (Al)
     * @param swapGuid An optional device guid to be swapped with the provisioned device. If specified, must be an existing device guid for the specified subscription.
     *                 NOTE: For a given provisioned class/subclass, only one swap dguid can be specified at any given time.
     * @param count the number of provisioned devices of specified class/subclass to be added.
     *
     * @throws XanbooException if the operation fails.
     *
     */
   public void addProvisionedDevice(XanbooAdminPrincipal xCaller, long accountId, String subsId, String hwId, String classId, String subclassId, String installType, String swapGuid, int count) throws RemoteException, XanbooException;
   
   
     /**
     * Removes one or more provisioned/pending device(s) from a particular subscription.
     *
     * @param accountId parent account to remove the provisioned device record from.
     * @param subsId subscription identifier (e.g. CTN) to add the provisioned device record from.
     * @param hwId subscription hardware identifier (e.g. IMEI or Serial) to add the provisioned device record from.
     * @param classId 4-digit device class-id to identify the type/class of provisioned device to be removed.
     * @param subclassId 2-digit device subclass-id to identify the type/class of provisioned device to be removed. 
     * @param installType 1-char installation type. Must be one of 'S' (Self-Install), 'P' (Professional-install), or 'A' (Al)
     * @param swapGuid An optional device guid to be swapped with the provisioned device. If specified, the matching swap record will be removed.
     * @param count the number of provisioned devices of specified class/subclass to be removed.
     *
     * @throws XanbooException if the operation fails.
     *
     */
   public void deleteProvisionedDevice(XanbooAdminPrincipal xCaller, long accountId, String subsId, String hwId, String classId, String subclassId, String installType, String swapGuid, int count) throws RemoteException, XanbooException;
 
   
     /**
     * Retrieves the list of provisioned/pending device(s) for a particular account or subscription.
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param accountId parent account to query the provisioned device record(s) for.
     * @param subsId subscription identifier (e.g. CTN) to query the provisioned device record(s) for. If null, all records for the given account will be returned.
     * @param hwId subscription hardware identifier (e.g. IMEI or Serial) to query the provisioned device record(s) for. If subId is not null, hwId must also be specified to identify the subscription. If subId is null, this parameter is assumed null.
     * @param classId optional 4-digit device class-id to specify the type/class of provisioned device to be queried. If null, all classes will be returned. If subId is null, this parameter is assumed null.
     * @param subclassId optional 2-digit device subclass-id to identify the type/class of provisioned device to be queried. If null, all subclasses will be returned. If subId is null, this parameter is assumed null.
     * @param installType 1-char installation type to query for. Must be one of 'S' (Self-Install), 'P' (Professional-install), or 'A' (Al). If null, all installation type records will be returned.
     * 
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
   public XanbooResultSet getProvisionedDeviceList(XanbooAdminPrincipal xCaller, long accountId, String subsId, String hwId, String classId, String subclassId, String installType) throws RemoteException, XanbooException;
   
 
     /**
     * Returns the number of alarms for a given account and gateway.
     *
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param accountId  account to query the alarm count for.
     * @param gatewayGUID gateway GUID to query the alarm count for.
     *
     * @return count total number of alarms 
     */    
   public int getAlertCount(XanbooAdminPrincipal xCaller, long accountId, String gatewayGUID) throws RemoteException, XanbooException;


   /**
    * Retuns the details of a list alarm events
    * @param xCaller a XanbooAdminPrincipal object that identifies the caller
    * @param accountId  The account id of the event
    * @param archiveId The archive id of the event
    * @param extAccountId The external account id of the event
    * @param subsIdOrGguId Can be the subscription id or gguid
    * @param egrpId The event group id
    * @param startTS The starting timestamp
    * @param endTS The ending timestamp
    * @throws XanbooException
    */
   public XanbooResultSet getAlarmArchiveItem(XanbooAdminPrincipal xap, long accountId, long archiveId, String extAccountId, String subsIdOrGguId, String egrpId, String startTS, String endTS) throws RemoteException, XanbooException;


   /**
    * Updates the view count and last date viewd of an alarm event
    * @param xCaller a XanbooAdminPrincipal object that identifies the caller
    * @param archiveId The archive id of the event
    * @throws XanbooException
    */
   public void updateAlarmArchiveItem(XanbooAdminPrincipal xap, long archiveId) throws RemoteException, XanbooException;

   /**
    * Returns the notification opt-in status of the specified input.  If both profileAddress and token
    * are provided, both will be used in the query.  If only one of the inputs is provided, then
    * that input will be used in the query
    * @param xCaller
    * @param notificationAddress
    * @param token
    * @return the notification opt-in status or null if not found
    * @throws XanbooException
    */
   public Integer getNotificationOptInStatus(XanbooAdminPrincipal xCaller, String notificationAddress, String token) throws RemoteException, XanbooException;

   /**
    * Returns a Map of notification addresss and associated opt in status value for the specified input List.
    * @param xCaller
    * @param notificationAddress
    * @return
    * @throws XanbooException
    */
   public Map<String, Integer> getNotificationOptInStatus(XanbooAdminPrincipal xCaller, List<String> notificationAddress) throws RemoteException, XanbooException;

   /**
    * Sets the opt-in status with the specified value.  Either the token or (notification address and domain id of
    * the xCaller) are used to query the opt-in status.  If the opt-in status is not found, it will be created.
    * The profile type ID can be included in the notification address
    * For example 0%4045551212 (0 is the profile type ID, % is the separator
    * @param xCaller
    * @param notificationAddress
    * @param token
    * @param status
    * @throws XanbooException
    */
   public void setNotificationOptInStatus(XanbooAdminPrincipal xCaller, String notificationAddress, String token, int status) throws RemoteException, XanbooException;

   /**
    * Sets the opt-in status for each notification address in notificationAddresses.  The profile type ID can be included in the notification address
    * For example 0%4045551212 (0 is the profile type ID, % is the separator
    * @param xCaller
    * @param notificationAddresses  Map containing notification address - value pairs
    * @throws XanbooException
    */
   public void setNotificationOptInStatus(XanbooAdminPrincipal xCaller, Map<String, Integer> notificationAddresses) throws RemoteException, XanbooException;
   
   public XanbooResultSet getDomainLicenses(XanbooAdminPrincipal xCaller, String domainId) throws RemoteException,XanbooException;
   
   
    /**
     * Creates an audit log entry for administrative logout action on accounts and children gateways/devices
     *
     * @param xap a XanbooAdminPrincipal object that identifies the caller
     * @param accountId Required account id the audit action is executed on.
     * @param gatewayGUID Gateway GUID the audit action is executed on. Null, if no gateway guid is applicable.
     * @param deviceGUID Device identifier the audit action is executed on. Null, if no device guid is applicable.
     * 
     * @param actionSource Audit source string that identifies the application generating the audit log. Null, if not applicable.
     * @param actionDesc A required audit action description text to be logged.
     * @param actionValue
     * @throws XanbooException
     *
     */
    public void logoutAdmin(XanbooAdminPrincipal xap, long accountId, String gatewayGUID, String deviceGUID, String actionSource, String actionDesc, String actionValue) throws RemoteException,XanbooException;
    /**
     * Creates an audit log entry for new administrative action on accounts and children gateways/devices
     *
     * @param xap a XanbooAdminPrincipal object that identifies the caller
     * @param accountId Required account id the audit action is executed on.
     * @param gatewayGUID Gateway GUID the audit action is executed on. Null, if no gateway guid is applicable.
     * @param deviceGUID Device identifier the audit action is executed on. Null, if no device guid is applicable.
     * 
     * @param actionSource Audit source string that identifies the application generating the audit log. Null, if not applicable.
     * @param actionDesc A required audit action description text to be logged.
     * @param actionValue
     * @throws XanbooException
     *
     */
    public void newAdmin(XanbooAdminPrincipal xap, long accountId, String gatewayGUID, String deviceGUID, String actionSource, String actionDesc, String actionValue) throws RemoteException,XanbooException;
    
    /**
     * Creates an audit log entry for delete administrative user on accounts and children gateways/devices
     *
     * @param xap a XanbooAdminPrincipal object that identifies the caller
     * @param accountId Required account id the audit action is executed on.
     * @param gatewayGUID Gateway GUID the audit action is executed on. Null, if no gateway guid is applicable.
     * @param deviceGUID Device identifier the audit action is executed on. Null, if no device guid is applicable.
     * 
     * @param actionSource Audit source string that identifies the application generating the audit log. Null, if not applicable.
     * @param actionDesc A required audit action description text to be logged.
     * @param actionValue
     * @throws XanbooException
     *
     */
    public void deleteAdmin(XanbooAdminPrincipal xap, long accountId, String gatewayGUID, String deviceGUID, String actionSource, String actionDesc, String actionValue) throws RemoteException,XanbooException;
   
    /**
     * Retrieve subscription feature based on caller domain and mapping code
     * 
     * @param xCaller
     * @param mappingCodes
     * @return a List of features and mapping codes enabled for the caller's domain
     * @throws XanbooException 
     */
    public List<SubscriptionFeature> getDomainFeatureList(XanbooAdminPrincipal xCaller,List<String> mappingCodes)throws RemoteException, XanbooException;
   
    /**
     * Retrieve subscription feature based on domain and mapping code
     * 
     * @param domainId - domain id values used to filtered result.
     * @param mappingCode - mapping code If the mappingCode parameter  is null, all feature records for that domain. 
     * @return a List of features enabled for the domain
     * @throws XanbooException
     */
    public List<SubscriptionFeature> getDomainFeatureList(String domainId, List<String> mappingCode)throws RemoteException,XanbooException;

	
    /**
     * Adds a new subscription feature to an existing subscription
     * @param xCaller
     * @param accountId
     * @param subsId
     * @param hw_Id
     * @param lstAddSubFeatures
     * @param lstRemoveSubFeatures
     * @return
     * @throws XanbooException
     */
    public String addRemoveSubscriptionFeature(XanbooAdminPrincipal xCaller, XanbooSubscription xsub, List<SubscriptionFeature> featuresToAdd,List<SubscriptionFeature> featuresToRemove)throws RemoteException,XanbooException;


    /**
     * Returns a device model record of given language and model id, or all model records
     * @param xap
     * @param languageId Language Id to query.  If null, default will be english ("en")
     * @param modelId Model Id to query.  If null, all device models will be returned
     * @return The device model list
     * @throws XanbooException
     */
    public XanbooResultSet getDeviceModel(XanbooAdminPrincipal xap, String languageId, String modelId) throws XanbooException, RemoteException;
    
    /**
     * Returns subscriptions of matching market code, state, city, or postal code parameters
     * 
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param bMarket Identifies the business market
     * @param state - the two character state abbreviation where the subscription is located
     * @param city - the city where the subscription is located
     * @param postalCode
     * @param prefBit optional filter to return results where the customer has subscribed. 
     * @return
     * @throws XanbooException
     */
    public XanbooResultSet getSubscriptions(XanbooAdminPrincipal xCaller, String bMarket, String state, String city, String postalCode, Integer prefBit)throws RemoteException, XanbooException;
    
    /**
     * Retrieves the system dictionary descriptor/catalog 
     * <br>
     * The application should cache the returned XanbooCatalog entry to avoid multiple calls to this method.
     * @return a XanbooCatalog object that contains object representations of all mobject and definitions in the dictionary descriptor/catalog
     * @throws XanbooException
     */
    public XanbooCatalog getXanbooDictionaryCatalog() throws XanbooException, RemoteException;

    /**
     * sync sbn for a given account, hwid and subsid.
     * <p>
     * This method is used to update/create an sbn with ,
     * </p>
     * @param xCaller a XanbooAdminPrincipal object that identifies the caller
     * @param xSub a valid XanbooSubscription object with subscription details to be updated. The account id, subscription id and hardware id attributes are all required to identify a particular subscription to update.
     * @throws XanbooException
     */
    public void syncSbn(XanbooAdminPrincipal xCaller, XanbooSubscription xSub) throws RemoteException, XanbooException;
    
    /**
     * Method to return information about a specific device from DL Core database
     * @param xCaller
     * @param gguid - The gateway for the device
     * @param dguid - Specifies the device
     * @return - An instance of XanbooResultSet containing the attributes of the specified device
     * @throws RemoteException
     * @throws XanbooException 
     */
    public XanbooResultSet getDeviceListByGGuid(XanbooAdminPrincipal xCaller,String gguid,String dguid)throws RemoteException,XanbooException;
}
