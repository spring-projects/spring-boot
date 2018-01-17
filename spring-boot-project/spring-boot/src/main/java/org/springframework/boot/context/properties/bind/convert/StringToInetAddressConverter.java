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

package org.springframework.boot.context.properties.bind.convert;

import java.beans.PropertyEditor;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.core.convert.converter.Converter;

/**
 * {@link PropertyEditor} for {@link InetAddress} objects.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
class StringToInetAddressConverter implements Converter<String, InetAddress> {

	@Override
	public InetAddress convert(String source) {
		try {
			return InetAddress.getByName(source);
		}
		catch (UnknownHostException ex) {
			throw new IllegalStateException("Unknown host " + source, ex);
		}
	}

}
