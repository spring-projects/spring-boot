/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.web.client;

import java.net.InetAddress;

/**
 * Filter to decide if an {@link InetAddress} can be used.
 *
 * @author Scott Frederick
 * @since 3.2.0
 */
public interface InetAddressFilter {

	/**
	 * Return {@code true} if the address should be filtered out, and {@code false} if it
	 * should be used.
	 * @param address the address to filter
	 * @return {@code true} if the address should be filtered out, {@code false}
	 * otherwise.
	 */
	boolean filterAddress(InetAddress address);

}
