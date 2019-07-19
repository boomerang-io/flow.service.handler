package net.boomerangplatform.configuration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.auth.ApiKeyAuth;
import io.kubernetes.client.util.Config;

@Configuration
public class KubeConfiguration extends WebMvcConfigurerAdapter {

	@Value("${kube.api.base.path}")
	private String kubeApiBasePath;

	@Value("${kube.api.token}")
	private String kubeApiToken;
	
	@Value("${kube.api.debug}")
	private String kubeApiDebug;
	
	@Value("${kube.api.type}")
	private String kubeApiType;
	
	@Value("${spring.mvc.async.request-timeout}")
	private String asyncRequestTimeout;

	@Bean
	public ApiClient connectToKube() {
		ApiClient defaultClient = null;
		if (kubeApiType.equals("cluster")) {
			try {
				defaultClient = Config.fromCluster().setVerifyingSsl(false).setDebugging(kubeApiDebug.isEmpty() ? false : Boolean.valueOf(kubeApiDebug));
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			defaultClient = io.kubernetes.client.Configuration.getDefaultApiClient().setVerifyingSsl(false).setBasePath(kubeApiBasePath).setDebugging(kubeApiDebug.isEmpty() ? false : Boolean.valueOf(kubeApiDebug));
		}
		
		if (!kubeApiToken.isEmpty()) {
			ApiKeyAuth apiKeyAuth = (ApiKeyAuth) defaultClient.getAuthentication("BearerToken");
			apiKeyAuth.setApiKey(kubeApiToken);
			apiKeyAuth.setApiKeyPrefix("Bearer");
		}
		io.kubernetes.client.Configuration.setDefaultApiClient(defaultClient);
		defaultClient.getHttpClient().setReadTimeout(300, TimeUnit.SECONDS); //added for watcher to not timeout
		return defaultClient;

//		ApiClient defaultClient = Config.fromToken(kubeApiBasePath, kubeApiToken, false).setVerifyingSsl(false).setDebugging(kubeApiDebug.isEmpty() ? false : Boolean.valueOf(kubeApiDebug));
//		defaultClient.getHttpClient().setReadTimeout(60, TimeUnit.SECONDS);
	}
	
	@Override
	public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
		configurer.setDefaultTimeout(Integer.valueOf(asyncRequestTimeout));
		configurer.setTaskExecutor(asyncTaskExecutor());
	}
	
	@Bean
	public AsyncTaskExecutor asyncTaskExecutor() {
		return new SimpleAsyncTaskExecutor("async");
	}
}
