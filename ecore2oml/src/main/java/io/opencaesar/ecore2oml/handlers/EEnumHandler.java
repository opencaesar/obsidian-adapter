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

import static io.opencaesar.ecore2oml.util.Util.getMappedName;

import java.util.Map;

import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EObject;

import io.opencaesar.ecore2oml.Ecore2Oml;
import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlBuilder;

public class EEnumHandler implements ConversionHandler {

	@Override
	public EObject doConvert(EObject eObject, Vocabulary vocabulary, OmlBuilder oml,
			Map<CollectionKind, Object> collections,Ecore2Oml visitor) {
		EEnum object = (EEnum)eObject;
		final String name = getMappedName(object);
		final Literal[] literals = object.getELiterals().stream().map(i -> visitor.doSwitch(i)).toArray(Literal[]::new);
		return oml.addEnumeratedScalar(vocabulary, name, literals);
	}

}
