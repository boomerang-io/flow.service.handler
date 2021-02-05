package net.boomerangplatform.kube.service;

import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public interface LogKubeService {

  String getPodLog(
      String workflowId, String activityId, String taskId);

  StreamingResponseBody streamPodLog(
      HttpServletResponse response,
      String workflowId,
      String activityId,
      String taskId);

}
