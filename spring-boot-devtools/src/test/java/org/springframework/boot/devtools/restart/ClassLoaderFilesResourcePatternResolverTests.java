/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.devtools.restart;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.devtools.restart.ClassLoaderFilesResourcePatternResolver.DeletedClassLoaderFileResource;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile.Kind;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.context.support.ServletContextResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ClassLoaderFilesResourcePatternResolver}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
public class ClassLoaderFilesResourcePatternResolverTests {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	private ClassLoaderFiles files;

	private ClassLoaderFilesResourcePatternResolver resolver;

	@Before
	public void setup() {
		this.files = new ClassLoaderFiles();
		this.resolver = new ClassLoaderFilesResourcePatternResolver(
				new GenericApplicationContext(), this.files);
	}

	@Test
	public void getClassLoaderShouldReturnClassLoader() throws Exception {
		assertThat(this.resolver.getClassLoader()).isNotNull();
	}

	@Test
	public void getResourceShouldReturnResource() throws Exception {
		Resource resource = this.resolver.getResource("index.html");
		assertThat(resource).isNotNull().isInstanceOf(ClassPathResource.class);
	}

	@Test
	public void getResourceWhenHasServletContextShouldReturnServletResource()
			throws Exception {
		GenericWebApplicationContext context = new GenericWebApplicationContext(
				new MockServletContext());
		this.resolver = new ClassLoaderFilesResourcePatternResolver(context, this.files);
		Resource resource = this.resolver.getResource("index.html");
		assertThat(resource).isNotNull().isInstanceOf(ServletContextResource.class);
	}

	@Test
	public void getResourceWhenDeletedShouldReturnDeletedResource() throws Exception {
		File folder = this.temp.newFolder();
		File file = createFile(folder, "name.class");
		this.files.addFile(folder.getName(), "name.class",
				new ClassLoaderFile(Kind.DELETED, null));
		Resource resource = this.resolver.getResource("file:" + file.getAbsolutePath());
		assertThat(resource).isNotNull()
				.isInstanceOf(DeletedClassLoaderFileResource.class);
	}

	@Test
	public void getResourcesShouldReturnResources() throws Exception {
		File folder = this.temp.newFolder();
		createFile(folder, "name.class");
		Resource[] resources = this.resolver
				.getResources("file:" + folder.getAbsolutePath() + "/**");
		assertThat(resources).isNotEmpty();
	}

	@Test
	public void getResourcesWhenDeletedShouldFilterDeleted() throws Exception {
		File folder = this.temp.newFolder();
		createFile(folder, "name.class");
		this.files.addFile(folder.getName(), "name.class",
				new ClassLoaderFile(Kind.DELETED, null));
		Resource[] resources = this.resolver
				.getResources("file:" + folder.getAbsolutePath() + "/**");
		assertThat(resources).isEmpty();
	}

	@Test
	public void customResourceLoaderIsUsedInNonWebApplication() throws Exception {
		GenericApplicationContext context = new GenericApplicationContext();
		ResourceLoader resourceLoader = mock(ResourceLoader.class);
		context.setResourceLoader(resourceLoader);
		this.resolver = new ClassLoaderFilesResourcePatternResolver(context, this.files);
		this.resolver.getResource("foo.txt");
		verify(resourceLoader).getResource("foo.txt");
	}

	@Test
	public void customProtocolResolverIsUsedInNonWebApplication() throws Exception {
		GenericApplicationContext context = new GenericApplicationContext();
		Resource resource = mock(Resource.class);
		ProtocolResolver resolver = mockProtocolResolver("foo:some-file.txt", resource);
		context.addProtocolResolver(resolver);
		this.resolver = new ClassLoaderFilesResourcePatternResolver(context, this.files);
		Resource actual = this.resolver.getResource("foo:some-file.txt");
		assertThat(actual).isSameAs(resource);
		verify(resolver).resolve(eq("foo:some-file.txt"), any(ResourceLoader.class));
	}

	@Test
	public void customResourceLoaderIsUsedInWebApplication() throws Exception {
		GenericWebApplicationContext context = new GenericWebApplicationContext(
				new MockServletContext());
		ResourceLoader resourceLoader = mock(ResourceLoader.class);
		context.setResourceLoader(resourceLoader);
		this.resolver = new ClassLoaderFilesResourcePatternResolver(context, this.files);
		this.resolver.getResource("foo.txt");
		verify(resourceLoader).getResource("foo.txt");
	}

	@Test
	public void customProtocolResolverIsUsedInWebApplication() throws Exception {
		GenericWebApplicationContext context = new GenericWebApplicationContext(
				new MockServletContext());
		Resource resource = mock(Resource.class);
		ProtocolResolver resolver = mockProtocolResolver("foo:some-file.txt", resource);
		context.addProtocolResolver(resolver);
		this.resolver = new ClassLoaderFilesResourcePatternResolver(context, this.files);
		Resource actual = this.resolver.getResource("foo:some-file.txt");
		assertThat(actual).isSameAs(resource);
		verify(resolver).resolve(eq("foo:some-file.txt"), any(ResourceLoader.class));
	}

	private ProtocolResolver mockProtocolResolver(String path, Resource resource) {
		ProtocolResolver resolver = mock(ProtocolResolver.class);
		given(resolver.resolve(eq(path), any(ResourceLoader.class))).willReturn(resource);
		return resolver;
	}

	private File createFile(File folder, String name) throws IOException {
		File file = new File(folder, name);
		FileCopyUtils.copy("test".getBytes(), file);
		return file;
	}

}
