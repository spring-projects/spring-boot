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

package org.springframework.boot.jackson2.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.test.autoconfigure.json.ConditionalOnJsonTesters;
import org.springframework.boot.test.autoconfigure.json.JsonMarshalTesterRuntimeHints;
import org.springframework.boot.test.autoconfigure.json.JsonTesterFactoryBean;
import org.springframework.boot.test.json.GsonTester;
import org.springframework.boot.test.json.Jackson2Tester;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.Scope;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link GsonTester}.
 *
 * @author Phjllip Webb
 * @deprecated since 4.0.0 for removal in 4.3.0 in favor of Jackson 3.
 */
@Deprecated(since = "4.0.0", forRemoval = true)
@AutoConfiguration(after = Jackson2AutoConfiguration.class)
@ConditionalOnJsonTesters
@SuppressWarnings("removal")
final class Jackson2TesterTestAutoConfiguration {

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	@ConditionalOnBean(ObjectMapper.class)
	@ImportRuntimeHints(Jackson2TesterRuntimeHints.class)
	FactoryBean<Jackson2Tester<?>> jackson2TesterFactoryBean(ObjectMapper mapper) {
		return new JsonTesterFactoryBean<>(Jackson2Tester.class, mapper);
	}

	static class Jackson2TesterRuntimeHints extends JsonMarshalTesterRuntimeHints {

		Jackson2TesterRuntimeHints() {
			super(Jackson2Tester.class);
		}

	}

}
