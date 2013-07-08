/*
 * Copyright 2012 the original author or authors.
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

package org.springframework.bootstrap.config;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

/**
 * Base class for Yaml factories.
 * 
 * @author Dave Syer
 */
public class YamlProcessor {

	private final Log logger = LogFactory.getLog(getClass());

	private ResolutionMethod resolutionMethod = ResolutionMethod.OVERRIDE;

	private Resource[] resources = new Resource[0];

	private List<DocumentMatcher> documentMatchers = Collections.emptyList();

	private boolean matchDefault = true;

	/**
	 * A map of document matchers allowing callers to selectively use only some of the
	 * documents in a YAML resource. In YAML documents are separated by
	 * <code>---<code> lines, and each document is converted to properties before the match is made. E.g.
	 * 
	 * <pre>
	 * environment: dev
	 * url: http://dev.bar.com
	 * name: Developer Setup
	 * ---
	 * environment: prod
	 * url:http://foo.bar.com
	 * name: My Cool App
	 * </pre>
	 * 
	 * when mapped with <code>documentMatchers = YamlProcessor.mapMatcher({"environment": "prod"})</code>
	 * would end up as
	 * 
	 * <pre>
	 * environment=prod
	 * url=http://foo.bar.com
	 * name=My Cool App
	 * url=http://dev.bar.com
	 * </pre>
	 * 
	 * @param matchers a map of keys to value patterns (regular expressions)
	 */
	public void setDocumentMatchers(List<? extends DocumentMatcher> matchers) {
		this.documentMatchers = Collections.unmodifiableList(matchers);
	}

	/**
	 * Flag indicating that a document for which all the
	 * {@link #setDocumentMatchers(List) document matchers} abstain will nevertheless
	 * match.
	 * 
	 * @param matchDefault the flag to set (default true)
	 */
	public void setMatchDefault(boolean matchDefault) {
		this.matchDefault = matchDefault;
	}

	/**
	 * Method to use for resolving resources. Each resource will be converted to a Map, so
	 * this property is used to decide which map entries to keep in the final output from
	 * this factory. Possible values:
	 * <ul>
	 * <li><code>OVERRIDE</code> for replacing values from earlier in the list</li>
	 * <li><code>FIRST_FOUND</code> if you want to take the first resource in the list
	 * that exists and use just that.</li>
	 * </ul>
	 * 
	 * 
	 * @param resolutionMethod the resolution method to set. Defaults to OVERRIDE.
	 */
	public void setResolutionMethod(ResolutionMethod resolutionMethod) {
		this.resolutionMethod = resolutionMethod;
	}

	/**
	 * @param resources the resources to set
	 */
	public void setResources(Resource[] resources) {
		this.resources = (resources == null ? null : resources.clone());
	}

	/**
	 * Provides an opportunity for subclasses to process the Yaml parsed from the supplied
	 * resources. Each resource is parsed in turn and the documents inside checked against
	 * the {@link #setDocumentMatchers(List) matchers}. If a document matches it is passed
	 * into the callback, along with its representation as Properties. Depending on the
	 * {@link #setResolutionMethod(ResolutionMethod)} not all of the documents will be
	 * parsed.
	 * 
	 * @param callback a callback to delegate to once matching documents are found
	 */
	protected void process(MatchCallback callback) {
		Yaml yaml = new Yaml();
		for (Resource resource : this.resources) {
			boolean found = process(callback, yaml, resource);
			if (this.resolutionMethod == ResolutionMethod.FIRST_FOUND && found) {
				return;
			}
		}
	}

	private boolean process(MatchCallback callback, Yaml yaml, Resource resource) {
		int count = 0;
		try {
			this.logger.info("Loading from YAML: " + resource);
			for (Object object : yaml.loadAll(resource.getInputStream())) {
				if (object != null && process(asMap(object), callback)) {
					count++;
					if (this.resolutionMethod == ResolutionMethod.FIRST_FOUND) {
						break;
					}
				}
			}
			this.logger.info("Loaded " + count + " document" + (count > 1 ? "s" : "")
					+ " from YAML resource: " + resource);
		}
		catch (IOException ex) {
			handleProcessError(resource, ex);
		}
		return count > 0;
	}

	private void handleProcessError(Resource resource, IOException ex) {
		if (this.resolutionMethod != ResolutionMethod.FIRST_FOUND
				&& this.resolutionMethod != ResolutionMethod.OVERRIDE_AND_IGNORE) {
			throw new IllegalStateException(ex);
		}
		if (this.logger.isWarnEnabled()) {
			this.logger.warn("Could not load map from " + resource + ": "
					+ ex.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> asMap(Object object) {
		return (Map<String, Object>) object;
	}

	private boolean process(Map<String, Object> map, MatchCallback callback) {
		Properties properties = new Properties();
		assignProperties(properties, map, null);
		if (this.documentMatchers.isEmpty()) {
			this.logger.debug("Merging document (no matchers set)" + map);
			callback.process(properties, map);
		}
		else {
			boolean valueFound = false;
			MatchStatus result = MatchStatus.ABSTAIN;
			for (DocumentMatcher matcher : this.documentMatchers) {
				MatchStatus match = matcher.matches(properties);
				result = match.ordinal() < result.ordinal() ? match : result;
				if (match == MatchStatus.FOUND) {
					this.logger.debug("Matched document with document matcher: "
							+ properties);
					callback.process(properties, map);
					valueFound = true;
					// No need to check for more matches
					break;
				}
			}
			if (result == MatchStatus.ABSTAIN && this.matchDefault) {
				this.logger.debug("Matched document with default matcher: " + map);
				callback.process(properties, map);
			}
			else if (!valueFound) {
				this.logger.debug("Unmatched document");
				return false;
			}
		}
		return true;
	}

	private void assignProperties(Properties properties, Map<String, Object> input,
			String path) {
		for (Entry<String, Object> entry : input.entrySet()) {
			String key = entry.getKey();
			if (StringUtils.hasText(path)) {
				if (key.startsWith("[")) {
					key = path + key;
				}
				else {
					key = path + "." + key;
				}
			}
			Object value = entry.getValue();
			if (value instanceof String) {
				properties.put(key, value);
			}
			else if (value instanceof Map) {
				// Need a compound key
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) value;
				assignProperties(properties, map, key);
			}
			else if (value instanceof Collection) {
				// Need a compound key
				@SuppressWarnings("unchecked")
				Collection<Object> collection = (Collection<Object>) value;
				properties.put(key,
						StringUtils.collectionToCommaDelimitedString(collection));
				int count = 0;
				for (Object object : collection) {
					assignProperties(properties,
							Collections.singletonMap("[" + (count++) + "]", object), key);
				}
			}
			else {
				properties.put(key, value == null ? "" : value);
			}
		}
	}

	public interface MatchCallback {
		void process(Properties properties, Map<String, Object> map);
	}

	public interface DocumentMatcher {
		MatchStatus matches(Properties properties);
	}

	public static enum ResolutionMethod {
		OVERRIDE, OVERRIDE_AND_IGNORE, FIRST_FOUND
	}

	public static enum MatchStatus {
		FOUND, NOT_FOUND, ABSTAIN
	}

	/**
	 * Matches a document containing a given key and where the value of that key is an
	 * array containing one of the given values, or where one of the values matches one of
	 * the given values (interpreted as regexes).
	 */
	public static class ArrayDocumentMatcher implements DocumentMatcher {

		private String key;

		private String[] patterns;

		public ArrayDocumentMatcher(final String key, final String... patterns) {
			this.key = key;
			this.patterns = patterns;

		}

		@Override
		public MatchStatus matches(Properties properties) {
			if (!properties.containsKey(this.key)) {
				return MatchStatus.ABSTAIN;
			}
			Set<String> values = StringUtils.commaDelimitedListToSet(properties
					.getProperty(this.key));
			for (String pattern : this.patterns) {
				for (String value : values) {
					if (value.matches(pattern)) {
						return MatchStatus.FOUND;
					}
				}
			}
			return MatchStatus.NOT_FOUND;
		}

	}

}
