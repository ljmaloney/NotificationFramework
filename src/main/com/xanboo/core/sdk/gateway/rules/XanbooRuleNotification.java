/*
 * XanbooRuleNotification.java
 *
 * Created on November 21, 2005, 11:01 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.xanboo.core.sdk.gateway.rules;

/**
 *
 * @author rking
 */
public class XanbooRuleNotification implements java.io.Serializable{
    
    /** Creates a new instance of XanbooRuleNotification */
    public XanbooRuleNotification( int type, String address, boolean attach) {
        this.type = type;
        this.address = address;
        this.attach = attach;
    }

    /** Creates a new instance of XanbooRuleNotification */
    public XanbooRuleNotification( String address, boolean attach ) {
        
        int pos = address.indexOf( "%" );
        if( pos != -1 ) {
            setType( Integer.parseInt( address.substring( 0, pos ) ) );
            setAddress( address.substring( pos+1 ) );
        }else {
            setType( 0 );
            setAddress( address );
        }
        
        setAttach( attach );
        
    }
    
    protected XanbooRuleNotification( String not ) {
        
        setAttach( not.startsWith( "_na" ) );
        int pos1 = not.indexOf( "'" );
        int pos2 = not.indexOf( "%" );
        setType( Integer.parseInt( not.substring( pos1+1, pos2 ) ) );
        setAddress( not.substring( pos2+1, not.length()-1) );
        
    }

    /**
     * Holds value of property type.
     */
    private int type;

    /**
     * Getter for property type.
     * @return Value of property type.
     */
    public int getType() {
        return this.type;
    }

    /**
     * Setter for property type.
     * @param type New value of property type.
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * Holds value of property address.
     */
    private String address;

    /**
     * Getter for property address.
     * @return Value of property address.
     */
    public String getAddress() {
        return this.address;
    }

    /**
     * Setter for property address.
     * @param address New value of property address.
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Holds value of property attach.
     */
    private boolean attach;

    /**
     * Getter for property attach.
     * @return Value of property attach.
     */
    public boolean isAttach() {
        return this.attach;
    }

    /**
     * Setter for property attach.
     * @param attach New value of property attach.
     */
    public void setAttach(boolean attach) {
        this.attach = attach;
    }
    
    public String toString() {
        return address + " [" + type + "]" + (attach ? " + attach" : "");
    }
    
    public boolean equals( XanbooRuleNotification a ) {
        if ( a == null ) return false;
        return ( a.getType() == this.getType() && a.getAddress().equals(this.getAddress()) );
    }
    
}
