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

package org.springframework.boot.test.autoconfigure.restdocs;

import java.util.Map;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.restdocs.ManualRestDocumentation;
import org.springframework.util.StringUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} used by
 * {@link AutoConfigureRestDocs @AutoConfigureRestDocs}.
 *
 * @author Andy Wilkinson
 * @see AutoConfigureRestDocs
 */
class RestDocumentationContextProviderRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		Map<String, Object> annotationAttributes = importingClassMetadata
				.getAnnotationAttributes(AutoConfigureRestDocs.class.getName());
		BeanDefinitionBuilder definitionBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(ManualRestDocumentation.class);
		String outputDir = (String) annotationAttributes.get("outputDir");
		if (StringUtils.hasText(outputDir)) {
			definitionBuilder.addConstructorArgValue(outputDir);
		}
		registry.registerBeanDefinition(ManualRestDocumentation.class.getName(), definitionBuilder.getBeanDefinition());
	}

}
