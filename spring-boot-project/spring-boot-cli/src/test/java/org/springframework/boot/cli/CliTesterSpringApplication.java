/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.cli;

import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.context.WebServerPortFileWriter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ClassUtils;

/**
 * Custom {@link SpringApplication} used by {@link CliTester}.
 *
 * @author Andy Wilkinson
 */
public class CliTesterSpringApplication extends SpringApplication {

	static {
		if (ClassUtils.isPresent("org.apache.catalina.webresources.TomcatURLStreamHandlerFactory",
				CliTesterSpringApplication.class.getClassLoader())) {
			TomcatURLStreamHandlerFactory.disable();
		}
	}

	public CliTesterSpringApplication(Class<?>... sources) {
		super(sources);
	}

	@Override
	protected void postProcessApplicationContext(ConfigurableApplicationContext context) {
		context.addApplicationListener(new WebServerPortFileWriter());
	}

}
