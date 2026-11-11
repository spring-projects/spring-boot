/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.gson.autoconfigure;

import com.google.gson.Gson;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.test.autoconfigure.json.ConditionalOnJsonTesters;
import org.springframework.boot.test.autoconfigure.json.JsonMarshalTesterRuntimeHints;
import org.springframework.boot.test.autoconfigure.json.JsonTesterFactoryBean;
import org.springframework.boot.test.json.GsonTester;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.Scope;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link GsonTester}.
 *
 * @author Phjllip Webb
 */
@AutoConfiguration(after = GsonAutoConfiguration.class)
@ConditionalOnJsonTesters
final class GsonTesterTestAutoConfiguration {

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	@ConditionalOnBean(Gson.class)
	@ImportRuntimeHints(GsonTesterRuntimeHints.class)
	FactoryBean<GsonTester<?>> gsonTesterFactoryBean(Gson gson) {
		return new JsonTesterFactoryBean<>(GsonTester.class, gson);
	}

	static class GsonTesterRuntimeHints extends JsonMarshalTesterRuntimeHints {

		GsonTesterRuntimeHints() {
			super(GsonTester.class);
		}

	}

}
