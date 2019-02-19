/*s
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/servlet/MObjectServlet.java,v $
 * $Id: MObjectServlet.java,v 1.37 2011/07/01 16:11:30 levent Exp $
 *
 * Copyright 2002 Xanboo, Inc.
 *
 */


package com.xanboo.core.sdk.servlet;


import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.xanboo.core.security.XanbooPrincipal;
import com.xanboo.core.model.XanbooBinaryContent;
import com.xanboo.core.util.*;
import com.xanboo.core.sdk.device.*;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.util.fs.AbstractFSProvider;


/**
 * Servlet implementation to retrieve binary managed object values.
 * <p>
 * The MOBjectServlet allows the retrieval of a managed object's binary data via HTTP request. An http request to retrieve binary data should be formated something like:
 * <PRE>http://tin.corecam.com/testsuite/xanboo/device/device.mobject?d=_camera_manager_1&g=f88fdf5c845e45f986e84b7c615a0375&o=1025</PRE>
 * There are 3 parameters supplied - the gateway GUID (g), the device GUID (d), and the managed object ID (o). <br>
 * There must also be a valid XanbooPrincipal object stored in the session attribute named 'XUSR'. This session attribute name may be overriden using a
 * servlet init parameter named 'session.key'.
 *
 * </p>
 */
// Note that we now internally support res=160x120 and ft=wbmp attributes - not yet documented
public class MObjectServlet extends HttpServlet {
    private Logger logger=null;
    
    private DeviceManager dManager=null;
    
    private String imageDir = null;    //path to image resources used only on case of errors
    private String sessionKey = null;
    private EjbProxy proxy = null;
    /**
     * Servlet initialization method
     */
    public void init() throws ServletException{
        try {
            logger=LoggerFactory.getLogger(this.getClass().getName());
            if(logger.isDebugEnabled()) {
                logger.debug("[init()]:");
            }
            
            // get directory for default default images
            imageDir = getServletContext().getRealPath( getInitParameter("default-image-dir") );
            sessionKey=getInitParameter("session.key");
            if (sessionKey==null) sessionKey="XUSR";
            
            getEJB(GlobalNames.EJB_DEVICE_MANAGER);
            
        }catch(Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[init()]: Exception:" + e.getMessage(), e);
            }else{
                logger.error("[init()]: Exception:" + e.getMessage(), e);
            }
        }
    }

    
    /**
     * Gets refrence(s) to EJB objects used by this servlet, only if there was no valid refrence
     *
     */
    private void getEJB(String jndiName) {
        if(logger.isDebugEnabled()) {
            logger.debug("[getEJB()]:");
        } 
        
     
            if(proxy == null) proxy = new EjbProxy(GlobalNames.SDK_JNDI_CONTEXT_FACTORY, GlobalNames.SDK_JNDI_PROVIDER_URL);
            dManager = (DeviceManager) proxy.getObj(jndiName);
            if(logger.isDebugEnabled()) {
                logger.debug("[getEJB()]: device manager EJB ref. was null, now it is set");
            } 
     
    }
    
    
    /** Handles HTTP <code>GET</code> requests.
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
        doPost(request, response);
    }
    
    
    /**
     * Servlet POST request handler
     * the request must include the following parameters:
     * g: gatewayGUID of the device
     * d: ID of the device
     * o: managed objectd id (oid) to retrieve the value for
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        if(logger.isDebugEnabled()) {
            logger.debug("[doPost()]:");
        }
        getEJB(GlobalNames.EJB_DEVICE_MANAGER);
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

        if(xp==null) {
            response.reset();
            response.sendError(response.SC_FORBIDDEN);
            return;
        }
        
        
        try {
            boolean isValid = this.isValidRequest(request);
            if(!isValid) {
                if(!response.isCommitted()) {
                    response.reset();
                    response.sendError(response.SC_NOT_FOUND);  // Failed to process request
                }
                return;
            }

            String contentType = null;
            
            String gg = request.getParameter("g");
            String dg = request.getParameter("d");
            String oid = request.getParameter("o");
            String returnLastModified = request.getParameter("lm");
            
            XanbooBinaryContent xb = dManager.getMObjectBinary(xp, gg, dg, oid);
            if(xb==null || xb.getBinaryContent()==null) {
                if(!response.isCommitted()) {
                    response.reset();
                    response.sendError(response.SC_NOT_FOUND);  // Failed to process request
                }
                return;
            }else {
                if(returnLastModified!=null) {  //if last-modified header requested thru "lm" param
                    String lastUpdate = xb.getLastModified();
                    if(lastUpdate!=null && lastUpdate.length()>18) {
                        //send it as a header !
                        SimpleDateFormat formatter= new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
                        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
                        Date lastU = formatter.parse(lastUpdate);
                        response.setDateHeader("Last-Modified", lastU.getTime());
                    }
                }
                
                byte[] fileBytes =  xb.getBinaryContent();
                this.sendBytes(response, fileBytes, xb.getContentType(), "mobject_"+dg+"_"+oid+".bin");
                xb.setBinaryContent(null);
            }

        } catch(Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[doGet()]:" + e.getMessage(), e);
            }
            
            if (!response.isCommitted()) {
                response.reset();
                response.sendError(response.SC_NOT_FOUND);  // Failed to process request
            }
        }
    }
    
    
    /**
     * Validates and saves the required request parameters
     * @param request HTTP request that contains user and appliance information
     *
     * @return true is request contained: domain id, account id, gateway GUID, and device GUID
     *
     */
    private boolean isValidRequest(HttpServletRequest request) {
        boolean isValid = false;
        try {
            if(logger.isDebugEnabled()) {
                logger.debug("[isValidRequest()]:");
            }
            
            if(request.getSession(false).getAttribute( "SESSION_LAST_HIT" ) != null ) {
                long lastHit = ((Long) request.getSession().getAttribute( "SESSION_LAST_HIT" ) ).longValue();
                long now = System.currentTimeMillis();
                int timeout = request.getSession().getMaxInactiveInterval() * 1000;
                if((now - lastHit) > (timeout)) {
                    request.getSession().invalidate();
                    return false;
                }
            }
            
            //get domain id and account id, gateway guid and device guid from request
            XanbooPrincipal xp=(XanbooPrincipal)request.getSession().getAttribute(sessionKey);
            if(xp==null) return false;
            String domainId = xp.getDomain();
            long accountId = xp.getAccountId();
            
            String deviceGUID = request.getParameter("d");
            String gatewayGUID = request.getParameter("g");
            String MObjectId = request.getParameter("o");
            String contentType = request.getParameter("t");
            
            //Allow backward compatibility with UI that does not supply the contentType parameter - to be removed
            if ( contentType == null ) {
                contentType = "image/jpeg";
            }
            
            if(accountId <= 0) return false;
            
            if( !XanbooUtil.isValidParameter( deviceGUID, new String[]{".", "\\", "~", "'", "`"} ) ||
            !XanbooUtil.isValidParameter( gatewayGUID, new String[]{"..", "\\", "/", "~", "'", "`" } ) ||
            !XanbooUtil.isValidParameter( MObjectId, new String[]{"..", "\\", "~", "'", "`" } ) ||
            !XanbooUtil.isValidParameter( contentType, new String[]{"..", "\\", "~", "'", "`" } ) ){
                if(logger.isDebugEnabled()) {
                    logger.error( "[isValidRequest] - invalid request param:" + gatewayGUID + ":" + deviceGUID + ":" + MObjectId + ":" + contentType );
                }
                return false;
            }
            
            //success
            isValid = true;
            
        } catch (Exception e) {
            if(logger.isDebugEnabled()) {
                logger.error("[isValidRequest()]: Exception: " + e.getMessage(), e);
            }
        }
        return isValid;
    }
    
    
    /**
     * Writes an array of bytes into the response stream
     * @param response HTTP response that will carry the image file to a browser
     * @param bytes A byte array that contains the content
     *
     * @throws XanbooException
     */
    private void sendBytes(HttpServletResponse response, byte[] fileBytes, String contentType, String fName) throws XanbooException {
        ServletOutputStream out = null;
        try {
            if(logger.isDebugEnabled()) {
                logger.debug("[sendBytes()]: len:" + fileBytes.length + ", type:" + contentType);
            }else {
                logger.info("****[sendBytes()]: len:" + fileBytes.length + ", type:" + contentType);

            }

            //send anti-cache headers
            //response.setHeader( "Pragma", "no-cache" );
            response.setHeader( "Cache-Control", "private, must-revalidate" );
            response.setHeader( "Expires", "-1" );
            
            //if(fName!=null) {
            //    response.setHeader( "Content-disposition", "attachment; filename=" + fName );
            //}
            
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
