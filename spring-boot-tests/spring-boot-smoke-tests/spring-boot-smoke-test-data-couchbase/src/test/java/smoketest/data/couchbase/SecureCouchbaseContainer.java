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

package smoketest.data.couchbase;

import java.time.Duration;

import com.github.dockerjava.api.command.InspectContainerResponse;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.testcontainers.couchbase.CouchbaseContainer;
import org.testcontainers.utility.MountableFile;

import org.springframework.boot.testsupport.testcontainers.DockerImageNames;

/**
 * A {@link CouchbaseContainer} for Couchbase with SSL configuration.
 *
 * @author Scott Frederick
 */
public class SecureCouchbaseContainer extends CouchbaseContainer {

	private static final int MANAGEMENT_PORT = 8091;

	private static final int KV_SSL_PORT = 11207;

	private static final String ADMIN_USER = "Administrator";

	private static final String ADMIN_PASSWORD = "password";

	public SecureCouchbaseContainer() {
		super(DockerImageNames.couchbase());
		withStartupAttempts(5);
		withStartupTimeout(Duration.ofMinutes(10));
		withCopyFileToContainer(MountableFile.forClasspathResource("/ssl/test-server.crt"),
				"/opt/couchbase/var/lib/couchbase/inbox/chain.pem");
		withCopyFileToContainer(MountableFile.forClasspathResource("/ssl/test-server.key"),
				"/opt/couchbase/var/lib/couchbase/inbox/pkey.key");
		withCopyFileToContainer(MountableFile.forClasspathResource("/ssl/test-ca.crt"),
				"/opt/couchbase/var/lib/couchbase/inbox/CA/ca.pem");
	}

	@Override
	public String getConnectionString() {
		return "couchbase://%s:%d".formatted(getHost(), getMappedPort(KV_SSL_PORT));
	}

	@Override
	protected void containerIsStarting(InspectContainerResponse containerInfo) {
		super.containerIsStarting(containerInfo);
		doHttpRequest("node/controller/loadTrustedCAs");
		doHttpRequest("node/controller/reloadCertificate");
	}

	private void doHttpRequest(String path) {
		Response response;
		try {
			String url = "http://%s:%d/%s".formatted(getHost(), getMappedPort(MANAGEMENT_PORT), path);
			Request.Builder requestBuilder = new Request.Builder().url(url)
				.header("Authorization", Credentials.basic(ADMIN_USER, ADMIN_PASSWORD))
				.post(RequestBody.create("".getBytes()));
			response = new OkHttpClient().newCall(requestBuilder.build()).execute();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Error calling Couchbase HTTP endpoint", ex);
		}
		if (!response.isSuccessful()) {
			throw new IllegalStateException("Error calling Couchbase HTTP endpoint: " + response);
		}
	}

}
