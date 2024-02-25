/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.context;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationContextInitializer} that sets the Spring
 * {@link ApplicationContext#getId() ApplicationContext ID}. The
 * {@code spring.application.name} property is used to create the ID. If the property is
 * not set {@code application} is used.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @since 1.0.0
 */
public class ContextIdApplicationContextInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

	private int order = Ordered.LOWEST_PRECEDENCE - 10;

	/**
     * Sets the order of the ContextIdApplicationContextInitializer.
     * 
     * @param order the order value to set
     */
    public void setOrder(int order) {
		this.order = order;
	}

	/**
     * Returns the order of this ContextIdApplicationContextInitializer.
     *
     * @return the order of this ContextIdApplicationContextInitializer
     */
    @Override
	public int getOrder() {
		return this.order;
	}

	/**
     * Initializes the application context by setting the context ID and registering it as a singleton bean.
     * 
     * @param applicationContext the configurable application context to be initialized
     */
    @Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		ContextId contextId = getContextId(applicationContext);
		applicationContext.setId(contextId.getId());
		applicationContext.getBeanFactory().registerSingleton(ContextId.class.getName(), contextId);
	}

	/**
     * Retrieves the context ID for the given application context.
     * If the application context has a parent and the parent contains a bean of type ContextId,
     * it creates a child ID by calling the createChildId() method on the parent's ContextId bean.
     * Otherwise, it creates a new ContextId using the application ID from the environment.
     *
     * @param applicationContext the configurable application context
     * @return the context ID
     */
    private ContextId getContextId(ConfigurableApplicationContext applicationContext) {
		ApplicationContext parent = applicationContext.getParent();
		if (parent != null && parent.containsBean(ContextId.class.getName())) {
			return parent.getBean(ContextId.class).createChildId();
		}
		return new ContextId(getApplicationId(applicationContext.getEnvironment()));
	}

	/**
     * Returns the application ID based on the given environment.
     * 
     * @param environment the configurable environment
     * @return the application ID
     */
    private String getApplicationId(ConfigurableEnvironment environment) {
		String name = environment.getProperty("spring.application.name");
		return StringUtils.hasText(name) ? name : "application";
	}

	/**
	 * The ID of a context.
	 */
	static class ContextId {

		private final AtomicLong children = new AtomicLong();

		private final String id;

		/**
         * Constructs a new ContextId object with the specified id.
         * 
         * @param id the id to be set for the ContextId object
         */
        ContextId(String id) {
			this.id = id;
		}

		/**
         * Creates a new child ContextId by appending the current ContextId with a hyphen and the incremented value of the children counter.
         * 
         * @return the newly created child ContextId
         */
        ContextId createChildId() {
			return new ContextId(this.id + "-" + this.children.incrementAndGet());
		}

		/**
         * Returns the ID of the context.
         *
         * @return the ID of the context
         */
        String getId() {
			return this.id;
		}

	}

}
