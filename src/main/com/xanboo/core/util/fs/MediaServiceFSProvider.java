/*
 * Copyright 2015 AT&T Digital Life
 */
package com.xanboo.core.util.fs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Properties;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

/**
 * AT&T Media Service client provider implementation.
 * 
 */
public class MediaServiceFSProvider implements XanbooFSInterface {

    private Logger logger;
    private static int connectionTimeout=6000; //default
    private static int socketTimeout=6000;     //default
    private static boolean initialized;
    private static String mediaSvcUser;
    private static String mediaSvcPasswd;
    private static String mediaSvcUrl;
    private static String mediaSvcBulkDeleteUrl;
    private static String mediaSvcProxyHost=null;
    private static int mediaSvcProxyPort=-1;
    private RequestConfig requestConfig = null;

    public void initialize(String props) {

        logger = LoggerFactory.getLogger(this.getClass().getName());

        synchronized (this) {

            if (!initialized) {

                Properties mediaSvcProps = new Properties();

                try {
                    String propertiesFileLocation = System.getProperty("app.fs.mediasvc.config");
                    FileInputStream in = new FileInputStream(propertiesFileLocation);
                    mediaSvcProps.load(in);
                    in.close();

                    if (!mediaSvcProps.containsKey("mediasvc.authUser"))
                        throw new Exception("mediasvc.authUser does not exist");
                    else
                        mediaSvcUser = mediaSvcProps.getProperty("mediasvc.authUser");

                    if (!mediaSvcProps.containsKey("mediasvc.authPasswd"))
                        throw new Exception("mediasvc.authPasswd does not exist");
                    else
                        mediaSvcPasswd = decryptPassword(mediaSvcProps.getProperty("mediasvc.authPasswd"));

                    if (!mediaSvcProps.containsKey("mediasvc.url"))
                        throw new Exception("mediasvc.url does not exist");
                    else
                        mediaSvcUrl = mediaSvcProps.getProperty("mediasvc.url");

                    if (!mediaSvcProps.containsKey("mediasvc.bulk.delete.url"))
                        throw new Exception("mediasvc.bulk.delete.url does not exist");
                    else
                        mediaSvcBulkDeleteUrl = mediaSvcProps.getProperty("mediasvc.bulk.delete.url");

                    if (mediaSvcProps.containsKey("mediasvc.connectionTimeout")) {
                        //Sets the amount of time to wait (in milliseconds) when initially establishing
                        //a connection before giving up and timing out. A value of 0 means infinity.
                        connectionTimeout = Integer.parseInt(mediaSvcProps.getProperty("mediasvc.connectionTimeout"));
                        connectionTimeout = connectionTimeout * 1000;
                    }
                    if (mediaSvcProps.containsKey("mediasvc.socketTimeout")) {
                        //Sets the amount of time to wait (in milliseconds) for data to be transferred
                        //over an established, open connection before the connection times out and is
                        //closed. A value of 0 means infinity.
                        socketTimeout = Integer.parseInt(mediaSvcProps.getProperty("mediasvc.socketTimeout"));
                        socketTimeout = socketTimeout * 1000;
                    }

                    if (mediaSvcProps.containsKey("mediasvc.proxy.host")) {
                        mediaSvcProxyHost = mediaSvcProps.getProperty("mediasvc.proxy.host");
                    }

                    if (mediaSvcProps.containsKey("mediasvc.proxy.port")) {
                        mediaSvcProxyPort = Integer.parseInt(mediaSvcProps.getProperty("mediasvc.proxy.port"));
                    }

                    if (mediaSvcProxyHost != null) {
                        HttpHost proxy = null;
                        if (mediaSvcProxyPort > -1)
                            proxy = new HttpHost(mediaSvcProxyHost, mediaSvcProxyPort);
                        else
                            proxy = new HttpHost(mediaSvcProxyHost);

                        requestConfig = RequestConfig.custom()
  					.setConnectTimeout(connectionTimeout)
  					.setConnectionRequestTimeout(connectionTimeout)
  					.setSocketTimeout(socketTimeout)
                                        .setProxy(proxy).build();

                    } else {
                       requestConfig = RequestConfig.custom()
  					.setConnectTimeout(connectionTimeout)
  					.setConnectionRequestTimeout(connectionTimeout)
  					.setSocketTimeout(socketTimeout).build();
                    }

                    initialized = true;
                } catch (Exception e) {
                    logger.error("[MediaServiceFSProvider()]: initialization error " + e.getMessage());
                }
            }
        }
    }

    public boolean checkDir(String dirPath, boolean create) {
        //Not implementing create.  You cannot create an empty directory
        //in the Media Service clould implementation
        return true;

    }

    public boolean mkDir(String dirPath) {
        return true;
    }

    public boolean rmDir(String dirPath, boolean recursive) {    
        return true;
    }

    public String[] listDir(String dirPath) {       
        return null;
    }    

    public boolean storeFile(File srcFile, String destFilePath, boolean createDirs) throws Exception {       
        return true;
    }
    
    public boolean storeFile(byte [] bytes, String destFilePath, boolean createDirs) throws Exception {
        return true;
    }

    public boolean removeFile(String filePath) {

        HttpDelete deleteRequest = null;
        HttpClient client = null;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("[MediaServiceFSProvider()]: removeFile " + filePath);
            }
          
            client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();            
            deleteRequest = new HttpDelete(getMediaResource(mediaSvcUrl, filePath));
            deleteRequest.setHeader(HttpHeaders.AUTHORIZATION, getAuthorizationString());
            deleteRequest.setHeader(HttpHeaders.CONNECTION, "close");
            HttpResponse response = client.execute(deleteRequest);

            int responseCode = response.getStatusLine().getStatusCode();
            
            if ( (responseCode >=200 && responseCode <=299)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("[MediaServiceFSProvider()]: removeFile response code " + responseCode);
                }
                return true;
            }
        } catch (Exception e) {
            logger.error("[MediaServiceFSProvider()]: removeFile error " + e.getMessage() );
        } finally {
            try {
                if (deleteRequest != null) {
                    deleteRequest.releaseConnection();
                    deleteRequest = null;
                }
                client = null;
            } catch (Exception e) {
                logger.warn("[MediaServiceFSProvider()]: removeFile release resources error " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * Executes the MAS bulk delete service
     * @param filePaths JSON array containing filePaths
     * @return
     */
    public List<String> removeFiles(List<String> filePaths) {
        HttpPost postRequest = null;
        HttpResponse response = null;
        HttpClient client = null;
        InputStream is = null;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("[MediaServiceFSProvider()]: removeFiles ");
            }

            client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
            postRequest = new HttpPost(mediaSvcBulkDeleteUrl);
            postRequest.setHeader(HttpHeaders.AUTHORIZATION, getAuthorizationString());
            postRequest.setHeader(HttpHeaders.CONNECTION, "close");
            postRequest.setHeader("Accept", "application/json");
            postRequest.setHeader("Content-Type", "application/json");

            JSONArray masInput = new JSONArray();
            for (String str : filePaths) {
                masInput.add(str);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("sending to MAS " + masInput.toString());
            }
                
            StringEntity entity = new StringEntity(masInput.toString());
            postRequest.setEntity(entity);
            response = client.execute(postRequest);

            int responseCode = response.getStatusLine().getStatusCode();

            if ( (responseCode >=200 && responseCode <=299)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("[MediaServiceFSProvider()]: removeFiles response code " + responseCode);
                }

                is = response.getEntity().getContent();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                int size = 2048, len;
                byte[] buf = new byte[size];
                while ((len = is.read(buf, 0, size)) != -1) {
                    bos.write(buf, 0, len);
                }

                ArrayList<String> returnList = new ArrayList<String>();
                if (bos.size() == 0)
                    return returnList;

                JSONParser parser = new JSONParser();
                JSONArray arr = (JSONArray) parser.parse(bos.toString());

                for (Object obj : arr) {
                    returnList.add(obj.toString());
                }
                return returnList;
            } else {
                    logger.warn("[MediaServiceFSProvider()]: removeFiles response code " + responseCode);
            }
        } catch (Exception e) {
            logger.error("[MediaServiceFSProvider()]: removeFiles error " + e.getMessage() );
        } finally {
            try {
                if (postRequest != null) {
                    postRequest.releaseConnection();
                    postRequest = null;
                }
                if (is != null)
                    is.close();
                if (response != null || response.getEntity() != null)
                    EntityUtils.consume(response.getEntity());
                client = null;
            } catch (Exception e) {
                logger.warn("[MediaServiceFSProvider()]: removeFiles release resources error " + e.getMessage());
            }
        }
        return null;
    }

    public byte[] getFileBytes(String filePath) throws Exception {

        if (logger.isDebugEnabled()) {
                logger.debug("[MediaServiceFSProvider()]: getFileBytes " + filePath);
        }

        HttpGet request = null;
        HttpResponse response = null;
        InputStream is = null;
        HttpClient client = null;

        try {           
            client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();        
	    request = new HttpGet(getMediaResource(mediaSvcUrl, filePath));
            request.setHeader(HttpHeaders.AUTHORIZATION, getAuthorizationString());
            request.setHeader(HttpHeaders.CONNECTION, "close");
            response = client.execute(request);

            int responseCode = response.getStatusLine().getStatusCode();
            if ( responseCode >=200 && responseCode <=299) {

                if (logger.isDebugEnabled()) {
                    logger.debug("[MediaServiceFSProvider()]: Media Service: " + mediaSvcUrl + " responseCode " + responseCode +
                            " responseStatusLine " + response.getStatusLine() );
                }

                is = response.getEntity().getContent();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                int size = 2048, len;
                byte[] buf = new byte[size];
                while ((len = is.read(buf, 0, size)) != -1) {
                    bos.write(buf, 0, len);
                }
                return bos.toByteArray();
            } else {
                throw new Exception("GET " + filePath + " unsuccessful.  Response code: " + responseCode);
            }
            
        } catch (Exception e) {
            logger.error("[MediaServiceFSProvider()]: getFileBytes error " + e.getMessage() );
            throw e;
        } finally {
            try {        
                if (request != null) {
                    request.releaseConnection();
                    request = null;
                }
                if (is != null)
                    is.close();
                if (response != null || response.getEntity() != null)
                    EntityUtils.consume(response.getEntity());
                client = null;
            } catch (Exception e) {
                logger.warn("[MediaServiceFSProvider()]: getFileBytes release resources error " + e.getMessage());
            }
        }
    }

    public InputStream getFileAsStream(String filePath) throws Exception {

        try {
            if (logger.isDebugEnabled()) {
                logger.debug("[MediaServiceFSProvider()]: getFileAsStream " + filePath);
            }

            ByteArrayInputStream in = new ByteArrayInputStream(getFileBytes(filePath));
            return in;

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[MediaServiceFSProvider()]: getFileAsStream error " + e.getMessage() );
            throw e;
        }
    }

    private String getAuthorizationString() throws Exception {
        String auth = mediaSvcUser + ":" + mediaSvcPasswd;
	byte[] encodedAuth = Base64.encodeBase64(auth.getBytes());
	String authString = "Basic " + new String(encodedAuth);
        return authString;
    }

    private String getMediaResource(String url, String filePath) throws Exception {
        if (!url.endsWith("/") && !filePath.startsWith("/"))
            return url + "/" + filePath;
        else
            return url + filePath;
    }
    
    /**
     * The password is encrypted using:
     * https://access.redhat.com/knowledge/docs/en-US/JBoss_Enterprise_Application_Platform/5/html/Security_Guide/Encrypting_Data_Source_Passwords.html
     * @return decrypted password
     * @throws Exception
     */
    private static String decryptPassword(String encryptedText) throws Exception {

        byte[] kbytes = "jaas is the way".getBytes();
        SecretKeySpec key = new SecretKeySpec(kbytes, "Blowfish");

        BigInteger n = new BigInteger(encryptedText, 16);
        byte[] encoding = n.toByteArray();

        Cipher cipher = Cipher.getInstance("Blowfish");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decode = cipher.doFinal(encoding);
        return new String(decode);
    }

}
