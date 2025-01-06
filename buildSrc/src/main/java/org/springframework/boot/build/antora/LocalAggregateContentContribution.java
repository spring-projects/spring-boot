/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.build.antora;

import org.gradle.api.Project;
import org.gradle.api.file.CopySpec;

import org.springframework.boot.build.antora.Extensions.AntoraExtensionsConfiguration.ZipContentsCollector.AlwaysInclude;

/**
 * A contribution of aggregate content that cannot be consumed by other projects.
 *
 * @author Andy Wilkinson
 */
class LocalAggregateContentContribution extends ContentContribution {

	protected LocalAggregateContentContribution(Project project, String name) {
		super(project, name, "local-aggregate");
	}

	@Override
	void produceFrom(CopySpec copySpec) {
		super.configureProduction(copySpec);
		configurePlaybookGeneration(this::addToAlwaysInclude);
	}

	private void addToAlwaysInclude(GenerateAntoraPlaybook task) {
		task.getAntoraExtensions()
			.getZipContentsCollector()
			.getAlwaysInclude()
			.add(new AlwaysInclude(getName(), "local-aggregate-content"));
	}

}
