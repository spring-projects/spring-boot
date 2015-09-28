/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.metrics.atsd;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.util.ObjectUtils;


/**
 * Default implementation of {@link AtsdDataEncoder}.
 *
 * @author Alexander Tokarev.
 */
public class DefaultAtsdDataEncoder implements AtsdDataEncoder {
	private static final String COMMAND_NAME = "series";
	private static final String METRIC_PREFIX = " m:";
	private static final String ENTITY_PREFIX = " e:";
	private static final String TAGS_PREFIX = " t:";
	private static final String TIMESTAMP_PREFIX = " ms:";
	private static final String COMMAND_DELIMITER = "\n";
	private static final Pattern FORBIDDEN_CHARS = Pattern.compile("[\\s'\"]+");

	@Override
	public String encode(AtsdData... atsdData) {
		Map<EntityTagsTimestamp, Map<String, Number>> groups = new LinkedHashMap<EntityTagsTimestamp, Map<String, Number>>();
		for (AtsdData data : atsdData) {
			EntityTagsTimestamp key = new EntityTagsTimestamp(data);
			Map<String, Number> group = groups.get(key);
			if (group == null) {
				group = new LinkedHashMap<String, Number>();
				groups.put(key, group);
			}
			group.put(data.getAtsdName().getMetric(), data.getValue());
		}
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<EntityTagsTimestamp, Map<String, Number>> groupKeyValue : groups.entrySet()) {
			sb.append(COMMAND_NAME);
			sb.append(ENTITY_PREFIX).append(clean(groupKeyValue.getKey().entity));
			Long timestamp = groupKeyValue.getKey().timestamp;
			if (timestamp != null) {
				sb.append(TIMESTAMP_PREFIX).append(timestamp);
			}
			appendTags(sb, groupKeyValue.getKey().tags);
			appendMetrics(sb, groupKeyValue.getValue());
			sb.append(COMMAND_DELIMITER);
		}
		return sb.toString();
	}

	private void appendMetrics(StringBuilder sb, Map<String, Number> metrics) {
		for (Map.Entry<String, Number> metricAndValue : metrics.entrySet()) {
			sb.append(METRIC_PREFIX).append(clean(metricAndValue.getKey()))
					.append('=').append(metricAndValue.getValue());
		}
	}

	private void appendTags(StringBuilder sb, Map<String, String> tags) {
		for (Map.Entry<String, String> tagNameAndValue : tags.entrySet()) {
			sb.append(TAGS_PREFIX).append(clean(tagNameAndValue.getKey()))
					.append('=').append(quoteIfNeeded(tagNameAndValue.getValue()));
		}
	}

	protected String quoteIfNeeded(String value) {
		return needQuote(value) ? '"' + value + '"' : value;
	}

	private boolean needQuote(String str) {
		int strLen = str.length();
		for (int i = 0; i < strLen; i++) {
			char ch = str.charAt(i);
			if (Character.isWhitespace(ch) || ch == '"' || ch == '=') {
				return true;
			}
		}
		return false;
	}

	protected String clean(String value) {
		return FORBIDDEN_CHARS.matcher(value).replaceAll("_");
	}


	private static class EntityTagsTimestamp {
		private final String entity;
		private final Map<String, String> tags;
		private final Long timestamp;

		EntityTagsTimestamp(AtsdData atsdData) {
			this.entity = atsdData.getAtsdName().getEntity();
			this.tags = atsdData.getAtsdName().getTags();
			this.timestamp = atsdData.getTimestamp();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			EntityTagsTimestamp that = (EntityTagsTimestamp) o;

			return this.entity.equals(that.entity)
					&& this.tags.equals(that.tags)
					&& ObjectUtils.nullSafeEquals(this.timestamp, that.timestamp);
		}

		@Override
		public int hashCode() {
			int result = this.entity.hashCode();
			result = 31 * result + this.tags.hashCode();
			result = 31 * result + ObjectUtils.nullSafeHashCode(this.timestamp);
			return result;
		}
	}
}
