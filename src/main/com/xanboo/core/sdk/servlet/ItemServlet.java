/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/servlet/ItemServlet.java,v $
 * $Id: ItemServlet.java,v 1.35 2011/07/01 16:11:30 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */


package com.xanboo.core.sdk.servlet;


import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.util.*;
import com.xanboo.core.sdk.inbox.*;
import com.xanboo.core.model.XanbooBinaryContent;
import com.xanboo.core.util.fs.AbstractFSProvider;

/** 
 * The ItemServlet allows the retrieval of binary item data that is associated with inbox and folder items.
 * <br>
 * <p> When an item, or an item list is retrieved (through InboxManager or FolderManager), there are a number of parameters associated with each
 * item which may be passed as GET parameters to this servlet to retrieve it's associated binary data.</p>
 *
 * <p>On receipt of such a request, this servlet will pass through the binary data associated with the requested item. It is generally good practice
 * not to call this servlet for items which do not have associated binary data, but in the event that this does happen, a default thumbnail will be returned.<br>
 * It is the responsibility of the UI to assign an appropriate viewer for the binary data.</p>
 * A request to the servlet for an image/jpeg type item would be formatted as follows:
 *  <b><PRE>&lt;img src="getItem.item?m=data00&c=image%2Fjpeg&p=2002%2F0919&f=item_200209190226129187.jpg"&gt; </PRE></b>
 * The request contains the the URI of the servlet, along with 4 URL Encoded get parameters named m, c, p and f.
 * The parameters to pass in the get string may come from one of two places - either a XanbooItem object resulted from a getItem() call, or from columns in a 
 * XanbooResultSet returned by a getItemList() call. The correct parameters/columns to use for the get parameter values in both cases are:
 *<br>
 * <ul>
 *  <li> <b> m </b> <br>
 *      Either from XanbooItem.getMount() or from the column 'ITEM_MOUNT'
 *  </li>
 *  <li> <b> c </b> <br>
 *      Either from XanbooItem.getItemType() or from the column named 'ITEM_CONTENTTYPE'
 *  </li>
 *  <li> <b> p </b> <br>
 *      Either from XanbooItem.getItemDirectory() or from the column named 'ITEM_PATH'
 *  </li>
 *  <li> <b> f </b> <br>
 *      Either from XanbooItem.getItemFilename() or from the column named 'ITEM_FILE'
 *  </li>
 *  <li> <b> t </b> <br>
 *      This is an optional parameter, which if present, instructs the servlet to return the thumbnail for the item. (eg. &t=1)
 *  </li>
 * </ul>
 * 
 * <p>
 * There must also be a valid XanbooPrincipal object stored in the session attribute named 'XUSR'. This session attribute name may be overriden using a 
 * servlet init parameter named 'session.key'.
 * </p>
 * 
 * When configured in the applications web.xml, this servlet has a parameter named 'default-image-dir'. This parameter should point to a directory which contains the deault
 * thumbnail files which are used when an item cannot have an appropriate thumbnail generated from it's own content. The directory should contain your default icons with
 * the following names:
 *<ol>
 *  <li> thumb_default.gif </li>
 *  <li> thumb_image.gif </li>
 *  <li> thumb_video.gif </li>
 *  <li>thumb_audio.gif </li>
 *</ol>
 *
 *If the file type is not of image, video or audio, the default image file is returned.
 * <br>
 * Please remember to URL encode the values of all get parameters using java.net.URLEncoder.encode()
 */
public class ItemServlet extends HttpServlet {
    private Logger logger=null;
    
    private InboxManager iManager=null;

    private String imageDir = null;    //path to image resources used only on case of errors
    private String sessionKey = null;
    private EjbProxy proxy = null;
    
    /** Servlet initialization
     */
    public void init() throws ServletException{
        
      try {
        logger=LoggerFactory.getLogger(this.getClass().getName());
        if(logger.isDebugEnabled()) {
           logger.debug("[init()]:");
        }
        
        // get directory for default thumbnail icons
        imageDir = getServletContext().getRealPath( getInitParameter("default-image-dir") );
        sessionKey=getInitParameter("session.key");
        if (sessionKey==null) sessionKey="XUSR";

      initEJB();

        
      }catch(Exception e) {
           if(logger.isDebugEnabled()) {
              logger.error("[init()]: Exception:" + e.getMessage(), e);
           }else {
              logger.error("[init()]: Exception:" + e.getMessage());
           }
      }    
    }


	private void initEJB() {
		
		        if(proxy == null) proxy = new EjbProxy(GlobalNames.SDK_JNDI_CONTEXT_FACTORY, GlobalNames.SDK_JNDI_PROVIDER_URL);
		        iManager = (InboxManager) proxy.getObj(GlobalNames.EJB_INBOX_MANAGER);
		        if(logger.isDebugEnabled()) {
		            logger.debug("[getEJB()]: inbox manager EJB ref. was null, now it is set");
		        }
		 
	}

    
    /** Handles HTTP <code>GET</code> requests.
    */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
                                        throws ServletException, java.io.IOException {
        doPost(request, response);
    }     
    
    
    /** Servlet handler for POST requests
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) 
                throws ServletException, IOException { 
        if(logger.isDebugEnabled()) {
            logger.debug("[doGet()]:");
        }

        initEJB();
        
        // initialize the SDK, if not done yet
        if(!GlobalNames.isInitialized()) {
            try {
                SDKInitializer sdkInit = new SDKInitializer(getServletContext().getRealPath("/"), GlobalNames.SDK_JNDI_CONTEXT_FACTORY, GlobalNames.SDK_JNDI_PROVIDER_URL);
            }catch(Exception e) {
                if(logger.isDebugEnabled()) {
                    logger.debug("[init()]: Error initializing Core SDK parameters from DB, using defaults", e);
                }
            }
        }


        XanbooPrincipal xp = null;

        //caller XP from session?
        if(request.getSession(false) != null) {
            xp =  (XanbooPrincipal) request.getSession(false).getAttribute(sessionKey); 
        }

        //caller XP from request?
        if(xp==null) {
           xp = (XanbooPrincipal) request.getAttribute(sessionKey);
        }
        
        if(xp==null) {
            response.sendError(response.SC_FORBIDDEN);
            return;
        }
        
        
        try {
             boolean isValid = this.isValidRequest(xp, request);
             boolean isProcessed = false;
             if(isValid) {
                 isProcessed = processRequest(xp, false,request,response);   //don't use default
                 if(!isProcessed) {
                    isProcessed = processRequest(xp, true,request,response); //try again with default                
                 }
             }else { 
                 isProcessed = processRequest(xp, true,request,response); //use default                
             }

             if(!isProcessed) {
                if(!response.isCommitted()) {
                  response.reset();
                  response.sendError(response.SC_NOT_FOUND);  // Failed to process request 
                }
             }
             
        }catch(Exception e) {
            if(logger.isDebugEnabled()) {
               logger.error("[doGet()]: Exception:" + e.getMessage());
            }
            if (!response.isCommitted()) {
                response.reset();
                response.sendError(response.SC_NOT_FOUND);  // Failed to process request 
            }
        }
    }
    
    /** Processes a request
     * @param isUseDefault Boolean, set to false to get the requested files, or true if default images should be used instead.
     * @param response HttpServletResponse that will be used to return the file
     *
     * @return True if request was successfully processed or false if failed. 
     **/
    private boolean processRequest(XanbooPrincipal xp, boolean isUseDefault, HttpServletRequest request, 
                                                HttpServletResponse response) {
         boolean isProcessed = false;
         String filePath = null;
         try { 
             if(logger.isDebugEnabled()) {
               logger.debug("[processRequest()]:");
             }
                  

            //if app tier running seperate
            ///---if(!isUseDefault && GlobalNames.SDK_JNDI_PROVIDER_URL!=null && GlobalNames.SDK_JNDI_PROVIDER_URL.length()>0) {
            if(!isUseDefault) {
               String itemMount = request.getParameter("m");   //absolute mount point
               String fileName = request.getParameter("f"); //filename of item or thumb
               String itemDirectory = request.getParameter("p"); //item directory
               boolean isThumb = false;
               if(request.getParameter("t") != null) {
                   isThumb = true;
               }

               XanbooBinaryContent xb = iManager.getItemBinary(xp, itemMount, itemDirectory, fileName, isThumb);
               if(xb!=null && xb.getBinaryContent()!=null) {
                    byte[] fileBytes =  xb.getBinaryContent();
                    this.sendBytes(response, fileBytes, request.getParameter("c"), (request.getParameter("save") != null), fileName);
                    xb.setBinaryContent(null);
                    return true;
               }

            ///---}else if(!isUseDefault) {
            ///---   filePath = this.getItemPath(xp, request);
            }else {
               filePath = this.getDefaultPath(request);
            }

            if(filePath == null) return false;

            if(!isUseDefault) {
                 String fileContentType = request.getParameter("c"); //file content type
                 this.sendFile(response, filePath, fileContentType, request.getParameter("save") != null );
            }else {
                 this.sendFile(response, filePath, "image/gif", request.getParameter("save") != null );  // send default thumb w/ default thumb type
            }

            isProcessed = true;

        }catch(Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[processRequest()]: Exception:" + e.getMessage(), e);
            }         
        }     
        
        return isProcessed;
    }
    
    
    /**
     * Validates and saves the required request parameters
     * @param request HTTP request that contains user and appliance information
     *
     * @return true is request contained: domain id, account id, gateway GUID, and device GUID
     *
     */
    private boolean isValidRequest(XanbooPrincipal xp, HttpServletRequest request) {
       boolean isValid = false;
       try {
           if(logger.isDebugEnabled()) {
              logger.debug("[isValidRequest()]:");
           }

           //get domain id and account id, gateway guid and device guid from request
           String domainId = xp.getDomain();   
           long accountId = xp.getAccountId();
                      
           String itemMount = request.getParameter("m"); //absolute mount point
           //String itemType = "item";
           //if(request.getParameter("t") != null) {
           //    itemType = "thumb";
           //}
           
           String fileName = request.getParameter("f"); //filename of item or thumb
           String fileContentType = request.getParameter("c"); //file content type
           if (request.getParameter("c")==null){ fileContentType=""; }
           String itemDirectory = request.getParameter("p"); //item directory
    
           if(accountId <= 0) return false;
                      
           if( !XanbooUtil.isValidParameter( itemMount, new String[]{".", "\\", "~", "'", "`"} ) ||
                !XanbooUtil.isValidParameter( itemDirectory, new String[]{".", "\\", "~", "'", "`"} ) || 
                !XanbooUtil.isValidParameter( fileName, new String[]{"..", "\\", "/", "~", "'", "`" } ) ||
                !XanbooUtil.isValidParameter( fileContentType, new String[]{"..", "\\", "~", "'", "`" } )){
                    if(logger.isDebugEnabled()) {
                        logger.error( "[isValidRequest] - invalid request param:" + itemMount + ":" + itemDirectory + ":" + fileName );
                    }
                    return false;
                }
                
           //success
           isValid = true;
           
       }catch(Exception e) {   
            if(logger.isDebugEnabled()) {
              logger.error("[isValidRequest()]: Exception:" + e.getMessage(), e);
            }            
       }
       return isValid;
    }
    
    
    
    /**
     * Writes a file into the response stream 
     * @param request HTTP request that contains user and appliance information
     * @param response HTTP response that will carry the image file to a browser
     * @param filePath A string that contains the file path
     * @param fileContentType  a string that contains the file mime type
     *
     * @throws XanbooException
     */
    private void sendFile(HttpServletResponse response, String filePath, String fileContentType, boolean saveAs) throws XanbooException {
      FileInputStream fis = null;
      ServletOutputStream out = null;
      try {
          if(logger.isDebugEnabled()) {
              logger.debug("[sendFile()]:");
           }
          
          File f = new File(filePath);
          int fileLength = (int) f.length();
          fis = new FileInputStream(f);

          if( saveAs ) {
              response.setHeader( "Content-disposition", "attachment; filename=" + f.getName() );
          }
          
          response.setContentType(fileContentType);
          response.setContentLength(fileLength);
                    
          out = response.getOutputStream();
          
          final int MAX_BUFFER = 32768;
          byte[] buffer = new byte[MAX_BUFFER];
          int bytesRead = 0;
          int len = 0;
          response.setBufferSize(MAX_BUFFER);
          while ((len = fis.available()) > 0) {
              len=fis.read(buffer, 0, MAX_BUFFER);
              if(len==-1) break;
              out.write(buffer, 0, len); 
              out.flush();
              bytesRead += len;
              if (bytesRead >= fileLength) break;
          }
          
          if(bytesRead<fileLength) {
             if(logger.isDebugEnabled()) {
                logger.error("[sendFile()]: Could not send the whole file");
             }
          }
          
      }catch (Exception e) {   
            if(logger.isDebugEnabled()) {
              logger.error("[sendFile()]: Exception:" + e.getMessage(), e);
            }      
          throw new XanbooException(10050);
      }finally {
          try {
            if (fis != null) fis.close(); 
            if (out != null) out.close(); 
          }catch (IOException ioe) {
              if(logger.isDebugEnabled()) {
                 logger.error("[sendFile()]: IOException:", ioe);
              }    
          }
       }
    }
    
    /**
     * Gets the item or thumb file path 
     *
     * @param request HTTP request that contains user and appliance information
     *
     * @return A string that contains the item file path or null if it failed
     */
/****    
    private String getItemPath(XanbooPrincipal xp, HttpServletRequest request) {
        
       String itemPath = null;
        
       
       //get domain id and account id, gateway guid and device guid from request
       String domainId = xp.getDomain();   
       long accountId = xp.getAccountId();

       String itemMount = request.getParameter("m");   //absolute mount point
       //String itemType = "item";
       //if(request.getParameter("t") != null) {
       //    itemType = "thumb";
       //}
       boolean isThumb=false;
       if(request.getParameter("t") != null) {
           isThumb = true;
       }
       
       String fileName = request.getParameter("f"); //filename of item or thumb
       if(fileName==null || fileName.length()==0) return null;
       
       String itemDirectory = request.getParameter("p"); //item directory
        
       try {
          if(logger.isDebugEnabled()) {
             logger.debug("[getItemPath()]:");
          } 

          //itemPath = GlobalNames.getBaseAccountDir(itemMount, domainId, XanbooUtil.getAccountDir(accountId) ) +        
          //           "/" + itemType + "/" + itemDirectory + "/" + fileName + "/" ;  
          itemPath = AbstractFSProvider.getBaseDir(itemMount, domainId, accountId, (isThumb ? AbstractFSProvider.DIR_ACCOUNT_THUMB : AbstractFSProvider.DIR_ACCOUNT_ITEM)) + 
                     "/" + itemDirectory + "/" + fileName + "/" ; 
          
       }catch(Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getItemPath()]: Exception:" + e.getMessage(), e);
            }        
        }
        
        return itemPath;   
    }
****/
    
     /**
      * Gets the file path of a default thumb file, only in case of errors getting the item file
      *
      * @return A string that contains the thumb file path or null if it failed
      */
    private String getDefaultPath(HttpServletRequest request) {
        String thumbPath = null;
        try {
              if(logger.isDebugEnabled()) {
                 logger.debug("[getDefaultPath()]:");
              } 
              
              String thumbName = null;
              String fileContentType = request.getParameter("c"); //file content type
              if (request.getParameter("c")==null){ fileContentType=""; }
              
              if (fileContentType.indexOf( "-rejected" ) != -1 ) {
                  thumbName = "rejected";
              } else if(fileContentType.indexOf("image") != -1) { 
                  thumbName = "image";
              } else if (fileContentType.indexOf("video") != -1) {
                  thumbName = "video";
              } else if(fileContentType.indexOf("text") != -1) {
                  thumbName = "text";
              } else if(fileContentType.indexOf("audio") != -1) {
                  thumbName = "audio";
              } else {
                  thumbName = "default";
              }
              
              thumbPath = imageDir + "/thumb_" + thumbName + ".gif";

        }catch(Exception e) {
            if(logger.isDebugEnabled()) {
              logger.error("[getDefaultPath()]: Exception:" + e.getMessage(), e);
            }       
        }
        
        return thumbPath;
    }


    /**
     * Writes an array of bytes into the response stream
     * @param response HTTP response that will carry the image file to a browser
     * @param bytes A byte array that contains the content
     *
     * @throws XanbooException
     */
    private void sendBytes(HttpServletResponse response, byte[] fileBytes, String contentType, boolean saveAs, String fName) throws XanbooException {
        ServletOutputStream out = null;
        
        if(contentType==null) contentType="image/jpeg";

        try {
            if(logger.isDebugEnabled()) {
                logger.debug("[sendBytes()]: len:" + fileBytes.length + ", type:" + contentType);
            }else {
                logger.info("****[sendBytes()]: len:" + fileBytes.length + ", type:" + contentType);
            }

            if(saveAs) {
                response.setHeader( "Content-disposition", "attachment; filename=" + fName );
            }

            response.setContentType(contentType);
            response.setContentLength(fileBytes.length);
            response.setBufferSize(fileBytes.length);

            out = response.getOutputStream();
            out.write(fileBytes);
            out.flush();

        } catch (Exception e) {
            e.printStackTrace();
            if(logger.isDebugEnabled()) {
                logger.error("[sendBytes()]: Exception: ", e );
            } else {
                logger.error("[sendBytes()]: Exception: " + e.getMessage() );
            }

            throw new XanbooException(10050);
        } finally {
            try {
                if (out != null) out.close();
            } catch (IOException ioe) {
                if(logger.isDebugEnabled()) {
                    logger.error("[sendBytes()]: IOException: " , ioe);
                } else {
                    logger.error("[sendBytes()]: IOException: " + ioe.getLocalizedMessage());
                }

            }
        }
    }

}
