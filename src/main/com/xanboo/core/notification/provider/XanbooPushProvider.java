/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.notification.provider;

import java.util.HashMap;

import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;

import com.xanboo.core.notification.AbstractJsonNotificationProvider;
import com.xanboo.core.notification.NotificationDestination;
import com.xanboo.core.notification.NotificationMessageInterface;
import com.xanboo.core.notification.XanbooMessageMap;
import com.xanboo.core.util.XanbooException;
import com.xanboo.core.util.XanbooUtil;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * Notification provider to send push messages via Android or Apple. Sends JSON formatted data
 * to the push engine. 
 * 
 * @author lm899p
 */
public class XanbooPushProvider extends AbstractJsonNotificationProvider
{
    Logger log = null;
    String silent = null;
    
    public XanbooPushProvider()
    {
        log = LoggerFactory.getLogger(getClass().getName());
    }
    
    /**
    * Initialization routines for this class.
    * @param config 
    */
    @Override
    public void initialize(HashMap config)
    {
        super.initialize(config);
        log.debug("[initialize()] - initializing instance of XanbooPushProvider");
        silent = (String)config.get("silent.notification");
    }
    
    @Override
    public String createJSON(NotificationDestination destination, NotificationMessageInterface message)
    {
        if ( log.isDebugEnabled() ) {
            log.debug("[createJSON()] - create JSON formatted data for message="+message.toString());
        }
        
        String transId = "";
        String gguid = message.getMessageProperties().getGatewayUID();
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss-");
        transId = df.format(new Date())+XanbooUtil.generateTransactonId(gguid,10);
        StringBuilder jsonBldr = new StringBuilder();
        
        jsonBldr.append("{");
        //only send pnId when the destination is not null and not equal to accountId or gateway
        String dest = destination.getDestinationAddress();
        if ( dest != null )
        {
           if ( dest.equals(message.getAccountId().toString()) ||  dest.equals(message.getMessageProperties().getGatewayUID()))
                dest = null; 
        }
                
        if ( dest != null )
            jsonBldr.append("\"pnId\":\"").append(dest).append("\",");
        
        jsonBldr.append("\"accId\":").append(message.getAccountId()).append(",");
        jsonBldr.append("\"gguid\":\"").append(message.getMessageProperties().getGatewayUID()).append("\",");
        jsonBldr.append("\"msg\":\"").append(message.getMessage()).append("\",");
        
        if ( message.getMessageProperties().get(XanbooMessageMap.BUTTON_NAME) != null )
            jsonBldr.append("\"actionKey\":\"").append(message.getMessageProperties().get(XanbooMessageMap.BUTTON_NAME)).append("\",");
        
        if ( silent != null && !silent.equals("unused"))
            jsonBldr.append("\"silent.notification\":").append("\"").append(silent).append("\",");
        else
            jsonBldr.append("\"silent.notification\":").append("\"").append(message.getSilentNotification()).append("\",");
        
        jsonBldr.append("\"dguid\":\"").append(message.getMessageProperties().get(XanbooMessageMap.DGUID)).append("\",");
        jsonBldr.append("\"catalogId\":\"").append(message.getMessageProperties().get(XanbooMessageMap.CATALOG_ID)).append("\",");
        jsonBldr.append("\"ntype\":\"").append(message.getMessageType().getType()).append("\",");
        
        switch(message.getMessageType())
        {
            case EVENT_MESSAGE :
                jsonBldr.append("\"eid\":").append(message.getMessageProperties().getEventId()).append(",");
                break;
            case OID_MESSAGE :
                jsonBldr.append("\"eid\":").append(message.getMessageProperties().getEventId()-10000).append(",");
                jsonBldr.append("\"value\":\"").append(message.getMessageProperties().get(XanbooMessageMap.OBJECT_VALUE)).append("\",");
                break;
            case DEVICE_ACTION : 
                jsonBldr.append("\"eid\":\"").append(((XanbooMessageMap.DeviceActionType)message.getMessageProperties().get(XanbooMessageMap.DEVICE_ACTION_TYPE)).getType()).append("\",");
                break;
            default :
                throw new RuntimeException("Invalid message type, provider only recognizes event, oid, and device");
        }
                
        jsonBldr.append("\"transactionId\":\"").append(transId).append("\"");
        //custom fields
        StringBuilder custBldr = new StringBuilder();
        custBldr.append("\"custom\": {");
        if ( message.getMessageProperties().containsKey(XanbooMessageMap.DOORBELL_CAMERA_URL) )
            custBldr.append("\"cameraUrls\":"+message.getMessageProperties().getCameraUrlsJson());
        String customFldJSON = message.getCustomMapJSON();
        if ( customFldJSON != null && !customFldJSON.equalsIgnoreCase("") )
        {
            if ( message.getMessageProperties().containsKey(XanbooMessageMap.DOORBELL_CAMERA_URL) ) custBldr.append(", ");
            custBldr.append(customFldJSON);
        }
        custBldr.append("}");
        if ( !custBldr.toString().endsWith("{}"))
        {
            jsonBldr.append(", ");
            jsonBldr.append(custBldr.toString());
        }
        
        //if ( message.getMessageProperties().containsKey(XanbooMessageMap.BUTTON_LINK) || message.getMessageProperties().containsKey(XanbooMessageMap.DOORBELL_CAMERA_URL))
        //{
        //    //jsonBldr.append("\"custom\": {\"key\": \"_value\",\"lastname\": \"_lastname\",\"url\":\"\"}");
        //    jsonBldr.append(", \"custom\" : {" );
        //    if ( message.getMessageProperties().containsKey(XanbooMessageMap.BUTTON_LINK))
        //        jsonBldr.append("\"url\": \"").append(message.getMessageProperties().get(XanbooMessageMap.BUTTON_LINK)).append("\"");
        //    if ( message.getMessageProperties().containsKey(XanbooMessageMap.DOORBELL_CAMERA_URL))
        //    {
        //        if ( message.getMessageProperties().containsKey(XanbooMessageMap.BUTTON_LINK))
        //            jsonBldr.append(",");
         //       jsonBldr.append("\"cameraUrls\":"+message.getMessageProperties().getCameraUrlsJson());
         //   }
         //   jsonBldr.append("}");
        //}
        jsonBldr.append("}");
               
        return jsonBldr.toString();
    }

    @Override
    public void processResponse(Integer responseCode, Boolean success,String content,NotificationMessageInterface message)
    {
        if ( log.isDebugEnabled() )
            log.debug("[processResponse()] - responseCode="+responseCode+", success="+success+", content="+content);
        
        message.getResponseMap().setResponseCode(responseCode);
        if ( content == null  || content.equalsIgnoreCase("") || content.equalsIgnoreCase("{}"))
        {
            message.getResponseMap().setDestinations("");
            message.getResponseMap().setSuccess(Boolean.FALSE);
        }
        else
        {
            message.getResponseMap().setSuccess(Boolean.TRUE);
        }        
    }
}
