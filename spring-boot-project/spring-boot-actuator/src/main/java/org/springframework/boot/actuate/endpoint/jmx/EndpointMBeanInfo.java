/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.jmx;

import java.util.Map;

import javax.management.MBeanInfo;

import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.Operation;

/**
 * The {@link MBeanInfo} for a particular {@link EndpointInfo endpoint}. Maps operation
 * names to an {@link Operation}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public final class EndpointMBeanInfo {

	private final String endpointId;

	private final MBeanInfo mBeanInfo;

	private final Map<String, JmxOperation> operations;

	public EndpointMBeanInfo(String endpointId, MBeanInfo mBeanInfo,
			Map<String, JmxOperation> operations) {
		this.endpointId = endpointId;
		this.mBeanInfo = mBeanInfo;
		this.operations = operations;
	}

	public String getEndpointId() {
		return this.endpointId;
	}

	public MBeanInfo getMbeanInfo() {
		return this.mBeanInfo;
	}

	public Map<String, JmxOperation> getOperations() {
		return this.operations;
	}

}
