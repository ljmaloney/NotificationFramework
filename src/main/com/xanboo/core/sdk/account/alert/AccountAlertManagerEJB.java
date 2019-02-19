/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.sdk.account.alert;

import com.xanboo.core.model.device.XanbooDevice;
import com.xanboo.core.notification.NotificationDestination;
import com.xanboo.core.notification.XanbooCustomMap;
import com.xanboo.core.notification.XanbooMessageMap;
import com.xanboo.core.notification.XanbooNotificationException;
import com.xanboo.core.notification.XanbooNotificationFactory;
import com.xanboo.core.notification.XanbooNotificationMessage;
import java.sql.Connection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.CreateException;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import com.xanboo.core.sdk.account.XanbooSubscription;
import com.xanboo.core.security.XanbooAdminPrincipal;
import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.template.DomainTemplateCache;
import com.xanboo.core.util.EServicePusher;
import com.xanboo.core.util.GlobalNames;
import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;
import com.xanboo.core.util.PushMessageQueue;
import com.xanboo.core.util.SimpleACSIClient;
import com.xanboo.core.util.XanbooException;
import com.xanboo.core.util.XanbooUtil;
import java.util.Date;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * EJB to manage account alert information. 
 * 
 * @author lm899p
 */
@Stateless (name="AccountAlertManager")
@TransactionManagement( TransactionManagementType.BEAN )
@Remote(AccountAlertManager.class)
public class AccountAlertManagerEJB 
{
    private Logger logger = null;
    // related DAO class
   private AccountAlertDAO dao=null;
   
   private static ExecutorService execService;  
   
   private HashMap<String,Integer> createAlertTypeEID = new HashMap<String,Integer>();
   private HashMap<String,Integer> cancelAlertTypeEID = new HashMap<String,Integer>();
   private static final int TEMPLATE_PUSH_NOTIFICATION     = 8;
   
 @PostConstruct
    public void init() throws CreateException 
    {
        execService = Executors.newCachedThreadPool();
        try 
        {
            // create a logger instance
            logger=LoggerFactory.getLogger(this.getClass().getName());
            if(logger.isDebugEnabled()) 
                logger.debug("[ejbCreate()]:");
            
            dao = new AccountAlertDAO(); 
            
            logger.info("[init()] - ceateAlert event id list : "+GlobalNames.ACCTALERT_CREATE_EVENT);
            String[] createEvents = GlobalNames.ACCTALERT_CREATE_EVENT.split(",");
            for ( String createEvt : createEvents )
            {
                String alrtCd = createEvt.substring(0,createEvt.indexOf(":"));
                String eventId = createEvt.substring(createEvt.indexOf(":")+1);
                createAlertTypeEID.put(alrtCd,new Integer(eventId));
            }
            
            logger.info("[init()] - cancel Alert evdnt id list : "+GlobalNames.ACCTALERT_CLEAR_EVENT);
            String[] cancelEvents = GlobalNames.ACCTALERT_CLEAR_EVENT.split(",");
            for ( String cancelEvent : cancelEvents )
            {
                String alrtCd = cancelEvent.substring(0,cancelEvent.indexOf(":"));
                String eventId = cancelEvent.substring((cancelEvent.indexOf(":")+1));
                cancelAlertTypeEID.put(alrtCd,new Integer(eventId));
            }
                                                                                                                                              
        }
        catch (Exception se) 
        {
            createAlertTypeEID.put("permit",510);
            cancelAlertTypeEID.put("permit",511); 
            //throw new CreateException("Failed to create AccountAlertManager:" + se.getMessage());
        }
        
    }
    
  
    /**
     * Clears an alert for the authenticated user. 
     * @param xCaller - The authenticated user for whom the alert is being cleared
     * @param subscription - the user's subscription info
     * @param alert - the alert to be cleared
     * @return
     * @throws XanbooException 
     */
    public Long clearAlert(XanbooPrincipal xCaller,XanbooSubscription subscription,XanbooAlert alert)throws XanbooException
    {
        if (logger.isDebugEnabled()) 
            logger.debug("[clearAlert()] - clearing "+alert);
        
        // now check the validity of the user
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        // first validate the input parameters
        if ( subscription.getSubsId() == null || subscription.getSubsId().equalsIgnoreCase(""))
        {
            throw new XanbooException(34001,"Subscription Id is required");
        }
        if ( subscription.getAccountId() == -1l && (subscription.getExtAccountId() == null || subscription.getExtAccountId().equalsIgnoreCase("")) )
        {
            throw new XanbooException(34004,"Neither Account Id or External AccountId (BAN) were provided");
        }
        
        if ( xCaller.getAccountId() != subscription.getAccountId() )
        {
            logger.warn("[clearAlert()] - requested account="+subscription.getAccountId()+" does not match authenticated account "+xCaller.getAccountId());
            throw new XanbooException(34002,"Requested account does not match authenticated account");
        }
        
        if ( alert.getDomainId() == null || alert.getDomainId().equalsIgnoreCase(""))
            alert.setDomainId(xCaller.getDomain());
        
        if ( !alert.getDomainId().equals(xCaller.getDomain()))
        {
            logger.warn("[clearAlert()] - domain for the credentials does not match the alert");
            throw new XanbooException(34003,"Alert domain does not match XanbooPrincipal domain");
        }
        
        alert.setEventId(cancelAlertTypeEID.get(alert.getAlertCode()));
        
        Connection conn = null;
        boolean rollback=false;
        Long alertId = -1l;
        try 
        {            
            conn = dao.getConnection();
            alertId = dao.clearAlert(conn, subscription, alert);
        }
        catch (XanbooException xe) 
        {
           if ( logger.isDebugEnabled()) 
               logger.debug("[clearAlert()] - ",xe);
           if ( xe.getCode() == 34005 )
               throw xe;
           throw new XanbooException(34000,"Unexpected error : "+xe.toString());
        }
        catch (Exception e) 
        {
            logger.warn("[clearAlert()] - an unexpected error occured",e);
            throw new XanbooException(34000,"Unexpected error : "+e.toString());
        }
        finally 
        {
            dao.closeConnection(conn, rollback);
        }
        return alertId;
    }
    /**
     * Clears an alert for an administrative user
     * @param xCaller
     * @param subscription
     * @param alert
     * @return
     * @throws XanbooException 
     */
    public Long clearAlert(XanbooAdminPrincipal xCaller,XanbooSubscription subscription,XanbooAlert alert)throws XanbooException
    {
        if (logger.isDebugEnabled()) 
            logger.debug("[getAlerts()] - accountId="+subscription.getAccountId()+", extAcctId="+subscription.getExtAccountId()+", subsId="+subscription.getSubsId());
                
        // first validate the input parameters
        if ( subscription.getSubsId() == null || subscription.getSubsId().equalsIgnoreCase(""))
        {
            throw new XanbooException(34001,"Subscription Id is required");
        }
        if ( subscription.getAccountId() == -1l && (subscription.getExtAccountId() == null || subscription.getExtAccountId().equalsIgnoreCase("")) )
        {
            throw new XanbooException(34004,"Neither Account Id or External AccountId (BAN) were provided");
        }
             
        if ( alert.getDomainId() == null || alert.getDomainId().equalsIgnoreCase(""))
            alert.setDomainId(xCaller.getDomain());
        
        if ( !alert.getDomainId().equals(xCaller.getDomain()))
        {
            logger.warn("[clearAlert()] - domain for the credentials does not match the alert");
            throw new XanbooException(34003,"Alert domain does not match XanbooPrincipal domain");
        }
        
        if ( alert.getDomainId() == null || alert.getDomainId().equalsIgnoreCase(""))
            alert.setDomainId(xCaller.getDomain());
        
        if ( !alert.getDomainId().equals(xCaller.getDomain()))
        {
            logger.warn("[clearAlert()] - domain for the credentials does not match the alert");
            throw new XanbooException(34003,"Alert domain does not match XanbooPrincipal domain");
        }
        
        alert.setEventId(cancelAlertTypeEID.get(alert.getAlertCode()));
        
        Connection conn = null;
        boolean rollback=false;
        Long alertId = -1l;
        try 
        {            
            conn = dao.getConnection();
            alertId = dao.clearAlert(conn, subscription, alert);
            
        }
        catch (XanbooException xe) 
        {
           if ( logger.isDebugEnabled()) 
               logger.debug("[clearAlert()] - ",xe);
           if ( xe.getCode() == 34005 )
               throw xe;
           throw new XanbooException(34000,"Unexpected error : "+xe.toString());
        }
        catch (Exception e) 
        {
            logger.warn("[clearAlert()] - an unexpected error occured",e);
            throw new XanbooException(34000,"Unexpected error : "+e.toString());
        }
        finally 
        {
            dao.closeConnection(conn, rollback);
        }
        
        if ( alert.getMessageType() != null && alert.getMessageType() == XanbooAlert.MessageTypeType.PUSH_ONLY)
        {
            sendPushNotification(subscription,alert);
        }
        
        return alertId;
    }
    /**
     * Returns the active alerts for the requested user
     * @param xCaller - the authenticated user
     * @param subscription - account / subscription info
     * @param alertId - a specific alert to return
     * @return
     * @throws XanbooException 
     */
    public List<XanbooAlert> getAlerts(XanbooPrincipal xCaller,XanbooSubscription subscription,Long alertId) throws XanbooException
    {
        if (logger.isDebugEnabled()) 
            logger.debug("[getAlerts()] - accountId="+subscription.getAccountId()+", extAcctId="+subscription.getExtAccountId()+", subsId="+subscription.getSubsId());
        
        // now check the validity of the user
        XanbooUtil.checkCallerPrivilege(xCaller);
        
        // first validate the input parameters
        if ( subscription.getSubsId() == null || subscription.getSubsId().equalsIgnoreCase(""))
        {
            throw new XanbooException(34001,"Subscription Id is required");
        }
        if ( subscription.getAccountId() == -1l && (subscription.getExtAccountId() == null || subscription.getExtAccountId().equalsIgnoreCase("")) )
        {
            throw new XanbooException(34004,"Neither Account Id or External AccountId (BAN) were provided");
        }
        
        if ( xCaller.getAccountId() != subscription.getAccountId() )
        {
            logger.warn("[getAlerts()] - requested account="+subscription.getAccountId()+" does not match authenticated account "+xCaller.getAccountId());
            throw new XanbooException(34002,"Requested account does not match authenticated account");
        }

        
        Connection conn = null;
        boolean rollback=false;
        try 
        {            
            conn = dao.getConnection();
            return dao.getAlerts(conn,subscription,xCaller.getDomain(),alertId);
        }
        catch (XanbooException xe) 
        {
           if ( logger.isDebugEnabled()) 
               logger.debug("[getAlerts()] - ",xe);
           if ( xe.getCode() == 34005 )
               throw xe;
           throw new XanbooException(34000,"Unexpected error : "+xe.toString());
        }
        catch (Exception e) 
        {
            logger.warn("[getAlerts()] - an unexpected error occured",e);
            throw new XanbooException(34000,"Unexpected error : "+e.toString());
        }
        finally 
        {
            dao.closeConnection(conn, rollback);
        }
        //return new ArrayList<XanbooAlert>();
    }
    /**
     * Returns the active alerts for the requested account 
     * @param xCaller - the authenticated admin user
     * @param subscription - the account / subscription
     * @param alertId - a specific alert to return
     * @return
     * @throws XanbooException 
     */
    public List<XanbooAlert> getAlerts(XanbooAdminPrincipal xCaller,XanbooSubscription subscription,Long alertId)throws XanbooException
    {
        if (logger.isDebugEnabled()) 
            logger.debug("[getAlerts()] - accountId="+subscription.getAccountId()+", extAcctId="+subscription.getExtAccountId()+", subsId="+subscription.getSubsId());
        
       // first validate the input parameters
        if ( subscription.getSubsId() == null || subscription.getSubsId().equalsIgnoreCase(""))
        {
            throw new XanbooException(34001,"Subscription Id is required");
        }
        if ( subscription.getAccountId() == -1l && (subscription.getExtAccountId() == null || subscription.getExtAccountId().equalsIgnoreCase("")) )
        {
            throw new XanbooException(34004,"Neither Account Id or External AccountId (BAN) were provided");
        }
                
        Connection conn = null;
        boolean rollback=false;
        try 
        {            
            conn = dao.getConnection();
            return dao.getAlerts(conn,subscription,xCaller.getDomain(),alertId);
        }
        catch (XanbooException xe) 
        {
           if ( logger.isDebugEnabled()) 
               logger.debug("[getAlerts()] - ",xe);
           if ( xe.getCode() == 34005 )
               throw xe;
           throw new XanbooException(34000,"Unexpected error : "+xe.toString());
        }
        catch (Exception e) 
        {
            logger.warn("[getAlerts()] - an unexpected error occured",e);
            throw new XanbooException(34000,"Unexpected error : "+e.toString());
        }
        finally 
        {
            dao.closeConnection(conn, rollback);
        }
        //return new ArrayList<XanbooAlert>();
    }
    /**
     * Creates a new alert. 
     * @param xCaller - authenticated admin user
     * @param subscription - identifie the account / subscripton for the alert
     * @param alert - the alert data
     * @return
     * @throws XanbooException 
     */
    public Long newAlert(XanbooAdminPrincipal xCaller,XanbooSubscription subscription,XanbooAlert alert)throws XanbooException
    {
        if (logger.isDebugEnabled()) 
            logger.debug("[newAlert()] - attempt to create new alert "+alert);
                
        // first validate the input parameters
        if ( subscription.getSubsId() == null || subscription.getSubsId().equalsIgnoreCase(""))
        {
            throw new XanbooException(34001,"Subscription Id is required");
        }
        if ( subscription.getAccountId() == -1l && (subscription.getExtAccountId() == null || subscription.getExtAccountId().equalsIgnoreCase("")) )
        {
            throw new XanbooException(34004,"Neither Account Id or External AccountId (BAN) were provided");
        }
        
        if ( alert.getDomainId() == null || alert.getDomainId().equalsIgnoreCase(""))
            alert.setDomainId(xCaller.getDomain());
        
        if ( !alert.getDomainId().equals(xCaller.getDomain()))
        {
            logger.warn("[clearAlert()] - domain for the credentials does not match the alert");
            throw new XanbooException(34003,"Alert domain does not match XanbooPrincipal domain");
        }
        
        alert.setEventId(createAlertTypeEID.get(alert.getAlertCode()));
        if ( alert.getAlertEvent() == XanbooAlert.EventType.CLEAR_ALERT )
            alert.setEventId(cancelAlertTypeEID.get(alert.getAlertCode()));
        
        if ( alert.getCreateDate() == null )
            alert.setCreateDate(new Date());
        
        Connection conn = null;
        boolean rollback=false;
        XanbooAlert newAlert = null;
        try 
        {            
            conn = dao.getConnection();
            //only insert db record when message type is push and alert or alert only
            if ( alert.getMessageType() == XanbooAlert.MessageTypeType.PUSH_AND_ALERT || alert.getMessageType() == XanbooAlert.MessageTypeType.ALERT_ONLY)
            {
                newAlert = dao.newAlert(conn, subscription, alert);
                if ( subscription.getAccountId() <= 0 )
                    subscription.setAccountId(newAlert.getAccountId());
                
                alert = newAlert;
                
                if (logger.isDebugEnabled()) 
                    logger.debug("[newAlert()] - created new alert "+alert);
            }
            else 
            {
               if ( subscription.getAccountId() != -1l) //copy the account id from the subscription object
                   alert.setAccountId(subscription.getAccountId());
               else //if no account id in the subscription, get it from the db
                    dao.getAccountId(conn,subscription,alert);
         
            }           
        } 
        catch (XanbooException xe) 
        {
           if ( logger.isDebugEnabled()) 
               logger.debug("[newAlert()] - ",xe);
           if ( xe.getCode() == 34005 )
               throw xe;
           throw new XanbooException(34000,"Unexpected error : "+xe.toString());
        }
        catch (Exception e) 
        {
            logger.warn("[newAlert()] - an unexpected error occured",e);
            throw new XanbooException(34000,"Unexpected error : "+e.toString());
        }
        finally 
        {
            dao.closeConnection(conn, rollback);
        }
        
        if ( alert.getMessageType() == XanbooAlert.MessageTypeType.PUSH_ONLY || alert.getMessageType() == XanbooAlert.MessageTypeType.PUSH_AND_ALERT)
            sendPushNotification(subscription,alert);
        
        if ( newAlert != null ) return newAlert.getAlertId();
                
        return -1l;
    }
    
    private void sendPushNotification(XanbooSubscription subs,XanbooAlert alert)
    {
        if(GlobalNames.ESERVICE_URL !=null) 
        {
            try
            {
                EServicePusher.postSendNotification(subs.getAccountId(), subs.getGguid(), alert.getAlertText(), null);
            }
            catch(Exception e)
            {
                logger.warn("[sendPushNotification - failed sending message, message="+alert.getAlertText(),e);
            }
        }
        
        execService.submit(new PushAlertTask(subs,alert));
    }
    
    class PushAlertTask extends TimerTask
    {
        private XanbooSubscription subs = null;
        private XanbooAlert alert = null;
        private Logger log = null;
        
        public PushAlertTask(XanbooSubscription subs,XanbooAlert alert)
        {
            this.subs = subs;
            this.alert = alert;
            log = LoggerFactory.getLogger(getClass().getName());
            DomainTemplateCache.getInstance();
        }
        @Override
        public void run()
        {
            if ( log.isDebugEnabled() )
                logger.debug("[run()] - sending push notification for alert="+alert);
            
            try	
            {
                XanbooNotificationMessage message = new XanbooNotificationMessage();
                NotificationDestination destination = new NotificationDestination();
                XanbooMessageMap msgMap = message.getMessageProperties();
                message.addToAddress(destination);
            
                destination.setProfileTypeId(GlobalNames.PUSH_PROFILE_TYPE);
                //use account id for account level notifications
                destination.setDestinationAddress(subs.getAccountId()+"");  
            
                message.setAccountId(new Long(subs.getAccountId()).intValue());
                message.setTemplateTypeId(TEMPLATE_PUSH_NOTIFICATION);
                message.setExternalAccountId(subs.getExtAccountId());
                message.setLanguage("en");              //hard-coded for now
                message.setSubscriberName("");          //not used
            
                //configure the message map
                msgMap.put(XanbooMessageMap.ACCOUNTID, subs.getAccountId());
                msgMap.put(XanbooMessageMap.ID, alert.getEventId()+"");
                msgMap.put(XanbooMessageMap.EVENT, alert.getAlertText());
                msgMap.put(XanbooMessageMap.SOURCE, alert.getAlertSource());
                msgMap.put(XanbooMessageMap.GATEWAY, "Digital Life System Gateway");
                msgMap.put(XanbooMessageMap.DEVICE,  "");       
                msgMap.put(XanbooMessageMap.BRAND,   "");
                msgMap.put(XanbooMessageMap.DOMAINID, alert.getDomainId());
                msgMap.put(XanbooMessageMap.GGUID, "0");
                msgMap.put(XanbooMessageMap.DGUID, "0");
                msgMap.put(XanbooMessageMap.TIMESTAMP, alert.getCreateDate());
                msgMap.put(XanbooMessageMap.SRCGUID, "0");
                msgMap.put(XanbooMessageMap.SRC_GATEWAY_NAME, "");
                msgMap.put(XanbooMessageMap.SRC_DEVICE_NAME, "");
                msgMap.put(XanbooMessageMap.ACCESSKEY, "");        //should this be extAccID?  
                if ( alert.getAlertPushLabel() != null )
                    msgMap.put(XanbooMessageMap.BUTTON_NAME, alert.getAlertPushLabel());
                if ( alert.getAlertPushUrl() != null )
                    message.putCustomProperty(XanbooMessageMap.BUTTON_LINK, alert.getAlertPushUrl(), XanbooCustomMap.DataType.STRING);

                if ( XanbooUtil.isNotEmpty(alert.getResponseTemplateId()) )
                    message.putCustomProperty(XanbooCustomMap.RESPONSE_REQ, Boolean.TRUE, XanbooCustomMap.DataType.BOOLEAN);
                else
                    message.putCustomProperty(XanbooCustomMap.RESPONSE_REQ, Boolean.FALSE, XanbooCustomMap.DataType.BOOLEAN);
                
                log.debug("[PushAlertTask.run()] - XanbooNotificationMessage="+message);
                
                XanbooNotificationFactory notificationFactory = XanbooNotificationFactory.getInstance();
                notificationFactory.sendMessage(message);
                //handle any results processng
                if ( XanbooUtil.isNotEmpty(alert.getResponseTemplateId()) && GlobalNames.MODULE_CSI_ENABLED 
                    && alert.getDomainId()!=null && alert.getDomainId().equals("DL"))
                {
                    Boolean success = message.getResponseMap().getSuccess();
                    String devId = message.getResponseMap().getDestinations();
                    SimpleACSIClient csiClient = new SimpleACSIClient();
                    int retCode = csiClient.sendConfirmationNotification(subs.getExtAccountId(),alert.getCreateDate(), success, devId,alert.getResponseTemplateId());
                }
            }
            catch (XanbooNotificationException ex)
            {
                log.warn("[PushAlertTask.run()] - XanbooNotificationException sending alert notification message", ex);
            }
            catch(Exception ex)
            {
                log.warn("[PushAlertTask.run()] - Exception sending alert notification message", ex);
            }
        }
    }
}
