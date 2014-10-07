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

package org.springframework.boot.test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.springframework.asm.Opcodes;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import static org.junit.Assert.assertEquals;

/**
 * @author Andy Wilkinson
 */
public abstract class AbstractConfigurationClassTests {

	private ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	@Test
	public void allBeanMethodsArePublic() throws IOException, ClassNotFoundException {
		Set<String> nonPublicBeanMethods = new HashSet<String>();
		for (AnnotationMetadata configurationClass : findConfigurationClasses()) {
			Set<MethodMetadata> beanMethods = configurationClass
					.getAnnotatedMethods(Bean.class.getName());
			for (MethodMetadata methodMetadata : beanMethods) {
				if (!isPublic(methodMetadata)) {
					nonPublicBeanMethods.add(methodMetadata.getDeclaringClassName() + "."
							+ methodMetadata.getMethodName());
				}
			}
		}

		assertEquals("Found non-public @Bean methods: " + nonPublicBeanMethods, 0,
				nonPublicBeanMethods.size());
	}

	private Set<AnnotationMetadata> findConfigurationClasses() throws IOException {
		Set<AnnotationMetadata> configurationClasses = new HashSet<AnnotationMetadata>();
		Resource[] resources = this.resolver.getResources("classpath*:"
				+ getClass().getPackage().getName().replace(".", "/") + "/**/*.class");

		for (Resource resource : resources) {
			if (!isTestClass(resource)) {
				MetadataReader metadataReader = new SimpleMetadataReaderFactory()
						.getMetadataReader(resource);
				AnnotationMetadata annotationMetadata = metadataReader
						.getAnnotationMetadata();
				if (annotationMetadata.getAnnotationTypes().contains(
						Configuration.class.getName())) {
					configurationClasses.add(annotationMetadata);
				}
			}
		}
		return configurationClasses;
	}

	private boolean isTestClass(Resource resource) throws IOException {
		return resource.getFile().getAbsolutePath()
				.contains("target" + File.separator + "test-classes");
	}

	private boolean isPublic(MethodMetadata methodMetadata) {
		int access = (Integer) new DirectFieldAccessor(methodMetadata)
				.getPropertyValue("access");

		return (access & Opcodes.ACC_PUBLIC) != 0;
	}

}
