/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.metrics.web.tomcat;

import java.util.Collections;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.tomcat.TomcatMetrics;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Manager;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

/**
 * Binds {@link TomcatMetrics} in response to the {@link ApplicationStartedEvent}.
 *
 * @author Andy Wilkinson
 * @since 2.1.0
 */
public class TomcatMetricsBinder implements ApplicationListener<ApplicationStartedEvent>, DisposableBean {

	private final MeterRegistry meterRegistry;

	private final Iterable<Tag> tags;

	private volatile TomcatMetrics tomcatMetrics;

	public TomcatMetricsBinder(MeterRegistry meterRegistry) {
		this(meterRegistry, Collections.emptyList());
	}

	public TomcatMetricsBinder(MeterRegistry meterRegistry, Iterable<Tag> tags) {
		this.meterRegistry = meterRegistry;
		this.tags = tags;
	}

	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		ApplicationContext applicationContext = event.getApplicationContext();
		Manager manager = findManager(applicationContext);
		this.tomcatMetrics = new TomcatMetrics(manager, this.tags);
		this.tomcatMetrics.bindTo(this.meterRegistry);
	}

	private Manager findManager(ApplicationContext applicationContext) {
		if (applicationContext instanceof WebServerApplicationContext) {
			WebServer webServer = ((WebServerApplicationContext) applicationContext).getWebServer();
			if (webServer instanceof TomcatWebServer) {
				Context context = findContext((TomcatWebServer) webServer);
				return context.getManager();
			}
		}
		return null;
	}

	private Context findContext(TomcatWebServer tomcatWebServer) {
		for (Container container : tomcatWebServer.getTomcat().getHost().findChildren()) {
			if (container instanceof Context) {
				return (Context) container;
			}
		}
		return null;
	}

	@Override
	public void destroy() {
		if (this.tomcatMetrics != null) {
			this.tomcatMetrics.close();
		}
	}

}
