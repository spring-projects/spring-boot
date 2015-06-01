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

package org.springframework.boot.actuate.endpoint;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * {@link Endpoint} to shutdown the {@link ApplicationContext}.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 */
@ConfigurationProperties(prefix = "endpoints.shutdown", ignoreUnknownFields = false)
public class ShutdownEndpoint extends AbstractEndpoint<Map<String, Object>> implements
		ApplicationContextAware {

	private ConfigurableApplicationContext context;

	/**
	 * Create a new {@link ShutdownEndpoint} instance.
	 */
	public ShutdownEndpoint() {
		super("shutdown", true, false);
	}

	@Override
	public Map<String, Object> invoke() {

		if (this.context == null) {
			return Collections.<String, Object> singletonMap("message",
					"No context to shutdown.");
		}

		try {
			return Collections.<String, Object> singletonMap("message",
					"Shutting down, bye...");
		}
		finally {

			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(500L);
					}
					catch (InterruptedException ex) {
						// Swallow exception and continue
					}
					ShutdownEndpoint.this.context.close();
				}
			}).start();

		}
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		if (context instanceof ConfigurableApplicationContext) {
			this.context = (ConfigurableApplicationContext) context;
		}
	}

}
