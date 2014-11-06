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

package org.springframework.boot.context.embedded.tomcat;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;

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
 * @see #apply(TomcatEmbeddedContext, String)
 */
class SkipPatternJarScanner extends StandardJarScanner {

	private static final String JAR_SCAN_FILTER_CLASS = "org.apache.tomcat.JarScanFilter";

	private final JarScanner jarScanner;

	private final SkipPattern pattern;

	SkipPatternJarScanner(JarScanner jarScanner, String pattern) {
		Assert.notNull(jarScanner, "JarScanner must not be null");
		this.jarScanner = jarScanner;
		this.pattern = (pattern == null ? new SkipPattern() : new SkipPattern(pattern));
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
		Method scanMethod = ReflectionUtils.findMethod(this.jarScanner.getClass(),
				"scan", ServletContext.class, ClassLoader.class,
				JarScannerCallback.class, Set.class);
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
	 * Apply this decorator the specified context.
	 * @param context the context to apply to
	 * @param pattern the jar skip pattern or {@code null} for defaults
	 */
	public static void apply(TomcatEmbeddedContext context, String pattern) {
		SkipPatternJarScanner scanner = new SkipPatternJarScanner(
				context.getJarScanner(), pattern);
		context.setJarScanner(scanner);
	}

	/**
	 * Tomcat 8 specific logic to setup the scanner.
	 */
	private static class Tomcat8TldSkipSetter {

		private final StandardJarScanner jarScanner;

		public Tomcat8TldSkipSetter(StandardJarScanner jarScanner) {
			this.jarScanner = jarScanner;
		}

		public void setSkipPattern(SkipPattern pattern) {
			StandardJarScanFilter filter = new StandardJarScanFilter();
			filter.setTldSkip(pattern.asCommaDelimitedString());
			this.jarScanner.setJarScanFilter(filter);
		}

	}

	/**
	 * Skip patterns used by Spring Boot
	 */
	private static class SkipPattern {

		private Set<String> patterns = new LinkedHashSet<String>();

		protected SkipPattern() {
			// Same as Tomcat
			add("ant-*.jar");
			add("aspectj*.jar");
			add("commons-beanutils*.jar");
			add("commons-codec*.jar");
			add("commons-collections*.jar");
			add("commons-dbcp*.jar");
			add("commons-digester*.jar");
			add("commons-fileupload*.jar");
			add("commons-httpclient*.jar");
			add("commons-io*.jar");
			add("commons-lang*.jar");
			add("commons-logging*.jar");
			add("commons-math*.jar");
			add("commons-pool*.jar");
			add("geronimo-spec-jaxrpc*.jar");
			add("h2*.jar");
			add("hamcrest*.jar");
			add("hibernate*.jar");
			add("jmx*.jar");
			add("jmx-tools-*.jar");
			add("jta*.jar");
			add("junit-*.jar");
			add("httpclient*.jar");
			add("log4j-*.jar");
			add("mail*.jar");
			add("org.hamcrest*.jar");
			add("slf4j*.jar");
			add("tomcat-embed-core-*.jar");
			add("tomcat-embed-logging-*.jar");
			add("tomcat-jdbc-*.jar");
			add("tomcat-juli-*.jar");
			add("tools.jar");
			add("wsdl4j*.jar");
			add("xercesImpl-*.jar");
			add("xmlParserAPIs-*.jar");
			add("xml-apis-*.jar");

			// Additional
			add("antlr-*.jar");
			add("aopalliance-*.jar");
			add("aspectjrt-*.jar");
			add("aspectjweaver-*.jar");
			add("classmate-*.jar");
			add("dom4j-*.jar");
			add("ecj-*.jar");
			add("ehcache-core-*.jar");
			add("hibernate-core-*.jar");
			add("hibernate-commons-annotations-*.jar");
			add("hibernate-entitymanager-*.jar");
			add("hibernate-jpa-2.1-api-*.jar");
			add("hibernate-validator-*.jar");
			add("hsqldb-*.jar");
			add("jackson-annotations-*.jar");
			add("jackson-core-*.jar");
			add("jackson-databind-*.jar");
			add("jandex-*.jar");
			add("javassist-*.jar");
			add("jboss-logging-*.jar");
			add("jboss-transaction-api_*.jar");
			add("jcl-over-slf4j-*.jar");
			add("jdom-*.jar");
			add("joda-time-*.jar");
			add("jul-to-slf4j-*.jar");
			add("log4j-over-slf4j-*.jar");
			add("logback-classic-*.jar");
			add("logback-core-*.jar");
			add("rome-*.jar");
			add("slf4j-api-*.jar");
			add("spring-aop-*.jar");
			add("spring-aspects-*.jar");
			add("spring-beans-*.jar");
			add("spring-boot-*.jar");
			add("spring-core-*.jar");
			add("spring-context-*.jar");
			add("spring-data-*.jar");
			add("spring-expression-*.jar");
			add("spring-jdbc-*.jar,");
			add("spring-orm-*.jar");
			add("spring-oxm-*.jar");
			add("spring-tx-*.jar");
			add("snakeyaml-*.jar");
			add("tomcat-embed-el-*.jar");
			add("validation-api-*.jar");
			add("xml-apis-*.jar");
		}

		public SkipPattern(String patterns) {
			StringTokenizer tokenizer = new StringTokenizer(patterns, ",");
			while (tokenizer.hasMoreElements()) {
				add(tokenizer.nextToken());
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
