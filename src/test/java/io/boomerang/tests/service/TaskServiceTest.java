package io.boomerang.tests.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;

@SpringBootTest
@EnableKubernetesMockClient(crud = true)
public class TaskServiceTest {

  KubernetesClient client;

  @Autowired
  private MockKubeServiceImpl mockKubeService;

  @Test
  public void testCreateTaskConfigMapWithEmptyParams() {
    
    System.out.println("testCreateTaskConfigMapWithEmptyParams() - namespace: " + client.getConfiguration().getNamespace());

    Map<String, String> params = new HashMap<>(); 
    mockKubeService.createTaskConfigMap("test-cm-workflow", "20210926", "202109260627", "Test Task", "1234", "2021092606271234", null, params);
    
    ConfigMapList configMapList = client.configMaps().inAnyNamespace().list();
    System.out.println("testCreateTaskConfigMapWithEmptyParams() - " + configMapList.toString());
    assertNotNull(configMapList);
    assertEquals(1, configMapList.getItems().size());
  }

}
