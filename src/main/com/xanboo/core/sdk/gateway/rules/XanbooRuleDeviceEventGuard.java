/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/gateway/rules/XanbooRuleDeviceEventGuard.java,v $
 * $Id: XanbooRuleDeviceEventGuard.java,v 1.8 2005/02/08 17:44:04 rking Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.gateway.rules;

import com.xanboo.core.util.XanbooException;

/**
 * A XanbooRuleEventGuard defines a device event guard upon which execution of XanbooRule is triggered.<br>
 * E.g. a XanbooRuleEventGuard could specify 'when motion detected on camera 1'.
 * <br>
 * A XanbooRuleEventGuard defines the following properties:
 * <ul>
 *  <li><b>Device Id (GUID)</b><br>
 *      Identifies the source device to generate the event.
 *  </li>
 *  <li><b>Event Id</b><br>
 *      Identifies the event that will trigger this guard.
 *  </li>
 * </ul>
 *
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleGuard XanbooRuleGuard
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRuleEventGuard XanbooRuleEventGuard
 */
public class XanbooRuleDeviceEventGuard extends XanbooRuleEventGuard {
    
    
    /**
     * Holds value of property deviceGUID.
     */
    private String deviceGUID;
    
    /**
     * Holds value of property eventId.
     */
    private String eventId;
    
    private static final String[] SEPARATORS = {".E", ".!E"};
    
    private boolean whenDisarmed;
    
    /** 
     * Creates a new instance of XanbooRuleEventGuard with given parameters.
     *
     * @param deviceGUID Identifies the id of the device that will generate the event. The value needs to be either a valid deviceGUID for the gateway, or
     *        XanbooRule.ANY_DEVICE constant to indicate an any device trigger
     * @param eventId Identifies the event which will trigger the guard. The value needs to be either a valid eID for the device specified, or
     *        XanbooRule.ANY_EVENT constant to indicate an any event trigger
     * 
     * Note XanbooRule.ANY_DEVICE and XanbooRule.ANY_EVENT constants canNOt be combined together as a device guard event trigger.
     */ 
    public XanbooRuleDeviceEventGuard(String deviceGUID, String eventId) {
        this.deviceGUID = deviceGUID;
        this.eventId = eventId;
        this.whenDisarmed = false;
    }
    
    /**
     * Method to return a XanbooGuard object from an internal mobject oid string representation.
     * <br>This method is used internally and should be avoided by SDK developers !!!
     **/
    protected XanbooRuleDeviceEventGuard(String guardString) {
        String guard = guardString.toUpperCase();
        
        for ( int i=0; i<SEPARATORS.length; i++ ) {
            int p1 = guard.indexOf( SEPARATORS[i] );
            if ( p1 != -1 ) {
                this.deviceGUID = guardString.substring( 0, p1 );
                this.eventId = guardString.substring( p1 + SEPARATORS[i].length() );//preserve case of device ID
                this.whenDisarmed = i==1; // second SEPARATOR (!e) is for when disarmed.
                break;
            }
        }
        
    }
    
    /**
     * Returns the source device ID for the event, configured with this guard.
     * @return deviceGUID device identifier.
     */
    public String getDeviceGUID() {
        return this.deviceGUID;
    }
    
    
    /**
     * Returns the ID of the event, configured with this guard.
     * @return eventId event identifier.
     */
    public String getEventId() {
        return this.eventId;
    }
    
    /** 
     * Returns string representation of this guard.
     * <br>This method is used internally and should be avoided by SDK developers !!!
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        
        boolean subGuards = ( this.guardArray != null && this.guardArray.size() > 0 );

        // Append the operator
        sb.append( super.toString() );
        
        if ( subGuards ) {
            sb.append( "(" );
        }
        
        // Append the device event string
        sb.append( this.deviceGUID ).append( whenDisarmed?".!e":".e" ).append( this.eventId );

        // Append sub-guards
        if ( subGuards ) {
            for ( int i=0; i<this.guardArray.size(); i++ ) {
                XanbooRuleGuard g = (XanbooRuleGuard) guardArray.get( i );
                sb.append( g.toString() );
            }
            sb.append( ")" );
        }
        
        return sb.toString();
    }

    /**
     * Validates this guard for correct format and completeness
     * 
     * @throws XanbooException if an error was found
     */
    public void validate() throws XanbooException {
        if(this.deviceGUID==null || this.eventId==null) {
            throw new XanbooException( 29055 );
        }
        
        // any event on any device trigger combination not allowed
        if(this.deviceGUID.trim().equalsIgnoreCase(XanbooRule.ANY_DEVICE) && this.eventId.trim().equalsIgnoreCase(XanbooRule.ANY_EVENT)) {
            throw new XanbooException( 29056 );
        }
        
    }
    
    /**
     * Sets source device Id for the guard.
     *
     * @param deviceGUID source device identifier
     */
    public void setDeviceGUID(String deviceGUID) {
        this.deviceGUID = deviceGUID;
    }
    
    /**
     * Sets the event for the guard.
     *
     * @param eventId id of the event associated with the device.
     */
    public void setEventId( String eventId ) {
        this.eventId = eventId;
    }

    /**
     * Returns whether this guard triggers even when the device is disarmed.
     * Defaults to false.
     */
    public boolean isWhenDisarmed() {
        return whenDisarmed;
    }
    
    /**
     * Sets whether this guard triggers even when the device is disarmed
     * Defaults to false.
     */
    public void setWhenDisarmed( boolean whenDisarmed ) {
        this.whenDisarmed = whenDisarmed;
    }
}
