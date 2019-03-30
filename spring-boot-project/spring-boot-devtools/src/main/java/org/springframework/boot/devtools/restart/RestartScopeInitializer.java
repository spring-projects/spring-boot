/*
 * Copyright 2012-2017 the original author or authors.
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
public class RestartScopeInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		applicationContext.getBeanFactory().registerScope("restart", new RestartScope());
	}

	/**
	 * {@link Scope} that stores beans as {@link Restarter} attributes.
	 */
	private static class RestartScope implements Scope {

		@Override
		public Object get(String name, ObjectFactory<?> objectFactory) {
			return Restarter.getInstance().getOrAddAttribute(name, objectFactory);
		}

		@Override
		public Object remove(String name) {
			return Restarter.getInstance().removeAttribute(name);
		}

		@Override
		public void registerDestructionCallback(String name, Runnable callback) {
		}

		@Override
		public Object resolveContextualObject(String key) {
			return null;
		}

		@Override
		public String getConversationId() {
			return null;
		}

	}

}
