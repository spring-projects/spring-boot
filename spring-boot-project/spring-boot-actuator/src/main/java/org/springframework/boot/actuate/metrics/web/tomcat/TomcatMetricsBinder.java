/*
 * Copyright 2012-2022 the original author or authors.
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

	/**
     * Constructs a new TomcatMetricsBinder with the specified MeterRegistry and an empty list of additional tags.
     *
     * @param meterRegistry the MeterRegistry to bind Tomcat metrics to
     */
    public TomcatMetricsBinder(MeterRegistry meterRegistry) {
		this(meterRegistry, Collections.emptyList());
	}

	/**
     * Constructs a new TomcatMetricsBinder with the specified MeterRegistry and tags.
     *
     * @param meterRegistry the MeterRegistry to bind the metrics to
     * @param tags the tags to associate with the metrics
     */
    public TomcatMetricsBinder(MeterRegistry meterRegistry, Iterable<Tag> tags) {
		this.meterRegistry = meterRegistry;
		this.tags = tags;
	}

	/**
     * This method is called when the application has started.
     * It retrieves the application context from the event and finds the manager.
     * It then creates a new instance of TomcatMetrics with the manager and tags provided.
     * Finally, it binds the TomcatMetrics to the meter registry.
     * 
     * @param event The ApplicationStartedEvent that triggered this method
     */
    @Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		ApplicationContext applicationContext = event.getApplicationContext();
		Manager manager = findManager(applicationContext);
		this.tomcatMetrics = new TomcatMetrics(manager, this.tags);
		this.tomcatMetrics.bindTo(this.meterRegistry);
	}

	/**
     * Finds the manager of the Tomcat web server in the given application context.
     * 
     * @param applicationContext the application context to search for the Tomcat web server
     * @return the manager of the Tomcat web server, or null if not found
     */
    private Manager findManager(ApplicationContext applicationContext) {
		if (applicationContext instanceof WebServerApplicationContext webServerApplicationContext) {
			WebServer webServer = webServerApplicationContext.getWebServer();
			if (webServer instanceof TomcatWebServer tomcatWebServer) {
				Context context = findContext(tomcatWebServer);
				if (context != null) {
					return context.getManager();
				}
			}
		}
		return null;
	}

	/**
     * Finds the first Context object in the given TomcatWebServer.
     * 
     * @param tomcatWebServer the TomcatWebServer to search for the Context object
     * @return the first Context object found, or null if none is found
     */
    private Context findContext(TomcatWebServer tomcatWebServer) {
		for (Container container : tomcatWebServer.getTomcat().getHost().findChildren()) {
			if (container instanceof Context context) {
				return context;
			}
		}
		return null;
	}

	/**
     * This method is called when the TomcatMetricsBinder is being destroyed.
     * It closes the TomcatMetrics instance if it is not null.
     */
    @Override
	public void destroy() {
		if (this.tomcatMetrics != null) {
			this.tomcatMetrics.close();
		}
	}

}
