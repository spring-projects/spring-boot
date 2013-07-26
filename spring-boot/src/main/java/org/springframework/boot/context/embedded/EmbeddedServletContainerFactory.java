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

package org.springframework.boot.context.embedded;

import org.apache.catalina.core.ApplicationContext;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;

/**
 * Factory interface that can be used to create {@link EmbeddedServletContainer}s.
 * Implementations are encouraged to extends
 * {@link AbstractEmbeddedServletContainerFactory} when possible.
 * 
 * @author Phillip Webb
 * @see EmbeddedServletContainer
 * @see AbstractEmbeddedServletContainerFactory
 * @see JettyEmbeddedServletContainerFactory
 * @see TomcatEmbeddedServletContainerFactory
 */
public interface EmbeddedServletContainerFactory {

	/**
	 * Gets a new fully configured but paused {@link EmbeddedServletContainer} instance.
	 * Clients should not be able to connect to the returned server until
	 * {@link EmbeddedServletContainer#start()} is called (which happens when the
	 * {@link ApplicationContext} has been fully refreshed).
	 * @param initializers {@link ServletContextInitializer}s that should be applied as
	 * the container starts
	 * @return a fully configured and started {@link EmbeddedServletContainer}
	 * @see EmbeddedServletContainer#stop()
	 */
	EmbeddedServletContainer getEmbeddedServletContainer(
			ServletContextInitializer... initializers);

}
