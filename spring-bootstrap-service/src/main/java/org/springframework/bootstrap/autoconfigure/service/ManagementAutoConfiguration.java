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
package org.springframework.bootstrap.autoconfigure.service;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bootstrap.context.annotation.ConditionalOnExpression;
import org.springframework.bootstrap.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.bootstrap.service.properties.ContainerProperties;
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
public class ManagementAutoConfiguration implements ApplicationContextAware,
		DisposableBean, ApplicationListener<ContextRefreshedEvent> {

	private ApplicationContext parent;
	private ConfigurableApplicationContext context;

	@Autowired
	private ContainerProperties configuration = new ContainerProperties();

	@ConditionalOnExpression("${container.port:8080} == ${container.management_port:8080}")
	@Configuration
	@Import({ VarzAutoConfiguration.class, HealthzAutoConfiguration.class,
			ShutdownAutoConfiguration.class, TraceAutoConfiguration.class })
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
		if (this.configuration.getPort() != this.configuration.getManagementPort()) {
			AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext();
			context.setParent(this.parent);
			context.register(ManagementContainerConfiguration.class,
					VarzAutoConfiguration.class, HealthzAutoConfiguration.class,
					ShutdownAutoConfiguration.class, TraceAutoConfiguration.class);
			context.refresh();
			this.context = context;
		}
	}

}
