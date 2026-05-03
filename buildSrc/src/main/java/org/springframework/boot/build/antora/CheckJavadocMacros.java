/*
 * Copyright 2025 the original author or authors.
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

package org.springframework.boot.build.antora;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationException;

import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.FieldVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.asm.Type;
import org.springframework.util.function.ThrowingConsumer;

/**
 * A task to check {@code javadoc:[]} macros in Antora source files.
 *
 * @author Andy Wilkinson
 */
public abstract class CheckJavadocMacros extends DefaultTask {

	private static final Pattern JAVADOC_MACRO_PATTERN = Pattern.compile("javadoc:(.*?)\\[(.*?)\\]");

	private final Path projectRoot;

	private FileCollection source;

	private FileCollection classpath;

	public CheckJavadocMacros() {
		this.projectRoot = getProject().getRootDir().toPath();
	}

	@InputFiles
	public FileCollection getSource() {
		return this.source;
	}

	public void setSource(FileCollection source) {
		this.source = source;
	}

	@Optional
	@Classpath
	public FileCollection getClasspath() {
		return this.classpath;
	}

	public void setClasspath(FileCollection classpath) {
		this.classpath = classpath;
	}

	@OutputDirectory
	public abstract DirectoryProperty getOutputDirectory();

	@TaskAction
	void checkJavadocMacros() {
		Set<String> availableClasses = indexClasspath();
		List<String> problems = new ArrayList<>();
		this.source.getAsFileTree()
			.filter((file) -> file.getName().endsWith(".adoc"))
			.forEach((file) -> problems.addAll(checkJavadocMacros(file, availableClasses)));
		File outputFile = getOutputDirectory().file("failure-report.txt").get().getAsFile();
		writeReport(problems, outputFile);
		if (!problems.isEmpty()) {
			throw new VerificationException("Javadoc macro check failed. See '%s' for details".formatted(outputFile));
		}
	}

	private Set<String> indexClasspath() {
		Set<String> availableClasses = StreamSupport.stream(this.classpath.spliterator(), false).flatMap((root) -> {
			if (root.isFile()) {
				try (JarFile jar = new JarFile(root)) {
					return jar.stream()
						.map(JarEntry::getName)
						.filter((entryName) -> entryName.endsWith(".class"))
						.map((className) -> {
							if (className.startsWith("META-INF/versions/")) {
								className = className.substring("META-INF/versions/".length());
							}
							className = className.substring(0, className.length() - ".class".length());
							className = className.replace('/', '.');
							return className;
						})
						.toList()
						.stream();
				}
				catch (IOException ex) {
					throw new UncheckedIOException(ex);
				}
			}
			return Stream.empty();
		}).collect(Collectors.toSet());
		return availableClasses;
	}

	private List<String> checkJavadocMacros(File adocFile, Set<String> availableClasses) {
		List<String> problems = new ArrayList<>();
		List<JavadocMacro> macros = JavadocMacro.parse(adocFile);
		for (JavadocMacro macro : macros) {
			if (!classIsAvailable(macro.className.name, availableClasses)) {
				problems.add(this.projectRoot.relativize(macro.className.origin.file.toPath()) + ":"
						+ macro.className.origin.line + ":" + macro.className.origin.column + ": class "
						+ macro.className.name + " does not exist.");
			}
			else {
				JavadocAnchor anchor = macro.anchor;
				if (anchor != null) {
					if (anchor instanceof MethodAnchor methodAnchor) {
						MethodMatcher methodMatcher = new MethodMatcher(methodAnchor);
						inputStreamOf(macro.className.name, (stream) -> {
							ClassReader reader = new ClassReader(stream);
							reader.accept(methodMatcher, 0);
						});
						if (!methodMatcher.matched) {
							problems.add(this.projectRoot.relativize(macro.anchor.origin.file.toPath()) + ":"
									+ macro.anchor.origin.line + ":" + methodAnchor.origin().column + ": method "
									+ methodAnchor + " does not exist");
						}
					}
					else if (anchor instanceof FieldAnchor fieldAnchor) {
						FieldMatcher fieldMatcher = new FieldMatcher(fieldAnchor);
						inputStreamOf(macro.className.name, (stream) -> {
							ClassReader reader = new ClassReader(stream);
							reader.accept(fieldMatcher, 0);
						});
						if (!fieldMatcher.matched) {
							problems.add(this.projectRoot.relativize(macro.anchor.origin.file.toPath()) + ":"
									+ macro.anchor.origin.line + ":" + fieldAnchor.origin().column + ": field "
									+ fieldAnchor.name + " does not exist");
						}
					}
				}
			}
		}
		return problems;
	}

	private boolean classIsAvailable(String className, Set<String> availableClasses) {
		if (availableClasses.contains(className)) {
			return true;
		}
		if (className.startsWith("java.") || className.startsWith("javax.")) {
			return jdkResourceForClass(className) != null;
		}
		return false;
	}

	private URL jdkResourceForClass(String className) {
		return getClass().getClassLoader().getResource(className.replace(".", "/") + ".class");
	}

	private void inputStreamOf(String className, ThrowingConsumer<InputStream> streamHandler) {
		for (File root : this.classpath) {
			if (root.isFile()) {
				try (JarFile jar = new JarFile(root)) {
					ZipEntry entry = jar.getEntry(className.replace(".", "/") + ".class");
					if (entry != null) {
						try (InputStream stream = jar.getInputStream(entry)) {
							streamHandler.accept(stream);
						}
						return;
					}
				}
				catch (IOException ex) {
					throw new UncheckedIOException(ex);
				}
			}
		}
		URL resource = jdkResourceForClass(className);
		if (resource != null) {
			try (InputStream stream = resource.openStream()) {
				streamHandler.accept(stream);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}
	}

	private void writeReport(List<String> problems, File outputFile) {
		outputFile.getParentFile().mkdirs();
		StringBuilder report = new StringBuilder();
		if (!problems.isEmpty()) {
			if (problems.size() == 1) {
				report.append("Found 1 javadoc macro problem:%n".formatted());
			}
			else {
				report.append("Found %d javadoc macro problems:%n".formatted(problems.size()));
			}
			problems.forEach((problem) -> report.append("%s%n".formatted(problem)));
		}
		try {
			Files.writeString(outputFile.toPath(), report.toString(), StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static final class JavadocMacro {

		private final ClassName className;

		private final JavadocAnchor anchor;

		private JavadocMacro(ClassName className, JavadocAnchor anchor) {
			this.className = className;
			this.anchor = anchor;
		}

		private static List<JavadocMacro> parse(File adocFile) {
			List<JavadocMacro> macros = new ArrayList<>();
			try {
				Path adocFilePath = adocFile.toPath();
				List<String> lines = Files.readAllLines(adocFilePath);
				for (int i = 0; i < lines.size(); i++) {
					Matcher matcher = JAVADOC_MACRO_PATTERN.matcher(lines.get(i));
					while (matcher.find()) {
						Origin classNameOrigin = new Origin(adocFile, i + 1, matcher.start(1) + 1);
						String target = matcher.group(1);
						String className = target;
						int endOfUrlAttribute = className.indexOf("}/");
						if (endOfUrlAttribute != -1) {
							className = className.substring(endOfUrlAttribute + 2);
						}
						JavadocAnchor anchor = null;
						int anchorIndex = className.indexOf("#");
						if (anchorIndex != -1) {
							anchor = JavadocAnchor.of(className.substring(anchorIndex + 1), new Origin(adocFile,
									classNameOrigin.line(), classNameOrigin.column + anchorIndex + 1));
							className = className.substring(0, anchorIndex);
						}
						macros.add(new JavadocMacro(new ClassName(classNameOrigin, className), anchor));
					}
				}
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
			return macros;
		}

	}

	private static final class ClassName {

		private final Origin origin;

		private final String name;

		private ClassName(Origin origin, String name) {
			this.origin = origin;
			this.name = name;
		}

	}

	private record Origin(File file, int line, int column) {

	}

	private abstract static class JavadocAnchor {

		private final Origin origin;

		protected JavadocAnchor(Origin origin) {
			this.origin = origin;
		}

		Origin origin() {
			return this.origin;
		}

		private static JavadocAnchor of(String anchor, Origin origin) {
			JavadocAnchor javadocAnchor = WellKnownAnchor.of(anchor, origin);
			if (javadocAnchor == null) {
				javadocAnchor = MethodAnchor.of(anchor, origin);
			}
			if (javadocAnchor == null) {
				javadocAnchor = FieldAnchor.of(anchor, origin);
			}
			return javadocAnchor;
		}

	}

	private static final class WellKnownAnchor extends JavadocAnchor {

		private WellKnownAnchor(Origin origin) {
			super(origin);
		}

		private static WellKnownAnchor of(String anchor, Origin origin) {
			if (anchor.equals("enum-constant-summary")) {
				return new WellKnownAnchor(origin);
			}
			return null;
		}

	}

	private static final class MethodAnchor extends JavadocAnchor {

		private final String name;

		private final List<String> arguments;

		private MethodAnchor(String name, List<String> arguments, Origin origin) {
			super(origin);
			this.name = name;
			this.arguments = arguments;
		}

		@Override
		public String toString() {
			return this.name + "(" + String.join(", ", this.arguments + ")");
		}

		static MethodAnchor of(String anchor, Origin origin) {
			if (!anchor.contains("(")) {
				return null;
			}
			int openingIndex = anchor.indexOf('(');
			String name = anchor.substring(0, openingIndex);
			List<String> arguments = Stream.of(anchor.substring(openingIndex + 1, anchor.length() - 1).split(","))
				.map(String::trim)
				.map((argument) -> argument.endsWith("...") ? argument.replace("...", "[]") : argument)
				.toList();
			return new MethodAnchor(name, arguments, origin);
		}

	}

	private static final class FieldAnchor extends JavadocAnchor {

		private final String name;

		private FieldAnchor(String name, Origin origin) {
			super(origin);
			this.name = name;
		}

		static FieldAnchor of(String anchor, Origin origin) {
			return new FieldAnchor(anchor, origin);
		}

	}

	private static final class MethodMatcher extends ClassVisitor {

		private final MethodAnchor methodAnchor;

		private boolean matched = false;

		private MethodMatcher(MethodAnchor methodAnchor) {
			super(SpringAsmInfo.ASM_VERSION);
			this.methodAnchor = methodAnchor;
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
				String[] exceptions) {
			if (!this.matched && name.equals(this.methodAnchor.name)) {
				Type type = Type.getType(descriptor);
				if (type.getArgumentCount() == this.methodAnchor.arguments.size()) {
					List<String> argumentTypeNames = Arrays.asList(type.getArgumentTypes())
						.stream()
						.map(Type::getClassName)
						.toList();
					if (argumentTypeNames.equals(this.methodAnchor.arguments)) {
						this.matched = true;
					}
				}
			}
			return null;
		}

	}

	private static final class FieldMatcher extends ClassVisitor {

		private final FieldAnchor fieldAnchor;

		private boolean matched = false;

		private FieldMatcher(FieldAnchor fieldAnchor) {
			super(SpringAsmInfo.ASM_VERSION);
			this.fieldAnchor = fieldAnchor;
		}

		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			if (!this.matched && name.equals(this.fieldAnchor.name)) {
				this.matched = true;
			}
			return null;
		}

	}

}
