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

package org.springframework.boot.context.embedded;

import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * A {@code ServletContextInitializer} that configures init parameters on the
 * {@code ServletContext}.
 *
 * @author Andy Wilkinson
 * @since 1.2.0
 * @see ServletContext#setInitParameter(String, String)
 */
public class InitParameterConfiguringServletContextInitializer
		implements ServletContextInitializer {

	private final Map<String, String> parameters;

	public InitParameterConfiguringServletContextInitializer(
			Map<String, String> parameters) {
		this.parameters = parameters;
	}

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		for (Entry<String, String> entry : this.parameters.entrySet()) {
			servletContext.setInitParameter(entry.getKey(), entry.getValue());
		}
	}

}
