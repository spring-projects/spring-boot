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

package org.springframework.boot.autoconfigure.lifecycle;

import javax.management.MalformedObjectNameException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jmx.export.MBeanExporter;

/**
 * Register a JMX component that allows to manage the lifecycle of the current
 * application. Intended for internal use only.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 * @see SpringApplicationLifecycleMXBean
 */
@Configuration
@AutoConfigureAfter(JmxAutoConfiguration.class)
@ConditionalOnProperty(value = "spring.application.lifecycle.enabled", havingValue = "true", matchIfMissing = false)
class SpringApplicationLifecycleAutoConfiguration {

	/**
	 * The property to use to customize the {@code ObjectName} of the application lifecycle mbean.
	 */
	static final String JMX_NAME_PROPERTY = "spring.application.lifecycle.jmx-name";

	/**
	 * The default {@code ObjectName} of the application lifecycle mbean.
	 */
	static final String DEFAULT_JMX_NAME = "org.springframework.boot:type=Lifecycle,name=springApplicationLifecycle";

	@Autowired(required = false)
	private MBeanExporter mbeanExporter;

	@Autowired
	private Environment environment;

	@Bean
	public SpringApplicationLifecycleRegistrar springApplicationLifecycleRegistrar()
			throws MalformedObjectNameException {

		String jmxName = this.environment.getProperty(JMX_NAME_PROPERTY, DEFAULT_JMX_NAME);
		if (mbeanExporter != null) { // Make sure to not register that MBean twice
			mbeanExporter.addExcludedBean(jmxName);
		}
		return new SpringApplicationLifecycleRegistrar(jmxName);
	}

}
