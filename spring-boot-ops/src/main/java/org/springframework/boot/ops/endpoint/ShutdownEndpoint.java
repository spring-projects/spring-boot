/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.ops.endpoint;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ops.properties.ManagementServerProperties;
import org.springframework.boot.strap.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * {@link ActionEndpoint} to shutdown the {@link ApplicationContext}.
 * 
 * @author Dave Syer
 */
@ConfigurationProperties(name = "endpoints.shutdown", ignoreUnknownFields = false)
public class ShutdownEndpoint extends AbstractEndpoint<Map<String, Object>> implements
		ApplicationContextAware, ActionEndpoint<Map<String, Object>> {

	private ConfigurableApplicationContext context;

	@Autowired(required = false)
	private ManagementServerProperties configuration = new ManagementServerProperties();

	/**
	 * Create a new {@link ShutdownEndpoint} instance.
	 */
	public ShutdownEndpoint() {
		super("/shutdown");
	}

	@Override
	public Map<String, Object> invoke() {
		if (this.configuration == null || !this.configuration.isAllowShutdown()
				|| this.context == null) {
			return Collections.<String, Object> singletonMap("message",
					"Shutdown not enabled, sorry.");
		}

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

		return Collections.<String, Object> singletonMap("message",
				"Shutting down, bye...");
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		if (context instanceof ConfigurableApplicationContext) {
			this.context = (ConfigurableApplicationContext) context;
		}
	}

}
