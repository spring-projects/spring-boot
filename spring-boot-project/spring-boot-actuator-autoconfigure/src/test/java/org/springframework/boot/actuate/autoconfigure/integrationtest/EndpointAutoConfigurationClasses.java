/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.integrationtest;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.actuate.autoconfigure.audit.AuditEventsEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.condition.ConditionsReportEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.context.ShutdownEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.context.properties.ConfigurationPropertiesReportEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.env.EnvironmentEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.management.ThreadDumpEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.trace.http.HttpTraceEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.mappings.MappingsEndpointAutoConfiguration;
import org.springframework.util.ClassUtils;

/**
 * A list of all endpoint auto-configuration classes for use in tests.
 */
final class EndpointAutoConfigurationClasses {

	static final Class<?>[] ALL;

	static {
		List<Class<?>> all = new ArrayList<>();
		all.add(AuditEventsEndpointAutoConfiguration.class);
		all.add(BeansEndpointAutoConfiguration.class);
		all.add(ConditionsReportEndpointAutoConfiguration.class);
		all.add(ConfigurationPropertiesReportEndpointAutoConfiguration.class);
		all.add(ShutdownEndpointAutoConfiguration.class);
		all.add(EnvironmentEndpointAutoConfiguration.class);
		all.add(HealthEndpointAutoConfiguration.class);
		all.add(InfoEndpointAutoConfiguration.class);
		all.add(ThreadDumpEndpointAutoConfiguration.class);
		all.add(HttpTraceEndpointAutoConfiguration.class);
		all.add(MappingsEndpointAutoConfiguration.class);
		ALL = ClassUtils.toClassArray(all);
	}

	private EndpointAutoConfigurationClasses() {
	}

}
