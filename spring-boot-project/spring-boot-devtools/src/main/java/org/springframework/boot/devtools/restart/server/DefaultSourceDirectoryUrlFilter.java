/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.devtools.restart.server;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link SourceDirectoryUrlFilter} that attempts to match URLs
 * using common naming conventions.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public class DefaultSourceDirectoryUrlFilter implements SourceDirectoryUrlFilter {

	private static final String[] COMMON_ENDINGS = { "/target/classes", "/bin" };

	private static final Pattern URL_MODULE_PATTERN = Pattern.compile(".*\\/(.+)\\.jar");

	private static final Pattern VERSION_PATTERN = Pattern.compile("^-\\d+(?:\\.\\d+)*(?:[.-].+)?$");

	private static final Set<String> SKIPPED_PROJECTS = new HashSet<>(Arrays.asList("spring-boot",
			"spring-boot-devtools", "spring-boot-autoconfigure", "spring-boot-actuator", "spring-boot-starter"));

	@Override
	public boolean isMatch(String sourceDirectory, URL url) {
		String jarName = getJarName(url);
		if (!StringUtils.hasLength(jarName)) {
			return false;
		}
		return isMatch(sourceDirectory, jarName);
	}

	private String getJarName(URL url) {
		Matcher matcher = URL_MODULE_PATTERN.matcher(url.toString());
		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}

	private boolean isMatch(String sourceDirectory, String jarName) {
		sourceDirectory = stripTrailingSlash(sourceDirectory);
		sourceDirectory = stripCommonEnds(sourceDirectory);
		String[] directories = StringUtils.delimitedListToStringArray(sourceDirectory, "/");
		for (int i = directories.length - 1; i >= 0; i--) {
			if (isDirectoryMatch(directories[i], jarName)) {
				return true;
			}
		}
		return false;
	}

	private boolean isDirectoryMatch(String directory, String jarName) {
		if (!jarName.startsWith(directory) || SKIPPED_PROJECTS.contains(directory)) {
			return false;
		}
		String version = jarName.substring(directory.length());
		return version.isEmpty() || VERSION_PATTERN.matcher(version).matches();
	}

	private String stripTrailingSlash(String string) {
		if (string.endsWith("/")) {
			return string.substring(0, string.length() - 1);
		}
		return string;
	}

	private String stripCommonEnds(String string) {
		for (String ending : COMMON_ENDINGS) {
			if (string.endsWith(ending)) {
				return string.substring(0, string.length() - ending.length());
			}
		}
		return string;
	}

}
