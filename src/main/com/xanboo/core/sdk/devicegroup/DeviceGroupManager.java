/*
 * DeviceGroupManager.java
 *
 * Created on April 15, 2004, 12:51 PM
 */

package com.xanboo.core.sdk.devicegroup;

import java.rmi.RemoteException;

import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.XanbooException;


/**
 * <p>Remote Interface for the DeviceGroupManagerEJB</p>
 * <p>
 * A device group is defined as a list of devices that can be presented and addressed as a command 
 * target as a single entity.
 * <br>
 * </p>
 *
 * @see com.xanboo.core.sdk.device.DeviceManager DeviceManager for further details on guids and managed objects.
 */

public interface DeviceGroupManager  {

    /**
     * Creates a new device group associated with the specified gateway.
     * 
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID Indicates which gateway to associate the group with
     * @param label The text label to use for OID0 for this group
     * @param deviceList a csv list of devices to associate with this group
     *
     * @throws XanbooException if the group could not be created
     *
     * @return String the deviceId generated for the newly created device group.
     */
    public String newDeviceGroup( XanbooPrincipal xCaller, String gatewayGUID, String label, String deviceList ) throws XanbooException, RemoteException;

    /**
     * Updates the label and device list for an existing device group.
     * 
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID Indicates which gateway to associate the group with
     * @param deviceGUID Identifies the specific device group associated with the gateway
     * @param label The new text label to use for this group
     * @param deviceList The new csv list of devices to associate with this group
     *
     * @throws XanbooException if the group could not be updates
     *
     */
    public String updateDeviceGroup( XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String label, String deviceList ) throws XanbooException, RemoteException;
    
    /**
     * Retrieves a list of device groups that exists within an account.
     *
     *<br>
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
     *
     * @throws XanbooException if the group could not be updates
     *
     */
    public XanbooResultSet getDeviceGroupList( XanbooPrincipal xCaller ) throws XanbooException, RemoteException;
    
    /**
     * Returns definition and value of a managed object or objects belonging to a device group instance.
     *
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
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID Identifies the device group to be queried
     * @param mobjectId the managed object id to retrieve the values and definitions for. If null,
     *                  all managed object values for the specified device are returned.
     *
     * @return a XanbooResultSet which contains a HashMap list of managed objects
     *
     * @throws XanbooException if there was a problem querying the managed object.
     *
     */
    public XanbooResultSet getMObject(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String mobjectId) throws XanbooException, RemoteException;

    /**
     * Permanently deletes a device group from a customer's account.
     * <p>
     * This method is used to completely remove a device group, and all of its associated data (including event logs) from the system. Once a device group is deleted, it cannot be restored, device groups do not appear in the accounts wastebasket section.
     * <br>
     * </p>
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID the parent gateway GUID of the device group
     * @param deviceGUID identifies the specific device group to delete
     *
     * @throws XanbooException if there was a problem deleting the device group.
     *
     */    
    public void deleteDeviceGroup(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID) throws XanbooException, RemoteException;
    
}
