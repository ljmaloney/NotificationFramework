/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.sdk.account.alert;

import java.sql.Connection;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Set;
import com.xanboo.core.sdk.account.XanbooSubscription;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.util.BaseDAO;
import com.xanboo.core.util.BaseHandlerDAO;
import com.xanboo.core.util.DAOFactory;
import com.xanboo.core.util.GlobalNames;
import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;
import com.xanboo.core.util.SQLParam;
import com.xanboo.core.util.XanbooException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;


/**
 *
 * @author lm899p
 */
public class AccountAlertDAO extends BaseHandlerDAO 
{
    public static final String ALERTINFO_FIELD_SEPARATOR = "|";
    private BaseDAO dao;
    private Logger logger;
    
    public AccountAlertDAO() throws XanbooException 
    {
        try 
        {
            // obtain a Logger instance
            logger=LoggerFactory.getLogger(this.getClass().getName());
            if(logger.isDebugEnabled()) 
                logger.debug("[AccountAlertDAO()]:");
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
                logger.error("[AccountAlertDAO()]: " + ne.getMessage(), ne);
            else 
                logger.error("[AccountAlertDAO()]: " + ne.getMessage());
            throw new XanbooException(20014, "[AccountAlertDAO()]: " + ne.getMessage() );
        }
    }
    /**
     * Method calls CLEAR_ALERT stored procedure.  <br/>
     * @param conn
     * @param subscription
     * @param alert
     * @return
     * @throws XanbooException 
     */
    public Long clearAlert(Connection conn,XanbooSubscription subscription,XanbooAlert alert)throws XanbooException
    {
        if ( logger.isDebugEnabled() )
            logger.debug("[clearAlert()]");
        
        SQLParam[] params = new SQLParam[9+2];
        params[0] = new SQLParam(alert.getDomainId(),Types.VARCHAR);
        params[1] = (subscription.getAccountId() == -1l || subscription.getAccountId() == -1l) ? new SQLParam(null,Types.NULL): new SQLParam(subscription.getAccountId(),Types.NUMERIC);
        params[2] = (subscription.getExtAccountId() == null || subscription.getExtAccountId().equalsIgnoreCase("")) ? new SQLParam(null,Types.NULL) : new SQLParam(subscription.getExtAccountId(),Types.VARCHAR);
        params[3] = (subscription.getSubsId() == null || subscription.getSubsId().equalsIgnoreCase(""))  ?new SQLParam(null,Types.NULL) : new SQLParam(subscription.getSubsId(),Types.VARCHAR);
        params[4] =  new SQLParam(null,Types.NULL);
        params[5] = new SQLParam(alert.getAlertSource(),Types.VARCHAR);
        params[6] = new SQLParam(alert.getAlertCode(),Types.VARCHAR);
        params[7] = new SQLParam(alert.getMessageType().getDbValue(),Types.INTEGER);
        params[8] = new SQLParam(alert.getAlertText(),Types.VARCHAR);
        
        XanbooResultSet rs = dao.callSP(conn,"XC_ACCT_ALERT_PKG.CLEAR_ALERT", params);
        if ( !rs.isEmpty() )
        {
            alert.setAlertId(rs.getElementLong(0, "ALERT_ID"));
            alert.setAccountId(rs.getElementLong(0, "ACCOUNT_ID"));
            alert.setPushPrefs(rs.getElementInteger(0,"PUSH_PREFS"));
            subscription.setAccountId(rs.getElementLong(0,"ACCOUNT_ID"));
            subscription.setTzone(rs.getElementString(0,"TZNAME"));
            alert.setLanguageId(rs.getElementString(0,"LOCALE"));
            if ( logger.isDebugEnabled() )
                logger.debug("[clearAlert()] - cleared alert, alertId="+alert.getAlertId()+" for account="+subscription.getAccountId());
            return rs.getElementLong(0, "ALERT_ID");
        }
        
        return null;
    }
    
    public void getAccountId(Connection conn,XanbooSubscription subscription,XanbooAlert alert)throws XanbooException
    {
        if ( logger.isDebugEnabled() )
            logger.debug("[getAccountId()]");
        
        SQLParam[] params = new SQLParam[3+2];
        
        params[0] = new SQLParam(alert.getDomainId(),Types.VARCHAR);
        params[1] = (subscription.getExtAccountId() == null || subscription.getExtAccountId().equalsIgnoreCase("")) ? new SQLParam(null,Types.NULL) : new SQLParam(subscription.getExtAccountId(),Types.VARCHAR);
        params[2] = (subscription.getSubsId() == null || subscription.getSubsId().equalsIgnoreCase("")) ? new SQLParam(null,Types.NULL) : new SQLParam(subscription.getSubsId(),Types.VARCHAR);
        
        XanbooResultSet rs = dao.callSP(conn,"XC_ACCT_ALERT_PKG.GET_ACCT_ID",params);
        
        if (!rs.isEmpty())
        {
            alert.setAccountId(rs.getElementLong(0, "ACCOUNT_ID"));
            subscription.setAccountId(rs.getElementLong(0,"ACCOUNT_ID"));
            if ( logger.isDebugEnabled())
                logger.debug("[getAccountId()] - found account id "+alert.getAccountId());
        }
    }
    
    public List<XanbooAlert> getAlerts(Connection conn,XanbooSubscription subscription,String domain,Long alertId)throws XanbooException
    {
        if ( logger.isDebugEnabled() )
            logger.debug("[getAlerts()]");
        
        SQLParam[] params = new SQLParam[5+2];
        
        params[0] = new SQLParam(domain,Types.VARCHAR);
        params[1] = (subscription.getAccountId() == -1l) ? new SQLParam(null,Types.NULL): new SQLParam(subscription.getAccountId(),Types.NUMERIC);
        params[2] = (subscription.getExtAccountId() == null || subscription.getExtAccountId().equalsIgnoreCase("")) ? new SQLParam(null,Types.NULL) : new SQLParam(subscription.getExtAccountId(),Types.VARCHAR);
        params[3] = (subscription.getSubsId() == null || subscription.getSubsId().equalsIgnoreCase("")) ? new SQLParam(null,Types.NULL) : new SQLParam(subscription.getSubsId(),Types.VARCHAR);
        params[4] = alertId != null ? new SQLParam(alertId,Types.NUMERIC) : new SQLParam(null,Types.NULL);
        
        XanbooResultSet rs = dao.callSP(conn, "XC_ACCT_ALERT_PKG.GET_ALERTS", params);
        if ( logger.isDebugEnabled() )
            logger.debug("[getAlerts()] found "+rs.size()+" alerts");
        
        List<XanbooAlert> alerts = new ArrayList<XanbooAlert>();
        int index = 0; 
        for ( Object obj : rs )
        {
            XanbooAlert alert = createAlertInstance(rs,index);
            alerts.add(alert);
            index++;
        }
        return alerts;
    }
    /**
     * @param conn
     * @param subscription
     * @param alert
     * @return
     * @throws XanbooException 
     */
    public XanbooAlert newAlert(Connection conn,XanbooSubscription subscription,XanbooAlert alert)throws XanbooException
    {
        if ( logger.isDebugEnabled() )
            logger.debug("[newAlert()]");
        
        DateFormat expDtFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar cal = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
        expDtFormat.setCalendar(cal);
        
        SQLParam[] params = new SQLParam[13+2];
        
        params[0] = new SQLParam(alert.getDomainId(),Types.VARCHAR);
        params[1] = (subscription.getAccountId() == -1l) ? new SQLParam(null,Types.NULL): new SQLParam(subscription.getAccountId(),Types.NUMERIC);
        params[2] = (subscription.getExtAccountId() == null || subscription.getExtAccountId().equalsIgnoreCase("")) ? new SQLParam(null,Types.NULL) : new SQLParam(subscription.getExtAccountId(),Types.VARCHAR);
        params[3] = (subscription.getSubsId() == null || subscription.getSubsId().equalsIgnoreCase("")) ? new SQLParam(null,Types.NULL) : new SQLParam(subscription.getSubsId(),Types.VARCHAR);
        params[4] = new SQLParam(null,Types.NULL);
        params[5] = new SQLParam(alert.getAlertSource(),Types.VARCHAR);
        params[6] = new SQLParam(alert.getAlertCode(),Types.VARCHAR);
        params[7] = new SQLParam(alert.getEventId(),Types.INTEGER);
        params[8] = new SQLParam(alert.getAlertBehavior().getDBValue(),Types.VARCHAR);
        params[9] = new SQLParam(alert.getMessageType().getDbValue(),Types.INTEGER);
        //params[10] = alert.getExpirationDate() != null ? new SQLParam(alert.getExpirationDate(),Types.DATE) : new SQLParam(null,Types.NULL);
        params[10] = alert.getExpirationDate() != null ? new SQLParam(expDtFormat.format(alert.getExpirationDate()),Types.VARCHAR) : new SQLParam(null,Types.NULL);
        params[11] = new SQLParam(alert.getAlertText(),Types.VARCHAR);
        if ( alert.getAlertInfo() != null && !alert.getAlertInfo().isEmpty())
        {
            HashMap<String,String> infoMap = alert.getAlertInfo();
            Set<String> keys = infoMap.keySet();
            StringBuilder infoBldr = new StringBuilder();
            for ( String key : keys )
            {
                if ( infoBldr.length() > 0)
                    infoBldr.append(ALERTINFO_FIELD_SEPARATOR);
                infoBldr.append(key).append("=").append(infoMap.get(key));
            }
            params[12] = new SQLParam(infoBldr.toString(),Types.VARCHAR);
        }
        else 
            params[12] = new SQLParam(null,Types.NULL);
        
        XanbooResultSet rs = dao.callSP(conn, "XC_ACCT_ALERT_PKG.UPDATE_ALERT", params);
        if ( rs.isEmpty() )
            return null;
        
        XanbooAlert newAlert = createAlertInstance(rs,0);
        newAlert.setLanguageId(rs.getElementString(0,"LOCALE"));
        newAlert.setAlertPushLabel(alert.getAlertPushLabel());
        newAlert.setAlertPushUrl(alert.getAlertPushUrl());
        newAlert.setResponseTemplateId(alert.getResponseTemplateId());
        subscription.setTzone(rs.getElementString(0, "TZNAME"));
        subscription.setAccountId(rs.getElementLong(0,"ACCOUNT_ID"));
        
        if ( logger.isDebugEnabled() )
            logger.debug("[newAlert()] - created new alert - "+newAlert);
        
        return newAlert;
    }
    
    private XanbooAlert createAlertInstance(XanbooResultSet rs,int index)
    {
        if ( logger.isDebugEnabled() )
            logger.debug("[createAlertInstance()]");
        XanbooAlert alert = new XanbooAlert();
        alert.setAlertId(rs.getElementLong(index,"ALERT_ID"));
        alert.setAccountId(rs.getElementLong(index, "ACCOUNT_ID"));
        alert.setEventId(rs.getElementInteger(index, "EVENT_ID"));
        alert.setAlertEvent(XanbooAlert.EventType.NEW_ALERT);
        if ( GlobalNames.ACCTALERT_CLEAR_EVENT.indexOf(alert.getEventId().toString()) > -1 )
            alert.setAlertEvent(XanbooAlert.EventType.CLEAR_ALERT);
        //alert.setAlertEvent(XanbooAlert.EventType.getEvent(rs.getElementInteger(index, "EVENT_ID")));
        alert.setAlertCode(rs.getElementString(index, "ALERT_CODE"));
        alert.setAlertSource(rs.getElementString(index,"ALERT_SOURCE"));
        alert.setAlertText(rs.getElementString(index,"ALERT_TEXT"));
        alert.setCreateDate(rs.getElementDate(index,"CREATE_DATE"));
        alert.setDomainId(rs.getElementString(index,"DOMAIN_ID"));
        alert.setExpirationDate(rs.getElementDate(index,"EXPIRATION_DATE"));
        alert.setMessageType(XanbooAlert.MessageTypeType.getType(rs.getElementInteger(index, "MSG_TYPE")));
        alert.setStatusInd(rs.getElementInteger(index,"STATUS_IND"));
        String alrtBhvrCd = rs.getElementString(index,"ALERT_BEHAVIOR");
        if ( logger.isDebugEnabled() ) logger.debug("[createAlertInstance()] - alert_behavior="+alrtBhvrCd);
        XanbooAlert.AlertBehaviorType ab = XanbooAlert.AlertBehaviorType.CLEAR_ON_VIEW;
        if ( rs.getElementString(index,"ALERT_BEHAVIOR") != null && 
             rs.getElementString(index,"ALERT_BEHAVIOR").equals(XanbooAlert.AlertBehaviorType.HOLD_ON_VIEW.getDBValue()))
            ab = XanbooAlert.AlertBehaviorType.HOLD_ON_VIEW;
        alert.setAlertBehavior(ab);
        alert.setPushPrefs(rs.getElementInteger(0, "PUSH_PREFS"));
        alert.setSubscriberId(rs.getElementString(index,"SUBS_ID"));
        String infoString = rs.getElementString(index,"ALERT_INFO");
        //logger.debug("[createAlertInstance()] - alertInfo="+infoString);
        if ( infoString != null && !infoString.equalsIgnoreCase(""))
        {
            String[] pairs = null;
            if ( infoString.indexOf(ALERTINFO_FIELD_SEPARATOR) > 0 ) 
                pairs = infoString.split("\\"+ALERTINFO_FIELD_SEPARATOR);
            else
            {
                pairs = new String[1];
                pairs[0] = infoString;
            }
            //for ( int i = 0; i < pairs.length; i++ )
            //{
            //    logger.debug("[createAlertInstance()] - pair["+pairs[i]+"]");
            //}
            for ( int i = 0; i < pairs.length; i++ )
            {
            //    logger.debug("[createAlertInstance()] - pair["+pairs[i]+"]");
                String key = pairs[i].substring(0,pairs[i].indexOf("="));
                String val = pairs[i].substring((pairs[i].indexOf("=")+1));
                alert.putAlertInfo(key, val);
            }
        }
        if ( logger.isDebugEnabled() )
            logger.debug("[createAlertInstance()] - created alert "+alert);
        return alert;
    }
}
