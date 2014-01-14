/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.cli;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.boot.cli.command.grab.CleanCommand;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Dave Syer
 */
public class GrapesCleaner {

	private static final String VERSION;
	static {
		try {
			File pom = new File("pom.xml");
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(pom);
			Element parent = (Element) document.getDocumentElement()
					.getElementsByTagName("parent").item(0);
			VERSION = parent.getElementsByTagName("version").item(0).getFirstChild()
					.getTextContent();
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static void cleanIfNecessary() throws Exception {
		File installedJar = new File(getMavenRepository(), String.format(
				"org/springframework/boot/spring-boot/%s/spring-boot-%s.jar", VERSION,
				VERSION));
		File grapesJar = new File(getGrapesCache(), String.format(
				"org.springframework.boot/spring-boot/jars/spring-boot-%s.jar", VERSION));
		if (!VERSION.contains("SNAPSHOT") || installedJar.exists() && grapesJar.exists()
				&& installedJar.lastModified() <= grapesJar.lastModified()) {
			return;
		}
		new CleanCommand().run();
	}

	private static File getMavenRepository() {
		return new File(System.getProperty("user.home"), ".m2/repository");
	}

	private static File getGrapesCache() {
		return new File(System.getProperty("user.home"), ".groovy/grapes");
	}

}
