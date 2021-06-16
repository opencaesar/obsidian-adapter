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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.opencaesar.ecore2oml.util.SemanticFlagKind;

public class SemanticFlags {

	private Map<String, Set<SemanticFlagKind>> iriToFlags = new HashMap<>();

	public Set<SemanticFlagKind> getSemanticFlags(String iri) {
		Set<SemanticFlagKind> flags = iriToFlags.getOrDefault(iri, Collections.emptySet());
		return flags;
	}

	public void init(List<URISemanticFlags> flags) {
		if (flags != null) {
			for (URISemanticFlags iriFlags : flags) {
				Set<SemanticFlagKind> flagsSet = new HashSet<>(iriFlags.onFlags);
				iriToFlags.put(iriFlags.iri, flagsSet);
			}
		}
	}

}
