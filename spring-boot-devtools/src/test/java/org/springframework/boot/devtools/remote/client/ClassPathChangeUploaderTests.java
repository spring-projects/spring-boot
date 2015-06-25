/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.devtools.remote.client;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.devtools.classpath.ClassPathChangedEvent;
import org.springframework.boot.devtools.filewatch.ChangedFile;
import org.springframework.boot.devtools.filewatch.ChangedFiles;
import org.springframework.boot.devtools.filewatch.ChangedFile.Type;
import org.springframework.boot.devtools.remote.client.ClassPathChangeUploader;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile.Kind;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles.SourceFolder;
import org.springframework.boot.devtools.test.MockClientHttpRequestFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.util.FileCopyUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ClassPathChangeUploader}.
 *
 * @author Phillip Webb
 */
public class ClassPathChangeUploaderTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	private MockClientHttpRequestFactory requestFactory;

	private ClassPathChangeUploader uploader;

	@Before
	public void setup() {
		this.requestFactory = new MockClientHttpRequestFactory();
		this.uploader = new ClassPathChangeUploader("http://localhost/upload",
				this.requestFactory);
	}

	@Test
	public void urlMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("URL must not be empty");
		new ClassPathChangeUploader(null, this.requestFactory);
	}

	@Test
	public void urlMustNotBeEmpty() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("URL must not be empty");
		new ClassPathChangeUploader("", this.requestFactory);
	}

	@Test
	public void requestFactoryMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("RequestFactory must not be null");
		new ClassPathChangeUploader("http://localhost:8080", null);
	}

	@Test
	public void urlMustNotBeMalformed() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Malformed URL 'htttttp:///ttest'");
		new ClassPathChangeUploader("htttttp:///ttest", this.requestFactory);
	}

	@Test
	public void sendsClassLoaderFiles() throws Exception {
		File sourceFolder = this.temp.newFolder();
		Set<ChangedFile> files = new LinkedHashSet<ChangedFile>();
		File file1 = createFile(sourceFolder, "File1");
		File file2 = createFile(sourceFolder, "File2");
		File file3 = createFile(sourceFolder, "File3");
		files.add(new ChangedFile(sourceFolder, file1, Type.ADD));
		files.add(new ChangedFile(sourceFolder, file2, Type.MODIFY));
		files.add(new ChangedFile(sourceFolder, file3, Type.DELETE));
		Set<ChangedFiles> changeSet = new LinkedHashSet<ChangedFiles>();
		changeSet.add(new ChangedFiles(sourceFolder, files));
		ClassPathChangedEvent event = new ClassPathChangedEvent(this, changeSet, false);
		this.requestFactory.willRespond(HttpStatus.OK);
		this.uploader.onApplicationEvent(event);
		MockClientHttpRequest request = this.requestFactory.getExecutedRequests().get(0);
		ClassLoaderFiles classLoaderFiles = deserialize(request.getBodyAsBytes());
		Collection<SourceFolder> sourceFolders = classLoaderFiles.getSourceFolders();
		assertThat(sourceFolders.size(), equalTo(1));
		SourceFolder classSourceFolder = sourceFolders.iterator().next();
		assertThat(classSourceFolder.getName(), equalTo(sourceFolder.getAbsolutePath()));
		Iterator<ClassLoaderFile> classFiles = classSourceFolder.getFiles().iterator();
		assertClassFile(classFiles.next(), "File1", ClassLoaderFile.Kind.ADDED);
		assertClassFile(classFiles.next(), "File2", ClassLoaderFile.Kind.MODIFIED);
		assertClassFile(classFiles.next(), null, ClassLoaderFile.Kind.DELETED);
		assertThat(classFiles.hasNext(), equalTo(false));
	}

	private void assertClassFile(ClassLoaderFile file, String content, Kind kind) {
		assertThat(file.getContents(),
				equalTo(content == null ? null : content.getBytes()));
		assertThat(file.getKind(), equalTo(kind));
	}

	private File createFile(File sourceFolder, String name) throws IOException {
		File file = new File(sourceFolder, name);
		FileCopyUtils.copy(name.getBytes(), file);
		return file;
	}

	private ClassLoaderFiles deserialize(byte[] bytes) throws IOException,
			ClassNotFoundException {
		ObjectInputStream objectInputStream = new ObjectInputStream(
				new ByteArrayInputStream(bytes));
		return (ClassLoaderFiles) objectInputStream.readObject();
	}

}
