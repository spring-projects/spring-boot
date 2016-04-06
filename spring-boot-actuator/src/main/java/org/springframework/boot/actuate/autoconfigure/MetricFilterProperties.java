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

package org.springframework.boot.actuate.autoconfigure;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the {@link MetricsFilter}.
 *
 * @author Sebastian Kirsch
 * @author Phillip Webb
 * @since 1.4.0
 */
@ConfigurationProperties("endpoints.metrics.filter")
public class MetricFilterProperties {

	/**
	 * Submissions that should be made to the gauge.
	 */
	private Set<MetricsFilterSubmission> gaugeSubmissions;

	/**
	 * Submissions that should be made to the counter.
	 */
	private Set<MetricsFilterSubmission> counterSubmissions;

	public MetricFilterProperties() {
		this.gaugeSubmissions = new HashSet<MetricsFilterSubmission>(
				EnumSet.of(MetricsFilterSubmission.MERGED));
		this.counterSubmissions = new HashSet<MetricsFilterSubmission>(
				EnumSet.of(MetricsFilterSubmission.MERGED));
	}

	public Set<MetricsFilterSubmission> getGaugeSubmissions() {
		return this.gaugeSubmissions;
	}

	public void setGaugeSubmissions(Set<MetricsFilterSubmission> gaugeSubmissions) {
		this.gaugeSubmissions = gaugeSubmissions;
	}

	public Set<MetricsFilterSubmission> getCounterSubmissions() {
		return this.counterSubmissions;
	}

	public void setCounterSubmissions(Set<MetricsFilterSubmission> counterSubmissions) {
		this.counterSubmissions = counterSubmissions;
	}

	boolean shouldSubmitToGauge(MetricsFilterSubmission submission) {
		return shouldSubmit(this.gaugeSubmissions, submission);
	}

	boolean shouldSubmitToCounter(MetricsFilterSubmission submission) {
		return shouldSubmit(this.counterSubmissions, submission);
	}

	private boolean shouldSubmit(Set<MetricsFilterSubmission> submissions,
			MetricsFilterSubmission submission) {
		return submissions != null && submissions.contains(submission);
	}

}
