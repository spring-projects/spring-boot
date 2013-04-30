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
package org.springframework.bootstrap.service.shutdown;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bootstrap.service.properties.ContainerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.support.ServletRequestHandledEvent;

/**
 * @author Dave Syer
 * 
 */
@Controller
public class ShutdownEndpoint implements ApplicationContextAware,
		ApplicationListener<ServletRequestHandledEvent> {

	private static Log logger = LogFactory.getLog(ShutdownEndpoint.class);

	private ConfigurableApplicationContext context;

	@Autowired
	private ContainerProperties configuration = new ContainerProperties();

	@RequestMapping(value = "${endpoints.shutdown.path:/shutdown}", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> shutdown() {
		if (this.configuration.isAllowShutdown()) {
			return Collections.<String, Object> singletonMap("message",
					"Shutting down, bye...");
		} else {
			return Collections.<String, Object> singletonMap("message",
					"Shutdown not enabled, sorry.");
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		if (context instanceof ConfigurableApplicationContext) {
			this.context = (ConfigurableApplicationContext) context;
		}
	}

	@Override
	public void onApplicationEvent(ServletRequestHandledEvent event) {

		if (this.context != null && this.configuration.isAllowShutdown()) {

			new Thread(new Runnable() {
				@Override
				public void run() {
					logger.info("Shutting down Spring in response to admin request");
					ConfigurableApplicationContext context = ShutdownEndpoint.this.context;
					ApplicationContext parent = context.getParent();
					context.close();
					if (parent != null
							&& parent instanceof ConfigurableApplicationContext) {
						context = (ConfigurableApplicationContext) parent;
						context.close();
						parent = context.getParent();
					}
				}
			}).start();

		}
	}

}
