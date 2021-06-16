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
import io.opencaesar.oml.util.OmlBuilder;

public class EPackageHandler implements ConversionHandler {
	
	private final URI outputResourceURI;
	
	public EPackageHandler(URI outputRURI) {
		// TODO Auto-generated constructor stub
		this.outputResourceURI = outputRURI;
	}

	@Override
	public EObject doConvert(EObject eObject, Vocabulary vocabulary, OmlBuilder oml,
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
