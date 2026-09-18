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

package org.springframework.boot.jackson.autoconfigure;

import tools.jackson.dataformat.xml.XmlMapper;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.test.autoconfigure.json.JsonTesterFactoryBean;
import org.springframework.boot.test.autoconfigure.xml.ConditionalOnXmlTesters;
import org.springframework.boot.test.autoconfigure.xml.XmlMarshalTesterRuntimeHints;
import org.springframework.boot.test.xml.JacksonXmlTester;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.Scope;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link JacksonXmlTester}.
 *
 * @author Tiziano Basile
 */
@AutoConfiguration(after = JacksonAutoConfiguration.class)
@ConditionalOnXmlTesters
final class JacksonXmlTesterTestAutoConfiguration {

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	@ConditionalOnBean(XmlMapper.class)
	@ImportRuntimeHints(JacksonXmlTesterRuntimeHints.class)
	FactoryBean<JacksonXmlTester<?>> jacksonXmlTesterFactoryBean(XmlMapper mapper) {
		return new JsonTesterFactoryBean<>(JacksonXmlTester.class, mapper);
	}

	static class JacksonXmlTesterRuntimeHints extends XmlMarshalTesterRuntimeHints {

		JacksonXmlTesterRuntimeHints() {
			super(JacksonXmlTester.class);
		}

	}

}
