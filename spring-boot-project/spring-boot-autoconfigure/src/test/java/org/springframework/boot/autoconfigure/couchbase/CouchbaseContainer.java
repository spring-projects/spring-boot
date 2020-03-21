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

package org.springframework.boot.autoconfigure.couchbase;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.couchbase.client.core.env.SeedNode;
import com.couchbase.client.core.env.TimeoutConfig;
import com.couchbase.client.core.util.UrlQueryStringBuilder;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.apache.commons.compress.utils.Sets;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.SocatContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import org.testcontainers.utility.ThrowingFunction;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.testcontainers.shaded.org.apache.commons.codec.binary.Base64.encodeBase64String;

/**
 * Temporary copy of TestContainers's Couchbase support until it works against Couchbase
 * SDK v3.
 */
class CouchbaseContainer extends GenericContainer<CouchbaseContainer> {

	public static final String VERSION = "5.5.1";

	public static final String DOCKER_IMAGE_NAME = "couchbase/server:";

	public static final ObjectMapper MAPPER = new ObjectMapper();

	public static final String STATIC_CONFIG = "/opt/couchbase/etc/couchbase/static_config";

	public static final String CAPI_CONFIG = "/opt/couchbase/etc/couchdb/default.d/capi.ini";

	private static final int REQUIRED_DEFAULT_PASSWORD_LENGTH = 6;

	private String memoryQuota = "300";

	private String indexMemoryQuota = "300";

	private String clusterUsername = "Administrator";

	private String clusterPassword = "password";

	private boolean keyValue = true;

	private boolean query = true;

	private boolean index = true;

	private boolean primaryIndex = false;

	private boolean fts = false;

	private ClusterEnvironment couchbaseEnvironment;

	private Cluster couchbaseCluster;

	private final List<BucketAndUserSettings> newBuckets = new ArrayList<>();

	private String urlBase;

	private SocatContainer proxy;

	CouchbaseContainer() {
		this(DOCKER_IMAGE_NAME + VERSION);
	}

	CouchbaseContainer(String imageName) {
		super(imageName);

		withNetwork(Network.SHARED);
		setWaitStrategy(new HttpWaitStrategy().forPath("/ui/index.html"));
	}

	@Override
	public Set<Integer> getLivenessCheckPortNumbers() {
		return Sets.newHashSet(getMappedPort(CouchbasePort.REST));
	}

	@Override
	protected void configure() {
		if (this.clusterPassword.length() < REQUIRED_DEFAULT_PASSWORD_LENGTH) {
			logger().warn("The provided cluster admin password length is less then the default password policy length. "
					+ "Cluster start will fail if configured password requirements are not met.");
		}
	}

	@Override
	protected void doStart() {
		startProxy(getNetworkAliases().get(0));
		try {
			super.doStart();
		}
		catch (Throwable e) {
			this.proxy.stop();
			throw e;
		}
	}

	private void startProxy(String networkAlias) {
		this.proxy = new SocatContainer().withNetwork(getNetwork());

		for (CouchbasePort port : CouchbaseContainer.CouchbasePort.values()) {
			if (port.isDynamic()) {
				this.proxy.withTarget(port.getOriginalPort(), networkAlias);
			}
			else {
				this.proxy.addExposedPort(port.getOriginalPort());
			}
		}

		this.proxy.setWaitStrategy(null);
		this.proxy.start();

		ExecCreateCmdResponse createCmdResponse = this.dockerClient.execCreateCmd(this.proxy.getContainerId())
				.withCmd("sh", "-c",
						Stream.of(CouchbaseContainer.CouchbasePort.values())
								.map(port -> "/usr/bin/socat " + "TCP-LISTEN:" + port.getOriginalPort()
										+ ",fork,reuseaddr " + "TCP:" + networkAlias + ":" + getMappedPort(port))
								.collect(Collectors.joining(" & ", "true", "")))
				.exec();
		try {
			this.dockerClient.execStartCmd(createCmdResponse.getId()).exec(new ExecStartResultCallback())
					.awaitCompletion(10, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			throw new RuntimeException("Interrupted docker start", e);
		}
	}

	@Override
	public List<Integer> getExposedPorts() {
		return this.proxy.getExposedPorts();
	}

	@Override
	public String getContainerIpAddress() {
		return this.proxy.getContainerIpAddress();
	}

	@Override
	public Integer getMappedPort(int originalPort) {
		return this.proxy.getMappedPort(originalPort);
	}

	protected Integer getMappedPort(CouchbasePort port) {
		return getMappedPort(port.getOriginalPort());
	}

	@Override
	public List<Integer> getBoundPortNumbers() {
		return this.proxy.getBoundPortNumbers();
	}

	@Override
	@SuppressWarnings({ "unchecked", "ConstantConditions" })
	public void stop() {
		try {
			stopCluster();
			this.couchbaseCluster = null;
			this.couchbaseEnvironment = null;
		}
		finally {
			Stream.<Runnable>of(super::stop, this.proxy::stop).parallel().forEach(Runnable::run);
		}
	}

	private void stopCluster() {
		getCouchbaseCluster().disconnect();
		getCouchbaseEnvironment().shutdown();
	}

	CouchbaseContainer withNewBucket(BucketSettings bucketSettings) {
		this.newBuckets.add(new BucketAndUserSettings(bucketSettings));
		return self();
	}

	private void initUrlBase() {
		if (this.urlBase == null) {
			this.urlBase = String.format("http://%s:%s", getContainerIpAddress(), getMappedPort(CouchbasePort.REST));
		}
	}

	void initCluster() throws Exception {
		String poolURL = "/pools/default";
		String poolPayload = "memoryQuota=" + URLEncoder.encode(this.memoryQuota, "UTF-8") + "&indexMemoryQuota="
				+ URLEncoder.encode(this.indexMemoryQuota, "UTF-8");

		String setupServicesURL = "/node/controller/setupServices";
		StringBuilder servicePayloadBuilder = new StringBuilder();
		if (this.keyValue) {
			servicePayloadBuilder.append("kv,");
		}
		if (this.query) {
			servicePayloadBuilder.append("n1ql,");
		}
		if (this.index) {
			servicePayloadBuilder.append("index,");
		}
		if (this.fts) {
			servicePayloadBuilder.append("fts,");
		}
		String setupServiceContent = "services=" + URLEncoder.encode(servicePayloadBuilder.toString(), "UTF-8");

		String webSettingsURL = "/settings/web";
		String webSettingsContent = "username=" + URLEncoder.encode(this.clusterUsername, "UTF-8") + "&password="
				+ URLEncoder.encode(this.clusterPassword, "UTF-8") + "&port=8091";

		callCouchbaseRestAPI(poolURL, poolPayload);
		callCouchbaseRestAPI(setupServicesURL, setupServiceContent);
		callCouchbaseRestAPI(webSettingsURL, webSettingsContent);

		createNodeWaitStrategy().waitUntilReady(this);
		callCouchbaseRestAPI("/settings/indexes",
				"indexerThreads=0&logLevel=info&maxRollbackPoints=5&storageMode=memory_optimized");
	}

	@NotNull
	private HttpWaitStrategy createNodeWaitStrategy() {
		return new HttpWaitStrategy().forPath("/pools/default/")
				.withBasicCredentials(this.clusterUsername, this.clusterPassword).forStatusCode(HTTP_OK)
				.forResponsePredicate(response -> {
					try {
						return Optional.of(MAPPER.readTree(response)).map(n -> n.at("/nodes/0/status"))
								.map(JsonNode::asText).map("healthy"::equals).orElse(false);
					}
					catch (IOException e) {
						logger().error("Unable to parse response {}", response);
						return false;
					}
				});
	}

	void createBucket(BucketSettings bucketSetting, boolean primaryIndex) throws IOException {
		// Insert Bucket
		String payload = convertSettingsToParams(bucketSetting, false).build();
		callCouchbaseRestAPI("/pools/default/buckets/", payload);

		// Check that the bucket is ready before moving on
		new HttpWaitStrategy().forPath("/pools/default/buckets/" + bucketSetting.name())
				.withBasicCredentials(this.clusterUsername, this.clusterPassword).forStatusCode(HTTP_OK)
				.waitUntilReady(this);

		if (this.index && this.primaryIndex) {
			Bucket bucket = getCouchbaseCluster().bucket(bucketSetting.name());
			bucket.waitUntilReady(Duration.ofSeconds(10));
			if (primaryIndex) {
				getCouchbaseCluster().queryIndexes().createPrimaryIndex(bucketSetting.name());
			}
		}
	}

	void callCouchbaseRestAPI(String url, String payload) throws IOException {
		initUrlBase();

		String fullUrl = this.urlBase + url;
		HttpURLConnection httpConnection = (HttpURLConnection) ((new URL(fullUrl).openConnection()));
		httpConnection.setDoOutput(true);
		httpConnection.setRequestMethod("POST");
		httpConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		String encoded = encodeBase64String(
				(this.clusterUsername + ":" + this.clusterPassword).getBytes(StandardCharsets.UTF_8));

		httpConnection.setRequestProperty("Authorization", "Basic " + encoded);
		DataOutputStream out = new DataOutputStream(httpConnection.getOutputStream());
		out.writeBytes(payload);
		out.flush();
		httpConnection.getResponseCode();
	}

	@Override
	protected void containerIsCreated(String containerId) {
		patchConfig(STATIC_CONFIG, this::addMappedPorts);
		// capi needs a special configuration, see
		// https://developer.couchbase.com/documentation/server/current/install/install-ports.html
		patchConfig(CAPI_CONFIG, this::replaceCapiPort);
	}

	private void patchConfig(String configLocation, ThrowingFunction<String, String> patchFunction) {
		String patchedConfig = copyFileFromContainer(configLocation,
				inputStream -> patchFunction.apply(IOUtils.toString(inputStream, StandardCharsets.UTF_8)));
		copyFileToContainer(Transferable.of(patchedConfig.getBytes(StandardCharsets.UTF_8)), configLocation);
	}

	private String addMappedPorts(String originalConfig) {
		String portConfig = Stream.of(CouchbaseContainer.CouchbasePort.values()).filter(port -> !port.isDynamic())
				.map(port -> String.format("{%s, %d}.", port.name, getMappedPort(port)))
				.collect(Collectors.joining("\n"));
		return String.format("%s\n%s", originalConfig, portConfig);
	}

	private String replaceCapiPort(String originalConfig) {
		return Arrays.stream(originalConfig.split("\n"))
				.map(s -> (s.matches("port\\s*=\\s*" + CouchbaseContainer.CouchbasePort.CAPI.getOriginalPort()))
						? "port = " + getMappedPort(CouchbaseContainer.CouchbasePort.CAPI) : s)
				.collect(Collectors.joining("\n"));
	}

	@Override
	protected void containerIsStarted(InspectContainerResponse containerInfo) {
		try {
			initCluster();
		}
		catch (Exception e) {
			throw new RuntimeException("Could not init cluster", e);
		}

		if (!this.newBuckets.isEmpty()) {
			for (BucketAndUserSettings bucket : this.newBuckets) {
				try {
					createBucket(bucket.getBucketSettings(), this.primaryIndex);
				}
				catch (Exception e) {
					throw new RuntimeException("Could not create bucket", e);
				}
			}
		}
	}

	private Cluster createCouchbaseCluster() {
		SeedNode seedNode = SeedNode.create(getContainerIpAddress(),
				Optional.of(getMappedPort(CouchbaseContainer.CouchbasePort.MEMCACHED)),
				Optional.of(getMappedPort(CouchbaseContainer.CouchbasePort.REST)));

		return Cluster.connect(new HashSet<>(Collections.singletonList(seedNode)), ClusterOptions
				.clusterOptions(this.clusterUsername, this.clusterPassword).environment(getCouchbaseEnvironment()));
	}

	synchronized ClusterEnvironment getCouchbaseEnvironment() {
		if (this.couchbaseEnvironment == null) {
			this.couchbaseEnvironment = createCouchbaseEnvironment();
		}
		return this.couchbaseEnvironment;
	}

	synchronized Cluster getCouchbaseCluster() {
		if (this.couchbaseCluster == null) {
			this.couchbaseCluster = createCouchbaseCluster();
		}
		return this.couchbaseCluster;
	}

	private ClusterEnvironment createCouchbaseEnvironment() {
		return ClusterEnvironment.builder().timeoutConfig(TimeoutConfig.kvTimeout(Duration.ofSeconds(10))).build();
	}

	CouchbaseContainer withMemoryQuota(String memoryQuota) {
		this.memoryQuota = memoryQuota;
		return self();
	}

	CouchbaseContainer withIndexMemoryQuota(String indexMemoryQuota) {
		this.indexMemoryQuota = indexMemoryQuota;
		return self();
	}

	CouchbaseContainer withClusterAdmin(String username, String password) {
		this.clusterUsername = username;
		this.clusterPassword = password;
		return self();
	}

	CouchbaseContainer withKeyValue(boolean keyValue) {
		this.keyValue = keyValue;
		return self();
	}

	CouchbaseContainer withQuery(boolean query) {
		this.query = query;
		return self();
	}

	CouchbaseContainer withIndex(boolean index) {
		this.index = index;
		return self();
	}

	CouchbaseContainer withPrimaryIndex(boolean primaryIndex) {
		this.primaryIndex = primaryIndex;
		return self();
	}

	public CouchbaseContainer withFts(boolean fts) {
		this.fts = fts;
		return self();
	}

	enum CouchbasePort {

		REST("rest_port", 8091, true), CAPI("capi_port", 8092, false), QUERY("query_port", 8093, false), FTS(
				"fts_http_port", 8094, false), CBAS("cbas_http_port", 8095, false), EVENTING("eventing_http_port", 8096,
						false), MEMCACHED_SSL("memcached_ssl_port", 11207, false), MEMCACHED("memcached_port", 11210,
								false), REST_SSL("ssl_rest_port", 18091, true), CAPI_SSL("ssl_capi_port", 18092,
										false), QUERY_SSL("ssl_query_port", 18093, false), FTS_SSL("fts_ssl_port",
												18094, false), CBAS_SSL("cbas_ssl_port", 18095,
														false), EVENTING_SSL("eventing_ssl_port", 18096, false);

		final String name;

		final int originalPort;

		final boolean dynamic;

		CouchbasePort(String name, int originalPort, boolean dynamic) {
			this.name = name;
			this.originalPort = originalPort;
			this.dynamic = dynamic;
		}

		public String getName() {
			return this.name;
		}

		public int getOriginalPort() {
			return this.originalPort;
		}

		public boolean isDynamic() {
			return this.dynamic;
		}

	}

	private class BucketAndUserSettings {

		private final BucketSettings bucketSettings;

		BucketAndUserSettings(BucketSettings bucketSettings) {
			this.bucketSettings = bucketSettings;
		}

		BucketSettings getBucketSettings() {
			return this.bucketSettings;
		}

	}

	private UrlQueryStringBuilder convertSettingsToParams(final BucketSettings settings, boolean update) {
		UrlQueryStringBuilder params = UrlQueryStringBuilder.createForUrlSafeNames();

		params.add("ramQuotaMB", settings.ramQuotaMB());
		params.add("replicaNumber", settings.numReplicas());
		params.add("flushEnabled", settings.flushEnabled() ? 1 : 0);
		params.add("maxTTL", settings.maxTTL());
		params.add("evictionPolicy", settings.ejectionPolicy().alias());
		params.add("compressionMode", settings.compressionMode().alias());

		// The following values must not be changed on update
		if (!update) {
			params.add("name", settings.name());
			params.add("bucketType", settings.bucketType().alias());
			params.add("conflictResolutionType", settings.conflictResolutionType().alias());
			params.add("replicaIndex", settings.replicaIndexes() ? 1 : 0);
		}

		return params;
	}

}
