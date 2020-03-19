/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.kubernetes;

import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.kubernetes.ApplicationStateProvider;
import org.springframework.boot.kubernetes.SpringApplicationEventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration} for
 * {@link ApplicationStateProvider}.
 *
 * @author Brian Clozel
 * @since 2.3.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
public class ApplicationStateAutoConfiguration {

	@Bean
	public ApplicationStateProvider applicationStateProvider() {
		return new ApplicationStateProvider();
	}

	@Bean
	public SpringApplicationEventListener springApplicationEventListener() {
		return new SpringApplicationEventListener();
	}

}
