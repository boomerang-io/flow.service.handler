package net.boomerangplatform.kube.service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.reflect.TypeToken;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.BatchV1Api;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1JobList;
import io.kubernetes.client.models.V1Namespace;
import io.kubernetes.client.models.V1NamespaceList;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watch;

@Service
public class KubeServiceImpl implements KubeService {
	
	@Value("${kube.image}")
	private String kubeImage;

	@Value("${kube.namespace}")
	protected String kubeNamespace;
	
	@Value("${kube.api.pretty}")
	private String kubeApiPretty;

	@Value("${kube.api.includeunitialized}")
	protected Boolean kubeApiIncludeuninitialized;
	
	@Override
	public V1NamespaceList getAllNamespaces() {
		V1NamespaceList list = new V1NamespaceList();
		try {
			CoreV1Api api = new CoreV1Api();
			list = api.listNamespace(null, null, null, null, null, null, null, null, null);
		} catch (ApiException e) {
			e.printStackTrace();
		}
		return list;
	}

	@Override
	public V1JobList getAllJobs() {
		V1JobList list = new V1JobList();

		String _continue = "";
		String fieldSelector = "";
		String labelSelector = "";
		Integer limit = 25;
		String resourceVersion = "";
		Integer timeoutSeconds = 60;
		Boolean watch = false;

		try {
			BatchV1Api api = new BatchV1Api();
			list = api.listNamespacedJob(kubeNamespace, kubeApiIncludeuninitialized, kubeApiPretty, _continue, fieldSelector,
					labelSelector, limit, resourceVersion, timeoutSeconds, watch);

		} catch (ApiException e) {
			e.printStackTrace();
		}
		return list;
	}

	@Override
	public void watchNamespace() throws ApiException, IOException {
		
		ApiClient client = Config.defaultClient();
	    client.getHttpClient().setReadTimeout(60, TimeUnit.SECONDS);
	    Configuration.setDefaultApiClient(client);

		CoreV1Api api = new CoreV1Api();

		Watch<V1Namespace> watch = Watch.createWatch(client,
				api.listNamespaceCall(null, null, null, null, null, 120, null, null, Boolean.TRUE, null, null),
				new TypeToken<Watch.Response<V1Namespace>>() {
				}.getType());

		try {
			for (Watch.Response<V1Namespace> item : watch) {
				System.out.printf("%s : %s%n", item.type, item.object.getMetadata().getName());
			}
		} finally {
			watch.close();
		}
	}
}
