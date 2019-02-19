/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.sdk.pai;

import java.io.StringWriter;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;

import java.io.Writer;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import javax.ejb.CreateException;
import javax.ejb.Remote;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.att.dlife.httplib.http.XanbooHttpHeader;
import com.att.dlife.httplib.http.XanbooHttpRESTConnector;
import com.att.dlife.httplib.http.XanbooHttpResponse;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooAdminPrincipal;
import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;
import com.xanboo.core.util.XanbooException;
import com.xanboo.pai.server.xml.Cmd;
import com.xanboo.pai.server.xml.CmdDataType;
import com.xanboo.pai.server.xml.CmdHeaderType;


/**
 *
 * @author lm899p
 */
@Remote (PAICommandREST.class)
@Stateless (name="PAICommandREST")
@TransactionManagement( TransactionManagementType.BEAN )
public class PAICommandRESTEJB  
{
    private Logger logger = null;
    private PAICommandREST_DAO dao = null;
    private SimpleDateFormat gmtDtFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    JAXBContext context = null;
    private List<MarshallerWrapper> marshallerWrapperList = new ArrayList<MarshallerWrapper>();
    
    @PostConstruct
    public void init() throws RemoteException, CreateException 
    {
        gmtDtFmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        try 
        {
            // get a logger (lazy)
            if(logger==null) 
            {
                logger=LoggerFactory.getLogger(this.getClass().getName());
            }
            if(logger.isDebugEnabled()) 
            {
                logger.debug("[init()]:");
            }
            
            dao = new PAICommandREST_DAO();

        }
        catch (Exception se) 
        {
            if(logger.isDebugEnabled()) 
            {
                logger.debug("[init()]: Failed  @init PAICommandRESTEJB:Exception ", se);
            }
            throw new CreateException("Failed @ init PAICommandRESTEJB:" + se.getLocalizedMessage());
        }
    }
    
  
    
    /**
     *  
     * @param xCaller
     * @param gatewayGuid - xhdr element
     * @param deviceGuid - xhdr element
     * @param timestamp - xhdr element
     * @param comandQueueId - xhdr element
     * @param parameters - xdata element(s). The valid keys are "PARAM", and "VALUE"
     * @throws XanbooException 
     */
    public String sendSetObject(XanbooAdminPrincipal xCaller,String gatewayGuid,String deviceGuid,Date timestamp,Long commandQueueId,List<HashMap> parameters)throws XanbooException
    {
        XanbooResultSet entry = getEntry(gatewayGuid);
        return sendSetObject(xCaller,gatewayGuid,deviceGuid,timestamp,commandQueueId,
                             entry.getElementString(0, "LOCAL_PAISERVER_URI"),entry.getElementString(0,"ACCESS_TOKEN"),parameters);
    }
    /**
     * 
     * @param xCaller
     * @param gatewayGuid
     * @param deviceGuid
     * @param timestamp
     * @param comandQueueId
     * @param paiServerUrl
     * @param paiAccessToken
     * @param parameters
     * @return
     * @throws XanbooException 
     */
    public String sendSetObject(XanbooAdminPrincipal xCaller,String gatewayGuid,String deviceGuid,Date timestamp,Long comandQueueId,
                                String paiServerUrl,String paiAccessToken,List<HashMap> parameters) throws XanbooException
    {
        if ( logger.isDebugEnabled() )
            logger.debug("[sendSetObject] - sending command to "+gatewayGuid+" via URL "+paiServerUrl+" using token="+paiAccessToken);
        String prefix="_";
        Cmd cmd = new Cmd();
        CmdHeaderType cmdHdr = new CmdHeaderType();
        cmd.setXhdr(cmdHdr);
        cmdHdr.setGguid(gatewayGuid);
        cmdHdr.setDguid(deviceGuid);
        cmdHdr.setTimestamp(gmtDtFmt.format(timestamp));
        cmdHdr.setCqid(BigInteger.valueOf(comandQueueId));
        if ( parameters.size() == 1 )
        {
            cmdHdr.setAction("setObject");
            HashMap<String,String>params = parameters.get(0);
            //oid 
            CmdDataType data = new CmdDataType();
            cmd.getXdata().add(data);
            data.setParam("oid");
            data.setValue(params.get("PARAM"));
            data.setPrefix(prefix);
            //value
            data = new CmdDataType();
            cmd.getXdata().add(data);
            data.setParam("value");
            data.setValue(params.get("VALUE"));
            data.setPrefix(prefix);
        }
        else
        {
            cmdHdr.setAction("setObjectList");
            prefix = "_oid";
            for ( HashMap<String,String> params : parameters )
            {
                CmdDataType data = new CmdDataType();
                cmd.getXdata().add(data);
                data.setParam(params.get("PARAM"));
                data.setValue(params.get("VALUE"));
                data.setPrefix(prefix);
            }
        }
        
        String xmlCmd = objectToXML(cmd);
        if ( logger.isDebugEnabled() )
            logger.debug("[sendSetObject] - XML sent to PAI server : "+xmlCmd);
        
        try
        {
            StringBuilder urlBldr = new StringBuilder(paiServerUrl);
            urlBldr.append("/sendCommand/");
            if ( logger.isDebugEnabled())
                logger.debug("[sendSetObject] - URL String for disconnect command - "+urlBldr.toString());
            
            XanbooHttpHeader<String,String> header = new XanbooHttpHeader<String,String>();
            header.put(XanbooHttpHeader.AUTHORIZATION, "token:"+paiAccessToken);
            header.put(XanbooHttpHeader.CONTENT_TYPE, "application/xml");
            XanbooHttpRESTConnector restConnector = new XanbooHttpRESTConnector();
            //restConnector.postRequest(String server,XanbooHttpHeader<String,String> headerMap,Map<String, String> params, String payload,Boolean createResponse);
            XanbooHttpResponse response = restConnector.postRequest(urlBldr.toString(),header,null,xmlCmd,true);
            if(logger.isDebugEnabled()) {
                logger.debug("[sendSetObject] - "+prefix+" command sent to "+gatewayGuid+", status="+response.getResponseCode()+" "+response.getContent());
            }
            return response.getContent();
        }
        catch(XanbooException ex)
        {
            logger.warn("[sendSetObject] - exception sending "+cmd.getXhdr().getAction()+" command for "+gatewayGuid,ex);
            throw ex;
        }
        catch(Exception ex)
        {
            String errorStatus = "error";
            if ( ex instanceof UnknownHostException )
            {
                errorStatus="unknownhost";
            }
            if ( ex instanceof NoRouteToHostException )
            {
                errorStatus="noroute";
            }
            if ( ex instanceof ConnectException )
            {
                errorStatus="connect_error";
            }
            //if the error status is not the generic "error", there was some issue sending message to the gateway
            //using the PAI server on the registry, delete the registry entry
            if ( !errorStatus.equals("error"))  
            {
                logger.info("[sendSetObject] - deleting registry entry for "+gatewayGuid+" / "+paiAccessToken+" due to "+ex.toString());
                deleteEntry(gatewayGuid,paiAccessToken);
            }
            logger.warn("[sendSetObject] - exception sending "+cmd.getXhdr().getAction()+" command for "+gatewayGuid,ex);
            return errorStatus;
        }
    }
    /**
     * Method called by the SDK to send the disconnect command to the gateway.
     * @param xCaller
     * @param gguid
     * @param reconnect
     * @throws XanbooException 
     */
    public void sendDisconnect(XanbooAdminPrincipal xCaller,String gguid,boolean reconnect) throws XanbooException
    {
        logger.info("[sendDisconnect] - sending disconnect command to gguid : "+gguid);
        String token = null;
        try
        {
            XanbooResultSet entry = getEntry(gguid);
            if ( entry.size() == 0 )
            {
                return; //no registry entry exists for the gguid
            }
            token = entry.getElementString(0,"ACCESS_TOKEN");
            StringBuilder bldr = new StringBuilder(entry.getElementString(0, "LOCAL_PAISERVER_URI"));
            bldr.append("/sendCloseCommand/").append(gguid);
            if ( logger.isDebugEnabled())
                logger.debug("[sendDisconnect] - URL String for disconnect command - "+bldr.toString());

            HashMap<String,String> params = new HashMap<String,String>();
            params.put("reconnect",""+reconnect);
            XanbooHttpHeader<String,String> header = new XanbooHttpHeader<String,String>();
            header.put(XanbooHttpHeader.AUTHORIZATION, "token:"+token);
            XanbooHttpRESTConnector restConnector = new XanbooHttpRESTConnector();
            //restConnector.postRequest(String server,XanbooHttpHeader<String,String> headerMap,Map<String, String> params, String payload,Boolean createResponse);
            XanbooHttpResponse response = restConnector.postRequest(bldr.toString(),header,params,"disconnect",true);
            logger.info("[sendDisconnect] - disconnect command sent, status="+response.getResponseCode()+" "+response.getContent());
        }
        catch(XanbooException ex)
        {
            logger.warn("[sendDisconnect] - exception sending disconnect command for "+gguid,ex);
        }
        catch(Exception ex)
        {
            String errorStatus = "error";
            if ( ex instanceof UnknownHostException )
            {
                errorStatus="unknownhost";
            }
            if ( ex instanceof NoRouteToHostException )
            {
                errorStatus="noroute";
            }
            if ( ex instanceof ConnectException )
            {
                errorStatus="connect_error";
            }
            //if the error status is not the generic "error", there was some issue sending message to the gateway
            //using the PAI server on the registry, delete the registry entry
            if ( !errorStatus.equals("error"))  
            {
                logger.info("[sendDisconnect] - deleting registry entry for "+gguid+"/"+token+" due to "+ex.toString());
                deleteEntry(gguid,token);
            }
            logger.warn("[sendDisconnect] - exception sending disconnect command for "+gguid,ex);
        }   
    }
    
    public void updateCommandStatus(XanbooAdminPrincipal xCaller,Long commandQueueId)throws XanbooException
    {
        if ( logger.isDebugEnabled() )
            logger.debug("[updateCommandStatus] - updating command status to \"1\" for "+commandQueueId);
        Connection conn=null;
        try 
        {
            
            conn=dao.getConnection();
            dao.updateCommandStatus(conn, commandQueueId);
            
        }
        catch (XanbooException xe) 
        {
            logger.warn("[updateCommandStatus] - exception updating commandqueue status for "+commandQueueId,xe);
            throw xe;
        }
        finally 
        {
            dao.closeConnection(conn, false);
        } 
    }
    
    private void deleteEntry(String gatewayGuid,String tokenId)throws XanbooException
    {
        if ( logger.isDebugEnabled() )
            logger.debug("[deleteEntry] - deleting registry entry for "+gatewayGuid);
        Connection conn=null;
        try 
        {
            
            conn=dao.getConnection();
            
            dao.deleteEntry(conn, gatewayGuid,tokenId);
            
        }
        catch (XanbooException xe) 
        {
            logger.warn("[deleteEntry] - exception deleting registry entry for "+gatewayGuid);
            throw xe;
        }
        finally 
        {
            dao.closeConnection(conn, false);
        } 
    }
    
    private XanbooResultSet getEntry(String gatewayGuid) throws XanbooException
    {
        if ( logger.isDebugEnabled() )
            logger.debug("[getEntry] - retrieve registry entry for "+gatewayGuid);
        Connection conn=null;
        try 
        {
            
            conn=dao.getConnection();
            XanbooResultSet entryResultSet = dao.getEntry(conn, gatewayGuid);
            if ( logger.isDebugEnabled() )
            {
                if ( entryResultSet.size() == 0 ) logger.debug("[getEntry] - no registry entry for "+gatewayGuid);
                else logger.debug("[getEntry] - found registry entry for "+gatewayGuid);
            }
            return entryResultSet;
            
        }
        catch (XanbooException xe) 
        {
            logger.warn("[getEntry] - exception when attempting to retrieve registry entry for "+gatewayGuid,xe);
            throw xe;
        }
        finally 
        {
            dao.closeConnection(conn, false);
        } 
    }
    
    private String objectToXML(Cmd xailCmd)
    {
        try
        {
            StringWriter writer = new StringWriter();
            /*if ( context == null )
                context = JAXBContext.newInstance("com.xanboo.pai.server.xml");
            context.createMarshaller().marshal(xailCmd, writer);
            * */
            MarshallerWrapper wrapper = getWrapperFromPool();
            wrapper.marshal(xailCmd, writer);
            poolWrapper(wrapper);
            String xmlString = writer.toString();
            //System.out.println("PAI XML Data : "+xmlString);
            return xmlString;
        }
        catch (JAXBException ex)
        {
            logger.warn("[objectToXML] - Error marshalling XML "+ex);
            return null;
        }
    }
    
    private synchronized MarshallerWrapper getWrapperFromPool() throws JAXBException
    {
        //if there is at least one member of the pool,
        //remove the first wrapper from the pool and return it to the caller
        if ( marshallerWrapperList.size() > 1 )
        {
            //remove it from the pool. The wrapper cannot be used by multiple threads simultaneously
            MarshallerWrapper wrapper = marshallerWrapperList.remove(0);
            wrapper.setInUse(Boolean.TRUE);
            return wrapper;
        }
        //if the pool is completely empty, create a new marshaller and wrapper
        //not added to the pool here
        if ( context == null )
            context = JAXBContext.newInstance("com.xanboo.pai.server.xml");
        Marshaller marshaller = context.createMarshaller();
        MarshallerWrapper wrapper = new MarshallerWrapper(marshaller);
        wrapper.setInUse(Boolean.TRUE);
        return wrapper;
    }
    private void poolWrapper(MarshallerWrapper wrapper)
    {
        wrapper.setInUse(Boolean.FALSE);
        //if the size of the pool is less than ten, add the wrapper to the pool
        if ( marshallerWrapperList.size() < 10 )
        {
            marshallerWrapperList.add(wrapper);
            return;
        }
        else
        {
            wrapper.destroy();
        }
    }
       
    class MarshallerWrapper 
    {
        boolean isInUse = false;
        long lastUsed = -1l;
        Marshaller marshaller = null;
        public MarshallerWrapper(Marshaller m)
        {
            this.marshaller = m;
            lastUsed = System.currentTimeMillis();
        }
        public boolean isInUse()
        {
            return this.isInUse;
        }
        public void setInUse(Boolean use)
        {
            this.isInUse = use;
        }
        public void marshal(Cmd xailCmd,Writer writer)throws JAXBException
        {
            this.marshaller.marshal(xailCmd, writer);
            this.lastUsed = System.currentTimeMillis();
        }
        public void destroy()
        {
            marshaller = null;
        }
    }
}
