/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/servlet/InvitationItemServlet.java,v $
 * $Id: InvitationItemServlet.java,v 1.27 2011/07/01 16:11:30 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */
 
package com.xanboo.core.sdk.servlet;           

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.xanboo.core.model.XanbooItem;
import com.xanboo.core.model.XanbooBinaryContent;
import com.xanboo.core.sdk.invitation.InvitationManager;
import com.xanboo.core.sdk.inbox.InboxManager;
import com.xanboo.core.security.XanbooPrincipal;

import com.xanboo.core.util.*;
import com.xanboo.core.util.fs.AbstractFSProvider;

/** 
 * The ItemServlet allows the retrieval of binary item data that is associated with an invitation item.
 * <br>
 * <p> When an invitation item, or an invitation item list is retrieved (through InvitationManager), there are a number of parameters associated with each
 * item which may be passed as GET parameters to this servlet to retrieve it's associated binary data.</p>
 *
 * <p>On receipt of such a request, this servlet will pass through the binary data associated with the requested item. It is generally good practice
 * not to call this servlet for items which do not have associated binary data, but in the event that this does happen, a default thumbnail will be returned.<br>
 * It is the responsibility of the UI to assign an appropriate viewer for the binary data.
 * </p>
 * A request to the servlet for an image/jpeg type item would be formatted as follows:
 *  <b><PRE>&lt;img src="item.invite?i=1002&k=eae54c23eadb0640f0a0c2add69b960152972af416&fi=1617"&gt; </PRE></b>
 * The request contains the the URI of the servlet, along with 3 URL Encoded get parameters named i, k and fi.
 * The parameters to pass in the get string may come from one of three places - either a XanbooItem object resulted from a getInvitationItemByKey() call, or from columns in a 
 * XanbooResultSet returned by a getInvitationItemListByKey() call, or from a parameter in the original invitation url.
 * The correct parameters/columns to use for the get parameter values in both cases are:
 *
 *<br>
 * <ul>
 *  <li> <b> i </b> <br>
 *      This must be the same value of the 'i' parameter supplied with the http request for the invitation.
 *  </li>
 *  <li> <b> k </b> <br>
 *      As with 'i', this value is supplied with with the originating invitation request.
 *  </li>
 *  <li> <b> fi </b> <br>
 *      From either XanbooItem.getItemId() or from the column named 'FOLDERITEM_ID'
 *  </li>
 *  <li> <b> t </b> <br>
 *      This is an optional parameter, which if present, instructs the servlet to return the thumbnail for the item. (eg. &t=1)
 *  </li>
 * </ul>
 * 
 * When configured in the applications web.xml, this servlet has a parameter named 'default-image-dir'. This parameter should point to a directory which contains the deault
 * thumbnail files which are used when an item cannot have an appropriate thumbnail generated from it's own content. The directory should contain your default icons with
 * the following names:
 *<ul>
 *  <li> thumb_default.gif </li>
 *  <li> thumb_image.gif </li>
 *  <li> thumb_video.gif </li>
 *  <li>thumb_audio.gif </li>
 *</ul>
 *
 *If the file type is not of image, video or audio, the default image file is returned.
 * <br>
 * Please remember to URL encode the values of all get parameters using java.net.URLEncoder.encode()
 */
public class InvitationItemServlet extends HttpServlet {
   
    private InvitationManager iManager=null;
    private InboxManager ibManager=null;

    private Logger logger;
    private String baseDir=null;
    
    private EjbProxy proxy = null;
    
    /** Initializes the servlet.
    */  
    public void init() throws ServletException {
      try {
        logger=LoggerFactory.getLogger(this.getClass().getName());
        if(logger.isDebugEnabled()) {
           logger.debug("[init()]:");
        }

        baseDir = getServletContext().getRealPath( getInitParameter("default-image-dir") );
        
        getEJB();
        
      }catch(Exception e) {
           if(logger.isDebugEnabled()) {
              logger.error("[init()]: Exception:" + e.getMessage(), e);
           }else {
              logger.error("[init()]: Exception:" + e.getMessage());
           }
      }    
    }
    
    
    /**
     * Returns an EJB reference to the EJB specified with a JNDI name, if necessary
     */
    public void getEJB()  {
        

     
        	 if(proxy==null) proxy = new EjbProxy(GlobalNames.SDK_JNDI_CONTEXT_FACTORY, GlobalNames.SDK_JNDI_PROVIDER_URL);
           try {
               iManager = (InvitationManager) proxy.getObj(GlobalNames.EJB_INVITATION_MANAGER);
               ibManager = (InboxManager) proxy.getObj(GlobalNames.EJB_INBOX_MANAGER);
           }catch(Exception e) {
               // throw new XanbooException(2000, "Exception getting reference to '" + GlobalNames.EJB_INVITATION_MANAGER + "'");
        	   if(logger.isDebugEnabled()) {
                   logger.error("[init()]: Exception:" + e.getMessage(), e);
                }else {
                   logger.error("[init()]: Exception:" + e.getMessage());
                }
           }
 
    }


    /** Handles HTTP <code>GET</code> requests.
    */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
                                        throws ServletException, java.io.IOException {
        doPost(request, response);
    } 
    
    
    /** Handles HTTP <code>POST</code> requests.
    */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
                                        throws ServletException, java.io.IOException {
        if(logger.isDebugEnabled()) {
            logger.debug("[doGet()]:");
        }
        getEJB();
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

        try {
            // if valid request
            if(request.getParameter("i")!=null && request.getParameter("k")!=null && request.getParameter("fi")!=null) {
             //   getEJB();
                long invitationId = Long.parseLong(request.getParameter("i"));
                String viewKey = request.getParameter("k");
                long folderItemId = Long.parseLong(request.getParameter("fi"));
                XanbooItem item = iManager.getInvitationItemByKey( invitationId, viewKey, folderItemId );
                sendItem(response, item, request.getParameter("t") );
            }else {
                if(!response.isCommitted()) {
                  response.reset();
                  response.sendError(response.SC_NOT_FOUND);  // Failed to process request 
                }
            }
        }catch(Exception e) {
           if(logger.isDebugEnabled()) {
              logger.error("[doGet()]: Exception:" + e.getMessage(), e);
           }      

           if(!response.isCommitted()) {
                response.reset();
                response.sendError(response.SC_NOT_FOUND);  // Failed to process request
            }
        }
        
    }

    
    /** Returns a short description of the servlet.
    */
    public String getServletInfo() {
        return "Returns invitation item data";
    }
    
    /**
     * Writes a file into the response stream 
     * @param response HTTP response that will carry the image file to a browser
     *
     * @throws XanbooException
     */
    private void sendItem(HttpServletResponse response, XanbooItem item, String sendThumb) throws Exception {
      try {
          if(logger.isDebugEnabled()) {
              logger.debug("[sendItem()]:");
          }
          
          String filePath;
          String contentType = null;

          XanbooPrincipal xp = new XanbooPrincipal(item.getDomain(), null, item.getAccountId(), -1);  //dummy principal to retrieve item binary
          XanbooBinaryContent xb = ibManager.getItemBinary(xp, item.getMount(), item.getItemDirectory(), item.getItemFilename(), (sendThumb != null));
          if(xb!=null && xb.getBinaryContent()!=null) {
                 byte[] fileBytes =  xb.getBinaryContent();
                 this.sendBytes(response, fileBytes, item.getItemType(), false, null);
                 xb.setBinaryContent(null);
                 return;
          }else {
                 response.reset();
                 response.sendError(response.SC_NOT_FOUND);  // Failed to process request
          }
          
      }catch(Exception e) {
          throw e;
      }

    }
                    
    
    /**
     * Writes a file into the response stream 
     * @param response HTTP response that will carry the image file to a browser
     * @param file A File object for the file to be sent
     *
     * @throws XanbooException
     */
/****    
    private void sendFile(HttpServletResponse response, File f, String fileContentType) throws XanbooException {
      FileInputStream fis = null;
      ServletOutputStream out = null;
      try {
          if(logger.isDebugEnabled()) {
              logger.debug("[sendFile()]:");
           }
          
          int fileLength = (int) f.length();
          fis = new FileInputStream(f);
          
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

    
    private File getThumbFile( String contentType ) {
        try {
            File f = new File( baseDir + "/thumb_" + contentType.substring(0, contentType.indexOf("/") ) + ".gif" );
            if (!f.exists()) {
                f = new File( baseDir + "/thumb_default.gif");
                return f;
            } else {
                return f;
            }
        } catch (Exception e) {
                return (new File( baseDir + "/thumb_default.gif"));
        }
    }
*****/    
    

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