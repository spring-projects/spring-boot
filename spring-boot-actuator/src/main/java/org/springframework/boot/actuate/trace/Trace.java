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

package org.springframework.boot.actuate.trace;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.Map;

/**
 * A value object representing a trace event: at a particular time with a simple (map)
 * information. Can be used for analyzing contextual information such as HTTP headers.
 *
 * @author Dave Syer
 */
@JsonInclude(Include.NON_NULL)
public final class Trace {

	private final String id;
	private final Date started;
	private Date ended;

	private final Map<String, Object> info;

	public Trace(String id, Date started, Map<String, Object> info) {
		super();
		Assert.notNull(id, "Id must not be null");
		Assert.notNull(started, "Timestamp must not be null");
		Assert.notNull(info, "Info must not be null");
		this.id = id;
		this.started = started;
		this.info = info;
	}

	public String getId() {
		return id;
	}

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
	public Date getStarted() {
		return this.started;
	}

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
	public Date getEnded() {
		return ended;
	}

	protected void setEnded(Date ended) {
		this.ended = ended;
	}

	public Map<String, Object> getInfo() {
		return this.info;
	}

}
