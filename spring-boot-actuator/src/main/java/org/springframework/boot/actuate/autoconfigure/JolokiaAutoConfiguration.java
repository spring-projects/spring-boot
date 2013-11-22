/*
 * Copyright 2013 the original author or authors.
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

import java.util.Map;

import org.jolokia.http.AgentServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.JolokiaEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for embedding Jolokia, a JMX-HTTP
 * bridge giving an alternative to JSR-160 connectors.
 * 
 * <p>
 * This configuration will get automatically enabled as soon as the Jolokia
 * {@link AgentServlet} is on the classpath. To disable set
 * <code>endpoints.jolokia.enabled: false</code>.
 * 
 * <p>
 * Additional configuration parameters for Jolokia can be provided by specifying
 * <code>jolokia.config. ...</code> properties. See the <a
 * href="http://jolokia.org">http://jolokia.org</a> web site for more information on
 * supported configuration parameters.
 * 
 * @author Christian Dupuis
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass({ AgentServlet.class })
@ConditionalOnBean(EmbeddedServletContainerFactory.class)
@AutoConfigureBefore(ManagementSecurityAutoConfiguration.class)
@AutoConfigureAfter(EmbeddedServletContainerAutoConfiguration.class)
@ConditionalOnExpression("${endpoints.jolokia.enabled:true}")
public class JolokiaAutoConfiguration {

	private RelaxedPropertyResolver environment;

	@Autowired
	public void setEnvironment(Environment environment) {
		this.environment = new RelaxedPropertyResolver(environment);
	}

	@Bean
	@ConditionalOnMissingBean({ AgentServlet.class })
	public AgentServlet jolokiaServlet() {
		return new AgentServlet();
	}

	@Bean
	@ConditionalOnMissingBean()
	public ServletRegistrationBean jolokiaServletRegistration() {
		ServletRegistrationBean registrationBean = new ServletRegistrationBean(
				jolokiaServlet(), this.environment.getProperty("endpoints.jolokia.path",
						"/jolokia") + "/*");
		addInitParameters(registrationBean);
		return registrationBean;
	}

	@Bean
	@ConditionalOnMissingBean
	public JolokiaEndpoint jolokiaEndpoint() {
		return new JolokiaEndpoint();
	}

	protected void addInitParameters(ServletRegistrationBean registrationBean) {
		Map<String, Object> configParameters = this.environment
				.getSubProperties("jolokia.config.");
		for (Map.Entry<String, Object> configParameter : configParameters.entrySet()) {
			registrationBean.addInitParameter(configParameter.getKey(), configParameter
					.getValue().toString());
		}
	}
}
