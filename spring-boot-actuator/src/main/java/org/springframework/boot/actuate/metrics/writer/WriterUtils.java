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

package org.springframework.boot.actuate.metrics.writer;

import java.io.Flushable;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.actuate.metrics.export.MetricCopyExporter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author Dave Syer
 */
public class WriterUtils {

	private static final Log logger = LogFactory.getLog(MetricCopyExporter.class);

	public static void flush(MetricWriter writer) {
		if (writer instanceof CompositeMetricWriter) {
			for (MetricWriter element : (CompositeMetricWriter) writer) {
				flush(element);
			}
		}
		try {
			if (ClassUtils.isPresent("java.io.Flushable", null)) {
				if (writer instanceof Flushable) {
					((Flushable) writer).flush();
					return;
				}
			}
			Method method = ReflectionUtils.findMethod(writer.getClass(), "flush");
			if (method != null) {
				ReflectionUtils.invokeMethod(method, writer);
			}
		}
		catch (Exception e) {
			logger.warn("Could not flush MetricWriter: " + e.getClass() + ": "
					+ e.getMessage());
		}
	}

}
