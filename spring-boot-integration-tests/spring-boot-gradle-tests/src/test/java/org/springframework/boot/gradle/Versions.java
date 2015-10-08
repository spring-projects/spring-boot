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

package org.springframework.boot.gradle;

import java.io.FileReader;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.InputSource;

/**
 * @author Andy Wilkinson
 */
public final class Versions {

	private Versions() {
	}

	public static String getBootVersion() {
		return evaluateExpression(
				"/*[local-name()='project']/*[local-name()='version']" + "/text()");
	}

	public static String getSpringLoadedVersion() {
		return evaluateExpression(
				"/*[local-name()='project']/*[local-name()='properties']"
						+ "/*[local-name()='spring-loaded.version']/text()");
	}

	public static String getSpringVersion() {
		return evaluateExpression(
				"/*[local-name()='project']/*[local-name()='properties']"
						+ "/*[local-name()='spring.version']/text()");
	}

	private static String evaluateExpression(String expression) {
		try {
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			XPathExpression expr = xpath.compile(expression);
			String version = expr.evaluate(
					new InputSource(new FileReader("target/dependencies-pom.xml")));
			return version;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to evaluate expression", ex);
		}
	}
}
