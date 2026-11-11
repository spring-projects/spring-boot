/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.servlet.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.servlet.filter.OrderedCharacterEncodingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.CharacterEncodingFilter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for configuring the encoding to use
 * in Servlet web applications.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 4.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(ServletEncodingProperties.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(CharacterEncodingFilter.class)
@ConditionalOnBooleanProperty(name = "spring.servlet.encoding.enabled", matchIfMissing = true)
public final class HttpEncodingAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	CharacterEncodingFilter characterEncodingFilter(ServletEncodingProperties properties) {
		CharacterEncodingFilter filter = new OrderedCharacterEncodingFilter();
		filter.setEncoding(properties.getCharset().name());
		filter.setForceRequestEncoding(properties.shouldForce(ServletEncodingProperties.HttpMessageType.REQUEST));
		filter.setForceResponseEncoding(properties.shouldForce(ServletEncodingProperties.HttpMessageType.RESPONSE));
		return filter;
	}

}
