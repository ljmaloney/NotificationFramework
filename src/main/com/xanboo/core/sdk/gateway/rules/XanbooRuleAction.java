/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/gateway/rules/XanbooRuleAction.java,v $
 * $Id: XanbooRuleAction.java,v 1.11 2005/11/22 19:41:52 rking Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.gateway.rules;

import com.xanboo.core.util.XanbooException;

/**
 * An instance of XanbooRuleAction defines a device action which will take place during the execution of a {@link com.xanboo.core.sdk.gateway.rules.XanbooRule XanbooRule}.
 * <br><br>
 * An action specifies a value to assign to a managed object on a particular device (identified by a device GUID and a managed object ID).
 * See {@link com.xanboo.core.sdk.device.DeviceManager DeviceManager} for further details on GUIDs and managed objects.
 * <br><br>
 * Note that not all managed objects defined by a device are allowed to be used in rule actions. The 'public' attribute of mobject definitions within the descriptors
 * determine if a mobject may be referenced from an action or not. 
 * <br>
 * <br>
 * Refer to the SDK Programmer's Guide for details on all Rules and Action related topics.<BR>
 * @see com.xanboo.core.sdk.gateway.rules.XanbooRule XanbooRule
 */
public class XanbooRuleAction implements java.io.Serializable{
    
    /**
     * Holds value of property deviceGUID.
     */
    private String deviceGUID;
    
    /**
     * Holds value of property mobjectId.
     */
    private String mobjectId;
    
    /**
     * Holds value of property value.
     */
    private String value;

    /**
     * Holds value of property delay
     */
    private int delay;
    
    /** 
     * Creates a new instance of XanbooRuleAction with given parameters.
     *
     * @param deviceGUID identifies the device on which the action will take place.
     * @param mobjectId identifies the managed object on the device to set.
     * @param value the value to set to for the managed object.
     * @param delay the delay in seconds before the action executes
     */
    public XanbooRuleAction( String deviceGUID, String mobjectId, String value, int delay ) {
        this.deviceGUID = deviceGUID;
        this.mobjectId = mobjectId;
        this.value = value;
        this.delay = delay;
    }

    /** 
     * Creates a new instance of XanbooRuleAction with given parameters.
     *
     * @param deviceGUID identifies the device on which the action will take place.
     * @param mobjectId identifies the managed object on the device to set.
     * @param value the value to set to for the managed object.
     */
    public XanbooRuleAction( String deviceGUID, String mobjectId, String value ) {
        this.deviceGUID = deviceGUID;
        this.mobjectId = mobjectId;
        this.value = value;
        this.delay = 0;
    }    
    
    /*
    public static void main( String[] args ) {
        try {
 
            String[] actions = { "0.o1100='disarmed'", "0.o1100.d300='disarmed'" };
            
            for ( int i=0; i<actions.length; i++ ) {
                String a = actions[i];
                XanbooRuleAction action = new XanbooRuleAction( a );
                System.err.println("OLD: " + a);
                System.err.println("NEW: " + action.toString() );
                System.err.println("TEST: " + a.equals( action.toString() ));
                System.err.println( action.getDelay() );
            }
            
            
        } catch ( Exception e ) {
            e.printStackTrace();
        } 
    }//*
    
    /** Creates a new instance of XanbooRuleAction from internal mobject oid string representation.
     * <br>This method is used internally and should be avoided by SDK developers.
     */
    protected XanbooRuleAction( String action ) throws XanbooException {
        int p1 = action.indexOf( ".o" );
        int p2 = action.indexOf( ".d", p1 );
        int p3 = action.indexOf( "=" );
        
        if ( p1 == -1 || p1 == -1 ) {
            throw new XanbooException( 29010, "Invalid rule action specification" );
        }
        if ( p2 == -1 ) {
            deviceGUID = action.substring( 0, p1 );
            mobjectId = action.substring( p1+2, p3 );
            value = action.substring( p3+2, action.lastIndexOf( "'" ) );
        } else {
            deviceGUID = action.substring( 0, p1 );
            mobjectId = action.substring( p1+2, p2 );
            delay = Integer.parseInt( action.substring( p2+2, p3 ) );
            value = action.substring( p3+2, action.lastIndexOf( "'" ) );
        }
    }
    
    /**
     * Returns the device Id (GUID) of this action.
     */
    public String getDeviceGUID() {
        return this.deviceGUID;
    }
    
    /**
     * Returns the managed object ID of this action.
     */
    public String getMobjectId() {
        return this.mobjectId;
    }
    
    /**
     * Returns the value which is to be assigned to the managed object of this action.
     */
    public String getValue() {
        return this.value;
    }

    public int getDelay() {
        return this.delay;
    }
    
    public void setDelay( int d ) {
        this.delay = d;
    }
    
    /** Returns internal mobject oid string representation.
     * <br>This method is used internally and should be avoided by SDK developers.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append( this.deviceGUID );
        sb.append( ".o" );
        sb.append( this.mobjectId );
        if ( this.delay > 0 ) {
            sb.append( ".d" ).append( this.delay );
        }
        sb.append( "='");
        sb.append( this.value );
        sb.append( "'" );
        return sb.toString();
    }

    /**
     * Validates this action for correct format and completeness
     * 
     * @throws XanbooException if an error was found
     */
    public void validate() throws XanbooException {
        if ( this.deviceGUID == null || this.mobjectId == null || this.value == null ) {
            throw new XanbooException( 29010 );
        }
    }
    
    public boolean equals( XanbooRuleAction a ) {
        if ( a == null ) return false;
        return( a.getDelay() == this.getDelay() && a.getDeviceGUID().equals(this.getDeviceGUID()) && a.getMobjectId().equals(this.getMobjectId()) && a.getValue().equals(this.getValue()) );
    }
    
}
