/*
 * Copyright 2012-2015 the original author or authors.
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
package org.springframework.boot.configurationprocessor;

import java.util.Set;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;

/**
 * Data object containing information about a finished build.
 *
 * @author Kris De Volder
 */
public class BuildResult {

	public final ConfigurationMetadata metadata;

	public final Set<String> processedTypes;

	public final boolean isIncremental;

	public BuildResult(boolean isIncremental, ConfigurationMetadata metadata,
			Set<String> processedTypes) {
		this.isIncremental = isIncremental;
		this.metadata = metadata;
		this.processedTypes = processedTypes;
	}

	public BuildResult(TestConfigurationMetadataAnnotationProcessor processor) {
		this(processor.isIncremental(), processor.getMetadata(),
				processor.processedSourceTypes);
	}

}
