/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.info;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.validation.BindException;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for various project information.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@Configuration
@EnableConfigurationProperties(ProjectInfoProperties.class)
public class ProjectInfoAutoConfiguration {

	@Autowired
	private ProjectInfoProperties properties;

	private final ConversionService conversionService = createConversionService();

	@Conditional(GitResourceAvailableCondition.class)
	@ConditionalOnMissingBean
	@Bean
	public GitInfo gitInfo() throws Exception {
		GitInfo gitInfo = new GitInfo();
		bindPropertiesTo(gitInfo, this.properties.getGit().getLocation(), "git");
		return gitInfo;
	}

	protected void bindPropertiesTo(Object target, Resource location, String prefix)
			throws BindException, IOException {
		PropertiesConfigurationFactory<Object> factory =
				new PropertiesConfigurationFactory<Object>(target);
		factory.setConversionService(this.conversionService);
		factory.setTargetName(prefix);
		Properties gitInfoProperties = PropertiesLoaderUtils.loadProperties(location);
		factory.setProperties(gitInfoProperties);
		factory.bindPropertiesToTarget();
	}

	private static ConversionService createConversionService() {
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new StringToDateConverter());
		return conversionService;
	}


	static class GitResourceAvailableCondition extends SpringBootCondition {

		private final ResourceLoader defaultResourceLoader = new DefaultResourceLoader();

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			ResourceLoader loader = context.getResourceLoader() == null
					? this.defaultResourceLoader : context.getResourceLoader();
			PropertyResolver propertyResolver = context.getEnvironment();
			RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
					propertyResolver, "spring.info.git.");
			String location = resolver.getProperty("location");
			if (location == null) {
				resolver = new RelaxedPropertyResolver(propertyResolver, "spring.git.");
				location = resolver.getProperty("properties");
				if (location == null) {
					location = "classpath:git.properties";
				}
			}
			boolean match = loader.getResource(location).exists();
			return new ConditionOutcome(match,
					"Git info " + (match ? "found" : "not found") + " at " + location);
		}

	}

	private static class StringToDateConverter implements Converter<String, Date> {

		@Override
		public Date convert(String s) {
			Long epoch = parseEpochSecond(s);
			if (epoch != null) {
				return new Date(epoch);
			}
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US);
			try {
				return format.parse(s);
			}
			catch (ParseException ex) {
				throw new ConversionFailedException(TypeDescriptor.valueOf(String.class),
						TypeDescriptor.valueOf(Date.class), s, ex);
			}
		}

		/**
		 * Attempt to parse a {@code Long} from the specified input, representing the
		 * epoch time in seconds.
		 * @param s the input
		 * @return the epoch time in msec
		 */
		private Long parseEpochSecond(String s) {
			try {
				return Long.parseLong(s) * 1000;
			}
			catch (NumberFormatException e) {
				return null;
			}
		}
	}

}
