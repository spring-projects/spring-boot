/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.testsupport.classpath.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.SearchOption;

import org.springframework.util.StreamUtils;

/**
 * {@link Extension} for managing resources in tests. Resources are made available through
 * {@link Thread#getContextClassLoader() thread context class loader}.
 *
 * @author Andy Wilkinson
 * @see WithPackageResources
 * @see WithResource
 * @see WithResourceDirectory
 */
class ResourcesExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

	private static final String RESOURCES_KEY = ResourcesExtension.class.getName() + ".resources";

	private static final String TCCL_KEY = ResourcesExtension.class.getName() + ".tccl";

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		Store store = context.getStore(Namespace.create(ResourcesExtension.class));
		Resources resources = new Resources(Files.createTempDirectory("resources"));
		store.put(RESOURCES_KEY, resources);
		Method testMethod = context.getRequiredTestMethod();
		resourcesOf(testMethod).forEach((resource) -> resources.addResource(resource.name(), resource.content()));
		resourceDirectoriesOf(testMethod).forEach((directory) -> resources.addDirectory(directory.value()));
		packageResourcesOf(testMethod).forEach((withPackageResources) -> resources
			.addPackage(testMethod.getDeclaringClass().getPackage().getName(), withPackageResources.value()));
		ResourcesClassLoader classLoader = new ResourcesClassLoader(context.getRequiredTestClass().getClassLoader(),
				resources);
		store.put(TCCL_KEY, Thread.currentThread().getContextClassLoader());
		Thread.currentThread().setContextClassLoader(classLoader);
	}

	private List<WithResource> resourcesOf(Method method) {
		return withAnnotationsOf(method, WithResource.class);
	}

	private List<WithResourceDirectory> resourceDirectoriesOf(Method method) {
		return withAnnotationsOf(method, WithResourceDirectory.class);
	}

	private <A extends Annotation> List<A> withAnnotationsOf(Method method, Class<A> annotationType) {
		List<A> annotations = new ArrayList<>();
		AnnotationSupport.findRepeatableAnnotations(method, annotationType).forEach(annotations::add);
		Class<?> type = method.getDeclaringClass();
		while (type != null) {
			AnnotationSupport.findRepeatableAnnotations(type, annotationType).forEach(annotations::add);
			type = type.getEnclosingClass();
		}
		return annotations;
	}

	private List<WithPackageResources> packageResourcesOf(Method method) {
		List<WithPackageResources> annotations = new ArrayList<>();
		AnnotationSupport.findAnnotation(method, WithPackageResources.class).ifPresent(annotations::add);
		AnnotationSupport
			.findAnnotation(method.getDeclaringClass(), WithPackageResources.class,
					SearchOption.INCLUDE_ENCLOSING_CLASSES)
			.ifPresent(annotations::add);
		return annotations;
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		Store store = context.getStore(Namespace.create(ResourcesExtension.class));
		store.get(RESOURCES_KEY, Resources.class).delete();
		Thread.currentThread().setContextClassLoader(store.get(TCCL_KEY, ClassLoader.class));
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		return parameterContext.isAnnotated(ResourcesRoot.class) || parameterContext.isAnnotated(ResourcePath.class)
				|| parameterContext.isAnnotated(ResourceContent.class);
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		if (parameterContext.isAnnotated(ResourcesRoot.class)) {
			return resolveResourcesRoot(parameterContext, extensionContext);
		}
		if (parameterContext.isAnnotated(ResourcePath.class)) {
			return resolveResourcePath(parameterContext, extensionContext);
		}
		if (parameterContext.isAnnotated(ResourceContent.class)) {
			return resolveResourceContent(parameterContext, extensionContext);
		}
		throw new ParameterResolutionException(
				"Parameter is not annotated with @ResourcesRoot, @ResourceContent, or @ResourcePath");
	}

	private Object resolveResourcesRoot(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Resources resources = getResources(extensionContext);
		Class<?> parameterType = parameterContext.getParameter().getType();
		if (parameterType.isAssignableFrom(Path.class)) {
			return resources.getRoot();
		}
		else if (parameterType.isAssignableFrom(File.class)) {
			return resources.getRoot().toFile();
		}
		throw new IllegalStateException(
				"@ResourcesRoot is not supported with parameter type '" + parameterType.getName() + "'");
	}

	private Object resolveResourcePath(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Resources resources = getResources(extensionContext);
		Class<?> parameterType = parameterContext.getParameter().getType();
		Path resourcePath = resources.getRoot()
			.resolve(parameterContext.findAnnotation(ResourcePath.class).get().value());
		if (parameterType.isAssignableFrom(Path.class)) {
			return resourcePath;
		}
		else if (parameterType.isAssignableFrom(File.class)) {
			return resourcePath.toFile();
		}
		else if (parameterType.isAssignableFrom(String.class)) {
			return resourcePath.toString();
		}
		throw new IllegalStateException(
				"@ResourcePath is not supported with parameter type '" + parameterType.getName() + "'");
	}

	private Object resolveResourceContent(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Resources resources = getResources(extensionContext);
		Class<?> parameterType = parameterContext.getParameter().getType();
		Path resourcePath = resources.getRoot()
			.resolve(parameterContext.findAnnotation(ResourceContent.class).get().value());
		if (parameterType.isAssignableFrom(String.class)) {
			try (InputStream in = Files.newInputStream(resourcePath)) {
				return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}
		throw new IllegalStateException(
				"@ResourceContent is not supported with parameter type '" + parameterType.getName() + "'");
	}

	private Resources getResources(ExtensionContext extensionContext) {
		Store store = extensionContext.getStore(Namespace.create(ResourcesExtension.class));
		Resources resources = store.get(RESOURCES_KEY, Resources.class);
		return resources;
	}

}
