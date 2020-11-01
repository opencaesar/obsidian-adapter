package io.opencaesar.ecore2oml.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: check if using Ecore URI Converter is a better Choice
public class URIMapper {
	
	private Map<String,String> mappedIRIs = new HashMap<>();
	static private URIMapper _instance = new URIMapper();
	
	static public URIMapper getInstance() {
		return _instance;
	}
	
	public String getMappedIRI(String original) {
		String newIRI = mappedIRIs.get(original);
		return newIRI!=null? newIRI:original;
	}

	public static void init(List<URIMapping> uriMapping) {
		synchronized (URIMapper.class) {
			if (uriMapping!=null) {
				for (URIMapping mapping : uriMapping) {
					_instance.mappedIRIs.put(mapping.NSURI, mapping.value);
				}
			}
		}
	}

}
