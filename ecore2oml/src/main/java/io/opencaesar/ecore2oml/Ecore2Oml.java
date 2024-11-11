/**
 * 
 * Copyright 2022 Modelware Solutions LLC and CEA-LIST.
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
package io.opencaesar.ecore2oml;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreSwitch;

import io.opencaesar.ecore2oml.Ecore2OmlApp.Options;
import io.opencaesar.ecore2oml.EcoreUtilities.ERelationBuilder;
import io.opencaesar.ecore2oml.EcoreUtilities.ERelationBuilder.ERelation;
import io.opencaesar.oml.CardinalityRestrictionKind;
import io.opencaesar.oml.ConceptInstance;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.Element;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.Property;
import io.opencaesar.oml.Relation;
import io.opencaesar.oml.ReverseRelation;
import io.opencaesar.oml.Scalar;
import io.opencaesar.oml.UnreifiedRelation;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlBuilder;

class Ecore2Oml extends EcoreSwitch<EObject> {

	private static final String XSD_NS = "http://www.w3.org/2001/XMLSchema#";
	private static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	private static final String RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#";
	
	private final Resource inputResource;
	private final OmlBuilder oml;
	private final Options options;

	private final Map<Object, Element> ecore2Oml;
	private final ERelationBuilder eRelationBuilder;
	
	
	public Ecore2Oml(Resource inputResource, OmlBuilder oml, Options options) {
		this.inputResource = inputResource;
		this.oml = oml;
		this.options = options;
		
		this.ecore2Oml = new HashMap<>();
		this.eRelationBuilder = new ERelationBuilder();
	}

	public void run() {
		// iterate over the model contents to convert it
		for (EObject root : inputResource.getContents()) {
			doSwitch(root);
		}
	}
	
	@Override
	public EObject caseEPackage(EPackage object) {
		final String namespace = getNamespace(object);
		final String pefix = getPrefix(object);
		final URI uri = getUri(namespace);
		
		// create the vocabulary
		Vocabulary vocabulary = oml.createVocabulary(uri, namespace, pefix);
		ecore2Oml.put(object, vocabulary);

		// if the root package, build eRelations
		if (object.getESuperPackage() == null) {
			eRelationBuilder.build(inputResource);
		}
		
		// add annotations
		addOntologyAnnotations(vocabulary, object);
		
		// iterate over sub packages
		object.getESubpackages().stream().forEach(p -> doSwitch(p));

		// iterate over classifiers
		object.getEClassifiers().stream().forEach(c -> doSwitch(c));
		
		return vocabulary;
	}

	@Override
	public EObject caseEClass(EClass object) {
		// special case: ignore the XML Document Root class
		if (object.getEReferences().stream().anyMatch(i -> i.getName().equals("xMLNSPrefixMap"))) {
			return null;
		}

		// get the vocabulary
		final Vocabulary vocabulary = (Vocabulary) ecore2Oml.get(object.getEPackage()); 

		// create the entity
		final Entity entity;
		if (EcoreUtilities.isAbstract(object)) {
			entity = oml.addAspect(vocabulary, object.getName());
		} else {
			entity = oml.addConcept(vocabulary, object.getName());
		}
		ecore2Oml.put(object, entity);

		// add annotations
		addMemberAnnotations(vocabulary, entity, object);
		
		// add specializations
		object.getESuperTypes().stream().filter(i -> !i.eIsProxy()).forEach( sup -> {
			oml.addSpecializationAxiom(vocabulary, entity.getIri(), getIri(sup));
		});
		
		// add properties
		object.getEStructuralFeatures().stream().forEach(f -> doSwitch(f));
		
		return entity;
	}

	@Override
	public EObject caseEDataType(EDataType object) {
		// get the vocabulary
		final Vocabulary vocabulary = (Vocabulary) ecore2Oml.get(object.getEPackage()); 

		// create scalar
		final Scalar scalar = oml.addScalar(vocabulary, object.getName());
		ecore2Oml.put(object, scalar);

		// add annotations
		addMemberAnnotations(vocabulary, scalar, object);

		// add equivalences
		//TODO: map facet restrictions
		oml.addScalarEquivalenceAxiom(vocabulary, scalar.getIri(), getBaseIri(object), null, null, null, null, null, null, null, null, null);

		// Handle the case of scalar that is a union of enums (generated by XML adapter)
		if (object.getInstanceClassName().equals("org.eclipse.emf.common.util.Enumerator")) {
			final String memberTypes = EcoreUtilities.getExtendedMetadataProperty(object, "memberTypes");
			if (memberTypes != null) {
				final List<Literal> literals = Arrays.asList(memberTypes.split(" ")).stream()
						.map(t -> (EDataType) object.getEPackage().getEClassifier(t))
						.filter(t -> t instanceof EEnum)
						.map(t -> (EEnum)t)
						.flatMap(t -> t.getELiterals().stream())
						.map(l -> (Literal)doSwitch(l))
						.collect(Collectors.toList());
				if (!literals.isEmpty()) {
					oml.addLiteralEnumerationAxiom(vocabulary, scalar.getIri(), literals.toArray(new Literal[0]));
				}
			}
		}
		
		return scalar;
	}

	@Override
	public EObject caseEEnum(EEnum object) {
		// get the vocabulary
		final Vocabulary vocabulary = (Vocabulary) ecore2Oml.get(object.getEPackage()); 

		// create scalar
		final Scalar scalar = oml.addScalar(vocabulary, object.getName());
		ecore2Oml.put(object, scalar);

		// add annotations
		addMemberAnnotations(vocabulary, scalar, object);

		// add literals
		final Literal[] literals = object.getELiterals().stream().map(i -> doSwitch(i)).toArray(Literal[]::new);
		oml.addLiteralEnumerationAxiom(vocabulary, scalar.getIri(), literals);
		
		return scalar;
	}
	
	@Override
	public EObject caseEEnumLiteral(EEnumLiteral object) {
		// get the vocabulary
		final Vocabulary vocabulary = (Vocabulary) ecore2Oml.get(object.getEEnum().getEPackage()); 
		
		// create literal
		return oml.createQuotedLiteral(vocabulary, object.getLiteral(), null, null);
	}

	@Override
	public EObject caseEAttribute(EAttribute object) {
		// special case: ignore a feature map entry
		if (object.getEAttributeType().getName().equals("EFeatureMapEntry")) {
			return null;
		}
		
		// get the vocabulary
		final Vocabulary vocabulary = (Vocabulary) ecore2Oml.get(object.getEContainingClass().getEPackage()); 
		
		// create property
		final String name = getName(object);
		final String domainIri = getIri(object.getEContainingClass());
		final String rangeIri = getIri(object.getEAttributeType());
		final boolean isFunctional = object.getUpperBound() == 1;
		Property property = oml.addScalarProperty(vocabulary, name, Collections.singletonList(domainIri), Collections.singletonList(rangeIri), isFunctional);
		ecore2Oml.put(object, property);

		// add annotations
		addMemberAnnotations(vocabulary, property, object);

		// add isOrdered annotation
		if (object.isOrdered()) {
			getMinimalEcoreVocabulary();
			oml.addAnnotation(vocabulary, property.getIri(), getMappedIri(EcoreUtilities.getEcoreProperty("isOrdered")), new Literal[0]);
		}

		// add specializations
		EcoreUtilities.getSuperEAttributes(object).forEach( sup -> oml.addSpecializationAxiom(vocabulary, property.getIri(), getIri(sup)));
		
		// add restrictions
		if (object.getLowerBound() > 0 || object.getUpperBound() > 1) {
			if (object.getLowerBound() == object.getUpperBound()) {
				oml.addPropertyCardinalityRestrictionAxiom(vocabulary, domainIri, property.getIri(), CardinalityRestrictionKind.EXACTLY, object.getLowerBound(), null);
			} else {
				if (object.getLowerBound() > 0) {
					oml.addPropertyCardinalityRestrictionAxiom(vocabulary, domainIri, property.getIri(), CardinalityRestrictionKind.MIN, object.getLowerBound(), null);
				}
				if (object.getUpperBound() > 1) {
					oml.addPropertyCardinalityRestrictionAxiom(vocabulary, domainIri, property.getIri(), CardinalityRestrictionKind.MAX, object.getUpperBound(), null);
				}
			}
		}

		return property;
	}

	@Override
	public EObject caseEReference(EReference object) {
		// ignore if we already processed
		if (ecore2Oml.containsKey(object)) {
			return null;
		}

		// get the vocabulary
		final Vocabulary vocabulary = (Vocabulary) ecore2Oml.get(object.getEContainingClass().getEPackage()); 

		// get the calculated eRelation
		final ERelation eRelation = eRelationBuilder.get(object);
		
		// get the two references
		final EReference reference = eRelation.forward;
		final EReference opposite = eRelation.reverse;

		// gather characteristics
		final String sourceIri = getIri((reference != null) ? reference.getEContainingClass() : opposite.getEReferenceType());
		final String targetIri = getIri((reference != null) ? reference.getEReferenceType() : opposite.getEContainingClass());
		final boolean isFunctional = (reference != null) ? reference.getUpperBound()==1 : false;
		final boolean isInverseFunctional = (opposite != null) ? opposite.getLowerBound()==1 : false;

		// create unreified relation
		UnreifiedRelation relation = null;
		if (reference != null) {
			relation = oml.addUnreifiedRelation(
				vocabulary, getName(reference), 
				Collections.singletonList(sourceIri), 
				Collections.singletonList(targetIri), 
				isFunctional, isInverseFunctional,
				false, false, false, false, false);
			ecore2Oml.put(reference, relation);
			
			// add annotations
			addMemberAnnotations(vocabulary, relation, reference);
			
			// add isOrdered annotation
			if (reference.isOrdered()) {
				getMinimalEcoreVocabulary();
				oml.addAnnotation(vocabulary, relation.getIri(), getMappedIri(EcoreUtilities.getEcoreProperty("isOrdered")), new Literal[0]);
			}

			// create reverse
			if (opposite != null) {
				final ReverseRelation reverse = oml.addReverseRelation(relation, getName(opposite));
				ecore2Oml.put(opposite, reverse);

				// add annotations
				addMemberAnnotations(vocabulary, reverse, opposite);
			}
		} else if (opposite != null) {
			relation = oml.addUnreifiedRelation(
					vocabulary, getName(opposite), 
					Collections.singletonList(targetIri),
					Collections.singletonList(sourceIri), 
					isInverseFunctional, isFunctional,
					false, false, false, false, false);
			ecore2Oml.put(opposite, relation);
				
			// add annotations
			addMemberAnnotations(vocabulary, relation, opposite);

			// add isOrdered annotation
			if (opposite.isOrdered()) {
				getMinimalEcoreVocabulary();
				oml.addAnnotation(vocabulary, relation.getIri(), getMappedIri(EcoreUtilities.getEcoreProperty("isOrdered")), new Literal[0]);
			}
		}
		
		// add specializations
		for (ERelation superERelation : eRelation.supers) {
			final EReference superReference = superERelation.forward;
			final EReference superOpposite = superERelation.reverse;
			if (reference != null && superReference != null) {
				Relation subRelation = relation;
				oml.addSpecializationAxiom(vocabulary, subRelation.getIri(), getIri(superReference));
			} else if (opposite != null && superOpposite != null) {
				Relation subRelation = (relation.getReverseRelation() != null) ? relation.getReverseRelation() : relation;
				oml.addSpecializationAxiom(vocabulary, subRelation.getIri(), getIri(superOpposite));
			} else {
				assert false : "Cannot match ends of association"+eRelation+" with those of super association "+superERelation;
			}
		}
		
		// add restrictions on source
		if (reference != null && (reference.getLowerBound() > 0 || reference.getUpperBound() > 1)) {
			if (reference.getLowerBound() == reference.getUpperBound()) {
				oml.addPropertyCardinalityRestrictionAxiom(vocabulary, sourceIri, relation.getIri(), CardinalityRestrictionKind.EXACTLY, reference.getLowerBound(), null);
			} else {
				if (reference.getLowerBound() > 0) {
					oml.addPropertyCardinalityRestrictionAxiom(vocabulary, sourceIri, relation.getIri(), CardinalityRestrictionKind.MIN, reference.getLowerBound(), null);
				}
				if (reference.getUpperBound() > 1) {
					oml.addPropertyCardinalityRestrictionAxiom(vocabulary, sourceIri, relation.getIri(), CardinalityRestrictionKind.MAX, reference.getUpperBound(), null);
				}
			}
		}
		
		// add restrictions on target
		if (relation.getReverseRelation() != null && (opposite.getLowerBound() > 0 || opposite.getUpperBound() > 1))	 {
			if (opposite.getLowerBound() == opposite.getUpperBound()) {
				oml.addPropertyCardinalityRestrictionAxiom(vocabulary, targetIri, relation.getReverseRelation().getIri(), CardinalityRestrictionKind.EXACTLY, opposite.getLowerBound(), null);
			} else {
				if (opposite.getLowerBound() > 0) {
					oml.addPropertyCardinalityRestrictionAxiom(vocabulary, targetIri, relation.getReverseRelation().getIri(), CardinalityRestrictionKind.MIN, opposite.getLowerBound(), null);
				}
				if (opposite.getUpperBound() > 1) {
					oml.addPropertyCardinalityRestrictionAxiom(vocabulary, targetIri, relation.getReverseRelation().getIri(), CardinalityRestrictionKind.MAX, opposite.getUpperBound(), null);
				}
			}
		}

		return relation;
	}
	
	@Override
	public EObject defaultCase(EObject object) {
		// ignore any Ecore object since we already handle them
		if (object.eClass().getEPackage() == EcorePackage.eINSTANCE) {
			return null;
		}
		// create concept instance
		return createConceptInstance(object);
	}

	protected ConceptInstance createConceptInstance(EObject object) {
		// get the description (assumption is that there is a single one)
		Description description = (Description) getDescription();
		
		// create concept instance
		ConceptInstance instance = oml.addConceptInstance(description, getName(object));
		ecore2Oml.put(object, instance);
		
		// add annotations
		addMemberAnnotations(description, instance, object);
		
		// add type
		oml.addTypeAssertion(description, instance.getIri(), getIri(object.eClass()));
		
		// add nested contents
		object.eContents().forEach(i -> doSwitch(i));
		
		return instance;
	}
	
	protected Description getDescription() {
		var description = (Description) ecore2Oml.get("description");
		if (description == null) {
			final var inputUri = inputResource.getURI();
			final var relativePath = inputUri.authority()+inputUri.path()+"."+options.outputFileExtension;
			final var uri = URI.createFileURI(options.outputFolderPath+File.separator+relativePath);
			
			final var iri = "http://" + relativePath;
			final var namespace = iri+"#";
			final var pefix = URI.createURI(iri).lastSegment();
				
			// create description
			description = oml.createDescription(uri, namespace, pefix);
			ecore2Oml.put("description", description);
		}
		return description;
	}

	protected Vocabulary getMinimalEcoreVocabulary( ) {
		var vocabulary = (Vocabulary) ecore2Oml.get("ecoreVocabulary");
		if (vocabulary == null) {
			final String namespace = EcoreUtilities.getEcoreNamespace();
			vocabulary = oml.createVocabulary(getUri(namespace), namespace, "ecore");
			ecore2Oml.put("ecoreVocabulary", vocabulary);
			
			oml.addAnnotationProperty(vocabulary, "isOrdered");
			oml.addScalarProperty(vocabulary, "order", Collections.singletonList(RDF_NS+"Statement"),  Collections.singletonList(XSD_NS+"int"), false);
		}
		return vocabulary;
	}

	private void addOntologyAnnotations(Ontology ontology, ENamedElement object) {
		// add rdfs:label
		var name = object.getName();
		var literal = oml.createQuotedLiteral(ontology, name, null, null);
		oml.addAnnotation(ontology, RDFS_NS+"label", literal);

		// add rfds:comment
		var documentation = EcoreUtilities.getGenModelProperty(object, "documentation");
		if (documentation != null) {
			literal = oml.createQuotedLiteral(ontology, documentation.trim(), null, null);
			oml.addAnnotation(ontology, RDFS_NS+"comment", literal);
		}
	}

	private void addMemberAnnotations(Ontology ontology, Member member, ENamedElement object) {
		// add rdfs:label
		var name = object.getName();
		var literal = oml.createQuotedLiteral(ontology, name, null, null);
		oml.addAnnotation(ontology, member.getIri(), RDFS_NS+"label", literal);

		// add rfds:comment
		var documentation = EcoreUtilities.getGenModelProperty(object, "documentation");
		if (documentation != null) {
			literal = oml.createQuotedLiteral(ontology, documentation.trim(), null, null);
			oml.addAnnotation(ontology, member.getIri(), RDFS_NS+"comment", literal);
		}
	}

	protected void addMemberAnnotations(Ontology ontology, Member member, EObject object) {
		// add rdfs:label
		var name = object.eResource().getURIFragment(object);
		var literal = oml.createQuotedLiteral(ontology, name, null, null);
		oml.addAnnotation(ontology, member.getIri(), RDFS_NS+"label", literal);
	}
	
	//----------
	// Utilities
	// ---------

	private String getName(EObject object) {
		return object.eResource().getURIFragment(object);
	}
	
	private String getName(EStructuralFeature object) {
		return object.getEContainingClass().getName() + "_" + object.getName();
	}

	private String getIri(EStructuralFeature object) {
		return getNamespace(object.getEContainingClass().getEPackage()) + getName(object);
	}
	
	public String getBaseIri(EDataType object) {
		final String baseType = EcoreUtilities.getExtendedMetadataProperty(object, "baseType");
		
		final String baseIri;
		if (baseType == null) {
			baseIri = RDFS_NS+"Literal"; 
		} else if (baseType.contains("#") || baseType.contains("/")) {
			baseIri = baseType;
		} else {
			baseIri = getNamespace(object.getEPackage())+baseType;
		}
		
		return getMappedIri(baseIri);
	}

	private String getIri(EClassifier object) {
		var iri = getNamespace(object.getEPackage())+object.getName();
		return getMappedIri(iri);
	}
	
	private String getNamespace(EPackage object) {
		String iri = object.getNsURI();
		if (!iri.endsWith("#") && !iri.endsWith("/")) {
			iri = iri+"#";
		}
		return getMappedIri(iri);
	}

	private String getPrefix(EPackage object) {
		return object.getNsPrefix();
	}

	private URI getUri(String namespace) {
		var uri = URI.createURI(namespace);
		var relativePath = uri.authority()+uri.path()+"."+options.outputFileExtension;
		return URI.createFileURI(options.outputFolderPath+File.separator+relativePath);
	}

	private String getMappedIri(String iri) {
		for( var e : options.namespaceMap2.entrySet()) {
			if (iri.startsWith(e.getKey())) {
				return e.getValue() + iri.substring(e.getKey().length());
			}
		}
		return iri;
	}

}