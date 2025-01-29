// package io.boomerang.kube.service;

// import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
// import static com.github.tomakehurst.wiremock.client.WireMock.get;
// import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
// import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
// import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
// import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
// import static org.junit.Assert.assertEquals;
// import static org.junit.Assert.assertNotNull;
// import static org.junit.Assert.assertNull;
// import java.io.IOException;
// import org.junit.Before;
// import org.junit.Rule;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.http.MediaType;
// import org.springframework.test.context.ActiveProfiles;
// import org.springframework.test.context.ContextConfiguration;
// import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
// import com.github.tomakehurst.wiremock.junit.WireMockRule;
// import io.boomerang.Application;
// import io.boomerang.kube.exception.KubeRuntimeException;
// import io.boomerang.kube.service.KubeService;
// import io.boomerang.kube.service.KubeServiceImpl;
// import io.kubernetes.client.ApiClient;
// import io.kubernetes.client.Configuration;
// import io.kubernetes.client.models.V1JobList;
// import io.kubernetes.client.models.V1NamespaceList;
// import io.kubernetes.client.util.ClientBuilder;

// @RunWith(SpringJUnit4ClassRunner.class)
// @SpringBootTest
// @ContextConfiguration(classes = {Application.class, BaseKubeTest.class})
// @ActiveProfiles("local")
// public class KubeServiceTest {

//   @Autowired
//   private KubeService kubeService;

//   private ApiClient client;

//   private static final int PORT = 8089;
//   @Rule
//   public WireMockRule wireMockRule = new WireMockRule(PORT);

//   @Before
//   public void setup() throws IOException {
//     client = new ClientBuilder().setBasePath("http://localhost:" + PORT).build();
//     Configuration.setDefaultApiClient(client);
//     ((KubeServiceImpl) kubeService).setApiClient(client);
//   }

//   @Test
//   public void testGetAllNamespaces() {
//     stubFor(get(urlPathEqualTo("/api/v1/namespaces")).willReturn(
//         okForContentType(MediaType.APPLICATION_JSON_VALUE, "{\"apiVersion\": \"1.0\"}")));

//     V1NamespaceList namespaceList = kubeService.getAllNamespaces();
//     assertNotNull(namespaceList);
//     assertEquals("1.0", namespaceList.getApiVersion());
//   }

//   @Test
//   public void testGetAllNamespacesWithException() {
//     stubFor(get(urlPathEqualTo("/api/v1/namespaces")).willReturn(aResponse().withStatus(404)
//         .withHeader("Content-Type", "text/plain").withBody("Not Found")));

//     V1NamespaceList namespaceList = kubeService.getAllNamespaces();
//     assertNotNull(namespaceList);
//     assertNull(namespaceList.getApiVersion());
//   }

//   @Test
//   public void testGetAllJobs() {
//     stubFor(get(urlPathMatching("/apis/batch/v1/namespaces/default/jobs")).willReturn(
//         okForContentType(MediaType.APPLICATION_JSON_VALUE, "{\"apiVersion\": \"1.0\"}")));

//     V1JobList jobList = kubeService.getAllJobs();
//     assertNotNull(jobList);
//     assertEquals("1.0", jobList.getApiVersion());
//   }

//   @Test
//   public void testGetAllJobsWithException() {
//     stubFor(get(urlPathMatching("/apis/batch/v1/namespaces/default/jobs")).willReturn(aResponse()
//         .withStatus(404).withHeader("Content-Type", "text/plain").withBody("Not Found")));

//     V1JobList jobList = kubeService.getAllJobs();
//     assertNotNull(jobList);
//     assertNull(jobList.getApiVersion());
//   }

//   @Test
//   public void testWatchNamespace() {
//     stubFor(get(urlPathMatching("/api/v1/namespaces"))
//         .willReturn(okForContentType("application/json;stream=watch",
//             "{\"apiVersion\": \"1.0\", \"metadata\": {\"name\": \"testMetadata\"}}")));

//     kubeService.watchNamespace();
//   }

//   @Test(expected = KubeRuntimeException.class)
//   public void testWatchNamespaceWithException() {
//     stubFor(get(urlPathMatching("/api/v1/namespaces")).willReturn(aResponse().withStatus(404)
//         .withHeader("Content-Type", "text/plain").withBody("Not Found")));

//     kubeService.watchNamespace();
//   }

// }
