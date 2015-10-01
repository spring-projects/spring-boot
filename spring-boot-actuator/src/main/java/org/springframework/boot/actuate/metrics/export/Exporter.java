/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.metrics.export;

/**
 * Generic interface for metric exports. As you scale up metric collection you will often
 * need to buffer metric data locally and export it periodically (e.g. for aggregation
 * across a cluster), so this is the marker interface for those operations. The trigger of
 * an export operation might be periodic or event driven, but it remains outside the scope
 * of this interface. You might for instance create an instance of an Exporter and trigger
 * it using a {@code @Scheduled} annotation in a Spring ApplicationContext.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
public interface Exporter {

	/**
	 * Export metric data.
	 */
	void export();

}
