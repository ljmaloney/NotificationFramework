/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/gateway/GatewayManagerEJB.java,v $
 * $Id: GatewayManagerEJB.java,v 1.28 2011/07/01 16:11:31 levent Exp $
 *
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.gateway;

import java.sql.Connection;
import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.ejb.CreateException;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import org.apache.log4j.Logger;

import com.xanboo.core.model.XanbooAccessKey;
import com.xanboo.core.sdk.device.DeviceManager;
import com.xanboo.core.sdk.gateway.rules.XanbooRule;
import com.xanboo.core.sdk.util.MObjectMap;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.EjbProxy;
import com.xanboo.core.util.GlobalNames;
import com.xanboo.core.util.XanbooException;
import com.xanboo.core.util.XanbooUtil;

/**
 * <p>
 * Session Bean implementation of <code>GatewayManager</code>. This bean acts as a wrapper class for
 * special Gateway related Core SDK methods.
 * </p>
 */
@Remote (GatewayManager.class)
@Stateless (name="GatewayManager" )
@TransactionManagement( TransactionManagementType.BEAN )

public class GatewayManagerEJB   {
    
    private static final String RULE_GUARD_SEPARATOR = "|";
    private static final String ACCESS_KEY_SEPARATOR = "|";
   
    private Logger logger = Logger.getLogger(GatewayManagerEJB.class);
    private GatewayManagerDAO dao = null;
    
    private DeviceManager dManager ;
    
    private static final String RULE_OID = "512";
    private static final String EXEC_RULE_OID = "513";
    private static final String ACCESS_KEY_OID = "650";
    
    @PostConstruct
    public void init() throws CreateException {
        
        try {
            // create a logger instance
          //  logger=LoggerFactory.getLogger(GatewayManagerEJB.class.getName());
            if(logger.isDebugEnabled()) {
                logger.debug("[ejbCreate()]:");
            }
            EjbProxy proxy = new EjbProxy();
           dao = new GatewayManagerDAO();
           dManager = (DeviceManager) proxy.getObj(GlobalNames.EJB_DEVICE_MANAGER);
            
        }catch (Exception se) {
            if(logger.isDebugEnabled()) {
                logger.error("[init()]: " + se.getMessage(), se);
            }else {
                logger.error("[init()]: " + se.getMessage());
            }
            throw new CreateException("Failed to @ init DeviceGroupManager:" + se.getMessage());
        }
    }
    
    
    /* gets a reference to the DeviceManager EJB, if necessary */
/*    public void getDeviceManagerEJB() {
        if(dManager==null) {
            EjbProxy proxy = new EjbProxy();
            try {
                dManager = (DeviceManager) proxy.getObj(GlobalNames.EJB_DEVICE_MANAGER);
            }catch(Exception e) {
                if(logger.isDebugEnabled()) {
                    logger.error("[getDeviceManagerEJB()]:" + e.getMessage(), e);
                }else {
                    logger.error("[getDeviceManagerEJB()]:" + e.getMessage());
                }
            }
        }
    }*/
    
    
    public XanbooRule[] getRules( XanbooPrincipal xCaller, String gatewayGUID ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getRules()]:");
        }
        
        // validate the input parameters
        if(gatewayGUID==null || gatewayGUID.trim().equals("") ){
            throw new XanbooException(10050);
        }
        
        XanbooRule[] rules = null;
        
        try {
            
            // first validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            // retrieve mobject list for entire gateway via DeviceManagerEJB
            XanbooResultSet mobjects = dManager.getMObject( xCaller, gatewayGUID, "0", null );
            MObjectMap map = new MObjectMap( mobjects );
            
            int rowCount = Integer.parseInt( map.getValue( RULE_OID ) );
            
            // parse result set into array of XanbooRule objects
            rules = new XanbooRule[ rowCount ];
            for ( int i=0; i<rowCount; i++ ) {
                try {
                    String baseOid = RULE_OID + "." + Integer.toString(i) + ".";
                    String g = map.getPending( baseOid + "0" );
                    int pos = g.indexOf( RULE_GUARD_SEPARATOR );                    
                    rules[i] = new XanbooRule( i, Integer.parseInt( g.substring( 0, 1 ), 16 ), g.substring(1, pos), g.substring( pos + 1 ), map.getPending( baseOid + "1" ) );
                } catch ( Exception ne ) {
                    //Could be a NumberFormatExcepion, or a XanbooException - either way, there's not much we can do about it apart from create a blank rule.
                    rules[i] = new XanbooRule( i );
                }
            }
            
            return rules;
        } catch ( java.lang.NumberFormatException nfe ) {
            //This happens when trying to determine rowCount for a gateway descriptor that does not support table objects.
            return null;
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[getRules()]: " + e.getMessage(), e);
            }else {
                logger.error("[getRules()]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[getRules]:" + e.getMessage());
        }
    }
    
    /* Updates a device rule */
    public void updateRule( XanbooPrincipal xCaller, String gatewayGUID, int ruleIndex, XanbooRule rule ) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[updateRule()]:");
        }
        
        // validate the input parameters
        if(gatewayGUID==null || gatewayGUID.trim().equals("") || rule == null || ruleIndex < 0 ){
            throw new XanbooException(10050);
        } 
        
        rule.validate();
        
        try {
            
            // first validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            rule.validate();
            
            XanbooRule[] rules = this.getRules( xCaller, gatewayGUID );
            XanbooRule oldRule = rules[ruleIndex];
            
            // User may not change the status of a rule unless it is active or suspended.
            if( !(( oldRule.getStatus() == XanbooRule.STATUS_ACTIVE || oldRule.getStatus() == XanbooRule.STATUS_SUSPENDED ) &&
                ( rule.getStatus() == XanbooRule.STATUS_ACTIVE || rule.getStatus() == XanbooRule.STATUS_SUSPENDED ))) {
                // We allow updating of the rule between active and suspended status
                // we do not allow updating of rule status for other cases - use previous status.
                rule.setStatus( oldRule.getStatus() );
            }
            
            // check to see if there is an attempted update to any read-only attributes
            if ( !oldRule.isActionWritable() && !oldRule.toActionString().toUpperCase().equals( rule.toActionString().toUpperCase() ) ) {
                // sdk user tried to change read-only action attribute
                throw new XanbooException( 29052, "Rule Action is read only and cannot be changed." );
            } else if ( !oldRule.isNameWritable() && !oldRule.getName().toUpperCase().equals( rule.getName().toUpperCase() ) ) {
                // sdk user tried to change read-only rule name
                throw new XanbooException( 29050, "Rule Name is read only and cannot be changed." );
            } else if ( !oldRule.isGuardWritable() && !oldRule.toGuardString().toUpperCase().equals( rule.toGuardString().toUpperCase() ) ) {
                // sdk user tried to change read-only guard
                throw new XanbooException( 29051, "Rule Guard is read only and cannot be changed." );
            } else if ( !(oldRule.isGuardWritable()==rule.isGuardWritable()) ||
                        !(oldRule.isNameWritable()==rule.isNameWritable()) ||
                        !(oldRule.isActionWritable()==rule.isActionWritable()) ) {
                // access rights may not be changed by sdk user.
                throw new XanbooException( 29053, "Access rights cannot be changed." );
            }

            StringBuffer row0 = new StringBuffer();
            row0.append( Integer.toString(rule.getStatus(), 16) );
            if ( !oldRule.isNameWritable() ) {
                if ( !oldRule.isGuardWritable() && !oldRule.isActionWritable() ) {
                    row0.append('&');
                } else if ( oldRule.isGuardWritable() && oldRule.isActionWritable() ) {
                    row0.append('%');
                } else if ( !oldRule.isGuardWritable() ) {
                    row0.append('~');
                }
            }
            row0.append( rule.getName() ).append( RULE_GUARD_SEPARATOR ).append( rule.toGuardString() );
            
            String baseOID = RULE_OID + "." + Integer.toString( ruleIndex ) + "." ;
            String[] oids = { baseOID + "0", baseOID + "1" };
            String[] vals = { row0.toString(), rule.toActionString() };
            
            dManager.setMObject( xCaller, gatewayGUID, "0", oids, vals );
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[updateRule()]: " + e.getMessage(), e);
            }else {
                logger.error("[updateRule()]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[updateRule]:" + e.getMessage());
        }
    }
    
    /* Deletes existing device rule */
    public void deleteRule( XanbooPrincipal xCaller, String gatewayGUID, int ruleIndex ) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteRule()]:");
        }
        
        // validate the input parameters
        if(gatewayGUID==null || gatewayGUID.trim().equals("") || ruleIndex < 0){
            throw new XanbooException(10050);
        }        
        
        try {
            
            // first validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            String baseOID = RULE_OID + "." + Integer.toString( ruleIndex ) + "." ;
            String[] oids = { baseOID + "0", baseOID + "1" };
            String[] vals = { "", "" };
            dManager.setMObject( xCaller, gatewayGUID, "0", oids, vals );
            
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[deleteRule()]: " + e.getMessage(), e);
            }else {
                logger.error("[deleteRule()]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[deleteRule]:" + e.getMessage());
        }
    }
    
    public int addRuleSpace( XanbooPrincipal xCaller, String gatewayGUID ) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[addRuleSpace()]:");
        }
        
        // validate the input parameters
        if(gatewayGUID==null || gatewayGUID.trim().equals("") ){
            throw new XanbooException(10050);
        }        
        
        try {
            
            // first validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            XanbooResultSet baseRow = dManager.getMObject( xCaller, gatewayGUID, "0", RULE_OID );
            int rowCount = baseRow.getElementInteger( 0, "PENDING_VALUE" );
            
            //Try to increase the size of the table.
            try {
                dManager.setMObject( xCaller, gatewayGUID, "0", RULE_OID, Integer.toString( rowCount + 1 ) );
                return rowCount;
            } catch ( XanbooException xe ) {
                throw new XanbooException(29040, "Failed to add rule space. Maximum size reached.");
            }
            
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[addRuleSpace()]: " + e.getMessage(), e);
            }else {
                logger.error("[addRuleSpace()]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[addRuleSpace]:" + e.getMessage());
        }
    }
    
    public int deleteRuleSpace( XanbooPrincipal xCaller, String gatewayGUID ) throws XanbooException{
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteRuleSpace()]:");
        }
        
        // validate the input parameters
        if(gatewayGUID==null || gatewayGUID.trim().equals("") ){
            throw new XanbooException(10050);
        }        
        
        try {
            
            // first validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            XanbooResultSet baseRow = dManager.getMObject( xCaller, gatewayGUID, "0", RULE_OID );
            int rowCount = baseRow.getElementInteger( 0, "PENDING_VALUE" );
            
            //Cannot make table size less than zero
            if ( rowCount < 1 ) {
                throw new XanbooException(29045, "Failed to delete rule space. Minimum size reached.");
            }
            
            // Do not allow rule space to delete if last rule in list is protected
            XanbooRule[] rules = getRules(xCaller, gatewayGUID);
            XanbooRule rule = rules[rowCount-1];
            if( rule != null && (!rule.isActionWritable() || !rule.isGuardWritable() || !rule.isNameWritable()) ){
                throw new XanbooException(29046, "Failed to delete rule space. Cannot delete read-only rule" );
            }
            
            //Try to decrease the size of the table.
            dManager.setMObject( xCaller, gatewayGUID, "0", RULE_OID, Integer.toString( rowCount - 1 ) );
            return rowCount;
            
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[deleteRuleSpace()]: " + e.getMessage(), e);
            }else {
                logger.error("[deleteRuleSpace()]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[deleteRuleSpace]:" + e.getMessage());
        }
    }
    
    public void execRule( XanbooPrincipal xCaller, String gatewayGUID, String ruleName ) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[execRule()]:");
        }

        // validate the input parameters
        if(gatewayGUID==null || gatewayGUID.trim().equals("") || ruleName==null || ruleName.trim().equals("")){
            throw new XanbooException(10050);
        }        
        
        try {
            long r = dManager.setMObject( xCaller, gatewayGUID, "0", EXEC_RULE_OID, ruleName );
            if( r == -1) throw new XanbooException(10050,"Invalid Gateway or Rule");
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[execRule()]: " + e.getMessage(), e);
            }else {
                logger.error("[execRule()]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[execRule]:" + e.getMessage());
        }        
    }
    
    public void execRule( XanbooPrincipal xCaller, String gatewayGUID, int ruleIndex ) throws XanbooException {
        execRule( xCaller, gatewayGUID, "@" + Integer.toString( ruleIndex ) );
    }
    
    public XanbooAccessKey[] getAccessKeys( XanbooPrincipal xCaller, String gatewayGUID ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getAccessKeys()]:");
        }
        
        // validate the input parameters
        if(gatewayGUID==null || gatewayGUID.trim().equals("") ){
            throw new XanbooException(10050);
        }
        
        try {
            
            // first validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            // retrieve mobject list for entire gateway via DeviceManagerEJB
            XanbooResultSet mobjects = dManager.getMObject( xCaller, gatewayGUID, "0", null );
            MObjectMap map = new MObjectMap( mobjects );
            
            int rowCount = Integer.parseInt( map.getValue( ACCESS_KEY_OID ) );

            ArrayList keys = new ArrayList();
            
            // parse result set into array of XanbooRule objects
            //keys = new XanbooAccessKey[ rowCount ];
            for ( int i=0; i<rowCount; i++ ) {
                try {
                    String oid = ACCESS_KEY_OID + "." + Integer.toString(i) + ".0";
                    String g = map.getPending( oid );
                    int p1 = g.indexOf( ACCESS_KEY_SEPARATOR );
                    int p2 = g.indexOf( ACCESS_KEY_SEPARATOR, p1+1 );
                    //rules[i] = new XanbooAccessKey( i, Integer.parseInt( g.substring( 0, 1 ) ), g.substring(2, pos), g.substring( pos + 1 ) );
                    
                    int status =  Integer.parseInt( g.substring( 0, 1 ) );
                    String keyId = g.substring( 2, p2 );
                    String name = g.substring( p2 +1 ) ;
                    
                    keys.add( new XanbooAccessKey( keyId, status, name ) );
                    
                    //keys[i] = new XanbooAccessKey( i, map.getPending( oid ) );
                } catch ( Exception e ) {
                    //Could be a NumberFormatExcepion, or a XanbooException - either way, there's not much we can do about it apart from create a blank rule.
                    //keys[i] = new XanbooAccessKey( i );
                }
            }
            
            return (XanbooAccessKey[]) keys.toArray(new XanbooAccessKey[0]);
            
        } catch ( java.lang.NumberFormatException nfe ) {
            //This happens when trying to determine rowCount for a gateway descriptor that does not support table objects.
            return null;
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[getAccessKeys()]: " + e.getMessage(), e);
            }else {
                logger.error("[getAccessKeys()]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[getAccessKeys]:" + e.getMessage());
        }        
    }
    
    public void updateAccessKey( XanbooPrincipal xCaller, String gatewayGUID, XanbooAccessKey key ) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[updateAccessKey()]:");
        }
        
        // validate the input parameters
        if(gatewayGUID==null || gatewayGUID.trim().equals("")){
            throw new XanbooException(10050);
        }        
        
        Connection conn = null;
        
        try {
            
            // first validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            //key.validate();
            conn=dao.getConnection();
            String oid = dao.getAccessKeyMobjectId( conn, xCaller, gatewayGUID, "0", "650.", key.getKeyId() );

            dManager.setMObject( xCaller, gatewayGUID, "0", oid, key == null ? "" : key.toString() );
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[updateAccessKey()]: " + e.getMessage(), e);
            }else {
                logger.error("[updateAccessKey()]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[updateAccessKey]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
    }    
    
    public void deleteAccessKey( XanbooPrincipal xCaller, String gatewayGUID, String keyId ) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteAccessKey()]:");
        }
        
        if(logger.isDebugEnabled()) {
            logger.debug("[updateAccessKey()]:");
        }
        
        // validate the input parameters
        if(gatewayGUID==null || gatewayGUID.trim().equals("")){
            throw new XanbooException(10050);
        }        
        
        Connection conn = null;
        
        try {
            
            // first validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            
            //key.validate();
            conn=dao.getConnection();
            String oid = dao.getAccessKeyMobjectId( conn, xCaller, gatewayGUID, "0", "650.", keyId );

            dManager.setMObject( xCaller, gatewayGUID, "0", oid, "" );
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[updateAccessKey()]: " + e.getMessage(), e);
            }else {
                logger.error("[updateAccessKey()]: " + e.getMessage());
            }
            throw new XanbooException(10030, "[updateAccessKey]:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
    }
    
}
