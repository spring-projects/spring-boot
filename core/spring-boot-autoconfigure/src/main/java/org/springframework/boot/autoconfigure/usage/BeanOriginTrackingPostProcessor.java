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

package org.springframework.boot.autoconfigure.usage;

import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Simple {@link BeanPostProcessor} capturing bean class -> code source locations.
 * This is intentionally lightweight; it ignores infrastructure beans.
 */
/**
 * Experimental – captures bean -> code source (jar/location) mapping for enrichment.
 */
public class BeanOriginTrackingPostProcessor implements BeanPostProcessor {

    private final Map<String, String> beanOrigins = new ConcurrentHashMap<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> type = bean.getClass();
        if (type.getName().startsWith("org.springframework.")) {
            return bean; // skip core Spring infrastructure for clarity
        }
        String location = findLocation(type);
        if (location != null) {
            this.beanOrigins.put(beanName, location);
        }
        return bean;
    }

    private String findLocation(Class<?> type) {
        try {
            ProtectionDomain pd = type.getProtectionDomain();
            if (pd == null) {
                return null;
            }
            CodeSource cs = pd.getCodeSource();
            if (cs == null) {
                return null;
            }
            URL url = cs.getLocation();
            return (url != null ? url.toString() : null);
        }
        catch (Throwable ex) {
            return null;
        }
    }

    public Map<String, String> getBeanOrigins() {
        return Collections.unmodifiableMap(this.beanOrigins);
    }
}
