/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.jolokia;

import org.jolokia.http.AgentServlet;

import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ManagementServletContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.ServletWrappingController;

/**
 * {@link ManagementContextConfiguration} for embedding Jolokia, a JMX-HTTP bridge giving
 * an alternative to JSR-160 connectors.
 * <p>
 * This configuration will get automatically enabled as soon as the Jolokia
 * {@link AgentServlet} is on the classpath. To disable it set
 * {@code management.jolokia.enabled=false}.
 * <p>
 * Additional configuration parameters for Jolokia can be provided by specifying
 * {@code management.jolokia.config.*} properties. See the
 * <a href="http://jolokia.org">http://jolokia.org</a> web site for more information on
 * supported configuration parameters.
 *
 * @author Christian Dupuis
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ManagementContextConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ AgentServlet.class, ServletWrappingController.class })
@ConditionalOnProperty(value = "management.jolokia.enabled", havingValue = "true")
@EnableConfigurationProperties(JolokiaProperties.class)
public class JolokiaManagementContextConfiguration {

	private final ManagementServletContext managementServletContext;

	private final JolokiaProperties properties;

	public JolokiaManagementContextConfiguration(
			ManagementServletContext managementServletContext,
			JolokiaProperties properties) {
		this.managementServletContext = managementServletContext;
		this.properties = properties;
	}

	@Bean
	public ServletRegistrationBean<AgentServlet> jolokiaServlet() {
		String path = this.managementServletContext.getServletPath()
				+ this.properties.getPath();
		String urlMapping = (path.endsWith("/") ? path + "*" : path + "/*");
		ServletRegistrationBean<AgentServlet> registration = new ServletRegistrationBean<>(
				new AgentServlet(), urlMapping);
		registration.setInitParameters(this.properties.getConfig());
		return registration;
	}

}
