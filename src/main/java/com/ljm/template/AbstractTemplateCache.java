/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ljm.template;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract class to implement a template cache / pool. 
 * 
 * @author Luther Maloney
 * @since October 2013
 */
public abstract class AbstractTemplateCache
{
	protected ConcurrentHashMap<String,Template> templateCache = new ConcurrentHashMap<String,Template>();
		
	public static AbstractTemplateCache getInstance()
	{
		return null;
	}
	
	public synchronized void initialize()
	{
		HashMap<String,Template> tempCache = loadTemplates();
		templateCache.putAll(tempCache);
	
	}
	/**
	 * returns a template from the cache/pool using the specified name
	 * @param templateName
	 * @return 
	 */
	public Template getTemplate(String templateName)
	{
		return templateCache.get(templateName);
	}
	/** 
	 * Returns true if the template specified by the templateName is loaded in the cache.
	 * @param templateName
	 * @return 
	 */
	protected boolean isTemplateCached(String templateName)
	{
		return templateCache.containsKey(templateName);
	}
	/** 
	 * reloads the templates 
	 */
	public void reloadTemplates()
	{
		HashMap<String,Template> tempCache = loadTemplates(); //reload templates via method
		
		templateCache.putAll(tempCache);
		
		/*synchronized(templateCache) //swap out contents of the cache
		{
			templateCache.clear();
			templateCache.putAll(tempCache);
		}*/
	}
	/**
	 * Method to be implemented in any concrete implmentation to load the templates into the cache.
	 * @return 
	 */
	public abstract HashMap<String,Template> loadTemplates();
	
}
