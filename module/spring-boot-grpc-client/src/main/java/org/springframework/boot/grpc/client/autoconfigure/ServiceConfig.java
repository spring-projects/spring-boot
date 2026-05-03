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

package org.springframework.boot.grpc.client.autoconfigure;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import io.grpc.Status;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.PropertyMapper.Source.Adapter;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.unit.DataSize;

/**
 * Bindable service configuration for gRPC channel. Allows type safe binding of common
 * service configuration options which can ultimately be applied to the {@link Map}
 * provided by a {@link GrpcClientDefaultServiceConfigCustomizer}.
 * <p>
 * The configuration provided here is a subset of the canonical <a href=
 * "https://github.com/grpc/grpc-proto/blob/master/grpc/service_config/service_config.proto">service_config.proto</a>
 * protocol definition. For advanced or experimental service configurations, use the
 * {@link GrpcClientDefaultServiceConfigCustomizer} to directly add any entries supported
 * by {@code grpc-java}.
 *
 * @author Phillip Webb
 * @param loadbalancing load balancing configurations in the order that they should be
 * applied
 * @param method method configuration
 * @param retrythrottling retry throttling policy
 * @param healthcheck health check configuration
 * @since 4.1.0
 * @see GrpcClientDefaultServiceConfigCustomizer
 * @see io.grpc.internal.ServiceConfigUtil
 */
public record ServiceConfig(@Nullable List<LoadBalancingConfig> loadbalancing, @Nullable List<MethodConfig> method,
		@Nullable RetryThrottlingPolicy retrythrottling, @Nullable HealthCheckConfig healthcheck) {

	static final String HEALTH_CHECK_CONFIG_KEY = "healthCheckConfig";

	static final String HEALTH_CHECK_SERVICE_NAME_KEY = "serviceName";

	/**
	 * Apply this service config to the given gRPC Java config Map.
	 * @param grpcJavaConfig the gRPC Java config map
	 */
	public void applyTo(Map<String, Object> grpcJavaConfig) {
		applyTo(new GrpcJavaConfig(grpcJavaConfig));
	}

	private void applyTo(GrpcJavaConfig config) {
		PropertyMapper map = PropertyMapper.get();
		map.from(this::loadbalancing)
			.as(listOf(LoadBalancingConfig::grpcJavaConfig))
			.to(config.in("loadBalancingConfig"));
		map.from(this::method).as(listOf(MethodConfig::grpcJavaConfig)).to(config.in("methodConfig"));
		map.from(this::retrythrottling).as(RetryThrottlingPolicy::grpcJavaConfig).to(config.in("retryThrottling"));
		map.from(this::healthcheck).as(HealthCheckConfig::grpcJavaConfig).to(config.in(HEALTH_CHECK_CONFIG_KEY));
	}

	static <T> Adapter<List<T>, @Nullable List<Map<String, Object>>> listOf(Function<T, Map<String, Object>> adapter) {
		return (list) -> (!CollectionUtils.isEmpty(list)) ? list.stream().map(adapter).toList() : null;
	}

	static String durationString(Duration duration) {
		return duration.getSeconds() + "." + duration.getNano() + "s";
	}

	static String bytesString(DataSize dataSize) {
		return Long.toString(dataSize.toBytes());
	}

	/**
	 * Load balancing config.
	 *
	 * @param pickfirst 'pick first' load balancing
	 * @param roundrobin 'round robin' load balancing
	 * @param weightedroundrobin 'weighted round robin' load balancing
	 * @param grpc 'grpc' load balancing
	 */
	public record LoadBalancingConfig(@Nullable PickFirstLoadBalancingConfig pickfirst,
			@Nullable RoundRobinLoadBalancingConfig roundrobin,
			@Nullable WeightedRoundRobinLoadBalancingConfig weightedroundrobin,
			@Nullable GrpcLoadBalancingConfig grpc) {

		public LoadBalancingConfig {
			if (pickfirst == null && roundrobin == null && weightedroundrobin == null && grpc == null) {
				throw new InvalidConfigurationPropertyValueException("loadbalancing", null,
						"Missing load balancing strategy");
			}
			MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
				entries.put("loadbalancing.pickfirst", pickfirst);
				entries.put("loadbalancing.roundrobin", roundrobin);
				entries.put("loadbalancing.weightedroundrobin", weightedroundrobin);
				entries.put("loadbalancing.grpc", grpc);
			});
		}

		Map<String, Object> grpcJavaConfig() {
			LinkedHashMap<String, Object> grpcJavaConfig = new LinkedHashMap<>();
			PropertyMapper map = PropertyMapper.get();
			map.from(this::pickfirst)
				.as(PickFirstLoadBalancingConfig::grpcJavaConfig)
				.to((loadBalancingConfig) -> grpcJavaConfig.put("pick_first", loadBalancingConfig));
			map.from(this::roundrobin)
				.as(RoundRobinLoadBalancingConfig::grpcJavaConfig)
				.to((loadBalancingConfig) -> grpcJavaConfig.put("round_robin", loadBalancingConfig));
			map.from(this::weightedroundrobin)
				.as(WeightedRoundRobinLoadBalancingConfig::grpcJavaConfig)
				.to((loadBalancingConfig) -> grpcJavaConfig.put("weighted_round_robin", loadBalancingConfig));
			map.from(this::grpc)
				.as(GrpcLoadBalancingConfig::grpcJavaConfig)
				.to((loadBalancingConfig) -> grpcJavaConfig.put("grpclb", loadBalancingConfig));
			return grpcJavaConfig;
		}

		/**
		 * 'pick first' load balancing.
		 *
		 * @param shuffleAddressList randomly shuffle the list of addresses received from
		 * the name resolver before attempting to connect to them.
		 */
		public record PickFirstLoadBalancingConfig(Boolean shuffleAddressList) {

			Map<String, Object> grpcJavaConfig() {
				// Aligned with PickFirstLoadBalancerProvider
				GrpcJavaConfig grpcJavaConfig = new GrpcJavaConfig();
				PropertyMapper map = PropertyMapper.get();
				map.from(this::shuffleAddressList).to(grpcJavaConfig.in("shuffleAddressList"));
				return grpcJavaConfig.asMap();
			}

		}

		/**
		 * 'round robin' load balancing.
		 */
		public record RoundRobinLoadBalancingConfig() {

			/**
			 * Return the gRPC java config as supported by the
			 * {@code SecretRoundRobinLoadBalancerProvider}.
			 * @return the config
			 */
			Map<String, Object> grpcJavaConfig() {
				return Collections.emptyMap();
			}

		}

		/**
		 * 'weighted round robin' load balancing.
		 *
		 * @param blackoutPeriod must report load metrics continuously for at least this
		 * long before the endpoint weight will be used
		 * @param weightExpirationPeriod if has not reported load metrics in this long,
		 * then we stop using the reported weight
		 * @param outOfBandReportingPeriod load reporting interval to request from the
		 * server
		 * @param enableOutOfBandLoadReport whether to enable out-of-band utilization
		 * reporting collections from the endpoints
		 * @param weightUpdatePeriod how often endpoint weights are recalculated
		 * @param errorUtilizationPenalty multiplier used to adjust endpoint weights with
		 * the error rate calculated as eps/qps
		 */
		public record WeightedRoundRobinLoadBalancingConfig(Duration blackoutPeriod, Duration weightExpirationPeriod,
				Duration outOfBandReportingPeriod, Boolean enableOutOfBandLoadReport, Duration weightUpdatePeriod,
				Float errorUtilizationPenalty) {

			Map<String, Object> grpcJavaConfig() {
				// Aligned with WeightedRoundRobinLoadBalancerProvider
				GrpcJavaConfig grpcJavaConfig = new GrpcJavaConfig();
				PropertyMapper map = PropertyMapper.get();
				map.from(this::blackoutPeriod)
					.as(ServiceConfig::durationString)
					.to(grpcJavaConfig.in("blackoutPeriod"));
				map.from(this::weightExpirationPeriod)
					.as(ServiceConfig::durationString)
					.to(grpcJavaConfig.in("weightExpirationPeriod"));
				map.from(this::outOfBandReportingPeriod)
					.as(ServiceConfig::durationString)
					.to(grpcJavaConfig.in("oobReportingPeriod"));
				map.from(this::enableOutOfBandLoadReport).to(grpcJavaConfig.in("enableOobLoadReport"));
				map.from(this::weightUpdatePeriod)
					.as(ServiceConfig::durationString)
					.to(grpcJavaConfig.in("weightUpdatePeriod"));
				map.from(this::errorUtilizationPenalty).to(grpcJavaConfig.in("errorUtilizationPenalty"));
				return grpcJavaConfig.asMap();
			}

		}

		/**
		 * 'grpc' load balancing.
		 *
		 * @param child what load balancer policies to use for routing between the backend
		 * addresses
		 * @param serviceName override of the service name to be sent to the balancer
		 * @param initialFallbackTimeout timeout in seconds for receiving the server list
		 */
		public record GrpcLoadBalancingConfig(List<LoadBalancingConfig> child, String serviceName,
				Duration initialFallbackTimeout) {

			public GrpcLoadBalancingConfig {
				child.forEach(this::assertChild);
			}

			private void assertChild(LoadBalancingConfig child) {
				if (child.pickfirst() == null && child.roundrobin() == null) {
					throw new InvalidConfigurationPropertyValueException("loadbalancing.grpc.child", null,
							"Only 'pickfirst' or 'roundrobin' child load balancer strategies can be used");
				}
			}

			Map<String, Object> grpcJavaConfig() {
				// Aligned with GrpclbLoadBalancerProvider
				GrpcJavaConfig grpcJavaConfig = new GrpcJavaConfig();
				PropertyMapper map = PropertyMapper.get();
				map.from(this::child)
					.as(listOf(LoadBalancingConfig::grpcJavaConfig))
					.to(grpcJavaConfig.in("childPolicy"));
				map.from(this::serviceName).to(grpcJavaConfig.in("serviceName"));
				map.from(this::initialFallbackTimeout)
					.as(ServiceConfig::durationString)
					.to(grpcJavaConfig.in("initialFallbackTimeout"));
				return grpcJavaConfig.asMap();
			}

		}

	}

	/**
	 * Method configuration.
	 *
	 * @param name Names of the methods to which this configuration applies
	 * @param waitForReady Whether RPCs sent to this method should wait until the
	 * connection is ready by default
	 * @param maxRequestMessage maximum allowed payload size for an individual request or
	 * object in a stream
	 * @param maxResponseMessage maximum allowed payload size for an individual response
	 * or object in a stream
	 * @param timeout default timeout for RPCs sent to this method
	 * @param retry retry policy for outgoing RPCs
	 * @param hedging hedging policy for outgoing RPCs
	 */
	public record MethodConfig(List<Name> name, Boolean waitForReady, DataSize maxRequestMessage,
			DataSize maxResponseMessage, Duration timeout, RetryPolicy retry, HedgingPolicy hedging) {

		public MethodConfig {
			MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
				entries.put("method.retry", retry);
				entries.put("method.hedging", hedging);
			});
		}

		static @Nullable List<Map<String, Object>> grpcJavaConfigs(List<MethodConfig> methodConfigs) {
			return (!CollectionUtils.isEmpty(methodConfigs))
					? methodConfigs.stream().map(MethodConfig::grpcJavaConfig).toList() : null;
		}

		Map<String, Object> grpcJavaConfig() {
			GrpcJavaConfig grpcJavaConfig = new GrpcJavaConfig();
			PropertyMapper map = PropertyMapper.get();
			map.from(this::name).as(listOf(Name::grpcJavaConfig)).to(grpcJavaConfig.in("name"));
			map.from(this::waitForReady).to(grpcJavaConfig.in("waitForReady"));
			map.from(this::maxRequestMessage)
				.as(ServiceConfig::bytesString)
				.to(grpcJavaConfig.in("maxRequestMessageBytes"));
			map.from(this::maxResponseMessage)
				.as(ServiceConfig::bytesString)
				.to(grpcJavaConfig.in("maxResponseMessageBytes"));
			map.from(this::timeout).as(ServiceConfig::durationString).to(grpcJavaConfig.in("timeout"));
			map.from(this::retry).as(RetryPolicy::grpcJavaConfig).to(grpcJavaConfig.in("retryPolicy"));
			map.from(this::hedging).as(HedgingPolicy::grpcJavaConfig).to(grpcJavaConfig.in("hedgingPolicy"));
			return grpcJavaConfig.asMap();
		}

		/**
		 * The name of a gRPC method.
		 *
		 * @param service service name
		 * @param method method name
		 */
		public record Name(String service, String method) {

			Map<String, Object> grpcJavaConfig() {
				GrpcJavaConfig grpcJavaConfig = new GrpcJavaConfig();
				PropertyMapper map = PropertyMapper.get();
				map.from(this::service).to(grpcJavaConfig.in("service"));
				map.from(this::method).to(grpcJavaConfig.in("method"));
				return grpcJavaConfig.asMap();
			}

		}

		/**
		 * Retry policy for outgoing RPCs.
		 *
		 * @param maxAttempts maximum number of RPC attempts, including the original
		 * attempt
		 * @param initialBackoff initial exponential backoff
		 * @param maxBackoff maximum exponential backoff
		 * @param backoffMultiplier exponential backoff multiplier
		 * @param perAttemptReceiveTimeout per-attempt receive timeout
		 * @param retryableStatusCodes status codes that may be retried
		 */
		public record RetryPolicy(Integer maxAttempts, Duration initialBackoff, Duration maxBackoff,
				Double backoffMultiplier, Duration perAttemptReceiveTimeout, Set<Status.Code> retryableStatusCodes) {

			Map<String, Object> grpcJavaConfig() {
				GrpcJavaConfig grpcJavaConfig = new GrpcJavaConfig();
				PropertyMapper map = PropertyMapper.get();
				map.from(this::maxAttempts).as(Objects::toString).to(grpcJavaConfig.in("maxAttempts"));
				map.from(this::initialBackoff)
					.as(ServiceConfig::durationString)
					.to(grpcJavaConfig.in("initialBackoff"));
				map.from(this::maxBackoff).as(ServiceConfig::durationString).to(grpcJavaConfig.in("maxBackoff"));
				map.from(this::backoffMultiplier).to(grpcJavaConfig.in("backoffMultiplier"));
				map.from(this::perAttemptReceiveTimeout)
					.as(ServiceConfig::durationString)
					.to(grpcJavaConfig.in("perAttemptRecvTimeout"));
				map.from(this::retryableStatusCodes)
					.as((codes) -> codes.stream().map(Objects::toString).toList())
					.to(grpcJavaConfig.in("retryableStatusCodes"));
				return grpcJavaConfig.asMap();
			}

		}

		/**
		 * Hedging policy for outgoing RPCs.
		 *
		 * @param maxAttempts maximum number of send attempts
		 * @param delay delay for subsequent RPCs
		 * @param nonFatalStatusCodes status codes which indicate other hedged RPCs may
		 * still succeed
		 */
		public record HedgingPolicy(Integer maxAttempts, Duration delay, Set<Status.Code> nonFatalStatusCodes) {

			Map<String, Object> grpcJavaConfig() {
				GrpcJavaConfig grpcJavaConfig = new GrpcJavaConfig();
				PropertyMapper map = PropertyMapper.get();
				map.from(this::maxAttempts).as(Objects::toString).to(grpcJavaConfig.in("maxAttempts"));
				map.from(this::delay).as(ServiceConfig::durationString).to(grpcJavaConfig.in("hedgingDelay"));
				map.from(this::nonFatalStatusCodes)
					.as((codes) -> codes.stream().map(Objects::toString).toList())
					.to(grpcJavaConfig.in("nonFatalStatusCodes"));
				return grpcJavaConfig.asMap();
			}

		}
	}

	/**
	 * Retry throttling policy.
	 *
	 * @param maxTokens maximum number of tokens
	 * @param tokenRatio the token ratio
	 */
	public record RetryThrottlingPolicy(Float maxTokens, Float tokenRatio) {

		Map<String, Object> grpcJavaConfig() {
			GrpcJavaConfig grpcJavaConfig = new GrpcJavaConfig();
			PropertyMapper map = PropertyMapper.get();
			map.from(this::maxTokens).as(Objects::toString).to(grpcJavaConfig.in("maxTokens"));
			map.from(this::tokenRatio).as(Objects::toString).to(grpcJavaConfig.in("tokenRatio"));
			return grpcJavaConfig.asMap();
		}

	}

	/**
	 * Health check configuration.
	 *
	 * @param serviceName service name to use in the health-checking request.
	 */
	public record HealthCheckConfig(String serviceName) {

		Map<String, Object> grpcJavaConfig() {
			GrpcJavaConfig grpcJavaConfig = new GrpcJavaConfig();
			PropertyMapper map = PropertyMapper.get();
			map.from(this::serviceName).to(grpcJavaConfig.in(HEALTH_CHECK_SERVICE_NAME_KEY));
			return grpcJavaConfig.asMap();
		}

	}

	/**
	 * Internal helper to collection gRPC java config.
	 *
	 * @param asMap the underlying data as a map
	 */
	record GrpcJavaConfig(Map<String, Object> asMap) {

		GrpcJavaConfig() {
			this(new LinkedHashMap<>());
		}

		<T> Consumer<T> in(String key) {
			return (value) -> this.asMap.put(key, value);
		}

	}

}
