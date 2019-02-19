/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/account/XanbooAccount.java,v $
 * $Id: XanbooAccount.java,v 1.8 2010/10/08 18:24:59 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.account;

import java.util.*;
import com.xanboo.core.sdk.util.XanbooResultSet;

/**
 * Class to represent Xanboo Accounts
 */
public class XanbooAccount implements java.io.Serializable {
    private static final long serialVersionUID = 7880728482928061375L;
    
    /** Constant value to indicate account domain is defaulted (xanboo) */
    public static final String DEFAULT_DOMAIN="xanboo";

    /** Constant value to indicate account is not to be changed or defaulted */
    public static final int STATUS_UNCHANGED=-1;
    
    /** Constant value to indicate account is in 'Inactive/Preregister' status */
    public static final int STATUS_INACTIVE=0;

    /** Constant value to indicate account is in 'Active' status */
    public static final int STATUS_ACTIVE=1;

    /** Constant value to indicate account is in 'Disabled/Suspended' status */
    public static final int STATUS_DISABLED=2;

    /** Constant value to indicate account is in 'Cancelled' status */
    public static final int STATUS_CANCELLED=3;
    
    /** Constant value to indicate account type is not to be changed/updated or defaulted */
    public static final int TYPE_UNCHANGED=-1;
    
    /* Account type constant for regular, commercial accounts (default) */
    public static final int TYPE_REGULAR=0;
    
    /* Account type constant for non-paying, demo accounts */
    public static final int TYPE_DEMO=1;

    public static final int TYPE_BETA=2;
    
    /** Constant value to indicate account device provisioning override is not to be changed/updated or defaulted */
    public static final int PROVISIONING_UNCHANGED=-1;
    
    /** Constant value to indicate account device provisioning is disabled */
    public static final int PROVISIONING_DISABLED=0;
    
    /** Constant value to indicate account device provisioning is enabled */
    public static final int PROVISIONING_ENABLED=1;

    /** Constant value to indicate account device provisioning override does not exist and domain level flags will be used*/
    public static final int PROVISIONING_NOOVERRIDE=2;
    
    
    /** Holds account id value */
    private long accountId;
    
    /** Holds account domain id. */
    private String domainId;
    
    /** Holds account type. Supported values are TYPE_REGULAR and TYPE_DEMO. For update operations TYPE_UNCHANGED value is allowed too */
    private int type;
    
    /** Holds account status id. Supported values are STATUS_ACTIVE, STATUS_INACTIVE, STATUS_DISABLED, and STATUS_CANCELLED. For update operations STATUS_UNCHANGED value is allowed too */
    private int statusId;
    
    /** Holds an external account id for the account. */
    private String extAccountId;
    
    /** Holds the current number of items in the account inbox */
    private int inboxCount;
    
    /** Holds the current number of items in the account wastebasket */
    private int trashCount;
    
    /* Holds a reference to an account  XanbooUser object (typically account master user) */
    private XanbooUser xUser;
   
    /** Holds account token. */
    private String token;

    /** Holds account fifo purgin preference. */
    private int fifoPurging;
   
    /** Holds account device provisioning flag for self-install.  Supported values are PROVISIONING_ENABLED, PROVISIONING_DISABLED, and PROVISIONING_NOOVERRIDE. For update operations PROVISIONING_UNCHANGED value is allowed too */
    private int selfInstallProvisioningFlag;

    
    
    /** Default constructor: Creates new XanbooAccount */
    public XanbooAccount() {
        this.accountId=-1L;
        this.domainId=XanbooAccount.DEFAULT_DOMAIN;
        this.type=TYPE_UNCHANGED;
        this.statusId=XanbooAccount.STATUS_ACTIVE;
        this.extAccountId="";
        this.inboxCount=0;
        this.trashCount=0;
        this.xUser=null;
        this.token=null;
        this.fifoPurging=-1;
        this.selfInstallProvisioningFlag=TYPE_UNCHANGED;
    }
    
    
     /** Creates a new Xanboo Account from given parameters 
      * @param accountId    account id
      * @param domainId     account domain
      * @param statusId     account status
      * @param extAccountId an external account id for the account
      * @param inboxCount  number of items in the account inbox
      * @param trashCount   number of items in the account wastebasket
      */
    public XanbooAccount(long accountId, String domainId, int statusId, String extAccountId,
                            int inboxCount, int trashCount) {
        this.accountId=accountId;
        this.domainId=domainId;
        this.type=TYPE_UNCHANGED;
        this.statusId=statusId;
        this.extAccountId=extAccountId;
        this.inboxCount=inboxCount;
        this.trashCount=trashCount;
        this.xUser=null;
        this.selfInstallProvisioningFlag=TYPE_UNCHANGED;
    }

     /** Creates a new Xanboo Account from given parameters 
      * @param accountId    account id
      * @param domainId     account domain
      * @param statusId     account status
      * @param token a gateway installation token
      */
    public XanbooAccount(long accountId, String domainId, int statusId, String token) {
        this.accountId=accountId;
        this.domainId=domainId;
        this.type=TYPE_UNCHANGED;
        this.statusId=statusId;
        this.extAccountId=null;
        this.inboxCount=0;
        this.trashCount=0;
        this.xUser=null;
        this.token=token;
        this.fifoPurging=-1;
        this.selfInstallProvisioningFlag=TYPE_UNCHANGED;
    }

     /** Creates a new Xanboo Account from a XanbooResultSet object and given row 
      * @param xrs a XanbooResulSet object containing account query results from an SDK call
      * @param row row number to construct the object from. Must be a >=0 value and cannot be bigger than resulset size
      */
    public XanbooAccount(XanbooResultSet xrs, int row) throws InstantiationException {
        if(xrs==null || row<0 || row>=xrs.size()) {
            throw new InstantiationException("Invalid resultset or row");
        }
        
        this.accountId=xrs.getElementLong(row, "ACCOUNT_ID");
        this.domainId=xrs.getElementString(row, "DOMAIN_ID");
        this.type=xrs.getElementInteger(row, "TYPE");
        this.statusId=xrs.getElementInteger(row, "STATUS_ID");
        this.extAccountId=xrs.getElementString(row, "EXT_ACCOUNT_ID");
        this.inboxCount=xrs.getElementInteger(row, "INBOX_COUNT");
        this.trashCount=xrs.getElementInteger(row, "TRASH_COUNT");
        this.xUser=null;
        this.token=xrs.getElementString(row, "TOKEN");
        this.fifoPurging=xrs.getElementInteger(row, "ENABLE_FIFO_PURGE");
        this.selfInstallProvisioningFlag=xrs.getElementInteger(row, "SELF_PROVISIONING_OVERRIDE");
    }
    
    
    /** Gets the account id.
     * @return account id value.
     */
    public long getAccountId() {
        return this.accountId;
    }
    
    /** Sets the account id.
     * @param accountId account id value to set.
     */
    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }
    
    /** Gets the domain id of the account.
     * @return account domain id.
     */
    public String getDomain() {
        return this.domainId;
    }
    
    /** Sets the domain id for the account.
     * @param domainId New value of property domainId.
     */
    public void setDomain(String domainId) {
        this.domainId = domainId;
    }
    
    
    /** Gets the account type.
     * @return account type (0:regular, 1:demo, 3:beta)
     */
    public int getType() {
        return this.type;
    }
    
    /** Sets the account type.
     * @param type account type value to set (TYPE_REGULAR:regular, TYPE_DEMO:demo, TYPE_BETA: beta,TYPE_UNCHANGED:no change ). If set to TYPE_UNCHANGED, the flag will not be updated in related updateAccount calls.
     */
    public void setType(int type) {
        if(type==TYPE_UNCHANGED || type==TYPE_REGULAR || type==TYPE_DEMO || type==TYPE_BETA || type==(TYPE_DEMO|TYPE_BETA)) {
            this.type = type;
        }
    }
    
    /** Returns if the account is of demo type or not
     * @return true if account is demo type, false otherwise
     */
    public boolean isDemo() {
        return (this.type & TYPE_DEMO)==TYPE_DEMO ? true : false;
    }

    /** Returns if the account is of beta type or not
     * @return true if account is beta type, false otherwise
     */
    public boolean isBeta() {
        return (this.type & TYPE_BETA)==TYPE_BETA ? true : false;
    }

    
    /** Gets the account status.
     * @return statusId value of account status. Possible values are XanbooAccount.STATUS_INACTIVE and XanbooAccount.STATUS_ACTIVE
     */
    public int getStatus() {
        return statusId;
    }
    
    /** Sets the account status.
     * @param statusId status value to set. Possible values are XanbooAccount.STATUS_INACTIVE and XanbooAccount.STATUS_ACTIVE
     */
    public void setStatus(int statusId) {
        this.statusId = statusId;
    }
    
    /** Gets the external account id associated with the account.
     * @return the external account id.
     */
    public String getExtAccountId() {
        return extAccountId;
    }
    
    /** Sets the external account id associated with the account.
     * @param extAccountId the external account id value.
     */
    public void setExtAccountId(String extAccountId) {
        this.extAccountId = extAccountId;
    }
    
    /** Gets the number of items in account inbox.
     * @return inbox item count.
     */
    public int getInboxCount() {
        return inboxCount;
    }
    
    /** Sets the number of items in account inbox.
     * @param inboxCount value to set the number of inbox items.
     */
    public void setInboxCount(int inboxCount) {
        this.inboxCount = inboxCount;
    }
        
    /** Gets the number of items in account wastebasket.
     * @return wastebasket item count.
     */
    public int getTrashCount() {
        return trashCount;
    }
    
    /** Sets the number of items in account wastebasket.
     * @param trashCount value to set the number of trash items.
     */
    public void setTrashCount(int trashCount) {
        this.trashCount = trashCount;
    }
    
    /** Gets the Xanboo user associated with the account.
     * @return xUser a XanbooUser object reference.
     */
    public XanbooUser getUser() {
        return this.xUser;
    }

    /** Gets the Xanboo user associated with the account.
     * @return xUser a XanbooUser object reference.
     */
    public void setUser(XanbooUser xUser) {
        this.xUser=xUser;
    }
    
    /** Getter for property token.
     * @return Value of property token.
     */
    public String getToken() {
        return token;
    }    
    
    /** Setter for property token.
     * @param token New value of property token.
     */
    public void setToken(String token) {
        this.token = token;
    }


    /** Gets the account fifo purging preference.
     * @return flag fifo purging preference flag (0:disabled, 1:enabled)
     */
    public int getFifoPurgingFlag() {
        return this.fifoPurging;
    }

    /** Sets the account fifo purging preference.
     * @param flag fifo purging flag (0:disabled, 1:enabled)
     */
    public void setFifoPurgingFlag(int fifoPurging) {
        if(fifoPurging>=0 && fifoPurging<=1)  //only 0 and 1 allowed for now to disable/enable purging
            this.fifoPurging = fifoPurging;
        else
            this.fifoPurging = -1;
    }

    
    /** Gets the self install provisioning flag override for the account
     * @return the flag value (PROVISIONING_UNCHANGED:no change, PROVISIONING_DISABLED:disabled, PROVISIONING_ENABLED:enabled, PROVISIONING_NOOVERRIDE:no override,use domain default).
     */
    public int getSelfInstallProvisioningFlag() {
        return selfInstallProvisioningFlag;
    }
    
    /** Sets the self install provisioning flag override for the account
     * @param flag value (PROVISIONING_UNCHANGED:no change, PROVISIONING_DISABLED:disabled, PROVISIONING_ENABLED:enabled, PROVISIONING_NOOVERRIDE:no override,use domain default). If set to PROVISIONING_UNCHANGED, the flag will not be updated in related updateAccount calls.
     */
    public void setSelfInstallProvisioningFlag(int flag) {
        if(flag==PROVISIONING_UNCHANGED || flag==PROVISIONING_DISABLED ||flag==PROVISIONING_ENABLED ||flag==PROVISIONING_NOOVERRIDE) {
            this.selfInstallProvisioningFlag = flag;
        }
    }
    
    
    public String toString() {
        return "ACCID:" + this.accountId + ", DOMAIN:" + this.domainId;
    }

}
