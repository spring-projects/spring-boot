/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.servlet.autoconfigure.HttpEncodingAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Tests for the auto-configuration imported by {@link WebMvcTest @WebMvcTest}.
 *
 * @author Andy Wilkinson
 * @author Levi Puot Paul
 * @author Madhura Bhave
 */
@WebMvcTest
class WebMvcTestAutoConfigurationIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void taskExecutionAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(TaskExecutionAutoConfiguration.class));
	}

	@Test
	void asyncTaskExecutorWithApplicationTaskExecutor() {
		assertThat(this.applicationContext.getBeansOfType(AsyncTaskExecutor.class)).hasSize(1);
		assertThat(this.applicationContext.getBean(RequestMappingHandlerAdapter.class)).extracting("taskExecutor")
			.isSameAs(this.applicationContext.getBean("applicationTaskExecutor"));
	}

	@Test
	void httpEncodingAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(HttpEncodingAutoConfiguration.class));
	}

}
