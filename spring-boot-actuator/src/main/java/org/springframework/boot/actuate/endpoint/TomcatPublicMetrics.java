/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Manager;
import org.apache.catalina.session.ManagerBase;
import org.springframework.beans.BeansException;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * A {@link PublicMetrics} implementation that provides Tomcat statistics.
 *
 * @author Johannes Stelzer
 * @author Phillip Webb
 * @since 1.2.0
 */
public class TomcatPublicMetrics implements PublicMetrics, ApplicationContextAware {

	private ApplicationContext applicationContext;

	@Override
	public Collection<Metric<?>> metrics() {
		if (this.applicationContext instanceof EmbeddedWebApplicationContext) {
			Manager manager = getManager((EmbeddedWebApplicationContext) this.applicationContext);
			if (manager != null) {
				return metrics(manager);
			}
		}
		return Collections.emptySet();
	}

	private Manager getManager(EmbeddedWebApplicationContext applicationContext) {
		EmbeddedServletContainer embeddedServletContainer = applicationContext
				.getEmbeddedServletContainer();
		if (embeddedServletContainer instanceof TomcatEmbeddedServletContainer) {
			return getManager((TomcatEmbeddedServletContainer) embeddedServletContainer);
		}
		return null;
	}

	private Manager getManager(TomcatEmbeddedServletContainer servletContainer) {
		for (Container container : servletContainer.getTomcat().getHost().findChildren()) {
			if (container instanceof Context) {
				return ((Context) container).getManager();
			}
		}
		return null;
	}

	private Collection<Metric<?>> metrics(Manager manager) {
		List<Metric<?>> metrics = new ArrayList<Metric<?>>(2);
		if (manager instanceof ManagerBase) {
			addMetric(metrics, "httpsessions.max",
					((ManagerBase) manager).getMaxActiveSessions());
		}
		addMetric(metrics, "httpsessions.active", manager.getActiveSessions());
		return metrics;
	}

	private void addMetric(List<Metric<?>> metrics, String name, Integer value) {
		metrics.add(new Metric<Integer>(name, value));
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

}
