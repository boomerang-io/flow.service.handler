package net.boomerangplatform.elastic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpHost;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class ElasticConfiguration {

	private static final Logger LOGGER = LogManager.getLogger(ElasticConfiguration.class);

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

	@Bean
	public RestHighLevelClient elasticRestClient() {

		if (streamLogsFromElastic()) {
			LOGGER.info("Configuring log streaming for elastic.");

			try {
				KeyStore truststore = KeyStore.getInstance("jks");
				try (InputStream is = Files.newInputStream(Paths.get(keystorePath))) {
					truststore.load(is, keystorePassword.toCharArray());
				}
				SSLContextBuilder sslBuilder = SSLContexts.custom().loadKeyMaterial(truststore,
						keystorePassword.toCharArray());
				final SSLContext sslContext = sslBuilder.build();
				RestClientBuilder builder = RestClient.builder(new HttpHost(elasticHost, elasticPort, "https"))
						.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setSSLContext(sslContext));
				return new RestHighLevelClient(builder);
			} catch (IOException | GeneralSecurityException e) {
				LOGGER.error("Error: ", e);
				e.printStackTrace();
			}
		}
		else {
			LOGGER.info("Elastic search has been disabled");
		}
		
		LOGGER.info("Returning empty elastic configuration.");
		return null;
	}

	private boolean streamLogsFromElastic() {
		return "elastic".equals(loggingType);
	}
}
