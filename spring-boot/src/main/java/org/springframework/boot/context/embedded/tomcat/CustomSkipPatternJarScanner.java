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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;

import org.apache.tomcat.JarScanner;
import org.apache.tomcat.JarScannerCallback;
import org.springframework.util.Assert;

/**
 * {@link JarScanner} decorator allowing alternative default jar pattern matching.
 * 
 * @author Phillip Webb
 * @see #apply(TomcatEmbeddedContext, String)
 */
class SkipPatternJarScanner implements JarScanner {

	private final JarScanner jarScanner;

	private final SkipPattern pattern;

	SkipPatternJarScanner(JarScanner jarScanner, String pattern) {
		Assert.notNull(jarScanner, "JarScanner must not be null");
		this.jarScanner = jarScanner;
		this.pattern = (pattern == null ? new SkipPattern() : new SkipPattern(pattern));
	}

	@Override
	public void scan(ServletContext context, ClassLoader classloader,
			JarScannerCallback callback, Set<String> jarsToSkip) {
		this.jarScanner.scan(context, classloader, callback,
				(jarsToSkip == null ? this.pattern.asSet() : jarsToSkip));
	}

	/**
	 * Apply this decorator the specified context.
	 * @param context the context to apply to
	 * @param pattern the jar skip pattern or {@code null} for defaults
	 */
	public static void apply(TomcatEmbeddedContext context, String pattern) {
		context.setJarScanner(new SkipPatternJarScanner(context.getJarScanner(), pattern));
	}

	private static class SkipPattern {

		private Set<String> patterns = new LinkedHashSet<String>();

		protected SkipPattern() {
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

		public Set<String> asSet() {
			return Collections.unmodifiableSet(this.patterns);
		}

	}

}
