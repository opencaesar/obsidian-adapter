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

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import io.opencaesar.oml.AnnotationProperty;
import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.Concept;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.IdentifiedElement;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.PropertyRangeRestrictionAxiom;
import io.opencaesar.oml.RangeRestrictionKind;
import io.opencaesar.oml.Relation;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.Scalar;
import io.opencaesar.oml.ScalarProperty;
import io.opencaesar.oml.SemanticProperty;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlSearch;

class Oml2Class {

	final ResourceSet inputResourceSet;
	final String templatePath;
	final Aspect thingAspect;
	final Scalar booleanScalar;
	final Scalar dateTimeScalar;
	final Scalar realScalar;
	final AnnotationProperty hasIconProperty;
	
	public Oml2Class(ResourceSet inputResourceSet, String templatePath) {
		this.inputResourceSet = inputResourceSet;
		this.templatePath = templatePath;

		this.thingAspect = (Aspect) OmlRead.getMemberByIri(inputResourceSet, "http://www.w3.org/2002/07/owl#Thing");
		
		this.booleanScalar = (Scalar) OmlRead.getMemberByIri(inputResourceSet, "http://www.w3.org/2001/XMLSchema#boolean");
		this.dateTimeScalar = (Scalar) OmlRead.getMemberByIri(inputResourceSet, "http://www.w3.org/2001/XMLSchema#dateTime");
		this.realScalar = (Scalar) OmlRead.getMemberByIri(inputResourceSet, "http://www.w3.org/2002/07/owl#real");
	
		this.hasIconProperty = (AnnotationProperty) OmlRead.getMemberByIri(inputResourceSet, "http://opencaesar.io/obsidian#hasIcon");
	}
	
	public String generate(Entity entity, Set<Resource> scope) {
		var s = new StringBuffer();
		s.append("---\n");
		
		// get annotations
		var icon = findAnnotationValue(entity, hasIconProperty, scope);
		
		// add class metadata
		s.append("version: \"2.1\"\n");
		s.append("limit: 20\n");
		s.append("mapWithTag: true\n");
		s.append("icon: "+icon+"\n");
		s.append("tagNames:\n"); 
		s.append("filesPaths:\n"); 
		s.append("bookmarksGroups:\n"); 
		s.append("excludes:\n"); 
		s.append("extends:\n"); 
		s.append("savedViews: []\n");
		s.append("favoriteView:\n"); 
		s.append("fieldsOrder: []\n");
		
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
		
		if (properties.size()>0) {
			s.append("fields:\n");
		}
		
		// generate fields
		for (var property: properties) {
			if (property instanceof ScalarProperty) {
				s.append("- name: "+property.getName()+"\n");
				var range = OmlSearch.findRanges(property, scope).iterator().next();
				if (OmlSearch.findIsSubTermOf(range, booleanScalar, scope)) {
					s.append("  type: Boolean\n");
				} else if (OmlSearch.findIsSubTermOf(range, realScalar, scope)) {
					s.append("  type: Number\n");
				} else if (OmlSearch.findIsSubTermOf(range, dateTimeScalar, scope)) {
					s.append("  type: DateTime\n");
				} else if (OmlSearch.findIsEnumeratedScalar((Scalar)range, scope)) {
					s.append("  type: Select\n");
					s.append("  options:\n");
					s.append("    sourceType: ValuesList\n");
					s.append("    valuesList:\n");
					int k = 1;
					for (var literal : OmlSearch.findEnumerationLiterals((Scalar)range, scope)) {
						s.append("      \""+(k++)+"\": "+literal.getLexicalValue()+"\n");
					}
				} else {
					s.append("  type: Input\n");
				}
				s.append("  path: \"\"\n");
				s.append("  id: f"+property.hashCode()+"\n");
			} else if (property instanceof Relation) {
				var targets = getMostSpecificRelationTargets(entity, (Relation)property, scope);
				if (targets.size()>0) {
					s.append(generateRelationField(property.getName(), property.hashCode(), property.isFunctional(), targets));
				}
			}
		}
		
		// relation entities additionally have 'from' and 'to' properties
		if (entity instanceof RelationEntity) {
			// from property
			var re = (RelationEntity)entity;
			var sources = getMostSpecificTypes(OmlSearch.findSources(re, scope), scope);
			if (sources.size()>0) {
				s.append(generateRelationField("hasSource", entity.hashCode()+1, ((RelationEntity) entity).isFunctional(), sources));
			}
			// to property
			var targets = getMostSpecificTypes(OmlSearch.findTargets(re, scope), scope);
			if (targets.size()>0) {
				s.append(generateRelationField("hasTarget", entity.hashCode()+2, ((RelationEntity) entity).isFunctional(), targets));
			}
		}
		
		s.append("---");
		return s.toString();
	}

	private String generateRelationField(String name, int hashCode, boolean functional, Set<Entity> ranges) {
		StringBuffer s = new StringBuffer();
		s.append("- name: "+name+"\n");
		if (functional) {
			s.append("  type: File\n");
		} else {
			s.append("  type: MultiFile\n");
		}
		var types = ranges.stream().distinct().map(r -> "#"+r.getOntology().getPrefix()+"/"+r.getName()).collect(Collectors.joining(" or "));
		s.append("  options:\n");
		s.append("    dvQueryString: \"dv.pages('"+types+" and !\\\""+templatePath+"\\\"')\"\n");
		s.append("  path: \"\"\n");
		s.append("  id: f"+hashCode+"\n");
		return s.toString();
	}
	
	private Set<Entity> getMostSpecificRelationTargets(Entity entity, Relation relation, Set<Resource> scope) {
		var targets = OmlSearch.findAllSuperTerms(entity, true, scope).stream()
				.map(j -> (Entity)j)
				.flatMap(j -> OmlSearch.findPropertyRestrictionAxioms(j, scope).stream())
				.filter(j -> j instanceof PropertyRangeRestrictionAxiom) 
				.map(j -> (PropertyRangeRestrictionAxiom)j)
				.filter(j -> j.getProperty() == relation && j.getKind() == RangeRestrictionKind.ALL)
				.map(j -> (Entity)j.getRange())
				.collect(Collectors.toSet());
		if (targets.size() == 0) {
			targets = OmlSearch.findRanges(relation, scope).stream()
				.map(j -> (Entity)j)
				.collect(Collectors.toSet());
		}
		return getMostSpecificTypes(targets, scope);
	}

	private Set<Entity> getMostSpecificTypes(Set<Entity> types, Set<Resource> scope) {
		final var mostSpecificTypes = new HashSet<>(types);
        for (var r : types) {
        	mostSpecificTypes.removeAll(OmlSearch.findAllSuperTerms(r, false, scope));
        }
		return mostSpecificTypes.stream().flatMap(j -> OmlSearch.findAllSubTerms(j, true, scope).stream())
			.filter(j -> j instanceof Concept || j instanceof RelationEntity)
			.map(j -> (Entity)j)
			.collect(Collectors.toSet());
	}

	private String findAnnotationValue(IdentifiedElement element, AnnotationProperty property, Set<Resource> scope) {
		return OmlSearch.findAnnotationValues(element, hasIconProperty, scope).stream()
				.filter(i -> i instanceof Literal)
				.map(i -> ((Literal)i).getStringValue())
				.findFirst()
				.orElse("");
	}

}