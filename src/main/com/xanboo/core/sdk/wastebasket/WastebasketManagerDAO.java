/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/wastebasket/WastebasketManagerDAO.java,v $
 * $Id: WastebasketManagerDAO.java,v 1.11 2003/11/13 23:03:02 guray Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.wastebasket;

import java.util.*;
import java.sql.*;

import com.xanboo.core.util.*;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooPrincipal;

/**
 * This class is the DAO class to wrap all generic database calls for SDK WastebasketManager methods. 
 * Database specific methods are handled by implementation classes. These implementation
 * classes extend the BaseDAO class to actually perform the database operations. An instance of
 * an implementation class is created during contruction of this class.
*/
class WastebasketManagerDAO extends BaseHandlerDAO {

    private BaseDAO dao;
    private Logger logger;
    
    /**
     * Default constructor. Takes no arguments
     *
     * @throws XanbooException
     */
    public WastebasketManagerDAO() throws XanbooException {

        try {
            // obtain a Logger instance
            logger=LoggerFactory.getLogger(this.getClass().getName());
            if(logger.isDebugEnabled()) {
                logger.debug("[WastebasketManagerDAO()]:");
            }

            // create implementation Class for Oracle, Sybase, etc.
            dao = (BaseDAO) DAOFactory.getDAO();

            // get the Connection factory DataSource for CoreDS
            getDataSource(GlobalNames.COREDS);
            
        }catch(XanbooException xe) {
            throw xe;
        }catch(Exception ne) {
            if(logger.isDebugEnabled()) {
              logger.error("[WasteBasketManagerDAO()]: " + ne.getMessage(), ne);
            }else {
              logger.error("[WasteBasketManagerDAO()]: " + ne.getMessage());
            }               
            throw new XanbooException(20014, "[WasteBasketManagerDAO()]:" + ne.getMessage());
        }
    }

    /**
     * Retrieves one of more items in the wastebasket of the specified account and user
     * @param accountId the account id of the user
     * @param userId the user id
     * 
     * @return list of wastebasket items in a XanbooResultSet object
     * @throws XanbooException
     */
    public XanbooResultSet getItemList(Connection conn, XanbooPrincipal xCaller) throws XanbooException {
     if(logger.isDebugEnabled()) {
            logger.debug("[getItemList()]:");
        }
        XanbooResultSet qResults = null;
        SQLParam[] args=new SQLParam[3+2];     // 6 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Integer(-1), Types.INTEGER, true);
 
        qResults = (XanbooResultSet)dao.callSP(conn, "XC_WASTEBASKET_PKG.GETITEMLIST", args);
        qResults.setSize(((Integer) args[2].getParam()).intValue());
        
        return qResults;
    }
    
    
    /**
     * Retrieves one of more items in the wastebasket of the specified account and user
     * given a starting point and number of returned items
     * @param accountId the account id of the user
     * @param userId the user id
     * @param startRow starting row number
     * @param numRows max number of returned rows
     *
     * @return list of wastebasket items in a XanbooResultSet object
     * @throws XanbooException
     */
    public XanbooResultSet getItemList(Connection conn, XanbooPrincipal xCaller, int startRow, int numRows) throws XanbooException {
     if(logger.isDebugEnabled()) {
            logger.debug("[getItemList()]:");
        }
        XanbooResultSet qResults = null;
        
        if ( startRow == 0 && numRows == 0 ) {
            /* If zero rows have been requested, we only need the total count, so use the optimized stored procedure */
            qResults = this.getItemCounts( conn, xCaller );
            qResults.setSize( Integer.parseInt( (String) ((HashMap)qResults.get(0)).get("TRASH_COUNT") ) );
            qResults.clear();
            
        } else {
            SQLParam[] args=new SQLParam[3+2];     // 6 SP parameters + 2 std parameters (errno, errmsg)
            // set IN params
            args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
            args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
            args[2] = new SQLParam(new Integer(-1), Types.INTEGER, true);

            qResults = (XanbooResultSet)dao.callSP(conn, "XC_WASTEBASKET_PKG.GETITEMLIST", args, startRow, numRows);
            qResults.setSize(((Integer) args[2].getParam()).intValue());
        }
        return qResults;
    }
    
    
    /**
     * Deletes on or more items from the user's wastebasket
     * @param xCaller a XanbooPrincipal object
     * @param trashItemIds a list containing one or more trash item id that will be permenantly deleted
     * 
     * @throws XanbooException
     */
    public void deleteItem(Connection conn, XanbooPrincipal xCaller, long trashItemId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteItem()]:");
        }

        SQLParam[] args=new SQLParam[3+2];     // 3 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(Long.toString(trashItemId));
        
        dao.callSP(conn, "XC_WASTEBASKET_PKG.DELETEITEM", args, false);

    }
    
    /**
     * Restores one or more items from the wastebasket to its origianl location
     * @param xCaller a XanbooPrincipal object
     * @param trashItemIds a list containing one or more trash item id that will be restored
     * 
     * @throws XanbooException
     */
    public void undeleteItem(Connection conn, XanbooPrincipal xCaller, long trashItemId, long targetFolderId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[undeleteItem()]:");
        }

        SQLParam[] args=new SQLParam[4+2];     // 3 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(trashItemId), Types.BIGINT);
        args[3] = new SQLParam(new Long(targetFolderId), Types.BIGINT);

        dao.callSP(conn, "XC_WASTEBASKET_PKG.UNDELETEITEM", args, false);
        
    }
    
    /**
     * Deletes all items from the wastebasket, making them permenantly erased
     * @param xCaller a XanbooPrincipal object
     * 
     * @throws XanbooException
     */
    public void emptyWB(Connection conn, XanbooPrincipal xCaller) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[emptyWB()]:");
        }
        SQLParam[] args=new SQLParam[2+2];     // 3 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);

        dao.callSP(conn, "XC_WASTEBASKET_PKG.EMPTYWB", args, false);

    }
    
    /**
     * Retrieves inbox and trash item counts for a specific account
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties are used to identify which inbox to retrieve.
     *
     * @return a XanbooResultSet containing item counts.
     */   
    private XanbooResultSet getItemCounts(Connection conn, XanbooPrincipal xCaller ) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[getItemCounts()]:");
        }
        XanbooResultSet results = null;
        SQLParam[] args=new SQLParam[2+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);

        results = (XanbooResultSet)dao.callSP(conn, "XC_UTIL_PKG.GETITEMCOUNTS", args);

        return results;
    }

}