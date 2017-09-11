/**
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
package org.springframework.boot.actuate.metrics.export;

import java.util.Properties;

/**
 * @since 2.0.0
 * @author Jon Schneider
 */
public abstract class RegistryConfigurationProperties {
    private Properties props = new Properties();

    protected abstract String prefix();

    public String get(String k) {
        return props.getProperty(k);
    }

    protected void set(String k, Object v) {
        props.put(prefix() + "." + k, v.toString());
    }
}
