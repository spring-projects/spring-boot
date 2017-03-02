/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.gradle.bundling;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.loader.tools.FileUtils;

/**
 * Encapsulates the configuration of the launch script for an executable jar or war.
 *
 * @author Andy Wilkinson
 */
public class LaunchScriptConfiguration implements Serializable {

	private boolean included = false;

	private final Map<String, String> properties = new HashMap<String, String>();

	private File script;

	public boolean isIncluded() {
		return this.included;
	}

	public void setIncluded(boolean included) {
		this.included = included;
	}

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public void properties(Map<String, String> properties) {
		this.properties.putAll(properties);
	}

	public File getScript() {
		return this.script;
	}

	public void setScript(File script) {
		this.script = script;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.included ? 1231 : 1237);
		result = prime * result
				+ ((this.properties == null) ? 0 : this.properties.hashCode());
		result = prime * result + ((this.script == null) ? 0 : this.script.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		LaunchScriptConfiguration other = (LaunchScriptConfiguration) obj;
		if (this.included != other.included) {
			return false;
		}
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

}
