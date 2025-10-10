/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.test.http.client;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ClassUtils;

/**
 * {@link ContextCustomizerFactory} to disable the use of global resources in
 * {@link ReactorResourceFactory} preventing test cleanup issues.
 *
 * @author Phillip Webb
 */
class DisableReactorResourceFactoryGlobalResourcesContextCustomizerFactory implements ContextCustomizerFactory {

	private static final String REACTOR_RESOURCE_FACTORY_CLASS = "org.springframework.http.client.ReactorResourceFactory";

	private static final String REACTOR_NETTY_CLASS = "reactor.netty.ReactorNetty";

	@Override
	public @Nullable ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		ClassLoader classLoader = testClass.getClassLoader();
		if (ClassUtils.isPresent(REACTOR_RESOURCE_FACTORY_CLASS, classLoader)
				&& ClassUtils.isPresent(REACTOR_NETTY_CLASS, classLoader)) {
			return new DisableReactorResourceFactoryGlobalResourcesContextCustomizerCustomizer();
		}
		return null;

	}

	static final class DisableReactorResourceFactoryGlobalResourcesContextCustomizerCustomizer
			implements ContextCustomizer {

		private DisableReactorResourceFactoryGlobalResourcesContextCustomizerCustomizer() {
		}

		@Override
		public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
			context.getBeanFactory()
				.registerSingleton(DisableReactorResourceFactoryGlobalResourcesBeanPostProcessor.class.getName(),
						new DisableReactorResourceFactoryGlobalResourcesBeanPostProcessor());
		}

		@Override
		public boolean equals(Object obj) {
			return (obj instanceof DisableReactorResourceFactoryGlobalResourcesContextCustomizerCustomizer);
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

	}

}
