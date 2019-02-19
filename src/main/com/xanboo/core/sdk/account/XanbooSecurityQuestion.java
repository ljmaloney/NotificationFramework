/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.sdk.account;

/**
 *
 * @author lt9550
 */
public class XanbooSecurityQuestion implements java.io.Serializable {
    private static final long serialVersionUID = 5603405972256045088L;
    
    /** Constant value to indicate maximum number of security question objects supported for a user */
    public static final int MAX_SUPPORTED=2;
    
    /** Constant value to indicate security question is not set */
    public static final int NOTSET=0;
    
    /** Constant value to indicate security question ID should not be changed on update operations */
    public static final int UNCHANGED=-1;
    
    /** Holds user security question ID 1. Can be set to XanbooSecurityQuestion.NOTSET, XanbooSecurityQuestion.UNCHANGED and any positive integer */
    private int secQ;
    
    /** Holds user security answer 1. */
    private String secA;
 
    
    /** Default constructor: Creates a new XanbooSecurityQuestion */
    public XanbooSecurityQuestion() {
        this.secQ=-1;
        this.secA=null;
    }

    /** Creates a new XanbooSecurityQuestion from input parameters */
    public XanbooSecurityQuestion(int secQ, String secA) {
        setSecQuestionId(secQ);
        setSecAnswer(secA);
    }
    
    /** Returns security question ID */
    public int getSecQuestionId() {
        return secQ;
    }
    
    /** Sets security question ID. Can be set to XanbooSecurityQuestion.NOTSET, XanbooSecurityQuestion.UNCHANGED and any positive integer
     that corresponds to a security question ID. Passing XanbooSecurityQuestion.NOTSET to the update calls (via updateUser) will clear/unset
     the existing security question record.
     Query calls will return the answer field as null since they are hashed and not readable. */
    public void setSecQuestionId(int secQ) {
        if(secQ==NOTSET || secQ==UNCHANGED || secQ>0) {
            this.secQ = secQ;
        }else {
            this.secQ = NOTSET;
        }
    }

    /** Returns security question answer (may be hashed on query returns) */
    public String getSecAnswer() {
        return secA;
    }
    
    /** Sets security question answer.  
     Query calls will return the answer field as null since they are hashed and not readable. */
    public void setSecAnswer(String secA) {
        this.secA = secA;
    }
    
    /** Validates if the object contains a valid question id and an answer*/
    public boolean isValid() {
        if(this.secQ>=0 && this.secA!=null && this.secA.trim().length()>0) return true;
        return false;
    }
    
    public String toString() {
        return "SECURITY QUESTION:  ID=" + this.getSecQuestionId() + ",  ANSWER=" + this.getSecAnswer();
    }
    
}
