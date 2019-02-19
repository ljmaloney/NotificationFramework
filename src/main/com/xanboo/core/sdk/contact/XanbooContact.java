/*
 * $Source: /export/home/cvsroot/xancore/src/sdk/com/xanboo/core/sdk/contact/XanbooContact.java,v $
 * $Id: XanbooContact.java,v 1.3 2002/06/21 15:50:22 rking Exp $
 * 
 * Copyright 2002 Xanboo, Inc.
 *
 */

package com.xanboo.core.sdk.contact;

import com.xanboo.core.util.*;

/**
 * Class to represent Xanboo contact information for users and contacts
 * <br/>
 * The correspondence between phone, phoneCell, and fax to the phone[n]Type, phone[n]ProfileType, and phone[n]SMS
 * <ul>
 *  <li>phone -> phone1Type, phone1ProfileType, phone1SMS
 *  <li>phoneCell -> phone2Type, phone2ProfileType, phone2SMS
 *  <li>fax -> phone3Type, phone3ProfileType, phone3SMS
 * </ul>
 */
public class XanbooContact implements java.io.Serializable {
    private static final long serialVersionUID = 5968651903197247212L;
    
    private static final int INFO_USER=0;
    private static final int INFO_CONTACT=1;
    
    private long accountId;     // account id
    private long userId;        // user that owns the contact
    private long contactId;     // contact identfifier
    private int type;           // contact type -> 0:user info, 1:contact
    
    private String company;
    private String lastName;
    private String firstName;
    private String middleName;
    
    private String phone;
    private String cellPhone;
    private String fax;
    private String email;
    private String pagerId;
    private String url;

    private String address1;
    private String address2;
    private String city;
    private String state;
    private String zip;
    private String zip4;
    private String areaCode;
    private String country;

    private String username;    

    private String relationship;
    private GenderType gender;
    private long emailProfileType;
    private String phone1Type;
    private long phone1ProfileType;
    private boolean phone1SMSPref;
    private String phone2Type;
    private long phone2ProfileType;
    private boolean phone2SMSPref;
    private String phone3Type;
    private long phone3ProfileType;
    private boolean phone3SMSPref;
        
    public enum GenderType 
    {
        MALE("M"),FEMALE("F");
        private String type = "";
        GenderType(String type){this.type = type;}
        private String type(){return this.type;}
        public String getDBValue(){return type;}
    };    
    
    /**
     * Default constructor: Creates a blank contact record.
     */
    public XanbooContact() {
        this.accountId=-1L;
        this.userId=-1L;
        this.contactId=-1L;
        
        this.type=1;        // 0:user info, 1:contact info
        
        this.company="";
        this.lastName="";
        this.firstName="";
        this.middleName="";
        this.phone="";
        this.cellPhone="";
        this.fax="";
        this.email="";
        this.pagerId="";
        this.url="";

        this.address1="";
        this.address2="";
        this.city="";
        this.state="";
        this.zip="";
        this.zip4="";
        this.areaCode="";
        this.country="";
        
        relationship = "";
        emailProfileType = -1;
        phone1Type = "";
        phone1ProfileType = -1;
        phone1SMSPref = false;
        phone2Type = "";
        phone2ProfileType = -1;
        phone2SMSPref = false;
        phone3ProfileType = -1;
        phone3Type = "";
        phone3SMSPref = false;
    }

    /**
     * validates a contact information within the Xanboo system
     * 
     * @return true if contact information in XanbooContact were valid, or false if not valid
     */
    public boolean isValid() { 
        //..short circuit ok
        if ((this.accountId <= 0L) || 
            (this.userId <= 0L) || 
            (this.lastName.trim().equals("")) ||
            (!XanbooUtil.isValidEmail(this.email))) {    
            return false;  // invalid contact iformation
          }
          return true;   // valid contact information
    }
    
    // getters
    
    /** Gets the account id for the contact */
    public long getAccountId() { return this.accountId; }
    
    /** Gets the user id for the contact */
    public long getUserId() { return this.userId; }

    /** Gets the contact id for the contact object */
    public long getContactId() { return this.contactId; }
    
    /** Gets the contact type. Possible values are XanbooContact.INFO_USER and XanbooContact.INFO_CONTACT. */
    public int getType() { return this.type; }

    /** Gets contact company name */
    public String getCompany() { return this.company; }
    
    /** Gets contact last name */
    public String getLastName() { return this.lastName; }
    
    /** Gets contact first name */
    public String getFirstName() { return this.firstName; }
    
    /** Gets contact middle name */
    public String getMiddleName() { return this.middleName; }
    
    /** Gets contact phone number */
    public String getPhone() { return this.phone; }

    /** Gets contact cell phone number */
    public String getCellPhone() { return this.cellPhone; }

    /** Gets contact fax number */
    public String getFax() { return this.fax; }

    /** Gets contact email address */
    public String getEmail() { return this.email; }
    
    /** Gets contact pager id */
    public String getPager() { return this.pagerId; }
    
    /** Gets contact url address */
    public String getUrl() { return this.url; }

    /** Gets contact street address1 */
    public String getAddress1() { return this.address1; }
    
    /** Gets contact street address2 */
    public String getAddress2() { return this.address2; }
    
    /** Gets contact city */
    public String getCity() { return this.city; }
    
    /** Gets contact state */
    public String getState() { return this.state; }
    
    /** Gets contact zip code (5-digit max) */
    public String getZip() { return this.zip; }
    
    /** Gets contact zip code (4-digit max)*/
    public String getZip4() { return this.zip4; }
    
    /** Gets contact area code */
    public String getAreaCode() { return this.areaCode; }
    
    /** Gets contact country */
    public String getCountry() { return this.country; }
    
    public String getRelationship() { return this.relationship; }
    
    public String getGender() {return (gender != null ? this.gender.getDBValue() : null); }
    
    public long getEmailProfileType() { return this.emailProfileType;}
    
    public String getPhone1Type(){return this.phone1Type;}
    
    public long getPhone1ProfileType(){return this.phone1ProfileType;}
    
    public boolean getPhone1SMS(){return this.phone1SMSPref;}
    
    public String getPhone2Type(){return this.phone2Type;}
    
    public long getPhone2ProfileType(){return this.phone2ProfileType;}
    
    public boolean getPhone2SMS(){return this.phone2SMSPref;}
    
    public String getPhone3Type(){return this.phone3Type;}
    
    public long getPhone3ProfileType(){return this.phone3ProfileType;}
    
    public boolean getPhone3SMS(){return this.phone3SMSPref;}

    /** Sets contact account id */
    public void setAccountId(long accountId) { this.accountId=accountId; }
    
    /** Sets contact user id */
    public void setUserId(long userId) { this.userId=userId; }
    
    /** Sets contact identifier */
    public void setContactId(long contactId) { this.contactId=contactId; }
    
    /** Sets contact type. Possible values are XanbooContact.INFO_USER and XanbooContact.INFO_CONTACT. */
    public void setType(int type) { this.type=type; }

    /** Sets contact company name */
    public void setCompany(String company) { this.company=company; }

    /** Sets contact last name */
    public void setLastName(String lastName) { this.lastName=lastName; }

    /** Sets contact first name */
    public void setFirstName(String firstName) { this.firstName=firstName; }

    /** Sets contact middle name */
    public void setMiddleName(String middleName) { this.middleName=middleName; }
    
    /** Sets contact phone number */
    public void setPhone(String phone) { this.phone=phone; }

    /** Sets contact cell phone number */
    public void setCellPhone(String cellPhone) { this.cellPhone=cellPhone; }

    /** Sets contact fax number */
    public void setFax(String fax) { this.fax=fax; }

    /** Sets contact email address */
    public void setEmail(String email) { this.email=email; }

    /** Sets contact pager id */
    public void setPager(String pagerId) { this.pagerId=pagerId; }

    /** Sets contact url address */
    public void setUrl(String url) { this.url=url; }

    /** Sets contact street address1 */
    public void setAddress1(String address1) { this.address1=address1; }

    /** Sets contact street address2 */
    public void setAddress2(String address2) { this.address2=address2; }

    /** Sets contact city */
    public void setCity(String city) { this.city=city; }

    /** Sets contact state */
    public void setState(String state) { this.state=state; }

    /** Sets contact zip code (5-digit max) */
    public void setZip(String zip) { this.zip=zip; }

    /** Sets contact zip code (4-digit max)*/
    public void setZip4(String zip4) { this.zip4=zip4; }

    /** Sets contact area code */
    public void setAreaCode(String areaCode) { this.areaCode=areaCode; }

    /** Sets contact country */
    public void setCountry(String country) { this.country=country; }
    
    public void setRelationship(String relationship) { this.relationship = relationship; }
    
    public void setGender(String gender) 
    { 
        if ( gender.equals(GenderType.MALE.getDBValue()))
            this.gender = GenderType.MALE;
        else if ( gender.equals(GenderType.FEMALE.getDBValue()))
            this.gender = GenderType.FEMALE;
    }
    
    public void setEmailProfileType(long type) {  this.emailProfileType = type ;}
    
    public void setPhone1Type(String type){ this.phone1Type = type;}
    
    public void setPhone1ProfileType(long type){ this.phone1ProfileType = type;}
    
    public void setPhone1SMS(boolean sms){ this.phone1SMSPref = sms;}
    
    public void setPhone2Type(String type){ this.phone2Type=type;}
    
    public void setPhone2ProfileType(long type){ this.phone2ProfileType=type;}
    
    public void setPhone2SMS(boolean sms){ this.phone2SMSPref=sms;}
    
    public void setPhone3Type(String type){ this.phone3Type=type;}
    
    public void setPhone3ProfileType(long type){ this.phone3ProfileType=type;}
    
    public void setPhone3SMS(boolean sms){ this.phone3SMSPref=sms;}

    public boolean hasAnyData() {
        if(this.lastName!=null && this.lastName.length()>0) return true;
        if(this.firstName!=null && this.firstName.length()>0) return true;
        if(this.address1!=null && this.address1.length()>0) return true;
        if(this.address2!=null && this.address2.length()>0) return true;
        if(this.city!=null && this.city.length()>0) return true;
        if(this.state!=null && this.state.length()>0) return true;
        if(this.zip!=null && this.zip.length()>0) return true;
        if(this.zip4!=null && this.zip4.length()>0) return true;
        if(this.country!=null && this.country.length()>0) return true;
        if(this.phone!=null && this.phone.length()>0) return true;
        if(this.cellPhone!=null && this.cellPhone.length()>0) return true;

        return false;
    }
 
    /** Dumps the content of the object to stderr */
    public void dump() {
        System.err.println("ACCOUNT ID   :" + this.accountId);
        System.err.println("USER ID      :" + this.userId);
        System.err.println("CONTACT ID   :" + this.contactId);
        System.err.println("TYPE         :" + this.type);
        
        System.err.println("COMPANY      :" + this.company);
        System.err.println("LASTNAME     :" + this.lastName);
        System.err.println("FIRSTNAME    :" + this.firstName);
        System.err.println("MIDDLENAME   :" + this.middleName);

        System.err.println("PHONE        :" + this.phone);
        System.err.println("CELLPHONE    :" + this.cellPhone);
        System.err.println("FAX          :" + this.fax);
        System.err.println("EMAIL        :" + this.email);
        System.err.println("PAGER        :" + this.pagerId);
        System.err.println("URL          :" + this.url);

        System.err.println("ADDRESS1     :" + this.address1);
        System.err.println("ADDRESS2     :" + this.address2);
        System.err.println("CITY         :" + this.city);
        System.err.println("STATE        :" + this.state);
        System.err.println("ZIP          :" + this.zip);
        System.err.println("ZIP4         :" + this.zip4);
        System.err.println("AREACODE     :" + this.areaCode);
        System.err.println("COUNTRY      :" + this.country);
        System.err.println("USERNAME     :" + this.username);
    }
    
    /** Getter for property username.
     * @return Value of property username.
     */
    public String getUsername() {
        return username;
    }
    
    /** Setter for property username.
     * @param username New value of property username.
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
}


