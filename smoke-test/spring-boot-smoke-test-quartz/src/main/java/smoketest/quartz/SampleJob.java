/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.quartz;

import org.jspecify.annotations.Nullable;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.springframework.scheduling.quartz.QuartzJobBean;

public class SampleJob extends QuartzJobBean {

	private @Nullable String name;

	// Invoked if a Job data map entry with that name
	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		System.out.println(String.format("Hello %s!", this.name));
	}

}
