/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.thymeleaf.autoconfigure;

import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.spring6.ISpringTemplateEngine;
import org.thymeleaf.spring6.ISpringWebFluxTemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.SpringWebFluxTemplateEngine;
import org.thymeleaf.templateresolver.ITemplateResolver;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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

	@Configuration(proxyBeanMethods = false)
	static class DefaultTemplateEngineConfiguration {

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

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = Type.REACTIVE)
	static class ReactiveTemplateEngineConfiguration {

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
