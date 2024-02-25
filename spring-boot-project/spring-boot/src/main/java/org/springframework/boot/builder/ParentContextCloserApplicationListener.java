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

package org.springframework.boot.builder;

import java.lang.ref.WeakReference;

import org.springframework.beans.BeansException;
import org.springframework.boot.builder.ParentContextApplicationContextInitializer.ParentContextAvailableEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.Ordered;
import org.springframework.util.ObjectUtils;

/**
 * Listener that closes the application context if its parent is closed. It listens for
 * refresh events and grabs the current context from there, and then listens for closed
 * events and propagates it down the hierarchy.
 *
 * @author Dave Syer
 * @author Eric Bottard
 * @since 1.0.0
 */
public class ParentContextCloserApplicationListener
		implements ApplicationListener<ParentContextAvailableEvent>, ApplicationContextAware, Ordered {

	private final int order = Ordered.LOWEST_PRECEDENCE - 10;

	private ApplicationContext context;

	/**
	 * Returns the order value of this ParentContextCloserApplicationListener.
	 * @return the order value of this ParentContextCloserApplicationListener
	 */
	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Sets the application context for this ParentContextCloserApplicationListener.
	 * @param context the application context to be set
	 * @throws BeansException if an error occurs while setting the application context
	 */
	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}

	/**
	 * This method is called when a ParentContextAvailableEvent is triggered. It installs
	 * a listener in the parent application context if necessary.
	 * @param event The ParentContextAvailableEvent that was triggered
	 */
	@Override
	public void onApplicationEvent(ParentContextAvailableEvent event) {
		maybeInstallListenerInParent(event.getApplicationContext());
	}

	/**
	 * Installs a listener in the parent context to close the child context.
	 * @param child The child context to install the listener for.
	 */
	private void maybeInstallListenerInParent(ConfigurableApplicationContext child) {
		if (child == this.context && child.getParent() instanceof ConfigurableApplicationContext parent) {
			parent.addApplicationListener(createContextCloserListener(child));
		}
	}

	/**
	 * Subclasses may override to create their own subclass of ContextCloserListener. This
	 * still enforces the use of a weak reference.
	 * @param child the child context
	 * @return the {@link ContextCloserListener} to use
	 */
	protected ContextCloserListener createContextCloserListener(ConfigurableApplicationContext child) {
		return new ContextCloserListener(child);
	}

	/**
	 * {@link ApplicationListener} to close the context.
	 */
	protected static class ContextCloserListener implements ApplicationListener<ContextClosedEvent> {

		private final WeakReference<ConfigurableApplicationContext> childContext;

		/**
		 * Constructs a new ContextCloserListener with the specified child context.
		 * @param childContext the child context to be associated with the listener
		 */
		public ContextCloserListener(ConfigurableApplicationContext childContext) {
			this.childContext = new WeakReference<>(childContext);
		}

		/**
		 * This method is called when the application context is closed. It checks if the
		 * child context is active and if it is the parent of the closed context. If both
		 * conditions are met, it closes the child context.
		 * @param event The event triggered when the application context is closed.
		 */
		@Override
		public void onApplicationEvent(ContextClosedEvent event) {
			ConfigurableApplicationContext context = this.childContext.get();
			if ((context != null) && (event.getApplicationContext() == context.getParent()) && context.isActive()) {
				context.close();
			}
		}

		/**
		 * Compares this ContextCloserListener object to the specified object. The result
		 * is true if and only if the argument is not null and is a ContextCloserListener
		 * object that represents the same child context as this object.
		 * @param obj the object to compare this ContextCloserListener against
		 * @return true if the given object represents a ContextCloserListener equivalent
		 * to this ContextCloserListener, false otherwise
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (obj instanceof ContextCloserListener other) {
				return ObjectUtils.nullSafeEquals(this.childContext.get(), other.childContext.get());
			}
			return super.equals(obj);
		}

		/**
		 * Returns a hash code value for the object. This method overrides the hashCode()
		 * method in the Object class.
		 * @return the hash code value for the object
		 */
		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(this.childContext.get());
		}

	}

}
