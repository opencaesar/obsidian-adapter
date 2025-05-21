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

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import io.opencaesar.oml.AnnotationProperty;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.IdentifiedElement;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Property;
import io.opencaesar.oml.PropertyRangeRestrictionAxiom;
import io.opencaesar.oml.RangeRestrictionKind;
import io.opencaesar.oml.Relation;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.Scalar;
import io.opencaesar.oml.ScalarProperty;
import io.opencaesar.oml.SemanticProperty;
import io.opencaesar.oml.Type;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlSearch;

class Oml2Class {

	final ResourceSet inputResourceSet;
	final String templatePath;
	final Scalar booleanScalar;
	final Scalar dateTimeScalar;
	final Scalar realScalar;
	final AnnotationProperty labelProperty;
	final AnnotationProperty commentProperty;
	final AnnotationProperty hasIconProperty;
	
	public Oml2Class(ResourceSet inputResourceSet, String templatePath) {
		this.inputResourceSet = inputResourceSet;
		this.templatePath = templatePath;
		
		this.booleanScalar = (Scalar) OmlRead.getMemberByIri(inputResourceSet, "http://www.w3.org/2001/XMLSchema#boolean");
		this.dateTimeScalar = (Scalar) OmlRead.getMemberByIri(inputResourceSet, "http://www.w3.org/2001/XMLSchema#dateTime");
		this.realScalar = (Scalar) OmlRead.getMemberByIri(inputResourceSet, "http://www.w3.org/2002/07/owl#real");
		this.labelProperty = (AnnotationProperty) OmlRead.getMemberByAbbreviatedIri(inputResourceSet, "rdfs:label");
		this.commentProperty = (AnnotationProperty) OmlRead.getMemberByAbbreviatedIri(inputResourceSet, "rdfs:comment");
	
		this.hasIconProperty = (AnnotationProperty) OmlRead.getMemberByIri(inputResourceSet, "http://opencaesar.io/obsidian#hasIcon");
	}
	
	public String generateFrontMatter(Entity entity, List<SemanticProperty> properties, Set<Resource> scope) {
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
				
		if (properties.size()>0) {
			s.append("fields:\n");
		}
		
		// generate fields
		for (var property: properties) {
			if (property instanceof ScalarProperty) {
				s.append("- name: "+property.getName()+"\n");
				var range = getMostSpecificPropertyRanges(entity, property, scope).iterator().next();
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
				s.append("  id: f"+property.getName().hashCode()+"\n");
			} else if (property instanceof Relation) {
				var ranges = getMostSpecificPropertyRanges(entity, property, scope);
				if (ranges.size()>0) {
					s.append(generateRelationField(property.getName(), property.getName().hashCode(), property.isFunctional(), ranges));
				}
			}
		}
		
		// relation entities additionally have 'from' and 'to' properties
		if (entity instanceof RelationEntity) {
			// from property
			var re = (RelationEntity)entity;
			var sources = getMostSpecificTypes(new HashSet<>(OmlSearch.findSources(re, scope)), scope);
			if (sources.size()>0) {
				s.append(generateRelationField("hasSource", entity.getName().hashCode()+1, ((RelationEntity) entity).isFunctional(), sources));
			}
			// to property
			var targets = getMostSpecificTypes(new HashSet<>(OmlSearch.findTargets(re, scope)), scope);
			if (targets.size()>0) {
				s.append(generateRelationField("hasTarget", entity.getName().hashCode()+2, ((RelationEntity) entity).isFunctional(), targets));
			}
		}
		
		s.append("---\n");
		
		return s.toString();
	}

	public String generateBody(Entity entity, List<SemanticProperty> properties, Set<Resource> scope) {
		var s = new StringBuffer();

		// write property fields
        var seen = new HashMap<String, Property>();
		properties.stream().sorted(Comparator.comparing(p -> p.getName())).forEach(property -> {
			var name = property.getName();
			if (!seen.containsKey(name)) {
				seen.put(name, property);
				if (labelProperty != null) {
					var label = OmlRead.getAnnotationLiteralValue(property, labelProperty);
					if (label != null) {
						s.append("# "+label.getStringValue()+"\n");
					} else {
						s.append("# "+name+"\n");
					}
				}
				if (commentProperty != null) {
					var comment = OmlRead.getAnnotationLiteralValue(property, commentProperty);
					if (comment != null) {
						s.append(comment.getStringValue()+"\n\n");
					} else {
						s.append("\n\n");
					}
				}
			} else {
				throw new RuntimeException("Property "+seen.get(name).getAbbreviatedIri()
						+" has the same name as "+property.getAbbreviatedIri()
						+" in the context of entity "+entity.getAbbreviatedIri());
			}
		});

		// write relation entity source and target
		if (entity instanceof RelationEntity) {
			s.append("# Sources\n");
			s.append("The sources of this relation\n\n");
			s.append("# Targets\n");
			s.append("The targets of this relation\n\n");
			s.append("\n\n");
		}
		
		return s.toString();
	}
	
	private String generateRelationField(String name, int hashCode, boolean functional, Set<Type> ranges) {
		StringBuffer s = new StringBuffer();
		s.append("- name: "+name+"\n");
		if (functional) {
			s.append("  type: File\n");
		} else {
			s.append("  type: MultiFile\n");
		}
		var types = ranges.stream().distinct().map(r -> "#"+r.getOntology().getPrefix()+"/"+r.getName()).sorted().collect(Collectors.joining(" or "));
		s.append("  options:\n");
		s.append("    dvQueryString: \"dv.pages('"+types+" and !\\\""+templatePath+"\\\"')\"\n");
		s.append("  path: \"\"\n");
		s.append("  id: f"+hashCode+"\n");
		return s.toString();
	}
	
	private Set<Type> getMostSpecificPropertyRanges(Entity entity, SemanticProperty property, Set<Resource> scope) {
		var ranges = OmlSearch.findAllSuperTerms(entity, true, scope).stream()
				.map(j -> (Entity)j)
				.flatMap(j -> OmlSearch.findPropertyRestrictionAxioms(j, scope).stream())
				.filter(j -> j instanceof PropertyRangeRestrictionAxiom) 
				.map(j -> (PropertyRangeRestrictionAxiom)j)
				.filter(j -> j.getProperty() == property)
				.filter(j -> (j.getKind() == RangeRestrictionKind.ALL) || (j.getKind() == RangeRestrictionKind.SOME && j.getProperty().isFunctional()))
				.map(j -> j.getRange())
				.collect(Collectors.toSet());
		if (ranges.size() == 0) {
			ranges = OmlSearch.findRanges(property, scope).stream()
				.map(j -> j)
				.collect(Collectors.toSet());
		}
		return getMostSpecificTypes(ranges, scope);
	}

	private Set<Type> getMostSpecificTypes(Set<Type> types, Set<Resource> scope) {
		final var mostSpecificTypes = new HashSet<>(types);
        for (var r : types) {
        	mostSpecificTypes.removeAll(OmlSearch.findAllSuperTerms(r, false, scope));
        }
		return mostSpecificTypes.stream().flatMap(j -> OmlSearch.findAllSubTerms(j, true, scope).stream())
			.filter(j -> j instanceof Type)
			.map(j -> (Type)j)
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