package com.xanboo.core.sdk.services;

import java.sql.Connection;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.xanboo.core.sdk.services.model.ServiceSubscription;
import com.xanboo.core.sdk.util.XanbooResultSet;
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
 * This class is the DAO class to wrap all generic database calls for SDK ServiceManager methods.
 * Database specific methods are handled by implementation classes. These implementation
 * classes extend the BaseDAO class to actually perform the database operations. An instance of
 * an implementation class is created during construction of this class.
 */
class ServiceManagerDAO extends BaseHandlerDAO {
    
    private BaseDAO dao;
    private Logger logger;
    
    /**
     * Default constructor. Takes no arguments
     *
     * @throws XanbooException
     */
    public ServiceManagerDAO() throws XanbooException {
        
        try {
            // obtain a Logger instance
            logger=LoggerFactory.getLogger(this.getClass().getName());
            if(logger.isDebugEnabled()) {
                logger.debug("[ServiceManagerDAO()]:");
            }
            
            // create implementation Class for Oracle, Sybase, etc.
            dao = (BaseDAO) DAOFactory.getDAO();
            
            // get the Connection factory DataSource for CoreDS
            getDataSource(GlobalNames.COREDS);
            
        }catch(XanbooException xe) {
            throw xe;
        }catch(Exception ne) {
            if(logger.isDebugEnabled()) {
              logger.error("[ServiceManagerDAO()] Exception:" + ne.getMessage(), ne);
            }else {
              logger.error("[ServiceManagerDAO()] Exception: " + ne.getMessage());
            }                
            throw new XanbooException(60014, "[ServiceManagerDAO()] Exception:" + ne.getMessage());
        }
    }
    
    
    public XanbooResultSet getAvailableServiceList(Connection conn, XanbooPrincipal xCaller, String gatewayGUID, Boolean isSelfInstallable) throws XanbooException {
        if(logger.isDebugEnabled()){
            logger.debug("[getAvailableServiceList]");
        }
        
        XanbooResultSet results = null;
		try {
			SQLParam[] args=new SQLParam[3+2];     // SP parameters + 2 std parameters (errno, errmsg)

			// set IN params
			args[0] = new SQLParam( new Long(xCaller.getAccountId()), Types.BIGINT ); //account ID
			args[1] = new SQLParam( gatewayGUID );      //gguid
			
			args[2] = new SQLParam( isSelfInstallable==null? null : (isSelfInstallable?1:0) );      //gguid
			
			results = (XanbooResultSet)dao.callSP(conn, "XC_EXTSERVICES_PKG.GETAVAILABLESERVICELIST", args);
		} catch(XanbooException xe) {
            if (logger.isDebugEnabled()) {
                logger.debug( "[getAvailableServiceList()]:" + xe.getErrorMessage());
            }
            throw xe;
        } 
        
        return results;
    }
    
    public XanbooResultSet getServiceDescriptorList(Connection conn, XanbooPrincipal xCaller, String serviceId) throws XanbooException {
        if(logger.isDebugEnabled()){
            logger.debug("[getServiceDescriptorList]");
        }

        XanbooResultSet results = null;
		try {
			SQLParam[] args=new SQLParam[1+2];     // SP parameters + 2 std parameters (errno, errmsg)

			// set IN params
			args[0] = new SQLParam( serviceId );
			
			results = (XanbooResultSet)dao.callSP(conn, "XC_EXTSERVICES_PKG.GETSERVICEDESCRIPTORLIST", args);
	    } catch(XanbooException xe) {
            if (logger.isDebugEnabled()) {
                logger.debug( "[getServiceDescriptorList()]:" + xe.getErrorMessage());
            }
            throw xe;
        }    
        
        return results;
    }
    
    
    public void bindServiceSubscription(Connection conn, XanbooPrincipal xCaller, ServiceSubscription subs, boolean unbind) throws XanbooException {    
        if (logger.isDebugEnabled()) {
            logger.debug("[bindServiceSubscription()]:");
        }
        
        try{
            SQLParam[] args=new SQLParam[6+2];     //SP parameters + 2 std parameters (errno, errmsg)
            
            // setting IN params
            args[0] = new SQLParam( new Long(xCaller.getAccountId()), Types.BIGINT ); //account ID
            args[1] = new SQLParam( new Long(xCaller.getUserId()), Types.BIGINT );    //user ID
            args[2] = new SQLParam( subs.getServiceId() );                            //service id
            args[3] = new SQLParam( subs.getSgGuid() );                               //service gateway guid as a subs id
            args[4] = new SQLParam(subs.getGguid(), Types.VARCHAR);                   // gguid to bind/unbind
            args[5] = new SQLParam( new Integer(unbind ? 1 : 0), Types.INTEGER );    //unbind flag
            
            dao.callSP(conn, "XC_EXTSERVICES_PKG.BINDSERVICESUBSCRIPTION", args, false);
            
        }catch(XanbooException xe) {
            if (logger.isDebugEnabled()) {
                logger.debug( "[bindServiceSubscription()]:" + xe.getErrorMessage());
            }
            throw xe;
        }    
    }
    
    public List<ServiceSubscription> getServiceSubscription(Connection conn, XanbooPrincipal xCaller, String serviceId, String gatewayGUID) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getServiceSubscription()]:");
        }
        List<ServiceSubscription> subs = null;
        try{
            SQLParam[] args=new SQLParam[3+2];     //SP parameters + 2 std parameters (errno, errmsg)
            
            // setting IN params
            args[0] = new SQLParam( new Long(xCaller.getAccountId()), Types.BIGINT ); //account ID
            args[1] = new SQLParam( serviceId );     // service id
            args[2] = new SQLParam( gatewayGUID );  // gatewayGUID
            
            
            //Call SP
            XanbooResultSet xrs = dao.callSP(conn, "XC_EXTSERVICES_PKG.GETSERVICESUBSCRIPTION", args);
            if(xrs!=null && xrs.size()>0) {
            	subs = new ArrayList<ServiceSubscription>();
                for(int i=0; i<xrs.size(); i++) {
                    HashMap elem = (HashMap) xrs.get(i);
                    ServiceSubscription sub = new ServiceSubscription();
                    sub.setAccountId(Long.parseLong( (String) elem.get("ACCOUNT_ID") ));
                    sub.setServiceId( (String) elem.get("SERVICE_ID") );
                    sub.setGguid((String) elem.get("GGUID"));
                    sub.setSgGuid( (String) elem.get("SGGUID") );
                    sub.setExtAccountId( (String) elem.get("SERVICE_ACCOUNT_ID") );
                    sub.setUserName( (String) elem.get("ACCESS_USERNAME") );
                    sub.setAccessToken( (String) elem.get("ACCESS_TOKEN") );
                    sub.setRefershToken((String) elem.get("REFRESH_TOKEN")); 
                    if(XanbooUtil.isNotEmpty(sub.getRefershToken()) && sub.getRefershToken().contains("|")) {
                     	int lastIndex = sub.getRefershToken().lastIndexOf("|");
                     	sub.setTokenExpiration(sub.getRefershToken().substring(lastIndex + 1));
                     	sub.setRefershToken(sub.getRefershToken().substring(0, lastIndex));
                     }
                    sub.setStatusId(Integer.parseInt((String) elem.get("STATUS_ID")));
                    sub.setDomainId((String) elem.get("DOMAIN_ID"));
                    subs.add(sub);
                }
            }
        }catch(XanbooException xe) {
            if (logger.isDebugEnabled()) {
                logger.debug( "[getServiceSubscription()]:" + xe.getErrorMessage());
            }
            throw xe;
        }    
        return subs;
    }
    
    public ServiceSubscription getServiceSubscription(Connection conn,  String serviceId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getServiceSubscription() by service id]:");
        }
        ServiceSubscription sub = null;
        try{
            SQLParam[] args=new SQLParam[1+2];     //SP parameters + 2 std parameters (errno, errmsg)
            
            // setting IN params
         
            args[0] = new SQLParam( serviceId );     // service id
            
            
            
            //Call SP
            XanbooResultSet xrs = dao.callSP(conn, "XC_EXTSERVICES_PKG.getServiceSubscriptionBySrvcId", args);
            if(xrs!=null && xrs.size()>0) {
            	sub = new ServiceSubscription();
                for(int i=0; i< xrs.size(); i++) {
                    HashMap elem = (HashMap) xrs.get(i);
                    sub.setAccountId(Long.parseLong( (String) elem.get("ACCOUNT_ID") ));
                    sub.setServiceId( (String) elem.get("SERVICE_ID") );
                    sub.setGguid((String) elem.get("GGUID"));
                    sub.setSgGuid( (String) elem.get("SGGUID") );
                    sub.setExtAccountId( (String) elem.get("SERVICE_ACCOUNT_ID") );
                    sub.setUserName( (String) elem.get("ACCESS_USERNAME") );
                    sub.setAccessToken( (String) elem.get("ACCESS_TOKEN") );
                    sub.setRefershToken((String) elem.get("REFRESH_TOKEN")); 
                    if(XanbooUtil.isNotEmpty(sub.getRefershToken()) && sub.getRefershToken().contains("|")) {
                     	int lastIndex = sub.getRefershToken().lastIndexOf("|");
                     	sub.setTokenExpiration(sub.getRefershToken().substring(lastIndex + 1));
                     	sub.setRefershToken(sub.getRefershToken().substring(0, lastIndex));
                     }
                    sub.setStatusId(Integer.parseInt((String) elem.get("STATUS_ID")));
                    if(elem.containsKey("NAME")) {
                    	sub.setServiceName((String) elem.get("NAME"));
                    }
                    sub.setDomainId((String) elem.get("DOMAIN_ID"));
                }
            }
        }catch(XanbooException xe) {
            if (logger.isDebugEnabled()) {
                logger.debug( "[getServiceSubscription()]:" + xe.getErrorMessage());
            }
            throw xe;
        }    
        return sub;
    }

    
   public XanbooResultSet getDeviceList(Connection conn, XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getDeviceList()]:");
        }
        
        try{
            
            SQLParam[] args=new SQLParam[4+2];     // SP parameters + 2 std parameters (errno, errmsg)
            // Setting IN params
            args[0] = new SQLParam(null, Types.VARCHAR); // external service id.
            args[1] = new SQLParam(gatewayGUID, Types.VARCHAR); // DLC/gguid 
            args[2] = new SQLParam(null, Types.VARCHAR); // assuming null service gateway guid.
            args[3] = new SQLParam(deviceGUID, Types.VARCHAR); // device guid.
            
            //Call SP
            return dao.callSP(conn, "XC_EXTSERVICES_PKG.GETDEVICELIST", args);
            
        }catch(XanbooException xe) {
            if (logger.isDebugEnabled()) {
                logger.debug( "[getDeviceList()]:" + xe.getErrorMessage());
            }
            throw xe;
        }    
    }
}
