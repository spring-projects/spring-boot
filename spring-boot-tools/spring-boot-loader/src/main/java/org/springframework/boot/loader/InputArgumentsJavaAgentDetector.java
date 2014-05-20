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

package org.springframework.boot.loader;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A {@link JavaAgentDetector} that detects jars supplied via the {@code -javaagent} JVM
 * input argument.
 *
 * @author Andy Wilkinson
 * @since 1.1.0
 */
public class InputArgumentsJavaAgentDetector implements JavaAgentDetector {

	private static final String JAVA_AGENT_PREFIX = "-javaagent:";

	private final Set<URL> javaAgentJars;

	public InputArgumentsJavaAgentDetector() {
		this(getInputArguments());
	}

	InputArgumentsJavaAgentDetector(List<String> inputArguments) {
		this.javaAgentJars = getJavaAgentJars(inputArguments);
	}

	private static List<String> getInputArguments() {
		try {
			return AccessController.doPrivileged(new PrivilegedAction<List<String>>() {
				@Override
				public List<String> run() {
					return ManagementFactory.getRuntimeMXBean().getInputArguments();
				}
			});
		}
		catch (Exception ex) {
			return Collections.<String> emptyList();
		}
	}

	private Set<URL> getJavaAgentJars(List<String> inputArguments) {
		Set<URL> javaAgentJars = new HashSet<URL>();
		for (String argument : inputArguments) {
			String path = getJavaAgentJarPath(argument);
			if (path != null) {
				try {
					javaAgentJars.add(new File(path).getCanonicalFile().toURI().toURL());
				}
				catch (IOException ex) {
					throw new IllegalStateException(
							"Failed to determine canonical path of Java agent at path '"
									+ path + "'");
				}
			}
		}
		return javaAgentJars;
	}

	private String getJavaAgentJarPath(String arg) {
		if (arg.startsWith(JAVA_AGENT_PREFIX)) {
			String path = arg.substring(JAVA_AGENT_PREFIX.length());
			int equalsIndex = path.indexOf('=');
			if (equalsIndex > -1) {
				path = path.substring(0, equalsIndex);
			}
			return path;
		}

		return null;
	}

	@Override
	public boolean isJavaAgentJar(URL url) {
		return this.javaAgentJars.contains(url);
	}

}
