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

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;

import io.opencaesar.ecore2oml.Ecore2Oml;
import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.ecore2oml.util.Util;
import io.opencaesar.oml.AnnotatedElement;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlBuilder;

public interface ConversionHandler {
	

	public EObject doConvert(EObject object, Vocabulary vocabulary, OmlBuilder oml,
			Map<CollectionKind, Object> collections,Ecore2Oml visitor) ;
	
	default public void postConvert(Vocabulary vocabulary, OmlBuilder oml,
			Map<CollectionKind, Object> collections,Ecore2Oml visitor) {
		// No OP
	}
	
	default public EObject convert(EObject object, Vocabulary vocabulary, OmlBuilder oml,
			Map<CollectionKind, Object> collections,Ecore2Oml visitor) {
		EObject retVal = doConvert(object, vocabulary, oml, collections, visitor);
		if (object instanceof ENamedElement && retVal instanceof AnnotatedElement) {
			if (retVal instanceof Member) {
				Util.addTitleAnnotationIfNeeded((ENamedElement)object, (Member)retVal, oml, vocabulary);
			}
			Util.addLabelAnnotation((ENamedElement)object, (AnnotatedElement)retVal,oml,vocabulary);
			Util.addDescriptionAnnotation((ENamedElement)object, (AnnotatedElement)retVal,oml,vocabulary);
		}
		return retVal;
	}
	

}
