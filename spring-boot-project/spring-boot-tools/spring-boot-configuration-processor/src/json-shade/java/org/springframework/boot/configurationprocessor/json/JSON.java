/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.springframework.boot.configurationprocessor.json;

class JSON {

	static double checkDouble(double d) throws JSONException {
		if (Double.isInfinite(d) || Double.isNaN(d)) {
			throw new JSONException("Forbidden numeric value: " + d);
		}
		return d;
	}

	static Boolean toBoolean(Object value) {
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		if (value instanceof String) {
			String stringValue = (String) value;
			if ("true".equalsIgnoreCase(stringValue)) {
				return true;
			}
			if ("false".equalsIgnoreCase(stringValue)) {
				return false;
			}
		}
		return null;
	}

	static Double toDouble(Object value) {
		if (value instanceof Double) {
			return (Double) value;
		}
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		if (value instanceof String) {
			try {
				return Double.valueOf((String) value);
			}
			catch (NumberFormatException ignored) {
			}
		}
		return null;
	}

	static Integer toInteger(Object value) {
		if (value instanceof Integer) {
			return (Integer) value;
		}
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		if (value instanceof String) {
			try {
				return (int) Double.parseDouble((String) value);
			}
			catch (NumberFormatException ignored) {
			}
		}
		return null;
	}

	static Long toLong(Object value) {
		if (value instanceof Long) {
			return (Long) value;
		}
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		if (value instanceof String) {
			try {
				return (long) Double.parseDouble((String) value);
			}
			catch (NumberFormatException ignored) {
			}
		}
		return null;
	}

	static String toString(Object value) {
		if (value instanceof String) {
			return (String) value;
		}
		if (value != null) {
			return String.valueOf(value);
		}
		return null;
	}

	public static JSONException typeMismatch(Object indexOrName, Object actual, String requiredType)
			throws JSONException {
		if (actual == null) {
			throw new JSONException("Value at " + indexOrName + " is null.");
		}
		throw new JSONException("Value " + actual + " at " + indexOrName + " of type " + actual.getClass().getName()
				+ " cannot be converted to " + requiredType);
	}

	public static JSONException typeMismatch(Object actual, String requiredType) throws JSONException {
		if (actual == null) {
			throw new JSONException("Value is null.");
		}
		throw new JSONException("Value " + actual + " of type " + actual.getClass().getName()
				+ " cannot be converted to " + requiredType);
	}

}
