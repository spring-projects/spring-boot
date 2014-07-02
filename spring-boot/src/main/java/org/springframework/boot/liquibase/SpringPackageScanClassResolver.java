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

package org.springframework.boot.liquibase;

import java.io.IOException;
import java.util.Set;

import liquibase.servicelocator.DefaultPackageScanClassResolver;
import liquibase.servicelocator.PackageScanClassResolver;
import liquibase.servicelocator.PackageScanFilter;

import org.apache.commons.logging.Log;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;

/**
 * Liquibase {@link PackageScanClassResolver} implementation that uses Spring's resource
 * scanning to locate classes. This variant is safe to use with Spring Boot packaged
 * executable JARs.
 *
 * @author Phillip Webb
 */
public class SpringPackageScanClassResolver extends DefaultPackageScanClassResolver {

	private final Log logger;

	public SpringPackageScanClassResolver(Log logger) {
		this.logger = logger;
	}

	@Override
	protected void find(PackageScanFilter test, String packageName, ClassLoader loader,
			Set<Class<?>> classes) {
		MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(
				loader);
		try {
			Resource[] resources = scan(loader, packageName);
			for (Resource resource : resources) {
				Class<?> candidate = loadClass(loader, metadataReaderFactory, resource);
				if (candidate != null && test.matches(candidate)) {
					classes.add(candidate);
				}
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private Resource[] scan(ClassLoader loader, String packageName) throws IOException {
		ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(loader);
		String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
				+ ClassUtils.convertClassNameToResourcePath(packageName) + "/**/*.class";
		Resource[] resources = resolver.getResources(pattern);
		return resources;
	}

	private Class<?> loadClass(ClassLoader loader, MetadataReaderFactory readerFactory,
			Resource resource) {
		try {
			MetadataReader reader = readerFactory.getMetadataReader(resource);
			return ClassUtils.forName(reader.getClassMetadata().getClassName(), loader);
		}
		catch (Exception ex) {
			if (this.logger.isWarnEnabled()) {
				this.logger.warn("Ignoring cadidate class resource " + resource, ex);
			}
			return null;
		}
	}

}
