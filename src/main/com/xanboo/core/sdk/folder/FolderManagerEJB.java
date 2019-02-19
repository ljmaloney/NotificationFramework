/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/folder/FolderManagerEJB.java,v $
 * $Id: FolderManagerEJB.java,v 1.40 2008/09/25 18:39:51 levent Exp $
 *
 * Copyright 2002 Xanboo, Inc.
 *
 */
package com.xanboo.core.sdk.folder;

import com.xanboo.core.util.*;
import com.xanboo.core.model.*;
import com.xanboo.core.sdk.AbstractSDKArchiveManagerEJB;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.sdk.util.XanbooResultSet;

import java.sql.Connection;
import java.util.StringTokenizer;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;


/**
 * <p>
 * Session Bean implementation of <code>FolderManager</code>. This bean acts as a wrapper class for
 * all folder and folder item related Core SDK methods.
 * </p>
*/
@Stateless (name="FolderManager")
@TransactionManagement( TransactionManagementType.BEAN )
@Remote (FolderManager.class)
public class FolderManagerEJB extends AbstractSDKArchiveManagerEJB  {
    
    private FolderManagerDAO dao=null;
    
    //define sort by fields, in ascending and descending orders
    /**
     * Sort by folder name in an ascending order, default for folders list
     */
    public static final int SORT_BY_FOLDERNAME_ASC = 1;
    /**
     * Sort by folder name in a descending order
     */
    public static final int SORT_BY_FOLDERNAME_DESC = -1;

    
    //folder items
    /**
     * Sort by time when the folder item was first received, in an ascending order
     */
    public static final int SORT_BY_TIME_ASC = 4;       //sort by time when item was received
    /**
     * Sort by time when the folder item was first received, in a descending order, default
     */
    public static final int SORT_BY_TIME_DESC = -4;

    
    /**
     * Maximum or minimum number (if negative) that could be used to define sorting
     */
    private final int SORT_BY_LIMIT = 4;    //for range check
    
    @PostConstruct
    public void init() throws Exception {
        dao = new FolderManagerDAO();
    }
    
    
    //--------------- Business methods ------------------------------------------------------
    

    public XanbooResultSet getFolderList(XanbooPrincipal xCaller) throws XanbooException{
        
        if (logger.isDebugEnabled()) {
            logger.debug("[getFolderList()]:");
        }
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0 ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        try {
            conn=dao.getConnection();
            return dao.getFolderList(conn, xCaller, this.SORT_BY_FOLDERNAME_ASC);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getFolderList()]: " + e.getMessage(), e);
            }else {
              logger.error("[getFolderList()]: " + e.getMessage());
            }                             
            throw new XanbooException(10030, "[getFolderList()]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
    }
    

    public XanbooResultSet getFolderList(XanbooPrincipal xCaller, int startRow, int numRows, int sortBy) throws XanbooException{
        
        if (logger.isDebugEnabled()) {
            logger.debug("[getFolderList(start,num)]:");
        }
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0
            || startRow < 0 || numRows < 0
            || (sortBy < -1*this.SORT_BY_LIMIT)
            || (sortBy > this.SORT_BY_LIMIT)) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        try {
            conn=dao.getConnection();
            return dao.getFolderList(conn, xCaller, startRow, numRows, sortBy);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getFolderList(start,num)]: " + e.getMessage(), e);
            }else {
              logger.error("[getFolderList(start,num)]: " + e.getMessage());
            }            
            throw new XanbooException(10030, "[getFolderList(start,num)]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
    }
    

    public long addFolder(XanbooPrincipal xCaller, XanbooFolder folder) throws XanbooException{
        
        if (logger.isDebugEnabled()) {
            logger.debug("[addFolder()]:");
        }
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0 || (folder.getName()).equals("") ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        long folderId=-1;
        Connection conn=null;
        boolean rollback=false;
        
        try {
            conn=dao.getConnection();
            folderId = dao.addFolder(conn, xCaller, folder);
        }catch (XanbooException xe) {
            rollback = true;
            throw xe;
        }catch (Exception e) {
            rollback = true;
            if(logger.isDebugEnabled()) {
              logger.error("[addFolder]: " + e.getMessage(), e);
            }else {
              logger.error("[addFolder]: " + e.getMessage());
            }                        
            throw new XanbooException(10030, "[addFolder]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        return folderId;
    }
    

    public void deleteFolder(XanbooPrincipal xCaller, long[] folderId, boolean forceFlag) throws XanbooException{
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteFolder()]:");
        }
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0 || folderId.length == 0) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();
            for (int i = 0 ; i < folderId.length ; i++) {
                dao.deleteFolder(conn, xCaller, folderId[i], forceFlag);
            }
        }catch (XanbooException xe) {
            rollback = true;
            throw xe;
        }catch (Exception e) {
            rollback = true;
            if(logger.isDebugEnabled()) {
              logger.error("[deleteFolder]: " + e.getMessage(), e);
            }else {
              logger.error("[deleteFolder]: " + e.getMessage());
            }                        
            throw new XanbooException(10030, "[deleteFolder]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
    }
    

    public void updateFolder(XanbooPrincipal xCaller, XanbooFolder folder) throws XanbooException{
        if (logger.isDebugEnabled()) {
            logger.debug("[updateFolder()]:");
        }
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0 ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();
            dao.updateFolder(conn, xCaller, folder);
        }catch (XanbooException xe) {
            rollback = true;
            throw xe;
        }catch (Exception e) {
            rollback = true;
            if(logger.isDebugEnabled()) {
              logger.error("[updateFolder]: " + e.getMessage(), e);
            }else {
              logger.error("[updateFolder]: " + e.getMessage());
            }                                    
            throw new XanbooException(10030, "[updateFolder]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
    }
    

    public void emptyFolder(XanbooPrincipal xCaller, long folderId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[emptyFolder()]:");
        }
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0 ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback = false;
        
        try {
            conn=dao.getConnection();
            dao.emptyFolder(conn, xCaller, folderId);
        }catch (XanbooException xe) {
            rollback = true;
            throw xe;
        }catch (Exception e) {
            rollback = true;
            if(logger.isDebugEnabled()) {
              logger.error("[emptyFolder]: " + e.getMessage(), e);
            }else {
              logger.error("[emptyFolder]: " + e.getMessage());
            }                
            throw new XanbooException(10030, "[emptyFolder]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
    }

    
    public XanbooResultSet getItemList( XanbooPrincipal xCaller, long folderId ) throws XanbooException{
        return this.getItemList( xCaller, folderId, null, null );
    }
    
    public XanbooResultSet getItemList(XanbooPrincipal xCaller, long folderId, String gatewayGUID, String deviceGUID) throws XanbooException{
        if (logger.isDebugEnabled()) {
            logger.debug("[getItemList()]:");
        }
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0 || folderId == 0) {
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
            return dao.getItemList(conn, xCaller, folderId, gatewayGUID, deviceGUID, this.SORT_BY_TIME_DESC );
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getItemList]: " + e.getMessage(), e);
            }else {
              logger.error("[getItemList]: " + e.getMessage());
            }              
            throw new XanbooException(10030, "[getItemList]: " + e.getMessage());
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
              logger.error("[getCorrelatedItemList())]" + e.getMessage());
            }                        
            throw new XanbooException(10030, "[getCorrelatedItemList()]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
        
    }
    

    public XanbooResultSet getNotes(XanbooPrincipal xCaller, long itemId) throws XanbooException{
        if (logger.isDebugEnabled()) {
            logger.debug("[getnotes()]:");
        }
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0 || itemId <1) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        try {
            conn=dao.getConnection();
            return dao.getNotes(conn, xCaller, itemId);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getNotes]: " + e.getMessage(), e);
            }else {
              logger.error("[getNotes]: " + e.getMessage());
            }                          
            throw new XanbooException(10030, "[getNotes]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
    }
    

    public void deleteNote(XanbooPrincipal xCaller, long noteId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteNote()]:");
        }
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0 || noteId < 1 ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();
            dao.deleteNote(conn, xCaller, noteId);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[deleteNotes]: " + e.getMessage(), e);
            }else {
              logger.error("[deleteNotes]: " + e.getMessage());
            }                                      
            throw new XanbooException(10030, "[deleteNote]: " + e.getMessage() );
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
    }

    public XanbooResultSet getItemList(XanbooPrincipal xCaller, long folderId, int startRow, int numRows, int sortBy) throws XanbooException{
        return this.getItemList(xCaller, folderId, null, null, null, null, null, null, startRow, numRows, sortBy);
    }
        
    public XanbooResultSet getItemList(XanbooPrincipal xCaller, long folderId, String gatewayGUID, String deviceGUID, int startRow, int numRows, int sortBy) throws XanbooException {
        return this.getItemList(xCaller, folderId, gatewayGUID, deviceGUID, null, null, null, null, startRow, numRows, sortBy);
    }
        
    public XanbooResultSet getItemList(XanbooPrincipal xCaller, long folderId, String gatewayGUID, String deviceGUID, Date fromDate, Date toDate, String contentType, String eID, int startRow, int numRows, int sortBy) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getItemList(start,num)]:");
        }
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        String dguids = deviceGUID;
        String eids = eID;
        
        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0 || folderId == 0
                    || startRow < 0 || numRows < 0 || (sortBy < -1*this.SORT_BY_LIMIT) || (sortBy > this.SORT_BY_LIMIT)) {
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
            return dao.getItemList(conn, xCaller, folderId, gatewayGUID, dguids, fromDate, toDate, contentType, eids, startRow, numRows, sortBy);
            
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getItemList(start,num)]: " + e.getMessage(), e);
            }else {
              logger.error("[getItemList(start,num)]: " + e.getMessage());
            }                                      
            throw new XanbooException(10030, "[getItemList(start,num)]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
    }
    
    

    public XanbooItem getItem(XanbooPrincipal xCaller, long itemId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getItem()]:");
        }
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0 || itemId < 1 ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        XanbooItem item = new XanbooItem();
        boolean rollback = true;
        Connection conn=null;
        try {
            conn=dao.getConnection();
            item = dao.getItem(conn, xCaller, itemId);
            rollback = false;
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getItem]: " + e.getMessage(), e);
            }else {
              logger.error("[getItem]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[getItem]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
        return item;
    }
    

    public XanbooFolder getFolder(XanbooPrincipal xCaller, long folderId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getFolder()]:");
        }
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0 || folderId < 1 ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        XanbooFolder folder = new XanbooFolder();
        boolean rollback = true;
        Connection conn=null;
        try {
            conn=dao.getConnection();
            folder = dao.getFolder(conn, xCaller, folderId);
            rollback = false;
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getFolder]: " + e.getMessage(), e);
            }else {
              logger.error("[getFolder]: " + e.getMessage());
            }            
            throw new XanbooException(10030, "[getFolder]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
        return folder;
    }
    
    

    public long addItem(XanbooPrincipal xCaller, XanbooItem item, long folderId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[addItem()]:");
        }
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        if ( xCaller.getUserId() < 1 || xCaller.getAccountId() < 1 || xCaller.getDomain()==null || xCaller.getDomain().length()==0 || folderId <1 || 
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
            dao.addItem(conn, xCaller, item, folderId);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[addItem]: " + e.getMessage(), e);
            }else {
              logger.error("[addItem]: " + e.getMessage());
            }                        
            throw new XanbooException(10030, "[addItem]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        return item.getItemId();

    }
    
    public void deleteItemList(XanbooPrincipal xCaller, long folderId, String gatewayGUID, String deviceGUID, String contentType) throws XanbooException {
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
            dao.deleteItem(conn, xCaller, -1, folderId, gatewayGUID, deviceGUID, contentType);
            
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
    

    public void deleteItem(XanbooPrincipal xCaller, long[] itemIds) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteItem()]:");
        }
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0 || itemIds.length < 1 ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();
            for (int i = 0 ; i<itemIds.length ; i++ ) {
                dao.deleteItem(conn, xCaller, itemIds[i], -1, null, null, null);
            }
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[deleteItem]: " + e.getMessage(), e);
            }else {
              logger.error("[deleteItem]: " + e.getMessage());
            }
            rollback=true;
            throw new XanbooException(10030, "[deleteItem]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
    }
    

    public void moveItem(XanbooPrincipal xCaller, long itemIds[], long folderId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[moveItem()]:");
        }
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0 || folderId == 0 ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();
            for (int i=0 ; i<itemIds.length ; i++) {
                dao.moveItem(conn, xCaller, itemIds[i], folderId);
            }
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[moveItem]: " + e.getMessage(), e);
            }else {
              logger.error("[moveItem]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[moveItem]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
    }
    
    

    public void copyItem(XanbooPrincipal xCaller, long itemIds[], long folderId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[copyItem()]:");
        }
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0 || folderId == 0 ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();
            for (int i=0 ; i < itemIds.length ; i++ ){
                dao.copyItem(conn, xCaller, itemIds[i], folderId);
            }
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[copyItem]: " + e.getMessage(), e);
            }else {
              logger.error("[copyItem]: " + e.getMessage());
            }
            rollback=true;
            throw new XanbooException(10030, "[copyItem]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
    }
    

    public void updateItem(XanbooPrincipal xCaller, long itemId, String name) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateItem()]:");
        }
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0 || itemId == 0 ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();
            dao.updateItem(conn, xCaller, itemId, name);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[updateItem]: " + e.getMessage(), e);
            }else {
              logger.error("[updateItem]: " + e.getMessage());
            }
            rollback=true;
            throw new XanbooException(10030, "[updateItem]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
    }
    

    public void addNote(XanbooPrincipal xCaller, long itemId, String note) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[addNote()]:");
        }
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0 || itemId == 0 || note.length() > 256 ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();
            dao.addNote(conn, xCaller, itemId, note);
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[addNote]: " + e.getMessage(), e);
            }else {
              logger.error("[addNote]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[addNote]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
    }    
}