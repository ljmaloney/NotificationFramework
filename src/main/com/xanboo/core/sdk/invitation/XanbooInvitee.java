/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/invitation/XanbooInvitee.java,v $
 * $Id: XanbooInvitee.java,v 1.2 2003/01/09 18:34:04 levent Exp $
 * 
 * Copyright Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.invitation;

import java.util.Date;


/**
 * Class to represent Xanboo Folder/Item Invitees
 */
public class XanbooInvitee implements java.io.Serializable {
    private static final long serialVersionUID = -2057712892918708584L;

    /** Holds value of property invitationId. */
    private long invitationId;
    
    /** Holds value of property groupId. */
    private long groupId;
    
    /** Holds value of property contactId. */
    private long contactId;
    
    /** Holds value of property toAddress. */
    private String toAddress;
    
    /** Holds value of property viewKey. */
    private String viewKey;
    
    private long inviteeId;
    
    /** 0 - pending (message will send) 1- sent( will not be processed) */
    private int sentStatus;
    
    private Date lastViewed;
    
    
    /** Creates new XanbooInvitee */
    public XanbooInvitee() {
        this.toAddress = "";
        this.contactId = 0;
        this.groupId = 0;

    }

    /** Getter for property invitationId.
     * @return Value of property invitationId.
     */
    public long getInvitationId() {
        return invitationId;
    }
    
    /** Setter for property invitationId.
     * @param invitationId New value of property invitationId.
     */
    public void setInvitationId(long invitationId) {
        this.invitationId = invitationId;
    }
    
    /** Getter for property groupId.
     * @return Value of property groupId.
     */
    public long getGroupId() {
        return groupId;
    }
    
    /** Setter for property groupId.
     * @param groupId New value of property groupId.
     */
    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }
    
    /** Getter for property contactId.
     * @return Value of property contactId.
     */
    public long getContactId() {
        return contactId;
    }
    
    /** Setter for property contactId.
     * @param contactId New value of property contactId.
     */
    public void setContactId(long contactId) {
        this.contactId = contactId;
    }
    
    /** Getter for property viewKey.
     * @return Value of property viewKey.
     */
    public String getViewKey() {
        return viewKey;
    }
    
    /** Setter for property viewKey.
     * @param viewKey New value of property viewKey.
     */
    public void setViewKey(String viewKey) {
        this.viewKey = viewKey;
    }

	/**
	 * @return the toAddress
	 */
	public String getToAddress() {
		return toAddress;
	}

	/**
	 * @param toAddress the toAddress to set
	 */
	public void setToAddress(String toAddress) {
		this.toAddress = toAddress;
	}

	/**
	 * @return the inviteeId
	 */
	public long getInviteeId() {
		return inviteeId;
	}

	/**
	 * @param inviteeId the inviteeId to set
	 */
	public void setInviteeId(long inviteeId) {
		this.inviteeId = inviteeId;
	}

	/**
	 * @return the sentStatus
	 */
	public int getSentStatus() {
		return sentStatus;
	}

	/**
	 * @param sentStatus the sentStatus to set
	 */
	public void setSentStatus(int sentStatus) {
		this.sentStatus = sentStatus;
	}

	/**
	 * @return the lastViewed
	 */
	public Date getLastViewed() {
		return lastViewed;
	}

	/**
	 * @param lastViewed the lastViewed to set
	 */
	public void setLastViewed(Date lastViewed) {
		this.lastViewed = lastViewed;
	}
	
}