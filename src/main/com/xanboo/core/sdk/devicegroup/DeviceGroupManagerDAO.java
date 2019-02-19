/*
 * DeviceGroupManagerDAO.java
 *
 * Created on April 15, 2004, 12:51 PM
 */

package com.xanboo.core.sdk.devicegroup;

import java.util.*;
import java.sql.*;

import com.xanboo.core.util.*;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.model.XanbooGateway;

import com.xanboo.core.security.XanbooPrincipal;

/**
 *
 * @author  Administrator
 */
class DeviceGroupManagerDAO extends BaseHandlerDAO {
    
    private BaseDAO dao;
    private Logger logger;
    
    /** Creates a new instance of DeviceGroupManagerDAO */
    public DeviceGroupManagerDAO() throws XanbooException {
        
        try {
            // obtain a Logger instance
            logger=LoggerFactory.getLogger(this.getClass().getName());
            if(logger.isDebugEnabled()) {
                logger.debug("[DeviceGroupManagerDAO()]:");
            }
            
            // create implementation Class for Oracle, Sybase, etc.
            dao = (BaseDAO) DAOFactory.getDAO();
            
            // get the Connection factory DataSource for CoreDS
            getDataSource(GlobalNames.COREDS);
            
        }catch(XanbooException xe) {
            throw xe;
        }catch(Exception ne) {
            if(logger.isDebugEnabled()) {
                logger.error("[DeviceGroupManagerDAO()] Exception:" + ne.getMessage(), ne);
            }else {
                logger.error("[DeviceGroupManagerDAO()] Exception: " + ne.getMessage());
            }
            throw new XanbooException(20014, "[DeviceGroupManagerDAO()] Exception:" + ne.getMessage());
        }
    }
    
    
    /* Creates a device group - returns guid oc created group */
    public String newDeviceGroup(Connection conn, long accountId, long userId, String aI, String aP, String devI, String devP, String catalogId, String label, String deviceList ) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[newDeviceGroup()]:");
        }
        
        try{
            SQLParam[] args=new SQLParam[9+2];     //SP parameters + 2 std parameters (errno, errmsg)
            
            // setting IN params
            args[0] =  new SQLParam( new Long(accountId), Types.BIGINT );               //account ID
            args[1] =  new SQLParam( new Long(userId), Types.BIGINT );                  //user ID
            args[2] =  new SQLParam( aI );                                              //gateway guid
            args[3] =  new SQLParam( aP );                                              //gateway password
            args[4] =  new SQLParam( devI, Types.VARCHAR );                             //device guid
            args[5] =  new SQLParam( devP );                                            //new device password
            args[6] =  new SQLParam( catalogId );                                       //catalog id
            args[7] =  new SQLParam( label );                                           //device label
            args[8] =  new SQLParam( deviceList );                                      //model
            
            //Call SP
            dao.callSP(conn, "XC_DEVICEGROUP_PKG.NEWDEVICE", args, false);
            
            //Return guid of created group
            return devI;
            
        }catch(XanbooException xe) {
            if (logger.isDebugEnabled()) {
                logger.debug( "[newDeviceGroup()]:" + xe.getErrorMessage());
            }
            throw xe;
        }catch(Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("[newDeviceGroup()]:" + e.getMessage(), e);
            } else {
                logger.debug("[newDeviceGroup()]:" + e.getMessage() );
            }
            throw new XanbooException(10030);  //Exception while executing DAO method;
        }
        
    }
    
}
