/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ljm.template;


import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.xanboo.core.sdk.sysadmin.SysAdminManager;
import com.xanboo.core.util.EjbProxy;
import com.xanboo.core.util.GlobalNames;
import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;

/**
 * A cache/pool of templates loaded from the DOMAIN_TEMPLATES table. 
 * 
 * @author Luther J Maloney
 * @since  October 2013
 */
public class DomainTemplateCache extends AbstractTemplateCache
{
	private static DomainTemplateCache instance = null;
	private Logger log = LoggerFactory.getLogger(getClass().getName());
    private long reloadIntervalMillis = 60*60*1000; //default 1 hour
    private static long lastRefreshMillis = 0l;
    private static Object lock = new Object();
   
	/**
	 * Constructor
	 */
	protected DomainTemplateCache()
	{
		
           reloadIntervalMillis = GlobalNames.NOTIF_TEMPL_RELOAD_INTERVAL * 60 * 1000;
           log.info("GlobalNames.NOTIF_TEMPL_RELOAD_INTERVAL :"+ GlobalNames.NOTIF_TEMPL_RELOAD_INTERVAL);
           log.info("  reloadIntervalMillis :"+ reloadIntervalMillis);
          /* if(GlobalNames.NOTIF_TEMPL_RELOAD_INTERVAL < 60){
        	   reloadIntervalMillis = 60 * 24 * 10 *60 * 1000;
        	   log.info(" failover reloadIntervalMillis :"+ reloadIntervalMillis);
           }*/
           
      
	}
	/**
	 * Returns the current active instance of the template cache (singleton pattern)
	 * @return 
	 */
	public static DomainTemplateCache getInstance()
	{
		if ( instance == null )
		{
			synchronized(lock) 
			{
				if(instance == null){
					instance = new DomainTemplateCache();
					instance.initialize();
					lastRefreshMillis = System.currentTimeMillis();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates the template key in the format of<br/>
	 * domainId-templateTypeId-profileTypeId-language-eventIt
	 * @param domainId
	 * @param lang
	 * @param templateTypeId
	 * @param profileTypeId
	 * @param eventId
	 * @return 
	 */
	public String createTemplateKey(String domainId,String lang,Integer templateTypeId,Integer profileTypeId,Integer eventId)
	{
		StringBuilder str = new StringBuilder();
		str.append(domainId);
		str.append("-");
		str.append(templateTypeId);
		str.append("-");
		str.append(profileTypeId);
		str.append("-");
		str.append(lang);
		str.append("-");
		str.append(eventId);
		return str.toString();
	}
	/**
	 * Attempts to find a template going from the most specific match to the default template.
	 * @param domainId
	 * @param lang
	 * @param tempTypId
	 * @param profileTypId
	 * @param eventId
	 * @return 
	 */
	public XanbooDomainTemplate getTemplate(String domainId,String lang,Integer tempTypId,Integer profileTypId,Integer eventId)
	{
		String templateKey = createTemplateKey(domainId,lang,tempTypId,profileTypId,eventId);
		
		log.debug("[getTemplate()] - Searching for template matching = "+templateKey);
		
		XanbooDomainTemplate template = null;
		
		//1. use the complete key
		if ( isTemplateCached(templateKey))
			template = (XanbooDomainTemplate)getTemplate(templateKey);
        
        //1.a if language > 2, drop try with language.substring(0,2)
        if ( template == null && lang.length() > 2 )
        {
            String sl = lang.substring(0,2);
            templateKey = createTemplateKey(domainId,sl,tempTypId,profileTypId,eventId);
            if ( isTemplateCached(templateKey))
                template = (XanbooDomainTemplate)getTemplate(templateKey);
        }

		//2.try "default" domain instead of specified domain, keeping all else equal .. 
		templateKey = createTemplateKey("default",lang,tempTypId,profileTypId,eventId);
		if ( template == null && isTemplateCached(templateKey))
			template = (XanbooDomainTemplate)getTemplate(templateKey);
        
        if ( template == null && lang.length() > 2 )
        {
            templateKey=createTemplateKey("default",lang.substring(0,2),tempTypId,profileTypId,eventId);
            if ( isTemplateCached(templateKey))
                template = (XanbooDomainTemplate)getTemplate(templateKey);
        }
        //use two char locale for all default templates
        if ( lang.length() > 2 ) lang = lang.substring(0,2);
        
        //3.use default profileTypeId of 0, all else the same .. 
        templateKey = createTemplateKey(domainId,lang,tempTypId,0,eventId);
		if ( template == null && isTemplateCached(templateKey))
			template = (XanbooDomainTemplate)getTemplate(templateKey);
        
        //4. use the default event (event_id = 0)
		templateKey = createTemplateKey(domainId,lang,tempTypId,profileTypId,0);
		if ( template == null && isTemplateCached(templateKey))
			template = (XanbooDomainTemplate)getTemplate(templateKey);
		
		//5.try "default" domain instead of specified domain, keeping all else equal .. 
		//the "default" domain template for the template type, profile, and lang
		templateKey = createTemplateKey("default",lang,tempTypId,profileTypId,0);
		if ( template == null && isTemplateCached(templateKey))
			template = (XanbooDomainTemplate)getTemplate(templateKey);
		
		//6.Try the default template for the domain
		templateKey = createTemplateKey(domainId,lang,0,0,0);
		if ( template == null && isTemplateCached(templateKey))
			template = (XanbooDomainTemplate)getTemplate(templateKey);
		
		//7.Try the default template for the domain
		templateKey = createTemplateKey("default","en",0,0,0);
		if ( template == null && isTemplateCached(templateKey))
			template = (XanbooDomainTemplate)getTemplate(templateKey);
		
		if ( log.isDebugEnabled() )log.debug("[getTemplate()] - Using template "+template.getTemplateName());
		
		if ( log.isDebugEnabled() )log.debug("[getTemplate()] - reloadIntervalMillis :  "+reloadIntervalMillis + " : lastRefreshMillis : " + lastRefreshMillis + " System.currentTimeMillis() : " + System.currentTimeMillis());
		
      
		
		
		if (System.currentTimeMillis() > (this.reloadIntervalMillis + lastRefreshMillis)) {
			if (!GlobalNames.templateCacheLock.isWriteLocked()) {
				GlobalNames.templateCacheLock.writeLock().lock();

				if (System.currentTimeMillis() > (this.reloadIntervalMillis + lastRefreshMillis)) {
					Thread th = new Thread("TemplateCacheReload") {
						public void run() {
							log.info("[TemplateCacheReload.run()] - reload template cache from source");
							reloadTemplates();
							lastRefreshMillis = System.currentTimeMillis();
							GlobalNames.templateCacheLock.writeLock().unlock();;
						}
					};
					th.start();
				}
			}

		}
        
		return template;
	}
    protected SysAdminManager getSysAdminManager()
    {
        EjbProxy proxy = new EjbProxy(GlobalNames.SDK_JNDI_CONTEXT_FACTORY, GlobalNames.SDK_JNDI_PROVIDER_URL); // remote invocation
		SysAdminManager sManager = null;
		try 
		{
			log.debug("[loadTemplates()] - Attempt to retrieve reference to SystemManager EJB");
			sManager = (SysAdminManager) proxy.getObj(GlobalNames.EJB_SYSADMIN_MANAGER);
		}
		catch(Exception e) 
		{
			log.error("[loadTemplates()]: Exception " + e.getMessage(),e);
            throw new RuntimeException("Unable to obtain reference to SysAdminManager bean. Cannot load templates");
		}
        return sManager;
    }
	/**
	 * Method to load the templates
	 * @return 
	 */
	@Override
	public HashMap<String,Template> loadTemplates()
	{
		SysAdminManager sManager = getSysAdminManager();
		
		HashMap<String,Template> templateMap = new HashMap<String,Template>();
		try
		{
			//this code is for testing purposes only!!!
			log.info("[loadTemplates()] - Begin loading templates from DOMAIN_TEMPLATES");
			List<HashMap<String,Object>> resultList = sManager.getDomainTemplateList();
			for (HashMap<String,Object> rowMap : resultList)
			{
				if ( log.isDebugEnabled())
                    log.debug("[loadTemplates()] Loading template from "+rowMap);
				XanbooDomainTemplate template = null;
				String domainId = (String)rowMap.get(XanbooDomainTemplate.DOMAIN_ID);
				String contentType = (String)rowMap.get(XanbooDomainTemplate.CONTENT_TYPE);
				Integer templateTypId = new Integer(rowMap.get(XanbooDomainTemplate.TEMPLATE_TYPE_ID).toString());
				Integer profileTypId = new Integer(rowMap.get(XanbooDomainTemplate.PROFILE_TYPE_ID).toString());
				String lang = (String)rowMap.get(XanbooDomainTemplate.LANG);
				Integer eventId = new Integer(rowMap.get(XanbooDomainTemplate.EVENT_ID).toString());
				String subject = (String)rowMap.get(XanbooDomainTemplate.SUBJECT);
				String text = (String)rowMap.get(XanbooDomainTemplate.MESSAGE);

				if ( rowMap.get(XanbooDomainTemplate.SERVER_MESSAGE) == null )
					template = new XanbooDomainTemplate(domainId,templateTypId,contentType,
														profileTypId,lang,eventId,subject,text);
				else
				{
					if ( rowMap.get(XanbooDomainTemplate.SERVER_MESSAGE) instanceof String)
						template = new XanbooDomainTemplate(domainId,templateTypId,contentType,
															profileTypId,lang,eventId,subject,text,
															new StringReader((String)rowMap.get(XanbooDomainTemplate.SERVER_MESSAGE)));
					else
					{
						template = new XanbooDomainTemplate(domainId,templateTypId,contentType,
															profileTypId,lang,eventId,subject,text,(StringBuffer)rowMap.get(XanbooDomainTemplate.SERVER_MESSAGE));
					}
				}
				log.info("[loadTemplates()] - Loaded templateName="+template.getTemplateName()+" template="+template);
				templateMap.put(template.getTemplateName(), template);
			}
		}
		catch (Exception ex)
		{
			log.error("[loadTemplates()] - Exception loading templates from DOMAIN_TEMPLATES", ex);
		}
       
		return templateMap;
	}
}
