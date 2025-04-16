/**
 * 
 * Copyright 2019-2021 California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
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
package io.opencaesar.oml2obsidian;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.Property;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.SemanticProperty;
import io.opencaesar.oml.util.OmlRead;

class Oml2Template {
	
	final ResourceSet inputResourceSet;
	final Aspect thingAspect;

	public Oml2Template(ResourceSet inputResourceSet) {
		this.inputResourceSet = inputResourceSet;
		this.thingAspect = (Aspect) OmlRead.getMemberByIri(inputResourceSet, "http://www.w3.org/2002/07/owl#Thing");
	}
	
	public String generate(Entity entity, List<SemanticProperty> properties, Set<Resource> scope) {
		var s = new StringBuffer();
		s.append("---\n");
		s.append("tags:\n");
		s.append("  - "+entity.getOntology().getPrefix() + "/" + entity.getName()+"\n");
				
		// write property fields
        var seen = new HashMap<String, Property>();
		for (var property: properties) {
			var name = property.getName();
			if (!seen.containsKey(name)) {
				seen.put(name, property);
				var functional = property.isFunctional();
				if (functional) {
					s.append(name+":\n");
				} else {
					s.append(name+": []\n");
				}
			} else {
				throw new RuntimeException("Property "+seen.get(name).getAbbreviatedIri()
						+" has the same name as "+property.getAbbreviatedIri()
						+" in the context of entity "+entity.getAbbreviatedIri());
			}
		}

		// write relation entity source and target
		if (entity instanceof RelationEntity) {
			s.append("hasSource: []\n");
			s.append("hasTarget: []\n");
		}

		s.append("---\n");
		return s.toString();
	}
}