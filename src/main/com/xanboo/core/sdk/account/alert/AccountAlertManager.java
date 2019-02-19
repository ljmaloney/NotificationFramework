/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.sdk.account.alert;

import java.rmi.RemoteException;
import java.util.List;

import com.xanboo.core.sdk.account.XanbooSubscription;
import com.xanboo.core.security.XanbooAdminPrincipal;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.XanbooException;

/**
 * EJB for the creating new alerts, sending push notifications of alerts, and clearing alerts from the system. 
 * <br/>
 * The "clearAlert" and "getAlert" methods have two signatures. One signature with a <code>XanbooPrincipal</code> instance to
 * enable calls from the DigitalLife user interface with the user's login credentials. Another signature with a <code>XanbooAdminPrincipal</code>
 * instance for calls from the BSS API. 
 * <br/>
 * @author Luther J Maloney
 * @since March 2015
 * 
 */

public interface AccountAlertManager   
{
    /**
     * Called by the DigitalLife UI to clear an alert specified by the <code>XanbooAlert</code> parameter. 
     * <br/>
     * <br/>
     * To clear the alert, the <code>XanbooSubscription</code> instance must have at least one of the following:
     * <ul>
     *  <li>accountId - the Xanboo/DigitalLife internal account Id
     *  <li>extAccountId - the external account id (billing account number / BAN)
     *  <li>subsId - the subscription id (CTN)
     * </ul>
     * If more than one of accountId, extAccountId, and subsId is provided, the order of precedence is:<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;accountId, extAccountId, then subsId. 
     * <br/>
     * <br/>
     * The <code>XanbooAlert</code> instance must have the following parameters set:
     * <ul>
     *  <li>alertSource - the source system of the alert
     *  <li>alertCode - the code associated with the alert 
     * <ul>
     * <em>Note:</em>This method is intended to be called by the Digital Life UI, and does not contain any functionality 
     * for sending push notifications to the user.
     * @param xCaller - The authentication credentials for the Digital Life user
     * @param subscription - An instance of <code>XanbooSubscription</code> which specifies the account for the alert
     * @param alert - An instance of <code>XanbooAlert</code> to specify the alert to clear.
     * @return The alertId of the alert cleared by the system. 
     * @throws RemoteException
     * @throws XanbooException 
     */
    public Long clearAlert(XanbooPrincipal xCaller,XanbooSubscription subscription,XanbooAlert alert)throws RemoteException, XanbooException;
    /**
     * Called by the DigitalLife UI to clear an alert specified by the <code>XanbooAlert</code> parameter. 
     * <br/>
     * <br/>
     * To clear the alert, the <code>XanbooSubscription</code> instance must have at least one of the following:
     * <ul>
     *  <li>accountId - the Xanboo/DigitalLife internal account Id
     *  <li>extAccountId - the external account id (billing account number / BAN)
     *  <li>subsId - the subscription id (CTN)
     * </ul>
     * If more than one of accountId, extAccountId, and subsId is provided, the order of precedence is:<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;accountId, extAccountId, then subsId. 
     * <br/>
     * <br/>
     * The <code>XanbooAlert</code> instance must have the following parameters set:
     * <ul>
     *  <li>alertSource - the source system of the alert
     *  <li>alertCode - the code associated with the alert 
     * <ul>
     * <em>Note:</em> This method is called by external systems via the BSS API. THis method contains logic to send a push notification
     * to the user. 
     * <br/>
     * <br/>
     * To send a push notification to the user with an "alert clear" message, the following parameters must 
     * be provided in the <code>XanbooAlert</code> instance:
     * <ul>
     *  <li>messageType - This parameter must be set to "push" or "pushAndAlert". If this parameter is null or "alert", no push notification is sent.
     *  <li>alertText - This is the text of the push notification message. This field is required when messageType is populated as above. 
     * </ul>
     * @param xCaller - System login credentials.
     * @param subscription - An instance of <code>XanbooSubscription</code> which specifies the account for the alert
     * @param alert - An instance of <code>XanbooAlert</code> to specify the alert to clear.
     * @return The alertId of the alert cleared by the system. 
     * @throws RemoteException
     * @throws XanbooException 
     */
    public Long clearAlert(XanbooAdminPrincipal xCaller,XanbooSubscription subscription,XanbooAlert alert)throws RemoteException, XanbooException;
    /**
     * Retrieves the an alert or alerts from the database for the account specified by 
     * the <code>XanbooSubscription</code> parameter. Only active alerts are returned, active is defined as
     * an alert that is either unexpired or not cleared. 
     * <br/>
     * <br/>
     * To query for alerts, the <code>XanbooSubscription</code> instance must have at least one of the following:
     * <ul>
     *  <li>accountId - the Xanboo/DigitalLife internal account Id
     *  <li>extAccountId - the external account id (billing account number / BAN)
     *  <li>subsId - the subscription id (CTN)
     * </ul>
     * If more than one of accountId, extAccountId, and subsId is provided, the order of precedence is:<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;accountId, extAccountId, then subsId. 
     * <br/>
     * @param xCaller - the Xanboo/DigitalLife user's login credentials
     * @param subscription - Specifies the account information for the alert(s)
     * @param alertId - optional parameter identifying a specific alert to return
     * @return - A List of <code>XanbooAlert</code> instances containing the alerts
     * @throws RemoteException
     * @throws XanbooException 
     */
    public List<XanbooAlert> getAlerts(XanbooPrincipal xCaller,XanbooSubscription subscription,Long alertId) throws RemoteException, XanbooException;
   /**
     * Retrieves the an alert or alerts from the database for the account specified by 
     * the <code>XanbooSubscription</code> parameter. Only active alerts are returned, active is defined as
     * an alert that is either unexpired or not cleared. 
     * <br/>
     * <br/>
     * To query for alerts, the <code>XanbooSubscription</code> instance must have at least one of the following:
     * <ul>
     *  <li>accountId - the Xanboo/DigitalLife internal account Id
     *  <li>extAccountId - the external account id (billing account number / BAN)
     *  <li>subsId - the subscription id (CTN)
     * </ul>
     * If more than one of accountId, extAccountId, and subsId is provided, the order of precedence is:<br/>
     * &nbsp;&nbsp;&nbsp;&nbsp;accountId, extAccountId, then subsId. 
     * <br/>
     * @param xCaller - System login credentials
     * @param subscription - Specifies the account information for the alert(s)
     * @param alertId - optional parameter identifying a specific alert to return
     * @return - A List of <code>XanbooAlert</code> instances containing the alerts
     * @throws RemoteException
     * @throws XanbooException 
     */
    public List<XanbooAlert> getAlerts(XanbooAdminPrincipal xCaller,XanbooSubscription subscription,Long alertId)throws RemoteException, XanbooException;
    /**
     * Method to create a new alert and optionally send a push notification containing the alertText in the <code>XanbooAlert</code> instance. 
     * <br/><br/>
     * <ul>
     *  <li>Handling for messageType:<br/>
     *      <ul>
     *          <li>push - a push notification is sent and no alert is created
     *          <li>alert - an alert is created without sending a push notification
     *          <li>pushAndAlert - a alert is created and a push notification is sent
     *      </ul>
     *  <li>Handling for alertBehavior
     *      <ul>
     *          <li>clearOnView - Instructs the UI to clear the alert when the user views the alert
     *          <li>holdOnView  - The source system creating the alert clears the alert. The UI does not clear the alert. 
     *      </ul>
     * </ul>
     * To create an alerts, the <code>XanbooSubscription</code> instance must have at least one of the following:
     * <ul>
     *  <li>accountId - the Xanboo/DigitalLife internal account Id
     *  <li>extAccountId - the external account id (billing account number / BAN)
     *  <li>subsId - the subscription id (CTN)
     * </ul>
     * The <code>XanbooAlert</code> instance must have the following parameters set:
     * <ul>
     *  <li>domain - Identifies the digital life domain
     *  <li>alertSource - the source system of the alert
     *  <li>alertCode - the code associated with the alert 
     *  <li>messageType - one of push, alert, or pushAndAlert, specifies the handling for the alert
     *  <li>alertText - The text of the alert/notification. Limited to 128 characters
     * <ul>
     * Optional parameters in <code>XanbooAlert</code>:
     * <ul>
     *  <li>alertBehavior - one of either clearOnView or holdOnView. If not provided, the value is defaulted to clearOnView
     *  <li>expireDate - the date and time (in GMT) when the alert expires
     *  <li>alertInfo - A <code>HashMap</code> containing name=value pairs which specifies additional information for the alert.
     * <ul>
     * @param xCaller - The credentials of the calling application, this method should only be called from the BSS api. 
     * @param subscription - Identifies the account associated with the alert/notification
     * @param alert - Contains the data for the alert/notification
     * @return - The alertId of the newly created alert. 
     * @throws RemoteException
     * @throws XanbooException 
     */
    public Long newAlert(XanbooAdminPrincipal xCaller,XanbooSubscription subscription,XanbooAlert alert)throws RemoteException, XanbooException;
}
