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
package org.springframework.bootstrap.actuate.autoconfigure;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bootstrap.actuate.properties.ManagementServerProperties;
import org.springframework.bootstrap.actuate.properties.ServerProperties;
import org.springframework.bootstrap.context.annotation.ConditionalOnExpression;
import org.springframework.bootstrap.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * @author Dave Syer
 * 
 */
@Configuration
@ConditionalOnExpression("${management.port:${server.port:8080}}>0")
public class ManagementConfiguration implements ApplicationContextAware, DisposableBean,
		ApplicationListener<ContextRefreshedEvent> {

	private ApplicationContext parent;
	private ConfigurableApplicationContext context;

	@Autowired
	private ServerProperties configuration = new ServerProperties();

	@Autowired
	private ManagementServerProperties management = new ManagementServerProperties();

	@ConditionalOnExpression("${server.port:8080} == ${management.port:${server.port:8080}}")
	@Configuration
	@Import({ MetricsConfiguration.class, HealthConfiguration.class,
			ShutdownConfiguration.class, TraceConfiguration.class })
	public static class ManagementEndpointsConfiguration {
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.parent = applicationContext;
	}

	@Override
	public void destroy() throws Exception {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (event.getSource() != this.parent) {
			return;
		}
		if (this.configuration.getPort() != this.management.getPort()) {
			AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext();
			context.setParent(this.parent);
			context.register(ManagementServerConfiguration.class,
					MetricsConfiguration.class, HealthConfiguration.class,
					ShutdownConfiguration.class, TraceConfiguration.class);
			context.refresh();
			this.context = context;
		}
	}

}
