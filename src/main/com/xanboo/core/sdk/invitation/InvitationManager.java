/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/invitation/InvitationManager.java,v $
 * $Id: InvitationManager.java,v 1.7 2007/11/19 22:44:35 levent Exp $
 * 
 * Copyright Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.invitation;

import java.rmi.RemoteException;

import com.xanboo.core.model.XanbooGateway;
import com.xanboo.core.model.XanbooItem;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.XanbooException;

/**
 * <p>Remote Interface for the InvitationManagerEJB</p>
 *
 *
 * <b> Glossary </b>
 * <ul>
 *  <li>
 *  <b>Invitation</b> <br>
 *  Is a method of inviting external entities to view an account's data. The entity to which an invitation relates may be either a folder, an individual folder item, or a device.
 *  The recipients of the invitation are known as it's invitees.<br>
 *  </li>
 *  <li>
 *  <b>Invitees</b> <br>
 *  Recipients of an invitation - these may consist of contacts, or free-form email addresses. <br>
 *  When received by an invitee, an invitation contains a url, which in turn contains a unique key which identifies the invitee to the server, and to which entity
 *  the invitation relates to. Invitations may be retrieved using this key by using the key related getInvitation methods, and the item sevlet which is part of the SDK.
 * </li>
 * </ul>
 *
 */

public interface InvitationManager   
{
    /** Constant to identify Folder invitations */
    public static final short INVITATION_FOLDER = 1;
    /** Constant to identify Item invitations */
    public static final short INVITATION_ITEM   = 2;
    /** Constant to identify Device invitations */
    public static final short INVITATION_DEVICE = 3;
    
         /**
         * Sends out an invitation to user's folder. All items in the folder are then viewable by the invitees using their unique view keys.
         * <br>
         * The invitation is sent to a number of invitees represented by an array of XanbooInvitee objects.<br>
         * A XanbooInvitee can be either a contact, a contact group, or a free-form email address. All invitees receive the same message to view the same
         * invitation, but invitees are tracked separately with different view keys, and may be individually removed from, or added to an invitation.
         *
         * @param xCaller The accountId and userId properties are used to authenticate this call
         * @param folderId The ID of the folder that we are inviting people to view
         * @param subject The email/subject header of the invitation that will be recieved by the invitees
         * @param message The message body of the invitation recieved by the invitees
         * @param invitees An array of XanbooInvitee objects to indentify the users who will recieve the invitation
         * @param expiration The expiration date for the invitation
         * 
         * @throws XanbooException if the send failed.
        */    
        public void sendFolderInvitation(XanbooPrincipal xCaller, long folderId, String subject, String message, XanbooInvitee invitees[], java.util.Date expiration ) throws XanbooException, RemoteException; 
        
        
        
         /**
         * Sends out an invitation to an item. The item is then viewable by the invitees using their unique view keys.
         * <br>
         * The invitation is sent to a number of invitees represented by an array of XanbooInvitee objects.<br>
         * A XanbooInvitee can be either a contact, a contact group, or a free-form email address. All invitees receive the same message to view the same
         * invitation, but invitees are tracked separately with different view keys, and may be individually removed from, or added to an invitation.
         *
         * @param xCaller The accountId and userId properties are used to authenticate this call
         * @param itemId The ID of the item that we are inviting people to view
         * @param subject The email/subject header of the invitation that will be recieved by the invitees
         * @param message The message body of the invitation recieved by the invitees
         * @param invitees An array of XanbooInvitee objects to indentify the users who will recieve the invitation
         * @param expiration The expiration date for the invitation
         * 
         * @throws XanbooException if the send failed.
        */         
        public void sendItemInvitation (XanbooPrincipal xCaller, long itemId, String subject, String message, XanbooInvitee invitees[], java.util.Date expiration ) throws XanbooException, RemoteException;
        

        
         /**
         * Sends out an invitation to a device. The device is then viewable and potentially controllabel by the invitees using their unique view keys.
         * <br>
         * The invitation is sent to a number of invitees represented by an array of XanbooInvitee objects.<br>
         * A XanbooInvitee can be either a contact, a contact group, or a free-form email address. All invitees receive the same message to view the same
         * invitation, but invitees are tracked separately with different view keys, and may be individually removed from, or added to an invitation.
         *
         * @param xCaller The accountId and userId properties are used to authenticate this call
         * @param gatewayGuid The ID of the gateway which controls the device instance
         * @param deviceGuid The ID of the device for which we are inviting people to view
         * @param subject The email/subject header of the invitation that will be recieved by the invitees
         * @param message The message body of the invitation recieved by the invitees
         * @param invitees An array of XanbooInvitee objects to indentify the users who will recieve the invitation
         * @param expiration The expiration date for the invitation
         * 
         * @throws XanbooException if the send failed.
        */         
        public void sendDeviceInvitation (XanbooPrincipal xCaller, String gatewayGuid, String deviceGuid, String subject, String message, XanbooInvitee invitees[], java.util.Date expiration ) throws XanbooException, RemoteException;
        
        
        
         /**
         * Add an invitee to an existing invitation.
         *
         * @param xCaller The accountId and userId properties are used to authenticate this call
         * @param invitationId The ID of the invitation to which we want to add invitees.
         * @param invitees An array of XanbooInvitee objects to indentify the users who will recieve the invitation
         *
         * @throws XanbooException if the operation failed.
        */         
        public void addInvitee(XanbooPrincipal xCaller, long invitationId, XanbooInvitee invitees[]) throws XanbooException, RemoteException;
        
        
        
        /**
         * Retrieves a list of invitees that are recipients of a particular invitation.
         * <br>
         * <b> Columns Returned: </b>
         * <UL>
         *  <li> INVITEE_ID </li>
         *  <li> VIEW_KEY </li>
         *  <li> CONTACT_ID </li>
         *  <li> TO_EMAIL </li>
         *  <li> DATE_LASTVIEWED </li>
         *  <li> CGROUP_ID </li>
         *  <li> VIEW_COUNT </li>
         * </UL>
         * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
         *
         *
         * @param xCaller The accountId and userId properties are used to authenticate this call
         * @param invitationId The ID of the invitation from which we want to retrieve invitees
         *
         * @return a XanbooResultSet of invitees
         * @throws XanbooException if the operation failed.
        */        
        public XanbooResultSet getInviteeList(XanbooPrincipal xCaller, long invitationId) throws XanbooException, RemoteException;
        
        
        
        /**
         * Deletes an invitation and it's associated invitees.
         * This has the effect of removing all view priviliges from all recipients of the invitation.
         *
         * @param xCaller The accountId and userId properties are used to authenticate this call
         * @param invitationId The Invitation to be deleted
         *
         * @throws XanbooException if the operation failed.
        */        
        public void deleteInvitation(XanbooPrincipal xCaller, long[] invitationId) throws XanbooException, RemoteException;
        
        

        /**
         * Retrieves either an invitation, or a list of invitations.
         * <p>
         * <b> Columns Returned: </b>
         * <UL>
         *  <li> INVITATION_ID </li>
         *  <li> DATE_CREATED </li>
         *  <li> OBJECTTYPE_ID </li>
         *  <li> EXPIRATION </li>
         *  <li> OBJECT_ID </li>
         *  <li> SUBJECT </li>
         *  <li> NAME </li>
         *  <li> TZNAME </li>
         * </UL>
         * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
         * </p>
         * <p>
         * The OBJECTTYPE_ID column returned can be used to determine what kind of invitation the invitation is. While an ID value of
         * InvitationManager.INVITATION_ITEM flags the invitation as a Folder Item invitation, a value of InvitationManager.INVITATION_FOLDER
         * indicates it is a Folder, and a value of InvitationManager.INVITATION_DEVICE indicates it is a Device invitation.<br>
         * </p>
         * <p>
         * The TZNAME column specifies the timezone of the sender at the time the invitation was created. Note that all dates
         * and times returned by Xanboo SDK are in GMT, and may require an offset to be applied before presentation.<br>
         * </p>
         * @param xCaller The accountId and userId properties are used to authenticate this call
         * @param invitationId The ID of the invitation to retrive (0 for all)
         * @return a XanbooResultSet of invitations
         *
         * @throws XanbooException if the operation failed.
        */        
        public XanbooResultSet getInvitation(XanbooPrincipal xCaller, long invitationId) throws XanbooException, RemoteException;
        
        
        
        /**
         * Removes an invitee from an invitation.
         * <br>
         * This allows revocation of view privileges from one of or more recipients of an invitation.
         *
         * @param xCaller The accountId and userId properties are used to authenticate this call
         * @param invitationId The Invitation from which the invitee is to be deleted.
         * @param inviteeId The ID of the invitee to be deleted.
         *
         * @throws XanbooException if the operation failed.
        */        
        public void deleteInvitee(XanbooPrincipal xCaller, long invitationId, long[] inviteeId) throws XanbooException, RemoteException;
        
        
        
        /**
         * Retrieves an invitation and some of it's associated parameters using the view key received by an invitee.
         * <p>
         * This method does not require a XanbooPrincipal object, as it is expected to be used by unauthenticated viewers
         * of invitations.
         * <br>
         * <b> Columns Returned: </b>
         * <UL>
         *  <li> INVITATION_ID </li>
         *  <li> MESSAGE </li>
         *  <li> DATE_CREATED </li>
         *  <li> OBJECTTYPE_ID </li>
         *  <li> EXPIRATION </li>
         *  <li> OBJECT_ID </li>
         *  <li> NAME </li>
         *  <li> SUBJECT </li>
         *  <li> TZNAME </li>
         * </UL>
         * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
         * </p>
         * <p>
         * The OBJECTTYPE_ID column returned can be used to determine what kind of invitation the invitation is. While an ID value of
         * InvitationManager.INVITATION_ITEM flags the invitation as a Folder Item invitation, a value of InvitationManager.INVITATION_FOLDER
         * indicates it is a Folder, and a value of InvitationManager.INVITATION_DEVICE indicates it is a Device invitation.<br>
         * </p>
         * <p>
         * The TZNAME column specifies the timezone of the sender at the time the invitation was created. Note that all dates
         * and times returned by Xanboo SDK are in GMT, and may require an offset to be applied before presentation.<br>
         * </p>
         *
         * @param inviteeId The ID of the invitee to retrieve
         * @param viewKey The key received by the invitee (to authenticate the viewer)
         *
         * @return A XanbooResultSet object
         * @throws XanbooException if the invitation could not be retrieved.
         */        
        public XanbooResultSet getInvitationByKey( long inviteeId, String viewKey ) throws XanbooException, RemoteException ;
        
        
        
        /**
         * Returns a list of items associated with an invitation by the view key received by the invitee.
         * In the case of an item invitation, only one row will be returned. In the case of a folder invitation, only one item will be returned, but the
         * objecttype_id returned by the getInvitationByKey will have already identified whether this was an item or folder invitation.
         * <br>
         * <b> Columns Returned: </b>
         * <UL>
         *  <li> DATE_CREATED </li>
         *  <li> TIMESTAMP </li>
         *  <li> SOURCE_LABEL </li>
         *  <li> FOLDERITEM_ID </li>
         *  <li> NAME </li>
         *  <li> ITEM_CONTENTTYPE </li>
         * </UL>
         * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
         *
         * @param invitationId The ID of the invitation that the invitee is associated with
         * @param viewKey The key received by the invitee (to authenticate the viewer)
         *
         * @return XanbooResultSet containing the requested data.
         * @throws XanbooException if the operation failed.
         */        
        public XanbooResultSet getInvitationItemListByKey( long invitationId, String viewKey ) throws XanbooException, RemoteException ;
        
        
        
        /**
         * Returns a list of items associated with an invitation by the view key received by the invitee.
         * In the case of an item invitation, only one row will be returned. In the case of a folder invitation, only one item will be returned, but the
         * objecttype_id returned by the getInvitationByKey will have already identified whether this was an item or folder invitation.
         * <br>
         * <b> Columns Returned: </b>
         * <UL>
         *  <li> DATE_CREATED </li>
         *  <li> TIMESTAMP </li>
         *  <li> SOURCE_LABEL </li>
         *  <li> FOLDERITEM_ID </li>
         *  <li> NAME </li>
         * </UL>
         * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
         *
         * @param invitationId The ID of the invitation that the invitee is associated with
         * @param viewKey The key received by the invitee (to authenticate the viewer)
         * @param startRow The row to start retrieving records from
         * @param numRows The maximum number of rows to retrieve
         * @param sortBy predefined number representing a sort by field and sorting order
         * and must be one of the {@link com.xanboo.core.sdk.invitation.InvitationManagerEJB#SORT_BY_ITEMNAME_ASC sorting numbers} for folder items
         *
         * @return XanbooResultSet containing the requested data.
         * @throws XanbooException if the operation failed.
         */        
        public XanbooResultSet getInvitationItemListByKey( long invitationId, String viewKey, int startRow, int numRows, int sortBy ) throws XanbooException, RemoteException;
        
        
        
        /**
         * Returns a XanbooItem object that belongs to a particular invitation item
         *
         * @param invitationId The ID of the invitation that the invitee is associated with
         * @param viewKey The key received by the invitee (to authenticate the viewer)
         * @param folderItemId The ID of the item to be returned
         *
         * @return XanbooItem containing data specific to the requested item.
         * @throws XanbooException if the operation failed.
         */        
        public XanbooItem getInvitationItemByKey( long invitationId, String viewKey, long folderItemId ) throws XanbooException, RemoteException;
        
        
        
        /**
         * Returns a item notes that belongs to a particular invitation item
         *
         * @param invitationId The ID of the invitation that the invitee is associated with
         * @param viewKey The key received by the invitee (to authenticate the viewer)
         * @param folderItemId The ID of the item to be returned
         *
         * @return XanbooItem containing data specific to the requested item.
         * @throws XanbooException if the operation failed.
         */        
        public XanbooResultSet getInvitationItemNotesByKey( long invitationId, String viewKey, long folderItemId ) throws XanbooException, RemoteException;
        

        /**
         * Authenticates a guest viewer by view key
         *
         * @param inviteeId The ID of the invitee, as recieved in the invitation email
         * @param viewKey The view key as received in the invitation email
         *
         * @return XanbooGuestPrincipal who has access to the resources shared by this invitation
         */
        public XanbooGuestPrincipal authenticateInvitee( long inviteeId, String viewKey) throws XanbooException, RemoteException;
 
        
        /**
         * Retrieves the invitation details to which a guest has been invited to view
         * <p>
         * <b> Columns Returned: </b>
         * <UL>
         *  <li> INVITATION_ID </li>
         *  <li> MESSAGE </li>
         *  <li> DATE_CREATED </li>
         *  <li> OBJECTTYPE_ID </li>
         *  <li> EXPIRATION </li>
         *  <li> OBJECT_ID </li>
         *  <li> NAME </li>
         *  <li> SUBJECT </li>
         *  <li> TZNAME </li>
         * </UL>
         * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
         * </p>
         * <p>
         * The OBJECTTYPE_ID column returned can be used to determine what kind of invitation the invitation is. While an ID value of
         * InvitationManager.INVITATION_ITEM flags the invitation as a Folder Item invitation, a value of InvitationManager.INVITATION_FOLDER
         * indicates it is a Folder, and a value of InvitationManager.INVITATION_DEVICE indicates it is a Device invitation.<br>
         * </p>
         *
         * @param guest identifies the guest and used to determine what items may be viewed
         *
         * @return XanbooResultSet of items to which the guest is allowed to view.
         */
        public XanbooResultSet getInvitation( XanbooGuestPrincipal guest ) throws XanbooException, RemoteException;
        

        /**
         * Retrieves a gateway info object associated with a device invitation.
         * Gateway object contains information critical to webcam-based invitations
         *
         * @param guest identifies the guest and used to determine what items may be viewed
         *
         * @return XanbooGateway
         */
        public XanbooGateway getGatewayInfo( XanbooGuestPrincipal guest ) throws XanbooException, RemoteException;
 

        /**
         * Allows a guest viewer to send commands to a device to which he has been invited to view.
         * <br>
         * Note that at the time of writing, this call allows access to all writable managed objects of the shared device.
         * It is therefore up to the UI app to restrict access as necessary.
         *
         * @param XanbooGuestPrincipal identifies the guest and used to determine what items may be viewed
         * @param mobjectId is the managed object to be set
         * @param mobjectValue is the value to apply to the managed object.
         *
         * @throws XanbooException if the command could not be sent
         */
        public void setMObject(XanbooGuestPrincipal xCaller, String mobjectId, String mobjectValue) throws XanbooException, RemoteException;
        
        /**
         * @see InvitationManager#setMObject( XanbooGuestPrincipal, String, String ) Similar to setMObject( XanbooGuestPrincipal, String, String );
         */
        public void setMObject(XanbooGuestPrincipal xCaller, String[] mobjectId, String[] mobjectValue) throws XanbooException, RemoteException;
        
        /**
         * Allow the create a new device invitation
         * @param xCaller
         * @param gatewayGuid
         * @param deviceGuid
         * @param message
         * @param xanbooInvitee
         * @param expiration
         * @return
         * @throws XanbooException
         * @throws RemoteException
         */
        public XanbooInvitee[] newDeviceInvitation (XanbooPrincipal xCaller, String gatewayGuid, String deviceGuid, String message, XanbooInvitee xanbooInvitee[], java.util.Date expiration ) throws XanbooException, RemoteException;
        
        /**
         * 
         * @param xCaller
         * @param mobjectId
         * @return
         * @throws RemoteException
         * @throws XanbooException
         */
        public XanbooResultSet getMObject(XanbooGuestPrincipal xCaller, String mobjectId) throws RemoteException, XanbooException;
        /**
         * 
         * @param xCaller
         * @param mobjectId
         * @return
         * @throws RemoteException
         * @throws XanbooException
         */
        public XanbooResultSet getMObjectBinary(XanbooGuestPrincipal xCaller, String mobjectId) throws RemoteException, XanbooException;
        
}
