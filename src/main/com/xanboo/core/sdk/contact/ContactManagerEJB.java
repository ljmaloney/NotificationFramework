/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/contact/ContactManagerEJB.java,v $
 * $Id: ContactManagerEJB.java,v 1.18 2003/11/13 23:03:03 guray Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */


package com.xanboo.core.sdk.contact;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.ejb.CreateException;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import com.xanboo.core.mbus.MBusSynchronizer;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.GlobalNames;
import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;
import com.xanboo.core.util.XanbooException;
import com.xanboo.core.util.XanbooUtil;

/* todo : if the account is inactive, we shouldn't allow any operations from the beginning */

/**
 * Session Bean implementation of <code>ContactManager</code>. This bean acts as a wrapper class for
 * all contact and contact group related Core SDK methods.
 *
 */
@Remote (ContactManager.class)
@Stateless (name="ContactManager")
@TransactionManagement( TransactionManagementType.BEAN )
public class ContactManagerEJB  {
 
   private Logger logger;
   
   // related DAO class
   private ContactManagerDAO dao=null;
   
    //define sort by fields for contacts, in ascending and descending orders   
    /** 
     * Sort by last name in an ascending order, default
     */
    public static final int SORT_BY_LASTNAME_ASC = 1;
    /** 
     * Sort by last name in a descending order
     */
    public static final int SORT_BY_LASTNAME_DESC = -1;    
    /** 
     * Sort by email in an ascending order
     */
    public static final int SORT_BY_EMAIL_ASC = 2;
    /** 
     * Sort by last name in a descending order
     */
    public static final int SORT_BY_EMAIL_DESC = -2;
    /**
     * Maximum or minimum number (if negative) that could be used to define sorting
     */
    private final int SORT_BY_LIMIT = 2;  //for range check

    @PostConstruct
   public void init() throws CreateException {
       try {
           // create a logger instance
           logger = LoggerFactory.getLogger(this.getClass().getName());
           dao = new ContactManagerDAO();
        }catch (Exception se) {
            if(logger.isDebugEnabled()) {
              logger.error("[ejbCreate()]: " + se.getMessage(), se);
            }else {
              logger.error("[ejbCreate()]: " + se.getMessage());
            }               
            throw new CreateException("[ejbCreate()]: " + se.getMessage());
        }
   }


   
   // Business methods
    

   public long newContact(XanbooPrincipal xCaller, XanbooContact xContact) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[newContact()]:");
        }
        boolean doMBusSync = true;//MBus 
        // first validate the caller and privileges
        try {
            XanbooUtil.checkCallerPrivilege(xCaller);
        }catch(XanbooException xe) {
        	doMBusSync = false;//MBus 
            throw xe;
        }        
        
        // validate 
        try { 
             xContact.setAccountId(xCaller.getAccountId());
             xContact.setUserId(xCaller.getUserId());
             xContact.setType(1);    // make sure it is contact information
             if (!xContact.isValid()) {
                throw new XanbooException(10050);  
            }
            //set default values for phone[n]ProfileType
            if ( xContact.getPhone() != null && !xContact.getPhone().equalsIgnoreCase("")
                 && xContact.getPhone1SMS() && xContact.getPhone1ProfileType() == -1 )
                xContact.setPhone1ProfileType(GlobalNames.SMS_PROFILE_TYPE);
            if ( xContact.getCellPhone() != null && !xContact.getCellPhone().equalsIgnoreCase("") 
                 && xContact.getPhone2SMS() && xContact.getPhone2ProfileType() == -1)
                xContact.setPhone2ProfileType(GlobalNames.SMS_PROFILE_TYPE);
            if ( xContact.getFax()!= null && !xContact.getFax().equalsIgnoreCase("") 
                 && xContact.getPhone3SMS() && xContact.getPhone3ProfileType() == -1)
                xContact.setPhone3ProfileType(GlobalNames.SMS_PROFILE_TYPE);
            //set default values for emailProfileType
            if ( xContact.getEmail() != null && xContact.getEmail().equalsIgnoreCase("") && xContact.getEmailProfileType() == -1)
                xContact.setEmailProfileType(GlobalNames.EMAIL_PROFILE_TYPE);
                
        }catch(Exception e) {
        	doMBusSync = false;//MBus 
            throw new XanbooException(10050);
        }
        
        // create the contact in the database
        Connection conn = null;
        boolean rollback=false;
        
        try {
            conn = dao.getConnection();       // get a connection to the database
            dao.newContact(conn, xContact);   // add the contact information to the db
        }catch(XanbooException xe) {
        	doMBusSync = false;//MBus 
            rollback=true;
            throw xe;
        }catch(Exception e) {
        	doMBusSync = false;//MBus 
            rollback=true;
            if(logger.isDebugEnabled()) {
              logger.error("[newContact()]: " + e.getMessage(), e);
            }else {
              logger.error("[newContact()]: " + e.getMessage());
            }               
            throw new XanbooException(10030, "[newContact()]: Exception:" + e.getMessage());
        }finally {    
            dao.closeConnection(conn,rollback);
            
            //MBus Start- Send user.update message
            if(GlobalNames.MBUS_SYNC_ENABLED && doMBusSync){
            	

                       	
            	 if(logger.isDebugEnabled()) {
            		 
            		 StringBuffer sb = new StringBuffer("[MBus : publish messages : ContactManagerEJB.newContact()]:");
            		 sb.append("\n Domain : " + xCaller.getDomain() );
            		 sb.append("\n AccountId : " + xCaller.getAccountId());
            		 sb.append("\n Email : " + xContact.getEmail() );
            		 
            		 sb.append("\n login : " + xContact.getUsername() );
            		 sb.append("\n ph1 - phone : " + xContact.getPhone());
            		 sb.append("\n ph2 - cellphone : " + xContact.getCellPhone() );
            		 sb.append("\n fname : " + xContact.getFirstName() );
            		 sb.append("\n lname : " + xContact.getLastName() );
            		 
            		 sb.append("\n UserId : " + xCaller.getUserId() );
            		
            		 
                     logger.debug( sb.toString() );
                   }
            	 
            	MBusSynchronizer.updateUser( xCaller.getDomain(), xCaller.getAccountId(), null, xContact.getUsername(), xContact.getEmail(), null, xContact.getPhone(), xContact.getCellPhone(),
            			xContact.getFirstName(), xContact.getLastName(), xCaller.getUserId() ,  "ContactManagerEJB.newContact" );
   
            }
            // MBus end
        }

        // return a new contact id
        return xContact.getContactId();   
   }


   public void updateContact(XanbooPrincipal xCaller, XanbooContact xContact) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateContact()]:");
        }
        boolean doMBusSync = true;//MBus 
        // first validate the caller and privileges
        XanbooUtil.checkCallerPrivilege(xCaller);

        // validate the contact information and the contact id
        try {
            xContact.setAccountId(xCaller.getAccountId());
            xContact.setUserId(xCaller.getUserId());
            if ((xContact.getContactId() <= 0L) || (!xContact.isValid())) {
                throw new XanbooException(10050);  
            }
            
            //set default values for phone[n]ProfileType
            if ( xContact.getPhone() != null && !xContact.getPhone().equalsIgnoreCase("")
                 && xContact.getPhone1SMS() && xContact.getPhone1ProfileType() == -1 )
                xContact.setPhone1ProfileType(GlobalNames.SMS_PROFILE_TYPE);
            if ( xContact.getCellPhone() != null && !xContact.getCellPhone().equalsIgnoreCase("") 
                 && xContact.getPhone2SMS() && xContact.getPhone2ProfileType() == -1)
                xContact.setPhone2ProfileType(GlobalNames.SMS_PROFILE_TYPE);
            if ( xContact.getFax()!= null && !xContact.getFax().equalsIgnoreCase("") 
                 && xContact.getPhone3SMS() && xContact.getPhone3ProfileType() == -1)
                xContact.setPhone3ProfileType(GlobalNames.SMS_PROFILE_TYPE);
            //set default values for emailProfileType
            if ( xContact.getEmail() != null && xContact.getEmail().equalsIgnoreCase("") && xContact.getEmailProfileType() == -1)
                xContact.setEmailProfileType(GlobalNames.EMAIL_PROFILE_TYPE);
            
        }catch(Exception e) {
        	doMBusSync = false;//MBus 
            throw new XanbooException(10050);
        }
        
        // create the contact in the database
        Connection conn = null;
        boolean rollback = false;
        
        try {
            conn = dao.getConnection();          // get a connection to the database
            dao.updateContact(conn, xContact);   // update existing contact information in the db
        }catch(XanbooException xe) {
        	doMBusSync = false;//MBus 
            rollback = true;
            throw xe;
        }catch(Exception e) {
            rollback = true;
            doMBusSync = false;//MBus 
            if(logger.isDebugEnabled()) {
              logger.error("[updateContact()]: " + e.getMessage(), e);
            }else {
              logger.error("[updateContact()]: " + e.getMessage());
            }                
            throw new XanbooException(10030, "[updateContact()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
            
            //MBus Start- Send user.update message
            if(GlobalNames.MBUS_SYNC_ENABLED && doMBusSync){
            	

                       	
            	 if(logger.isDebugEnabled()) {
            		 
            		 StringBuffer sb = new StringBuffer("[MBus : publish messages : ContactManagerEJB.updateContact()]:");
            		 sb.append("\n Domain : " + xCaller.getDomain() );
            		 sb.append("\n AccountId : " + xCaller.getAccountId());
            		 sb.append("\n Email : " + xContact.getEmail() );
            		 
            		 sb.append("\n login : " + xContact.getUsername() );
            		 sb.append("\n ph1 - phone : " + xContact.getPhone());
            		 sb.append("\n ph2 - cellphone : " + xContact.getCellPhone() );
            		 sb.append("\n fname : " + xContact.getFirstName() );
            		 sb.append("\n lname : " + xContact.getLastName() );
            		 
            		 sb.append("\n UserId : " + xCaller.getUserId() );
            		
            		 
                     logger.debug( sb.toString() );
                   }
            	 
            	MBusSynchronizer.updateUser( xCaller.getDomain(), xCaller.getAccountId(), null, xContact.getUsername(), xContact.getEmail(), null, xContact.getPhone(), xContact.getCellPhone(),
            			xContact.getFirstName(), xContact.getLastName(), xCaller.getUserId() ,   "ContactManagerEJB.updateContact" );
   
            }
            // MBus end
            
        }
   }
   

   public void deleteContact(XanbooPrincipal xCaller, long[] contactIds) throws XanbooException {
       if (logger.isDebugEnabled()) {
            logger.debug("[deleteContact()]:");
        }
       
        // first validate the caller and privileges
        long accountId;
        long userId;
        try {
             XanbooUtil.checkCallerPrivilege(xCaller);
             accountId = xCaller.getAccountId();
             userId = xCaller.getUserId();
        }catch(XanbooException xe) {
            throw xe;
        }        
       
        if ((accountId <= 0L) || (userId <= 0L) || (contactIds == null) || (contactIds.length == 0)) {
            throw new XanbooException(10050);  
        }
        
        // delete the contact from the database
        Connection conn = null;
        boolean rollback = false;
        
        try {
            conn = dao.getConnection();          // get a connection to the database
            for ( int i=0; i<contactIds.length; i++ ) {
                dao.deleteContact(conn, accountId, userId, contactIds[i]);   // delete the contact
            }
        }catch(XanbooException xe) {
            rollback = true;
            throw xe;
        }catch(Exception e) {
            rollback = true;
            if(logger.isDebugEnabled()) {
              logger.error("[deleteContact()]: " + e.getMessage(), e);
            }else {
              logger.error("[deleteContact()]: " + e.getMessage());
            }                 
            throw new XanbooException(10030, "[deleteContact()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn, rollback);
        }
   }
  

    public XanbooContact getContact(XanbooPrincipal xCaller, long contactId) throws XanbooException {
       if (logger.isDebugEnabled()) {
            logger.debug("[getContact()]:");
        }
        
        // validate the parameters before sending them to the database
        long accountId;
        long userId;
        
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        // first validate the caller and privileges
        try {
            accountId = xCaller.getAccountId();
            userId = xCaller.getUserId();
            if ((accountId <= 0L) || (userId <= 0L) || (contactId <= 0L)) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        // get the contact from the database
        Connection conn = null;
        
        try {
            conn = dao.getConnection();          // get a connection to the database
            ArrayList qResult=dao.getContact(conn, accountId, userId,contactId);
                       
            HashMap row = (HashMap) qResult.get(0);

            XanbooContact xContact = new XanbooContact();
            xContact.setAccountId(accountId);
            xContact.setUserId(userId);
            xContact.setContactId(contactId);
        
            xContact.setCompany((String) row.get("COMPANY"));
            xContact.setLastName((String) row.get("LASTNAME"));
            xContact.setFirstName((String) row.get("FIRSTNAME"));
            xContact.setMiddleName((String) row.get("MIDDLENAME"));
            xContact.setPhone((String) row.get("PHONE"));
            xContact.setCellPhone((String) row.get("PHONE_CELL"));
            xContact.setFax((String) row.get("FAX"));
            xContact.setAddress1((String) row.get("ADDRESS1"));
            xContact.setAddress2((String) row.get("ADDRESS2"));
            xContact.setCity((String) row.get("CITY"));
            xContact.setState((String) row.get("STATE"));
            xContact.setZip((String) row.get("ZIP"));
            xContact.setZip4((String) row.get("ZIP4"));
            xContact.setAreaCode((String) row.get("AREACODE"));
            xContact.setCountry((String) row.get("COUNTRY"));
            xContact.setEmail((String) row.get("EMAIL"));
            xContact.setPager((String) row.get("PAGER_ID"));
            xContact.setUrl((String) row.get("URL"));
            
            xContact.setRelationship((String)row.get("RELATIONSHIP"));
            xContact.setGender((String)row.get("GENDER"));
            xContact.setPhone1Type((String)row.get("PHONE_1_TYPE"));
            xContact.setPhone2Type((String)row.get("PHONE_2_TYPE"));
            xContact.setPhone3Type((String)row.get("PHONE_3_TYPE"));
            if ( row.get("EMAIL_PTYPE") != null && !row.get("EMAIL_PTYPE").toString().equalsIgnoreCase(""))
                xContact.setEmailProfileType(Long.parseLong((String)row.get("EMAIL_PTYPE")));
            if ( row.get("PHONE_1_PTYPE") != null && !row.get("PHONE_1_PTYPE").toString().equalsIgnoreCase(""))
                xContact.setPhone1ProfileType(Long.parseLong((String)row.get("PHONE_1_PTYPE")));
            if ( row.get("PHONE_2_PTYPE") != null && !row.get("PHONE_2_PTYPE").toString().equalsIgnoreCase(""))
                xContact.setPhone2ProfileType(Long.parseLong((String)row.get("PHONE_2_PTYPE")));
            if ( row.get("PHONE_3_PTYPE") != null && !row.get("PHONE_3_PTYPE").toString().equalsIgnoreCase(""))
                xContact.setPhone3ProfileType(Long.parseLong((String)row.get("PHONE_3_PTYPE")));
            
            String smsPrefs = (String)row.get("SMS_PREFS");
            if ( smsPrefs != null && !smsPrefs.equalsIgnoreCase(""))
            {
                xContact.setPhone1SMS(smsPrefs.charAt(0) == '1' ? true : false);
                xContact.setPhone2SMS(smsPrefs.charAt(1) == '1' ? true : false);
                xContact.setPhone3SMS(smsPrefs.charAt(2) == '1' ? true : false);
            }
            return xContact;
            
        }catch(XanbooException xe) {
            throw xe;
        }catch(Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getContact()]: " + e.getMessage(), e);
            }else {
              logger.error("[getContact()]: " + e.getMessage());
            }               
            throw new XanbooException(25005, "[getContact()]: Exception:" + e.getMessage());
         }finally {    
            dao.closeConnection(conn);
         }
    }  
   

   public XanbooResultSet getContactList(XanbooPrincipal xCaller) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getContactList()]:");
        }
       
        // validate the parameters
        long accountId;
        long userId;      
        
        XanbooUtil.checkCallerPrivilege(xCaller);

        try {
            accountId = xCaller.getAccountId();
            userId = xCaller.getUserId();
            if ( (accountId <= 0L) || (userId <=0L) )  {
               throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        
        try {
            conn=dao.getConnection();
            return dao.getContactList(conn,accountId,userId,this.SORT_BY_LASTNAME_ASC);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getContactList()]: " + e.getMessage(), e);
            }else {
              logger.error("[getContactList()]: " + e.getMessage());
            }               
            throw new XanbooException(10030, "[getContactList()]: Exception:" + e.getMessage() );
        }finally {
            dao.closeConnection(conn);
        }
     }
    

   public XanbooResultSet getContactList(XanbooPrincipal xCaller, int startRow, int numRows, int sortBy) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getContactList(start,num)]:");
        }
       
        // validate the parameters
        long accountId;
        long userId;      

        XanbooUtil.checkCallerPrivilege(xCaller);
        
        // first validate the caller and privileges
        try {
            accountId = xCaller.getAccountId();
            userId = xCaller.getUserId();
            if ( (accountId <= 0L) || (userId <=0L) || (startRow < 0) || (numRows < 0) 
                    || (sortBy < (-1*this.SORT_BY_LIMIT))
                    || (sortBy > this.SORT_BY_LIMIT))  {
                throw new XanbooException(10050);
            }
        } catch ( Exception e ) {
            throw new XanbooException(10050);
        }
            
        
        Connection conn=null;
        
        try {
            conn=dao.getConnection();
            return dao.getContactList(conn,accountId,userId,startRow,numRows,sortBy);
        }catch (XanbooException xe) {
            throw xe;
        }catch (Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getContactList(start,num)]: " + e.getMessage(), e);
            }else {
              logger.error("[getContactList(start,num)]: " + e.getMessage());
            }                  
            throw new XanbooException(10030, "[getContactList(start,num)]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn);
        }
     }
     
    // contact group methods ------------------------------
     

    public long newContactGroup(XanbooPrincipal xCaller, XanbooContactGroup xCGroup) throws XanbooException {
     if (logger.isDebugEnabled()) {
            logger.debug("[newContactGroup()]:");
        }
                  
        // first validate the caller and privileges
        XanbooUtil.checkCallerPrivilege(xCaller);
     
        // validate 
        try {
            xCGroup.setAccountId(xCaller.getAccountId());
            xCGroup.setUserId(xCaller.getUserId());
            if (!xCGroup.isValid()) {
                throw new XanbooException(10050);  
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        // create the contact group in the database
        Connection conn = null;
        boolean rollback = false;
        
        try {
            conn = dao.getConnection();       // get a connection to the database
            dao.newContactGroup(conn, xCGroup);   // add the contact group information to the db
        }catch(XanbooException xe) {
            rollback = true;
            throw xe;
        }catch(Exception e) {
            rollback = true;
            if(logger.isDebugEnabled()) {
              logger.error("[newContactGroup()]: " + e.getMessage(), e);
            }else {
              logger.error("[newContactGroup()]: " + e.getMessage());
            }               
            throw new XanbooException(10030, "[newContactGroup()]: Exception:" + e.getMessage());
        }finally {    
            dao.closeConnection(conn, rollback);
        }
            
        // return a new contact id
        return xCGroup.getCGroupId();   
    }
    
    

    public void updateContactGroup(XanbooPrincipal xCaller, XanbooContactGroup xCGroup) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateContactGroup()]:");
        }
        
        // first validate the caller and privileges
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        // validate the contact group information
        try {
            xCGroup.setAccountId(xCaller.getAccountId());
            xCGroup.setUserId(xCaller.getUserId());
            if (!xCGroup.isValid()) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        // create the contact in the database
        Connection conn = null;
        boolean rollback = false;
        
        try {
            conn = dao.getConnection();          // get a connection to the database
            dao.updateContactGroup(conn, xCGroup);   // update existing contact group information in the db
        }catch(XanbooException xe) {
            rollback = true;
            throw xe;
        }catch(Exception e) {
            rollback = true;
            if(logger.isDebugEnabled()) {
              logger.error("[updateContactGroup()]: " + e.getMessage(), e);
            }else {
              logger.error("[updateContactGroup()]: " + e.getMessage());
            }             
            throw new XanbooException(10030, "[updateContactGroup()]: Exception:" + e.getMessage());
        }finally {    
            dao.closeConnection(conn,rollback);
        }
    }
    

    public void deleteContactGroup(XanbooPrincipal xCaller, long[] cgroupIds) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteContactGroup()]:");
        }
        

        XanbooUtil.checkCallerPrivilege(xCaller);
        
        // validate the parameters before sending them to the database
        long accountId;
        long userId;
        
        // first validate the caller and privileges
        try {
            accountId = xCaller.getAccountId();
            userId = xCaller.getUserId();
            if ((accountId <= 0L) || (userId <= 0L) || (cgroupIds == null) || (cgroupIds.length==0)) {
                throw new XanbooException(10050);  
            }
        }catch(Exception e) {
            throw new XanbooException(10050);  
        }
       
        // delete the contact group(s) from the database
        Connection conn = null;
        boolean rollback = false;
        
        try {
            conn = dao.getConnection();          // get a connection to the database
            for ( int i=0; i<cgroupIds.length; i++ ) {
                dao.deleteContactGroup(conn, accountId, userId, cgroupIds[i]);   // delete the contact group(s)
            }
        }catch(XanbooException xe) {
            rollback = true;
            throw xe;
        }catch(Exception e) {
            rollback = true;
            if(logger.isDebugEnabled()) {
              logger.error("[deleteContactGroup()]: " + e.getMessage(), e);
            }else {
              logger.error("[deleteContactGroup()]: " + e.getMessage());
            }                     
            throw new XanbooException(10030, "[deleteContactGroup()]: Exception:" + e.getMessage());
        }finally {    
            dao.closeConnection(conn,rollback);
        }
    }
    
    
 
    public void newContactGroupMember(XanbooPrincipal xCaller, long cgroupId, long[] contactIds) throws XanbooException {
       if (logger.isDebugEnabled()) {
            logger.debug("[newContactGroupMember()]:");
        }
                  
        // first validate the caller and privileges
        XanbooUtil.checkCallerPrivilege(xCaller);

        // validate 
        long accountId;
        long userId;
        try {
            accountId = xCaller.getAccountId();
            userId = xCaller.getUserId();
            if ((accountId <= 0L) || (userId <= 0L) || (contactIds == null) || (contactIds.length == 0)) {
                throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
       
        // create the contact member(s) in the database
        Connection conn = null;
        boolean rollback = false;
        
        try {
            conn = dao.getConnection();       // get a connection to the database
            dao.newContactGroupMember(conn, accountId,userId,cgroupId,contactIds);   // add the contact(s) to the group
        }catch(XanbooException xe) {
            rollback = true;
            throw xe;
        }catch(Exception e) {
            rollback = true;
            if(logger.isDebugEnabled()) {
              logger.error("[newContactGroupMember()]: " + e.getMessage(), e);
            }else {
              logger.error("[newContactGroupMember()]: " + e.getMessage());
            }                                 
            throw new XanbooException(10030, "[newContactGroupMember()]: Exception:" + e.getMessage());
        }finally {    
            dao.closeConnection(conn,rollback);
        }
    }
    

    public void deleteContactGroupMember(XanbooPrincipal xCaller, long cgroupId, long[] contactIds) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteContactGroupMember()]:");
        }
        
        // validate the parameters before sending them to the database
        long accountId;
        long userId;
        
        // first validate the caller and privileges
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            accountId = xCaller.getAccountId();
            userId = xCaller.getUserId();
            if ((accountId <= 0L) || (userId <= 0L) || (cgroupId <= 0L) || (contactIds == null) || (contactIds.length == 0)) {
                throw new XanbooException(10050);  
            }
        }catch(Exception e) {
            throw new XanbooException(10050);  
        }
        
        
        // delete the contact group(s) from the database
        Connection conn = null;
        boolean rollback = false;
        
        try {
            conn = dao.getConnection();          // get a connection to the database
            for ( int i=0; i< contactIds.length ; i++ ) {
                dao.deleteContactGroupMember(conn, accountId, userId, cgroupId, contactIds[i]);   // delete the contact(s) from the group
            }
        }catch(XanbooException xe) {
            rollback = true;
            throw xe;
        }catch(Exception e) {
            rollback = true;
            if(logger.isDebugEnabled()) {
              logger.error("[deleteContactGroupMember()]: " + e.getMessage(), e);
            }else {
              logger.error("[deleteContactGroupMember()]: " + e.getMessage());
            }              
            throw new XanbooException(10030, "[deleteContactGroupMember()]: Exception:" + e.getMessage());
        }finally {    
            dao.closeConnection(conn,rollback);
        }
    }
    

    public XanbooContactGroup getContactGroup(XanbooPrincipal xCaller, long cgroupId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getContactGroup()]:");
        }
        
        // validate the parameters before sending them to the database
        long accountId;
        long userId;
        // first validate the caller and privileges
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            accountId = xCaller.getAccountId();
            userId = xCaller.getUserId();
            if ((accountId <= 0L) || (userId <= 0L) || (cgroupId <= 0L)) {
                throw new XanbooException(10050);  
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        
        // get the contact from the database
        Connection conn = null;
        boolean rollback = false;
        
        try {
            conn = dao.getConnection();          // get a connection to the database
            ArrayList qResult=dao.getContactGroup(conn, accountId, userId,cgroupId);
                       
            HashMap row = (HashMap) qResult.get(0);

            XanbooContactGroup xCGroup = new XanbooContactGroup();
            xCGroup.setAccountId(accountId);
            xCGroup.setUserId(userId);
            xCGroup.setCGroupId(cgroupId);
        
            xCGroup.setCGroupName((String) row.get("NAME"));
            xCGroup.setDescription((String) row.get("DESCRIPTION"));
               
            return xCGroup;
            
        }catch(XanbooException xe) {
            rollback = true;
            throw xe;
        }catch(Exception e) {
            rollback = true;
            if(logger.isDebugEnabled()) {
              logger.error("[getContactGroup()]: " + e.getMessage(), e);
            }else {
              logger.error("[getContactGroup()]: " + e.getMessage());
            }               
            throw new XanbooException(25005, "[getContactGroup()]: Exception:" + e.getMessage());
         }finally {    
            dao.closeConnection(conn,rollback);
         }
    }
    

    public XanbooResultSet getContactGroupList(XanbooPrincipal xCaller) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getContactGroupList()]:");
        }
       
        long accountId;
        long userId;      
        // first validate the caller and privileges
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            accountId = xCaller.getAccountId();
            userId = xCaller.getUserId();
            if ( (accountId <= 0L) || (userId <= 0L) ) {
                throw new XanbooException(10050);
            }
        }catch(Exception xe) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback = false;
        
        try {
            conn=dao.getConnection();
            return dao.getContactGroupList(conn,accountId,userId);
        }catch (XanbooException xe) {
            rollback = true;
            throw xe;
        }catch (Exception e) {
            rollback = true;
            if(logger.isDebugEnabled()) {
              logger.error("[getContactGroupList()]: " + e.getMessage(), e);
            }else {
              logger.error("[getContactGroupList()]: " + e.getMessage());
            }                           
            throw new XanbooException(10030, "[getContactGroupList()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn,rollback);
        }
    }
    

    public XanbooResultSet getContactGroupMemberList(XanbooPrincipal xCaller, long cgroupId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getContactGroupMemberList()]:");
        }
       
        // validate the parameters
        long accountId;
        long userId;      
        // first validate the caller and privileges
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        try {
            accountId = xCaller.getAccountId();
            userId = xCaller.getUserId();
            if ((accountId <= 0L) || (userId <=0L) || (cgroupId <=0L))  {
               throw new XanbooException(10050);
            }
        }catch(Exception e) {
            throw new XanbooException(10050);
        }
        
        Connection conn=null;
        boolean rollback = false;
        
        try {
            conn=dao.getConnection();
            return dao.getContactGroupMemberList(conn,accountId,userId,cgroupId);
        }catch (XanbooException xe) {
            rollback = true;
            throw xe;
        }catch (Exception e) {
            rollback = true;
            if(logger.isDebugEnabled()) {
              logger.error("[getContactGroupMemberList()]: " + e.getMessage(), e);
            }else {
              logger.error("[getContactGroupMemberList()]: " + e.getMessage());
            }            
            throw new XanbooException(10030, "[getContactGroupMemberList()]: Exception:" + e.getMessage());
        }finally {
            dao.closeConnection(conn,rollback);
        }
       
    }
}
