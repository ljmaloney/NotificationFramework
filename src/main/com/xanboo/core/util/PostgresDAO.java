/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/util/PostgresDAO.java,v $
 * $Id: PostgresDAO.java,v 1.1 2007/05/09 17:04:10 levent Exp $
 * 
 * Copyright 2007 Xanboo, Inc.
 *
 */

package com.xanboo.core.util;

import java.sql.*;
import javax.sql.*;
import java.math.BigDecimal;
import java.util.*;
import java.text.DateFormat;

import com.xanboo.core.sdk.util.XanbooResultSet;

/**
 * BaseDAO implementation for PostgreSQL. It provides Postgres specific method implementations
 * for all other generic DAO classes.
*/
public class PostgresDAO extends BaseDAO {
 
    private Logger logger;
    
    /**
     * Default constructor. Takes no arguments
     */
    public PostgresDAO() {
        // obtain a Logger instance
        logger=LoggerFactory.getLogger(this.getClass().getName());
        if(logger.isDebugEnabled()) {
            logger.debug("[OracleDAO()]:");
        }
    }

    
    /**
     * Method to invokes stored procedures w/o resultsets
     *
     * @param   spName    stored procedure name
     * @param   args      an array for stored procedure arguments
     * @param   commitFlag  a int flag to specify if commit will be done in the SP or not
     */
    public void callSP(Connection conn, String spName, SQLParam[] args, boolean commitFlag) throws XanbooException {
        //No packages in postgres: replace '.' in SP names with '_'
        int ix=spName.indexOf('.');
        if(ix>0) spName=spName.substring(0,ix) + "_" + spName.substring(ix+1);
        if(logger.isDebugEnabled()) {
            logger.debug("[callSP()]: calling " + spName + " (w/o RS)");
            logger.debug("SP ARGS:");
            for(int i=0; i<args.length-2; i++) {
                if(args[i].isOutput()) continue;
                logger.debug("   IN[" + i + "]=" + (args[i]==null || args[i].getParam()==null ? "NULL" : args[i].getParam().toString()));
            }
        }

        
        // standard errorcode, errormessage output params       
        args[args.length-2] = new SQLParam(new Integer(0), Types.INTEGER, true); // errno
        args[args.length-1] = new SQLParam(".", Types.VARCHAR, true);

        
        // create the statement string
        StringBuffer command = new StringBuffer("{call " + spName + "(") ;
        for(int i=0; i<args.length; i++) {
            command.append("?");
            if(i!=args.length-1)
                command.append(",");
        }
        command.append(")}");

        CallableStatement stmt=null;

        try {
            if(conn==null) {
                if(logger.isDebugEnabled()) {
                    logger.debug("[callSP()]: Connection NULL");
                }
                throw new XanbooException(20020);
            }
            
            //now add input/output params to the query
            stmt = (CallableStatement) conn.prepareCall(command.toString());
            
            for(int i=1; i<args.length+1; i++) {
               SQLParam currentArg = args[i-1];
               if(currentArg.getParamType() == Types.INTEGER) {
                   if(!currentArg.isOutput()) {   // IN parameter
                       if(currentArg.getParam()==null) {
                           stmt.setNull(i, Types.INTEGER);
                       }else {
                           stmt.setInt(i, ((Integer) currentArg.getParam()).intValue());
                       }
                   }else {                                // OUT parameter
                        stmt.registerOutParameter(i, Types.INTEGER, 0);
                   }
                   
               }else if(currentArg.getParamType() == Types.BIGINT) {
                   if(!currentArg.isOutput()) {   // IN parameter
                       if(currentArg.getParam()==null) {
                           ////stmt.setNull(i, Types.BIGINT);
                           stmt.setNull(i, Types.INTEGER);
                       }else {
                           ////stmt.setLong(i, ((Long) currentArg.getParam()).longValue());
                           stmt.setInt(i, ((Long) currentArg.getParam()).intValue());
                       }
                   }else {                                // OUT parameter
                        ////-LT  disabling, must use BIGINT !!!
                        ////stmt.registerOutParameter(i, Types.BIGINT, 0);
                        stmt.registerOutParameter(i, Types.INTEGER, 0);
                   }
                   
               }else if(currentArg.getParamType() == Types.FLOAT) {
                   if(!currentArg.isOutput()) {   // IN parameter
                       if(currentArg.getParam()==null) {
                           stmt.setNull(i, Types.NUMERIC);
                       }else {
                           stmt.setBigDecimal(i, (new BigDecimal(((Float)currentArg.getParam()).floatValue())));
                       }
                   }else {                                // OUT parameter
                        stmt.registerOutParameter(i, Types.NUMERIC, 5);
                   }
                   
               }else if(currentArg.getParamType() == Types.DOUBLE) {
                   if(!currentArg.isOutput()) {   // IN parameter
                       if(currentArg.getParam()==null) {
                           stmt.setNull(i, Types.NUMERIC);
                       }else {
                           stmt.setBigDecimal(i, (new BigDecimal(((Double)currentArg.getParam()).doubleValue())));
                       }
                   }else {                                // OUT parameter
                        stmt.registerOutParameter(i, Types.NUMERIC, 5);
                   }
                   
               }else if(currentArg.getParamType() == Types.DATE) {
                   if(!currentArg.isOutput()) {   // IN parameter
                       if(currentArg.getParam()==null) {
                           stmt.setNull(i, Types.TIMESTAMP);
                       }else {
                           stmt.setTimestamp(i, new java.sql.Timestamp(((java.util.Date) currentArg.getParam()).getTime()));
                       }
                   }else {                                // OUT parameter
                        stmt.registerOutParameter(i, Types.TIMESTAMP);
                   }
                   
               }else if(currentArg.getParamType() == Types.NULL){
                   if(!currentArg.isOutput()) {   // IN parameter
                        stmt.setNull(i, Types.VARCHAR);
                   }
                   
               }else {
                   if(!currentArg.isOutput()) {   // IN parameter
                       if(currentArg.getParam()==null) {
                           stmt.setNull(i, Types.VARCHAR);
                       }else {
                           stmt.setString(i, currentArg.getParam().toString());
                       }
                   }else {                                // OUT parameter
                        stmt.registerOutParameter(i, Types.VARCHAR);
                   }
               }
            }   // end for loop

            
            stmt.execute();

            for(int i=1; i<args.length+1; i++) {
               SQLParam currentArg = (SQLParam) args[i-1];
               if(currentArg.isOutput()) {
                  if(currentArg.getParamType() == Types.INTEGER) {
                     args[i-1] = new SQLParam(new Integer(stmt.getInt(i)), Types.INTEGER);
                     
                  }else if(currentArg.getParamType() == Types.BIGINT) {
                     ////args[i-1] = new SQLParam(new Long(stmt.getLong(i)), Types.BIGINT);
                     args[i-1] = new SQLParam(new Long((long) stmt.getInt(i)), Types.BIGINT);
                     
                  }else if(currentArg.getParamType() == Types.FLOAT) {
                      if(stmt.getBigDecimal(i)==null)
                         args[i-1] = new SQLParam(null, Types.FLOAT);
                      else
                         args[i-1] = new SQLParam(new Float(stmt.getBigDecimal(i).floatValue()), Types.FLOAT);
                      
                  }else if(currentArg.getParamType() == Types.DOUBLE) {
                      if(stmt.getBigDecimal(i)==null)
                         args[i-1] = new SQLParam(null, Types.DOUBLE);
                      else
                         args[i-1] = new SQLParam(new Double(stmt.getBigDecimal(i).doubleValue()), Types.DOUBLE);

                  }else if(currentArg.getParamType() == Types.DATE) {
                     args[i-1] = new SQLParam(new java.util.Date(stmt.getTimestamp(i).getTime()), Types.DATE);
                  
                  }else {
                     if(stmt.getString(i) != null)
                        args[i-1] = new SQLParam(stmt.getString(i));
                  }
               }
            }

            closeStatement(stmt);
        }catch(SQLException ae) {
            if(logger.isDebugEnabled()) {
                logger.debug("[callSP()]: SQL Exception:", ae);
            }            
            closeStatement(stmt);
            throw new XanbooException(20080, ae.getLocalizedMessage());
        }

        // if query failed, throw a XanbooException with code and msg
        int code=((Integer) args[args.length-2].getParam()).intValue();
        String msg=args[args.length-1].getParam().toString();
        if(code!=0) {
            if(logger.isDebugEnabled()) {
                logger.debug("[callSP()]: Xanboo Exception CODE:" + code + ", MSG:" + msg);
            }            
            throw new XanbooException(code,msg);
        }

        if(logger.isDebugEnabled()) {
            logger.debug("[callSP()]: END " + spName + " (w/o RS)");
        }
        
    }    
    
    
    
    /**
     * Method to invoke query stored procedures. Returns query results in an ArrayList collection.
     *
     * @param   spName    stored procedure name
     * @param   args      an array of stored procedure arguments
     * @param   startRow  starting row for the returned results  (starts with 0)
     * @param   numRows   max number of rows to be returned 
     *
     * @return  XanbooResultSet (ArrayList) object for the query results
     */
    public XanbooResultSet callSP(Connection conn, String spName, SQLParam[] args, int startRow, int numRows) throws XanbooException {
        //No packages in postgres: replace '.' in SP names with '_'
        int ix=spName.indexOf('.');
        if(ix>0) spName=spName.substring(0,ix) + "_" + spName.substring(ix+1);

        if(logger.isDebugEnabled()) {
            logger.debug("[callSP()]: calling " + spName + " (w/ RS)");
            logger.debug("SP ARGS:");
            for(int i=0; i<args.length-2; i++) {
                if(args[i].isOutput()) continue;
                logger.debug("   IN[" + i + "]=" + (args[i]==null || args[i].getParam()==null ? "NULL" : args[i].getParam().toString()));
            }
        }

        if(startRow<0) startRow=0;
        if(numRows==0) numRows=1;
        
        // standard errorcode, errormessage output params       
        args[args.length-2] = new SQLParam(new Integer(0), Types.INTEGER, true); // errno
        args[args.length-1] = new SQLParam(".", Types.VARCHAR, true);

        // create the statement string
        StringBuffer command = new StringBuffer("{ ? = call " + spName + "(") ;
        boolean firstOne=true;
        for(int i=0; i<args.length; i++) {
            if(args[i].isOutput()) continue;
            if(!firstOne) command.append(",");
            command.append("?");
            firstOne=false;
        }
        command.append(") }");

        
        CallableStatement stmt = null;
        ResultSet rscursor=null;

        try {
            if(conn==null) {
                if(logger.isDebugEnabled()) {
                    logger.debug("[callSP()]: Connection NULL");
                }
                throw new XanbooException(20020);
            }

            stmt = conn.prepareCall(command.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            stmt.setFetchSize(50);

            //register the cursor first
            stmt.registerOutParameter(1, Types.OTHER);

            //now add input params to the SP
            for(int i=2; i<args.length+2; i++) {
               SQLParam currentArg = args[i-2];
               if(currentArg.isOutput()) continue;  //only process input params
               
               if(currentArg.getParamType() == Types.INTEGER) {
                   if(currentArg.getParam()==null) {
                       stmt.setNull(i, Types.INTEGER);
                   }else {
                       stmt.setInt(i, ((Integer) currentArg.getParam()).intValue());
                   }
                   
               }else if(currentArg.getParamType() == Types.BIGINT) {
                   if(currentArg.getParam()==null) {
                       ////stmt.setNull(i, Types.BIGINT);
                       stmt.setNull(i, Types.INTEGER);
                   }else {
                       ////stmt.setLong(i, ((Long) currentArg.getParam()).longValue());
                       stmt.setInt(i, ((Long) currentArg.getParam()).intValue());
                   }
                   
               }else if(currentArg.getParamType() == Types.FLOAT) {
                   if(currentArg.getParam()==null) {
                       stmt.setNull(i, Types.NUMERIC);
                   }else {
                       stmt.setBigDecimal(i, (new BigDecimal(((Float)currentArg.getParam()).floatValue())));
                   }
                   
               }else if(currentArg.getParamType() == Types.DOUBLE) {
                   if(currentArg.getParam()==null) {
                       stmt.setNull(i, Types.NUMERIC);
                   }else {
                       stmt.setBigDecimal(i, (new BigDecimal(((Double)currentArg.getParam()).doubleValue())));
                   }
                   
               }else if(currentArg.getParamType() == Types.DATE) {
                   if(currentArg.getParam()==null) {
                       stmt.setNull(i, Types.TIMESTAMP);
                   }else {
                       stmt.setTimestamp(i, new java.sql.Timestamp(((java.util.Date) currentArg.getParam()).getTime()));
                   }
                   
               }else if(currentArg.getParamType() == Types.NULL){
                    stmt.setNull(i, Types.VARCHAR);
                   
               }else {
                   if(currentArg.getParam()==null) {
                       stmt.setNull(i, Types.VARCHAR);
                   }else {
                       stmt.setString(i, currentArg.getParam().toString());
                   }
               }
            }   // end for loop

            
            // now execute the statement, and get output parameters in a 1-row CURSORSET return
            try {
                stmt.execute();
                rscursor = (ResultSet) stmt.getObject(1);
            }catch(Exception e) { 
                if(logger.isDebugEnabled()) {
                    logger.debug("[callSP()]: Exception:", e);
                }                            
                closeStatement(stmt);
                throw new XanbooException(20080, e.getLocalizedMessage());
                
            }
            
            //-- Queries will always return a cursor since Postgres does not support
            //-- OUT params and cursor returns together
            //-- On error: O_ERRNO and O_ERRMSG columns will be returned, and O_ERRNO
            //-- will have a positive integer value
            //-- Other output params will be returned with column names O_OUTn, where
            //-- n is the stored procedure parameter index starting with 0 !
            
            XanbooResultSet queryResult = new XanbooResultSet();
            
            ResultSetMetaData rsMeta=rscursor.getMetaData();
            int colCount=rsMeta.getColumnCount();
            int rowCount=0;

            int errorCode=0;
            boolean firstTime=true;
            while(rscursor.next()) {

                //MUST READ the first ROW to detect if this is an error cursor or not
                //add row to the query result
                HashMap hashRow=new HashMap();
                errorCode=0;
                for(int i=1; i<colCount+1; i++) {
                    String columnName = rsMeta.getColumnName(i).toUpperCase();
                    
                    //System.out.println("***********COL NAME: " + columnName);
                    if(columnName != null) {
                        //check if this is an error column
                        if(columnName.equals("O_ERRNO")) {
                            errorCode=rscursor.getInt(i);
                            continue;
                        
                        //check if this is an out parameter column
                        //since out params are same on all rows, do this only on first row
                        }else if(errorCode==0 && (rowCount-startRow)==0 && columnName.startsWith("O_OUT") && columnName.length()>5) {
                            try {
                                int parIx = Integer.parseInt(columnName.substring(5));
                                SQLParam currentArg = args[parIx];
                                if(currentArg.isOutput()) {
                                  if(currentArg.getParamType() == Types.INTEGER) {
                                     currentArg.setParam(new Integer(rscursor.getInt(i)));
                                  }else if(currentArg.getParamType() == Types.BIGINT) {
                                     ////currentArg.setParam(new Long(rscursor.getLong(i)));
                                     currentArg.setParam(new Long((long) rscursor.getInt(i)));
                                  }else if(currentArg.getParamType() == Types.FLOAT) {
                                     currentArg.setParam(new Float(rscursor.getFloat(i)));
                                  }else if(currentArg.getParamType() == Types.DOUBLE) {
                                     currentArg.setParam(new Double(rscursor.getDouble(i)));
                                  }else if(currentArg.getParamType() == Types.DATE) {
                                     currentArg.setParam(rscursor.getTimestamp(i));
                                  }else if(rscursor.getString(i) != null) {
                                     currentArg.setParam(rscursor.getString(i));
                                  }
                                }
                            //ignore exceptions for output param processing
                            }catch(Exception eee) { 
                                //eee.printStackTrace();
                            }
                        }
                        
                        //!!!continue so that out params are not added to resultset
                        if(columnName.startsWith("O_OUT")) continue;
                        
                        //if null value
                        if(rscursor.getString(i) == null) {
                            hashRow.put(columnName, "");
                            continue;
                        }
                        
                        if(rsMeta.getColumnType(i) == Types.INTEGER) {
                            hashRow.put(columnName, Integer.toString(rscursor.getInt(i)));

                        }else if(rsMeta.getColumnType(i) == Types.BIGINT) {
                            ////hashRow.put(columnName, Long.toString(rscursor.getLong(i)));
                            hashRow.put(columnName, Integer.toString(rscursor.getInt(i)));

                        }else if(rsMeta.getColumnType(i) == Types.FLOAT) {
                            hashRow.put(columnName, Float.toString(rscursor.getFloat(i)));

                        }else if(rsMeta.getColumnType(i) == Types.DOUBLE) {
                            hashRow.put(columnName, Double.toString(rscursor.getDouble(i)));
                            
                        }else if(rsMeta.getColumnType(i) == Types.NUMERIC) {
                            hashRow.put(columnName, rscursor.getBigDecimal(i).toString());
                            
                        }else if(rsMeta.getColumnType(i) == Types.TIMESTAMP || rsMeta.getColumnType(i) == Types.DATE) {
                            ////hashRow.put(columnName, XanbooUtil.getISO8601(rscursor.getTimestamp(i)));
                            hashRow.put(columnName, rscursor.getTimestamp(i).toString());
                            
                        }else if(rsMeta.getColumnType(i) == Types.VARCHAR) {
                            hashRow.put(columnName, rscursor.getString(i));
                        }
                        
                    }//end if
                }//end for loop (for each query result column)
                
                //throw XanbooException if there was an error code returned !!!!!
                if(errorCode>0) {
                    closeResultSet(rscursor);
                    closeStatement(stmt);
                    if(hashRow.get("O_ERRMSG")==null) {
                        if(logger.isDebugEnabled()) {
                            logger.debug("[callSP()]: Xanboo Exception CODE:" + errorCode);
                        }            
                        throw new XanbooException(errorCode);
                    }else {
                        if(logger.isDebugEnabled()) {
                            logger.debug("[callSP()]: Xanboo Exception CODE:" + errorCode + ", MSG=" + (String) hashRow.get("O_ERRMSG"));
                        }            
                        throw new XanbooException(errorCode, (String) hashRow.get("O_ERRMSG"));
                    }
                }
                
                if(firstTime && startRow>0) {
                    //try to absolute() position and check if it worked or not
                    rscursor.absolute(startRow);
                    if(rscursor.getRow()==(startRow)) {
                        rowCount=startRow;
                        firstTime=false;
                        continue;
                    }
                }
                firstTime=false;
                
                // advance record until startrow, in case absolute() positioning did not work !
                if(startRow > rowCount) {
                    rowCount++;
                    continue;
                }
                
                // no error, add the row to our queryResult array
                queryResult.add((rowCount-startRow), hashRow);
                rowCount++;
                if(numRows!=-1 && ((rowCount-startRow) >= numRows)) break;
                    
            } // end rscursor.next()
            
            closeResultSet(rscursor);
            closeStatement(stmt);

            if(logger.isDebugEnabled()) {
                logger.debug("[callSP()]: END " + spName + " (w/ RS)");
            }

            //NO/NULL QUERY RESULTS, what to do with OUT PARAMS
            //set integers to 0 (for totalCounts) for now - BAD!
            if(queryResult.size()==0) {
                for(int i=0; i<args.length; i++) {
                   SQLParam currentArg = args[i];
                   if(currentArg.isOutput() && currentArg.getParamType() == Types.INTEGER) {
                         currentArg.setParam(new Integer(0));
                   }
                }
            }
            
            return queryResult;
            
        }catch(SQLException ae) {
            if(logger.isDebugEnabled()) {
                logger.debug("[callSP()]: SQL Exception:", ae);
            }            
            
            if(rscursor!=null) closeResultSet(rscursor);
            closeStatement(stmt);
            throw new XanbooException(20080, ae.getLocalizedMessage());
        }

        
    }

    
}
