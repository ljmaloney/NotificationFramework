/*
 * Copyright 2015 AT&T Digital Life
 */
package com.xanboo.core.util.fs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.math.BigInteger;

import java.util.Properties;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import org.apache.commons.io.IOUtils;

import com.xanboo.core.util.Logger;
import com.xanboo.core.util.LoggerFactory;


/**
 * AT&T CLoud Storage media/file server/storage provider implementation.
 * 
 */
public class ATTCloudFSProvider implements XanbooFSInterface {

    private static AmazonS3 s3;
    private static String S3_BUCKET;
    private Logger logger;

    public void initialize(String props) {

        logger = LoggerFactory.getLogger(this.getClass().getName());

        synchronized (this) {

            if (s3 == null) {

                Properties cloudProviderProps = new Properties();

                try {
                    String propertiesFileLocation = System.getProperty("app.fs.provider.config");
                    FileInputStream in = new FileInputStream(propertiesFileLocation);
                    cloudProviderProps.load(in);
                    in.close();

                    if (!cloudProviderProps.containsKey("S3_ACCESS_KEY_ID")) {
                        throw new Exception("S3_ACCESS_KEY_ID does not exist");
                    }
                    if (!cloudProviderProps.containsKey("S3_SECRET_KEY")) {
                        throw new Exception("S3_SECRET_KEY does not exist");
                    }
                    if (!cloudProviderProps.containsKey("S3_ENDPOINT")) {
                        throw new Exception("S3_ENDPOINT does not exist");
                    }
                    if (!cloudProviderProps.containsKey("S3_BUCKET")) {
                        throw new Exception("S3_BUCKET does not exist");
                    } else {
                        S3_BUCKET = cloudProviderProps.getProperty("S3_BUCKET");
                    }

                    ClientConfiguration cc = new ClientConfiguration();
                    // Force use of v2 Signer.  ECS does not support v4 signatures yet.
                    // Without this setting, read operations would return an HTTP 403 forbidden error.
                    cc.setSignerOverride("S3SignerType");

                    if (cloudProviderProps.containsKey("CONNECTION_TIMEOUT")) {
                        //Sets the amount of time to wait (in milliseconds) when initially establishing
                        //a connection before giving up and timing out. A value of 0 means infinity.
                        cc.setConnectionTimeout(Integer.parseInt(cloudProviderProps.getProperty("CONNECTION_TIMEOUT")));
                    }
                    if (cloudProviderProps.containsKey("SOCKET_TIMEOUT")) {
                        //Sets the amount of time to wait (in milliseconds) for data to be transferred
                        //over an established, open connection before the connection times out and is
                        //closed. A value of 0 means infinity.
                        cc.setSocketTimeout(Integer.parseInt(cloudProviderProps.getProperty("SOCKET_TIMEOUT")));
                    }
                    if (cloudProviderProps.containsKey("MAX_RETRIES")) {
                        //Sets the maximum number of retry attempts for failed requests
                        cc.setMaxErrorRetry(Integer.parseInt(cloudProviderProps.getProperty("MAX_RETRIES")));
                    }

                    if (cloudProviderProps.containsKey("MAX_CONNECTIONS")) {
                        //Sets the maximum number of allowed open HTTP connections
                        cc.setMaxConnections(Integer.parseInt(cloudProviderProps.getProperty("MAX_CONNECTIONS")));
                    }

                    if (cloudProviderProps.containsKey("CONNECTION_TTL")) {
                        //Sets the expiration time (in milliseconds) for a connection in the connection pool.
                        cc.setConnectionTTL(Integer.parseInt(cloudProviderProps.getProperty("CONNECTION_TTL")));
                    }

                    BasicAWSCredentials creds = new BasicAWSCredentials(cloudProviderProps.getProperty("S3_ACCESS_KEY_ID"),
                            decryptPassword(cloudProviderProps.getProperty("S3_SECRET_KEY")));

                    s3 = new AmazonS3Client(creds, cc);
                    s3.setEndpoint(cloudProviderProps.getProperty("S3_ENDPOINT"));

                    // Path-style bucket naming
                    S3ClientOptions opts = new S3ClientOptions();
                    opts.setPathStyleAccess(true);
                    s3.setS3ClientOptions(opts);

                    //Check if bucket exists.  If not create it
                    if (!s3.doesBucketExist(S3_BUCKET)) {
                        s3.createBucket(S3_BUCKET);
                    }
                } catch (Exception e) {
                    logger.error("[AttCloudFSProvider()]: initialization error " + e.getMessage());
                }
            }
        }
    }

    public boolean checkDir(String dirPath, boolean create) {

        try {
            //Not implementing create.  You cannot create an empty directory
            //in the S3 clould implementation
            ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
                    .withBucketName(S3_BUCKET)
                    .withPrefix(dirPath));

            if (objectListing != null && objectListing.getObjectSummaries() != null && objectListing.getObjectSummaries().size() > 0) {
                return true;
            }
        } catch (Exception e) {
            logger.error("[ATTCloudFSProvider()]: checkDir error " + e.getMessage() );
        }
        return false;
    }

    public boolean mkDir(String dirPath) {
        return true;
    }

    public boolean rmDir(String dirPath, boolean recursive) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("[AttCloudFSProvider()]: rmDir " + dirPath);
            }
            //Not implementing recursive as stated in XanbooFSInterface.
            //In the cloud, you will not have an empty directory
        
            ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
                    .withBucketName(S3_BUCKET)
                    .withPrefix(dirPath));

            //delete all of the objects with the specified dirPath (key prefix)
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("[AttCloudFSProvider()]: deleting object " + objectSummary.getKey());
                }
                s3.deleteObject(S3_BUCKET, objectSummary.getKey());
            }
            return true;
        } catch (Exception e) {
            logger.error("[ATTCloudFSProvider()]: rmDir error " + e.getMessage() );
        }
        return false;
    }

    public String[] listDir(String dirPath) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("[AttCloudFSProvider()]: listDir " + dirPath);
            }
            ObjectListing objectListing = s3.listObjects(new ListObjectsRequest().withBucketName(S3_BUCKET).withPrefix(dirPath));

            if (objectListing == null || objectListing.getObjectSummaries() == null || objectListing.getObjectSummaries().size() == 0) {
                return null;
            }
            String[] returnVal = new String[objectListing.getObjectSummaries().size()];

            int i=0;

            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                returnVal[i++] =  objectSummary.getKey();
            }

            return returnVal;
        } catch (Exception e) {
            logger.error("[ATTCloudFSProvider()]: listDir error " + e.getMessage() );
        }
        return null;
    }    

    public boolean storeFile(File srcFile, String destFilePath, boolean createDirs) throws Exception {

        try {
            //The client-side and server-side md5 checksum are compared after upload.  If
            //they are not the same, an AmazonClientException is generated.
           s3.putObject(S3_BUCKET, destFilePath, srcFile);
           return true;
        } catch (Exception e) {
            logger.error("[ATTCloudFSProvider()]: storFile error " + e.getMessage() );
        }
        return false;
    }
    
    public boolean storeFile(byte [] bytes, String destFilePath, boolean createDirs) throws Exception {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("[AttCloudFSProvider()]: storing object " + destFilePath);
            }
            //Set the content length prior to s3.putObject to avoid:
            //"No content length specified for stream data.  Stream contents will
            //be buffered in memory and could result in out of memory errors."
            ObjectMetadata metaData = new ObjectMetadata();
            metaData.setContentLength(bytes.length);
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            s3.putObject(S3_BUCKET, destFilePath, bis, metaData);
            return true;
        } catch (Exception e) {
            logger.error("[ATTCloudFSProvider()]: storFile error " + e.getMessage() );
        }
        return false;
    }

    public boolean removeFile(String filePath) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("[AttCloudFSProvider()]: removeFile " + filePath);
            }
            s3.deleteObject(S3_BUCKET, filePath);
            return true;
        } catch (Exception e) {
            logger.error("[ATTCloudFSProvider()]: removeFile error " + e.getMessage() );
        }
        return false;
    }

    public List<String> removeFiles(List<String> filePaths) {
        return null;
    }

    public byte[] getFileBytes(String filePath) throws Exception {

        S3ObjectInputStream is = null;

        try {
            if (logger.isDebugEnabled()) {
                logger.debug("[AttCloudFSProvider()]: getFileBytes " + filePath);
            }
            // read the object from the bucket
            S3Object object = s3.getObject(S3_BUCKET, filePath);
            if (object == null) return null;

            is = object.getObjectContent();
            if (is == null)  return null;
            
            return IOUtils.toByteArray(is);
        } catch (Exception e) {
            logger.error("[ATTCloudFSProvider()]: getFileBytes error " + e.getMessage() );
        } finally {
            if (is != null) {
                is.close();
            }
        }
        return null;
    }

    public java.io.InputStream getFileAsStream(String filePath) throws Exception {
        return null;
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
