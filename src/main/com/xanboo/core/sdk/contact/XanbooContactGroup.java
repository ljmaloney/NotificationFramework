/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/contact/XanbooContactGroup.java,v $
 * $Id: XanbooContactGroup.java,v 1.1.1.1 2002/05/06 19:17:02 rking Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.contact;


/**
 * Class to represent Xanboo contact group information for contacts
 */
public class XanbooContactGroup implements java.io.Serializable {
    
    private long accountId;     // account id
    private long userId;        // user that owns the contact
    private long cgroupId;       // contact group identfifier
        
    private String cgroupName;
    private String description;
    
    /**
     * Default constructor: Creates a blank contact group record.
     */
    public XanbooContactGroup() {
        this.accountId=-1L;
        this.userId=-1L;
        this.cgroupId=-1L;
        
        this.cgroupName="";
        this.description="";
    }
    
    /**
     * validates a contact group information within the Xanboo system
     * 
     * @return true if contact group information in XanbooContact were valid, or false if not valid
     */
    public boolean isValid() { 
         //.. short circuit..ok
         if ((this.accountId <= 0L) || 
            (this.userId <= 0L) || 
            (this.cgroupName.trim().equals(""))) {    
            return false;  // invalid contact group iformation
          }
          return true;   // valid contact group information
    }
    
    // getters
    
    /** Gets the account id for the contact group */
    public long getAccountId() { return this.accountId; }
    
    /** Gets the user id for the contact group */
    public long getUserId() { return this.userId; }

    /** Gets the contact group id for the contact gorup object */
    public long getCGroupId() { return this.cgroupId; }
    
    /** Gets contact group name */
    public String getCGroupName() { return this.cgroupName; }
    
    /** Gets contact group description */
    public String getDescription() { return this.description; }

    // setters 
    
    /** Sets contact group account id */
    public void setAccountId(long accountId) { this.accountId=accountId; }
    
    /** Sets contact group user id */
    public void setUserId(long userId) { this.userId=userId; }
    
    /** Sets contact group identifier */
    public void setCGroupId(long cgroupId) { this.cgroupId=cgroupId; }
    
    /** Sets contact group name */
    public void setCGroupName(String cgroupName) { this.cgroupName=cgroupName; }

    /** Sets contact group description */
    public void setDescription(String description) { this.description=description; }
 
    /** Dumps the content of the object to stderr */
    public void dump() {
        System.err.println("ACCOUNT ID   :" + this.accountId);
        System.err.println("USER ID      :" + this.userId);
        System.err.println("CGROUP ID   :" + this.cgroupId);
        
        System.err.println("GROUP NAME   :" + this.cgroupName);
        System.err.println("DESCRIPTION  :" + this.description);
    }
    
}


