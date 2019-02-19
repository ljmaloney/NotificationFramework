/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/inbox/InboxManagerDAO.java,v $
 * $Id: InboxManagerDAO.java,v 1.24 2010/11/02 15:19:36 levent Exp $
 *
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.inbox;

import java.util.Date;
import java.util.HashMap;
import java.sql.Connection;
import java.sql.Types;

import com.xanboo.core.util.*;
import com.xanboo.core.model.*;
import com.xanboo.core.sdk.account.*;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.util.fs.FSMountPoint;

/**
 * This class is the DAO class ta wrap all generic database calls for SDK InboxManager methods.
 * Database specific methods are handled by implementation classes. These implementation
 * classes extend the BaseDAO class to actually perform the database operations. An instance of
 * an implementation class is created during contruction of this class.
 */
class InboxManagerDAO extends BaseHandlerDAO {
    
    private BaseDAO dao;
    private Logger logger;
    
    /** Creates new InboxManagerDAO */
    public InboxManagerDAO() throws XanbooException{
        
        try {
            // obtain a Logger instance
            logger=LoggerFactory.getLogger(this.getClass().getName());
            
            // create implementation Class for Oracle, Sybase, etc.
            dao = (BaseDAO) DAOFactory.getDAO();
            
            // get the Connection factory DataSource for CoreDS
            getDataSource(GlobalNames.COREDS);
            
        }catch(XanbooException xe) {
            throw xe;
        }catch(Exception ne) {
            if(logger.isDebugEnabled()) {
              logger.error("[InboxManagerDAO()]: " + ne.getMessage(), ne);
            }else {
              logger.error("[InboxManagerDAO()]: " + ne.getMessage());
            }              
            throw new XanbooException(20014, "[InboxManagerDAO()]: " + ne.getMessage());
        }
        
    }
    
    /**
     * Create a new item in the users inbox
     * @param conn The database connection to use for this transaction
     * @param xCalled The accountId and userId properties of the user are used to validate this call
     * @param item A XanbooItem object from which the new item information is extracted.
     */
    public void addItem(Connection conn, XanbooPrincipal xCaller, XanbooItem item) throws XanbooException {
        
        if(logger.isDebugEnabled()) {
            logger.debug("[addItem()]:");
        }
        SQLParam[] args=new SQLParam[14+2];     // 15 SP parameters + 2 std parameters (errno, errmsg)
        
        // setting IN params
        
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(item.getGatewayGUID(), Types.VARCHAR);
        args[3] = new SQLParam(item.getDeviceGUID(), Types.VARCHAR);
        args[4] = new SQLParam(item.getName(), Types.VARCHAR);
        args[5] = new SQLParam(item.getSourceLabel(), Types.VARCHAR);
        args[6] = new SQLParam(item.getMount(), Types.VARCHAR); //item mount w/ or w/o mount prefix
        args[7] = new SQLParam(item.getItemDirectory(), Types.VARCHAR);
        args[8] = new SQLParam(item.getItemFilename(), Types.VARCHAR);
        args[9] = new SQLParam(item.getItemType(), Types.VARCHAR);
        args[10] = new SQLParam(new Integer((int)item.getItemSize()), Types.INTEGER);
        args[11] = new SQLParam(item.getThumbFilename(), Types.VARCHAR);
        args[12] = new SQLParam(item.getThumbType(), Types.VARCHAR);
        args[13] = new SQLParam(new Integer((int)item.getThumbSize()), Types.INTEGER);
        
        dao.callSP(conn, "XC_INBOX_PKG.ADDITEM", args, false);
        
    }
    
    
    /**
     * Deletes item(s) from a users inbox
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties of the user are used to validate this call
     * @param inboxItemId The ID of the item to delete. If -1, items will be deleted by item attributes
     * @param gatewayGUID Filters items by gateway guid - null for all
     * @param deviceGUID Filters items by device guid - null for all
     * @param contentType Filters items by content type - null for all
     */ 
    public void deleteItem(Connection conn, XanbooPrincipal xCaller, long itemId, String gatewayGUID, String deviceGUID, String contentType) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteItemList()]:");
        }
        
        SQLParam[] args=new SQLParam[6+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(itemId), Types.BIGINT);
        args[3] = new SQLParam( gatewayGUID, Types.VARCHAR );
        args[4] = new SQLParam( deviceGUID, Types.VARCHAR );
        args[5] = new SQLParam( contentType, Types.VARCHAR );
        
        if ( GlobalNames.MODULE_WASTEBASKET_ENABLED ) {
            dao.callSP(conn, "XC_INBOX_PKG.DELETEITEM", args, false);
        } else {
            dao.callSP(conn, "XC_INBOX_PKG.DELETEITEMIMMEDIATE", args, false);
        }
    }

    
    
    /**
     * Deletes all items from a users inbox.
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties of the user are used to validate this call
     */
    public void emptyInbox(Connection conn, XanbooPrincipal xCaller) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[emptyInbox()]:");
        }
        
        SQLParam[] args=new SQLParam[2+2];     // SP parameters + 2 std parameters (errno, errmsg)       // set IN params
        
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        
        if ( GlobalNames.MODULE_WASTEBASKET_ENABLED ) {
            dao.callSP(conn, "XC_INBOX_PKG.EMPTYINBOX", args, false);
        } else {
            dao.callSP(conn, "XC_INBOX_PKG.EMPTYINBOXIMMEDIATE", args, false);
        }
        
    }
    
    
    /**
     * Moves an item from a users inbox to a regular folder
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties of the user are used to validate this call
     * @param inboxItemId the ID of the item to move
     * @param folderId The destination folder
     */
    public void moveItem(Connection conn, XanbooPrincipal xCaller, long inboxItemId, long folderId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[moveItem()]:");
        }
        SQLParam[] args=new SQLParam[4+2];     // 4 SP parameters + 2 std parameters (errno, errmsg)
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(inboxItemId), Types.BIGINT);
        args[3] = new SQLParam(new Long(folderId), Types.BIGINT);
        
        dao.callSP(conn, "XC_INBOX_PKG.MOVEITEM", args, false);
        
    }
    
    
    /**
     * Retrieves items from a users inbox
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties are used to identify which inbox to retrieve.
     * @param gatewayGUID Filters items by gateway guid - null for all
     * @param deviceGUID Filters items by device guid - null for all
     * @param sortBy predefined number representing a sort by field and sorting order
     * and must be one of the {@link InboxManagerEJB#SORT_BY_NAME_ASC sorting numbers} for inbox items
     *
     * @return a XanbooResultSet of all items in the specified account.
     */
    public XanbooResultSet getItemList(Connection conn, XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, int sortBy) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[getItemList()]:");
        }
        XanbooResultSet results = null;
        SQLParam[] args=new SQLParam[6+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Integer(sortBy), Types.INTEGER);
        args[3] = new SQLParam( gatewayGUID, Types.VARCHAR );
        args[4] = new SQLParam( deviceGUID, Types.VARCHAR );
        args[5] = new SQLParam(new Integer(-1), Types.INTEGER, true); //total count
        
        results = (XanbooResultSet)dao.callSP(conn, "XC_INBOX_PKG.GETITEMLIST", args);
        results.setSize(((Integer) args[5].getParam()).intValue());
        
        return results;
    }
    
    /**
     * Retrieves items from a users inbox given a starting point and number or returned items
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties are used to identify which inbox to retrieve.
     * @param gatewayGUID Filters items by gateway guid - null for all
     * @param deviceGUID Filters items by device guid - null for all
     * @param fromDate Filters items by a starting timestamp - null for all
     * @param fromDate Filters items by an ending timestamp - null for all
     * @param contentType Filters items by content type - null for all
     * @param eID If not null, return only items of a particular event ID. 
     * @param startRow starting row number
     * @param numRows max number of returned rows
     * @param sortBy predefined number representing a sort by field and sorting order
     * and must be one of the {@link InboxManagerEJB#SORT_BY_NAME_ASC sorting numbers} for inbox items
     *
     *
     * @return a XanbooResultSet of all items in the specified account.
     */
    public XanbooResultSet getItemList(Connection conn, XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, Date fromDate, Date toDate, String contentType, String eID, int startRow, int numRows, int sortBy ) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[getItemList(start,num)]:");
        }
        
        XanbooResultSet results = null;
        
        if ( startRow == 0 && numRows == 0 ) {
            /* If zero rows have been requested, we only need the total count, so use the optimized stored procedure */
            results = this.getItemCounts( conn, xCaller );
            results.setSize( Integer.parseInt( (String) ((HashMap)results.get(0)).get("INBOX_COUNT") ) );
            results.clear();
            
        } else {
            SQLParam[] args=new SQLParam[12+2];     // SP parameters + 2 std parameters (errno, errmsg)
            // set IN params
            args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
            args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
            args[2] = new SQLParam(new Integer(sortBy), Types.INTEGER);
            args[3] = new SQLParam( gatewayGUID, Types.VARCHAR );
            args[4] = new SQLParam( deviceGUID, Types.VARCHAR );
            args[5] = new SQLParam( fromDate, Types.DATE );
            args[6] = new SQLParam( toDate, Types.DATE );
            args[7] = new SQLParam( contentType, Types.VARCHAR );
            args[8] = new SQLParam( eID, Types.VARCHAR );
            
            args[9] = new SQLParam(new Integer(startRow) , Types.INTEGER );
            args[10] = new SQLParam(new Integer(numRows) , Types.INTEGER );
            
            args[11] = new SQLParam(new Integer(-1), Types.INTEGER, true); //total count
            
            results = (XanbooResultSet)dao.callSP(conn, "XC_INBOX_PKG.GETITEMLISTPAGE", args);
            results.setSize(((Integer) args[11].getParam()).intValue());
            
        }
        
        return results;
    }
    
    
    /**
     * Retrieves correlated items from a users inbox
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties are used to identify which inbox to retrieve.
     * @param gatewayGUID origin gateway guid generating correlated events/items.
     * @param eventGroupId a string identifier shared by all items correlated with an event condition.  .
     *
     * @return a XanbooResultSet of all items in the specified account.
     */
    public XanbooResultSet getItemList(Connection conn, XanbooPrincipal xCaller, String gatewayGUID, String eventGroupId) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[getItemList()]:");
        }
        XanbooResultSet results = null;
        SQLParam[] args=new SQLParam[4+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID, Types.VARCHAR );
        args[3] = new SQLParam(eventGroupId, Types.VARCHAR );
        
        results = (XanbooResultSet)dao.callSP(conn, "XC_INBOX_PKG.GETITEMLIST", args);
        
        return results;
    }
    
    
    /**
     * Retrieves a specific item from a users inbox
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties are used to validate this call
     * @param itemId The ID of the item to retrieve
     */
    public XanbooItem getItem(Connection conn, XanbooPrincipal xCaller, long itemId, boolean returnPrevNext, boolean updateViewCount) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getItem()]:");
        }
        
        XanbooResultSet iTmp = null;
        SQLParam[] args=new SQLParam[5+2];     // 3 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(itemId), Types.BIGINT);
        args[3] = new SQLParam(new Integer((returnPrevNext ? 1 : 0)), Types.INTEGER );  //if true pass non-null
        args[4] = new SQLParam(new Integer((updateViewCount ? 1 : 0)), Types.INTEGER );  //if true pass non-null
        
        iTmp = (XanbooResultSet) dao.callSP(conn, "XC_INBOX_PKG.GETITEM", args);
        if (iTmp.size() == 0) {
            throw new XanbooException(22120, "Failed to get item. Item not found. [" + xCaller.getAccountId() + "/" + itemId + "]");
        }
        
        XanbooItem item = new XanbooItem() ;
        HashMap itemRecord = (HashMap)iTmp.get(0);
        item.setItemId( itemId );
        item.setGatewayGUID( (String)itemRecord.get("GATEWAY_GUID") );
        item.setDeviceGUID( (String)itemRecord.get("DEVICE_GUID") );
        if(itemRecord.get("EVENT_ID")!=null && ((String)itemRecord.get("EVENT_ID")).trim().length()>0) item.setEventId( Integer.parseInt((String)itemRecord.get("EVENT_ID")) );
        item.setName( (String)itemRecord.get("NAME") );
        item.setSourceLabel( (String)itemRecord.get("SOURCE_LABEL"));
        item.setMount( (String)itemRecord.get("ITEM_MOUNT"));
        item.setItemDirectory( (String)itemRecord.get("ITEM_PATH"));
        item.setItemFilename( (String)itemRecord.get("ITEM_FILE"));
        item.setItemType( (String)itemRecord.get("ITEM_CONTENTTYPE"));
        item.setItemSize( Long.parseLong((String) itemRecord.get("ITEM_SIZE")));
        item.setThumbFilename( (String)itemRecord.get("THUMB_FILE")) ;
        item.setThumbType( (String)itemRecord.get("THUMB_CONTENTTYPE"));
        item.setThumbSize( Long.parseLong((String) itemRecord.get("THUMB_SIZE")));
        item.setCreationDate( (String)itemRecord.get("DATE_CREATED"));
        item.setDomain( xCaller.getDomain() );
        item.setAccountId( xCaller.getAccountId() );
        item.setTimestamp( (String)itemRecord.get("TIMESTAMP"));
        if(itemRecord.get("PREV_ITEM_ID") != null && itemRecord.get("PREV_ITEM_ID") != "" ) item.setPrevItemId( Long.parseLong((String) itemRecord.get("PREV_ITEM_ID")));
        if(itemRecord.get("NEXT_ITEM_ID") != null && itemRecord.get("NEXT_ITEM_ID") != "" ) item.setNextItemId( Long.parseLong((String) itemRecord.get("NEXT_ITEM_ID")));

        item.setEventGroupId( (String)itemRecord.get("EGROUP_ID"));
        item.setSourceDeviceGUID( (String)itemRecord.get("SRC_DEVICE_GUID"));
        item.setAccessKeyId( (String)itemRecord.get("ACCESSKEY_ID"));
        try {
            item.setViewCount(Integer.parseInt( (String) itemRecord.get("VIEW_COUNT")));
        }catch(Exception ee) { item.setViewCount(0); };
        
        return item;
        
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
