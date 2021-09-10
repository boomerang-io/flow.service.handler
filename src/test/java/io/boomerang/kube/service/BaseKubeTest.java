package io.boomerang.kube.service;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public abstract class BaseKubeTest {

  @Bean
  @Primary
  public RestHighLevelClient elasticRestClient() {

    HttpHost[] httpHosts = new HttpHost[1];
    httpHosts[0] = new HttpHost("localhost", 9200, "http");

    RestClientBuilder builder = RestClient.builder(httpHosts);
    return new RestHighLevelClient(builder.build());
  }

}
