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

package org.springframework.boot.strap.logging;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ClassUtils;

/**
 * Abstract base class for {@link LoggingSystem} implementations.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public abstract class AbstractLoggingSystem extends LoggingSystem {

	private final ClassLoader classLoader;

	private final String[] paths;

	public AbstractLoggingSystem(ClassLoader classLoader, String... paths) {
		this.classLoader = classLoader;
		this.paths = paths.clone();
	}

	protected final ClassLoader getClassLoader() {
		return this.classLoader;
	}

	@Override
	public void beforeInitialize() {
		initializeWithSensibleDefaults();
	}

	@Override
	public void initialize() {
		for (String path : this.paths) {
			ClassPathResource resource = new ClassPathResource(path, this.classLoader);
			if (resource.exists()) {
				initialize("classpath:" + path);
				return;
			}
		}
		initializeWithSensibleDefaults();
	}

	protected void initializeWithSensibleDefaults() {
		initialize(getPackagedConfigFile(this.paths[this.paths.length - 1]));
	}

	protected final String getPackagedConfigFile(String fileName) {
		String defaultPath = ClassUtils.getPackageName(getClass());
		defaultPath = defaultPath.replace(".", "/");
		defaultPath = defaultPath + "/" + fileName;
		defaultPath = "classpath:" + defaultPath;
		return defaultPath;
	}

}
