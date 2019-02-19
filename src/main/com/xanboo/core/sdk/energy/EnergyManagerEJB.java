package com.xanboo.core.sdk.energy;

import java.sql.Connection;
import java.rmi.RemoteException;

import com.xanboo.core.sdk.AbstractSDKManagerEJB;
import com.xanboo.core.util.*;
import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.security.XanbooPrincipal;

import java.util.Date;

import javax.annotation.PostConstruct;
import javax.ejb.CreateException;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
/**
 * <p>
 * Session Bean implementation of <code>EnergyManager</code>. This bean acts as a wrapper class for
 * all device, device query/controls, event and notification setup related Core SDK methods.
 * </p>
 */
@Stateless (name="EnergyManager")
@TransactionManagement( TransactionManagementType.BEAN )
@Remote (EnergyManager.class)
public class EnergyManagerEJB extends AbstractSDKManagerEJB  
{
    // related DAO class
    private EnergyManagerDAO dao=null;
    
    @PostConstruct
    public void init() throws CreateException 
    {
    	 try 
         {
             // create a logger instance
             logger=LoggerFactory.getLogger(this.getClass().getName());
             if(logger.isDebugEnabled()) 
             {
                 logger.debug("[ejbCreate()]:");
             }
        dao = new EnergyManagerDAO();
        
         }
         catch (Exception se) 
         {
             throw new CreateException("Failed to create EnergyManagerEJB:" + se.getMessage());
         }
    }
    /**
     * 
     * @param domainId
     * @return
     * @throws RemoteException
     * @throws XanbooException 
     */
    public XanbooResultSet getDeviceTypes(String domainId)throws RemoteException, XanbooException
    {
        if ( domainId == null )
        {
            logger.debug("[getDeviceTypes()] domainId not specified, using default domain");
            domainId = "default";
        }
        Connection conn=null;
        try 
        {
            conn=dao.getConnection();
            
            return dao.getDeviceTypes(conn,domainId); 
   
        }
        catch (XanbooException xe) 
        {
            throw xe;
        }
        catch (Exception e) 
        {
            if(logger.isDebugEnabled()) 
            {
              logger.error("[getDeviceTypes()]: " + e.getMessage(), e);
            }
            else 
            {
              logger.error("[getDeviceTypes()]: " + e.getMessage());
            }                            
            throw new XanbooException(10030, "[getDeviceTypes]:" + e.getMessage());
        }
        finally 
        {
            dao.closeConnection(conn, false);
        }
    }
    /**
     * 
     * @param principal
     * @param GGUID
     * @param includeDetails
     * @param timePeriod
     * @param startDate
     * @param endDate
     * @param startDate2
     * @param endDate2
     * @return
     * @throws XanbooException 
     */
    public XanbooResultSet getUsageData(XanbooPrincipal principal,String GGUID,boolean includeDetails,String timePeriod,
                                        java.util.Date startDate,java.util.Date endDate,java.util.Date startDate2,java.util.Date endDate2)throws XanbooException
    {
        //add validation
       this.validateCommonParameters(principal, timePeriod, startDate, endDate);
       
       if ( startDate2 != null && endDate2 == null )
            throw new XanbooException(10050,"Missing required parameter, endDate2 was not specified for startDate2");
       
       if ( startDate2 != null && endDate2 != null && startDate2.after(endDate2) )
            throw new XanbooException(10050,"Invalid date range. Start date must be before end date");
       
       return getUsageData(principal,GGUID,null,includeDetails,timePeriod,startDate,endDate,startDate2,endDate2); 
    }
    /**
     * 
     * @param principal
     * @param GGUID
     * @param DGUID
     * @param timePeriod
     * @param startDate
     * @param endDate
     * @param startDate2
     * @param endDate2
     * @return
     * @throws XanbooException 
     */
    public XanbooResultSet getUsageData(XanbooPrincipal principal,String GGUID,String[] DGUID,String timePeriod,
                                        java.util.Date startDate,java.util.Date endDate,java.util.Date startDate2,java.util.Date endDate2)throws XanbooException
    {
         //add validation
       this.validateCommonParameters(principal, timePeriod, startDate, endDate);
       
       if ( startDate2 != null && endDate2 == null )
            throw new XanbooException(10050,"Missing required parameter, endDate2 was not specified for startDate2");
       
       if ( startDate2 != null && endDate2 != null && startDate2.after(endDate2) )
            throw new XanbooException(10050,"Invalid date range. Start date must be before end date");
        
       //gateway guid is required when passing device guid(s)
       if ( GGUID == null )
        throw new XanbooException(10050,"Missing required parameter : GGUID");
       
        //validate the device guid list
        if ( DGUID != null )
        {
            int validDguid = 0;
            for ( String dguid : DGUID )
            {
                if ( dguid != null && !dguid.equalsIgnoreCase(""))
                    validDguid++;
            }
            if ( validDguid == 0 )
                throw new XanbooException(10050,"Missing required parameter, gateway GUID is required");    
        }
        else //DGUID is null
            throw new XanbooException(10050,"Missing required parameter, one or more device GUID are required");
        
        return getUsageData(principal,GGUID,DGUID,false,timePeriod,startDate,endDate,startDate2,endDate2);
    } 
    /**
     * 
     * @param principal
     * @param GGUID
     * @param DGUID
     * @param includeDetails
     * @param timePeriod
     * @param startDate_1
     * @param endDate_1
     * @param startDate_2
     * @param endDate_2
     * @return
     * @throws XanbooException 
     */
    public XanbooResultSet getUsageData(XanbooPrincipal principal,String GGUID,String[] DGUID,boolean includeDetails,
                                                     String timePeriod,java.util.Date startDate_1,java.util.Date endDate_1,
                                                     java.util.Date startDate_2,java.util.Date endDate_2)throws XanbooException
    {
       
       logger.debug("[getUsageData() ] Retrieve usage data for account, principal="+principal.getAccountId());
        
       if ( logger.isDebugEnabled() )
       {
            StringBuffer parmList = new StringBuffer();
            parmList.append("accountId=").append(principal.getAccountId());
            if ( GGUID !=  null )
                parmList.append(",GGUID=").append(GGUID);
            if ( DGUID != null )
            {
                parmList.append(",DGUID=[");
                for ( String devGuid : DGUID )
                {
                    parmList.append(devGuid).append(",");
                }
                parmList.append("]");
            }
            parmList.append(",includeDetails=").append(includeDetails);
            parmList.append(",timePeriod=").append(timePeriod);
            parmList.append(",startDate=").append(startDate_1);
            parmList.append(",endDate=").append(endDate_1);
            parmList.append(",startDate=").append(startDate_2);
            parmList.append(",endDate=").append(endDate_2);
            
            logger.debug("[getUsageData()] "+parmList);
        }
        
        Connection conn=null;
        try 
        {
            // first validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(principal);
            
            conn=dao.getConnection();
            
            return dao.getUsageData(conn,principal, GGUID, DGUID, includeDetails,timePeriod, startDate_1, endDate_1,startDate_2, endDate_2);
   
        }
        catch (XanbooException xe) 
        {
            throw xe;
        }
        catch (Exception e) 
        {
            if(logger.isDebugEnabled()) 
            {
              logger.error("[getUsageData()]: " + e.getMessage(), e);
            }
            else 
            {
              logger.error("[getUsageData()]: " + e.getMessage());
            }                            
            throw new XanbooException(10030, "[getUsageData]:" + e.getMessage());
        }
        finally 
        {
            dao.closeConnection(conn, false);
        }
    }
    
    /**
     * 
     * @param principal
     * @param GGUID
     * @param deviceTypes
     * @param includeDetails
     * @param timePeriod
     * @param startDate_1
     * @param endDate_1
     * @param startDate_2
     * @param endDate_2
     * @return
     * @throws XanbooException 
     */
    public XanbooResultSet getUsageDataByType(XanbooPrincipal principal,String GGUID,String[] deviceTypes,boolean includeDetails,
                                              String timePeriod,java.util.Date startDate_1,java.util.Date endDate_1,
                                              java.util.Date startDate_2,java.util.Date endDate_2) throws XanbooException
    {
        this.validateCommonParameters(principal, timePeriod, startDate_1,endDate_1);
        
        logger.info("[getUsageDataByType() ] Retrieve usage data for account, principal="+principal.getAccountId());
        
        if ( startDate_2 != null && endDate_2 == null )
            throw new XanbooException(10050,"Missing required parameter, endDate2 was not specified for startDate2");
       
        if ( startDate_2 != null && endDate_2 != null && startDate_2.after(endDate_2) )
            throw new XanbooException(10050,"Invalid date range. Start date must be before end date");
        
        if ( logger.isDebugEnabled() )
        {
            StringBuffer parmList = new StringBuffer();
            parmList.append("accountId=").append(principal.getAccountId());
            if ( GGUID !=  null )
                parmList.append(",GGUID=").append(GGUID);
            if ( deviceTypes != null )
            {
                parmList.append(",deviceTypeId=[");
                for ( String id : deviceTypes )
                {
                    parmList.append(id).append(",");
                }
                parmList.append("]");
            }
            parmList.append(",includeDetails=").append(includeDetails);
            parmList.append(",timePeriod=").append(timePeriod);
            parmList.append(",startDate=").append(startDate_1);
            parmList.append(",endDate=").append(endDate_1);
            
            logger.debug("[getUsageDataByType()] "+parmList);
        }
        
        Connection conn=null;
        try 
        {
            // first validate the caller and privileges
            XanbooUtil.checkCallerPrivilege(principal);
            
            conn=dao.getConnection();
            
            return dao.getUsageDataByType(conn,principal, GGUID, deviceTypes,includeDetails, timePeriod,startDate_1, endDate_1, startDate_2,endDate_2);
   
        }
        catch (XanbooException xe) 
        {
            throw xe;
        }
        catch (Exception e) 
        {
            if(logger.isDebugEnabled()) 
            {
              logger.error("[getUsageDataByType()]: " + e.getMessage(), e);
            }
            else 
            {
              logger.error("[getUsageDataByType()]: " + e.getMessage());
            }                            
            throw new XanbooException(10030, "[getUsageDataByType]:" + e.getMessage());
        }
        finally 
        {
            dao.closeConnection(conn, false);
        }
    }
    
    private boolean validateCommonParameters(XanbooPrincipal principal,String timePeriod,Date startDate,Date endDate) throws XanbooException
    {
        //check required parameters. 
        if ( principal == null || timePeriod == null || startDate == null || endDate == null )
        {
            throw new XanbooException(10050,"Missing required parameter");
        }
        //validate time period requested
        if ( !timePeriod.equalsIgnoreCase(EnergyManager.ENERGY_USAGE_DAILY) &&
             !timePeriod.equalsIgnoreCase(EnergyManager.ENERGY_USAGE_HOURLY) &&
             !timePeriod.equalsIgnoreCase(EnergyManager.ENERGY_USAGE_MONTHLY))
            throw new XanbooException(10050,"Invalid timePeriod parameter. Should be one of (H)ourly, (D)aily, or (M)onthly");
        //validate start date & enddate. valid conditions startDate==endDate or endDate>startDate. If startDate>endDate, error
        if ( startDate.after(endDate) )
            throw new XanbooException(10050,"Invalid date range. Start date must be before end date");
        
        return true;
    }
}
