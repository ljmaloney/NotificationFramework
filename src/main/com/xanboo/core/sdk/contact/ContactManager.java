/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/contact/ContactManager.java,v $
 * $Id: ContactManager.java,v 1.2 2003/07/18 20:22:56 guray Exp $
 *
 * Copyright 2002 Xanboo, Inc.
 *
 */

 package com.xanboo.core.sdk.contact;

 import java.rmi.RemoteException;

import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.XanbooException;

  
 /**
  * Remote Interface for the ContactManagerEJB
  *
  */
 public interface ContactManager  
 {
   /**
     * Adds a new contact to the account specified by xCaller.
     * A contact is an address book entry that contains various information, such as name, address etc.
     * For a full list of properties associated, see the <CODE>XanbooContact</CODE> class.
     *
     * @param xCaller a XanbooPrincipal object identifying the caller.
     * @param xContact A XanbooContact object from which the new contact information is extracted.
     * 
     * @return the contact id associated with the newly created contact.
     * @throws XanbooException if the contact failed to be added.
     */
    public long newContact(XanbooPrincipal xCaller,XanbooContact xContact) throws RemoteException, XanbooException;
    
    
    
   /**
     * Updates the information associated with an individual XanbooContact.
     * The contact is identified by the ID field of the XanbooContact object, and all of it's associated parameters are updated
     * with the new information supplied.<br>
     * A simple way to update just a few parameters of a contact is to first retrieve the desired XanbooContact using the getContact method,
     * applying the changes, and then passing it into this method.
    *
     * @param xCaller a XanbooPrincipal object identifying the caller.
     * @param xContact A XanbooContact object from which the new contact information is extracted.
     * 
     * @throws XanbooException if the update failed.
     */    
    public void updateContact(XanbooPrincipal xCaller,XanbooContact xContact) throws RemoteException, XanbooException;
    
    
    
   /**
     * Deletes one or more Xanboo contacts by supplying their Id(s).
     * Deleted contacts will appear in the account's wastebasket if wastebasket is enabled as defined by 'module.wastebasket.enabled' parameter in xancore-config..
     *
     * @param xCaller a XanbooPrincipal object identifying the caller.
     * @param contactIds A collection of one or more contact ids that will be deleted
     * 
     * @throws XanbooException if the delete failed.
     */    
    public void deleteContact(XanbooPrincipal xCaller, long[] contactIds) throws RemoteException, XanbooException;
    
    
    
   /**
     * Gets a single XanbooContact object for a contact specified by it's ID.
    *
     * @param xCaller a XanbooPrincipal object identifying the caller.
     * @param contactId the contact id that will have its information retrieved
     * 
     * @return XanbooContact object
     * @throws XanbooException
     */    
    public XanbooContact getContact(XanbooPrincipal xCaller, long contactId) throws RemoteException, XanbooException;
    
    
    
   /**
     * Returns a list of all XanbooContacts associated with the xCaller's account.
     *
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> CONTACT_ID </li>
     *  <li> LASTNAME </li>
     *  <li> FIRSTNAME </li>
     *  <li> MIDDLENAME </li>
     *  <li> ADDRESS1 </li>
     *  <li> ADDRESS2 </li>
     *  <li> CITY </li>
     *  <li> STATE </li>
     *  <li> COUNTRY </li>
     *  <li> ZIP </li>
     *  <li> ZIP4 </li>
     *  <li> EMAIL </li>
     *  <li> URL </li>
     *  <li> AREACODE </li>
     *  <li> TYPE </li>
     *  <li> PAGER_ID </li>
     *  <li> PHONE_CELL </li>
     *  <li> COMPANY </li>
     *  <li> PHONE </li>
     *  <li> FAX </li>
     *  <li> RELATIONSHIP</li>
     *  <li> GENDER</li>
     *  <li> SMS_PREFS</li>
     *  <li> EMAIL_PTYPE</li>
     *  <li> PHONE_1_TYPE</li>
     *  <li> PHONE_1 PTYPE</li>
     *  <li> PHONE_2_TYPE</li>
     *  <li> PHONE_2 PTYPE</li>
     *  <li> PHONE_3_TYPE</li>
     *  <li> PHONE_3 PTYPE</li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooPrincipal object identifying the caller.
     *
     * @return a list of Xanboo contacts
     * @throws XanbooException if the listing failed.
     */    
    public XanbooResultSet getContactList(XanbooPrincipal xCaller) throws RemoteException, XanbooException;
    
    
    
   /**
     * Returns a list of all XanbooContacts associated with the xCaller's account with pagination controls
     *
     * @param xCaller a XanbooPrincipal object identifying the caller.
     * @param startRow the row at which to begin retrieving records 
     * @param numRows max number of returned rows 
     * @param sortBy predefined number representing a sort by field and sorting order
     * and must be one of the {@link com.xanboo.core.sdk.contact.ContactManagerEJB#SORT_BY_LASTNAME_ASC sorting numbers} for contacts
     *
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> CONTACT_ID </li>
     *  <li> LASTNAME </li>
     *  <li> FIRSTNAME </li>
     *  <li> MIDDLENAME </li>
     *  <li> ADDRESS1 </li>
     *  <li> ADDRESS2 </li>
     *  <li> CITY </li>
     *  <li> STATE </li>
     *  <li> COUNTRY </li>
     *  <li> ZIP </li>
     *  <li> ZIP4 </li>
     *  <li> EMAIL </li>
     *  <li> URL </li>
     *  <li> AREACODE </li>
     *  <li> TYPE </li>
     *  <li> PAGER_ID </li>
     *  <li> PHONE_CELL </li>
     *  <li> COMPANY </li>
     *  <li> PHONE </li>
     *  <li> FAX </li>
     *  <li> RELATIONSHIP</li>
     *  <li> GENDER</li>
     *  <li> SMS_PREFS</li>
     *  <li> EMAIL_PTYPE</li>
     *  <li> PHONE_1_TYPE</li>
     *  <li> PHONE_1 PTYPE</li>
     *  <li> PHONE_2_TYPE</li>
     *  <li> PHONE_2 PTYPE</li>
     *  <li> PHONE_3_TYPE</li>
     *  <li> PHONE_3 PTYPE</li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @return a list of Xanboo contacts
     * @throws XanbooException if the listing failed.
     */    
    public XanbooResultSet getContactList(XanbooPrincipal xCaller,int startRow,int numRows, int sortBy) throws RemoteException, XanbooException;
    
    
    
    /**
     * Adds a new contact group within the caller's account.
     *
     * @param xCaller a XanbooPrincipal object identifying the caller.
     * @param xCGroup A XanbooContactGroup object from which the new contact group information is extracted.
     * 
     * @return the contact group id associated with the newly created contact.
     * @throws XanbooException if the creation failed.
     */     
    public long newContactGroup(XanbooPrincipal xCaller,XanbooContactGroup xCGroup) throws RemoteException, XanbooException;
    
    
    
    /**
     * Updates the information associated with a XanbooContatGroup.<br>
     * The group is identified by the ID field of the XanbooContact object, and all of it's associated parameters are updated with the new ones 
     * supplied.<br>
     * The easiest way to update just a few parameters of a group is to first retrieve the desired XanbooGroup using the getContactGroup method,
     * applying the changes, and then passing it into this method.
     *
     * @param xCaller a XanbooPrincipal object identifying the caller.
     * @param xCGroup A XanbooContactGroup object from which the new contact group information is extracted.
     * 
     * @throws XanbooException if the update failed.
     */    
    public void updateContactGroup(XanbooPrincipal xCaller, XanbooContactGroup xCGroup) throws RemoteException, XanbooException;
    
    
    
    /**
     * Deletes one or more Xanboo contact groups.
     * Deleted contact groups will appear in the account's wastebasket if wastebasket is enabled as defined by 'module.wastebasket.enabled' parameter in xancore-config.
     *
     * @param xCaller a XanbooPrincipal object identifying the caller.
     * @param cgroupIds a collection of one or more contact group ids that will be removed.
     * 
     * @throws XanbooException if the deletion failed.
     */    
    public void deleteContactGroup(XanbooPrincipal xCaller, long[] cgroupIds) throws RemoteException, XanbooException;
    
    
    
    /**
     * Puts a contact or a number of contacts in a group.
     * The contact Ids supplied will be associated with the group specified by the cgroupId parameter. These contacts will subsequently get
     * returned when the getContactGroupMemberList() method is called for the specified group.
     *
     * @param xCaller a XanbooPrincipal object identifying the caller.
     * @param cgroupId a contact group id
     * @param contactId a collection of one or more contact ids to be added to the group
     * 
     * @throws XanbooException if the operation failed.
     */    
    public void newContactGroupMember(XanbooPrincipal xCaller, long cgroupId, long[] contactIds) throws RemoteException, XanbooException; 
    
    
    
    /**
     * Removes a xanboo contact from a group.
     * The association between each of the supplied contacts, and the supplied group will be removed. Note that this does not
     * delete any XanbooContacts or XanbooContactGroups - only the association between the two.
     *
     * @param xCaller a XanbooPrincipal object identifying the caller.
     * @param cgroupIds a list of one or more contact ids that will be deleted from a contact group
     * 
     * @throws XanbooException if the disassociated failed.
     */    
    public void deleteContactGroupMember(XanbooPrincipal xCaller, long cgroupId, long[] contactIds) throws RemoteException, XanbooException;
    
    
    
    /**
     * Gets a Xanboo contact group object for the group of the specified ID.
     * A contact group can be thought of as a container of XanbooContacts. The group may indicate some shared property of all of the contacts within it
     * such as 'friends' or 'family'. Contacts exist independantly of groups, so an individual contact may exist in multiple groups.
     * This methods does not return the contacts that exist within the group, only meta data associated with the group itself.
     * To retrieve the contacts within the group, use <code>getContactGroupMemberList</code>
     *
     * @param xCaller a XanbooPrincipal object identifying the caller.
     * @param cgroupId the contact group id that will have its information retrieved
     * 
     * @return XanbooContactGroup object
     * @throws XanbooException if the operation failed.
     */    
    public XanbooContactGroup getContactGroup(XanbooPrincipal xCaller, long cgroupId) throws RemoteException, XanbooException;
    
    
    
    /**
     * Gets a list of Xanboo contact groups within the caller's account.
     *
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> ACCOUNT_ID </li>
     *  <li> USER_ID </li>
     *  <li> DESCRIPTION </li>
     *  <li> CGROUP_ID </li>
     *  <li> NAME </li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
     *
     * @param xCaller a XanbooPrincipal object identifying the caller.
     *
     * @return a list of Xanboo contact groups
     * @throws XanbooException if the operation failed.
     */    
    public XanbooResultSet getContactGroupList(XanbooPrincipal xCaller) throws RemoteException, XanbooException;
    
    
    
    /**
     * Gets a list of Xanboo contacts that exist within the specified group.
     * 
     * <b> Columns Returned: </b>
     * <UL>
     *  <li> CONTACT_ID </li>
     *  <li> LASTNAME </li>
     *  <li> FIRSTNAME </li>
     *  <li> MIDDLENAME </li>
     *  <li> ADDRESS1 </li>
     *  <li> ADDRESS2 </li>
     *  <li> CITY </li>
     *  <li> STATE </li>
     *  <li> COUNTRY </li>
     *  <li> ZIP </li>
     *  <li> ZIP4 </li>
     *  <li> EMAIL </li>
     *  <li> URL </li>
     *  <li> AREACODE </li>
     *  <li> TYPE </li>
     *  <li> PAGER_ID </li>
     *  <li> PHONE_CELL </li>
     *  <li> COMPANY </li>
     *  <li> PHONE </li>
     *  <li> FAX </li>
     *  <li> RELATIONSHIP</li>
     *  <li> GENDER</li>
     *  <li> SMS_PREFS</li>
     *  <li> EMAIL_PTYPE</li>
     *  <li> PHONE_1_TYPE</li>
     *  <li> PHONE_1 PTYPE</li>
     *  <li> PHONE_2_TYPE</li>
     *  <li> PHONE_2 PTYPE</li>
     *  <li> PHONE_3_TYPE</li>
     *  <li> PHONE_3 PTYPE</li>
     * </UL>
     * (note that in case of version changes, view XanbooResultSet.toXML() to confirm exact column names)<br>
    *
     * @param xCaller a XanbooPrincipal object identifying the caller.
     *
     * @return a list of Xanboo contact members in a contact group
     * @throws XanbooException if the operation failed.
     */    
    public XanbooResultSet getContactGroupMemberList(XanbooPrincipal xCaller, long cgroupId) throws RemoteException, XanbooException;
 }

 
