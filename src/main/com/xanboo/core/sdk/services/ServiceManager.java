package com.xanboo.core.sdk.services;

import java.rmi.RemoteException;
import java.util.List;

import com.xanboo.core.model.XanbooMobject;
import com.xanboo.core.sdk.services.model.ServiceObject;
import com.xanboo.core.sdk.services.model.ServiceSubscription;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.XanbooException;

/**
 * <p>Remote Interface for the ServiceManagerEJB</p>

 *  <li> <b>GATEWAY_GUID</b> <br>
 *          A globally unique gateway identifier.
 *
 * <li> <b>DEVICE_GUID</b> <br>
 *          A device identifier unique within its parent gateway domain.

 * <li> <b>Managed Object</b> <br>
 *          A managed object is an addressable entity that exists within a device. A managed object ID may often be referred to as OID. <br>
 * </li>      
 * 
 */

public interface ServiceManager   {

    /**
     * Returns a list of all available external services for a specified account.
     * The columns returned in the XanbooResultset are:
     * <UL>
     *  <li> SERVICE_ID </li>
     *  <li> NAME </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     * 
     * <br>
     * @param xCaller - a XanbooPrincipal object that identifies the caller account
     * @param gatewayGUID - If null, all services enabled and available for the account will be returned. Otherwise, only 
                            services available for that particular DLC subscription (by market code) will be returned.
     * 
     * @return a XanbooResultSet which contains a HashMap list of Service definitions
     * @throws XanbooException
     * 
     */
    public XanbooResultSet getAvailableServiceList(XanbooPrincipal xCaller, String gatewayGUID) throws XanbooException, RemoteException;
    
    
    /**
     * Returns a list of all available descriptor catalog ids for the given service id. 
     * The columns returned in the XanbooResultset are:
     * <UL>
     *  <li> SERVICE_ID </li>
     *  <li> CATALOG_ID </li>
     *  <li> NAME </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     * 
     * <br>
     * @param xCaller - a XanbooPrincipal object that identifies the caller account
     * @param serviceId - service identifier string to get the subscriptions for. If null, all descriptors for all existing services will be returned.
     * 
     * @return a XanbooResultSet which contains a HashMap list of catalog ids
     * @throws XanbooException 
     */
    public XanbooResultSet getServiceDescriptorList(XanbooPrincipal xCaller, String serviceId) throws XanbooException, RemoteException;
    
  
    /**
     * This method should be invoked by credential required base services only.
     * 
     * Creates a service subscription and Service Gateway device instance by associating the caller account to an External Service subscription. 
     * This call does make calls to the external service network itself to validate the subscription credentials before creating the local subscription records.
     * It can also (optionally) auto-bind the Subscription instance to a particular DLC or set of DLCs, if specific gateway guid/DLC is not specified then 
     *    1. If it is multiple binding service, it will bind to all the gateway guids for that account.
     *    2. If it is single binding service, then gguid should be specified, if not specified will bind existing DLC but if account has multiple 
     *       DLC then it will throw error message "gateway binding is not specified.".
     * 
     * <br>
     * @param xCaller - a XanbooPrincipal object that identifies the caller account
     * @param subs - a ServiceSubscription object to pass an external Service id and credentials, typically an access token,  
              to access and subscribe to that service
     * @return  A ServiceSubscription object that includes the new Subscription information including the new Service Gateway  device instance GGUID.
     * @throws XanbooException 
     * 
    */
    public ServiceSubscription newServiceSubscription(XanbooPrincipal xCaller, ServiceSubscription subs) throws XanbooException, RemoteException;
    
    
    /**
     * This method should be invoked by credential required base services only. in order to bind the gguid/DLC the subscription must be present.
     * 
     * Binds/Unbinds a service subscription and associated Service Gateway device instance to/from a set of DLCs on the caller account.
     * 
     * <br>
     * @param xCaller - a XanbooPrincipal object that identifies the caller account
     * @param subs - a ServiceSubscription object to pass an external Service Id and credentials to access that service. A subscription
     *               with provided serviceId and sGguid attributes must already exist/subscribed to the caller account.
     * @param unbind - if false, will do the binding. If true, will unbind the gateway from the subscription
     * 
     * @throws XanbooException 
     */
    public void bindServiceSubscription(XanbooPrincipal xCaller, ServiceSubscription subs, boolean unbind) throws XanbooException, RemoteException;
    
    
    /**
     * Returns a list of all external service subscriptions and related details for the caller account. This call only returns local subscription details and does NOT make
     * calls to the external service itself to validate and query the current state of the subscriptions.
     * 
     * <br>
     * @param xCaller - a XanbooPrincipal object that identifies the caller account
     * @param serviceId -  service identifier string to get the subscriptions for. If null, all subscriptions for all existing services will be returned.
     * 
     * @return a list of ServiceSubscription objects
     * @throws XanbooException 
     * 
     */
    public List<ServiceSubscription> getServiceSubscription(XanbooPrincipal xCaller, String serviceId) throws XanbooException, RemoteException;
    
    
    /**
     * This method should be invoked by credential required base services only.
     * 
     * Updates an existing service subscription information for the caller account
     * 
     * <br>
     * @param xCaller - a XanbooPrincipal object that identifies the caller account
     * @param subs - a ServiceSubscription object to verify/update. Only subscription credentials (username) and access token can be verified/updated. 
     *               A subscription with provided serviceId and sGguid attributes must already exist/subscribed to the caller account. If the access token
     *               attribute on the subscription object is null, existing token will be re-verified thru the Service network and DL service subscription 
     *               record will updated, if successful. If the access token attribute is not null, the value will be verified thru the Service 
     *               network and the DL service subscription record will overridden with the new token, if successful. If username/password credentials are
     *               set on the subscription object, they will be verified thru the Service network and a new token will be obtained. Both token and user 
     *               credentials on the DL service subscription record will updated in this case, if successful.
     * 
     * @throws XanbooException
     */
    public void updateServiceSubscription (XanbooPrincipal xCaller, ServiceSubscription subs) throws XanbooException, RemoteException;
    

    /**
     * Cancels an existing service subscription for the caller account. 
     * 
     * <br>
     * @param xCaller - a XanbooPrincipal object that identifies the caller account
     * @param subs - a ServiceSubscription object to with subscription info to be canceled. A subscription with provided serviceId and sGguid 
     *               attributes must already exist/subscribed to the caller account.
     * 
     * @throws XanbooException 
     * 
     */
    public void cancelServiceSubscription(XanbooPrincipal xCaller, ServiceSubscription subs) throws XanbooException, RemoteException;
    
    
    /**
     * Queries and retrieves all or selected list of Service Object definitions of a particular external service subscription, and optionally 
     * creates associated DL Service device instances on the caller account and bound DLCs. This call DOES make calls to the external service network
     * itself to validate the subscription credentials before retrieving the service objects. A subscription with provided serviceId and sGguid 
     * attributes must already exist/subscribed to the caller account.
     * 
     * <br>
     * @param xCaller - a XanbooPrincipal object that identifies the caller account
     * @param subs - a ServiceSubscription object to query a particular service subscription
     * @param sObjects - a list of ServiceObjects to be retrieved. If null, all objects will be returned
     * @param importObjects - if true, returned objects will be imported to the account
     * 
     * @return a list of ServiceObject instances 
     * @throws XanbooException
     */
    public List<ServiceObject> getServiceObjectList(XanbooPrincipal xCaller, ServiceSubscription subs, List<ServiceObject> sObjects, boolean importObjects) throws XanbooException, RemoteException;
    

	/**
     * Creates a new Service Device/Object instance 'both' on the external Service network (if supported) and within DL Core, with some initial attributes (specified as mobject id/value pairs) 
     * External Service devices should only be created thru this call. Essentially will do the following:
     *      1) Create the Service object on the External Service network with specified initial attribute values (if supported)
     *      2) If #1 is supported and succeeds:
     *          2.1) create a corresponding device instance under the Service Subscription Gateway object on the caller DL account
     *          2.2) if the subscription is bound to physical DL Controller, create corresponding device instances on that DLC as well
     * 
     * <br>
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID - Create a service device instance on the existing gateway guid/DLC.
     * @param catalogId the descriptor catalog id associated with the service device to be instantiated. The catalog ID supplied 
     *        must be that of a valid service descriptor already loaded into the system.
     * @param label a custom name/alias to be assigned to the device
     * @param mobjects a list of XanbooMobject objects to initialize the Service Object with 
     * @return The Device GUID of the newly created service device.
     * @throws XanbooException if the device could not be created.
     *
     */
    public ServiceObject newDevice(XanbooPrincipal xCaller, String gatewayGUID, String catalogId, String label, List<XanbooMobject> mobjects) throws XanbooException, RemoteException;
    
    
    /**
     * Creates a new Service Device/Object instance & it's children device/object 'both' on the external Service network (if supported) and within DL Core. 
     *      1) Create the Service object  & it's children device/object on the External Service network. 
     *      2) If #1 is supported and succeeds:
     *          2.1) create a corresponding device instances under the Service Subscription Gateway object on the caller DL account
     *          2.2) if the subscription is bound to physical DL Controller, create corresponding device instances on that DLC as well
     * 
     * <br>
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID - Create a service device instance on the existing gateway guid/DLC.
     * @param sObject - service object containing label, catalogId, unique key(hwSerial)
     * @return The Device GUID of the newly created service device.
     * @throws XanbooException if the device could not be created.
     *
     */
    public ServiceObject newDevice( XanbooPrincipal xCaller, String gatewayGUID, ServiceObject sObject) throws RemoteException, XanbooException;
    
	/**
     * Remove an existing Service Device/Object instance from the subscription and external Service Network (if supported by the Service). 
     * The deletion of a gateway also involves the deletion of all of its sub devices, and their associated data.
     * 
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID -Remove the service device instance from existing gateway/DLC.
     *             subscription on the caller account.
     * @param deviceGUID - a DL deviceGUID to be deleted.
     *
     * @throws XanbooException, if the instance cannot be deleted
     *
     */    
    public void deleteDevice(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID) throws XanbooException, RemoteException;
    
  	/**
     * Removes existing Service Device/Object instances from the subscription and external Service Network (if supported by the Service). 
     * The deletion of a gateway also involves the deletion of all of its sub devices, and their associated data.
     * 
     * @param xCaller a XanbooPrincipal object that identifies the caller
  	 * @param gatewayGUID -Remove the service device instance from existing gateway/DLC.
  	 * @param sObjects - a list of ServiceObjects to be deleted. Service object instance passed must have at least its deviceGUID attribute populated
  	 * @param cancelSubs - if true, cancel the service subscription if device doesn't exist for that particular service. 
     * @throws XanbooException, if the instance(s) cannot be deleted
     *
     */    
    public void deleteDevice(XanbooPrincipal xCaller, String gatewayGUID, List<ServiceObject> sObjects, boolean cancelSubs) throws XanbooException, RemoteException;


    /**
     * Retrieves a list of all existing DL Service device instances for a given service subscription. This call does NOT make calls to the external Service
     * Network itself to get the full, current object list, but rather already imported/created instances at the DL side. The DeviceManager getDeviceList()
     * calls may be used to retrieve device instances for a particular DLC, which will however return both native devices and external service devices 
     * bound to it. Some of the columns returned in the XanbooResultset are:
     * <UL>
     *  <li> GATEWAY_GUID </li>
     *  <li> DEVICE_GUID </li>
     *  <li> CATALOG_ID </li>
     *  <li> EXT_OBJECT_ID </li>
     *  <li> PARENT_DGUID </li>
     *  <li> LABEL </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)
     * 
     * <br>
     * @param xCaller - a XanbooPrincipal object that identifies the caller account
     * @param gatewayGUID the parent gateway GUID of the device.
     * @param deviceGUID the device identifier to get the device information for.
     * 
     * @return a XanbooResultSet which contains a HashMap list of Service Device instances
     * @throws XanbooException
     * 
     */
    public XanbooResultSet getDeviceList(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID) throws RemoteException, XanbooException;

 
	/**
     * Sets Service Device instance managed object instance values (sending device commands).
     * The Device Manager EJB setMObject() call may also be used to set managed objects on the service devices.
     * 
     * <br>
     * @param xCaller - a XanbooPrincipal object that identifies the caller account
     * @param gatewayGUID - object to set the service device instances on
     * @param deviceGUID - the service device instance dguid/identifier for the command(s), if 4 digit classid is being passed
     *                     into this parameter then all the devices belongs to this classId and given gatewayGUID will be updated. (e.g deviceGUID = 0908)
     * @param mobjects a list of XanbooMobject objects to set the Service Object with 
     * 
     * 
     * @return a numeric, unique command queue/transaction identifier, if applicable
     * @throws XanbooException
     * 
     */
    public long setMObject(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, List<XanbooMobject> mobjects) throws XanbooException, RemoteException;
    
    
    /**
     * Gets device instance managed object instance values.
     * The  Device Manager EJB getMObject() call may also be used to get managed objects on the service devices.
     * 
     * <br>
     * @param xCaller - a XanbooPrincipal object that identifies the caller account
     * @param gatewayGUID - object to set the service device instances on
     * @param deviceGUID - the service device instance dguid/identifier for the command(s)
     * @param List<String> mobjectId - if null, all mobject will be returned for given device guid. Otherwise specified list of mobject will be returned.
     * @return a List<XanbooMobject> list of mobjects.
     * @throws XanbooException
     * 
     */
    public List<XanbooMobject> getMObject(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, List<String> mobjectId) throws XanbooException, RemoteException;
    
    
    /**
     * Verifies if the external service subscription is valid by making call to the external network.
     * 
     * <br>
     * @param xCaller - a XanbooPrincipal object that identifies the caller account
     * @param subs - a ServiceSubscription object to query a particular service subscription 
     * @return boolean  
     * @throws XanbooException
     */
    public boolean verifySubscription( XanbooPrincipal xCaller,ServiceSubscription subscription) throws RemoteException,XanbooException;
    
    /**
     * Returns a list of all available external services for a specified account.
     * The columns returned in the XanbooResultset are:
     * <UL>
     *  <li> SERVICE_ID </li>
     *  <li> NAME </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     * 
     * <br>
     * @param xCaller - a XanbooPrincipal object that identifies the caller account
     * @param gatewayGUID - If null, all services enabled and available for the account will be returned. Otherwise, only 
                            services available for that particular DLC subscription (by market code) will be returned.
     * @param isSelfInstallable - if true - returns self install able services else all
     * @return a XanbooResultSet which contains a HashMap list of Service definitions
     * @throws XanbooException
     * 
     */ 
    public XanbooResultSet getAvailableServiceList(XanbooPrincipal xCaller, String gatewayGUID, Boolean isSelfInstallable) throws RemoteException,XanbooException;
     
     
     /**
      * Returns a external service subscription and related details for the given service id. This call only returns local subscription details and does NOT make
      * calls to the external service itself to validate and query the current state of the subscriptions.
      * 
      * <br>
      * @param serviceId -  service identifier string to get the subscriptions for. it's mandatory parameter.
      * 
      * @return a ServiceSubscription object
      * @throws XanbooException 
      * 
      */     
     public ServiceSubscription getServiceSubscriptionBySvcId( String serviceId) throws XanbooException ;
}
