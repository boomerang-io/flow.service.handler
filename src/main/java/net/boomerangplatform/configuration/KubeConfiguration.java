package net.boomerangplatform.configuration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.auth.ApiKeyAuth;
import io.kubernetes.client.util.Config;
import net.boomerangplatform.kube.exception.KubeRuntimeException;

@Configuration
public class KubeConfiguration {

  private static final Logger LOGGER = LogManager.getLogger(KubeConfiguration.class);

  @Value("${kube.api.base.path}")
  private String kubeApiBasePath;

  @Value("${kube.api.token}")
  private String kubeApiToken;

  @Value("${kube.api.debug}")
  private String kubeApiDebug;

  @Value("${kube.api.type}")
  private String kubeApiType;

  @Value("${kube.api.timeout}")
  private Integer kubeApiTimeOut;

  @Bean
  public ApiClient connectToKube() {
    ApiClient defaultClient = null;
    if ("cluster".equals(kubeApiType)) {
      try {

        defaultClient = Config.fromCluster().setVerifyingSsl(false)
            .setDebugging(kubeApiDebug.isEmpty() ? Boolean.FALSE : Boolean.valueOf(kubeApiDebug));
      } catch (IOException e) {
        LOGGER.error("Exception: ", e);
        throw new KubeRuntimeException("GetApiClient exception.", e);
      }
    } else {
      defaultClient = io.kubernetes.client.Configuration.getDefaultApiClient()
          .setVerifyingSsl(false).setBasePath(kubeApiBasePath)
          .setDebugging(kubeApiDebug.isEmpty() ? Boolean.FALSE : Boolean.valueOf(kubeApiDebug));
    }

    if (!kubeApiToken.isEmpty()) {
      ApiKeyAuth apiKeyAuth = (ApiKeyAuth) defaultClient.getAuthentication("BearerToken");
      apiKeyAuth.setApiKey(kubeApiToken);
      apiKeyAuth.setApiKeyPrefix("Bearer");
    }
    defaultClient.getHttpClient().setReadTimeout(kubeApiTimeOut.longValue(), TimeUnit.SECONDS);
    io.kubernetes.client.Configuration.setDefaultApiClient(defaultClient);

    return defaultClient;
  }
}
