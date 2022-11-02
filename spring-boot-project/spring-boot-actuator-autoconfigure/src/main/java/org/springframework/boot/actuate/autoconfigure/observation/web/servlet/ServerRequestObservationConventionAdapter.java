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

package org.springframework.boot.actuate.autoconfigure.observation.web.servlet;

import java.util.List;

import io.micrometer.common.KeyValues;
import io.micrometer.core.instrument.Tag;
import io.micrometer.observation.Observation;

import org.springframework.boot.actuate.metrics.web.servlet.DefaultWebMvcTagsProvider;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcTagsContributor;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcTagsProvider;
import org.springframework.http.observation.ServerRequestObservationContext;
import org.springframework.http.observation.ServerRequestObservationConvention;
import org.springframework.util.Assert;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Adapter class that applies {@link WebMvcTagsProvider} tags as a
 * {@link ServerRequestObservationConvention}.
 *
 * @author Brian Clozel
 */
@SuppressWarnings("removal")
@Deprecated(since = "3.0.0", forRemoval = true)
class ServerRequestObservationConventionAdapter implements ServerRequestObservationConvention {

	private final String observationName;

	private final WebMvcTagsProvider tagsProvider;

	ServerRequestObservationConventionAdapter(String observationName, WebMvcTagsProvider tagsProvider,
			List<WebMvcTagsContributor> contributors) {
		Assert.state((tagsProvider != null) || (contributors != null),
				"adapter should adapt to a WebMvcTagsProvider or a list of contributors");
		this.observationName = observationName;
		this.tagsProvider = (tagsProvider != null) ? tagsProvider : new DefaultWebMvcTagsProvider(contributors);
	}

	@Override
	public String getName() {
		return this.observationName;
	}

	@Override
	public boolean supportsContext(Observation.Context context) {
		return context instanceof ServerRequestObservationContext;
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
		Iterable<Tag> tags = this.tagsProvider.getTags(context.getCarrier(), context.getResponse(), getHandler(context),
				context.getError());
		return KeyValues.of(tags, Tag::getKey, Tag::getValue);
	}

	private Object getHandler(ServerRequestObservationContext context) {
		return context.getCarrier().getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
	}

}
