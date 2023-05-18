/*
 * Copyright 2012-2023 the original author or authors.
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

package org.test;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

@Configuration(proxyBeanMethods = false)
public class SampleApplication {

	public static void main(String[] args) {
		Assert.state(!ClassUtils.isPresent("org.springframework.boot.devtools.autoconfigure.DevToolsProperties", null), "Should not have devtools");
		Assert.state(!ClassUtils.isPresent("org.springframework.boot.docker.compose.core.DockerCompose", null), "Should not have docker-compose");
		SpringApplication.run(SampleApplication.class, args);
	}

}
