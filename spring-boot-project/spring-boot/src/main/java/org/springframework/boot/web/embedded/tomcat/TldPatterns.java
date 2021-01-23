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

package org.springframework.boot.web.embedded.tomcat;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * TLD skip and scan patterns used by Spring Boot.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
final class TldPatterns {

	private static final Set<String> TOMCAT_SKIP;

	static {
		// Same as Tomcat
		Set<String> skipPatterns = new LinkedHashSet<>();
		skipPatterns.add("ant-*.jar");
		skipPatterns.add("aspectj*.jar");
		skipPatterns.add("commons-beanutils*.jar");
		skipPatterns.add("commons-codec*.jar");
		skipPatterns.add("commons-collections*.jar");
		skipPatterns.add("commons-dbcp*.jar");
		skipPatterns.add("commons-digester*.jar");
		skipPatterns.add("commons-fileupload*.jar");
		skipPatterns.add("commons-httpclient*.jar");
		skipPatterns.add("commons-io*.jar");
		skipPatterns.add("commons-lang*.jar");
		skipPatterns.add("commons-logging*.jar");
		skipPatterns.add("commons-math*.jar");
		skipPatterns.add("commons-pool*.jar");
		skipPatterns.add("geronimo-spec-jaxrpc*.jar");
		skipPatterns.add("h2*.jar");
		skipPatterns.add("hamcrest*.jar");
		skipPatterns.add("hibernate*.jar");
		skipPatterns.add("jaxb-runtime-*.jar");
		skipPatterns.add("jmx*.jar");
		skipPatterns.add("jmx-tools-*.jar");
		skipPatterns.add("jta*.jar");
		skipPatterns.add("junit-*.jar");
		skipPatterns.add("httpclient*.jar");
		skipPatterns.add("log4j-*.jar");
		skipPatterns.add("mail*.jar");
		skipPatterns.add("org.hamcrest*.jar");
		skipPatterns.add("slf4j*.jar");
		skipPatterns.add("tomcat-embed-core-*.jar");
		skipPatterns.add("tomcat-embed-logging-*.jar");
		skipPatterns.add("tomcat-jdbc-*.jar");
		skipPatterns.add("tomcat-juli-*.jar");
		skipPatterns.add("tools.jar");
		skipPatterns.add("wsdl4j*.jar");
		skipPatterns.add("xercesImpl-*.jar");
		skipPatterns.add("xmlParserAPIs-*.jar");
		skipPatterns.add("xml-apis-*.jar");
		TOMCAT_SKIP = Collections.unmodifiableSet(skipPatterns);
	}

	private static final Set<String> ADDITIONAL_SKIP;

	static {
		// Additional typical for Spring Boot applications
		Set<String> skipPatterns = new LinkedHashSet<>();
		skipPatterns.add("antlr-*.jar");
		skipPatterns.add("aopalliance-*.jar");
		skipPatterns.add("aspectjrt-*.jar");
		skipPatterns.add("aspectjweaver-*.jar");
		skipPatterns.add("classmate-*.jar");
		skipPatterns.add("dom4j-*.jar");
		skipPatterns.add("ecj-*.jar");
		skipPatterns.add("ehcache-core-*.jar");
		skipPatterns.add("hibernate-core-*.jar");
		skipPatterns.add("hibernate-commons-annotations-*.jar");
		skipPatterns.add("hibernate-entitymanager-*.jar");
		skipPatterns.add("hibernate-jpa-2.1-api-*.jar");
		skipPatterns.add("hibernate-validator-*.jar");
		skipPatterns.add("hsqldb-*.jar");
		skipPatterns.add("jackson-annotations-*.jar");
		skipPatterns.add("jackson-core-*.jar");
		skipPatterns.add("jackson-databind-*.jar");
		skipPatterns.add("jandex-*.jar");
		skipPatterns.add("javassist-*.jar");
		skipPatterns.add("jboss-logging-*.jar");
		skipPatterns.add("jboss-transaction-api_*.jar");
		skipPatterns.add("jcl-over-slf4j-*.jar");
		skipPatterns.add("jdom-*.jar");
		skipPatterns.add("jul-to-slf4j-*.jar");
		skipPatterns.add("log4j-over-slf4j-*.jar");
		skipPatterns.add("logback-classic-*.jar");
		skipPatterns.add("logback-core-*.jar");
		skipPatterns.add("rome-*.jar");
		skipPatterns.add("slf4j-api-*.jar");
		skipPatterns.add("spring-aop-*.jar");
		skipPatterns.add("spring-aspects-*.jar");
		skipPatterns.add("spring-beans-*.jar");
		skipPatterns.add("spring-boot-*.jar");
		skipPatterns.add("spring-core-*.jar");
		skipPatterns.add("spring-context-*.jar");
		skipPatterns.add("spring-data-*.jar");
		skipPatterns.add("spring-expression-*.jar");
		skipPatterns.add("spring-jdbc-*.jar,");
		skipPatterns.add("spring-orm-*.jar");
		skipPatterns.add("spring-oxm-*.jar");
		skipPatterns.add("spring-tx-*.jar");
		skipPatterns.add("snakeyaml-*.jar");
		skipPatterns.add("tomcat-embed-el-*.jar");
		skipPatterns.add("validation-api-*.jar");
		skipPatterns.add("xml-apis-*.jar");
		ADDITIONAL_SKIP = Collections.unmodifiableSet(skipPatterns);
	}

	static final Set<String> DEFAULT_SKIP;

	static {
		Set<String> skipPatterns = new LinkedHashSet<>();
		skipPatterns.addAll(TOMCAT_SKIP);
		skipPatterns.addAll(ADDITIONAL_SKIP);
		DEFAULT_SKIP = Collections.unmodifiableSet(skipPatterns);
	}

	private static final Set<String> TOMCAT_SCAN;

	static {
		Set<String> scanPatterns = new LinkedHashSet<>();
		scanPatterns.add("log4j-taglib*.jar");
		scanPatterns.add("log4j-web*.jar");
		scanPatterns.add("log4javascript*.jar");
		scanPatterns.add("slf4j-taglib*.jar");
		TOMCAT_SCAN = Collections.unmodifiableSet(scanPatterns);
	}

	static final Set<String> DEFAULT_SCAN;

	static {
		Set<String> scanPatterns = new LinkedHashSet<>();
		scanPatterns.addAll(TOMCAT_SCAN);
		DEFAULT_SCAN = Collections.unmodifiableSet(scanPatterns);
	}

	private TldPatterns() {
	}

}
