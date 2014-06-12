/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.context.embedded.tomcat;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.core.StandardContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.util.Assert;

/**
 * Tomcat {@link LifecycleListener} that calls {@link ServletContextInitializer}s.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public class ServletContextInitializerLifecycleListener implements LifecycleListener {

	private static Log logger = LogFactory
			.getLog(ServletContextInitializerLifecycleListener.class);

	private final ServletContextInitializer[] initializers;
	private Exception startUpException;

	/**
	 * Create a new {@link ServletContextInitializerLifecycleListener} instance with the
	 * specified initializers.
	 * @param initializers the initializers to call
	 */
	public ServletContextInitializerLifecycleListener(
			ServletContextInitializer... initializers) {
		this.initializers = initializers;
	}

	public Exception getStartUpException() {
		return this.startUpException;
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
				catch (Exception ex) {
					this.startUpException = ex;
					// Prevent Tomcat from logging and re-throwing when we know we can
					// deal with it in the main thread, but log for information here.
					logger.error("Error starting Tomcat context: "
							+ ex.getClass().getName());
					break;
				}
			}
		}
	}

}
