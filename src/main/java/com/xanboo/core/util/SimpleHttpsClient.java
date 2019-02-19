package com.xanboo.core.util;

import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;

import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.URL;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

/*
   The system properties that control the behavior of Keep-Alive are:
        http.keepAlive=<boolean>        default: true
        http.maxConnections=<int>       default: 5
 */
public class SimpleHttpsClient {
    private static Logger logger = LoggerFactory.getLogger(SimpleHttpsClient.class.getName());
    
    private static boolean isInitialized = false;

    public SimpleHttpsClient() {}
    
    private static synchronized void initialize() {
        if(isInitialized) return;
        isInitialized=true;

        // Install trust manager 
        try {

            // Now you are telling the JRE to ignore the hostname
            HostnameVerifier hv = new XHostnameVerifier();
            HttpsURLConnection.setDefaultHostnameVerifier(hv);

            SSLContext sc = SSLContext.getInstance("TLS", "SunJSSE");
            XTrustManager xtm = new XTrustManager();
            xtm.initialize();
            TrustManager trustManagers[] = { xtm };
            sc.init(null, trustManagers, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        }catch (Exception e) {
            logger.error("Can not initialize Trust Manager", e);
        }

    }
    
    private static HttpURLConnection getHttpUrlConnection(String pageUrl) throws Exception {
        URL url1 = new URL(pageUrl);
        if(logger.isDebugEnabled()) logger.debug("Sending request to URL:" + url1);   
        HttpURLConnection conn = (HttpURLConnection) url1.openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(5000);
        
        return conn;
    }

    private static void returnHttpUrlConnection(HttpURLConnection conn, String pageUrl) throws Exception {
    }
    

    public static SimpleHttpResponse get(String pageUrl, boolean returnResponse) {
        return sendRequest(pageUrl, "GET", null, returnResponse);
    }

    public static SimpleHttpResponse post(String pageUrl, String postData, boolean returnResponse) {
        return sendRequest(pageUrl, "POST", postData, returnResponse);
        
    }
    
    
    private static SimpleHttpResponse sendRequest(String pageUrl, String requestMethod, String postData, boolean returnResponse) {
        if(pageUrl.startsWith("https") && !isInitialized) {
            initialize();
        }
        
        HttpURLConnection conn = null;
        SimpleHttpResponse resp = null;
        
        try {
            
            conn = getHttpUrlConnection(pageUrl);
            
            conn.setRequestMethod(requestMethod);
            if(returnResponse) conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setDefaultUseCaches(false);
            conn.setFollowRedirects(true);
            
            
            //conn.setRequestProperty("User-Agent", USER_AGENT);
            //Logger.log(Logger.DEBUG, "Sending cookie:" + getSessionId());
            //conn.setRequestProperty("Cookie", getSessionId());

            if(postData!=null && postData.length()>0) {
                conn.setDoOutput(true);
                PrintWriter out = new PrintWriter(conn.getOutputStream());
                out.write(postData);
                //out.write(java.net.URLEncoder.encode(postData, "ISO-8859-1"));
                out.close();        
                if(logger.isDebugEnabled()) logger.debug("posting data: " + postData);
            }

/*
             Logger.log(Logger.DEBUG, "HEADERS\n---------------");
             for(int i=1;;++i) {
               String key;
               String value;
               if((key = conn.getHeaderFieldKey(i)) == null) break;
               if((value = conn.getHeaderField(i)) == null) break;
               Logger.log(Logger.DEBUG, key + ":" + value);
             }
*/
            int respCode = conn.getResponseCode();
            String respMsg = conn.getResponseMessage();
            
            if(logger.isDebugEnabled()) logger.debug("RESPONSE:" +  respCode + " " + respMsg + "***");

            resp = new SimpleHttpResponse(respCode, respMsg);
             
            if(respCode==HttpURLConnection.HTTP_OK) {
                 ///if(returnResponse) {
                    StringBuffer responseBuffer = new StringBuffer();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;
                    while((line = reader.readLine()) != null) {
                        responseBuffer.append(line.trim());
                    }
                    reader.close();
                    resp.setResponse(responseBuffer.toString());
                 ///}
            }
             
            returnHttpUrlConnection(conn, pageUrl);
             
            return resp;
             
        }catch(UnknownHostException uhe) { 
            logger.warn("postRequest(): Exception: ", uhe);
        }catch(NoRouteToHostException nre) {
            logger.warn("postRequest(): Exception: ", nre);
        }catch(IOException ioe) {
            if(logger.isDebugEnabled()) logger.debug("postRequest(): Exception: ", ioe);

            try {
                if(conn!=null) {
                    InputStream is = conn.getErrorStream();
                    if(is!=null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        String line;
                        while((line = reader.readLine()) != null) {
                            ; //do nothing, just exhaust the error stream
                        }
                        reader.close();
                    }
                }
            }catch(Exception e) { } //error reading error stream
        }catch(Exception e) {
            logger.warn("postRequest(): Exception: " + e.getMessage());
        }

        return resp;
    }   
    


    // inner class to override default trust manager - Trusts ALL !!!
    private static class XHostnameVerifier implements HostnameVerifier {
        public boolean verify(String urlHostName, SSLSession session) {
            if(logger.isDebugEnabled()) logger.debug("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
            return true;
        }
    }

    // inner class to override default trust manager - Trusts ALL !!!
    private static class XTrustManager implements X509TrustManager {       
        public void initialize() { }

        public void checkClientTrusted(X509Certificate[] chain, String authType)
                                    throws CertificateException {
            //logger.debug("XTrustManager:checkClientTrusted(): ");
        }

        public void checkServerTrusted(X509Certificate[] certs,
                               String authType)
                        throws CertificateException  {       
            //logger.debug("XTrustManager:checkServerTrusted(): ");
            if(!isChainTrusted(certs)) {
                throw new CertificateException("Invalid Certificate");
            }

        }

        public boolean isClientTrusted(X509Certificate[] certs) {
            //logger.debug("XTrustManager:IsClientTrusted(): ");
            return true;
        }

        public boolean isServerTrusted(X509Certificate[] certs) {
            //logger.debug("XTrustManager:IsServerTrusted(): ");
            return isChainTrusted(certs);
        }

        public X509Certificate[] getAcceptedIssuers() {
            //logger.debug("XTrustManager:getAcceptedIssuers(): ");

            X509Certificate[] X509Certs = null;
            return X509Certs;
        }


        // isChainTrusted searches the keyStore for any certificate in the certificate chain.
        private boolean isChainTrusted(X509Certificate[] chain) {
            //logger.debug("XTrustManager:IsChainTrusted(): ");
            return true;    
        }


    }    
    
}
