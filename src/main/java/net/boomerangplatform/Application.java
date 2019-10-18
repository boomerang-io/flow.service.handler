package net.boomerangplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.rest.RestClientAutoConfiguration;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {ElasticsearchAutoConfiguration.class, RestClientAutoConfiguration.class})
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
