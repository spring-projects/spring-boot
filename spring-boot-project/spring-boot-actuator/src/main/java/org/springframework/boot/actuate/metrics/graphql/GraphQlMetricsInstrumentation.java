/*
 * Copyright 2020-2022 the original author or authors.
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

package org.springframework.boot.actuate.metrics.graphql;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;

import graphql.ExecutionResult;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.SimpleInstrumentationContext;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

import org.springframework.boot.actuate.metrics.AutoTimer;
import org.springframework.lang.Nullable;

/**
 * Micrometer-based {@link SimpleInstrumentation}.
 *
 * @author Brian Clozel
 * @since 2.7.0
 */
public class GraphQlMetricsInstrumentation extends SimpleInstrumentation {

	private final MeterRegistry registry;

	private final GraphQlTagsProvider tagsProvider;

	private final AutoTimer autoTimer;

	private final DistributionSummary dataFetchingSummary;

	public GraphQlMetricsInstrumentation(MeterRegistry registry, GraphQlTagsProvider tagsProvider,
			AutoTimer autoTimer) {
		this.registry = registry;
		this.tagsProvider = tagsProvider;
		this.autoTimer = autoTimer;
		this.dataFetchingSummary = DistributionSummary.builder("graphql.request.datafetch.count").baseUnit("calls")
				.description("Count of DataFetcher calls per request.").register(this.registry);
	}

	@Override
	public InstrumentationState createState() {
		return new RequestMetricsInstrumentationState(this.autoTimer, this.registry);
	}

	@Override
	public InstrumentationContext<ExecutionResult> beginExecution(InstrumentationExecutionParameters parameters) {
		if (this.autoTimer.isEnabled()) {
			RequestMetricsInstrumentationState state = parameters.getInstrumentationState();
			state.startTimer();
			return new SimpleInstrumentationContext<ExecutionResult>() {
				@Override
				public void onCompleted(ExecutionResult result, Throwable exc) {
					Iterable<Tag> tags = GraphQlMetricsInstrumentation.this.tagsProvider.getExecutionTags(parameters,
							result, exc);
					state.tags(tags).stopTimer();
					if (!result.getErrors().isEmpty()) {
						result.getErrors()
								.forEach((error) -> GraphQlMetricsInstrumentation.this.registry.counter("graphql.error",
										GraphQlMetricsInstrumentation.this.tagsProvider.getErrorTags(parameters, error))
										.increment());
					}
					GraphQlMetricsInstrumentation.this.dataFetchingSummary.record(state.getDataFetchingCount());
				}
			};
		}
		return super.beginExecution(parameters);
	}

	@Override
	public DataFetcher<?> instrumentDataFetcher(DataFetcher<?> dataFetcher,
			InstrumentationFieldFetchParameters parameters) {
		if (this.autoTimer.isEnabled() && !parameters.isTrivialDataFetcher()) {
			return (environment) -> {
				Timer.Sample sample = Timer.start(this.registry);
				try {
					Object value = dataFetcher.get(environment);
					if (value instanceof CompletionStage<?>) {
						CompletionStage<?> completion = (CompletionStage<?>) value;
						return completion.whenComplete(
								(result, error) -> recordDataFetcherMetric(sample, dataFetcher, parameters, error));
					}
					else {
						recordDataFetcherMetric(sample, dataFetcher, parameters, null);
						return value;
					}
				}
				catch (Throwable throwable) {
					recordDataFetcherMetric(sample, dataFetcher, parameters, throwable);
					throw throwable;
				}
			};
		}
		return super.instrumentDataFetcher(dataFetcher, parameters);
	}

	private void recordDataFetcherMetric(Timer.Sample sample, DataFetcher<?> dataFetcher,
			InstrumentationFieldFetchParameters parameters, @Nullable Throwable throwable) {
		Timer.Builder timer = this.autoTimer.builder("graphql.datafetcher");
		timer.tags(this.tagsProvider.getDataFetchingTags(dataFetcher, parameters, throwable));
		sample.stop(timer.register(this.registry));
		RequestMetricsInstrumentationState state = parameters.getInstrumentationState();
		state.incrementDataFetchingCount();
	}

	static class RequestMetricsInstrumentationState implements InstrumentationState {

		private final MeterRegistry registry;

		private final Timer.Builder timer;

		private Timer.Sample sample;

		private final AtomicLong dataFetchingCount = new AtomicLong();

		RequestMetricsInstrumentationState(AutoTimer autoTimer, MeterRegistry registry) {
			this.timer = autoTimer.builder("graphql.request");
			this.registry = registry;
		}

		RequestMetricsInstrumentationState tags(Iterable<Tag> tags) {
			this.timer.tags(tags);
			return this;
		}

		void startTimer() {
			this.sample = Timer.start(this.registry);
		}

		void stopTimer() {
			this.sample.stop(this.timer.register(this.registry));
		}

		void incrementDataFetchingCount() {
			this.dataFetchingCount.incrementAndGet();
		}

		long getDataFetchingCount() {
			return this.dataFetchingCount.get();
		}

	}

}
