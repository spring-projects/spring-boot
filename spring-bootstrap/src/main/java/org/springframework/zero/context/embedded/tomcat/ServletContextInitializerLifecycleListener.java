/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.zero.context.embedded.tomcat;

import javax.servlet.ServletException;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.core.StandardContext;
import org.springframework.util.Assert;
import org.springframework.zero.context.embedded.ServletContextInitializer;

/**
 * Tomcat {@link LifecycleListener} that calls {@link ServletContextInitializer}s.
 * 
 * @author Phillip Webb
 */
public class ServletContextInitializerLifecycleListener implements LifecycleListener {

	private ServletContextInitializer[] initializers;

	/**
	 * Create a new {@link ServletContextInitializerLifecycleListener} instance with the
	 * specified initializers.
	 * @param initializers the initializers to call
	 */
	public ServletContextInitializerLifecycleListener(
			ServletContextInitializer... initializers) {
		this.initializers = initializers;
	}

	@Override
	public void lifecycleEvent(LifecycleEvent event) {
		if (Lifecycle.CONFIGURE_START_EVENT.equals(event.getType())) {
			Assert.isInstanceOf(StandardContext.class, event.getSource());
			StandardContext standardContext = (StandardContext) event.getSource();
			for (ServletContextInitializer initializer : this.initializers) {
				try {
					initializer.onStartup(standardContext.getServletContext());
				}
				catch (ServletException ex) {
					throw new IllegalStateException(ex);
				}
			}
		}
	}

}
