package com.xanboo.core.sdk.services;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import org.apache.commons.lang.StringUtils;
import com.xanboo.core.extservices.gateway.ServiceGatewayFactory;
import com.xanboo.core.extservices.model.ExternalService;
import com.xanboo.core.extservices.outbound.ServiceOutboundProxy;
import com.xanboo.core.model.XanbooMobject;
import com.xanboo.core.sdk.AbstractSDKManagerEJB;
import com.xanboo.core.sdk.device.DeviceManager;
import com.xanboo.core.sdk.services.model.ServiceObject;
import com.xanboo.core.sdk.services.model.ServiceSubscription;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooAdminPrincipal;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.EjbProxy;
import com.xanboo.core.util.GlobalNames;
import com.xanboo.core.util.XanbooException;
import com.xanboo.core.util.XanbooUtil;


/**
 * <p>
 * Session Bean implementation of <code>ServiceManager</code>. This bean acts as a wrapper class for
 * all device, device query/controls, event and notification setup related Core SDK methods.
 * </p>
 */
@Remote (ServiceManager.class)
@Stateless (name="ServiceManager")
@TransactionManagement( TransactionManagementType.BEAN )
public class ServiceManagerEJB extends AbstractSDKManagerEJB  {
   
	// related DAO class
    private ServiceManagerDAO dao=null;
    
    private ServiceOutboundProxy soProxy = null;
    
    private XanbooAdminPrincipal xap = null;
    
    @PostConstruct
    public void init() throws Exception {
        try {
			dao = new ServiceManagerDAO();
      
			EjbProxy proxy = new EjbProxy();
			soProxy = (ServiceOutboundProxy) proxy.getObj( GlobalNames.EJB_OUTBOUND_PROXY );
			
			xap=new XanbooAdminPrincipal(GlobalNames.DEFAULT_APP_USER, 0, 0);    //for service proxy ejb calls - dummy for now
		} catch (Exception e) {
			logger.error("[init()]: Exception " + e.getMessage());
		}
    }
    
    
    //--------------- private helper methods ------------------------------------------------------
    /* gets an EJB reference to the ServiceOutboundProxyEJB, if necessary */
/*    private void getServiceOutboundProxyEJB() {
        if ( soProxy == null ) {
            EjbProxy proxy = new EjbProxy();
            try {
                soProxy = (ServiceOutboundProxy) proxy.getObj( GlobalNames.EJB_OUTBOUND_PROXY );
            }catch (Exception e) {
                if ( logger.isDebugEnabled() ) {
                    logger.error("[getServiceOutboundProxyEJB()]: Exception " + e.getMessage(), e);
                }else {
                    logger.error("[getServiceOutboundProxyEJB()]: Exception " + e.getMessage());
                }
            }
        }
    }*/

    
    //--------------- Business methods ------------------------------------------------------
    
    //returns all supported services enabled for the caller account
    public XanbooResultSet getAvailableServiceList(XanbooPrincipal xCaller, String gatewayGUID) throws XanbooException {
        
       return getAvailableServiceList(xCaller, gatewayGUID, null);
    }
    
    
    public XanbooResultSet getAvailableServiceList(XanbooPrincipal xCaller, String gatewayGUID, Boolean isSelfInstallable) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getAvailableServiceList()]:");
        }
        
        Connection conn=null;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);

            conn=dao.getConnection();
            return dao.getAvailableServiceList(conn, xCaller, gatewayGUID, isSelfInstallable);
            
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getAvailableServiceList()]" + e.getMessage(), e);
            }else {
              logger.error("[getAvailableServiceList()]" + e.getMessage());
            }
            throw new XanbooException(60000, "[getAvailableServiceList]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }          
    }
    
    //returns all device descriptor catalogs ids for the caller account
    public XanbooResultSet getServiceDescriptorList(XanbooPrincipal xCaller, String serviceId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getServiceDescriptorList()]:");
        }
        
        // validate the subscription object
        if(serviceId!=null && !XanbooUtil.isValidServiceId(serviceId)) {
            throw new XanbooException(60050, "Invalid service id.");
        }
        
        Connection conn=null;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);

            conn=dao.getConnection();
            return dao.getServiceDescriptorList(conn, xCaller, serviceId);
            
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getServiceDescriptorList()]" + e.getMessage(), e);
            }else {
              logger.error("[getServiceDescriptorList()]" + e.getMessage());
            }
            throw new XanbooException(60000, "[getServiceDescriptorList]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }          
    }
    
    //public XanbooCatalog getServiceDeviceCatalog(XanbooPrincipal xCaller, String serviceId) throws XanbooException {
    //}

    //creates a new subscription to the external service network
    public ServiceSubscription newServiceSubscription(XanbooPrincipal xCaller, ServiceSubscription subs) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[newServiceSubscription()]:");
        }
        
        // validate the subscription object
        if(subs==null || !XanbooUtil.isValidServiceId(subs.getServiceId()) || subs.getAccountId()!=xCaller.getAccountId()) {
            throw new XanbooException(60050, "Invalid service subscription/account information.");
        }
        
        if ( subs.getGguid() == null || subs.getGguid().length() == 0 )
            throw new  XanbooException(60050,"Gateway GUID is required");
        
        ExternalService extService = ServiceGatewayFactory.getExternalService(subs.getServiceId());
        if(extService!=null) {
        	/* User/UI should not invoke this method for the external service(s), for which subscription will be created implicitly during  
        	 * device creation. this method only be called for the external service(s) which required explicitly register/supply credentials etc.
        	 */
        	
        	if(XanbooUtil.isBitOn(extService.getSubsType(), 0) || 
        			(subs.getAuthCode() != null && subs.getAuthCode().trim().length() > 0)) { 
        		
	        	if(subs.getUserName()==null && subs.getAuthCode() == null && subs.getAccessToken()==null) {   //at least one must be specified
	                throw new XanbooException(60050, "Invalid service subscription access information.");
	            }
	        	
	       //     getServiceOutboundProxyEJB();
	            
	            try {
	                xap.setAccountPrincipal(xCaller);   //set account principal to pass caller account info
	                ServiceSubscription nsubs = soProxy.newServiceSubscription(xap, subs);
	                return nsubs;
	            }catch(XanbooException xe) {
	                throw xe;
	            }catch(Exception e) {
	                if(logger.isDebugEnabled()) {
	                  logger.error("[newServiceSubscription()]" + e.getMessage(), e);
	                }else {
	                  logger.error("[newServiceSubscription()]" + e.getMessage());
	                }
	                throw new XanbooException(60000, "[newServiceSubscription]:" + e.getMessage());
	            }
        	} else {
            	throw new XanbooException(60050, "Subscription can't be created explicity.");
            }
        }  else {
        	throw new XanbooException(60050, "Unable to find service reference.");
        }
    }
    
    //binds/unbinds a set of DLCs to a given service subscription - should we move to outboundProxy?
    public void bindServiceSubscription(XanbooPrincipal xCaller, ServiceSubscription subs, boolean unbind) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[bindServiceSubscription()]:");
        }

        // validate the subscription object
        if(subs==null || !XanbooUtil.isValidServiceId(subs.getServiceId()) || subs.getAccountId()!=xCaller.getAccountId() ||
            !XanbooUtil.isExternalServiceDevice(subs.getSgGuid()) || subs.getGguid() == null) {
                throw new XanbooException(60050, "Invalid service subscription/account information.");
        }
        
        //validate gguids passed. Must not be a service gateway
        if(XanbooUtil.isExternalServiceDevice(subs.getGguid())) {
            throw new XanbooException(60050, "Invalid gateway guid to bind/unbind.");
        }
        
        // Retrieve service reference record from the cache.
        ExternalService extService = ServiceGatewayFactory.getExternalService(subs.getServiceId());
       
        if(extService == null) {
        	throw new XanbooException(60050, "Unable to find service reference.");

        } else {
        	
        	 // Call will be allowed only for services which supports multiple bindings(ST bit1=1) AND GGUID binding (ST bit23=01).
    	 	if( XanbooUtil.isBitOn(extService.getSubsType(), 1) &&
    	 			XanbooUtil.isBitOn(extService.getSubsType(), 2) && !XanbooUtil.isBitOn(extService.getSubsType(), 3)) {
    	 		
            	Connection conn=null;
                try {
                    // validate the caller and privileges
                    XanbooUtil.checkCallerPrivilege(xCaller);

                    conn=dao.getConnection();
                    dao.bindServiceSubscription(conn, xCaller, subs, unbind);
                    
                }catch (XanbooException xe) {
                    throw xe;
                }catch (Exception e) {
                    if(logger.isDebugEnabled()) {
                      logger.error("[bindServiceSubscription()]" + e.getMessage(), e);
                    }else {
                      logger.error("[bindServiceSubscription()]" + e.getMessage());
                    }
                    throw new XanbooException(60000, "[bindServiceSubscription]:" + e.getMessage());
                }finally {
                    dao.closeConnection(conn);
                }          
    	 	} else {
    	 		throw new XanbooException(60050, "Mutliple binding is not supported.");
    	 	}
        }
    }
    
    // returns service subscriptions for the caller account
    public List<ServiceSubscription> getServiceSubscription(XanbooPrincipal xCaller, String serviceId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getServiceSubscription()]:");
        }
        
        // must be a valid service id, if specified
        if(serviceId!=null && !XanbooUtil.isValidServiceId(serviceId)) {
            throw new XanbooException(60050, "Invalid service subscription/account information.");
        }
        
        Connection conn=null;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            return dao.getServiceSubscription(conn, xCaller, serviceId, null);
            
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getServiceSubscription()]" + e.getMessage(), e);
            }else {
              logger.error("[getServiceSubscription()]" + e.getMessage());
            }
            throw new XanbooException(60000, "[getServiceSubscription]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }          
    }
    
    // updates a service subscription
    public void updateServiceSubscription (XanbooPrincipal xCaller, ServiceSubscription subs) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateServiceSubscription()]:");
        }
        
        // validate the subscription object
        if(subs==null || !XanbooUtil.isValidServiceId(subs.getServiceId())) {
              throw new XanbooException(60050, "Invalid service subscription/account information.");
        }
  
    	 ExternalService extService = ServiceGatewayFactory.getExternalService(subs.getServiceId());
         if(extService == null) {
        	 throw new XanbooException(60050, "Unable to find service reference.");
         }  else {
        	 try {
                 xap.setAccountPrincipal(xCaller);   //set account principal to pass caller account info
                 soProxy.updateServiceSubscription(xap, subs);
             }catch(XanbooException xe) {
                 throw xe;
             }catch(Exception e) {
                 if(logger.isDebugEnabled()) {
                   logger.error("[updateServiceSubscription()]" + e.getMessage(), e);
                 }else {
                   logger.error("[updateServiceSubscription()]" + e.getMessage());
                 }
                 throw new XanbooException(60000, "[updateServiceSubscription]:" + e.getMessage());
             }
         }
    }
    
    // cancels an external service subscription
    public void cancelServiceSubscription(XanbooPrincipal xCaller, ServiceSubscription subs) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[cancelServiceSubscription()]:");
        }
        
        // validate the subscription object
        if(subs==null || !XanbooUtil.isValidServiceId(subs.getServiceId()) || subs.getAccountId()!=xCaller.getAccountId() ||
            !XanbooUtil.isExternalServiceDevice(subs.getSgGuid()) ) {
                throw new XanbooException(60050, "Invalid service subscription/account information.");
        }
        
      //  getServiceOutboundProxyEJB();
        
        try {
            xap.setAccountPrincipal(xCaller);   //set account principal to pass caller account info
            soProxy.cancelServiceSubscription(xap, subs);
        }catch(XanbooException xe) {
            throw xe;
        }catch(Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[cancelServiceSubscription()]" + e.getMessage(), e);
            }else {
              logger.error("[cancelServiceSubscription()]" + e.getMessage());
            }
            throw new XanbooException(60000, "[cancelServiceSubscription]:" + e.getMessage());
        }
       
    }
    
    
    //queries/imports existing external service subscription objects to the caller account
    public List<ServiceObject> getServiceObjectList(XanbooPrincipal xCaller, ServiceSubscription subs, List<ServiceObject> sObjects, boolean importObjects) throws XanbooException {
    	if (logger.isDebugEnabled()) {
            logger.debug("[getServiceObjectList()]:");
        }
        
        // validate the subscription object
        if(subs==null || !XanbooUtil.isValidServiceId(subs.getServiceId()) || subs.getAccountId()!=xCaller.getAccountId() ||
            !XanbooUtil.isExternalServiceDevice(subs.getSgGuid()) ) {
                throw new XanbooException(60050, "Invalid service subscription/account information.");
        }
        
        // validate gguids passed. Must not be a service gateway
        if((!subs.getGguid().startsWith(GlobalNames.DLLITE_GATEWAY_PREFIX) && XanbooUtil.isExternalServiceDevice(subs.getGguid()))) {
            throw new XanbooException(60050, "Invalid gateway guid.");
        }
        
     //   getServiceOutboundProxyEJB();
        
        try {
            xap.setAccountPrincipal(xCaller);   //set account principal to pass caller account info
            ServiceSubscription nsubs = soProxy.getServiceObjectList(xap,subs,sObjects, importObjects);
            return nsubs.getServiceObjects();
        }catch(XanbooException xe) {
            throw xe;
        }catch(Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getServiceObjectList()]" + e.getMessage(), e);
            }else {
              logger.error("[getServiceObjectList()]" + e.getMessage());
            }
            throw new XanbooException(60000, "[getServiceObjectList]:" + e.getMessage());
        }
    }
    	
    // Creates a new Service Device/Object instance 'both' on the external Service network (if supported) and within DL Core with some initial values.
    public ServiceObject newDevice( XanbooPrincipal xCaller, String gatewayGUID, String catalogId, String label, List<XanbooMobject> mobjects) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[newDevice()]:");
        }            
            ServiceObject sobj = new ServiceObject();
            sobj.setCatalogId(catalogId);
            sobj.setName(label);
            sobj.setInitialMObjects(mobjects);
            return newDevice(xCaller, gatewayGUID, sobj);        
      
    }
    
    
    public ServiceObject newDevice( XanbooPrincipal xCaller, String gatewayGUID, ServiceObject sobj) throws XanbooException {
    	if(logger.isDebugEnabled()) {
            logger.debug("[newDevice()]:");
        }

        //validate gguids passed is not empty.
        if(StringUtils.isBlank(gatewayGUID) ) {
            throw new XanbooException(60050, "Invalid gateway guid.");
        }
          
        //validate catalog id matches the subscription service id
        if(sobj.getCatalogId()==null || sobj.getCatalogId().trim().length()<14) {
            throw new XanbooException(60050, "Invalid catalog id.");
        }
        
        if(sobj.getCatalogId().length()==14) sobj.setCatalogId("0"+sobj.getCatalogId());
         
        
     //   getServiceOutboundProxyEJB();
        
        try {
            xap.setAccountPrincipal(xCaller);   //set account principal to pass caller account info
            sobj = soProxy.newServiceObject(xap, gatewayGUID, sobj);
            return sobj;
        }catch(XanbooException xe) {
            throw xe;
        }catch(Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[newDevice()]" + e.getMessage(), e);
            }else {
              logger.error("[newDevice()]" + e.getMessage());
            }
            throw new XanbooException(60000, "[newDevice]:" + e.getMessage());
        }
    }
    
    public void deleteDevice(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteDevice()]: ");
        }
            List<ServiceObject> serviceObjects = new ArrayList<ServiceObject>();
            ServiceObject svcObj = new ServiceObject();
            svcObj.setDeviceGuid(deviceGUID);
            serviceObjects.add(svcObj);
            
            deleteDevice(xCaller, gatewayGUID, serviceObjects, false);
    }
    
    
    /*  Removes existing Service Device/Object instances within DL Core. This call does NOT delete the object from the external network
     *  Will do the following:
     *     1) delete the given existing device instance(s) under the Service Subscription Gateway object on the caller DL account
     *     2) delete the instances from all bound DLC instances, if any    
     * 
     */
    public void deleteDevice(XanbooPrincipal xCaller, String gatewayGUID, List<ServiceObject> sObjects, boolean cancelSubs) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteDevice()]: ");
        }
        
        // validate gguids must be passed. 
        if(gatewayGUID==null || gatewayGUID.length() == 0) {
            throw new XanbooException(60050, "Invalid gateway guid to bind/unbind.");
        }
          
        //validate objects to be deleted
        if(sObjects==null || sObjects.size()==0) {
            throw new XanbooException(60050, "No device id to delete.");
        }
        for(int i=0; i<sObjects.size(); i++) {
            if(sObjects.get(i)==null || !XanbooUtil.isExternalServiceDevice(sObjects.get(i).getDeviceGuid())) {
                throw new XanbooException(60050, "Invalid service device id delete.");
            }
        }

      //  getServiceOutboundProxyEJB();
        
        try {
            xap.setAccountPrincipal(xCaller);   //set account principal to pass caller account info
            soProxy.deleteServiceObject(xap, gatewayGUID, sObjects, cancelSubs);
            
        }catch(XanbooException xe) {
            throw xe;
        }catch(Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[deleteDevice()]" + e.getMessage(), e);
            }else {
              logger.error("[deleteDevice()]" + e.getMessage());
            }
            throw new XanbooException(60000, "[deleteDevice]:" + e.getMessage());
        }
        
    }
    
    public XanbooResultSet getDeviceList(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getDeviceList()]:");
        }

        Connection conn=null;
        try {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            return dao.getDeviceList(conn, xCaller, gatewayGUID, deviceGUID);
            
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getDeviceList()]" + e.getMessage(), e);
            }else {
              logger.error("[getDeviceList()]" + e.getMessage());
            }
            throw new XanbooException(60000, "[getDeviceList]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }          
        
    }

    public long setMObject(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, List<XanbooMobject> mobjects) throws XanbooException {
    	 if (logger.isDebugEnabled()) {
             logger.debug("[setMObject()]:");
         }
  
         if(mobjects == null || mobjects.size() == 0) {
         	throw new XanbooException(60050, "Invalid arguments.");
         }
       
         XanbooResultSet resultSet = null;
         
         //Retrieve list of devices/dguids based on 4 digit classId passed into the deviceGUID.
         if(deviceGUID!=null && deviceGUID.length() == 4) { 
        	Object obj = getEJB(GlobalNames.EJB_DEVICE_MANAGER);
			if(obj!=null && obj instanceof DeviceManager) {
				DeviceManager ejb = (DeviceManager)obj;
				try {
					resultSet = ejb.getDeviceListByClass(xCaller, deviceGUID);
				 }catch(XanbooException xe) {
	                 throw xe;
	             }catch(Exception e) {
	                 if(logger.isDebugEnabled()) {
	                   logger.error("[setMObject()]" + e.getMessage(), e);
	                 }
	                 throw new XanbooException(60000, "[setMObject]:" + e.getMessage());
	             } 
			}else {
				 throw new XanbooException(60002, "[setMObject]: Unable to looup" + GlobalNames.EJB_DEVICE_MANAGER);
			}
         }

         try {
        	// get a instance of ServiceOutboundProxyEJB.
        //	getServiceOutboundProxyEJB();
        	
        	//Set account principal to pass caller account info
        	xap.setAccountPrincipal(xCaller);  
			if(resultSet!=null && resultSet.size() > 0) { 
				for(int i = 0; i< resultSet.size(); i++) {
			    	HashMap mObjectMap = (HashMap) resultSet.get(i);
			    	soProxy.setMobject(xap, gatewayGUID, mObjectMap.get("DEVICE_GUID").toString(), mobjects);	
			    }
			}else {
				    soProxy.setMobject(xap, gatewayGUID, deviceGUID, mobjects);	
			}
         }catch(XanbooException xe) {
             throw xe;
         }catch(Exception e) {
             if(logger.isDebugEnabled()) {
               logger.error("[setMObject()]" + e.getMessage(), e);
             }else {
               logger.error("[setMObject()]" + e.getMessage());
             }
             throw new XanbooException(60000, "[setMObject]:" + e.getMessage());
         } 
         return -1;
     }
    
    
    public List<XanbooMobject> getMObject(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, List<String> mobjectId) throws XanbooException {
        List<XanbooMobject> mObjects = null; 
        if (logger.isDebugEnabled()) {
              logger.debug("[getMObject()]:");
          }
          
         // validate gguids passed. Must not be a service gateway
          if(gatewayGUID==null) {
              throw new XanbooException(60050, "Invalid gateway guid.");
          }
        
     //     getServiceOutboundProxyEJB();
          
          try {
              xap.setAccountPrincipal(xCaller);   //set account principal to pass caller account info
              mObjects = soProxy.getMObject(xap, gatewayGUID, deviceGUID, mobjectId);      
          }catch(XanbooException xe) {
              throw xe;
          }catch(Exception e) {
              if(logger.isDebugEnabled()) {
                logger.error("[getMObject()]" + e.getMessage(), e);
              }else {
                logger.error("[getMObject()]" + e.getMessage());
              }
              throw new XanbooException(60000, "[getMObject]:" + e.getMessage());
          }
          return mObjects;
     }

 // verify a service subscription
    public boolean verifySubscription (XanbooPrincipal xCaller, ServiceSubscription subs) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[verifySubscription()]:");
        }
        
        // validate the subscription object
        if(subs==null || !XanbooUtil.isValidServiceId(subs.getServiceId()) || subs.getAccountId()!=xCaller.getAccountId()) {
                throw new XanbooException(60050, "Invalid service subscription/account information.");
        }
        
          	
    	 ExternalService extService = ServiceGatewayFactory.getExternalService(subs.getServiceId());
         if(extService == null) {
        	 throw new XanbooException(60050, "Unable to find service reference.");
         }  else {
         
         	//	 getServiceOutboundProxyEJB();
                 try {
                     xap.setAccountPrincipal(xCaller);   //set account principal to pass caller account info
                     return soProxy.verifySubscription(xap, subs);
                 }catch(XanbooException xe) {
                     throw xe;
                 }catch(Exception e) {
                     if(logger.isDebugEnabled()) {
                       logger.error("[verifySubscription()]" + e.getMessage(), e);
                     }else {
                       logger.error("[verifySubscription()]" + e.getMessage());
                     }
                     throw new XanbooException(60037, "[verifySubscription]:" + e.getMessage());
                 }
         	}
         }
    
    // returns service subscriptions for the caller account
    public ServiceSubscription getServiceSubscriptionBySvcId( String serviceId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getServiceSubscription()]:");
        }
        
        // must be a valid service id.
        if(serviceId == null || !XanbooUtil.isValidServiceId(serviceId)) {
            throw new XanbooException(60050, "Please specify service Id.");
        }
        
        Connection conn=null;
        try {
            conn=dao.getConnection();
            return dao.getServiceSubscription(conn, serviceId);
            
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getServiceSubscription()]" + e.getMessage(), e);
            }else {
              logger.error("[getServiceSubscription()]" + e.getMessage());
            }
            throw new XanbooException(60000, "[getServiceSubscription]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }          
    }
}
