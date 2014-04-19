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

package org.springframework.boot.test;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Listener that injects the server port (if one is discoverable from the application
 * context)into a field annotated with {@link Value @Value("dynamic.port")}.
 * 
 * @author Dave Syer
 */
public class EmbeddedServletContainerListener extends AbstractTestExecutionListener {

	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		ApplicationContext context = testContext.getApplicationContext();
		if (!(context instanceof EmbeddedWebApplicationContext)) {
			return;
		}
		EmbeddedWebApplicationContext embedded = (EmbeddedWebApplicationContext) context;
		Map<String, EmbeddedServletContainer> containers = embedded
				.getEmbeddedServletContainers();
		for (String name : containers.keySet()) {
			int port = containers.get(name).getPort();
			EnvironmentTestUtils.addEnvironment(embedded, "local." + name + ".port:"
					+ port);
		}
	}
}
