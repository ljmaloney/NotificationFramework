/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/wastebasket/WastebasketManager.java,v $
 * $Id: WastebasketManager.java,v 1.7 2003/07/18 20:22:57 guray Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.wastebasket;

import java.rmi.RemoteException;

import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.XanbooException;

/**
 * Remote Interface for the WastebasketManagerEJB
 * <p>
 * When items, folders and contacts are deleted, they appear in the user's wastebasket. While in the wastebasket, items will not count towards the account's quota.
 * </p>
 * <p>
 *  Objects in the wastebasket have a type field. Type may be one of:
 *  <ol>
 *      <li> 0 - Inbox Item </li>
 *      <li> 1 - Folder </li>
 *      <li> 2 - Folder Item </li>
 *      <li> 6 - Contact </li>
 *      <li> 7 - Contact group </li>
 *  </ol>
 *</p>
 *<p>
 *
 */


public interface WastebasketManager   
{
    /**
     * Deletes one or more items from the user's wastebasket.
     * <p>
     * Once deleted from the wastebaseket, an item is permenantly deleted, and may never be restored.
     * </p>
     *
     * @param xCaller a XanbooPrincipal object
     * @param trashItemIds a list containing one or more trash item id that will be permenantly deleted
     * 
     * @throws XanbooException if the delete failed
     */    
    public void deleteItem(XanbooPrincipal xCaller, long[] trashItemIds) throws RemoteException, XanbooException;
  
  
  
    /**
     * Restores one or more items from the wastebasket, optionally, to a folder.
     * <br>
     * When folders, inbox items, and folder items are restored, they are resotred to the specified folder. The ID must therefore be that of a valid folder beloning to the current account.
     * <br>
     * In the case of contacts and contact groups the targetFolderId is ignored, and they are simply restored to their base location.
     * <br>
     * @param xCaller a XanbooPrincipal object
     * @param trashItemIds a list containing one or more trash item id that will be restored
     * @param targetFolderId specifies the folder in which to place the restored items.
     * 
     * @throws XanbooException if the undelete failed.
     */  
    public void undeleteItem(XanbooPrincipal xCaller, long[] trashItemIds, long targetFolderId) throws RemoteException, XanbooException;
  
  
  
   /**
     * Retrieves a list of items that exist in the caller's wastebasket.
     * <br>
     * The wastebasket contains deleted items, folders, contacts and contact groups. The OBJECTTYE_ID column specifies the type of the trash item - see above.
     *
     * <br>
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> OBJECT_SIZE </li>
     *  <li> DESCRIPTION </li>
     *  <li> DATE_CREATED </li>
     *  <li> OBJECTTYPE_ID </li>
     *  <li> OBJECT_ID </li>
     *  <li> TRASHITEM_ID </li>
     *  <li> ACCOUNT_ID </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooPrincipal object
     *
     * @return list of wastebasket items in a XanbooResultSet object
     * @throws XanbooException if the operation failed
     */  
    public XanbooResultSet getItemList(XanbooPrincipal xCaller) throws RemoteException, XanbooException;
  
  
  
    /**
     * Retrieves a list of items that exist in the caller's wastebasket with pagination controls.
     * <br>
     * The wastebasket contains deleted items, folders, contacts and contact groups. The OBJECTTYE_ID column specifies the type of the trash item - see above.
     *
     * <br>
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> OBJECT_SIZE </li>
     *  <li> DESCRIPTION </li>
     *  <li> DATE_CREATED </li>
     *  <li> OBJECTTYPE_ID </li>
     *  <li> OBJECT_ID </li>
     *  <li> TRASHITEM_ID </li>
     *  <li> ACCOUNT_ID </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooPrincipal object
     * @param startRow starting row number
     * @param numRows max number of returned rows
     *
     * @return list of wastebasket items in a XanbooResultSet object
     * @throws XanbooException if the operation failed
     */  
    public XanbooResultSet getItemList(XanbooPrincipal xCaller, int startRow, int numRows) throws RemoteException, XanbooException;
  
  
  
    /**
     * Permenantly deletes all items from the account's wastebasket.
     * <br>
     * Emptied items may not be restored.
     *
     * @param xCaller a XanbooPrincipal object
     * 
     * @throws XanbooException if the empty failed.
     */  
    public void emptyWB(XanbooPrincipal xCaller) throws RemoteException, XanbooException;
}
