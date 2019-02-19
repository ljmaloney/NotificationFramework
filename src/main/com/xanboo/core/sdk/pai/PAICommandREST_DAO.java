/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.sdk.pai;

import java.sql.Connection;
import java.sql.Types;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.util.BaseDAO;
import com.xanboo.core.util.BaseHandlerDAO;
import com.xanboo.core.util.DAOFactory;
import com.xanboo.core.util.GlobalNames;
import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;
import com.xanboo.core.util.SQLParam;
import com.xanboo.core.util.XanbooException;

/**
 *
 * @author lm899p
 */
public class PAICommandREST_DAO extends BaseHandlerDAO
{
    private Logger logger = null;
    private BaseDAO dao;
    
    private static final String DELETE_ENTRY = "XC_PAI_REGISTRY_PKG.DELETE_ENTRY";
    private static final String GET_ENTRY = "XC_PAI_REGISTRY_PKG.GET_ENTRY";
    //private static final String UPDATE_ENTRY = "XC_PAI_REGISTRY_PKG.UPDATE_ENTRY";
    private static final String UPDATE_CMD_STATUS = "XC_DEVICE_PKG.updateCommandQueueStatus";

    public PAICommandREST_DAO() throws XanbooException
    {
        try 
        {
            // obtain a Logger instance
            logger=LoggerFactory.getLogger(this.getClass().getName());
            if(logger.isDebugEnabled()) {
                logger.debug("[PAICommandREST_DAO()]:");
            }
            
            // create implementation Class for Oracle, Sybase, etc.
            dao = (BaseDAO) DAOFactory.getDAO();
            
            // get the Connection factory DataSource for CoreDS
            getDataSource(GlobalNames.COREDS);
            
        }
        catch(XanbooException xe) 
        {
            logger.debug("[PAICommandREST_DAO()]: XanbooException " + xe.getErrorMessage());
            throw xe;
        }
        catch(Exception e) 
        {
            if(logger.isDebugEnabled()) 
                logger.debug("[PAICommandREST_DAO()]:" + e.getMessage(), e);
            else 
                logger.debug("[PAICommandREST_DAO()]:" + e.getMessage() );
            throw new XanbooException(20014, e.getLocalizedMessage());
        }
    }
        
    public void deleteEntry(Connection conn,String gguid,String token) throws XanbooException
    {
        if(logger.isDebugEnabled()) 
            logger.debug("[deleteEntry()]: GGUID=" + gguid+" token="+token);

        SQLParam[] args=new SQLParam[2+2];     //SP parameters + 2 std parameters (errno, errmsg)

        // set IN params
        args[0] = new SQLParam(gguid,Types.VARCHAR);
        args[1] = new SQLParam(token,Types.VARCHAR);

        try 
        {
            dao.callSP(conn,DELETE_ENTRY , args, true);
        }
        catch(XanbooException xe) 
        {
            if(logger.isDebugEnabled())
                logger.debug("[deleteEntry()]",xe);
            throw xe;
        }
    }
    public XanbooResultSet getEntry(Connection conn,String gguid) throws XanbooException
    {
        if ( logger.isDebugEnabled() )
            logger.debug("[getEntry] - get registry entry for "+gguid);
        SQLParam[] args = new SQLParam[2+1];
        args[0] = new SQLParam(gguid,Types.VARCHAR);
        XanbooResultSet resultSet = dao.callSP(conn,GET_ENTRY, args);
        /* if ( resultSet != null && !resultSet.isEmpty())
        {
           PAIRegistryEntry entry = new PAIRegistryEntry();
            entry.setGatewayGUID(resultSet.getElementString(0, "GATEWAY_GUID"));
            entry.setConnectTime(resultSet.getElementDate(0,"CONNECTION_DATE"));
            entry.setAccessToken(resultSet.getElementString(0, "ACCESS_TOKEN"));
            entry.setWebSocketSessionId(resultSet.getElementString(0, "SESSION_ID"));
            entry.setPAIServerURI(resultSet.getElementString(0, "LOCAL_PAISERVER_URI"));
            return entry;
        }*/
        return resultSet;
    }
    
    public void updateCommandStatus(Connection conn,Long commandQueueId) throws XanbooException
    {
        if ( logger.isDebugEnabled() )
            logger.debug("[updateCommandStatus] - update commandQueueId="+commandQueueId+" status to \"1\"");
        SQLParam[] args = new SQLParam[2+2]; // commandqueue_id + status_id
        args[0] = new SQLParam(commandQueueId);
        args[1] = new SQLParam(1,Types.INTEGER);
        dao.callSP(conn, UPDATE_CMD_STATUS, args, true);
    }
}
