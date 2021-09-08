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

import java.util.Map;

import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;

import io.opencaesar.ecore2oml.Ecore2Oml;
import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.ecore2oml.util.Util;
import io.opencaesar.oml.FacetedScalar;
import io.opencaesar.oml.SeparatorKind;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlBuilder;
import io.opencaesar.oml.util.OmlConstants;

public class EDataTypeHandler implements ConversionHandler{

	@Override
	public EObject doConvert(EObject eObject, Vocabulary vocabulary, OmlBuilder oml, Map<CollectionKind, Object> collections,Ecore2Oml visitor) {
		EDataType object = (EDataType)eObject;
		final String name = Util.getMappedName(object);
		final FacetedScalar scalar = oml.addFacetedScalar(vocabulary, name, null, null, null, null, null, null, null, null, null);
		String base = "";
		switch (name) {
			case "Boolean":
				base = "boolean";
				break;
			case "Integer":
				base = "integer";
				break;
			case "Real":
				base = "double";
				break;
			case "UnlimitedNatural":
				base = "integer";
				break;
			default:
				base = "string";
				break;
		}
		String baseIRI = OmlConstants.XSD_NS + base;
		oml.addSpecializationAxiom(vocabulary, scalar.getIri(), baseIRI);
		Util.addVocabularyExtensionIfNeeded(vocabulary, OmlConstants.XSD_IRI, SeparatorKind.HASH, OmlConstants.XSD_PREFIX, oml);
		return scalar;
	}

}
