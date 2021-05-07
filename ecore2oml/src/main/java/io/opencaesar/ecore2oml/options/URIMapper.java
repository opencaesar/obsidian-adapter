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
