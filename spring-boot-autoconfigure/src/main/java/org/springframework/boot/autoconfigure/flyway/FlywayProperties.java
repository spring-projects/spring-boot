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

package org.springframework.boot.autoconfigure.flyway;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Flyway database migrations.
 * 
 * @author Dave Syer
 */
@ConfigurationProperties(prefix = "flyway", ignoreUnknownFields = false)
public class FlywayProperties {

	private List<String> locations = Arrays.asList("db/migrations");

	private List<String> schemas = new ArrayList<String>();

	private String prefix = "V";

	private String suffix = ".sql";

	private String initVersion = "1";

	private boolean checkLocation = false;

	private boolean enabled = true;

	public String getPrefix() {
		return this.prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getSuffix() {
		return this.suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public String getInitVersion() {
		return this.initVersion;
	}

	public void setInitVersion(String initVersion) {
		this.initVersion = initVersion;
	}

	public void setLocations(List<String> locations) {
		this.locations = locations;
	}

	public List<String> getLocations() {
		return this.locations;
	}

	public List<String> getSchemas() {
		return this.schemas;
	}

	public void setSchemas(List<String> schemas) {
		this.schemas = schemas;
	}

	public void setCheckLocation(boolean checkLocation) {
		this.checkLocation = checkLocation;
	}

	public boolean isCheckLocation() {
		return this.checkLocation;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
