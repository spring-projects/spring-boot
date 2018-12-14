/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.springframework.boot.configurationprocessor.json;

// Note: this class was written without inspecting the non-free org.json source code.

/**
 * Thrown to indicate a problem with the JSON API. Such problems include:
 * <ul>
 * <li>Attempts to parse or construct malformed documents
 * <li>Use of null as a name
 * <li>Use of numeric types not available to JSON, such as {@link Double#isNaN() NaNs} or
 * {@link Double#isInfinite() infinities}.
 * <li>Lookups using an out of range index or nonexistent name
 * <li>Type mismatches on lookups
 * </ul>
 * <p>
 * Although this is a checked exception, it is rarely recoverable. Most callers should
 * simply wrap this exception in an unchecked exception and rethrow: <pre class="code">
 *     public JSONArray toJSONObject() {
 *     try {
 *         JSONObject result = new JSONObject();
 *         ...
 *     } catch (JSONException e) {
 *         throw new RuntimeException(e);
 *     }
 * }</pre>
 */
public class JSONException extends Exception {

	public JSONException(String s) {
		super(s);
	}

}
