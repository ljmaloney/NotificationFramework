/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/folder/XanbooFolder.java,v $
 * $Id: XanbooFolder.java,v 1.4 2002/06/12 20:45:35 levent Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.folder;

/**
 * Class to represent Xanboo folder
 */
public class XanbooFolder implements java.io.Serializable {
    private static final long serialVersionUID = -8326553657714041036L;

    /** Holds value of property userId. */
    private long userId;
    
    /** Holds value of property accountId. */
    private long accountId;
    
    /** Holds value of property folderId. */
    private long folderId;
    
    /** Holds value of property parentFolderId. */
    private long parentFolderId;
    
    /** Holds value of property type. */
    private int type;
    
    /** Holds value of property name. */
    private String name;
    
    /** Holds value of property description. */
    private String description;
    
    /** Holds value of property itemCount. */
    private int itemCount;
    
    /** Holds value of property publicFolder. */
    private boolean publicFolder;
    
    /** Holds value of property subfolderCount. */
    private int subfolderCount;
    
    /** Holds value of property isPublic. */
    private int isPublic;
    
    /** Holds value of property creationDate. */
    private String creationDate;
    
    /** Creates new XanbooFolder */
    public XanbooFolder() {
    }

    /** Creates new XanbooFolder */
    public XanbooFolder(String name) {
        this.name = name;
        this.description = "desc";
        this.parentFolderId = 0;
    }

    /** Getter for property userId.
     * @return Value of property userId.
     */
    public long getUserId() {
        return userId;
    }
    
    /** Setter for property userId.
     * @param userId New value of property userId.
     */
    public void setUserId(long userId) {
        this.userId = userId;
    }
    
    /** Getter for property accountId.
     * @return Value of property accountId.
     */
    public long getAccountId() {
        return accountId;
    }
    
    /** Setter for property accountId.
     * @param accountId New value of property accountId.
     */
    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }
    
    /** Getter for property folderId.
     * @return Value of property folderId.
     */
    public long getFolderId() {
        return folderId;
    }
    
    /** Setter for property folderId.
     * @param folderId New value of property folderId.
     */
    public void setFolderId(long folderId) {
        this.folderId = folderId;
    }
    
    /** Getter for property parentFolderId.
     * @return Value of property parentFolderId.
     */
    public long getParentFolderId() {
        return parentFolderId;
    }
    
    /** Setter for property parentFolderId.
     * @param parentFolderId New value of property parentFolderId.
     */
    public void setParentFolderId(long parentFolderId) {
        this.parentFolderId = parentFolderId;
    }
    
    /** Getter for property type.
     * @return Value of property type.
     */
    public int getType() {
        return type;
    }
    
    /** Setter for property type.
     * @param type New value of property type.
     */
    public void setType(int type) {
        this.type = type;
    }
    
    /** Getter for property name.
     * @return Value of property name.
     */
    public String getName() {
        return name;
    }
    
    /** Setter for property name.
     * @param name New value of property name.
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /** Getter for property description.
     * @return Value of property description.
     */
    public String getDescription() {
        return description;
    }
    
    /** Setter for property description.
     * @param description New value of property description.
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /** Getter for property itemCount.
     * @return Value of property itemCount.
     */
    public int getItemCount() {
        return itemCount;
    }
    
    /** Setter for property itemCount.
     * @param itemCount New value of property itemCount.
     */
    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }
    
    /** Getter for property publicFolder.
     * @return Value of property publicFolder.
     */
    public boolean isPublicFolder() {
        return publicFolder;
    }
    
    /** Setter for property publicFolder.
     * @param publicFolder New value of property publicFolder.
     */
    public void setPublicFolder(boolean publicFolder) {
        this.publicFolder = publicFolder;
    }
    
    /** Getter for property subfolderCount.
     * @return Value of property subfolderCount.
     */
    public int getSubfolderCount() {
        return subfolderCount;
    }
    
    /** Setter for property subfolderCount.
     * @param subfolderCount New value of property subfolderCount.
     */
    public void setSubfolderCount(int subfolderCount) {
        this.subfolderCount = subfolderCount;
    }
    
    /** Getter for property isPublic.
     * @return Value of property isPublic.
     */
    public int getIsPublic() {
        return isPublic;
    }
    
    /** Setter for property isPublic.
     * @param isPublic New value of property isPublic.
     */
    public void setIsPublic(int isPublic) {
        this.isPublic = isPublic;
    }
    
    /** Getter for property creationDate.
     * @return Value of property creationDate.
     */
    public String getCreationDate() {
        return creationDate;
    }
    
    /** Setter for property creationDate.
     * @param creationDate New value of property creationDate.
     */
    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }
    
}
