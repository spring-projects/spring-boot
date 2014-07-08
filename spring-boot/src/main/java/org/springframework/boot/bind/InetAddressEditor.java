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

package org.springframework.boot.bind;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * {@link PropertyEditor} for {@link InetAddress} objects.
 *
 * @author Dave Syer
 */
public class InetAddressEditor extends PropertyEditorSupport implements PropertyEditor {

	@Override
	public String getAsText() {
		return ((InetAddress) getValue()).getHostAddress();
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		try {
			setValue(InetAddress.getByName(text));
		}
		catch (UnknownHostException ex) {
			throw new IllegalArgumentException("Cannot locate host", ex);
		}
	}

}
