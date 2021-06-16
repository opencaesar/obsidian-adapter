/**
 * 
 * Copyright 2021 Modelware Solutions and CAE-LIST.
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
 * 
 */
package io.opencaesar.ecore2oml.options;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: check if using Ecore URI Converter is a better Choice
public class URIMapper {

	private Map<String, String> mappedIRIs = new HashMap<>();

	public String getMappedIRI(String original) {
		String newIRI = mappedIRIs.get(original);
		return newIRI != null ? newIRI : original;
	}

	public void init(List<URIMapping> uriMapping) {

		if (uriMapping != null) {
			for (URIMapping mapping : uriMapping) {
				mappedIRIs.put(mapping.NSURI, mapping.value);
			}
		}
	}

}
