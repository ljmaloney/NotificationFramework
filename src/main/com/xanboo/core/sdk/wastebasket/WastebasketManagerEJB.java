/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/wastebasket/WastebasketManagerEJB.java,v $
 * $Id: WastebasketManagerEJB.java,v 1.17 2003/11/13 23:03:02 guray Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */


package com.xanboo.core.sdk.wastebasket;

import java.sql.Connection;

import javax.annotation.PostConstruct;
import javax.ejb.CreateException;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;
import com.xanboo.core.util.XanbooException;

/**
 * Session Bean implementation of <code>WastebasketManager</code>. This bean acts as a wrapper class for
 * all wastebasket and wastebasket item related Core SDK methods.
*/
@Remote (WastebasketManager.class)
@Stateless (name="WastebasketManager")
@TransactionManagement( TransactionManagementType.BEAN )
public class WastebasketManagerEJB   {

    private SessionContext context;
    private Logger logger;

    // related DAO class
    private WastebasketManagerDAO dao=null;
    
    @PostConstruct
    public void init() throws CreateException {
        try {
            // create a logger instance
            logger=LoggerFactory.getLogger(this.getClass().getName());
            dao = new WastebasketManagerDAO();
        }catch (Exception se) {
            throw new CreateException("Failed to create WastebasketManager:" + se.getLocalizedMessage());
        }
    }


    

    //--------------- Business methods ------------------------------------------------------
      

    public XanbooResultSet getItemList(XanbooPrincipal xCaller) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[trash:getItemList()]:");
        }
       
        // validate parameters
        try {
             if ( xCaller.getAccountId() <= 0L | xCaller.getUserId() <= 0L ) {
                 throw new XanbooException(10050);
             }
        }catch (Exception e) {
             throw new XanbooException(10050);
        }
               
        Connection conn=null;
        boolean rollback = false;
        
        try {
            conn=dao.getConnection();
            return dao.getItemList(conn, xCaller);
        }catch (XanbooException xe) {
            rollback = true;
            throw xe;
        }catch (Exception e) {
            rollback = true;
            if(logger.isDebugEnabled()) {
              logger.error("[trash:getItemList()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[trash:getItemList()]: Exception:" + e.getMessage());
            }
            throw new XanbooException(10030, "[trash:getItemList()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn,rollback);
        }
    }
    

    public XanbooResultSet getItemList(XanbooPrincipal xCaller, int startRow, int numRows) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[trash:getItemList2()]:");
        }
       
        // validate parameters
        try {
             if ( xCaller.getAccountId() <= 0L | xCaller.getUserId() <= 0L ) {
                 throw new XanbooException(10050);
             }
        }catch (Exception e) {
             throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback = false;
        
        try {
            conn=dao.getConnection();
            return dao.getItemList(conn, xCaller, startRow, numRows);
        }catch (XanbooException xe) {
            rollback = true;
            throw xe;
        }catch (Exception e) {
            rollback = true;
            if(logger.isDebugEnabled()) {
              logger.error("[trash:getItemList2()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[trash:getItemList2()]: Exception:" + e.getMessage());
            }            
            throw new XanbooException(10030, "[trash:getItemList2()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn,rollback);
        }
    }
    

    public void deleteItem(XanbooPrincipal xCaller, long[] trashItemIds) throws XanbooException {
     if (logger.isDebugEnabled()) {
            logger.debug("[trash:deleteItem()]:");
        }
        
        // validate parameters
        try {
             if ( xCaller.getAccountId() <= 0L | xCaller.getUserId() <= 0L ) {
                 throw new XanbooException(10050);
             }
        }catch (Exception e) {
             throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback = false;
        
        try {
            conn=dao.getConnection();
            for (int i=0; i<trashItemIds.length; i++) {
                dao.deleteItem(conn, xCaller, trashItemIds[i]);
            }
        }catch (XanbooException xe) {
            rollback = true;
            throw xe;
        }catch (Exception e) {
            rollback = true;
            if(logger.isDebugEnabled()) {
              logger.error("[trash:deleteItem()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[trash:deleteItem()]: Exception:" + e.getMessage());
            }            
            throw new XanbooException(10030, "[trash:deleteItem()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn,rollback);
        }
    }
    

    public void undeleteItem(XanbooPrincipal xCaller, long[] trashItemIds, long targetFolderId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[trash:undeleteItem()]:");
        }
          
        // validate parameters
        try {
             if ( xCaller.getAccountId() <= 0L | xCaller.getUserId() <= 0L ) {
                 throw new XanbooException(10050);
             }
        }catch (Exception e) {
             throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback = false;
        
        try {
            conn=dao.getConnection();
            for (int i=0; i<trashItemIds.length; i++) {
                dao.undeleteItem(conn, xCaller, trashItemIds[i], targetFolderId);
            }
        }catch (XanbooException xe) {
            rollback = true;
            throw xe;
        }catch (Exception e) {
            rollback = true;
            if(logger.isDebugEnabled()) {
              logger.error("[trash:undeleteItem()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[trash:undeleteItem()]: Exception:" + e.getMessage());
            }                        
            throw new XanbooException(10030, "[trash:undeleteItem()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn,rollback);
        }
    }
    

    public void emptyWB(XanbooPrincipal xCaller) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[trash:deleteItem()]:");
        }
        
        // validate parameters
        try {
             if ( xCaller.getAccountId() <= 0L | xCaller.getUserId() <= 0L ) {
                 throw new XanbooException(10050);
             }
        }catch (Exception e) {
             throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback = false;
        
        try {
            conn=dao.getConnection();
            dao.emptyWB(conn, xCaller);
        }catch (XanbooException xe) {
            rollback = true;
            throw xe;
        }catch (Exception e) {
            rollback = true;
            if(logger.isDebugEnabled()) {
              logger.error("[trash:emptyWB()]: Exception:" + e.getMessage(), e);
            }else {
              logger.error("[trash:emptyWB()]: Exception:" + e.getMessage());
            }                        
            throw new XanbooException(10030, "[trash:emptyWB()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn,rollback);
        }
    }
    
}
