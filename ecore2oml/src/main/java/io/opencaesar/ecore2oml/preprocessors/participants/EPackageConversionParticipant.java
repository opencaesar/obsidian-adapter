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
package io.opencaesar.ecore2oml.preprocessors.participants;

import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;

import io.opencaesar.ecore2oml.preprocessors.CollectionKind;

public class EPackageConversionParticipant extends ConversionParticipant {
	
	private Logger LOGGER = LogManager.getLogger(EPackageConversionParticipant.class);

	@Override
	public void handle(EObject element, Map<CollectionKind, Object> collections) {
		LOGGER.debug("Hanlding : " + element);
		
	}

	@Override
	public void postProcess(Map<CollectionKind, Object> collections) {
		LOGGER.debug("Post Processing");
		
	}

	

	

}
