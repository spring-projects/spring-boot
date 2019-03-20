/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.context.embedded.tomcat;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * TLD Skip Patterns used by Spring Boot.
 *
 * @author Phillip Webb
 */
final class TldSkipPatterns {

	private static final Set<String> TOMCAT;

	static {
		// Same as Tomcat
		Set<String> patterns = new LinkedHashSet<String>();
		patterns.add("ant-*.jar");
		patterns.add("aspectj*.jar");
		patterns.add("commons-beanutils*.jar");
		patterns.add("commons-codec*.jar");
		patterns.add("commons-collections*.jar");
		patterns.add("commons-dbcp*.jar");
		patterns.add("commons-digester*.jar");
		patterns.add("commons-fileupload*.jar");
		patterns.add("commons-httpclient*.jar");
		patterns.add("commons-io*.jar");
		patterns.add("commons-lang*.jar");
		patterns.add("commons-logging*.jar");
		patterns.add("commons-math*.jar");
		patterns.add("commons-pool*.jar");
		patterns.add("geronimo-spec-jaxrpc*.jar");
		patterns.add("h2*.jar");
		patterns.add("hamcrest*.jar");
		patterns.add("hibernate*.jar");
		patterns.add("jmx*.jar");
		patterns.add("jmx-tools-*.jar");
		patterns.add("jta*.jar");
		patterns.add("junit-*.jar");
		patterns.add("httpclient*.jar");
		patterns.add("log4j-*.jar");
		patterns.add("mail*.jar");
		patterns.add("org.hamcrest*.jar");
		patterns.add("slf4j*.jar");
		patterns.add("tomcat-embed-core-*.jar");
		patterns.add("tomcat-embed-logging-*.jar");
		patterns.add("tomcat-jdbc-*.jar");
		patterns.add("tomcat-juli-*.jar");
		patterns.add("tools.jar");
		patterns.add("wsdl4j*.jar");
		patterns.add("xercesImpl-*.jar");
		patterns.add("xmlParserAPIs-*.jar");
		patterns.add("xml-apis-*.jar");
		TOMCAT = Collections.unmodifiableSet(patterns);
	}

	private static final Set<String> ADDITIONAL;

	static {
		// Additional typical for Spring Boot applications
		Set<String> patterns = new LinkedHashSet<String>();
		patterns.add("antlr-*.jar");
		patterns.add("aopalliance-*.jar");
		patterns.add("aspectjrt-*.jar");
		patterns.add("aspectjweaver-*.jar");
		patterns.add("classmate-*.jar");
		patterns.add("dom4j-*.jar");
		patterns.add("ecj-*.jar");
		patterns.add("ehcache-core-*.jar");
		patterns.add("hibernate-core-*.jar");
		patterns.add("hibernate-commons-annotations-*.jar");
		patterns.add("hibernate-entitymanager-*.jar");
		patterns.add("hibernate-jpa-2.1-api-*.jar");
		patterns.add("hibernate-validator-*.jar");
		patterns.add("hsqldb-*.jar");
		patterns.add("jackson-annotations-*.jar");
		patterns.add("jackson-core-*.jar");
		patterns.add("jackson-databind-*.jar");
		patterns.add("jandex-*.jar");
		patterns.add("javassist-*.jar");
		patterns.add("jboss-logging-*.jar");
		patterns.add("jboss-transaction-api_*.jar");
		patterns.add("jcl-over-slf4j-*.jar");
		patterns.add("jdom-*.jar");
		patterns.add("jul-to-slf4j-*.jar");
		patterns.add("log4j-over-slf4j-*.jar");
		patterns.add("logback-classic-*.jar");
		patterns.add("logback-core-*.jar");
		patterns.add("rome-*.jar");
		patterns.add("slf4j-api-*.jar");
		patterns.add("spring-aop-*.jar");
		patterns.add("spring-aspects-*.jar");
		patterns.add("spring-beans-*.jar");
		patterns.add("spring-boot-*.jar");
		patterns.add("spring-core-*.jar");
		patterns.add("spring-context-*.jar");
		patterns.add("spring-data-*.jar");
		patterns.add("spring-expression-*.jar");
		patterns.add("spring-jdbc-*.jar,");
		patterns.add("spring-orm-*.jar");
		patterns.add("spring-oxm-*.jar");
		patterns.add("spring-tx-*.jar");
		patterns.add("snakeyaml-*.jar");
		patterns.add("tomcat-embed-el-*.jar");
		patterns.add("validation-api-*.jar");
		patterns.add("xml-apis-*.jar");
		ADDITIONAL = Collections.unmodifiableSet(patterns);
	}

	static final Set<String> DEFAULT;

	static {
		Set<String> patterns = new LinkedHashSet<String>();
		patterns.addAll(TOMCAT);
		patterns.addAll(ADDITIONAL);
		DEFAULT = Collections.unmodifiableSet(patterns);
	}

	private TldSkipPatterns() {
	}

}
