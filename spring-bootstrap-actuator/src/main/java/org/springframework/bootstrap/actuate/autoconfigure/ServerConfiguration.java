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

import java.util.Collections;

import javax.servlet.Servlet;

import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.RemoteIpValve;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.bootstrap.actuate.error.ErrorEndpoint;
import org.springframework.bootstrap.actuate.properties.ServerProperties;
import org.springframework.bootstrap.actuate.properties.ServerProperties.Tomcat;
import org.springframework.bootstrap.context.annotation.ConditionalOnClass;
import org.springframework.bootstrap.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.bootstrap.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.bootstrap.context.embedded.ErrorPage;
import org.springframework.bootstrap.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

/**
 * Configuration for injecting externalized properties into the container (e.g. tomcat).
 * 
 * @author Dave Syer
 */
// Slight hack here (BeanPostProcessor), to force the server properties to be bound in
// the right order
@Configuration
@ConditionalOnClass({ Servlet.class })
@Order(Integer.MIN_VALUE)
@Import(InfoConfiguration.class)
public class ServerConfiguration implements BeanPostProcessor, BeanFactoryAware {

	private BeanFactory beanFactory;

	// Don't do this! We don't get a callback for our own dependencies (lifecycle).
	// @Autowired
	// private AbstractEmbeddedServletContainerFactory factory;

	private boolean initialized = false;

	@Value("${endpoints.error.path:/error}")
	private String errorPath = "/error";

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Bean
	public ErrorEndpoint errorEndpoint() {
		return new ErrorEndpoint();
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {

		if (bean instanceof EmbeddedServletContainerFactory) {

			if (bean instanceof AbstractEmbeddedServletContainerFactory
					&& !this.initialized) {

				// Cannot use @Autowired because the injection happens too early
				ServerProperties server = this.beanFactory
						.getBean(ServerProperties.class);

				AbstractEmbeddedServletContainerFactory factory = (AbstractEmbeddedServletContainerFactory) bean;
				factory.setPort(server.getPort());
				factory.setContextPath(server.getContextPath());

				if (factory instanceof TomcatEmbeddedServletContainerFactory) {
					configureTomcat((TomcatEmbeddedServletContainerFactory) factory,
							server);
				}

				factory.setErrorPages(Collections
						.singleton(new ErrorPage(this.errorPath)));
				this.initialized = true;

			}

		}

		return bean;

	}

	private void configureTomcat(TomcatEmbeddedServletContainerFactory tomcatFactory,
			ServerProperties configuration) {

		Tomcat tomcat = configuration.getTomcat();
		if (tomcat.getBasedir() != null) {
			tomcatFactory.setBaseDirectory(tomcat.getBasedir());
		}

		String remoteIpHeader = tomcat.getRemoteIpHeader();
		String protocolHeader = tomcat.getProtocolHeader();

		if (StringUtils.hasText(remoteIpHeader) || StringUtils.hasText(protocolHeader)) {
			RemoteIpValve valve = new RemoteIpValve();
			valve.setRemoteIpHeader(remoteIpHeader);
			valve.setProtocolHeader(protocolHeader);
			tomcatFactory.addContextValves(valve);
		}

		String pattern = tomcat.getAccessLogPattern();
		if (pattern != null) {
			AccessLogValve valve = new AccessLogValve();
			valve.setPattern(pattern);
			valve.setSuffix(".log");
			tomcatFactory.addContextValves(valve);
		}

	}

}
