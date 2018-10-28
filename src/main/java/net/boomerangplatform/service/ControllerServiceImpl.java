package net.boomerangplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ControllerServiceImpl implements ControllerService {
	
	@Autowired
    private KubeService kubeService;

	@Override
	public String executeTask(String labelName) {
		
		try {
			kubeService.createJob(labelName);
			return kubeService.watchJob(labelName);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return e.toString();
		}
	}
}
