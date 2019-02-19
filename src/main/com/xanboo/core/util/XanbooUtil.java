/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/util/XanbooUtil.java,v $
 * $Id: XanbooUtil.java,v 1.38 2011/05/02 17:55:43 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.UUID;
import com.xanboo.core.security.XanbooEncryptionProvider;
import com.xanboo.core.security.XanbooEncryptionProviderFactory;
import com.xanboo.core.security.XanbooPrincipal;

/* 
 * Utility class that provides static utility methods
 *
 */
public class XanbooUtil {

   // private static SimpleDateFormat sdf=null;
    
    public static final int SUPPORTED_ALGORITHM_MD5         = 0;
    public static final int SUPPORTED_ALGORITHM_SHA1        = 1;
    public static final int SUPPORTED_ALGORITHM_SHA256      = 2;
    public static final String[] SUPPORTED_ALGORITHMS = { "MD5", "SHA-1", "SHA-256" };	//do not change the values or the order of values !!!
    
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private static final String rndStr = new String("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
    
	private static Logger logger = LoggerFactory.getLogger("com.xanboo.core.util.XanbooUtil");
    
    /** Creates new XanbooUtil */
    public XanbooUtil() {
    }

    /**
     * Gets a comma separates list of long numbers
     * @param ids An array of long numbers
     *
     * @return A string that contains the list of long numbers separated by commas
     */
    public static String getCSV( long[] ids ) {
        StringBuffer csv = new StringBuffer();
        csv.append( ids[0] );
        for (int i = 1 ; i < ids.length ; i++) {
            csv.append( "," + Long.toString( ids[i] ) );
        }
        return csv.toString();
    }

    
    /**
     * Gets an array of long ids from a string 
     * @param s A string that contains a list of long numbers separated by ',', '+', ';', or spaces
     *
     * @return An array of long numbers
     * @throws XanbooException if the string was empty or one of its separated contents is not a number
     */
    public static long[] getIds(String s) throws XanbooException {
         /* make sure the string is not empty or null 
            note: the following if depends on short circuit check */ 
         if ((s == null) || (s.trim().equals(""))) {
              System.err.println("error: empty list.");
              throw new XanbooException(10050);
          }
         
          /* parse a list of strings separated by , + space or ; */ 
          StringTokenizer st = new StringTokenizer(s,",+ ;");
          long[] ids = new long[st.countTokens()];
          int i=0;
          while (st.hasMoreTokens()) {
              try {
              
                  ids[i] = Long.parseLong(st.nextToken());
                  System.err.println(i+"="+ids[i]);  //just for testing
                  i++;
              
              } catch (NumberFormatException e1) {
                  System.err.println("bad number in the list.");
                  throw new XanbooException(10050);
              } catch (ArrayIndexOutOfBoundsException e2) {
                  System.err.println("bad array index.");
                  throw new XanbooException(10050);
              }
          }          
          return ids;     
    }
    
    /**
     * Checks if a given caller principal has access to an account. Compares caller principal's account id
     * with the account id passed to the EJB.
     * @param xCaller XanbooPrincipal object that contains information about an authenticated user
     * @param accountId A long number that contains an account id
     *
     * @throws XanbooException if xCaller contained invalid information, or if the accountId in xCaller doesn't match the accountId parameter
     */
    public static void checkCallerPrivilege(XanbooPrincipal xCaller, long accountId) throws XanbooException {

        if(xCaller==null || xCaller.getAccountId()<1 || xCaller.getUserId()<1) {
            throw new XanbooException(10005);   // invalid caller
        }

        // Don't allow callers to operates on another account
        // TO DO: must allow, if caller is an ADMIN user. How to detect ??? Roles ???
        if(xCaller.getAccountId()!=accountId) {
            throw new XanbooException(10010);
        }

    }

    /**
     * Validates iso8601 time format
     * @param time A text string to validate
     *
     * @return True if string contains a correctly formatted iso8601 time format
     */
    public static boolean validateTimeFormat( String time ) {
        try {
            String timeFormat = "yyyy-MM-dd hh:mm:ss";        
            if (time.length() != timeFormat.length()) 
                return false;
            SimpleDateFormat formatter = new SimpleDateFormat (timeFormat);
            formatter.parse( time );
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Checks if a given caller principal is valid or not.
     * @param xCaller A XanbooPrincipal object that should contain valid information about an authenticated user
     *
     * @throws XanbooException if xCaller doesn't contain valid information
     */
    public static void checkCallerPrivilege(XanbooPrincipal xCaller) throws XanbooException {
        if(xCaller==null || xCaller.getAccountId()<0 || xCaller.getUserId()<0) {
            throw new XanbooException(10005);   // invalid caller
        }
    }

    /**
     * Generates a unique key
     *
     * @param maxLength The maximum length of the returned key
     *
     * @return A string that contains a unique key of maximum length 64 bytes
     */
    public static String generateKey( int maxLength) {
        Date today = new Date();
        Random randNum=new Random();
        String now = Long.toHexString(randNum.nextLong()) + Long.toHexString(today.getTime()) + Long.toHexString(randNum.nextLong());
        return (now.length()>maxLength ? now.substring(0,maxLength) : now);
    }    

    /**
     * Generates a random password of given length
     *
     * @param len The length of the returned password
     *
     * @return A string that contains a unique key of maximum length 64 bytes
     */
    public static String generatePassword( int len) {
        Random randNum=new Random(System.currentTimeMillis());
        
        String allowed = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVXYZ";
        
        StringBuffer pass = new StringBuffer(len);
        for(int i=0; i<len; i++) {
            int ix = randNum.nextInt(allowed.length());
            pass.append(allowed.charAt(ix));
        }
        return pass.toString();
    }    
    
    
    /**
     * Does simple validation to an email assuming that it doesn't contain not allowed characters
     * @param the_email A string that contains an email address
     *
     * @return true if email is a valid format
     */
     public static boolean isValidEmail(String the_email) {
        the_email = the_email.trim();        //trim, note: inside spaces are not removed
        int atPos = the_email.indexOf('@');
        int dotPos = the_email.indexOf('.');
        int emailLength = the_email.length();

        if ((emailLength < 3) ||     //at least 3 characters
           (atPos <= 0) ||          //must have @ and not as fisrt char
           (dotPos == 0) ||         //must not have . as first char
           (the_email.charAt(emailLength-1)=='@') ||  //must not have @ as last char
           (the_email.charAt(emailLength-1)=='.') ||  //must not have . as last char
           (the_email.indexOf(".@") >= 0) ||  //must not have . right before @
           (the_email.indexOf("@.") >= 0) ||  //must not have . right after @
           (the_email.indexOf("..") >= 0) ||   //must not have . followed by another one
           (the_email.indexOf(" ") >= 0))   //must not have spaces inside
           return false;
        else
           return true;
     }


    /**
     * Simple validation check for phone numbers. Phone numbers must be min 7 digit numeric values, cannot start with 0, and of same digits
     * @param the_phone A string that contains a phone number
     *
     * @return true if email is a valid format
     */
     public static boolean isValidPhone(String the_phone) {
        the_phone = the_phone.trim();        //trim, note: inside spaces are not removed

        if(!isValidString(the_phone, "0123456789")) return false;   //only digits
        if(the_phone.length()<7) return false;                      //min 7 digits
        if(the_phone.startsWith("0")) return false;                 //cannot start with 0
        
        boolean digitsAllSame=true;
        char ch = the_phone.charAt(0);
        for(int i=1; i<the_phone.length(); i++) {
            if(ch!=the_phone.charAt(i)) {   //found non-repeat char
                digitsAllSame=false;
                break;
            }
        }
        if(digitsAllSame) return false;                             //all same digit numbers not allowed
        
        return true;
     }
     
     
    /**
     * Does simple validation of a string by checking that all of it's characters are allowed.
     *
     * @param str The string to validate
     * @param allowedChars A string containing all characters that are allowed 
     *
     * @return true if all characters in str are listed as allowed in allowedChars
     */
     public static boolean isValidString(String str, String allowedChars) {
       str = str.trim();
       for (int i=0,n=str.length(); i<n; i++) {
           if (allowedChars.indexOf(str.charAt(i)) == -1) {
               return false;
           }
       }
       return true;
       
     }     
     
    /**
     * Does simple validation to a pager number to ensure it doesn't contain invalid characters
     * Will allow the use of ()-+ and numeric characters
     * @param target A string that contains a pager number
     *
     * @return true if pager number is valid
     */
     public static boolean isValidPager(String target) {
       target = target.trim();
       String goodChars = "0123456789()-+ ";
       String numericals = "0123456789";
       int numbers = 0;
       int length = target.length();
       for (int i=0; i<target.length(); i++) {
           char thisChar = target.charAt(i);
           if (goodChars.indexOf(thisChar) == -1) {
               return false;
           } else if (numericals.indexOf(thisChar) !=-1 ){
               numbers++;
           }
       }    
       
       //If there are less than 6 numbers or more than 15, reject it (15 for international pagers ?).
       if (numbers < 6 || numbers > 15) {
           return false;
       } else {
           return true;
       }
       
     }
       
     /**
      * Checks to see if a parameter is valid by checking for disallowed substrings
      * @param param The string that we want to check
      * @param forbiddenStrings An array of strings that the existence of will invalidate the parameter
      *
      * @return true if the parameter is valid (not null & contains no forbidden strings)
      */
     public static boolean isValidParameter( String param, String[] forbiddenStrings) {
         if (param == null || param.trim() == "") {
             return false;
         }
         for (int i=0; i<forbiddenStrings.length; i++) {
             if (forbiddenStrings[i] == null || forbiddenStrings[i].trim() == "" || (param.indexOf(forbiddenStrings[i]) != -1)) {
                 return false;
             }
         }
         return true;
     }
            
    /**
     * Replaces an occurrence of a string with another string in a string
     * @param inString The original string that we would like to change
     * @param replaceFrom The part of the original string that we would like to replace
     * @param replaceTo The new string that will be used to modify the original string
     *
     * @return A new version of inString after a global replacement
     */
    public static String replace(String inString, String replaceFrom, String replaceTo) {
        int ix=inString.indexOf(replaceFrom);
        if(ix==-1) return inString;

        StringBuffer buffer=new StringBuffer(inString.length()*2).append(inString);
        while(ix > -1) {
            if(replaceTo==null || replaceTo.length()==0) {
                buffer.delete(ix, ix+replaceFrom.length());
            }else {
                buffer.replace(ix, ix+replaceFrom.length(), replaceTo);
            }
            ix=buffer.toString().indexOf(replaceFrom);
        }
        
        return buffer.toString();
    }
     
    
    /**
     * Replaces a new line string ("\n") with a new line char 
     * @param inString A string that contains the new line represented as a string instead of a char
     *
     * @return A new version of inString after a global replacement of the new line string with a new line char
     */
    public static String replaceNL(String inString) {
        StringBuffer buffer=new StringBuffer(inString);
        
        int ix=0;
        while((ix=buffer.toString().indexOf("\\n")) > -1) {
            buffer.replace(ix, ix+2, "\n");
        }
        
        return buffer.toString();
    }
    
    /**
     * left pads a string with space character
     * @param s string to be padded
     * @param n total length of the string to be padded
     * @return padded string
     */
    public static String padLeft(String s, int n) {
        return String.format("%1$" + n + "s", s);
    }

    
    /**
     * Returns hashed value associated w/ a string using default hashing algorithm (MD5)
     * @param inString string to be hashed
     *
     * @return a new string that contains the hash value
     */
    public static String hashString(String inString) {
	return hashString(inString, SUPPORTED_ALGORITHM_MD5, null);
    }


    /**
     * Returns hashed value associated w/ a string
     * @param inString string to be hashed
     * @param alg hashing algorithm to be used (MD5, SHA-1, SHA-256)
     * @param salt 16char salt value to be used for hashing. 
     *        If null, no salting will be used
     *        if salt is non-16char value, a random salt value will be generated.
     *
     * @return a new string that contains the hash value in the format:
     *        if salting used:  <ID-1char><SALT-16char><salted-hash-value>
     *        if no salting  :  <hash-value>
     */
    public static String hashString(String inString, int algID, String salt) {
        if(inString == null) return null;

        if(algID<0 || algID>SUPPORTED_ALGORITHMS.length-1) algID=0;  //default MD5, if not a valid value

        MessageDigest md = null;
  	try { md = MessageDigest.getInstance(SUPPORTED_ALGORITHMS[algID]); }catch(Exception e) {  return inString; }


	if(salt==null) {                // no salting for backwards compatibility
            ;
	}else if(salt.length()==16) {   // use salt parameter passed
            md.update(salt.getBytes());

        }else {                         // generate salt and use it
            Random generator = new Random();
            generator.setSeed(System.currentTimeMillis());
	    String rnd = "" + generator.nextInt(100000000) + generator.nextInt(100000000);
	    rnd = padLeft(rnd, 16);
	    salt = rnd;
            //System.out.println("\nSALT   : " + salt + ", len=" + salt.length());  //16 digits
            md.update(salt.getBytes());
        }

        md.update(inString.getBytes());
        byte[] hashValueBytes = md.digest();

        char[] hex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        StringBuffer hashValue = new StringBuffer();
        for(int i=hashValueBytes.length-1; i>=0; i--) {
            int c = ((char) hashValueBytes[i] & 0xFF);
            hashValue.append(hex[c>>4]);
            hashValue.append(hex[c & 0xF]);
            
            // just simple added security
            if(i==6) {
                hashValue.append(hashValue.charAt(2));
            }else if(i==2) {
                hashValue.append(hashValue.charAt(9));
            }
        }
        
        if(salt==null)
            return hashValue.toString();
        else
            return algID + salt + hashValue.toString();
    }
    
    
    /**
     * Returns the directory segment for an accountid 
     * @param accountId
     *
     * @return relative account directory path
     */
    public static String getAccountDir(long accountId) {
        
        int seg = (int) accountId/10000;  // account directories are segmented into 10K blocks
        
        if(seg > 9) {
            return seg + "/" + accountId;
        }else {
            return "0" + seg + "/" + accountId;
        }
    }
    
    public static String createUniqueFileName(Calendar cal) {

        // to make sure we have a unique filename
       Random randNum=new Random(Calendar.MILLISECOND);
       
       return(Integer.toString(cal.get(Calendar.YEAR)) +
               (cal.get(Calendar.MONTH)<10 ? "0" : "") + (cal.get(Calendar.MONTH)+1) + 
               (cal.get(Calendar.DATE)<10 ? "0" : "") + cal.get(Calendar.DATE) +
               (cal.get(Calendar.HOUR)<10 ? "0" : "") + cal.get(Calendar.HOUR) +
               (cal.get(Calendar.MINUTE)<10 ? "0" : "") + cal.get(Calendar.MINUTE) +
               (cal.get(Calendar.SECOND)<10 ? "0" : "") + cal.get(Calendar.SECOND) +
               cal.get(Calendar.MILLISECOND) + randNum.nextInt(999));
    }

    
    public static Date convertToISO8601Date( String date ) {
    	Date dateObj = null;
    	SimpleDateFormat formatter= new SimpleDateFormat (ISO_DATE_FORMAT);
         try {
        	 dateObj = formatter.parse(date);
		  } catch (ParseException e) {
			 e.printStackTrace();
		  }
        return dateObj;
    }
    
    public static String getISO8601( Date date ) {
        SimpleDateFormat formatter= new SimpleDateFormat (ISO_DATE_FORMAT);
        return (formatter.format(date));
    }
    
    public static String convertGMTDateToISO8601( String date ) {
    	SimpleDateFormat sdf = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss.SSS");
        try {
			 Date parseDateObj = sdf.parse(date);
		 	 return getISO8601(parseDateObj);
		  } catch (ParseException e) {
			 e.printStackTrace();
		  }
        return null;
    }
    
    public static String getCurrentGMTDateTime() {
    	return getDateTimeInGMT(null);
    }
    
    public static String getDateTimeInGMT(Date date) {
        SimpleDateFormat sdf= new SimpleDateFormat(ISO_DATE_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    	if(date!=null) {
        	 return sdf.format(date);
        }
        return sdf.format(new Date());
    }
    
    /** 
     * Generates a gateway installation/registration token. 
     * @param prefix an optional prefix string for the resulting token string. Typically,
     *        domain id of the owner account is used for this parameter. Should not contain
     *        any '@' character !
     * @param tokenKey a unique key to be hashed to generate a unique token string.
     *        Typically owner account id is used as the tokenKey.
     *
     * @return a new, unique token string
     */
    public static String generateRegistrationToken(String prefix, String tokenKey) {
        
        if(tokenKey==null) return null;
        
        // use un-salted MD5 hash for tokens: will return 34-char hex string
        String tokenRaw = XanbooUtil.hashString(tokenKey + Long.toHexString(new Date().getTime()), SUPPORTED_ALGORITHM_MD5, null);
        
	StringBuffer token;

        if(prefix!=null) {
            if(prefix.indexOf('@') != -1) return null;
            token = new StringBuffer(prefix);
            token.append("@");
        }else {
            token = new StringBuffer("XC-");
        }

        token.append(tokenRaw.substring(0, 4));
        token.append("-");
        token.append(tokenRaw.substring(4, 10));
        token.append("-");
        token.append(tokenRaw.substring(10, 16));
        token.append("-");
        token.append(tokenRaw.substring(16, 20));
        token.append("-");
        token.append(tokenRaw.substring(20, 24));

        //e.g. 4D52@1850-DEAF57-18CAFE-B43B-539F
        return token.toString();
    }    

   
    public static String generateTransactonId(Object seedHash,int length)
    {
        int seedHashCode = seedHash.hashCode();
        //ensure this is a unique seed ... get the current timestamp and add the gguid hash code
        //this is to avoid a case where two gateways connect act exactly the same millisecond and 
        //as a result, the two gateways use the same token
        long seed = System.currentTimeMillis() + (long)seedHashCode;
        Random rnd = new Random(seed); 
        StringBuilder bldr = new StringBuilder();
        //32 character token string, randomly generated
        for ( int i = 0; i < length; i++ )
        {
            double d = rnd.nextDouble();
            int index = (int)(rndStr.length() * d);
            if ( index == rndStr.length() )
                index = index - 1;
            bldr.append(rndStr.charAt(index));
        }
        return bldr.toString();
    }
    
    /* encodes special html characters */
    public static final String htmlEncode(String s){
       if(s==null) return null;

       StringBuffer sb = new StringBuffer(s.length()+20);
       int n = s.length();
       for (int i = 0; i < n; i++) {
          char c = s.charAt(i);
          switch (c) {
             case '<' : sb.append("&lt;"); break;
             case '>' : sb.append("&gt;"); break;
             case '&' : sb.append("&amp;"); break;
             case '"' : sb.append("&quot;"); break;
             case '\'': sb.append("&#39;"); break;
             case '\n': sb.append("&#10;"); break;
             case '\r': sb.append("&#13;"); break;
             default  : sb.append(c); break;
          }
       }
       return sb.toString();
    }

    
    public static byte[] getFileBytes(String filePath) throws Exception {

        InputStream in = null;
        try {
            File f = new File(filePath);
            int fileSize = (int) f.length();

            in =  new BufferedInputStream(new FileInputStream(f));
            byte[] fileBytes = new byte[fileSize];
            int readFileSize = in.read(fileBytes, 0, fileSize);

            if(readFileSize!=fileSize) {
                throw new Exception("Item size read doesnt match: " + readFileSize + " != " + fileSize);
            }

            return fileBytes;

        }catch(Exception ioe) {
            throw ioe;
        }finally {
            if(in != null) try{ in.close();}catch (Exception e){}
        }

    }
    
    
    public static String localizeDate(String inDate, String tzName, String localeId ) {
 
        try {
            
         //   if ( sdf == null ) {
        	SimpleDateFormat   sdf = new SimpleDateFormat("yyyy-MM-dd H:mm:ss");
                sdf.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("GMT")));
        //    }
            java.util.Date d = sdf.parse( inDate );

            /*TimeZone tz = TimeZone.getTimeZone( tzName );

            Locale loc;
            if(localeId!=null && localeId.length()==5) {
                loc = new Locale(localeId.substring(0, 2), localeId.substring(3));
            }else if(localeId!=null && localeId.length()==2) {
                loc = new Locale(localeId, "");
            }else {
                loc = Locale.getDefault();
            }
            
            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, loc);
            df.setCalendar(Calendar.getInstance( tz ));
            
            return ( df.format( d ) );*/
            return localizeDate(d, tzName,localeId);
            
        } catch ( Exception e ) {
            return inDate;
        }
        
    }
    
    public static String localizeDate(java.util.Date inDate, String tzName, String localeId ) 
    {
         try {
            
           TimeZone tz = TimeZone.getTimeZone( tzName );

            Locale loc;
            if(localeId!=null && localeId.length()==5) {
                loc = new Locale(localeId.substring(0, 2), localeId.substring(3));
            }else if(localeId!=null && localeId.length()==2) {
                loc = new Locale(localeId, "");
            }else {
                loc = Locale.getDefault();
            }
            
            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, loc);
            df.setCalendar(Calendar.getInstance( tz ));
            
            return ( df.format( inDate ) );
            
        } catch ( Exception e ) {
            return inDate.toString();
        }
    }

    public static boolean hasSqlInjection(String in) {
        if(in==null) return false;
        
        if(in.indexOf("'") != -1) return true;
        return false;
    }
    
    
    public static String generateGUID(String prefix, int len) throws IllegalArgumentException {
        if((prefix.length()+7)>=len) throw new IllegalArgumentException("Length argument too low.");
            
        //format "<prefix><Y><DDD><MMM><random>"
        String allowed = "0123456789abcdefghijklmnopqrstuvwxyz";
        
        StringBuffer sb = new StringBuffer(len);
        sb.append(prefix);
        
        Calendar c = Calendar.getInstance();
        sb.append( Integer.toHexString( c.get( Calendar.YEAR ) % 1000 ) );  //Y
        sb.append( Integer.toHexString( c.get( Calendar.DAY_OF_YEAR ) ) );  //DDD (max)
        sb.append( Long.toHexString( c.getTime().getTime() % 65000 ) );     //MMMM (max)
	
	    int currLen = sb.length();        

        Random randNum=new Random(System.currentTimeMillis());
        for(int i=0; i<len-currLen; i++) {
            int ix = randNum.nextInt(allowed.length());
            sb.append(allowed.charAt(ix));
        }
        
        return sb.toString();
    }    
    
    
    public static String generateDGUID(String prefix, int len) throws IllegalArgumentException {
        if((prefix.length() + 4) >= len) throw new IllegalArgumentException("Length argument too low.");
            
        String allowed = "0123456789abcdefghijklmnopqrstuvwxyz";
        
        StringBuffer sb = new StringBuffer(len);
        sb.append(prefix);
        
        // 1 random character/digit.
        Random randNum = new Random();
        int ix = randNum.nextInt(allowed.length());
        sb.append(allowed.charAt(ix));
        
        // 1 digit from current second last digit.
        Calendar calendar = Calendar.getInstance();
        sb.append( calendar.get(Calendar.SECOND)%10);
        
        int currLen = sb.length(); 
        
        // 3 digit from milliseconds.
        UUID temp =  UUID.randomUUID();
        sb.append(temp.toString().substring(0, len-currLen));
        return sb.toString();
    }
    
    public static boolean isExternalServiceGUID(String gguid) {
        if(gguid!=null && gguid.startsWith("X")) 
            return true;
        else
            return false;
    }
    
    public static boolean isExternalServiceDevice(String devideGUID) {
        if(devideGUID!=null && devideGUID.startsWith("X")) 
            return true;
        else
            return false;
    }

    public static boolean isValidServiceId(String serviceId) {
        if(serviceId!=null && serviceId.length()==4 && serviceId.startsWith("A")) 
            return true;
        else
            return false;
    }
    
    public static String getExternalServiceId(String catalogId) {
    	if(catalogId!=null) {  
    		if(catalogId.length()==14) {
    			return catalogId.substring(0, 4);
    		}else if(catalogId.length()==15) {
    			return catalogId.substring(1, 5);
    		}
    	}
    	return null;
    }
    
    /*
     * Return the specified bit position is ON/OFF for a given number.
     */      
    public static boolean isBitOn(final int n, final int position) {
	    int mask = 1 << position;
	    return (n & mask) == mask;
	}
    
    public static double convertCelsiusToXanbooDegreeInDouble(String celcius) {
    	if(celcius!=null && celcius.trim().length() > 0) {
    		//return 2 * (Float.parseFloat(celcius) + 40 );
    		DecimalFormat newFormat = new DecimalFormat("#.##");
    		return Double.valueOf(newFormat.format(2 * (Float.parseFloat(celcius) + 40 )));
    	}
    	return 0.0;
    }
    
    public static int convertCelsiusToXanbooDegree(String celcius) {
    	if(celcius!=null && celcius.trim().length() > 0) {
    		return Math.round(2 * (Float.parseFloat(celcius) + 40 ));
    	}
    	return 0;
    } 
    
    public static int convertFahrenheitToXanbooDegree(String fahrenheit) {
    	if(fahrenheit!=null && fahrenheit.trim().length() > 0) {
    		float celcius = (float)(5.0/9.0)*(Float.parseFloat(fahrenheit) - 32);
    		return Math.round(2 * (celcius + 40 ));
    	}
    	return 0;
    }
    
    public static long convertXanbooDegreeToFahrenheit(String xanbooDegree) {
    	if(xanbooDegree!=null && xanbooDegree.trim().length() > 0) {
    		return Math.round(convertXanbooDegreeToCelsius(xanbooDegree) * 9/5 + 32);
    	}
    	return 0;
    }
    
    public static double convertXanbooDegreeToCelsius(String xanbooDegree) {
    	if(xanbooDegree!=null && xanbooDegree.trim().length() > 0) {
    		DecimalFormat newFormat = new DecimalFormat("#.##");
    		return Double.valueOf(newFormat.format(Double.parseDouble(xanbooDegree)/2 - 40));
    		
    	}
    	return 0.0;
    }
    
        public static Integer getSubsFlag(int iSubsFlag, String gguid,
			int iSubsFlagMask) {

		
		Integer uFlag = 0;
		Integer uSubFlag = 0;

		/* update subs flags, if not null */

		if (iSubsFlag < 0) { /*
							 * if negative value, it will be OR'ed with the
							 * current flags value
							 */
			if (iSubsFlag == -9999) { /*
									 * restore service request - set bit0-1 to
									 * 11, clear bit 7 (brick DLC)
									 */

				uFlag = iSubsFlag & 892; /* save higher bits */
				uSubFlag = uFlag + 3;

			} else if (iSubsFlag == -9998) { /*
											 * suspend service request - set
											 * bit0-1 to 01
											 */

				if ((uSubFlag & 3) == 0) { /*
											 * check if current status is
											 * cancelled
											 */

					/*
					 * if cancelled, set new status flags to negative value to
					 * prevent record update!!!
					 */
					uSubFlag = -1;
				} else {
					/*
					 * !!!!! allow suspension, only if there is a real gguid
					 * (after installation takes place) !!!
					 */
					if (gguid != null && gguid.length() > 0
							&& gguid.startsWith("DL-")) {

						uFlag = uSubFlag & 1020; // save higher bits
						uSubFlag = uFlag + 1;
					} else {
						// if no real dguid, set new status flags to negative
						// value to prevent record update!!!
						uSubFlag = -1;
					}
				}

			} else if (iSubsFlag == -9997) { /*
											 * cancel service request - set
											 * bit0-1 to 00 - DISABLED from EJB
											 * side anyways
											 */

				uFlag = uSubFlag & 1020; /* save higher bits */
				uSubFlag = uFlag + 0;

			} else if (iSubsFlag == -9995) { /*
											 * set 3g-only profile request - set
											 * bit2-3 to 00
											 */

				uFlag = uSubFlag & 1011;
				/* save higher and lower bits */
				uSubFlag = uFlag + 0;

			} else if (iSubsFlag == -9994) { /*
											 * set 3g+bb profile request - set
											 * bit2-3 to 01
											 */

				uFlag = uSubFlag & 1011; /* save higher and lower bits */
				uSubFlag = uFlag + 4;

			}

			/* positive value, update with the incoming value and mask */
		} else if (iSubsFlagMask <= 0) {

			uSubFlag = iSubsFlag;

			/* no mask, set flags as is */
		} else {
			/* calculate new flags from current, input and mask values */
			// u_subs_flags :=
			// XC_UTIL_PKG.calcSubscriptionFlags(u_subs_flags, i_subs_flags,
			// i_subs_flags_mask);
			Integer P1 = iSubsFlag & iSubsFlagMask;

			Integer P2 = ((0 - iSubsFlagMask) - 1);
			P2 = uSubFlag & P2;

			/* now bitor P1 and P2 ---> (P1+P2) - bitand(P1, P2) */
			uSubFlag = (P1 + P2) - (P1 & P2);

		}

		return uSubFlag;

	}
        
        public static String getGMTDateTime(String date) {

    		String returnVal = null;
    		try {
    			
    		
    				
    			if (date != null) {
    				/*SimpleDateFormat df = new SimpleDateFormat(
    						"yyyy-MM-dd HH:mm:ss");
    				df.setTimeZone(TimeZone.getTimeZone("GMT"));
    				returnVal = df.format(date);*/
    				return date;
    			} else {
    				returnVal = getCurrentGMTDateTime();
    			}
    		} catch (Exception e) {
    			// ignore
    			returnVal = getCurrentGMTDateTime();
    		}

    		return returnVal;
    	}
    
        public static boolean isCurrentGMTDateAfterTargetGMTDate(String date, int days) {
    	    boolean isDateAfter = false;
        	try {
        		SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        			formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        			Date parsedDate =	formatter.parse(date);
    	      	Calendar targetDateCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    	      		targetDateCal.setTime(parsedDate);
    	      		targetDateCal.add(Calendar.DATE, days);
    		    Calendar currDateCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    		    	isDateAfter = currDateCal.after(targetDateCal);
    		    
        	} catch (ParseException e) {
    			e.printStackTrace();
    		}
    		return isDateAfter;
    	}     
        
        public static boolean isCurrentGMTDateAfterTargetGMTDateInMinutes(String date, int minutes) {
    	    boolean isDateAfter = false;
        	try {
        		SimpleDateFormat formatter = new SimpleDateFormat(ISO_DATE_FORMAT);
        			formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        			Date parsedDate =	formatter.parse(date);
    	      	Calendar targetDateCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    	      		targetDateCal.setTime(parsedDate);
    	      		targetDateCal.add(Calendar.MINUTE, minutes);
    		    Calendar currDateCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    		    	isDateAfter = currDateCal.after(targetDateCal);
    		    
        	} catch (ParseException e) {
    			e.printStackTrace();
    		}
    		return isDateAfter;
    	}  
        
        public static boolean isNotEmpty(Object obj) {
    		if(obj == null || "".equals(obj)) {
    			return false;
    		}else { 
    			if (obj instanceof String && ((String)obj).length() <= 0) {
    				return false;
    			}
    		}
    		return true;
    	}
        
        public static <T> Iterable<T> emptyIfNull(Iterable<T> iterable) {
            return iterable == null ? Collections.<T>emptyList() : iterable;
        }
        
        
    /**
     * @param listOfStrings
     * @param separator
     * @return
     */
    public static String listToCsv(List<String> listOfStrings, char separator) {
            StringBuilder sb = new StringBuilder();

            // all but last
            for(int i = 0; i < listOfStrings.size() - 1 ; i++) {
                sb.append(listOfStrings.get(i));
                sb.append(separator);
            }

            // last string, no separator
            if(listOfStrings.size() > 0){
                sb.append(listOfStrings.get(listOfStrings.size()-1));
            }

            return sb.toString();
        }

    /**
     *
     * @param input
     * @return the date and time from the DB DATE column
     * @throws XanbooException
     */
    public static java.util.Date getDate(SQLParam input) throws XanbooException {
        if (input == null || input.getParam() == null) {
            return null;
        }
        return getDate(input.getParam().toString());
    }

    public static java.util.Date getDate(String input) throws XanbooException {
        if (input == null || input.trim().equals("")) {
            return null;
        }
        try {
            SimpleDateFormat sdf= new SimpleDateFormat(ISO_DATE_FORMAT);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            return sdf.parse(input);
        } catch (Exception e) {
            return null;
        }
    }
    
    public static String filterCatalogId(String catalogId) {
		if (catalogId == null || catalogId.length() < 15)
			return catalogId;
		if (catalogId.length() >= 15) {
			catalogId = catalogId.substring(1, 15);
		}
		return catalogId;
	}
    
    public static XanbooEncryptionProvider getEncryptionProvider(XanbooEncryptionProvider encProvider) {
    	try {
			  if (encProvider == null) {
			    	encProvider = XanbooEncryptionProviderFactory.getProvider();
			  }
		} catch (Exception e) {
			logger.error("[getEncryptionProvider()]: Unable to obtain an encryption provider instance: "+ e.getMessage());
		}
	   return encProvider;
    }
    
    public static String encrypt(XanbooEncryptionProvider encProvider,  String value) throws  XanbooException {
    	String encryptedVal = null;
    	if(encProvider == null) encProvider = getEncryptionProvider(encProvider);
		
    	if(encProvider!=null) {
			try {
				encryptedVal = encProvider.encrypt(value);
			}catch (XanbooException xe) {
				logger.error("[encrypt()]: Unable to encrypt  value [" + value +"]" + xe.getMessage());
				throw xe;
			}
		}
	   return encryptedVal;
    }
}