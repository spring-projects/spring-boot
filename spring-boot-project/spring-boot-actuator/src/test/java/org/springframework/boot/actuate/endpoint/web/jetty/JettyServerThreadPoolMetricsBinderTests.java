/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.boot.actuate.endpoint.web.jetty;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.junit.Test;

import org.springframework.boot.actuate.metrics.web.jetty.JettyServerThreadPoolMetricsBinder;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.web.embedded.jetty.JettyWebServer;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JettyServerThreadPoolMetricsBinder}.
 *
 * @author Oleksii Bondar
 */
public class JettyServerThreadPoolMetricsBinderTests {

	private static final String JETTY_THREADS_CURRENT = "jetty.threads.current";

	@Test
	public void reportMetrics() {
		ApplicationStartedEvent event = mock(ApplicationStartedEvent.class);
		AnnotationConfigServletWebServerApplicationContext context = mock(
				AnnotationConfigServletWebServerApplicationContext.class);
		given(event.getApplicationContext()).willReturn(context);
		JettyWebServer webServer = mock(JettyWebServer.class);
		given(context.getWebServer()).willReturn(webServer);
		Server server = mock(Server.class);
		given(webServer.getServer()).willReturn(server);
		ThreadPool threadPool = mock(ThreadPool.class);
		given(server.getThreadPool()).willReturn(threadPool);

		MeterRegistry meterRegistry = bindToMeterRegistry(event);

		meterRegistry.get(JETTY_THREADS_CURRENT).gauge();
	}

	@Test(expected = MeterNotFoundException.class)
	public void doNotReportMetricsForNonWebServerApplicationContext() {
		ApplicationStartedEvent event = mock(ApplicationStartedEvent.class);
		GenericApplicationContext context = mock(GenericApplicationContext.class);
		given(event.getApplicationContext()).willReturn(context);

		MeterRegistry meterRegistry = bindToMeterRegistry(event);

		meterRegistry.get(JETTY_THREADS_CURRENT).gauge();
	}

	@Test(expected = MeterNotFoundException.class)
	public void doNotReportMetricsForNonJettyWebServer() {
		ApplicationStartedEvent event = mock(ApplicationStartedEvent.class);
		AnnotationConfigServletWebServerApplicationContext context = mock(
				AnnotationConfigServletWebServerApplicationContext.class);
		given(event.getApplicationContext()).willReturn(context);
		TomcatWebServer webServer = mock(TomcatWebServer.class);
		given(context.getWebServer()).willReturn(webServer);

		MeterRegistry meterRegistry = bindToMeterRegistry(event);

		meterRegistry.get(JETTY_THREADS_CURRENT).gauge();
	}

	private MeterRegistry bindToMeterRegistry(ApplicationStartedEvent event) {
		MeterRegistry meterRegistry = new SimpleMeterRegistry();
		JettyServerThreadPoolMetricsBinder metricsBinder = new JettyServerThreadPoolMetricsBinder(
				meterRegistry);
		metricsBinder.onApplicationEvent(event);
		return meterRegistry;
	}

}
