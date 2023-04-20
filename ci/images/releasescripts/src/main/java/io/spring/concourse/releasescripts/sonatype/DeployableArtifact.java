/*
 * Copyright 2012-2021 the original author or authors.
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

package io.spring.concourse.releasescripts.sonatype;

import org.springframework.core.io.Resource;

/**
 * An artifact that can be deployed.
 *
 * @author Andy Wilkinson
 */
class DeployableArtifact {

	private final Resource resource;

	private final String path;

	DeployableArtifact(Resource resource, String path) {
		this.resource = resource;
		this.path = path;
	}

	Resource getResource() {
		return this.resource;
	}

	String getPath() {
		return this.path;
	}

}