/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/account/XanbooUser.java,v $
 * $Id: XanbooUser.java,v 1.10 2010/10/08 18:24:59 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.account;
import java.util.Date;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.util.XanbooUtil;

/**
 * Class to represent Xanboo users
 */
public class XanbooUser implements java.io.Serializable {
    private static final long serialVersionUID = 8044609285295005442L;

    /** Constant value to indicate user is in inactive status */
    public static final int STATUS_INACTIVE=1;

    /** Constant value to indicate user is in active status */
    public static final int STATUS_ACTIVE=0;

    /** Constant value to indicate user status should not be changed on update operations */
    public static final int STATUS_UNCHANGED=-1;
    

    /** Holds user id for the user. */
    private long userId;
    
    /** Holds account id for the user. */
    private long accountId;

    /** Holds domain id for the user. */
    private String domainId;
    
    /** Holds username for the user. */
    private String username;
    
    /** Holds password for the user. */
    private String password;

    /** Flag to indicate if the user is a master account user or not. */
    private boolean isMaster;
    
    /** Holds user status. */
    private int statusId;
    
    /** Holds preferred language id for the user. */
    private String languageId;
       
    /** Holds the timezone value for the user. */
    private String timezoneId="-";
    
    /** Holds user's email address. */
    private String email;

    /** Holds an optional external user id for the user. */
    private String extUserId;
    
    /** Holds optional, extra preferences for the user. */
    private String prefs;

    /** Holds account fifo purgin preference. */
    private int fifoPurging;
    
    /** Flag to indicate if the current user password is temporary or not. */
    private int isTempPassword;

    /** Security question objects associated with the user */
    private XanbooSecurityQuestion[] secQ;
    
    /** Hold ptype%phone_cell value to determine if password reset email needs to be sent to SMS or specfic profile tpe. */
    private String passwordnotificationProfile;

    private Date lastPasswordChange;

    /* name-value pairs of personal attributes.
     * Format: name1=value1&name2=value2...
     */
    private String profileData;
    /** Default constructor: Creates a new XanbooUser */
    public XanbooUser() {
        setUserId(-1L);
        setAccountId(-1L);
        setUsername("");
        setMaster(false);
        setLanguage(null);
        setTimezone("GMT");
        setStatus(STATUS_UNCHANGED);
        setPrefs("");
        setFifoPurgingFlag(-1);
        setSecurityQuestions(null);
        setTemporaryPasswordFlag(-1);
    }
    
    
    /** Creates new XanbooUser from given parameters 
     * @param userId       user id
     * @param accountId    account id
     * @param username     user name
     * @param password     user password
     * @param isMaster     flag to indicate if the user is a master account user or not
     * @param languageId   preferred user language id (e.g. "en", "en_US", "fr", "fr_CA", etc.)
     * @param timezoneId   a supported timezone id for the user
     * @param statusId     user status
     */
    public XanbooUser(long userId, long accountId, String username, String password, 
                        boolean isMaster, String languageId, String timezoneId, int statusId) {
        setUserId(userId);
        setAccountId(accountId);
        setUsername(username);
        setTemporaryPasswordFlag(-1);
        setPassword(password);
        setMaster(isMaster);
        setLanguage(languageId);
        setTimezone(timezoneId);
        setStatus(statusId);
        setPrefs("");
        setFifoPurgingFlag(-1);
        setSecurityQuestions(null);
    }
    
    /** Creates new XanbooUser from given parameters 
     * @param accountId    account id
     * @param username     user name
     * @param password     user password
     */
    public XanbooUser( long accountId, String username, String password ) {
        setAccountId(accountId);
        setUsername(username);
        setTemporaryPasswordFlag(-1);
        setPassword(password);
        setPrefs("");
        setFifoPurgingFlag(-1);
        setSecurityQuestions(null);
        setLanguage(null);
    }
    
     /** Creates a new XanbooUser from a XanbooResultSet object and given row 
      * @param xrs a XanbooResulSet object containing user query results from an SDK call
      * @param row row number to construct the object from. Must be a >=0 value and cannot be bigger than resulset size
      */
    
    public XanbooUser(XanbooResultSet xrs, int row) throws InstantiationException {
        if(xrs==null || row<0 || row>=xrs.size()) {
            throw new InstantiationException("Invalid resultset or row");
        }

        setAccountId(xrs.getElementLong(row, "ACCOUNT_ID"));
        setDomain(xrs.getElementString(row, "DOMAIN_ID"));
        setUserId(xrs.getElementLong(row, "USER_ID"));
        setUsername(xrs.getElementString(row, "USERNAME"));
        ////setPassword(xrs.getElementString(row, "PASSWORD"));
        setTemporaryPasswordFlag(xrs.getElementInteger(row, "IS_TEMP_PASSWORD"));
        setStatus(xrs.getElementInteger(row, "STATUS_ID"));
        setLanguage(xrs.getElementString(row, "LANGUAGE_ID"));
        setTimezone(xrs.getElementString(row, "TZNAME"));
        setEmail(xrs.getElementString(row, "EMAIL"));
        setMaster( (xrs.getElementInteger(row, "IS_MASTER")== 1 ) ? true : false);
        setExtUserId(xrs.getElementString(row, "EXT_USER_ID"));
        setPrefs(xrs.getElementString(row, "PREFS"));
        setFifoPurgingFlag(xrs.getElementInteger(row, "ENABLE_FIFO_PURGE"));

        /* set security questions if set */
        XanbooSecurityQuestion xsq1 = new XanbooSecurityQuestion(xrs.getElementInteger(row, "SECQ_Q1"), xrs.getElementString(row, "SECQ_A1"));
        XanbooSecurityQuestion[] xsq = null;
        if(xsq1.isValid()) {
            xsq1.setSecAnswer(null);    //null answer since it is hashed anyways
            xsq = new XanbooSecurityQuestion[XanbooSecurityQuestion.MAX_SUPPORTED];
            xsq[0] = xsq1;
            
            XanbooSecurityQuestion xsq2 = new XanbooSecurityQuestion(xrs.getElementInteger(row, "SECQ_Q2"), xrs.getElementString(row, "SECQ_A2"));
            if(xsq2.isValid()) {
                xsq2.setSecAnswer(null);  //null answer since it is hashed anyways
                xsq[1] = xsq2;
            }else {
                xsq[1] = null;
            }
        }
        
        
        setSecurityQuestions(xsq);
        setProfileData(xrs.getElementString(row, "PROFILE_DATA"));        
        setLastPasswordChange(xrs.getElementDate(row, "LAST_PASSWORD_CHANGE"));   
    }
    
    
    /** Gets the id for the user.
     * @return user id.
     */
    public long getUserId() {
        return this.userId;
    }
    
    /** Sets the id for the user.
     * @param userId user id to set.
     */
    public void setUserId(long userId) {
        if(userId<-1) userId=-1;
        this.userId = userId;
    }
    
    /** Gets user account id.
     * @return user account id.
     */
    public long getAccountId() {
        return this.accountId;
    }
    
    /** Sets user account id.
     * @param accountId user account id to set.
     */
    public void setAccountId(long accountId) {
        if(accountId<-1) accountId=-1;
        this.accountId = accountId;
    }
    
    
    /** Gets the domain id of the user account.
     * @return account domain id.
     */
    public String getDomain() {
        return this.domainId;
    }
    
    /** Sets the domain id for the useraccount.
     * @param domainId New value of property domainId.
     */
    public void setDomain(String domainId) {
        this.domainId = domainId;
    }
    
    
    /** Gets user login name.
     * @return user login name.
     */
    public String getUsername() {
        return this.username;
    }
    
    /** Sets user login name.
     * @param username new user login name.
     */
    public void setUsername(String username) {
        if(username!=null)
            this.username = username.trim();
        else
            this.username = null;
    }
    
    /** Gets user login password.
     * @return user login password.
     * Query calls will return the password as null since it is hashed and not readable.
     */
    public String getPassword() {
        return this.password;
    }
    
    /** Sets user login password.
     * @param password new user login password.
     */
    public void setPassword(String password) {
        if(password!=null)
            this.password = password.trim();
        else
            this.password = null;
        
    }

    /** Sets user login password with an internally generated temporary password.
     * When this call is used, the temporary password flag for the user is set
     */
    public void setTemporaryPassword() {
        this.password = XanbooUtil.generatePassword(10);   //generates a temp password of max length 10
        setTemporaryPasswordFlag(1);
    }
    
    
    /** Checks if the user is a master account user or not.
     * @return master boolean flag.
     */
    public boolean isMaster() {
        return this.isMaster;
    }
    
    /** Sets the master flag for the user.
     * @param isMaster boolean value to set the master flag.
     */
    public void setMaster(boolean isMaster) {
        this.isMaster = isMaster;
    }

    
    /** Gets the user language setting.
     * @return language id.
     */
    public String getLanguage() {
        return this.languageId;
    }
    
    /** Sets the user language setting.
     * @param languageId language id to set.
     */
    public void setLanguage(String languageId) {
        this.languageId = languageId;
    }
    
    
    /** Gets the user timezone setting.
     * @return timezone id.
     */
    public String getTimezone() {
        return this.timezoneId;
    }
    
    /** Sets the user timezone setting.
     * @param timezoneId timezone id to set .
     */
    public void setTimezone(String timezoneId) {
        this.timezoneId = timezoneId;
    }

    
    /** Gets the user status.
     * @return user status value. Possible values are XanbooUser.STATUS_INACTIVE and XanbooUser.STATUS_ACTIVE.
     */   
    public int getStatus() {
        return statusId;
    }
    
    /** Sets the user status.
     * @param statusId status id value. Possible values are XanbooUser.STATUS_INACTIVE, XanbooUser.STATUS_ACTIVE and XanbooUser.STATUS_UNCHANGED.
     * For update user operations, the status will be updated only if NOT set to XanbooUser.STATUS_UNCHANGED!
     */
    public void setStatus(int statusId) {
        if(statusId!=STATUS_ACTIVE && statusId!=STATUS_INACTIVE && statusId!=STATUS_UNCHANGED) return;
        this.statusId = statusId;
    }
    
    public void dump() {
        System.err.println("USER ID      :" + this.userId);
        System.err.println("ACCOUNT ID  :" + this.accountId);
        System.err.println("USERNAME    :" + this.username);
        System.err.println("PASSWORD    :" + this.password);
        System.err.println("STATUS      :" + this.statusId);
        System.err.println("ISMASTER    :" + (this.isMaster ? "YES" : "NO"));
        System.err.println("LANGUAGE    :" + this.languageId);
        System.err.println("TIMZEZONE   :" + this.timezoneId);
        System.err.println("LAST PASSWORD CHANGE   :" + this.lastPasswordChange);
    }
    
    /** Gets user's email address.
     * @return Value of property email.
     */
    public String getEmail() {
        return email;
    }
    
    /** Sets user's email address.
     * @param email New value of property email.
     */
    public void setEmail(String email) {
        this.email = email;
    }
    
    /** Gets user's external user id, if exists.
     * @return Value of property extUserId.
     */
    public String getExtUserId() {
        return extUserId;
    }
    
    /** Sets user's external user id.
     * @param extUserId New value of property extUserId.
     */
    public void setExtUserId(String extUserId) {
        this.extUserId = extUserId;
    }

    /** Gets extra preferences for the user.
     * @return Value of property prefs.
     */
    public String getPrefs() {
        return prefs;
    }
    
    /** Sets extra user preferences.
     * @param extUserId New value of property prefs.
     */
    public void setPrefs(String prefs) {
        this.prefs = prefs;
    }

    /** Gets the account fifo purging preference (only for master users).
     * @return flag fifo purging preference flag (0:disabled, 1:enabled)
     */
    public int getFifoPurgingFlag() {
        return this.fifoPurging;
    }

    /** Sets the account fifo purging preference (only for master users).
     * @param flag fifo purging flag (0:disabled, 1:enabled). -1 value will not update purging flag.
     */
    public void setFifoPurgingFlag(int fifoPurging) {
        if(fifoPurging>=0 && fifoPurging<=1)  //only 0 and 1 allowed for now to disable/enable purging
            this.fifoPurging = fifoPurging;
        else
            this.fifoPurging = -1;
    }

    /** Checks if the user has a temp password or not. 
     * @return flag (0:not-temporary, 1:temporary).
     */
    public int getTemporaryPasswordFlag() {
        return this.isTempPassword;
    }
    
    /** Sets the temporary password flag for the user.
     * @param flag to set (0:not-temporary, 1:temporary).  -1 value will not update the flag.
     */
    public void setTemporaryPasswordFlag(int flag) {
        if(flag>=-2 && flag<=1)  //only 0 and 1 allowed for now, and -1 as unset, and -2 as password reset starts with @ - email sent
            this.isTempPassword = flag;
        else
            this.isTempPassword = -1;
    }


    /** Returns security questions for the User. 
     * @return an array of XanbooSecurityQuestion objects. Null array return indicates no security questions defined for the user. 
     *         Non-null, valid array elements indicate that particular security question is set/defined.
     *         Query calls will return the security question answer fields as null since they are hashed and not readable.
     */
    public XanbooSecurityQuestion[] getSecurityQuestions() {
        return this.secQ;
    }

    /** Sets security questions for the User object.
     * Security questions for a XanbooUser can be added/updated/cleared via updateUser call. newUser calls ignore security question array and will not add them to the user entry !!!
     * @param XanbooSecurityQuestion[] an array of XanbooSecurityQuestion objects to be associated with the user.
     * 
     * When a non-null XanbooSecurityQuestion array is passed thru UpdateUser calls, the following rules are applicable:
     * Null array members or XanbooSecurityQuestion objects with questionId set to XanbooSecurityQuestion.UNCHANGED will NOT be updated!
     * Only Non-null XanbooSecurityQuestion objects passed in the array with a valid questionID greater than 0 will be updated. 
     * Non-null XanbooSecurityQuestion objects with questionId set to XanbooSecurityQuestion.UNSET will be cleared/deleted! 
     * 
     * @throws IllegalArgumentException if the passed array of size greater than supported max security questions (see XanbooSecurityQuestion.MAX_SUPPORTED)
     */
    public void setSecurityQuestions(XanbooSecurityQuestion[] secQ) throws IllegalArgumentException {
        if(secQ!=null && secQ.length>XanbooSecurityQuestion.MAX_SUPPORTED) {
            throw new IllegalArgumentException();
        }
        this.secQ = secQ;
    }
    
    
    public String toString() {
        return "XUSER:" + this.getUserId() + ":" + this.getUsername();
    }
    
    /** 
     @param set passwordnotificationProfile ptype%phone_cell value to send the reset password email to the right profile type(ex:SMS)
    */
   public void setPasswordnotificationProfile(String passwordnotificationProfile) {
       this.passwordnotificationProfile = passwordnotificationProfile;
   }

   /** 
    * @return passwordnotificationProfile ptype%phone_cell value to send the reset password email to the right profile type(ex:SMS) 
    */
   public String getPasswordnotificationProfile() {
       return this.passwordnotificationProfile;
   }

   /**
    * @return a string containing name-value pairs of personal attributes
    */
    public String getProfileData() {
        return profileData;
    }

    /**
     *
     * @param profileData assumed to be a String containing name-value pairs of personal attributes
     * Format: name1=value1&name2=value2...
     */
    public void setProfileData(String profileData) {
        this.profileData = profileData;
    }

    public Date getLastPasswordChange() {
        return lastPasswordChange;
    }

    public void setLastPasswordChange(Date lastPasswordChange) {
        this.lastPasswordChange = lastPasswordChange;
    }
}
