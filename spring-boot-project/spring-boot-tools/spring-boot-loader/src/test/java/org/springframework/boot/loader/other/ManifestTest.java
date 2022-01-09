package org.springframework.boot.loader.other;

import org.junit.Test;

import java.io.*;
import java.util.jar.Manifest;

public class ManifestTest {

	@Test
	public void test1() throws IOException {
		Manifest manifest = new Manifest();

		System.out.println(new File("target/test-classes/MANIFEST.MF").getAbsolutePath());
		FileInputStream inputStream  = new FileInputStream("target/test-classes/MANIFEST.MF");
		manifest.read(inputStream);
		System.out.println(manifest);

		manifest.getEntries().put("testentry", manifest.getMainAttributes());

		manifest.write(new FileOutputStream("d:/aaa.MF"));
	}

}
