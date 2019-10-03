package net.boomerangplatform.elastic;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MockElasticConfiguration {

  @Bean("elasticRestClient")
  @Primary
  public RestHighLevelClient elasticRestClient() {
    HttpHost[] httpHosts = new HttpHost[1];
    httpHosts[0] = new HttpHost("localhost", 9200, "http");
    
    RestClientBuilder builder = RestClient.builder(httpHosts);
    return new RestHighLevelClient(builder);
  }
}
