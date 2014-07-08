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

package org.springframework.boot.actuate.autoconfigure;

import org.junit.Test;
import org.springframework.boot.actuate.trace.InMemoryTraceRepository;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TraceRepositoryAutoConfiguration}.
 *
 * @author Phillip Webb
 */
public class TraceRepositoryAutoConfigurationTests {

	@Test
	public void configuresInMemoryTraceRepository() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				TraceRepositoryAutoConfiguration.class);
		assertNotNull(context.getBean(InMemoryTraceRepository.class));
		context.close();
	}

	@Test
	public void skipsIfRepositoryExists() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				Config.class, TraceRepositoryAutoConfiguration.class);
		assertThat(context.getBeansOfType(InMemoryTraceRepository.class).size(),
				equalTo(0));
		assertThat(context.getBeansOfType(TraceRepository.class).size(), equalTo(1));
		context.close();
	}

	@Configuration
	public static class Config {

		@Bean
		public TraceRepository traceRepository() {
			return mock(TraceRepository.class);
		}

	}

}
