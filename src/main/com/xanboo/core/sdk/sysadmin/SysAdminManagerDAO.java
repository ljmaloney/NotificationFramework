/*
 * $Source:  $
 * $Id:  $
 *
 * Copyright 2013 AT&T Digital Life
 *
 */

package com.xanboo.core.sdk.sysadmin;

import java.sql.Connection;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.xanboo.core.model.DomainFeatures;
import com.xanboo.core.model.XanbooGateway;
import com.xanboo.core.model.device.XanbooDevice;
import com.xanboo.core.sdk.account.XanbooAccount;
import com.xanboo.core.sdk.account.XanbooNotificationProfile;
import com.xanboo.core.sdk.account.XanbooSubscription;
import com.xanboo.core.sdk.services.model.ServiceSubscription;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooAdminPrincipal;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.BaseDAO;
import com.xanboo.core.util.BaseHandlerDAO;
import com.xanboo.core.util.DAOFactory;
import com.xanboo.core.util.GlobalNames;
import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;
import com.xanboo.core.util.SQLParam;
import com.xanboo.core.util.XanbooException;
import com.xanboo.core.util.XanbooUtil;

/**
 * This class is the DAO class to wrap all generic database calls for SDK SysAdminManager methods.
 * Database specific methods are handled by implementation classes. These implementation
 * classes extend the BaseDAO class to actually perform the database operations. An instance of
 * an implementation class is created during contruction of this class.
 */
public class SysAdminManagerDAO extends BaseHandlerDAO {
    
    private BaseDAO dao;
    private Logger logger;
    
    XanbooAdminPrincipal xap;
   
    /**
     * Default constructor. Takes no arguments
     *
     * @throws XanbooException
     */
    public SysAdminManagerDAO() throws XanbooException {
        
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
              logger.error("[SysAdminManagerDAO()]: " + ne.getMessage(), ne);
            }else {
              logger.error("[SysAdminManagerDAO()]: " + ne.getMessage());
            }               
            throw new XanbooException(20014, "[SysAdminManagerDAO()]: " + ne.getMessage());
        }
    }
    
    /**
     * Returns a paged list of user records
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param acctId to get the user list for. If -1, all user records will be returned.
     * @param startRow the starting row number for the user list
     * @param numRow the number of user records to be returned.
     *
     * @return a XanbooResultSet which contains a HashMap list of account users
     * @throws XanbooException
     *
     * @see XanbooPrincipal
     * @see XanbooResultSet
     */
    public XanbooResultSet getUserList(Connection conn, XanbooAdminPrincipal xCaller, long acctId, int startRow, int numRows) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[getUserList()]:");
        }
        
        XanbooResultSet results = null;
        SQLParam[] args=new SQLParam[3+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        if(acctId==-1) {
            args[1] = new SQLParam(null, Types.BIGINT);
        } else {
            args[1] = new SQLParam(new Long(acctId), Types.BIGINT);
        }
        
        args[2] = new SQLParam(new Integer(-1), Types.INTEGER, true);
        
        results = (XanbooResultSet)dao.callSP(conn, "XC_SYSADMIN_PKG.GETUSERLIST", args, startRow, numRows);
        results.setSize(((Integer) args[2].getParam()).intValue());
        
        return results;
        
    }
    

    public String updateAccount(Connection conn, XanbooAdminPrincipal xCaller, XanbooAccount xAccount) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateAccount()]:");
        }
        
        SQLParam[] args=new SQLParam[10+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam(new Long(xAccount.getAccountId()), Types.BIGINT);
        args[2] = new SQLParam(xAccount.getType()==XanbooAccount.TYPE_UNCHANGED ? null : xAccount.getType());
        args[3] = new SQLParam((xAccount.getStatus()==XanbooAccount.STATUS_UNCHANGED ? null : new Integer(xAccount.getStatus())), Types.INTEGER);
        args[4] = new SQLParam((xAccount.getToken()==null || xAccount.getToken().length()==0) ? null : xAccount.getToken());
        args[5] = new SQLParam((xAccount.getExtAccountId()==null || xAccount.getExtAccountId().length()==0) ? null : xAccount.getExtAccountId());
        args[6] = new SQLParam(xAccount.getFifoPurgingFlag()==-1 ? null : xAccount.getFifoPurgingFlag());
        args[7] = new SQLParam(xAccount.getSelfInstallProvisioningFlag()==XanbooAccount.PROVISIONING_UNCHANGED ? null : xAccount.getSelfInstallProvisioningFlag());
        args[8] = new SQLParam(null,Types.NULL);
        args[9] = new SQLParam("", Types.VARCHAR, true);   // for returning domainid

        dao.callSP(conn, "XC_SYSADMIN_PKG.UPDATEACCOUNT", args, false);

        return (args[9].getParam().toString());   // return domainId for the account updated
    }

    
    
    /**
     * Updates status for a specific account device
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier to update.
     * @param status The new status for the appliance - must be one of DeviceManagerEJB.DEVICE_STATUS_ACTIVE or DeviceManagerEJB.DEVICE_STATUS_INACTIVE
     *
     * @throws XanbooException
     */
    public void updateDeviceStatus(Connection conn, XanbooAdminPrincipal xCaller, long accountId, String gatewayGUID,
    String deviceGUID, int status) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[updateDeviceStatus()]: ");
        }
        
        SQLParam[] args=new SQLParam[5+2];     // 5 SP parameter + 2 std parameters (errno, errmsg)
        
        // setting IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID, Types.VARCHAR);
        args[3] = new SQLParam(deviceGUID, Types.VARCHAR);
        args[4] = new SQLParam(new Integer(status), Types.INTEGER);
        
        dao.callSP(conn, "XC_SYSADMIN_PKG.UPDATEDEVICESTATUS", args, false);
        
    }


    public void updateDevice(Connection conn, XanbooAdminPrincipal xCaller, long accountId, XanbooDevice xanbooDevice) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[updateDevice()]: ");
        }

        SQLParam[] args=new SQLParam[7+2];     // 5 SP parameter + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[2] = new SQLParam(xanbooDevice.getGatewayGUID(), Types.VARCHAR);
        args[3] = new SQLParam(xanbooDevice.getDeviceGUID(), Types.VARCHAR);
        args[4] = new SQLParam(new Integer(xanbooDevice.getStatus()), Types.INTEGER);
        args[5] = new SQLParam(xanbooDevice.getInstallerId(), Types.VARCHAR);
        args[6] = new SQLParam(xanbooDevice.getSourceId(), Types.VARCHAR);

        dao.callSP(conn, "XC_SYSADMIN_PKG.UPDATEDEVICE", args, false);
       

    }
    
    /**
     * Returns global device class reference list in the system
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param lang the language in which the class list will be returned.
     *
     * @return a XanbooResultSet which contains a HashMap list of device classes
     * @throws XanbooException
     *
     * @see XanbooPrincipal
     * @see XanbooResultSet
     */
    public XanbooResultSet getDeviceClassList(Connection conn, XanbooAdminPrincipal xCaller, String lang) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[getDeviceClassList()]:");
        }
        
        SQLParam[] args=new SQLParam[2+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam( xCaller == null ? "" : xCaller.getUsername(), Types.VARCHAR );
        args[1] = new SQLParam( lang, Types.VARCHAR);
        
        return dao.callSP(conn, "XC_SYSADMIN_PKG.GETDEVICECLASSLIST", args);
        
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
    
    
    
    public XanbooNotificationProfile[] getNotificationProfile(Connection conn, long accountId, long userId, boolean getEmergencyContacts) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getNotificationProfile()]:");
        }

        SQLParam[] args=new SQLParam[2+2];     // 2 SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);

        XanbooResultSet xrs = dao.callSP(conn, "XC_ACCOUNT_PKG.GETNOTIFICATIONPROFILE", args);

        if(xrs==null || xrs.size()==0) return null;

        XanbooNotificationProfile[] xnp = new XanbooNotificationProfile[xrs.size()];

        //if(logger.isDebugEnabled()) logger.debug("[getNotificationProfile()]: RS: " + xrs.toXML());

        int cnt=0;
        for(int i=0; i<xrs.size(); i++) {
            xnp[i] = null;

            HashMap np = (HashMap) xrs.get(i);

            long npId = Long.parseLong((String) np.get("PROFILE_ID"));
            String isEM = String.valueOf(np.get("IS_EMERGENCY_CONTACT"));

            boolean npIsEmergencyContact = (isEM!=null && isEM.length()>0 && Integer.parseInt( isEM )==1) ? true : false;
            if(!getEmergencyContacts && npIsEmergencyContact) continue; //if flag is false, dont return emergency contacts

            xnp[cnt] = new XanbooNotificationProfile(npId, npIsEmergencyContact);
            xnp[cnt].setName((String) np.get("DESCRIPTION"));

            String ptype = (String) np.get("PROFILETYPE_ID");
            if(ptype!=null && ptype.length()>0) {
                xnp[cnt].setType( Integer.parseInt(ptype) );
                xnp[cnt].setAddress((String) np.get("PROFILE_ADDRESS"));
            }

            ptype = (String) np.get("PROFILETYPE_ID2");
            if(ptype!=null && ptype.length()>0) {
                xnp[cnt].setType2( Integer.parseInt(ptype) );
                xnp[cnt].setAddress2((String) np.get("PROFILE_ADDRESS2"));
            }

            ptype = (String) np.get("PROFILETYPE_ID3");
            if(ptype!=null && ptype.length()>0) {
                xnp[cnt].setType3( Integer.parseInt(ptype) );
                xnp[cnt].setAddress3((String) np.get("PROFILE_ADDRESS3"));
            }

            xnp[cnt].setGguid((String) np.get("GATEWAY_GUID"));
            
            ptype = (String) np.get("CONTACT_ORDER");
            if(ptype!=null && ptype.length()>0) {
                xnp[cnt].setOrder( Integer.parseInt(ptype) );
            }

            xnp[cnt].setCodeword( (String) np.get("CODEWORD") );

            //contact order per action plan
            for(int j=1; j<10; j++) {
                ptype = (String) np.get("CONTACT_ORDER_AP" + j);
                if(ptype!=null && ptype.length()>0) {
                    xnp[cnt].setActionPlanContactOrder(j, Short.parseShort(ptype) );
                }
            }
            
            //xnp[cnt].dump();
            cnt++;
        }

        //resize array if necessary

        if(cnt==xrs.size()) return xnp; // no need to resize

        //resize
        XanbooNotificationProfile[] xnp2 = new XanbooNotificationProfile[cnt];
        int ix=0;
        for(int i=0; i<cnt; i++) {
            if(xnp[i]==null) continue;
            xnp2[ix] = xnp[i];
            ix++;
        }
        return xnp2;

     }

    
    
    /**
     * Returns a paged list of devices of specified class in the system.
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param dClass device class id for the query. If null, all devices are returned. Valid class ids
     *        can be obtained via <code>getDeviceClassList</code> method.
     * @param startRow the starting row number for the account list
     * @param numRow the number of account records to be returned
     *
     * @return a XanbooResultSet which contains a HashMap list of devices
     * @throws XanbooException
     *
     * @see XanbooPrincipal
     * @see XanbooResultSet
     */
    public XanbooResultSet getDeviceListByClass(Connection conn, XanbooAdminPrincipal xCaller, String dClass, int startRow, int numRows) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[getDeviceListByClass()]:");
        }
        
        XanbooResultSet results = null;
        SQLParam[] args=new SQLParam[3+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam(dClass, Types.VARCHAR);
        args[2] = new SQLParam(new Integer(-1), Types.INTEGER, true);
        
        results = (XanbooResultSet)dao.callSP(conn, "XC_SYSADMIN_PKG.GETDEVICELIST", args, startRow, numRows);
        results.setSize(((Integer) args[2].getParam()).intValue());
        
        return results;
        
    }


    /**
     * Returns global device class reference list in the system
     * @param conn The database connection to use for this call
     * @param CTN ID of the gateway
     * @param imei gateway.
     * @param serialNo of gateway.
     * @return a XanbooResultSet which contains a HashMap list of device classes
     * @throws XanbooException
     *
     * @see XanbooPrincipal
     * @see XanbooResultSet
     */
    public XanbooResultSet getGateway(Connection conn, XanbooAdminPrincipal xCaller, String subsId, String imei, String serialNo, String gatewayGuid) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[getGateway()]:");
        }
        
        SQLParam[] args=new SQLParam[5+2];     // SP parameters + 2 std parameters (errno, errmsg) and total count for future use.
        // set IN params
        args[0] = new SQLParam( xCaller == null ? "" : xCaller.getUsername(), Types.VARCHAR );
        if(subsId ==null || (subsId != null && subsId.length() == 0 ) )
            args[1] = new SQLParam( subsId, Types.NULL);
        else
            args[1] = new SQLParam( subsId, Types.VARCHAR);
        
        if(imei ==null ||(imei != null && imei.length() == 0)  )
            args[2] = new SQLParam( imei, Types.NULL);
        else
            args[2] = new SQLParam( imei, Types.VARCHAR);
        
        if(serialNo ==null ||(serialNo != null && serialNo.length() == 0)  )
            args[3] = new SQLParam( serialNo, Types.NULL);
        else
            args[3] = new SQLParam( serialNo, Types.VARCHAR); 
        
        if(gatewayGuid ==null ||(gatewayGuid != null && gatewayGuid.length() == 0)  )
            args[4] = new SQLParam( gatewayGuid, Types.NULL);
        else
            args[4] = new SQLParam( gatewayGuid, Types.VARCHAR);         
        
        return dao.callSP(conn, "XC_SYSADMIN_PKG.getGateways", args);
        
    }        
    
       
    
    /**
     * Creates a broadcast message to all accounts in the system
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param message the broadcast message (max 512 characters)
     * @param lang the language id of the broadcast message
     *
     * @return the broadcast message id assigned
     * @throws XanbooException
     *
     * @see XanbooPrincipal
     */
    public long newBroadcastMessage(Connection conn, XanbooAdminPrincipal xCaller, String message, String lang, long[] acctId) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[newBroadcastMessage()]:");
        }
        
        SQLParam[] args=new SQLParam[5+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam(message, Types.VARCHAR);
        args[2] = new SQLParam(lang, Types.VARCHAR);
        
        // OUT params
        args[4] = new SQLParam(new Long(-1), Types.BIGINT, true);
        
        if(acctId==null || acctId.length < 1) {   // no specific account
            args[3] = new SQLParam(null, Types.BIGINT);
            dao.callSP(conn, "XC_SYSADMIN_PKG.NEWBROADCASTMESSAGE", args, false);
        } else {
            for(int i=0; i<acctId.length; i++) {
                args[3] = new SQLParam(new Long(acctId[i]), Types.BIGINT);
                dao.callSP(conn, "XC_SYSADMIN_PKG.NEWBROADCASTMESSAGE", args, false);
            }
        }
        
        return ((Long) args[4].getParam()).longValue();
        
    }
    
  
    /**
     * Deletes one or more broadcast messages from the system
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param messageId an array of message ids to be removed.
     *
     * @throws XanbooException
     *
     * @see XanbooPrincipal
     */
    public void deleteBroadcastMessage(Connection conn, XanbooAdminPrincipal xCaller, long[] messageId) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteBroadcastMessage()]:");
        }
        
        SQLParam[] args=new SQLParam[2+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        
        for(int i=0; i<messageId.length; i++) {
            args[1] = new SQLParam(new Long(messageId[i]), Types.BIGINT);
            dao.callSP(conn, "XC_SYSADMIN_PKG.DELETEBROADCASTMESSAGE", args, false);
            
        }
    }
    
    
    
    /**
     * Returns the list of managed objects that are enabled for data collection
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     *
     * @return a XanbooResultSet which contains a HashMap list of data collection entries.
     * @throws XanbooException
     *
     * @see XanbooPrincipal
     * @see XanbooResultSet
     */
    public XanbooResultSet getMObjectHistoryTable(Connection conn, XanbooAdminPrincipal xCaller ) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[getMObjectHistoryTable()]:");
        }
        
        SQLParam[] args=new SQLParam[1+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        
        return dao.callSP(conn, "XC_SYSADMIN_PKG.GETMOBJECTHISTORYTABLE", args);
        
    }
    
    
    /**
     * Enables value history recording for a list of managed objects.
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param catalogId the catalog id for the managed objects
     * @param mobjectId array of managed object ids
     *
     * @throws XanbooException
     *
     * @see XanbooPrincipal
     */
    public void enableMObjectHistory(Connection conn, XanbooAdminPrincipal xCaller, String catalogId, String[] mobjectId) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[enableMObjectHistory()]:");
        }
        
        SQLParam[] args=new SQLParam[3+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam(catalogId, Types.VARCHAR);
        
        for(int i=0; i<mobjectId.length; i++) {
            args[2] = new SQLParam(mobjectId[i], Types.VARCHAR);
            dao.callSP(conn, "XC_SYSADMIN_PKG.ENABLEMOBJECTHISTORY", args, false);
        }
        
    }
    
    
    /**
     * Disables value history recording for a list of managed objects.
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param catalogId the catalog id for the managed objects
     * @param mobjectId array of managed object ids
     *
     * @throws XanbooException
     *
     * @see XanbooPrincipal
     */
    public void disableMObjectHistory(Connection conn, XanbooAdminPrincipal xCaller, String catalogId, String[] mobjectId) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[disableMObjectHistory()]:");
        }
        
        SQLParam[] args=new SQLParam[3+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam(catalogId, Types.VARCHAR);
        
        for(int i=0; i<mobjectId.length; i++) {
            args[2] = new SQLParam(mobjectId[i], Types.VARCHAR);
            dao.callSP(conn, "XC_SYSADMIN_PKG.DISABLEMOBJECTHISTORY", args, false);
        }
        
    }
    
    
    /**
     * Deletes managed object history values for a specified date interval. This method is
     * typically used for cleaning up recorded values of managed objects enabled for data collection.
     *
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param catalogId the catalog id for the managed object. If null, all managed object history
     *        values will be returned regardless of the managed object id specified.
     * @param mobjectId managed object id to retrieve history values for. If null, all managed object
     *        history values for the specified catalog id will be returned.
     * @param fromDate value history interval start date
     * @param toDate value history interval end date
     *
     * @throws XanbooException
     */
    public void deleteMObjectHistory(Connection conn, XanbooAdminPrincipal xCaller,
    String catalogId, String mobjectId, java.util.Date fromDate, java.util.Date toDate) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteMObjectHistory()]: ");
        }
        
        // setting IN params
        SQLParam[] args=null;
        args = new SQLParam[5+2];
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam(catalogId, Types.VARCHAR);
        args[2] = new SQLParam(mobjectId, Types.VARCHAR);
        args[3] = new SQLParam(fromDate, Types.DATE);
        args[4] = new SQLParam(toDate, Types.DATE);
        
        dao.callSP(conn, "XC_SYSADMIN_PKG.DELETEMOBJECTHISTORY", args, false);
    }
    
    
    /**
     * Returns managed object history values for a specified date interval. This method is
     * typically used for retrieving recorded values of managed objects enabled for data collection.
     *
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param catalogId the catalog id for the managed object. If null, all managed object history
     *        values will be returned regardless of the managed object id specified.
     * @param mobjectId managed object id to retrieve history values for. If null, all managed object
     *        history values for the specified catalog id will be returned.
     * @param fromDate value history interval start date
     * @param toDate value history interval end date
     *
     * @return an XanbooResultSet which contains a HashMap list of devices
     * @throws XanbooException
     */
    public XanbooResultSet getMObjectHistory(Connection conn, XanbooAdminPrincipal xCaller,
    String catalogId, String mobjectId, java.util.Date fromDate, java.util.Date toDate) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getMObjectHistory()]: ");
        }
        
        // setting IN params
        SQLParam[] args=null;
        args = new SQLParam[5+2];
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam(catalogId, Types.VARCHAR);
        args[2] = new SQLParam(mobjectId, Types.VARCHAR);
        args[3] = new SQLParam(fromDate, Types.DATE);
        args[4] = new SQLParam(toDate, Types.DATE);
       
        return dao.callSP(conn, "XC_SYSADMIN_PKG.GETMOBJECTHISTORY", args);
    }
    
    
    /**
     *  Retuns either a list of all system parameters or one specific parameter.
     *  @param conn The database connection to use for this call
     *  @param param The parameter to retrieve, or null to return all system parameters
     *  @return A XanbooResultSet of system parameters.
     */
    public XanbooResultSet getSystemParam( Connection conn, XanbooAdminPrincipal xap, String param ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getSystemParam()]: ");
        }
        
        SQLParam[] args=new SQLParam[2+2];     // 2 SP parameters + 2 std parameters (errno, errmsg)
        
        // setting IN params
        args[0] = new SQLParam(xap == null ? null : xap.getUsername());
        args[1] = new SQLParam(param, Types.VARCHAR);
        
        return dao.callSP(conn, "XC_ADMIN_PKG.getSystemParam", args);
        
    }
    
    /**
     * Returns a XanbooPrincipal (without authenticating)
     * @param conn The database connection to use for this call
     * @param domainId user domain identifier
     * @param extUserId The external user Id of the user to authenticate
     *
     * @return a XanbooPrincipal object
     * @throws XanbooException
     */
    public XanbooPrincipal getXanbooPrincipal(Connection conn, XanbooAdminPrincipal xCaller, String domainId, String extUserId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getXanbooPrincipal()]: ");
        }
        
        SQLParam[] args=new SQLParam[11+2];     // SP parameters + 2 std parameters (errno, errmsg)
        
        // setting IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam(domainId);
        args[2] = new SQLParam(extUserId);
        
        // OUT parameters
        args[3] = new SQLParam(new Long(-1), Types.BIGINT, true);     // for returning user id
        args[4] = new SQLParam(new Long(-1), Types.BIGINT, true);     // for returning account id
        args[5] = new SQLParam("", Types.VARCHAR, true);                // for returning username
        args[6] = new SQLParam(new Integer(-1), Types.INTEGER, true);  // for returning account status id
        args[7] = new SQLParam(new Integer(-1), Types.INTEGER, true);  // for returning user master flag
        args[8] = new SQLParam("", Types.VARCHAR, true);                //for returning language
        args[9] = new SQLParam("", Types.VARCHAR, true);                //for returning tz
        args[10] = new SQLParam("", Types.VARCHAR, true);                //for returning domain id
        
        
        dao.callSP(conn, "XC_SYSADMIN_PKG.GETXANBOOPRINCIPAL", args, false);
        
        // TO DO: what to do in case account is not active !!!
        XanbooPrincipal xp = new XanbooPrincipal(args[10].getParam().toString(), args[5].getParam().toString());
        xp.setUserId(((Long) args[3].getParam()).longValue());
        xp.setAccountId(((Long) args[4].getParam()).longValue());
        xp.setMaster(((Integer) args[7].getParam()).intValue()==1 ? true : false);
        xp.setLanguage(args[8].getParam().toString());
        xp.setTimezone(args[9].getParam().toString());
        
        return xp;
    }
    
    /**
     * Deletes a device
     *
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param accountId The account of the gateway to be deleted.
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier
     *
     * @return a String which contains the catalog id for the device to be deleted
     * @throws XanbooException
     */
    public String deleteDevice(Connection conn, XanbooAdminPrincipal xCaller, long dAccountId, String gatewayGUID, String deviceGUID) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteDevice()]: ");
        }
        
        SQLParam[] args=new SQLParam[5+2];     // SP parameters + 2 std parameters (errno, errmsg)
        
        // setting IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam(new Long(dAccountId), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID, Types.VARCHAR);
        args[3] = new SQLParam(deviceGUID, Types.VARCHAR);
        args[4] = new SQLParam("", Types.VARCHAR, true);   // returned catalog id
        
        dao.callSP(conn, "XC_SYSADMIN_PKG.DELETEDEVICE", args, false);
        
        String catId = (String) args[4].getParam();
        if(catId!=null && catId.length()>14) catId=catId.substring(1);
        return catId;        
    }
    
    /**
     * Completey removes an account from the xanboo system
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param dAccountId The ID of the account to delete
     *
     * @throws XanbooException
     *
     * @see XanbooPrincipal
     */
    public void deleteAccount(Connection conn, XanbooAdminPrincipal xCaller, long dAccountId) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteAccount()]:");
        }
        
        SQLParam[] args=new SQLParam[2+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam(new Long(dAccountId), Types.BIGINT);
        
        dao.callSP(conn, "XC_SYSADMIN_PKG.DELETEACCOUNT", args, false);
        
    }
    
    /**
     *  Retuns either a list of all loaded device descriptors, or managed objects of a particular descriptor
     *  @param conn The database connection to use for this call
     *  @param accountId caller account id
     *  @param userId  caller user id.
     *  @param catalogId the catalog id for device descriptor. If null, list of all loaded descriptors will
     *      be returned.
     *  @return A XanbooResultSet of system parameters.
     */
    public XanbooResultSet getDeviceDescriptor( Connection conn, String catalogId ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getDeviceDescriptor()]: ");
        }
        
        SQLParam[] args=new SQLParam[1+2];     // 2 SP parameters + 2 std parameters (errno, errmsg)
        
        // setting IN params
        args[0] = new SQLParam(catalogId, Types.VARCHAR);
        
        return dao.callSP(conn, "XC_SYSADMIN_PKG.getDeviceDescriptor", args);
        
    }
    
    /**
     * Returns event log entries for a specific gateway or device instance
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param dAccountId  The account id of the eventlogs to be retrieved.
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier If null, all event log entries for the
     *                   whole gateway are returned.
     * @param eventId the event identifier to retrieve the log entries for. If null,
     *                all events log records for the specified gateway/device are returned.
     *
     * @return a XanbooResultSet which contains a HashMap list of devices
     * @throws XanbooException
     */
    public XanbooResultSet getDeviceEventLog(Connection conn, XanbooAdminPrincipal xCaller, long dAccountId, String gatewayGUID, String deviceGUID, String eventId) throws XanbooException {
        
        if (logger.isDebugEnabled()) {
            logger.debug("[getDeviceEventLog()]: ");
        }
        
        SQLParam[] args=new SQLParam[5+2];     // 2 SP parameters + 2 std parameters (errno, errmsg)
        
        // setting IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam(new Long(dAccountId), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID, Types.VARCHAR);
        args[3] = new SQLParam(deviceGUID, Types.VARCHAR);
        try {
            args[4] = new SQLParam((eventId == null ? null : new Integer(eventId)), Types.INTEGER);
        }catch(Exception eee) { args[4] = new SQLParam(null, Types.INTEGER); }
        
        
        return dao.callSP(conn, "XC_SYSADMIN_PKG.GETDEVICEEVENTLOG", args);
        
    }
    
    
    /**
     * Clears event log entries for a specific gateway or device instance
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param dAccountId  The account id of the eventlogs to be deleted.
     * @param gatewayGUID the parent gateway GUID of the device
     * @param deviceGUID the device identifier If null, all event log entries for the
     *                   whole gateway are cleared.
     * @param eventId the event identifier to retrieve the log entries for. If null,
     *                all events log records for the specified gateway/device are cleared.
     *
     * @throws XanbooException
     */
    public void clearDeviceEventLog(Connection conn, XanbooAdminPrincipal xCaller, long dAccountId, String gatewayGUID, String deviceGUID, String eventId) throws XanbooException {
        
        if(logger.isDebugEnabled()) {
            logger.debug("[clearDeviceEventLog()]: ");
        }
        
        // setting IN params
        SQLParam[] args=null;
        args = new SQLParam[5+2];
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam(new Long(dAccountId), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID, Types.VARCHAR);
        args[3] = new SQLParam(deviceGUID, Types.VARCHAR);
        try {
            args[4] = new SQLParam((eventId == null ? null : new Integer(eventId)), Types.INTEGER);
        }catch(Exception eee) { args[4] = new SQLParam(null, Types.INTEGER); }
        
        dao.callSP(conn, "XC_SYSADMIN_PKG.CLEARDEVICEEVENTLOG", args, false);
        
    }
    
    /**
     * Returns a paged list of accounts in the system.
     * @param conn The database connection to use for this call
     * @param accountId caller account id
     * @param userId  caller user id.
     * @param acctId  account id of the account.
     * @param startRow the starting row number for the account list
     * @param numRow the number of account records to be returned
     *
     * @return a XanbooResultSet which contains a HashMap list of Xanboo accounts
     * @throws XanbooException
     *
     * @see XanbooPrincipal
     * @see XanbooResultSet
     */
    public XanbooResultSet getAccount(Connection conn, XanbooAdminPrincipal xCaller, long acctId, String username, int startRow, int numRows) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[getAccount()]:");
        }
        
        XanbooResultSet results = null;
        SQLParam[] args=new SQLParam[6+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam((acctId == -1 ? null : new Long(acctId)), Types.BIGINT );
        //DLDP 2795 Serach by username or user email address or contact phone        
        if(username != null && username.startsWith("@")) {        	
        	args[2] = new SQLParam(null);
        	//Remove @ to send as email address parameter
	        args[3] = new SQLParam(username.substring(1, username.length()));
	        args[4] = new SQLParam(null);
        } else if(username != null && username.startsWith("#")) {        	
        	args[2] = new SQLParam(null);
        	args[3] = new SQLParam(null);
        	//Remove # to send as contact phone parameter
	        args[4] = new SQLParam(username.substring(1, username.length()));        
        } else {
	        args[2] = new SQLParam(username);
	        args[3] = new SQLParam(null);
	        args[4] = new SQLParam(null);
        }
        args[5] = new SQLParam(new Integer(-1), Types.INTEGER, true);
        
        results = (XanbooResultSet)dao.callSP(conn, "XC_SYSADMIN_PKG.GETACCOUNT", args, startRow, numRows);
        results.setSize(((Integer) args[5].getParam()).intValue());
        
        return results;
        
    }
    
        
    public XanbooAdminPrincipal authenticateAdmin(Connection conn, String adminUser, String password) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[authenticateAdmin()]: ");
        }

        SQLParam[] args=new SQLParam[5+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(adminUser);

        // OUT parameters
        args[1] = new SQLParam("", Types.VARCHAR, true);               // for returning admin pwd hash
        args[2] = new SQLParam(new Integer(-1), Types.INTEGER, true);  // for returning admin role
        args[3] = new SQLParam(new Integer(-1), Types.INTEGER, true);  // for returning admin level
        args[4] = new SQLParam("", Types.VARCHAR, true);  // for returning admin domain
        
        dao.callSP(conn, "XC_SYSADMIN_PKG.AUTHENTICATEADMIN", args, false);

        //parse returned hash to determine hash algorithm and salt
        //hash format: <hash-id>:<salt>:<hash>  or just <hash>
        String currentPasswordHash = args[1].getParam().toString();
        String hashVal  = null;
        String hashSalt = null;
        int hashAlg=XanbooUtil.SUPPORTED_ALGORITHM_MD5;  //default MD5

        String hashedPassword;
        if(currentPasswordHash.length()==34) {   //old MD5-only hashing with no salting, for backward compatibility
            hashedPassword = XanbooUtil.hashString(password, hashAlg, null);
        }else {     //parse algorithm and salt, then hash the incoming password for comparison
            hashAlg  = Integer.parseInt(currentPasswordHash.substring(0,1));
            hashSalt = currentPasswordHash.substring(1,17);
            hashedPassword = XanbooUtil.hashString(password, hashAlg, hashSalt);    //use the same algorithm and salt in the stored hash
        }

       
        
        int role = ((Integer)args[2].getParam()).intValue();
        int level = ((Integer)args[3].getParam()).intValue();
        String domain = args[4].getParam().toString();
        
        //create a XanbooAdminPrincipal object with the username, accountId, userId
        XanbooAdminPrincipal xap = new XanbooAdminPrincipal(adminUser, role, level);
        xap.setDomain( domain );
        this.xap = xap;
        //check password match, throw exception if not
        if(!currentPasswordHash.equals(hashedPassword)) {
            throw new XanbooException(27005, "Login failure. Invalid admin username or password");
        }
        return xap;

    }
    
    
    public XanbooAdminPrincipal authenticateAdmin(Connection conn, String adminUser) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[authenticateAdmin()]: ");
        }

        SQLParam[] args=new SQLParam[5+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(adminUser);

        // OUT parameters
        args[1] = new SQLParam("", Types.VARCHAR, true);               // for returning admin pwd hash
        args[2] = new SQLParam(new Integer(-1), Types.INTEGER, true);  // for returning admin role
        args[3] = new SQLParam(new Integer(-1), Types.INTEGER, true);  // for returning admin level
        args[4] = new SQLParam("", Types.VARCHAR, true);  // for returning admin domain
        
        dao.callSP(conn, "XC_SYSADMIN_PKG.AUTHENTICATEADMIN", args, false);

        //parse returned hash to determine hash algorithm and salt
        //hash format: <hash-id>:<salt>:<hash>  or just <hash>
        String currentPasswordHash = args[1].getParam().toString();
        String hashVal  = null;
        String hashSalt = null;
        int hashAlg=XanbooUtil.SUPPORTED_ALGORITHM_MD5;  //default MD5
        /*
        String hashedPassword;
        if(currentPasswordHash.length()==34) {   //old MD5-only hashing with no salting, for backward compatibility
            hashedPassword = XanbooUtil.hashString(password, hashAlg, null);
        }else {     //parse algorithm and salt, then hash the incoming password for comparison
            hashAlg  = Integer.parseInt(currentPasswordHash.substring(0,1));
            hashSalt = currentPasswordHash.substring(1,17);
            hashedPassword = XanbooUtil.hashString(password, hashAlg, hashSalt);    //use the same algorithm and salt in the stored hash
        }
         * 
         */

        //check password match, throw exception if not
        //if(!currentPasswordHash.equals(hashedPassword)) {
        //    throw new XanbooException(27005, "Login failure. Invalid admin username or password");
        //}
        
        int role = ((Integer)args[2].getParam()).intValue();
        int level = ((Integer)args[3].getParam()).intValue();
        String domain = args[4].getParam().toString();
        
        //create a XanbooAdminPrincipal object with the username, accountId, userId
        XanbooAdminPrincipal xap = new XanbooAdminPrincipal(adminUser, role, level);
        xap.setDomain( domain );
        return xap;

    }
    
    
    public void updateAdmin(Connection conn, XanbooAdminPrincipal xCaller, String adminId, String password) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[updateAdmin()]: ");
        }
        
        updateAdmin(conn, xCaller, adminId, password, -1, -1, null);
        
    } 
    
    public void updateAdmin(Connection conn, XanbooAdminPrincipal xCaller, String adminId, String password, int roleId, int adminLevel, String domainId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[updateAdmin()]: ");
        }
        
        // setting IN params
        SQLParam[] args=null;
        args = new SQLParam[6+2];

        args[0] = new SQLParam(xCaller.getUsername());
        args[1] = new SQLParam(adminId, Types.VARCHAR);
        if(password == null || (password != null && password.length() == 0)) {
            args[2] = new SQLParam(password, Types.NULL);
        } else {
        	args[2] = new SQLParam(XanbooUtil.hashString(password, GlobalNames.APP_HASHING_ALGORITHM, "-1"), Types.VARCHAR);
        }        
        if(roleId == -1) {
            args[3] = new SQLParam(null, Types.BIGINT);
        } else {
            args[3] = new SQLParam(new Long(roleId), Types.BIGINT);
        }
        if(adminLevel == -1) {
            args[4] = new SQLParam(null, Types.BIGINT);
        } else {
            args[4] = new SQLParam(new Long(adminLevel), Types.BIGINT);
        }  
        if(domainId == null || (domainId != null && domainId.length() == 0)) {
            args[5] = new SQLParam(domainId, Types.NULL);
        } else {
        	args[5] = new SQLParam(domainId, Types.VARCHAR);
        }          
        
        dao.callSP(conn, "XC_SYSADMIN_PKG.UPDATEADMIN", args, false);        
    }     
    

    public XanbooResultSet getProfileTypeList(Connection conn) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[getProfileTypeList()]:");
        }
        
        XanbooResultSet results = null;
        SQLParam[] args=new SQLParam[1+2];     // SP parameters + 2 std parameters (errno, errmsg)
       
        args[0] = new SQLParam(new Integer(-1), Types.INTEGER, true);
        
        results = (XanbooResultSet)dao.callSP(conn, "XC_SYSADMIN_PKG.GETPROFILETYPELIST", args);
        results.setSize(((Integer) args[0].getParam()).intValue());
        
        return results;
        
    } 
 
    
    public XanbooResultSet getEventCatalog( Connection conn, String classId ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getEventCatalog()]:");
        }

        XanbooResultSet events = null;

        try {

            // setting IN params
            SQLParam[] args = new SQLParam[1+2];
            args[0] = new SQLParam(classId, Types.VARCHAR);

            events = dao.callSP(conn, "XC_SYSADMIN_PKG.getEventCatalog", args);
            
        }catch(XanbooException xe) {
            if (logger.isDebugEnabled()) {
                logger.debug( "[getEventCatalog()]:" + xe.getErrorMessage());
            }
            throw xe;
        }catch(Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("[getEventCatalog()]:" + e.getMessage(), e);
            } else {
                logger.debug("[getEventCatalog()]:" + e.getMessage() );
            }
            throw new XanbooException(10030);  //Exception while executing DAO method;
        }

        return events;

    }    

    /* Gets a list of domains */
    public XanbooResultSet getDomainList(Connection conn) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[getDomainList()]:");
        }
        
        XanbooResultSet results = null;
        SQLParam[] args=new SQLParam[0+2];     // SP parameters + 2 std parameters (errno, errmsg)
        
        results = (XanbooResultSet)dao.callSP(conn, "XC_SYSADMIN_PKG.GETDOMAINLIST", args);
        
        return results;
        
    }

    
    /* Gets a list of domain message templates */
    public XanbooResultSet getDomainTemplateList(Connection conn) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[getDomainTemplateList()]:");
        }
        
        XanbooResultSet results = null;
        SQLParam[] args=new SQLParam[0+2];     // SP parameters + 2 std parameters (errno, errmsg)
        
        results = (XanbooResultSet)dao.callSP(conn, "XC_SYSADMIN_PKG.GETDOMAINTEMPLATELIST", args);
        
        return results;
        
    }


    public XanbooGateway getGatewayInfo(Connection conn, long accountId, String gatewayGUID)  throws XanbooException {
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
            
            return gwy;
        }catch(XanbooException xe) {
            throw xe;
        }
    }


     /**
     * Creates an emergency contact profile for the subscription
     */
     private long newNotificationProfile(Connection conn, long accountId, XanbooNotificationProfile xnp) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[newNotificationProfile()]:");
        }

        SQLParam[] args=new SQLParam[24+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(null);
        args[2] = new SQLParam( xnp.getName() );
        args[3] = new SQLParam( new Integer(xnp.getType()), Types.INTEGER );
        args[4] = new SQLParam( xnp.getAddress() );

        if(xnp.hasValidProfileAddress2()) {
            args[5] = new SQLParam( new Integer(xnp.getType2()), Types.INTEGER );
            args[6] = new SQLParam( xnp.getAddress2() );
        } else {
            args[5] = new SQLParam( null );
            args[6] = new SQLParam( null );
        }

        if(xnp.hasValidProfileAddress3()) {
            args[7] = new SQLParam( new Integer(xnp.getType3()), Types.INTEGER );
            args[8] = new SQLParam( xnp.getAddress3() );
        } else {
            args[7] = new SQLParam( null );
            args[8] = new SQLParam( null );
        }

        //is emergency contact flag
        args[9] = new SQLParam(new Integer( xnp.isEmergencyContact() ? 1 : 0 ), Types.INTEGER );

        //gateway guid
        if(xnp.getGguid()!=null)
            args[10] = new SQLParam(xnp.getGguid());
        else
            args[10] = new SQLParam( null );

        //contact order
        if(xnp.getOrder()>0)
            args[11] = new SQLParam(new Integer( xnp.getOrder() ), Types.INTEGER );
        else
            args[11] = new SQLParam( null );

        //codeword
        args[12] = new SQLParam(xnp.getCodeword() );

        //contact order per action plan
        for(int j=1; j<10; j++) {
            args[12+j] = new SQLParam(new Integer( xnp.getActionPlanContactOrder(j) ), Types.INTEGER );
        }

        //max allowed notif profiles
        args[22] = new SQLParam(new Integer( GlobalNames.NOTIFICATION_PROFILE_MAX ), Types.INTEGER );

        //OUT parameter: returning profile ID
        args[23] = new SQLParam(new Long(-1), Types.BIGINT, true);


        dao.callSP(conn, "XC_ACCOUNT_PKG.NEWNOTIFICATIONPROFILE", args, false);

        // return profile id
        return ((Long) args[23].getParam()).longValue();

     }


    /* Creates a new controller subscription associated with an account  */
     // vp889x -- Replaced with newSubscription(Connection conn, XanbooAdminPrincipal xCaller, XanbooSubscription xsub) .. No Usage 
/*    String newSubscription(Connection conn, XanbooAdminPrincipal xCaller, long accountId, String subsId, int subsFlags,
            String hwId, String label, String tzName, String masterPin, String masterDuress, String alarmPass,
             XanbooContact subsInfo, XanbooNotificationProfile[] emergencyContacts) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[newSubscription()]:");
        }

        SQLParam[] args=new SQLParam[23+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // set IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam( new Long(accountId), Types.BIGINT );
        args[2] = new SQLParam( subsId.trim() );
        args[3] = new SQLParam( new Integer(subsFlags), Types.INTEGER );
        args[4] = new SQLParam( hwId.trim() );
        args[5] = new SQLParam( label.trim() );
        args[6] = new SQLParam( tzName.trim() );
        args[7] = new SQLParam( (masterPin==null || masterPin.trim().length()==0) ?  null : masterPin.trim() );
        args[8] = new SQLParam( (masterDuress==null || masterDuress.trim().length()==0) ?  null : masterDuress );
        args[9] = new SQLParam( (alarmPass==null || alarmPass.trim().length()==0) ?  null : alarmPass );
        args[10] = new SQLParam( subsInfo.getLastName().trim() );
        args[11] = new SQLParam( subsInfo.getFirstName().trim() );
        args[12] = new SQLParam( subsInfo.getAddress1().trim() );
        args[13] = new SQLParam( subsInfo.getAddress2().trim() );
        args[14] = new SQLParam( subsInfo.getCity().trim() );
        args[15] = new SQLParam( subsInfo.getState().trim() );
        args[16] = new SQLParam( subsInfo.getZip().trim() );
        args[17] = new SQLParam( subsInfo.getZip4().trim() );
        args[18] = new SQLParam( subsInfo.getCountry().trim() );
        args[19] = new SQLParam( null );
        args[20] = new SQLParam( null );
        args[21] = new SQLParam( null );    //null subs features for old call
        args[22] = new SQLParam("", Types.VARCHAR, true);   // for returning temporary subscription gguid

        dao.callSP(conn, "XC_SYSADMIN_PKG.NEWSUBSCRIPTION", args, false);   // add subscription

        String gguid = args[22].getParam().toString();

         now add each valid emergency contact 
        if(emergencyContacts!=null) {
            for(int i=0; i<emergencyContacts.length; i++) {
                if(emergencyContacts[i].isValidEmergencyContact()) {
                    emergencyContacts[i].setGguid(gguid);
                    newNotificationProfile(conn, accountId, emergencyContacts[i]);
                }else {
                    break;
                }
            }
        }

        return gguid;

    }*/


    /* Creates a new controller subscription associated with an account  */
    String newSubscription(Connection conn, XanbooAdminPrincipal xCaller, XanbooSubscription xsub) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[newSubscription()]:");
        }

        SQLParam[] args=new SQLParam[26+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // set IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam( new Long(xsub.getAccountId()), Types.BIGINT );
        args[2] = new SQLParam( xsub.getSubsId() );
        args[3] = new SQLParam( new Integer(xsub.getSubsFlags()), Types.INTEGER );
        args[4] = new SQLParam( xsub.getHwId() );
        args[5] = new SQLParam( xsub.getLabel() );
        args[6] = new SQLParam( xsub.getTzone() );
        args[7] = new SQLParam( (xsub.getDisarmPin()==null || xsub.getDisarmPin().length()==0) ?  null : xsub.getDisarmPin() );
        args[8] = new SQLParam( (xsub.getDuressPin()==null || xsub.getDuressPin().length()==0) ?  null : xsub.getDuressPin() );
        args[9] = new SQLParam( (xsub.getAlarmCode()==null || xsub.getAlarmCode().length()==0) ?  null : xsub.getAlarmCode() );
        args[10] = new SQLParam( xsub.getSubsInfo().getLastName() );
        args[11] = new SQLParam( xsub.getSubsInfo().getFirstName() );
        args[12] = new SQLParam( xsub.getSubsInfo().getAddress1() );
        args[13] = new SQLParam( xsub.getSubsInfo().getAddress2() );
        args[14] = new SQLParam( xsub.getSubsInfo().getCity() );
        args[15] = new SQLParam( xsub.getSubsInfo().getState() );
        args[16] = new SQLParam( xsub.getSubsInfo().getZip() );
        args[17] = new SQLParam( xsub.getSubsInfo().getZip4() );
        args[18] = new SQLParam( xsub.getSubsInfo().getCountry() );
        args[19] = new SQLParam( xsub.getbMarket() );
        args[20] = new SQLParam( xsub.getbSubMarket() );
        args[21] = new SQLParam( xsub.getSubsFeatures() );
        args[22] = new SQLParam( xsub.getTcFlag() ); // DLDP 2728 Changes -vp889x --start  
        args[23] = new SQLParam( xsub.getInstallType());
        args[24] = new SQLParam( xsub.getSubscriptionClass() == null || xsub.getSubscriptionClass().equalsIgnoreCase("") ? "DLSEC" : xsub.getSubscriptionClass());
        args[25] = new SQLParam("", Types.VARCHAR, true);   // for returning temporary subscription gguid

        dao.callSP(conn, "XC_SYSADMIN_PKG.NEWSUBSCRIPTION", args, false);   // add subscription

        String gguid = args[25].getParam().toString();
        // DLDP 2728 Changes -vp889x --end  

        /* now add each valid emergency contact */
        XanbooNotificationProfile[] emergencyContacts = xsub.getNotificationProfiles();
        if(emergencyContacts!=null) {
            for(int i=0; i<emergencyContacts.length; i++) {
                if(emergencyContacts[i].isValidEmergencyContact()) {
                    emergencyContacts[i].setGguid(gguid);
                    newNotificationProfile(conn, xsub.getAccountId(), emergencyContacts[i]);
                }else {
                    break;
                }
            }
        }
        return gguid;
    }
    
    
    /* Updates an existing controller subscription associated with an account  */
    XanbooSubscription updateSubscription(Connection conn, XanbooAdminPrincipal xCaller, XanbooSubscription xsub, String hwIdNew, int alarmDelay, int tcFlag) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateSubscription()]:");
        }

        SQLParam[] args=new SQLParam[27+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // set IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam( new Long(xsub.getAccountId()), Types.BIGINT );
        args[2] = new SQLParam( xsub.getSubsId() );
        args[3] = new SQLParam( xsub.getHwId() );

        if(xsub.isSubsFlagsToBeUpdated()) {
            args[4] = new SQLParam( new Integer(xsub.getSubsFlags()), Types.INTEGER );
            args[5] = new SQLParam( new Integer(xsub.getSubsFlagsMask()), Types.INTEGER );
        }else {
            args[4] = new SQLParam( null );
            args[5] = new SQLParam( null );
        }
            
        args[6]  = new SQLParam( (hwIdNew!=null && hwIdNew.trim().length()>0) ? hwIdNew.trim() : null );
        
        
        args[7] = new SQLParam( xsub.getLabel() );
        args[8] = new SQLParam( xsub.getTzone() );
        args[9] = new SQLParam( (xsub.getDisarmPin()==null || xsub.getDisarmPin().length()==0) ?  null : xsub.getDisarmPin() );
        args[10] = new SQLParam( (xsub.getDuressPin()==null || xsub.getDuressPin().length()==0) ?  null : xsub.getDuressPin() );
        args[11] = new SQLParam( (xsub.getAlarmCode()==null || xsub.getAlarmCode().length()==0) ?  null : xsub.getAlarmCode() );
        if(xsub.getSubsInfo()!=null) {
            args[12] = new SQLParam( xsub.getSubsInfo().getLastName() );
            args[13] = new SQLParam( xsub.getSubsInfo().getFirstName() );
            args[14] = new SQLParam( xsub.getSubsInfo().getAddress1() );
            args[15] = new SQLParam( xsub.getSubsInfo().getAddress2() );
            args[16] = new SQLParam( xsub.getSubsInfo().getCity() );
            args[17] = new SQLParam( xsub.getSubsInfo().getState() );
            args[18] = new SQLParam( xsub.getSubsInfo().getZip() );
            args[19] = new SQLParam( xsub.getSubsInfo().getZip4() );
            args[20] = new SQLParam( xsub.getSubsInfo().getCountry() );
        }else {
            args[12] = new SQLParam( null );
            args[13] = new SQLParam( null );
            args[14] = new SQLParam( null );
            args[15] = new SQLParam( null );
            args[16] = new SQLParam( null );
            args[17] = new SQLParam( null );
            args[18] = new SQLParam( null );
            args[19] = new SQLParam( null );
            args[20] = new SQLParam( null );
        }
        
        if(alarmDelay>=0)
            args[21] = new SQLParam( new Integer(alarmDelay), Types.INTEGER );
        else
            args[21] = new SQLParam( null );
        
        if(tcFlag>=0)
            args[22] = new SQLParam( new Integer(tcFlag), Types.INTEGER );
        else
            args[22] = new SQLParam( null );
        
        args[23] = new SQLParam(xsub.getSubsFeatures() );   // subscription features col to update
        args[24] = new SQLParam( xsub.getInstallType());
        
        args[25] = new SQLParam("", Types.VARCHAR, true);   // for returning subscription gguid
        args[26] = new SQLParam(new Integer(-1), Types.INTEGER, true);   //for returneing current subs flags value

        dao.callSP(conn, "XC_SYSADMIN_PKG.UPDATESUBSCRIPTION", args, false);

        xsub.setGguid(args[25].getParam().toString());
        xsub.setSubsFlags(((Integer) args[26].getParam()).intValue());
        return xsub;
        
    }
    
    public List<String> getSubsFeatureACL(Connection conn, Long accountId, String featureId)throws XanbooException
    {
        ArrayList<String> outLst = new ArrayList<String>();
        if ( logger.isDebugEnabled() )
            logger.debug("[getSubsFeatureACL()] - get service ids for feature="+featureId);
        
        SQLParam[] args = new SQLParam[1+2];
        args[0] = new SQLParam(featureId,Types.VARCHAR);
        
         XanbooResultSet results = (XanbooResultSet) dao.callSP(conn, "XC_EXTSERVICES_PKG.get_subs_feature_acl", args);
         
         for ( int i = 0; i < results.size(); i++ )
         {
             String serviceId = results.getElementString(i, "VENDOR_ID");
             outLst.add(serviceId);
         }
        
        return outLst;
    }
    
    public List<ServiceSubscription> getAccountSubscriptionSvcs(Connection conn, Long accountId, String featureId) throws XanbooException
    {
        if ( logger.isDebugEnabled() )
            logger.debug("[getAccountSubscriptionSvcs]  accountId="+accountId+", featureId="+featureId);
        
        String procName = null;
        SQLParam[] args = null;
        
        if ( featureId == null )
        {
            procName = "XC_EXTSERVICES_PKG.getServiceSubscription";
            args = new SQLParam[3+2];
            args[0] = new SQLParam(accountId,Types.NUMERIC);
            args[1] = new SQLParam(null,Types.NULL);
            args[2] = new SQLParam("XV"+accountId,Types.VARCHAR);
        }
        else
        {
            procName = "XC_EXTSERVICES_PKG.getAccountSubscriptionSvcs";
            args = new SQLParam[3+2];
            args[0] = new SQLParam(accountId,Types.NUMERIC);
            args[1] = new SQLParam(featureId,Types.VARCHAR);
            args[2] = new SQLParam(1,Types.NUMERIC); //instruct stored proc to only return services being removed for featureId
        }
        
        XanbooResultSet rs = (XanbooResultSet)dao.callSP(conn,procName,args);
        List<ServiceSubscription> svcsSubs = new ArrayList<ServiceSubscription>();
        int len = rs.size();
        for ( int i = 0; i < len; i++ )
        {
            ServiceSubscription subs = new ServiceSubscription();
            svcsSubs.add(subs);
            subs.setAccountId(accountId);
            subs.setServiceId(rs.getElementString(i, "SERVICE_ID"));
            subs.setGguid(rs.getElementString(i, "GGUID"));
            subs.setSgGuid(rs.getElementString(i,"SGGUID"));
        }
        return svcsSubs;
    }
    
    /* Cancels an existing subscription for an account  */
    XanbooSubscription cancelSubscription(Connection conn, XanbooAdminPrincipal xCaller, XanbooSubscription xsub) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[cancelSubscription()]:");
        }

        SQLParam[] args=new SQLParam[8+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // set IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam( new Long(xsub.getAccountId()), Types.BIGINT );
        args[2] = new SQLParam( xsub.getSubsId() );
        args[3] = new SQLParam( xsub.getHwId() );
        args[4] = new SQLParam("", Types.VARCHAR, true);   // for returning subscription gguid
        args[5] = new SQLParam(new Integer(-1), Types.INTEGER, true);   //for returneing current subs flags value
        args[6] = new SQLParam("",Types.VARCHAR,true); //returning subscription features
        args[7] = new SQLParam("",Types.VARCHAR,true); //returning subscription type(class)

        dao.callSP(conn, "XC_SYSADMIN_PKG.CANCELSUBSCRIPTION", args, false);   // cancel subscription

        xsub.setGguid(args[4].getParam().toString());
        xsub.setSubsFlags(((Integer) args[5].getParam()).intValue());
        xsub.setSubsFeatures(args[6].getParam().toString());
        xsub.setSubscriptionClass(args[7].getParam().toString());
        return xsub;
    }
    
 /* delets a existing subscription for an account  */
    XanbooSubscription deleteSubscription(Connection conn, XanbooAdminPrincipal xCaller, XanbooSubscription xsub) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[cancelSubscription()]:");
        }

        SQLParam[] args=new SQLParam[4+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // set IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam( new Long(xsub.getAccountId()), Types.BIGINT );
        args[2] = new SQLParam( xsub.getSubsId() );
        args[3] = new SQLParam( xsub.getHwId() );

        dao.callSP(conn, "XC_SYSADMIN_PKG.deleteSubscription", args, false);   // cancel subscription
        //there is no need return subs.Keep it for future requirements.
        return xsub;
    }

    /* Gets a subscription record */
    XanbooResultSet getSubscription(Connection conn, XanbooAdminPrincipal xCaller, long accountId, String subsId, String hwId) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[getSubscription()]:");
        }

        XanbooResultSet results = null;
        SQLParam[] args=new SQLParam[4+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // set IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        if(accountId>0)
            args[1] = new SQLParam( new Long(accountId), Types.BIGINT );
        else
            args[1] = new SQLParam( null, Types.BIGINT );

        args[2] = new SQLParam( subsId );
        args[3] = new SQLParam( hwId );

        results = (XanbooResultSet) dao.callSP(conn, "XC_SYSADMIN_PKG.GETSUBSCRIPTION", args);

        return results;

    }

    
    public void auditLog(Connection conn, XanbooAdminPrincipal xCaller, long accountId, String gatewayGUID, String deviceGUID, String actionSource, String actionDesc, String actionValue) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[auditLog()]: ");
        }
        
        // setting IN params
        SQLParam[] args=null;
        args = new SQLParam[8+2];

        args[0] = new SQLParam(xCaller.getUsername());
        args[1] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[2] = new SQLParam(gatewayGUID, Types.VARCHAR);
        args[3] = new SQLParam(deviceGUID, Types.VARCHAR);
        args[4] = new SQLParam(actionSource, Types.VARCHAR);
        args[5] = new SQLParam(actionDesc, Types.VARCHAR);
        args[6] = new SQLParam(actionValue, Types.VARCHAR);
        args[7] = new SQLParam(new Long(-1), Types.BIGINT, true);       //returning auditlog id

        dao.callSP(conn, "XC_SYSADMIN_PKG.LOGAUDITACTIVITY", args, false);

        //return ((Long) args[6].getParam()).longValue();
    }
    
    
    public XanbooResultSet getLocationCodeList(Connection conn, String domainId, String lang, int level) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[getLocationCodeList()]:");
        }
        
        SQLParam[] args=new SQLParam[4+2];     // SP parameters + 2 std parameters (errno, errmsg)
       
        args[0] = new SQLParam( (domainId==null || domainId.trim().length()==0) ?  null : domainId.trim() );
        args[1] = new SQLParam( (lang==null || lang.trim().length()==0) ?  null : lang.trim() );
        args[2] = new SQLParam(new Integer(level), Types.INTEGER);
        args[3] = new SQLParam(null);
        
        XanbooResultSet results = (XanbooResultSet)dao.callSP(conn, "XC_SYSADMIN_PKG.GETLOCATIONCODELIST", args);
        
        return results;
        
    } 

    public void addDeleteProvisionedDevice(Connection conn, XanbooAdminPrincipal xCaller, long accountId, String subsId, String hwId, String classId, String subclassId, String installType, String swapGuid, int count, boolean isDelete) throws XanbooException  {
        if(logger.isDebugEnabled()){
            logger.debug((isDelete ? "[deleteProvisionedDevice]: " : "[addProvisionedDevice]: "));
        }
        
        SQLParam[] args=new SQLParam[10+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // set IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam( new Long(accountId), Types.BIGINT );
        args[2] = new SQLParam( subsId );
        args[3] = new SQLParam( hwId );
        args[4] = new SQLParam( classId );
        args[5] = new SQLParam( subclassId );
        args[6] = new SQLParam( installType );
        args[7] = new SQLParam( swapGuid );
        if(isDelete)
            args[8] = new SQLParam(1, Types.INTEGER  );
        else
            args[8] = new SQLParam(0, Types.INTEGER  );
        args[9] = new SQLParam( count );

        if(swapGuid==null || swapGuid.length()==0)
            dao.callSP(conn, "XC_SYSADMIN_PKG.ADDDELETEPROVISIONEDDEVICE", args, false);
        else
            dao.callSP(conn, "XC_SYSADMIN_PKG.ADDDELETESWAPDEVICE", args, false);
        
    } 
    
    
    public XanbooResultSet getProvisionedDeviceList(Connection conn, XanbooAdminPrincipal xCaller, long accountId, String subsId, String hwId, String classId, String subclassId, String installType) throws XanbooException   {
        if(logger.isDebugEnabled()){
            logger.debug("[getProvisionedDeviceList]");
        }

        SQLParam[] args=new SQLParam[7+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // set IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam( new Long(accountId), Types.BIGINT );
        args[2] = new SQLParam( subsId );
        args[3] = new SQLParam( hwId );
        args[4] = new SQLParam( classId );
        args[5] = new SQLParam( subclassId );
        args[6] = new SQLParam( installType );
        
        XanbooResultSet results = (XanbooResultSet)dao.callSP(conn, "XC_SYSADMIN_PKG.GETPROVISIONEDDEVICELIST", args);
        
        return results;
    } 
    
    public XanbooResultSet getSupportedDeviceList(Connection conn, String domainId, String installType, String monType) throws XanbooException   {
        if(logger.isDebugEnabled()){
            logger.debug("[getSupportedDeviceList]");
        }

        SQLParam[] args=new SQLParam[3+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // set IN params
        args[0] = new SQLParam( domainId );
        args[1] = new SQLParam( installType );
        args[2] = new SQLParam( monType );
        
        XanbooResultSet results = (XanbooResultSet)dao.callSP(conn, "XC_SYSADMIN_PKG.GETSUPPORTEDDEVICELIST", args);
        
        return results;
    } 

    /**
     * Returns the alarm archive item(s) matching the search criteria
     * @param conn
     * @param xap
     * @param accountId
     * @param archiveId
     * @param extAccountId
     * @param subsIdOrGguId
     * @param egrpId
     * @param startTS
     * @param endTS
     * @return
     * @throws XanbooException
     */
    public XanbooResultSet getAlarmArchiveItem( Connection conn, XanbooAdminPrincipal xap, long accountId, long archiveId, String extAccountId, String subsIdOrGguId, String egrpId, String startTS, String endTS ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getAlarmArchiveItem()]");
        }

        SQLParam[] args=new SQLParam[8+2];     // 4 SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam( new Long(archiveId), Types.BIGINT );
        args[1] = new SQLParam(xap.getDomain());
        args[2] = new SQLParam( new Long(accountId), Types.BIGINT );
        args[3] = new SQLParam(extAccountId);
        args[4] = new SQLParam(subsIdOrGguId);
        args[5] = new SQLParam(egrpId);
        args[6] = new SQLParam(startTS);
        args[7] = new SQLParam(endTS);

        return dao.callSP(conn, "XC_ADMIN_PKG.getAlarmArchiveItem", args);
    }

    public void updateAlarmArchiveItem( Connection conn, XanbooAdminPrincipal xap, long archiveId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateAlarmArchiveItem()]: archive id "+archiveId);
        }

        SQLParam[] args=new SQLParam[1+2];     // 4 SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam( new Long(archiveId), Types.BIGINT );


        dao.callSP(conn, "XC_ADMIN_PKG.updateAlarmArchiveItem", args, false);

    }

     /**
     * Returns the total number of alerts for an account/gguid
     */
     int getAlertCount(Connection conn, XanbooAdminPrincipal xCaller, long accountId, String gatewayGUID) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getAlertCount()]:");
        }

        SQLParam[] args=new SQLParam[4+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[2] = new SQLParam( gatewayGUID );

        //OUT parameter: returning profile ID
        args[3] = new SQLParam(new Integer(-1), Types.INTEGER, true);


        dao.callSP(conn, "XC_MISC_PKG.GETALERTCOUNT", args, false);

        // return profile id
        return ((Integer) args[3].getParam()).intValue();

     }

     public Integer getNotificationOptInStatus(Connection conn, String domainId, String notificationAddress, String token) throws XanbooException {
        SQLParam[] args=new SQLParam[4+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(domainId );
        args[1] = new SQLParam((notificationAddress==null || notificationAddress.length()==0) ? null : notificationAddress);
        args[2] = new SQLParam((token==null || token.length()==0) ? null : token);

        //OUT parameter: returning opt-in status
        args[3] = new SQLParam(new Integer(-1), Types.INTEGER, true);


        dao.callSP(conn, "XC_SYSADMIN_PKG.GETNOTIFICATIONOPTINSTATUS", args, false);

        // return opt-in status
        Integer returnVal = (Integer) args[3].getParam();
        if (returnVal == null || returnVal == -1)
            return null;
        return returnVal;
     }

     public Map<String, Integer> getNotificationOptInStatus( Connection conn, String domainId, List<String> notificationAddresses ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getNotificationOptInStatusList()]");
        }

        StringBuffer notificationAddressStr = new StringBuffer();

        for (String notificationAddress : notificationAddresses) {
            if (notificationAddressStr.length() > 0)
                 notificationAddressStr.append(",");

             notificationAddressStr.append("'").append(notificationAddress).append("'");
        }

        SQLParam[] args=new SQLParam[3+2];     // 2 SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(domainId);
        args[1] = new SQLParam(notificationAddressStr);
        args[2] = new SQLParam(null);

        XanbooResultSet dbOutput = dao.callSP(conn, "XC_SYSADMIN_PKG.getNotificationOptInStatusList", args);

        HashMap result = new HashMap<String, Integer>();

        if (dbOutput == null) return result;

        for (int i=0; (dbOutput != null && i < dbOutput.size()); i++) {
                String addr = dbOutput.getElementString(i, "PROFILE_ADDRESS");
                Integer statusId = dbOutput.getElementInteger(i, "STATUS_ID");
                result.put(addr, statusId);
        }
        return result;
    }

    public void setNotificationOptInStatus( Connection conn, long accountId, String domainId, String notificationAddress, String token, int status, String language, String tzname, String profileType ) throws XanbooException {
        SQLParam[] args=new SQLParam[9+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(domainId);
        if (accountId <= 0)
            args[1] = new SQLParam(null,Types.NULL);
        else
            args[1] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[2] = new SQLParam((notificationAddress==null || notificationAddress.length()==0) ? null : notificationAddress);
        args[3] = new SQLParam((token==null || token.length()==0) ? null : token);
        args[4] = new SQLParam(new Integer(status), Types.INTEGER);
        args[5] = new SQLParam((language==null || language.length()==0) ? null : language);
        args[6] = new SQLParam((tzname==null || tzname.length()==0) ? null : tzname);
        args[7] = new SQLParam((profileType==null || profileType.length()==0) ? null : profileType);

        args[8] = new SQLParam("", Types.VARCHAR, true); //returns the token
        dao.callSP(conn, "XC_SYSADMIN_PKG.SETNOTIFICATIONOPTINSTATUS", args, false);
    }
    
    public XanbooResultSet getDomainLicenses(Connection conn, String domainId) throws XanbooException {
        if(logger.isDebugEnabled()){
            logger.debug("[getDomainLicenses]");
        }
        
        XanbooResultSet results = null;
		try {
			SQLParam[] args=new SQLParam[1+2];     // SP parameters + 2 std parameters (errno, errmsg)

			// set IN params
			args[0] = new SQLParam( domainId );      //domainId
			
			results = (XanbooResultSet)dao.callSP(conn, "XC_SYSADMIN_PKG.GETDOMAINLICENSES", args);
		} catch(XanbooException xe) {
            if (logger.isDebugEnabled()) {
                logger.debug( "[getDomainLicenses()]:" + xe.getErrorMessage());
            }
            throw xe;
        } 
        return results;
    }

	public XanbooAdminPrincipal getXap() {
		return xap;
	}
    

	 /**
	 * Method to retrieve feature list based on domain. 
	 * @param conn - database connection object 
	 * @param domainId - domain id value
	 * @return instance of XanbooResultSet. 
	 * @throws XanbooException
	 */
	public XanbooResultSet getDomainFeatureList(Connection conn, String domainId)throws XanbooException{
		 if(logger.isDebugEnabled()){
	            logger.debug("[getDomainFeatureList]");
	        }
	        
	        XanbooResultSet results = null;
			try {
				SQLParam[] args=new SQLParam[1+2];     // SP parameters + 2 std parameters (errno, errmsg)

				// set IN params
				args[0] = new SQLParam( domainId );      //domainId
				
				
				results = (XanbooResultSet)dao.callSP(conn, "XC_SYSADMIN_PKG.GETSUBSCRIPTIONFEATURE", args);
			} catch(XanbooException xe) {
	            if (logger.isDebugEnabled()) {
	                logger.debug( "[getDomainFeatureList()]:" + xe.getErrorMessage());
	            }
	            throw xe;
	        } 
	        return results;
	 }
	
	
	 
		
		/**
		 * @param conn - database connection object 
		 * @param bMarket - business market
		 * @param state - state
		 * @param city - city
		 * @param zip - zip
		 * @return - instance of XanbooResultSet 
		 * @throws XanbooException
		 */
		public XanbooResultSet getSubscriptions(Connection conn, String bMarket, String state, String city, String zip, Integer prefBit)throws XanbooException{
			 if(logger.isDebugEnabled()){
		            logger.debug("[getSubscriptions]");
		        }
		        
		        XanbooResultSet results = null;
				try {
					SQLParam[] args=new SQLParam[5+2];     // SP parameters + 2 std parameters (errno, errmsg)

					// set IN params
					args[0] = new SQLParam(bMarket);      			//business market
					args[1] = new SQLParam(state);      //state
					args[2] = new SQLParam(city);      //city
					args[3] = new SQLParam(zip);      //zip
					args[4] = new SQLParam(prefBit);      // preference bit
					
					
					results = (XanbooResultSet)dao.callSP(conn, "XC_SYSADMIN_PKG.GETSUBSCRIPTIONS", args);
				} catch(XanbooException xe) {
		            if (logger.isDebugEnabled()) {
		                logger.debug( "[getSubscriptions()]:" + xe.getErrorMessage());
		            }
		            throw xe;
		        } 
		        return results;
		 }

    /**
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
        SQLParam[] args = new SQLParam[2 + 2];     // SP parameters + 2 std parameters (errno, errmsg)

        // set IN params
        args[0] = new SQLParam(languageId);
        args[1] = new SQLParam(modelId);


        results = (XanbooResultSet) dao.callSP(conn, "XC_DEVICE_PKG.GETDEVICEMODELREF", args);

        return results;
    }
    
	public XanbooResultSet getGatewayGuids(Connection conn, Long accountId) throws XanbooException {		        
		if(logger.isDebugEnabled()){
			logger.debug("[getGatewayGuids]");
		}

		SQLParam[] args=new SQLParam[1+2];     // SP parameters 1 + 2 std parameters (errno, errmsg)

		args[0] = new SQLParam( accountId );
		        
		XanbooResultSet results = (XanbooResultSet)dao.callSP(conn, "XC_SYSADMIN_PKG.GETGATEWAYGUIDS", args);
		        
		return results;
	}
	
	/**
     * retrieve domain references .   
     * @param domainFeatures
     * @throws XanbooException
     */
    public XanbooResultSet getDomainFeatureList(Connection conn, DomainFeatures domainFeatures) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[getDomainFeatureList()]:");
        }
        
        SQLParam[] args=new SQLParam[4+2];     // SP parameters + 2 std parameters (errno, errmsg)
       
        args[0] = new SQLParam( (domainFeatures.getDomainId()==null || domainFeatures.getDomainId().trim().length()==0) ?  null : domainFeatures.getDomainId().trim() );
        args[1] = new SQLParam( (domainFeatures.getFeatureId()==null ||domainFeatures.getFeatureId().length()==0) ?  null : domainFeatures.getFeatureId().trim() );
        args[2] = new SQLParam( (domainFeatures.getSocMappingId() ==null ||domainFeatures.getSocMappingId().length()==0) ?  null : domainFeatures.getSocMappingId().trim() );        
        args[3] = new SQLParam( (domainFeatures.getTc_acceptance() ==null ||domainFeatures.getTc_acceptance().length()==0) ?  null : domainFeatures.getTc_acceptance().trim() );
        
        XanbooResultSet results = (XanbooResultSet)dao.callSP(conn, "XC_ADMIN_PKG.GETDOMAINFEATURELIST", args);
        
        return results;
        
    }
    
    public XanbooResultSet getDeviceListByGGuid(Connection conn,Long accountId,String gguid,String dguid)throws XanbooException
    {
        if ( logger.isDebugEnabled() )
            logger.debug("[getDeviceList()]");
        
        SQLParam[] args=new SQLParam[2+2];     // 2 SP parameter + 2 std parameters (errno, errmsg)
        
        // setting IN params
        args[0] = new SQLParam(gguid, Types.VARCHAR);
        args[1] = dguid == null ? new SQLParam(null,Types.NULL) : new SQLParam(dguid, Types.VARCHAR);
        
        return dao.callSP(conn, "XC_SYSADMIN_PKG.GET_DEVICE_LIST", args);
    }
}
