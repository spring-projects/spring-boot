/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.gradle.api.Project;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;

import org.springframework.boot.loader.tools.FileUtils;

/**
 * Encapsulates the configuration of the launch script for an executable jar or war.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@SuppressWarnings("serial")
public class LaunchScriptConfiguration implements Serializable {

	private static final Pattern WHITE_SPACE_PATTERN = Pattern.compile("\\s+");

	private static final Pattern LINE_FEED_PATTERN = Pattern.compile("\n");

	private final Map<String, String> properties = new HashMap<>();

	private File script;

	public LaunchScriptConfiguration() {
	}

	LaunchScriptConfiguration(AbstractArchiveTask archiveTask) {
		Project project = archiveTask.getProject();
		putIfMissing(this.properties, "initInfoProvides", archiveTask.getBaseName());
		putIfMissing(this.properties, "initInfoShortDescription", removeLineBreaks(project.getDescription()),
				archiveTask.getBaseName());
		putIfMissing(this.properties, "initInfoDescription", augmentLineBreaks(project.getDescription()),
				archiveTask.getBaseName());
	}

	/**
	 * Returns the properties that are applied to the launch script when it's being
	 * including in the executable archive.
	 * @return the properties
	 */
	public Map<String, String> getProperties() {
		return this.properties;
	}

	/**
	 * Sets the properties that are applied to the launch script when it's being including
	 * in the executable archive.
	 * @param properties the properties
	 */
	public void properties(Map<String, String> properties) {
		this.properties.putAll(properties);
	}

	/**
	 * Returns the script {@link File} that will be included in the executable archive.
	 * When {@code null}, the default launch script will be used.
	 * @return the script file
	 */
	public File getScript() {
		return this.script;
	}

	/**
	 * Sets the script {@link File} that will be included in the executable archive. When
	 * {@code null}, the default launch script will be used.
	 * @param script the script file
	 */
	public void setScript(File script) {
		this.script = script;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		LaunchScriptConfiguration other = (LaunchScriptConfiguration) obj;
		if (!this.properties.equals(other.properties)) {
			return false;
		}
		if (this.script == null) {
			if (other.script != null) {
				return false;
			}
		}
		else if (!this.script.equals(other.script)) {
			return false;
		}
		else if (!equalContents(this.script, other.script)) {
			return false;
		}
		return true;
	}

	private boolean equalContents(File one, File two) {
		try {
			return FileUtils.sha1Hash(one).equals(FileUtils.sha1Hash(two));
		}
		catch (IOException ex) {
			return false;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.properties == null) ? 0 : this.properties.hashCode());
		result = prime * result + ((this.script == null) ? 0 : this.script.hashCode());
		return result;
	}

	private String removeLineBreaks(String string) {
		return (string != null) ? WHITE_SPACE_PATTERN.matcher(string).replaceAll(" ") : null;
	}

	private String augmentLineBreaks(String string) {
		return (string != null) ? LINE_FEED_PATTERN.matcher(string).replaceAll("\n#  ") : null;
	}

	private void putIfMissing(Map<String, String> properties, String key, String... valueCandidates) {
		if (!properties.containsKey(key)) {
			for (String candidate : valueCandidates) {
				if (candidate != null && !candidate.isEmpty()) {
					properties.put(key, candidate);
					return;
				}
			}
		}
	}

}
