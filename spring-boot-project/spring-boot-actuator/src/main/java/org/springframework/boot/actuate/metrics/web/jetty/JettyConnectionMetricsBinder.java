/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.metrics.web.jetty;

import java.util.Collections;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jetty.JettyConnectionMetrics;
import org.eclipse.jetty.server.Server;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.embedded.jetty.JettyWebServer;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

/**
 * Binds {@link JettyConnectionMetrics} in response to the
 * {@link ApplicationStartedEvent}.
 *
 * @author Chris Bono
 * @since 2.6.0
 */
public class JettyConnectionMetricsBinder implements ApplicationListener<ApplicationStartedEvent> {

	private final MeterRegistry meterRegistry;

	private final Iterable<Tag> tags;

	public JettyConnectionMetricsBinder(MeterRegistry meterRegistry) {
		this(meterRegistry, Collections.emptyList());
	}

	public JettyConnectionMetricsBinder(MeterRegistry meterRegistry, Iterable<Tag> tags) {
		this.meterRegistry = meterRegistry;
		this.tags = tags;
	}

	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		ApplicationContext applicationContext = event.getApplicationContext();
		Server server = findServer(applicationContext);
		if (server != null) {
			JettyConnectionMetrics.addToAllConnectors(server, this.meterRegistry, this.tags);
		}
	}

	private Server findServer(ApplicationContext applicationContext) {
		if (applicationContext instanceof WebServerApplicationContext) {
			WebServer webServer = ((WebServerApplicationContext) applicationContext).getWebServer();
			if (webServer instanceof JettyWebServer) {
				return ((JettyWebServer) webServer).getServer();
			}
		}
		return null;
	}

}
