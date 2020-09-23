package net.boomerangplatform.service;

import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpEntity;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import net.boomerangplatform.kube.exception.KubeRuntimeException;
import net.boomerangplatform.kube.service.AbstractKubeServiceImpl;

@Service
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
  
  
  @Value("${kube.worker.logging.loki.host}")
  protected String lokiHost;
  
  @Value("${kube.worker.logging.loki.port}")
  protected String lokiPort;
 

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
        return streamLogsFromElastic(taskActivityId);
      } else if (streamLogsFromLoki()) {
        return streamLogsFromLoki( workflowId, workflowActivityId, taskId, taskActivityId);
      } else {
        return getDefaultErrorMessage(getMessageUnableToAccessLogs());
      }
    } catch (KubeRuntimeException e) {
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

  // TODO: reduce complexity, refactor method
  private StreamingResponseBody streamLogsFromLoki(String workflowId,
      String workflowActivityId, String taskId, String taskActivityId) {

    LOGGER.info(
        "Streaming logs from loki: " + taskActivityId);


    return outputStream -> {
  
      PrintWriter printWriter = new PrintWriter(outputStream);
      
      final String filter =  createLokoFilter(workflowId, workflowActivityId, taskId, taskActivityId);
      LOGGER.info(
          "Loki filter: " + filter);
      
      final String encodedQuery = URLEncoder.encode(filter, StandardCharsets.UTF_8);
      final Integer limit = 5000; // max chunk size supported by Loki
      final String direction = "forward";  //default backward
      final String lokiEndpoint = "http://" + lokiHost + ":" + lokiPort;
      final String uri =  lokiEndpoint + 
          "/loki/api/v1/query_range?&limit=" + Integer.toString(limit) +
          "&direction=" + direction + 
          "&query=" + encodedQuery;
          // If no `end` argument is defined, it will be automatically set to `now()` by server 

      LOGGER.info(
            "Loki endpoint: " + uri);
            
      String start = "&start=0"; // Thursday, January 1, 1970 12:00:00 AM
      Boolean moreLogsAvailable = Boolean.TRUE; // TODO: create a method instead
      CloseableHttpClient httpClient = HttpClients.createDefault();
      
      try {
        while(moreLogsAvailable.equals(Boolean.TRUE)){
          HttpGet request = new HttpGet(uri + start);
          JSONObject currentLogBatch;
            
          CloseableHttpResponse response = httpClient.execute(request);
          try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {

              //TODO check if result is in JSON format
              currentLogBatch = new JSONObject(EntityUtils.toString(entity));
                  
              JSONArray queryResults = currentLogBatch.getJSONObject("data").getJSONArray("result");
              JSONArray logArray;
              String logEntry;
                
              if(queryResults.length() > 0){
                logArray = queryResults.getJSONObject(0).optJSONArray("values");

                int index = 1;
                if(start.equals("&start=0")) index = 0; //no prior log line to overlap

                for(; index < logArray.length(); index++){//print line by line
                  logEntry = logArray.getJSONArray(index).get(0).toString() + " " 
                            + logArray.getJSONArray(index).get(1).toString();
                  printWriter.println(logEntry); //TODO: can I generate multiline payloads?
                }

                if(logArray.length() < limit){ //check if the current iteration is the last one
                  moreLogsAvailable = Boolean.FALSE;
                }else{
                  JSONArray lastEntry = logArray.getJSONArray(logArray.length() - 1);
                  start = "&start=" + lastEntry.get(0).toString();                          
                }
              }else{
                moreLogsAvailable = Boolean.FALSE;
              }
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

  private String createLokoFilter(String workflowId, String workflowActivityId, String taskId,
      String taskActivityId) {
    return "{bmrg_activity=\""
        + workflowActivityId
        + "\",bmrg_workflow=\"" + 
        workflowId + "\",bmrg_task=\""
            + taskId
            + "\"}";
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
