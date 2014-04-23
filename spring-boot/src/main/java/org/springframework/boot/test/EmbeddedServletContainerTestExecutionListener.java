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

package org.springframework.boot.test;

import java.util.Map;

import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Listener that injects the server port into an {@link Environment} property named
 * {@literal local.&lt;server&gt;.port}. Useful when the server is running on a dynamic
 * port.
 * 
 * @author Dave Syer
 */
public class EmbeddedServletContainerTestExecutionListener extends
		AbstractTestExecutionListener {

	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		ApplicationContext context = testContext.getApplicationContext();
		if (context instanceof EmbeddedWebApplicationContext) {
			prepareTestInstance((EmbeddedWebApplicationContext) context);
		}
	}

	private void prepareTestInstance(EmbeddedWebApplicationContext context) {
		for (Map.Entry<String, EmbeddedServletContainer> entry : context
				.getEmbeddedServletContainers().entrySet()) {
			EnvironmentTestUtils.addEnvironment(context, "local." + entry.getKey()
					+ ".port:" + entry.getValue().getPort());
		}
	}
}
