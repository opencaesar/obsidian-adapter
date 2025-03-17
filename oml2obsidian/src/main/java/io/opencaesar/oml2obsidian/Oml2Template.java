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

import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.SemanticProperty;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlSearch;

class Oml2Template {
	
	final ResourceSet inputResourceSet;
	final Aspect thingAspect;

	public Oml2Template(ResourceSet inputResourceSet) {
		this.inputResourceSet = inputResourceSet;
		this.thingAspect = (Aspect) OmlRead.getMemberByIri(inputResourceSet, "http://www.w3.org/2002/07/owl#Thing");
	}
	
	public String generate(Entity entity, Set<Resource> scope) {
		var s = new StringBuffer();
		s.append("---\n");
		s.append("tags:\n");
		s.append("  - "+entity.getOntology().getPrefix() + "/" + entity.getName()+"\n");
		
		// collect all properties in the domain of the entity
		var properties = OmlSearch.findAllSuperTerms(entity, true, scope).stream()
			.map(j -> (Entity)j)
			.flatMap(j -> OmlSearch.findSemanticPropertiesWithDomain(j, scope).stream())
			.collect(Collectors.toList());
		
		// add all properties with no domains or with owl:Thing domain
		properties.addAll(OmlRead.getOntologies(inputResourceSet).stream()
			.flatMap(i -> OmlRead.getMembers(i).stream())
			.filter(i -> i instanceof SemanticProperty)
			.map(i -> (SemanticProperty)i)
			.filter(i -> {
				var domains = OmlSearch.findDomains(i, scope);
				return domains.isEmpty() || domains.contains(thingAspect);
			})
			.toList());
		
		// write relation entity source and target
		if (entity instanceof RelationEntity) {
			s.append("hasSource:\n");
			s.append("hasTarget:\n");
		}

		// write property fields
		for (var property: properties) {
			s.append(property.getName()+":\n");
		}

		s.append("---");
		return s.toString();
	}
}