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

	@Bean
	public ApiClient connectToKube() {
		ApiClient defaultClient = io.kubernetes.client.Configuration.getDefaultApiClient().setVerifyingSsl(false).setDebugging(true);
		defaultClient.setBasePath(kubeApiBasePath);

		ApiKeyAuth fakeBearerToken = (ApiKeyAuth) defaultClient.getAuthentication("BearerToken");
		fakeBearerToken.setApiKey(kubeApiToken);
		fakeBearerToken.setApiKeyPrefix("Bearer");
		
		return defaultClient;
	}
}
