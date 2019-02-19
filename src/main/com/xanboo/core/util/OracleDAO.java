/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/util/OracleDAO.java,v $
 * $Id: OracleDAO.java,v 1.23 2009/03/13 18:45:10 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.util;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import oracle.jdbc.OracleTypes;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import com.xanboo.core.sdk.util.XanbooResultSet;
import static com.xanboo.core.util.SQLParam.IN_OUT;


/**
 * BaseDAO implementation for Oracle. It provides Oracle specific method implementations
 * for all other generic DAO classes.
*/
public class OracleDAO extends BaseDAO {
    
    private Logger logger;
    
    
    /**
     * Default constructor. Takes no arguments
     */
    public OracleDAO() throws XanbooException {
        
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
        if(logger.isDebugEnabled()) {
            logger.debug("[callSP()]: calling " + spName + " (w/o RS)");
            
            //for(int i=0; i<args.length-2; i++) {
            //    logger.debug("   ARG[" + i + "]=" + (args[i]==null || args[i].getParam()==null ? "NULL" : args[i].getParam().toString()));
            //}
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

        CallableStatement stmt=null;   // oracle specific (in order to support VARRAYs) - previously OracleCallableStatement
        ArrayList inputStreams = new ArrayList();   //to keep track of open input streams for LOB handling
        try {
            if(conn==null) {
                if(logger.isDebugEnabled()) {
                    logger.debug("[callSP()]: Connection NULL");
                }
                throw new XanbooException(20020);
            }
            
            stmt = (CallableStatement) conn.prepareCall(command.toString()); //previously OracleCallableStatement
            
            for(int i=1; i<args.length+1; i++) {
               SQLParam currentArg = args[i-1];
               if(currentArg.getParamType() == Types.INTEGER) {
            	   if(currentArg.isOutput() || IN_OUT == currentArg.getRetType()) {
            		   stmt.registerOutParameter(i, Types.NUMERIC, 0); // OUT parameter
            	   }
            	   
            	   if((!currentArg.isOutput()) || IN_OUT == currentArg.getRetType()) {
            		   if(currentArg.getParam()==null) { // IN parameter
                           stmt.setNull(i, Types.INTEGER);
                       }else {
                           stmt.setInt(i, ((Integer) currentArg.getParam()).intValue());
                       }
            	   }
               }else if(currentArg.getParamType() == Types.BIGINT) {
                   if(currentArg.isOutput() || IN_OUT == currentArg.getRetType()) {
                	   stmt.registerOutParameter(i, Types.NUMERIC, 0); // OUT parameter
            	   }
            	   
            	   if((!currentArg.isOutput()) || IN_OUT == currentArg.getRetType()) {
            		   if(currentArg.getParam()==null) {  // IN parameter
                           stmt.setNull(i, Types.NUMERIC);
                       }else {
                           stmt.setLong(i, ((Long) currentArg.getParam()).longValue());
                       }
            	   }
               }else if(currentArg.getParamType() == Types.FLOAT) {
            	   if(currentArg.isOutput() || IN_OUT == currentArg.getRetType()) {
                	   stmt.registerOutParameter(i, Types.NUMERIC, 0); // OUT parameter
            	   }
            	   
            	   if((!currentArg.isOutput()) || IN_OUT == currentArg.getRetType()) {
            		   if(currentArg.getParam()==null) {  // IN parameter
                           stmt.setNull(i, Types.NUMERIC);
                       }else {
                    	   stmt.setFloat(i, ((Float) currentArg.getParam()).floatValue());
                       }
            	   }
               }else if(currentArg.getParamType() == Types.DOUBLE) {
            	   if(currentArg.isOutput() || IN_OUT == currentArg.getRetType()) {
                	   stmt.registerOutParameter(i, Types.NUMERIC, 0); // OUT parameter
            	   }
            	   
            	   if((!currentArg.isOutput()) || IN_OUT == currentArg.getRetType()) {
            		   if(currentArg.getParam()==null) {  // IN parameter
                           stmt.setNull(i, Types.NUMERIC);
                       }else {
                    	   stmt.setDouble(i, ((Double) currentArg.getParam()).doubleValue());
                       }
            	   }
               }else if(currentArg.getParamType() == Types.DATE) {
            	   if(currentArg.isOutput() || IN_OUT == currentArg.getRetType()) {
            		   stmt.registerOutParameter(i, Types.DATE); // OUT parameter
            	   }
            	   
            	   if((!currentArg.isOutput()) || IN_OUT == currentArg.getRetType()) {
            		   if(currentArg.getParam()==null) {  // IN parameter
                           stmt.setNull(i, Types.TIMESTAMP);
                       }else {
                    	   stmt.setTimestamp(i, new java.sql.Timestamp(((java.util.Date)currentArg.getParam()).getTime()));
                       }
            	   }
              }else if(currentArg.getParamType() == Types.ARRAY) {
                   ArrayDescriptor desc=null;
                   if(currentArg.getParamType() == Types.ARRAY) {
                       desc = ArrayDescriptor.createDescriptor("NUMARRAY", conn);
                   }
                   ARRAY thisArray = new ARRAY(desc, conn, currentArg.getParam());
                   if(!currentArg.isOutput()) {
                      stmt.setObject(i, thisArray ); //previously stmt.setARRAY
                   }
              }else if(currentArg.getParamType() == Types.BLOB) {
                   if(currentArg.isOutput() || IN_OUT == currentArg.getRetType()) {
                	   stmt.registerOutParameter(i, Types.BLOB); // OUT parameter
            	   }
            	   
            	   if((!currentArg.isOutput()) || IN_OUT == currentArg.getRetType()) {
            		   if(currentArg.getParam()==null) {  // IN parameter
            			   stmt.setNull(i, Types.BLOB);
                       }else {
                    	   byte[] blobBytes = (byte[]) currentArg.getParam();
                           ByteArrayInputStream in = new ByteArrayInputStream( blobBytes );
                           inputStreams.add(in);
                           stmt.setBinaryStream(i, in, (int) blobBytes.length); 
                       }
            	   }
               }else if(currentArg.getParamType() == Types.CLOB) {
                  
            	   if(currentArg.isOutput() || IN_OUT == currentArg.getRetType()) {
                	   stmt.registerOutParameter(i, Types.CLOB); // OUT parameter
            	   }
            	   
            	   if((!currentArg.isOutput()) || IN_OUT == currentArg.getRetType()) {  // IN parameter
                	   if(currentArg.getParam()==null) {  // IN parameter
            			   stmt.setNull(i, Types.CLOB);
                       }else {
                    	   byte[] clobBytes = (byte[]) currentArg.getParam();
                           ByteArrayInputStream in = new ByteArrayInputStream( clobBytes );
                           inputStreams.add(in);
                           InputStreamReader inr = new InputStreamReader(in);
                           stmt.setCharacterStream(i, inr);
                       }
            	   }
               }else if(currentArg.getParamType() == Types.NULL){
                   if(!currentArg.isOutput()) {   // IN parameter
                        stmt.setNull(i, Types.VARCHAR);
                   }
               }else {
                   if(currentArg.isOutput() || IN_OUT == currentArg.getRetType()) {
                	   stmt.registerOutParameter(i, Types.VARCHAR); // OUT parameter
            	   }
            	   
            	   if((!currentArg.isOutput()) || IN_OUT == currentArg.getRetType()) {
            		   if(currentArg.getParam()==null) {
                           stmt.setNull(i, Types.VARCHAR);
                       }else {
                           stmt.setString(i, currentArg.getParam().toString());
                       }
            	   }
               }

            }   // end for loop

            stmt.execute();
            

            for(int i=1; i<args.length+1; i++) {
               SQLParam currentArg = (SQLParam) args[i-1];
               if(currentArg.isOutput() || IN_OUT == currentArg.getRetType()) {
                  if(currentArg.getParamType() == Types.BIGINT) {
                     args[i-1] = new SQLParam(new Long(stmt.getLong(i)), Types.BIGINT);
                  }else if(currentArg.getParamType() == Types.INTEGER) {
                     args[i-1] = new SQLParam(new Integer(stmt.getInt(i)), Types.INTEGER);
                  }else if(currentArg.getParamType() == Types.FLOAT) {
                     args[i-1] = new SQLParam(new Float(stmt.getFloat(i)), Types.FLOAT);
                  }else if(currentArg.getParamType() == Types.DOUBLE) {
                     args[i-1] = new SQLParam(new Double(stmt.getDouble(i)), Types.DOUBLE);
                  }else if(currentArg.getParamType() == Types.DATE) {
                     args[i-1] = new SQLParam(stmt.getDate(i), Types.DATE);
                  }else if(currentArg.getParamType() == Types.BLOB) {
                      Blob blob = stmt.getBlob(i);
                      if(blob!=null && blob.length()>0)
                          args[i-1] = new SQLParam(blob.getBytes(1, (int)blob.length()), Types.BLOB);
                      else
                          args[i-1] = new SQLParam(null, Types.BLOB);
                      if(blob!=null) blob.free();
                  }else if(currentArg.getParamType() == Types.CLOB) {
                      Clob clob = stmt.getClob(i);
                      if(clob!=null && clob.length()>0) {
                          args[i-1] = new SQLParam(clob.getSubString(1, (int)clob.length()), Types.CLOB);
                      }else {
                          args[i-1] = new SQLParam(null, Types.CLOB);
                      }
                      if(clob!=null) clob.free();
                  }else {
                     if(stmt.getString(i) != null)
                        args[i-1] = new SQLParam(stmt.getString(i));
                  }
               }
            }

        }catch(SQLException ae) {
            if(logger.isDebugEnabled()) {
                logger.debug("[callSP()]: SQL Exception:", ae);
            }            
            throw new XanbooException(20080, ae.getLocalizedMessage());
        }finally {
            if(stmt!=null) closeStatement(stmt);
            //release input streams used for lob processing
            for(int i=0; i<inputStreams.size();i++) {
                ByteArrayInputStream in = (ByteArrayInputStream) inputStreams.get(i);
                if(in!=null) {
                    try { in.close(); }catch(Exception ee) {}                
                    
                }
            }
        }

        // if query failed, throw a XanbooException with code and msg
        int code=((Integer) args[args.length-2].getParam()).intValue();
        String msg=args[args.length-1].getParam().toString();
        if(code!=GlobalNames.XAIL_CODE_ACK) {
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
     * @param   startRow  starting row for the returned results 
     * @param   numRows   max number of rows to be returned 
     *
     * @return  XanbooResultSet (ArrayList) object for the query results
     */
    public XanbooResultSet callSP(Connection conn, String spName, SQLParam[] args, int startRow, int numRows) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[callSP()]: calling " + spName + " (w/ RS)");
            //for(int i=0; i<args.length-2; i++) {
            //    logger.debug("   ARG[" + i + "]=" + (args[i]==null || args[i].getParam()==null ? "NULL" : args[i].getParam().toString()));
            //}
        }

        // standard errorcode, errormessage output params       
        args[args.length-2] = new SQLParam(new Integer(0), Types.INTEGER, true); // errno
        args[args.length-1] = new SQLParam(".", Types.VARCHAR, true);

        // create the statement string
        StringBuffer command = new StringBuffer("{? = call " + spName + "(") ;
        for(int i=0; i<args.length; i++) {
            command.append("?");
            if(i!=args.length-1)
                command.append(",");
        }
        command.append(")}");

        CallableStatement stmt=null; // oracle specific (in order to support VARRAYs) - previously OracleCallableStatement

        try {
            if(conn==null) {
                if(logger.isDebugEnabled()) {
                    logger.debug("[callSP()]: Connection NULL");
                }
                throw new XanbooException(20020);
            }
            
            stmt = (CallableStatement) conn.prepareCall(command.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY); //previously OracleCallableStatement
            stmt.registerOutParameter(1, OracleTypes.CURSOR);   // ORACLE specific cursor type, no other choice !!!
            
            for(int i=2; i<args.length+2; i++) {
               SQLParam currentArg = args[i-2];
               if(currentArg.getParamType() == Types.INTEGER) {
                   if(!currentArg.isOutput()) {   // IN parameter
                      if(currentArg.getParam()==null) {
                          stmt.setNull(i, Types.INTEGER);
                      }else {
                          stmt.setInt(i, ((Integer) currentArg.getParam()).intValue());
                      }
                   }else {                                // OUT parameter
                        stmt.registerOutParameter(i, Types.NUMERIC, 0);
                   }
               }else if(currentArg.getParamType() == Types.BIGINT) {
                   if(!currentArg.isOutput()) {   // IN parameter
                      if(currentArg.getParam()==null) {
                          stmt.setNull(i, Types.NUMERIC);
                      }else {
                          stmt.setLong(i, ((Long) currentArg.getParam()).longValue());
                      }
                   }else {                                // OUT parameter
                        stmt.registerOutParameter(i, Types.NUMERIC, 0);
                   }
               }else if(currentArg.getParamType() == Types.FLOAT) {
                   if(!currentArg.isOutput()) {   // IN parameter
                      if(currentArg.getParam()==null) {
                          stmt.setNull(i, Types.NUMERIC);
                      }else {
                          stmt.setFloat(i, ((Float) currentArg.getParam()).floatValue());
                      }
                   }else {                                // OUT parameter
                        stmt.registerOutParameter(i, Types.NUMERIC, 3);
                   }
               }else if(currentArg.getParamType() == Types.DOUBLE) {
                   if(!currentArg.isOutput()) {   // IN parameter
                      if(currentArg.getParam()==null) {
                          stmt.setNull(i, Types.NUMERIC);
                      }else {
                          stmt.setDouble(i, ((Double) currentArg.getParam()).doubleValue());
                      }
                   }else {                                // OUT parameter
                        stmt.registerOutParameter(i, Types.NUMERIC, 3);
                   }
               }else if(currentArg.getParamType() == Types.DATE) {
                   if(!currentArg.isOutput()) {   // IN parameter
                       if(currentArg.getParam()==null) {
                           stmt.setNull(i, Types.TIMESTAMP);
                       }else {
                           stmt.setTimestamp(i, new java.sql.Timestamp(((java.util.Date)currentArg.getParam()).getTime()));
                       }
                   }else {                                // OUT parameter
                        stmt.registerOutParameter(i, Types.DATE);
                   }
               }else if(currentArg.getParamType() == Types.ARRAY) {
                   ArrayDescriptor desc=null;
                   if(currentArg.getParamType() == Types.ARRAY) {
                       desc = ArrayDescriptor.createDescriptor("NUMARRAY", conn);
                   }
                   ARRAY thisArray = new ARRAY(desc, conn, currentArg.getParam());
                   if(!currentArg.isOutput()) {
                       stmt.setObject(i, thisArray); //previously setARRAY
                   }
               }else if(currentArg.getParamType() == Types.BLOB) {
                   throw new XanbooException(20081, "Query calls do not support LOB types as input parameters");
               }else if(currentArg.getParamType() == Types.CLOB) {
                   throw new XanbooException(20081, "Query calls do not support LOB types as input parameters");
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

            
            ResultSet rset=null;
//            ArrayList queryResult = new ArrayList();
            XanbooResultSet queryResult = new XanbooResultSet();
            
            try {
                stmt.execute();
            }catch(Exception e) { 
                if(logger.isDebugEnabled()) {
                    logger.debug("[callSP()]: Exception:", e);
                }                            
                if(stmt!=null) closeStatement(stmt);
                throw new XanbooException(20080, e.getLocalizedMessage());
            }

            // getting the output parameters
            for(int i=2; i<args.length+2; i++) {
               SQLParam currentArg = args[i-2];
               if(currentArg.isOutput()) {
                  if(currentArg.getParamType() == Types.BIGINT) {
                     args[i-2] = new SQLParam(new Long(stmt.getLong(i)), Types.BIGINT);
                  }else if(currentArg.getParamType() == Types.INTEGER) {
                     args[i-2] = new SQLParam(new Integer(stmt.getInt(i)), Types.INTEGER);
                  }else if(currentArg.getParamType() == Types.FLOAT) {
                     args[i-2] = new SQLParam(new Float(stmt.getFloat(i)), Types.FLOAT);
                  }else if(currentArg.getParamType() == Types.DOUBLE) {
                     args[i-2] = new SQLParam(new Double(stmt.getDouble(i)), Types.DOUBLE);
                  }else if(currentArg.getParamType() == Types.DATE) {
                     args[i-2] =  new SQLParam(stmt.getDate(i), Types.DATE);
                  }else if(currentArg.getParamType() == Types.BLOB) {
                      Blob blob = stmt.getBlob(i);
                      if(blob!=null && blob.length()>0)
                          args[i-2] = new SQLParam(blob.getBytes(1, (int)blob.length()), Types.BLOB);
                      else
                          args[i-2] = new SQLParam(null, Types.BLOB);
                      if(blob!=null) blob.free();
                  }else if(currentArg.getParamType() == Types.CLOB) {
                      Clob clob = stmt.getClob(i);
                      if(clob!=null && clob.length()>0)
                          args[i-2] = new SQLParam(clob.getSubString(1, (int)clob.length()), Types.CLOB);
                      else
                          args[i-2] = new SQLParam(null, Types.CLOB);
                      if(clob!=null) clob.free();
                  }else {
                     if(stmt.getString(i) != null)
                        args[i-2] =  new SQLParam(stmt.getString(i));
                  }
               }
            }
            
            // now get the cursor and query results
            boolean failed=false;
            try {
                rset = (ResultSet)stmt.getObject(1); // Oracle Specific - previously getCursor
            }catch(Exception e) { 
                failed=true;    // invalid cursor, just ignore
            }
            logger.info("Theresetresult = "+ rset);
            if(!failed && rset != null) { //null check
                ResultSetMetaData rsetMeta=rset.getMetaData();
                int colCount=rsetMeta.getColumnCount();
    /*
                for(int i=1; i<colCount+1; i++) {
                        Debug.println("COLUMN[" + i + "]," + rsetMeta.getColumnName(i));
                        Debug.println("TYPE[" + i + "]," + rsetMeta.getColumnType(i));
                }
    */

                int rowCount=0;
                while(rset.next()) {
                    if(rowCount==9) rset.setFetchSize(FETCH_SIZE_MAX);       //increase fetch size for this query, if more than 10recs returned
                    
                    // seek to the first row requested
                    if(startRow > rowCount) {
                        rowCount++;
                        continue;
                    }
                    HashMap hashRow=new HashMap();
                    for(int i=1; i<colCount+1; i++) {
                        String columnName = rsetMeta.getColumnName(i);
                        if(columnName != null) {
                            hashRow.put(columnName, "");
                            if(rsetMeta.getColumnType(i) == Types.NUMERIC) {
                                if(rset.getBigDecimal(i)!=null) {
                                    hashRow.put(columnName, rset.getBigDecimal(i).toString());
                                }
                            /*
                            }else if(rsetMeta.getColumnType(i) == Types.DATE) {
                                if(rset.getDate(i)!=null) {
                                    //hashRow.put(columnName, rset.getDate(i).toString());
                                    hashRow.put(columnName, XanbooUtil.getISO8601(rset.getDate(i)));
                                }
                            */
                            }else if(rsetMeta.getColumnType(i) == Types.TIMESTAMP || rsetMeta.getColumnType(i) == Types.DATE) {
                                if(rset.getTimestamp(i) != null) {
                                    //hashRow.put(columnName, DateFormat.getDateInstance().format(rset.getTimestamp(i)).toString());
                                    hashRow.put(columnName, XanbooUtil.getISO8601(rset.getTimestamp(i)));
                                }
                            }else if(rsetMeta.getColumnType(i) == Types.VARCHAR) {
                                if(rset.getString(i) != null) {
                                    hashRow.put(columnName, rset.getString(i));
                                }
                            }else if(rsetMeta.getColumnType(i) == Types.BLOB) {
                                Blob blob = rset.getBlob(i);
                                if(blob!=null && blob.length()>0) {
                                    hashRow.put(columnName, blob.getBytes(1, (int)blob.length()));
                                }
                                if(blob!=null) blob.free();
                            }else if(rsetMeta.getColumnType(i) == Types.CLOB) {
                                Clob clob = rset.getClob(i);
                                if(clob!=null && clob.length()>0) {
                                    hashRow.put(columnName, clob.getSubString(1, (int)clob.length()));
                                }
                                if(clob!=null) clob.free();
                            }
                        } 
                    } // end for loop

                    // now add the row to our queryResult array
                    queryResult.add((rowCount-startRow), hashRow);
                    rowCount++;
                    if(numRows!=-1 && ((rowCount-startRow) >= numRows)) break;
                }
            }

/*            
            // looping thru the rest of the RS, just to get total row count
            while(rset.next()) {
                rowCount++;
            }
*/          
            closeResultSet(rset);
            closeStatement(stmt);
            
            // if query failed, throw a XanbooException with code and msg
            int code=((Integer) args[args.length-2].getParam()).intValue();
            String msg=args[args.length-1].getParam().toString();
            if(code!=GlobalNames.XAIL_CODE_ACK) {
                if(logger.isDebugEnabled()) {
                    logger.debug("[callSP()]: Xanboo Exception CODE:" + code + ", MSG:" + msg);
                }            
                throw new XanbooException(code,msg);
            }

            if(logger.isDebugEnabled()) {
                logger.debug("[callSP()]: END " + spName + " (w/ RS)");
            }

            // return query results
            return queryResult;

        }catch(SQLException ae) {
            if(logger.isDebugEnabled()) {
                logger.debug("[callSP()]: SQL Exception:", ae);
            }            
            
            throw new XanbooException(20080, ae.getLocalizedMessage());
        }finally {
            if(stmt!=null) closeStatement(stmt);
        }
        
    }    

    
    /**
     * Method to invoke query stored procedures. Returns query results in an ArrayList collection.
     *
     * @param   spName    stored procedure name
     * @param   args      an array of stored procedure arguments
     *
     * @return  XanbooResultSet (ArrayList) object for the query results
     */
    public XanbooResultSet callSP(Connection conn, String spName, SQLParam[] args) throws XanbooException {
        if(logger.isDebugEnabled()) {
            logger.debug("[callSP2()]: calling " + spName + " (w/ RS)");
            //for(int i=0; i<args.length-2; i++) {
            //    logger.debug("   ARG[" + i + "]=" + (args[i]==null || args[i].getParam()==null ? "NULL" : args[i].getParam().toString()));
            //}
        }

        // standard errorcode, errormessage output params       
        args[args.length-2] = new SQLParam(new Integer(0), Types.INTEGER, true); // errno
        args[args.length-1] = new SQLParam(".", Types.VARCHAR, true);

        // create the statement string
        StringBuffer command = new StringBuffer("{? = call " + spName + "(") ;
        for(int i=0; i<args.length; i++) {
            command.append("?");
            if(i!=args.length-1)
                command.append(",");
        }
        command.append(")}");

        CallableStatement stmt=null; // oracle specific (in order to support VARRAYs) - previously OracleCallableStatement

        try {
            if(conn==null) {
                if(logger.isDebugEnabled()) {
                    logger.debug("[callSP()]: Connection NULL");
                }
                throw new XanbooException(20020);
            }
            
            stmt = (CallableStatement) conn.prepareCall(command.toString(), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY); //previously OracleCallableStatement
            stmt.registerOutParameter(1, OracleTypes.CURSOR);   // ORACLE specific cursor type, no other choice !!!
            
            for(int i=2; i<args.length+2; i++) {
               SQLParam currentArg = args[i-2];
               if(currentArg.getParamType() == Types.INTEGER) {
                   if(!currentArg.isOutput()) {   // IN parameter
                      if(currentArg.getParam()==null) {
                          stmt.setNull(i, Types.INTEGER);
                      }else {
                          stmt.setInt(i, ((Integer) currentArg.getParam()).intValue());
                      }
                   }else {                                // OUT parameter
                        stmt.registerOutParameter(i, Types.NUMERIC, 0);
                   }
               }else if(currentArg.getParamType() == Types.BIGINT) {
                   if(!currentArg.isOutput()) {   // IN parameter
                      if(currentArg.getParam()==null) {
                          stmt.setNull(i, Types.NUMERIC);
                      }else {
                          stmt.setLong(i, ((Long) currentArg.getParam()).longValue());
                      }
                   }else {                                // OUT parameter
                        stmt.registerOutParameter(i, Types.NUMERIC, 0);
                   }
               }else if(currentArg.getParamType() == Types.FLOAT) {
                   if(!currentArg.isOutput()) {   // IN parameter
                      if(currentArg.getParam()==null) {
                          stmt.setNull(i, Types.NUMERIC);
                      }else {
                          stmt.setFloat(i, ((Float) currentArg.getParam()).floatValue());
                      }
                   }else {                                // OUT parameter
                        stmt.registerOutParameter(i, Types.NUMERIC, 3);
                   }
               }else if(currentArg.getParamType() == Types.DOUBLE) {
                   if(!currentArg.isOutput()) {   // IN parameter
                      if(currentArg.getParam()==null) {
                          stmt.setNull(i, Types.NUMERIC);
                      }else {
                          stmt.setDouble(i, ((Double) currentArg.getParam()).doubleValue());
                      }
                   }else {                                // OUT parameter
                        stmt.registerOutParameter(i, Types.NUMERIC, 3);
                   }
               }else if(currentArg.getParamType() == Types.DATE) {
                   if(!currentArg.isOutput()) {   // IN parameter
                       if(currentArg.getParam()==null) {
                           stmt.setNull(i, Types.TIMESTAMP);
                       }else {
                           stmt.setTimestamp(i, new java.sql.Timestamp(((java.util.Date)currentArg.getParam()).getTime()));
                       }
                   }else {                                // OUT parameter
                        stmt.registerOutParameter(i, Types.DATE);
                   }
               }else if(currentArg.getParamType() == Types.ARRAY) {
                   ArrayDescriptor desc=null;
                   if(currentArg.getParamType() == Types.ARRAY) {
                       desc = ArrayDescriptor.createDescriptor("NUMARRAY", conn);
                   }
                   ARRAY thisArray = new ARRAY(desc, conn, currentArg.getParam());
                   if(!currentArg.isOutput()) {
                       stmt.setObject(i, thisArray); //previously setARRAY
                   }
               }else if(currentArg.getParamType() == Types.BLOB) {
                   throw new XanbooException(20081, "Query calls do not support LOB types as input parameters");
               }else if(currentArg.getParamType() == Types.CLOB) {
                   throw new XanbooException(20081, "Query calls do not support LOB types as input parameters");
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

            
            ResultSet rset=null;
//            ArrayList queryResult = new ArrayList();
            XanbooResultSet queryResult = new XanbooResultSet();
            
            try {
                stmt.execute();
            }catch(Exception e) { 
                if(logger.isDebugEnabled()) {
                    logger.debug("[callSP()]: Exception:", e);
                }                            
                if(stmt!=null) closeStatement(stmt);
                throw new XanbooException(20080, e.getLocalizedMessage());
            }

            // getting the output parameters
            for(int i=2; i<args.length+2; i++) {
               SQLParam currentArg = args[i-2];
               if(currentArg.isOutput()) {
                  if(currentArg.getParamType() == Types.BIGINT) {
                     args[i-2] = new SQLParam(new Long(stmt.getLong(i)), Types.BIGINT);
                  }else if(currentArg.getParamType() == Types.INTEGER) {
                     args[i-2] = new SQLParam(new Integer(stmt.getInt(i)), Types.INTEGER);
                  }else if(currentArg.getParamType() == Types.FLOAT) {
                     args[i-2] = new SQLParam(new Float(stmt.getFloat(i)), Types.FLOAT);
                  }else if(currentArg.getParamType() == Types.DOUBLE) {
                     args[i-2] = new SQLParam(new Double(stmt.getDouble(i)), Types.DOUBLE);
                  }else if(currentArg.getParamType() == Types.DATE) {
                     args[i-2] =  new SQLParam(stmt.getDate(i), Types.DATE);
                  }else if(currentArg.getParamType() == Types.BLOB) {
                      Blob blob = stmt.getBlob(i);
                      if(blob!=null && blob.length()>0)
                          args[i-2] = new SQLParam(blob.getBytes(1, (int)blob.length()), Types.BLOB);
                      else
                          args[i-2] = new SQLParam(null, Types.BLOB);
                      if(blob!=null) blob.free();
                  }else if(currentArg.getParamType() == Types.CLOB) {
                      Clob clob = stmt.getClob(i);
                      if(clob!=null && clob.length()>0)
                          args[i-2] = new SQLParam(clob.getSubString(1, (int)clob.length()), Types.CLOB);
                      else
                          args[i-2] = new SQLParam(null, Types.CLOB);
                      if(clob!=null) clob.free();
                  }else {
                     if(stmt.getString(i) != null)
                        args[i-2] =  new SQLParam(stmt.getString(i));
                  }
               }
            }
            
            // now get the cursor and query results
            boolean failed=false;
            try {
                rset = (ResultSet)stmt.getObject(1); // Oracle Specific - previously getCursor
            }catch(Exception e) { 
                failed=true;    // invalid cursor, just ignore
            }
            logger.info("Theresetresult = "+ rset);
            if(!failed && rset != null) { //null check
                ResultSetMetaData rsetMeta=rset.getMetaData();
                int colCount=rsetMeta.getColumnCount();
    /*
                for(int i=1; i<colCount+1; i++) {
                        Debug.println("COLUMN[" + i + "]," + rsetMeta.getColumnName(i));
                        Debug.println("TYPE[" + i + "]," + rsetMeta.getColumnType(i));
                }
    */

                int rowCount=0;
                while(rset.next()) {
                    if(rowCount==9) rset.setFetchSize(FETCH_SIZE_MAX);       //increase fetch size for this query, if more than 10recs returned
                        
                    HashMap hashRow=new HashMap();
                    for(int i=1; i<colCount+1; i++) {
                        String columnName = rsetMeta.getColumnName(i);
                        if(columnName != null) {
                            hashRow.put(columnName, "");
                            if(rsetMeta.getColumnType(i) == Types.NUMERIC) {
                                if(rset.getBigDecimal(i)!=null) {
                                    hashRow.put(columnName, rset.getBigDecimal(i).toString());
                                }
                            /*
                            }else if(rsetMeta.getColumnType(i) == Types.DATE) {
                                if(rset.getDate(i)!=null) {
                                    //hashRow.put(columnName, rset.getDate(i).toString());
                                    hashRow.put(columnName, XanbooUtil.getISO8601(rset.getDate(i)));
                                }
                            */
                            }else if(rsetMeta.getColumnType(i) == Types.TIMESTAMP || rsetMeta.getColumnType(i) == Types.DATE) {
                                if(rset.getTimestamp(i) != null) {
                                    //hashRow.put(columnName, DateFormat.getDateInstance().format(rset.getTimestamp(i)).toString());
                                    hashRow.put(columnName, XanbooUtil.getISO8601(rset.getTimestamp(i)));
                                }
                            }else if(rsetMeta.getColumnType(i) == Types.VARCHAR) {
                                if(rset.getString(i) != null) {
                                    hashRow.put(columnName, rset.getString(i));
                                }
                            }else if(rsetMeta.getColumnType(i) == Types.BLOB) {
                                Blob blob = rset.getBlob(i);
                                if(blob!=null && blob.length()>0) {
                                    hashRow.put(columnName, blob.getBytes(1, (int)blob.length()));
                                }
                                if(blob!=null) blob.free();
                            }else if(rsetMeta.getColumnType(i) == Types.CLOB) {
                                Clob clob = rset.getClob(i);
                                if(clob!=null && clob.length()>0) {
                                    hashRow.put(columnName, clob.getSubString(1, (int)clob.length()));
                                }
                                if(clob!=null) clob.free();
                            }
                        } 
                    } // end for loop

                    // now add the row to our queryResult array
                    queryResult.add(rowCount, hashRow);
                    rowCount++;
                    queryResult.setSize(rowCount);
                }
            }

            closeResultSet(rset);
            closeStatement(stmt);
            
            // if query failed, throw a XanbooException with code and msg
            int code=((Integer) args[args.length-2].getParam()).intValue();
            String msg=args[args.length-1].getParam().toString();
            if(code!=GlobalNames.XAIL_CODE_ACK) {
                if(logger.isDebugEnabled()) {
                    logger.debug("[callSP()]: Xanboo Exception CODE:" + code + ", MSG:" + msg);
                }            
                throw new XanbooException(code,msg);
            }

            if(logger.isDebugEnabled()) {
                logger.debug("[callSP2()]: END " + spName + " (w/ RS)");
            }

            // return query results
            return queryResult;

        }catch(SQLException ae) {
            if(logger.isDebugEnabled()) {
                logger.debug("[callSP()]: SQL Exception:", ae);
            }            
            
            throw new XanbooException(20080, ae.getLocalizedMessage());
        }finally {
            if(stmt!=null) closeStatement(stmt);
        }
        
    }    

}
