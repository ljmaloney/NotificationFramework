/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/account/AccountManagerDAO.java,v $
 * $Id: AccountManagerDAO.java,v 1.51 2011/02/11 17:53:31 levent Exp $
 * 
 * Copyright 2011 AT&T Digital Life
 *
 */

package com.xanboo.core.sdk.account;

import java.sql.*;
import java.util.*;
import java.io.*;
import com.xanboo.core.util.*;
import com.xanboo.core.util.fs.*;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.sdk.contact.*;

/**
 * This class is the DAO class to wrap all generic database calls for SDK AccountManager methods. 
 * Database specific methods are handled by implementation classes. These implementation
 * classes extend the BaseDAO class to actually perform the database operations. An instance of
 * an implementation class is created during contruction of this class.
*/
class AccountManagerDAO extends BaseHandlerDAO {

    private BaseDAO dao;
    private Logger logger;
    
    /**
     * Default constructor. Takes no arguments
     *
     * @throws XanbooException
     */
    public AccountManagerDAO() throws XanbooException {

        try {
            // obtain a Logger instance
            logger=LoggerFactory.getLogger(this.getClass().getName());
            if(logger.isDebugEnabled()) {
                logger.debug("[AccountManagerDAO()]:");
            }

            // create implementation Class for Oracle, Sybase, etc.
            dao = (BaseDAO) DAOFactory.getDAO();
            
            // get the Connection factory DataSource for CoreDS
            getDataSource(GlobalNames.COREDS);
            
        }catch(XanbooException xe) {
            throw xe;
        }catch(Exception ne) {
            if(logger.isDebugEnabled()) {
              logger.error("[AccountManagerDAO()]: " + ne.getMessage(), ne);
            }else {
              logger.error("[AccountManagerDAO()]: " + ne.getMessage());
            }               
            throw new XanbooException(20014, "[AccountManagerDAO()]: " + ne.getMessage() );
        }
    }

    
    /**
     * Create a new Xanboo Account
     * @param conn The database connection to use for this call
     * @param xAccount A XanbooAccount object from which the new account information is extracted.
     * @param userQuota user quota value for initial account setup.
     * @param diskQuota disk quota value in Kbytes for initial account setup.
     * @param gatewayQuota gateway quota value for initial account setup.
     *
     * @throws XanbooException
     */
    public XanbooPrincipal newAccount(Connection conn, XanbooAccount xAccount, long userQuota, long diskQuota, long gatewayQuota) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[newAccount()]: ");
        }

        SQLParam[] args=new SQLParam[20+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(xAccount.getDomain());   // domain
        args[1] = new SQLParam(new Integer(xAccount.getType()), Types.INTEGER);     // account type
        args[2] = new SQLParam(new Integer(xAccount.getStatus()), Types.INTEGER);   // status
        args[3] = new SQLParam(xAccount.getExtAccountId());                         // external account id
        args[4] = new SQLParam(xAccount.getUser().getExtUserId());                         // external user id
        
        // setting master user info parameters
        args[5] = new SQLParam(xAccount.getUser().getUsername());
        //generate a salted hash !
        args[6] = new SQLParam(XanbooUtil.hashString(xAccount.getUser().getPassword(), GlobalNames.APP_HASHING_ALGORITHM, "-1"));
        args[7] = new SQLParam(xAccount.getUser().getEmail());
        args[8] = new SQLParam(xAccount.getUser().getLanguage());
        args[9] = new SQLParam(xAccount.getUser().getTimezone());
        
        // setting account quota parameters
        args[10] = new SQLParam(new Long(userQuota), Types.BIGINT);  // user quota
        args[11] = new SQLParam(new Long(diskQuota), Types.BIGINT);  // disk quota
        args[12] = new SQLParam(new Long(gatewayQuota), Types.BIGINT);  // disk quota
        args[13] = new SQLParam(xAccount.getUser().getPrefs());                   //user prefs
        args[14] = new SQLParam(xAccount.getToken());                   //token        
        args[15] = new SQLParam(xAccount.getFifoPurgingFlag()==-1 ? null : xAccount.getFifoPurgingFlag());  //default auto-purging-pref
        
        if(xAccount.getUser().getTemporaryPasswordFlag()==-1)                                      //user temp password flag
            args[16] = new SQLParam(null);
        else
            args[16] = new SQLParam(xAccount.getUser().getTemporaryPasswordFlag());
        
        args[17] = new SQLParam(null,Types.NULL);
        
        // OUT parameters
        args[18] = new SQLParam(new Long(-1), Types.BIGINT, true); //accountId
        args[19] = new SQLParam(new Long(-1), Types.BIGINT, true); //userId
        

        dao.callSP(conn, "XC_ACCOUNT_PKG.NEWACCOUNT", args, false);
 
        xAccount.setAccountId(((Long) args[18].getParam()).longValue());

        // db records ok, now create the account Filesystem directory structure
        // form the directory base path to save account data
        //String baseDir = GlobalNames.getBaseAccountDir(null, xAccount.getDomain(), XanbooUtil.getAccountDir(xAccount.getAccountId()) );
        
        //obtain the mount point to be used for the account and its handler FS provider id (prefix)
        FSMountPoint mp = FSProviderCache.getInstance().getMountPointForAccount(xAccount.getAccountId());

        String itemBaseDir = AbstractFSProvider.getBaseDir(mp.getMountPath(), xAccount.getDomain(), xAccount.getAccountId(), AbstractFSProvider.DIR_ACCOUNT_ITEM);        
        String thumbBaseDir = AbstractFSProvider.getBaseDir(mp.getMountPath(), xAccount.getDomain(), xAccount.getAccountId(), AbstractFSProvider.DIR_ACCOUNT_THUMB); 
        String mobjectBaseDir = AbstractFSProvider.getBaseDir(mp.getMountPath(), xAccount.getDomain(), xAccount.getAccountId(), AbstractFSProvider.DIR_ACCOUNT_MOBJECT);
        
        try {
            //Create the directories
            if(!XanbooFSProviderProxy.getInstance().mkDir(mp.getProviderId(), itemBaseDir)) throw new Exception("Failed to create directory " + itemBaseDir);
            XanbooFSProviderProxy.getInstance().mkDir(mp.getProviderId(), thumbBaseDir);
            XanbooFSProviderProxy.getInstance().mkDir(mp.getProviderId(), mobjectBaseDir);

        }catch(Exception e) {
            logger.debug("[newAccount()]: Can not create account data store. ", e);
            throw new XanbooException(21044, "Can not create account data store." + e.getMessage());
        }

        
        // success
        XanbooPrincipal xp = new XanbooPrincipal(xAccount.getDomain(), xAccount.getUser().getUsername());
        xp.setUserId(((Long) args[19].getParam()).longValue());
        xp.setAccountId(((Long) args[18].getParam()).longValue());
        xp.setMaster(true);
        xp.setLanguage(xAccount.getUser().getLanguage());
        xp.setTimezone(xAccount.getUser().getTimezone());        
        xp.setExtAccountId(xAccount.getExtAccountId());
        
        /* for the new user/subscription ,send the default password to the user */
        if(xAccount.getUser().getPassword()!=null && xAccount.getUser().getTemporaryPasswordFlag()==1 && xAccount.getUser().getEmail()!=null && xAccount.getUser().getEmail().length()>0) {
            queueAction(conn, xAccount.getDomain(), xAccount.getAccountId(), xAccount.getUser().getExtUserId(), "0", "0", "13", xAccount.getUser().getPassword(), "Auto-notify", xAccount.getUser().getLanguage(), xAccount.getUser().getTimezone(), "", xAccount.getUser().getEmail() );
        }

        return xp;
        
    }


    /**
     * Adds a new user to a Xanboo account
     * @param conn The database connection to use for this call
     * @param xUser a XanbooUser object from which the new user information is extracted.
     *
     * @return a user id associated with the newly created user.
     * @throws XanbooException
     */
    public void newUser(Connection conn, XanbooUser xUser) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[newUser()]: ");
        }

        SQLParam[] args=new SQLParam[12+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(xUser.getAccountId()), Types.BIGINT);   // account Id to add the user
        args[1] = new SQLParam(xUser.getExtUserId());                // external account id
        args[2] = new SQLParam(xUser.getUsername());                // user name
        //generate a salted hash !
        args[3] = new SQLParam(XanbooUtil.hashString(xUser.getPassword(), GlobalNames.APP_HASHING_ALGORITHM, "-1"));                // hashed password
        args[4] = new SQLParam(xUser.getEmail());                // email
        args[5] = new SQLParam(xUser.getLanguage());              // language
        args[6] = new SQLParam(xUser.getTimezone());                // timezone
        args[7] = new SQLParam(new Integer(0), Types.INTEGER);      // master flag (only non-masters can be added)
        args[8] = new SQLParam(xUser.getPrefs());                   //user prefs
       
        if(xUser.getTemporaryPasswordFlag()==-1)                                      //user temp password flag
            args[9] = new SQLParam(null);
        else
            args[9] = new SQLParam(xUser.getTemporaryPasswordFlag());

        args[10] = new SQLParam(xUser.getProfileData());

        // OUT parameter: returning user ID
        args[11] = new SQLParam(new Long(-1), Types.BIGINT, true);
        

        dao.callSP(conn, "XC_ACCOUNT_PKG.NEWUSER", args, false);
 
        xUser.setUserId(((Long) args[11].getParam()).longValue());

    }
    

    /**
     * Deletes a Xanboo account user.
     * @param conn The database connection to use for this call
     * @param accountId account id to delete the user from.
     * @param userId    the user id to delete.
     *
     * @throws XanbooException
     */
    public void deleteUser(Connection conn, long accountId, long userId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[deleteUser()]: ");
        }

        SQLParam[] args=new SQLParam[2+2];     // 2 SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);   // owner account Id
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);      // user Id to delete
        
        dao.callSP(conn, "XC_ACCOUNT_PKG.DELETEUSER", args, false);
 
    }

    
    /**
     * Updates a Xanboo account user record.
     * @param conn The database connection to use for this call
     * @param xUser a XanbooUser object from which the new user information is extracted.
     *
     * @return a user id associated with the newly created user.
     * @throws XanbooException
     */
    public void updateUser(Connection conn, XanbooUser xUser) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[updateUser()]: ");
        }

        SQLParam[] args=new SQLParam[17+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(xUser.getAccountId()), Types.BIGINT);   // owner account Id
        args[1] = new SQLParam(new Long(xUser.getUserId()), Types.BIGINT);      // user Id to update
        args[2] = new SQLParam(xUser.getUsername(), Types.VARCHAR);             // user name
        //generate a salted hash !
        args[3] = new SQLParam(XanbooUtil.hashString(xUser.getPassword(), GlobalNames.APP_HASHING_ALGORITHM, "-1"), Types.VARCHAR);                // password
        args[4] = new SQLParam(xUser.getLanguage(), Types.VARCHAR);             // language
        args[5] = new SQLParam(xUser.getTimezone(), Types.VARCHAR);             // timezone
        args[6] = new SQLParam(xUser.getExtUserId(), Types.VARCHAR);            // timezone
        args[7] = new SQLParam(xUser.getPrefs(), Types.VARCHAR);                // prefs        
        args[8] = new SQLParam(xUser.getEmail(), Types.VARCHAR);                // email address
        if(xUser.getFifoPurgingFlag()==-1)                                      //account fifo purging flag
            args[9] = new SQLParam(null);
        else
            args[9] = new SQLParam(new Integer(xUser.getFifoPurgingFlag()), Types.INTEGER);

        if(xUser.getTemporaryPasswordFlag()==-1 || xUser.getTemporaryPasswordFlag() == -2)  //user temp password flag, -2 from BSS api so that email will be sent but no database update.
            args[10] = new SQLParam(null);
        else
            args[10] = new SQLParam(xUser.getTemporaryPasswordFlag());
        
        //2 security questions and answers
        args[11] = new SQLParam(null);                                          //q1
        args[12] = new SQLParam(null);                                          //a1
        args[13] = new SQLParam(null);                                          //q2
        args[14] = new SQLParam(null);                                          //a2
        
        if(xUser.getStatus()==XanbooUser.STATUS_UNCHANGED)                      //user status id
            args[15] = new SQLParam(null);  //dont update status
        else
            args[15] = new SQLParam(xUser.getStatus(), Types.INTEGER);  
            
        XanbooSecurityQuestion[] xsq = xUser.getSecurityQuestions();
        
        if(xsq!=null && xsq.length>0) {
            if(xsq[0]!=null&& xsq[0].isValid()) {
                args[11] = new SQLParam(new Integer(xsq[0].getSecQuestionId()), Types.INTEGER);
                args[12] = new SQLParam(XanbooUtil.hashString(xsq[0].getSecAnswer(), GlobalNames.APP_HASHING_ALGORITHM, "-1"), Types.VARCHAR);
                ////args[12] = new SQLParam(xsq[0].getSecAnswer(), Types.VARCHAR);
            }
            
            //if second one defined
            if(xsq.length>1 && xsq[1]!=null && xsq[1].isValid()) {
                args[13] = new SQLParam(new Integer(xsq[1].getSecQuestionId()), Types.INTEGER);
                args[14] = new SQLParam(XanbooUtil.hashString(xsq[1].getSecAnswer(), GlobalNames.APP_HASHING_ALGORITHM, "-1"), Types.VARCHAR);
                ////args[14] = new SQLParam(xsq[1].getSecAnswer(), Types.VARCHAR);
            }
        }

        args[16] = new SQLParam(xUser.getProfileData(), Types.VARCHAR);

        dao.callSP(conn, "XC_ACCOUNT_PKG.UPDATEUSER", args, false);

        
        /* if password is being reset, send an pwd reset email to the user.if the pwd notification profile is set.Use it. */
        //if(xUser.getPassword()!=null && xUser.getTemporaryPasswordFlag()==1 && xUser.getEmail()!=null && xUser.getEmail().length()>0) {
        if(xUser.getPassword()!=null && ( xUser.getTemporaryPasswordFlag()==1 || xUser.getTemporaryPasswordFlag() == -2)) {
            if(xUser.getPasswordnotificationProfile() != null && xUser.getPasswordnotificationProfile().indexOf("%") > 0){  // Telephone number - SMS
            	String sType = xUser.getPasswordnotificationProfile().substring(0,xUser.getPasswordnotificationProfile().indexOf("%"));
                String destNumber = xUser.getPasswordnotificationProfile().substring((xUser.getPasswordnotificationProfile().indexOf("%")+1));
            	queueAction(conn, xUser.getDomain(), xUser.getAccountId(), xUser.getExtUserId(), "0", "0", "12", xUser.getPassword(), "Auto-notify", xUser.getLanguage(), xUser.getTimezone(), sType, destNumber );
            }
            else	
            	if(xUser.getEmail()!=null && xUser.getEmail().length()>0) { // For Email
            		queueAction(conn, xUser.getDomain(), xUser.getAccountId(), xUser.getExtUserId(), "0", "0", "12", xUser.getPassword(), "Auto-notify", xUser.getLanguage(), xUser.getTimezone(), "", xUser.getEmail() );
            	}
        }
        
    }
    

    /**
     * Authenticates a Xanboo User from Core DB by password
     * @param conn The database connection to use for this call
     * @param domainId user account domain identifier
     * @param userName username to authenticate
     * @param password user password
     *
     * @return a XanbooPrincipal object
     * @throws XanbooException
     */
     public XanbooPrincipal authenticateUser(Connection conn, String domainId, String username, String password) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[authenticateUser()]: domain=" + domainId + ", username=" + username );
        }

        //pre-authenticate the username and get the hashed security answer and password!!!
        SQLParam[] args=new SQLParam[7+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(domainId);
        args[1] = new SQLParam(username);
        args[2] = new SQLParam(-1, Types.INTEGER);         //pass -1 for no sec question id
        args[3] = new SQLParam("", Types.VARCHAR, true);   //returned hashed pwd
        args[4] = new SQLParam("", Types.VARCHAR, true);   //returned hashed security answer
        args[5] = new SQLParam(new Long(-1), Types.BIGINT, true);     // for returning account id
        args[6] = new SQLParam(new Long(-1), Types.BIGINT, true);     // for returning user id

        dao.callSP(conn, "XC_ACCOUNT_PKG.PREAUTHENTICATEUSER", args, false);

        //parse returned hash to determine hash algorithm and salt
        //hash format: <hash-id>:<salt>:<hash>  or just <hash>
        String currentPasswordHash = args[3].getParam().toString();
        String hashVal  = null;
        String hashSalt = null;
        int hashAlg=XanbooUtil.SUPPORTED_ALGORITHM_MD5;  //default MD5

        String hashedPassword;
        if(currentPasswordHash.length()==34) {   //old MD5-only hashing with no salting, for backward compatibility
            hashedPassword = XanbooUtil.hashString(password, hashAlg, null);
        }else {     //parse algorithm and salt, then hash the incoming password for comparison
            hashAlg  = Integer.parseInt(currentPasswordHash.substring(0,1));
            hashSalt = currentPasswordHash.substring(1,17);
            hashedPassword = XanbooUtil.hashString(password, hashAlg, hashSalt);    //use the same algorithm and salt in the stored hash
        }

        //now actual authentication with proper hashing used
        //could check pwd here too, but there is all that login attempt logic too I don't have time to look into now -LT
        args=new SQLParam[12+2];     // 11 SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(domainId);
        args[1] = new SQLParam(username);
        args[2] = new SQLParam(hashedPassword);
        args[3] = new SQLParam(-1, Types.INTEGER);         //pass -1 for no sec question id

        // OUT parameters
        args[4] = new SQLParam(new Long(-1), Types.BIGINT, true);     // for returning user id
        args[5] = new SQLParam(new Long(-1), Types.BIGINT, true);     // for returning account id
        args[6] = new SQLParam(new Integer(-1), Types.INTEGER, true);  // for returning account status id
        args[7] = new SQLParam(new Integer(-1), Types.INTEGER, true);  // for returning user master flag
        args[8] = new SQLParam("", Types.VARCHAR, true);    // user language info
        args[9] = new SQLParam("", Types.VARCHAR, true);    // user timezone info
        args[10] = new SQLParam("", Types.VARCHAR, true);   //user prefs info
        args[11] = new SQLParam(null, Types.VARCHAR, true);          //last password change
        
        dao.callSP(conn, "XC_ACCOUNT_PKG.AUTHENTICATEUSER", args, false);
 
        // TO DO: what to do in case account is not active !!!
        XanbooPrincipal xp = new XanbooPrincipal(domainId, username);
        xp.setUserId(((Long) args[4].getParam()).longValue());
        xp.setAccountId(((Long) args[5].getParam()).longValue());
        xp.setMaster(((Integer) args[7].getParam()).intValue()==1 ? true : false);
        xp.setLanguage(args[8].getParam().toString());
        xp.setTimezone(args[9].getParam().toString());
        xp.setPrefs(args[10].getParam().toString());
        xp.setLastPasswordChange(XanbooUtil.getDate(args[11]));

        if(logger.isDebugEnabled()) {
            logger.debug("[authenticateUser()]: AUTH1 lastPasswordChange=" + xp.getLastPasswordChange() + " for username " + username);
        }

        return xp;
     }

     
     /**
      * Authenticates a Xanboo User from Core DB by a security question object
      * @param conn The database connection to use for this call
      * @param domainId user account domain identifier
      * @param userName username to authenticate
      * @param sq a XanbooSecurityQuestion object with a valid security question id and answer to match
      *
      * @return a XanbooPrincipal object
      * @throws XanbooException
      */
     /**
      * Authenticates a Xanboo User from Core DB by a security question object
      * @param conn The database connection to use for this call
      * @param domainId user account domain identifier
      * @param userName username to authenticate
      * @param sq a XanbooSecurityQuestion object with a valid security question id and answer to match
      *
      * @return a XanbooPrincipal object
      * @throws XanbooException
      */
      public XanbooPrincipal authenticateUser(Connection conn, String domainId, String username, XanbooSecurityQuestion sq) throws XanbooException {
         if(logger.isDebugEnabled()) {
             logger.debug("[authenticateUser()]: domain=" + domainId + ", username=" + username + ", secQ=" + sq.getSecQuestionId());
         }
         
         //pre-authenticate the username and get the hashed security answer and password!!!
         SQLParam[] args=new SQLParam[7+2];     // SP parameters + 2 std parameters (errno, errmsg)

         // setting IN params
         args[0] = new SQLParam(domainId);
         args[1] = new SQLParam(username);
         args[2] = new SQLParam(sq.getSecQuestionId(), Types.INTEGER);
         args[3] = new SQLParam("", Types.VARCHAR, true);   //returned hashed pwd
         args[4] = new SQLParam("", Types.VARCHAR, true);   //returned hashed security answer
         args[5] = new SQLParam(new Long(-1), Types.BIGINT, true);     // for returning account id
         args[6] = new SQLParam(new Long(-1), Types.BIGINT, true);     // for returning user id

         dao.callSP(conn, "XC_ACCOUNT_PKG.PREAUTHENTICATEUSER", args, false);

         // store use current hashed password to authenticate to get a XanbooPrincipal !
         String currentPasswordHash = args[3].getParam().toString();

         //first
         String currentSecA = args[4].getParam().toString();
         String hashVal  = null;
         String hashSalt = null;
         int hashAlg=XanbooUtil.SUPPORTED_ALGORITHM_MD5;  //default MD5

         String hashedSecA;
         if(currentSecA.length()==34) {   //old MD5-only hashing with no salting, for backward compatibility
             hashedSecA = XanbooUtil.hashString(sq.getSecAnswer(), hashAlg, null);
         }else {     //parse algorithm and salt, then hash the incoming password for comparison
             hashAlg  = Integer.parseInt(currentSecA.substring(0,1));
             hashSalt = currentSecA.substring(1,17);
             hashedSecA = XanbooUtil.hashString(sq.getSecAnswer(), hashAlg, hashSalt);    //use the same algorithm and salt in the stored hash
         }
         
         //check if answers match, throw exception if not
         if(!currentSecA.equals(hashedSecA)) {
             Long accId = (Long) args[5].getParam(); //returned acc id from pre auth call
             Long userId = (Long) args[6].getParam(); //returned user id from pre auth call
             
             /* update failed login attempt count for failure, and continue */
             args=new SQLParam[2+2];     // SP parameters + 2 std parameters (errno, errmsg)
             args[0] = new SQLParam(accId, Types.BIGINT);     // acc id
             args[1] = new SQLParam(userId, Types.BIGINT);     // user id
             dao.callSP(conn, "XC_ACCOUNT_PKG.UPDATEFAILEDSECQATTEMPT", args, false);
             
             /* !!!!!!!! special nak code to prevent rollback - do not change !!!!!!! */
             throw new XanbooException(210502, "Authentication failure. Failed to match security question answer.");
         } 
                
         //could check pwd here too, but there is all that login attempt logic too I don't have time to look into now -LT
         args=new SQLParam[12+2];     // 11 SP parameters + 2 std parameters (errno, errmsg)

         // setting IN params
         args[0] = new SQLParam(domainId);
         args[1] = new SQLParam(username);
         args[2] = new SQLParam(currentPasswordHash);
         args[3] = new SQLParam(sq.getSecQuestionId(), Types.INTEGER);

         // OUT parameters
         args[4] = new SQLParam(new Long(-1), Types.BIGINT, true);     // for returning user id
         args[5] = new SQLParam(new Long(-1), Types.BIGINT, true);     // for returning account id
         args[6] = new SQLParam(new Integer(-1), Types.INTEGER, true);  // for returning account status id
         args[7] = new SQLParam(new Integer(-1), Types.INTEGER, true);  // for returning user master flag
         args[8] = new SQLParam("", Types.VARCHAR, true);    // user language info
         args[9] = new SQLParam("", Types.VARCHAR, true);    // user timezone info
         args[10] = new SQLParam("", Types.VARCHAR, true);   //user prefs info
         args[11] = new SQLParam(null, Types.VARCHAR, true);          //last password change

         dao.callSP(conn, "XC_ACCOUNT_PKG.AUTHENTICATEUSER", args, false);
  
         // TO DO: what to do in case account is not active !!!
         XanbooPrincipal xp = new XanbooPrincipal(domainId, username);
         xp.setUserId(((Long) args[4].getParam()).longValue());
         xp.setAccountId(((Long) args[5].getParam()).longValue());
         xp.setMaster(((Integer) args[7].getParam()).intValue()==1 ? true : false);
         xp.setLanguage(args[8].getParam().toString());
         xp.setTimezone(args[9].getParam().toString());
         xp.setPrefs(args[10].getParam().toString());
         xp.setLastPasswordChange(XanbooUtil.getDate(args[11]));

         if(logger.isDebugEnabled()) {
            logger.debug("[authenticateUser()]: AUTH2 lastPasswordChange=" + xp.getLastPasswordChange() + " for username " + username);
         }

         return xp;
      }
         
     
    /**
     * Authenticates a Xanboo User from Core DB by a security question object
     * @param conn The database connection to use for this call
     * @param domainId user account domain identifier
     * @param userName username to authenticate
     * @param sq a XanbooSecurityQuestion object with a valid security question id and answer to match
     *
     * @return a XanbooPrincipal object
     * @throws XanbooException
     */
     public XanbooPrincipal authenticateUser(Connection conn, String domainId, String username, XanbooSecurityQuestion[] sq) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[authenticateUser()]: domain=" + domainId + ", username=" + username + ", secQ=" + sq[0].getSecQuestionId() + ", secQ2=" + sq[1].getSecQuestionId());
        }
        
        //pre-authenticate the username and get the hashed security answer and password!!!
        SQLParam[] args=new SQLParam[9+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(domainId);
        args[1] = new SQLParam(username);
        args[2] = new SQLParam(sq[0].getSecQuestionId(), Types.INTEGER);
        args[3] = new SQLParam(sq[1].getSecQuestionId(), Types.INTEGER);
        args[4] = new SQLParam("", Types.VARCHAR, true);   //returned hashed pwd
        args[5] = new SQLParam("", Types.VARCHAR, true);   //returned hashed security answer
        args[6] = new SQLParam("", Types.VARCHAR, true);   //returned hashed security answer 2      
        args[7] = new SQLParam(new Long(-1), Types.BIGINT, true);     // for returning account id
        args[8] = new SQLParam(new Long(-1), Types.BIGINT, true);     // for returning user id

        dao.callSP(conn, "XC_ACCOUNT_PKG.PREAUTHENTICATEUSER", args, false);

        // store use current hashed password to authenticate to get a XanbooPrincipal !
        String currentPasswordHash = args[4].getParam().toString();

        //first
        String currentSecA = args[5].getParam().toString();
        String currentSecA2 = args[6].getParam().toString();
        
        String hashVal  = null;
        String hashSalt = null;
        int hashAlg=XanbooUtil.SUPPORTED_ALGORITHM_MD5;  //default MD5

        String hashedSecA;
        String hashedSecA2;
        if(currentSecA.length()==34) {   //old MD5-only hashing with no salting, for backward compatibility
            hashedSecA = XanbooUtil.hashString(sq[0].getSecAnswer(), hashAlg, null);
        }else {     //parse algorithm and salt, then hash the incoming password for comparison
            hashAlg  = Integer.parseInt(currentSecA.substring(0,1));
            hashSalt = currentSecA.substring(1,17);
            hashedSecA = XanbooUtil.hashString(sq[0].getSecAnswer(), hashAlg, hashSalt);    //use the same algorithm and salt in the stored hash
        }
        
        if(currentSecA2.length()==34) {   //old MD5-only hashing with no salting, for backward compatibility
            hashedSecA2 = XanbooUtil.hashString(sq[1].getSecAnswer(), hashAlg, null);
        }else {     //parse algorithm and salt, then hash the incoming password for comparison
            hashAlg  = Integer.parseInt(currentSecA.substring(0,1));
            hashSalt = currentSecA2.substring(1,17);
            hashedSecA2 = XanbooUtil.hashString(sq[1].getSecAnswer(), hashAlg, hashSalt);    //use the same algorithm and salt in the stored hash
        }        
        
        //check if answers match, throw exception if not
        if(!currentSecA.equals(hashedSecA) || !currentSecA2.equals(hashedSecA2)) {
            Long accId = (Long) args[7].getParam(); //returned acc id from pre auth call
            Long userId = (Long) args[8].getParam(); //returned user id from pre auth call
            
            /* update failed login attempt count for failure, and continue */
            args=new SQLParam[2+2];     // SP parameters + 2 std parameters (errno, errmsg)
            args[0] = new SQLParam(accId, Types.BIGINT);     // acc id
            args[1] = new SQLParam(userId, Types.BIGINT);     // user id
            dao.callSP(conn, "XC_ACCOUNT_PKG.UPDATEFAILEDSECQATTEMPT", args, false);
            
            /* !!!!!!!! special nak code to prevent rollback - do not change !!!!!!! */
            throw new XanbooException(210502, "Authentication failure. Failed to match security question answer.");
        } 
               
        //could check pwd here too, but there is all that login attempt logic too I don't have time to look into now -LT
        args=new SQLParam[12+2];     // 11 SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(domainId);
        args[1] = new SQLParam(username);
        args[2] = new SQLParam(currentPasswordHash);
        args[3] = new SQLParam(sq[0].getSecQuestionId(), Types.INTEGER);

        // OUT parameters
        args[4] = new SQLParam(new Long(-1), Types.BIGINT, true);     // for returning user id
        args[5] = new SQLParam(new Long(-1), Types.BIGINT, true);     // for returning account id
        args[6] = new SQLParam(new Integer(-1), Types.INTEGER, true);  // for returning account status id
        args[7] = new SQLParam(new Integer(-1), Types.INTEGER, true);  // for returning user master flag
        args[8] = new SQLParam("", Types.VARCHAR, true);    // user language info
        args[9] = new SQLParam("", Types.VARCHAR, true);    // user timezone info
        args[10] = new SQLParam("", Types.VARCHAR, true);   //user prefs info
        args[11] = new SQLParam(null, Types.VARCHAR, true);          //last password change
        
        dao.callSP(conn, "XC_ACCOUNT_PKG.AUTHENTICATEUSER", args, false);
 
        // TO DO: what to do in case account is not active !!!
        XanbooPrincipal xp = new XanbooPrincipal(domainId, username);
        xp.setUserId(((Long) args[4].getParam()).longValue());
        xp.setAccountId(((Long) args[5].getParam()).longValue());
        xp.setMaster(((Integer) args[7].getParam()).intValue()==1 ? true : false);
        xp.setLanguage(args[8].getParam().toString());
        xp.setTimezone(args[9].getParam().toString());
        xp.setPrefs(args[10].getParam().toString());
        xp.setLastPasswordChange(XanbooUtil.getDate(args[11]));

        if (logger.isDebugEnabled()) {
            logger.debug("[authenticateUser()]: AUTH3 lastPasswordChange=" + xp.getLastPasswordChange() + " for username " + username);
        }

        return xp;
     }
     
     
    /**
     * Authenticates a Xanboo User from Core DB by ext user id
     * @param conn The database connection to use for this call
     * @param domainId user domain identifier
     * @param extUserId The external user Id of the user to authenticate
     *
     * @return a XanbooPrincipal object
     * @throws XanbooException
     */
     public XanbooPrincipal authenticateUser(Connection conn, String domainId, String extUserId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[authenticateUser()]: domain=" + domainId + ", extUserId=" + extUserId);
        }

        SQLParam[] args=new SQLParam[11+2];     // 11 SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(domainId);
        args[1] = new SQLParam(extUserId);

        // OUT parameters
        args[2] = new SQLParam(new Long(-1), Types.BIGINT, true);     // for returning user id
        args[3] = new SQLParam(new Long(-1), Types.BIGINT, true);     // for returning account id
        args[4] = new SQLParam("", Types.VARCHAR, true);                // for returning username
        args[5] = new SQLParam(new Integer(-1), Types.INTEGER, true);  // for returning account status id
        args[6] = new SQLParam(new Integer(-1), Types.INTEGER, true);  // for returning user master flag
        args[7] = new SQLParam("", Types.VARCHAR, true);                //for returning language
        args[8] = new SQLParam("", Types.VARCHAR, true);                //for returning tz
        args[9] = new SQLParam("", Types.VARCHAR, true);                //for returning prefs
        args[10] = new SQLParam(null, Types.VARCHAR, true);          //last password change
        
        dao.callSP(conn, "XC_ACCOUNT_PKG.AUTHENTICATEUSER", args, false);
 
        // TO DO: what to do in case account is not active !!!
        XanbooPrincipal xp = new XanbooPrincipal(domainId, args[4].getParam().toString());
        xp.setUserId(((Long) args[2].getParam()).longValue());
        xp.setAccountId(((Long) args[3].getParam()).longValue());
        xp.setMaster(((Integer) args[6].getParam()).intValue()==1 ? true : false);
        xp.setLanguage(args[7].getParam().toString());
        xp.setTimezone(args[8].getParam().toString());
        xp.setPrefs(args[9].getParam().toString());
        xp.setLastPasswordChange(XanbooUtil.getDate(args[10]));

        if (logger.isDebugEnabled()) {
            logger.debug("[authenticateUser()]: AUTH4 lastPasswordChange=" + xp.getLastPasswordChange() + " for extUserId " + extUserId);
        }

        return xp;
     }

     
    /**
     * Gets a list of users for a specific account
     * @param conn The database connection to use for this call
     * @param accountId the account id to get the user list for
     *
     * @return a XanbooResultSet which contains a HashMap list of user records
     * @throws XanbooException
     */
     public XanbooResultSet getUserList(Connection conn, long accountId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getUserList()]: ");
        }

        SQLParam[] args=new SQLParam[1+2];     // 1 SP parameter + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);

        return dao.callSP(conn, "XC_ACCOUNT_PKG.GETUSERLIST", args);
 
     }
     
     
    /**
     * Gets all account quota values for a given account
     * @param conn The database connection to use for this call
     * @param accountId the account id to get the user list for
     *
     * @return a XanbooResultSet which contains a HashMap list of account quotas
     * @throws XanbooException
     */
     public XanbooResultSet getAccountQuotaList(Connection conn, long accountId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getAccountQuotaList()]: ");
        }

        SQLParam[] args=new SQLParam[1+2];     // 1 SP parameter + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);

        return dao.callSP(conn, "XC_ACCOUNT_PKG.GETACCOUNTQUOTALIST", args);

     }
     
     
     
    /**
     * Gets specific account quota values for a given account and quota id.
     * @param conn The database connection to use for this call
     * @param xQuota a XanbooQuota object for requested quota.
     *
     * @throws XanbooException
     */
     public void getAccountQuota(Connection conn, XanbooQuota xQuota) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getAccountQuota()]: ");
        }

        SQLParam[] args=new SQLParam[6+2];     // 6 SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(xQuota.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Integer(xQuota.getQuotaId()), Types.INTEGER);
        args[2] = new SQLParam( xQuota.getQuotaClass(), Types.VARCHAR );
        // OUT params
        args[3] = new SQLParam(new Long(-1), Types.BIGINT, true);  // for returning quota value
        args[4] = new SQLParam(new Long(-1), Types.BIGINT, true);  // for returning quota usage value
        args[5] = new SQLParam(new Integer(-1), Types.INTEGER, true);  // for returning quota reset period

        dao.callSP(conn, "XC_ACCOUNT_PKG.GETACCOUNTQUOTA", args, false);

        xQuota.setQuotaValue(((Long) args[3].getParam()).longValue());
        xQuota.setCurrentValue(((Long) args[4].getParam()).longValue());
        xQuota.setResetPeriod(((Integer) args[5].getParam()).intValue());

     }

     
    /**
     * Updates specific account quota values for a given account and quota id.
     * @param conn The database connection to use for this call
     * @param xQuota a XanbooQuota object for requested quota update.
     *
     * @throws XanbooException
     *
     */
     public void updateAccountQuota(Connection conn, XanbooQuota xQuota, boolean forceFlag) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[updateAccountQuota()]: ");
        }

        /* The internal representation of gateway quota differs from other devices.
         * We accomodate this here by converting device-based gateway quota to a gateway quota
         */
        if ( (xQuota.getQuotaId() == XanbooQuota.GATEWAY) || (xQuota.getQuotaId() == XanbooQuota.DEVICE && xQuota.getQuotaClass().equals( "0000" )) ) {
            xQuota.setQuotaId( XanbooQuota.GATEWAY );
            xQuota.setQuotaClass( "0000" );
        }
        
        SQLParam[] args=new SQLParam[6+2];     // 6 SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(xQuota.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Integer(xQuota.getQuotaId()), Types.INTEGER);
        args[2] = new SQLParam(xQuota.getQuotaClass(), Types.VARCHAR);
        args[3] = new SQLParam(new Long(xQuota.getQuotaValueLong()), Types.BIGINT);  // quota value to set
        args[4] = new SQLParam(new Integer(xQuota.getResetPeriod()), Types.INTEGER);  // quota reset period to set
        args[5] = new SQLParam(new Integer( forceFlag ? 1 : 0 ), Types.INTEGER );  // force reduction of quota below current value

        dao.callSP(conn, "XC_ACCOUNT_PKG.UPDATEACCOUNTQUOTA", args, false);

     }

     
     /**
     * Updates contact information for a Xanboo account user.
     * @param conn The database connection to use for this call
     * @param xContact a XanbooContact object from which the user contact information will be extracted.
     *
     * @throws XanbooException
     */
     public void updateUserInformation(Connection conn, XanbooContact xContact) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[updateUserInformation()]: ");
        }

        SQLParam[] args=new SQLParam[30+2];     // 20 SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(xContact.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xContact.getUserId()), Types.BIGINT);

        args[2] = new SQLParam(xContact.getCompany());
        args[3] = new SQLParam(xContact.getLastName());
        args[4] = new SQLParam(xContact.getFirstName());
        args[5] = new SQLParam(xContact.getMiddleName());
        args[6] = new SQLParam(xContact.getPhone());
        args[7] = new SQLParam(xContact.getCellPhone());
        args[8] = new SQLParam(xContact.getFax());
        args[9] = new SQLParam(xContact.getAddress1());
        args[10] = new SQLParam(xContact.getAddress2());
        args[11] = new SQLParam(xContact.getCity());
        args[12] = new SQLParam(xContact.getState());
        args[13] = new SQLParam(xContact.getZip());
        args[14] = new SQLParam(xContact.getZip4());
        args[15] = new SQLParam(xContact.getAreaCode());
        args[16] = new SQLParam(xContact.getCountry());
        args[17] = new SQLParam(xContact.getEmail());
        args[18] = new SQLParam(xContact.getPager());
        args[19] = new SQLParam(xContact.getUrl());
        args[20] = new SQLParam(xContact.getRelationship());
        args[21] = new SQLParam(xContact.getGender());
        StringBuilder smsB = new StringBuilder();
        smsB.append(xContact.getPhone1SMS() ? "1" : "0");
        smsB.append(xContact.getPhone2SMS() ? "1" : "0");
        smsB.append(xContact.getPhone3SMS() ? "1" : "0");
        args[22] = new SQLParam(smsB.toString());
        args[23] = new SQLParam(xContact.getEmailProfileType(),Types.BIGINT);
        args[24] = new SQLParam(xContact.getPhone1Type());
        args[25] = new SQLParam(xContact.getPhone1ProfileType(),Types.BIGINT);    
        args[26] = new SQLParam(xContact.getPhone2Type());
        args[27] = new SQLParam(xContact.getPhone2ProfileType(),Types.BIGINT);  
        args[28] = new SQLParam(xContact.getPhone3Type());
        args[29] = new SQLParam(xContact.getPhone3ProfileType(),Types.BIGINT);  
        
        dao.callSP(conn, "XC_ACCOUNT_PKG.UPDATEUSERCONTACTINFO", args, false);
        
     }
     

    /**
     * Gets contact information for a Xanboo account user.
     * @param conn The database connection to use for this call
     * @param accountId the account id to get the user list for
     * @param userId    the user id to get the information for.
     *
     * @return a XanbooContact object that contains user contact information.
     * @throws XanbooException
     */
     public XanbooContact getUserInformation(Connection conn, long accountId, long userId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getUserInformation()]: ");
        }

        SQLParam[] args=new SQLParam[2+2];     // 2 SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);      // user Id to get info

        XanbooResultSet qResult=null;
        
        try {
            qResult = dao.callSP(conn, "XC_ACCOUNT_PKG.GETUSERCONTACTINFO", args);
            
            HashMap row = (HashMap) qResult.get(0);
            XanbooContact xContact = new XanbooContact();
            xContact.setAccountId(accountId);
            xContact.setUserId(userId);
            xContact.setContactId(Long.parseLong((String) row.get("CONTACT_ID")));

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
            xContact.setUsername((String) row.get("USERNAME"));
            xContact.setRelationship((String)row.get("RELATIONSHIP"));
            xContact.setGender((String)row.get("GENDER"));
            xContact.setEmailProfileType(qResult.getElementInteger(0, "EMAIL_PTYPE"));
            xContact.setPhone1Type((String)row.get("PHONE_1_TYPE"));
            xContact.setPhone2Type((String)row.get("PHONE_2_TYPE"));
            xContact.setPhone3Type((String)row.get("PHONE_3_TYPE"));
            xContact.setPhone1ProfileType(qResult.getElementLong(0, "PHONE_1_PTYPE"));
            xContact.setPhone1ProfileType(qResult.getElementLong(0, "PHONE_2_PTYPE"));
            xContact.setPhone1ProfileType(qResult.getElementLong(0, "PHONE_3_PTYPE"));
            String smsPrefs = (String)row.get("SMS_PREFS");
            if ( smsPrefs != null && !smsPrefs.equalsIgnoreCase("") )
            {
                xContact.setPhone1SMS(smsPrefs.charAt(0) == '1' ? true : false);
                xContact.setPhone2SMS(smsPrefs.charAt(1) == '1' ? true : false);
                xContact.setPhone3SMS(smsPrefs.charAt(2) == '1' ? true : false);
            }
            return xContact;
        }catch (XanbooException xe) {
            throw xe;
        }catch(Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getUserInformation()]: " + e.getMessage(), e);
            }else {
              logger.error("[getUserInformation()]: " + e.getMessage());
            }               
            throw new XanbooException(21005, "[getUserInformation()]: " + e.getMessage() );
        }
        
     }
     
     
    /**
     * Returns notification profile records for the caller's account
     * @param conn The database connection to use for this call
     * @param accountId the account id to get the notification profile for
     * @param userId the user id to get the notification profile for.
     *
     * @return a XanbooResultSet which contains a HashMap list of account notification profile
     * @throws XanbooException
     */
     public XanbooResultSet getNotificationProfile(Connection conn, long accountId, long userId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getNotificationProfile()]:");
        }
        
        SQLParam[] args=new SQLParam[2+2];     // 2 SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);

        return dao.callSP(conn, "XC_ACCOUNT_PKG.GETNOTIFICATIONPROFILE", args);
        
     }


     XanbooNotificationProfile[] getNotificationProfile(Connection conn, long accountId, long userId, boolean getEmergencyContacts) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getNotificationProfile()]:");
        }

        SQLParam[] args=new SQLParam[2+2];     // 2 SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);

        XanbooResultSet xrs = dao.callSP(conn, "XC_ACCOUNT_PKG.GETNOTIFICATIONPROFILE", args);

        if(xrs==null || xrs.size()==0) return null;

        XanbooNotificationProfile[] xnp = new XanbooNotificationProfile[xrs.size()];

        //if(logger.isDebugEnabled()) logger.debug("[getNotificationProfile()]: RS: " + xrs.toXML());

        int cnt=0;
        for(int i=0; i<xrs.size(); i++) {
            xnp[i] = null;

            HashMap np = (HashMap) xrs.get(i);

            long npId = Long.parseLong((String) np.get("PROFILE_ID"));
            String isEM = String.valueOf(np.get("IS_EMERGENCY_CONTACT"));

            boolean npIsEmergencyContact = (isEM!=null && isEM.length()>0 && Integer.parseInt( isEM )==1) ? true : false;
            if(!getEmergencyContacts && npIsEmergencyContact) continue; //if flag is false, dont return emergency contacts

            xnp[cnt] = new XanbooNotificationProfile(npId, npIsEmergencyContact);
            xnp[cnt].setName((String) np.get("DESCRIPTION"));

            String ptype = (String) np.get("PROFILETYPE_ID");
            if(ptype!=null && ptype.length()>0) {
                xnp[cnt].setType( Integer.parseInt(ptype) );
                xnp[cnt].setAddress((String) np.get("PROFILE_ADDRESS"));
            }

            ptype = (String) np.get("PROFILETYPE_ID2");
            if(ptype!=null && ptype.length()>0) {
                xnp[cnt].setType2( Integer.parseInt(ptype) );
                xnp[cnt].setAddress2((String) np.get("PROFILE_ADDRESS2"));
            }

            ptype = (String) np.get("PROFILETYPE_ID3");
            if(ptype!=null && ptype.length()>0) {
                xnp[cnt].setType3( Integer.parseInt(ptype) );
                xnp[cnt].setAddress3((String) np.get("PROFILE_ADDRESS3"));
            }

            xnp[cnt].setGguid((String) np.get("GATEWAY_GUID"));
            
            ptype = (String) np.get("CONTACT_ORDER");
            if(ptype!=null && ptype.length()>0) {
                xnp[cnt].setOrder( Integer.parseInt(ptype) );
            }

            xnp[cnt].setCodeword( (String) np.get("CODEWORD") );

            //contact order per action plan
            for(int j=1; j<10; j++) {
                ptype = (String) np.get("CONTACT_ORDER_AP" + j);
                if(ptype!=null && ptype.length()>0) {
                    xnp[cnt].setActionPlanContactOrder(j, Short.parseShort(ptype) );
                }
            }
            
            //xnp[cnt].dump();
            cnt++;
        }

        //resize array if necessary

        if(cnt==xrs.size()) return xnp; // no need to resize

        //resize
        XanbooNotificationProfile[] xnp2 = new XanbooNotificationProfile[cnt];
        int ix=0;
        for(int i=0; i<cnt; i++) {
            if(xnp[i]==null) continue;
            xnp2[ix] = xnp[i];
            ix++;
        }
        return xnp2;

     }

     
    /**
     * Creates a new notification profile for the caller's account.
     */
     long newNotificationProfile(Connection conn, long accountId, long userId, XanbooNotificationProfile xnp) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[newNotificationProfile()]:");
        }

        SQLParam[] args=new SQLParam[24+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam( xnp.getName() );
        args[3] = new SQLParam( new Integer(xnp.getType()), Types.INTEGER );
        args[4] = new SQLParam( xnp.getAddress() );

        if(xnp.hasValidProfileAddress2()) {
            args[5] = new SQLParam( new Integer(xnp.getType2()), Types.INTEGER );
            args[6] = new SQLParam( xnp.getAddress2() );
        } else {
            args[5] = new SQLParam( null );
            args[6] = new SQLParam( null );
        }

        if(xnp.hasValidProfileAddress3()) {
            args[7] = new SQLParam( new Integer(xnp.getType3()), Types.INTEGER );
            args[8] = new SQLParam( xnp.getAddress3() );
        } else {
            args[7] = new SQLParam( null );
            args[8] = new SQLParam( null );
        }

        //is emergency contact flag
        args[9] = new SQLParam(new Integer( xnp.isEmergencyContact() ? 1 : 0 ), Types.INTEGER );

        //gateway guid
        if(xnp.getGguid()!=null)
            args[10] = new SQLParam(xnp.getGguid());
        else
            args[10] = new SQLParam( null );

        //contact order
        if(xnp.getOrder()>0)
            args[11] = new SQLParam(new Integer( xnp.getOrder() ), Types.INTEGER );
        else
            args[11] = new SQLParam( null );

        //codeword
        args[12] = new SQLParam(xnp.getCodeword() );
        
        //contact order per action plan
        for(int j=1; j<10; j++) {
            args[12+j] = new SQLParam(new Integer( xnp.getActionPlanContactOrder(j) ), Types.INTEGER );
        }

        //max allowed notif profiles
        args[22] = new SQLParam(new Integer( GlobalNames.NOTIFICATION_PROFILE_MAX ), Types.INTEGER );

        //OUT parameter: returning profile ID
        args[23] = new SQLParam(new Long(-1), Types.BIGINT, true);


        dao.callSP(conn, "XC_ACCOUNT_PKG.NEWNOTIFICATIONPROFILE", args, false);

        // return profile id
        return ((Long) args[23].getParam()).longValue();

     }


    /**
     * Removes notification profile(s) from the caller's account.
     * @param conn The database connection to use for this call
     * @param accountId the account id to get the notification profile for
     * @param userId the user id to get the notification profile for.
     * @param profileId an array of addresses/ids (email address or pager pin number) to be removed. 
     *
     * @throws XanbooException
     */
     public void deleteNotificationProfile(Connection conn, long accountId, long userId, long[] profileId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteNotificationProfile()]:");
        }

        SQLParam[] args=new SQLParam[6+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam(new Integer((GlobalNames.MODULE_SBN_ENABLED ? 1 : 0)), Types.INTEGER);

        if(profileId==null) {   // delete all profiles

            XanbooNotificationProfile[] xnp = null;
            
            //if SBN enabled, first retrieve all profile records
            if(GlobalNames.MODULE_SBN_ENABLED) {
                try {
                    XanbooResultSet xrs = dao.callSP(conn, "XC_ACCOUNT_PKG.GETNOTIFICATIONPROFILE", args);
                    if(xrs!=null && xrs.size()>0) {
                        xnp = new XanbooNotificationProfile[xrs.size()];
                        for(int i=0; i<xrs.size(); i++) {
                            HashMap np = (HashMap) xrs.get(i);
                            long npId = Long.parseLong((String) np.get("PROFILE_ID"));
                            String isEM = String.valueOf(np.get("IS_EMERGENCY_CONTACT"));
                            boolean npIsEmergencyContact = (isEM!=null && isEM.length()>0 && Integer.parseInt( isEM )==1) ? true : false;
                            if(npIsEmergencyContact) {
                                xnp[i] = new XanbooNotificationProfile(npId, npIsEmergencyContact);
                                xnp[i].setGguid((String) np.get("GATEWAY_GUID"));
                                xnp[i].setOrder( Integer.parseInt( (String) np.get("CONTACT_ORDER") ) );
                            }else {
                                xnp[i] = null;
                            }
                        }
                    }
                }catch(Exception eee) {
                    //ignore
                    eee.printStackTrace();
                }
            }

            //first remove the Xanboo DB records
            args[3] = new SQLParam(null, Types.BIGINT);
            args[4] = new SQLParam("", Types.VARCHAR, true);                // returning emergency contact gguid
            args[5] = new SQLParam(new Integer(-1), Types.INTEGER, true);   // returning emergency contact order/sequence
            dao.callSP(conn, "XC_ACCOUNT_PKG.DELETENOTIFICATIONPROFILE", args, false);
            
            //now remove SBN EC records
            if(GlobalNames.MODULE_SBN_ENABLED && xnp!=null && xnp.length>0) {
                for(int i=0; i<xnp.length; i++) {
                    if(xnp[i]!=null) {
                        if(xnp[i].getGguid()!=null && xnp[i].getGguid().length()>0 && xnp[i].getOrder()>0) {
                            SBNSynchronizer sbn = new SBNSynchronizer();
                            boolean sbnOK = sbn.deleteEmergencyContact(xnp[i].getGguid(), xnp[i].getOrder());
                            if(!sbnOK) {    //SBN sync failed!
                                throw new XanbooException(21232, "Failed to delete notification profiles. SBN synchronization failed.");
                            }
                        }
                        
                    }
                }
            }
            
        }else {                 // delete profiles with given ids
            for(int i=0; i<profileId.length; i++) {
                args[3] = new SQLParam(new Long(profileId[i]), Types.BIGINT);
                args[4] = new SQLParam("", Types.VARCHAR, true);                // returning emergency contact gguid
                args[5] = new SQLParam(new Integer(-1), Types.INTEGER, true);   // returning emergency contact order/sequence
                dao.callSP(conn, "XC_ACCOUNT_PKG.DELETENOTIFICATIONPROFILE", args, false);

                //sync with SBN, if a real gguid and seq is associated with the deleted emergency contact!
                if(GlobalNames.MODULE_SBN_ENABLED) {
                    String gguid = (String) args[4].getParam();
                    int seq = ((Integer) args[5].getParam()).intValue();

                    if(gguid!=null && gguid.length()>0 && seq>0) {
                        SBNSynchronizer sbn = new SBNSynchronizer();
                        boolean sbnOK = sbn.deleteEmergencyContact(gguid, seq);
                        if(!sbnOK) {    //SBN sync failed!
                            throw new XanbooException(21232, "Failed to delete notification profile. SBN synchronization failed.");
                        }
                    }
                }

            }//end for
        }
        
     }

     

    /**
     * Updates an existing notification profile.
     */
     void updateNotificationProfile(Connection conn, long accountId, long userId, XanbooNotificationProfile xnp) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[updateNotificationProfile()]: ");
        }

        SQLParam[] args=new SQLParam[23+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam(new Long(xnp.getProfileId()), Types.BIGINT);  // profile id to update

        args[3] = new SQLParam( xnp.getName() );
        args[4] = new SQLParam( new Integer(xnp.getType()), Types.INTEGER );
        args[5] = new SQLParam( xnp.getAddress() );

        if(xnp.hasValidProfileAddress2()) {
            args[6] = new SQLParam( new Integer(xnp.getType2()), Types.INTEGER );
            args[7] = new SQLParam( xnp.getAddress2() );
        } else {
            args[6] = new SQLParam( null );
            args[7] = new SQLParam( null );
        }

        if(xnp.hasValidProfileAddress3()) {
            args[8] = new SQLParam( new Integer(xnp.getType3()), Types.INTEGER );
            args[9] = new SQLParam( xnp.getAddress3() );
        } else {
            args[8] = new SQLParam( null );
            args[9] = new SQLParam( null );
        }

        //is emergency contact flag
        args[10] = new SQLParam(new Integer( xnp.isEmergencyContact() ? 1 : 0 ), Types.INTEGER );

        //gateway guid
        if(xnp.getGguid()!=null)
            args[11] = new SQLParam(xnp.getGguid());
        else
            args[11] = new SQLParam( null );

        //contact order
        if(xnp.getOrder()>0)
            args[12] = new SQLParam(new Integer( xnp.getOrder() ), Types.INTEGER );
        else
            args[12] = new SQLParam( null );

        //codeword
        args[13] = new SQLParam(xnp.getCodeword() );
        
        //contact order per action plan
        for(int j=1; j<10; j++) {
            args[13+j] = new SQLParam(new Integer( xnp.getActionPlanContactOrder(j) ), Types.INTEGER );
        }


        dao.callSP(conn, "XC_ACCOUNT_PKG.UPDATENOTIFICATIONPROFILE", args, false);

     }

     
    /**
     * Sends test messages to notification profile(s) specified
     * @param conn The database connection to use for this call
     * @param accountId the account id to get the notification profile for
     * @param userId the user id to test the notification profiles for.
     * @param profileId an array of addresses/ids (email address or pager pin number) to be tested. 
     *
     * @throws XanbooException
     */
     public void testNotificationProfile(Connection conn, long accountId, long userId, long[] profileId) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[testNotificationProfile()]:");
        }

        SQLParam[] args=new SQLParam[3+2];     // 3 SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);

        for(int i=0; i<profileId.length; i++) {
            args[2] = new SQLParam(new Long(profileId[i]), Types.BIGINT);
            dao.callSP(conn, "XC_ACCOUNT_PKG.TESTNOTIFICATIONPROFILE", args, false);
        }
        
     }
     
     
    /**
     * Returns broadcast messages (if any) waiting for an account
     * @param conn The database connection to use for this call
     * @param accountId the account id to get the messages for
     * @param userId the user id to get the messages for
     * @param lang Language id of the message to be retrieved (e.g. "en")
     *
     * @return a XanbooResultSet which contains a HashMap list of messages
     * @throws XanbooException
     */
     public XanbooResultSet getBroadcastMessage(Connection conn, long accountId, long userId, String lang) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getBroadcastMessage()]:");
        }
        
        SQLParam[] args=new SQLParam[3+2];     // 2 SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(new Long(userId), Types.BIGINT);
        args[2] = new SQLParam(lang, Types.VARCHAR);
        
        return dao.callSP(conn, "XC_ACCOUNT_PKG.GETBROADCASTMESSAGE", args);
        
     }     
     
    /**
     * Retrieves a specific XanbooUser object
     * @param conn The database connection to use for this call
     * @param xCaller The accountId and userId properties are used to authenticate this call
     * @param userId The ID of the user to retrieve
     * @return A XanoboUser of the requested ID
    */        
    public XanbooUser getUser(Connection conn, XanbooPrincipal xCaller, long userId) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getUser()]:");
        }

        XanbooResultSet iTmp = null;
        SQLParam[] args=new SQLParam[3+2];     // 3 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(userId), Types.BIGINT);

        iTmp = (XanbooResultSet) dao.callSP(conn, "XC_ACCOUNT_PKG.GETUSER", args);
        if (iTmp.size() == 0) {
            throw new XanbooException(21007, "Get user failed. Invalid ID");
        }

        try {
            XanbooUser user = new XanbooUser(iTmp, 0);
            if (logger.isDebugEnabled()) {
                logger.debug("Get User lastPasswordChange " +user.getLastPasswordChange() + " for user " + userId);
            }
            return user;
        }catch(Exception ee) {
            throw new XanbooException(21007, "Get user failed. Instantiation Exception");
        }
        
    }

    
    public XanbooUser[] locateUser(Connection conn, String domainId, String username, String email, boolean sendUsernameReminder) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[getUser()]:");
        }

        XanbooResultSet iTmp = null;
        SQLParam[] args=new SQLParam[3+2];     // 3 SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(domainId);
        args[1] = new SQLParam(username);
        args[2] = new SQLParam(email);
        
        iTmp = (XanbooResultSet) dao.callSP(conn, "XC_ACCOUNT_PKG.LOCATEUSER", args);
        if (iTmp.size() == 0) {
            throw new XanbooException(21007, "Locate user failed. Username or email not registered.");
        }

        try {
            String ids = "";
            XanbooUser[] users = new XanbooUser[iTmp.size()];
            String lang="en";
            for(int i=0; i<iTmp.size(); i++) {
                users[i] = new XanbooUser(iTmp, i);
                if(ids.length()>0) {
                    if((ids.length()+users[i].getUsername().length()+2) > 256) {
                        queueAction(conn, domainId, users[0].getAccountId(), users[0].getExtUserId(), "0", "0", "14", ids, "Auto-notify", users[0].getLanguage(), users[0].getTimezone(), "", users[0].getEmail() );
                        ids = "";
                    } else {
                        ids = ids + ", ";
                    }
                }
                ids = ids + users[i].getUsername();
            }
            
            if(sendUsernameReminder && ids.length()>0) {
                // list od ids is the event label/msg
                queueAction(conn, domainId, users[0].getAccountId(), users[0].getExtUserId(), "0", "0", "14", ids, "Auto-notify", users[0].getLanguage(), users[0].getTimezone(), "", users[0].getEmail() );
            }
            
            return users;
        }catch(Exception ee) {
            throw new XanbooException(21007, "Locate user failed. Instantiation Exception");
        }
        
    }
    
    
    public void updateUserACL( Connection conn, XanbooPrincipal xCaller, long userId, int objectTypeId, long objectId, int accessId ) throws XanbooException {
        updateUserACL( conn, xCaller, userId, objectTypeId, Long.toString( objectId ), accessId );
        
    }
    
    public void updateUserACL( Connection conn, XanbooPrincipal xCaller, long userId, int objectTypeId, String objectId, int accessId ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateUserACL()]:");
        }

        SQLParam[] args=new SQLParam[6+2];     // 3 SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(userId), Types.BIGINT);
        args[3] = new SQLParam(new Integer(objectTypeId), Types.INTEGER);
        args[4] = new SQLParam( objectId );
        args[5] = new SQLParam(new Integer(accessId), Types.INTEGER);

        dao.callSP(conn, "XC_ACCOUNT_PKG.UPDATEUSERACL", args, false);

    }
     
     public XanbooResultSet getUserACL(Connection conn, XanbooPrincipal xCaller, long userId, String gatewayGUID, String deviceGUID) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getUserACL()device]:");
        }
        
        return this.getUserACL(conn, xCaller, userId, 3, gatewayGUID );
        
     }
         
    
    /**
     * Returns acl directives for the specified user, object type and, optionally a specific object
     */
     private XanbooResultSet getUserACL(Connection conn, XanbooPrincipal xCaller, long userId, int objectTypeId, String objectId ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getUserACL()]:");
        }
        
        SQLParam[] args=new SQLParam[5+2];     // 2 SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam(new Long(userId), Types.BIGINT);
        args[3] = new SQLParam(new Integer(objectTypeId), Types.INTEGER);
        args[4] = new SQLParam(objectId, Types.VARCHAR);

        return dao.callSP(conn, "XC_ACCOUNT_PKG.GETUSERACL", args);
        
     }     
    
    public XanbooResultSet getAccount(Connection conn, XanbooPrincipal xCaller) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[getAccount()]:");
        }
        
        XanbooResultSet results = null;
        SQLParam[] args=new SQLParam[2+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        
        results = (XanbooResultSet)dao.callSP(conn, "XC_ACCOUNT_PKG.GETACCOUNT", args);
        
        return results;
        
    }


    public void updateAccount(Connection conn, XanbooPrincipal xCaller, XanbooAccount xAccount) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateAccount()]:");
        }

        SQLParam[] args=new SQLParam[7+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        args[2] = new SQLParam((xAccount.getStatus()==XanbooAccount.STATUS_UNCHANGED ? null : new Integer(xAccount.getStatus())), Types.INTEGER);
        args[3] = new SQLParam((xAccount.getToken()==null || xAccount.getToken().length()==0) ? null : xAccount.getToken());
        args[4] = new SQLParam((xAccount.getExtAccountId()==null || xAccount.getExtAccountId().length()==0) ? null : xAccount.getExtAccountId());
        args[5] = new SQLParam(xAccount.getFifoPurgingFlag()==-1 ? null : xAccount.getFifoPurgingFlag());
        //args[6] = xAccount.getPushPreferences() == null ? new SQLParam(null,Types.NULL) : new SQLParam(xAccount.getPushPreferences(),Types.INTEGER);
        args[6] = new SQLParam(null,Types.NULL);

        dao.callSP(conn, "XC_ACCOUNT_PKG.UPDATEACCOUNT", args, false);
    }


    public void deleteAccount(Connection conn, XanbooPrincipal xCaller) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[deleteAccount()]:");
        }
        
        SQLParam[] args=new SQLParam[2+2];     // SP parameters + 2 std parameters (errno, errmsg)
        // set IN params
        args[0] = new SQLParam(new Long(xCaller.getAccountId()), Types.BIGINT);
        args[1] = new SQLParam(new Long(xCaller.getUserId()), Types.BIGINT);
        
        dao.callSP(conn, "XC_ACCOUNT_PKG.DELETEACCOUNT", args, false);
        
    }


    /* Updates an existing controller subscription associated with the account  */
    XanbooSubscription updateSubscription(Connection conn, XanbooPrincipal xCaller, String subsId, String hwId, String masterPin, String masterDuress, String alarmPass, XanbooContact subsInfo, String hwIdNew, int alarmDelay, int tcFlag) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[updateSubscription()]:");
        }

        SQLParam[] args=new SQLParam[27+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // set IN params
        args[0] = new SQLParam( null );         //not checked anyways
        args[1] = new SQLParam( new Long(xCaller.getAccountId()), Types.BIGINT );
        args[2] = new SQLParam( subsId.trim() );
        args[3] = new SQLParam( hwId.trim() );

        //won't allow updates to these
        args[4]  = new SQLParam( null );     //null subs flags
        args[5] = new SQLParam( null );      //subs flags mask
        args[6]  = new SQLParam( (hwIdNew==null || hwIdNew.trim().length()==0) ?  null : hwIdNew.trim() );   //new hwId
        args[7]  = new SQLParam( null );    //label
        args[8]  = new SQLParam( null );    //tz

        args[9]  = new SQLParam( (masterPin==null || masterPin.trim().length()==0) ?  null : masterPin.trim() );
        args[10]  = new SQLParam( (masterDuress==null || masterDuress.trim().length()==0) ?  null : masterDuress );
        args[11]  = new SQLParam( (alarmPass==null || alarmPass.trim().length()==0) ?  null : alarmPass );

        //will come all null anyways
        args[12] = new SQLParam( subsInfo!=null ? subsInfo.getLastName() : null );
        args[13] = new SQLParam( subsInfo!=null ? subsInfo.getFirstName() : null );
        args[14] = new SQLParam( subsInfo!=null ? subsInfo.getAddress1() : null );
        args[15] = new SQLParam( subsInfo!=null ? subsInfo.getAddress2() : null );
        args[16] = new SQLParam( subsInfo!=null ? subsInfo.getCity() : null );
        args[17] = new SQLParam( subsInfo!=null ? subsInfo.getState() : null );
        args[18] = new SQLParam( subsInfo!=null ? subsInfo.getZip() : null );
        args[19] = new SQLParam( subsInfo!=null ? subsInfo.getZip4() : null );
        args[20] = new SQLParam( subsInfo!=null ? subsInfo.getCountry() : null );
        if(alarmDelay>=0)
            args[21] = new SQLParam( new Integer(alarmDelay), Types.INTEGER );
        else
            args[21] = new SQLParam( null );
        if(tcFlag>=0)
            args[22] = new SQLParam( new Integer(tcFlag), Types.INTEGER );
        else
            args[22] = new SQLParam( null );        //null for tc flag to not update it
        
        args[23] = new SQLParam( null );   // pass null subscription features to no allow updates!
        args[24] = new SQLParam( null);   //  pass null install type.
        
        args[25] = new SQLParam("", Types.VARCHAR, true);   // for returning subscription gguid
        args[26] = new SQLParam(new Integer(-1), Types.INTEGER, true);   //for returneing current subs flags value

        dao.callSP(conn, "XC_SYSADMIN_PKG.UPDATESUBSCRIPTION", args, false);

        XanbooSubscription xsub = new XanbooSubscription(xCaller.getAccountId(),null);
        xsub.setSubsId(subsId);
        xsub.setHwId(hwId);
        xsub.setSubsInfo(subsInfo);
        
        xsub.setGguid(args[25].getParam().toString());
        xsub.setSubsFlags(((Integer) args[26].getParam()).intValue());
        return xsub;

    }


    /* Gets a subscription record */
    XanbooResultSet getSubscription(Connection conn, XanbooPrincipal xCaller, String subsId, String hwId) throws XanbooException  {
        if (logger.isDebugEnabled()) {
            logger.debug("[getSubscription()]:");
        }

        XanbooResultSet results = null;
        SQLParam[] args=new SQLParam[4+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // set IN params
        args[0] = new SQLParam( null ); //not checked anyways
        args[1] = new SQLParam( new Long(xCaller.getAccountId()), Types.BIGINT );
        args[2] = new SQLParam( subsId );
        args[3] = new SQLParam( hwId );

        results = (XanbooResultSet) dao.callSP(conn, "XC_SYSADMIN_PKG.GETSUBSCRIPTION", args);

        return results;

    }

    
    public XanbooResultSet getProvisionedDeviceList(Connection conn, XanbooPrincipal xCaller, String subsId, String hwId, String classId, String subclassId, String installType) throws XanbooException   {
        if(logger.isDebugEnabled()){
            logger.debug("[getProvisionedDeviceList]");
        }

        SQLParam[] args=new SQLParam[7+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // set IN params
        args[0] = new SQLParam( xCaller.getUsername() );
        args[1] = new SQLParam( new Long(xCaller.getAccountId()), Types.BIGINT );
        args[2] = new SQLParam( subsId );
        args[3] = new SQLParam( hwId );
        args[4] = new SQLParam( classId );
        args[5] = new SQLParam( subclassId );
        args[6] = new SQLParam( installType );
        
        XanbooResultSet results = (XanbooResultSet)dao.callSP(conn, "XC_SYSADMIN_PKG.GETPROVISIONEDDEVICELIST", args);
        
        return results;
    } 

    
     /**
     * Returns the total number of alerts for an account/gguid
     */
     int getAlertCount(Connection conn, XanbooPrincipal xCaller, String gatewayGUID) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getAlertCount()]:");
        }

        SQLParam[] args=new SQLParam[4+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam( "_customer_" );     //fake id
        args[1] = new SQLParam( new Long(xCaller.getAccountId()), Types.BIGINT );
        args[2] = new SQLParam( gatewayGUID );

        //OUT parameter: returning profile ID
        args[3] = new SQLParam(new Integer(-1), Types.INTEGER, true);


        dao.callSP(conn, "XC_MISC_PKG.GETALERTCOUNT", args, false);

        // return profile id
        return ((Integer) args[3].getParam()).intValue();

     }
     
     
     
    /**
     * Queues notification actions on the occurrance of an event.
     *
     * @param conn The database connection to use for this transaction
     * @param xc XailCommand prepared with deviceGUID, password, eventId, and timestamp
     * @param accountId For authentication
     * @param gatewayGUID
     *
     * @return the new event log id
     * @throws XanbooException
     */
    protected void queueAction(Connection conn, String domainId, long accountId, String extAccId, String gguid, String dguid, String eId, String label, String srcLabel, String lang, String tz, String ptype, String pAddr) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[queueAction()]:");
        }
        
        
        try {
            SQLParam[] args=new SQLParam[12+2];     //SP parameters + 2 std parameters (errno, errmsg)
            
            // setting IN params
            args[0] = new SQLParam( domainId );
            args[1] = new SQLParam( new Long(accountId), Types.BIGINT );
            args[2] = new SQLParam( extAccId );
            args[3] = new SQLParam( gguid );   
            args[4] = new SQLParam( dguid );       
            args[5] = new SQLParam( eId );       
            args[6] = new SQLParam( label );       
            args[7] = new SQLParam( srcLabel );       
            args[8] = new SQLParam( lang );       
            args[9] = new SQLParam( tz );       
            args[10] = new SQLParam( ptype );       
            args[11] = new SQLParam( pAddr );       

            dao.callSP(conn, "XC_ACCOUNT_PKG.QUEUEACTION", args, false);

        }catch(XanbooException xe) {
            /* ignore quota reached error */
            if(xe.getCode()==282) {
                if (logger.isDebugEnabled()) {
                    logger.debug( "[queueAction()]: " + xe.getErrorMessage() + " - account id:" + accountId);
                }
                return;
            }
            
            if (logger.isDebugEnabled()) {
                logger.debug( "[queueAction()]: " + xe.getErrorMessage());
            }
            throw xe;
        }catch(Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("[queueAction()]:" + e.getMessage(), e);
            } else {
                logger.debug("[queueAction()]:" + e.getMessage() );
            }
            throw new XanbooException(10030);  //Exception while executing DAO method;
        }
       
    }    
     
     public Integer getNotificationOptInStatus(Connection conn, String domainId, String notificationAddress, String token) throws XanbooException {
        SQLParam[] args=new SQLParam[4+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(domainId );
        args[1] = new SQLParam((notificationAddress==null || notificationAddress.length()==0) ? null : notificationAddress);
        args[2] = new SQLParam((token==null || token.length()==0) ? null : token);

        //OUT parameter: returning opt-in status
        args[3] = new SQLParam(new Integer(-1), Types.INTEGER, true);


        dao.callSP(conn, "XC_SYSADMIN_PKG.GETNOTIFICATIONOPTINSTATUS", args, false);

        // return opt-in status
        Integer returnVal = (Integer) args[3].getParam();
        if (returnVal == null || returnVal == -1)
            return null;
        return returnVal;
     }

     public Map<String, Integer> getNotificationOptInStatus( Connection conn, String domainId, List<String> notificationAddresses ) throws XanbooException {
        if (logger.isDebugEnabled()) {
            logger.debug("[getNotificationOptInStatusList()]");
        }

        StringBuffer notificationAddressStr = new StringBuffer();

        for (String notificationAddress : notificationAddresses) {
            if (notificationAddressStr.length() > 0)
                 notificationAddressStr.append(",");

             notificationAddressStr.append("'").append(notificationAddress).append("'");
        }

        SQLParam[] args=new SQLParam[3+2];     // 2 SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(domainId);
        args[1] = new SQLParam(notificationAddressStr);
        args[2] = new SQLParam(null);

        XanbooResultSet dbOutput = dao.callSP(conn, "XC_SYSADMIN_PKG.getNotificationOptInStatusList", args);

        HashMap result = new HashMap<String, Integer>();

        if (dbOutput == null) return result;

        for (int i=0; (dbOutput != null && i < dbOutput.size()); i++) {
                String addr = dbOutput.getElementString(i, "PROFILE_ADDRESS");
                Integer statusId = dbOutput.getElementInteger(i, "STATUS_ID");
                result.put(addr, statusId);
        }
        return result;
    }

    public void setNotificationOptInStatus( Connection conn, String domainId, Long accountId,String notificationAddress, String token, int status, String language, String tzname, String profileType ) throws XanbooException {
        SQLParam[] args=new SQLParam[9+2];     // SP parameters + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(domainId);
        args[1] = new SQLParam(accountId);
        args[2] = new SQLParam((notificationAddress==null || notificationAddress.length()==0) ? null : notificationAddress);
        args[3] = new SQLParam((token==null || token.length()==0) ? null : token);
        args[4] = new SQLParam(new Integer(status), Types.INTEGER);
        args[5] = new SQLParam((language==null || language.length()==0) ? null : language);
        args[6] = new SQLParam((tzname==null || tzname.length()==0) ? null : tzname);
        args[7] = new SQLParam((profileType==null || profileType.length()==0) ? null : profileType);

        args[8] = new SQLParam("", Types.VARCHAR, true); //returns the token
        dao.callSP(conn, "XC_SYSADMIN_PKG.SETNOTIFICATIONOPTINSTATUS", args, false);
    }
    
    
    public XanbooResultSet getSupportedDeviceList(Connection conn, String domainId, String installType, String monType) throws XanbooException {
      
    	if(logger.isDebugEnabled()){
            logger.debug("[getSupportedDeviceList]");
        }

        SQLParam[] args=new SQLParam[3+2];     // SP parameters 3 + 2 std parameters (errno, errmsg)

        // set IN params 
        args[0] = new SQLParam( domainId );
        args[1] = new SQLParam( installType );
        args[2] = new SQLParam( monType );
        
        XanbooResultSet results = (XanbooResultSet)dao.callSP(conn, "XC_SYSADMIN_PKG.GETSUPPORTEDDEVICELIST", args);
        
        return results;
    } 
    
    public XanbooResultSet getSupportedDeviceList(Connection conn, String domainId, String subsId, String hwId,  boolean includeProvisioned) throws XanbooException {
        
    	if(logger.isDebugEnabled()) {
            logger.debug("[getSupportedDeviceList]");
        }

        SQLParam[] args=new SQLParam[4+2];     // SP parameters 4 + 2 std parameters (errno, errmsg)

        // set IN params
        args[0] = new SQLParam( domainId );
        args[1] = new SQLParam( subsId );
        args[2] = new SQLParam( hwId );
        args[3] = new SQLParam( includeProvisioned ? 1 : 0, Types.INTEGER);
        
        XanbooResultSet results = (XanbooResultSet) dao.callSP(conn, "XC_SYSADMIN_PKG.GETSUPPORTEDDEVICELIST", args);
        
        return results;
    }
    
 public XanbooResultSet getSupportedClassList(Connection conn,String domainId, Long accountId, String gateway_guid,String subsId, String hwId) throws XanbooException {
        
    	if(logger.isDebugEnabled()) {
            logger.debug("[getSupportedClassList]");
        }

        SQLParam[] args=new SQLParam[5+2];     // SP parameters 4 + 2 std parameters (errno, errmsg)

        // set IN params
        args[0] = new SQLParam( accountId );
        args[1] = new SQLParam( gateway_guid );
        args[2] = new SQLParam( subsId );
        args[3] = new SQLParam( hwId);
        args[4] = new SQLParam( domainId);
        
        XanbooResultSet results = (XanbooResultSet) dao.callSP(conn, "XC_ACCOUNT_PKG.GETSUPPORTEDCLASSLIST", args);
        
        return results;
    }
 
	public XanbooResultSet getGatewayGuids(Connection conn, Long accountId) throws XanbooException {		        
		if(logger.isDebugEnabled()){
			logger.debug("[getGatewayGuids]");
		}

		SQLParam[] args=new SQLParam[1+2];     // SP parameters 1 + 2 std parameters (errno, errmsg)

		args[0] = new SQLParam( accountId );
		        
		XanbooResultSet results = (XanbooResultSet)dao.callSP(conn, "XC_SYSADMIN_PKG.GETGATEWAYGUIDS", args);
		        
		return results;
	} 
 
}
