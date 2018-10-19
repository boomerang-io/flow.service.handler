package net.boomerangplatform.service;

import java.util.Base64;

import org.springframework.stereotype.Service;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.auth.ApiKeyAuth;
import io.kubernetes.client.models.V1NamespaceList;

@Service
public class KubeServiceImpl implements KubeService {

    @Override
    public V1NamespaceList getAllNamespaces() {
//    	ApiClient defaultClient = Configuration.getDefaultApiClient().setVerifyingSsl(false);      
//        ApiKeyAuth BearerToken = (ApiKeyAuth) defaultClient.getAuthentication("BearerToken");
//        BearerToken.setApiKey("ntk5WcP7XmfmSHUg00tRM4ytMMdMALjB0FyvHll1HK-k");
//        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//        BearerToken.setApiKeyPrefix("Token");        
        
//        ApiClient client = null;
//        try {
//        	client = Config.defaultClient();
//        } catch (IOException e) {
//        	e.printStackTrace();
//        }
//        Configuration.setDefaultApiClient(client);
                
//        String credentials= new String(Base64.getEncoder().encode("admin:1CfaLDU#".getBytes()));
        ApiClient  defaultClient = Configuration.getDefaultApiClient().setVerifyingSsl(false);
        defaultClient.setBasePath("http://9.42.74.37");
        ApiKeyAuth fakeBearerToken = (ApiKeyAuth) defaultClient.getAuthentication("BearerToken");
//        fakeBearerToken.setApiKey(credentials);
//        fakeBearerToken.setApiKeyPrefix("Basic");               
        fakeBearerToken.setApiKey("ntk5WcP7XmfmSHUg00tRM4ytMMdMALjB0FyvHll1HK-k");
        fakeBearerToken.setApiKeyPrefix("Token");                  
        
        V1NamespaceList list = new V1NamespaceList();         		
        try {
        	CoreV1Api api = new CoreV1Api();
			list = api.listNamespace(null, null, null, null, null, null, null, null, null);
		} catch (ApiException e) {
			e.printStackTrace();
		}
        
        return list;
    }
}
