package net.boomerangplatform.configuration;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.util.Config;

@Configuration
public class KubeConfiguration {

	@Value("${kube.api.base.path}")
	public String kubeApiBasePath;

	@Value("${kube.api.token}")
	public String kubeApiToken;
	
	@Value("${kube.api.debug}")
	public String kubeApiDebug;

	@Bean
	public ApiClient connectToKube() {
//		ApiClient defaultClient = null;
//		if (kubeApiBasePath.equals("")) {
		ApiClient defaultClient = io.kubernetes.client.Configuration.getDefaultApiClient().setVerifyingSsl(false).setDebugging(true);
		io.kubernetes.client.Configuration.setDefaultApiClient(defaultClient);
		return defaultClient;
//		} else {
//			defaultClient = io.kubernetes.client.Configuration.getDefaultApiClient().setVerifyingSsl(false).setDebugging(kubeApiDebug.isEmpty() ? false : Boolean.valueOf(kubeApiDebug));
//			defaultClient.setBasePath(kubeApiBasePath);
//			defaultClient.getHttpClient().setReadTimeout(60, TimeUnit.SECONDS); //added for watcher to not timeout
//	
//			ApiKeyAuth apiKeyAuth = (ApiKeyAuth) defaultClient.getAuthentication("BearerToken");
//			apiKeyAuth.setApiKey(kubeApiToken);
//			apiKeyAuth.setApiKeyPrefix("Bearer");
//		}
//		ApiClient defaultClient = Config.fromToken(kubeApiBasePath, kubeApiToken, false).setVerifyingSsl(false).setDebugging(kubeApiDebug.isEmpty() ? false : Boolean.valueOf(kubeApiDebug));
//		defaultClient.getHttpClient().setReadTimeout(60, TimeUnit.SECONDS);
	}
}
