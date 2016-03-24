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

import java.util.Collections;
import java.util.Map;

/**
 * ATSD name. This name represents ATSD time series identifier.
 *
 * @author Alexander Tokarev.
 * @see <a href="https://axibase.com/products/axibase-time-series-database/data-model/">ATSD Data Model</a>
 */
public class AtsdName {
	private String metric;
	private String entity;
	private Map<String, String> tags;

	public AtsdName(String metric, String entity, Map<String, String> tags) {
		this.metric = metric;
		this.entity = entity;
		this.tags = (tags == null) ? Collections.<String, String>emptyMap() : Collections.unmodifiableMap(tags);
	}

	public String getMetric() {
		return this.metric;
	}

	public String getEntity() {
		return this.entity;
	}

	public Map<String, String> getTags() {
		return this.tags;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		AtsdName atsdName = (AtsdName) o;

		return this.metric.equals(atsdName.metric)
				&& this.entity.equals(atsdName.entity)
				&& this.tags.equals(atsdName.tags);

	}

	@Override
	public int hashCode() {
		int result = this.metric.hashCode();
		result = 31 * result + this.entity.hashCode();
		result = 31 * result + this.tags.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "AtsdName{" +
				"metric='" + this.metric + '\'' +
				", entity='" + this.entity + '\'' +
				", tags=" + this.tags +
				'}';
	}
}
