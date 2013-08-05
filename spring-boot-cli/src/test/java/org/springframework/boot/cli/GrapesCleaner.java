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

package org.springframework.boot.cli;

import java.io.File;

import org.springframework.boot.cli.command.CleanCommand;

/**
 * @author Dave Syer
 */
public class GrapesCleaner {

	// FIXME get the version
	private static final String VERSION = "0.5.0.BUILD-SNAPSHOT";

	public static void cleanIfNecessary() throws Exception {
		File installedJar = new File(getMavenRepository(), String.format(
				"org/springframework/boot/spring-boot/%s/spring-boot-%s.jar", VERSION,
				VERSION));
		File grapesJar = new File(getGrapesCache(), String.format(
				"org.springframework.boot/spring-boot/jars/spring-boot-%s.jar", VERSION));
		if (!VERSION.contains("SNAPSHOT") || installedJar.exists() && grapesJar.exists()
				&& installedJar.lastModified() <= grapesJar.lastModified()) {
			return;
		}
		new CleanCommand().run();
	}

	private static File getMavenRepository() {
		return new File(System.getProperty("user.home"), ".m2/repository");
	}

	private static File getGrapesCache() {
		return new File(System.getProperty("user.home"), ".groovy/grapes");
	}

}
