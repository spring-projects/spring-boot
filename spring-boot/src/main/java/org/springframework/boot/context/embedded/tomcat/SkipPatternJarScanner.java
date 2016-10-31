/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.context.embedded.tomcat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.tomcat.JarScanner;
import org.apache.tomcat.JarScannerCallback;
import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.apache.tomcat.util.scan.StandardJarScanner;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link JarScanner} decorator allowing alternative default jar pattern matching. This
 * class extends {@link StandardJarScanner} rather than implementing the
 * {@link JarScanner} due to API changes introduced in Tomcat 8.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @see #apply(TomcatEmbeddedContext, Set)
 */
class SkipPatternJarScanner extends StandardJarScanner {

	private static final String JAR_SCAN_FILTER_CLASS = "org.apache.tomcat.JarScanFilter";

	private final JarScanner jarScanner;

	private final SkipPattern pattern;

	SkipPatternJarScanner(JarScanner jarScanner, Set<String> patterns) {
		Assert.notNull(jarScanner, "JarScanner must not be null");
		this.jarScanner = jarScanner;
		this.pattern = (patterns == null ? new SkipPattern(defaultPatterns()) : new SkipPattern(patterns));
		setPatternToTomcat8SkipFilter(this.pattern);
	}

	private void setPatternToTomcat8SkipFilter(SkipPattern pattern) {
		if (ClassUtils.isPresent(JAR_SCAN_FILTER_CLASS, null)) {
			new Tomcat8TldSkipSetter(this).setSkipPattern(pattern);
		}
	}

	// For Tomcat 7 compatibility
	public void scan(ServletContext context, ClassLoader classloader,
			JarScannerCallback callback, Set<String> jarsToSkip) {
		Method scanMethod = ReflectionUtils.findMethod(this.jarScanner.getClass(), "scan",
				ServletContext.class, ClassLoader.class, JarScannerCallback.class,
				Set.class);
		Assert.notNull(scanMethod, "Unable to find scan method");
		try {
			scanMethod.invoke(this.jarScanner, context, classloader, callback,
					(jarsToSkip == null ? this.pattern.asSet() : jarsToSkip));
		}
		catch (Exception ex) {
			throw new IllegalStateException("Tomcat 7 reflection failed", ex);
		}
	}

	/**
	 * Return the default skip patterns to use.
	 * @return the default skip patterns
	 */
	static Set<String> defaultPatterns() {
		return new LinkedHashSet<String>(Arrays.asList(
				// Same as Tomcat
				"ant-*.jar",
				"aspectj*.jar",
				"commons-beanutils*.jar",
				"commons-codec*.jar",
				"commons-collections*.jar",
				"commons-dbcp*.jar",
				"commons-digester*.jar",
				"commons-fileupload*.jar",
				"commons-httpclient*.jar",
				"commons-io*.jar",
				"commons-lang*.jar",
				"commons-logging*.jar",
				"commons-math*.jar",
				"commons-pool*.jar",
				"geronimo-spec-jaxrpc*.jar",
				"h2*.jar",
				"hamcrest*.jar",
				"hibernate*.jar",
				"jmx*.jar",
				"jmx-tools-*.jar",
				"jta*.jar",
				"junit-*.jar",
				"httpclient*.jar",
				"log4j-*.jar",
				"mail*.jar",
				"org.hamcrest*.jar",
				"slf4j*.jar",
				"tomcat-embed-core-*.jar",
				"tomcat-embed-logging-*.jar",
				"tomcat-jdbc-*.jar",
				"tomcat-juli-*.jar",
				"tools.jar",
				"wsdl4j*.jar",
				"xercesImpl-*.jar",
				"xmlParserAPIs-*.jar",
				"xml-apis-*.jar",

				// Additional
				"antlr-*.jar",
				"aopalliance-*.jar",
				"aspectjrt-*.jar",
				"aspectjweaver-*.jar",
				"classmate-*.jar",
				"dom4j-*.jar",
				"ecj-*.jar",
				"ehcache-core-*.jar",
				"hibernate-core-*.jar",
				"hibernate-commons-annotations-*.jar",
				"hibernate-entitymanager-*.jar",
				"hibernate-jpa-2.1-api-*.jar",
				"hibernate-validator-*.jar",
				"hsqldb-*.jar",
				"jackson-annotations-*.jar",
				"jackson-core-*.jar",
				"jackson-databind-*.jar",
				"jandex-*.jar",
				"javassist-*.jar",
				"jboss-logging-*.jar",
				"jboss-transaction-api_*.jar",
				"jcl-over-slf4j-*.jar",
				"jdom-*.jar",
				"jul-to-slf4j-*.jar",
				"log4j-over-slf4j-*.jar",
				"logback-classic-*.jar",
				"logback-core-*.jar",
				"rome-*.jar",
				"slf4j-api-*.jar",
				"spring-aop-*.jar",
				"spring-aspects-*.jar",
				"spring-beans-*.jar",
				"spring-boot-*.jar",
				"spring-core-*.jar",
				"spring-context-*.jar",
				"spring-data-*.jar",
				"spring-expression-*.jar",
				"spring-jdbc-*.jar,",
				"spring-orm-*.jar",
				"spring-oxm-*.jar",
				"spring-tx-*.jar",
				"snakeyaml-*.jar",
				"tomcat-embed-el-*.jar",
				"validation-api-*.jar",
				"xml-apis-*.jar"));
	}

	/**
	 * Apply this decorator the specified context.
	 * @param context the context to apply to
	 * @param patterns the jar skip patterns or {@code null} for defaults
	 */
	static void apply(TomcatEmbeddedContext context, Set<String> patterns) {
		SkipPatternJarScanner scanner = new SkipPatternJarScanner(context.getJarScanner(),
				patterns);
		context.setJarScanner(scanner);
	}

	/**
	 * Tomcat 8 specific logic to setup the scanner.
	 */
	private static class Tomcat8TldSkipSetter {

		private final StandardJarScanner jarScanner;

		Tomcat8TldSkipSetter(StandardJarScanner jarScanner) {
			this.jarScanner = jarScanner;
		}

		public void setSkipPattern(SkipPattern pattern) {
			StandardJarScanFilter filter = new StandardJarScanFilter();
			filter.setTldSkip(pattern.asCommaDelimitedString());
			this.jarScanner.setJarScanFilter(filter);
		}

	}

	/**
	 * Skip patterns used by Spring Boot.
	 */
	private static class SkipPattern {

		private Set<String> patterns = new LinkedHashSet<String>();

		SkipPattern(Set<String> patterns) {
			for (String pattern : patterns) {
				add(pattern);
			}
		}

		protected void add(String patterns) {
			Assert.notNull(patterns, "Patterns must not be null");
			if (patterns.length() > 0 && !patterns.trim().startsWith(",")) {
				this.patterns.add(",");
			}
			this.patterns.add(patterns);
		}

		public String asCommaDelimitedString() {
			return StringUtils.collectionToCommaDelimitedString(this.patterns);
		}

		public Set<String> asSet() {
			return Collections.unmodifiableSet(this.patterns);
		}

	}

}
