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

package org.springframework.boot.loader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

/**
 * Creates a simple test jar.
 *
 * @author Phillip Webb
 */
public abstract class TestJarCreator {

	private static final int BASE_VERSION = 8;

	private static final int RUNTIME_VERSION;

	static {
		int version;
		try {
			Object runtimeVersion = Runtime.class.getMethod("version").invoke(null);
			version = (int) runtimeVersion.getClass().getMethod("major").invoke(runtimeVersion);
		}
		catch (Throwable ex) {
			version = BASE_VERSION;
		}
		RUNTIME_VERSION = version;
	}

	public static void createTestJar(File file) throws Exception {
		createTestJar(file, false);
	}

	public static void createTestJar(File file, boolean unpackNested) throws Exception {
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		try (JarOutputStream jarOutputStream = new JarOutputStream(fileOutputStream)) {
			jarOutputStream.setComment("outer");
			writeManifest(jarOutputStream, "j1");
			writeEntry(jarOutputStream, "1.dat", 1);
			writeEntry(jarOutputStream, "2.dat", 2);
			writeDirEntry(jarOutputStream, "d/");
			writeEntry(jarOutputStream, "d/9.dat", 9);
			writeDirEntry(jarOutputStream, "special/");
			writeEntry(jarOutputStream, "special/\u00EB.dat", '\u00EB');
			writeNestedEntry("nested.jar", unpackNested, jarOutputStream);
			writeNestedEntry("another-nested.jar", unpackNested, jarOutputStream);
			writeNestedEntry("space nested.jar", unpackNested, jarOutputStream);
			writeNestedMultiReleaseEntry("multi-release.jar", unpackNested, jarOutputStream);
		}
	}

	private static void writeNestedEntry(String name, boolean unpackNested, JarOutputStream jarOutputStream)
			throws Exception {
		writeNestedEntry(name, unpackNested, jarOutputStream, false);
	}

	private static void writeNestedMultiReleaseEntry(String name, boolean unpackNested, JarOutputStream jarOutputStream)
			throws Exception {
		writeNestedEntry(name, unpackNested, jarOutputStream, true);
	}

	private static void writeNestedEntry(String name, boolean unpackNested, JarOutputStream jarOutputStream,
			boolean multiRelease) throws Exception {
		JarEntry nestedEntry = new JarEntry(name);
		byte[] nestedJarData = getNestedJarData(multiRelease);
		nestedEntry.setSize(nestedJarData.length);
		nestedEntry.setCompressedSize(nestedJarData.length);
		if (unpackNested) {
			nestedEntry.setComment("UNPACK:0000000000000000000000000000000000000000");
		}
		CRC32 crc32 = new CRC32();
		crc32.update(nestedJarData);
		nestedEntry.setCrc(crc32.getValue());
		nestedEntry.setMethod(ZipEntry.STORED);
		jarOutputStream.putNextEntry(nestedEntry);
		jarOutputStream.write(nestedJarData);
		jarOutputStream.closeEntry();
	}

	private static byte[] getNestedJarData(boolean multiRelease) throws Exception {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		JarOutputStream jarOutputStream = new JarOutputStream(byteArrayOutputStream);
		jarOutputStream.setComment("nested");
		writeManifest(jarOutputStream, "j2", multiRelease);
		if (multiRelease) {
			writeEntry(jarOutputStream, "multi-release.dat", BASE_VERSION);
			writeEntry(jarOutputStream, String.format("META-INF/versions/%d/multi-release.dat", RUNTIME_VERSION),
					RUNTIME_VERSION);
		}
		else {
			writeEntry(jarOutputStream, "3.dat", 3);
			writeEntry(jarOutputStream, "4.dat", 4);
			writeEntry(jarOutputStream, "\u00E4.dat", '\u00E4');
		}
		jarOutputStream.close();
		return byteArrayOutputStream.toByteArray();
	}

	private static void writeManifest(JarOutputStream jarOutputStream, String name) throws Exception {
		writeManifest(jarOutputStream, name, false);
	}

	private static void writeManifest(JarOutputStream jarOutputStream, String name, boolean multiRelease)
			throws Exception {
		writeDirEntry(jarOutputStream, "META-INF/");
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().putValue("Built-By", name);
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		if (multiRelease) {
			manifest.getMainAttributes().putValue("Multi-Release", Boolean.toString(true));
		}
		jarOutputStream.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
		manifest.write(jarOutputStream);
		jarOutputStream.closeEntry();
	}

	private static void writeDirEntry(JarOutputStream jarOutputStream, String name) throws IOException {
		jarOutputStream.putNextEntry(new JarEntry(name));
		jarOutputStream.closeEntry();
	}

	private static void writeEntry(JarOutputStream jarOutputStream, String name, int data) throws IOException {
		jarOutputStream.putNextEntry(new JarEntry(name));
		jarOutputStream.write(new byte[] { (byte) data });
		jarOutputStream.closeEntry();
	}

}
