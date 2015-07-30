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

package org.springframework.boot.autoconfigure.couchbase;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Couchbase.
 *
 * @author Eddú Meléndez
 * @since 1.4.0
 */
@ConfigurationProperties(prefix = "spring.data.couchbase")
public class CouchbaseProperties {

	/**
	 * Couchabase server hosts.
	 */
	private String[] hosts;

	/**
	 * Couchbase bucket name.
	 */
	private String bucketName;

	/**
	 * Couchbase bucket password.
	 */
	private String bucketPassword;

	public String[] getHosts() {
		return this.hosts;
	}

	public void setHosts(String[] hosts) {
		this.hosts = hosts;
	}

	public String getBucketName() {
		return this.bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	public String getBucketPassword() {
		return this.bucketPassword;
	}

	public void setBucketPassword(String bucketPassword) {
		this.bucketPassword = bucketPassword;
	}

}
