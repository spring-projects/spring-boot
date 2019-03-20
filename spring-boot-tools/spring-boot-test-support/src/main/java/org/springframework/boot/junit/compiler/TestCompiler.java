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

package org.springframework.boot.junit.compiler;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.annotation.processing.Processor;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.junit.rules.TemporaryFolder;

/**
 * Wrapper to make the {@link JavaCompiler} easier to use in tests.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class TestCompiler {

	/**
	 * The default source folder.
	 */
	public static final File SOURCE_FOLDER = new File("src/test/java");

	private final JavaCompiler compiler;

	private final StandardJavaFileManager fileManager;

	private final File outputLocation;

	public TestCompiler(TemporaryFolder temporaryFolder) throws IOException {
		this(ToolProvider.getSystemJavaCompiler(), temporaryFolder);
	}

	public TestCompiler(JavaCompiler compiler, TemporaryFolder temporaryFolder)
			throws IOException {
		this.compiler = compiler;
		this.fileManager = compiler.getStandardFileManager(null, null, null);
		this.outputLocation = temporaryFolder.newFolder();
		Iterable<? extends File> temp = Arrays.asList(this.outputLocation);
		this.fileManager.setLocation(StandardLocation.CLASS_OUTPUT, temp);
		this.fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, temp);
	}

	public TestCompilationTask getTask(Collection<File> sourceFiles) {
		Iterable<? extends JavaFileObject> javaFileObjects = this.fileManager
				.getJavaFileObjectsFromFiles(sourceFiles);
		return getTask(javaFileObjects);
	}

	public TestCompilationTask getTask(Class<?>... types) {
		Iterable<? extends JavaFileObject> javaFileObjects = getJavaFileObjects(types);
		return getTask(javaFileObjects);
	}

	private TestCompilationTask getTask(
			Iterable<? extends JavaFileObject> javaFileObjects) {
		return new TestCompilationTask(this.compiler.getTask(null, this.fileManager, null,
				null, null, javaFileObjects));
	}

	public File getOutputLocation() {
		return this.outputLocation;
	}

	private Iterable<? extends JavaFileObject> getJavaFileObjects(Class<?>... types) {
		File[] files = new File[types.length];
		for (int i = 0; i < types.length; i++) {
			files[i] = getFile(types[i]);
		}
		return this.fileManager.getJavaFileObjects(files);
	}

	protected File getFile(Class<?> type) {
		return new File(getSourceFolder(), sourcePathFor(type));
	}

	public static String sourcePathFor(Class<?> type) {
		return type.getName().replace('.', '/') + ".java";
	}

	protected File getSourceFolder() {
		return SOURCE_FOLDER;
	}

	/**
	 * A compilation task.
	 */
	public static class TestCompilationTask {

		private final CompilationTask task;

		public TestCompilationTask(CompilationTask task) {
			this.task = task;
		}

		public void call(Processor... processors) {
			this.task.setProcessors(Arrays.asList(processors));
			if (!this.task.call()) {
				throw new IllegalStateException("Compilation failed");
			}
		}

	}

}
