/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.apache.commons.logging.impl.NoOpLog;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationErrorEvent;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AutoConfigurationReportLoggingInitializer}.
 * 
 * @author Phillip Webb
 */
public class AutoConfigurationReportLoggingInitializerTests {

	private static ThreadLocal<Log> logThreadLocal = new ThreadLocal<Log>();

	private Log log;

	private AutoConfigurationReportLoggingInitializer initializer;

	protected List<String> debugLog = new ArrayList<String>();

	protected List<String> infoLog = new ArrayList<String>();

	@Before
	public void setup() {

		this.log = mock(Log.class);
		logThreadLocal.set(this.log);

		given(this.log.isDebugEnabled()).willReturn(true);
		willAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return AutoConfigurationReportLoggingInitializerTests.this.debugLog
						.add(String.valueOf(invocation.getArguments()[0]));
			}
		}).given(this.log).debug(anyObject());

		given(this.log.isInfoEnabled()).willReturn(true);
		willAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return AutoConfigurationReportLoggingInitializerTests.this.infoLog
						.add(String.valueOf(invocation.getArguments()[0]));
			}
		}).given(this.log).info(anyObject());

		LogFactory.releaseAll();
		System.setProperty(LogFactory.FACTORY_PROPERTY, MockLogFactory.class.getName());
		this.initializer = new AutoConfigurationReportLoggingInitializer();
	}

	@After
	public void cleanup() {
		System.clearProperty(LogFactory.FACTORY_PROPERTIES);
		LogFactory.releaseAll();
	}

	@Test
	public void logsDebugOnContextRefresh() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		this.initializer.initialize(context);
		context.register(Config.class);
		context.refresh();
		this.initializer.onApplicationEvent(new ContextRefreshedEvent(context));
		assertThat(this.debugLog.size(), not(equalTo(0)));
	}

	@Test
	public void logsInfoAndDebugOnError() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		this.initializer.initialize(context);
		context.register(ErrorConfig.class);
		try {
			context.refresh();
			fail("Did not error");
		}
		catch (Exception ex) {
			this.initializer.onApplicationEvent(new SpringApplicationErrorEvent(
					new SpringApplication(), context, new String[] {}, ex));
		}

		assertThat(this.debugLog.size(), not(equalTo(0)));
		assertThat(this.infoLog.size(), not(equalTo(0)));
	}

	@Test
	public void logsOutput() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		this.initializer.initialize(context);
		context.register(Config.class);
		context.refresh();
		this.initializer.onApplicationEvent(new ContextRefreshedEvent(context));
		for (String message : this.debugLog) {
			System.out.println(message);
		}
		// Just basic sanity check, test is for visual inspection
		String l = this.debugLog.get(0);
		assertThat(l, containsString("not a web application (OnWebApplicationCondition)"));
	}

	@Test
	public void canBeUsedInApplicationContext() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(Config.class);
		new AutoConfigurationReportLoggingInitializer().initialize(context);
		context.refresh();
		assertNotNull(context.getBean(AutoConfigurationReport.class));
	}

	@Test
	public void canBeUsedInNonGenericApplicationContext() throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(Config.class);
		new AutoConfigurationReportLoggingInitializer().initialize(context);
		context.refresh();
		assertNotNull(context.getBean(AutoConfigurationReport.class));
	}

	@Test
	public void noErrorIfNotInitialized() throws Exception {
		this.initializer.onApplicationEvent(new SpringApplicationErrorEvent(
				new SpringApplication(), null, new String[0], new RuntimeException(
						"Planned")));
		assertThat(this.infoLog.get(0),
				containsString("Unable to provide auto-configuration report"));
	}

	public static class MockLogFactory extends LogFactoryImpl {
		@Override
		public Log getInstance(String name) throws LogConfigurationException {
			if (AutoConfigurationReportLoggingInitializer.class.getName().equals(name)) {
				return logThreadLocal.get();
			}
			return new NoOpLog();
		}
	}

	@Configuration
	@Import({ WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class })
	static class Config {

	}

	@Configuration
	@Import(WebMvcAutoConfiguration.class)
	static class ErrorConfig {
		@Bean
		public String iBreak() {
			throw new RuntimeException();
		}
	}

}
