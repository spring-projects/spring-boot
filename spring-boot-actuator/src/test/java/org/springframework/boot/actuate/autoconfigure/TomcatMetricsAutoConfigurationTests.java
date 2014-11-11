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

package org.springframework.boot.actuate.autoconfigure;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.endpoint.TomcatPublicMetrics;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration.EmbeddedTomcat;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link TomcatMetricsAutoConfiguration}.
 *
 * @author Johannes Stelzer
 */
public class TomcatMetricsAutoConfigurationTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void after() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void no_Tomcat() {
		this.context.register(TomcatMetricsAutoConfiguration.class);
		this.context.refresh();
		this.context.getBean(TomcatPublicMetrics.class);
	}

	@Test
	public void has_Tomcat() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(EmbeddedTomcat.class);
		this.context.register(TomcatMetricsAutoConfiguration.class);
		this.context.refresh();

		TomcatPublicMetrics publicMetrics = this.context
				.getBean(TomcatPublicMetrics.class);
		assertNotNull(publicMetrics);
	}

}
