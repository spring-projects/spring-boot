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

package org.springframework.boot.info;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Properties;

/**
 * Provide git-related information such as commit id and time.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
public class GitProperties extends InfoProperties {

	public GitProperties(Properties entries) {
		super(processEntries(entries));
	}

	/**
	 * Return the name of the branch or {@code null}.
	 * @return the branch
	 */
	public String getBranch() {
		return get("branch");
	}

	/**
	 * Return the full id of the commit or {@code null}.
	 * @return the full commit id
	 */
	public String getCommitId() {
		return get("commit.id");
	}

	/**
	 * Return the abbreviated id of the commit or {@code null}.
	 * @return the short commit id
	 */
	public String getShortCommitId() {
		String shortId = get("commit.id.abbrev");
		if (shortId != null) {
			return shortId;
		}
		String id = getCommitId();
		if (id == null) {
			return null;
		}
		return (id.length() > 7 ? id.substring(0, 7) : id);
	}

	/**
	 * Return the timestamp of the commit or {@code null}.
	 * <p>
	 * If the original value could not be parsed properly, it is still available with the
	 * {@code commit.time} key.
	 * @return the commit time
	 * @see #get(String)
	 */
	public Instant getCommitTime() {
		return getInstant("commit.time");
	}

	private static Properties processEntries(Properties properties) {
		coercePropertyToEpoch(properties, "commit.time");
		coercePropertyToEpoch(properties, "build.time");
		Object commitId = properties.get("commit.id");
		if (commitId != null) {
			// Can get converted into a map, so we copy the entry as a nested key
			properties.put("commit.id.full", commitId);
		}
		return properties;
	}

	private static void coercePropertyToEpoch(Properties properties, String key) {
		String value = properties.getProperty(key);
		if (value != null) {
			properties.setProperty(key, coerceToEpoch(value));
		}
	}

	/**
	 * Attempt to convert the specified value to epoch time. Git properties information
	 * are known to be specified either as epoch time in seconds or using a specific date
	 * format.
	 * @param s the value to coerce to
	 * @return the epoch time in milliseconds or the original value if it couldn't be
	 * converted
	 */
	private static String coerceToEpoch(String s) {
		Long epoch = parseEpochSecond(s);
		if (epoch != null) {
			return String.valueOf(epoch);
		}
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		try {
			return String.valueOf(format.parse(s).getTime());
		}
		catch (ParseException ex) {
			return s;
		}
	}

	private static Long parseEpochSecond(String s) {
		try {
			return Long.parseLong(s) * 1000;
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

}
