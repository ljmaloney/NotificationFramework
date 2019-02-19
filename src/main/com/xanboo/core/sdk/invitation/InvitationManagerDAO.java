/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/invitation/InvitationManagerDAO.java,v $
 * $Id: InvitationManagerDAO.java,v 1.15 2008/09/25 18:47:34 levent Exp $
 *
 * Copyright Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.invitation;

import java.util.*;
import java.sql.*;

import com.xanboo.core.util.*;
import com.xanboo.core.model.*;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.model.XanbooGateway;


/**
 * This class is the DAO class to wrap all generic database calls for SDK InvitationManager methods.
 * Database specific methods are handled by implementation classes. These implementation
 * classes extend the BaseDAO class to actually perform the database operations. An instance of
 * an implementation class is created during contruction of this class.
 */
class InvitationManagerDAO extends BaseHandlerDAO {
    
    private BaseDAO dao;
    private Logger logger;
    
    /** Creates new InvitationManagerDAO */
    public InvitationManagerDAO() throws XanbooException{
        
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
              logger.error("[InvitationManagerDAO()]: " + ne.getMessage(), ne);
            }else {
              logger.error("[InvitationManagerDAO()]: " + ne.getMessage());
            }
            throw new XanbooException(20014, "[InvitationManagerDAO()]: " + ne.getMessage());
        }
    }
    
    public XanbooInvitee[] addInvitation(Connection conn, XanbooPrincipal xCaller, String subject, String message, int objectType, String objectId, java.util.Date expiration, XanbooInvitee[] invitees) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[addInvitation()]:");
        }
        
        long invitationId=0;
        int existingInviteeCount=0;
        
        SQLParam[] args=new SQLParam[9+2];     // 4 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Integer(objectType), Types.INTEGER);
        args[3] = new SQLParam(new String(objectId), Types.VARCHAR);
        args[4] = new SQLParam(message);
        args[5] = new SQLParam(subject);
        args[6] = new SQLParam(XanbooUtil.getDateTimeInGMT(expiration), Types.VARCHAR);
        
        // set OUT params
        args[7] = new SQLParam(new Long(0), Types.BIGINT, true);
        args[8] = new SQLParam(new Integer(0), Types.INTEGER, true);
        
        dao.callSP(conn, "XC_INVITATION_PKG.addInvitation", args, false);

        // check device invitation quota
        if( objectType == InvitationManager.INVITATION_DEVICE ) {
            existingInviteeCount = ((Integer) args[8].getParam()).intValue();
            if( existingInviteeCount + invitees.length > GlobalNames.DEVICE_INVITATION_QUOTA ) throw new XanbooException(28322, "Device invitation quota exceeded");
        }

        invitationId = ((Long) args[7].getParam()).longValue();
        XanbooInvitee[] xanbooInviteeTemp = null;
        if(invitees!= null && invitees.length>0) {
        	xanbooInviteeTemp = new XanbooInvitee[invitees.length];
        }
        for (int i=0; i<invitees.length; i++) {
            xanbooInviteeTemp[i] = addInvitee( conn, xCaller, invitees[i], invitationId);
        }
        return xanbooInviteeTemp;
    }
    
    
    /**
     * Add a recipient (invitee) to an invitation
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties of the user are used to authenticate this call
     * @param invitation_id The ID of the invitation to which we want to add a recipient.
     * @param invitee A XanbooInvitee object identifying the recipient
     * @param invitationId The ID of the invitation with which this invitee is associated
     */
    public XanbooInvitee addInvitee(Connection conn, XanbooPrincipal xCaller, XanbooInvitee invitee, long invitationId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[addInvitee()]:"  + Long.toString(invitee.getContactId()));
        }
        XanbooInvitee xanbooInvitee = new XanbooInvitee();
        String viewKey = XanbooUtil.generateKey(56);
        long inviteeId=0;
        SQLParam[] args=new SQLParam[10+2];     // 9 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT); //account id
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);    //user id
        args[2] = new SQLParam(new Long(invitationId), Types.BIGINT);           //invitation id
        args[3] = new SQLParam(new Long(invitee.getGroupId()), Types.BIGINT );  //group id
        args[4] = new SQLParam(new Long(invitee.getContactId()), Types.BIGINT );//contact id
        args[5] = new SQLParam(new String(invitee.getToAddress()));               //to email
        args[6] = new SQLParam(new String(viewKey));           					//view key
        args[7] = new SQLParam(new Long(invitee.getSentStatus()), Types.BIGINT); //invitationstatus_id
        args[8] = new SQLParam(invitee.getLastViewed(), Types.DATE);      		//lastviewed date
        
        // set OUT params
        args[9] = new SQLParam(new Long(0), Types.BIGINT, true); // return the INVITEE_ID
        
        dao.callSP(conn, "XC_INVITATION_PKG.addInvitee", args, false);
        
        if (args[9].getParam()!=null && !"".equals(args[9].getParam())){
        	inviteeId = ((Long) args[9].getParam()).longValue();
        }
        
        xanbooInvitee.setContactId(invitee.getContactId());
        xanbooInvitee.setGroupId(invitee.getGroupId());
        xanbooInvitee.setInvitationId(invitationId);
        xanbooInvitee.setToAddress(invitee.getToAddress());
        xanbooInvitee.setViewKey(viewKey);
        xanbooInvitee.setInviteeId(inviteeId);
        
        return xanbooInvitee;
    }
    
    
    /**
     * Retrieves a list of invitees that are recipients of a particular inviatation
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties are used to authenticate this call.
     * @param invitationId The ID of the invitation from which we want to retrieve invitees
     * @return A XanbooResultSet of invitees who recieved the specified invitation
     */
    public XanbooResultSet getInviteeList(Connection conn, XanbooPrincipal xCaller, long invitationId ) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[getInviteeList(start,num)]:");
        }
        
        SQLParam[] args=new SQLParam[3+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(invitationId), Types.BIGINT); //folder ID
        
        return dao.callSP(conn, "XC_INVITATION_PKG.GETINVITEELIST", args );
        
    }
    
    /**
     * Deletes an invitation and all of it's associated invitees from the database.
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties are used to authenticate this call
     * @param invitationId The ID of the invitation to be deleted.
     */
    public void deleteInvitation(Connection conn, XanbooPrincipal xCaller, long invitationId ) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteInvitation()]:");
        }
        
        SQLParam[] args=new SQLParam[3+2];     // 4 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(invitationId), Types.BIGINT);
        
        dao.callSP(conn, "XC_INVITATION_PKG.DELETEINVITATION", args, false);
        
    }
    
    
    /**
     * Retrieves either a specific invitation, or a list of invitations associated with a user.
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties are used to authenticate this call.
     * @param invitationId The ID of the invitation from which we want to retrieve invitees. Use 0 for a full list
     * @return a XanbooResultSet of invitations.
     */
    public XanbooResultSet getInvitation(Connection conn, XanbooPrincipal xCaller, long invitationId ) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[getInvitation(start,num)]:");
        }
        
        SQLParam[] args=new SQLParam[3+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(invitationId), Types.BIGINT); //folder ID
        
        return dao.callSP(conn, "XC_INVITATION_PKG.GETINVITATION", args );
        
    }
    
    
    /**
     * Removes an invitee from an invitation by ID. The ID can
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties are used to authenticate this call
     * @param invitationId The Invitation from which the invitee is to be deleted
     * @param inviteeId The Id of the invitee to be removed
     */
    public void deleteInvitee(Connection conn, XanbooPrincipal xCaller, long invitationId, long inviteeId ) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteInvitee()]:");
        }
        
        SQLParam[] args=new SQLParam[4+2];     // 4 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(invitationId), Types.BIGINT);
        args[3] = new SQLParam(new Long(inviteeId), Types.BIGINT);
        
        dao.callSP(conn, "XC_INVITATION_PKG.DELETEINVITEE", args, false);
        
    }
    
    
    /**
     * Retrieves a XanbooResultSet by ID & invitation key
     * @param conn The database connection to use for this call
     * @param invitationId The ID of the invitation to retrieve
     * @param viewKey The key received by the invitee (to authenticate the viewer)
     * @return A XanbooResultSet object
     */
    public XanbooResultSet getInvitationByKey( Connection conn, long inviteeId, String viewKey ) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[getInvitationByKey]:");
        }
        
        SQLParam[] args=new SQLParam[2+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(inviteeId), Types.BIGINT);
        args[1] = new SQLParam(viewKey, Types.VARCHAR);
        
        return dao.callSP(conn, "XC_INVITATION_PKG.GETINVITATIONBYKEY", args );
        
    }
    
    /**
     * Returns a list of items that the invitee has been invited to see
     * @param conn The database connection to use for this call
     * @param invitationId The ID of the invitation that the invitee is associated with
     * @param viewKey The key received by the invitee (to authenticate the viewer)
     */
    public XanbooResultSet getInvitationItemListByKey( Connection conn, long invitationId, String viewKey, int sortBy ) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[getInvitationItemListByKey]:");
        }
        
        SQLParam[] args=new SQLParam[4+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(invitationId), Types.BIGINT);
        args[1] = new SQLParam(viewKey, Types.VARCHAR);
        args[2] = new SQLParam(new Integer(sortBy), Types.INTEGER);
        args[3] = new SQLParam(new Integer(-1), Types.INTEGER, true);
        
        return dao.callSP(conn, "XC_INVITATION_PKG.GETINVITATIONITEMLISTBYKEY", args );
        
    }
    
    /**
     * Returns a list of items that the invitee has been invited to see
     * @param conn The database connection to use for this call
     * @param invitationId The ID of the invitation that the invitee is associated with
     * @param viewKey The key received by the invitee (to authenticate the viewer)
     */
    public XanbooResultSet getInvitationItemListByKey( Connection conn, long invitationId, String viewKey, int startRow, int numRows, int sortBy ) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[getInvitationItemListByKey]:");
        }
        
        SQLParam[] args=new SQLParam[4+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(invitationId), Types.BIGINT);
        args[1] = new SQLParam(viewKey, Types.VARCHAR);
        args[2] = new SQLParam(new Integer(sortBy), Types.INTEGER);
        args[3] = new SQLParam(new Integer(-1), Types.INTEGER, true);
        
        XanbooResultSet results = dao.callSP(conn, "XC_INVITATION_PKG.GETINVITATIONITEMLISTBYKEY", args, startRow, numRows );
        results.setSize(((Integer) args[3].getParam()).intValue());
        return results;
    }
    
    
    /**
     * Returns a list of items that the invitee has been invited to see
     * @param conn The database connection to use for this call
     * @param invitationId The ID of the invitation that the invitee is associated with
     * @param viewKey The key received by the invitee (to authenticate the viewer)
     */
    public XanbooResultSet getInvitationItemNotesByKey( Connection conn, long invitationId, String viewKey, long folderItemId ) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[getInvitationItemNotesByKey]:");
        }
        
        SQLParam[] args=new SQLParam[3+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(invitationId), Types.BIGINT);   //invitation id
        args[1] = new SQLParam(viewKey, Types.VARCHAR);                 //view key
        args[2] = new SQLParam(new Long(folderItemId), Types.BIGINT);   //item id
        
        XanbooResultSet results = dao.callSP(conn, "XC_INVITATION_PKG.GETINVITATIONITEMNOTESBYKEY", args );
        results.setSize(((Integer) args[3].getParam()).intValue());
        return results;
        
    }
    
    
    /**
     * Returns a XanbooItem object that belongs to a parituclar inviation
     * @param conn The database connection to use for this call
     * @param invitationId The ID of the invitation that the invitee is associated with
     * @param viewKey The key received by the invitee (to authenticate the viewer)
     * @param folderItemId The ID of the item to be returned
     */
    public XanbooItem getInvitationItemByKey( Connection conn, long invitationId, String viewKey, long folderItemId ) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[getInvitationItemByKey]:");
        }
        
        XanbooResultSet iTmp = null;
        SQLParam[] args=new SQLParam[3+2];     // 3 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(invitationId), Types.BIGINT);
        args[1] = new SQLParam(viewKey, Types.VARCHAR);
        args[2] = new SQLParam(new Long(folderItemId), Types.BIGINT);
        
        iTmp = (XanbooResultSet) dao.callSP(conn, "XC_INVITATION_PKG.GETINVITATIONITEMBYKEY", args);
        if (iTmp.size() == 0) {
            throw new XanbooException(22120, "Failed to get item. Item not found.");
        }
        
        XanbooItem item = new XanbooItem() ;
        HashMap itemRecord = (HashMap)iTmp.get(0);
        item.setItemId( folderItemId );
        item.setName( (String)itemRecord.get("NAME") );
        item.setSourceLabel( (String)itemRecord.get("SOURCE_LABEL"));
        item.setStatus( Integer.parseInt((String)itemRecord.get("STATUS_ID")) );
        item.setCreationDate( (String)itemRecord.get("DATE_CREATED"));
        item.setTimestamp( (String)itemRecord.get("TIMESTAMP"));
        
        item.setMount( (String)itemRecord.get("ITEM_MOUNT"));
        item.setItemDirectory( (String)itemRecord.get("ITEM_PATH"));
        item.setItemFilename( (String)itemRecord.get("ITEM_FILE"));
        item.setThumbFilename( (String)itemRecord.get("THUMB_FILE"));
        item.setDomain( (String)itemRecord.get("DOMAIN_ID"));
        item.setAccountId( Long.parseLong((String)itemRecord.get("ACCOUNT_ID")));
        
        item.setItemType( (String)itemRecord.get("ITEM_CONTENTTYPE"));
        item.setItemSize( Long.parseLong((String) itemRecord.get("ITEM_SIZE")));
        
        if(itemRecord.get("PREV_ITEM_ID") != null && itemRecord.get("PREV_ITEM_ID") != "" ) item.setPrevItemId( Long.parseLong((String) itemRecord.get("PREV_ITEM_ID")));
        if(itemRecord.get("NEXT_ITEM_ID") != null && itemRecord.get("NEXT_ITEM_ID") != "" ) item.setNextItemId( Long.parseLong((String) itemRecord.get("NEXT_ITEM_ID")));
        
        return item;
        
    }
    


    public XanbooGateway getGatewayInfo(Connection conn, long accountId, String gatewayGUID)  throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getGatewayInfo()]: ");
        }
        
        SQLParam[] args=new SQLParam[22+2];     // SP parameter + 2 std parameters (errno, errmsg)
        
        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT); // fortunately accountId isn't authenticated
        args[1] = new SQLParam(gatewayGUID, Types.VARCHAR);
        
        // OUT param --> action id just created.
        args[2] = new SQLParam("", Types.VARCHAR, true);   // headerIP
        args[3] = new SQLParam("", Types.VARCHAR, true);   // applianceIP
        args[4] = new SQLParam("", Types.VARCHAR, true);   // applianceIP2
        args[5] = new SQLParam("", Types.VARCHAR, true);   // inbound
        args[6] = new SQLParam("", Types.VARCHAR, true);   // inboundPort
        args[7] = new SQLParam("", Types.VARCHAR, true);   // token
        args[8] = new SQLParam("", Types.VARCHAR, true);   // proxyIP
        args[9] = new SQLParam("", Types.VARCHAR, true);   // proxyPort
        args[10] = new SQLParam("", Types.VARCHAR, true);   // capabilities oid
        args[11] = new SQLParam("", Types.VARCHAR, true);   // inboundPortUDP
        args[12] = new SQLParam("", Types.VARCHAR, true);   // NAT inbound
        args[13] = new SQLParam("", Types.VARCHAR, true);   // Alarm Manager
        args[14] = new SQLParam("", Types.VARCHAR, true);   // LVOA Token
        args[15] = new SQLParam("", Types.VARCHAR, true);   // LVOA Policy
        args[16] = new SQLParam("", Types.VARCHAR, true);   // 3G IP
        args[17] = new SQLParam("", Types.VARCHAR, true);   // 3G&BB status
        args[18] = new SQLParam(new Long(-1), Types.BIGINT, true);    //account id
        args[19] = new SQLParam("",Types.VARCHAR,true); //pai_Server_url
        args[20] = new SQLParam("",Types.VARCHAR,true); //pai_token
        args[21] = new SQLParam("",Types.VARCHAR,true); //log level
        
        XanbooGateway gwy = new XanbooGateway(gatewayGUID, -1);
        
        try {
            dao.callSP(conn, "XC_DEVICE_PKG.GETGATEWAYINFO", args, false);
            gwy.setHeaderIP((String) args[2].getParam());
            gwy.setApplianceIP((String) args[3].getParam());
            gwy.setApplianceIP2((String) args[4].getParam());

            try { gwy.setInbound(Integer.parseInt(args[5].getParam().toString()));
            }catch(NumberFormatException ne) { gwy.setInbound(0);   }    //default
            
            try { gwy.setInboundPort(Integer.parseInt(args[6].getParam().toString()));
            }catch(NumberFormatException ne) { gwy.setInboundPort(2047);   }    //default

            gwy.setToken((String) args[7].getParam());
            gwy.setProxyIP((String) args[8].getParam());
            
            try { gwy.setProxyPort(Integer.parseInt(args[9].getParam().toString()));
            }catch(NumberFormatException ne) {  gwy.setProxyPort(2047);    } //default

            gwy.setCaps((String) args[10].getParam());

            try { gwy.setInboundPortUDP(Integer.parseInt(args[11].getParam().toString()));
            }catch(NumberFormatException ne) { gwy.setInboundPortUDP(-999);   }    //default
            
            try { gwy.setNATInbound(Integer.parseInt(args[12].getParam().toString()));
            }catch(NumberFormatException ne) { gwy.setNATInbound(0);   }    //default

            gwy.setAlarmManager((String) args[13].getParam());
            gwy.setALVToken((String) args[14].getParam());
            gwy.setALVPolicy((String) args[15].getParam());
            gwy.setWirelessIP((String) args[16].getParam());

            try { gwy.setConnStatus(Integer.parseInt(args[17].getParam().toString()));
            }catch(NumberFormatException ne) { gwy.setConnStatus(0);   }    //default

            gwy.setAccount(Long.parseLong(args[18].getParam().toString()));

            gwy.setPAIServerURI((String)args[19].getParam());
            gwy.setPAIAccessToken((String)args[20].getParam());
            if ( logger.isDebugEnabled() )
                logger.debug("[getGatewayInfo] - paiServerURL="+gwy.getPAIServerURI()+" token="+gwy.getPAIAccessToken());

            try { gwy.setLogLevel(Integer.parseInt(args[21].getParam().toString()));
            }catch(NumberFormatException ne) { gwy.setLogLevel(-1);   }    //default
            
            return gwy;
        }catch(XanbooException xe) {
            throw xe;
        }
        
    }    
    
    public long setMObject(Connection conn, long accountId, long userId, String gatewayGUID, String deviceGUID, String[] mobjectId, String[] mobjectValue) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[setMObject()]: ");
        }
        
        int COMMANDID_SET_OBJECT = 0;
        int COMMANDID_SET_OBJECT_LIST = 1;
        
        SQLParam[] args=new SQLParam[10+2];     // SP parameter + 2 std parameters (errno, errmsg)
        
        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID, Types.VARCHAR);
        args[3] = new SQLParam(deviceGUID, Types.VARCHAR);
        // set the first pair (which is guaranteed to exist
        args[4] = new SQLParam(mobjectId[0], Types.VARCHAR);
        args[5] = new SQLParam(mobjectValue[0], Types.VARCHAR);
        args[6] = new SQLParam(null, Types.NULL);
        
        args[8] = new SQLParam(new Integer( (mobjectId.length == 1) ? COMMANDID_SET_OBJECT : COMMANDID_SET_OBJECT_LIST ), Types.INTEGER);
        args[9] = new SQLParam(new Long(-1), Types.BIGINT, true);
        
        long commandQueueId=-1;
        
        
        try {
            int checkDuplicateCommands = 0;

            // filter dup commands, only if it is a single mobject set and it is NOT webcam on/off command
            // no way to determine 1040 is issued for a camera device, disabling dup check for all 1040 sets
            if(mobjectId.length==1 && !mobjectId[0].equals("1040")) {
                checkDuplicateCommands = 1;
            }
            
            // do not check dups for group commands
            if(deviceGUID.charAt(0)=='g' || deviceGUID.charAt(0)=='G') {
                checkDuplicateCommands = 0;
            }
            args[7] = new SQLParam(new Integer(checkDuplicateCommands), Types.INTEGER);
            
            dao.callSP(conn, "XC_DEVICE_PKG.SETMOBJECT", args, false);
            commandQueueId = ((Long) args[9].getParam()).longValue();
            
            // insert other mobject id/value pairs as part of the same setmobject command (set as a group)
            args=new SQLParam[8+2];     // SP parameters + 2 std parameters (errno, errmsg)
            args[0] = new SQLParam(new Long(commandQueueId), Types.BIGINT);
            for(int i=1; i<mobjectId.length; i++) {
                args[1] = new SQLParam(new Long(accountId), Types.BIGINT);
                args[2] = new SQLParam(gatewayGUID, Types.VARCHAR);
                args[3] = new SQLParam(deviceGUID, Types.VARCHAR);
                args[4] = new SQLParam(mobjectId[i], Types.VARCHAR);
                args[5] = new SQLParam(mobjectValue[i], Types.VARCHAR);
                args[6] = new SQLParam(null, Types.NULL);
                args[7] = new SQLParam(new Integer(checkDuplicateCommands), Types.INTEGER);
                dao.callSP(conn, "XC_DEVICE_PKG.SETMOBJECTPARAM", args, false);
            }
        }catch(XanbooException xe) {
            throw xe;
        }
        
        return commandQueueId;
    }
    
    /**
     * Returns managed object values and definitions for a device instance
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier of the managed object.
     * @param mobjectId the managed object id to retrieve the values and definitions for. If null,
     *                  all managed object values for the specified device are returned.
     *
     * @return a XanbooResultSet which contains a HashMap list of devices
     * @throws XanbooException
     */
    public XanbooResultSet getMObject(Connection conn, long accountId, long userId, String gatewayGUID,
                String deviceGUID, String mobjectId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getMObject()]: ");
        }
        
        // setting IN params
        SQLParam[] args = new SQLParam[5+2];
        
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID, Types.VARCHAR);
        args[3] = new SQLParam(deviceGUID, Types.VARCHAR);
        args[4] = new SQLParam(mobjectId, Types.VARCHAR);
        
        try {
            return dao.callSP(conn, "XC_DEVICE_PKG.GETMOBJECT", args);
        }catch(XanbooException xe) {
            throw xe;
        }
        
    }
    
    
    /**
     * Returns managed object binaries for a device/mobject instance
     */
    public XanbooResultSet getMObjectBinary(Connection conn, long accountId, long userId, String gatewayGUID,
                String deviceGUID, String mobjectId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getMObjectBinary()]: ");
        }
        
        // setting IN params
        SQLParam[] args = new SQLParam[5+2];
        
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID, Types.VARCHAR);
        args[3] = new SQLParam(deviceGUID, Types.VARCHAR);
        args[4] = new SQLParam(mobjectId, Types.VARCHAR);
        
        try {
            return dao.callSP(conn, "XC_DEVICE_PKG.GETMOBJECTBINARY", args);
        }catch(XanbooException xe) {
            throw xe;
        }
        
    }
    
}
