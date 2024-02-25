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

package org.springframework.boot.rsocket.context;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.rsocket.server.RSocketServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

/**
 * {@link ApplicationContextInitializer} that sets {@link Environment} properties for the
 * ports that {@link RSocketServer} servers are actually listening on. The property
 * {@literal "local.rsocket.server.port"} can be injected directly into tests using
 * {@link Value @Value} or obtained through the {@link Environment}.
 * <p>
 * Properties are automatically propagated up to any parent context.
 *
 * @author Verónica Vásquez
 * @author Eddú Meléndez
 * @since 2.2.0
 */
public class RSocketPortInfoApplicationContextInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	/**
     * Initializes the ConfigurableApplicationContext by adding an application listener.
     * The listener is responsible for handling events related to the application context.
     * 
     * @param applicationContext the ConfigurableApplicationContext to be initialized
     */
    @Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		applicationContext.addApplicationListener(new Listener(applicationContext));
	}

	/**
     * Listener class.
     */
    private static class Listener implements ApplicationListener<RSocketServerInitializedEvent> {

		private static final String PROPERTY_NAME = "local.rsocket.server.port";

		private static final String PROPERTY_SOURCE_NAME = "server.ports";

		private final ConfigurableApplicationContext applicationContext;

		/**
         * Constructs a new Listener with the specified ConfigurableApplicationContext.
         * 
         * @param applicationContext the ConfigurableApplicationContext to be set
         */
        Listener(ConfigurableApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		/**
         * This method is called when the RSocket server is initialized.
         * It sets the port property in the application context if the server address is not null.
         *
         * @param event The RSocketServerInitializedEvent object representing the server initialization event.
         */
        @Override
		public void onApplicationEvent(RSocketServerInitializedEvent event) {
			if (event.getServer().address() != null) {
				setPortProperty(this.applicationContext, event.getServer().address().getPort());
			}
		}

		/**
         * Sets the port property for the given ApplicationContext and its parent ApplicationContexts.
         * 
         * @param context the ApplicationContext to set the port property for
         * @param port the port value to set
         */
        private void setPortProperty(ApplicationContext context, int port) {
			if (context instanceof ConfigurableApplicationContext configurableContext) {
				setPortProperty(configurableContext.getEnvironment(), port);
			}
			if (context.getParent() != null) {
				setPortProperty(context.getParent(), port);
			}
		}

		/**
         * Sets the port property in the given environment.
         * 
         * @param environment the configurable environment
         * @param port the port value to be set
         */
        private void setPortProperty(ConfigurableEnvironment environment, int port) {
			MutablePropertySources sources = environment.getPropertySources();
			PropertySource<?> source = sources.get(PROPERTY_SOURCE_NAME);
			if (source == null) {
				source = new MapPropertySource(PROPERTY_SOURCE_NAME, new HashMap<>());
				sources.addFirst(source);
			}
			setPortProperty(port, source);
		}

		/**
         * Sets the port property for the specified source.
         * 
         * @param port the port value to set
         * @param source the property source to update
         */
        @SuppressWarnings("unchecked")
		private void setPortProperty(int port, PropertySource<?> source) {
			((Map<String, Object>) source.getSource()).put(PROPERTY_NAME, port);
		}

	}

}
