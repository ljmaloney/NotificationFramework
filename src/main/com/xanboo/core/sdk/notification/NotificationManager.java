/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.sdk.notification;

import java.rmi.RemoteException;
import java.util.List;

import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.XanbooException;

/**
 * NotificationManager<br/>
 * <br/>
 * This EJB contains functionality to acknowledges receipt of a notification message, create, modify, and delete a notification chain,
 * and perform the initial processing of the notification event. 
 * <br/>
 * @author lm899p
 */

public interface NotificationManager   
{
    /**
     * This method is used to acknowledge receipt of the notification message by the contact. 
     * @param xCaller
     * @param queueId
     * @param messageToken
     * @param contactId
     * @throws RemoteException
     * @throws XanbooException 
     */
    public void acknowledgeMessage(XanbooPrincipal xCaller,Long queueId,String messageToken,Long contactId)throws RemoteException, XanbooException;
    /**
     * Deletes an entire notification chain from the system including all of the recipients in the chain. 
     * <br/> 
     * <b>Note : This method does not delete any contacts or user records</b>
     * @param xCaller - Identify the logged in user calling the method  
     * @param chainId - The identifier of the chain to delete
     * @throws RemoteException
     * @throws XanbooException - This is thrown when attempting to delete a chain when the chain is being used to send a notification. 
     */
    public void deleteNotificationChain(XanbooPrincipal xCaller,Long chainId)throws RemoteException, XanbooException;
    /**
     * Deletes a contact from a notification chain. If the deleted contact is not the last item in the chain, all subsequent members of the chain
     * are moved up by one. 
     * @param xCaller
     * @param chainId
     * @param contactId
     * @throws RemoteException
     * @throws XanbooException - Thrown when attempting to modify a notification chain which is being used to send notification messages.
     */
    public void deleteNotificationChainItem(XanbooPrincipal xCaller,Long chainId,Long contactId)throws RemoteException, XanbooException;
    /**
     * Deletes a specific recipient from the notification chain
     * @param xCaller - Identifies the account of the caller
     * @param chainId
     * @param item - The recipient to delete from the chain
     * @throws RemoteException
     * @throws XanbooException 
     */
    public void deleteNotificationChainItem(XanbooPrincipal xCaller,Long chainId,NotificationChainRecipient item)throws RemoteException,XanbooException;
    /**
     * Inserts a link (contact/recipient) into a notification chain.
     * @param xCaller - Identifies the Digital Life user calling inserting a recipient into the notification chain. 
     * @param chainId - Identifies the notification chain being modified. 
     * @param item - The <code>NotificationChainRecipient</code> to insert. 
     * @throws RemoteException
     * @throws XanbooException - Thrown when 1. The insert attempt will cause the length of the chain to exceed the max notification chain length
     *                                       2. Attempting to modify the notification chain when a notification is being processed using this chain.
     */
    public void newNotificationChainItem(XanbooPrincipal xCaller,Long chainId,NotificationChainRecipient item)throws RemoteException,XanbooException;
    /**
     * Returns the list of notification chains for the given criteria. 
     * @param xCaller - Identifies the logged in digital life user
     * @param accountId - Specifies the account for which to return the notification chain(s)
     * @return a collection of the notification chains for the specified account
     * @throws RemoteException
     * @throws XanbooException 
     */
    public List<NotificationChain> getNotificationChainList(XanbooPrincipal xCaller,Long accountId)throws RemoteException, XanbooException;
    /**
     * Returns the list of notification chain items (recipients) for a given chain. 
     * @param xcaller - Identifies the user 
     * @param chainId - Specifies the the chain for which to return the chain items
     * @return List<NotificationChainItem>
     * @throws RemoteException
     * @throws XanbooException 
     */
    public List<NotificationChainRecipient> getNotificationChainItemList(XanbooPrincipal xcaller,Long chainId)throws RemoteException, XanbooException;
    /**
     * Returns any notification messages available to be sent. Not for use by the user interface
     * @param xCaller
     * @return XanbooResultSet 
     * @throws RemoteException
     * @throws XanbooException 
     */
    public XanbooResultSet getPendingNotifications(XanbooPrincipal xCaller)throws RemoteException, XanbooException;
    
    /**
     * Method to terminate sending notification messages to the recipients in the chain. 
     * @param xCaller - Identifies the user terminating the chain
     * @param queueId - identifies the queue
     * @param messageToken - A token to identify the message
     * @param overrideSBNFlag - indicates if the value of <code>NotificationChain.sendToSBN</code> is overridden. If a "null" is passed for this 
     *                          parameter, the value of <code>NotificationChain.sendToSBN</code> is used. Otherwise, if a value is passed, true indicates 
     *                          an alarm should be sent (even if <code>NotificationChain.sendToSBN()</code> is false). False indicates that no alarm is to be
     *                          sent even if <code>NotificationChain.sendToSBN()</code> is true. 
     * @throws RemoteException
     * @throws XanbooException 
     */
    public void terminatePendingNotification(XanbooPrincipal xCaller,Integer queueId,String messageToken,Boolean sendAlarm)throws RemoteException,XanbooException;
    /**
     * Method to create or update a notification chain.
     * @param xCaller - The user creating the chain
     * @param notificationChain - The populated NotificationChain instance. If the chainId is null or -1, a new chain is created. 
     * @return instance of <code>NotificationChain</code> with chainId set.
     * @throws RemoteException
     * @throws XanbooException 
     */
    public NotificationChain addUpdateNotificationChain(XanbooPrincipal xCaller,NotificationChain notificationChain)throws RemoteException, XanbooException;
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
                                                     Boolean sendToSBN,Boolean useAsDefault,NotificationChainRecipient[] items)throws RemoteException, XanbooException;
}
