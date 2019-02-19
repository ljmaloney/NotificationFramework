/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.sdk.energy;


import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.xanboo.core.util.LoggerFactory;
import com.xanboo.core.util.XanbooException;
import java.util.Hashtable;
import java.util.Random;

/**
 *
 * @author lm899p
 */
public class EnergyLoadTestServlet extends HttpServlet
{
    private int threadCount = 0;
    private com.xanboo.core.util.Logger logger;
    List<Long> executionTimes = new ArrayList<Long>();
    int queryCount = 0;
    
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        logger=LoggerFactory.getLogger(this.getClass().getName());
    }
    
    private synchronized void incrementThreadCount(boolean increment)
    {
        if ( increment )
            threadCount = threadCount + 1;
        else
            threadCount= threadCount - 1;
        logger.info("[incrementThreadCount()] number of executing device threads is "+threadCount);
    }
    
    private synchronized void addExecutionTime(long time)
    {
        executionTimes.add(time);
    }
    
    private synchronized void incrementQueryCount(boolean increment)
    {
        if ( increment )
            queryCount = queryCount + 1;
        else
            queryCount = queryCount - 1;
    }
    
    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        int numberOfGateways = new Integer(request.getParameter("nbrGateways"));
        int numberDevices = new Integer(request.getParameter("nbrDevices"));
        double energyPrice = new Double(request.getParameter("energyPrice"));
        double energyIncrement = new Double(request.getParameter("energyIncrement"));
        long timeSliceTime = new Long(request.getParameter("timeSliceTime"));
        int maxTimeSliceCount = new Integer(request.getParameter("timeSliceCount"));
        int maxQueryCount = 0;
        
        threadCount = 0;
        queryCount = 0;
        executionTimes.clear();
        
        Date startTime = new Date();
        
        for ( int i = 0; i < numberOfGateways; i++ )
        {
            try
            {
                Gateway g = new Gateway("GATEWAY-GG"+i,numberDevices,energyIncrement,energyPrice,timeSliceTime,maxTimeSliceCount);
                Thread.currentThread().sleep(250);
            }
            catch (InterruptedException ex)
            {
                logger.warn("error starting threads",ex);
            }
        }
        
        while(threadCount > 0 )
        {
            try
            {
                Thread.currentThread().sleep(1000);
                logger.info("number of executing device threads is "+threadCount);
                logger.info("current executing queryies "+queryCount);
                if ( queryCount > maxQueryCount )
                    maxQueryCount = queryCount;
            }
            catch (InterruptedException ex)
            {
                logger.warn("thread error",ex);
            }
        }
        Date stopTime = new Date();
        
        long avgExecutionTime;
        long totalExecutionTime = 0;
        long maxExecTime = 0;
        long timeMore1000Count = 0;
        long timeMore2000Count = 0;
        long timeMore3000Count = 0;
        long timeMore5000Count = 0;
        
        for ( long time : executionTimes )
        {
            totalExecutionTime+=time;
            if ( time > maxExecTime )
                maxExecTime = time;
            if ( time > 1000 )
                timeMore1000Count++;
            if ( time > 2000 )
                timeMore2000Count++;
            if ( time > 3000 )
                timeMore3000Count++;
            if ( time > 5000 )
                timeMore5000Count++;
        }
        avgExecutionTime = totalExecutionTime / executionTimes.size();
        
       
        
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try
        {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet EnergyLoadTestServlet</title>");            
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Energy Management - Update Usage Load Test</h1>");
            out.println("<br/><br/>");
            out.println("<table border=1 width='50%'>");
            out.println("   <tr>");
            out.println("       <th>Parameter</th><th>value</th>");
            out.println("   </tr>");
            out.println("   <tr>");
            out.println("       <td>Number of Gateways</td><td>"+numberOfGateways+"</td>");
            out.println("   </tr>");
            out.println("   <tr>");
            out.println("       <td>Number of Devices(per Gateway)</td><td>"+numberDevices+"</td>");
            out.println("   </tr>");
            out.println("   <tr>");
            out.println("       <td>Energy Price</td><td>"+energyPrice+"</td>");
            out.println("   </tr>");
            out.println("   <tr>");
            out.println("       <td>Energy Increment</td><td>"+energyIncrement+"</td>");
            out.println("   </tr>");
            out.println("   <tr>");
            out.println("       <td>Time Slice Time</td><td>"+timeSliceTime+"</td>");
            out.println("   </tr>");
            out.println("   <tr>");
            out.println("       <td>Time Slice Count</td><td>"+maxTimeSliceCount+"</td>");
            out.println("   </tr>");
            out.println("   <tr>");
            out.println("       <td>Start Date/Time</td><td>"+startTime+"</td>");
            out.println("   </tr>");
            out.println("   <tr>");
            out.println("       <td>Stop Date/Time</td><td>"+stopTime+"</td>");
            out.println("   </tr>");
            out.println("   <tr>");
            out.println("       <td>Number proc updates</td><td>"+executionTimes.size()+"</td>");
            out.println("   </tr>");
            out.println("   <tr>");
            out.println("       <td>Average Execution Time</td><td>"+avgExecutionTime+"</td>");
            out.println("   </tr>");
            out.println("   <tr>");
            out.println("       <td>Max Execution Time</td><td>"+maxExecTime+"</td>");
            out.println("   </tr>");
            out.println("   <tr>");
            out.println("       <td>Number of Executions > 1000 millis</td><td>"+timeMore1000Count+"</td>");
            out.println("   </tr>");
            out.println("   <tr>");
            out.println("       <td>Number of Executions > 2000 millis</td><td>"+timeMore2000Count+"</td>");
            out.println("   </tr>");
            out.println("   <tr>");
            out.println("       <td>Number of Executions > 3000 millis</td><td>"+timeMore3000Count+"</td>");
            out.println("   </tr>");
            out.println("   <tr>");
            out.println("       <td>Number of Executions > 5000 millis</td><td>"+timeMore5000Count+"</td>");
            out.println("   </tr>");
            out.println("   <tr>");
            out.println("       <td>Max concurrent usage updates</td><td>"+maxQueryCount+"</td>");
            out.println("   </tr>");
            out.println("</table");
            out.println("</body>");
            out.println("</html>");
        }
        finally
        {            
            out.close();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)throws ServletException, IOException
    {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)throws ServletException, IOException
    {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo()
    {
        return "Short description";
    }// </editor-fold>
    
    class Gateway
    {
        String gguid = null;
        List<DeviceThread> deviceThreads = new ArrayList<DeviceThread>();
        public Gateway(String gguid,int deviceCount,double usage,double price,long sliceInterval,int sliceCount)
        {
            this.gguid = gguid;
            logger.info("Starting gateway "+gguid+" with deviceCount="+deviceCount);
            for ( int i = 0; i < deviceCount; i++ )
            {
                DeviceThread dt = new DeviceThread(this,"device-"+i,price, usage, sliceInterval,sliceCount);
                dt.start();
            }
        }
    }
    class DeviceThread extends Thread
    {
        EnergyManagerDAO dao = null;
        Hashtable jsonDataTbl = new Hashtable();
        Gateway gateway = null;
        String dguid = null;
        double price;
        double usage;
        long sliceInterval;
        int sliceCount;
        boolean randomUsage = false;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        public DeviceThread(Gateway g,String deviceId,double price, double usage,long sliceInterval,int sliceCount)
        {
            gateway = g;
            dguid = deviceId;
            this.price = price;
            this.usage = usage;
            if ( usage < 0 ) 
                randomUsage = true;
            this.sliceInterval = sliceInterval;
            this.sliceCount = sliceCount;
            jsonDataTbl.put("accumulatedKwH",0.0);
            jsonDataTbl.put("currentKwH",0.0);
            jsonDataTbl.put("currentCost",0.0);
            jsonDataTbl.put("currentPrice",price);
            jsonDataTbl.put("dailyCost",0.0);
            jsonDataTbl.put("dailyKwH",0.0);
            jsonDataTbl.put("dateTime","YYYY-MM-DD hh:mm:ss");
            jsonDataTbl.put("hourlyCost",0.0);
            jsonDataTbl.put("hourlyKwH",0.0);
            jsonDataTbl.put("monthlyCost",0.0);
            jsonDataTbl.put("monthlyKwH",0.0);
            jsonDataTbl.put("timeSlice",0);
            jsonDataTbl.put("typeCode","0301");
            try
            {
                dao = new EnergyManagerDAO();
            }
            catch (XanbooException ex)
            {
                logger.warn("[DeviceThread] Exception creating device thread", ex);
            }
        }
        @Override
        public void run()
        {
            logger.info("start device="+dguid+" for gateway"+gateway.gguid);
            incrementThreadCount(true);
            int prevSlice = 0;
            int timeSlice = 0;
            
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            StringBuilder jsonData = new StringBuilder();
            for (int sliceNbr = 1; sliceNbr <= sliceCount; sliceNbr++) 
            {
                try
                {
                    sleep(sliceInterval);
                    if ( randomUsage )
                        usage=new Random().nextDouble();
                    timeSlice = timeSlice + 1;
                    if ( timeSlice == 97 ) timeSlice=1;
                    logger.info("device="+dguid+" processing timeSlice="+sliceNbr+" of "+sliceCount);
                    
                    cal.add(Calendar.MINUTE, 15);
                    cal.add(Calendar.SECOND,-1);
                    if ( (prevSlice % 4) == 0 )  
                    {
                        cal.set(Calendar.SECOND, 59);
                        cal.set(Calendar.MILLISECOND,999);
                    }
                    
                    jsonDataTbl.put("accumulatedKwH",((Double)jsonDataTbl.get("accumulatedKwH") + usage));
                    jsonDataTbl.put("currentKwH",usage);
                    jsonDataTbl.put("currentCost",(usage*price));
                    //logger.info("prevSlice="+prevSlice+" prevSlice%4="+(prevSlice%4));
                    if ( (prevSlice % 4) == 0 )  
                    {
                        jsonDataTbl.put("hourlyCost",usage*price);
                        jsonDataTbl.put("hourlyKwH",usage);
                    }
                    else
                    {
                        jsonDataTbl.put("hourlyKwH",((Double)jsonDataTbl.get("hourlyKwH")+usage));
                        jsonDataTbl.put("hourlyCost",((Double)jsonDataTbl.get("hourlyKwH")*price));
                    }
                    if ( (prevSlice % 96) == 0 )
                    {
                        jsonDataTbl.put("dailyCost",usage*price);
                        jsonDataTbl.put("dailyKwH",usage);
                    }
                    else
                    {
                        jsonDataTbl.put("dailyKwH",((Double)jsonDataTbl.get("dailyKwH")+usage));
                        jsonDataTbl.put("dailyCost",((Double)jsonDataTbl.get("dailyKwH")*price));
                    }
                    
                    jsonDataTbl.put("monthlyKwH",((Double)jsonDataTbl.get("monthlyKwH")+usage));
                    jsonDataTbl.put("monthlyCost",((Double)jsonDataTbl.get("monthlyKwH")*price));
                    jsonDataTbl.put("timeSlice",timeSlice);
                    jsonDataTbl.put("dateTime",sdf.format(cal.getTime()));
            
                    jsonData = new StringBuilder();
                    jsonData.append("{");
                    for ( Enumeration keys = jsonDataTbl.keys(); keys.hasMoreElements(); )
                    {
                        String key = (String)keys.nextElement();
                        Object val = jsonDataTbl.get(key);
                        
                        if (jsonData.toString().length() > 1 )
                            jsonData.append((","));
                        
                        jsonData.append("\"");
                        jsonData.append(key);
                        jsonData.append("\":");
                        if ( val instanceof String )
                        {
                            jsonData.append("\"");
                            jsonData.append(val.toString());
                            jsonData.append("\"");
                        }
                        else
                            jsonData.append(val.toString());
                    }
                    jsonData.append("}");
                    //logger.info("jsonData-"+jsonData.toString());
                    prevSlice = sliceNbr;
                    incrementQueryCount(true);
                    long startTimeMillis = System.currentTimeMillis();
                    dao.updateUsageData(gateway.gguid, dguid, jsonData.toString());
                    long stopTimeMillis = System.currentTimeMillis();
                    incrementQueryCount(false);
                    addExecutionTime((stopTimeMillis - startTimeMillis));
                }
                catch (InterruptedException ex)
                {
                   logger.warn("interruptedException - ",ex);
                }
                catch (XanbooException ex)
                {
                     logger.warn("XanbooException - gateway="+gateway.gguid+" device="+this.dguid+" energy jsonData - "+jsonData,ex);
                }
            }
            incrementThreadCount(false);
        }
    }
}
