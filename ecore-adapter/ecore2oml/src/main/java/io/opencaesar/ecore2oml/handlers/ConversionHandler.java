package io.opencaesar.ecore2oml.handlers;

import java.util.Map;

import org.eclipse.emf.ecore.EObject;

import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlWriter;

public interface ConversionHandler {
	

	public EObject convert(EObject object, Vocabulary vocabulary, OmlWriter oml,
			Map<CollectionKind, Object> collections) ;
	
	default public void postConvert(Vocabulary vocabulary, OmlWriter oml,
			Map<CollectionKind, Object> collections) {
		// No OP
	}
	

}
