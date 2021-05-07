package io.opencaesar.ecore2oml.handlers;

import static io.opencaesar.ecore2oml.util.NameSpaces.OWL;

import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import io.opencaesar.ecore2oml.Ecore2Oml;
import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.ecore2oml.util.Util;
import io.opencaesar.oml.SeparatorKind;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlWriter;

public class EPackageHandler implements ConversionHandler {
	
	private final URI outputResourceURI;
	
	public EPackageHandler(URI outputRURI) {
		// TODO Auto-generated constructor stub
		this.outputResourceURI = outputRURI;
	}

	@Override
	public EObject doConvert(EObject eObject, Vocabulary vocabulary, OmlWriter oml,
			Map<CollectionKind, Object> collections, Ecore2Oml visitor) {
		EPackage object = (EPackage)eObject;
		final String iri = Util.getIri(object, visitor.context);
		final SeparatorKind separator = Util.getSeparator(object);
		final String pefix = Util.getPrefix(object);
		
		vocabulary = oml.createVocabulary(outputResourceURI, iri, separator, pefix);
		visitor.setVocabulary(vocabulary);
		oml.addVocabularyExtension(vocabulary, OWL, null);
		object.getEClassifiers().stream().forEach(c -> visitor.doSwitch(c));
		return vocabulary;
	}

}
