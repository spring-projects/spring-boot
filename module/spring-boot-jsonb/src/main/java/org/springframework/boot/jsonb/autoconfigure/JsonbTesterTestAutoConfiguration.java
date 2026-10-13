/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jsonb.autoconfigure;

import jakarta.json.bind.Jsonb;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.test.autoconfigure.json.ConditionalOnJsonTesters;
import org.springframework.boot.test.autoconfigure.json.JsonMarshalTesterRuntimeHints;
import org.springframework.boot.test.autoconfigure.json.JsonTesterFactoryBean;
import org.springframework.boot.test.json.JsonbTester;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.Scope;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link JsonbTester}.
 *
 * @author Phjllip Webb
 */
@AutoConfiguration(after = JsonbAutoConfiguration.class)
@ConditionalOnJsonTesters
final class JsonbTesterTestAutoConfiguration {

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	@ConditionalOnBean(Jsonb.class)
	@ImportRuntimeHints(JsonbJsonTesterRuntimeHints.class)
	FactoryBean<JsonbTester<?>> jsonbTesterFactoryBean(Jsonb jsonb) {
		return new JsonTesterFactoryBean<>(JsonbTester.class, jsonb);
	}

	static class JsonbJsonTesterRuntimeHints extends JsonMarshalTesterRuntimeHints {

		JsonbJsonTesterRuntimeHints() {
			super(JsonbTester.class);
		}

	}

}
