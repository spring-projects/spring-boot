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

package org.springframework.bootstrap.autoconfigure.web;

import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.RemoteIpValve;
import org.springframework.beans.BeansException;
import org.springframework.bootstrap.context.annotation.ConditionalOnMissingBean;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.bootstrap.context.annotation.EnableConfigurationProperties;
import org.springframework.bootstrap.context.embedded.ConfigurableEmbeddedServletContainerFactory;
import org.springframework.bootstrap.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.bootstrap.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.bootstrap.properties.ServerProperties;
import org.springframework.bootstrap.properties.ServerProperties.Tomcat;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} that configures the
 * {@link ConfigurableEmbeddedServletContainerFactory} from a {@link ServerProperties}
 * bean.
 * 
 * @author Dave Syer
 */
@Configuration
@EnableConfigurationProperties
public class ServerPropertiesAutoConfiguration implements
		EmbeddedServletContainerCustomizer, ApplicationContextAware {

	private ApplicationContext applicationContext;

	@Bean(name = "org.springframework.bootstrap.properties.ServerProperties")
	@ConditionalOnMissingBean
	public ServerProperties serverProperties() {
		return new ServerProperties();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void customize(ConfigurableEmbeddedServletContainerFactory factory) {

		// Need to do a look up here to make it lazy
		ServerProperties server = this.applicationContext.getBean(ServerProperties.class);

		factory.setPort(server.getPort());
		factory.setAddress(server.getAddress());
		factory.setContextPath(server.getContextPath());

		if (factory instanceof TomcatEmbeddedServletContainerFactory) {
			configureTomcat((TomcatEmbeddedServletContainerFactory) factory, server);
		}

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
