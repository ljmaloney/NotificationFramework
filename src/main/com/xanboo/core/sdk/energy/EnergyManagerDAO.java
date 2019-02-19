package com.xanboo.core.sdk.energy;

import com.xanboo.core.util.*;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooPrincipal;
import java.sql.Connection;
import java.sql.Types;
import java.util.HashMap;

/**
 * This class is the DAO class to wrap all generic database calls for SDK EnergyManager methods.
 * Database specific methods are handled by implementation classes. These implementation
 * classes extend the BaseDAO class to actually perform the database operations. An instance of
 * an implementation class is created during construction of this class.
 */
class EnergyManagerDAO extends BaseHandlerDAO 
{
    private BaseDAO dao;
    private Logger logger;
    
    public static final String SP_GET_DEVICE_TYPES = "XC_ENERGY_PKG.GET_DEVICE_TYPES";
    public static final String SP_GET_USAGE_DATA = "XC_ENERGY_PKG.GET_USAGE_DATA";
    public static final String SP_GET_USAGE_BYTYPE = "XC_ENERGY_PKG.GET_USAGE_DATA_BYTYPE";
    
    /**
     * Default constructor. Takes no arguments
     *
     * @throws XanbooException
     */
    public EnergyManagerDAO() throws XanbooException 
    {
        try 
        {
            // obtain a Logger instance
            logger=LoggerFactory.getLogger(this.getClass().getName());
            if(logger.isDebugEnabled()) 
            {
                logger.debug("[EnergyManagerDAO()]:");
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
              logger.error("[DeviceManagerDAO()] Exception:" + ne.getMessage(), ne);
            }
            else 
            {
              logger.error("[DeviceManagerDAO()] Exception: " + ne.getMessage());
            }                
            throw new XanbooException(20014, "[DeviceManagerDAO()] Exception:" + ne.getMessage());
        }
    }
    
    
    public XanbooResultSet getDeviceTypes(Connection conn,String domainId)throws XanbooException
    {
       logger.debug("[getDeviceTypes()] - retrieve device types for domainId - "+domainId);
       
       SQLParam[] args = new SQLParam[1+2];
       args[0] = new SQLParam(domainId,Types.VARCHAR);
       long startTime = System.currentTimeMillis();
       XanbooResultSet resultSet = dao.callSP(conn,SP_GET_DEVICE_TYPES, args);
           
       long stopTime = System.currentTimeMillis();
       logger.debug("[getDeviceTypes()] - elapsed time to retrieve device types is "+(stopTime-startTime)+" milliseconds");
         
       return resultSet;
    }
    
    public XanbooResultSet getUsageData(Connection conn,XanbooPrincipal principal,String GGUID,String[] DGUID,boolean includeDetails,
                                        String timePeriod,java.util.Date startDate,java.util.Date endDate) throws XanbooException
    {
        return getUsageData(conn,principal,GGUID,DGUID,includeDetails,timePeriod,startDate,endDate,null,null);
    }
   
    public XanbooResultSet getUsageData(Connection conn,XanbooPrincipal principal,String GGUID,String[] DGUID,boolean includeDetails,
                                        String timePeriod,java.util.Date startDate_1,java.util.Date endDate_1,
                                        java.util.Date startDate_2,java.util.Date endDate_2)throws XanbooException
    {
        logger.info("[getUsageData()]");
       
        SQLParam[] args = new SQLParam[9+2];
        args[0] = new SQLParam(principal.getAccountId(),Types.BIGINT); //required
        if ( GGUID == null )
            args[1] = new SQLParam(null,Types.NULL);
        else
            args[1] = new SQLParam(GGUID,Types.VARCHAR);
        if ( DGUID != null )
        {
            StringBuffer str = new StringBuffer();
            for ( String s  : DGUID )
            {
                if ( str.length() > 0 )
                    str.append(",");
                str.append("'"+s+"'");
            }
            args[2] = new SQLParam(str.toString(),Types.VARCHAR);
        }
        else
            args[2] = new SQLParam(null,Types.NULL);
        args[3] = new SQLParam((includeDetails ? "T" : "F"),Types.VARCHAR); //required
        args[4] = new SQLParam(timePeriod,Types.VARCHAR); //required
        args[5] = new SQLParam(startDate_1,Types.DATE); //required
        args[6] = new SQLParam(endDate_1,Types.DATE);   //required
        if ( startDate_2 == null )
            args[7] = new SQLParam(null,Types.NULL);
        else
            args[7] = new SQLParam(startDate_2,Types.DATE);
        if ( endDate_2 == null )
            args[8] = new SQLParam(null,Types.NULL);
        else
            args[8] = new SQLParam(endDate_2,Types.DATE);
        
        long startTime = System.currentTimeMillis();
        XanbooResultSet resultSet = dao.callSP(conn,SP_GET_USAGE_DATA, args);
        long stopTime = System.currentTimeMillis();
        logger.debug("[getUsageData()] - elapsed time to retrieve device types is "+(stopTime-startTime)+" milliseconds");
        
        if ( args[10] != null )
            logger.debug("[getUsageData()] - o_errmsg="+args[10].getParam().toString());
        
        if ( logger.isDebugEnabled() )
        {
            logger.debug("[getUsageData()] - Number of records returned from get_usage_data : "+resultSet.size());
            for ( int i = 0; i < resultSet.size(); i++ )
            {
                HashMap resultMap = (HashMap)resultSet.get(i);
                logger.debug("[getUsageData()] - usageData : "+resultMap);
            }
        }
        
        return resultSet;
    }
    
    public XanbooResultSet getUsageDataByType(Connection conn,XanbooPrincipal principal,String GGUID,String[] deviceTypeId,boolean includeDetails,
                                              String timePeriod,java.util.Date startDate_1,java.util.Date endDate_1)throws XanbooException
    {
        return getUsageDataByType(conn,principal,GGUID,deviceTypeId,includeDetails,timePeriod,startDate_1,endDate_1,null,null);
    }
    
    public XanbooResultSet getUsageDataByType(Connection conn,XanbooPrincipal principal,String GGUID,String[] deviceTypeId,boolean includeDetails,
                                              String timePeriod,java.util.Date startDate_1,java.util.Date endDate_1,
                                              java.util.Date startDate_2,java.util.Date endDate_2)throws XanbooException
    {
        logger.debug("[getUsageDataByType()]");
       
        SQLParam[] args = new SQLParam[9+2];
        args[0] = new SQLParam(principal.getAccountId(),Types.BIGINT);
        args[1] = new SQLParam(GGUID,Types.VARCHAR);
        if ( deviceTypeId != null )
        {
            StringBuffer str = new StringBuffer();
            for ( String s  : deviceTypeId )
            {
                if ( str.length() > 0 )
                    str.append(",");
                str.append("'"+s+"'");
            }
            args[2] = new SQLParam(str.toString(),Types.VARCHAR);
        }
        else
            args[2] = new SQLParam(null,Types.NULL);
        args[3] = new SQLParam((includeDetails ? "T" : "F"),Types.VARCHAR);
        args[4] = new SQLParam(timePeriod,Types.VARCHAR);
        args[5] = new SQLParam(startDate_1,Types.DATE);
        args[6] = new SQLParam(endDate_1,Types.DATE);
        args[7] = new SQLParam(startDate_2,Types.DATE);
        args[8] = new SQLParam(endDate_2,Types.DATE);
        
        long startTime = System.currentTimeMillis();
        XanbooResultSet resultSet = dao.callSP(conn,SP_GET_USAGE_BYTYPE, args);
           
        long stopTime = System.currentTimeMillis();
        logger.debug("[getDeviceTypes()] - elapsed time to retrieve device types is "+(stopTime-startTime)+" milliseconds");
           
        return resultSet;
    }
    
    public void updateUsageData(String gguid,String dguid,String jsonData) throws XanbooException
    {
        
        Connection conn = null;
        //SQLParam[] jsonArgs = new SQLParam[1+2];
        //jsonArgs[0] = new SQLParam(jsonData,Types.VARCHAR);
        
        SQLParam[] args = new SQLParam[5+2];
        args[0] = new SQLParam(gguid,Types.VARCHAR);
        args[1] = new SQLParam(dguid,Types.VARCHAR);
        args[2] = new SQLParam("000000",Types.VARCHAR);
        args[3] = new SQLParam(new java.util.Date(),Types.DATE);
        args[4] = new SQLParam(jsonData,Types.VARCHAR);
        
        try
        {
            conn = super.getConnection();
            dao.callSP(conn, "xc_energy_pkg.update_usage_data", args, true);
            super.closeConnection(conn);
        }
        catch(XanbooException xe)
        {
            if ( conn != null )
                super.closeConnection(conn);
            throw xe;
        }
    }
}
