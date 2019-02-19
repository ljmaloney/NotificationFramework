/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/invitation/XanbooGuestPrincipal.java,v $
 * $Id: XanbooGuestPrincipal.java,v 1.3 2007/11/19 22:50:36 levent Exp $
 *
 * Copyright Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.invitation;

/**
 * Security principal used to identify the recipient of an invitation.
 * Allows retrieval of resources shared out by the invitation.
 */

public class XanbooGuestPrincipal implements java.io.Serializable{
    private static final long serialVersionUID = -401109320220343809L;

    protected long accountId;
    protected long userId;
    protected String domain;
    protected long inviteeId;
    protected String viewKey;
    protected long invitationId;
    private long objectType;
    private short type;
    private long objectId;
    private String msg;
    private String subject;
    String gatewayGuid;
    String deviceGuid;
    
    /** Creates a new instance of XanbooGuestPrincipal */
    protected XanbooGuestPrincipal( String viewKey, long invitationId, long inviteeId, short type, long objectId ) {
        this.viewKey = viewKey;
        this.invitationId = invitationId;
        this.inviteeId = inviteeId;
        this.type = type;
        this.objectId= objectId;
    }

    /** Creates a new instance of XanbooGuestPrincipal */
    protected XanbooGuestPrincipal( String viewKey, long invitationId, long inviteeId, short type, String gatewayGuid, String deviceGuid ) {
        this.viewKey = viewKey;
        this.invitationId = invitationId;
        this.inviteeId = inviteeId;
        this.type = type;
        this.gatewayGuid = gatewayGuid;
        this.deviceGuid = deviceGuid;
    }

    /**
     * Returns the id of the device that this guest is allowed to view
     */
    public String getDeviceGuid() {
        return this.deviceGuid;
    }
    
    /**
     * Returns the id of the gateway that owns the device that this user is allowed to view
     */
    public String getGatewayGuid() {
        return this.gatewayGuid;
    }
    
    public long getAccountId() {
        return this.accountId;
    }
    public String getDomain() {
        return this.domain;
    }

    public long getInvitationId() {
        return this.invitationId;
    }
    public short getInvitationType() {
        return this.type;
    }
    public String getInvitationKey() {
        return this.viewKey;
    }
    public long getInvitationObjectId() {
        return this.objectId;
    }
    
    
    /**
     * Returns a brief description of the guest principal.
     * Exact details of the format are unspecified and subject to change.
     */
    public String toString() {
       return "XanbooGuestPrincipal: " + userId + "/" + accountId + "@" + domain + " "+ viewKey + ":" + inviteeId + ":" + gatewayGuid + ":" + deviceGuid; 
    }
    
    public String getMessage() {
        return this.msg;
    }
    
    public String getSubject() {
        return this.subject;
    }
    
    public void setMessage( String message ) {
        this.msg = message;
    }
    
    public void setSubject( String subject ) {
        this.subject = subject;
    }
}
