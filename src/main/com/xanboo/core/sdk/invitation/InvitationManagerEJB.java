/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/invitation/InvitationManagerEJB.java,v $
 * $Id: InvitationManagerEJB.java,v 1.14 2011/05/02 17:41:25 levent Exp $
 *
 * Copyright Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.invitation;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.ejb.CreateException;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import com.xanboo.core.model.XanbooBinaryContent;
import com.xanboo.core.model.XanbooGateway;
import com.xanboo.core.model.XanbooItem;
import com.xanboo.core.sdk.AbstractSDKManagerEJB;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.GlobalNames;
import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;
import com.xanboo.core.util.XanbooException;
import com.xanboo.core.util.XanbooUtil;
import com.xanboo.core.util.fs.AbstractFSProvider;
import com.xanboo.core.util.fs.XanbooFSProviderProxy;


/**
 * <p>
 * Session Bean implementation of <code>InvitationManager</code>. This bean acts as a wrapper class for
 * all invitation related Core SDK methods.
 * </p>
 *
 */
@Stateless (name  = "InvitationManager")
@TransactionManagement( TransactionManagementType.BEAN )
@Remote (InvitationManager.class)
public class InvitationManagerEJB   {
    
   
    private Logger logger;
    
    private InvitationManagerDAO dao=null;
    
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
     * Sort by folder item name in an ascending order
     */
    public static final int SORT_BY_ITEMNAME_ASC = 2;
    /**
     * Sort by folder item name in a descending order
     */
    public static final int SORT_BY_ITEMNAME_DESC = -2;
    /**
     * Sort by folder item source (appliance name) in an ascending order
     */
    public static final int SORT_BY_SOURCE_ASC = 3;
    /**
     * Sort by folder item source (appliance name) in a descending order
     */
    public static final int SORT_BY_SOURCE_DESC = -3;
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
    
    public static final String OID_TOBE_FILTERED="#99@";
    
    @PostConstruct
    public void init() throws CreateException {
        try {
            // create a logger instance
            logger=LoggerFactory.getLogger(this.getClass().getName());
            if(logger.isDebugEnabled()) {
                logger.debug("[ejbCreate()]:");
            }
            
            dao = new InvitationManagerDAO();
            
        }catch (Exception se) {
            throw new CreateException("Failed to create InvitationManager:" + se.getLocalizedMessage());
        }
    }
    

    
    
    //--------------- Business methods ------------------------------------------------------
    
    
    
    public XanbooInvitee[] sendFolderInvitation(XanbooPrincipal xCaller, long folderId, String subject, String message, XanbooInvitee[] invitees, java.util.Date expiration ) throws XanbooException {
        try {
            return this.sendInvitation(xCaller, InvitationManager.INVITATION_FOLDER, Long.toString(folderId), subject, message, invitees, expiration );
        } catch ( XanbooException xe ) {
            throw xe;
        }
    }
    
    
    public XanbooInvitee[] sendItemInvitation(XanbooPrincipal xCaller, long itemId, String subject, String message, XanbooInvitee[] invitees, java.util.Date expiration ) throws XanbooException {
        try {
        	return this.sendInvitation(xCaller, InvitationManager.INVITATION_ITEM, Long.toString(itemId), subject, message, invitees, expiration );
        } catch ( XanbooException xe ) {
            throw xe;
        }
    }
    
    public XanbooInvitee[] sendDeviceInvitation(XanbooPrincipal xCaller, String gatewayGuid, String deviceGuid, String subject, String message, XanbooInvitee[] invitees, java.util.Date expiration ) throws XanbooException {
        try {

        	return this.sendInvitation(xCaller, InvitationManager.INVITATION_DEVICE, gatewayGuid + "|" + deviceGuid, subject, message, invitees, expiration );

        } catch ( XanbooException xe ) {
            throw xe;
        }
    }
    
    
    public void addInvitee(XanbooPrincipal xCaller, long invitationId, XanbooInvitee[] invitees) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[sendFolderInvitation()]:");
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
            for (int i=0; i<invitees.length; i++) {
                dao.addInvitee( conn, xCaller, invitees[i], invitationId) ;
            }
        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
                logger.error("[addInvitee]: " + e.getMessage(), e);
            }else {
                logger.error("[addInvitee]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[addInvitee]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
    }
    
    /**
     * Sends an invitation to a particular folder
     */
    private XanbooInvitee[] sendInvitation(XanbooPrincipal xCaller, short objectType, String targetItemId, String subject, String message, XanbooInvitee[] invitees, java.util.Date expiration ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[sendInvitation()]:");
        }
        XanbooInvitee[]  xanbooInvitee = null;
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0 || targetItemId == null ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();
            xanbooInvitee = dao.addInvitation( conn, xCaller, subject, message, objectType, targetItemId, expiration, invitees);

        }catch (XanbooException xe) {
            rollback=true;
            throw xe;
        }catch (Exception e) {
            rollback=true;
            if(logger.isDebugEnabled()) {
                logger.error("[sendInvitation]: " + e.getMessage(), e);
            }else {
                logger.error("[sendInvitation]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[sendInvitation]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        return xanbooInvitee;
    }
    
    
    public XanbooResultSet getInviteeList(XanbooPrincipal xCaller, long invitationId) throws XanbooException{
        if (logger.isDebugEnabled()) {
            logger.debug("[getInviteeList()]:");
        }
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0 || invitationId== 0 ) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        try {
            conn=dao.getConnection();
            return dao.getInviteeList(conn, xCaller, invitationId);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[getInviteeList]: " + e.getMessage(), e);
            }else {
                logger.error("[getInviteeList]: " + e.getMessage());
            }
            throw new XanbooException(10030, "Exception while executing DAO method");
        }finally {
            dao.closeConnection(conn);
        }
    }
    
    
    public void deleteInvitation(XanbooPrincipal xCaller, long[] invitationId) throws XanbooException{
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteInvitation()]:");
        }
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0 || invitationId.length == 0) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();
            for (int i=0;i<invitationId.length;i++) {
                dao.deleteInvitation(conn, xCaller, invitationId[i]);
            }
        }catch (XanbooException xe) {
            rollback = true;
            throw xe;
        }catch (Exception e) {
            rollback = true;
            if(logger.isDebugEnabled()) {
                logger.error("[deleteInvitation]: " + e.getMessage(), e);
            }else {
                logger.error("[deleteInvitation]: " + e.getMessage());
            }
            throw new XanbooException(10030, "Exception while executing DAO method");
        }finally {
            dao.closeConnection(conn, rollback);
        }
    }
    
    
    public XanbooResultSet getInvitation(XanbooPrincipal xCaller, long invitationId) throws XanbooException{
        if (logger.isDebugEnabled()) {
            logger.debug("[getInvitation()]:");
        }
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0 || invitationId < 0) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        try {
            conn=dao.getConnection();
            return dao.getInvitation(conn, xCaller, invitationId);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[getInvitation]: " + e.getMessage(), e);
            }else {
                logger.error("[getInvitation]: " + e.getMessage());
            }
            throw new XanbooException(10030, "Exception while executing DAO method");
        }finally {
            dao.closeConnection(conn);
        }
    }
    
    
    
    public void deleteInvitee(XanbooPrincipal xCaller, long invitationId, long[] inviteeId) throws XanbooException{
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteInvitee()]:");
        }
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            if ( xCaller.getUserId() == 0 || xCaller.getAccountId() == 0 || inviteeId.length == 0 || invitationId == 0) {
                throw new XanbooException(10050);
            }
        } catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback=false;
        try {
            conn=dao.getConnection();
            for (int i=0;i<inviteeId.length;i++) {
                dao.deleteInvitee(conn, xCaller, invitationId, inviteeId[i]);
            }
        }catch (XanbooException xe) {
            rollback = true;
            throw xe;
        }catch (Exception e) {
            rollback = true;
            if(logger.isDebugEnabled()) {
                logger.error("[deleteInvitee]: " + e.getMessage(), e);
            }else {
                logger.error("[deleteInvitee]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[deleteInvitee]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
    }
    
    
    
    public XanbooResultSet getInvitationByKey( long inviteeId, String viewKey ) throws XanbooException{
        if (logger.isDebugEnabled()) {
            logger.debug("[getInvitationByKey()]:");
        }
        
        try {
            if ( inviteeId == 0 || viewKey == null || viewKey == "") {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback = true;
        try {
            conn=dao.getConnection();
            XanbooResultSet result = dao.getInvitationByKey(conn, inviteeId, viewKey);
            if (result.size() != 1) {
                throw new XanbooException(23300, "Failed to get invitation by key. Not found");
            } else {
                rollback = false;
                return result;
            }
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[getInvitationByKey]: " + e.getMessage(), e);
            }else {
                logger.error("[getInvitationByKey]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[getInvitationByKey]: " + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
    }
    
    
    public XanbooResultSet getInvitationItemListByKey( long invitationId, String viewKey ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getInvitationItemListByKey()]:");
        }
        
        try {
            if ( invitationId == 0 || viewKey == null || viewKey == "") {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        int sortBy = this.SORT_BY_TIME_ASC;
        
        Connection conn=null;
        try {
            conn=dao.getConnection();
            return dao.getInvitationItemListByKey(conn, invitationId, viewKey, sortBy);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[getInvitationItemListByKey]: " + e.getMessage(), e);
            }else {
                logger.error("[getInvitationItemListByKey]: " + e.getMessage());
            }
            throw new XanbooException(10030, "Exception while executing DAO method");
        }finally {
            dao.closeConnection(conn);
        }
    }
    
    
    
    public XanbooResultSet getInvitationItemListByKey( long invitationId, String viewKey, int startRow, int numRows, int sortBy ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getInvitationItemListByKey()]:");
        }
        try {
            if ( invitationId == 0 || viewKey == null || viewKey == "" || (sortBy < -1*this.SORT_BY_LIMIT) || (sortBy > this.SORT_BY_LIMIT)) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        try {
            conn=dao.getConnection();
            return dao.getInvitationItemListByKey(conn, invitationId, viewKey, startRow, numRows, sortBy);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[getInvitationItemListByKey]: " + e.getMessage(), e);
            }else {
                logger.error("[getInvitationItemListByKey]: " + e.getMessage());
            }
            throw new XanbooException(10030, "Exception while executing DAO method");
        }finally {
            dao.closeConnection(conn);
        }
    }
    
    
    
    public XanbooItem getInvitationItemByKey( long invitationId, String viewKey, long folderItemId ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getInvitationItemByKey()]:");
        }
        
        try {
            if ( invitationId == 0 || viewKey == null || viewKey == "") {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        try {
            conn=dao.getConnection();
            return dao.getInvitationItemByKey(conn, invitationId, viewKey, folderItemId);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[getInvitationItemByKey]: " + e.getMessage(), e);
            }else {
                logger.error("[getInvitationItemByKey]: " + e.getMessage());
            }
            throw new XanbooException(10030, "Exception while executing DAO method");
        }finally {
            dao.closeConnection(conn);
        }
    }
    
    
    
    public XanbooResultSet getInvitationItemNotesByKey( long invitationId, String viewKey, long folderItemId ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getInvitationItemNotesByKey()]:");
        }
        
        try {
            if ( invitationId == 0 || viewKey == null || viewKey == "") {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        try {
            conn=dao.getConnection();
            return dao.getInvitationItemNotesByKey(conn, invitationId, viewKey, folderItemId);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[getInvitationItemNotesByKey]: " + e.getMessage(), e);
            }else {
                logger.error("[getInvitationItemNotesByKey]: " + e.getMessage());
            }
            throw new XanbooException(10030, "Exception while executing DAO method");
        }finally {
            dao.closeConnection(conn);
        }
    }
    
    
    public XanbooGuestPrincipal authenticateInvitee( long inviteeId, String viewKey) throws XanbooException{
        if (logger.isDebugEnabled()) {
            logger.debug("[authenticateInvitation()]:");
        }
        
        XanbooResultSet invitation = this.getInvitationByKey(inviteeId, viewKey);
        
        String objectId = invitation.getElementString(0, "OBJECT_ID");
        short type = Short.parseShort(invitation.getElementString(0,"OBJECTTYPE_ID"));
        
        switch ( type ) {
            case InvitationManager.INVITATION_DEVICE:
                int i = objectId.indexOf( '|' );
                if( i == -1 ) throw new XanbooException( 10050, "Invalid Device Object ID");
                XanbooGuestPrincipal guest = new XanbooGuestPrincipal( viewKey, invitation.getElementLong(0,"INVITATION_ID"),inviteeId, type, objectId.substring(0, i), objectId.substring( i+1 ) );
                guest.accountId = invitation.getElementLong(0,"ACCOUNT_ID");
                guest.userId = invitation.getElementLong(0,"USER_ID");
                guest.domain = invitation.getElementString(0,"DOMAIN_ID");
                guest.setMessage(invitation.getElementString(0,"MESSAGE"));
                guest.setSubject(invitation.getElementString(0,"SUBJECT"));
                
                return guest;
            default: 
                throw new XanbooException( 10050, "Guest Principal only support device invitations at this time");
        }
    }

    public XanbooResultSet getInvitation( XanbooGuestPrincipal guest ) throws XanbooException, RemoteException{
        if( logger.isDebugEnabled() ) {
            logger.debug( "[getInvitation()]: " + guest );
        }
        
        return this.getInvitationByKey( guest.inviteeId, guest.viewKey );

    }
    
    public XanbooGateway getGatewayInfo( XanbooGuestPrincipal guest ) throws XanbooException, RemoteException{
        if( logger.isDebugEnabled() ) {
            logger.debug( "[getGatewayInfo()]: " + guest );
        }
        
        if ( guest == null ) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        try {
            conn=dao.getConnection();
            return dao.getGatewayInfo(conn, guest.accountId, guest.gatewayGuid );
        }catch (XanbooException xe) {
            if( xe.getCode() == 26640 ) {
                throw new XanbooException( 28325, "Failed to retrieve invitation. Not found" );
                /* THE DEVICE FOR THIS INVITATION HAS BEEN DELETED !!! - TODO ?!? */
            }
                
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[getGatewayInfo]: " + e.getMessage(), e);
            }else {
                logger.error("[getGatewayInfo]: " + e.getMessage());
            }
            throw new XanbooException(10030, "Exception while executing DAO method");
        }finally {
            dao.closeConnection(conn);
        }        
    }
    


    public void setMObject(XanbooGuestPrincipal xCaller, String mobjectId, String mobjectValue) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[setMObject()]: ");
        }
        
        String[] ids = new String[1];
        String[] values = new String[1];
        
        ids[0] = mobjectId;
        values[0] = mobjectValue;
        
        try {
            setMObject(xCaller, ids, values);
        }catch (XanbooException xe) {
            throw xe;
        }
    }
    
    

    public void setMObject(XanbooGuestPrincipal xCaller, String[] mobjectId, String[] mobjectValue) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[setMObject()]: ");
        }
        
        String gatewayGUID = xCaller.gatewayGuid;
        String deviceGUID = xCaller.deviceGuid;
        
        // validate the input parameters
        if( gatewayGUID==null || deviceGUID==null || gatewayGUID.trim().equals("") || deviceGUID.trim().equals("")
        || mobjectId.length==0 || mobjectValue.length==0) {
            throw new XanbooException(10050);
        }
        
        /* ENABLE BACKWARD COMPATIBILITY WITH THE NOTION THAT A GATEWAY'S DEVICE GUID IS THE SAME AS IT'S GATEWAY_GUID */
        deviceGUID = gatewayGUID.equals(deviceGUID) ? "0" : deviceGUID;
        
        // validate mobjectid/value pairs
        for(int i=0; i<mobjectId.length; i++) {
            if(mobjectId[i]==null || mobjectValue[i]==null
            || mobjectId[i].trim().length()==0 || mobjectId[i].length()>16
            || mobjectValue[i].length()>GlobalNames.MOBJECT_VALUE_MAXLEN) {
                throw new XanbooException(10050);
            }
            
            if(mobjectValue[i].length()==0) {
                mobjectValue[i] = "null";
            }
        }
        
        
        Connection conn=null;
        boolean rollback=false;
        XanbooGateway gwyInfo=new XanbooGateway(gatewayGUID, xCaller.accountId);
        
        long commandQueueId=-1;
        
        try {
            // validate the caller and privileges
            //XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            commandQueueId = dao.setMObject(conn, xCaller.accountId, xCaller.userId, gatewayGUID, deviceGUID, mobjectId, mobjectValue);
            
            gwyInfo = dao.getGatewayInfo(conn, xCaller.accountId, gatewayGUID);
            AbstractSDKManagerEJB.pollGateway(gwyInfo);
            
        }catch (XanbooException xe) {
            rollback=true;
            if(xe.getCode() != 26134) {  // do not fail, if no command was inserted, since there was already a command in the queue
                throw xe;
            }
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[setMObject()]: " + e.getMessage(), e);
            }else {
              logger.error("[setMObject()]: " + e.getMessage());
            }                                          
            rollback=true;
            throw new XanbooException(10030, "[setMObject]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
        
    }
    
    public XanbooInvitee[] newDeviceInvitation (XanbooPrincipal xCaller, String gatewayGuid, String deviceGuid, String message, XanbooInvitee xanbooInvitee[], java.util.Date expiration ) throws XanbooException, RemoteException {
		 if (logger.isDebugEnabled()) {
		     logger.debug("[newDeviceInvitation()]:");
		 }
		 XanbooInvitee[] xanbooInviteeObj = null;
		 String objectId = gatewayGuid + "|" + deviceGuid;
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
		     xanbooInviteeObj = dao.addInvitation( conn, xCaller, "", message, 3, objectId, expiration, xanbooInvitee);
		 }catch (XanbooException xe) {
		     rollback=true;
		     throw xe;
		 }catch (Exception e) {
		     rollback=true;
		     if(logger.isDebugEnabled()) {
		         logger.error("[newDeviceInvitation]: " + e.getMessage(), e);
		 }else {
		     logger.error("[newDeviceInvitation]: " + e.getMessage());
		 }
		     throw new XanbooException(10030, "[newDeviceInvitation]: " + e.getMessage());
		 }finally {
		     dao.closeConnection(conn, rollback);
		 }
    	return xanbooInviteeObj;
    }
    
    public XanbooResultSet getMObject(XanbooGuestPrincipal xCaller, String mobjectId) throws RemoteException, XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getMObject()]: " + xCaller.getGatewayGuid() + ":" + xCaller.getDeviceGuid() + ":" + mobjectId );
        }

        XanbooResultSet rs=null;
        // validate the input parameters
        if(xCaller.getGatewayGuid()==null || xCaller.getGatewayGuid().trim().equals("")) {
            throw new XanbooException(10050);
        }
        
        /* ENABLE BACKWARD COMPATIBILITY WITH THE NOTION THAT A GATEWAY'S DEVICE GUID IS THE SAME AS IT'S GATEWAY_GUID */
        String deviceGUID = this.checkForGatewayGUID( xCaller.getGatewayGuid(), xCaller.getDeviceGuid() );
        
        Connection conn=null;
        try {

        	conn=dao.getConnection();
            rs= dao.getMObject(conn, xCaller.accountId, xCaller.userId, xCaller.getGatewayGuid(), deviceGUID, mobjectId);
            
            //filter oids if their value is "#99@".  
            Iterator<HashMap> iter = rs.iterator();
            while (iter.hasNext()) {            	
            	 HashMap row = (HashMap) iter.next();
                if (row.get("VALUE").toString().equalsIgnoreCase(OID_TOBE_FILTERED)) {
                    iter.remove();
                }
            }
      
            return rs;	
            
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getMObject()]: " + e.getMessage(), e);
            }else {
              logger.error("[getMObject()]: " + e.getMessage());
            }                              
            throw new XanbooException(10030, "[getMObject]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
    }
    
    /*
     * This method is a temporary fix to allow backward compatibility with the notion that a gateway's device guid is the same as it's gateway guid.
     * <br>
     * The new system instead has the character 0 (zero) for a gateway's device guid.
     */
    private String checkForGatewayGUID(String gatewayGUID, String deviceGUID) {
        if (deviceGUID != null && gatewayGUID != null && deviceGUID.equals(gatewayGUID)) {
            return "0";
        } else {
            return deviceGUID;
        }
        
    }
    

    
    public XanbooBinaryContent getMObjectBinary(XanbooGuestPrincipal xCaller, String mobjectId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getMObjectBinary()]: " + xCaller.getGatewayGuid() + ":" + xCaller.getDeviceGuid() + ":" + mobjectId );
        }
        
        // validate the input parameters
        if(xCaller.getGatewayGuid()==null || xCaller.getGatewayGuid().trim().equals("") || xCaller.getDeviceGuid()==null || xCaller.getDeviceGuid().trim().equals("") || mobjectId==null || mobjectId.trim().equals("")) {
            throw new XanbooException(10050);
        }
        
        
        Connection conn=null;
        try {
            conn=dao.getConnection();
            
            //first get content type from mobject_value record
            XanbooResultSet rs = dao.getMObject(conn, xCaller.getAccountId(), xCaller.userId, xCaller.getGatewayGuid(), xCaller.getDeviceGuid(), mobjectId);
            if(rs!=null && rs.getSize()>0) {   //mobject record found in mobject_value, get content type and timestamp
                HashMap row = (HashMap) rs.get(0);

                XanbooBinaryContent xbin = null;  
                String contentType = (String) row.get("VALUE");
                String ts = (String) row.get("TIMESTAMP");

                //now get binary content from mobject_binary table
                rs = dao.getMObjectBinary(conn, xCaller.getAccountId(), xCaller.userId, xCaller.getGatewayGuid(), xCaller.getDeviceGuid(), mobjectId);                
                if(rs!=null && rs.getSize()>0) {   //binary found in DB mobject_binary table, return it!
                    row = (HashMap) rs.get(0);
                    xbin = new XanbooBinaryContent(contentType, (byte[]) row.get("CONTENT"));
                    xbin.setLastModified(ts);
                    return xbin;
                }

                //not found in DB mobject_binary -> get it from File system as legacy BAU - TO bE DEPRECATED!!!
                String filePath = AbstractFSProvider.getBaseDir(null, xCaller.getDomain(), xCaller.getAccountId(), AbstractFSProvider.DIR_ACCOUNT_MOBJECT) + "/" + xCaller.getGatewayGuid() + "/" + xCaller.getDeviceGuid() + "." + mobjectId + ".0" ;
                try {
                    byte[] bytes = XanbooFSProviderProxy.getInstance().getFileBytes(null, filePath);
                    xbin = new XanbooBinaryContent(contentType, bytes);
                    xbin.setLastModified(ts);
                    return xbin;
                }catch(Exception e) {
                    throw new XanbooException(22120, "Failed to get item binary from filesystem. File not found: " + filePath);
                }
            }
            
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getMObjectBinary()]: " + e.getMessage(), e);
            }else {
              logger.error("[getMObjectBinary()]: " + e.getMessage());
            }                              
            throw new XanbooException(10030, "[getMObjectBinary]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }

        return null;    //mobject not found
    }
    
    
}
