package net.boomerangplatform.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.auth.ApiKeyAuth;

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
		ApiClient defaultClient = io.kubernetes.client.Configuration.getDefaultApiClient().setVerifyingSsl(false).setDebugging(kubeApiDebug.isEmpty() ? false : Boolean.valueOf(kubeApiDebug));
		defaultClient.setBasePath(kubeApiBasePath);

		ApiKeyAuth apiKeyAuth = (ApiKeyAuth) defaultClient.getAuthentication("BearerToken");
		apiKeyAuth.setApiKey(kubeApiToken);
		apiKeyAuth.setApiKeyPrefix("Bearer");
		
		return defaultClient;
	}
}
