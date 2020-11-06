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
    try {
      if ("cluster".equals(kubeApiType)) {
        defaultClient =
            Config.fromCluster()
                .setVerifyingSsl(false);

      } else if ("custom".equals(kubeApiType)) {
        defaultClient =
            io.kubernetes.client.Configuration.getDefaultApiClient()
                .setVerifyingSsl(false)
                .setBasePath(kubeApiBasePath);
        ApiKeyAuth apiKeyAuth = (ApiKeyAuth) defaultClient.getAuthentication("BearerToken");
        apiKeyAuth.setApiKey(kubeApiToken);
        apiKeyAuth.setApiKeyPrefix("Bearer");
      } else {
        defaultClient =
            Config.defaultClient()
                .setVerifyingSsl(false);
      }
    } catch (IOException e) {
      LOGGER.error("Exception: ", e);
      throw new KubeRuntimeException("GetApiClient exception.", e);
    }

    defaultClient.getHttpClient().setReadTimeout(kubeApiTimeOut.longValue(), TimeUnit.SECONDS);
    defaultClient.setDebugging(
        kubeApiDebug.isEmpty() ? Boolean.FALSE : Boolean.valueOf(kubeApiDebug));
    io.kubernetes.client.Configuration.setDefaultApiClient(defaultClient);

    LOGGER.info("Connecting to: " + defaultClient.getBasePath());
    return defaultClient;
  }
}
