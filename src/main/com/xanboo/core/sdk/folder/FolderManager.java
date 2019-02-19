/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/folder/FolderManager.java,v $
 * $Id: FolderManager.java,v 1.21 2008/09/25 18:39:51 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.folder;

import java.rmi.RemoteException;
import java.util.Date;

import com.xanboo.core.model.XanbooItem;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.XanbooException;

/**
 * Remote Interface for the FolderManagerEJB
 *
 *
 * <p>
 * Folders are containers of items that can be created, renamed, and deleted.
 * Note that FolderManager does not interact with the inbox. The inbox is considered a 'special' folder, and has it's own InboxHandler class.
 *
 * Items can be moved (not copied) from the inbox to a regular folder. Items can be moved and copied between regular folders.
 *
 * </p>
 *
 * <b> Glossary </b>
 * <ul>
 * <li>
 *  <b>Notes</b> <br>
 *  Are a number of free-form text annotations associated with an item. Notes may be added, deleted and retrieved using note-related SDK calls.
 * </li>
 * </ul>
 *
 * @see com.xanboo.core.sdk.inbox.InboxManagerEJB
 */

public interface FolderManager   
{
        /**
         * Retrieves a list of all folders belonging to the caller's account.
         * <br>
         * <b> Columns Returned: </b>
         * <UL>
         *  <li> FOLDER_ID </li>
         *  <li> ITEM_COUNT </li>
         *  <li> ACCOUNT_ID </li>
         *  <li> STATUS_ID </li>
         *  <li> DATE_CREATED </li>
         *  <li> DESCRIPTION </li>
         *  <li> USER_ID </li>
         *  <li> PARENT_FOLDER_ID </li>
         *  <li> SUBFOLDER_COUNT </li>
         *  <li> TYPE </li>
         *  <li> NAME </li>
         *  <li> ISPUBLIC </li>
         * </UL>
         * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
         *
         * @param xCaller The accountId and userId properties are used to authenticate this call
         * @return a XanbooResultSet of folders that exist in the caller's account.
         *
         * @throws XanbooException if the operation failed.
         */
        public XanbooResultSet getFolderList(XanbooPrincipal xCaller) throws XanbooException, RemoteException;
        
        
        
        /**
         * Retrieves a list of folders belonging to the caller's account with pagination controls.
         * <br>
         * <b> Columns Returned: </b>
         * <UL>
         *  <li> FOLDER_ID </li>
         *  <li> ITEM_COUNT </li>
         *  <li> ACCOUNT_ID </li>
         *  <li> STATUS_ID </li>
         *  <li> DATE_CREATED </li>
         *  <li> DESCRIPTION </li>
         *  <li> USER_ID </li>
         *  <li> PARENT_FOLDER_ID </li>
         *  <li> SUBFOLDER_COUNT </li>
         *  <li> TYPE </li>
         *  <li> NAME </li>
         *  <li> ISPUBLIC </li>
         * </UL>
         * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
         *
         * @param xCaller The accountId and userId properties are used to authenticate this call
         * @param startRow starting row number
         * @param numRows max number of returned rows
         * @param sortBy predefined number representing a sort by field and sorting order
         * and must be one of the {@link com.xanboo.core.sdk.folder.FolderManagerEJB#SORT_BY_FOLDERNAME_ASC SORT_BY_FOLDERNAME} sort constants
         *
         * @return a XanbooResultSet of folders that exist in the caller's account.
         *
         * @throws XanbooException if the operation failed.
         */        
        public XanbooResultSet getFolderList(XanbooPrincipal xCaller, int startRow, int numRows, int sortBy) throws XanbooException, RemoteException;
        
        
        
        /**
         * Retrieves a specific XanbooFolder object.
         *
         * @param xCaller The accountId and userId properties are used to authenticate this call
         * @param folderId The ID of the folder to retrieve
         *
         * @return A XanbooFolder of the requested ID
         * @throws XanbooException if the operation failed.
         * @see com.xanboo.core.sdk.folder.XanbooFolder
         *
         */        
        public XanbooFolder getFolder(XanbooPrincipal xCaller, long folderId) throws XanbooException, RemoteException; 
        
        
        
        /**
         * Adds a new folder to the account of the caller.
         *
         * @param xCaller The accountId and userId properties are used to authenticate this call
         * @param folder a Xanboofolder object from which the new folder information is extracted.
         *
         * @return long The ID of the folder created.
         * @throws XanbooException if the folder is not created.
         */        
        public long addFolder(XanbooPrincipal xCaller, XanbooFolder folder) throws XanbooException, RemoteException;
        
        
        
        /**
         * Removes a folder from the caller's account.
         * The removed folder will appear in the caller's wastebasket, if wastebasket is enabled as defined by 'module.wastebasket.enabled' parameter in xancore-config..
         *
         * @param xCaller The accountId and userId properties are used to authenticate this call
         * @param folderId An array of folder IDs to be deleted
         * @param forceFlag If true, a folder will be deleted even if it contains items. If false, the deletion of a non-empty folder will fail
         *
         * @throws XanbooException if the deletion failed.
         */        
        public void deleteFolder(XanbooPrincipal xCaller, long[] folderId, boolean forceFlag) throws XanbooException, RemoteException;
        
        
        
        /**
         * Allows updates to the name and description of a folder.
         * One way of updating a folders details it to first retrieve a corresponding XanbooFolder object with getFolder(), apply the changes to the returned object
         * and then use it with this method.
         *
         * @param xCaller The accountId and userId properties are used to authenticate this call
         * @param folder The name and description attributes of this object are applied to the folder
         *
         * @throws XanbooException if the update failed.
         */        
        public void updateFolder(XanbooPrincipal xCaller, XanbooFolder folder ) throws XanbooException, RemoteException;
        
        

        /**
         * Clears the entire contents of a folder, moving all it's items to the caller's wastebasket.
         * All emptied items appear in the caller's wastebasket, if wastebasket is enabled as defined by 'module.wastebasket.enabled' parameter in xancore-config.
         *
         * @param xCaller The accountId and userId properties are used to authenticate this call.
         * @param folderId The ID of the folder to empty. This folder is not deleted.
         *
         * @throws XanbooException if the empty failed.
         */        
        public void emptyFolder(XanbooPrincipal xCaller, long folderId ) throws XanbooException, RemoteException;
        
        
        
        /**
         * Retrieves a list of all items that exist in a particular folder belonging to the caller.
         * <p>
         * Note that other getItemList calls are availalble that offer features such as {@link #getItemList(XanbooPrincipal, long, int, int, int) pagination}, {@link #getItemList(XanbooPrincipal, long, String, String) source device filtering}, or {@link #getItemList(XanbooPrincipal, long, String, String, int, int, int) both}
         * <br>
         * Also note that if the content type for an item ends with -rejected, this signifies that the user had insufficient available disk quota to store the item file.
         * </p>
         * <b> Columns Returned: </b>
         * <UL>
         *  <li> ACCOUNT_ID </li>
         *  <li> DATE_CREATED </li>
         *  <li> DEVICE_GUID </li>
         *  <li> EVENTLOG_ID </li>
         *  <li> FOLDERITEM_ID </li>
         *  <li> FOLDER_ID </li>
         *  <li> GATEWAY_GUID </li>
         *  <li> ITEM_CONTENTTYPE </li>
         *  <li> ITEM_FILE </li>
         *  <li> ITEM_ID </li>
         *  <li> ITEM_MOUNT </li>
         *  <li> ITEM_PATH </li>
         *  <li> NAME </li>
         *  <li> SOURCE_LABEL </li>
         *  <li> THUMB_FILE </li>
         *  <li> TIMESTAMP </li>
         *  <li> VIEW_COUNT </li>
         *  <li> ITEM_SIZE </li>
         * </UL>
         * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
         *
         * @param xCaller The accountId and userId properties are used to authenticate this call.
         * @param folderId The ID of the folder from which we want to retrieve items.
         *
         * @return a XanbooResultSet of itemsIds, and some limited meta data.
         *
         * @throws XanbooException if the operation failed.
         */        
        public XanbooResultSet getItemList(XanbooPrincipal xCaller, long folderId ) throws XanbooException, RemoteException;
        
        
        
        /**
         * Identical operation to {@link #getItemList(XanbooPrincipal, long)} method except with source device filtering.
         * 
         * @param xCaller The accountId and userId properties are used to authenticate this call.
         * @param folderId The ID of the folder from which we want to retrieve items.
         * @param gatewayGUID If not null, return only items which originated at the specified gateway.
         * @param deviceGUID If not null, return only items which originated at the specified device and gateway.
         *
         * @return a XanbooResultSet of itemsIds, and some limited meta data.
         *
         * @throws XanbooException if the operation failed.
         */        
        public XanbooResultSet getItemList(XanbooPrincipal xCaller, long folderId, String gatewayGUID, String deviceGUID ) throws XanbooException, RemoteException;
        

        /**
         * Identical operation to {@link #getItemList(XanbooPrincipal, long)} method except with pagination controls.
         *
         * @param xCaller The accountId and userId properties are used to authenticate this call.
         * @param folderId The ID of the folder from which we want to retrieve items.
         * @param startRow starting row number
         * @param numRows max number of returned rows
         * @param sortBy predefined number representing a sort by field and sorting order
         * and must be one of the {@link com.xanboo.core.sdk.folder.FolderManagerEJB#SORT_BY_TIME_ASC SORT_BY_TIME} sort constants
         *
         * @return a XanbooResultSet of itemsIds, and some limited meta data.
         *
         * @throws XanbooException if the operation failed.
         *
         * @deprecated  replaced by {@link #getItemList(XanbooPrincipal, long, String, String, Date, Date, String, String, int, int, int)}
         */        
        public XanbooResultSet getItemList(XanbooPrincipal xCaller, long folderId, int startRow, int numRows, int sortBy ) throws XanbooException, RemoteException;
        
        
        
        /**
         * Identical operation to {@link #getItemList(XanbooPrincipal, long)} method except with source device filtering and pagination control.
         * <br>
         * Note that the specification of a deviceGUID inherently requires a valid gatewayGUID as deviceGUID is only unique within a gateway.
         *
         * @param xCaller The accountId and userId properties are used to authenticate this call.
         * @param folderId The ID of the folder from which we want to retrieve items.
         * @param gatewayGUID If not null, return only items which originated at the specified gateway.
         * @param deviceGUID If not null, return only items which originated at the specified device and gateway.
         * @param startRow starting row number
         * @param numRows max number of returned rows
         * @param sortBy predefined number representing a sort by field and sorting order
         * and must be one of the {@link com.xanboo.core.sdk.folder.FolderManagerEJB#SORT_BY_TIME_ASC SORT_BY_TIME} sort constants
         *
         * @return a XanbooResultSet of itemsIds, and some limited meta data.
         *
         * @throws XanbooException if the operation failed.
         * 
         * @deprecated  replaced by {@link #getItemList(XanbooPrincipal, long, String, String, Date, Date, String, String, int, int, int)}
         */        
        public XanbooResultSet getItemList(XanbooPrincipal xCaller, long folderId, String gatewayGUID, String deviceGUID, int startRow, int numRows, int sortBy ) throws XanbooException, RemoteException;

        
        
        /**
         * Identical operation to {@link #getItemList(XanbooPrincipal)} method except with filtering and pagination controls.
         * <br>
         * Note that the specification of a deviceGUID inherently requires a valid gatewayGUID as deviceGUID is only unique within a gateway.
         *
         * @param xCaller The accountId and userId properties are used to authenticate this call.
         * @param folderId The ID of the folder from which we want to retrieve items.
         * @param gatewayGUID If not null, return only items which originated at the specified gateway.
         * @param deviceGUID If not null, return only items which originated at the specified devices. This could be a single device identifier on the specified gateway or a CSV list of device guids to query items from multiple devices.
         * @param fromDate If not null, return only items after specified timestamp (in GMT).
         * @param toDate If not null, return only items before specified timestamp (in GMT).
         * @param contentType If not null, return only items of specific content type. Allowed values are: "text", "image", "video", and "media", where "media" returns both image and video items.
         * @param eID If not null, return only items of a particular event ID. 
         * @param startRow starting row number
         * @param numRows max number of returned rows
         * @param sortBy predefined number representing a sort by field and sorting order
         * and must be one of the {@link com.xanboo.core.sdk.folder.FolderManagerEJB#SORT_BY_TIME_ASC SORT_BY_TIME} sort constants
         *
         * @return a XanbooResultSet of itemsIds, and some limited meta data.
         *
         * @throws XanbooException if the operation failed.
         * 
         */        
        public XanbooResultSet getItemList(XanbooPrincipal xCaller, long folderId, String gatewayGUID, String deviceGUID, Date fromDate, Date toDate, String contentType, String eID, int startRow, int numRows, int sortBy) throws XanbooException, RemoteException;
        

        /**
         * Returns a list of all folder items that that are associated/correlated with an event group id.
         *
         * @param xCaller The accountId and userId properties are used to identify the owner account.
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
         * Retrieves a specific item from a folder belonging to the caller.
         *
         * @param xCaller The accountId and userId properties are used to authenticate this call
         * @param itemId The ID of the item to retrieve
         *
         * @return A XanbooItem of the requested ID
         * @throws XanbooException if the operation failed.
         */        
        public XanbooItem getItem(XanbooPrincipal xCaller, long itemId ) throws XanbooException, RemoteException;
        
        
        
        /**
         * Adds a new item to one of the caller's existing folders.
         * <br>
         * A XanbooItem object to pass to this method may be created using the XanbooItem constructor that accepts a File input parameter.
         *
         * @param xCaller The accountId and userId properties are used to authenticate this call
         * @param item An object representing the item to be added
         * @param folderId The id of the folder in which to add the item
         *
         * @return The ID of the newly created item if successful
         * @throws XanbooException if the operation failed.
         */        
        public long addItem(XanbooPrincipal xCaller, XanbooItem item, long folderId) throws XanbooException, RemoteException;
        
        

        /**
         * Deletes a number of items from the caller's folder(s)
         * If any one item fails to be deleted (invalid ID, etc) then the entire operation is invalidated (no items will be deleted). Deleted item(s) will
         * appear in wastebasket, if wastebasket is enabled as defined by 'module.wastebasket.enabled' parameter in xancore-config.
         *
         * @param xCaller The accountId and userId properties of the user are used to authenticate this call
         * @param itemIds An array of integers containing the ids of the items to be deleted
         *
         * @throws XanbooException if the deletion of any items failed.
         * @see com.xanboo.core.model.XanbooItem
         */        
        public void deleteItem(XanbooPrincipal xCaller, long[] itemIds) throws XanbooException, RemoteException;
        
        
        /**
         * Deletes a list of items from a user's inbox.
         * Once deleted, items will appear in the caller's wastebasket, if wastebasket is enabled, or removed permanently otherwise.
         * When an item is restored from the wastebasket, it appears back in the inbox.
         *
         * @param xCaller The accountId and userId properties of the user are used to validate this call
         * @param folderId The id of the folder in which to remove items from. If -1, items will be removed across all folders based on other source device and content criteria
         * @param gatewayGUID If not null, delete only items which originated at the specified gateway.
         * @param deviceGUID If not null, delete only items which originated at the specified devices. A gatewayGUID parameter is also required if a deviceGuid is specified.
         * @param contentType If not null, delete only items of specific content type. Allowed values are: "text", "image", "video", and "media", where "media" removed both image and video items.
         * @throws XanbooException if any items specified could not be deleted.
         */    
        public void deleteItemList(XanbooPrincipal xCaller, long folderId, String gatewayGUID, String deviceGUID, String contentType) throws XanbooException, RemoteException;
        
        
        /**
         * Moves item(s) from one folder to another already existing folder.
         *
         * @param xCaller The accountId and userId properties of the user are used to authenticate this call
         * @param itemId An array of item ids to move
         * @param folderId The destination folder of the item(s)
         *
         * @throws XanbooException if the move operation failed.
         * @see com.xanboo.core.model.XanbooItem
         */        
        public void moveItem(XanbooPrincipal xCaller, long[] itemIds, long folderId) throws XanbooException, RemoteException;
        
        
        
        /**
         * Copies item(s) from one folder to another.
         *
         * @param xCaller The accountId and userId properties of the user are used to validate this call
         * @param itemId An array of item ids to copy
         * @param folderId The destination folder of the item(s)
         *
         * @throws XanbooException if the copy operation failed.
         * @see com.xanboo.core.model.XanbooItem
         */        
        public void copyItem(XanbooPrincipal xCaller, long[] itemIds, long folderId) throws XanbooException, RemoteException;
        
        
        
        /**
         * Updates the name of an item
         *
         * @param xCaller The accountId and userId properties of the user are used to authenticate this call
         * @param itemId The ID of the item to update
         * @param name The new name to assign to the item
         *
         * @throws XanbooException if the rename failed
         * @see com.xanboo.core.model.XanbooItem
         */        
        public void updateItem(XanbooPrincipal xCaller, long itemId, String name) throws XanbooException, RemoteException;
        
        
        
        /**
         * Adds a note to an existing item.
         *<br>
         *  A folder item may have any number of notes associated with it. The maximum length for a note is 256 characters.
         *
         * @param xCaller The accountId and userId properties of the user are used to authenticate this call
         * @param itemId The ID of the item on which to add the note
         * @param note The text to associated with this item (no more than 256 characters)
         *
         * @throws XanbooException
         * @see com.xanboo.core.model.XanbooItem
         */        
        public void addNote(XanbooPrincipal xCaller, long itemId, String note) throws XanbooException, RemoteException;
        
        
        
        /**
         * Retrieves a list of notes associated with an item.
         * <br>
         * <b> Columns Returned: </b>
         * <UL>
         *  <li> DATE_CREATED </li>
         *  <li> ITEMNOTE_ID </li>
         *  <li> NOTE </li>
         * </UL>
         * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
         *
         * @param conn The database connection to use for this call
         * @param xCaller The accountId and userId properties are used to authenticate this call.
         * @param itemId The item from which to retrieve notes.
         *
         * @return A XanbooResultSet of notes associated with the specified item.
         * @throws XanbooException if the operation failed
         */        
        public XanbooResultSet getNotes(XanbooPrincipal xCaller, long itemId) throws XanbooException, RemoteException;
        
        
        
        /**
         * Deletes a note associated with an item
         * Note deletion is permenant - they do not appear in the wastebasket.
         *
         * @param xCaller The accountId and userId properties are used to authenticate this call
         * @param noteId The Id of the note to delete
         *
         * @throws XanbooException if the deletion failed
         */
        //TODO - change this to accept long[] noteId        
        public void deleteNote(XanbooPrincipal xCaller, long noteId ) throws XanbooException, RemoteException;        
}
