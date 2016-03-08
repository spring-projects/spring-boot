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

package org.springframework.boot.actuate.info;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.info.GitProperties;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * An {@link InfoContributor} that exposes {@link GitProperties}.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
public class GitInfoContributor implements InfoContributor {

	private static final Log logger = LogFactory.getLog(GitInfoContributor.class);

	private final GitProperties properties;

	private final Mode mode;

	public GitInfoContributor(GitProperties properties, Mode mode) {
		this.properties = properties;
		this.mode = mode;
	}

	public GitInfoContributor(GitProperties properties) {
		this(properties, Mode.SIMPLE);
	}

	protected final GitProperties getProperties() {
		return this.properties;
	}

	protected final Mode getMode() {
		return this.mode;
	}

	@Override
	public void contribute(Info.Builder builder) {
		builder.withDetail("git", extractContent());
	}

	/**
	 * Extract the content to contribute to the info endpoint.
	 * @return the content to expose
	 */
	protected Map<String, Object> extractContent() {
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(toPropertySources());
		Map<String, Object> content = new PropertySourcesBinder(propertySources).extractAll("");
		postProcess(content);
		return content;
	}

	/**
	 * Post-process the content to expose. By default, well known keys representing dates
	 * are converted to {@link Date} instances.
	 * @param content the content to expose
	 */
	protected void postProcess(Map<String, Object> content) {
		coerceDate(getNestedMap(content, "commit"), "time");
		coerceDate(getNestedMap(content, "build"), "time");
	}

	/**
	 * Coerce the specified key if the value is a proper epoch time.
	 * @param content the content to expose
	 * @param key the property to coerce
	 */
	protected void coerceDate(Map<String, Object> content, String key) {
		Object value = content.get(key);
		if (value != null) {
			try {
				long epoch = Long.parseLong(value.toString());
				content.put(key, new Date(epoch));
			}
			catch (NumberFormatException ex) {
				logger.warn("Expected a date for '" + key + "'", ex);
			}
		}
	}

	/**
	 * Return the {@link PropertySource} to use.
	 * @return the property source
	 */
	protected PropertySource<?> toPropertySources() {
		if (this.mode.equals(Mode.FULL)) {
			return this.properties.toPropertySource();
		}
		else {
			Properties props = new Properties();
			copyIfSet(this.properties, props, "branch");
			String commitId = this.properties.getShortCommitId();
			if (commitId != null) {
				props.put("commit.id", commitId);
			}
			copyIfSet(this.properties, props, "commit.time");
			return new PropertiesPropertySource("git", props);
		}
	}

	private void copyIfSet(GitProperties source, Properties target, String key) {
		String value = source.get(key);
		if (StringUtils.hasText(value)) {
			target.put(key, value);
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getNestedMap(Map<String, Object> map, String key) {
		Object o = map.get(key);
		if (o == null) {
			return Collections.emptyMap();
		}
		else {
			return (Map<String, Object>) o;
		}
	}

	/**
	 * Defines how git properties should be exposed.
	 */
	public enum Mode {

		/**
		 * Expose all available data, including custom properties.
		 */
		FULL,

		/**
		 * Expose a pre-defined set of core settings only.
		 */
		SIMPLE

	}

}
