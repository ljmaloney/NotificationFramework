/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.template;

import java.io.*;
import java.util.HashMap;
import java.util.ArrayList;
import com.xanboo.core.util.LoggerFactory;

/**
 * Loads a template file from the specifed location and parses the file based on 
 * directives contained in the template. All directives in the template must start with 
 * a "[#" and end with a "]". If one of the directives is "[#psql]", then parts of the
 * template are conditional, which is to say that some parts of the template will only be executed 
 * if keys are placed into a properties object. In this case parts of the template 
 * will be surrounded by pairs of matching tags like the following : <br>
 *	[somekeyname]
 *     text to include
 *  [/somekeyname]. <br>
 * Nesting of conditional sections is not allowed. The conditional directive MUST appear as the first line
 * of the file where used.<br>
 * Comments start with a "#" and continue to the end of the line. All comments are discarded.
 * Parameters to be replaced in the template are surrounded by a "%%" and a "%%" like %%somekey%%}. The value
 * to replace the parameter is contained in hashtable object.
 * @author  Luther J Maloney
 * @since October 2004
 */
public class Template implements Cloneable
{
	//the name of the template / file
	protected String templateName = null;
	//the path to the template file
	protected String path = null;
	protected ArrayList directives = new ArrayList();
	protected boolean psql = false;
	//instance variable for the template
	protected StringBuffer templateBuffer = null;
	/**
	 * Constructor
	 */
	public Template()
	{
	}
	/**
	 * Constructor<br/>
	 * Creates a new instance of <code>Template</code>, loading the template from the file system
	 * using the path and file name provided. 
	 * @param path - the Path to the template file
	 * @param name - the name of the template file
	 */
	public Template(String path,String name)
	{
		try
		{
			this.path = path;
			this.templateName = name;
			//load the SQL template from the file
			String fileName = path+File.separator+templateName;
			File tempFile = new File(fileName);
				//validate that the file exists and can be read
			if ( !tempFile.exists() || !tempFile.canRead() )
					throw new RuntimeException("Template : "+fileName+" does not exist or cannot be read");
			this.loadTemplate(new FileReader(fileName));
		}
		catch (FileNotFoundException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException("Exception "+ex.toString()+" reading template from path="+path+" and file="+name);
		}
	}
	/**
	 * Constructor<br/>
	 * Creates a new instance of <code>Template</code>, loading the template from the inputStream specified. 
	 * @param path
	 * @param name
	 * @param inputStream 
	 */
	public Template(String path,String name,InputStream inputStream)
	{
		this.path = path;
		this.templateName = name;
		this.loadTemplate(new InputStreamReader(inputStream));
	}
	/*
	 * Creates a copy of this object. 
	 */
	@Override
	public Object clone()
	{
		Template t = new Template();
		t.path = path;
		t.templateName = templateName;
		t.directives = directives;
		t.psql = psql;
		t.templateBuffer = templateBuffer;
		return t;
	}
	public ArrayList getDirectives()
	{
		return directives;
	}
	public String getHandleMissingParameters()
	{
		return "Exception";
	}
	public boolean isPreParsedSQL()
	{
		return psql;
	}
	/**
	 * Returns the path name to the template (file based template). <br/>
	 * This should be null if the template is loaded from non-file datasource.
	 * @return 
	 */
	public String getPathName()
	{
		return path;
	}
	/**
	 * The name of the template, normaly a file name. 
	 * @return 
	 */
	public String getTemplateName()
	{
		return templateName;
	}
	/**
	 * Sets the name of the template. Used by any sub-class only
	 * @param name 
	 */
	protected void setTemplateName(String name)
	{
		this.templateName = name;
	}
	/**
	 * The template buffer, provides a method for a subclass to set/change the contents of the buffer.
	 * @param str 
	 */
	protected void setTemplateBuffer(StringBuffer str)
	{
		this.templateBuffer = str;
	}
	/**
	 * Method to parse the template. <br>
	 * The method will first attempt to determine if the template must be pre-parsed, that is
	 * if some lines in the SQL file are conditionally included. Those lines are set off by having
	 * a [keyname] before them and a [/keyname] after them. The following is an example : <br>
	 * [includeKey]<br>
	 *   and table_z.column1 = table_z.column2<br>
	 * [/includeKey]<br>
	 * OR
	 * [includeKey]<br>
	 *   and table_z.column1 = {some_parameter}<br>
	 * [/includeKey]<br>
	 * If the 'includeKey' is a key in the Hashtable argument, the above lines of template are included
	 * in the s. This is completed prior to any parameter replacement
	 * being done, so that any parameters in the conditional lines will be handled appropriately.
	 * <br>The parameters in
	 * the template are denoted by a "%%" and a "%%". The parser will attempt to find a
	 * key in the hashtable for each "key" in the template ( ie "%%key%%"). If a key/parameter
	 * exists in the template but not in the Hashtable, a RuntimeException will be thrown.  
	 * @param args
	 * @return 
	 */
	public String parseTemplate(HashMap args)
	{
		return parseTemplate(templateBuffer,args);
	}
	/**
	 * Method to parse the template. <br>
	 * The method will first attempt to determine if the template must be pre-parsed, that is
	 * if some lines in the SQL file are conditionally included. Those lines are set off by having
	 * a [keyname] before them and a [/keyname] after them. The following is an example : <br>
	 * [includeKey]<br>
	 *   and table_z.column1 = table_z.column2<br>
	 * [/includeKey]<br>
	 * OR
	 * [includeKey]<br>
	 *   and table_z.column1 = {some_parameter}<br>
	 * [/includeKey]<br>
	 * If the 'includeKey' is a key in the Hashtable argument, the above lines of template are included
	 * in the s. This is completed prior to any parameter replacement
	 * being done, so that any parameters in the conditional lines will be handled appropriately.
	 * <br>The parameters in
	 * the template are denoted by a "%%" and a "%%". The parser will attempt to find a
	 * key in the hashtable for each "key" in the template ( ie "%%key%%"). If a key/parameter
	 * exists in the template but not in the Hashtable, a RuntimeException will be thro
	 * @param buffer - the template buffer	
	 * @param args - the parameters to be replaced
	 * @return 
	 */
	protected String parseTemplate(StringBuffer buffer,HashMap args)
	{
		String tmp = buffer.toString(); //creates a local copy, insures method is thread safe while maintaining performance
		//System.out.println("[Template:parseTemplate()] - tmp="+tmp+"|");
        StringBuilder out = new StringBuilder();
		//determine if this sql template should be pre-parsed
		if ( psql )
			tmp = preParse(buffer.toString(),args);
		//process the string as a character array for speed
		char[] chars = tmp.toCharArray();
		boolean buildkey = false;
		//buffer to hold any parameter key found
		StringBuilder keyb = new StringBuilder();
		for ( int i = 0; i < chars.length; i++ )
		{
			//System.out.println("[Template.parseTemplate()] chars["+i+"]="+chars[i]+"|");
            if ( chars[i] == '%' && chars[i+1] == '%' && !buildkey )	//if the current character is a "{"
			{
				if ( i > 0 && chars[i-1] == '\\') //escape sequence
				{
					out.setCharAt((out.length()-1),chars[i]);	//will need to remove escape char from output
					continue;  //consume this character
				}
				else //not an escape sequence ... start of a key
				{
					buildkey = true;
					i++;
					continue;  //consume this character
				}
			}
			if ( chars[i] == '%' && chars[i+1] == '%' && buildkey ) //closing "}"
			{
				if ( i > 0 && chars[i-1] == '\\' )	//if the } is being escaped
				{
					out.setCharAt((out.length()-1),chars[i]);	//replace escape character
					continue;
				}
				else
				{
					buildkey=false;	// finished with the key
					if ( !args.containsKey(keyb.toString()))	//does the key exist in the hashtable
					{
						if ( getHandleMissingParameters().equalsIgnoreCase("Exception")) 
							throw new RuntimeException("Required parameter "+keyb.toString()+" was not found");
						else if ( getHandleMissingParameters().equalsIgnoreCase("Log_WARN"))
						{
							LoggerFactory.getLogger(getClass().getName()).warn("Required parameter "+keyb.toString()+" was not found");
						}
						else if ( getHandleMissingParameters().equalsIgnoreCase("Log_ERROR"))
						{
							LoggerFactory.getLogger(getClass().getName()).error("Required parameter "+keyb.toString()+" was not found");
						}
						else
						{
							LoggerFactory.getLogger(getClass().getName()).warn("Required parameter "+keyb.toString()+" was not found");
						}
					}
					else
					{
						//replace the value of the key from the hashtable argument
						out.append(args.get(keyb.toString()).toString());
					}
					
					//re-initialize the key buffer
					keyb = new StringBuilder();
					i++;
					continue;
				}
			}
			if ( buildkey )
			{
				keyb.append(chars[i]);	//placing chars into key buffer
				continue;
			}
            //handle cases where \r or \n exists as printed characters in text, transform to appropriate control characters
            if ( chars[i] == '\\' && (chars[i+1] == 'n' || chars[i+1] == 'r' || chars[i+1] == 't') )
            {
                if (chars[i+1] == 'n')
                    out.append((char)10);   //LF or \n
                if ( chars[i+1] == 'r')
                   out.append((char)13);    //CR or \r
                if ( chars[i+1] == 't')
                    out.append((char)9);    //TAB or \t
                i++;
                continue;
            } 
            out.append(chars[i]);	//default is to place chars into output buffer
		}
		//return the resulting string to the caller
        //System.out.println("[Template:parseTemplate()] - parsed template "+out.toString()+"|");
		return out.toString();
	}
	/**
	 * Method used to load the template. 
	 * @param templateReader 
	 */
	protected void loadTemplate(Reader templateReader)
	{
		String fileName = null;
		try
		{
			//first load a temp buffer from the file 
			//fileName = path+File.separator+templateName;
			//File tempFile = new File(fileName);
			//validate that the file exists and can be read
			//if the SQL file does not exist or cannot be read, throw an exception
			//if ( !tempFile.exists() || !tempFile.canRead() )
			//	throw new RuntimeException("Template : "+fileName+" does not exist or cannot be read");
			//create the buffered reader
			BufferedReader bir = new BufferedReader(templateReader,10*1024);
			//create a buffer to load the file into ... 
			StringBuilder loadTemplateBuffer = new StringBuilder();
			//loop 
			String line = null;
			while ( ( line=bir.readLine() ) != null )
			{
				//this is a directive ... 
				if ( line.startsWith("[#"))
				{
					//if the line of the file is the "[#psql]" directive ... set the psql flag
					if ( line.equals("[#psql]") )
						psql = true;
					else
						directives.add(line.substring(line.indexOf("#"),line.lastIndexOf("]")));
					continue;
				}
                if ( loadTemplateBuffer.length() > 0 )
                    loadTemplateBuffer.append("\r\n");		//for readability in log files when required
				/*if ( line.indexOf("#") == 0)	//this line is a comment .. ignore
					continue;
				if ( line.indexOf("#") > -1 )	//the rest of this line is a comment
				{
					//check for escape character ... 
					int index = line.indexOf("#");
					if ( line.charAt((index-1)) == '\\' )
					{
						//the '#' is escaped ..... remove escape character
						line = line.substring(0,index-1)+line.substring(index);
					}
					else
						line = line.substring(line.indexOf("#"));
				}*/
                LoggerFactory.getLogger(getClass().getName()).debug("Loaded line :"+line+" from templateFile="+getTemplateName());
				loadTemplateBuffer.append(line);
			}
			templateBuffer = new StringBuffer(loadTemplateBuffer.toString());
		}
		catch(Exception e)
		{
			LoggerFactory.getLogger(getClass().getName()).error("Error "+e.toString()+" reading template from "+fileName,e);
			throw new RuntimeException("There was a problem loading the SQL template file");
		}
	}
	//private helper method to preParse the SQL 
	private String preParse(String template,HashMap args)
	{
		StringBuilder out = new StringBuilder(template);	//initialize a string buffer ... the buffer will be manipulated
		int index = -1;
		while ( (index=out.indexOf("["))>-1 )	//loop until no more keys can be found
		{
			String key = null;
			//is this an escaped character?
			if ( index > 0 && template.charAt(index-1) == '\\' )
			{
				//this is an escaped character ... 
				out.deleteCharAt(index-1);
			}
			else	//not an escaped character
			{
				int end = out.indexOf("]",index); //get the end delimiter
				if ( end < 0 )	//if end delimiter could not be found
					throw new RuntimeException("Could not find psql end key delimiter");
				key = out.substring(index+1,end); //get the actual key for the preparse
				String t = "[/"+key+"]"; //a temp string used later
				if ( args.containsKey(key))	//if the key from the sql template is in the hashtable
				{
					//this part of the sql should be executed ...
					//will need to remove the markup from the sql string
					out.delete(index,end+1);	//delete the key
					int sei = out.indexOf(t);	//find the ending index
					if ( sei < 0 )	//if the corresponding end tag cannot be found ... throw exception
						throw new RuntimeException("Could not find end tag for "+key);
					int endtagindex = out.indexOf("]",sei);	//the end index
					out.delete(sei,endtagindex+1); //delete end tag
				}
				else
				{
					//this part of the sql will not be executed .. remove it
					int sei = out.indexOf(t);
					if ( sei < 0 )
						throw new RuntimeException("Could not find end tag for "+key);
					int endtagindex = out.indexOf("]",sei);
					out.delete(index,endtagindex+1);	//remove sql not being used
				}
			}
		}
		//return the output
		return out.toString();
	}
	/**
	 * Override <code>Object.equals</code>. 
	 * @param o
	 * @return 
	 */
	@Override
	public boolean equals(Object o)
	{
		if ( o instanceof Template )
		{
			Template t = (Template)o;
			return t.getTemplateName().equals(templateName);
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		int hash = 3;
		hash = 73 * hash + (this.templateName != null ? this.templateName.hashCode() : 0);
		return hash;
	}
}