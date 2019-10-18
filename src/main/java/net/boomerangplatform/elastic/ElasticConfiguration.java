package net.boomerangplatform.elastic;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpHost;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.boomerangplatform.kube.service.AbstractKubeService;

import org.elasticsearch.client.RestHighLevelClient;

@Configuration
public class ElasticConfiguration {

	@Value("${kube.worker.logging.keystore.name}")
	protected String keystorePath;

	@Value("${kube.worker.logging.keystore.password}")
	protected String keystorePassword;

	@Value("${kube.worker.logging.host}")
	protected String elasticHost;

	@Value("${kube.worker.logging.port}")
	protected Integer elasticPort;

	@Value("${kube.worker.logging.type}")
	protected String loggingType;

    private static final Logger LOGGER = LogManager.getLogger(ElasticConfiguration.class);
	  
	@Bean
	public RestHighLevelClient elasticRestClient() {
		if (streamLogsFromElastic()) {
			LOGGER.info("Configuring elastic client");
			try {
				KeyStore truststore = KeyStore.getInstance("jks");
				try (InputStream is = Files.newInputStream(Paths.get(keystorePath))) {
					truststore.load(is, keystorePassword.toCharArray());
				}
				SSLContextBuilder sslBuilder = SSLContexts.custom().loadKeyMaterial(truststore,
						keystorePassword.toCharArray());
				final SSLContext sslContext = sslBuilder.build();
				RestClientBuilder builder = RestClient.builder(new HttpHost(elasticHost, elasticPort, "https"))
						.setHttpClientConfigCallback(new HttpClientConfigCallback() {
							@Override
							public HttpAsyncClientBuilder customizeHttpClient(
									HttpAsyncClientBuilder httpClientBuilder) {
								return httpClientBuilder.setSSLContext(sslContext);
							}
						});
				return new RestHighLevelClient(builder.build());
			} catch (Exception e) {
				LOGGER.error("Error connecting to elsatic.");
				LOGGER.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return null;
	}

	private boolean streamLogsFromElastic() {
		return "elastic".equals(loggingType);
	}
}