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

package org.springframework.boot.context.embedded;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.util.ReflectionUtils;

import org.springframework.boot.testsupport.BuildOutput;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplateHandler;

/**
 * {@link TestTemplateInvocationContextProvider} for templated
 * {@link EmbeddedServletContainerTest embedded servlet container tests}.
 *
 * @author Andy Wilkinson
 */
class EmbeddedServerContainerInvocationContextProvider
		implements TestTemplateInvocationContextProvider, AfterAllCallback {

	private static final Set<String> CONTAINERS = new HashSet<>(Arrays.asList("jetty", "tomcat", "undertow"));

	private static final BuildOutput buildOutput = new BuildOutput(
			EmbeddedServerContainerInvocationContextProvider.class);

	private final Map<String, AbstractApplicationLauncher> launcherCache = new HashMap<>();

	private final Path tempDir;

	EmbeddedServerContainerInvocationContextProvider() throws IOException {
		this.tempDir = Files.createTempDirectory("embedded-servlet-container-tests");
	}

	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		return true;
	}

	@Override
	public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
		EmbeddedServletContainerTest annotation = context.getRequiredTestClass()
				.getAnnotation(EmbeddedServletContainerTest.class);
		return CONTAINERS
				.stream().map(
						(container) -> getApplication(annotation, container))
				.flatMap(
						(builder) -> Stream
								.of(annotation.launchers()).map(
										(launcherClass) -> getAbstractApplicationLauncher(builder, launcherClass))
								.map((launcher) -> new EmbeddedServletContainerInvocationContext(
										StringUtils.capitalize(builder.getContainer()) + ": "
												+ launcher.getDescription(builder.getPackaging()),
										launcher)));
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		cleanupCaches();
		FileSystemUtils.deleteRecursively(this.tempDir);
	}

	private void cleanupCaches() {
		this.launcherCache.values().forEach(AbstractApplicationLauncher::destroyProcess);
		this.launcherCache.clear();
	}

	private AbstractApplicationLauncher getAbstractApplicationLauncher(Application application,
			Class<? extends AbstractApplicationLauncher> launcherClass) {
		String cacheKey = application.getContainer() + ":" + application.getPackaging() + ":" + launcherClass.getName();
		if (this.launcherCache.containsKey(cacheKey)) {
			return this.launcherCache.get(cacheKey);
		}
		AbstractApplicationLauncher launcher = ReflectionUtils.newInstance(launcherClass, application,
				new File(buildOutput.getRootLocation(), "app-launcher-" + UUID.randomUUID()));
		this.launcherCache.put(cacheKey, launcher);
		return launcher;
	}

	private Application getApplication(EmbeddedServletContainerTest annotation, String container) {
		return new Application(annotation.packaging(), container);
	}

	static class EmbeddedServletContainerInvocationContext implements TestTemplateInvocationContext, ParameterResolver {

		private final String name;

		private final AbstractApplicationLauncher launcher;

		EmbeddedServletContainerInvocationContext(String name, AbstractApplicationLauncher launcher) {
			this.name = name;
			this.launcher = launcher;
		}

		@Override
		public List<Extension> getAdditionalExtensions() {
			return Arrays.asList(this.launcher, new RestTemplateParameterResolver(this.launcher));
		}

		@Override
		public String getDisplayName(int invocationIndex) {
			return this.name;
		}

		@Override
		public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			if (parameterContext.getParameter().getType().equals(AbstractApplicationLauncher.class)) {
				return true;
			}
			if (parameterContext.getParameter().getType().equals(RestTemplate.class)) {
				return true;
			}
			return false;
		}

		@Override
		public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			if (parameterContext.getParameter().getType().equals(AbstractApplicationLauncher.class)) {
				return this.launcher;
			}
			return null;
		}

	}

	private static final class RestTemplateParameterResolver implements ParameterResolver {

		private final AbstractApplicationLauncher launcher;

		private RestTemplateParameterResolver(AbstractApplicationLauncher launcher) {
			this.launcher = launcher;
		}

		@Override
		public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			return parameterContext.getParameter().getType().equals(RestTemplate.class);
		}

		@Override
		public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
			RestTemplate rest = new RestTemplate(new HttpComponentsClientHttpRequestFactory(
					HttpClients.custom().setRetryHandler(new StandardHttpRequestRetryHandler(10, false)).build()));
			rest.setErrorHandler(new ResponseErrorHandler() {

				@Override
				public boolean hasError(ClientHttpResponse response) throws IOException {
					return false;
				}

				@Override
				public void handleError(ClientHttpResponse response) throws IOException {

				}

			});
			rest.setUriTemplateHandler(new UriTemplateHandler() {

				@Override
				public URI expand(String uriTemplate, Object... uriVariables) {
					return URI.create("http://localhost:" + RestTemplateParameterResolver.this.launcher.getHttpPort()
							+ uriTemplate);
				}

				@Override
				public URI expand(String uriTemplate, Map<String, ?> uriVariables) {
					return URI.create("http://localhost:" + RestTemplateParameterResolver.this.launcher.getHttpPort()
							+ uriTemplate);
				}

			});
			return rest;
		}

	}

}
