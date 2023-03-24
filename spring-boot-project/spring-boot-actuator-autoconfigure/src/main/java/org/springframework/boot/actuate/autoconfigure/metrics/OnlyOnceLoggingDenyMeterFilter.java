/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * {@link MeterFilter} to log only once a warning message and deny a {@link Id Meter.Id}.
 *
 * @author Jon Schneider
 * @author Dmytro Nosan
 * @since 2.0.5
 */
public final class OnlyOnceLoggingDenyMeterFilter implements MeterFilter {

	private static final Log logger = LogFactory.getLog(OnlyOnceLoggingDenyMeterFilter.class);

	private final AtomicBoolean alreadyWarned = new AtomicBoolean();

	private final Supplier<String> message;

	public OnlyOnceLoggingDenyMeterFilter(Supplier<String> message) {
		Assert.notNull(message, "Message must not be null");
		this.message = message;
	}

	@Override
	public MeterFilterReply accept(Id id) {
		if (logger.isWarnEnabled() && this.alreadyWarned.compareAndSet(false, true)) {
			logger.warn(this.message.get());
		}
		return MeterFilterReply.DENY;
	}

}
