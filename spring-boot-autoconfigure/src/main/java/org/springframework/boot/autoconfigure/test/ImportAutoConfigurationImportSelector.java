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

package org.springframework.boot.autoconfigure.test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.boot.autoconfigure.EnableAutoConfigurationImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Variant of {@link EnableAutoConfigurationImportSelector} for
 * {@link ImportAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class ImportAutoConfigurationImportSelector
		extends EnableAutoConfigurationImportSelector {

	@Override
	protected Class<?> getAnnotationClass() {
		return ImportAutoConfiguration.class;
	}

	@Override
	protected Set<String> getExclusions(AnnotationMetadata metadata,
			AnnotationAttributes attributes) {
		return Collections.emptySet();
	}

	@Override
	protected List<String> getCandidateConfigurations(AnnotationMetadata metadata,
			AnnotationAttributes attributes) {
		return asList(attributes, "value");
	}

}
