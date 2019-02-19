/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.xanboo.core.sdk.pai;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.xanboo.core.security.XanbooAdminPrincipal;
import com.xanboo.core.util.XanbooException;

/**
 *
 * @author lm899p
 */

public interface PAICommandREST
{
    public void sendDisconnect(XanbooAdminPrincipal xCaller,String gguid,boolean reconnect)throws RemoteException, XanbooException;
    
    public String sendSetObject(XanbooAdminPrincipal xCaller,String gatewayGuid,String deviceGuid,Date timestamp,
                              Long comandQueueId,List<HashMap> parameters) throws RemoteException, XanbooException;
    
    public String sendSetObject(XanbooAdminPrincipal xCaller,String gatewayGuid,String deviceGuid,Date timestamp,Long comandQueueId,
                                String paiServerUrl,String paiAccessToken,List<HashMap> parameters) throws RemoteException, XanbooException;
    
    public void updateCommandStatus(XanbooAdminPrincipal xCaller,Long commandQueueId)throws RemoteException,XanbooException;
}
