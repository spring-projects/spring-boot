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

package org.springframework.autoconfigure.main;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.LogFactory;
import org.springframework.autoconfigure.EnableAutoConfiguration;
import org.springframework.bootstrap.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.SpringVersion;
import org.springframework.util.ClassUtils;

/**
 * Very simple main class that can be used to launch an application from sources (class,
 * package or XML). Useful for demos and testing, perhaps less for production use (where
 * the {@link SpringApplication} run methods are often more convenient).
 * 
 * @author Dave Syer
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan
public abstract class Spring {

	// FIXME can we delete this? is it used? does it belong here

	private static ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
			true);

	private static ApplicationContext context;

	/**
	 * @return the context if there is one
	 */
	public static ApplicationContext getApplicationContext() {
		return context;
	}

	/**
	 * A basic main that can be used to launch an application.
	 * 
	 * @param args command line arguments
	 * @see SpringApplication#run(Object[], String[])
	 * @see SpringApplication#run(Object, String...)
	 */
	public static void main(String[] args) throws Exception {

		List<String> strings = new ArrayList<String>();
		List<Object> sources = new ArrayList<Object>();

		for (String arg : args) {
			if (ClassUtils.isPresent(arg, null)) {
				sources.add(ClassUtils.forName(arg, null));
			}
			else if (arg.endsWith(".xml")) {
				sources.add(arg);
			}
			else if (!scanner.findCandidateComponents(arg).isEmpty()) {
				sources.add(arg);
			}
			else {
				strings.add(arg);
			}
		}

		if (sources.isEmpty()) {
			sources.add(Spring.class);
		}

		context = SpringApplication.run(sources.toArray(new Object[sources.size()]),
				strings.toArray(new String[strings.size()]));

		LogFactory.getLog(Spring.class).info(
				"Running Spring " + SpringVersion.getVersion() + " with sources: "
						+ sources);

	}

}
