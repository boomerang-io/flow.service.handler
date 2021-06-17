package net.boomerangplatform.service;

import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import net.boomerangplatform.kube.exception.KubeRuntimeException;
import net.boomerangplatform.kube.service.LogKubeServiceImpl;
import net.boomerangplatform.kube.service.NewHelperKubeServiceImpl;

@Service
public class LogServiceImpl implements LogService {

  private static final Logger LOGGER = LogManager.getLogger(LogServiceImpl.class);

  @Value("${kube.worker.logging.type}")
  protected String loggingType;

  @Autowired
  private MessageSource messageSource;

  @Autowired
  private NewHelperKubeServiceImpl helperKubeService;

  @Autowired
  private LogKubeServiceImpl logKubeService;

  @Value("${kube.worker.logging.host}")
  protected String lokiHost;

  @Value("${kube.worker.logging.port}")
  protected String lokiPort;
  
  @Override
  public String getLogForTask(String workflowId,
      String workflowActivityId, String taskId, String taskActivityId) {
    
    return logKubeService.getPodLog(workflowId, workflowActivityId, taskId, taskActivityId, null);
  }

  @Override
  public StreamingResponseBody streamLogForTask(HttpServletResponse response, String workflowId,
      String workflowActivityId, String taskId, String taskActivityId) {
//    StreamingResponseBody srb = null;
    try {
//      if (logKubeService.isKubePodAvailable(workflowId, workflowActivityId, taskId, taskActivityId)
//          && "default".equals(loggingType)) {
        if ("default".equals(loggingType)) {
        return logKubeService.streamPodLog(response, workflowId, workflowActivityId, taskId, taskActivityId, null);
      } else if ("loki".equals(loggingType)) {
        return streamLogsFromLoki(workflowId, taskId, taskActivityId);
      } else if ("elastic".equals(loggingType)) {
        return getDefaultErrorMessage(getMessageDeprecated());
      } else {
        return getDefaultErrorMessage(getMessageUnableToAccessLogs());
      }
    } catch (KubeRuntimeException e) {
      return getDefaultErrorMessage(getMessageServerError());
    }
  }

  // TODO: reduce complexity, refactor method
  private StreamingResponseBody streamLogsFromLoki(String workflowId,
      String taskId, String taskActivityId) {

    LOGGER.info("Streaming logs from loki for task ("+ taskId + ") and activity (" + taskActivityId + ")");


    return outputStream -> {

      PrintWriter printWriter = new PrintWriter(outputStream);

      final String filter =
          createLokiFilter(workflowId, taskId, taskActivityId);
      LOGGER.info("Loki filter: " + filter);

      final String encodedQuery = URLEncoder.encode(filter, StandardCharsets.UTF_8);
      final Integer limit = 5000; // max chunk size supported by Loki
      final String direction = "forward"; // default backward
      final String lokiEndpoint = "http://" + lokiHost + ":" + lokiPort;
      final String uri = lokiEndpoint + "/loki/api/v1/query_range?&limit=" + Integer.toString(limit)
          + "&direction=" + direction + "&query=" + encodedQuery;
      // If no `end` argument is defined, it will be automatically set to `now()` by server

      LOGGER.info("Loki endpoint: " + uri);

      String start = "&start=0"; // Thursday, January 1, 1970 12:00:00 AM
      Boolean moreLogsAvailable = Boolean.TRUE; // TODO: create a method instead
      CloseableHttpClient httpClient = HttpClients.createDefault();

      try {
        while (moreLogsAvailable.equals(Boolean.TRUE)) {
          HttpGet request = new HttpGet(uri + start);
          JSONObject currentLogBatch;

          CloseableHttpResponse response = httpClient.execute(request);
          try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {

              // TODO check if result is in JSON format
              currentLogBatch = new JSONObject(EntityUtils.toString(entity));

              JSONArray queryResults = currentLogBatch.getJSONObject("data").getJSONArray("result");
              JSONArray logArray;
              String logEntry;

              if (queryResults.length() == 0) {
                printWriter.println(getMessageUnableToAccessLogs());
                printWriter.flush();
                printWriter.close();
                return;
              }


              if (queryResults.length() > 0) {
                logArray = queryResults.getJSONObject(0).optJSONArray("values");

                int index = 1;
                if (start.equals("&start=0"))
                  index = 0; // no prior log line to overlap

                for (; index < logArray.length(); index++) {// print line by line
                  logEntry = logArray.getJSONArray(index).get(1).toString();
                  printWriter.print(logEntry); // TODO: can I generate multiline payloads?
                }

                if (logArray.length() < limit) { // check if the current iteration is the last one
                  moreLogsAvailable = Boolean.FALSE;
                } else {
                  JSONArray lastEntry = logArray.getJSONArray(logArray.length() - 1);
                  start = "&start=" + lastEntry.get(0).toString();
                }
              } else {
                moreLogsAvailable = Boolean.FALSE;
              }
            } else {
              printWriter.println(getMessageUnableToAccessLogs());
              printWriter.flush();
              printWriter.close();
              return;
            }
          } finally {
            response.close();
          }
        }
      } finally {
        httpClient.close();
      }
      printWriter.flush();
      printWriter.close();
    };
  }

  private String createLokiFilter(String workflowId, String taskId, String taskActivityId) {
    return "{bmrg_task_activity=\"" + taskActivityId + "\",bmrg_workflow=\"" + workflowId
        + "\",bmrg_task=\"" + taskId + "\",bmrg_container=\"step-task\"}";
  }

  protected StreamingResponseBody getDefaultErrorMessage(String message) {
    LOGGER.info("Returning back default message.");

    return outputStream -> {
      outputStream.write(message.getBytes(StandardCharsets.UTF_8));
      outputStream.flush();
      outputStream.close();
    };
  }

  private String getMessageDeprecated() {
    MessageSourceAccessor accessor = new MessageSourceAccessor(messageSource);
    return accessor.getMessage("DEPRECATED_LOGS_OPTION");
  }

  private String getMessageUnableToAccessLogs() {
    MessageSourceAccessor accessor = new MessageSourceAccessor(messageSource);
    return accessor.getMessage("UNABLE_RETRIEVE_LOGS");
  }

  private String getMessageServerError() {
    MessageSourceAccessor accessor = new MessageSourceAccessor(messageSource);
    return accessor.getMessage("INTERNAL_SERVER_ERROR_LOGS");
  }
}
