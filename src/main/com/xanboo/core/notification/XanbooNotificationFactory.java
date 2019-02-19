/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.notification;

import java.io.StringReader;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Date;
import java.util.Properties;
import com.xanboo.core.template.DomainTemplateCache;
import com.xanboo.core.template.XanbooDomainTemplate;
import com.xanboo.core.sdk.sysadmin.SysAdminManager;
import com.xanboo.core.util.EjbProxy;
import com.xanboo.core.util.GlobalNames;
import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;
import com.xanboo.core.util.XanbooUtil;
import java.util.Set;

/**
 * Class to handle the loading and initialization of the notification providers. The class also encapsulates logic
 * to send the message to the provider or providers. 
 * <br/>
 * <br/>
 * This class uses the PROFILETYPE_REF table to create instances of a <code>XanbooNotificationProviderInterface</code>. The concrete 
 * implementation is specified by the PROVIDER_CLASSNAME column of the table. The provider specific configuration properties are 
 * specified in the PROPERTIES column. 
 * <br/>
 * <br/>
 * To implement a new provider either implement the <code>XanbooNotificationProviderInterface</code> or extend one of the abstract 
 * classes that most closely matches the provider specification. 
 * <br/>
 * <br/>
 * When sending a notification, create an instance of <code>NotificationMessageInterface</code> or use the default implementation 
 * <code>XanbooNotificationMessage</code>. Once the notification message is created, call the sendMessage() method of this 
 * class with the <code>NotificationMessageInterface</code> as the argument. The sendMessage method will located the appropriate 
 * provider, apply the template, and use the provider to send the message. 
 * 
 * @author Luther Maloney
 * @since October 2013
 */
public class XanbooNotificationFactory
{
    private static XanbooNotificationFactory instance;
    private Logger log = null;
   
    /**
    * The list of providers
    */
    private List<XanbooNotificationProviderInterface> providers = new ArrayList<XanbooNotificationProviderInterface>();

    /**
    * Private constructor / singleton pattern
    */
    protected XanbooNotificationFactory()
    {
        log = LoggerFactory.getLogger(this.getClass().getName());
    }
    /**
    * Obtains a reference to the global instance of the factory
    * @return 
    */
    public static XanbooNotificationFactory getInstance() 
    {
        if ( instance == null )
        {
            instance = new XanbooNotificationFactory();
            instance.initialize();
        }
        return instance;
    }
    /**
    * Returns the provider specified by the providerTypeId
    * @param providerTypeId
    * @return 
    */
    public XanbooNotificationProviderInterface getProvider(Integer providerTypeId)
    {
        for ( XanbooNotificationProviderInterface provider : providers )
        {
            if ( provider.getProfileTypeId().intValue() == providerTypeId.intValue())
                return provider;
        }
        return null;
    }
    /**
    * Returns the provider based on the provider suffix
    * @param providerSuffix
    * @return 
    */
    public XanbooNotificationProviderInterface getProvider(String providerSuffix)
    {
        for ( XanbooNotificationProviderInterface provider : providers )
        {
            if ( provider.getProfileSuffix().equals(providerSuffix))
                return provider;
        }
        return null;
    }
    /**
    * Initializes the factory
    */
    public void initialize() 
    {
        loadProviders();
    }
    protected SysAdminManager getSysAdminManager()
    {
        log.info("[getSysAdminManager()] - ");
        EjbProxy proxy =  new EjbProxy(GlobalNames.SDK_JNDI_CONTEXT_FACTORY, GlobalNames.SDK_JNDI_PROVIDER_URL); // vp889x - for remote invocation 
        SysAdminManager sManager = null;
        try 
        {
            if ( log.isDebugEnabled() )
                log.debug("[getSysAdminManager()] - Attempt to retrieve reference to SystemManager EJB");
            sManager = (SysAdminManager) proxy.getObj(GlobalNames.EJB_SYSADMIN_MANAGER);
        }
        catch(Exception e) 
        {
            if ( log.isDebugEnabled() )
                log.error("[getSysAdminManager()]: Exception " + e.getMessage(),e);
            else
                log.error("[getSysAdminManager()]: Exception " + e.getMessage());
        }
        return sManager;
    }
    
    protected DomainTemplateCache getTemplateCache()
    {
        DomainTemplateCache templateCache = DomainTemplateCache.getInstance();
        return templateCache;
    }
    /**
    * Method to create instances of <code>XanbooNotificationProviderInterface</code> from the PROFILETYPE_REF table. The 
    * concrete instance is specified by the PROVIDER_CLASSNAME column of the table. 
    */
    public void loadProviders() 
    {
        log.info("[loadProviders()] - Loading notification providers from PROFILETYPE_REF table");
        SysAdminManager sManager = getSysAdminManager();
        try
        {
            List<HashMap> resultList = sManager.getProfileTypeList();
            for (HashMap<String,Object> rowMap : resultList)
            {
                log.info("[loadProviders()] processing provider - "+rowMap);
                String className = (String)rowMap.get(XanbooNotificationProviderInterface.CLASSNAME);
                if ( className == null || className.equalsIgnoreCase(""))
                {
                    log.info("[loadProviders()] "+XanbooNotificationProviderInterface.CLASSNAME+" was not provided for provider "+
                                rowMap.get(XanbooNotificationProviderInterface.PROFILE_ID));
                    continue;
                }
                log.info("[loadProviders()] - Initialize provider "+className+" for "+rowMap.get(XanbooNotificationProviderInterface.PROFILE_ID)+ " "+rowMap.get(XanbooNotificationProviderInterface.PROFILE_DESCR));
                //create the provider instance from the classname
                XanbooNotificationProviderInterface provider = (XanbooNotificationProviderInterface)Class.forName(className).newInstance();
                providers.add(provider);
                HashMap<String,Object> configMap = new HashMap<String,Object>();
                //initialize the config properties using the data from the row being processed
                configMap.putAll(rowMap);
                //if a value exists in the PROVIDER_PROPERTIES coluumn 				
                if ( rowMap.containsKey(XanbooNotificationProviderInterface.PROPERTIES) && ((String)rowMap.get(XanbooNotificationProviderInterface.PROPERTIES)).length() > 0 )
                {
                    String props = (String)rowMap.get(XanbooNotificationProviderInterface.PROPERTIES);
                    //test to determine if the column contains name=value pairs or a file name
                    //if the value of the PROVIDER_PROPERTIES column does not end with ".properties", load config from the column value
                    if ( !props.endsWith(".properties") )
                    {
                        log.info("[loadProviders()] - Configuration properties loaded from PROVIDER_PROPERTIES column for provider : "+rowMap.get(XanbooNotificationProviderInterface.PROFILE_ID));
                        Properties p = new Properties();
                        p.load(new StringReader(props));
                        configMap.putAll((Map)p);
                        configMap.remove(XanbooNotificationProviderInterface.PROPERTIES);
                    }
                    else //a file name was specified as a value for PROVIDER_PROPERTIES
                    {
                        try
                        {
                            log.debug("[loadProviders()] - Attempting to load configuration properties for provider "+provider.getProfileTypeId()+" from "+props);
                            configMap.putAll((Map)this.loadProviderProperties(props));
                        }
                        catch(FileNotFoundException fnf)
                        {
                            if ( log.isDebugEnabled() )
                                log.error("[loadProviders()] - FileNotFoundException when attempting to read "+props,fnf);
                            else
                                log.error("[loadProviders()] - FileNotFoundException when attempting to read "+props);
                        }
                        catch(IOException ioe)
                        {
                            if ( log.isDebugEnabled())
                                log.error("[loadProviders()] - IOException when attempting to read "+props,ioe);
                            else
                                log.error("[loadProviders()] - IOException when attempting to read "+props);
                        }
                    }
                }
                configMap.put("PTYPE_USER",configMap.get(XanbooNotificationProviderInterface.PROFILE_USERNM));
                configMap.put("PTYPE_PASS",configMap.get(XanbooNotificationProviderInterface.PROFILE_USERPASS));
                //call the intialize method on the provider
                provider.initialize(configMap);
            }
        }
        catch (Exception ex)
        {
            if ( log.isDebugEnabled() )
                log.error("[loadProviders()] -  Exception loading notification providers from PROFILETYPE_REF", ex);
            else
                log.error("[loadProviders()] -  Exception loading notification providers from PROFILETYPE_REF, "+ex.toString());
        }
    }
    /** 
    * This method sends the notification message to the providers specified by the <code>NotificationDestination</code>
    * for the message. <br/>
    * First the destintions for the message are sorted and separated into "buckets" (destinations with the same provider)<br/>
    * Next the template from <code>DomainTemplateCache</code> is retrieved and applied.<br/>
    * Finally, loop through all the destination "buckets" and send the message to each destination. 
    * @param message 
    */
    public void sendMessage(NotificationMessageInterface message) throws XanbooNotificationException
    {
        long startTime = System.currentTimeMillis();
        //get thte template from the cache for the message
        String domainId = message.getMessageProperties().getDomainId();
        String language = message.getLanguage();

        //if missing or empty string, default to "en"
        if ( language == null || language.equalsIgnoreCase(""))
            language = "en";

        Integer templateTypeId = message.getTemplateTypeId();
        Integer eventId = message.getMessageProperties().getEventId();
        if ( log.isDebugEnabled() )
            log.debug("[sendMessage()] - Sending message [domainId="+domainId+", templateTypeId="+templateTypeId+", eventId="+eventId+"]");
        boolean isLegacyDLC = false;

        //this field will not be passed in the new XAIL
        //if this field exists (is not null) and is not an empty string, the message is from a legacy DLC
        if ( message.getMessage() != null && !message.getMessage().equalsIgnoreCase(""))	 
            isLegacyDLC = true;

        //separate destinations into distinct providers, start with sorted list of providers
        List<NotificationDestination> destinations = message.getSortedToDestinations();
        HashMap<String,List<NotificationDestination>> destinationMap = createDestinationBuckets(destinations);

        //destinations separated into "buckets" by provider, loop through buckets to send notifications
        DomainTemplateCache templateCache = getTemplateCache();
        Iterator<String> keyIterator = destinationMap.keySet().iterator();
        while ( keyIterator.hasNext() )
        {
            String profileKey = (String)keyIterator.next();
            List<NotificationDestination> destList = destinationMap.get(profileKey);
            //send the message\
            XanbooNotificationProviderInterface provider = null;
            if ( profileKey.indexOf("@") > -1 || profileKey.indexOf("http") > -1 )
                provider = this.getProvider(profileKey);
            else
                provider = this.getProvider(new Integer(profileKey));

            if ( provider == null )
            {
                log.error("[sendMessage()] - Notification provider not properly configured, profileKey="+profileKey);
                throw new XanbooNotificationException("Notification provider not properly configured, profileKey="+profileKey,null,false);
            }

            if(log.isDebugEnabled()) {
                log.debug("[sendMessage()] - Sending message using provider "+provider.getProfileTypeId()+" "+provider.getProfileSuffix());
            }
            //handling for USERNAME and USERPASS columns of PROFILETYPE_REF table. This is required for legacy support
            String userName = (String) provider.getConfigMap().get("USERNAME");
            String userPass = (String) provider.getConfigMap().get("USERPASS");
            if ( userName != null && !userName.equalsIgnoreCase(""))
                message.getMessageProperties().put(XanbooMessageMap.PTYPE_USER, userName);
            if ( userPass != null && !userPass.equalsIgnoreCase(""))
                message.getMessageProperties().put(XanbooMessageMap.PTYPE_PASS, userPass);
            //if the XAIL command is not a legacy DLC command ( the _text attribute is missing), locate and process template
            XanbooDomainTemplate template = null;
            if ( !isLegacyDLC )
            {
                template = templateCache.getTemplate(domainId,language,templateTypeId,provider.getProfileTypeId(),eventId);
                if(log.isDebugEnabled()) {
                    log.debug("[sendMessage()] - Sending notification using template = "+template);
                }
            }
            else //using a legacy DLC XAIL command, 
            {
                //in legacy DLC, most template parameter replacement occurs in DLC. only limited parameter 
                //replacment occurs in xail command processing.
                log.debug("[sendMessage()] - Sending notification using legacy DLA/DLC template");
                template = new LocalTemplate(message);
            }

            if ( !isLegacyDLC && provider.encodeParametersBeforeTemplate() )
            {
                XanbooMessageMap msgMap = message.getMessageProperties();
                Set<String> keys = msgMap.keySet();
                for ( String key : keys )
                {
                    Object value = msgMap.get(key);
                    if ( value instanceof String )
                        msgMap.put(key, provider.encodeMessage((String)value));
                }
            }

            if ( provider.canAcceptDestinationList() )
            {
                try
                {
                    applyTemplate(message,provider,template,isLegacyDLC);
                }
                catch(NullPointerException npe)
                {
                    if ( log.isDebugEnabled() )
                        log.error("[sendMessage()] - Error processing template "+template+" using message "+message, npe);
                    throw new XanbooNotificationException("Error processing template "+template+" using message "+message,npe,false);
                }
                //perform any validations for the message
                provider.validate(message);
                if ( log.isDebugEnabled() )
                    log.debug("[sendMessage()] - Sending notification subject="+message.getSubject()+" and message : \r\n"+message.getMessage());
                provider.sendMessage(destList, message);
            }
            else //provider cannot accept multiple destinations, the destination is a part of the provider specific template
            {
                for (NotificationDestination destination : destList )
                {
                    XanbooMessageMap map = message.getMessageProperties();
                    map.put(XanbooMessageMap.TOADDRESS, destination.getDestinationAddress());
                    
                    if (GlobalNames.NOTIFICATION_OPT_IN_ENABLED  && XanbooUtil.isValidEmail(destination.getDestinationAddress())) 
                    {
                        String optInOutUrl = (String) provider.getConfigMap().get("OPTINOUT.URL");
                        String token       = destination.getToken();
                        if (optInOutUrl != null && optInOutUrl.trim().length() > 0 && token != null && token.trim().length() > 0) 
                        {
                            map.put(XanbooMessageMap.OPTINOUT_URL, optInOutUrl);
                            map.put(XanbooMessageMap.OPTINOUT_TOKEN, token);
                            if ( log.isDebugEnabled() )
                                log.debug("[sendMessage()] - Set OPTINOUT_URL:"+optInOutUrl+" OPTINOUT_TOKEN:"+token);
                        } 
                        else 
                        {
                            log.warn("[sendMessage()] - provider OPTINOUT.URL and/or destination address token are missing.  Destination address:" + destination.getDestinationAddress() + " OPTINOUT.URL:" + optInOutUrl + " token:" + token);
                        }
                    }
                    try
                    {
                        applyTemplate(message,provider,template,isLegacyDLC);
                    }
                    catch(NullPointerException npe)
                    {
                        if ( log.isDebugEnabled() )
                            log.error("[sendMessage()] - Error processing template "+template+" using message "+message, npe);
                        throw new XanbooNotificationException("Error processing template "+template+" using message "+message,npe,false);
                    }
                    if ( log.isDebugEnabled() )
                        log.debug("[sendMessage()] - Sending notification destination="+destination.getDestinationAddress()+" subject="+message.getSubject()+" and message "+message.getMessage());
                    provider.sendMessage(destination, message);
                }
            }
        }
        long stopTime = System.currentTimeMillis();
        if ( log.isDebugEnabled() )
            log.debug("[sendMessage()] - Elapsed time to send message to all destinations - "+(stopTime - startTime)+" milliseconds");
    }
    
    protected void applyTemplate(NotificationMessageInterface message,XanbooNotificationProviderInterface provider,XanbooDomainTemplate template,boolean isLegacyDLC)
    {
        if ( !isLegacyDLC )
        {
            //subject template override logic. 
            if ( message.overrideSubjectTemplate() && message.getSubject() != null && !message.getSubject().equalsIgnoreCase(""))
            {
                XanbooDomainTemplate localTemplate = new LocalTemplate(message);
                //apply template
                message.setSubject(template.parseSubjectTemplate(message.getMessageProperties()));
            }
            else
                message.setSubject(template.parseSubjectTemplate(message.getMessageProperties()));
            //logic to handle default (email) template being used to send SMS message. Email can handle HTML / sms cannot
            //if the provider cannot accept the content type of SVR_MSG_TMPL, the MESSAGE_TMPL field is used instead
            if ( provider.canAccept(template.getContentType()))
                message.setMessage(template.parseTemplate(message.getMessageProperties()),template.getContentType());
            else
                message.setMessage(template.parseShortMessage(message.getMessageProperties()));
        }
        else //using a legacy DLC XAIL command, 
        {
            //in legacy DLC, most template parameter replacement occurs in DLC. only limited parameter 
            //replacment occurs in xail command processing.
            if ( log.isDebugEnabled())
                log.debug("[sendMessage()] - Sending notification using legacy DLA/DLC template");
            //apply template
            message.setSubject(template.parseSubjectTemplate(message.getMessageProperties()));
            message.setMessage(template.parseShortMessage(message.getMessageProperties()));
        }
    }
    /**
    * Helper method to separate the destinations into "buckets" based on provider. 
    * Useful when XAIL command sends multiple provider types. 
    * 
    * @param destinations
    * @return 
    */
    protected HashMap<String,List<NotificationDestination>> createDestinationBuckets(List<NotificationDestination> destinations)
    {
        HashMap<String,List<NotificationDestination>> destinationMap = new HashMap<String,List<NotificationDestination>>();
        List<NotificationDestination> splitList = new ArrayList<NotificationDestination>();
        String oldkey = null;
        int bucketCount = 0;
        if ( log.isDebugEnabled())
            log.debug("[createDestinationBuckets()] - Split destinations into buckets, total number of destinations "+destinations.size());
        for ( NotificationDestination destination : destinations )
        {
            String newkey = (destination.getProfileTypeId() != null && destination.getProfileTypeId().intValue() != destination.PROFILE_NOT_SPECIFIED) ? destination.getProfileTypeId().toString() : destination.getSuffix();
            if ( splitList.isEmpty() )
            {
                destinationMap.put(newkey, splitList);
            }
            else if ( !newkey.equals(oldkey))	//keys dont match .. different destination .. different "bucket"
            {
                splitList = new ArrayList<NotificationDestination>();
                destinationMap.put(newkey, splitList);
                bucketCount++;
            }
            splitList.add(destination);
            oldkey = newkey;
        }
        if ( log.isDebugEnabled() )
            log.debug("[createDestinationBuckets()] - Destinations sorted and split into "+bucketCount+" buckets");
        return destinationMap;
    }
    /**
    * Loads configuration properties from the file system either as a resource or a file
    * @param fileName
    * @return
    * @throws FileNotFoundException
    * @throws IOException 
    */
    protected Properties loadProviderProperties(String fileName) throws FileNotFoundException, IOException
    {
        //can be loaded as a resource of from file system. Attempt file system
        File propFile = new File(fileName);
        InputStream fileStream = null;
        if ( propFile.exists() && propFile.isFile() ) //test to determine if the file exists and is on the file system
        {
            if ( log.isDebugEnabled() )
                log.debug("[loadProviderProperties()] - Loading provider configuration properties from file "+fileName);
            fileStream = new BufferedInputStream(new FileInputStream(propFile),1024);
        }
        else //assume the properties file can be loaded as a resource
        {
            if ( log.isDebugEnabled() )
                log.debug("[loadProviderProperties()] - Loading provider configuration properties "+fileName+" as a resource");
            InputStream inputStream = this.getClass().getResourceAsStream(fileName);
            fileStream = new BufferedInputStream(inputStream,1024);
        }
        Properties p = new Properties();
        p.load(fileStream);
        fileStream.close();
        return p;
    }

    public static void main(String[] args)
    {
        System.out.println("testing notification factory");
        XanbooNotificationFactory factory = XanbooNotificationFactory.getInstance();

        XanbooNotificationMessage message = new XanbooNotificationMessage();
        message.setFromAddress("info@xanboo.com");
        message.addToAddress("0%lm899p@att.com");
        //message.addToAddress("0%RP385F@att.com");
        message.addToAddress("400%14042341417");
        message.addToAddress("0%maloney1@mindspring.com");
        //message.addToAddress("410%4045160065");
        message.addToAddress("410%4042341417");
        message.setTemplateTypeId(0);//default for notifications
        message.setTimestamp(new Date());
        message.getMessageProperties().put(XanbooMessageMap.ID, 0);
        message.getMessageProperties().put(XanbooMessageMap.DOMAINID, "DL");
        message.getMessageProperties().put(XanbooMessageMap.EVENT, "By the pricking of my thumbs, something wicked this way comes!");
        message.getMessageProperties().put(XanbooMessageMap.DEVICE,"Salem");
        message.getMessageProperties().put(XanbooMessageMap.GATEWAY, "MASS");
        message.getMessageProperties().put(XanbooMessageMap.TIMESTAMP, new Date().toString());
        message.getMessageProperties().put(XanbooMessageMap.SUBSCRIBER_NAME,"John Doe");
        try
        {
            factory.sendMessage(message);
        }
        catch(Exception ex)
        {}

    }

    //inner class to handle legacy DLA template processing
    class LocalTemplate extends XanbooDomainTemplate
    {
        public LocalTemplate(NotificationMessageInterface message)
        {
            subjectBuffer = new StringBuffer((message.getSubject() != null ? message.getSubject() : ""));
            messageBuffer = new StringBuffer(message.getMessage());
        }
    }
}
