/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.notification.provider;


import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import com.xanboo.core.util.LoggerFactory;
import com.xanboo.core.util.Logger;
import com.xanboo.core.util.SimpleACSIClient;
import com.xanboo.core.notification.AbstractSOAPNotificationProvider;
import com.xanboo.core.notification.NotificationDestination;
import com.xanboo.core.notification.NotificationMessageInterface;
import com.xanboo.core.notification.XanbooMessageMap;
import com.xanboo.core.notification.XanbooNotificationException;
/**
* Provider for ATT destinations / accounts.
* 
* @author Luther Maloney
* @since October 2013
*/
public class AcsiNotificationProvider extends AbstractSOAPNotificationProvider
{
    Logger logger = LoggerFactory.getLogger(getClass().getName());
    /**
    * Constructor
    */
    public AcsiNotificationProvider()
    {

    }
    /**
    * Initialize the instance of AcsiNotificationProvider
    * @param configMap 
    */
    @Override
    public void initialize(HashMap configMap)
    {
        super.initialize(configMap);
        logger.info("[initialize()] - Initializing ACSI notification provider");
    }
    /**
    * The ACSI client can accomidate a list/array of destinations.
    * @param destinations
    * @param message 
    */
    @Override
    public void sendMessage(List<NotificationDestination> destinations,NotificationMessageInterface message) throws XanbooNotificationException
    {
        long startTime = System.currentTimeMillis();
        String[] destArray = new String[destinations.size()];
        StringBuffer dest = new StringBuffer();
        for ( int i = 0; i < destinations.size(); i ++ )
        {
            destArray[i] = destinations.get(i).getDestinationAddress();
            if ( i > 0 )
                dest.append(";");
            dest.append(destArray[i]);
        }
        if ( logger.isDebugEnabled() )
            logger.debug("[sendMessage] - Sending to "+dest+" message : " + message.toString());
        SimpleACSIClient acsi = new SimpleACSIClient();

        String unsubscribeURL = null;
        String optInOutURL = (String) message.getMessageProperties().get(XanbooMessageMap.OPTINOUT_URL);
        String token       = (String) message.getMessageProperties().get(XanbooMessageMap.OPTINOUT_TOKEN);
        if (optInOutURL != null && token != null) {
            unsubscribeURL = optInOutURL + "?status_id=0&token=" + token;
        }
        
        int rc = acsi.send(message.getExternalAccountId(), message.getMessageProperties().getGatewayUID(), 
                           message.getSubscriberId(), message.getSubscriberName(),destArray, 
                           (String)message.getMessageProperties().get(XanbooMessageMap.TIMESTAMP), 
                           message.getSubject(), message.getMessage(), message.getAlarmCode(), unsubscribeURL,
                           (String)message.getMessageProperties().get(XanbooMessageMap.ID));
        if(rc==0) 
        {
            logger.info("[sendMessage()]: ACSI NOTIFICATION SENT to:" + dest + ", extAcc:" + message.getExternalAccountId() + ", subid:" + message.getSubscriberId());
            if(logger.isDebugEnabled()) 
            {
                logger.info("[sendMessage()]:     " + message.getSubject() + "\n" + message.getMessage());
            }
        }
        else 
        {
            logger.warn("[sendMessage()]: ACSI NOTIFICATION FAILED to:" + dest + ", extAcc:" + message.getExternalAccountId() + ", subid:" + message.getSubscriberId() + ", RC:" + rc);
            if(logger.isDebugEnabled()) 
            {
                logger.warn("[sendMessage()]:     " + message.getSubject() + "\n" + message.getMessage());
            }
            throw new XanbooNotificationException("ACSI Notification Failed : destination="+dest+", subid:" + message.getSubscriberId() + ", RC:" + rc);
        }
        long stopTime = System.currentTimeMillis();
        if ( logger.isDebugEnabled())
            logger.debug("[sendMessage()] - Elapsed time to send notification using ACSI client is "+(stopTime-startTime)+" milliseconds");
    }

    @Override
    public void sendMessage(NotificationDestination destination, NotificationMessageInterface message)throws XanbooNotificationException
    {
        List<NotificationDestination> destList = new ArrayList<NotificationDestination>();
        destList.add(destination);
        sendMessage(destList,message);
    }
}
