/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/gateway/GatewayManager.java,v $
 * $Id: GatewayManager.java,v 1.10 2004/07/12 16:04:01 rking Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.gateway;

import java.rmi.RemoteException;

import com.xanboo.core.model.XanbooAccessKey;
import com.xanboo.core.sdk.gateway.rules.XanbooRule;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.XanbooException;



/**
 * <p>Remote Interface for the GatewayManager EJB</p>
 * <p>
 * Provides methods to manipulate gateway related data, such as device rule configuration.
 * <br><br>
 * XanbooExceptions thrown by the GatewayManager use SDK error codes 29XXX.
 * </p>
 *
 * @see com.xanboo.core.sdk.device.DeviceManager DeviceManager
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRule XanbooRule
 */

public interface GatewayManager   {

    /**
     * Retrieve the Rules Table entries associated with a gateway. The table is returned as
     * an array of XanbooRUke objects,
     * 
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID gateway identifier GUID to retrieve the table for
     *
     * @throws XanbooException if an error occurred
     *
     * @return XanbooRule[] array of rules associated with this gateway
     */
    public XanbooRule[] getRules( XanbooPrincipal xp, String gatewayGUID ) throws XanbooException, RemoteException;
    
    /**
     * Updates an existing or adds a new device rule entry in a Gateway Rules Table.
     * 
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID gateway identifier GUID for the Rules Table
     * @param ruleIndex Table rules index for the rule to be updates
     * @param XanbooRule a rule object containing the updated values for the rule.
     *
     * @throws XanbooException if an error occurred
     *
     */    
    public void updateRule( XanbooPrincipal xCaller, String gatewayGUID, int ruleIndex, XanbooRule rule ) throws XanbooException, RemoteException;

    /**
     * Deletes an existing device rule entry in a Gateway Rules Table. This call does NOT affect the size of the
     * Rules table, it merely blanks out the rule entry.
     * 
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID gateway identifier GUID for the Rules Table
     * @param ruleIndex Table rules index for the rule to be deleted
     *
     * @throws XanbooException if an error occurred
     *
     */    
    public void deleteRule( XanbooPrincipal xCaller, String gatewayGUID, int ruleIndex ) throws XanbooException, RemoteException;
    
    /**
     * Increases the size of a Gateway Rules Table by one. This call is used when a new rule needs to be added to the
     * Rules table and existing rule entries are all in use. Note that there is a Gateway specific upper limit for the
     * Rules table size. Refer to Gateway descriptor PolicyCaps OID description or the SDK Programmer's Guide for details.
     * 
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID gateway identifier GUID for the Rules Table
     *
     * @throws XanbooException if an error occurred, often when the max table size has been reached.
     *
     * @return int the rule index of the newly added rule entry.
     */    
    public int addRuleSpace( XanbooPrincipal xCaller, String gatewayGUID ) throws XanbooException, RemoteException;
    
    
    /**
     * Decreases the size of a Gateway Rules Table by one. This call is used when there are too many unused rule entries
     * in the Rules table. Reducing the size of Rules table may improve Gateway Rules processing.
     * 
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID gateway identifier GUID for the Rules Table
     *
     * @throws XanbooException if an error occurred, often when the min table size has been reached.
     *
     * @return int the reduced, new size of the rules table
     */
    public int deleteRuleSpace( XanbooPrincipal xCaller, String gatewayGUID ) throws XanbooException, RemoteException;
    
    /**
     * Instructs the Gateway for manual, on demand execution of a rule by its name. The Gateway immediately executes the
     * requested rule's action list skipping its Guard list checks. Refer to the SDK Programmer's Guide for details.
     * 
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID gateway identifier GUID for the Rules Table
     * @param name The name of the rule to execute. A valid name within the table is required.
     *
     * @throws XanbooException if an error occurred
     *
     */
    public void execRule( XanbooPrincipal xCaller, String gatewayGUID, String ruleName ) throws XanbooException, RemoteException;
    
    /**
     * Instructs the Gateway for manual, on demand execution of a rule by its rule index. The Gateway immediately executes the
     * requested rule's action list skipping its Guard list checks. Refer to the SDK Programmer's Guide for details.
     * 
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID gateway identifier GUID for the Rules Table
     * @param ruleIndex Table rules index for the rule to be executed.
     *
     * @throws XanbooException if an error occurred
     *
     */
    public void execRule( XanbooPrincipal xCaller, String gatewayGUID, int ruleIndex ) throws XanbooException, RemoteException;
              
    /**
     * Retrieves a list of access keys associated with a gateway.
     * <br>
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID gateway identifier GUID for the access key
     *
     * @throws XanbooException if an error occurred
     */
    public XanbooAccessKey[] getAccessKeys( XanbooPrincipal xCaller, String gatewayGUID ) throws XanbooException, RemoteException ;
    
    /**
     * Updates an existing access key for a particular gateway.
     * <br>
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID gateway identifier GUID for the access key
     * @param key The key to update, identified by it's keyId property.
     *
     * @throws XanbooException if an error occurred
     */
    public void updateAccessKey( XanbooPrincipal xCaller, String gatewayGUID, XanbooAccessKey key ) throws XanbooException, RemoteException ;

    /**
     * Deletes an access key's entry from a gateway.
     * <br>
     *
     * @param xCaller a XanbooPrincipal object that identifies the caller
     * @param gatewayGUID gateway identifier GUID for the access key table
     * @param keyId The ID of the key to delete
     *
     * @throws XanbooException if an error occurred
     *
     */    
    public void deleteAccessKey( XanbooPrincipal xCaller, String gatewayGUID, String keyId ) throws XanbooException, RemoteException ;
}
