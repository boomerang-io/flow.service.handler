package io.boomerang.tests.service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import io.boomerang.kube.service.KubeServiceImpl;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

@SpringBootTest
@Component
public class MockKubeServiceImpl extends KubeServiceImpl {
  
  protected MockKubeServiceImpl() {
    Config config = new ConfigBuilder().withNamespace("default").build();
    super.client = new DefaultKubernetesClient(config);
  }

}
