/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/device/DeviceManager.java,v $
 * $Id: DeviceManager.java,v 1.33 2011/06/27 17:06:31 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.device;

import java.rmi.RemoteException;
import java.util.HashMap;

import com.xanboo.core.model.XanbooBinaryContent;
import com.xanboo.core.model.XanbooGateway;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.XanbooException;

/**
 * <p>Remote Interface for the DeviceManagerEJB</p>
 *
 * <b>Glossary</b>
 * <ul>
 * <li> <b>GATEWAY_GUID</b> <br>
 *          A globally unique gateway identifier.
 *
 * <li> <b>DEVICE_GUID</b> <br>
 *          A device identifier unique within its parent gateway domain.
 *
 * <li> <b>Event</b> <br>
 *          Defined in the device descriptor, a device may have any number of events to which notifications may be applied. <br>
 *          An example of an event would be a motion sensor being triggered, or a thermostat temperature crossing a set threshold.
 * </li>
 *  <li> <b>Notification</b> <br>
 *          A message delivered to a location (such as an email address or pager) upon the occurance of an event. A notification is a virtual concept and is implemented
 *          by assigning notification profiles to events with the use of notification actions.
 * </li>
 *  <li> <b>Notification Profile</b> <br>
 *          A destination address stored in the system to which a message may be sent upon the occurance of an event. Notification profiles are setup within an account using methods
 *          that can be found in <CODE>AccountManager</CODE>. <br>
 *          The SDK supports multiple types of target address, including pager numbers and email addresses.
 * </li>
 * <li> <b>Notification Action</b> <br>
 *          The association of a notification profile with an event is called a notification action. A notification action states
 *          'When event X takes place, send a message to destination Y' (Event X is defined in the descriptor, Y is an already existing notification profile)
 * </li>
 * <li> <b>Managed Object</b> <br>
 *          A managed object is an addressable entitiy that exists within a device. A managed object ID may often be referred to as OID. <br>
 *          Managed Objects
 *          <ul>
 *              <li> Have a type that is one of<br>
 *                  <ul>
 * <li> int<br>
 * An integer value defined with optional attributes of start, end, step - eg. <br>
 * "int:0,20,5" represents the nubmer 0-20 with a step of 5 (ie 5,10,15,20) <br>
 *                          "int" specifies an integer of any value
 * </li>
 * <li> String <br>
 *                          A string value defined with optional max length attribute. eg. <br>
 * "String:128" specifies a string with a maximum lengh of 128 characters
 *                          "String" specifies a string with no boundaries.
 * </li>
 *                      <li> enum (Enumeration) <br>
 *                          A list of possible string values, one of which may be valid at any time. eg <br>
 * "enum:image,video" specifies an object whose value choices are 'image' or 'video'.<br>
 *                          "enum:reset" specifies an object to which only one value may be sent - 'reset'.
 *                      </li>
 * <li>Binary (binary data) <br>
 * Binary data ususally consists of an image or a video clip, but is not restricted to these
 * </li>
 *                  </ul>
 *              </li>
 *              <li> Have an Access level specified from the SDK perspective, which is one of:<br>
 *                  <ul>
 * <li> 0 <br>
 * None - this specifies that no access of this object is permitted. Used for internal and system purposes.
 * </li>
 * <li> 1 <br>
 * Write Only. For example, a reset button represented as a managed object would be write only, as you can trigger it, but never read it's value. Commonly used for 'one shot' commands.
 * </li>
 * <li> 2 <br>
 * Read only. For example, the current temperature of a room as detected by an electronic thermostat is a read only value.
 * </li>
 * <li> 3 <br>
 * Read Write. For example, the current thermostat setting on a thermostat may be changed, and it also has a current setting that can be read.
 * </li>
 *                  </ul>
 *              </li>
 *          </ul>
 *  </li>
 *  <li> <b> Device Status </b> <br>
 *      The status id of a device may be one of:
 *      <ul>
 *      <li> <b>0</b> <br>
 *      Online - means the device is active and can be communicated with
 *      </li>
 *      <li> <b>1</b> <br>
 *      Unknown - This will be used rarely, if at all. For example, if a gateway has only intermittant communication with a device
 *      </li>
 * <li> <b>2</b> <br>
 *      Offline - If a device or gateway is not active and cannot be communicated with, it is considered offline.
 *      </li>
 *      <li> <b>3</b> <br>
 *      Inactive - This status is specifically assigned to a device or gateway using the updateDeviceStatus SDK call. When inactive, a device cannot be controlled.
 *      </li>
 *      </ul>
 *  </li>
 *
 *  </ul>
 *
 * <br>
 * @see com.xanboo.core.sdk.account.AccountManagerEJB
 */

public interface DeviceManager   
{
    /**
     * Returns a list of all device classes known by the specified caller.
     * Devices in a particular class may have diffent descriptors - this returns a list of distinct classes, not descriptors within those classes.
     * Distinct device classes include Appliance/Gateways, Camera Managers and Thermostats.
     *<br>
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> CLASS_ID </li>
     *  <li> NAME </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     *
     * @return a XanbooResultSet which contains a HashMap list of device classes
     * @throws XanbooException
     *
     */   
    public XanbooResultSet getDeviceClassList(XanbooPrincipal xCaller) throws RemoteException, XanbooException;
    
    
    
    /**
     * Returns a list of all device classes known by the specified caller using the specified language.
     * Devices in a particular class may have diffent descriptors - this returns a list of distinct classes, not descriptors within those classes.
     * Distinct device classes include Appliance/Gateways, Camera Managers and Thermostats.
     *<br>
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> CLASS_ID </li>
     *  <li> NAME </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param lang the language id in which the class names will be returned.
     *
     * @return a XanbooResultSet which contains a HashMap list of device classes
     * @throws XanbooException
     *
     */    
    public XanbooResultSet getDeviceClassList(XanbooPrincipal xCaller, String languageId) throws RemoteException, XanbooException;
    
    
    
    /**
     * Returns detailed (instance+catalog) information for a specific device belonging to the caller.
     *<br>
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> CATALOG_ID </li>
     *  <li> STATUS_ID </li>
     *  <li> FW_VERSION </li>
     *  <li> SW_VERSION </li>
     *  <li> HW_SERIALNO </li>
     *  <li> GATEWAY_GUID </li>
     *  <li> SOURCE_ID </li>
     *  <li> LABEL </li>
     *  <li> CLASS_ID </li>
     *  <li> TIMEZONE_ID </li>
     *  <li> DEVICE_GUID </li>
     *  <li> HW_MODEL </li>
     *  <li> LAST_CONTACT </li>
     *  <li> PROVIDER_ID </li>
     *  <li> HW_GUID </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * Note that in the case of a gateway, the device guid is 0 (zero).
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier to get the device information for.
     *
     * @return a XanbooResultSet which contains a HashMap list of devices
     * @throws XanbooException
     *
     */    
    public XanbooResultSet getDeviceList(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID) throws RemoteException, XanbooException;
    
    
    
    /**
     * Returns list of devices of the specified class owned by the caller.
     * <p>
     * The class can be specified as either a 2 character major class or 4 character specific class Id.
     * For example, requesting class '0200' would list managed objects for devices instanciated with class '0200', whereas requesting '02' would list for all devices of major class '02' (0200, 0201....02XX)
     * </p>
s      *<br>
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> CATALOG_ID </li>
     *  <li> LABEL </li>
     *  <li> LAST_CONTACT </li>
     *  <li> STATUS_ID </li>
     *  <li> GATEWAY_GUID </li>
     *  <li> DEVICE_GUID </li>
     *  <li> CLASS_ID </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param dClass device class id for the query. If null, all devices are returned.
     *
     * @return a XanbooResultSet which contains a HashMap list of devices of the specified class
     * @throws XanbooException
     *
     */    
    public XanbooResultSet getDeviceListByClass(XanbooPrincipal xCaller, String dClass) throws RemoteException, XanbooException;
    
    
    
    /**
     * Updates status value for a specific device instance.
     * This can be used to activate and deactive devices. Note that deactivated devices will still be returned by getDeviceList methods, but the
     * STATUS_ID column will reflect the inactive status. An inactive device's state is frozen, and any communication attempt from the device is rejected
     * until that device is reactivated.
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier to update.
     * @param status The new status for the device - must be one of DeviceManagerEJB.DEVICE_STATUS_ACTIVE
     *        or DeviceManagerEJB.DEVICE_STATUS_INACTIVE
     *
     * @throws XanbooException
     *
     */    
    public void updateDeviceStatus(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, int status) throws RemoteException, XanbooException;
    
    
    
    /**
     * Returns managed object values and definitions for a device instance.
     * A description of the access and type columns can be found above.
     *<br>
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> ACCESS_ID </li>
     *  <li> MGROUP </li>
     *  <li> ACCESS_METHOD </li>
     *  <li> LABEL </li>
     *  <li> ISPUBLIC </li>
     *  <li> VALUE </li>
     *  <li> TYPE </li>
     *  <li> MOBJECT_ID </li>
     *  <li> PENDING_VALUE </li>
     *  <li> TIMESTAMP </li>
     *  <li> DEFAULT_VALUE </li>
     *  <li> DEVICE_GUID </li>
     *  <li> GATEWAY_GUID</li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier of the managed object. If null, all managed object values for all devices for the specified gateway are returned.
     * @param mobjectId the managed object id to retrieve the values and definitions for. If null, all managed object values for the specified device are returned.
     *
     * @return a XanbooResultSet which contains a HashMap list of managed objects
     * @throws XanbooException
     *
     */
    public XanbooResultSet getMObject(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String mobjectId) throws RemoteException, XanbooException;
    
    
    
    /**
     * Sets the value of a individual managed object on a device instance.
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier for the destination device.
     * @param mobjectId managed object id to set the value for.
     * @param mobjectValue string value to set.
     *
     * @return a numeric, unique command queue/transaction identifier
     * @throws XanbooException
     *
     */    
    public long setMObject(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String mobjectId, String mobjectValue) throws RemoteException, XanbooException;
    
    
    
    /**
     * Sets the value of multiple managed objects on a device instance.
     * This method is useful for times when a large number of managed objects need their values to be set.
     * Two arrays are supplied, one of OIDs, and one of the values corresponding to those oids.
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier for the destination device.
     * @param mobjectId[] String Array of managed object ids to set.
     * @param mobjectValue[] String Array of managed object values to set.
     *
     * @return a numeric, unique command queue/transaction identifier
     * @throws XanbooException
     *
     */    
    public long setMObject(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String[] mobjectId, String[] mobjectValue) throws RemoteException, XanbooException;
    
    
    
    /**
     * Returns event definitions for a device instance.
     * By suppling a null eventId, all events may be retreived.
     *<br>
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> CATEGORY </li>
     *  <li> LABEL </li>
     *  <li> EVENT_ID </li>
     *  <li> ELEVEL </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier of the managed object.
     * @param eventId the event to retrieve the definitions for. If null,
     *                all event definitions for the specified device are returned.
     *
     * @return a XanbooResultSet which contains a HashMap list of device events
     * @throws XanbooException
     *
     */      
    public XanbooResultSet getDeviceEvent(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String eventId) throws RemoteException, XanbooException;
    
    
    
    /**
     * Returns event log entries for a gateway or device instance.
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
     * @param xCaller a XanbooPrincipal object that identifies the caller
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
    public XanbooResultSet getDeviceEventLog(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String eventId) throws RemoteException, XanbooException;    
    
    
    
    /**
     * Clears event log entries for a gateway or device instance
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier of the managed object. If null,
     *                   all event log entries for the whole gateway are cleared.
     * @param eventId the event identifier to retrieve the log entries for. If null,
     *                all events log records for the specified gateway/device are cleared.
     *
     * @throws XanbooException
     *
     */    
    public void clearDeviceEventLog(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String eventId) throws RemoteException, XanbooException;
    
    
    
    /**
     * Returns pending commands for a specific gateway or device instance.
     * Normally, commands will exist in the queue for a very amount of time. If items exist in the queue for longer than expected, it may be related
     * to a device/gateway being offline, or failed communication.
     * <br>
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> TIMESTAMP </li>
     *  <li> PARAM_NAME </li>
     *  <li> PARAM_VALUE </li>
     *  <li> COMMANDQUEUE_ID </li>
     *  <li> NAME </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier. If null, all pending commands for the whole gateway are returned.
     *
     * @return a XanbooResultSet which contains a HashMap list queued commands
     * @throws XanbooException
     *
     */
    public XanbooResultSet getCommandQueueItem(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID) throws RemoteException, XanbooException; 
    
    
    
    /**
     * Removes pending commands from a gateway/device command queue.
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID the gateway GUID of the command queue
     * @param queueId an array list of command queue ids to remove
     *
     * @throws XanbooException
     *
     */    
    public void deleteCommandQueueItem(XanbooPrincipal xCaller, String gatewayGUID, long[] queueId) throws RemoteException, XanbooException;
    
    
    
    /**
     * Clears all pending commands from a gateway/device instance.
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID the gateway GUID of the command queue.
     * @param deviceGUID the device id of the command queue. If null, all pending commands
     *                   for the whole gateway are cleared.
     *
     * @throws XanbooException
     *
     */    
    public void emptyCommandQueue(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID) throws RemoteException, XanbooException;
    
    
    
    /**
     * Returns list of notification actions defined for a device event instance.
     * A notification action is the association of a destination address (notification profile) with an event occurance (event)
     * <br>
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> ACTION_ID </li>
     *  <li> PROFILE_ID </li>
     *  <li> PROFILE_ADDRESS </li>
     *  <li> QUIET_TIME </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier.
     * @param eventId the event identifier to retrieve actions for.
     *
     * @return a XanbooResultSet which contains a HashMap list of notification actions
     * @throws XanbooException
     *
     */
    public XanbooResultSet getNotificationActionList(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String eventId) throws RemoteException, XanbooException;
    
    
    
    /**
     * Assigns a notification profile to an event.
     * When the supplied event occurs, a notification will be sent to the destination address defined by the supplied notification profile.
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier for the event
     * @param eventId the device event id to set the notifications for.
     * @param profileId an array of notification profile ids to be notified for this event.
     * @param quietTime time in seconds. This value allows only 1 notification to be sent for that particular event within specified seconds.
     *
     * @throws XanbooException
     *
     */    
    public void newNotificationAction(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String eventId, long[] profileId, int quietTime) throws RemoteException, XanbooException;
    
    /**
     * @deprecated  replaced by {@link #newNotificationAction(XanbooPrincipal, String, String, String, long[], int)}
     */
    public void newNotificationAction(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String eventId, long[] profileId) throws RemoteException, XanbooException;
        
    
    /**
     * Removes all notification actions defined for a specific device event instance.
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier for the event
     * @param eventId the device event id to delete the notification actions for.
     *
     * @throws XanbooException
     *
     */    
    public void deleteNotificationAction(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String eventId) throws RemoteException, XanbooException;
    
    
    
    /**
     * Removes individual notification action(s) defined for a specific device event instance.
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier for the event
     * @param eventId the device event id to delete the actions for.
     * @param actionId an array list of notification action ids to remove.
     *
     * @throws XanbooException
     *
     */    
    public void deleteNotificationAction(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String eventId, long[] actionId) throws RemoteException, XanbooException;
    
    
    
    /**
     * Retrieves a XanbooGateway object containing information about a specific gateway belonging to the caller.
     * <br>The returned XanbooGateway object contains various state information about the gateway, such as it's last known IP address, and security token.
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID the parent gateway GUID of the device
     * @return a XanbooGateway object
     *
     * @throws XanbooException
     *
     */    
    public XanbooGateway getGatewayInfo(XanbooPrincipal xCaller, String gatewayGUID) throws RemoteException, XanbooException;
    
    
    
    /**
     * Permanently deletes a device from a customers account.
     * <p>
     * This method is used to completely remove a device, and all of its associated data (including event logs) from the system. Once a device is deleted, it cannot be restored - it does not
     * appear in the accounts wastebasket section.
     * <br>
     * The deletion of a gateway also involves the deletion of all of its sub devices, and their associated data.
     * </p>
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier to delete.
     *
     * @throws XanbooException
     *
     */    
    public void deleteDevice(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID) throws RemoteException, XanbooException;
    
    

   /**
     * Retrieves the binary content of a managed object on a device instance.
     * <br> The supplied value may be either a string object, or a file object.
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier for the destination device.
     * @param mobjectId String ID of managed object ids to set.
     *
     * @return a XanbooBinaryContent object
     *
     * @throws XanbooException
     *
     */
    public XanbooBinaryContent getMObjectBinary(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String mobjectId) throws RemoteException, XanbooException;

    
    /**
     * Sets the value of a binary managed object on a device instance.
     * <br> The supplied value may be either a string object, or a file object.
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier for the destination device.
     * @param mobjectId String ID of managed object ids to set.
     * @param mobjectValue a XanbooBinaryContent to set the binary content
     *
     * @return a numeric, unique command queue/transaction identifier
     * @throws XanbooException
     *
     */    
    public long setMObjectBinary(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String mobjectId, XanbooBinaryContent mobjectContent) throws RemoteException, XanbooException;

    /**
     * Method to return the language for the gateway. First check the value of the language OID (OID 13), and return if possible. 
     * If OID 13 is not set, returns either the language from the master user record or the language of the domain. 
     * @param xCaller
     * @param gatewayGuid - The gateway to check for OID13.
     * @param includeMasterUser - If true, check the language_id on the master user record (when OID 13 is null)
     * @return
     * @throws RemoteException
     * @throws XanbooException 
     */
    public String getUserLanguage(XanbooPrincipal xCaller,String gatewayGuid,boolean includeMasterUser) throws RemoteException, XanbooException;
    
    /**
     * Creates an instance of a new device with the specified catalog Id.
     * <br>
     * The catalog ID supplied must be that of a descriptor already loaded into the system. 
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID the parent gateway on which to create the device
     * @param catalogId specifies the exact type of device class to instanciate.
     *
     * @return The Device GUID of the newly created device.
     * @throws XanbooException if the device could not be created.
     *
     */
    public String newDevice( XanbooPrincipal xCaller, String gatewayGUID, String catalogId, String label ) throws XanbooException, RemoteException;
 
    
    
    /**
     * Returns a HashMap of camera URLs by a given camera catalog
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param catalogId specifies a particular camera catalog id to return the URLs for. If null, URLs for all supported camera catalog ids will be returned.
     *
     * @return a HashMap of camera URLs. The returned Map keys would be formatted as "<catid>-<typ>-<bw>", where 'catid' is the camera catalog id, 'typ' is the
     *         streaming type/format (e.g. m3u8, swf, flv, mjpeg), and 'bw' is the requested bandwidth (0:low, 1:high).<br>
     *         So, for instance, the map entry for RC8230 catalog id "10030206000004", HLS stream (type:m3u8) and low-bandwidth would be:<br>
     * 	                 Key:  "10030206000004-m3u8-0"               Value:  "/img/stream.m3u8"<br>
     *         If the catalogId is not supported or recognized, null will be returned. If a camera does not support a specific streaming type/format, the related
     *         HashMap value will be returned null.
     * 
     * @throws XanbooException if the device could not be created.
     *
     */
    public HashMap<String,String> getCameraURL( XanbooPrincipal xCaller, String catalogId ) throws XanbooException, RemoteException;
    
    /**
     * 	Returns Domain Ref by domainId     
     *  @param xCaller XanbooPrincipal object with which to authenticate
     *  @param domainId The domain id of the domain ref
     *  @throws XanbooException
     * 
     */
    public XanbooResultSet getDomainRefByDomainId(XanbooPrincipal xCaller, String domainId) throws XanbooException, RemoteException;

    /**
     *
     * @param xCaller
     * @param languageId Language Id to query.  If null, default will be english ("en")
     * @param modelId Model Id to query.  If null, all device models will be returned
     * @return The device model list
     * @throws XanbooException
     * @throws RemoteException
     */
    public XanbooResultSet getDeviceModel(XanbooPrincipal xCaller, String languageId, String modelId) throws XanbooException, RemoteException;
    
}
