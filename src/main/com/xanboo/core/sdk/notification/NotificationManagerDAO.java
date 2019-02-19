/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.sdk.notification;

import java.sql.Connection;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.util.BaseDAO;
import com.xanboo.core.util.BaseHandlerDAO;
import com.xanboo.core.util.DAOFactory;
import com.xanboo.core.util.GlobalNames;
import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;
import com.xanboo.core.util.SQLParam;
import com.xanboo.core.util.XanbooException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.Properties;

/**
 * Data Access Object for Notification manager EJB
 * @author lm899p
 */
public class NotificationManagerDAO extends BaseHandlerDAO
{
    private BaseDAO dao;
    private Logger logger;
    
    public String DELETE_CHAIN = "XC_NOTIFICATION_PKG.DELETE_CHAIN";
    
    public NotificationManagerDAO()throws XanbooException
    {
         try 
         {
            // obtain a Logger instance
            logger=LoggerFactory.getLogger(this.getClass().getName());
            if(logger.isDebugEnabled()) 
            {
                logger.debug("[NotificationManagerDAO()]:");
            }

            // create implementation Class for Oracle, Sybase, etc.
            dao = (BaseDAO) DAOFactory.getDAO();
            
            // get the Connection factory DataSource for CoreDS
            getDataSource(GlobalNames.COREDS);
            
        }
        catch(XanbooException xe) 
        {
            throw xe;
        }
        catch(Exception ne) 
        {
            if(logger.isDebugEnabled()) 
            {
              logger.error("[NotificationManagerDAO()]: " + ne.getMessage(), ne);
            }
            else 
            {
              logger.error("[NotificationManagerDAO()]: " + ne.getMessage());
            }               
            throw new XanbooException(20014, "[NotificationManagerDAO()]: " + ne.getMessage() );
        }
    }
    /**
     * 
     * @param accountId
     * @param queueId
     * @param msgToken
     * @param contactId
     * @param conn
     * @throws XanbooException 
     */
    public void acknowledgeMessage(Long accountId,Long queueId,String msgToken,Long contactId,Connection conn)throws XanbooException
    {
        if(logger.isDebugEnabled()) 
        {
            logger.debug("[acknowledgeMessage()]: ");
        }

        SQLParam[] args=new SQLParam[4+2];     // 2 SP parameters + 2 std parameters (errno, errmsg)
        // setting IN params
        args[0] = new SQLParam(accountId,Types.BIGINT);   
        args[1] = new SQLParam("",Types.VARCHAR);
        args[2] = new SQLParam(queueId,Types.BIGINT);
        args[3] = new SQLParam(msgToken,Types.VARCHAR);
        
        dao.callSP(conn, "XC_NOTIFICATION_PKG.ACKNOWLEGE_MESSAGE", args, false);
    }
    /**
     * 
     * @param chainId
     * @param conn
     * @throws XanbooException 
     */
    public void deleteNotificationChain(Long chainId,Connection conn)throws XanbooException
    {
        if(logger.isDebugEnabled()) 
        {
            logger.debug("[deleteNotificationChain()]: ");
        }

        SQLParam[] args=new SQLParam[1+2];     // 2 SP parameters + 2 std parameters (errno, errmsg)
        // setting IN params
        args[0] = new SQLParam(chainId,Types.BIGINT);     
        
        dao.callSP(conn, DELETE_CHAIN, args, false);
    }
    /**
     * 
     * @param chainId
     * @param contactId
     * @param conn
     * @throws XanbooException 
     */
    public void deleteNotificationChainItem(long chainId, long contactId, Connection conn)throws XanbooException
    {
        if(logger.isDebugEnabled()) 
        {
            logger.debug("[deleteNotificationChainItem()]: ");
        }

        SQLParam[] args=new SQLParam[2+2];     // 2 SP parameters + 2 std parameters (errno, errmsg)
        // setting IN params
        args[0] = new SQLParam(chainId,Types.BIGINT);   
        args[1] = new SQLParam(contactId,Types.BIGINT);
        
        dao.callSP(conn, "XC_NOTIFICATION_PKG.DELETE_CHAIN_ITEM", args, false);
    }
    
    public void deleteNotificationChainItem(long accountId,long chainId,NotificationChainRecipient item, Connection conn)throws XanbooException
    {
        if ( logger.isDebugEnabled() )
            logger.debug("[deleteNotificationChainItem()]: ");
        
        SQLParam[] args=new SQLParam[5+2];     // 2 SP parameters + 2 std parameters (errno, errmsg)
        // setting IN params
        args[0] = new SQLParam(accountId,Types.BIGINT);
        args[1] = new SQLParam(chainId,Types.BIGINT);
        args[2] = (item.getContactId() != null && item.getContactId() != -1l) ? new SQLParam(item.getContactId(),Types.BIGINT) : new SQLParam(null,Types.NULL);
        args[3] = new SQLParam(item.getDestination(),Types.VARCHAR);
        args[4] = (item.getChainSeqNo() != null && item.getChainSeqNo() != -1) ? new SQLParam(item.getChainSeqNo(),Types.BIGINT) : new SQLParam(null,Types.NULL);
        
        dao.callSP(conn, "XC_NOTIFICATION_PKG.DELETE_CHAIN_ITEM", args, false);
    }
    
    /**
     * 
     * @param chainId
     * @param recipient
     * @param conn
     * @throws XanbooException 
     */
    public void newNotificationChainItem(long chainId,NotificationChainRecipient recipient,Connection conn)throws XanbooException
    {
        if(logger.isDebugEnabled()) 
        {
            logger.debug("[newNotificationChainItem()]: ");
        }

        SQLParam[] args=new SQLParam[5+2];     // 2 SP parameters + 2 std parameters (errno, errmsg)
        // setting IN params
        args[0] = new SQLParam(chainId,Types.BIGINT);   
        if ( recipient.getUserId() == null || recipient.getUserId() == -1 )
            args[1] = new SQLParam(null,Types.NULL);
        else
            args[1] = new SQLParam(recipient.getUserId(),Types.BIGINT);
        if ( recipient.getContactId() == null || recipient.getContactId() == -1 )
            args[2] = new SQLParam(null,Types.NULL);
        else
            args[2] = new SQLParam(recipient.getContactId(),Types.BIGINT);
        args[3] = new SQLParam(recipient.getWaitTime(),Types.INTEGER);
        args[4] = new SQLParam(recipient.getDestination().getDBValue(),Types.VARCHAR);
        
        dao.callSP(conn, "XC_NOTIFICATION_PKG.INSERT_CHAIN_RECIPIENT", args, false);
    }
    /**
     * 
     * @param accountId
     * @param conn
     * @return
     * @throws XanbooException 
     */
    public List<NotificationChain> getNotificationChainList(long accountId,Connection conn)throws XanbooException
    {
        if(logger.isDebugEnabled()) 
        {
            logger.debug("[getNotificationChainList()]: ");
        }

        SQLParam[] args=new SQLParam[1+2];     // 1 SP parameter + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);

        XanbooResultSet rs = dao.callSP(conn, "XC_NOTIFICATION_PKG.GET_CHAINS", args);
        ArrayList<NotificationChain> chains = new ArrayList<NotificationChain>(rs.size());
        for ( int i = 0; i < rs.size(); i++)
        {
            long chainId = rs.getElementLong(i, "CHAIN_ID");
            long acctId = rs.getElementLong(i,"ACCOUNT_ID");
            String type = rs.getElementString(i, "QUIET_TIME_TYPE");
            boolean sendToSBN = rs.getElementString(i, "SEND_TO_SBN").equals("1");
            boolean useAsDefault = rs.getElementString(i, "USE_AS_DEFAULT").equals("1");
            String descr = rs.getElementString(i,"DESCRIPTION");
            chains.add(new NotificationChain(chainId,acctId,sendToSBN,useAsDefault,type,descr));
        }
        return chains;
    }
    /**
     * 
     * @param chainId
     * @param conn
     * @return
     * @throws XanbooException 
     */
    public List<NotificationChainRecipient> getNotificationChainItemList(long chainId,Connection conn)throws XanbooException
    {
        if(logger.isDebugEnabled()) 
        {
            logger.debug("[getNotificationChainItemList()]: ");
        }

        SQLParam[] args=new SQLParam[1+2];     // 1 SP parameter + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(chainId), Types.BIGINT);

        XanbooResultSet rs = dao.callSP(conn, "XC_NOTIFICATION_PKG.GET_CHAIN_ITEMS", args);
        ArrayList<NotificationChainRecipient> items = new ArrayList<NotificationChainRecipient>(rs.size());
        for ( int i = 0; i < rs.size(); i++)
        {
            long rsChainId = rs.getElementLong(i, "CHAIN_ID");
            long contactId = rs.getElementLong(i, "CONTACT_ID");
            int waitTime = rs.getElementInteger(i, "QUIET_TIME");
            String dest = rs.getElementString(i, "DESTINATION");
            NotificationChainRecipient r = new NotificationChainRecipient(rsChainId,contactId,null,waitTime,dest);
            r.setChainGrpNo(rs.getElementInteger(i, "CHAIN_GRP_NO"));
            r.setChainSeqNo(rs.getElementInteger(i, "CHAIN_SEQ_NO"));
            items.add(r);
            if(logger.isDebugEnabled()) 
            {
                logger.debug("[getNotificationChainItemList()]: found recipient for chain, recipient is "+r);
            }
        }
        if ( logger.isDebugEnabled() )
            logger.debug("[getNotificationChainItemList()]: found "+items.size()+" recipients for chainId="+chainId);
        return items;
    }
    
    public XanbooResultSet getAccountPendingNotifications(Long accountId,Connection conn)throws XanbooException
    {
        if(logger.isDebugEnabled()) 
        {
            logger.debug("[getAccountPendingNotifications()]: ");
        }

        SQLParam[] args=new SQLParam[1+2];     // 1 SP parameter + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);

        XanbooResultSet rs = dao.callSP(conn, "XC_NOTIFICATION_PKG.GET_ACCT_PENDING_MESSAGES", args);
        if ( logger.isDebugEnabled() )
            logger.debug("[getAccountPendingNotifications()] - found "+rs.size()+" pending notification events");
        return rs;
    }
    
    public XanbooResultSet getPendingNotification(Long accountId, Long queueId, String token, Connection conn)throws XanbooException
    {
         if(logger.isDebugEnabled()) 
        {
            logger.debug("[getPendingNotification()]: ");
        }

        SQLParam[] args=new SQLParam[3+2];     // 1 SP parameter + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(accountId), Types.BIGINT);
        args[1] = new SQLParam(queueId,Types.BIGINT);
        args[2] = new SQLParam(token,Types.VARCHAR);

        XanbooResultSet rs = dao.callSP(conn, "XC_NOTIFICATION_PKG.GET_PENDING_MESSAGE", args);
        
        return rs;
    }
    
    /**
     * 
     * @param chain
     * @param conn
     * @return
     * @throws XanbooException 
     */
    public NotificationChain updateNotificationChain(NotificationChain chain,Connection conn)throws XanbooException
    {
        if(logger.isDebugEnabled()) 
        {
            logger.debug("[getNotificationChainItemList()]: ");
        }

        SQLParam[] args=new SQLParam[9+2];     // 1 SP parameter + 2 std parameters (errno, errmsg)

        // setting IN params
        args[0] = new SQLParam(new Long(chain.getAccountId()), Types.BIGINT);
        if ( chain.getChainId() == -1 )
            args[1] = new SQLParam(null, Types.NULL);
        else
            args[1] = new SQLParam(chain.getChainId(),Types.BIGINT);
        args[2] = new SQLParam(chain.getQuietTypeType().getDBValue(),Types.VARCHAR);
        args[3] = new SQLParam((chain.useAsDefault() ? "1" : "0"),Types.VARCHAR);
        args[4] = new SQLParam((chain.sendToSBN() ? "1" : "0"),Types.VARCHAR);
        args[5] = new SQLParam(chain.getDescription(),Types.VARCHAR);
        if ( chain.getRecipients() != null && chain.getRecipients().size() > 0 )
        {
            List<NotificationChainRecipient> recipients = chain.getRecipients();
            StringBuilder contactBldr = new StringBuilder();
            StringBuilder waitTmBldr = new StringBuilder();
            StringBuilder destBldr = new StringBuilder();
            for ( NotificationChainRecipient r : recipients )
            {
                contactBldr.append(r.getContactId()).append(",");
                waitTmBldr.append(r.getWaitTime()).append(",");
                destBldr.append(r.getDestination().getDBValue()).append(",");
            }
            args[6] = new SQLParam(contactBldr.toString().substring(0,contactBldr.length()-1),Types.VARCHAR);
            args[7] = new SQLParam(waitTmBldr.toString().substring(0,waitTmBldr.length()-1),Types.VARCHAR);
            args[8] = new SQLParam(destBldr.toString().substring(0,destBldr.length()-1),Types.VARCHAR);
        }
        else
        {
            args[6] = new SQLParam(null,Types.NULL);
            args[7] = new SQLParam(null,Types.NULL);
            args[8] = new SQLParam(null,Types.NULL);
        }

        XanbooResultSet rs = dao.callSP(conn, "XC_NOTIFICATION_PKG.UPDATE_NOTIFICATION_CHAIN", args);
        
        long chainId = rs.getElementLong(0, "CHAIN_ID");
        if ( chain.getChainId() == -1 )
            logger.debug("[updateNotificationChain] - creating new chain id="+chainId+" name="+chain.getDescription()+" for account="+chain.getAccountId());
        else
            logger.debug("[updateNotificationChain] - updated chain id="+chainId+" name="+chain.getDescription()+" for account="+chain.getAccountId());
        chain.setChainId(chainId);
        return chain;
    }
    
    public XanbooResultSet queueMessage(Connection conn,Long accountId,String gatewayGuid,String deviceGuid,
                                        Long chainId,Integer templateTypeId,Integer eventId,String token,
                                        Date messageTimestamp,String eventText,Properties messageProperties,Properties sbnAlarmParams)throws XanbooException
    {
        if ( logger.isDebugEnabled() )
            logger.debug("[queueMessage()]");
        
         SQLParam[] args=new SQLParam[10+2];
         args[0] = new SQLParam(accountId,Types.BIGINT);
         args[1] = new SQLParam(gatewayGuid,Types.VARCHAR);
         args[2] = new SQLParam(deviceGuid,Types.VARCHAR);
         args[3] = new SQLParam(templateTypeId,Types.BIGINT);
         args[4] = chainId == null ? new SQLParam(null,Types.NULL) : new SQLParam(chainId,Types.BIGINT);
         args[5] = new SQLParam(token,Types.VARCHAR);
         args[6] = new SQLParam(eventId,Types.BIGINT);
         args[7] = new SQLParam(eventText,Types.VARCHAR);
         args[8] = messageTimestamp == null ? new SQLParam(null,Types.NULL) : new SQLParam(messageTimestamp,Types.TIMESTAMP);
        
         StringWriter propWriter = new StringWriter();
         if ( messageProperties == null )
         {
            args[9] = new SQLParam(null,Types.NULL);
         }
         else
         {
            try
            {
                messageProperties.store(propWriter, null);
                args[9] = new SQLParam(propWriter.toString(),Types.VARCHAR);
            }
            catch (IOException ex)
            {
                logger.error("[queueMessage] - exception writing properties to string", ex);
                throw new XanbooException(33000,"Error writing message properties to database");
            }
         }
         
         if ( sbnAlarmParams == null )
             args[10] = new SQLParam(sbnAlarmParams,Types.NULL);
         else
         {
            try
            {
                propWriter = new StringWriter();
                messageProperties.store(propWriter, null);
                args[10] = new SQLParam(propWriter,Types.VARCHAR);
            }
            catch (IOException ex)
            {
                logger.error("[queueMessage] - exception writing properties to string", ex);
                throw new XanbooException(33000,"Error writing message properties to database");
            }
         }
         return dao.callSP(conn, "XC_NOTIFICATION_PKG.QUEUE_MESSAGE", args);
    }
}
