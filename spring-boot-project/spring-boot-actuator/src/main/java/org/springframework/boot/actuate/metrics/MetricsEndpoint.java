/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.metrics;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

import org.springframework.boot.actuate.endpoint.InvalidEndpointRequestException;
import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.lang.Nullable;

/**
 * An {@link Endpoint @Endpoint} for exposing the metrics held by a {@link MeterRegistry}.
 *
 * @author Jon Schneider
 * @author Phillip Webb
 * @since 2.0.0
 */
@Endpoint(id = "metrics")
public class MetricsEndpoint {

	private final MeterRegistry registry;

	/**
     * Constructs a new MetricsEndpoint with the specified MeterRegistry.
     * 
     * @param registry the MeterRegistry to be used for metrics tracking
     */
    public MetricsEndpoint(MeterRegistry registry) {
		this.registry = registry;
	}

	/**
     * Retrieves a list of metric names from the registry.
     * 
     * @return a MetricNamesDescriptor object containing the list of metric names
     */
    @ReadOperation
	public MetricNamesDescriptor listNames() {
		Set<String> names = new TreeSet<>();
		collectNames(names, this.registry);
		return new MetricNamesDescriptor(names);
	}

	/**
     * Collects the names of all meters in the given MeterRegistry.
     * If the MeterRegistry is a CompositeMeterRegistry, recursively collects the names from all member registries.
     * Otherwise, collects the names from the meters in the registry.
     *
     * @param names    the set to collect the meter names into
     * @param registry the MeterRegistry to collect the meter names from
     */
    private void collectNames(Set<String> names, MeterRegistry registry) {
		if (registry instanceof CompositeMeterRegistry compositeMeterRegistry) {
			compositeMeterRegistry.getRegistries().forEach((member) -> collectNames(names, member));
		}
		else {
			registry.getMeters().stream().map(this::getName).forEach(names::add);
		}
	}

	/**
     * Returns the name of the Meter object.
     *
     * @param meter the Meter object
     * @return the name of the Meter object
     */
    private String getName(Meter meter) {
		return meter.getId().getName();
	}

	/**
     * Retrieves the metric descriptor for the specified metric name and tags.
     * 
     * @param requiredMetricName The name of the metric to retrieve.
     * @param tag                The list of tags associated with the metric.
     * @return                   The metric descriptor if found, or null if not found.
     */
    @ReadOperation
	public MetricDescriptor metric(@Selector String requiredMetricName, @Nullable List<String> tag) {
		List<Tag> tags = parseTags(tag);
		Collection<Meter> meters = findFirstMatchingMeters(this.registry, requiredMetricName, tags);
		if (meters.isEmpty()) {
			return null;
		}
		Map<Statistic, Double> samples = getSamples(meters);
		Map<String, Set<String>> availableTags = getAvailableTags(meters);
		tags.forEach((t) -> availableTags.remove(t.getKey()));
		Meter.Id meterId = meters.iterator().next().getId();
		return new MetricDescriptor(requiredMetricName, meterId.getDescription(), meterId.getBaseUnit(),
				asList(samples, Sample::new), asList(availableTags, AvailableTag::new));
	}

	/**
     * Parses a list of tags into a list of Tag objects.
     * 
     * @param tags the list of tags to be parsed
     * @return the list of parsed Tag objects
     */
    private List<Tag> parseTags(List<String> tags) {
		return (tags != null) ? tags.stream().map(this::parseTag).toList() : Collections.emptyList();
	}

	/**
     * Parses a tag parameter into a Tag object.
     *
     * @param tag the tag parameter to parse
     * @return the parsed Tag object
     * @throws InvalidEndpointRequestException if the tag parameter is not in the form 'key:value'
     */
    private Tag parseTag(String tag) {
		String[] parts = tag.split(":", 2);
		if (parts.length != 2) {
			throw new InvalidEndpointRequestException(
					"Each tag parameter must be in the form 'key:value' but was: " + tag,
					"Each tag parameter must be in the form 'key:value'");
		}
		return Tag.of(parts[0], parts[1]);
	}

	/**
     * Finds the first set of meters that match the given name and tags in the provided MeterRegistry.
     * If the MeterRegistry is a CompositeMeterRegistry, the method recursively calls itself with the
     * underlying MeterRegistry until a match is found.
     *
     * @param registry The MeterRegistry to search for meters.
     * @param name     The name of the meters to find.
     * @param tags     The tags of the meters to find.
     * @return A collection of meters that match the given name and tags.
     */
    private Collection<Meter> findFirstMatchingMeters(MeterRegistry registry, String name, Iterable<Tag> tags) {
		if (registry instanceof CompositeMeterRegistry compositeMeterRegistry) {
			return findFirstMatchingMeters(compositeMeterRegistry, name, tags);
		}
		return registry.find(name).tags(tags).meters();
	}

	/**
     * Finds the first matching meters in the given CompositeMeterRegistry based on the provided name and tags.
     * 
     * @param composite The CompositeMeterRegistry to search for meters.
     * @param name The name of the meters to search for.
     * @param tags The tags of the meters to search for.
     * @return A collection of meters that match the given name and tags, or an empty collection if no matching meters are found.
     */
    private Collection<Meter> findFirstMatchingMeters(CompositeMeterRegistry composite, String name,
			Iterable<Tag> tags) {
		return composite.getRegistries()
			.stream()
			.map((registry) -> findFirstMatchingMeters(registry, name, tags))
			.filter((matching) -> !matching.isEmpty())
			.findFirst()
			.orElse(Collections.emptyList());
	}

	/**
     * Retrieves the samples for the given collection of meters.
     * 
     * @param meters the collection of meters to retrieve samples from
     * @return a map containing the statistics and their corresponding samples
     */
    private Map<Statistic, Double> getSamples(Collection<Meter> meters) {
		Map<Statistic, Double> samples = new LinkedHashMap<>();
		meters.forEach((meter) -> mergeMeasurements(samples, meter));
		return samples;
	}

	/**
     * Merges the measurements obtained from a meter into the given samples map.
     * 
     * @param samples the map of statistics and their corresponding values
     * @param meter the meter from which the measurements are obtained
     */
    private void mergeMeasurements(Map<Statistic, Double> samples, Meter meter) {
		meter.measure()
			.forEach((measurement) -> samples.merge(measurement.getStatistic(), measurement.getValue(),
					mergeFunction(measurement.getStatistic())));
	}

	/**
     * Returns a BiFunction that merges two Double values based on the specified Statistic.
     * 
     * @param statistic the Statistic to determine the merge operation
     * @return a BiFunction that performs the merge operation
     */
    private BiFunction<Double, Double, Double> mergeFunction(Statistic statistic) {
		return Statistic.MAX.equals(statistic) ? Double::max : Double::sum;
	}

	/**
     * Retrieves the available tags for a collection of meters.
     * 
     * @param meters the collection of meters to retrieve the available tags from
     * @return a map containing the available tags, where the key is the tag name and the value is a set of tag values
     */
    private Map<String, Set<String>> getAvailableTags(Collection<Meter> meters) {
		Map<String, Set<String>> availableTags = new HashMap<>();
		meters.forEach((meter) -> mergeAvailableTags(availableTags, meter));
		return availableTags;
	}

	/**
     * Merges the available tags with the tags of the given meter.
     * 
     * @param availableTags the map of available tags
     * @param meter the meter object
     */
    private void mergeAvailableTags(Map<String, Set<String>> availableTags, Meter meter) {
		meter.getId().getTags().forEach((tag) -> {
			Set<String> value = Collections.singleton(tag.getValue());
			availableTags.merge(tag.getKey(), value, this::merge);
		});
	}

	/**
     * Merges two sets into a single set.
     * 
     * @param set1 the first set to be merged
     * @param set2 the second set to be merged
     * @param <T> the type of elements in the sets
     * @return a new set containing all elements from both input sets
     */
    private <T> Set<T> merge(Set<T> set1, Set<T> set2) {
		Set<T> result = new HashSet<>(set1.size() + set2.size());
		result.addAll(set1);
		result.addAll(set2);
		return result;
	}

	/**
     * Converts a Map into a List using a provided mapper function.
     *
     * @param <K> the type of keys in the Map
     * @param <V> the type of values in the Map
     * @param <T> the type of elements in the resulting List
     * @param map the Map to be converted
     * @param mapper the function to apply to each key-value pair in the Map
     * @return a List containing the elements obtained by applying the mapper function to each key-value pair in the Map
     */
    private <K, V, T> List<T> asList(Map<K, V> map, BiFunction<K, V, T> mapper) {
		return map.entrySet().stream().map((entry) -> mapper.apply(entry.getKey(), entry.getValue())).toList();
	}

	/**
	 * Description of metric names.
	 */
	public static final class MetricNamesDescriptor implements OperationResponseBody {

		private final Set<String> names;

		/**
         * Constructs a new MetricNamesDescriptor with the specified set of names.
         * 
         * @param names the set of names to be used for the MetricNamesDescriptor
         */
        MetricNamesDescriptor(Set<String> names) {
			this.names = names;
		}

		/**
         * Returns a set of names.
         * 
         * @return a set of names
         */
        public Set<String> getNames() {
			return this.names;
		}

	}

	/**
	 * Description of a metric.
	 */
	public static final class MetricDescriptor implements OperationResponseBody {

		private final String name;

		private final String description;

		private final String baseUnit;

		private final List<Sample> measurements;

		private final List<AvailableTag> availableTags;

		/**
         * Constructs a new MetricDescriptor with the specified name, description, base unit, measurements, and available tags.
         * 
         * @param name the name of the metric descriptor
         * @param description the description of the metric descriptor
         * @param baseUnit the base unit of the metric descriptor
         * @param measurements the list of measurements associated with the metric descriptor
         * @param availableTags the list of available tags for the metric descriptor
         */
        MetricDescriptor(String name, String description, String baseUnit, List<Sample> measurements,
				List<AvailableTag> availableTags) {
			this.name = name;
			this.description = description;
			this.baseUnit = baseUnit;
			this.measurements = measurements;
			this.availableTags = availableTags;
		}

		/**
         * Returns the name of the MetricDescriptor.
         *
         * @return the name of the MetricDescriptor
         */
        public String getName() {
			return this.name;
		}

		/**
         * Returns the description of the MetricDescriptor.
         *
         * @return the description of the MetricDescriptor
         */
        public String getDescription() {
			return this.description;
		}

		/**
         * Returns the base unit of the MetricDescriptor.
         *
         * @return the base unit of the MetricDescriptor
         */
        public String getBaseUnit() {
			return this.baseUnit;
		}

		/**
         * Returns the list of measurements.
         *
         * @return the list of measurements
         */
        public List<Sample> getMeasurements() {
			return this.measurements;
		}

		/**
         * Returns the list of available tags.
         *
         * @return the list of available tags
         */
        public List<AvailableTag> getAvailableTags() {
			return this.availableTags;
		}

	}

	/**
	 * A set of tags for further dimensional drill-down and their potential values.
	 */
	public static final class AvailableTag {

		private final String tag;

		private final Set<String> values;

		/**
         * Creates a new instance of the AvailableTag class with the specified tag and set of values.
         * 
         * @param tag the tag associated with the AvailableTag
         * @param values the set of values associated with the AvailableTag
         */
        AvailableTag(String tag, Set<String> values) {
			this.tag = tag;
			this.values = values;
		}

		/**
         * Returns the tag of the AvailableTag object.
         *
         * @return the tag of the AvailableTag object
         */
        public String getTag() {
			return this.tag;
		}

		/**
         * Returns the set of values.
         *
         * @return the set of values
         */
        public Set<String> getValues() {
			return this.values;
		}

	}

	/**
	 * A measurement sample combining a {@link Statistic statistic} and a value.
	 */
	public static final class Sample {

		private final Statistic statistic;

		private final Double value;

		/**
         * Creates a new instance of the Sample class with the specified statistic and value.
         * 
         * @param statistic the statistic associated with the sample
         * @param value the value of the sample
         */
        Sample(Statistic statistic, Double value) {
			this.statistic = statistic;
			this.value = value;
		}

		/**
         * Returns the statistic object associated with this Sample.
         *
         * @return the statistic object
         */
        public Statistic getStatistic() {
			return this.statistic;
		}

		/**
         * Returns the value of the Sample object.
         *
         * @return the value of the Sample object
         */
        public Double getValue() {
			return this.value;
		}

		/**
         * Returns a string representation of the MeasurementSample object.
         *
         * @return a string representation of the MeasurementSample object
         */
        @Override
		public String toString() {
			return "MeasurementSample{statistic=" + this.statistic + ", value=" + this.value + '}';
		}

	}

}
