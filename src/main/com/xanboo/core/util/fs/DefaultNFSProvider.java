/*
 * Copyright 2015 AT&T Digital Life
 */

package com.xanboo.core.util.fs;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import com.xanboo.core.util.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
/**
 * Default Local File System media/file server/storage provider implementation.
 * 
 */
public class DefaultNFSProvider implements XanbooFSInterface {
    
    private static Logger logger = null;
    
    
    public void initialize(String props) {
       // Create a Logger
        logger=LoggerFactory.getLogger(this.getClass().getName());  
        if(logger.isDebugEnabled()) {
            logger.debug("[initialize()]: props=" + props);
        }
    }
    
        
    public boolean checkDir(String dirPath, boolean create) {
        try {
            File dir = new File(dirPath);
            
            boolean exists = dir.exists();
            if(!exists && create) {
                exists = dir.mkdirs();
            }
            
            return exists;
            
        }catch(Exception e) {
            return false;
        }
    }
    
    
    public boolean mkDir(String dirPath) {
        try {
            File dir = new File(dirPath);
            return dir.mkdirs();
            
        }catch(Exception e) {
            return false;
        }
    }

    
    public boolean rmDir(String dirPath, boolean recursive) {
        try {
            File dir = new File(dirPath);
            
            if (dir.isDirectory()) {
                String[] children = dir.list();
                for (int i=0; i<children.length; i++) {
                    boolean success = rmDir( children[i], recursive );
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
            
        }catch(Exception e) {
            return false;
        }
    }
    
    
    public String[] listDir(String dirPath) {
        try {
            File dir = new File(dirPath);
            return dir.list();
        }catch(Exception e) {
            return null;
        }
    }
    
    
    
    public boolean storeFile(File srcFile, String destFilePath, boolean createDirs) throws Exception {
        if(logger.isDebugEnabled()) {
            logger.debug("[storeFile()]: Storing '" + srcFile.getAbsolutePath() + "' to '" + destFilePath );
        }
        
        boolean succ = true;
                
        try {
            File destFile = new File(destFilePath);
            
            succ = copyFile(srcFile, destFile);

            if(!succ && createDirs) {
                //failed, check if parent dir exists, create if not
                if(!destFile.getParentFile().exists()) {
                  destFile.getParentFile().mkdirs();
                  
                  //retry
                  succ = copyFile(srcFile, destFile);
                }
            }

            if(!succ) {
                logger.error("[storeFile()]: Failed to store '" + srcFile.getAbsolutePath() + "' to '" + destFilePath );
            }
            
        }catch(Exception e) {
            logger.error("[storeFile()]: EXCEPTION storing '" + srcFile.getAbsolutePath() + "' to '" + destFilePath, e);
            succ = false;
        }
        
        return succ;
    }

    
    public boolean storeFile(byte[] bytes, String destFilePath, boolean createDirs) throws Exception {
        if(logger.isDebugEnabled()) {
            logger.debug("[storeFile()]: Storing bytes to '" + destFilePath );
        }
        
        boolean succ = true;
                
        try {
            File destFile = new File(destFilePath);
            
            succ = saveToFile(bytes, destFile);

            if(!succ && createDirs) {
                //failed, check if parent dir exists, create if not
                if(!destFile.getParentFile().exists()) {
                  destFile.getParentFile().mkdirs();
                  
                  //retry
                  succ = saveToFile(bytes, destFile);
                }
            }

            if(!succ) {
                logger.error("[storeFile()]: Failed to store bytes to '" + destFilePath );
            }
            
        }catch(Exception e) {
            logger.error("[storeFile()]: EXCEPTION storing bytes to '" + destFilePath, e);
            succ = false;
        }
        
        return succ;
    }
    

    public boolean removeFile(String filePath) {
        File f = new File(filePath);
        
        try {
            return f.delete();
        }catch(Exception e) {}
        
        return false;
    }
    
    public List<String> removeFiles(List<String> filePaths) {
        return null;
    }

    public static byte[] getFileBytes(File f) throws Exception {
        InputStream in = null;
        try {
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
    
    public byte[] getFileBytes(String filePath) throws Exception {
        File f = new File(filePath);
        return getFileBytes(f);
    }
    

    public java.io.InputStream getFileAsStream(String filePath) throws Exception {
        return null;
    }
    
    //-----------------------------------------------------------------------------------
    private static boolean copyFile(File source, File dest) { 
        FileChannel inputChannel = null; 
        FileChannel outputChannel = null; 

        try { 
            inputChannel = new FileInputStream(source).getChannel(); 
            outputChannel = new FileOutputStream(dest).getChannel(); 

            outputChannel.transferFrom(inputChannel, 0, inputChannel.size()); 
            
            return true;
        }catch(FileNotFoundException fe) {
            return false;
        }catch(IOException ioe) {
            return false;
            
        }finally { 
            try {
                if(inputChannel!=null) inputChannel.close(); 
                if(outputChannel!=null) outputChannel.close(); 
            }catch(IOException ioe) {}
        } 
    } 
            

    private static boolean saveToFile(byte[] fileBytes, File dest) {
        OutputStream fout = null;
        try {
            fout = new BufferedOutputStream(new FileOutputStream(dest));
            fout.write(fileBytes);

        }catch(FileNotFoundException fe) {
            return false;
        }catch(IOException ioe) {
            return false;
            
        } finally {
            try {
                if(fout != null) fout.close();
            }catch (Exception e) {}
        }
        
        return true;
    }
    
}
