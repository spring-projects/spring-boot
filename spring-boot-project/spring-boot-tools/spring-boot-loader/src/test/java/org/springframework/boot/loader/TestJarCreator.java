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
			writeEntry(jarOutputStream, "multi-release.dat", 8);
			writeEntry(jarOutputStream, "META-INF/versions/9/multi-release.dat", 9);
			writeEntry(jarOutputStream, "META-INF/versions/10/multi-release.dat", 10);
			writeEntry(jarOutputStream, "META-INF/versions/11/multi-release.dat", 11);
			writeEntry(jarOutputStream, "META-INF/versions/12/multi-release.dat", 12);
			writeEntry(jarOutputStream, "META-INF/versions/13/multi-release.dat", 13);
			writeEntry(jarOutputStream, "META-INF/versions/14/multi-release.dat", 14);
			writeEntry(jarOutputStream, "META-INF/versions/15/multi-release.dat", 15);
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
