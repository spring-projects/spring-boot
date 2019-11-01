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

package io.spring.concourse.releasescripts.artifactory.payload;

/**
 * Represents the response from Artifactory's buildInfo endpoint.
 *
 * @author Madhura Bhave
 */
public class BuildInfoResponse {

	private BuildInfo buildInfo;

	public BuildInfo getBuildInfo() {
		return this.buildInfo;
	}

	public void setBuildInfo(BuildInfo buildInfo) {
		this.buildInfo = buildInfo;
	}

	public static class BuildInfo {

		private String name;

		private String number;

		private String version;

		private Status[] statuses;

		private Module[] modules;

		public Status[] getStatuses() {
			return this.statuses;
		}

		public void setStatuses(Status[] statuses) {
			this.statuses = statuses;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getNumber() {
			return this.number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

		public Module[] getModules() {
			return this.modules;
		}

		public void setModules(Module[] modules) {
			this.modules = modules;
		}

		public String getVersion() {
			return this.version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

	}

	public static class Status {

		private String repository;

		public String getRepository() {
			return this.repository;
		}

		public void setRepository(String repository) {
			this.repository = repository;
		}

	}

	public static class Module {

		private String id;

		public String getId() {
			return this.id;
		}

		public void setId(String id) {
			this.id = id;
		}

	}

}
