/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.template;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class to contain details for Domain Templates. <br/>
 * The contents of the SVR_MDG_TMPL column are loaded into the template buffer of
 * the superclass, <code>Template</code>. 
 * 
 * @author Luther Maloney
 * @since October 2013
 */
public class XanbooDomainTemplate extends Template implements Cloneable
{
	//static variables for hashtable keys (columns from DOMAIN_TEMPLATES table)
	public static final String DOMAIN_ID = "DOMAIN_ID";
	public static final String CONTENT_TYPE = "SERVER_MSG_CTYPE";
	public static final String TEMPLATE_TYPE_ID = "TEMPLATETYPE_ID";
	public static final String PROFILE_TYPE_ID = "PROFILETYPE_ID";
	public static final String LANG = "LANGUAGE_ID";
	public static final String EVENT_ID = "EVENT_ID";
	public static final String SUBJECT = "SUBJECT_TMPL";
	public static final String MESSAGE = "MESSAGE_TMPL";
	public static final String SERVER_MESSAGE = "SERVER_MSG_TMPL";
	
	/*content type, defaults to text/plain */
	private String contentType = "text/plain";
	/* the domain id of the template */
	private String domainId;
	/* the template type */
	private int templateTypeId;
	/* the profile type (provider) for the template */
	private int profileTypeId;
	/* the language of the template, default is en*/
	private String language = "en";
	/* the event id for the template, default is 0*/
	private int eventId = 0;
	/* a buffer to contain the subject */
	protected StringBuffer subjectBuffer;
	/* a buffer to contain the template text (MESSAGE_TMPL) column. Used when sending  notification via SMS using email profile type*/
	protected StringBuffer messageBuffer;
	/**
	 * Constructor
	 */
	public XanbooDomainTemplate()
	{
		subjectBuffer = new StringBuffer();
		messageBuffer = new StringBuffer();
	}
	/**
	 * 
	 * @param domain
	 * @param templateType
	 * @param profileType
	 * @param lang
	 * @param eventId
	 * @param subject
	 * @param text 
	 */
	public XanbooDomainTemplate(String domain,Integer templateType,Integer profileType,String lang,
								Integer eventId,String subject,String text)
	{
		this.domainId = domain;
		this.templateTypeId = templateType;
		this.eventId = eventId;
		this.profileTypeId = profileType;
		this.language = lang;
		this.subjectBuffer = new StringBuffer(subject);
		if ( text != null )
			this.messageBuffer = new StringBuffer(text.trim());
		setTemplateName(generateTemplateName(domain,templateType,profileType,lang,eventId));
	}
	/**
	 * 
	 * @param domain
	 * @param templateType
	 * @param profileType
	 * @param lang
	 * @param eventId
	 * @param subject
	 * @param text 
	 */
	public XanbooDomainTemplate(String domain,Integer templateType,String contentType,
								Integer profileType,String lang,Integer eventId,String subject,String text)
	{
		this.domainId = domain;
		this.templateTypeId = templateType;
		this.eventId = eventId;
		this.profileTypeId = profileType;
		this.language = lang;
		this.subjectBuffer = new StringBuffer(subject.trim());
		if ( text != null )
			this.messageBuffer = new StringBuffer(text.trim());
		this.contentType = contentType;
		setTemplateName(generateTemplateName(domain,templateType,profileType,lang,eventId));
	}
	
	/**
	 * 
	 * @param domain
	 * @param templateType
	 * @param contentType
	 * @param profileType
	 * @param lang
	 * @param eventId
	 * @param subject
	 * @param text
	 * @param templateReader 
	 */
	public XanbooDomainTemplate(String domain,Integer templateType,String contentType,Integer profileType,
								String lang,Integer eventId,String subject,String text,Reader templateReader)
	{
		this.domainId = domain;
		this.templateTypeId = templateType;
		this.eventId = eventId;
		this.profileTypeId = profileType;
		this.language = lang;
		this.subjectBuffer = new StringBuffer(subject.trim());
		if ( text != null )
			this.messageBuffer = new StringBuffer(text.trim());
		this.contentType = contentType;
		setTemplateName(generateTemplateName(domain,templateType,profileType,lang,eventId));
		this.loadTemplate(templateReader);
	}
	/**
	 * 
	 * @param domain
	 * @param templateType
	 * @param contentType
	 * @param profileType
	 * @param lang
	 * @param eventId
	 * @param subject
	 * @param text
	 * @param templateBuffer 
	 */
	public XanbooDomainTemplate(String domain,Integer templateType,String contentType,Integer profileType,
								String lang,Integer eventId,String subject,String text,StringBuffer templateBuffer)
	{
		this.domainId = domain;
		this.templateTypeId = templateType;
		this.eventId = eventId;
		this.profileTypeId = profileType;
		this.language = lang;
		this.subjectBuffer = new StringBuffer(subject.trim());
		if ( text != null )
			this.messageBuffer = new StringBuffer(text.trim());
		this.contentType = contentType;
		this.setTemplateBuffer(templateBuffer);
		setTemplateName(generateTemplateName(domain,templateType,profileType,lang,eventId));
	}
	/**
	 * The content type for the template
	 * @return 
	 */
	public String getContentType()
	{
		return this.contentType;
	}
	/**
	 * The domain Id of the template
	 * @return 
	 */
	public String getDomainId()
	{
		return this.domainId;
	}
	/**
	 * The event id
	 * @return 
	 */
	public int getEventId()
	{
		return this.eventId;
	}
	@Override
	public String getHandleMissingParameters()
	{
		return "Log_WARN";
	}
	/**
	 * The language of the template
	 * @return 
	 */
	public String getLanguage()
	{
		return this.language;
	}
	/**
	 * The profile type id
	 * @return 
	 */
	public int getProfileTypeId()
	{
		return this.profileTypeId;
	}
	/**
	 * Returns the template type identifier
	 * @return 
	 */
	public int getTemplateTypeId()
	{
		return this.templateTypeId;
	}
	public String parseShortMessage(HashMap args)
	{
		return parseTemplate(messageBuffer,args).trim();
	}
	/**
	 * Parses the subject template
	 * @param args
	 * @return 
	 */
	public String parseSubjectTemplate(HashMap args) 
	{
		return parseTemplate(subjectBuffer,args).trim();
	}
	/**
	 * Creates the text version of the template name
	 * @param domain
	 * @param dtype
	 * @param ptype
	 * @param lang
	 * @param eventId
	 * @return 
	 */
	protected String generateTemplateName(String domain,Integer dtype,Integer ptype,String lang,Integer eventId)
	{
		StringBuilder str = new StringBuilder();
		str.append(domain);
		str.append("-");
		str.append(dtype);
		str.append("-");
		str.append(ptype);
		str.append("-");
		str.append(lang);
		str.append("-");
		str.append(eventId);
		return str.toString();
	}
	/**
	 * Returns true of the templates being compared have the same name
	 * @param o
	 * @return 
	 */
	@Override
	public boolean equals(Object o)
	{
		if ( o instanceof XanbooDomainTemplate )
		{
			XanbooDomainTemplate t = (XanbooDomainTemplate)o;
			return t.getTemplateName().equals(getTemplateName());
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 83 * hash + (this.domainId != null ? this.domainId.hashCode() : 0);
		hash = 83 * hash + this.templateTypeId;
		hash = 83 * hash + this.profileTypeId;
		hash = 83 * hash + (this.language != null ? this.language.hashCode() : 0);
		hash = 83 * hash + this.eventId;
		return hash;
	}
	
	@Override
	public String toString()
	{
		StringBuilder str = new StringBuilder();
		str.append("XanbooDomainTemplate[domainId=");
		str.append(domainId);
		str.append(",templateTypeId=");
		str.append(templateTypeId);
		str.append(",profileTypeId=");
		str.append(profileTypeId);
		str.append(",language=");
		str.append(language);
		str.append(",eventId=");
		str.append(eventId);
		str.append("]");
		return str.toString();
	}
	
	@Override
	public Object clone()
	{
		XanbooDomainTemplate template = new XanbooDomainTemplate();
		template.contentType = contentType;
		template.domainId = domainId;
		template.eventId = eventId;
		template.language = language;
		template.messageBuffer = new StringBuffer(messageBuffer);
		template.profileTypeId = profileTypeId;
		template.subjectBuffer = new StringBuffer(subjectBuffer);
		template.templateTypeId = templateTypeId;
		template.templateBuffer = new StringBuffer(templateBuffer);
		template.templateName = new String(templateName);
		template.psql = psql;
		template.directives = (ArrayList)directives.clone();
		return template;
	}
}
