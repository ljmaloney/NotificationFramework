/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.sdk.notification;

import java.io.IOException;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.annotation.PostConstruct;
import javax.ejb.CreateException;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import com.xanboo.core.sdk.AbstractSDKManagerEJB;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.GlobalNames;
import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;
import com.xanboo.core.util.SBNSynchronizer;
import com.xanboo.core.util.XanbooException;
import com.xanboo.core.util.XanbooUtil;

/**
 * NotificationManagerEJB<br/>
 * <br/>
 * This EJB contains functionality to acknowledges receipt of a notification message, create, modify, and delete a notification chain,
 * and perform the initial processing of the notification event. 
 * <br/>
 * @author lm899p
 */
@Remote (NotificationManager.class)
@Stateless (name  = "NotificationManager")
@TransactionManagement( TransactionManagementType.BEAN )
public class NotificationManagerEJB extends AbstractSDKManagerEJB  
{
    private Logger logger;
     // related DAO class
    private NotificationManagerDAO dao=null;
    
    public static final String SBN_MISC3_FIELD="DP01____##RW";
    public static final String SBN_ALARM_COMMENT_CD = "SC4";
    
    private final String rndStr = new String("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
    
 @PostConstruct
    public void init() throws CreateException 
    {
        try 
        {
            // create a logger instance
            logger=LoggerFactory.getLogger(this.getClass().getName());
            if(logger.isDebugEnabled()) 
            {
                logger.debug("[init()]:");
            }
            
            dao = new NotificationManagerDAO();

        }
        catch (Exception se) 
        {
            throw new CreateException("Failed to @init NotificationManager:" + se.getMessage());
        }
        
    }
    

    
    public String generateToken(String gguid)
    {
        int gguidHashCode = gguid.hashCode();
        //ensure this is a unique seed ... get the current timestamp and add the gguid hash code
        //this is to avoid a case where two gateways connect act exactly the same millisecond and 
        //as a result, the two gateways use the same token
        long seed = System.currentTimeMillis() + (long)gguidHashCode;
        Random rnd = new Random(seed); 
        StringBuilder bldr = new StringBuilder();
        //32 character token string, randomly generated
        for ( int i = 0; i < 8; i++ )
        {
            double d = rnd.nextDouble();
            int index = (int)(rndStr.length() * d);
            if ( index == rndStr.length() )
                index = index - 1;
            bldr.append(rndStr.charAt(index));
        }
        return bldr.toString();
    }
    

    
    
     /**
     * This method is used to acknowledge receipt of the notification message by the user (notification profile). 
     * @param xCaller
     * @param queueId
     * @param messageToken
     * @param profileId
     * @throws RemoteException
     * @throws XanbooException 
     */
    public void acknowledgeMessage(XanbooPrincipal xCaller,Long queueId,String messageToken,Long contactId)throws XanbooException
    {
        if (logger.isDebugEnabled()) 
        {
            logger.debug("[acknowledgeMessage()]:");
        }

        if ( queueId == null || messageToken == null  || messageToken.equalsIgnoreCase(""))
            throw new XanbooException(33006,"QueueId and MessageToken are required parameters");
        
        Connection conn=null;
        boolean rollback=false;
        try 
        {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            dao.acknowledgeMessage(xCaller.getAccountId(), queueId, messageToken, contactId,conn);
        }
        catch (XanbooException xe) 
        {
            rollback=true;            
            throw xe;
        }
        catch (Exception e) 
        {
            rollback=true;
            if(logger.isDebugEnabled()) 
            {
              logger.error("[acknowledgeMessage()]: Exception:" + e.getMessage(), e);
            }
            else 
            {
              logger.error("[acknowledgeMessage()]: Exception:" + e.getMessage());
            }                                        
            throw new XanbooException(10030, "[acknowledgeMessage()]: Exception:" + e.getMessage());
        }
        finally 
        {
            dao.closeConnection(conn, rollback);
        }
    }
    /**
     * Deletes an entire notification chain from the system including all of the references to notification profiles in the chain. 
     * <br/> 
     * <b>Note : This method does not delete any contacts or user records</b>
     * @param xCaller - Identify the logged in user calling the method  
     * @param chainId - The identifier of the chain to delete
     * @throws RemoteException
     * @throws XanbooException - This is thrown when attempting to delete a chain when the chain is being used to send a notification. 
     */
    public void deleteNotificationChain(XanbooPrincipal xCaller,Long chainId)throws XanbooException
    {
        if (logger.isDebugEnabled()) 
        {
            logger.debug("[deleteNotificationChain()]:");
        }
        if ( chainId == null || chainId == -1 )
            throw new XanbooException(33006,"ChainId is required");
        Connection conn=null;
        boolean rollback=false;
        try 
        {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            dao.deleteNotificationChain(chainId, conn);
        }
        catch (XanbooException xe) 
        {
            rollback=true;            
            throw xe;
        }
        catch (Exception e) 
        {
            rollback=true;
            if(logger.isDebugEnabled()) 
            {
              logger.error("[deleteNotificationChain()]: Exception:" + e.getMessage(), e);
            }
            else 
            {
              logger.error("[deleteNotificationChain()]: Exception:" + e.getMessage());
            }                                        
            throw new XanbooException(10030, "[deleteNotificationChain()]: Exception:" + e.getMessage());
        }
        finally 
        {
            dao.closeConnection(conn, rollback);
        }
    }
    /**
     * Deletes a notification profile from a notification chain. If the deleted profile is not the last item in the chain, all subsequent members of the chain
     * are moved up by one. 
     * @param xCaller
     * @param chainId
     * @param profileId
     * @throws RemoteException
     * @throws XanbooException - Thrown when attempting to modify a notification chain which is being used to send notification messages.
     */
    public void deleteNotificationChainItem(XanbooPrincipal xCaller,Long chainId,Long contactId)throws XanbooException
    {
        if (logger.isDebugEnabled()) 
        {
            logger.debug("[deleteNotificationChainItem()]:");
        }

        if ( chainId == null || contactId == null )
            throw new XanbooException(33006,"Missing one or more required parameters");
        
        Connection conn=null;
        boolean rollback=false;
        try 
        {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            dao.deleteNotificationChainItem(chainId, contactId, conn);
        }
        catch (XanbooException xe) 
        {
            rollback=true;            
            throw xe;
        }
        catch (Exception e) 
        {
            rollback=true;
            if(logger.isDebugEnabled()) 
            {
              logger.error("[deleteNotificationChainItem()]: Exception:" + e.getMessage(), e);
            }
            else 
            {
              logger.error("[deleteNotificationChainItem()]: Exception:" + e.getMessage());
            }                                        
            throw new XanbooException(10030, "[deleteNotificationChainItem()]: Exception:" + e.getMessage());
        }
        finally 
        {
            dao.closeConnection(conn, rollback);
        }
    }
    
    public void deleteNotificationChainItem(XanbooPrincipal xCaller,Long chainId,NotificationChainRecipient item)throws XanbooException
    {
        if (logger.isDebugEnabled()) 
        {
            logger.debug("[deleteNotificationChainItem()]:");
        }

        if ( chainId == null || item == null )
            throw new XanbooException(33006,"Missing one or more required parameters");
        
        if ( item.getDestination() == null && (item.getChainSeqNo() == null || item.getChainSeqNo() == -1))
            throw new XanbooException(33006,"Either the destination or sequence number must be populated");
        
        if ( item.getDestination() != null && (item.getContactId() == null || item.getContactId() == -1l))
            throw new XanbooException(33006,"Contact Id is required when destination is populated");
        
        Connection conn=null;
        boolean rollback=false;
        try 
        {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            dao.deleteNotificationChainItem(xCaller.getAccountId(),chainId, item, conn);
        }
        catch (XanbooException xe) 
        {
            rollback=true;            
            throw xe;
        }
        catch (Exception e) 
        {
            rollback=true;
            if(logger.isDebugEnabled()) 
            {
              logger.error("[deleteNotificationChainItem()]: Exception:" + e.getMessage(), e);
            }
            else 
            {
              logger.error("[deleteNotificationChainItem()]: Exception:" + e.getMessage());
            }                                        
            throw new XanbooException(10030, "[deleteNotificationChainItem()]: Exception:" + e.getMessage());
        }
        finally 
        {
            dao.closeConnection(conn, rollback);
        }
    }
    
    /**
     * Inserts a link (notification profile) into a notification chain.
     * @param xCaller - Identifies the Digital Life user calling inserting a profile into the notification chain. 
     * @param chainId - Identifies the notification chain being modified. 
     * @param item - The <code>NotificationChainRecipient</code> to insert. 
     * @throws RemoteException
     * @throws XanbooException - Thrown when 1. The insert attempt will cause the length of the chain to exceed the max notification chain length
     *                                       2. Attempting to modify the notification chain when a notification is being processed using this chain.
     */
    public void newNotificationChainItem(XanbooPrincipal xCaller,Long chainId,NotificationChainRecipient recipient)throws XanbooException
    {
        if (logger.isDebugEnabled()) 
        {
            logger.debug("[newNotificationChainItem()]:");
        }
        
        if ( chainId == null || chainId == -1 || recipient == null )
            throw new XanbooException(33006,"Missing one or more required parameters");
        
        if ( recipient.getContactId() == null && recipient.getUserId() == null )
            throw new XanbooException(33006,"Missing both contactId and userId.");
        
        if ( recipient.getDestination() == null )
            throw new XanbooException(33006,"Missing destination selection. Destination must be one of PHONE, PHONE_CELL, or FAX");

        Connection conn=null;
        boolean rollback=false;
        try 
        {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            dao.newNotificationChainItem(chainId, recipient, conn);
        }
        catch (XanbooException xe) 
        {
            rollback=true;            
            throw xe;
        }
        catch (Exception e) 
        {
            rollback=true;
            if(logger.isDebugEnabled()) 
            {
              logger.error("[newNotificationChainItem()]: Exception:" + e.getMessage(), e);
            }
            else 
            {
              logger.error("[newNotificationChainItem()]: Exception:" + e.getMessage());
            }                                        
            throw new XanbooException(10030, "[newNotificationChainItem()]: Exception:" + e.getMessage());
        }
        finally 
        {
            dao.closeConnection(conn, rollback);
        }
    }
    /**
     * Returns the list of notification chains for the given criteria. 
     * @param xCaller - Identifies the logged in digital life user
     * @param accountId - Specifies the account for which to return the notification chain(s)
     * @return XanbooResultSet containing the notification chains for the specified account
     * @throws RemoteException
     * @throws XanbooException 
     */
    public List<NotificationChain> getNotificationChainList(XanbooPrincipal xCaller,Long accountId)throws XanbooException
    {
        if (logger.isDebugEnabled()) 
        {
            logger.debug("[getNotificationChainList()]:");
        }

        if ( accountId == null || accountId == -1 )
            throw new XanbooException(33006,"Required parameter, accountId, is missing");
        
        Connection conn=null;
        boolean rollback=false;
        try 
        {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            return dao.getNotificationChainList(accountId, conn);
        }
        catch (XanbooException xe) 
        {
            rollback=true;            
            throw xe;
        }
        catch (Exception e) 
        {
            rollback=true;
            if(logger.isDebugEnabled()) 
            {
              logger.error("[getNotificationChainList()]: Exception:" + e.getMessage(), e);
            }
            else 
            {
              logger.error("[getNotificationChainList()]: Exception:" + e.getMessage());
            }                                        
            throw new XanbooException(10030, "[getNotificationChainList()]: Exception:" + e.getMessage());
        }
        finally 
        {
            dao.closeConnection(conn, rollback);
        }
    }
    /**
     * Returns the list of notification chain items (notification profiles) for a given chain. 
     * @param xcaller - Identifies the user 
     * @param chainId - Specifies the the chain for which to return the chain items
     * @return List<NotificationChainItem>
     * @throws RemoteException
     * @throws XanbooException 
     */
    public List<NotificationChainRecipient> getNotificationChainItemList(XanbooPrincipal xCaller,Long chainId)throws XanbooException
    {
        if (logger.isDebugEnabled()) 
        {
            logger.debug("[getNotificationChainItemList()]:");
        }
        
        if ( chainId == null || chainId == -1 )
            throw new XanbooException(33006,"chainId is required");

        Connection conn=null;
        boolean rollback=false;
        try 
        {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            return dao.getNotificationChainItemList(chainId, conn);
        }
        catch (XanbooException xe) 
        {
            rollback=true;            
            throw xe;
        }
        catch (Exception e) 
        {
            rollback=true;
            if(logger.isDebugEnabled()) 
            {
              logger.error("[getNotificationChainItemList()]: Exception:" + e.getMessage(), e);
            }
            else 
            {
              logger.error("[getNotificationChainItemList()]: Exception:" + e.getMessage());
            }                                        
            throw new XanbooException(10030, "[getNotificationChainItemList()]: Exception:" + e.getMessage());
        }
        finally 
        {
            dao.closeConnection(conn, rollback);
        }
    }
    
    private NotificationChain getNotificationChainItemList(XanbooPrincipal xCaller,NotificationChain chain)throws XanbooException
    {
        if ( chain == null )
            throw new XanbooException(33006,"NotificationChain is a required parameter");
        
        chain.setRecipients(getNotificationChainItemList(xCaller,chain.getChainId()));
        return chain;
    }
    /**
     * Returns any notification messages available to be sent. Not for use by the user interface
     * @param xCaller
     * @return XanbooResultSet 
     * @throws RemoteException
     * @throws XanbooException 
     */
    public XanbooResultSet getPendingNotifications(XanbooPrincipal xCaller)throws XanbooException
    {
        if (logger.isDebugEnabled()) 
        {
            logger.debug("[getPendingNotifications()]:");
        }

        Connection conn=null;
        boolean rollback=false;
        try 
        {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            return dao.getAccountPendingNotifications(xCaller.getAccountId(), conn);
        }
        catch (XanbooException xe) 
        {
            rollback=true;            
            throw xe;
        }
        catch (Exception e) 
        {
            rollback=true;
            if(logger.isDebugEnabled()) 
            {
              logger.error("[getPendingNotifications()]: Exception:" + e.getMessage(), e);
            }
            else 
            {
              logger.error("[getPendingNotifications()]: Exception:" + e.getMessage());
            }                                        
            throw new XanbooException(10030, "[getPendingNotifications()]: Exception:" + e.getMessage());
        }
        finally 
        {
            dao.closeConnection(conn, rollback);
        }
    }
    
    /**
     * 
     * @param xCaller
     * @param queueId
     * @param messageToken
     * @param sendAlarm
     * @throws XanbooException 
     */
    public void terminatePendingNotification(XanbooPrincipal xCaller,Integer queueId,String messageToken,Boolean overrideSBNFlag)throws XanbooException
    {
        if ( logger.isDebugEnabled() )
            logger.debug("[terminatePendingNotification()] queueId="+queueId+", messageToken="+messageToken+", overrideSBN="+overrideSBNFlag);
        
        //validation, queueId and messageToken are required
        if ( queueId == null || queueId < 0 )
            throw new XanbooException(33006,"Missing required parameter, queueId");
        if ( messageToken == null || messageToken.equalsIgnoreCase(""))
            throw new XanbooException(33006,"Missing required parameter, messageToken");
        
        Connection conn=null;
        boolean rollback=false;
        try 
        {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            //get the pending message
            XanbooResultSet resultSet = dao.getPendingNotification(xCaller.getAccountId(), new Long(queueId), messageToken, conn);
            
            Properties sbnProps = this.unmarshallProperties(resultSet.getElementString(0, "ALARM_PARAMS"));
            String gguid = resultSet.getElementString(0,"GATEWAY_GUID");
            String dguid = resultSet.getElementString(0, "DEVICE_GUID");
            String deviceName = resultSet.getElementString(0, "DEVICE_NAME");
            String sendToSBNStr = resultSet.getElementString(0,"SEND_TO_SBN");
            boolean sendAlarm = sendToSBNStr.equals("1") ? true : false;
            
            //determine if the sbn flag for the chain is being overriden
            if ( overrideSBNFlag != null )
            {
                if ( logger.isDebugEnabled() )
                    logger.debug("[terminatePendingNotification()] - overriding existing sendToSBN ("+sendAlarm+") with overrideSBNFLag="+overrideSBNFlag);
                sendAlarm = overrideSBNFlag;
            }
                      
            //if sending the alarm
            if ( sendAlarm )
            {
                if ( !sbnProps.containsKey("ALARM_EVENT_ID"))
                {
                    throw new XanbooException(33020,"Missing required parameter, ALARM_EVENT_ID must be passed when sendToSBN=true.");
                }
                
                StringBuilder str = new StringBuilder(SBN_ALARM_COMMENT_CD+" ");
                str.append(String.format("%1$4s ", sbnProps.getProperty("ALARM_EVENT_ID")));
                str.append(String.format("%1$10s ", dguid));
                if ( sbnProps.containsKey("COMMENT")) 
                    str.append(String.format("%1$30s", sbnProps.getProperty("COMMENT")));
                else
                    str.append(String.format("%1$30s", deviceName));

                SBNSynchronizer sbn = new SBNSynchronizer();

                //generate alarm call.  Sending empty string for @dguid param since it is in the comment field.
                //Last param is '@misc3' - set to a fixed value
                if ( logger.isDebugEnabled() )
                    logger.debug("[terminatePendingNotification()] - SBN Alarm gguid="+gguid+" comment : "+str.toString());

                sbn.generateAlarm(gguid, "", 0, str.toString(), SBN_MISC3_FIELD);
            }
            
            //terminate the remaining messages
            dao.acknowledgeMessage(xCaller.getAccountId(), new Long(queueId), messageToken, null,conn);
        }
        catch (XanbooException xe) 
        {
            rollback=true;            
            throw xe;
        }
        catch (Exception e) 
        {
            rollback=true;
            if(logger.isDebugEnabled()) 
            {
              logger.error("[terminatePendingNotification()]: Exception:" + e.getMessage(), e);
            }
            else 
            {
              logger.error("[terminatePendingNotification()]: Exception:" + e.getMessage());
            }                                        
            throw new XanbooException(10030, "[terminatePendingNotification()]: Exception:" + e.getMessage());
        }
        finally 
        {
            dao.closeConnection(conn, rollback);
        }
        
    }
    
    private Properties unmarshallProperties(String propStr)
    {
        
        Properties p = new Properties();
        if ( propStr != null && !propStr.equalsIgnoreCase(""))
        {
            try
            {
                StringReader sr = new StringReader(propStr);
                p.load(sr);
            }
            catch (IOException ex)
            {
                logger.warn("[unmarshallProperties()] - unexpected error ",ex);
            }
        }
        return p;
    }
    
    public NotificationChain addUpdateNotificationChain(XanbooPrincipal xCaller,NotificationChain notificationChain)throws XanbooException
    {
        if (logger.isDebugEnabled()) 
        {
            logger.debug("[updateNotificationChain()]:");
        }

        if ( notificationChain.getAccountId() == -1l)   //if accountId was not provided, use the accountId from the XanbooPrincipal
            notificationChain.setAccountId(xCaller.getAccountId());
        
        if ( notificationChain.getDescription() == null || notificationChain.getDescription().equalsIgnoreCase("") )
        {
            logger.warn("[updateNotificationChain()] - the name of the notification chain was not provided");
            throw new XanbooException(33006,"The name/description of the notification chain was not provided");
        }
        
        if ( notificationChain.getRecipients() != null && notificationChain.getRecipients().size() > 10 )
            throw new XanbooException(33012,"Maximum number of members in a notification chain exceeded");
        
        Connection conn=null;
        boolean rollback=false;
        try 
        {
            // validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(xCaller);
            conn=dao.getConnection();
            notificationChain = dao.updateNotificationChain(notificationChain, conn);
        }
        catch (XanbooException xe) 
        {
            rollback=true;            
            throw xe;
        }
        catch (Exception e) 
        {
            rollback=true;
            if(logger.isDebugEnabled()) 
            {
              logger.error("[updateNotificationChain()]: Exception:" + e.getMessage(), e);
            }
            else 
            {
              logger.error("[updateNotificationChain()]: Exception:" + e.getMessage());
            }                                        
            throw new XanbooException(10030, "[updateNotificationChain()]: Exception:" + e.getMessage());
        }
        finally 
        {
            dao.closeConnection(conn, rollback);
        }
        notificationChain = this.getNotificationChainItemList(xCaller,notificationChain);
        return notificationChain;
    }
    
    /**
     * Method to create or update a notification chain. 
     * @param xCaller - The user creating the chain
     * @param accountId - The account associated with the notification chain
     * @param chainId - The chain to modify/update. If this parameter is null, a new chain is created.
     * @param name - The name of the notification chain. The name of the chain must be unique for any given account
     * @param waitTimeType - Indicates how the quiet_time column is to be handled for recipients on this chain. 
     *                       Valid values are "event" and "interval". "event" - the time to wait between the occurrence of the notification event 
     *                       and sending the message to the recipient(s).  "interval" - the time to wait for acknowledgement after sending 
     *                       notification to the recipient.
     * @param sendToSBN - Parameter used to indicate that any notification event sent using this chain will be sent to SBN if the no recipient in the
     *                    chain has acknowleged the notification message.
     * @param useAsDefault - Indicates the chain is to be used as the default chain for ADLs
     * @param items - The list (array) of items (notification profiles) included in the notification chain. If this parameter is null the profiles
     *                in the notification chain are not modified. If this parameter is not null, the contents of this array replaces all of the
     *                profiles associated with the chain.
     * @return An instance of <code>NotificationChain</code> with all fields populated.
     * @throws RemoteException
     * @throws XanbooException 
     */
    public NotificationChain addUpdateNotificationChain(XanbooPrincipal xCaller,Long accountId,Long chainId,String name,String waitTimeType,
                                                     Boolean sendToSBN,Boolean useAsDefault,NotificationChainRecipient[] items)throws XanbooException
    {
        if (logger.isDebugEnabled()) 
        {
            logger.debug("[updateNotificationChain()]:");
        }
        
        if ( !waitTimeType.equals(NotificationChain.QuietTimeType.EVENT.getDBValue()) && 
             !waitTimeType.equals(NotificationChain.QuietTimeType.INTERVAL.getDBValue()) )
            throw new XanbooException(33007,"Incorrect value for wait time type");
        
        if ( accountId == null || name == null || name.equalsIgnoreCase(""))
            throw new XanbooException(33006,"Missing one or more required parameters");
        
        if ( items != null && items.length > GlobalNames.NOTIFICATION_CHAIN_MAX )
            throw new XanbooException(33012,"Maximum number of members in a notification chain exceeded");
        
        if ( items != null )
        {
            for ( NotificationChainRecipient recipient : items )
            {
                if ( recipient.getContactId() == null && recipient.getUserId() == null )
                    throw new XanbooException(33006,"Missing both contactId and userId.");
        
                if ( recipient.getDestination() == null )
                    throw new XanbooException(33006,"Missing destination selection. Destination must be one of PHONE, PHONE_CELL, or FAX");
            }
        }
        
        NotificationChain chain = new NotificationChain();
        chain.setAccountId(accountId);
        chain.setChainId((chainId == null ? -1 : chainId));
        chain.setSendToSBN((sendToSBN == null ? false : sendToSBN));
        chain.setUseAsDefault((useAsDefault == null ? false : useAsDefault));
        chain.setQuietTypeType(waitTimeType);
        chain.setDescription(name);
        if ( items != null )
        {
            for ( NotificationChainRecipient r : items )
                chain.addRecipient(r);
        }
        return addUpdateNotificationChain(xCaller,chain);
    }
}
