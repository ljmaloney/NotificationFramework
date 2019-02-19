/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.notification.provider;


import java.util.HashMap;
import java.io.IOException;
import com.xanboo.core.util.LoggerFactory;
import com.att.dlife.httplib.http.XanbooHttpResponse;
import com.xanboo.core.notification.NotificationDestination;
import com.xanboo.core.notification.NotificationMessageInterface;
import com.xanboo.core.notification.XanbooNotificationException;
import com.xanboo.core.notification.XanbooNotificationMessage;
import com.xanboo.core.util.Logger;
/**
 * Implementation of <code>XanbooNotificationProviderInterface</code> to encapsulate specifics for
 * the Soundbite implementation.
 * 
 * The Soundbite API consists of parameters sent via HTTP request to a given URL. The parameters are <br/>
 * <li>username - user name to identify client to Soundbite
 * <li>password - the password for the username
 * <li>shortcode - the shortcode specific to the service provider (the client to Soundbite)
 * <li>phone_number - the SMS phone number destination for the message
 * <li>message - the message text
 * <li>message_id - a unique message identifier
 * <li>carrier - the carrier code 
 * 
 * @author Luther J Maloney
 * @since October 2013
 */
public class SoundbiteNotificationProvider extends XanbooHttpNotificationProvider
{
    /**
    *	username: digitallife_intl
    password: gsg092LL
    shortcode: 99222
    phone_number
    message
    message_id
    carrier : 99901 In the US you can use carrier code 99901 if you don't know the US carrier.
    */
    private String userName = null;
    private transient String passwd = null;
    private String defaultCarrierCode = null;
    private String shortCode = null;
    private HashMap<Integer,String> statusCodeMap = new HashMap<Integer,String>();
    Logger logger = LoggerFactory.getLogger(getClass().getName());

    public SoundbiteNotificationProvider()
    {
        statusCodeMap.put(0,"Message has been accepted for processing.");
        statusCodeMap.put(1,"One or more request parameters missing or invalid.");
        statusCodeMap.put(2,"Authentication failed.");
        statusCodeMap.put(3,"Unrecognized carrier ID.");
        statusCodeMap.put(4,"Invalid shortcode. Shortcodes must be purely numeric.");
        statusCodeMap.put(5,"Shortcode not approved.");		
        statusCodeMap.put(6,"Throttled.");
        statusCodeMap.put(7,"Duplicate message ID.");
        statusCodeMap.put(98,"Internal server error.");
        statusCodeMap.put(99,"Other/Unknown error.");
    }
    /**
    * Custom init for SoundBite. Extract initialization parameters from the <code>HashMap</code> config parameters
    * and store in instance variables. 
    * @param config 
    */
    @Override
    public void initialize(HashMap config)
    {
        //call the super class
        super.initialize(config);
        //extract parameters from the hashmap and store in instance variables
        this.userName = (String) config.get("username");
        this.passwd = (String)config.get("password");
        //default carrier code (if not set)
        this.defaultCarrierCode = config.containsKey("carrier") ? (String)config.get("carrier") : "99901";
        //default short code
        this.shortCode = config.containsKey("shortcode") ? (String)config.get("shortcode") : "99222";
        logger.info("[initialize()] Provider initialized with [username="+userName+",password="+passwd+",carrier="+defaultCarrierCode+",shortCode="+shortCode+"]");
    }
    @Override
    public boolean encodeParametersBeforeTemplate()
    {
        return false;
    }
    /**
    * Sends a message using the Soundbite provider / url. 
    * @param destination
    * @param message 
    */
    @Override
    public void sendMessage(NotificationDestination destination,NotificationMessageInterface message) throws XanbooNotificationException
    {
        if ( logger.isDebugEnabled() )
        {
            logger.debug("[sendMessage()] sendMessage called, nd="+destination.toString());
            logger.debug("[sendMessage()] message params = "+message.toString());
        }
        //create message query string
        HashMap queryMap = new HashMap();
        //destination & message
        queryMap.put("phone_number", destination.getDestinationAddress());
        queryMap.put("message", message.getMessage().trim());
        queryMap.put("message_id", createMessageId(destination,message));
        //soundbite api parameters
        queryMap.put("login",userName);
        queryMap.put("password",passwd);
        queryMap.put("shortcode",shortCode);
        queryMap.put("carrier",defaultCarrierCode);

        String queryStr = super.generateQueryString(queryMap);
        
        if ( logger.isDebugEnabled())
            logger.debug("[sendMessage()] Query String = "+queryStr);

        //XanbooHttpConnector xanbooHttpConnector = new XanbooHttpConnector();
        //XanbooHttpResponse resp = null;
        try 
        {
            XanbooHttpResponse response = super.sendGetRequest(queryStr);

            if(response.isSuccess()) 
            {
                int code = Integer.parseInt(response.getContent().substring(response.getContent().indexOf("=")+1));
                if ( logger.isDebugEnabled() )
                    logger.debug("[sendMessage()] - Soundbite response : "+response + " - "+statusCodeMap.get(code));
                //if the response code from Soundbite is not "ok"
                if ( code != 0 )
                {
                    logger.warn("Error \""+statusCodeMap.get(code)+"\" sending notification to "+destination.getDestinationAddress()+" via Soundbite");
                    logger.warn("URL="+getUrl()+" queryStr="+queryStr);
                    logNotificationError(destination, message, queryStr, new Exception("Recieved status code: "+statusCodeMap.get(code)+" from Sountbite"));
                }
                else
                {
                    logger.info("[sendMessage()]: SoundBite notification sent to:" + destination.getDestinationAddress());
                }
            }
            else 
            {
                logger.warn("[sendMessage()] - Error = "+response.getResponseCode());
                throw new XanbooNotificationException("Error sending HTTP notification, errorcode="+response.getResponseCode());
            }

        }
        catch(IOException ioe)
        {
            logger.warn("IOException sending notification",ioe);
            logNotificationError(destination, message, "IOException sending notification using soundbite. QueryStr="+queryStr, ioe);
            throw new XanbooNotificationException("IOException sending notification using soundbite. QueryStr="+queryStr, ioe);
        }
        catch(Exception e) 
        {
            logNotificationError(destination, message, "Exception sending notification", e);
            logger.error("Exception sending notification using soundbite", e);
            throw new XanbooNotificationException("Exception sending notification using soundbite", e);
        }
    }
    /**
    * Helper method to create unique message id. 
    * @param destination
    * @param message
    * @return 
    */
    private String createMessageId(NotificationDestination destination,NotificationMessageInterface message)
    {
        return System.currentTimeMillis()+"";
    }

    public static void main(String args[])
    {
        try
        {
            System.out.println("testing soundbite");
            SoundbiteNotificationProvider provider = new SoundbiteNotificationProvider();
            HashMap map = new HashMap();
            map.put("username","digitallife_intl");
            map.put("password","gsg092LL");
            map.put("shortcode","99222");
            map.put("carrier","99901");
            map.put("HttpUrl","https://smsc-api.soundbite.com/httpapi/receiver");
            provider.initialize(map);
            //destination
            NotificationDestination nd = new NotificationDestination("111%14042341417");
            //message
            XanbooNotificationMessage message = new XanbooNotificationMessage();
            message.getToAddrList().add(nd);
            message.setSubject("test");
            message.setMessage("This is a test of soundbite api impl POC");
            provider.sendMessage(nd, message);
        }
        catch (XanbooNotificationException ex)
        {
            ex.printStackTrace();
        }
    }
}
