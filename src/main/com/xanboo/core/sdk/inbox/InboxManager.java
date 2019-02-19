/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/inbox/InboxManager.java,v $
 * $Id: InboxManager.java,v 1.8 2011/06/27 17:06:31 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.inbox;

import java.rmi.RemoteException;
import java.util.Date;

import com.xanboo.core.model.XanbooBinaryContent;
import com.xanboo.core.model.XanbooItem;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.XanbooException;

/**
 * Remote Interface for the InboxManagerEJB
 *
 * The Xanboo Inbox is where all events that occur for users's gateways will appear (unless otherwise configured).
 * If the event contained any payload, then that payload will be assigned to an item within the inbox.
 *
 * The inbox behaviour is similar to that of regular folders, the main difference being that items in the inbox cannot be copied to another location - 
 * they can only be moved to a regular folder. Also, inbox items may not have annotations, or be subject to an invitation (at least, not directly).
 *
 */

public interface InboxManager   
{
    /**
     * Adds a new item to the the caller's inbox.
     * <br>
     * A XanbooItem object to pass to this method may be created using the XanbooItem constructor that accepts a File input parameter.
     *
     * @param xCaller The accountId and userId properties of the user are used to validate this call
     * @param item A XanbooItem object from which the new item information is extracted.
     *
     * @return The ID of the newly created item if successful, otherwise null
     * @throws XanbooException if the item failed to be added.
    */
    public long addItem(XanbooPrincipal xCaller, XanbooItem item) throws XanbooException, RemoteException;
    
    
    
    /**
     * Deletes a number of items from a user's inbox.
     * Once deleted, items will appear in the caller's wastebasket if wastebasket is enabled, or removed permanently otherwise.
     * When an item is restored from the wastebasket, it appears back in the inbox.
     * If the deleted of one or more specified items fails, no items are deleted.
     *
     * @param xCaller The accountId and userId properties of the user are used to validate this call
     * @param inboxItemIds An array of integers contining the ids of the items to be deleted
     * @throws XanbooException if any items specified could not be deleted.
    */    
    public void deleteItem(XanbooPrincipal xCaller, long[] inboxItemIds) throws XanbooException, RemoteException;
    
    
    /**
     * Deletes a list of items from a user's inbox.
     * Once deleted, items will appear in the caller's wastebasket, if wastebasket is enabled, or removed permanently otherwise.
     * When an item is restored from the wastebasket, it appears back in the inbox.
     *
     * @param xCaller The accountId and userId properties of the user are used to validate this call
     * @param gatewayGUID If not null, delete only items which originated at the specified gateway.
     * @param deviceGUID If not null, delete only items which originated at the specified devices. A gatewayGUID parameter is also required if a deviceGuid is specified.
     * @param contentType If not null, delete only items of specific content type. Allowed values are: "text", "image", "video", and "media", where "media" removed both image and video items.
     * @throws XanbooException if any items specified could not be deleted.
    */    
    public void deleteItemList(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, String contentType) throws XanbooException, RemoteException;
    
    
    /**
     * Removes all items from an account's inbox. All removed items will appear in wastebasket, if wastebasket is enabled as defined by 'module.wastebasket.enabled' parameter in xancore-config.
     *
     * @param xCaller The accountId and userId properties of the user are used to validate this call
     * @throws XanbooException if the operation fails.
    */    
    public void emptyInbox(XanbooPrincipal xCaller) throws XanbooException, RemoteException;
    
    
    
    /**
     * Moves a number of items from a user's inbox to a regular folder.
     * <br>
     * If the move of one or more items fails, no items are moved. <br>
     * A list of possible folders to send items can be moved can be retrieved using the <code>FolderManager.getFolderList()</code>
     *
     * @param xCaller The accountId and userId properties of the user are used to validate this call
     * @param inboxItemIds An array of item ids that we want to move
     * @param folderId The destination folder
     *
     * @throws XanbooException if the move operation fails on one or more of the specified items.
     *
     * @see com.xanboo.core.sdk.folder.FolderManagerEJB
    */    
    public void moveItem(XanbooPrincipal xCaller, long[] inboxItemIds, long folderId) throws XanbooException, RemoteException;
    
    
    
    /**
     * Returns a list of all items that exist in the account's inbox.
     * <p>
     * Note that other getItemList calls are availalble that offer features such as {@link #getItemList(XanbooPrincipal, int, int, int) pagination}, {@link #getItemList(XanbooPrincipal, String, String) source device filtering}, or {@link #getItemList(XanbooPrincipal, String, String, int, int, int) both}
     * <br>
     * Also note that if the content type for an item ends with -rejected, this signifies that the user had insufficient available disk quota to store the item file.
     * </p>
     * <UL>
     *  <li> ACCOUNT_ID </li>
     *  <li> STATUS_ID </li>
     *  <li> ITEM_MOUNT </li>
     *  <li> ITEM_CONTENTTYPE </li>
     *  <li> ITEM_PATH </li>
     *  <li> DATE_CREATED </li>
     *  <li> SOURCE_LABEL </li>
     *  <li> INBOXITEM_ID </li>
     *  <li> THUMB_FILE </li>
     *  <li> ITEM_FILE </li>
     *  <li> GATEWAY_GUID </li>
     *  <li> DEVICE_GUID </li>
     *  <li> EVENTLOG_ID </li>
     *  <li> TIMESTAMP </li>
     *  <li> NAME </li>
     *  <li> VIEW_COUNT </li>
     *  <li> ITEM_SIZE </li>
     *  <li> EGROUP_ID </li>
     *  <li> SRC_DEVICE_GUID </li>
     *  <li> ACCESSKEY_ID </li>
     * </UL>
     *
     * @param xCaller The accountId and userId properties are used to identify which inbox to retrieve.
     *
     * @return a XanbooResultSet of items in the specified user's inbox.
     *
     * @throws XanbooException if the operation fails.
     *
     * @see com.xanboo.core.model.XanbooItem
     *
     */    
    public XanbooResultSet getItemList(XanbooPrincipal xCaller) throws XanbooException, RemoteException;
    
    
    /**
     * Identical operation to {@link #getItemList(XanbooPrincipal)} method except with source device filtering.
     *
     * @param xCaller The accountId and userId properties are used to identify which inbox to retrieve.
     * @param gatewayGUID If not null, return only items which originated at the specified gateway.
     * @param deviceGUID If not null, return only items which originated at the specified device and gateway.
     *
     * @return a XanbooResultSet of items in the specified user's inbox.
     *
     * @throws XanbooException if the operation fails.
     *
     * @see com.xanboo.core.model.XanbooItem
     * 
     */    
    public XanbooResultSet getItemList(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID) throws XanbooException, RemoteException;
    
    
    /**
     * Identical operation to {@link #getItemList(XanbooPrincipal)} method except with pagination control.
     *
     * @param xCaller The accountId and userId properties are used to identify which inbox to retrieve.
     * @param startRow The row from which to start retrieving items (for pagination control)
     * @param numRows The maximum number of rows to return (for pagination control)
     * @param sortBy predefined number representing a sort by field and sorting order
     * and must be one of the {@link com.xanboo.core.sdk.inbox.InboxManagerEJB#SORT_BY_NAME_ASC sorting numbers} for folder items
     *
     * @return a XanbooResultSet of items in the specified user's inbox.
     *
     * @throws XanbooException if the operation fails.
     *
     * @see com.xanboo.core.model.XanbooItem
     * 
     * @deprecated  replaced by {@link #getItemList(XanbooPrincipal, String, String, Date, Date, String, String, int, int, int)}
     */    
    public XanbooResultSet getItemList(XanbooPrincipal xCaller, int startRow, int numRows, int sortBy) throws XanbooException, RemoteException;
    
    
    
    /**
     * Identical operation to {@link #getItemList(XanbooPrincipal)} method except with source device filtering and pagination controls.
     * <br>
     * Note that the specification of a deviceGUID inherently requires a valid gatewayGUID as deviceGUID is only unique within a gateway.
     *
     * @param xCaller The accountId and userId properties are used to identify which inbox to retrieve.
     * @param gatewayGUID If not null, return only items which originated at the specified gateway.
     * @param deviceGUID If not null, return only items which originated at the specified device and gateway.
     * @param startRow The row from which to start retrieving items (for pagination control)
     * @param numRows The maximum number of rows to return (for pagination control)
     * @param sortBy predefined number representing a sort by field and sorting order
     * and must be one of the {@link com.xanboo.core.sdk.inbox.InboxManagerEJB#SORT_BY_NAME_ASC sorting numbers} for folder items
     *
     * @return a XanbooResultSet of items in the specified user's inbox.
     *
     * @throws XanbooException if the operation fails.
     *
     * @see com.xanboo.core.model.XanbooItem
     * 
     * @deprecated  replaced by {@link #getItemList(XanbooPrincipal, String, String, Date, Date, String, String, int, int, int)}
    */      
    public XanbooResultSet getItemList(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, int startRow, int numRows, int sortBy) throws XanbooException, RemoteException;
    
    
    /**
     * Identical operation to {@link #getItemList(XanbooPrincipal)} method except with filtering and pagination controls.
     * <br>
     * Note that the specification of a deviceGUID inherently requires a valid gatewayGUID as deviceGUID is only unique within a gateway.
     *
     * @param xCaller The accountId and userId properties are used to identify which inbox to retrieve.
     * @param gatewayGUID If not null, return only items which originated at the specified gateway.
     * @param deviceGUID If not null, return only items which originated at the specified devices. This could be a single device identifier on the specified gateway or a CSV list of device guids to query items from multiple devices.
     * @param fromDate If not null, return only items after specified timestamp (in GMT).
     * @param toDate If not null, return only items before specified timestamp (in GMT).
     * @param contentType If not null, return only items of specific content type. Allowed values are: "text", "image", "video", and "media", where "media" returns both image and video items.
     * @param eID If not null, return only items of a particular event ID. 
     * @param startRow The row from which to start retrieving items (for pagination control)
     * @param numRows The maximum number of rows to return (for pagination control)
     * @param sortBy predefined number representing a sort by field and sorting order
     * and must be one of the {@link com.xanboo.core.sdk.inbox.InboxManagerEJB#SORT_BY_NAME_ASC sorting numbers} for folder items
     *
     * @return a XanbooResultSet of items in the specified user's inbox.
     *
     * @throws XanbooException if the operation fails.
     *
     * @see com.xanboo.core.model.XanbooItem
    */      
    public XanbooResultSet getItemList(XanbooPrincipal xCaller, String gatewayGUID, String deviceGUID, Date fromDate, Date toDate, String contentType, String eID, int startRow, int numRows, int sortBy) throws XanbooException, RemoteException;
    
    
    
    /**
     * Returns a list of all inbox items that that are associated/correlated with an event group id.
     *
     * @param xCaller The accountId and userId properties are used to identify which inbox to retrieve.
     * @param gatewayGUID origin gateway guid generating correlated events/items.
     * @param eventGroupId a string identifier shared by all items correlated with an event condition.  .
     *
     * @return a XanbooResultSet of items in the specified user's inbox.
     *
     * @throws XanbooException if the operation fails.
     *
     * @see com.xanboo.core.model.XanbooItem
     *
     */    
    public XanbooResultSet getCorrelatedItemList(XanbooPrincipal xCaller, String gatewayGUID, String eventGroupId) throws XanbooException, RemoteException;
    
    
    
    /**
     * Retrieves a specific item from a user's inbox.
     *
     * @param xCaller The accountId and userId properties are used to validate this call
     * @param inboxItemId The ID of the item to retrieve
     *
     * @return A XanbooItem of the requested ID
     * @throws XanbooException if the operation failed
    */        
    public XanbooItem getItem(XanbooPrincipal xCaller, long inboxItemId) throws XanbooException, RemoteException;

    /**
     * Retrieves a specific item from a user's inbox.
     *
     * @param xCaller The accountId and userId properties are used to validate this call
     * @param inboxItemId The ID of the item to retrieve
     * @param returnPrevNext boolean to indicate if previous and next item ids will be returned or not
     * @param updateViewCount boolean to indicate if view count for this item will be incremented or not
     *
     * @return A XanbooItem of the requested ID
     * @throws XanbooException if the operation failed
    */
    public XanbooItem getItem(XanbooPrincipal xCaller, long inboxItemId, boolean returnPrevNext, boolean updateViewCount) throws XanbooException, RemoteException;



    /**
     * Retrieves binary content associated with an item
     *
     * @param xCaller The accountId and userId properties are used to validate this call
     * @param mo mount parameter for the item
     * @param dir item directory for the item
     * @param fname file name for the item
     * @param isThumb whether to return the full item or a thumbnail, if applicable
     *
     * @return a XanbooBinaryContent object
     * 
     * @throws XanbooException if the operation failed
    */
    public XanbooBinaryContent getItemBinary(XanbooPrincipal xCaller, String itemMount, String itemDir, String fileName, boolean isThumb) throws XanbooException, RemoteException;

}
