package com.xanboo.core.sdk.account;

import com.xanboo.core.sdk.util.XanbooResultSet;
import com.xanboo.core.util.GlobalNames;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;


public class DlaAuthServiceClient {

	private static final Logger logger = Logger.getLogger(DlaAuthServiceClient.class);

	public DlaAuthServiceClient() {
	}

	public void invalidateGatewayGuids(long accountId, XanbooResultSet gguidList) throws Exception {
   	 	if(logger.isDebugEnabled()){
   	 		logger.debug("[invalidateGatewayGuids] gguidList size = " + gguidList.size());
   	 	}
   	 	
		for (int i = 0; i < gguidList.size(); i++) {
			String gguid = gguidList.getElementString(i, "GATEWAY_GUID");
			if (gguid != null && gguid.length() > 0) {
				invalidate(gguid);
       	 		logger.info("[invalidateGatewayGuids]: invalidated cache for accountId = " + 
       	 						accountId + " gatewayId = " + gguid);
			}
		}
	}
	
	
	public void invalidate(String gatewayId) throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("DlaAuthServiceClient.invalidate(): gatewayId = " + gatewayId);
		}
				
        try {

    		String url = String.format("http://%s/invalidate/%s", GlobalNames.DLA_AUTHENTICATE_SERVICE_HOST, gatewayId);

    		if (logger.isDebugEnabled()) {
    			logger.debug("DlaAuthServiceClient.invalidate(): url = " + url);
    		}
    		
        	RequestConfig requestConfig = buildRequestConfig();

            HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
            HttpPost postRequest = new HttpPost(url);
            HttpResponse response = client.execute(postRequest);

            int responseCode = response.getStatusLine().getStatusCode();

            logger.info("[DlaAuthServiceClient()]: responseCode = " + responseCode + " for gatewayId = " + gatewayId);

        } catch (Exception e) {
        	logger.error("[invalidate()]: caught exception invalidating cache: " + e.getMessage());
            throw e;
        }
	}
	
	private RequestConfig buildRequestConfig() {
		int connectionTimeout = GlobalNames.DLA_AUTHENTICATE_SERVICE_CONNECTION_TIMEOUT;
		RequestConfig requestConfig = RequestConfig.custom()
		.setConnectTimeout(connectionTimeout)
		.setConnectionRequestTimeout(connectionTimeout)
		.setSocketTimeout(connectionTimeout).build();
        
        return requestConfig;
	}

}
