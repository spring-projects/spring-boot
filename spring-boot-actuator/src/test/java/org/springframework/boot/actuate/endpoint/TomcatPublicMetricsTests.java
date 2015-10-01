/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.Iterator;

import org.junit.Test;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.SocketUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link TomcatPublicMetrics}
 *
 * @author Johannes Stelzer
 * @author Phillip Webb
 */
public class TomcatPublicMetricsTests {

	@Test
	public void tomcatMetrics() throws Exception {
		AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext(
				Config.class);
		try {
			TomcatPublicMetrics tomcatMetrics = context
					.getBean(TomcatPublicMetrics.class);
			Iterator<Metric<?>> metrics = tomcatMetrics.metrics().iterator();
			assertThat(metrics.next().getName(), equalTo("httpsessions.max"));
			assertThat(metrics.next().getName(), equalTo("httpsessions.active"));
			assertThat(metrics.hasNext(), equalTo(false));
		}
		finally {
			context.close();
		}
	}

	@Configuration
	static class Config {

		@Bean
		public TomcatEmbeddedServletContainerFactory containerFactory() {
			TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
			factory.setPort(SocketUtils.findAvailableTcpPort(40000));
			return factory;
		}

		@Bean
		public TomcatPublicMetrics metrics() {
			return new TomcatPublicMetrics();
		}

	}

}
