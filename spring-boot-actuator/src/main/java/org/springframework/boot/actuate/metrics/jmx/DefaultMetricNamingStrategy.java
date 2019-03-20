/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.metrics.jmx;

import java.util.Hashtable;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.jmx.export.naming.KeyNamingStrategy;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.util.StringUtils;

/**
 * MBean naming strategy for metric keys. A metric name of {@code counter.foo.bar.spam}
 * translates to an object name with {@code type=counter}, {@code name=foo} and
 * {@code value=bar.spam}. This results in a more or less pleasing view with no tweaks in
 * jconsole or jvisualvm. The domain is copied from the input key and the type in the
 * input key is discarded.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
public class DefaultMetricNamingStrategy implements ObjectNamingStrategy {

	private ObjectNamingStrategy namingStrategy = new KeyNamingStrategy();

	@Override
	public ObjectName getObjectName(Object managedBean, String beanKey)
			throws MalformedObjectNameException {
		ObjectName objectName = this.namingStrategy.getObjectName(managedBean, beanKey);
		String domain = objectName.getDomain();
		Hashtable<String, String> table = new Hashtable<String, String>(
				objectName.getKeyPropertyList());
		String name = objectName.getKeyProperty("name");
		if (name != null) {
			table.remove("name");
			String[] parts = StringUtils.delimitedListToStringArray(name, ".");
			table.put("type", parts[0]);
			if (parts.length > 1) {
				table.put((parts.length > 2) ? "name" : "value", parts[1]);
			}
			if (parts.length > 2) {
				table.put("value",
						name.substring(parts[0].length() + parts[1].length() + 2));
			}
		}
		return new ObjectName(domain, table);
	}

}
