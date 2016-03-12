/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.logging;

import org.junit.Test;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link LoggingApplicationListener}.
 *
 * @author Stephane Nicoll
 */
public class LoggingApplicationListenerIntegrationTests {

	@Test
	public void loggingSystemRegisteredInTheContext() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(
				SampleService.class).web(false).run();
		try {
			SampleService service = context.getBean(SampleService.class);
			assertThat(service.loggingSystem).isNotNull();
		}
		finally {
			context.close();
		}
	}

	@Component
	static class SampleService {

		private final LoggingSystem loggingSystem;

		SampleService(LoggingSystem loggingSystem) {
			this.loggingSystem = loggingSystem;
		}

	}

}
