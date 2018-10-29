package net.boomerangplatform.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.kubernetes.client.ApiException;
import io.kubernetes.client.models.V1JobList;
import io.kubernetes.client.models.V1NamespaceList;
import net.boomerangplatform.service.ControllerService;
import net.boomerangplatform.service.KubeService;

@RestController

@RequestMapping("/controller")
public class ControllerController {

    @Autowired
    private KubeService kubeService;
    
    @Autowired
    private ControllerService controllerService;

    @RequestMapping(value = "/namespace", method = RequestMethod.GET)
    public V1NamespaceList getAllNamespaces() {
        return kubeService.getAllNamespaces();
    }
    
    @RequestMapping(value = "/namespace/watch", method = RequestMethod.GET)
    public void watchnamespace() {
        try {
			kubeService.watchNamespace();
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    @RequestMapping(value = "/jobs", method = RequestMethod.GET)
    public V1JobList getAllJobs() {
        return kubeService.getAllJobs();
    }
}