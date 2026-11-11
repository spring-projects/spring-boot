/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.quartz;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.springframework.scheduling.quartz.QuartzJobBean;

public class MySampleJob extends QuartzJobBean {

	// @fold:on // fields ...
	private MyService myService;

	private String name;

	// @fold:off

	// Inject "MyService" bean
	public void setMyService(MyService myService) {
		this.myService = myService;
	}

	// Inject the "name" job data property
	public void setName(String name) {
		this.name = name;
	}

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		this.myService.someMethod(context.getFireTime(), this.name);
	}

}
