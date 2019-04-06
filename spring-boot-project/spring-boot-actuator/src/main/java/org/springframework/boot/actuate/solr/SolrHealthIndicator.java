/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.solr;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.params.CoreAdminParams;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;

/**
 * {@link HealthIndicator} for Apache Solr.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Markus Schuch
 * @since 2.0.0
 */
public class SolrHealthIndicator extends AbstractHealthIndicator {

	private final SolrClient solrClient;

	private PathType detectedPathType = PathType.UNKNOWN;

	public SolrHealthIndicator(SolrClient solrClient) {
		super("Solr health check failed");
		this.solrClient = solrClient;
	}

	protected void doHealthCheck(Health.Builder builder) throws Exception {
		int statusCode;

		if (this.detectedPathType == PathType.ROOT) {
			statusCode = doCoreAdminCheck();
		}
		else if (this.detectedPathType == PathType.PARTICULAR_CORE) {
			statusCode = doPingCheck();
		}
		else {
			// We do not know yet, which is the promising
			// health check strategy, so we start trying with
			// a CoreAdmin check, which is the common case
			try {
				statusCode = doCoreAdminCheck();
				// When the CoreAdmin request returns with a
				// valid response, we can assume that this
				// SolrClient is configured with a baseUrl
				// pointing to the root path of the Solr instance
				this.detectedPathType = PathType.ROOT;
			}
			catch (HttpSolrClient.RemoteSolrException ex) {
				// CoreAdmin requests to not work with
				// SolrClients configured with a baseUrl pointing to
				// a particular core and a 404 response indicates
				// that this might be the case.
				if (ex.code() == HttpStatus.NOT_FOUND.value()) {
					statusCode = doPingCheck();
					// When the SolrPing returns with a valid
					// response, we can assume that the baseUrl
					// of this SolrClient points to a particular core
					this.detectedPathType = PathType.PARTICULAR_CORE;
				}
				else {
					// Rethrow every other response code leaving us
					// in the dark about the type of the baseUrl
					throw ex;
				}
			}
		}
		Status status = (statusCode != 0) ? Status.DOWN : Status.UP;
		builder.status(status).withDetail("status", statusCode).withDetail("detectedPathType",
				this.detectedPathType.toString());
	}

	private int doCoreAdminCheck() throws IOException, SolrServerException {
		CoreAdminRequest request = new CoreAdminRequest();
		request.setAction(CoreAdminParams.CoreAdminAction.STATUS);
		CoreAdminResponse response = request.process(this.solrClient);
		return response.getStatus();
	}

	private int doPingCheck() throws IOException, SolrServerException {
		return this.solrClient.ping().getStatus();
	}

	enum PathType {

		ROOT, PARTICULAR_CORE, UNKNOWN

	}

}
