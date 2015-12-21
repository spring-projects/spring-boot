/*
 * Copyright 2010-2015 the original author or authors.
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

package org.springframework.boot.cloudfoundry;

import org.springframework.boot.cloud.CloudFoundryVcapEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.Environment;

/**
 * An {@link EnvironmentPostProcessor} that knows where to find VCAP (a.k.a. Cloud
 * Foundry) meta data in the existing environment. It parses out the VCAP_APPLICATION and
 * VCAP_SERVICES meta data and dumps it in a form that is easily consumed by
 * {@link Environment} users. If the app is running in Cloud Foundry then both meta data
 * items are JSON objects encoded in OS environment variables. VCAP_APPLICATION is a
 * shallow hash with basic information about the application (name, instance id, instance
 * index, etc.), and VCAP_SERVICES is a hash of lists where the keys are service labels
 * and the values are lists of hashes of service instance meta data. Examples are:
 *
 * <pre class="code">
 * VCAP_APPLICATION: {"instance_id":"2ce0ac627a6c8e47e936d829a3a47b5b","instance_index":0,
 *   "version":"0138c4a6-2a73-416b-aca0-572c09f7ca53","name":"foo",
 *   "uris":["foo.cfapps.io"], ...}
 * VCAP_SERVICES: {"rds-mysql-1.0":[{"name":"mysql","label":"rds-mysql-1.0","plan":"10mb",
 *   "credentials":{"name":"d04fb13d27d964c62b267bbba1cffb9da","hostname":"mysql-service-public.clqg2e2w3ecf.us-east-1.rds.amazonaws.com",
 *   "host":"mysql-service-public.clqg2e2w3ecf.us-east-1.rds.amazonaws.com","port":3306,"user":"urpRuqTf8Cpe6",
 *   "username":"urpRuqTf8Cpe6","password":"pxLsGVpsC9A5S"}
 * }]}
 * </pre>
 *
 * These objects are flattened into properties. The VCAP_APPLICATION object goes straight
 * to {@code vcap.application.*} in a fairly obvious way, and the VCAP_SERVICES object is
 * unwrapped so that it is a hash of objects with key equal to the service instance name
 * (e.g. "mysql" in the example above), and value equal to that instances properties, and
 * then flattened in the same way. E.g.
 *
 * <pre class="code">
 * vcap.application.instance_id: 2ce0ac627a6c8e47e936d829a3a47b5b
 * vcap.application.version: 0138c4a6-2a73-416b-aca0-572c09f7ca53
 * vcap.application.name: foo
 * vcap.application.uris[0]: foo.cfapps.io
 *
 * vcap.services.mysql.name: mysql
 * vcap.services.mysql.label: rds-mysql-1.0
 * vcap.services.mysql.credentials.name: d04fb13d27d964c62b267bbba1cffb9da
 * vcap.services.mysql.credentials.port: 3306
 * vcap.services.mysql.credentials.host: mysql-service-public.clqg2e2w3ecf.us-east-1.rds.amazonaws.com
 * vcap.services.mysql.credentials.username: urpRuqTf8Cpe6
 * vcap.services.mysql.credentials.password: pxLsGVpsC9A5S
 * ...
 * </pre>
 *
 * N.B. this initializer is mainly intended for informational use (the application and
 * instance ids are particularly useful). For service binding you might find that Spring
 * Cloud is more convenient and more robust against potential changes in Cloud Foundry.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @deprecated since 1.3.0 in favor of CloudFoundryVcapEnvironmentPostProcessor
 */
@Deprecated
public class VcapEnvironmentPostProcessor
		extends CloudFoundryVcapEnvironmentPostProcessor {

}
