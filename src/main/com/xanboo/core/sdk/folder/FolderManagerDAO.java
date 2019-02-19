/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/folder/FolderManagerDAO.java,v $
 * $Id: FolderManagerDAO.java,v 1.32 2008/09/25 18:39:51 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.folder;

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
 * This class is the DAO class to wrap all generic database calls for SDK FolderManager methods. 
 * Database specific methods are handled by implementation classes. These implementation
 * classes extend the BaseDAO class to actually perform the database operations. An instance of
 * an implementation class is created during contruction of this class.
*/
class FolderManagerDAO extends BaseHandlerDAO {

    private BaseDAO dao;
    private Logger logger;

    /** Creates new FolderManagerDAO */
    public FolderManagerDAO() throws XanbooException{

        try {
            // obtain a Logger instance
            logger=LoggerFactory.getLogger(this.getClass().getName());
            if(logger.isDebugEnabled()) {
                logger.debug("[FolderManagerDAO()]:");
            }

            // create implementation Class for Oracle, Sybase, etc.
            dao = (BaseDAO) DAOFactory.getDAO();

            // get the Connection factory DataSource for CoreDS
            getDataSource(GlobalNames.COREDS);
            
        }catch(XanbooException xe) {
            throw xe;
        }catch(Exception ne) {
            if(logger.isDebugEnabled()) {
              logger.error("[FolderManagerDAO()]: " + ne.getMessage(), ne);
            }else {
              logger.error("[FolderManagerDAO()]: " + ne.getMessage());
            }
            throw new XanbooException(20014, "[FolderManagerDAO()]:" + ne.getMessage());
        }
    }

    /**
     * Retrives a list of folders for a specific account
     * @param conn The database connection to use for this call
     * @param xCaller  The accountId and userId properties are used to authenticate this call
     * @param sortBy predefined number representing a sort by field and sorting order
     * and must be one of the {@link FolderManagerEJB#SORT_BY_FOLDERNAME_ASC sorting numbers} for folders
     *
     * @return a XanbooResultSet of folder ids and names
    */
     public XanbooResultSet getFolderList(Connection conn, XanbooPrincipal xCaller, int sortBy) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[getFolderList()]:");
        }

        XanbooResultSet results = null;
        SQLParam[] args=new SQLParam[4+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Integer(sortBy), Types.INTEGER);
        args[3] = new SQLParam(new Integer(-1), Types.INTEGER, true);   /* total num of folders as return value */

        try {
            results = dao.callSP(conn, "XC_FOLDER_PKG.GETFOLDERLIST", args);
            results.setSize(((Integer) args[3].getParam()).intValue());
        }catch(XanbooException xe) {
            throw xe;
        }
        
        return results;
    }
    
    /**
     * Retrives a list of folders for a specific account
     * given a start point and number of returned folers
     * @param conn The database connection to use for this call
     * @param xCaller  The accountId and userId properties are used to authenticate this call
     * @param startRow starting row number 
     * @param numRows max number of returned rows
     * @param sortBy predefined number representing a sort by field and sorting order
     * and must be one of the {@link FolderManagerEJB#SORT_BY_FOLDERNAME_ASC sorting numbers} for folders
     *
     * @return a XanbooResultSet of folder ids and names
    */
     public XanbooResultSet getFolderList(Connection conn, XanbooPrincipal xCaller, int startRow, 
                                            int numRows, int sortBy) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[getFolderList()]:");
        }
        
        XanbooResultSet results = null;
        SQLParam[] args=new SQLParam[4+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Integer(sortBy), Types.INTEGER);
        args[3] = new SQLParam(new Integer(-1), Types.INTEGER, true);
        
        try {
            results = (XanbooResultSet)dao.callSP(conn, "XC_FOLDER_PKG.GETFOLDERLIST", args, startRow, numRows);
            results.setSize(((Integer) args[3].getParam()).intValue());
        }catch(XanbooException xe) {
            throw xe;
        }
        
        return results;
    }

    /**
     * Adds a new folder belonging to the specified user
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties are used to authenticate this call
     * @param folder The folder to create - containing name/description etc
    */    
    public long addFolder(Connection conn, XanbooPrincipal xCaller, XanbooFolder folder) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[addFolder()]:");
        }

        long folderId = -1;

        SQLParam[] args=new SQLParam[6+2];     // 4 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(folder.getName());
        args[3] = new SQLParam(folder.getDescription());
        args[4] = new SQLParam(new Long(folder.getParentFolderId()), Types.BIGINT);
        args[5] = new SQLParam(new Long(-1), Types.BIGINT, true); 

        try {
            dao.callSP(conn, "XC_FOLDER_PKG.ADDFOLDER", args, false);
            folderId = ((Long) args[5].getParam()).intValue();            
          //  folderId = ((Long) args[3].getParam()).longValue();
        }catch(XanbooException xe) {
            throw xe;
        }
        return folderId;
    }    
    
    /**
     * Removes a folder from a specified account. Items in this folder will be moved to the wastebasket
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties are used to authenticate this call
     * @param folderId The folder to be deleted
     * @param forceFlag If true, a folder will be deleted even if it contains items. If false, the deletetion of a non-empty folder will fail
    */    
    public void deleteFolder(Connection conn, XanbooPrincipal xCaller, long folderId, boolean forceFlag) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteFolder()]:");
        }
        int fFlag = 0;
        if (forceFlag) { fFlag = 1 ;}

        SQLParam[] args=new SQLParam[4+2];     // 4 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(folderId), Types.BIGINT); 
        args[3] = new SQLParam(new Integer(fFlag), Types.INTEGER); 

        if ( GlobalNames.MODULE_WASTEBASKET_ENABLED ) {
            dao.callSP(conn, "XC_FOLDER_PKG.DELETEFOLDER", args, false);
        } else {
            dao.callSP(conn, "XC_FOLDER_PKG.DELETEFOLDERIMMEDIATE", args, false);
        }
    }
    
    /**
     * Allows updates to the name and description of a folder
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties are used to authenticate this call
     * @param folder The name and description attributes of this object are applied to the folder
     */
    public void updateFolder(Connection conn, XanbooPrincipal xCaller, XanbooFolder folder) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[updateFolder()]:");
        }

        SQLParam[] args=new SQLParam[6+2];     // 4 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(folder.getFolderId()), Types.BIGINT); 
        args[3] = new SQLParam(folder.getName()); 
        args[4] = new SQLParam(folder.getDescription()); 
        args[5] = new SQLParam(new Long(folder.getParentFolderId()), Types.BIGINT); 

        try {
            dao.callSP(conn, "XC_FOLDER_PKG.UPDATEFOLDER", args, false);
        }catch(XanbooException xe) {
            throw xe;
        }
    }    
    
    
    /**
     * Clears the contents of a folder to the wastebasket
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties are used to authenticate this call.
     * @param folderId The ID of the folder to empty. This folder is not deleted.
    */
    public void emptyFolder(Connection conn, XanbooPrincipal xCaller, long folderId) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[emptyFolder()]:");
        }

        SQLParam[] args=new SQLParam[3+2];     // 3 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(folderId), Types.BIGINT); 

        if ( GlobalNames.MODULE_WASTEBASKET_ENABLED ) {
            dao.callSP(conn, "XC_FOLDER_PKG.EMPTYFOLDER", args, false);
        } else {
            dao.callSP(conn, "XC_FOLDER_PKG.EMPTYFOLDERIMMEDIATE", args, false);
        }
    }    

    
    /**
     * Retrieves a list of items that exist in a particular folder
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties are used to authenticate this call.
     * @param folderId The ID of the folder from which we want to retrieve items
     * @param gatewayGUID Filters items by gateway guid - null for all
     * @param deviceGUID Filters items by device guid - null for all
     * @param sortBy predefined number representing a sort by field and sorting order
     * and must be one of the {@link FolderManagerEJB#SORT_BY_ITEMNAME_ASC sorting numbers} for folder items
     *
     * @return a XanbooResultSet of itemsIds, and some limited meta data.
    */
    public XanbooResultSet getItemList(Connection conn, XanbooPrincipal xCaller, long folderId, String gatewayGUID, String deviceGUID, int sortBy ) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[getItemList()]:");
        }
        XanbooResultSet results = null;
        SQLParam[] args=new SQLParam[7+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(folderId), Types.BIGINT); //folder ID
        args[3] = new SQLParam(new Integer(sortBy), Types.INTEGER);
        args[4] = new SQLParam(gatewayGUID, Types.VARCHAR);
        args[5] = new SQLParam(deviceGUID, Types.VARCHAR);
        args[6] = new SQLParam(new Integer(-1), Types.INTEGER, true);
        
        try {
            results = (XanbooResultSet)dao.callSP(conn, "XC_FOLDER_PKG.GETITEMLIST", args);
            results.setSize(((Integer) args[6].getParam()).intValue());
        }catch(XanbooException xe) {
            throw xe;
        }

        return results;
    }

    
    
    /**
     * Retrieves a list of items that exist in a particular folder
     * given a start point and number of returned folers
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties are used to authenticate this call.
     * @param folderId The ID of the folder from which we want to retrieve items
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
    public XanbooResultSet getItemList(Connection conn, XanbooPrincipal xCaller, long folderId, String gatewayGUID, String deviceGUID, java.util.Date fromDate, java.util.Date toDate, String contentType, String eID, int startRow, int numRows, int sortBy ) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[getItemList()]:");
        }

        XanbooResultSet results = null;
        SQLParam[] args=new SQLParam[11+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(folderId), Types.BIGINT); //folder ID
        args[3] = new SQLParam(new Integer(sortBy), Types.INTEGER);
        args[4] = new SQLParam(gatewayGUID, Types.VARCHAR);
        args[5] = new SQLParam(deviceGUID, Types.VARCHAR);
        args[6] = new SQLParam( fromDate, Types.DATE );
        args[7] = new SQLParam( toDate, Types.DATE );
        args[8] = new SQLParam( contentType, Types.VARCHAR );
        args[9] = new SQLParam( eID, Types.VARCHAR );
        
        args[10] = new SQLParam(new Integer(-1), Types.INTEGER, true);

        try {
            results = (XanbooResultSet)dao.callSP(conn, "XC_FOLDER_PKG.GETITEMLIST", args, startRow, numRows);
            results.setSize(((Integer) args[10].getParam()).intValue());
        }catch(XanbooException xe) {
            throw xe;
        }
        return results;
    }
    
    
    /**
     * Retrieves correlated items from user folders
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
        
        results = (XanbooResultSet)dao.callSP(conn, "XC_FOLDER_PKG.GETITEMLIST", args);
        
        return results;
    }    
    
    
    /**
     * Retrieves a specific item from a users folder
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties are used to authenticate this call
     * @param itemId The ID of the item to retrieve
     * @return A XanbooItem of the requested ID
    */        
    public XanbooItem getItem(Connection conn, XanbooPrincipal xCaller, long itemId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getItem()]:");
        }

        XanbooResultSet iTmp = null;
        SQLParam[] args=new SQLParam[3+2];     // 3 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(itemId), Types.BIGINT);

        try {
            iTmp = (XanbooResultSet) dao.callSP(conn, "XC_FOLDER_PKG.GETITEM", args);
            if (iTmp.size() == 0) {
                throw new XanbooException(22120, "Failed to get item. Item not found.");
            }
        }catch(XanbooException xe) {
            throw xe;
        }

        XanbooItem item = new XanbooItem() ;
        HashMap itemRecord = (HashMap)iTmp.get(0);
        item.setItemId( itemId );
        item.setGatewayGUID( (String)itemRecord.get("GATEWAY_GUID") );
        item.setDeviceGUID( (String)itemRecord.get("DEVICE_GUID") );
        if(itemRecord.get("EVENT_ID")!=null && ((String)itemRecord.get("EVENT_ID")).trim().length()>0) item.setEventId( Integer.parseInt((String)itemRecord.get("EVENT_ID")) );
        item.setName( (String)itemRecord.get("NAME") );
        item.setDomain( xCaller.getDomain());
        item.setAccountId( xCaller.getAccountId() );        
        item.setSourceLabel( (String)itemRecord.get("SOURCE_LABEL"));
        item.setMount( (String)itemRecord.get("ITEM_MOUNT"));
        item.setItemDirectory( (String)itemRecord.get("ITEM_PATH"));
        item.setItemFilename( (String)itemRecord.get("ITEM_FILE"));
        item.setItemType( (String)itemRecord.get("ITEM_CONTENTTYPE"));
        item.setItemSize( Long.parseLong((String) itemRecord.get("ITEM_SIZE")));
        item.setThumbSize( Long.parseLong((String) itemRecord.get("THUMB_SIZE")));
        item.setThumbFilename( (String)itemRecord.get("THUMB_FILE")) ;
        item.setThumbType( (String)itemRecord.get("THUMB_CONTENTTYPE"));
        item.setStatus( Integer.parseInt((String)itemRecord.get("STATUS_ID")) );
        item.setCreationDate( (String)itemRecord.get("DATE_CREATED"));
        item.setTimestamp( (String)itemRecord.get("TIMESTAMP"));
        item.setViewCount( Integer.parseInt((String)itemRecord.get("VIEW_COUNT")));
        if(itemRecord.get("PREV_ITEM_ID") != null && itemRecord.get("PREV_ITEM_ID") != "" ) item.setPrevItemId( Long.parseLong((String) itemRecord.get("PREV_ITEM_ID")));
        if(itemRecord.get("NEXT_ITEM_ID") != null && itemRecord.get("NEXT_ITEM_ID") != "" ) item.setNextItemId( Long.parseLong((String) itemRecord.get("NEXT_ITEM_ID")));

        item.setEventGroupId( (String)itemRecord.get("EGROUP_ID"));
        item.setSourceDeviceGUID( (String)itemRecord.get("SRC_DEVICE_GUID"));
        item.setAccessKeyId( (String)itemRecord.get("ACCESSKEY_ID"));
        
        return item;

    }

    /**
     * Retrieves a specific folder
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties are used to authenticate this call
     * @param folderId The ID of the folder to retrieve
     * @return A XanbooFolder of the requested ID
    */        
    public XanbooFolder getFolder(Connection conn, XanbooPrincipal xCaller, long folderId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getFolder()]:");
        }

        XanbooResultSet iTmp = null;
        SQLParam[] args=new SQLParam[3+2];     // 3 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(folderId), Types.BIGINT);

        try {
            iTmp = (XanbooResultSet) dao.callSP(conn, "XC_FOLDER_PKG.GETFOLDER", args);
            if (iTmp.size() == 0) {
                throw new XanbooException(22120, "Failed to get item. Item not found.");
            }
        }catch(XanbooException xe) {
            throw xe;
        }

        XanbooFolder folder = new XanbooFolder() ;
        HashMap f = (HashMap)iTmp.get(0);
        folder.setAccountId( xCaller.getAccountId() );
        folder.setFolderId( folderId );
        folder.setParentFolderId( Integer.parseInt((String)f.get("PARENT_FOLDER_ID")) );
        folder.setName( (String)f.get("NAME"));
        folder.setDescription( (String)f.get("DESCRIPTION"));
        if (f.get("SUBFOLDER_COUNT") != null && (String)f.get("SUBFOLDER_COUNT") != "") {
            folder.setSubfolderCount( Integer.parseInt((String)f.get("SUBFOLDER_COUNT")) );
        }
        folder.setItemCount( Integer.parseInt((String)f.get("ITEM_COUNT")) );
        if (f.get("TYPE") != null && (String)f.get("TYPE") != "") {
            folder.setType( Integer.parseInt((String)f.get("TYPE")) );
        }
        if (f.get("ISPUBLIC") != null && (String)f.get("ISPUBLIC") != "") {
            folder.setIsPublic( Integer.parseInt((String)f.get("ISPUBLIC")) );
        }
        folder.setCreationDate( (String)f.get("DATE_CREATED") );
        return folder;

    }    
    
    
    /**
     * Adds an item to a users folder
     * @param conn The database connection to use for this transaction
     * @param xCaller The accountId and userId properties are used to authenticate this call
     * @param item An object representing the item to be added
     * @param folderId The id of the folder in which to add the item
    */    
    public void addItem(Connection conn, XanbooPrincipal xCaller, XanbooItem item, long folderId) throws XanbooException {

        if(logger.isDebugEnabled()) {
            logger.debug("[addItem()]:");
        }

        SQLParam[] args=new SQLParam[17+2];     // 16 SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params        
  
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(folderId), Types.BIGINT);
        args[3] = new SQLParam(null, Types.BIGINT);
        args[4] = new SQLParam(item.getGatewayGUID(), Types.VARCHAR);
        args[5] = new SQLParam(item.getDeviceGUID(), Types.VARCHAR);
        args[6] = new SQLParam(item.getName(), Types.VARCHAR);
        args[7] = new SQLParam(item.getSourceLabel(), Types.VARCHAR);
        args[8] = new SQLParam(item.getMount(), Types.VARCHAR); //item mount with or wo mount prefix
        args[9] = new SQLParam(item.getItemDirectory(), Types.VARCHAR);
        args[10] = new SQLParam(item.getItemFilename(), Types.VARCHAR);
        args[11] = new SQLParam(item.getItemType(), Types.VARCHAR);
        args[12] = new SQLParam(new Integer((int)item.getItemSize()), Types.INTEGER);
        args[13] = new SQLParam(item.getThumbFilename(), Types.VARCHAR);
        args[14] = new SQLParam(item.getThumbType(), Types.VARCHAR);
        args[15] = new SQLParam(new Integer((int)item.getThumbSize()), Types.INTEGER);
        args[16] = new SQLParam(new Integer((int)item.getViewCount()), Types.INTEGER);

        try {
            dao.callSP(conn, "XC_FOLDER_PKG.ADDITEM", args, false);
        }catch(XanbooException xe) {
            throw xe;
        }
    }    

    
    /**
     * Deletes an item or items from a users inbox
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties of the user are used to validate this call
     * @param folderItemId The ID of the item to delete. If -1, items will be deleted by item attributes
     * @param folderId Filters items by containing folder id - -1 to delete across folders
     * @param gatewayGUID Filters items by gateway guid - null for all
     * @param deviceGUID Filters items by device guid - null for all
     * @param contentType Filters items by content type - null for all
     */ 
    public void deleteItem(Connection conn, XanbooPrincipal xCaller, long folderItemId, long folderId, String gatewayGUID, String deviceGUID, String contentType) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteItem()]:");
        }
        
        SQLParam[] args=new SQLParam[7+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(folderItemId), Types.BIGINT);
        args[3] = new SQLParam(new Long(folderId), Types.BIGINT);
        args[4] = new SQLParam( gatewayGUID, Types.VARCHAR );
        args[5] = new SQLParam( deviceGUID, Types.VARCHAR );
        args[6] = new SQLParam( contentType, Types.VARCHAR );
        
        if ( GlobalNames.MODULE_WASTEBASKET_ENABLED ) {
            dao.callSP(conn, "XC_FOLDER_PKG.DELETEITEM", args, false);
        } else {
            dao.callSP(conn, "XC_FOLDER_PKG.DELETEITEMIMMEDIATE", args, false);
        }
    }
    
    
    /**
     * Moves item(s) from one folder to another
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties of the user are used to authenticate this call
     * @param itemId An array of item ids to move
     * @param folderId The destination folder of the item(s)
    */
    public void moveItem(Connection conn, XanbooPrincipal xCaller, long itemId, long folderId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[moveItem()]:");
        }
        
        SQLParam[] args=new SQLParam[4+2];     // 4 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(itemId), Types.BIGINT);
        args[3] = new SQLParam(new Long(folderId), Types.BIGINT);

        try {
            dao.callSP(conn, "XC_FOLDER_PKG.MOVEITEM", args, false);
        }catch(XanbooException xe) {
            throw xe;
        }
    }

    
    /**
     * Copy item(s) from one folder to another
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties of the user are used to validate this call
     * @param itemIds An array of item ids to copy
     * @param folderId The destination folder of the item(s)
    */    
    public void copyItem(Connection conn, XanbooPrincipal xCaller, long itemId, long folderId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[copyItem()]:");
        }
        SQLParam[] args=new SQLParam[4+2];     // 4 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(itemId), Types.BIGINT);
        args[3] = new SQLParam(new Long(folderId), Types.BIGINT);

        try {
            dao.callSP(conn, "XC_FOLDER_PKG.COPYITEM", args, false);
        }catch(XanbooException xe) {
            throw xe;
        }
    }

    
    /**
     * Updates the name of an item
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties of the user are used to authenticate this call
     * @param itemId The ID of the item to update
     * @param name The new name to assign to the item
    */
    public void updateItem(Connection conn, XanbooPrincipal xCaller, long itemId, String name) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[updateItem()]:");
        }

        SQLParam[] args=new SQLParam[4+2];     // 4 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(itemId), Types.BIGINT);
        args[3] = new SQLParam(name);

        try {
            dao.callSP(conn, "XC_FOLDER_PKG.UPDATEITEM", args, false);
        }catch(XanbooException xe) {
            throw xe;
        }
    }

    
    /**
     * Adds a note to an item
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties of the user are used to authenticate this call
     * @param itemIds The ID of the item on which to add the note
     * @param note The text to associated with this item
    */    
    public void addNote(Connection conn, XanbooPrincipal xCaller, long itemId, String note) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[addNote()]:");
        }

        SQLParam[] args=new SQLParam[4+2];     // 4 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(itemId), Types.BIGINT);
        args[3] = new SQLParam(note);

        try {
            dao.callSP(conn, "XC_FOLDER_PKG.ADDNOTE", args, false);
        }catch(XanbooException xe) {
            throw xe;
        }
    }    
    

    
    
 
    /**
     * Retrieves a list of notes associated with an item.
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties are used to authenticate this call.
     * @param itemId The item from which to retrieve ntoes
     * @return A XanbooResultSet of invitees who recieved the specified invitation
     */
    public XanbooResultSet getNotes(Connection conn, XanbooPrincipal xCaller, long itemId ) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[getNotes()]:");
        }

        SQLParam[] args=new SQLParam[3+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(itemId), Types.BIGINT); //itemId
        try {
            return dao.callSP(conn, "XC_FOLDER_PKG.getItemNotes", args );
        }catch(XanbooException xe) {
            throw xe;
        }
    }    

    /**
     * Deletes a note associated with an item
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties are used to authenticate this call
     * @param noteId The Id of the note to delete
    */    
    public void deleteNote(Connection conn, XanbooPrincipal xCaller, long noteId ) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteNote()]:");
        }

        SQLParam[] args=new SQLParam[3+2];     // 4 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(noteId), Types.BIGINT); 

        try {
            dao.callSP(conn, "XC_FOLDER_PKG.DELETEITEMNOTE", args, false);
        }catch(XanbooException xe) {
            throw xe;
        }
    }
    
}