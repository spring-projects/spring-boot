/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson.autoconfigure;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.test.autoconfigure.json.ConditionalOnJsonTesters;
import org.springframework.boot.test.autoconfigure.json.JsonMarshalTesterRuntimeHints;
import org.springframework.boot.test.autoconfigure.json.JsonTesterFactoryBean;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.Scope;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link JacksonTester}.
 *
 * @author Phillip Webb
 */
@AutoConfiguration(after = JacksonAutoConfiguration.class)
@ConditionalOnJsonTesters
final class JacksonTesterTestAutoConfiguration {

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	@ConditionalOnBean(JsonMapper.class)
	@ImportRuntimeHints(JacksonTesterRuntimeHints.class)
	FactoryBean<JacksonTester<?>> jacksonTesterFactoryBean(JsonMapper mapper) {
		return new JsonTesterFactoryBean<>(JacksonTester.class, mapper);
	}

	static class JacksonTesterRuntimeHints extends JsonMarshalTesterRuntimeHints {

		JacksonTesterRuntimeHints() {
			super(JacksonTester.class);
		}

	}

}
