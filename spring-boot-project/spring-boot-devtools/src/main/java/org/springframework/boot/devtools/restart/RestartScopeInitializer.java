/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.devtools.restart;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Support for a 'restart' {@link Scope} that allows beans to remain between restarts.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class RestartScopeInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	/**
     * Initializes the application context by registering the "restart" scope.
     * 
     * @param applicationContext the configurable application context
     */
    @Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		applicationContext.getBeanFactory().registerScope("restart", new RestartScope());
	}

	/**
	 * {@link Scope} that stores beans as {@link Restarter} attributes.
	 */
	private static final class RestartScope implements Scope {

		/**
         * Retrieves an attribute from the Restarter instance or adds it if it doesn't exist.
         * 
         * @param name the name of the attribute to retrieve or add
         * @param objectFactory the object factory used to create the attribute if it doesn't exist
         * @return the attribute associated with the given name
         */
        @Override
		public Object get(String name, ObjectFactory<?> objectFactory) {
			return Restarter.getInstance().getOrAddAttribute(name, objectFactory);
		}

		/**
         * Removes the attribute with the specified name from the RestartScope.
         * 
         * @param name the name of the attribute to be removed
         * @return the value of the removed attribute, or null if the attribute does not exist
         */
        @Override
		public Object remove(String name) {
			return Restarter.getInstance().removeAttribute(name);
		}

		/**
         * Registers a destruction callback for the given name.
         * 
         * @param name     the name of the object to register the destruction callback for
         * @param callback the callback to be executed when the object is destroyed
         */
        @Override
		public void registerDestructionCallback(String name, Runnable callback) {
		}

		/**
         * Resolves the contextual object for the given key.
         * 
         * @param key the key for the contextual object
         * @return the resolved contextual object, or null if not found
         */
        @Override
		public Object resolveContextualObject(String key) {
			return null;
		}

		/**
         * Returns the conversation ID associated with the restart scope.
         * 
         * @return the conversation ID associated with the restart scope, or null if none
         */
        @Override
		public String getConversationId() {
			return null;
		}

	}

}
