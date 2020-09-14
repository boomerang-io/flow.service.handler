package net.boomerangplatform.service;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchPhraseQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import net.boomerangplatform.error.BoomerangException;
import net.boomerangplatform.kube.exception.KubeRuntimeException;
import net.boomerangplatform.kube.service.AbstractKubeServiceImpl;

public class LogServiceImpl implements LogService {

  private static final Logger LOGGER = LogManager.getLogger(LogServiceImpl.class);

  @Value("${kube.worker.logging.type}")
  protected String loggingType;

  @Autowired
  private MessageSource messageSource;

  @Autowired(required = false)
  private RestHighLevelClient elasticRestClient;

  @Autowired
  private AbstractKubeServiceImpl kubeService;

  // TODO: Confirm if this is even used anywhere...
  // @Override
  // public Response getLogForTask(String workflowId, String workflowActivityId, String taskId,
  // String taskActivityId) {
  // Response response = new Response("0", "");
  // try {
  // if (kubeService.isKubePodAvailable(workflowId, workflowActivityId, taskId) &&
  // !streamLogsFromElastic() && !streamLogsFromLoki()) {
  // response.setMessage(kubeService.getPodLog(workflowId, workflowActivityId, taskId,
  // taskActivityId));
  //
  // } else if (streamLogsFromElastic()) {
  // return streamLogsFromElastic(workflowActivityId);
  // } else if (streamLogsFromLoki()) {
  // //TODO Loki Implementation
  // } else {
  // //TODO throw error that there is no log
  // }
  // } catch (KubeRuntimeException e) {
  // throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
  // }
  // return response;
  // }

  @Override
  public StreamingResponseBody streamLogForTask(HttpServletResponse response, String workflowId,
      String workflowActivityId, String taskId, String taskActivityId) {
    StreamingResponseBody srb = null;
    try {
      if (kubeService.isKubePodAvailable(workflowId, workflowActivityId, taskId)
          && !streamLogsFromElastic() && !streamLogsFromLoki()) {
        srb = kubeService.streamPodLog(response, workflowId, workflowActivityId, taskId,
            taskActivityId);
      } else if (streamLogsFromElastic()) {
        // TODO: double check WorkflowActivityId for CICD and TaskActivityId for Flow otherwise this
        // wont return flow
        return streamLogsFromElastic(workflowActivityId);
      } else if (streamLogsFromLoki()) {
        // TODO Loki Implementation
      } else {
        return getDefaultErrorMessage(getMessageUnableToAccessLogs());
      }
    } catch (KubeRuntimeException e) {
      // throw new BoomerangException(e, 1, e.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
      // TODO: do we need to just return the defaultErrorMessage stream?
      return getDefaultErrorMessage(getMessageServerError());
    }
    return srb;
  }

  protected boolean streamLogsFromElastic() {
    return "elastic".equals(loggingType);
  }

  protected boolean streamLogsFromLoki() {
    return "loki".equals(loggingType);
  }

  private StreamingResponseBody streamLogsFromElastic(String activityId) {
    LOGGER.info(
        "Streaming logs from elastic: " + kubeService.getJobPrefix() + "-" + activityId + "-*");

    LOGGER.info("kubernetes.pod=", kubeService.getJobPrefix() + "-" + activityId + "-*");
    return outputStream -> {


      PrintWriter printWriter = new PrintWriter(outputStream);

      final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));

      SearchRequest searchRequest = new SearchRequest("logstash-*");

      searchRequest.scroll(scroll);
      SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
      searchSourceBuilder.from(0);
      searchSourceBuilder.size(1000);
      searchSourceBuilder.sort("offset");


      MatchPhraseQueryBuilder podName = QueryBuilders.matchPhraseQuery("kubernetes.pod",
          kubeService.getJobPrefix() + "-" + activityId + "-*");

      MatchPhraseQueryBuilder containerName =
          QueryBuilders.matchPhraseQuery("kubernetes.container_name", "worker-cntr");
      BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery().must(podName).must(containerName);

      searchSourceBuilder.query(queryBuilder);
      searchRequest.source(searchSourceBuilder);

      SearchResponse searchResponse = elasticRestClient.search(searchRequest);
      SearchHit[] searchHits = searchResponse.getHits().getHits();
      LOGGER.info("Search returned back: " + searchHits.length);

      if (searchHits.length == 0) {
        printWriter.println(getMessageUnableToAccessLogs());
        printWriter.flush();
        printWriter.close();
        return;
      }

      for (SearchHit hits : searchHits) {
        String logMessage = (String) hits.getSourceAsMap().get("log");
        printWriter.println(logMessage);
      }

      String scrollId = searchResponse.getScrollId();
      while (searchHits != null && searchHits.length > 0) {
        SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
        scrollRequest.scroll(scroll);
        searchResponse = elasticRestClient.searchScroll(scrollRequest);
        scrollId = searchResponse.getScrollId();
        searchHits = searchResponse.getHits().getHits();
        LOGGER.info("Search returned back: " + searchHits.length);
        for (SearchHit hits : searchHits) {
          String logMessage = (String) hits.getSourceAsMap().get("log");
          printWriter.println(logMessage);
        }
      }

      ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
      clearScrollRequest.addScrollId(scrollId);
      elasticRestClient.clearScroll(clearScrollRequest);

      printWriter.flush();
      printWriter.close();
    };
  }

  protected StreamingResponseBody getDefaultErrorMessage(String message) {
    LOGGER.info("Returning back default message.");

    return outputStream -> {
      outputStream.write(message.getBytes(StandardCharsets.UTF_8));
      outputStream.flush();
      outputStream.close();
    };
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
