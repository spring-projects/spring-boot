/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.server.undertow;

import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWarDeployment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.server.servlet.undertow.UndertowDeploymentInfoCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.task.VirtualThreadTaskExecutor;

/**
 * {@link Configuration Configuration} for an Undertow-based reactive or servlet web
 * server.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@ConditionalOnNotWarDeployment
@Configuration(proxyBeanMethods = false)
public class UndertowWebServerConfiguration {

	@Bean
	UndertowWebServerFactoryCustomizer undertowWebServerFactoryCustomizer(Environment environment,
			ServerProperties serverProperties, UndertowServerProperties undertowProperties) {
		return new UndertowWebServerFactoryCustomizer(environment, serverProperties, undertowProperties);
	}

	@Bean
	@ConditionalOnThreading(Threading.VIRTUAL)
	UndertowDeploymentInfoCustomizer virtualThreadsUndertowDeploymentInfoCustomizer() {
		return (deploymentInfo) -> deploymentInfo.setExecutor(new VirtualThreadTaskExecutor("undertow-"));
	}

}
