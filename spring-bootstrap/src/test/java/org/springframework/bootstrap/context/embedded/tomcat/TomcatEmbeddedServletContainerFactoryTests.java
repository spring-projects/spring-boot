/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.bootstrap.context.embedded.tomcat;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.startup.Tomcat;
import org.junit.Test;
import org.mockito.InOrder;
import org.springframework.bootstrap.context.embedded.AbstractEmbeddedServletContainerFactoryTests;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TomcatEmbeddedServletContainerFactory} and
 * {@link TomcatEmbeddedServletContainer}.
 * 
 * @author Phillip Webb
 */
public class TomcatEmbeddedServletContainerFactoryTests extends
		AbstractEmbeddedServletContainerFactoryTests {

	@Override
	protected TomcatEmbeddedServletContainerFactory getFactory() {
		return new TomcatEmbeddedServletContainerFactory();
	}

	@Test
	public void tomcatListeners() throws Exception {
		TomcatEmbeddedServletContainerFactory factory = getFactory();
		LifecycleListener[] listeners = new LifecycleListener[4];
		for (int i = 0; i < listeners.length; i++) {
			listeners[i] = mock(LifecycleListener.class);
		}
		factory.setContextLifecycleListeners(Arrays.asList(listeners[0], listeners[1]));
		factory.addContextLifecycleListeners(listeners[2], listeners[3]);
		this.container = factory.getEmbeddedServletContainer();
		InOrder ordered = inOrder((Object[]) listeners);
		for (LifecycleListener listener : listeners) {
			ordered.verify(listener).lifecycleEvent((LifecycleEvent) anyObject());
		}
	}

	@Test
	public void sessionTimeout() throws Exception {
		TomcatEmbeddedServletContainerFactory factory = getFactory();
		factory.setSessionTimeout(10);
		assertTimeout(factory, 10);
	}

	@Test
	public void sessionTimeoutInMins() throws Exception {
		TomcatEmbeddedServletContainerFactory factory = getFactory();
		factory.setSessionTimeout(1, TimeUnit.MINUTES);
		assertTimeout(factory, 60);
	}

	private void assertTimeout(TomcatEmbeddedServletContainerFactory factory, int expected) {
		this.container = factory.getEmbeddedServletContainer();
		Tomcat tomcat = ((TomcatEmbeddedServletContainer) this.container).getTomcat();
		Context context = (Context) tomcat.getHost().findChildren()[0];
		assertThat(context.getSessionTimeout(), equalTo(expected));
	}

	// FIXME test valve
}
