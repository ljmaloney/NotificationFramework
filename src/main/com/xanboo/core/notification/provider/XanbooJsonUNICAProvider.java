/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.notification.provider;


import java.util.HashMap;
import java.io.IOException;
import com.att.dlife.httplib.http.XanbooHttpResponse;
import com.xanboo.core.notification.AbstractJsonNotificationProvider;
import com.xanboo.core.notification.NotificationDestination;
import com.xanboo.core.notification.NotificationMessageInterface;
import com.xanboo.core.notification.XanbooNotificationException;
import com.xanboo.core.notification.XanbooNotificationMessage;
import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;

/**
 * Class implementing a sms provider for sending JSON messages using UNICA
 * 
 * @author lm899p
 */
public class XanbooJsonUNICAProvider extends AbstractJsonNotificationProvider
{
    Logger logger = LoggerFactory.getLogger(getClass().getName());
    private String shortCode = null;
    private String senderAlias = null;
    private String phoneContext = null;
    private String countryCode = null;
    private boolean usePlus = false;
    private boolean useCountryCode = false;
    private boolean stripSeparatorChar = false;
    private String separatorChar = " ";
    
    public XanbooJsonUNICAProvider()
    {
        
    }
    
    @Override
    public void initialize(HashMap config)
    {
        logger.info("[intialize()] ");
        super.initialize(config);
        //provider specific configs and defaults
        this.shortCode = config.containsKey("shortcode") ? (String)config.get("shortcode") : "224663";
        this.phoneContext = config.containsKey("phonecontext") ? (String)config.get("phonecontext") : "";
        this.countryCode = config.containsKey("countrycode") ? (String)config.get("countrycode") : phoneContext;
        this.senderAlias = config.containsKey("senderAlias") ? (String)config.get("senderAlias") : null;
        
        //prepend the "+" symbol to the phone number
        String usePStr = config.containsKey("useplusintl") ? (String)config.get("useplusintl") : "false";
        usePlus = new Boolean(usePStr);
        if ( !phoneContext.equalsIgnoreCase("") && usePlus && !phoneContext.startsWith("+"))
        {
            phoneContext = "+"+phoneContext;
        }
        
        String useCountryCdStr = config.containsKey("usecountrycd") ? (String)config.get("usecountrycd") : "false";
        useCountryCode = new Boolean(useCountryCdStr);
        
        String stripSepChar = config.containsKey("stripseparator") ? (String)config.get("stripseparator") : "false";
        stripSeparatorChar = new Boolean(stripSepChar);
        if ( stripSeparatorChar )
            separatorChar = config.containsKey("separatorchar") ? (String)config.get("separatorchar") : " ";
    }

    @Override
    public String createJSON(NotificationDestination destination, NotificationMessageInterface message)
    {
        if ( logger.isDebugEnabled())
            logger.debug("[createJSON] - destination="+destination.toString()+" message="+message.toString());
        String destPhone = destination.getDestinationAddress();
        
        StringBuilder str = new StringBuilder();
        str.append("{\n");
        if ( senderAlias == null || senderAlias.equalsIgnoreCase(""))
        {
            str.append("\"from\": \"tel:");
            str.append(shortCode);
            if ( !phoneContext.equalsIgnoreCase(""))
            {
                str.append(";phone-context=");
                str.append(phoneContext);
            }
        }
        else
        {
            str.append("\"from\": \"alias:");
            str.append(senderAlias);
        }
        str.append("\",\n");
        str.append("\"to\": \"tel:");
        if ( usePlus && !destPhone.startsWith("+"))
            str.append("+");
        if ( useCountryCode )
            str.append(countryCode);
        if ( stripSeparatorChar )
            str.append(destPhone.replace(separatorChar, ""));
        else
            str.append(destPhone);
        str.append("\",\n");
        str.append("\"message\": \"");
        str.append(message.getMessage());
        str.append("\"\n");
        str.append("}");
        if ( logger.isDebugEnabled())
        logger.debug("[createJSON] - JSON Message : \r\n"+str);
        return str.toString();
    }

    @Override
    public void processResponse(Integer responseCode, Boolean success,String content,NotificationMessageInterface message)
    {
        if ( logger.isDebugEnabled() )
            logger.debug("[processResponse()] - responseCode="+responseCode+", success="+success+", content="+content);
                
    }
}
