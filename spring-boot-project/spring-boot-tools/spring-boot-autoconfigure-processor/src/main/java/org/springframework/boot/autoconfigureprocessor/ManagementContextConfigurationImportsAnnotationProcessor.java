/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigureprocessor;

import javax.annotation.processing.SupportedAnnotationTypes;

/**
 * Annotation processor to generate a
 * {@code META-INF/spring/org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration.imports}
 * file from {@code @ManagementContextConfiguration} annotations.
 *
 * @author Scott Frederick
 * @since 3.0.0
 */
@SupportedAnnotationTypes({ "org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration" })
public class ManagementContextConfigurationImportsAnnotationProcessor extends AbstractImportsAnnotationProcessor {

	@Override
	protected String getImportsFilePath() {
		return "META-INF/spring/org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration.imports";
	}

}
