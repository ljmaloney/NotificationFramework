/*
 * DeviceGroupManagerEJB.java
 *
 * Created on April 15, 2004, 12:51 PM
 */

package com.xanboo.core.sdk.devicegroup;

import java.sql.Connection;

import javax.annotation.PostConstruct;
import javax.ejb.CreateException;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import com.xanboo.core.model.XanbooGateway;
import com.xanboo.core.sdk.AbstractSDKManagerEJB;
import com.xanboo.core.sdk.device.DeviceManager;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.EjbProxy;
import com.xanboo.core.util.GlobalNames;
import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;
import com.xanboo.core.util.XanbooException;
import com.xanboo.core.util.XanbooUtil;

/**
 *
 * @author  Administrator
 */
@Stateless (name="DeviceGroupManager")
@TransactionManagement( TransactionManagementType.BEAN )
@Remote(DeviceGroupManager.class)

public class DeviceGroupManagerEJB   {

    private static final String OID_DEVICE_LIST = "1500"; //group descriptor oid that contains device list
    private static final String OID_LABEL = "0"; //label oid
    private static final String DEVICE_CLASS_GROUP = "1100"; //device group class ID
    
    private static final String SUPPORTED_DEVICE_GROUP_CATALOG = "000001100000003";
    
    //private SessionContext context;
    private Logger logger;
    private DeviceGroupManagerDAO dao = null;
   
    private DeviceManager dManager = null;
    
    @PostConstruct
    public void init() throws CreateException {

        try {
            // create a logger instance
            logger=LoggerFactory.getLogger(this.getClass().getName());
            if(logger.isDebugEnabled()) {
                logger.debug("[ejbCreate()]:");
            }
            EjbProxy proxy = new EjbProxy();
            dao = new DeviceGroupManagerDAO();
            dManager = (DeviceManager) proxy.getObj(GlobalNames.EJB_DEVICE_MANAGER);
            
        }catch (Exception se) {
            if(logger.isDebugEnabled()) {
              logger.error("[init()]: " + se.getMessage(), se);
            }else {
              logger.error("[init()]: " + se.getMessage());
            }                
            throw new CreateException("Failed @ init DeviceGroupManager:" + se.getMessage());
        }
    }
    
 
    /* Creates a new device group */
    public String newDeviceGroup( XanbooPrincipal xCaller, String gatewayGUID, String label, String deviceList ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[newDeviceGroup()]:");
        }

        
        // validate the input parameters
        if(xCaller == null || gatewayGUID==null || gatewayGUID.trim().equals("") ) {
            throw new XanbooException(10050);
        } else if ( label == null || label.trim().equals("") ) {
            throw new XanbooException(31010, "Missing parameter. Group label is required.");
        }
        
        
        boolean rollback = true;
        String deviceId = null;
        
        XanbooGateway gwyInfo = null;
        
        Connection conn=null;
        try {
            // first validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            conn=dao.getConnection();
            
            //Null parameters are gateway password and device password - these are calculated automagically by the SP if not supplied.
                                                                                                                       //000001100000000
            deviceId = XanbooUtil.generateGUID("G", 10);    //generate 10-digit dguid
            dao.newDeviceGroup(conn, xCaller.getAccountId(), xCaller.getUserId(), gatewayGUID, null, deviceId, null, SUPPORTED_DEVICE_GROUP_CATALOG, label, deviceList );
            
            rollback = false;

            gwyInfo = dManager.getGatewayInfo( xCaller, gatewayGUID);
            AbstractSDKManagerEJB.pollGateway(gwyInfo);
            
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[newDeviceGroup()]: " + e.getMessage(), e);
            }else {
              logger.error("[newDeviceGroup()]: " + e.getMessage());
            }                            
            throw new XanbooException(10030, "[newDeviceGroup]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }

        return deviceId;
    }

    /* Updates label and device list for an existing device group */
    public String updateDeviceGroup( XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String label, String deviceList ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateDeviceGroup()]:");
        }
        
        try {
            String[] oids = {OID_LABEL, OID_DEVICE_LIST};
            String[] values = { label, deviceList };
            this.dManager.setMObject( xCaller, gatewayGUID, deviceGUID, oids, values );
        } catch ( XanbooException xe ) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[updateDeviceGroup()]: " + e.getMessage(), e);
            }else {
              logger.error("[updateDeviceGroup()]: " + e.getMessage());
            }                            
            throw new XanbooException(10030, "[updateDeviceGroup]:" + e.getMessage());
        } 
        
        return null;
        
        
    }
    
    public XanbooResultSet getDeviceGroupList( XanbooPrincipal xCaller ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getDeviceGroupList()]:");
        }
        
        try {
            return this.dManager.getDeviceListByClass(xCaller, DEVICE_CLASS_GROUP);
        } catch ( XanbooException xe ) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getDeviceGroupList()]: " + e.getMessage(), e);
            }else {
              logger.error("[getDeviceGroupList()]: " + e.getMessage());
            }                            
            throw new XanbooException(10030, "[getDeviceGroupList]:" + e.getMessage());
        } 
        
    }
    
    public XanbooResultSet getMObject(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String mobjectId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getMObject()]: ");
        }
        
        try {
            return this.dManager.getMObject(xCaller, gatewayGUID, deviceGUID, mobjectId);
        } catch ( XanbooException xe ) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getMObject()]: " + e.getMessage(), e);
            }else {
              logger.error("[getMObject()]: " + e.getMessage());
            }                            
            throw new XanbooException(10030, "[getMObject]:" + e.getMessage());
        } 
        
    }    

    
    public void deleteDeviceGroup(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteDeviceGroup()]: ");
        }
        
        try {
            this.dManager.deleteDevice(xCaller, gatewayGUID, deviceGUID);
        } catch ( XanbooException xe ) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[deleteDeviceGroup()]: " + e.getMessage(), e);
            }else {
              logger.error("[deleteDeviceGroup()]: " + e.getMessage());
            }                            
            throw new XanbooException(10030, "[deleteDeviceGroup]:" + e.getMessage());
        } 
        
    }

	


}
