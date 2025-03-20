/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.groovy.template.autoconfigure;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GroovyTemplateProperties}.
 *
 * @author Stephane Nicoll
 */
class GroovyTemplatePropertiesTests {

	@Test
	void defaultContentType() {
		assertThat(new GroovyTemplateProperties().getContentType()).hasToString("text/html;charset=UTF-8");
	}

	@Test
	void customContentTypeDefaultCharset() {
		GroovyTemplateProperties properties = new GroovyTemplateProperties();
		properties.setContentType(MimeTypeUtils.parseMimeType("text/plain"));
		assertThat(properties.getContentType()).hasToString("text/plain;charset=UTF-8");
	}

	@Test
	void defaultContentTypeCustomCharset() {
		GroovyTemplateProperties properties = new GroovyTemplateProperties();
		properties.setCharset(StandardCharsets.UTF_16);
		assertThat(properties.getContentType()).hasToString("text/html;charset=UTF-16");
	}

	@Test
	void customContentTypeCustomCharset() {
		GroovyTemplateProperties properties = new GroovyTemplateProperties();
		properties.setContentType(MimeTypeUtils.parseMimeType("text/plain"));
		properties.setCharset(StandardCharsets.UTF_16);
		assertThat(properties.getContentType()).hasToString("text/plain;charset=UTF-16");
	}

	@Test
	void customContentTypeWithPropertyAndCustomCharset() {
		GroovyTemplateProperties properties = new GroovyTemplateProperties();
		properties.setContentType(MimeTypeUtils.parseMimeType("text/plain;foo=bar"));
		properties.setCharset(StandardCharsets.UTF_16);
		assertThat(properties.getContentType()).hasToString("text/plain;charset=UTF-16;foo=bar");
	}

}
