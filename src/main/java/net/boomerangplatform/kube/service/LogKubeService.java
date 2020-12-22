package net.boomerangplatform.kube.service;

import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public interface LogKubeService {

  String getPodLog(
      String workflowId, String workflowActivityId, String taskId, String taskActivityId);

  StreamingResponseBody streamPodLog(
      HttpServletResponse response,
      String workflowId,
      String workflowActivityId,
      String taskId,
      String taskActivityId);

}
