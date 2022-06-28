package io.boomerang.kube.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import io.boomerang.kube.exception.KubeRuntimeException;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

@Component
public class LogKubeServiceImpl implements LogKubeService {

  private static final Logger LOGGER = LogManager.getLogger(LogKubeServiceImpl.class);

  private static final int BYTE_SIZE = 1024;

  @Autowired
  private KubeHelperServiceImpl helperKubeService;
  
  KubernetesClient client = null;

  public LogKubeServiceImpl() {
    this.client = new DefaultKubernetesClient();
  }

  @Override
  public String getPodLog(String workflowId, String workflowActivityId, String taskId, String taskActivityId, Map<String, String> customLabels) {
    Map<String, String> labelSelector = helperKubeService.getTaskLabels(workflowId, workflowActivityId, taskId, taskActivityId, customLabels);

    try {
      List<Pod> pods = client.pods().withLabels(labelSelector).list().getItems();
    
      if (pods != null && !pods.isEmpty()) {
        Pod pod = pods.get(0);
        return client.pods().inNamespace(pod.getMetadata().getNamespace()).withName(pod.getMetadata().getName()).withPrettyOutput().getLog();
      } else {
        throw new KubeRuntimeException("No logs found for Task");
      }
    
    } catch (Exception e) {
      LOGGER.error("getPodLog Exception: ", e);
      throw new KubeRuntimeException("Error getPodLog", e);
    } 
  }

  @Override
  public StreamingResponseBody streamPodLog(HttpServletResponse response, String workflowId,
      String workflowActivityId, String taskId, String taskActivityId,
      Map<String, String> customLabels) {

    LOGGER.info("Stream logs from Kubernetes");

    Map<String, String> labelSelector = helperKubeService.getTaskLabels(workflowId,
        workflowActivityId, taskId, taskActivityId, customLabels);
    StreamingResponseBody responseBody = null;
    List<Pod> pods = client.pods().withLabels(labelSelector).list().getItems();
    if (!pods.isEmpty()) {
      Pod pod = client.pods().withLabels(labelSelector).list().getItems().get(0);

      try {
        InputStream inputStream = client.pods().inNamespace(pod.getMetadata().getNamespace())
            .withName(pod.getMetadata().getName()).watchLog().getOutput();
        responseBody = getPodLog(inputStream, pod.getMetadata().getName());
      } catch (Exception e) {

        LOGGER.error("streamPodLog Exception: ", e);
        throw new KubeRuntimeException("Error streamPodLog", e);

      }
    }
    return responseBody;
  }

  protected StreamingResponseBody getPodLog(InputStream inputStream, String podName) {
    return outputStream -> {
      byte[] data = new byte[BYTE_SIZE];
      int nRead = 0;
      int nReadSum = 0;
      LOGGER.info("Log stream started for pod " + podName + "...");
      try {
        while ((nRead = inputStream.read(data)) > 0) {
          outputStream.write(data, 0, nRead);
          nReadSum += nRead;
        }
      } finally {
        outputStream.flush();
        LOGGER.info("Log stream completed for pod " + podName + ", total bytes streamed=" + nReadSum
            + "...");
        inputStream.close();
        LOGGER.info("Log stream closed for pod " + podName + "...");
      }
    };
  }
}
