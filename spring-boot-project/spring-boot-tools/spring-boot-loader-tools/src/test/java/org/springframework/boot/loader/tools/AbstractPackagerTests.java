/*
 * Copyright 2012-2022 the original author or authors.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.zeroturnaround.zip.ZipUtil;

import org.springframework.boot.loader.tools.sample.ClassWithMainMethod;
import org.springframework.boot.loader.tools.sample.ClassWithoutMainMethod;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Abstract class for {@link Packager} based tests.
 *
 * @param <P> The packager type
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
abstract class AbstractPackagerTests<P extends Packager> {

	protected static final Libraries NO_LIBRARIES = (callback) -> {
	};

	private static final long JAN_1_1980;
	static {
		Calendar calendar = Calendar.getInstance();
		calendar.set(1980, 0, 1, 0, 0, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		JAN_1_1980 = calendar.getTime().getTime();
	}

	private static final long JAN_1_1985;
	static {
		Calendar calendar = Calendar.getInstance();
		calendar.set(1985, 0, 1, 0, 0, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		JAN_1_1985 = calendar.getTime().getTime();
	}

	@TempDir
	File tempDir;

	protected TestJarFile testJarFile;

	@BeforeEach
	void setup() throws IOException {
		this.testJarFile = new TestJarFile(this.tempDir);
	}

	@Test
	void specificMainClass() throws Exception {
		this.testJarFile.addClass("a/b/C.class", ClassWithoutMainMethod.class);
		P packager = createPackager();
		packager.setMainClass("a.b.C");
		execute(packager, NO_LIBRARIES);
		Manifest actualManifest = getPackagedManifest();
		assertThat(actualManifest.getMainAttributes().getValue("Main-Class"))
				.isEqualTo("org.springframework.boot.loader.JarLauncher");
		assertThat(actualManifest.getMainAttributes().getValue("Start-Class")).isEqualTo("a.b.C");
		assertThat(hasPackagedLauncherClasses()).isTrue();
	}

	@Test
	void mainClassFromManifest() throws Exception {
		this.testJarFile.addClass("a/b/C.class", ClassWithoutMainMethod.class);
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
		manifest.getMainAttributes().putValue("Main-Class", "a.b.C");
		this.testJarFile.addManifest(manifest);
		P packager = createPackager();
		execute(packager, NO_LIBRARIES);
		Manifest actualManifest = getPackagedManifest();
		assertThat(actualManifest.getMainAttributes().getValue("Main-Class"))
				.isEqualTo("org.springframework.boot.loader.JarLauncher");
		assertThat(actualManifest.getMainAttributes().getValue("Start-Class")).isEqualTo("a.b.C");
		assertThat(hasPackagedLauncherClasses()).isTrue();
	}

	@Test
	void mainClassFound() throws Exception {
		this.testJarFile.addClass("a/b/C.class", ClassWithMainMethod.class);
		P packager = createPackager();
		execute(packager, NO_LIBRARIES);
		Manifest actualManifest = getPackagedManifest();
		assertThat(actualManifest.getMainAttributes().getValue("Main-Class"))
				.isEqualTo("org.springframework.boot.loader.JarLauncher");
		assertThat(actualManifest.getMainAttributes().getValue("Start-Class")).isEqualTo("a.b.C");
		assertThat(hasPackagedLauncherClasses()).isTrue();
	}

	@Test
	void multipleMainClassFound() throws Exception {
		this.testJarFile.addClass("a/b/C.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("a/b/D.class", ClassWithMainMethod.class);
		P packager = createPackager();
		assertThatIllegalStateException().isThrownBy(() -> execute(packager, NO_LIBRARIES)).withMessageContaining(
				"Unable to find a single main class from the following candidates [a.b.C, a.b.D]");
	}

	@Test
	void noMainClass() throws Exception {
		this.testJarFile.addClass("a/b/C.class", ClassWithoutMainMethod.class);
		P packager = createPackager(this.testJarFile.getFile());
		assertThatIllegalStateException().isThrownBy(() -> execute(packager, NO_LIBRARIES))
				.withMessageContaining("Unable to find main class");
	}

	@Test
	void noMainClassAndLayoutIsNone() throws Exception {
		this.testJarFile.addClass("a/b/C.class", ClassWithMainMethod.class);
		P packager = createPackager();
		packager.setLayout(new Layouts.None());
		execute(packager, NO_LIBRARIES);
		Manifest actualManifest = getPackagedManifest();
		assertThat(actualManifest.getMainAttributes().getValue("Main-Class")).isEqualTo("a.b.C");
		assertThat(hasPackagedLauncherClasses()).isFalse();
	}

	@Test
	void noMainClassAndLayoutIsNoneWithNoMain() throws Exception {
		this.testJarFile.addClass("a/b/C.class", ClassWithoutMainMethod.class);
		P packager = createPackager();
		packager.setLayout(new Layouts.None());
		execute(packager, NO_LIBRARIES);
		Manifest actualManifest = getPackagedManifest();
		assertThat(actualManifest.getMainAttributes().getValue("Main-Class")).isNull();
		assertThat(hasPackagedLauncherClasses()).isFalse();
	}

	@Test
	void nullLibraries() throws Exception {
		this.testJarFile.addClass("a/b/C.class", ClassWithMainMethod.class);
		P packager = createPackager();
		assertThatIllegalArgumentException().isThrownBy(() -> execute(packager, null))
				.withMessageContaining("Libraries must not be null");
	}

	@Test
	void libraries() throws Exception {
		TestJarFile libJar = new TestJarFile(this.tempDir);
		libJar.addClass("a/b/C.class", ClassWithoutMainMethod.class, JAN_1_1985);
		File libJarFile = libJar.getFile();
		File libJarFileToUnpack = libJar.getFile();
		File libNonJarFile = new File(this.tempDir, "non-lib.jar");
		FileCopyUtils.copy(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 }, libNonJarFile);
		this.testJarFile.addClass("a/b/C.class", ClassWithMainMethod.class);
		this.testJarFile.addFile("BOOT-INF/lib/" + libJarFileToUnpack.getName(), libJarFileToUnpack);
		libJarFile.setLastModified(JAN_1_1980);
		P packager = createPackager();
		execute(packager, (callback) -> {
			callback.library(newLibrary(libJarFile, LibraryScope.COMPILE, false));
			callback.library(newLibrary(libJarFileToUnpack, LibraryScope.COMPILE, true));
			callback.library(newLibrary(libNonJarFile, LibraryScope.COMPILE, false));
		});
		assertThat(hasPackagedEntry("BOOT-INF/lib/" + libJarFile.getName())).isTrue();
		assertThat(hasPackagedEntry("BOOT-INF/lib/" + libJarFileToUnpack.getName())).isTrue();
		assertThat(hasPackagedEntry("BOOT-INF/lib/" + libNonJarFile.getName())).isFalse();
		ZipEntry entry = getPackagedEntry("BOOT-INF/lib/" + libJarFile.getName());
		assertThat(entry.getTime()).isEqualTo(JAN_1_1985);
		entry = getPackagedEntry("BOOT-INF/lib/" + libJarFileToUnpack.getName());
		assertThat(entry.getComment()).startsWith("UNPACK:");
		assertThat(entry.getComment()).hasSize(47);
	}

	@Test
	void classPathIndex() throws Exception {
		TestJarFile libJar1 = new TestJarFile(this.tempDir);
		libJar1.addClass("a/b/C.class", ClassWithoutMainMethod.class, JAN_1_1985);
		File libJarFile1 = libJar1.getFile();
		TestJarFile libJar2 = new TestJarFile(this.tempDir);
		libJar2.addClass("a/b/C.class", ClassWithoutMainMethod.class, JAN_1_1985);
		File libJarFile2 = libJar2.getFile();
		TestJarFile libJar3 = new TestJarFile(this.tempDir);
		libJar3.addClass("a/b/C.class", ClassWithoutMainMethod.class, JAN_1_1985);
		File libJarFile3 = libJar3.getFile();
		this.testJarFile.addClass("a/b/C.class", ClassWithMainMethod.class);
		File file = this.testJarFile.getFile();
		P packager = createPackager(file);
		execute(packager, (callback) -> {
			callback.library(newLibrary(libJarFile1, LibraryScope.COMPILE, false));
			callback.library(newLibrary(libJarFile2, LibraryScope.COMPILE, false));
			callback.library(newLibrary(libJarFile3, LibraryScope.COMPILE, false));
		});
		assertThat(hasPackagedEntry("BOOT-INF/classpath.idx")).isTrue();
		String index = getPackagedEntryContent("BOOT-INF/classpath.idx");
		String[] libraries = index.split("\\r?\\n");
		List<String> expected = Stream.of(libJarFile1, libJarFile2, libJarFile3)
				.map((jar) -> "- \"BOOT-INF/lib/" + jar.getName() + "\"").collect(Collectors.toList());
		assertThat(Arrays.asList(libraries)).containsExactlyElementsOf(expected);
	}

	@Test
	void layersIndex() throws Exception {
		TestJarFile libJar1 = new TestJarFile(this.tempDir);
		libJar1.addClass("a/b/C.class", ClassWithoutMainMethod.class, JAN_1_1985);
		File libJarFile1 = libJar1.getFile();
		TestJarFile libJar2 = new TestJarFile(this.tempDir);
		libJar2.addClass("a/b/C.class", ClassWithoutMainMethod.class, JAN_1_1985);
		File libJarFile2 = libJar2.getFile();
		TestJarFile libJar3 = new TestJarFile(this.tempDir);
		libJar3.addClass("a/b/C.class", ClassWithoutMainMethod.class, JAN_1_1985);
		File libJarFile3 = libJar3.getFile();
		this.testJarFile.addClass("a/b/C.class", ClassWithMainMethod.class);
		P packager = createPackager();
		TestLayers layers = new TestLayers();
		layers.addLibrary(libJarFile1, "0001");
		layers.addLibrary(libJarFile2, "0002");
		layers.addLibrary(libJarFile3, "0003");
		packager.setLayers(layers);
		packager.setIncludeRelevantJarModeJars(false);
		execute(packager, (callback) -> {
			callback.library(newLibrary(libJarFile1, LibraryScope.COMPILE, false));
			callback.library(newLibrary(libJarFile2, LibraryScope.COMPILE, false));
			callback.library(newLibrary(libJarFile3, LibraryScope.COMPILE, false));
		});
		assertThat(hasPackagedEntry("BOOT-INF/classpath.idx")).isTrue();
		String classpathIndex = getPackagedEntryContent("BOOT-INF/classpath.idx");
		List<String> expectedClasspathIndex = Stream.of(libJarFile1, libJarFile2, libJarFile3)
				.map((file) -> "- \"BOOT-INF/lib/" + file.getName() + "\"").collect(Collectors.toList());
		assertThat(Arrays.asList(classpathIndex.split("\\n"))).containsExactlyElementsOf(expectedClasspathIndex);
		assertThat(hasPackagedEntry("BOOT-INF/layers.idx")).isTrue();
		String layersIndex = getPackagedEntryContent("BOOT-INF/layers.idx");
		List<String> expectedLayers = new ArrayList<>();
		expectedLayers.add("- 'default':");
		expectedLayers.add("  - 'BOOT-INF/classes/'");
		expectedLayers.add("  - 'BOOT-INF/classpath.idx'");
		expectedLayers.add("  - 'BOOT-INF/layers.idx'");
		expectedLayers.add("  - 'META-INF/'");
		expectedLayers.add("  - 'org/'");
		expectedLayers.add("- '0001':");
		expectedLayers.add("  - 'BOOT-INF/lib/" + libJarFile1.getName() + "'");
		expectedLayers.add("- '0002':");
		expectedLayers.add("  - 'BOOT-INF/lib/" + libJarFile2.getName() + "'");
		expectedLayers.add("- '0003':");
		expectedLayers.add("  - 'BOOT-INF/lib/" + libJarFile3.getName() + "'");
		assertThat(layersIndex.split("\\n"))
				.containsExactly(expectedLayers.stream().map((s) -> s.replace('\'', '"')).toArray(String[]::new));
	}

	@Test
	void layersEnabledAddJarModeJar() throws Exception {
		this.testJarFile.addClass("a/b/C.class", ClassWithMainMethod.class);
		P packager = createPackager();
		TestLayers layers = new TestLayers();
		packager.setLayers(layers);
		execute(packager, Libraries.NONE);
		assertThat(hasPackagedEntry("BOOT-INF/classpath.idx")).isTrue();
		String classpathIndex = getPackagedEntryContent("BOOT-INF/classpath.idx");
		assertThat(Arrays.asList(classpathIndex.split("\\n")))
				.containsExactly("- \"BOOT-INF/lib/spring-boot-jarmode-layertools.jar\"");
		assertThat(hasPackagedEntry("BOOT-INF/layers.idx")).isTrue();
		String layersIndex = getPackagedEntryContent("BOOT-INF/layers.idx");
		List<String> expectedLayers = new ArrayList<>();
		expectedLayers.add("- 'default':");
		expectedLayers.add("  - 'BOOT-INF/'");
		expectedLayers.add("  - 'META-INF/'");
		expectedLayers.add("  - 'org/'");
		assertThat(layersIndex.split("\\n"))
				.containsExactly(expectedLayers.stream().map((s) -> s.replace('\'', '"')).toArray(String[]::new));
	}

	@Test
	void duplicateLibraries() throws Exception {
		TestJarFile libJar = new TestJarFile(this.tempDir);
		libJar.addClass("a/b/C.class", ClassWithoutMainMethod.class);
		File libJarFile = libJar.getFile();
		this.testJarFile.addClass("a/b/C.class", ClassWithMainMethod.class);
		P packager = createPackager();
		assertThatIllegalStateException().isThrownBy(() -> execute(packager, (callback) -> {
			callback.library(newLibrary(libJarFile, LibraryScope.COMPILE, false));
			callback.library(newLibrary(libJarFile, LibraryScope.COMPILE, false));
		})).withMessageContaining("Duplicate library");
	}

	@Test
	void customLayout() throws Exception {
		TestJarFile libJar = new TestJarFile(this.tempDir);
		libJar.addClass("a/b/C.class", ClassWithoutMainMethod.class);
		File libJarFile = libJar.getFile();
		this.testJarFile.addClass("a/b/C.class", ClassWithMainMethod.class);
		P packager = createPackager();
		Layout layout = mock(Layout.class);
		LibraryScope scope = mock(LibraryScope.class);
		given(layout.getLauncherClassName()).willReturn("testLauncher");
		given(layout.getLibraryLocation(anyString(), eq(scope))).willReturn("test/");
		given(layout.getLibraryLocation(anyString(), eq(LibraryScope.COMPILE))).willReturn("test-lib/");
		packager.setLayout(layout);
		execute(packager, (callback) -> callback.library(newLibrary(libJarFile, scope, false)));
		assertThat(hasPackagedEntry("test/" + libJarFile.getName())).isTrue();
		assertThat(getPackagedManifest().getMainAttributes().getValue("Spring-Boot-Lib")).isEqualTo("test-lib/");
		assertThat(getPackagedManifest().getMainAttributes().getValue("Main-Class")).isEqualTo("testLauncher");
	}

	@Test
	void customLayoutNoBootLib() throws Exception {
		TestJarFile libJar = new TestJarFile(this.tempDir);
		libJar.addClass("a/b/C.class", ClassWithoutMainMethod.class);
		File libJarFile = libJar.getFile();
		this.testJarFile.addClass("a/b/C.class", ClassWithMainMethod.class);
		P packager = createPackager();
		Layout layout = mock(Layout.class);
		LibraryScope scope = mock(LibraryScope.class);
		given(layout.getLauncherClassName()).willReturn("testLauncher");
		packager.setLayout(layout);
		execute(packager, (callback) -> callback.library(newLibrary(libJarFile, scope, false)));
		assertThat(getPackagedManifest().getMainAttributes().getValue("Spring-Boot-Lib")).isNull();
		assertThat(getPackagedManifest().getMainAttributes().getValue("Main-Class")).isEqualTo("testLauncher");
	}

	@Test
	void springBootVersion() throws Exception {
		this.testJarFile.addClass("a/b/C.class", ClassWithMainMethod.class);
		P packager = createPackager();
		execute(packager, NO_LIBRARIES);
		Manifest actualManifest = getPackagedManifest();
		assertThat(actualManifest.getMainAttributes()).containsKey(new Attributes.Name("Spring-Boot-Version"));
	}

	@Test
	void executableJarLayoutAttributes() throws Exception {
		this.testJarFile.addClass("a/b/C.class", ClassWithMainMethod.class);
		P packager = createPackager();
		execute(packager, NO_LIBRARIES);
		Manifest actualManifest = getPackagedManifest();
		assertThat(actualManifest.getMainAttributes()).containsEntry(new Attributes.Name("Spring-Boot-Lib"),
				"BOOT-INF/lib/");
		assertThat(actualManifest.getMainAttributes()).containsEntry(new Attributes.Name("Spring-Boot-Classes"),
				"BOOT-INF/classes/");
	}

	@Test
	void executableWarLayoutAttributes() throws Exception {
		this.testJarFile.addClass("WEB-INF/classes/a/b/C.class", ClassWithMainMethod.class);
		P packager = createPackager(this.testJarFile.getFile("war"));
		execute(packager, NO_LIBRARIES);
		Manifest actualManifest = getPackagedManifest();
		assertThat(actualManifest.getMainAttributes()).containsEntry(new Attributes.Name("Spring-Boot-Lib"),
				"WEB-INF/lib/");
		assertThat(actualManifest.getMainAttributes()).containsEntry(new Attributes.Name("Spring-Boot-Classes"),
				"WEB-INF/classes/");
	}

	@Test
	void nullCustomLayout() throws Exception {
		this.testJarFile.addClass("a/b/C.class", ClassWithoutMainMethod.class);
		Packager packager = createPackager();
		assertThatIllegalArgumentException().isThrownBy(() -> packager.setLayout(null))
				.withMessageContaining("Layout must not be null");
	}

	@Test
	void dontRecompressZips() throws Exception {
		TestJarFile nested = new TestJarFile(this.tempDir);
		nested.addClass("a/b/C.class", ClassWithoutMainMethod.class);
		File nestedFile = nested.getFile();
		this.testJarFile.addFile("test/nested.jar", nestedFile);
		this.testJarFile.addClass("A.class", ClassWithMainMethod.class);
		P packager = createPackager();
		execute(packager, (callback) -> callback.library(newLibrary(nestedFile, LibraryScope.COMPILE, false)));
		assertThat(getPackagedEntry("BOOT-INF/lib/" + nestedFile.getName()).getMethod()).isEqualTo(ZipEntry.STORED);
		assertThat(getPackagedEntry("BOOT-INF/classes/test/nested.jar").getMethod()).isEqualTo(ZipEntry.STORED);
	}

	@Test
	void unpackLibrariesTakePrecedenceOverExistingSourceEntries() throws Exception {
		TestJarFile nested = new TestJarFile(this.tempDir);
		nested.addClass("a/b/C.class", ClassWithoutMainMethod.class);
		File nestedFile = nested.getFile();
		String name = "BOOT-INF/lib/" + nestedFile.getName();
		this.testJarFile.addFile(name, nested.getFile());
		this.testJarFile.addClass("A.class", ClassWithMainMethod.class);
		P packager = createPackager();
		execute(packager, (callback) -> callback.library(newLibrary(nestedFile, LibraryScope.COMPILE, true)));
		assertThat(getPackagedEntry(name).getComment()).startsWith("UNPACK:");
	}

	@Test
	void existingSourceEntriesTakePrecedenceOverStandardLibraries() throws Exception {
		TestJarFile nested = new TestJarFile(this.tempDir);
		nested.addClass("a/b/C.class", ClassWithoutMainMethod.class);
		File nestedFile = nested.getFile();
		this.testJarFile.addFile("BOOT-INF/lib/" + nestedFile.getName(), nested.getFile());
		this.testJarFile.addClass("A.class", ClassWithMainMethod.class);
		P packager = createPackager();
		long sourceLength = nestedFile.length();
		execute(packager, (callback) -> {
			nestedFile.delete();
			File toZip = new File(this.tempDir, "to-zip");
			toZip.createNewFile();
			ZipUtil.packEntry(toZip, nestedFile);
			callback.library(newLibrary(nestedFile, LibraryScope.COMPILE, false));
		});
		assertThat(getPackagedEntry("BOOT-INF/lib/" + nestedFile.getName()).getSize()).isEqualTo(sourceLength);
	}

	@Test
	void metaInfIndexListIsRemovedFromRepackagedJar() throws Exception {
		this.testJarFile.addClass("A.class", ClassWithMainMethod.class);
		File indexList = new File(this.tempDir, "INDEX.LIST");
		indexList.createNewFile();
		this.testJarFile.addFile("META-INF/INDEX.LIST", indexList);
		P packager = createPackager();
		execute(packager, NO_LIBRARIES);
		assertThat(getPackagedEntry("META-INF/INDEX.LIST")).isNull();
	}

	@Test
	void customLayoutFactoryWithoutLayout() throws Exception {
		this.testJarFile.addClass("a/b/C.class", ClassWithMainMethod.class);
		P packager = createPackager();
		packager.setLayoutFactory(new TestLayoutFactory());
		execute(packager, NO_LIBRARIES);
		assertThat(getPackagedEntry("test")).isNotNull();
	}

	@Test
	void customLayoutFactoryWithLayout() throws Exception {
		this.testJarFile.addClass("a/b/C.class", ClassWithMainMethod.class);
		P packager = createPackager();
		packager.setLayoutFactory(new TestLayoutFactory());
		packager.setLayout(new Layouts.Jar());
		execute(packager, NO_LIBRARIES);
		assertThat(getPackagedEntry("test")).isNull();
	}

	@Test
	void metaInfAopXmlIsMovedBeneathBootInfClassesWhenRepackaged() throws Exception {
		this.testJarFile.addClass("A.class", ClassWithMainMethod.class);
		File aopXml = new File(this.tempDir, "aop.xml");
		aopXml.createNewFile();
		this.testJarFile.addFile("META-INF/aop.xml", aopXml);
		P packager = createPackager();
		execute(packager, NO_LIBRARIES);
		assertThat(getPackagedEntry("META-INF/aop.xml")).isNull();
		assertThat(getPackagedEntry("BOOT-INF/classes/META-INF/aop.xml")).isNotNull();
	}

	@Test
	void metaInfServicesFilesAreMovedBeneathBootInfClassesWhenRepackaged() throws Exception {
		this.testJarFile.addClass("A.class", ClassWithMainMethod.class);
		File service = new File(this.tempDir, "com.example.Service");
		service.createNewFile();
		this.testJarFile.addFile("META-INF/services/com.example.Service", service);
		P packager = createPackager();
		execute(packager, NO_LIBRARIES);
		assertThat(getPackagedEntry("META-INF/services/com.example.Service")).isNull();
		assertThat(getPackagedEntry("BOOT-INF/classes/META-INF/services/com.example.Service")).isNotNull();
	}

	@Test
	void allEntriesUseUnixPlatformAndUtf8NameEncoding() throws IOException {
		this.testJarFile.addClass("A.class", ClassWithMainMethod.class);
		P packager = createPackager();
		execute(packager, NO_LIBRARIES);
		for (ZipArchiveEntry entry : getAllPackagedEntries()) {
			assertThat(entry.getPlatform()).isEqualTo(ZipArchiveEntry.PLATFORM_UNIX);
			assertThat(entry.getGeneralPurposeBit().usesUTF8ForNames()).isTrue();
		}
	}

	@Test
	void loaderIsWrittenFirstThenApplicationClassesThenLibraries() throws IOException {
		this.testJarFile.addClass("com/example/Application.class", ClassWithMainMethod.class);
		File libraryOne = createLibraryJar();
		File libraryTwo = createLibraryJar();
		File libraryThree = createLibraryJar();
		P packager = createPackager();
		execute(packager, (callback) -> {
			callback.library(newLibrary(libraryOne, LibraryScope.COMPILE, false));
			callback.library(newLibrary(libraryTwo, LibraryScope.COMPILE, true));
			callback.library(newLibrary(libraryThree, LibraryScope.COMPILE, false));
		});
		assertThat(getPackagedEntryNames()).containsSubsequence("org/springframework/boot/loader/",
				"BOOT-INF/classes/com/example/Application.class", "BOOT-INF/lib/" + libraryOne.getName(),
				"BOOT-INF/lib/" + libraryTwo.getName(), "BOOT-INF/lib/" + libraryThree.getName());
	}

	@Test
	void existingEntryThatMatchesUnpackLibraryIsMarkedForUnpack() throws IOException {
		File library = createLibraryJar();
		this.testJarFile.addClass("WEB-INF/classes/com/example/Application.class", ClassWithMainMethod.class);
		this.testJarFile.addFile("WEB-INF/lib/" + library.getName(), library);
		P packager = createPackager(this.testJarFile.getFile("war"));
		packager.setLayout(new Layouts.War());
		execute(packager, (callback) -> callback.library(newLibrary(library, LibraryScope.COMPILE, true)));
		assertThat(getPackagedEntryNames()).containsSubsequence("org/springframework/boot/loader/",
				"WEB-INF/classes/com/example/Application.class", "WEB-INF/lib/" + library.getName());
		ZipEntry unpackLibrary = getPackagedEntry("WEB-INF/lib/" + library.getName());
		assertThat(unpackLibrary.getComment()).startsWith("UNPACK:");
	}

	@Test
	void layoutCanOmitLibraries() throws IOException {
		TestJarFile libJar = new TestJarFile(this.tempDir);
		libJar.addClass("a/b/C.class", ClassWithoutMainMethod.class);
		File libJarFile = libJar.getFile();
		this.testJarFile.addClass("a/b/C.class", ClassWithMainMethod.class);
		P packager = createPackager();
		Layout layout = mock(Layout.class);
		LibraryScope scope = mock(LibraryScope.class);
		packager.setLayout(layout);
		execute(packager, (callback) -> callback.library(newLibrary(libJarFile, scope, false)));
		assertThat(getPackagedEntryNames()).containsExactly("META-INF/", "META-INF/MANIFEST.MF", "a/", "a/b/",
				"a/b/C.class");
	}

	@Test
	void jarThatUsesCustomCompressionConfigurationCanBeRepackaged() throws IOException {
		File source = new File(this.tempDir, "source.jar");
		ZipOutputStream output = new ZipOutputStream(new FileOutputStream(source)) {
			{
				this.def = new Deflater(Deflater.NO_COMPRESSION, true);
			}
		};
		byte[] data = new byte[1024 * 1024];
		new Random().nextBytes(data);
		ZipEntry entry = new ZipEntry("entry.dat");
		output.putNextEntry(entry);
		output.write(data);
		output.closeEntry();
		output.close();
		P packager = createPackager(source);
		packager.setMainClass("com.example.Main");
		execute(packager, NO_LIBRARIES);
	}

	@Test
	void moduleInfoClassRemainsInRootOfJarWhenRepackaged() throws Exception {
		this.testJarFile.addClass("A.class", ClassWithMainMethod.class);
		this.testJarFile.addClass("module-info.class", ClassWithoutMainMethod.class);
		P packager = createPackager();
		execute(packager, NO_LIBRARIES);
		assertThat(getPackagedEntry("module-info.class")).isNotNull();
		assertThat(getPackagedEntry("BOOT-INF/classes/module-info.class")).isNull();
	}

	@Test
	void kotlinModuleMetadataMovesBeneathBootInfClassesWhenRepackaged() throws Exception {
		this.testJarFile.addClass("A.class", ClassWithMainMethod.class);
		File kotlinModule = new File(this.tempDir, "test.kotlin_module");
		kotlinModule.createNewFile();
		this.testJarFile.addFile("META-INF/test.kotlin_module", kotlinModule);
		P packager = createPackager();
		execute(packager, NO_LIBRARIES);
		assertThat(getPackagedEntry("META-INF/test.kotlin_module")).isNull();
		assertThat(getPackagedEntry("BOOT-INF/classes/META-INF/test.kotlin_module")).isNotNull();
	}

	@Test
	void entryFiltering() throws Exception {
		File webLibrary = createLibraryJar();
		File libraryOne = createLibraryJar();
		File libraryTwo = createLibraryJar();
		this.testJarFile.addClass("WEB-INF/classes/com/example/Application.class", ClassWithMainMethod.class);
		this.testJarFile.addFile("WEB-INF/lib/" + webLibrary.getName(), webLibrary);
		P packager = createPackager(this.testJarFile.getFile("war"));
		packager.setLayout(new Layouts.War());
		execute(packager, (callback) -> {
			callback.library(newLibrary(webLibrary, LibraryScope.COMPILE, false, false));
			callback.library(newLibrary(libraryOne, LibraryScope.COMPILE, false, false));
			callback.library(newLibrary(libraryTwo, LibraryScope.COMPILE, false, true));
		});
		Collection<String> packagedEntryNames = getPackagedEntryNames();
		packagedEntryNames.removeIf((name) -> !name.endsWith(".jar"));
		assertThat(packagedEntryNames).containsExactly("WEB-INF/lib/" + libraryTwo.getName());
	}

	private File createLibraryJar() throws IOException {
		TestJarFile library = new TestJarFile(this.tempDir);
		library.addClass("com/example/library/Library.class", ClassWithoutMainMethod.class);
		return library.getFile();
	}

	private Library newLibrary(File file, LibraryScope scope, boolean unpackRequired) {
		return new Library(null, file, scope, null, unpackRequired, false, true);
	}

	private Library newLibrary(File file, LibraryScope scope, boolean unpackRequired, boolean included) {
		return new Library(null, file, scope, null, unpackRequired, false, included);
	}

	protected final P createPackager() throws IOException {
		return createPackager(this.testJarFile.getFile());
	}

	protected abstract P createPackager(File source);

	protected abstract void execute(P packager, Libraries libraries) throws IOException;

	protected Collection<String> getPackagedEntryNames() throws IOException {
		return getAllPackagedEntries().stream().map(ZipArchiveEntry::getName).collect(Collectors.toList());
	}

	protected boolean hasPackagedLauncherClasses() throws IOException {
		return hasPackagedEntry("org/springframework/boot/")
				&& hasPackagedEntry("org/springframework/boot/loader/JarLauncher.class");
	}

	private boolean hasPackagedEntry(String name) throws IOException {
		return getPackagedEntry(name) != null;
	}

	protected ZipEntry getPackagedEntry(String name) throws IOException {
		return getAllPackagedEntries().stream().filter((entry) -> name.equals(entry.getName())).findFirst()
				.orElse(null);

	}

	protected abstract Collection<ZipArchiveEntry> getAllPackagedEntries() throws IOException;

	protected abstract Manifest getPackagedManifest() throws IOException;

	protected abstract String getPackagedEntryContent(String name) throws IOException;

	static class TestLayoutFactory implements LayoutFactory {

		@Override
		public Layout getLayout(File source) {
			return new TestLayout();
		}

	}

	static class TestLayout extends Layouts.Jar implements CustomLoaderLayout {

		@Override
		public void writeLoadedClasses(LoaderClassesWriter writer) throws IOException {
			writer.writeEntry("test", new ByteArrayInputStream("test".getBytes()));
		}

	}

	static class TestLayers implements Layers {

		private static final Layer DEFAULT_LAYER = new Layer("default");

		private Set<Layer> layers = new LinkedHashSet<>();

		private Map<String, Layer> libraries = new HashMap<>();

		TestLayers() {
			this.layers.add(DEFAULT_LAYER);
		}

		void addLibrary(File jarFile, String layerName) {
			Layer layer = new Layer(layerName);
			this.layers.add(layer);
			this.libraries.put(jarFile.getName(), layer);
		}

		@Override
		public Iterator<Layer> iterator() {
			return this.layers.iterator();
		}

		@Override
		public Stream<Layer> stream() {
			return this.layers.stream();
		}

		@Override
		public Layer getLayer(String name) {
			return DEFAULT_LAYER;
		}

		@Override
		public Layer getLayer(Library library) {
			String name = new File(library.getName()).getName();
			return this.libraries.getOrDefault(name, DEFAULT_LAYER);
		}

	}

}
