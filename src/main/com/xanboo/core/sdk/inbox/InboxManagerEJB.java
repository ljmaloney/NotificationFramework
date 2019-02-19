/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/inbox/InboxManagerEJB.java,v $
 * $Id: InboxManagerEJB.java,v 1.27 2011/07/07 21:36:40 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */


package com.xanboo.core.sdk.inbox;

import java.sql.Connection;
import java.util.Date;

import com.xanboo.core.sdk.AbstractSDKArchiveManagerEJB;
import com.xanboo.core.util.*;
import com.xanboo.core.util.fs.AbstractFSProvider;
import com.xanboo.core.model.*;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.util.fs.XanbooFSProviderProxy;

import java.util.StringTokenizer;

import javax.annotation.PostConstruct;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

/**
 * Session Bean implementation of <code>InboxManager</code>. This bean acts as a wrapper class for
 * all inbox and inbox item related Core SDK methods.
 *
*/
@Remote (InboxManager.class)
@Stateless (name  = "InboxManager")
@TransactionManagement( TransactionManagementType.BEAN )
public class InboxManagerEJB extends AbstractSDKArchiveManagerEJB   {

    private InboxManagerDAO dao=null;    
    
    //define sort by fields, in ascending and descending orders   
    /** 
     * Sort by inbox item name in an ascending order
     */
    public static final int SORT_BY_NAME_ASC = 1;
    /** 
     * Sort by inbox item name in a descending order
     */
    public static final int SORT_BY_NAME_DESC = -1;    
    /** 
     * Sort by item source (appliance name) in an ascending order
     */
    public static final int SORT_BY_SOURCE_ASC = 2;  
    /** 
     * Sort by item source (appliance name) in a descending order
     */
    public static final int SORT_BY_SOURCE_DESC = -2;
    /** 
     * Sort by time when item was received in an ascending order
     */
    public static final int SORT_BY_TIME_ASC = 3;  
    /** 
     * Sort by time when item was received in a descending order, default
     */
    public static final int SORT_BY_TIME_DESC = -3; 
     /**
     * Maximum or minimum number (if negative) that could be used to define sorting
     */
    private final int SORT_BY_LIMIT = 3;    //for range check

    @PostConstruct
    public void init() throws Exception {
        dao = new InboxManagerDAO();
    }


    //--------------- Business methods ------------------------------------------------------


    public long addItem(XanbooPrincipal xCaller, XanbooItem item) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[addItem()]:");
        }
        
        // first validate the caller and privileges
        XanbooUtil.checkCallerPrivilege(xCaller);

        if ( xCaller.getUserId() < 1 || xCaller.getAccountId() < 1 || xCaller.getDomain()==null || xCaller.getDomain().length()==0 || 
                item.getItemType()==null || item.getItemType().length()==0 || item.getItemFilename()==null || item.getItemFilename().length()==0) {
            throw new XanbooException(10050);
        }

        if ( item.getItemBytes()==null && (item.getMount().equals("") || item.getItemDirectory().equals("") || item.getItemSize() < 1) ) {
                throw new XanbooException(10050);
        }
            

        //if a binary is attached, need to process the file (save to items dir)
        if(item.getItemBytes()!=null) {
            item = this.saveItemBytes(xCaller.getDomain(), xCaller.getAccountId(), item);
        }
        
        Connection conn=null;
        boolean rollback=false;

        try {
            conn=dao.getConnection();
            dao.addItem(conn, xCaller, item);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[addItem()]: " + e.getMessage(), e);
            }else {
              logger.error("[addItem()]" + e.getMessage());
            }                 
            throw new XanbooException(10030, "[addItem]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        return item.getItemId();        
        
    }
    

    public void deleteItem(XanbooPrincipal xCaller, long[] inboxItemIds) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteItem()]:");
        }
        
        XanbooUtil.checkCallerPrivilege( xCaller);
        
        try {
            if ( xCaller.getUserId() < 1 || xCaller.getAccountId() < 1 || inboxItemIds.length < 1 ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }

        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();
            for (int i = 0 ; i < inboxItemIds.length ; i++) {
                dao.deleteItem(conn, xCaller, inboxItemIds[i], null, null, null);
            }
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[deleteItem()]: " + e.getMessage(), e);
            }else {
              logger.error("[deleteItem()]" + e.getMessage());
            }
            throw new XanbooException(10030, "[deleteItem]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }

    }
    

    
    public void deleteItemList(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String contentType) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteItemList()]:");
        }
        
        XanbooUtil.checkCallerPrivilege( xCaller);
        
        if(deviceGUID!=null && gatewayGUID==null) {
            throw new XanbooException(10050);
        }
        
        if(contentType!=null && !contentType.equals("text") && !contentType.equals("image") && !contentType.equals("video") && !contentType.equals("media")) {
            throw new XanbooException(10050);
        }
            
        //check sql injection for in params
        if(XanbooUtil.hasSqlInjection(gatewayGUID) || XanbooUtil.hasSqlInjection(deviceGUID)) {
            throw new XanbooException(10050);
        }        
        

        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();
            dao.deleteItem(conn, xCaller, -1, gatewayGUID, deviceGUID, contentType);
            
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[deleteItemList()]: " + e.getMessage(), e);
            }else {
              logger.error("[deleteItemList()]" + e.getMessage());
            }
            throw new XanbooException(10030, "[deleteItemList]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }

    }
    

    public void emptyInbox(XanbooPrincipal xCaller ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[emptyInbox()]:");
        }

        // first validate the caller and privileges
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() < 1 || xCaller.getAccountId() < 1 ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();
            dao.emptyInbox(conn, xCaller);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[emptyInbox()]: " + e.getMessage(), e);
            }else {
              logger.error("[emptyInbox()]" + e.getMessage());
            }            
            throw new XanbooException(10030, "[emptyInbox]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
    }
    

    public void moveItem(XanbooPrincipal xCaller, long[] inboxItemIds, long folderId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[moveItem()]:");
        }

        // first validate the caller and privileges
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() < 1 || xCaller.getAccountId() < 1 || inboxItemIds.length < 1 || folderId < 1 ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }

        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();
            for (int i=0; i<inboxItemIds.length; i++) {
                dao.moveItem(conn, xCaller, inboxItemIds[i], folderId);
            }
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[moveItem()]: " + e.getMessage(), e);
            }else {
              logger.error("[moveItem()]" + e.getMessage());
            }                        
            throw new XanbooException(10030, "[moveItem]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
    }


    public XanbooResultSet getItemList(XanbooPrincipal xCaller) throws XanbooException {
        return this.getItemList( xCaller, null, null );
    }
    
    public XanbooResultSet getItemList(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getItemList()]:");
        }

        try {
            // first validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);

            if ( xCaller.getUserId() < 1 || xCaller.getAccountId() < 1 ) {
                throw new XanbooException(10050);
            }

            //check sql injection for in params
            if(XanbooUtil.hasSqlInjection(gatewayGUID) || XanbooUtil.hasSqlInjection(deviceGUID)) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        
        Connection conn=null;
        try {
            conn=dao.getConnection();
            return dao.getItemList(conn, xCaller, gatewayGUID, deviceGUID, this.SORT_BY_TIME_DESC);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getItemList()]: " + e.getMessage(), e);
            }else {
              logger.error("[getItemList()]" + e.getMessage());
            }               
            throw new XanbooException(10030, "[getItemList]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
    }
    

    public XanbooResultSet getItemList(XanbooPrincipal xCaller, int startRow, int numRows, int sortBy) throws XanbooException {
        return this.getItemList(xCaller, null, null, null, null, null, null, startRow, numRows, sortBy) ;
    }

    
    public XanbooResultSet getItemList(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, int startRow, int numRows, int sortBy) throws XanbooException {
        return getItemList(xCaller, gatewayGUID, deviceGUID, null, null, null, null, startRow, numRows, sortBy);
    }

    
    public XanbooResultSet getItemList(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, Date fromDate, Date toDate, String contentType, String eID, int startRow, int numRows, int sortBy) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getItemList(start,num)]:");
        }

        // first validate the caller and privileges
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        String dguids = deviceGUID;
        String eids = eID;
        
        try {
            if( xCaller.getUserId() < 1 || xCaller.getAccountId() < 1 || startRow < 0 || numRows < 0 || (sortBy < -1*this.SORT_BY_LIMIT) || (sortBy > this.SORT_BY_LIMIT)) {
                throw new XanbooException(10050);
            }
            
            if(contentType!=null && !contentType.equals("text") && !contentType.equals("image") && !contentType.equals("video") && !contentType.equals("media")) {
                throw new XanbooException(10050);
            }
            
            //check sql injection for in params
            if(XanbooUtil.hasSqlInjection(gatewayGUID) || XanbooUtil.hasSqlInjection(deviceGUID) || XanbooUtil.hasSqlInjection(eID)) {
                throw new XanbooException(10050);
            }
            
            //normalize csv list of dguids
            if(deviceGUID!=null && deviceGUID.indexOf(",")!=-1) {
                StringTokenizer st = new StringTokenizer(deviceGUID, "," );
                int cnt = st.countTokens();
                dguids = "";
                for(int i=0;i<cnt; i++) {
                    String dguid = ((String) st.nextToken()).trim();
                    if(dguid.length()>0) dguids = dguids + (dguids.length()>0 ? "," : "") + dguid;
                }
            }

            //normalize csv list of eids
            if(eID!=null && eID.indexOf(",")!=-1) {
                StringTokenizer st = new StringTokenizer(eID, "," );
                int cnt = st.countTokens();
                eids = "";
                for(int i=0;i<cnt; i++) {
                    String eid = ((String) st.nextToken()).trim();
                    if(eid.length()>0) eids = eids + (eids.length()>0 ? "," : "") + eid;
                }
            }
            
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        try {
            conn=dao.getConnection();
            return dao.getItemList(conn, xCaller, gatewayGUID, dguids, fromDate, toDate, contentType, eids, startRow, numRows, sortBy);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getItemList(start,num)]: " + e.getMessage(), e);
            }else {
              logger.error("[getItemList(start,num)]" + e.getMessage());
            }                        
            throw new XanbooException(10030, "[getItemList(start,num)]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
    }

    
    public XanbooResultSet getCorrelatedItemList(XanbooPrincipal xCaller, String gatewayGUID, String eventGroupId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getCorrelatedItemList()]:");
        }

        // first validate the caller and privileges
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if(xCaller.getUserId() < 1 || xCaller.getAccountId() < 1 || gatewayGUID==null || gatewayGUID.length()==0 || eventGroupId==null || eventGroupId.length()==0) {
                throw new XanbooException(10050);
            }
            
            //check sql injection for in params
            if(XanbooUtil.hasSqlInjection(gatewayGUID) || XanbooUtil.hasSqlInjection(eventGroupId)) {
                throw new XanbooException(10050);
            }
            
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        try {
            conn=dao.getConnection();
            return dao.getItemList(conn, xCaller, gatewayGUID, eventGroupId);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getCorrelatedItemList()]: " + e.getMessage(), e);
            }else {
              logger.error("[getCorrelatedItemList()]" + e.getMessage());
            }                        
            throw new XanbooException(10030, "[getCorrelatedItemList]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
        
    }
   

    public XanbooItem getItem(XanbooPrincipal xCaller, long inboxItemId) throws XanbooException {
        return getItem(xCaller, inboxItemId, true, true);
    }

    public XanbooItem getItem(XanbooPrincipal xCaller, long inboxItemId, boolean returnPrevNext, boolean updateViewCount) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getItem()]:");
        }

        // first validate the caller and privileges
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() < 1 || xCaller.getAccountId() < 1 || inboxItemId < 1 ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        XanbooItem item = new XanbooItem();
        Connection conn=null;
        boolean rollback = true;
        try {
            conn=dao.getConnection();
            item = dao.getItem(conn, xCaller, inboxItemId, returnPrevNext, updateViewCount);
            rollback = false;
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getItem()]: " + e.getMessage(), e);
            }else {
              logger.error("[getItem()]" + e.getMessage());
            }
            throw new XanbooException(10030, "[getItem]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
        return item;
    }


    public XanbooBinaryContent getItemBinary(XanbooPrincipal xCaller, String itemMount, String itemDir, String fileName, boolean isThumb) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getItemBinary()]: m=" + itemMount + ",  dir=" + itemDir);
        }

        String itemPath = null;
        
        try {
            XanbooItem item = new XanbooItem();
            item.setMount(itemMount);

            itemPath = AbstractFSProvider.getBaseDir(item.getMountDir(), xCaller.getDomain(), xCaller.getAccountId(), (isThumb ? AbstractFSProvider.DIR_ACCOUNT_THUMB : AbstractFSProvider.DIR_ACCOUNT_ITEM)) + "/" +
                         itemDir + "/" + fileName;
            
            return new XanbooBinaryContent(null, XanbooFSProviderProxy.getInstance().getFileBytes(item.getMountPrefix(), itemPath) );
            ///--return new XanbooBinaryContent(null, XanbooUtil.getFileBytes(itemPath));
            
        }catch(Exception e) {
            if(logger.isDebugEnabled())
                logger.debug("[getItemBinary()]: EXCEPTION: fpath=" + itemPath, e);
            else
                logger.debug("[getItemBinary()]: EXCEPTION: fpath=" + itemPath + ",    exception:" + e.getMessage());
                
            throw new XanbooException(22120, "Failed to get item binary. File not found.");

        }

    }


}