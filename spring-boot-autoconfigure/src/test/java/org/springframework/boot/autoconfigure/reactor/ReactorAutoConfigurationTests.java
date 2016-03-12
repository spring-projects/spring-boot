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

package org.springframework.boot.autoconfigure.reactor;

import org.junit.Test;
import reactor.bus.EventBus;
import reactor.core.Dispatcher;
import reactor.core.dispatch.MpscDispatcher;
import reactor.core.dispatch.RingBufferDispatcher;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactorAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class ReactorAutoConfigurationTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void eventBusIsAvailable() {
		this.context.register(ReactorAutoConfiguration.class);
		this.context.refresh();
		EventBus eventBus = this.context.getBean(EventBus.class);
		assertThat(eventBus.getDispatcher()).isInstanceOf(RingBufferDispatcher.class);
		this.context.close();
	}

	@Test
	public void customEventBus() {
		this.context.register(TestConfiguration.class, ReactorAutoConfiguration.class);
		this.context.refresh();
		EventBus eventBus = this.context.getBean(EventBus.class);
		assertThat(eventBus.getDispatcher()).isInstanceOf(MpscDispatcher.class);
		this.context.close();
	}

	@Configuration
	protected static class TestConfiguration {

		@Bean(destroyMethod = "shutdown")
		public Dispatcher dispatcher() {
			return new MpscDispatcher("test");
		}

		@Bean
		public EventBus customEventBus() {
			return EventBus.create(dispatcher());
		}
	}

}
