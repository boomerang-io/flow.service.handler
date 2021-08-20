package net.boomerangplatform.kube.service;

import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public interface LogKubeService {

  StreamingResponseBody streamPodLog(
      HttpServletResponse response,
      String workflowId,
      String workflowActivityId,
      String taskId,
      String taskActivityId, Map<String, String> customLabels) throws IOException;

  String getPodLog(String workflowId, String workflowActivityId, String taskId,
      String taskActivityId, Map<String, String> customLabels);

}
