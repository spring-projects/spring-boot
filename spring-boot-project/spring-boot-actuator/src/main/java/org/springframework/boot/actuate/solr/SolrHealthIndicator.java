/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.params.CoreAdminParams;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

/**
 * {@link HealthIndicator} for Apache Solr.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class SolrHealthIndicator extends AbstractHealthIndicator {

	private final SolrClient solrClient;

	public SolrHealthIndicator(SolrClient solrClient) {
		super("Solr health check failed");
		this.solrClient = solrClient;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		CoreAdminRequest request = new CoreAdminRequest();
		request.setAction(CoreAdminParams.CoreAdminAction.STATUS);
		CoreAdminResponse response = request.process(this.solrClient);
		int statusCode = response.getStatus();
		Status status = (statusCode != 0) ? Status.DOWN : Status.UP;
		builder.status(status).withDetail("status", statusCode);
	}

}
