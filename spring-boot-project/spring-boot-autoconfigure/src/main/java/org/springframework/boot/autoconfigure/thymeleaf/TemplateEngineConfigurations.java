/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.thymeleaf;

import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.spring6.ISpringTemplateEngine;
import org.thymeleaf.spring6.ISpringWebFluxTemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.SpringWebFluxTemplateEngine;
import org.thymeleaf.templateresolver.ITemplateResolver;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration classes for Thymeleaf's {@link ITemplateEngine}. Imported by
 * {@link ThymeleafAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class TemplateEngineConfigurations {

	/**
	 * DefaultTemplateEngineConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	static class DefaultTemplateEngineConfiguration {

		/**
		 * Creates a SpringTemplateEngine bean if no other bean of type
		 * ISpringTemplateEngine is present.
		 * @param properties the ThymeleafProperties object containing the Thymeleaf
		 * configuration properties
		 * @param templateResolvers the object provider for ITemplateResolver beans
		 * @param dialects the object provider for IDialect beans
		 * @return the created SpringTemplateEngine bean
		 */
		@Bean
		@ConditionalOnMissingBean(ISpringTemplateEngine.class)
		SpringTemplateEngine templateEngine(ThymeleafProperties properties,
				ObjectProvider<ITemplateResolver> templateResolvers, ObjectProvider<IDialect> dialects) {
			SpringTemplateEngine engine = new SpringTemplateEngine();
			engine.setEnableSpringELCompiler(properties.isEnableSpringElCompiler());
			engine.setRenderHiddenMarkersBeforeCheckboxes(properties.isRenderHiddenMarkersBeforeCheckboxes());
			templateResolvers.orderedStream().forEach(engine::addTemplateResolver);
			dialects.orderedStream().forEach(engine::addDialect);
			return engine;
		}

	}

	/**
	 * ReactiveTemplateEngineConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = Type.REACTIVE)
	@ConditionalOnProperty(name = "spring.thymeleaf.enabled", matchIfMissing = true)
	static class ReactiveTemplateEngineConfiguration {

		/**
		 * Creates a SpringWebFluxTemplateEngine bean if no bean of type
		 * ISpringWebFluxTemplateEngine is present.
		 * @param properties the ThymeleafProperties object containing the Thymeleaf
		 * configuration properties
		 * @param templateResolvers the object provider for ITemplateResolver beans
		 * @param dialects the object provider for IDialect beans
		 * @return the created SpringWebFluxTemplateEngine bean
		 */
		@Bean
		@ConditionalOnMissingBean(ISpringWebFluxTemplateEngine.class)
		SpringWebFluxTemplateEngine templateEngine(ThymeleafProperties properties,
				ObjectProvider<ITemplateResolver> templateResolvers, ObjectProvider<IDialect> dialects) {
			SpringWebFluxTemplateEngine engine = new SpringWebFluxTemplateEngine();
			engine.setEnableSpringELCompiler(properties.isEnableSpringElCompiler());
			engine.setRenderHiddenMarkersBeforeCheckboxes(properties.isRenderHiddenMarkersBeforeCheckboxes());
			templateResolvers.orderedStream().forEach(engine::addTemplateResolver);
			dialects.orderedStream().forEach(engine::addDialect);
			return engine;
		}

	}

}
