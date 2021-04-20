package io.opencaesar.ecore2oml.options;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.opencaesar.ecore2oml.util.SemanticFlagKind;

public class SemanticFlags {
	
	private Map<String,Set<SemanticFlagKind>> iriToFlags = new HashMap<>();
	static private SemanticFlags _instance = new SemanticFlags();
	
	static public SemanticFlags getInstance() {
		return _instance;
	}
	
	public Set<SemanticFlagKind> getSemanticFlags(String iri) {
		Set<SemanticFlagKind> flags = iriToFlags.getOrDefault(iri,Collections.emptySet());
		return flags;
	}

	public static void init(List<URISemanticFlags> flags) {
		synchronized (SemanticFlags.class) {
			if (flags!=null) {
				for (URISemanticFlags iriFlags : flags) {
					Set<SemanticFlagKind> flagsSet = new HashSet<>(iriFlags.onFlags);
					_instance.iriToFlags.put(iriFlags.iri, flagsSet);
				}
			}
		}
	}

}
