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

package org.springframework.boot.autoconfigure.liquibase;

import javax.validation.constraints.NotNull;

import liquibase.integration.spring.SpringLiquibase;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties to configure {@link SpringLiquibase}.
 * 
 * @author Marcel Overdijk
 */
@ConfigurationProperties(prefix = "liquibase", ignoreUnknownFields = false)
public class LiquibaseProperties {

	@NotNull
	private String changeLog = "classpath:/db/changelog/db.changelog-master.yaml";

	private boolean checkChangeLogLocation = true;

	private String contexts;

	private String defaultSchema;

	private boolean dropFirst = false;

	private boolean shouldRun = true;

	public String getChangeLog() {
		return this.changeLog;
	}

	public void setChangeLog(String changeLog) {
		this.changeLog = changeLog;
	}

	public boolean isCheckChangeLogLocation() {
		return this.checkChangeLogLocation;
	}

	public void setCheckChangeLogLocation(boolean checkChangeLogLocation) {
		this.checkChangeLogLocation = checkChangeLogLocation;
	}

	public String getContexts() {
		return this.contexts;
	}

	public void setContexts(String contexts) {
		this.contexts = contexts;
	}

	public String getDefaultSchema() {
		return this.defaultSchema;
	}

	public void setDefaultSchema(String defaultSchema) {
		this.defaultSchema = defaultSchema;
	}

	public boolean isDropFirst() {
		return this.dropFirst;
	}

	public void setDropFirst(boolean dropFirst) {
		this.dropFirst = dropFirst;
	}

	public boolean isShouldRun() {
		return this.shouldRun;
	}

	public void setShouldRun(boolean shouldRun) {
		this.shouldRun = shouldRun;
	}
}
