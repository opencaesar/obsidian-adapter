package io.opencaesar.ecore2oml.handlers;

import static io.opencaesar.ecore2oml.util.Util.addLabelAnnotationIfNeeded;
import static io.opencaesar.ecore2oml.util.Util.handleNamedElementDoc;

import java.util.Map;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;

import io.opencaesar.ecore2oml.Ecore2Oml;
import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.ecore2oml.util.Util;
import io.opencaesar.oml.AnnotatedElement;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlWriter;

public interface ConversionHandler {
	

	public EObject doConvert(EObject object, Vocabulary vocabulary, OmlWriter oml,
			Map<CollectionKind, Object> collections,Ecore2Oml visitor) ;
	
	default public void postConvert(Vocabulary vocabulary, OmlWriter oml,
			Map<CollectionKind, Object> collections) {
		// No OP
	}
	
	default public EObject convert(EObject object, Vocabulary vocabulary, OmlWriter oml,
			Map<CollectionKind, Object> collections,Ecore2Oml visitor) {
		EObject retVal = doConvert(object, vocabulary, oml, collections, visitor);
		if (object instanceof ENamedElement && retVal instanceof AnnotatedElement) {
			if (retVal instanceof Member) {
				addLabelAnnotationIfNeeded((ENamedElement)object, (Member)retVal,oml,vocabulary);
			}
			Util.addTitle((ENamedElement)object, (AnnotatedElement)retVal, oml, vocabulary);
			handleNamedElementDoc((ENamedElement)object, (AnnotatedElement)retVal,oml,vocabulary);
		}
		return retVal;
	}
	

}
