package com.renjie120.hello;

import org.activiti.engine.RepositoryService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class TestBpmn {
	public static void main(String[] args) {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				 "spring.cfg.xml");
		RepositoryService repositoryService = (RepositoryService) applicationContext
				.getBean("repositoryService");
		// String deploymentId = repositoryService.createDeployment()
		// .addClasspathResource("hello.bpmn20.xml").deploy().getId();

		UserBean userBean = (UserBean) applicationContext.getBean("userBean");
		userBean.hello();
	}
}
