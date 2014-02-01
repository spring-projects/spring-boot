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

package org.springframework.boot;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ProfileDetector} that attempts to detect when the application is being developed
 * and adds a 'development' profile.
 * 
 * @author Phillip Webb
 */
public class DevelopmentProfileDetector implements ProfileDetector {

	private final Log logger = LogFactory.getLog(getClass());

	private static final String DEFAULT_PROFILE_NAME = "development";

	private static final String EXECUTABLE_JAR_CLASS = "org.springframework.boot.loader.Launcher";

	private static final String[] DEVELOPMENT_TIME_FILES = { "pom.xml", "build.gradle",
			"build.xml" };

	private final String profileName;

	public DevelopmentProfileDetector() {
		this(DEFAULT_PROFILE_NAME);
	}

	public DevelopmentProfileDetector(String profileName) {
		Assert.notNull(profileName, "ProfileName must not be null");
		this.profileName = profileName;
	}

	@Override
	public void addDetectedProfiles(ConfigurableEnvironment environment) {
		if (!isPackageAsJar() && isRunningInDevelopmentDirectory()) {
			environment.addActiveProfile(this.profileName);
		}
	}

	protected boolean isPackageAsJar() {
		if (ClassUtils.isPresent(EXECUTABLE_JAR_CLASS, null)) {
			this.logger.debug("Development profile not detected: "
					+ "running inside executable jar");
			return true;
		}
		String command = System.getProperty("sun.java.command");
		if (StringUtils.hasLength(command) && command.toLowerCase().contains(".jar")) {
			this.logger.debug("Development profile not detected: started from a jar");
			return true;
		}
		return false;
	}

	private boolean isRunningInDevelopmentDirectory() {
		File userDir = getUserDir();
		if (userDir != null && userDir.exists()) {
			for (String developementTimeFile : DEVELOPMENT_TIME_FILES) {
				if (new File(userDir, developementTimeFile).exists()) {
					this.logger.debug("Development profile detected: file "
							+ developementTimeFile + " present");
					return true;
				}
			}
		}
		return false;
	}

	protected File getUserDir() {
		String userDir = System.getProperty("user.dir");
		return (userDir == null ? null : new File(userDir));
	}
}
