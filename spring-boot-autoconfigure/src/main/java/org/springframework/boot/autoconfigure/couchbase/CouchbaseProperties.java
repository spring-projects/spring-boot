/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.couchbase;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.couchbase.client.CouchbaseClient;

/**
 * Couchbase properties.
 * 
 * @author Michael Nitschinger
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "spring.data.couchbase")
public class CouchbaseProperties {

	private String host = "127.0.0.1";

	private String bucket = "default";

	private String password = "";

	private CouchbaseClient client;

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getBucket() {
		return this.bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public CouchbaseClient createClient() throws URISyntaxException, IOException {
		if (this.client == null) {
			this.client = new CouchbaseClient(Arrays.asList(new URI("http://" + getHost()
					+ ":8091/pools")), getBucket(), getPassword());
		}
		return this.client;
	}

	public void closeClient() {
		if (this.client != null) {
			this.client.shutdown();
		}
	}

}
