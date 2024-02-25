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
import org.springframework.boot.actuate.endpoint.OperationResponseBody;
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

	/**
	 * Retrieves the beans descriptor for the current application context and its parent
	 * contexts.
	 * @return The beans descriptor containing information about the beans in the
	 * application context.
	 */
	@ReadOperation
	public BeansDescriptor beans() {
		Map<String, ContextBeansDescriptor> contexts = new HashMap<>();
		ConfigurableApplicationContext context = this.context;
		while (context != null) {
			contexts.put(context.getId(), ContextBeansDescriptor.describing(context));
			context = getConfigurableParent(context);
		}
		return new BeansDescriptor(contexts);
	}

	/**
	 * Retrieves the configurable parent application context of the given application
	 * context.
	 * @param context the application context to retrieve the parent from
	 * @return the configurable parent application context, or null if the parent is not a
	 * configurable application context
	 */
	private static ConfigurableApplicationContext getConfigurableParent(ConfigurableApplicationContext context) {
		ApplicationContext parent = context.getParent();
		if (parent instanceof ConfigurableApplicationContext configurableParent) {
			return configurableParent;
		}
		return null;
	}

	/**
	 * Description of an application's beans.
	 */
	public static final class BeansDescriptor implements OperationResponseBody {

		private final Map<String, ContextBeansDescriptor> contexts;

		/**
		 * Constructs a new BeansDescriptor with the specified contexts.
		 * @param contexts a map of String keys to ContextBeansDescriptor values
		 * representing the contexts
		 */
		private BeansDescriptor(Map<String, ContextBeansDescriptor> contexts) {
			this.contexts = contexts;
		}

		/**
		 * Returns the map of contexts in the BeansDescriptor.
		 * @return the map of contexts in the BeansDescriptor
		 */
		public Map<String, ContextBeansDescriptor> getContexts() {
			return this.contexts;
		}

	}

	/**
	 * Description of an application context beans.
	 */
	public static final class ContextBeansDescriptor {

		private final Map<String, BeanDescriptor> beans;

		private final String parentId;

		/**
		 * Constructs a new ContextBeansDescriptor with the specified beans and parentId.
		 * @param beans the map of beans with their corresponding bean descriptors
		 * @param parentId the identifier of the parent context
		 */
		private ContextBeansDescriptor(Map<String, BeanDescriptor> beans, String parentId) {
			this.beans = beans;
			this.parentId = parentId;
		}

		/**
		 * Returns the parent ID of the ContextBeansDescriptor.
		 * @return the parent ID of the ContextBeansDescriptor
		 */
		public String getParentId() {
			return this.parentId;
		}

		/**
		 * Returns a map of bean names to their corresponding BeanDescriptor objects.
		 * @return a map of bean names to BeanDescriptor objects
		 */
		public Map<String, BeanDescriptor> getBeans() {
			return this.beans;
		}

		/**
		 * Returns a ContextBeansDescriptor object describing the beans in the given
		 * ConfigurableApplicationContext.
		 * @param context the ConfigurableApplicationContext to describe
		 * @return a ContextBeansDescriptor object describing the beans in the given
		 * context, or null if the context is null
		 */
		private static ContextBeansDescriptor describing(ConfigurableApplicationContext context) {
			if (context == null) {
				return null;
			}
			ConfigurableApplicationContext parent = getConfigurableParent(context);
			return new ContextBeansDescriptor(describeBeans(context.getBeanFactory()),
					(parent != null) ? parent.getId() : null);
		}

		/**
		 * Returns a map of bean descriptors for all beans in the given bean factory.
		 * @param beanFactory the configurable listable bean factory
		 * @return a map of bean descriptors, where the key is the bean name and the value
		 * is the bean descriptor
		 */
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

		/**
		 * Returns a {@link BeanDescriptor} object that describes the specified bean.
		 * @param name the name of the bean
		 * @param definition the {@link BeanDefinition} object representing the bean's
		 * definition
		 * @param factory the {@link ConfigurableListableBeanFactory} object used to
		 * retrieve information about the bean
		 * @return a {@link BeanDescriptor} object containing information about the bean
		 */
		private static BeanDescriptor describeBean(String name, BeanDefinition definition,
				ConfigurableListableBeanFactory factory) {
			return new BeanDescriptor(factory.getAliases(name), definition.getScope(), factory.getType(name),
					definition.getResourceDescription(), factory.getDependenciesForBean(name));
		}

		/**
		 * Checks if a bean is eligible based on its name, definition, and bean factory.
		 * @param beanName the name of the bean
		 * @param bd the bean definition
		 * @param bf the configurable bean factory
		 * @return true if the bean is eligible, false otherwise
		 */
		private static boolean isBeanEligible(String beanName, BeanDefinition bd, ConfigurableBeanFactory bf) {
			return (bd.getRole() != BeanDefinition.ROLE_INFRASTRUCTURE
					&& (!bd.isLazyInit() || bf.containsSingleton(beanName)));
		}

	}

	/**
	 * Description of a bean.
	 */
	public static final class BeanDescriptor {

		private final String[] aliases;

		private final String scope;

		private final Class<?> type;

		private final String resource;

		private final String[] dependencies;

		/**
		 * Constructs a new BeanDescriptor with the specified aliases, scope, type,
		 * resource, and dependencies.
		 * @param aliases the aliases for the bean
		 * @param scope the scope of the bean (default is singleton)
		 * @param type the class type of the bean
		 * @param resource the resource associated with the bean
		 * @param dependencies the dependencies of the bean
		 */
		private BeanDescriptor(String[] aliases, String scope, Class<?> type, String resource, String[] dependencies) {
			this.aliases = aliases;
			this.scope = (StringUtils.hasText(scope) ? scope : BeanDefinition.SCOPE_SINGLETON);
			this.type = type;
			this.resource = resource;
			this.dependencies = dependencies;
		}

		/**
		 * Returns an array of aliases for this BeanDescriptor.
		 * @return an array of aliases
		 */
		public String[] getAliases() {
			return this.aliases;
		}

		/**
		 * Returns the scope of the BeanDescriptor.
		 * @return the scope of the BeanDescriptor
		 */
		public String getScope() {
			return this.scope;
		}

		/**
		 * Returns the type of the bean.
		 * @return the type of the bean
		 */
		public Class<?> getType() {
			return this.type;
		}

		/**
		 * Returns the resource associated with this BeanDescriptor.
		 * @return the resource associated with this BeanDescriptor
		 */
		public String getResource() {
			return this.resource;
		}

		/**
		 * Returns an array of dependencies.
		 * @return an array of dependencies
		 */
		public String[] getDependencies() {
			return this.dependencies;
		}

	}

}
