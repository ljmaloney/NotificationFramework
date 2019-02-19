/*
 * Copyright 2015 AT&T Digital Life
 */

package com.xanboo.core.util.fs;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;

/**
 * Interface to abstract media/file store write operations. 
 * 
 */
public interface XanbooFSInterface {
    
    /**
     * Initializes the provider implementation with a properties string
     * 
     * @param props 
     */
    abstract public void initialize(String props);
    
    
    /**
     * Checks if a folder with specified path on the file server exists or not, and create if requested. 
     * 
     * @param dirPath full directory path to create
     * @param create a boolean flag to create the folder if it doesn't exist
     * 
     * @return boolean true if success
     */
    abstract public boolean checkDir(String dirPath, boolean create);

    
    /**
     * Creates a new folder with specified path on the file server
     * 
     * @param dirPath full directory path to create
     * 
     * @return boolean true if success
     */
    abstract public boolean mkDir(String dirPath);

    
    /**
     * Removes an existing folder with specified path from the file server.
     * 
     * @param dirPath full directory path to remove
     * @param recursive boolean to indicate all folder content will be recursively removed as well. If false, folder must be empty to be removed.
     * 
     * @return boolean true if success
     */
    abstract public boolean rmDir(String dirPath, boolean recursive);
    

    /**
     * Returns the list of folder content
     * 
     * @param dirPath full directory to list
     * 
     * @return a String array of file and subdirectory names
     */
    abstract public String[] listDir(String dirPath);
    

    
    /**
     * Stores/saves a local file to a destination folder on the file server
     * Implementations must NOT delete/move the original source file passed
     * 
     * @param srcFile a File handler to save data from
     * @param destFilePath full destination path to store/save the file to
     * @param createDirs a boolean to created folders recursively, if destination path doesnt exist
     * 
     * @return boolean true if success
     */
    abstract public boolean storeFile(File srcFile, String destFilePath, boolean createDirs) throws Exception;
    
    
    /**
     * Stores/saves a byte array to a destination folder on the file server
     * 
     * @param bytes a byte array to store/save
     * @param destFilePath full destination path to store/save the file to
     * @param createDirs a boolean to created folders recursively, if destination path doesn't exist
     * 
     * @return boolean true if success
     */
    abstract public boolean storeFile(byte[] bytes, String destFilePath, boolean createDirs) throws Exception;

    
    
    /**
     * Removes an existing file with specified path on the file server
     * 
     * @param filePath full file path to remove
     * 
     * @return boolean true if success
     */
    abstract public boolean removeFile(String filePath);
    
    /**
     * Removes an existing list of files with specified paths on the file server
     *
     * @param filePaths a list of file paths to remove
     *
     * @return List a list of file paths that could not be removed
     */
    abstract public List<String> removeFiles(List<String> filePaths);
    
    /**
     * Reads/retrieves an existing file with specified path from the file server
     * 
     * @param filePath full file path to retrieve
     * 
     * @return byte[] the file content 
     */
    abstract public byte[] getFileBytes(String filePath) throws Exception;


    /**
     * Reads/retrieves an existing file with specified path from the file server
     *
     * @param filePath full file path to retrieve
     *
     * @return InputStream of the file
     */
    abstract public InputStream getFileAsStream(String filePath) throws Exception;
    
}
