/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.context;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.support.AbstractResourceBasedMessageSource;
import org.springframework.core.CollectionFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Configure an {@link AbstractResourceBasedMessageSource} with sensible defaults tuned
 * using configuration properties.
 * <p>
 * Can be injected into application code and used to define a custom message source whose
 * configuration is based upon that produced by auto-configuration.
 *
 * @author Henrique (henriquejsza)
 * @author Tanishq Choudhary
 * @since 4.2.0
 */
public class ResourceBasedMessageSourceConfigurer {

	private final MessageSourceProperties properties;

	/**
	 * Creates a new configurer that will use the given {@code properties}.
	 * @param properties properties to use
	 */
	public ResourceBasedMessageSourceConfigurer(MessageSourceProperties properties) {
		Assert.notNull(properties, "'properties' must not be null");
		this.properties = properties;
	}

	/**
	 * Configure the specified message source. The message source can be further tuned and
	 * default settings can be overridden.
	 * @param messageSource the {@link AbstractResourceBasedMessageSource} instance to
	 * configure
	 */
	public void configure(AbstractResourceBasedMessageSource messageSource) {
		Assert.notNull(messageSource, "'messageSource' must not be null");
		PropertyMapper map = PropertyMapper.get();
		map.from(this.properties::getBasename)
			.whenNot(CollectionUtils::isEmpty)
			.as((names) -> names.toArray(new String[0]))
			.to(messageSource::setBasenames);
		map.from(this.properties::getEncoding).as(Charset::name).to(messageSource::setDefaultEncoding);
		map.from(this.properties::isFallbackToSystemLocale).to(messageSource::setFallbackToSystemLocale);
		map.from(this.properties::getCacheDuration).as(Duration::toMillis).to(messageSource::setCacheMillis);
		map.from(this.properties.isAlwaysUseMessageFormat()).to(messageSource::setAlwaysUseMessageFormat);
		map.from(this.properties::isUseCodeAsDefaultMessage).to(messageSource::setUseCodeAsDefaultMessage);
		map.from(this.properties::getCommonMessages).as(this::loadCommonMessages).to(messageSource::setCommonMessages);
	}

	private Properties loadCommonMessages(List<Resource> resources) {
		Properties commonMessages = CollectionFactory.createSortedProperties(false);
		for (Resource resource : resources) {
			try {
				EncodedResource encodedResource = new EncodedResource(resource, this.properties.getEncoding());
				PropertiesLoaderUtils.fillProperties(commonMessages, encodedResource);
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Failed to load common messages from '%s'".formatted(resource), ex);
			}
		}
		return commonMessages;
	}

}
