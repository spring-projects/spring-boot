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

package org.springframework.boot.devtools.restart.classloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile.Kind;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link RestartClassLoader}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("resource")
class RestartClassLoaderTests {

	private static final String PACKAGE = RestartClassLoaderTests.class.getPackage().getName();

	private static final String PACKAGE_PATH = PACKAGE.replace('.', '/');

	private File sampleJarFile;

	private URLClassLoader parentClassLoader;

	private ClassLoaderFiles updatedFiles;

	private RestartClassLoader reloadClassLoader;

	@BeforeEach
	void setup(@TempDir File tempDir) throws Exception {
		this.sampleJarFile = createSampleJarFile(tempDir);
		URL url = this.sampleJarFile.toURI().toURL();
		ClassLoader classLoader = getClass().getClassLoader();
		URL[] urls = new URL[] { url };
		this.parentClassLoader = new URLClassLoader(urls, classLoader);
		this.updatedFiles = new ClassLoaderFiles();
		this.reloadClassLoader = new RestartClassLoader(this.parentClassLoader, urls, this.updatedFiles);
	}

	@AfterEach
	void tearDown() throws Exception {
		this.reloadClassLoader.close();
		this.parentClassLoader.close();
	}

	private File createSampleJarFile(File tempDir) throws IOException {
		File file = new File(tempDir, "sample.jar");
		JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(file));
		jarOutputStream.putNextEntry(new ZipEntry(PACKAGE_PATH + "/Sample.class"));
		StreamUtils.copy(getClass().getResourceAsStream("Sample.class"), jarOutputStream);
		jarOutputStream.closeEntry();
		jarOutputStream.putNextEntry(new ZipEntry(PACKAGE_PATH + "/Sample.txt"));
		StreamUtils.copy("fromchild", StandardCharsets.UTF_8, jarOutputStream);
		jarOutputStream.closeEntry();
		jarOutputStream.close();
		return file;
	}

	@Test
	void parentMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new RestartClassLoader(null, new URL[] {}))
				.withMessageContaining("Parent must not be null");
	}

	@Test
	void updatedFilesMustNotBeNull() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new RestartClassLoader(this.parentClassLoader, new URL[] {}, null))
				.withMessageContaining("UpdatedFiles must not be null");
	}

	@Test
	void getResourceFromReloadableUrl() throws Exception {
		String content = readString(this.reloadClassLoader.getResourceAsStream(PACKAGE_PATH + "/Sample.txt"));
		assertThat(content).startsWith("fromchild");
	}

	@Test
	void getResourceFromParent() throws Exception {
		String content = readString(this.reloadClassLoader.getResourceAsStream(PACKAGE_PATH + "/Parent.txt"));
		assertThat(content).startsWith("fromparent");
	}

	@Test
	void getResourcesFiltersDuplicates() throws Exception {
		List<URL> resources = toList(this.reloadClassLoader.getResources(PACKAGE_PATH + "/Sample.txt"));
		assertThat(resources.size()).isEqualTo(1);
	}

	@Test
	void loadClassFromReloadableUrl() throws Exception {
		Class<?> loaded = Class.forName(PACKAGE + ".Sample", false, this.reloadClassLoader);
		assertThat(loaded.getClassLoader()).isEqualTo(this.reloadClassLoader);
	}

	@Test
	void loadClassFromParent() throws Exception {
		Class<?> loaded = Class.forName(PACKAGE + ".SampleParent", false, this.reloadClassLoader);
		assertThat(loaded.getClassLoader()).isEqualTo(getClass().getClassLoader());
	}

	@Test
	void getDeletedResource() {
		String name = PACKAGE_PATH + "/Sample.txt";
		this.updatedFiles.addFile(name, new ClassLoaderFile(Kind.DELETED, null));
		assertThat(this.reloadClassLoader.getResource(name)).isNull();
	}

	@Test
	void getDeletedResourceAsStream() {
		String name = PACKAGE_PATH + "/Sample.txt";
		this.updatedFiles.addFile(name, new ClassLoaderFile(Kind.DELETED, null));
		assertThat(this.reloadClassLoader.getResourceAsStream(name)).isNull();
	}

	@Test
	void getUpdatedResource() throws Exception {
		String name = PACKAGE_PATH + "/Sample.txt";
		byte[] bytes = "abc".getBytes();
		this.updatedFiles.addFile(name, new ClassLoaderFile(Kind.MODIFIED, bytes));
		URL resource = this.reloadClassLoader.getResource(name);
		assertThat(FileCopyUtils.copyToByteArray(resource.openStream())).isEqualTo(bytes);
	}

	@Test
	void getResourcesWithDeleted() throws Exception {
		String name = PACKAGE_PATH + "/Sample.txt";
		this.updatedFiles.addFile(name, new ClassLoaderFile(Kind.DELETED, null));
		List<URL> resources = toList(this.reloadClassLoader.getResources(name));
		assertThat(resources).isEmpty();
	}

	@Test
	void getResourcesWithUpdated() throws Exception {
		String name = PACKAGE_PATH + "/Sample.txt";
		byte[] bytes = "abc".getBytes();
		this.updatedFiles.addFile(name, new ClassLoaderFile(Kind.MODIFIED, bytes));
		List<URL> resources = toList(this.reloadClassLoader.getResources(name));
		assertThat(FileCopyUtils.copyToByteArray(resources.get(0).openStream())).isEqualTo(bytes);
	}

	@Test
	void getDeletedClass() throws Exception {
		String name = PACKAGE_PATH + "/Sample.class";
		this.updatedFiles.addFile(name, new ClassLoaderFile(Kind.DELETED, null));
		assertThatExceptionOfType(ClassNotFoundException.class)
				.isThrownBy(() -> Class.forName(PACKAGE + ".Sample", false, this.reloadClassLoader));
	}

	@Test
	void getUpdatedClass() throws Exception {
		String name = PACKAGE_PATH + "/Sample.class";
		this.updatedFiles.addFile(name, new ClassLoaderFile(Kind.MODIFIED, new byte[10]));
		assertThatExceptionOfType(ClassFormatError.class)
				.isThrownBy(() -> Class.forName(PACKAGE + ".Sample", false, this.reloadClassLoader));
	}

	@Test
	void getAddedClass() throws Exception {
		String name = PACKAGE_PATH + "/SampleParent.class";
		byte[] bytes = FileCopyUtils.copyToByteArray(getClass().getResourceAsStream("SampleParent.class"));
		this.updatedFiles.addFile(name, new ClassLoaderFile(Kind.ADDED, bytes));
		Class<?> loaded = Class.forName(PACKAGE + ".SampleParent", false, this.reloadClassLoader);
		assertThat(loaded.getClassLoader()).isEqualTo(this.reloadClassLoader);
	}

	@Test
	void proxyOnClassFromSystemClassLoaderDoesNotYieldWarning() {
		ProxyFactory pf = new ProxyFactory(new HashMap<>());
		pf.setProxyTargetClass(true);
		pf.getProxy(this.reloadClassLoader);
		// Warning would happen outside the boundary of the test
	}

	@Test
	void packagePrivateClassLoadedByParentClassLoaderCanBeProxied() throws IOException {
		try (RestartClassLoader restartClassLoader = new RestartClassLoader(ExampleTransactional.class.getClassLoader(),
				new URL[] { this.sampleJarFile.toURI().toURL() }, this.updatedFiles)) {
			new ApplicationContextRunner().withClassLoader(restartClassLoader)
					.withUserConfiguration(ProxyConfiguration.class).run((context) -> {
						assertThat(context).hasNotFailed();
						ExampleTransactional transactional = context.getBean(ExampleTransactional.class);
						assertThat(AopUtils.isCglibProxy(transactional)).isTrue();
						assertThat(transactional.getClass().getClassLoader())
								.isEqualTo(ExampleTransactional.class.getClassLoader());
					});
		}
	}

	private String readString(InputStream in) throws IOException {
		return new String(FileCopyUtils.copyToByteArray(in));
	}

	private <T> List<T> toList(Enumeration<T> enumeration) {
		return (enumeration != null) ? Collections.list(enumeration) : Collections.emptyList();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAspectJAutoProxy(proxyTargetClass = true)
	@EnableTransactionManagement
	static class ProxyConfiguration {

		@Bean
		ExampleTransactional exampleTransactional() {
			return new ExampleTransactional();
		}

	}

	static class ExampleTransactional implements ExampleInterface {

		@Override
		@Transactional
		public String doIt() {
			return "hello";
		}

	}

	interface ExampleInterface {

		String doIt();

	}

}
