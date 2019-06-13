/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.context.embedded;

import java.io.File;
import java.io.FileReader;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.InputSource;

import org.springframework.util.StringUtils;

/**
 * Provides access to dependency versions by querying the project's pom.
 *
 * @author Andy Wilkinson
 */
final class Versions {

	private static final String PROPERTIES = "/*[local-name()='project']/*[local-name()='properties']";

	private Versions() {
	}

	public static String getBootVersion() {
		String baseDir = StringUtils.cleanPath(new File(".").getAbsolutePath());
		String mainBaseDir = evaluateExpression("pom.xml", PROPERTIES + "/*[local-name()='main.basedir']/text()");
		mainBaseDir = mainBaseDir.replace("${basedir}", baseDir);
		return evaluateExpression(mainBaseDir + "/pom.xml", PROPERTIES + "/*[local-name()='revision']/text()");
	}

	private static String evaluateExpression(String file, String expression) {
		try {
			InputSource source = new InputSource(new FileReader(file));
			XPath xpath = XPathFactory.newInstance().newXPath();
			return xpath.compile(expression).evaluate(source);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to evaluate expression", ex);
		}
	}

}
