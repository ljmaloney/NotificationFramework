/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/contact/ContactManagerDAO.java,v $
 * $Id: ContactManagerDAO.java,v 1.11 2003/11/13 23:03:03 guray Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.contact;

import java.util.*;
import java.sql.*;

import com.xanboo.core.util.*;
import com.xanboo.core.sdk.util.XanbooResultSet;

/**
 * This class is the DAO class to wrap all generic database calls for SDK ContactManager methods. 
 * Database specific methods are handled by implementation classes. These implementation
 * classes extend the BaseDAO class to actually perform the database operations. An instance of
 * an implementation class is created during contruction of this class.
 */
class ContactManagerDAO extends BaseHandlerDAO {
    
    private BaseDAO dao;
    private Logger logger;
      
    /**
     * Default constructor. Takes no arguments
     *
     * @throws XanbooException
     */
    public ContactManagerDAO() throws XanbooException {
        try {
            // obtain a logger instance
            logger = LoggerFactory.getLogger(this.getClass().getName());
            if (logger.isDebugEnabled()) {
                logger.debug("[ContactManagerDAO()]:");
            }

            // create implementation Class for the database (Oracle, Sybase, etc.)
            dao = (BaseDAO) DAOFactory.getDAO();
            // get the Connection factory DataSource for CoreDS
            getDataSource(GlobalNames.COREDS);

        } catch(XanbooException xe) {
            throw xe;
        }catch(Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error( "[ContactManagerDAO()]: " + e.getMessage(), e);
            }else {
                logger.error( "[ContactManagerDAO()]: " + e.getMessage());
            }            
            throw new XanbooException(20014, "[ContactManagerDAO()]: " + e.getMessage() );
        }
    }
    
   /**
     * Creates a new Xanboo Contact
     * @param xContact A XanbooContact object from which the new contact information is extracted.
     *
     * @throws XanbooException
     */

    public void newContact(Connection conn, XanbooContact xContact) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[newContact()]: ");
        }

        // prepare parameters for a stored procedure
        SQLParam[] args = new SQLParam[32+2];    // sp params + errno, errmsg

        // set the input paramters
        args[0] = new SQLParam(new Long(xContact.getAccountId()), Types.BIGINT); // account id
        args[1] = new SQLParam(new Long(xContact.getUserId()), Types.BIGINT);    // user id
        args[2] = new SQLParam(new Integer(xContact.getType()), Types.INTEGER);  //type
        args[3] = new SQLParam(xContact.getCompany());
        args[4] = new SQLParam(xContact.getLastName());
        args[5] = new SQLParam(xContact.getFirstName());
        args[6] = new SQLParam(xContact.getMiddleName());
        args[7] = new SQLParam(xContact.getPhone());
        args[8] = new SQLParam(xContact.getCellPhone());
        args[9] = new SQLParam(xContact.getFax());
        args[10] = new SQLParam(xContact.getAddress1());
        args[11] = new SQLParam(xContact.getAddress2());
        args[12] = new SQLParam(xContact.getCity());
        args[13] = new SQLParam(xContact.getState());
        args[14] = new SQLParam(xContact.getZip());
        args[15] = new SQLParam(xContact.getZip4());
        args[16] = new SQLParam(xContact.getAreaCode()); 
        args[17] = new SQLParam(xContact.getCountry());
        args[18] = new SQLParam(xContact.getEmail());
        args[19] = new SQLParam(xContact.getPager());
        args[20] = new SQLParam(xContact.getUrl());
        args[21] = new SQLParam(xContact.getRelationship());
        args[22] = new SQLParam(xContact.getGender());
        //sms
        StringBuilder smsB = new StringBuilder();
        smsB.append(xContact.getPhone1SMS() ? "1" : "0");
        smsB.append(xContact.getPhone2SMS() ? "1" : "0");
        smsB.append(xContact.getPhone3SMS() ? "1" : "0");
        args[23] = new SQLParam(smsB.toString());
        args[24] = (xContact.getEmailProfileType() == -1 ? new SQLParam(null,Types.NULL) : new SQLParam(xContact.getEmailProfileType(),Types.BIGINT));
        args[25] = new SQLParam(xContact.getPhone1Type());
        args[26] = (xContact.getPhone1ProfileType()== -1 ? new SQLParam(null,Types.NULL) : new SQLParam(xContact.getPhone1ProfileType(),Types.BIGINT));
        args[27] = new SQLParam(xContact.getPhone2Type());
        args[28] = (xContact.getPhone2ProfileType()== -1 ? new SQLParam(null,Types.NULL) : new SQLParam(xContact.getPhone2ProfileType(),Types.BIGINT));
        args[29] = new SQLParam(xContact.getPhone3Type());
        args[30] = (xContact.getPhone3ProfileType()== -1 ? new SQLParam(null,Types.NULL) : new SQLParam(xContact.getPhone3ProfileType(),Types.BIGINT));

        // set the output parameter: contact id (default in xContact is -1)
        args[31] = new SQLParam(new Long(xContact.getContactId()), Types.BIGINT, true);


        // now, execute the stored procedure to add the contact
        try {
            dao.callSP(conn, "XC_CONTACT_PKG.NEWCONTACT", args, false);
        }catch(XanbooException xe) {
            throw xe;
        }

        // was successfully added, set the new contact id in xContact
        xContact.setContactId(((Long) args[31].getParam()).longValue());
    }

    
   /**
     * Updates a Xanboo Contact
     * @param xContact A XanbooContact object from which the new contact information is extracted.
     *
     * @throws XanbooException
     */
    public void updateContact(Connection conn, XanbooContact xContact) throws XanbooException {
        if (logger.isDebugEnabled()) {
          logger.debug("[updateContact()]: ");
        }

        // prepare parameters for a stored procedure
        SQLParam[] args = new SQLParam[31+2];    // sp params + errno, errmsg

        // note, diff. from new: no output parameters, and contact id replaces type in args[2]
        // set the input paramters
        args[0] = new SQLParam(new Long(xContact.getAccountId()), Types.BIGINT); // account id
        args[1] = new SQLParam(new Long(xContact.getUserId()), Types.BIGINT);    // user id
        args[2] = new SQLParam(new Long(xContact.getContactId()), Types.BIGINT);  // contact id
        args[3] = new SQLParam(xContact.getCompany());
        args[4] = new SQLParam(xContact.getLastName());
        args[5] = new SQLParam(xContact.getFirstName());
        args[6] = new SQLParam(xContact.getMiddleName());
        args[7] = new SQLParam(xContact.getPhone());
        args[8] = new SQLParam(xContact.getCellPhone());
        args[9] = new SQLParam(xContact.getFax());
        args[10] = new SQLParam(xContact.getAddress1());
        args[11] = new SQLParam(xContact.getAddress2());
        args[12] = new SQLParam(xContact.getCity());
        args[13] = new SQLParam(xContact.getState());
        args[14] = new SQLParam(xContact.getZip());
        args[15] = new SQLParam(xContact.getZip4());
        args[16] = new SQLParam(xContact.getAreaCode()); 
        args[17] = new SQLParam(xContact.getCountry());
        args[18] = new SQLParam(xContact.getEmail());
        args[19] = new SQLParam(xContact.getPager());
        args[20] = new SQLParam(xContact.getUrl());
        
        args[21] = new SQLParam(xContact.getRelationship());
        args[22] = new SQLParam(xContact.getGender());
        //sms
        StringBuilder smsB = new StringBuilder();
        smsB.append(xContact.getPhone1SMS() ? "1" : "0");
        smsB.append(xContact.getPhone2SMS() ? "1" : "0");
        smsB.append(xContact.getPhone3SMS() ? "1" : "0");
        args[23] = new SQLParam(smsB.toString());
        args[24] = (xContact.getEmailProfileType() == -1 ? new SQLParam(null,Types.NULL) : new SQLParam(xContact.getEmailProfileType(),Types.BIGINT));
        args[25] = new SQLParam(xContact.getPhone1Type());
        args[26] = (xContact.getPhone1ProfileType()== -1 ? new SQLParam(null,Types.NULL) : new SQLParam(xContact.getPhone1ProfileType(),Types.BIGINT));
        args[27] = new SQLParam(xContact.getPhone2Type());
        args[28] = (xContact.getPhone2ProfileType()== -1 ? new SQLParam(null,Types.NULL) : new SQLParam(xContact.getPhone2ProfileType(),Types.BIGINT));
        args[29] = new SQLParam(xContact.getPhone3Type());
        args[30] = (xContact.getPhone3ProfileType()== -1 ? new SQLParam(null,Types.NULL) : new SQLParam(xContact.getPhone3ProfileType(),Types.BIGINT));

        // now, execute the stored procedure to update the contact information
        try {
            dao.callSP(conn, "XC_CONTACT_PKG.UPDATECONTACT", args, false);
        }catch(XanbooException xe) {
            throw xe;
        }
    }

    
    /**
     * Deletes a Xanboo contact 
     * @param accountId the account id of the user
     * @param userId the user id
     * @param contactId the contact id(s) that will have its information deleted
     * 
     * @throws XanbooException
     */
    public void deleteContact(Connection conn, long accountId, long userId, long contactId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteContact()]: ");
        }

        // prepare parameters for a stored procedure
        SQLParam[] args = new SQLParam[3+2];    // sp params + errno, errmsg

        // set the input paramters
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT); // account id
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);    // user id
        args[2] = new SQLParam(new Long(contactId), Types.BIGINT); // contact ids

        // now, execute the stored procedure to delete the contact
        if ( GlobalNames.MODULE_WASTEBASKET_ENABLED ) {
            dao.callSP(conn, "XC_CONTACT_PKG.DELETECONTACT", args, false);
        } else {
            dao.callSP(conn, "XC_CONTACT_PKG.DELETECONTACTIMMEDIATE", args, false);
        }
        
    }

    
    /**
     * Gets a Xanboo contact 
     * @param accountId the account id of the user
     * @param userId the user id
     * @param contactId the contact id that will have its information retreived
     * 
     * @return a Xanboo contact record
     * @throws XanbooException
     */
    public XanbooResultSet getContact(Connection conn, long accountId, long userId, long contactId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getContact()]: ");
        }

        // prepare parameters for a stored procedure
        SQLParam[] args = new SQLParam[3+2];    // sp params + errno, errmsg

        // set the input paramters
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT); // account id
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);    // user id
        args[2] = new SQLParam(new Long(contactId), Types.BIGINT); // contact id

        XanbooResultSet qResult=null;    
        // now, execute the stored procedure to get the contact
        try {
            return dao.callSP(conn, "XC_CONTACT_PKG.GETCONTACT", args);
        }catch(XanbooException xe) {
            throw xe;
        }
     }

     
    /**
     * Retrieves one of more contacts
     * @param accountId the account id of the user
     * @param userId the user id
     * @param sortBy predefined number representing a sort by field and sorting order
     * and must be one of the {@link ContactManagerEJB#SORT_BY_LASTNAME_ASC sorting numbers} for contacts
     *
     * @return list of Xanboo contact records
     * @throws XanbooException
     */
    public XanbooResultSet getContactList(Connection conn, long accountId, long userId, int sortBy) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getContactList()]:");
        }
         
        XanbooResultSet qResults = null;
        SQLParam[] args=new SQLParam[4+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam(new Integer(sortBy), Types.INTEGER);
        args[3] = new SQLParam(new Integer(-1), Types.INTEGER, true);

        try {
            qResults = dao.callSP(conn, "XC_CONTACT_PKG.GETCONTACTLIST", args);
            qResults.setSize(((Integer) args[3].getParam()).intValue());
        }catch(XanbooException xe) {
            throw xe;
        }

        return qResults;
     }

     
    /**
     * Retrieves one of more contacts given a start point and number of returned contacts
     * @param accountId the account id of the user
     * @param userId the user id
     * @param startRow starting row number
     * @param numRows max number of returned rows
     * @param sortBy predefined number representing a sort by field and sorting order
     * and must be one of the {@link ContactManagerEJB#SORT_BY_LASTNAME_ASC sorting numbers} for contacts
     *
     * @return list of Xanboo contact records
     * @throws XanbooException
     */
     public XanbooResultSet getContactList(Connection conn, long accountId, long userId, 
                      int startRow, int numRows, int sortBy) throws XanbooException {

        if(logger.isDebugEnabled()) {
            logger.debug("[getContactList()]:");
        }
          
        XanbooResultSet qResults = null;
        SQLParam[] args=new SQLParam[4+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam(new Integer(sortBy), Types.INTEGER);
        args[3] = new SQLParam(new Integer(-1), Types.INTEGER, true);

        try {
            qResults=dao.callSP(conn, "XC_CONTACT_PKG.GETCONTACTLIST", args, startRow, numRows);
            qResults.setSize(((Integer) args[3].getParam()).intValue());
        }catch(XanbooException xe) {
            throw xe;
        }
        
        return qResults;
     }
     
    // contact group methods -------------------------------------

     
     /**
      * Creates a new Contact Group
      * @param xCGroup a Xanboo Contact Group object from which the new contact group information is extracted.
      *
      * @throws XanbooException
      */
     public void newContactGroup(Connection conn, XanbooContactGroup xCGroup) throws XanbooException {
         if (logger.isDebugEnabled()) {
             logger.debug("[newContactGroup()]: ");
         }
         
         // prepare parameters for a stored procedure
         SQLParam[] args = new SQLParam[5+2];    // sp params + errno, errmsg
         
         // set the input paramters
         args[0] = new SQLParam(new Long(xCGroup.getAccountId()), Types.BIGINT); // account id
         args[1] = new SQLParam(new Long(xCGroup.getUserId()), Types.BIGINT);    // user id
         args[2] = new SQLParam(xCGroup.getCGroupName());  // group name
         args[3] = new SQLParam(xCGroup.getDescription());  // group description
         
         // set the output parameter: cgroup id (default in xCGroup is -1)
         args[4] = new SQLParam(new Long(xCGroup.getCGroupId()), Types.BIGINT, true);
         
         // now, execute the stored procedure to add the contact
         try {
             dao.callSP(conn, "XC_CONTACT_PKG.NEWCONTACTGROUP", args, false);
         }catch(XanbooException xe) {
             throw xe;
         }
         
         xCGroup.setCGroupId(((Long) args[4].getParam()).longValue());
     }
    
    
     /**
      * Updates a Xanboo Contact Group
      * @param xCGroup A XanbooContactGroup object from which the new contact group information is extracted.
      *
      * @throws XanbooException
      */
     public void updateContactGroup(Connection conn, XanbooContactGroup xCGroup) throws XanbooException {
         if (logger.isDebugEnabled()) {
             logger.debug("[updateContactGroup()]: ");
         }
         
         // prepare parameters for a stored procedure
         SQLParam[] args = new SQLParam[5+2];    // sp params + errno, errmsg
         
         // set the input paramters
         args[0] = new SQLParam(new Long(xCGroup.getAccountId()), Types.BIGINT); // account id
         args[1] = new SQLParam(new Long(xCGroup.getUserId()), Types.BIGINT);    // user id
         args[2] = new SQLParam(new Long(xCGroup.getCGroupId()), Types.BIGINT);    // contact group id
         args[3] = new SQLParam(xCGroup.getCGroupName());  // group name
         args[4] = new SQLParam(xCGroup.getDescription());  // group description
         
         // now, execute the stored procedure to update the contact information
         try {
             dao.callSP(conn, "XC_CONTACT_PKG.UPDATECONTACTGROUP", args, false);
         }catch(XanbooException xe) {
             throw xe;
         }
     }

    
     /**
      * Deletes a Xanboo contact group
      * @param accountId the account id of the user
      * @param userId the user id
      * @param cgroupId the contact group id(s) that will have its information deleted
      *
      * @throws XanbooException
      */
     public void deleteContactGroup(Connection conn, long accountId, long userId, long cgroupId) throws XanbooException {
         if (logger.isDebugEnabled()) {
             logger.debug("[deleteContactGroup()]: ");
         }
         
         // prepare parameters for a stored procedure
         SQLParam[] args = new SQLParam[3+2];    // sp params + errno, errmsg
         
         // set the input paramters
         args[0] = new SQLParam(new Long(accountId), Types.BIGINT); // account id
         args[1] = new SQLParam(new Long(userId), Types.BIGINT);    // user id
         args[2] = new SQLParam(new Long(cgroupId), Types.BIGINT); // contact group ids
         
         // now, execute the stored procedure to delete the contact group and all its members
         if ( GlobalNames.MODULE_WASTEBASKET_ENABLED ) {
             dao.callSP(conn, "XC_CONTACT_PKG.DELETECONTACTGROUP", args, false);
         } else {
             dao.callSP(conn, "XC_CONTACT_PKG.DELETECONTACTGROUPIMMEDIATE", args, false);
         }
         
     }

    
     /**
      * Deletes one or more contact group members
      * @param accountId the account id of the user
      * @param userId the user id
      * @param cgroupId the contact group id that will have one or more of its members deleted
      * @param contactIds member(s) in a contact group that will be deleted
      *
      * @throws XanbooException
      */
     public void deleteContactGroupMember(Connection conn, long accountId, long userId, long cgroupId, long contactId) throws XanbooException {
         if (logger.isDebugEnabled()) {
             logger.debug("[deleteContactGroupMember()]: ");
         }
         
         // prepare parameters for a stored procedure
         SQLParam[] args = new SQLParam[4+2];    // sp params + errno, errmsg
         
         // set the input paramters
         args[0] = new SQLParam(new Long(accountId), Types.BIGINT); // account id
         args[1] = new SQLParam(new Long(userId), Types.BIGINT);    // user id
         args[2] = new SQLParam(new Long(cgroupId), Types.BIGINT);  // contact group id
         args[3] = new SQLParam(new Long(contactId), Types.BIGINT); // contact ids in the selected group
         
         // now, execute the stored procedure to delete the contact group and all its members
         try {
             dao.callSP(conn, "XC_CONTACT_PKG.DELETECONTACTGROUPMEMBER", args, false);
         }catch(XanbooException xe) {
             throw xe;
         }
     }

    
     /**
      * Adds a new Contact to a group
      * @param accountId the account id of the user
      * @param userId the user id
      * @param cgroupId the contact group id that will have new members
      * @param contactIds contact id(s) that will be added to the group
      *
      * @throws XanbooException
      */
     public void newContactGroupMember(Connection conn, long accountId, long userId, long cgroupId, long[] contactIds) throws XanbooException {
         if (logger.isDebugEnabled()) {
             logger.debug("[newContactGroupMember()]: ");
         }
         
         // prepare parameters for a stored procedure
         String csv = XanbooUtil.getCSV(contactIds);
         SQLParam[] args = new SQLParam[4+2];    // sp params + errno, errmsg
         
         // set the input paramters
         args[0] = new SQLParam(new Long(accountId), Types.BIGINT); // account id
         args[1] = new SQLParam(new Long(userId), Types.BIGINT);    // user id
         args[2] = new SQLParam(new Long(cgroupId), Types.BIGINT);  // contact group id
         args[3] = new SQLParam(csv); // contact ids in the selected group
         
         // now, execute the stored procedure to add the contact(s)
         try {
             dao.callSP(conn, "XC_CONTACT_PKG.NEWCONTACTGROUPMEMBER", args, false);
         }catch(XanbooException xe) {
             throw xe;
         }
     }

    
     /**
      * Retrieves contact group information
      * @param accountId the account id of the user
      * @param userId the user id
      * @param cgroupId the group id
      *
      * @return Xanboo contact group record
      * @throws XanbooException
      */
     public XanbooResultSet getContactGroup(Connection conn, long accountId, long userId, long cgroupId) throws XanbooException {
         if (logger.isDebugEnabled()) {
             logger.debug("[getContactGroup()]: ");
         }
         
         // prepare parameters for a stored procedure
         SQLParam[] args = new SQLParam[3+2];    // sp params + errno, errmsg
         
         // set the input paramters
         args[0] = new SQLParam(new Long(accountId), Types.BIGINT); // account id
         args[1] = new SQLParam(new Long(userId), Types.BIGINT);    // user id
         args[2] = new SQLParam(new Long(cgroupId), Types.BIGINT); // contact group id
         
         XanbooResultSet qResult=null;
         // now, execute the stored procedure to get the contact group
         try {
             return dao.callSP(conn, "XC_CONTACT_PKG.GETCONTACTGROUP", args);
         }catch(XanbooException xe) {
             throw xe;
         }
     }
    
     /**
      * Adds a new Contact to a group
      * @param accountId the account id of the user
      * @param userId the user id
      * @param cgroupId the contact group id that will have new members
      * @param contactIds contact id(s) that will be added to the group
      *
      * @return a list of Xanboo contact group records
      * @throws XanbooException
      */
     public XanbooResultSet getContactGroupList(Connection conn, long accountId, long userId) throws XanbooException {
         if(logger.isDebugEnabled()) {
             logger.debug("[getContactGroupList()]:");
         }
         
         SQLParam[] args=new SQLParam[2+2];     // SP parameters + 2 std parameters (errno, errmsg)
         // set IN params
         args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
         args[1] = new SQLParam(new Long(userId), Types.BIGINT);
         
         try {
             return dao.callSP(conn, "XC_CONTACT_PKG.GETCONTACTGROUPLIST", args);
         }catch(XanbooException xe) {
             throw xe;
         }

     }
    
    
     /**
      * Retrieves one of more contacts in a group
      * @param accountId the account id of the user
      * @param userId the user id
      * @cgroupId the contact group id
      *
      * @return list of Xanboo contacts in a group
      * @throws XanbooException
      */
     public XanbooResultSet getContactGroupMemberList(Connection conn, long accountId, long userId, long cgroupId) throws XanbooException {
         if(logger.isDebugEnabled()) {
             logger.debug("[getContactGroupList()]:");
         }
         
         SQLParam[] args=new SQLParam[3+2];     // SP parameters + 2 std parameters (errno, errmsg)
         // set IN params
         args[0] = new SQLParam(new Long(accountId), Types.BIGINT);  // account id
         args[1] = new SQLParam(new Long(userId), Types.BIGINT);   // user id
         args[2] = new SQLParam(new Long(cgroupId), Types.BIGINT); // contact group id
         
         try {
             return dao.callSP(conn, "XC_CONTACT_PKG.GETCONTACTGROUPMEMBERLIST", args);
         }catch(XanbooException xe) {
             throw xe;
         }
     }

}