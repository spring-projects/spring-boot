/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.devtools.remote.client;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.devtools.classpath.ClassPathChangedEvent;
import org.springframework.boot.devtools.filewatch.ChangedFile;
import org.springframework.boot.devtools.filewatch.ChangedFile.Type;
import org.springframework.boot.devtools.filewatch.ChangedFiles;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile.Kind;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles.SourceDirectory;
import org.springframework.boot.devtools.test.MockClientHttpRequestFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ClassPathChangeUploader}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ClassPathChangeUploaderTests {

	private MockClientHttpRequestFactory requestFactory;

	private ClassPathChangeUploader uploader;

	@BeforeEach
	void setup() {
		this.requestFactory = new MockClientHttpRequestFactory();
		this.uploader = new ClassPathChangeUploader("http://localhost/upload", this.requestFactory);
	}

	@Test
	void urlMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ClassPathChangeUploader(null, this.requestFactory))
				.withMessageContaining("URL must not be empty");
	}

	@Test
	void urlMustNotBeEmpty() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ClassPathChangeUploader("", this.requestFactory))
				.withMessageContaining("URL must not be empty");
	}

	@Test
	void requestFactoryMustNotBeNull() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ClassPathChangeUploader("http://localhost:8080", null))
				.withMessageContaining("RequestFactory must not be null");
	}

	@Test
	void urlMustNotBeMalformed() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ClassPathChangeUploader("htttttp:///ttest", this.requestFactory))
				.withMessageContaining("Malformed URL 'htttttp:///ttest'");
	}

	@Test
	void sendsClassLoaderFiles(@TempDir File sourceDirectory) throws Exception {
		ClassPathChangedEvent event = createClassPathChangedEvent(sourceDirectory);
		this.requestFactory.willRespond(HttpStatus.OK);
		this.uploader.onApplicationEvent(event);
		assertThat(this.requestFactory.getExecutedRequests()).hasSize(1);
		MockClientHttpRequest request = this.requestFactory.getExecutedRequests().get(0);
		verifyUploadRequest(sourceDirectory, request);
	}

	@Test
	void retriesOnSocketException(@TempDir File sourceDirectory) throws Exception {
		ClassPathChangedEvent event = createClassPathChangedEvent(sourceDirectory);
		this.requestFactory.willRespond(new SocketException());
		this.requestFactory.willRespond(HttpStatus.OK);
		this.uploader.onApplicationEvent(event);
		assertThat(this.requestFactory.getExecutedRequests()).hasSize(2);
		verifyUploadRequest(sourceDirectory, this.requestFactory.getExecutedRequests().get(1));
	}

	private void verifyUploadRequest(File sourceDirectory, MockClientHttpRequest request)
			throws IOException, ClassNotFoundException {
		ClassLoaderFiles classLoaderFiles = deserialize(request.getBodyAsBytes());
		Collection<SourceDirectory> sourceDirectories = classLoaderFiles.getSourceDirectories();
		assertThat(sourceDirectories.size()).isEqualTo(1);
		SourceDirectory classSourceDirectory = sourceDirectories.iterator().next();
		assertThat(classSourceDirectory.getName()).isEqualTo(sourceDirectory.getAbsolutePath());
		Iterator<ClassLoaderFile> classFiles = classSourceDirectory.getFiles().iterator();
		assertClassFile(classFiles.next(), "File1", ClassLoaderFile.Kind.ADDED);
		assertClassFile(classFiles.next(), "File2", ClassLoaderFile.Kind.MODIFIED);
		assertClassFile(classFiles.next(), null, ClassLoaderFile.Kind.DELETED);
		assertThat(classFiles.hasNext()).isFalse();
	}

	private void assertClassFile(ClassLoaderFile file, String content, Kind kind) {
		assertThat(file.getContents()).isEqualTo((content != null) ? content.getBytes() : null);
		assertThat(file.getKind()).isEqualTo(kind);
	}

	private ClassPathChangedEvent createClassPathChangedEvent(File sourceDirectory) throws IOException {
		Set<ChangedFile> files = new LinkedHashSet<>();
		File file1 = createFile(sourceDirectory, "File1");
		File file2 = createFile(sourceDirectory, "File2");
		File file3 = createFile(sourceDirectory, "File3");
		files.add(new ChangedFile(sourceDirectory, file1, Type.ADD));
		files.add(new ChangedFile(sourceDirectory, file2, Type.MODIFY));
		files.add(new ChangedFile(sourceDirectory, file3, Type.DELETE));
		Set<ChangedFiles> changeSet = new LinkedHashSet<>();
		changeSet.add(new ChangedFiles(sourceDirectory, files));
		return new ClassPathChangedEvent(this, changeSet, false);
	}

	private File createFile(File sourceDirectory, String name) throws IOException {
		File file = new File(sourceDirectory, name);
		FileCopyUtils.copy(name.getBytes(), file);
		return file;
	}

	private ClassLoaderFiles deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
		ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(bytes));
		return (ClassLoaderFiles) objectInputStream.readObject();
	}

}
