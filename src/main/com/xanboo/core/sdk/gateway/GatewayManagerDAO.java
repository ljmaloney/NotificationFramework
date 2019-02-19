/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/gateway/GatewayManagerDAO.java,v $
 * $Id: GatewayManagerDAO.java,v 1.4 2004/07/12 16:04:01 rking Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.gateway;

import java.util.*;
import java.sql.*;

import com.xanboo.core.util.*;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.model.XanbooGateway;

import com.xanboo.core.security.XanbooPrincipal;

/**
 * This class is the DAO class to wrap all generic database calls for SDK GatewayManager methods. 
 * Database specific methods are handled by implementation classes. These implementation
 * classes extend the BaseDAO class to actually perform the database operations. An instance of
 * an implementation class is created during contruction of this class.
 */
class GatewayManagerDAO extends BaseHandlerDAO {
    
    private BaseDAO dao;
    private Logger logger;
    
    /** Creates a new instance of DeviceGroupManagerDAO */
    public GatewayManagerDAO() throws XanbooException {
        
        try {
            // obtain a Logger instance
            logger=LoggerFactory.getLogger(this.getClass().getName());
            if(logger.isDebugEnabled()) {
                logger.debug("[GatewayManagerDAO()]:");
            }
            
            // create implementation Class for Oracle, Sybase, etc.
            dao = (BaseDAO) DAOFactory.getDAO();
            
            // get the Connection factory DataSource for CoreDS
            getDataSource(GlobalNames.COREDS);
            
        }catch(XanbooException xe) {
            throw xe;
        }catch(Exception ne) {
            if(logger.isDebugEnabled()) {
                logger.error("[GatewayManagerDAO()] Exception:" + ne.getMessage(), ne);
            }else {
                logger.error("[GatewayManagerDAO()] Exception: " + ne.getMessage());
            }
            throw new XanbooException(20014, "[GatewayManagerDAO()] Exception:" + ne.getMessage());
        }
    }
    
    
    public String getAccessKeyMobjectId(Connection conn, XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String baseOID, String value ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getAccessKeyMobjectId()]:");
        }
        
        SQLParam[] args=new SQLParam[7+2];     // 3 SP parameters + 2 std parameters (errno, errmsg)
        
        // setting IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID, Types.VARCHAR);
        args[3] = new SQLParam(deviceGUID, Types.VARCHAR);
        args[4] = new SQLParam(baseOID, Types.VARCHAR);
        args[5] = new SQLParam(value, Types.VARCHAR);
        
        // OUT params - mobject ID
        args[6] = new SQLParam(new String(), Types.VARCHAR, true);

        try {
            dao.callSP(conn, "XC_DEVICE_PKG.getAccessKeyMobjectId", args, false);
            return (String) args[6].getParam();
            
        } catch(XanbooException xe) {
            throw xe;
        }
        
    }    
    
    
    
}
