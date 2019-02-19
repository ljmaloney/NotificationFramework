package com.xanboo.core.util;

public class SimpleHttpResponse {
    int retCode;
    String retMsg;
    String response;

    public SimpleHttpResponse(int ret, String msg) {
        this.retCode = ret;
        this.retMsg = msg;
    }

    public boolean isSuccess() { if(this.retCode==200) return true; else return false; }
    public int getReturnCode() { return this.retCode; }
    public String getReturnMsg() { return this.retMsg; }
    
    public String getResponse() { return this.response; }
    public void setResponse(String resp) { this.response = resp; }
}
