/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/device/DeviceManagerDAO.java,v $
 * $Id: DeviceManagerDAO.java,v 1.50 2010/05/07 16:33:11 levent Exp $
 *
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.device;

import java.util.*;
import java.sql.*;

import com.xanboo.core.util.*;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.model.XanbooGateway;

import com.xanboo.core.security.XanbooPrincipal;

/**
 * This class is the DAO class to wrap all generic database calls for SDK DeviceManager methods.
 * Database specific methods are handled by implementation classes. These implementation
 * classes extend the BaseDAO class to actually perform the database operations. An instance of
 * an implementation class is created during contruction of this class.
 */
class DeviceManagerDAO extends BaseHandlerDAO {
    
    private BaseDAO dao;
    private Logger logger;
    
    static final String SP_DEVICE_CLASS_LIST="XC_DEVICE_PKG.GETDEVICECLASSLIST";
    static final String SP_ACTION_TYPE_LIST="XC_DEVICE_PKG.GETACTIONTYPELIST";
    
    /**
     * Default constructor. Takes no arguments
     *
     * @throws XanbooException
     */
    public DeviceManagerDAO() throws XanbooException {
        
        try {
            // obtain a Logger instance
            logger=LoggerFactory.getLogger(this.getClass().getName());
            if(logger.isDebugEnabled()) {
                logger.debug("[DeviceManagerDAO()]:");
            }
            
            // create implementation Class for Oracle, Sybase, etc.
            dao = (BaseDAO) DAOFactory.getDAO();
            
            // get the Connection factory DataSource for CoreDS
            getDataSource(GlobalNames.COREDS);
            
        }catch(XanbooException xe) {
            throw xe;
        }catch(Exception ne) {
            if(logger.isDebugEnabled()) {
              logger.error("[DeviceManagerDAO()] Exception:" + ne.getMessage(), ne);
            }else {
              logger.error("[DeviceManagerDAO()] Exception: " + ne.getMessage());
            }                
            throw new XanbooException(20014, "[DeviceManagerDAO()] Exception:" + ne.getMessage());
        }
    }
    
    /**
     * Generic reference data query procewdure (device class list, action types, etc.)
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param spName stored procedure name to call for reference data retrieval.
     * @param lang the language in which the reference data will be returned.
     *
     * @return a XanbooResultSet which contains a HashMap list of device classes
     * @throws XanbooException
     */
    public XanbooResultSet getReferenceData(Connection conn, long accountId, long userId, String spName, String lang) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getReferenceData()]:");
        }
        
        SQLParam[] args=new SQLParam[3+2];     // 3 SP parameters + 2 std parameters (errno, errmsg)
        
        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam(lang, Types.VARCHAR);
        
        try {
            return dao.callSP(conn, spName, args);
        }catch(XanbooException xe) {
            throw xe;
        }
        
    }
    
    public String getUserLanguage(Connection conn,XanbooPrincipal xCaller,String gatewayGuid,boolean includeMasterUser) throws XanbooException
    {
        if (logger.isDebugEnabled()) 
        {
            logger.debug("[getUserLanguage()]:");
        }
        
        SQLParam[] args=new SQLParam[3+2];     // 3 SP parameters + 2 std parameters (errno, errmsg)
        
        // setting IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(gatewayGuid,Types.VARCHAR);
        if ( includeMasterUser )
            args[2] = new SQLParam(1, Types.NUMERIC);
        else
            args[2] = new SQLParam(0,Types.NUMERIC);
        
        try 
        {
            XanbooResultSet rs = dao.callSP(conn, "XC_DEVICE_PKG.getUserLanguage", args);
            HashMap map = (HashMap)rs.get(0); //only one row from this proc
            return (String)map.get("USER_LANGUAGE");            
        }
        catch(XanbooException xe) 
        {
            throw xe;
        }
    }
    
    /**
     * Returns the list of devices of the specified class within an account.
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param dClass device class id for the query. If null, all devices are returned.
     *
     * @return a XanbooResultSet which contains a HashMap list of devices
     * @throws XanbooException
     */
    public XanbooResultSet getDeviceListByClass(Connection conn, XanbooPrincipal xCaller, String dClass) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getDeviceList()]: ");
        }
        
        // setting IN params
        SQLParam[] args=null;
        args = new SQLParam[4+2];     // if class specified, 3 SP parameters
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Integer( xCaller.isMaster() ? 1 : 0 ), Types.INTEGER);
        args[3] = new SQLParam(dClass, Types.VARCHAR);
        
        try {
            return dao.callSP(conn, "XC_DEVICE_PKG.GETDEVICELISTBYCLASS", args);
        }catch(XanbooException xe) {
            throw xe;
        }
        
    }
    
    
    /**
     * Returns detailed (instance+catalog) information for a specific device
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier to get the device information for.
     *
     * @return a XanbooResultSet which contains a HashMap list of devices
     * @throws XanbooException
     */
    public XanbooResultSet getDeviceList(Connection conn, XanbooPrincipal xCaller, String gatewayGUID,
    String deviceGUID) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getDeviceList()]: ");
        }
        
        SQLParam[] args=new SQLParam[5+2];     // 2 SP parameter + 2 std parameters (errno, errmsg)
        
        // setting IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Integer( xCaller.isMaster() ? 1 : 0 ), Types.INTEGER);
        args[3] = new SQLParam(gatewayGUID, Types.VARCHAR);
        args[4] = new SQLParam(deviceGUID, Types.VARCHAR);
        
        try {
            return dao.callSP(conn, "XC_DEVICE_PKG.GETDEVICELIST", args);
        }catch(XanbooException xe) {
            throw xe;
        }
        
    }
    
    
    /**
     * Updates status for a specific device
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier to update.
     * @param status The new status for the appliance - must be one of DeviceManagerEJB.DEVICE_STATUS_ACTIVE or DeviceManagerEJB.DEVICE_STATUS_INACTIVE
     *
     * @throws XanbooException
     */
    public void updateDeviceStatus(Connection conn, long accountId, long userId, String gatewayGUID,
    String deviceGUID, int status) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[updateDeviceStatus()]: ");
        }
        
        SQLParam[] args=new SQLParam[5+2];     // 5 SP parameter + 2 std parameters (errno, errmsg)
        
        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID, Types.VARCHAR);
        args[3] = new SQLParam(deviceGUID, Types.VARCHAR);
        args[4] = new SQLParam(new Integer(status), Types.INTEGER);
        
        try {
            dao.callSP(conn, "XC_DEVICE_PKG.UPDATEDEVICESTATUS", args, false);
        }catch(XanbooException xe) {
            throw xe;
        }
        
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
        SQLParam[] args=null;
        args = new SQLParam[5+2];
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
    
    
    
    /**
     * Sets the value of managed object(s) on a device instance.
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier for the destination device.
     * @param mobjectId the managed object id to update.
     * @param label the new label to set for the managed object instance
     * @param desc the new description to set for the managed object instance
     *
     * @return a numeric command queue identifier
     * @throws XanbooException
     */
    public long setMObject(Connection conn, long accountId, long userId, String gatewayGUID,
    String deviceGUID, String[] mobjectId, String[] mobjectValue, String[] valueContentType) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[setMObject()]: ");
        }
        
        int COMMANDID_SET_OBJECT = 0;
        int COMMANDID_SET_OBJECT_LIST = 1;
        
        SQLParam[] args=new SQLParam[10+2];     // 7 SP parameter + 2 std parameters (errno, errmsg)
        
        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID, Types.VARCHAR);
        args[3] = new SQLParam(deviceGUID, Types.VARCHAR);
        // set the first pair (which is guaranteed to exist
        args[4] = new SQLParam(mobjectId[0], Types.VARCHAR);
        if(mobjectValue==null || mobjectValue[0]==null) {
            args[5] = new SQLParam("null", Types.VARCHAR);
        }else {
            args[5] = new SQLParam(mobjectValue[0], Types.VARCHAR);
        }

        if(valueContentType==null || valueContentType[0]==null) {
            args[6] = new SQLParam(null, Types.NULL);
        }else {
            args[6] = new SQLParam(valueContentType[0], Types.VARCHAR);
        }
        
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
                
                if(mobjectValue==null || mobjectValue[i]==null) {
                    args[5] = new SQLParam("null", Types.VARCHAR);
                }else {
                    args[5] = new SQLParam(mobjectValue[i], Types.VARCHAR);
                }
                
                if(valueContentType==null || valueContentType[i]==null) {
                    args[6] = new SQLParam(null, Types.NULL);
                }else {
                    args[6] = new SQLParam(valueContentType[i], Types.VARCHAR);
                }
                args[7] = new SQLParam(new Integer(checkDuplicateCommands), Types.INTEGER);
                
                dao.callSP(conn, "XC_DEVICE_PKG.SETMOBJECTPARAM", args, false);
            }
        }catch(XanbooException xe) {
            throw xe;
        }
        
        return commandQueueId;
    }
    

    /**
     * Sets the value of managed object binary for a device/mobject instance.
     */
    public void setMObjectBinary(Connection conn, long accountId, long userId, String gatewayGUID,
                    String deviceGUID, String mobjectId, byte[] content) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[setMObjectBinary()]: ");
        }
        
        SQLParam[] args=new SQLParam[6+2];     // 7 SP parameter + 2 std parameters (errno, errmsg)
        
        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID, Types.VARCHAR);
        args[3] = new SQLParam(deviceGUID, Types.VARCHAR);
        args[4] = new SQLParam(mobjectId, Types.VARCHAR);

        if(content==null) {
            args[5] = new SQLParam(null, Types.NULL);
        }else {
            args[5] = new SQLParam(content, Types.BLOB);
        }
        
        try {
            dao.callSP(conn, "XC_DEVICE_PKG.SETMOBJECTBINARY", args, false);
            
        }catch(XanbooException xe) {
            throw xe;
        }
    }
    
    
    /**
     * Returns event definitions for a device instance
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier for the event.
     * @param eventId the event to retrieve the definitions for. If null,
     *                all event definitions for the specified device are returned.
     *
     * @return a XanbooResultSet which contains a HashMap list of devices
     * @throws XanbooException
     */
    public XanbooResultSet getDeviceEvent(Connection conn, long accountId, long userId, String gatewayGUID,
    String deviceGUID, String eventId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getDeviceEvent()]: ");
        }
        
        // setting IN params
        SQLParam[] args = new SQLParam[5+2];
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID, Types.VARCHAR);
        args[3] = new SQLParam(deviceGUID, Types.VARCHAR);
        try {
            args[4] = new SQLParam((eventId == null ? null : new Integer(eventId)), Types.INTEGER);
        }catch(Exception eee) { args[4] = new SQLParam(null, Types.INTEGER); }
        
        try {
            return dao.callSP(conn, "XC_DEVICE_PKG.GETDEVICEEVENT", args);
        }catch(XanbooException xe) {
            throw xe;
        }
        
    }
    
    
    
    /**
     * Returns event log entries for a specific gateway or device instance
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier If null, all event log entries for the
     *                   whole gateway are returned.
     * @param eventId the event identifier to retrieve the log entries for. If null,
     *                all events log records for the specified gateway/device are returned.
     *
     * @return a XanbooResultSet which contains a HashMap list of devices
     * @throws XanbooException
     */
    public XanbooResultSet getDeviceEventLog(Connection conn, long accountId, long userId, String gatewayGUID,
    String deviceGUID, String eventId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getDeviceEventLog()]: ");
        }
        
        // setting IN params
        SQLParam[] args = new SQLParam[5+2];
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID, Types.VARCHAR);
        args[3] = new SQLParam(deviceGUID, Types.VARCHAR);
        try {
            args[4] = new SQLParam((eventId == null ? null : new Integer(eventId)), Types.INTEGER);
        }catch(Exception eee) { args[4] = new SQLParam(null, Types.INTEGER); }
        
        try {
            return dao.callSP(conn, "XC_DEVICE_PKG.GETDEVICEEVENTLOG", args);
        }catch(XanbooException xe) {
            throw xe;
        }
    }
    
    
    /**
     * Clears event log entries for a specific gateway or device instance
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier If null, all event log entries for the
     *                   whole gateway are cleared.
     * @param eventId the event identifier to retrieve the log entries for. If null,
     *                all events log records for the specified gateway/device are cleared.
     *
     * @throws XanbooException
     */
    public void clearDeviceEventLog(Connection conn, long accountId, long userId, String gatewayGUID,
    String deviceGUID, String eventId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[clearDeviceEventLog()]: ");
        }
        
        // setting IN params
        SQLParam[] args = new SQLParam[5+2];
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID, Types.VARCHAR);
        args[3] = new SQLParam(deviceGUID, Types.VARCHAR);
        try {
            args[4] = new SQLParam((eventId == null ? null : new Integer(eventId)), Types.INTEGER);
        }catch(Exception eee) { args[4] = new SQLParam(null, Types.INTEGER); }
        
        try {
            dao.callSP(conn, "XC_DEVICE_PKG.CLEARDEVICEEVENTLOG", args, false);
        }catch(XanbooException xe) {
            throw xe;
        }
    }
    
    
    /**
     * Returns pending commands for a specific gateway or device instance
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier. If null, all pending commands for the whole gateway are returned.
     *
     * @return a XanbooResultSet which contains a HashMap list of devices
     * @throws XanbooException
     */
    public XanbooResultSet getCommandQueueItem(Connection conn, long accountId, long userId, String gatewayGUID,
    String deviceGUID) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getCommandQueueItem()]: ");
        }
        
        // setting IN params
        SQLParam[] args = new SQLParam[4+2];
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID);
        args[3] = new SQLParam(deviceGUID);
        
        try {
            return dao.callSP(conn, "XC_DEVICE_PKG.GETCOMMANDQUEUEITEM", args);
        }catch(XanbooException xe) {
            throw xe;
        }
        
    }
    
    
    /**
     * Removes pending commands from a gateway/device command queue.
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param gatewayGUID the gateway GUID of the command queue
     * @param queueId an array list of command queue ids to remove
     *
     * @throws XanbooException
     */
    public void deleteCommandQueueItem(Connection conn, long accountId, long userId, String gatewayGUID,
    long[] queueId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteCommandQueueItem()]: ");
        }
        
        SQLParam[] args=new SQLParam[4+2];     // 7 SP parameter + 2 std parameters (errno, errmsg)
        
        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID);
        
        for(int i=0; i<queueId.length; i++) {
            args[3] = new SQLParam(new Long(queueId[i]), Types.BIGINT);
            
            try {
                dao.callSP(conn, "XC_DEVICE_PKG.DELETECOMMANDQUEUEITEM", args, false);
            }catch(XanbooException xe) {
                throw xe;
            }
        }
    }
    
    
    /**
     * Clears all pending commands from a gateway/device instance.
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param gatewayGUID the gateway GUID of the command queue.
     * @param deviceGUID the device id of the command queue. If null, all pending commands
     *                   for the whole gateway are cleared.
     *
     * @throws XanbooException
     */
    public void emptyCommandQueue(Connection conn, long accountId, long userId, String gatewayGUID, String deviceGUID) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[emptyCommandQueue()]: ");
        }
        
        SQLParam[] args=new SQLParam[4+2];     // 7 SP parameter + 2 std parameters (errno, errmsg)
        
        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID);
        args[3] = new SQLParam(deviceGUID);
        
        try {
            dao.callSP(conn, "XC_DEVICE_PKG.EMPTYCOMMANDQUEUE", args, false);
        }catch(XanbooException xe) {
            throw xe;
        }
        
    }
    
    
    /**
     * Returns list of notification actions defined for a device event instance
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier.
     * @param eventId the event identifier to retrieve the actions for.
     *
     * @return a XanbooResultSet which contains a HashMap list of devices
     * @throws XanbooException
     */
    public XanbooResultSet getNotificationActionList(Connection conn, long accountId, long userId, String gatewayGUID,
    String deviceGUID, String eventId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getNotificationActionList()]: ");
        }

        // setting IN params
        SQLParam[] args = new SQLParam[5+2];

        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID);
        args[3] = new SQLParam(deviceGUID);
        try {
            args[4] = new SQLParam((eventId == null ? null : new Integer(eventId)), Types.INTEGER);
        }catch(Exception eee) { args[4] = new SQLParam(null, Types.INTEGER); }

        try {
            return dao.callSP(conn, "XC_DEVICE_PKG.GETNOTIFICATIONACTIONLIST", args);
        }catch(XanbooException xe) {
            throw xe;
        }
        
    }
    
    
    
    /**
     * Adds a new action to be triggered for a device event instance.
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier for the event
     * @param eventId the device event id to update.
     * @param profileId an array of notification profile ids to be notified for this event.
     *
     * @throws XanbooException
     */
    public void newNotificationAction(Connection conn, long accountId, long userId, String gatewayGUID, String deviceGUID,
            String eventId, long[] profileId, int quietTime) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[newNotificationAction()]: ");
        }
        
        SQLParam[] args=new SQLParam[8+2];     // 6 SP parameter + 2 std parameters (errno, errmsg)
        
        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID);
        args[3] = new SQLParam(deviceGUID);
        try {
            args[4] = new SQLParam((eventId == null ? null : new Integer(eventId)), Types.INTEGER);
        }catch(Exception eee) { args[4] = new SQLParam(null, Types.INTEGER); }
        if (quietTime == -1 ) {
            args[5] = new SQLParam(null, Types.INTEGER);
        } else {
            args[5] = new SQLParam( new Integer( quietTime ), Types.INTEGER );
        }
        
        // OUT param --> action id just created.
        args[7] = new SQLParam(new Long(-1), Types.BIGINT, true);
        
        for(int i=0; i<profileId.length; i++) {
            args[6] = new SQLParam(new Long(profileId[i]), Types.BIGINT);
            
            try {
                dao.callSP(conn, "XC_DEVICE_PKG.NEWNOTIFICATIONACTION", args, false);
            }catch(XanbooException xe) {
                throw xe;
            }
        }   // end for loop
        
    }
    
    
    
    /**
     * Removes actions defined for a specific device event instance.
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier for the event
     * @param eventId the device event id to delete the actions for.
     * @param actionId an array list of action ids to remove.
     *
     * @throws XanbooException
     */
    public void deleteNotificationAction(Connection conn, long accountId, long userId, String gatewayGUID,
    String deviceGUID, String eventId, long[] actionId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteNotificationAction()]: ");
        }
        
        // setting IN params
        SQLParam[] args = new SQLParam[6+2];     // 6 SP parameter + 2 std parameters (errno, errmsg)
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID);
        args[3] = new SQLParam(deviceGUID);
        try {
            args[4] = new SQLParam((eventId == null ? null : new Integer(eventId)), Types.INTEGER);
        }catch(Exception eee) { args[4] = new SQLParam(null, Types.INTEGER); }
        
        for(int i=0; i<actionId.length; i++) {
            args[5] = new SQLParam(new Long(actionId[i]), Types.BIGINT);
            
            try {
                dao.callSP(conn, "XC_DEVICE_PKG.DELETENOTIFICATIONACTION", args, false);
            }catch(XanbooException xe) {
                throw xe;
            }
            if(actionId[i] == -1) break;   // if all deleted (-1), no need to process other array elements.
        }   // end for loop
    }
    
    
    public XanbooGateway getGatewayInfo(Connection conn, long accountId, String gatewayGUID)  throws XanbooException {
    	
    	long startMS = System.currentTimeMillis();
        if(logger.isDebugEnabled()) {
            logger.debug("[getGatewayInfo()]: ");
        }
        
        SQLParam[] args=new SQLParam[22+2];     // SP parameter + 2 std parameters (errno, errmsg)
        
        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
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
        args[13] = new SQLParam("", Types.VARCHAR, true);   // Alarm Manager ID
        args[14] = new SQLParam("", Types.VARCHAR, true);   // LiveVideoOnAlarmToken
        args[15] = new SQLParam("", Types.VARCHAR, true);   // LiveVideoOnAlarmPolicy
        args[16] = new SQLParam("", Types.VARCHAR, true);   // 3G IP
        args[17] = new SQLParam("", Types.VARCHAR, true);   // 3G&BB status
        args[18] = new SQLParam(new Long(-1), Types.BIGINT, true);    //account id
        args[19] = new SQLParam("",Types.VARCHAR,true); //pai_Server_url
        args[20] = new SQLParam("",Types.VARCHAR,true); //pai_token
        args[21] = new SQLParam("",Types.VARCHAR,true); //log level
        
        XanbooGateway gwy = new XanbooGateway(gatewayGUID, accountId);
        
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
            
            long endMS = System.currentTimeMillis();
            
            long trace = endMS-startMS;
            
            if(trace >GlobalNames.TRACE_TIME || logger.isDebugEnabled() || (gwy!=null && gwy.getGatewayGUID() != null && GlobalNames.GoldenGGUIDs.contains(gwy.getGatewayGUID()))  ){
            	
            	logger.info("ALERT : getGatewayInfo : "+ gwy.getGatewayGUID() +" : " + trace);
            }
            
            return gwy;
        }catch(XanbooException xe) {
            throw xe;
        }
/*        }catch(Exception e) {
            if (logger.isDebugEnabled()) {
                e.printStackTrace();
            }
            gwy.setInbound(0);
            return gwy;
            //Shouldn't we really throw an exception ?
        }*/
        
    }
    
    /**
     * Deletes a device
     *
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier
     *
     * @return a String which contains the catalog id for the device to be deleted
     * @throws XanbooException
     */
    public String deleteDevice(Connection conn, long accountId, long userId, String gatewayGUID,
    String deviceGUID) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteDevice()]: ");
        }
        
        SQLParam[] args=new SQLParam[5+2];     // SP parameters + 2 std parameters (errno, errmsg)
        
        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID, Types.VARCHAR);
        args[3] = new SQLParam(deviceGUID, Types.VARCHAR);
        args[4] = new SQLParam("", Types.VARCHAR, true);   // returned catalog id
        
        try {
            dao.callSP(conn, "XC_DEVICE_PKG.DELETEDEVICE", args, false);
            
            String catId = (String) args[4].getParam();
            if(catId!=null && catId.length()>14) catId=catId.substring(1);
            return catId;
        }catch(XanbooException xe) {
            throw xe;
        }
        
    }

    
    public String newDevice(Connection conn, XanbooPrincipal xCaller, String gatewayGUID, String catalogId, String label ) throws XanbooException {
        
        //addIPCamera( conn, xCaller, gatewayGUID, type, ip, port, un, pw );        
        if (logger.isDebugEnabled()) {
            logger.debug("[newDevice()]:");
        }
        
        try{
            SQLParam[] args=new SQLParam[9+2];     //SP parameters + 2 std parameters (errno, errmsg)
            
            String newDeviceGUID = XanbooUtil.generateGUID( "IP", 10 );     //generate 10-digit dguid
            
            // setting IN params
            args[0] = new SQLParam( new Long(xCaller.getAccountId()), Types.BIGINT ); //account ID
            args[1] = new SQLParam( new Long(xCaller.getUserId()), Types.BIGINT );    //user ID
            args[2] = new SQLParam( gatewayGUID );                                    //gateway guid
            args[3] = new SQLParam( null );                                           //gateway password
            args[4] = new SQLParam( newDeviceGUID );                                  //device guid
            args[5] = new SQLParam( null );                                           //new device password
            args[6] = new SQLParam( catalogId );                                      //catalog id
            args[7] = new SQLParam( label );                                          //device label
            args[8] = new SQLParam( null );                                           //device list - null
            
            //Call SP
            dao.callSP(conn, "XC_DEVICEGROUP_PKG.NEWDEVICE", args, false);
            
            //Return guid of created group
            //return args[9].getParam().toString(); // we no longer generate guid in the SP
            return newDeviceGUID;
            
            
        }catch(XanbooException xe) {
            if (logger.isDebugEnabled()) {
                logger.debug( "[newDevice()]:" + xe.getErrorMessage());
            }
            throw xe;
        }catch(Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("[newDevice()]:" + e.getMessage(), e);
            } else {
                logger.debug("[newDevice()]:" + e.getMessage() );
            }
            throw new XanbooException(10030);  //Exception while executing DAO method;
        }
        
    }    

    
    XanbooResultSet getCameraURL( Connection conn ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getCameraURL()]:");
        }
        
        XanbooResultSet results = null;
        SQLParam[] args=new SQLParam[0+2];     // SP parameters + 2 std parameters (errno, errmsg)
        
        results = (XanbooResultSet)dao.callSP(conn, "XC_DEVICE_PKG.GETCAMERAURL", args);
        
        return results;
        
    }
    
    /* Gets a subscription record */
    XanbooResultSet getSubscription(Connection conn, XanbooPrincipal xCaller, String subsId, String hwId) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[getSubscription()]:");
        }

        XanbooResultSet results = null;
        SQLParam[] args=new SQLParam[4+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // set IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam( new Long(xCaller.getAccountId()), Types.BIGINT );
        args[2] = new SQLParam( subsId );
        args[3] = new SQLParam( hwId );

        results = (XanbooResultSet) dao.callSP(conn, "XC_SYSADMIN_PKG.GETSUBSCRIPTION", args);

        return results;

    }
   
    /**
     * Returns Domain Ref by domainId 
     * @param conn The database connection to use for this transaction   
     * @param xCaller XanbooPrincipal object with which to authenticate
     * @param domainId The domain id of the domain ref
     * @return A XanbooResultSet of system parameters.
     * @throws XanbooException
     */    
    public XanbooResultSet getDomainRefByDomainId(Connection conn, XanbooPrincipal xCaller, String domainId) throws XanbooException {
        if(logger.isDebugEnabled()){
            logger.debug("[getDomainRefByDomainId]");
        }

        SQLParam[] args=new SQLParam[1+2];     // SP parameters + 2 std parameters (errno, errmsg)      
        // set IN params
        args[0] = new SQLParam( domainId,  Types.VARCHAR);
        
        XanbooResultSet results = (XanbooResultSet) dao.callSP(conn, "XC_ADMIN_PKG.GETDOMAINREFBYDOMAINID", args);
        
        return results;
    }     
    
    /**     *
     * @param languageId Language Id to query.
     * @param modelId Model Id to query.
     * @return A XanbooResultSet of the device model ref table
     * @throws XanbooException
     */
    public XanbooResultSet getDeviceModel(Connection conn, String languageId, String modelId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getDeviceModel()]:");
        }

        XanbooResultSet results = null;
        SQLParam[] args=new SQLParam[2+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // set IN params
        args[0] = new SQLParam( languageId );
        args[1] = new SQLParam( modelId );
       

        results = (XanbooResultSet) dao.callSP(conn, "XC_DEVICE_PKG.GETDEVICEMODELREF", args);

        return results;
    }
}
