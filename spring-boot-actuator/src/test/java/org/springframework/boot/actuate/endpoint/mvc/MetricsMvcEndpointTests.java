/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.actuate.endpoint.MetricsEndpoint;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MetricsMvcEndpoint}.
 *
 * @author Sergei Egorov
 */
public class MetricsMvcEndpointTests {
    
    private MetricsEndpoint endpoint = null;

    private MetricsMvcEndpoint mvc = null;

    @Before
    public void init() {
        this.endpoint = mock(MetricsEndpoint.class);
        given(this.endpoint.isEnabled()).willReturn(true);
        this.mvc = new MetricsMvcEndpoint(this.endpoint);
    }
    
    @Test
    public void all() {
        Map<String, Object> metrics = new HashMap<String, Object>();
        metrics.put("a", 1);
        given(this.endpoint.invoke()).willReturn(metrics);
        Object result = this.mvc.invoke();
        assertTrue(result instanceof Map);
        assertTrue(metrics.get("a") == ((Map<String, Object>) result).get("a"));
    }

    @Test
    public void value() {
        Map<String, Object> metrics = new HashMap<String, Object>();
        metrics.put("group1.a", 1);
        metrics.put("group1.b", 2);
        metrics.put("group2.a", 3);
        metrics.put("group2_a", 4);
        metrics.put("foo", "bar");
        given(this.endpoint.invoke()).willReturn(metrics);
        Object result = this.mvc.value(".*");
        assertTrue(result instanceof Map);
        assertTrue(metrics.size() == ((Map<String, Object>) result).size());
        assertTrue(metrics.get("group1.a") == ((Map<String, Object>) result).get("group1.a"));
        assertTrue(metrics.get("group1.b") == ((Map<String, Object>) result).get("group1.b"));
        assertTrue(metrics.get("group2.a") == ((Map<String, Object>) result).get("group2.a"));
        assertTrue(metrics.get("group2_a") == ((Map<String, Object>) result).get("group2_a"));
        assertTrue(metrics.get("foo") == ((Map<String, Object>) result).get("foo"));

        result = this.mvc.value("group[0-9]+\\..*");
        assertTrue(result instanceof Map);
        assertTrue(3 == ((Map<String, Object>) result).size());
        assertTrue(metrics.get("group1.a") == ((Map<String, Object>) result).get("group1.a"));
        assertTrue(metrics.get("group1.b") == ((Map<String, Object>) result).get("group1.b"));
        assertTrue(metrics.get("group2.a") == ((Map<String, Object>) result).get("group2.a"));

        result = this.mvc.value("group1\\..*");
        assertTrue(result instanceof Map);
        assertTrue(2 == ((Map<String, Object>) result).size());
        assertTrue(metrics.get("group1.a") == ((Map<String, Object>) result).get("group1.a"));
        assertTrue(metrics.get("group1.b") == ((Map<String, Object>) result).get("group1.b"));

        result = this.mvc.value("group2.a");
        assertTrue(result instanceof Map);
        assertTrue(1 == ((Map<String, Object>) result).size());
        assertTrue(metrics.get("group2.a") == ((Map<String, Object>) result).get("group2.a"));
    }

    @Test
    public void unknownValue() {
        given(this.endpoint.invoke()).willReturn(Collections.<String, Object>emptyMap());

        Object result = this.mvc.value("unknownKey");
        assertTrue(result instanceof Map);
        assertTrue(((Map) result).isEmpty());
    }
}
