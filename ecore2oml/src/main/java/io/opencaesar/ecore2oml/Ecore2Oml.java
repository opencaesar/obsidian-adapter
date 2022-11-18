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
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
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
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreSwitch;

import io.opencaesar.ecore2oml.AssociationBuilder.Association;
import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.Concept;
import io.opencaesar.oml.ConceptInstance;
import io.opencaesar.oml.Description;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.EnumeratedScalar;
import io.opencaesar.oml.ForwardRelation;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.Property;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.ReverseRelation;
import io.opencaesar.oml.Scalar;
import io.opencaesar.oml.SeparatorKind;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlBuilder;
import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlConstants;
import io.opencaesar.oml.util.OmlRead;

public class Ecore2Oml extends EcoreSwitch<EObject> {
	
	// Namespaces for core vocabularies
	private static final String XML_TYPE_IRI = "http://www.eclipse.org/emf/2003/XMLType";
	private static final String Ecore_IRI = "http://www.eclipse.org/emf/2002/Ecore";
	private static final String XSD_IRI = "http://www.w3.org/2001/XMLSchema";
	private static final String RDF_IRI = "http://www.w3.org/1999/02/22-rdf-syntax-ns";
	private static final String RDFS_IRI = "http://www.w3.org/2000/01/rdf-schema";
	private static final String OWL_IRI = "http://www.w3.org/2002/07/owl";
	private static final String EXTENDED_META_DATA_IRI = "http:///org/eclipse/emf/ecore/util/ExtendedMetaData";
	private static final String GEN_MODEL_IRI = "http://www.eclipse.org/emf/2002/GenModel";

	// XSD types
	private static final Map<String, String> xsdTypes = new HashMap<String, String>();
	static {
		xsdTypes.put("eenumerator", "string");		
		xsdTypes.put("ejavaclass", "string");		
		xsdTypes.put("ejavaobject", "string");		
		xsdTypes.put("estring", "string");		
		xsdTypes.put("eint", "int");		
		xsdTypes.put("eintegerobject", "int");		
		xsdTypes.put("elong", "long");		
		xsdTypes.put("eshort", "short");		
		xsdTypes.put("eboolean", "boolean");		
		xsdTypes.put("ebooleanobject", "boolean");		
		xsdTypes.put("edouble", "double");		
		xsdTypes.put("edoubleobject", "double");		
		xsdTypes.put("efloat", "float");		
		xsdTypes.put("efloatobject", "float");		
		xsdTypes.put("ebigdecimal", "decimal");		
		xsdTypes.put("ebyte", "byte");		
		xsdTypes.put("echar", "string");		
		xsdTypes.put("edate", "dateTime");		
		xsdTypes.put("eelist", "string");		
		xsdTypes.put("ediagnosticchain", "string");		
		xsdTypes.put("efeaturemap", "string");		
		xsdTypes.put("efeaturemapentry", "string");		
		xsdTypes.put("emap", "string");		
		xsdTypes.put("etreeiterator", "string");		
		xsdTypes.put("einvocationtargetexception", "string");		
		xsdTypes.put("eresource", "string");		
		xsdTypes.put("eresourceset", "string");		
		xsdTypes.put("anyuri", "anyURI");
		xsdTypes.put("base64binary", "base64Binary");
		xsdTypes.put("boolean", "boolean");
		xsdTypes.put("booleanobject", "boolean");
		xsdTypes.put("byte", "byte");
		xsdTypes.put("byteobject", "byte");
		xsdTypes.put("date", "dateTime");
		xsdTypes.put("datetime", "dateTime");
		xsdTypes.put("decimal", "decimal");
		xsdTypes.put("double", "double");
		xsdTypes.put("doubleobject", "double");
		xsdTypes.put("float", "float");
		xsdTypes.put("floatobject", "float");
		xsdTypes.put("hexbinary", "hexBinary");
		xsdTypes.put("int", "int");
		xsdTypes.put("intObject", "int");
		xsdTypes.put("integer", "integer");
		xsdTypes.put("language", "language");
		xsdTypes.put("long", "long");
		xsdTypes.put("longObject", "long");
		xsdTypes.put("ncname", "NCName");
		xsdTypes.put("nmtoken", "NMTOKEN");
		xsdTypes.put("name", "Name");
		xsdTypes.put("negativeinteger", "negativeInteger");
		xsdTypes.put("nonnegativeinteger", "nonNegativeInteger");
		xsdTypes.put("nonpositiveinteger", "nonPositiveInteger");
		xsdTypes.put("normalizedstring", "normalizedString");
		xsdTypes.put("positiveinteger", "positiveInteger");
		xsdTypes.put("short", "short");
		xsdTypes.put("shortobject", "short");
		xsdTypes.put("string", "string");
		xsdTypes.put("time", "dateTime");
		xsdTypes.put("token", "token");
		xsdTypes.put("unsignedbyte", "unsignedByte");
		xsdTypes.put("unsignedbyteObject", "unsignedByte");
		xsdTypes.put("unsignedint", "unsignedInt");
		xsdTypes.put("unsignedintobject", "unsignedInt");
		xsdTypes.put("unsignedlong", "unsignedLong");
		xsdTypes.put("unsignedshort", "unsignedShort");
		xsdTypes.put("unsignedshortobject", "unsignedShort");
		xsdTypes.put("id", "string");
		xsdTypes.put("qname", "string");
	}

	// OWL types
	private static final Map<String, String> owlTypes = new HashMap<String, String>();
	static {
		owlTypes.put("real", "real");		
		owlTypes.put("rational", "rational");		
	}

	// RDFS types
	private static final Map<String, String> rdfsTypes = new HashMap<String, String>();
	static {
		rdfsTypes.put("literal", "Literal");		
	}

	// RDF types
	private static final Map<String, String> rdfTypes = new HashMap<String, String>();
	static {
		rdfTypes.put("plainliteral", "PlainLiteral");		
		rdfTypes.put("xmlliteral", "XMLLiteral");		
	}

	private final File inputFolder;
	private final Resource inputResource;
	private final OmlCatalog catalog;
	private final OmlBuilder oml;

	private AssociationBuilder associations;
	private Set<URI> newURIs = new HashSet<>();
	private Set<URI> importedURIs = new HashSet<>();

	private Map<Object, Ontology> rootToOntology = new HashMap<>();
	
	public Ecore2Oml(File inputFolder, Resource inputResource, OmlCatalog catalog, OmlBuilder oml) {
		this.inputFolder = inputFolder;
		this.inputResource = inputResource;
		this.catalog = catalog;
		this.oml = oml;
		this.associations = new AssociationBuilder(inputResource);
	}

	public Set<URI> run() {
		// now process the model content
		for (EObject root : inputResource.getContents()) {
			doSwitch(root);
		}
		return newURIs;
	}
	
	public Set<URI> getImportedURIs() {
		return importedURIs;
	}

	private URI addImportedURI(EObject root) {
		URI uri = root.eResource().getURI();
		if (uri != null) {
			importedURIs.add(uri);
		}
		return uri;
	}

	private URI addNewURI(String iri) {
		URI uri = getMappedResourceURI(iri);
		if (uri != null) {
			newURIs.add(uri);
		}
		return uri;
	}
	
	private URI getMappedResourceURI(String iri) {
		try {
			String relativePath = catalog.resolveURI(iri)+"."+OmlConstants.OML_EXTENSION;
			return URI.createURI(relativePath);
		} catch (IOException e) {
			return null;
		}
	}
	
	@Override
	public EObject defaultCase(EObject object) {
		if (object.eContainer() == null) {
			createDescription(object);
		}
		if (object instanceof EAnnotation) {
			return null;
		}
		return caseEObject(object);
	}

	protected Description createDescription(EObject object) {
		if (rootToOntology.get(inputResource) == null) {
			File inputFile = new File(inputResource.getURI().trimFileExtension().toFileString());
			String relativePath = inputFolder.toURI().relativize(inputFile.toURI()).getPath();
			final String iri = "http://" + relativePath;
			final SeparatorKind separator = SeparatorKind.HASH;
			final String pefix = inputFile.getName();
			
			Description description = oml.createDescription(addNewURI(iri), iri, separator, pefix);
			rootToOntology.put(object.eResource(), description);
			return description;
		}
		return (Description) rootToOntology.get(inputResource);
	}

	protected EObject caseEObject(EObject object) {
		if (object.eClass().getEPackage() == EcorePackage.eINSTANCE) {
			return null;
		}
		final Description description = (Description) rootToOntology.get(object.eResource()); 
		ConceptInstance instance = oml.addConceptInstance(description, object.eResource().getURIFragment(object));
		String typeIri = getIri(description, object.eClass());
		oml.addConceptTypeAssertion(description, instance.getIri(), typeIri);
		object.eContents().forEach(i -> defaultCase(i));
		return instance;
	}
	
	@Override
	public EObject caseEPackage(EPackage object) {
		final String iri = getIri(object);
		final SeparatorKind separator = getSeparator(object);
		final String pefix = getPrefix(object);
		
		Vocabulary vocabulary = oml.createVocabulary(addNewURI(iri), iri, separator, pefix);
		rootToOntology.put(object, vocabulary);

		// build the associations first
		if (object.getESuperPackage() == null) {
			associations.build();
		}
		
		object.getEClassifiers().stream().forEach(c -> doSwitch(c));
		object.getESubpackages().stream().forEach(p -> doSwitch(p));

		return vocabulary;
	}

	@Override
	public EObject caseEEnum(EEnum object) {
		final Vocabulary vocabulary = (Vocabulary) rootToOntology.get(object.getEPackage()); 
		final String name = getEnumeratedScalarName(object);
		final Literal[] literals = object.getELiterals().stream().map(i -> doSwitch(i)).toArray(Literal[]::new);

		final EnumeratedScalar scalar = oml.addEnumeratedScalar(vocabulary, name, literals);
		addAnnotations(scalar, object);

		oml.addSpecializationAxiom(vocabulary, scalar.getIri(), getTermIriAndImportIfNeeded(vocabulary, XSD_IRI, SeparatorKind.HASH, "string", "xsd"));

		return scalar;
	}
	
	@Override
	public EObject caseEEnumLiteral(EEnumLiteral object) {
		final Vocabulary vocabulary = (Vocabulary) rootToOntology.get(object.getEEnum().getEPackage()); 
		final String name = getEnumerationLiteralName(object);

		return oml.createQuotedLiteral(vocabulary, name, null, null);
	}

	@Override
	public EObject caseEDataType(EDataType object) {
		final Vocabulary vocabulary = (Vocabulary) rootToOntology.get(object.getEPackage()); 
		final String name = getFacetedScalarName(object);

		// Ignore the case of EnumObject datatype (generated by XML adapter)
		if (object.getInstanceClassName().equals("org.eclipse.emf.common.util.Enumerator")) {
			return null;
		}

		// Get any specified base type
		String baseName = name;
		final EAnnotation annotation = object.getEAnnotation(EXTENDED_META_DATA_IRI);
		final String baseType = (annotation == null) ? null : annotation.getDetails().get("baseType");
		if (baseType != null) {
			String[] baseIriSegments = baseType.split(SeparatorKind.HASH.toString());
			if (baseIriSegments.length == 2) {
				baseName = baseIriSegments[1];
			} else {
				baseName = baseType;
			}
		}
		
		// Get the URI of the specified base type if any
		String baseIri = getStandardIri(vocabulary, baseName);
		if (baseIri == null) {
			EClassifier base = object.getEPackage().getEClassifier(baseName);
			if (base != null && base != object) {
				baseIri = vocabulary.getIri()+SeparatorKind.HASH+baseName;
			} else {
				baseIri = getTermIriAndImportIfNeeded(vocabulary, RDFS_IRI, SeparatorKind.HASH, "Literal", "rdfs");
			}				
		}

		final Scalar scalar = oml.addFacetedScalar(vocabulary, name, null, null, null, null, null, null, null, null, null);
		addAnnotations(scalar, object);
		oml.addSpecializationAxiom(vocabulary, scalar.getIri(), baseIri);

		return scalar;
	}

	@Override
	public EObject caseEClass(EClass object) {
		final Vocabulary vocabulary = (Vocabulary) rootToOntology.get(object.getEPackage()); 
		final boolean isXMLDocumentRootClass = object.getEReferences().stream().anyMatch(i -> i.getName().equals("xMLNSPrefixMap"));
		
		// ignore the XML Document Root class
		if (isXMLDocumentRootClass) {
			return null;
		}
		
		Entity entity = null;
		if (isAspect(object)) {
			entity = caseEClassToAspect(object);
		} else {
			entity = caseEClassToConcept(object);
		}
		for (EClass eSuperType : object.getESuperTypes()) {
			if (!eSuperType.eIsProxy()) {
				String superIri = getIri(vocabulary, eSuperType);
				if (superIri != null) {
					oml.addSpecializationAxiom(vocabulary, entity.getIri(), superIri);
				}
			}
		}
		addAnnotations(entity, object);
		
		object.getEStructuralFeatures().stream().forEach(f -> doSwitch(f));
		
		return entity;
	}
	
	private Aspect caseEClassToAspect(EClass object) {
		final Vocabulary vocabulary = (Vocabulary) rootToOntology.get(object.getEPackage()); 
		return oml.addAspect(vocabulary, getAspectName(object));
	}

	private Concept caseEClassToConcept(EClass object) {
		final Vocabulary vocabulary = (Vocabulary) rootToOntology.get(object.getEPackage()); 
		return oml.addConcept(vocabulary, getConceptName(object));
	}

	@Override
	public EObject caseEAttribute(EAttribute object) {
		final Vocabulary vocabulary = (Vocabulary) rootToOntology.get(object.getEContainingClass().getEPackage()); 
		final EClass domain = object.getEContainingClass();
		final EDataType range = object.getEAttributeType();
		final boolean isFeatureMap = object.getEType().getName().equals("EFeatureMapEntry");
		
		if (isFeatureMap) {
			return null;
		}

		// find the domain
		String domainIri = getIri(vocabulary, domain);
		// find the range
		String rangeIri = getIri(vocabulary, range);

		// create Property
		final String name = getScalarPropertyName(object);
		final boolean isFunctional = object.getUpperBound() == 1;
		Property property = oml.addScalarProperty(vocabulary, name, domainIri, rangeIri, isFunctional);
		addAnnotations(property, object);

		// Relation specialization
		for (EAttribute attr : getSuperEAttributes(object)) {
			final String attrPackageIri = getIri(attr.getEContainingClass().getEPackage());
			final SeparatorKind attrPackageSep = getSeparator(attr.getEContainingClass().getEPackage());
			final String attrPackagePrefix = getPrefix(attr.getEContainingClass().getEPackage());
			final String attrName = attr.getEContainingClass().getName() + "." + attr.getName();
			String baseIri = getTermIriAndImportIfNeeded(vocabulary, attrPackageIri, attrPackageSep, attrName, attrPackagePrefix);
			oml.addSpecializationAxiom(vocabulary, property.getIri(), baseIri);
		}

		return property;
	}

	private Set<EReference> eReferencesCache = new HashSet<EReference>();

	@Override
	public EObject caseEReference(EReference object) {
		final Vocabulary vocabulary = (Vocabulary) rootToOntology.get(object.getEContainingClass().getEPackage()); 

		if (eReferencesCache.contains(object)) {
			return null;
		}

		final Association association = associations.get(object);
		final EReference reference = association.forward;
		final EReference opposite = association.reverse;

		String name = association.getName();
		final EClass source = (reference != null) ? reference.getEContainingClass() : opposite.getEReferenceType();
		final EClass target = (reference != null) ? reference.getEReferenceType() : opposite.getEContainingClass();
		final boolean isFunctional = (reference != null) ? reference.getUpperBound()==1 : false;
		final boolean isInverseFunctional = (opposite != null) ? opposite.getLowerBound()==1 : false;

		// the relation entity's source
		String sourceIri = getIri(vocabulary, source);

		// the relation entity's target
		String targetIri = getIri(vocabulary, target);

		// the relation entity
		final RelationEntity entity = oml.addRelationEntity(
			vocabulary, name, sourceIri, targetIri, isFunctional, isInverseFunctional,
			false, false, false, false, false);
		
		// the forward relation
		if (reference != null) {
			final String forwardName = association.getForwardName();
			ForwardRelation forward = oml.addForwardRelation(entity, forwardName);
			addAnnotations(forward, reference);
			eReferencesCache.add(reference);
		}

		// the reverse relation
		if (opposite != null) {
			String reverseName = association.getReverseName();
			ReverseRelation reverse = oml.addReverseRelation(entity, reverseName);
			addAnnotations(reverse, opposite);
			eReferencesCache.add(opposite);
		}
		
		// Relation specialization
		for (Association superAss : association.supers) {
			final EReference superReference = superAss.forward;
			final EReference superOpposite = superAss.reverse;
			
			final String refName = superAss.getName();
			final EPackage ePackage = (superReference != null)
					? superReference.getEContainingClass().getEPackage()
					: superOpposite.getEContainingClass().getEPackage();
			final String refPackageIri = getIri(ePackage);
			final SeparatorKind refPackageSep = getSeparator(ePackage);
			final String refPackagePrefix = getPrefix(ePackage);
			
			String superEntityIri = getTermIriAndImportIfNeeded(vocabulary, refPackageIri, refPackageSep, refName, refPackagePrefix);
			oml.addSpecializationAxiom(vocabulary, entity.getIri(), superEntityIri);
		}
		
		return entity;
	}

	//----------------------------------------------------------------------
	// Utilities
	//----------------------------------------------------------------------

	private boolean isAspect(EClass object) {
		return (object.eIsProxy() || object.isAbstract() || object.isInterface()) && 
				object.getESuperTypes().stream().allMatch(i -> isAspect(i));
	}

	private void addAnnotations(Member member, ENamedElement object) {
		final Ontology ontology = member.getOntology(); 

		// add rdfs:label
		oml.addAnnotation(ontology, member.getIri(), getTermIriAndImportIfNeeded(ontology, RDFS_IRI, SeparatorKind.HASH, "label", "rdfs"), oml.createQuotedLiteral(ontology, object.getName(), null, null));

		// add rfds:comment
		EAnnotation genModel = object.getEAnnotation(GEN_MODEL_IRI);
		if (genModel != null) {
			String doc = genModel.getDetails().get("documentation");
			if (doc != null) {
				oml.addAnnotation(ontology, member.getIri(), getTermIriAndImportIfNeeded(ontology, RDFS_IRI, SeparatorKind.HASH, "comment", "rdfs"), oml.createQuotedLiteral(ontology, doc, null, null));
			}
		}
	}
	
	private String getPrefix(EPackage object) {
		return object.getNsPrefix();
	}	

	private SeparatorKind getSeparator(EPackage object) {
		final String nsURI = object.getNsURI();
		if (nsURI.endsWith("/")) {
			return SeparatorKind.SLASH;
		} else {
			return SeparatorKind.HASH;
		}
	}	

	private String getIri(EPackage object) {
		String nsURI = object.getNsURI();
		if (nsURI.endsWith("#") || nsURI.endsWith("/")) {
			nsURI = nsURI.substring(0, nsURI.length()-1);
		}
		return nsURI;
	}	

	private String getIri(Ontology ontology, EClassifier object) {
		final String name = object.getName();
		final String iri = getIri(object.getEPackage());
		final SeparatorKind sep = getSeparator(object.getEPackage());
		final String prefix =  getPrefix(object.getEPackage());

		if (object instanceof EDataType) {
			if (Ecore_IRI.equals(iri) || XML_TYPE_IRI.equals(iri)) {
				return getStandardIri(ontology, name);
			}
		}

		if (!ontology.getIri().equals(iri)) {
			addImportedURI(object.getEPackage());
		}
		
		return getTermIriAndImportIfNeeded(ontology, iri, sep, name, prefix);
	}

	private String getTermIriAndImportIfNeeded(Ontology ontology, String iri, SeparatorKind sep, String name, String prefix) {
		if (!iri.equals(ontology.getIri())) {
			if (!OmlRead.getImports(ontology).stream().anyMatch(i -> i.getIri().equals(iri))) {
				if (ontology instanceof Vocabulary) {
					oml.addVocabularyExtension((Vocabulary) ontology, iri, sep, prefix);
				} else if (ontology instanceof Description) {
					oml.addDescriptionUsage((Description) ontology, iri, sep, prefix);
				}
			}
		}
		return iri+sep+name;
	}
	
	private String getStandardIri(Ontology ontology, String name) {
		name = name.toLowerCase();
		if (xsdTypes.containsKey(name)) {
			return getTermIriAndImportIfNeeded(ontology, XSD_IRI, SeparatorKind.HASH, xsdTypes.get(name), "xsd");
		} else if (owlTypes.containsKey(name)) {
			return getTermIriAndImportIfNeeded(ontology, OWL_IRI, SeparatorKind.HASH, owlTypes.get(name), "owl");
		} else if (rdfsTypes.containsKey(name)) {
			return getTermIriAndImportIfNeeded(ontology, RDFS_IRI, SeparatorKind.HASH, rdfsTypes.get(name), "rdfs");
		} else if (rdfTypes.containsKey(name)) {
			return getTermIriAndImportIfNeeded(ontology, RDF_IRI, SeparatorKind.HASH, rdfTypes.get(name), "rdf");
		}
		System.out.println(name);
		return null;
	}

	private Set<EAttribute> getSuperEAttributes(EAttribute object) {
		Set<EAttribute> superRefs = new LinkedHashSet<EAttribute>() ;
		EAnnotation ann = object.getEAnnotation("subsets");
		if (ann != null) {
			for (EObject ref : ann.getReferences()) {
				superRefs.add((EAttribute)ref);
			}
		}
		ann = object.getEAnnotation("redefines");
		if (ann != null) {
			for (EObject ref : ann.getReferences()) {
				superRefs.add((EAttribute)ref);
			}
		}
		return superRefs;
	}

	private String getConceptName(EClass object) {
		return object.getName();
	}
	
	private String getAspectName(EClass object) {
		return object.getName();
	}

	private String getEnumeratedScalarName(EEnum object) {
		return object.getName();
	}

	private String getEnumerationLiteralName(EEnumLiteral object) {
		return object.getLiteral();
	}

	private String getFacetedScalarName(EDataType object) {
		return object.getName();
	}

	private String getScalarPropertyName(EAttribute object) {
		return object.getEContainingClass().getName() + "_" + object.getName();
	}

}