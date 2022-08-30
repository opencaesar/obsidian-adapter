package io.opencaesar.ecore2oml;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.eclipse.emf.ecore.util.EcoreSwitch;

import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.Concept;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.EnumeratedScalar;
import io.opencaesar.oml.ForwardRelation;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.Property;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.ReverseRelation;
import io.opencaesar.oml.Scalar;
import io.opencaesar.oml.SeparatorKind;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlBuilder;

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
		xsdTypes.put("estring", "string");		
		xsdTypes.put("eint", "int");		
		xsdTypes.put("eintegerobject", "int");		
		xsdTypes.put("eboolean", "boolean");		
		xsdTypes.put("ebooleanobject", "boolean");		
		xsdTypes.put("edouble", "double");		
		xsdTypes.put("edoubleobject", "double");		
		xsdTypes.put("efloat", "float");		
		xsdTypes.put("efloatobject", "float");		
		xsdTypes.put("ebigdecimal", "decimal");		
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

	private Vocabulary vocabulary;
	private final EPackage ePackage;
	private final URI outputResourceURI;
	private final OmlBuilder oml;
	
	private Map<String,EPackage> dependency = new HashMap<>();

	public Ecore2Oml(EPackage ePackage, URI outputResourceURI, OmlBuilder oml) {
		this.ePackage = ePackage;
		this.outputResourceURI = outputResourceURI;
		this.oml = oml;
	}
	
	public void run() {
		doSwitch(ePackage);
	}

	private void addExternalDepenedncy(String iri, EPackage ePackage) {
		dependency.put(iri,ePackage);
	}
	
	public Map<String,EPackage> getDependencies() {
		return dependency;
	}

	@Override
	public EObject caseEPackage(EPackage object) {
		final String iri = getIri(object);
		final SeparatorKind separator = getSeparator(object);
		final String pefix = getPrefix(object);
		
		vocabulary = oml.createVocabulary(outputResourceURI, iri, separator, pefix);
		object.getEClassifiers().stream().forEach(c -> doSwitch(c));
		
		return vocabulary;
	}

	@Override
	public EObject caseEEnum(EEnum object) {
		final String name = getEnumeratedScalarName(object);
		final Literal[] literals = object.getELiterals().stream().map(i -> doSwitch(i)).toArray(Literal[]::new);

		final EnumeratedScalar scalar = oml.addEnumeratedScalar(vocabulary, name, literals);
		addAnnotations(scalar, object);

		oml.addSpecializationAxiom(vocabulary, scalar.getIri(), getIriAndImportIfNeeded(XSD_IRI, SeparatorKind.HASH, "string", "xsd"));

		return scalar;
	}
	
	@Override
	public EObject caseEEnumLiteral(EEnumLiteral object) {
		final String name = getEnumerationLiteralName(object);

		return oml.createQuotedLiteral(vocabulary, name, null, null);
	}

	@Override
	public EObject caseEDataType(EDataType object) {
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
		String baseIri = getStandardIri(baseName);
		if (baseIri == null) {
			EClassifier base = object.getEPackage().getEClassifier(baseName);
			if (base != null && base != object) {
				baseIri = vocabulary.getIri()+SeparatorKind.HASH+baseName;
			} else {
				baseIri = getIriAndImportIfNeeded(RDFS_IRI, SeparatorKind.HASH, "Literal", "rdfs");
			}				
		}

		final Scalar scalar = oml.addFacetedScalar(vocabulary, name, null, null, null, null, null, null, null, null, null);
		addAnnotations(scalar, object);
		oml.addSpecializationAxiom(vocabulary, scalar.getIri(), baseIri);

		return scalar;
	}

	@Override
	public EObject caseEClass(EClass object) {
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
				String superIri = getIri(eSuperType);
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
		return oml.addAspect(vocabulary, getAspectName(object));
	}

	private Concept caseEClassToConcept(EClass object) {
		return oml.addConcept(vocabulary, getConceptName(object));
	}

	@Override
	public EObject caseEAttribute(EAttribute object) {
		final EClass domain = object.getEContainingClass();
		final EDataType range = object.getEAttributeType();
		final boolean isFeatureMap = object.getEType().getName().equals("EFeatureMapEntry");
		
		if (isFeatureMap) {
			return null;
		}

		// find the domain
		String domainIri = getIri(domain);
		// find the range
		String rangeIri = getIri(range);

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
			String baseIri = getIriAndImportIfNeeded(attrPackageIri, attrPackageSep, attrName, attrPackagePrefix);
			oml.addSpecializationAxiom(vocabulary, property.getIri(), baseIri);
		}

		return property;
	}

	private Set<EReference> eReferencesCache = new HashSet<EReference>();

	@Override
	public EObject caseEReference(EReference object) {
		if (eReferencesCache.contains(object)) {
			return null;
		}

		object = getForward(object, object.getEOpposite());
		if (object == null) {
			return null;
		}

		final String name = getRelationEntityName(object);
		final EClass source = object.getEContainingClass();
		final EClass target = object.getEReferenceType();
		final EReference opposite = object.getEOpposite();
		final boolean isFunctional = object.getUpperBound() == 1;
		final boolean isInverseFunctional = (opposite != null) && opposite.getLowerBound() == 1;

		// the relation entity's source
		String sourceIri = getIri(source);

		// the relation entity's target
		String targetIri = getIri(target);

		// the relation entity
		final RelationEntity entity = oml.addRelationEntity(
			vocabulary, name, sourceIri, targetIri, isFunctional, isInverseFunctional,
			false, false, false, false, false);
		
		// the forward relation
		final String forwardName = getRelationName(object);
		ForwardRelation forward = oml.addForwardRelation(entity, forwardName);
		addAnnotations(forward, object);
		eReferencesCache.add(object);

		// the reverse relation
		if (opposite != null) {
			String reverseName = getRelationName(opposite);
			ReverseRelation reverse = oml.addReverseRelation(entity, reverseName);
			addAnnotations(reverse, opposite);
			eReferencesCache.add(opposite);
		}
		
		// Relation specialization
		for (EReference ref : getSuperEReferences(object)) {
			final String refPackageIri = getIri(ref.getEContainingClass().getEPackage());
			final SeparatorKind refPackageSep = getSeparator(ref.getEContainingClass().getEPackage());
			final String refPackagePrefix = getPrefix(ref.getEContainingClass().getEPackage());
			final String refName = getRelationEntityName(ref);
			String baseIri = getIriAndImportIfNeeded(refPackageIri, refPackageSep, refName, refPackagePrefix);
			oml.addSpecializationAxiom(vocabulary, entity.getIri(), baseIri);
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
		// add rdfs:label
		oml.addAnnotation(vocabulary, member.getIri(), getIriAndImportIfNeeded(RDFS_IRI, SeparatorKind.HASH, "label", "rdfs"), oml.createQuotedLiteral(vocabulary, object.getName(), null, null));

		// add rfds:comment
		EAnnotation genModel = object.getEAnnotation(GEN_MODEL_IRI);
		if (genModel != null) {
			String doc = genModel.getDetails().get("documentation");
			if (doc != null) {
				oml.addAnnotation(vocabulary, member.getIri(), getIriAndImportIfNeeded(RDFS_IRI, SeparatorKind.HASH, "comment", "rdfs"), oml.createQuotedLiteral(vocabulary, doc, null, null));
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

	private String getIri(EClassifier object) {
		final String name = object.getName();
		final String iri = getIri(object.getEPackage());
		final SeparatorKind sep = getSeparator(object.getEPackage());
		final String prefix =  getPrefix(object.getEPackage());

		if (object instanceof EDataType) {
			if (Ecore_IRI.equals(iri) || XML_TYPE_IRI.equals(iri)) {
				return getStandardIri(name);
			}
		}

		if (!vocabulary.getIri().equals(iri)) {
			addExternalDepenedncy(iri, object.getEPackage());
		}
		
		return getIriAndImportIfNeeded(iri, sep, name, prefix);
	}

	private String getIriAndImportIfNeeded(String iri, SeparatorKind sep, String name, String prefix) {
		if (!iri.equals(vocabulary.getIri())) {
			if (!vocabulary.getOwnedImports().stream().anyMatch(i -> i.getIri().equals(iri))) {
				oml.addVocabularyExtension(vocabulary, iri, sep, prefix);
			}
		}
		return iri+sep+name;
	}
	
	private String getStandardIri(String name) {
		name = name.toLowerCase();
		if (xsdTypes.containsKey(name)) {
			return getIriAndImportIfNeeded(XSD_IRI, SeparatorKind.HASH, xsdTypes.get(name), "xsd");
		} else if (owlTypes.containsKey(name)) {
			return getIriAndImportIfNeeded(OWL_IRI, SeparatorKind.HASH, owlTypes.get(name), "owl");
		} else if (rdfsTypes.containsKey(name)) {
			return getIriAndImportIfNeeded(RDFS_IRI, SeparatorKind.HASH, rdfsTypes.get(name), "rdfs");
		} else if (rdfTypes.containsKey(name)) {
			return getIriAndImportIfNeeded(RDF_IRI, SeparatorKind.HASH, rdfTypes.get(name), "rdf");
		}
		return null;
	}

	private Map<EReference, EReference> eReferencesMap = new HashMap<EReference, EReference>();

	private EReference getForward(EReference ref1, EReference ref2) {
		if (eReferencesMap.containsKey(ref1)) {
			return eReferencesMap.get(ref1);
		}
		
		Iterator<EReference> i = getSuperEReferences(ref1).iterator();
		if (i.hasNext()) {
			EReference superRef1 = i.next();
			EReference superOppRef1 = superRef1.getEOpposite();
			EReference superForward = getForward(superRef1, superOppRef1);
			if (superForward == superRef1) {
				while (i.hasNext()) {
					superRef1 = i.next();
					superOppRef1 = superRef1.getEOpposite();
					eReferencesMap.put(superRef1, superRef1);
					eReferencesMap.put(superOppRef1, superRef1);
				}
				return ref1;
			} else {
				while (i.hasNext()) {
					superRef1 = i.next();
					superOppRef1 = superRef1.getEOpposite();
					eReferencesMap.put(superRef1, superOppRef1);
					eReferencesMap.put(superOppRef1, superOppRef1);
				}
				return ref2;
			}
		}
		
		EReference one = null;
		if (ref2 == null) {
			one = ref1;
		} else {
			// container wins
			if (ref1.isContainment() && !ref2.isContainment()) {
				one = ref1;
			} else if (ref2.isContainment() && !ref1.isContainment()) {
				one = ref2;
			}
			// higher multiplicity wins
			if (one == null) {
				int ref1Upper = (ref1.getUpperBound() == -1) ? Integer.MAX_VALUE : ref1.getUpperBound();
				int ref2Upper = (ref2.getUpperBound() == -1) ? Integer.MAX_VALUE : ref2.getUpperBound();
				if (ref1Upper > ref2Upper) {
					one = ref1;
				} else if (ref2Upper > ref1Upper) {
					one = ref2;
				}
			}
			// higher alphabetical wins
			if (one == null) {
				one = ref1.getName().compareTo(ref2.getName()) > 0 ? ref1 : ref2;
			}
		}
		
		eReferencesMap.put(one, one);
		if (one.getEOpposite() != null) {
			eReferencesMap.put(one.getEOpposite(), one);
		}
		return one;
	}
	
	private Set<EReference> getSuperEReferences(EReference object) {
		Set<EReference> superRefs = new LinkedHashSet<EReference>() ;
		EAnnotation ann = object.getEAnnotation("subsets");
		if (ann != null) {
			for (EObject ref : ann.getReferences()) {
				superRefs.add((EReference)ref);
			}
		}
		ann = object.getEAnnotation("redefines");
		if (ann != null) {
			for (EObject ref : ann.getReferences()) {
				superRefs.add((EReference)ref);
			}
		}
		return superRefs;
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

	private String getRelationEntityName(EReference object) {
		return object.getEContainingClass().getName() + "_" + object.getName() + "_A";
	}

	private String getRelationName(EReference object) {
		return object.getEContainingClass().getName() + "_"  + object.getName();
	}

}