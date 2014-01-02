/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.embedded.jetty;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.Test;
import org.mockito.InOrder;
import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactoryTests;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JettyEmbeddedServletContainerFactory} and
 * {@link JettyEmbeddedServletContainer}.
 * 
 * @author Phillip Webb
 */
public class JettyEmbeddedServletContainerFactoryTests extends
		AbstractEmbeddedServletContainerFactoryTests {

	@Override
	protected JettyEmbeddedServletContainerFactory getFactory() {
		return new JettyEmbeddedServletContainerFactory();
	}

	@Test
	public void jettyConfigurations() throws Exception {
		JettyEmbeddedServletContainerFactory factory = getFactory();
		Configuration[] configurations = new Configuration[4];
		for (int i = 0; i < configurations.length; i++) {
			configurations[i] = mock(Configuration.class);
		}
		factory.setConfigurations(Arrays.asList(configurations[0], configurations[1]));
		factory.addConfigurations(configurations[2], configurations[3]);
		this.container = factory.getEmbeddedServletContainer();
		InOrder ordered = inOrder((Object[]) configurations);
		for (Configuration configuration : configurations) {
			ordered.verify(configuration).configure((WebAppContext) anyObject());
		}
	}

	@Test
	public void sessionTimeout() throws Exception {
		JettyEmbeddedServletContainerFactory factory = getFactory();
		factory.setSessionTimeout(10);
		assertTimeout(factory, 10);
	}

	@Test
	public void sessionTimeoutInMins() throws Exception {
		JettyEmbeddedServletContainerFactory factory = getFactory();
		factory.setSessionTimeout(1, TimeUnit.MINUTES);
		assertTimeout(factory, 60);
	}

	private void assertTimeout(JettyEmbeddedServletContainerFactory factory, int expected) {
		this.container = factory.getEmbeddedServletContainer();
		JettyEmbeddedServletContainer jettyContainer = (JettyEmbeddedServletContainer) this.container;
		Handler[] handlers = jettyContainer.getServer().getChildHandlersByClass(
				WebAppContext.class);
		WebAppContext webAppContext = (WebAppContext) handlers[0];
		int actual = webAppContext.getSessionHandler().getSessionManager()
				.getMaxInactiveInterval();
		assertThat(actual, equalTo(expected));
	}

}
