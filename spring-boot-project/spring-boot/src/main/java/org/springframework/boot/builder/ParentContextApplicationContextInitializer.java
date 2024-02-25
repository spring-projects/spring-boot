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

package org.springframework.boot.builder;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;

/**
 * {@link ApplicationContextInitializer} for setting the parent context. Also publishes
 * {@link ParentContextAvailableEvent} when the context is refreshed to signal to other
 * listeners that the context is available and has a parent.
 *
 * @author Dave Syer
 * @since 1.0.0
 */
public class ParentContextApplicationContextInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

	private int order = Ordered.HIGHEST_PRECEDENCE;

	private final ApplicationContext parent;

	/**
     * Constructs a new ParentContextApplicationContextInitializer with the specified parent ApplicationContext.
     *
     * @param parent the parent ApplicationContext to set
     */
    public ParentContextApplicationContextInitializer(ApplicationContext parent) {
		this.parent = parent;
	}

	/**
     * Sets the order of the ParentContextApplicationContextInitializer.
     * 
     * @param order the order value to set
     */
    public void setOrder(int order) {
		this.order = order;
	}

	/**
     * Returns the order value of this ParentContextApplicationContextInitializer.
     *
     * @return the order value of this ParentContextApplicationContextInitializer
     */
    @Override
	public int getOrder() {
		return this.order;
	}

	/**
     * Initializes the application context.
     * 
     * @param applicationContext the configurable application context to be initialized
     */
    @Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		if (applicationContext != this.parent) {
			applicationContext.setParent(this.parent);
			applicationContext.addApplicationListener(EventPublisher.INSTANCE);
		}
	}

	/**
     * EventPublisher class.
     */
    private static final class EventPublisher implements ApplicationListener<ContextRefreshedEvent>, Ordered {

		private static final EventPublisher INSTANCE = new EventPublisher();

		/**
         * Returns the order of this EventPublisher.
         * 
         * @return the order of this EventPublisher
         */
        @Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}

		/**
         * This method is called when the application context is refreshed.
         * It checks if the context is an instance of ConfigurableApplicationContext and if it is the source of the event.
         * If both conditions are true, it publishes a ParentContextAvailableEvent to the context.
         * 
         * @param event The ContextRefreshedEvent that triggered this method.
         */
        @Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			ApplicationContext context = event.getApplicationContext();
			if (context instanceof ConfigurableApplicationContext && context == event.getSource()) {
				context.publishEvent(new ParentContextAvailableEvent((ConfigurableApplicationContext) context));
			}
		}

	}

	/**
	 * {@link ApplicationEvent} fired when a parent context is available.
	 */
	@SuppressWarnings("serial")
	public static class ParentContextAvailableEvent extends ApplicationEvent {

		/**
         * Constructs a new ParentContextAvailableEvent with the specified ConfigurableApplicationContext.
         * 
         * @param applicationContext the ConfigurableApplicationContext associated with this event
         */
        public ParentContextAvailableEvent(ConfigurableApplicationContext applicationContext) {
			super(applicationContext);
		}

		/**
         * Returns the application context associated with this event.
         * 
         * @return the application context
         */
        public ConfigurableApplicationContext getApplicationContext() {
			return (ConfigurableApplicationContext) getSource();
		}

	}

}
