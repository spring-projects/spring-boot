/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationShutdownHandlers;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.fail;

/**
 * {@link Extension} for {@link DockerComposeTest @DockerComposeTest}.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 */
class DockerComposeTestExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback, ParameterResolver {

	private static final Namespace NAMESPACE = Namespace.create(DockerComposeTestExtension.class);

	private static final String STORE_KEY_WORKSPACE = "workspace";

	private static final String STORE_KEY_APPLICATION_CONTEXT = "application-context";

	@Override
	public void beforeTestExecution(ExtensionContext context) throws Exception {
		Store store = context.getStore(NAMESPACE);
		Path workspace = Files.createTempDirectory("DockerComposeTestExtension-");
		store.put(STORE_KEY_WORKSPACE, workspace);
		try {
			Path composeFile = prepareComposeFile(workspace, context);
			copyAdditionalResources(workspace, context);
			SpringApplication application = prepareApplication(composeFile);
			store.put(STORE_KEY_APPLICATION_CONTEXT, application.run());
		}
		catch (Exception ex) {
			cleanUp(context);
			throw ex;
		}
	}

	private Path prepareComposeFile(Path workspace, ExtensionContext context) {
		DockerComposeTest dockerComposeTest = context.getRequiredTestMethod().getAnnotation(DockerComposeTest.class);
		TestImage image = dockerComposeTest.image();
		Resource composeResource = new ClassPathResource(dockerComposeTest.composeFile(),
				context.getRequiredTestClass());
		return transformedComposeFile(workspace, composeResource, image);
	}

	private Path transformedComposeFile(Path workspace, Resource composeFileResource, TestImage image) {
		try {
			String template = composeFileResource.getContentAsString(StandardCharsets.UTF_8);
			String content = template.replace("{imageName}", image.toString());
			Path composeFile = workspace.resolve("compose.yaml");
			Files.writeString(composeFile, content);
			return composeFile;
		}
		catch (IOException ex) {
			fail("Error transforming Docker compose file '" + composeFileResource + "': " + ex.getMessage(), ex);
			return null;
		}
	}

	private void copyAdditionalResources(Path workspace, ExtensionContext context) {
		DockerComposeTest dockerComposeTest = context.getRequiredTestMethod().getAnnotation(DockerComposeTest.class);
		for (String additionalResource : dockerComposeTest.additionalResources()) {
			Resource resource = new ClassPathResource(additionalResource, context.getRequiredTestClass());
			copyAdditionalResource(workspace, resource);
		}
	}

	private void copyAdditionalResource(Path workspace, Resource resource) {
		try {
			Path source = resource.getFile().toPath();
			Files.copy(source, workspace.resolve(source.getFileName()));
		}
		catch (IOException ex) {
			fail("Error copying additional resource '" + resource + "': " + ex.getMessage(), ex);
		}
	}

	private SpringApplication prepareApplication(Path composeFile) {
		SpringApplication application = new SpringApplication(Config.class);
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("spring.docker.compose.skip.in-tests", "false");
		properties.put("spring.docker.compose.file", composeFile);
		properties.put("spring.docker.compose.stop.command", "down");
		application.setDefaultProperties(properties);
		return application;
	}

	@Override
	public void afterTestExecution(ExtensionContext context) throws Exception {
		cleanUp(context);
	}

	private void cleanUp(ExtensionContext context) throws Exception {
		Store store = context.getStore(NAMESPACE);
		runShutdownHandlers();
		deleteWorkspace(store);
	}

	private void runShutdownHandlers() {
		SpringApplicationShutdownHandlers shutdownHandlers = SpringApplication.getShutdownHandlers();
		((Runnable) shutdownHandlers).run();
	}

	private void deleteWorkspace(Store store) throws IOException {
		Path workspace = (Path) store.get(STORE_KEY_WORKSPACE);
		if (workspace != null) {
			FileSystemUtils.deleteRecursively(workspace);
		}
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		return true;
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		ConfigurableApplicationContext applicationContext = extensionContext.getStore(NAMESPACE)
			.get(STORE_KEY_APPLICATION_CONTEXT, ConfigurableApplicationContext.class);
		return (applicationContext != null) ? applicationContext.getBean(parameterContext.getParameter().getType())
				: null;
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
