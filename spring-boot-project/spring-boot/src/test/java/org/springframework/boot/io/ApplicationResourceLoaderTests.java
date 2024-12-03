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

package org.springframework.boot.io;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Enumeration;
import java.util.function.UnaryOperator;

import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.context.support.ServletContextResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ApplicationResourceLoader}.
 *
 * @author Phillip Webb
 */
class ApplicationResourceLoaderTests {

	private static final String SPRING_FACTORIES = "META-INF/spring.factories";

	private static final String TEST_PROTOCOL_RESOLVERS_FACTORIES = "META-INF/spring-test-protocol-resolvers.factories";

	private static final String TEST_BASE_64_VALUE = Base64.getEncoder().encodeToString("test".getBytes());

	@Test
	void getIncludesProtocolResolvers() throws IOException {
		ResourceLoader loader = ApplicationResourceLoader.get();
		Resource resource = loader.getResource("base64:" + TEST_BASE_64_VALUE);
		assertThat(contentAsString(resource)).isEqualTo("test");
	}

	@Test
	void getWithClassPathIncludesProtocolResolvers() throws IOException {
		ClassLoader classLoader = new TestClassLoader(this::useTestProtocolResolversFactories);
		ResourceLoader loader = ApplicationResourceLoader.get(classLoader);
		Resource resource = loader.getResource("reverse:test");
		assertThat(contentAsString(resource)).isEqualTo("tset");
	}

	@Test
	void getWithClassPathWhenClassPathIsNullIncludesProtocolResolvers() throws IOException {
		ResourceLoader loader = ApplicationResourceLoader.get((ClassLoader) null);
		Resource resource = loader.getResource("base64:" + TEST_BASE_64_VALUE);
		assertThat(contentAsString(resource)).isEqualTo("test");
	}

	@Test
	void getWithClassPathAndSpringFactoriesLoaderIncludesProtocolResolvers() throws IOException {
		SpringFactoriesLoader springFactoriesLoader = SpringFactoriesLoader
			.forResourceLocation(TEST_PROTOCOL_RESOLVERS_FACTORIES);
		ResourceLoader loader = ApplicationResourceLoader.get((ClassLoader) null, springFactoriesLoader);
		Resource resource = loader.getResource("reverse:test");
		assertThat(contentAsString(resource)).isEqualTo("tset");
	}

	@Test
	void getWithClassPathAndSpringFactoriesLoaderWhenSpringFactoriesLoaderIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ApplicationResourceLoader.get((ClassLoader) null, null))
			.withMessage("'springFactoriesLoader' must not be null");
	}

	@Test
	void getWithResourceLoaderIncludesProtocolResolvers() throws IOException {
		ResourceLoader loader = ApplicationResourceLoader.get(new DefaultResourceLoader());
		Resource resource = loader.getResource("base64:" + TEST_BASE_64_VALUE);
		assertThat(contentAsString(resource)).isEqualTo("test");
	}

	@Test
	void getWithResourceLoaderDelegatesLoading() throws IOException {
		DefaultResourceLoader delegate = new TestResourceLoader();
		ResourceLoader loader = ApplicationResourceLoader.get(delegate);
		assertThat(contentAsString(loader.getResource("spring"))).isEqualTo("boot");
	}

	@Test
	void getWithResourceLoaderWhenResourceLoaderIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ApplicationResourceLoader.get((ResourceLoader) null))
			.withMessage("'resourceLoader' must not be null");
	}

	@Test
	void getWithResourceLoaderAndSpringFactoriesLoaderIncludesProtocolResolvers() throws IOException {
		DefaultResourceLoader delegate = new TestResourceLoader();
		ResourceLoader loader = ApplicationResourceLoader.get(delegate);
		Resource resource = loader.getResource("base64:" + TEST_BASE_64_VALUE);
		assertThat(contentAsString(resource)).isEqualTo("test");
	}

	@Test
	void getWithResourceLoaderAndSpringFactoriesLoaderWhenResourceLoaderIsNullThrowsException() {
		SpringFactoriesLoader springFactoriesLoader = SpringFactoriesLoader
			.forResourceLocation(TEST_PROTOCOL_RESOLVERS_FACTORIES);
		assertThatIllegalArgumentException()
			.isThrownBy(() -> ApplicationResourceLoader.get((ResourceLoader) null, springFactoriesLoader))
			.withMessage("'resourceLoader' must not be null");
	}

	@Test
	void getWithResourceLoaderAndSpringFactoriesLoaderWhenSpringFactoriesLoaderIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> ApplicationResourceLoader.get(new TestResourceLoader(), null))
			.withMessage("'springFactoriesLoader' must not be null");
	}

	@Test
	void getResourceWhenPathIsRelative() throws IOException {
		ResourceLoader loader = ApplicationResourceLoader.get();
		String name = "src/test/resources/" + TEST_PROTOCOL_RESOLVERS_FACTORIES;
		Resource resource = loader.getResource(name);
		assertThat(resource.getFile()).isEqualTo(new File(name));
	}

	@Test
	void getResourceWhenPathIsAbsolute() throws IOException {
		File file = new File("src/test/resources/" + TEST_PROTOCOL_RESOLVERS_FACTORIES);
		ResourceLoader loader = ApplicationResourceLoader.get();
		Resource resource = loader.getResource(file.getAbsolutePath());
		assertThat(resource.getFile()).hasSameBinaryContentAs(file);
	}

	@Test
	void getResourceWhenPathIsNull() {
		ResourceLoader loader = ApplicationResourceLoader.get();
		assertThatIllegalArgumentException().isThrownBy(() -> loader.getResource(null))
			.withMessage("Location must not be null");
	}

	@Test
	void getResourceWithPreferFileResolutionWhenFullPathWithClassPathResource() throws Exception {
		File file = new File("src/main/resources/a-file");
		ResourceLoader loader = ApplicationResourceLoader.get(new DefaultResourceLoader(), true);
		Resource resource = loader.getResource(file.getAbsolutePath());
		assertThat(resource).isInstanceOf(FileSystemResource.class);
		assertThat(resource.getFile().getAbsoluteFile()).isEqualTo(file.getAbsoluteFile());
		ResourceLoader regularLoader = ApplicationResourceLoader.get(new DefaultResourceLoader(), false);
		assertThat(regularLoader.getResource(file.getAbsolutePath())).isInstanceOf(ClassPathResource.class);
	}

	@Test
	void getResourceWithPreferFileResolutionWhenRelativePathWithClassPathResource() throws Exception {
		ResourceLoader loader = ApplicationResourceLoader.get(new DefaultResourceLoader(), true);
		Resource resource = loader.getResource("src/main/resources/a-file");
		assertThat(resource).isInstanceOf(FileSystemResource.class);
		assertThat(resource.getFile().getAbsoluteFile())
			.isEqualTo(new File("src/main/resources/a-file").getAbsoluteFile());
		ResourceLoader regularLoader = ApplicationResourceLoader.get(new DefaultResourceLoader(), false);
		assertThat(regularLoader.getResource("src/main/resources/a-file")).isInstanceOf(ClassPathResource.class);
	}

	@Test
	void getResourceWithPreferFileResolutionWhenExplicitClassPathPrefix() {
		ResourceLoader loader = ApplicationResourceLoader.get(new DefaultResourceLoader(), true);
		Resource resource = loader.getResource("classpath:a-file");
		assertThat(resource).isInstanceOf(ClassPathResource.class);
	}

	@Test
	void getResourceWithPreferFileResolutionWhenPathWithServletContextResource() throws Exception {
		ServletContext servletContext = new MockServletContext();
		ServletContextResourceLoader servletContextResourceLoader = new ServletContextResourceLoader(servletContext);
		ResourceLoader loader = ApplicationResourceLoader.get(servletContextResourceLoader, true);
		Resource resource = loader.getResource("src/main/resources/a-file");
		assertThat(resource).isInstanceOf(FileSystemResource.class);
		assertThat(resource.getFile().getAbsoluteFile())
			.isEqualTo(new File("src/main/resources/a-file").getAbsoluteFile());
		ResourceLoader regularLoader = ApplicationResourceLoader.get(servletContextResourceLoader, false);
		assertThat(regularLoader.getResource("src/main/resources/a-file")).isInstanceOf(ServletContextResource.class);
	}

	@Test
	void getClassLoaderReturnsDelegateClassLoader() {
		ClassLoader classLoader = new TestClassLoader(this::useTestProtocolResolversFactories);
		ResourceLoader loader = ApplicationResourceLoader.get(new DefaultResourceLoader(classLoader));
		assertThat(loader.getClassLoader()).isSameAs(classLoader);
	}

	private String contentAsString(Resource resource) throws IOException {
		return resource.getContentAsString(StandardCharsets.UTF_8);
	}

	private String useTestProtocolResolversFactories(String name) {
		return (!SPRING_FACTORIES.equals(name)) ? name : TEST_PROTOCOL_RESOLVERS_FACTORIES;
	}

	static class TestClassLoader extends ClassLoader {

		private final UnaryOperator<String> mapper;

		TestClassLoader(UnaryOperator<String> mapper) {
			super(Thread.currentThread().getContextClassLoader());
			this.mapper = mapper;
		}

		@Override
		public URL getResource(String name) {
			return super.getResource(this.mapper.apply(name));
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			return super.getResources(this.mapper.apply(name));
		}

	}

	static class TestResourceLoader extends DefaultResourceLoader {

		@Override
		public Resource getResource(String location) {
			return (!"spring".equals(location)) ? super.getResource(location)
					: new ByteArrayResource("boot".getBytes(StandardCharsets.UTF_8));
		}

	}

}
