package net.boomerangplatform.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.kubernetes.client.models.V1JobList;
import io.kubernetes.client.models.V1NamespaceList;
import net.boomerangplatform.kube.exception.KubeRuntimeException;
import net.boomerangplatform.kube.service.KubeService;

@RestController
@RequestMapping("/controller")
public class ControllerController {

  private static final Logger LOGGER = LogManager.getLogger(ControllerController.class);

  @Autowired
  private KubeService kubeService;

  @GetMapping(value = "/namespace")
  public V1NamespaceList getAllNamespaces() {
    return kubeService.getAllNamespaces();
  }

  @GetMapping(value = "/namespace/watch")
  public void watchnamespace() {
    try {
      kubeService.watchNamespace();
    } catch (KubeRuntimeException e) {
      LOGGER.error("Exception: ", e);
    }
  }

  @GetMapping(value = "/jobs")
  public V1JobList getAllJobs() {
    return kubeService.getAllJobs();
  }
}
