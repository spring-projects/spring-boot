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

package org.springframework.boot.loader.tools;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.tools.MainClassFinder.MainClass;
import org.springframework.boot.loader.tools.MainClassFinder.MainClassCallback;
import org.springframework.boot.loader.tools.sample.AnnotatedClassWithMainMethod;
import org.springframework.boot.loader.tools.sample.ClassWithMainMethod;
import org.springframework.boot.loader.tools.sample.ClassWithoutMainMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link MainClassFinder}.
 *
 * @author Phillip Webb
 */
class MainClassFinderTests {

	private TestJarFile testJarFile;

	@BeforeEach
	void setup(@TempDir File tempDir) {
		this.testJarFile = new TestJarFile(tempDir);
	}

	@Test
	void findMainClassInJar() throws Exception {
		this.testJarFile.addClass("B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("A.class", ClassWithoutMainMethod.class);
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			String actual = MainClassFinder.findMainClass(jarFile, "");
			assertThat(actual).isEqualTo("B");
		}
	}

	@Test
	void findMainClassInJarSubDirectory() throws Exception {
		this.testJarFile.addClass("a/b/c/D.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/F.class", ClassWithoutMainMethod.class);
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			String actual = MainClassFinder.findMainClass(jarFile, "");
			assertThat(actual).isEqualTo("a.b.c.D");
		}
	}

	@Test
	void usesBreadthFirstJarSearch() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			String actual = MainClassFinder.findMainClass(jarFile, "");
			assertThat(actual).isEqualTo("a.B");
		}
	}

	@Test
	void findSingleJarSearch() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			assertThatIllegalStateException().isThrownBy(() -> MainClassFinder.findSingleMainClass(jarFile, ""))
				.withMessageContaining(
						"Unable to find a single main class from the following candidates [a.B, a.b.c.E]");
		}
	}

	@Test
	void findSingleJarSearchPrefersAnnotatedMainClass() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", AnnotatedClassWithMainMethod.class);
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			String mainClass = MainClassFinder.findSingleMainClass(jarFile, "",
					"org.springframework.boot.loader.tools.sample.SomeApplication");
			assertThat(mainClass).isEqualTo("a.b.c.E");
		}
	}

	@Test
	void findMainClassInJarSubLocation() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			String actual = MainClassFinder.findMainClass(jarFile, "a/");
			assertThat(actual).isEqualTo("B");
		}

	}

	@Test
	void findMainClassInDirectory() throws Exception {
		this.testJarFile.addClass("B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("A.class", ClassWithoutMainMethod.class);
		String actual = MainClassFinder.findMainClass(this.testJarFile.getJarSource());
		assertThat(actual).isEqualTo("B");
	}

	@Test
	void findMainClassInSubDirectory() throws Exception {
		this.testJarFile.addClass("a/b/c/D.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/F.class", ClassWithoutMainMethod.class);
		String actual = MainClassFinder.findMainClass(this.testJarFile.getJarSource());
		assertThat(actual).isEqualTo("a.b.c.D");
	}

	@Test
	void usesBreadthFirstDirectorySearch() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		String actual = MainClassFinder.findMainClass(this.testJarFile.getJarSource());
		assertThat(actual).isEqualTo("a.B");
	}

	@Test
	void findSingleDirectorySearch() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithMainMethod.class);
		assertThatIllegalStateException()
			.isThrownBy(() -> MainClassFinder.findSingleMainClass(this.testJarFile.getJarSource()))
			.withMessageContaining("Unable to find a single main class from the following candidates [a.B, a.b.c.E]");
	}

	@Test
	void findSingleDirectorySearchPrefersAnnotatedMainClass() throws Exception {
		this.testJarFile.addClass("a/B.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", AnnotatedClassWithMainMethod.class);
		String mainClass = MainClassFinder.findSingleMainClass(this.testJarFile.getJarSource(),
				"org.springframework.boot.loader.tools.sample.SomeApplication");
		assertThat(mainClass).isEqualTo("a.b.c.E");
	}

	@Test
	void doWithDirectoryMainMethods() throws Exception {
		this.testJarFile.addClass("a/b/c/D.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/F.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/G.class", ClassWithMainMethod.class);
		ClassNameCollector callback = new ClassNameCollector();
		MainClassFinder.doWithMainClasses(this.testJarFile.getJarSource(), callback);
		assertThat(callback.getClassNames()).hasToString("[a.b.G, a.b.c.D]");
	}

	@Test
	void doWithJarMainMethods() throws Exception {
		this.testJarFile.addClass("a/b/c/D.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/c/E.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/F.class", ClassWithoutMainMethod.class);
		this.testJarFile.addClass("a/b/G.class", ClassWithMainMethod.class);
		ClassNameCollector callback = new ClassNameCollector();
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			MainClassFinder.doWithMainClasses(jarFile, null, callback);
			assertThat(callback.getClassNames()).hasToString("[a.b.G, a.b.c.D]");
		}
	}

	@Test
	void packagePrivateMainMethod() throws Exception {
		this.testJarFile.addFile("a/b/c/D.class", packagePrivateMainMethod(ClassFileVersion.JAVA_V25));
		ClassNameCollector callback = new ClassNameCollector();
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			MainClassFinder.doWithMainClasses(jarFile, null, callback);
			assertThat(callback.getClassNames()).hasToString("[a.b.c.D]");
		}
	}

	@Test
	void packagePrivateMainMethodBeforeJava25() throws Exception {
		this.testJarFile.addFile("a/b/c/D.class", packagePrivateMainMethod(ClassFileVersion.JAVA_V24));
		ClassNameCollector callback = new ClassNameCollector();
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			MainClassFinder.doWithMainClasses(jarFile, null, callback);
			assertThat(callback.getClassNames()).isEmpty();
		}
	}

	@Test
	void parameterlessMainMethod() throws Exception {
		this.testJarFile.addFile("a/b/c/D.class", parameterlessMainMethod(ClassFileVersion.JAVA_V25));
		ClassNameCollector callback = new ClassNameCollector();
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			MainClassFinder.doWithMainClasses(jarFile, null, callback);
			assertThat(callback.getClassNames()).hasToString("[a.b.c.D]");
		}
	}

	@Test
	void parameterlessMainMethodBeforeJava25() throws Exception {
		this.testJarFile.addFile("a/b/c/D.class", parameterlessMainMethod(ClassFileVersion.JAVA_V24));
		ClassNameCollector callback = new ClassNameCollector();
		try (JarFile jarFile = this.testJarFile.getJarFile()) {
			MainClassFinder.doWithMainClasses(jarFile, null, callback);
			assertThat(callback.getClassNames()).isEmpty();
		}
	}

	private ByteArrayInputStream packagePrivateMainMethod(ClassFileVersion classFileVersion) {
		byte[] bytecode = new ByteBuddy(classFileVersion).subclass(Object.class)
			.defineMethod("main", void.class, Modifier.STATIC)
			.withParameter(String[].class)
			.intercept(new EmptyBodyImplementation())
			.make()
			.getBytes();
		return new ByteArrayInputStream(bytecode);
	}

	private ByteArrayInputStream parameterlessMainMethod(ClassFileVersion classFileVersion) {
		byte[] bytecode = new ByteBuddy(classFileVersion).subclass(Object.class)
			.defineMethod("main", void.class, Modifier.STATIC | Modifier.PUBLIC)
			.intercept(new EmptyBodyImplementation())
			.make()
			.getBytes();
		return new ByteArrayInputStream(bytecode);
	}

	static class EmptyBodyImplementation implements Implementation {

		@Override
		public InstrumentedType prepare(InstrumentedType instrumentedType) {
			return instrumentedType;
		}

		@Override
		public ByteCodeAppender appender(Target implementationTarget) {
			return new ByteCodeAppender() {

				@Override
				public Size apply(MethodVisitor methodVisitor, Context implementationContext,
						MethodDescription instrumentedMethod) {
					methodVisitor.visitInsn(Opcodes.RETURN);
					return Size.ZERO;
				}

			};
		}

	}

	static class ClassNameCollector implements MainClassCallback<Object> {

		private final List<String> classNames = new ArrayList<>();

		@Override
		public Object doWith(MainClass mainClass) {
			this.classNames.add(mainClass.getName());
			return null;
		}

		List<String> getClassNames() {
			return this.classNames;
		}

	}

}
