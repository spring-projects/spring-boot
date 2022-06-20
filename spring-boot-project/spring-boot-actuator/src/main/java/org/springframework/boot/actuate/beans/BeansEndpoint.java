/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.beans;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

/**
 * {@link Endpoint @Endpoint} to expose details of an application's beans, grouped by
 * application context.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@Endpoint(id = "beans")
public class BeansEndpoint {

	private final ConfigurableApplicationContext context;

	/**
	 * Creates a new {@code BeansEndpoint} that will describe the beans in the given
	 * {@code context} and all of its ancestors.
	 * @param context the application context
	 * @see ConfigurableApplicationContext#getParent()
	 */
	public BeansEndpoint(ConfigurableApplicationContext context) {
		this.context = context;
	}

	@ReadOperation
	public ApplicationBeans beans() {
		Map<String, ContextBeans> contexts = new HashMap<>();
		ConfigurableApplicationContext context = this.context;
		while (context != null) {
			contexts.put(context.getId(), ContextBeans.describing(context));
			context = getConfigurableParent(context);
		}
		return new ApplicationBeans(contexts);
	}

	private static ConfigurableApplicationContext getConfigurableParent(ConfigurableApplicationContext context) {
		ApplicationContext parent = context.getParent();
		if (parent instanceof ConfigurableApplicationContext configurableParent) {
			return configurableParent;
		}
		return null;
	}

	/**
	 * A description of an application's beans, primarily intended for serialization to
	 * JSON.
	 */
	public static final class ApplicationBeans {

		private final Map<String, ContextBeans> contexts;

		private ApplicationBeans(Map<String, ContextBeans> contexts) {
			this.contexts = contexts;
		}

		public Map<String, ContextBeans> getContexts() {
			return this.contexts;
		}

	}

	/**
	 * A description of an application context, primarily intended for serialization to
	 * JSON.
	 */
	public static final class ContextBeans {

		private final Map<String, BeanDescriptor> beans;

		private final String parentId;

		private ContextBeans(Map<String, BeanDescriptor> beans, String parentId) {
			this.beans = beans;
			this.parentId = parentId;
		}

		public String getParentId() {
			return this.parentId;
		}

		public Map<String, BeanDescriptor> getBeans() {
			return this.beans;
		}

		private static ContextBeans describing(ConfigurableApplicationContext context) {
			if (context == null) {
				return null;
			}
			ConfigurableApplicationContext parent = getConfigurableParent(context);
			return new ContextBeans(describeBeans(context.getBeanFactory()), (parent != null) ? parent.getId() : null);
		}

		private static Map<String, BeanDescriptor> describeBeans(ConfigurableListableBeanFactory beanFactory) {
			Map<String, BeanDescriptor> beans = new HashMap<>();
			for (String beanName : beanFactory.getBeanDefinitionNames()) {
				BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
				if (isBeanEligible(beanName, definition, beanFactory)) {
					beans.put(beanName, describeBean(beanName, definition, beanFactory));
				}
			}
			return beans;
		}

		private static BeanDescriptor describeBean(String name, BeanDefinition definition,
				ConfigurableListableBeanFactory factory) {
			return new BeanDescriptor(factory.getAliases(name), definition.getScope(), factory.getType(name),
					definition.getResourceDescription(), factory.getDependenciesForBean(name));
		}

		private static boolean isBeanEligible(String beanName, BeanDefinition bd, ConfigurableBeanFactory bf) {
			return (bd.getRole() != BeanDefinition.ROLE_INFRASTRUCTURE
					&& (!bd.isLazyInit() || bf.containsSingleton(beanName)));
		}

	}

	/**
	 * A description of a bean in an application context, primarily intended for
	 * serialization to JSON.
	 */
	public static final class BeanDescriptor {

		private final String[] aliases;

		private final String scope;

		private final Class<?> type;

		private final String resource;

		private final String[] dependencies;

		private BeanDescriptor(String[] aliases, String scope, Class<?> type, String resource, String[] dependencies) {
			this.aliases = aliases;
			this.scope = (StringUtils.hasText(scope) ? scope : BeanDefinition.SCOPE_SINGLETON);
			this.type = type;
			this.resource = resource;
			this.dependencies = dependencies;
		}

		public String[] getAliases() {
			return this.aliases;
		}

		public String getScope() {
			return this.scope;
		}

		public Class<?> getType() {
			return this.type;
		}

		public String getResource() {
			return this.resource;
		}

		public String[] getDependencies() {
			return this.dependencies;
		}

	}

}
