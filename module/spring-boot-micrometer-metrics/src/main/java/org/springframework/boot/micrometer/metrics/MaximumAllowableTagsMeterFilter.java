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

package org.springframework.boot.micrometer.metrics;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * {@link MeterFilter} to log a single warning message and deny a {@link Id Meter.Id}
 * after a number of attempts for a given tag.
 *
 * @author Jon Schneider
 * @author Dmytro Nosan
 * @author Phillip Webb
 * @since 4.0.0
 */
public final class MaximumAllowableTagsMeterFilter implements MeterFilter {

	private final Log logger;

	private final AtomicBoolean alreadyWarned = new AtomicBoolean();

	private final String meterNamePrefix;

	private final int maximumTagValues;

	private final String tagKey;

	private final Supplier<String> message;

	private final Set<String> observedTagValues = ConcurrentHashMap.newKeySet();

	/**
	 * Create a new {@link MaximumAllowableTagsMeterFilter} with an upper bound on the
	 * number of tags produced by matching metrics.
	 * @param meterNamePrefix the prefix of the meter name to apply the filter to
	 * @param tagKey the tag to place an upper bound on
	 * @param maximumTagValues the total number of tag values that are allowable
	 */
	public MaximumAllowableTagsMeterFilter(String meterNamePrefix, String tagKey, int maximumTagValues) {
		this(meterNamePrefix, tagKey, maximumTagValues, (String) null);
	}

	/**
	 * Create a new {@link MaximumAllowableTagsMeterFilter} with an upper bound on the
	 * number of tags produced by matching metrics.
	 * @param meterNamePrefix the prefix of the meter name to apply the filter to
	 * @param tagKey the tag to place an upper bound on
	 * @param maximumTagValues the total number of tag values that are allowable
	 * @param hint an additional hint to add to the logged message or {@code null}
	 */
	public MaximumAllowableTagsMeterFilter(String meterNamePrefix, String tagKey, int maximumTagValues,
			@Nullable String hint) {
		this(null, meterNamePrefix, tagKey, maximumTagValues,
				() -> String.format("Reached the maximum number of '%s' tags for '%s'.%s", tagKey, meterNamePrefix,
						(hint != null) ? " " + hint : ""));
	}

	private MaximumAllowableTagsMeterFilter(@Nullable Log logger, String meterNamePrefix, String tagKey,
			int maximumTagValues, Supplier<String> message) {
		Assert.notNull(message, "'message' must not be null");
		Assert.isTrue(maximumTagValues >= 0, "'maximumTagValues' must be positive");
		this.logger = (logger != null) ? logger : LogFactory.getLog(MaximumAllowableTagsMeterFilter.class);
		this.meterNamePrefix = meterNamePrefix;
		this.maximumTagValues = maximumTagValues;
		this.tagKey = tagKey;
		this.message = message;
	}

	@Override
	public MeterFilterReply accept(Id id) {
		if (this.meterNamePrefix == null) {
			return logAndDeny();
		}
		String tagValue = id.getName().startsWith(this.meterNamePrefix) ? id.getTag(this.tagKey) : null;
		if (tagValue != null && !this.observedTagValues.contains(tagValue)) {
			if (this.observedTagValues.size() >= this.maximumTagValues) {
				return logAndDeny();
			}
			this.observedTagValues.add(tagValue);
		}
		return MeterFilterReply.NEUTRAL;
	}

	private MeterFilterReply logAndDeny() {
		if (this.logger.isWarnEnabled() && this.alreadyWarned.compareAndSet(false, true)) {
			this.logger.warn(this.message.get());
		}
		return MeterFilterReply.DENY;
	}

}
