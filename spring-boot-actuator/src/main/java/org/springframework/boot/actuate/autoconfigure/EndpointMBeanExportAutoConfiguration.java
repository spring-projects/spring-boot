/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import javax.management.MBeanServer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.jmx.EndpointMBeanExporter;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to enable JMX export for
 * {@link Endpoint}s.
 *
 * @author Christian Dupuis
 */
@Configuration
@ConditionalOnExpression("${endpoints.jmx.enabled:true} && ${spring.jmx.enabled:true}")
@AutoConfigureAfter({ EndpointAutoConfiguration.class, JmxAutoConfiguration.class })
@EnableConfigurationProperties(EndpointMBeanExportProperties.class)
public class EndpointMBeanExportAutoConfiguration {

	@Autowired
	EndpointMBeanExportProperties properties = new EndpointMBeanExportProperties();

	@Bean
	public EndpointMBeanExporter endpointMBeanExporter(MBeanServer server) {
		EndpointMBeanExporter mbeanExporter = new EndpointMBeanExporter();
		String domain = this.properties.getDomain();
		if (StringUtils.hasText(domain)) {
			mbeanExporter.setDomain(domain);
		}
		mbeanExporter.setServer(server);
		mbeanExporter.setEnsureUniqueRuntimeObjectNames(this.properties.isUniqueNames());
		mbeanExporter.setObjectNameStaticProperties(this.properties.getStaticNames());
		return mbeanExporter;
	}

	@Bean
	@ConditionalOnMissingBean(MBeanServer.class)
	public MBeanServer mbeanServer() {
		return new JmxAutoConfiguration().mbeanServer();
	}

}
