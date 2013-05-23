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

package org.springframework.bootstrap.actuate.autoconfigure;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.bootstrap.actuate.autoconfigure.ActuatorAutoConfiguration.ServerPropertiesConfiguration;
import org.springframework.bootstrap.actuate.endpoint.error.ErrorEndpoint;
import org.springframework.bootstrap.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.bootstrap.context.embedded.ConfigurableEmbeddedServletContainerFactory;
import org.springframework.bootstrap.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.bootstrap.context.embedded.ErrorPage;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import static org.junit.Assert.assertNotNull;

/**
 * @author Dave Syer
 */
public class ManagementServerConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@Test
	public void testWebConfiguration() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(ManagementServerConfiguration.class,
				ServerPropertiesConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(WebMvcConfigurationSupport.class));
		assertNotNull(this.context.getBean(ErrorEndpoint.class));
		ConfigurableEmbeddedServletContainerFactory factory = Mockito
				.mock(ConfigurableEmbeddedServletContainerFactory.class);
		this.context.getBean(EmbeddedServletContainerCustomizer.class).customize(factory);
		Mockito.verify(factory).addErrorPages(Mockito.any(ErrorPage.class));
		Mockito.verify(factory).setPort(8080);
	}

}
