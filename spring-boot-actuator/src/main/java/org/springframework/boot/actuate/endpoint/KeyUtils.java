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

package org.springframework.boot.actuate.endpoint;

import java.util.regex.Pattern;

/**
 * Convenience class for working with keys.
 *
 * @author Sergei Egorov
 */
public class KeyUtils {
    
    private static final String[] REGEX_PARTS = { "*", "$", "^", "+" };

    public static Pattern getPattern(String value) {
        return getPattern(value, "^");
    }

    public static Pattern getPattern(String value, String prefix) {
        if (isRegex(value)) {
            return Pattern.compile(value, Pattern.CASE_INSENSITIVE);
        }
        return Pattern.compile(prefix + Pattern.quote(value) + "$", Pattern.CASE_INSENSITIVE);
    }

    public static boolean isRegex(String value) {
        for (String part : REGEX_PARTS) {
            if (value.contains(part)) {
                return true;
            }
        }
        return false;
    }
}
