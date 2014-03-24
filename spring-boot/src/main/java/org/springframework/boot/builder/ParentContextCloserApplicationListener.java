/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.builder;

import java.lang.ref.WeakReference;

import org.springframework.boot.builder.ParentContextApplicationContextInitializer.ParentContextAvailableEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.Ordered;

/**
 * Listener that closes the application context if its parent is closed. It listens for refresh events and grabs the
 * current context from there, and then listens for closed events and propagates it down the hierarchy.
 * 
 * @author Dave Syer
 */
public class ParentContextCloserApplicationListener implements ApplicationListener<ParentContextAvailableEvent>,
		Ordered {

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 10;
	}

	@Override
	public final void onApplicationEvent(ParentContextAvailableEvent event) {
		maybeInstallListenerInParent(event.getApplicationContext());
	}

	private void maybeInstallListenerInParent(ConfigurableApplicationContext child) {
		if (child.getParent() instanceof ConfigurableApplicationContext) {
			ConfigurableApplicationContext parent = (ConfigurableApplicationContext) child.getParent();
			parent.addApplicationListener(createContextCloserListener(child));
		}
	}

	/**
	 * Subclasses may override to create their own subclass of ContextCloserListener. This still enforces the use of a
	 * weak reference.
	 */
	protected ContextCloserListener createContextCloserListener(ConfigurableApplicationContext child) {
		return new ContextCloserListener(child);
	}

	protected static class ContextCloserListener implements ApplicationListener<ContextClosedEvent> {

		private WeakReference<ConfigurableApplicationContext> contextRef;

		public ContextCloserListener(ConfigurableApplicationContext context) {
			this.contextRef = new WeakReference<ConfigurableApplicationContext>(context);

		}

		@Override
		public void onApplicationEvent(ContextClosedEvent event) {
			ConfigurableApplicationContext context = contextRef.get();
			if (context != null && event.getApplicationContext() == context.getParent() && context.isActive()) {
				context.close();
			}
		}

	}

}
