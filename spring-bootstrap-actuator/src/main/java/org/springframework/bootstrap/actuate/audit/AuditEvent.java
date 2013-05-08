/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.bootstrap.actuate.audit;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A value object representing an audit event: at a particular time, a particular user or
 * agent carried out an action of a particular type. This object records the details of
 * such an event.
 * 
 * @author Dave Syer
 * 
 */
public class AuditEvent {

	final private Date timestamp;
	final private String principal;
	final private String type;
	final private Map<String, Object> data;

	/**
	 * Create a new audit event for the current time from data provided as name-value
	 * pairs
	 */
	public AuditEvent(String principal, String type, String... data) {
		this(new Date(), principal, type, convert(data));
	}

	/**
	 * Create a new audit event for the current time
	 */
	public AuditEvent(String principal, String type, Map<String, Object> data) {
		this(new Date(), principal, type, data);
	}

	/**
	 * Create a new audit event.
	 */
	public AuditEvent(Date timestamp, String principal, String type,
			Map<String, Object> data) {
		this.timestamp = timestamp;
		this.principal = principal;
		this.type = type;
		this.data = Collections.unmodifiableMap(data);
	}

	public Date getTimestamp() {
		return this.timestamp;
	}

	public String getPrincipal() {
		return this.principal;
	}

	public String getType() {
		return this.type;
	}

	public Map<String, Object> getData() {
		return this.data;
	}

	private static Map<String, Object> convert(String[] data) {
		Map<String, Object> result = new HashMap<String, Object>();
		for (String entry : data) {
			if (entry.contains("=")) {
				int index = entry.indexOf("=");
				result.put(entry.substring(0, index), entry.substring(index + 1));
			} else {
				result.put(entry, null);
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return "AuditEvent [timestamp=" + this.timestamp + ", principal="
				+ this.principal + ", type=" + this.type + ", data=" + this.data + "]";
	}

}
