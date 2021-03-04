package net.boomerangplatform.kube.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import com.google.common.io.ByteStreams;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.PodLogs;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.util.Watch;
import net.boomerangplatform.kube.exception.KubeRuntimeException;

@Component
public class LogKubeServiceImpl implements LogKubeService {

  private static final Logger LOGGER = LogManager.getLogger(KubeService.class);

  private static final int BYTE_SIZE = 1024;

  @Autowired
  private HelperKubeServiceImpl helperKubeService;

  @Autowired
  private KubeServiceImpl kubeService;

  @Override
  public String getPodLog(String workflowId, String workflowActivityId, String taskId, String taskActivityId) {
    String labelSelector = helperKubeService.getLabelSelector("worker", workflowId, workflowActivityId, taskId, taskActivityId);

    PodLogs logs = new PodLogs();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      List<V1Pod> allPods = kubeService.getPods(labelSelector);

      V1Pod pod;

      if (allPods != null && !allPods.isEmpty()) {
        pod = allPods.get(0);
        InputStream is = logs.streamNamespacedPodLog(pod.getMetadata().getNamespace(),
            pod.getMetadata().getName(), "worker-cntr");

        ByteStreams.copy(is, baos);
      }
    } catch (ApiException | IOException e) {
      LOGGER.error("getPodLog Exception: ", e);
      throw new KubeRuntimeException("Error getPodLog", e);
    }

    return baos.toString(StandardCharsets.UTF_8);
  }

  @Override
  public boolean isKubePodAvailable(String workflowId, String workflowActivityId, String taskId, String taskActivityId) {
    String labelSelector = helperKubeService.getLabelSelector("worker", workflowId, workflowActivityId, taskId, taskActivityId);

    try {
      List<V1Pod> allPods = kubeService.getPods(labelSelector);

      if (allPods == null || allPods.isEmpty() || "succeeded".equalsIgnoreCase(allPods.get(0).getStatus().getPhase())
          || "failed".equalsIgnoreCase(allPods.get(0).getStatus().getPhase())) {
        LOGGER.info("isKubePodAvailable() - Not available");
        return false;
      }
      LOGGER.info("isKubePodAvailable() - Available");
    } catch (ApiException e) {
      LOGGER.error("streamPodLog Exception: ", e);
      throw new KubeRuntimeException("Error streamPodLog", e);
    }
    return true;
  }

  @Override
  public StreamingResponseBody streamPodLog(HttpServletResponse response, String workflowId,
      String workflowActivityId, String taskId, String taskActivityId) {

    LOGGER.info("Stream logs from Kubernetes");

    String labelSelector = helperKubeService.getLabelSelector("worker", workflowId, workflowActivityId, taskId, taskActivityId);
    StreamingResponseBody responseBody = null;
    try {
      Watch<V1Pod> watch = kubeService.createPodWatch(labelSelector, kubeService.getCoreApi());
      V1Pod pod = kubeService.getPod(watch);

      if (pod == null) {
        LOGGER.error("V1Pod is empty...");
      } else {
        if (pod.getStatus() == null) {
          LOGGER.error("Pod Status is empty");
        } else {
          LOGGER.info("Phase: " + pod.getStatus().getPhase());
        }
      }

      PodLogs logs = new PodLogs();
      InputStream inputStream = logs.streamNamespacedPodLog(pod);

      responseBody = getPodLog(inputStream, pod.getMetadata().getName());
    } catch (ApiException | IOException e) {
      // TODO: handle better throwing so that it can be caught in LogService and the default stream
      // returned rather than failure.
      LOGGER.error("streamPodLog Exception: ", e);
      throw new KubeRuntimeException("Error streamPodLog", e);
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
