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

package org.springframework.boot.autoconfigure.info;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for various project information.
 *
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @since 1.4.0
 */
@AutoConfiguration
@EnableConfigurationProperties(ProjectInfoProperties.class)
public class ProjectInfoAutoConfiguration {

	private final ProjectInfoProperties properties;

	/**
     * Constructs a new instance of ProjectInfoAutoConfiguration with the specified ProjectInfoProperties.
     * 
     * @param properties the ProjectInfoProperties to be used for configuration
     */
    public ProjectInfoAutoConfiguration(ProjectInfoProperties properties) {
		this.properties = properties;
	}

	/**
     * Creates a new instance of GitProperties by loading the properties from the specified location.
     * This method is conditionally executed based on the availability of GitResourceAvailableCondition and the absence of a bean of type GitProperties.
     * 
     * @return a new instance of GitProperties
     * @throws Exception if an error occurs while loading the properties
     */
    @Conditional(GitResourceAvailableCondition.class)
	@ConditionalOnMissingBean
	@Bean
	public GitProperties gitProperties() throws Exception {
		return new GitProperties(
				loadFrom(this.properties.getGit().getLocation(), "git", this.properties.getGit().getEncoding()));
	}

	/**
     * Creates a bean of type {@link BuildProperties} if the specified resource is available and no bean of the same type exists.
     * The resource location is determined by the property "spring.info.build.location" and defaults to "classpath:META-INF/build-info.properties".
     * 
     * @return the created {@link BuildProperties} bean
     * @throws Exception if an error occurs while loading the build properties
     */
    @ConditionalOnResource(resources = "${spring.info.build.location:classpath:META-INF/build-info.properties}")
	@ConditionalOnMissingBean
	@Bean
	public BuildProperties buildProperties() throws Exception {
		return new BuildProperties(
				loadFrom(this.properties.getBuild().getLocation(), "build", this.properties.getBuild().getEncoding()));
	}

	/**
     * Loads properties from a given resource location with a specified prefix and encoding.
     * 
     * @param location the resource location to load properties from
     * @param prefix the prefix to filter properties by
     * @param encoding the character encoding of the resource
     * @return a Properties object containing the filtered properties
     * @throws IOException if an I/O error occurs while loading the properties
     */
    protected Properties loadFrom(Resource location, String prefix, Charset encoding) throws IOException {
		prefix = prefix.endsWith(".") ? prefix : prefix + ".";
		Properties source = loadSource(location, encoding);
		Properties target = new Properties();
		for (String key : source.stringPropertyNames()) {
			if (key.startsWith(prefix)) {
				target.put(key.substring(prefix.length()), source.get(key));
			}
		}
		return target;
	}

	/**
     * Loads the properties from the given resource location with the specified encoding.
     * 
     * @param location the resource location to load the properties from
     * @param encoding the character encoding to use for reading the resource, can be null
     * @return the loaded properties
     * @throws IOException if an I/O error occurs while reading the resource
     */
    private Properties loadSource(Resource location, Charset encoding) throws IOException {
		if (encoding != null) {
			return PropertiesLoaderUtils.loadProperties(new EncodedResource(location, encoding));
		}
		return PropertiesLoaderUtils.loadProperties(location);
	}

	/**
     * GitResourceAvailableCondition class.
     */
    static class GitResourceAvailableCondition extends SpringBootCondition {

		/**
         * Determines the outcome of the condition for the availability of Git resource.
         * 
         * @param context   the condition context
         * @param metadata  the annotated type metadata
         * @return          the condition outcome
         */
        @Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ResourceLoader loader = context.getResourceLoader();
			Environment environment = context.getEnvironment();
			String location = environment.getProperty("spring.info.git.location");
			if (location == null) {
				location = "classpath:git.properties";
			}
			ConditionMessage.Builder message = ConditionMessage.forCondition("GitResource");
			if (loader.getResource(location).exists()) {
				return ConditionOutcome.match(message.found("git info at").items(location));
			}
			return ConditionOutcome.noMatch(message.didNotFind("git info at").items(location));
		}

	}

}
