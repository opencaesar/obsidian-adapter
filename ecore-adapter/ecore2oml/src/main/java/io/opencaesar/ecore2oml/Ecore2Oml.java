package io.opencaesar.ecore2oml;

import static io.opencaesar.oml.util.OmlRead.getMembers;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.exists;

import java.util.Collection;
import java.util.Optional;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreSwitch;
import org.eclipse.xtext.xbase.lib.StringExtensions;

import io.opencaesar.oml.AnnotationProperty;
import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.Concept;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.EnumeratedScalar;
import io.opencaesar.oml.FacetedScalar;
import io.opencaesar.oml.ForwardRelation;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.Property;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.ReverseRelation;
import io.opencaesar.oml.ScalarProperty;
import io.opencaesar.oml.SeparatorKind;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlWriter;

public class Ecore2Oml extends EcoreSwitch<EObject> {
	
	// Namespaces for core vocabularies
	private static final String XSD = "http://www.w3.org/2001/XMLSchema";
	private static final String DC = "http://purl.org/dc/elements/1.1";
	private static final String RDFS = "http://www.w3.org/2000/01/rdf-schema";
	private static final String OWL = "http://www.w3.org/2002/07/owl";
	private static final String OML = "http://opencaesar.io/oml";
	private static final String Ecore = "http://www.eclipse.org/emf/2002/Ecore";

	// Annotations defined by the Ecore2Oml transform
	private static enum AnnotationKind {
		isAnnotationProperty,
		isRelationEntity,
		isRelationSource,
		isRelationTarget,
		name,
		forwardName,
		reverseName,
		domainName,
		ignore,
	};

	private Vocabulary vocabulary;
	private final EPackage ePackage;
	private final URI outputResourceURI;
	private final OmlWriter oml;
	
	public Ecore2Oml(EPackage ePackage, URI outputResourceURI, OmlWriter oml) {
		this.ePackage = ePackage;
		this.outputResourceURI = outputResourceURI;
		this.oml = oml;
	}
	
	public void run() {
		doSwitch(ePackage);
	}

	@Override
	public EObject caseEPackage(EPackage object) {
		final String iri = getIri(object);
		final SeparatorKind separator = getSeparator(object);
		final String pefix = getPrefix(object);
		
		vocabulary = oml.createVocabulary(outputResourceURI, iri, separator, pefix);
		oml.addVocabularyExtension(vocabulary, OWL, null);
		object.getEClassifiers().stream().forEach(c -> doSwitch(c));
		
		return vocabulary;
	}

	@Override
	public EObject caseEEnum(EEnum object) {
		final String name = getMappedName(object);
		final Literal[] literals = object.getELiterals().stream().map(i -> doSwitch(i)).toArray(Literal[]::new);

		final EnumeratedScalar scalar = oml.addEnumeratedScalar(vocabulary, name, literals);
		addLabelAnnotatiopnIfNeeded(scalar, name);
		
		return scalar;
	}
	
	@Override
	public EObject caseEEnumLiteral(EEnumLiteral object) {
		return oml.createQuotedLiteral(vocabulary, getMappedName(object), null, null);
	}

	@Override
	public EObject caseEDataType(EDataType object) {
		final String name = getMappedName(object);

		final FacetedScalar scalar = oml.addFacetedScalar(vocabulary, name, null, null, null, null, null, null, null, null, null);
		addLabelAnnotatiopnIfNeeded(scalar, name);
		
		return scalar;
	}

	@Override
	public EObject caseEClass(EClass object) {
		final String name = getMappedName(object);

		Entity entity = null;
		if (isAnnotationSet(object, AnnotationKind.isRelationEntity)) {
			entity = caseEClassToRelationEntity(object);
		} else if (isAspect(object)) {
			entity = caseEClassToAspect(object);
		} else {
			entity = caseEClassToConcept(object);
		}
		for (EClass eSuperType : object.getESuperTypes()) {
			String superIri = getIri(eSuperType);
			if (superIri != null) {
				oml.addSpecializationAxiom(vocabulary, OmlRead.getIri(entity), superIri);
			}
		}
		addLabelAnnotatiopnIfNeeded(entity, name);
		object.getEStructuralFeatures().stream().forEach(f -> doSwitch(f));
		
		return entity;
	}
	
	private RelationEntity caseEClassToRelationEntity(EClass object) {
		final String sourceIri = getAnnotatedElementIri(object.getEAllReferences(), AnnotationKind.isRelationSource);
		final String targetIri = getAnnotatedElementIri(object.getEAllReferences(), AnnotationKind.isRelationTarget);
		final String forward = getAnnotationValue(object, AnnotationKind.forwardName);
		final String reverse = getAnnotationValue(object, AnnotationKind.reverseName);

		final RelationEntity entity = oml.addRelationEntity(
			vocabulary, getMappedName(object),sourceIri, targetIri,
			false, false, false, false, false, false, false);
		oml.addForwardRelation(entity, forward);
		if (reverse != null) {
			oml.addReverseRelation(entity, reverse);
		}
		
		return entity;
	}

	private Aspect caseEClassToAspect(EClass object) {
		return oml.addAspect(vocabulary, getMappedName(object));
	}

	private Concept caseEClassToConcept(EClass object) {
		return oml.addConcept(vocabulary, getMappedName(object));
	}

	@Override
	public EObject caseEAttribute(EAttribute object) {
		final String name = getMappedName(object);
		final EClass domain = object.getEContainingClass();
		final EDataType range = object.getEAttributeType();
		final boolean isDerived = object.isDerived();
		
		if (isDerived) {
			return null;
		}
		if (isAnnotationSet(object, AnnotationKind.ignore)) {
			return null;
		}
		if (isAnnotationSet(range, AnnotationKind.ignore)) {
			return null;
		}

		// find the domain
		String domainIri = getIri(domain);
		if (isAnnotationSet(object, AnnotationKind.domainName)) {
			final String aspectName = getAnnotationValue(object, AnnotationKind.domainName);
			if (!memberExists(aspectName)) {
				final Aspect aspect = oml.addAspect(vocabulary, aspectName);
				addGeneratedAnnotation(aspect);
			}
			final String aspectIri = OmlRead.getNamespace(vocabulary)+aspectName;
			oml.addSpecializationAxiom(vocabulary, getIri(domain), aspectIri);
			domainIri = aspectIri;
		}
		
		// find the range
		String rangeIri = getIri(range);

		// create Property
		Property property = null;
		if (isAnnotationSet(object, AnnotationKind.isAnnotationProperty)) {
			property = caseEAttributeToAnnotationProperty(object);		
		} else {
			property = caseEAttributeToScalarProperty(object, domainIri, rangeIri); 
		}
		addLabelAnnotatiopnIfNeeded(property, name);
		
		return property;
	}

	private AnnotationProperty caseEAttributeToAnnotationProperty(EAttribute object) {
		final String name = getMappedName(object);
		return oml.addAnnotationProperty(vocabulary, name);		
	}
	
	private ScalarProperty caseEAttributeToScalarProperty(EAttribute object, String domainIri, String rangeIri) {
		final String name = getMappedName(object);
		final boolean isFunctional = object.getUpperBound() == 1;
		return oml.addScalarProperty(vocabulary, name, domainIri, rangeIri, isFunctional);
	}

	@Override
	public EObject caseEReference(EReference object) {
		final String name = getMappedName(object);
		final EClass source = object.getEContainingClass();
		final EClass target = object.getEReferenceType();
		final EReference opposite = object.getEOpposite();
		final boolean isDerived = object.isDerived();
		final boolean isFunctional = object.getUpperBound() == 1;
		final boolean isInverseFunctional = (opposite != null) && opposite.getLowerBound() == 1;

		if (isDerived) {
			return null;
		}
		if (isAnnotationSet(object, AnnotationKind.ignore)) {
			return null;
		}
		if (isAnnotationSet(target, AnnotationKind.ignore)) {
			return null;
		}
		if (isAnnotationSet(object, AnnotationKind.isRelationSource) || 
			isAnnotationSet(object, AnnotationKind.isRelationTarget)) {
			return null;
		}
		
		// the relation entity's source
		String sourceIri = getIri(source);
		if (isAnnotationSet(object, AnnotationKind.domainName)) {
			final String aspectName = getAnnotationValue(object, AnnotationKind.domainName);
			if (!memberExists(aspectName)) {
				final Aspect aspect = oml.addAspect(vocabulary, aspectName);
				addGeneratedAnnotation(aspect);
			}
			final String aspectIri = OmlRead.getNamespace(vocabulary)+aspectName;
			oml.addSpecializationAxiom(vocabulary, getIri(source), aspectIri);
			sourceIri = aspectIri;
		}

		// the relation entity's target
		String targetIri = getIri(target);

		// the relation entity
		final String entityName = StringExtensions.toFirstUpper(name)+"Relation";
		final RelationEntity entity = oml.addRelationEntity(
			vocabulary, entityName, sourceIri, targetIri, isFunctional, isInverseFunctional,
			false, false, false, false, false);
		
		// the forward relation
		final String forwardName = name;
		ForwardRelation forward = oml.addForwardRelation(entity, forwardName);
		addLabelAnnotatiopnIfNeeded(forward, forwardName);

		// the reverse relation
		if (opposite != null) {
			String reverseName = getMappedName(opposite);
			if (isAnnotationSet(object, AnnotationKind.reverseName)) {
				reverseName = getAnnotationValue(object, AnnotationKind.reverseName);
			}
			ReverseRelation reverse = oml.addReverseRelation(entity, reverseName);
			addLabelAnnotatiopnIfNeeded(reverse, reverseName);
		}
		
		return entity;
	}

	//----------------------------------------------------------------------
	// Utilities
	//----------------------------------------------------------------------

	private boolean memberExists(String name) {
		return exists(getMembers(vocabulary), i -> i.getName().equals(name));		
	}
	
	private boolean isAspect(EClass object) {
		return (object.eIsProxy() || object.isAbstract() || object.isInterface()) && 
				object.getESuperTypes().stream().allMatch(i -> isAspect(i));
	}

	private void addLabelAnnotatiopnIfNeeded(Member object, String mappedName) {
		String originalName = object.getName();
		if (!originalName.equals(mappedName)) {
			Literal label = oml.createQuotedLiteral(vocabulary, originalName, null, null);
			oml.addAnnotation(vocabulary, OmlRead.getIri(object), RDFS+"#label", label);
		}
	}
	
	private void addGeneratedAnnotation(Member object) {
		Literal generated = oml.createQuotedLiteral(vocabulary, "generated", null, null);
		oml.addAnnotation(vocabulary, OmlRead.getIri(object), DC+"/source", generated);
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

	private String getIri(final ENamedElement object) {
		if (object instanceof EPackage) {
			return getIri((EPackage) object);
		} else if (object instanceof EClass) {
			return getIri((EClass) object);
		} else if (object instanceof EEnum) {
			return getIri((EEnum) object);
		} else if (object instanceof EDataType) {
			return getIri((EDataType) object);
		} else if (object instanceof EStructuralFeature) {
			return getIri((EStructuralFeature) object);
		}
		return null;
	}

	private String getIri(EPackage object) {
		String nsURI = object.getNsURI();
		if (nsURI.endsWith("#") || nsURI.endsWith("/")) {
			nsURI = nsURI.substring(0, nsURI.length()-1);
		}
		return nsURI;
	}	

	private String getIri(EClass object) {
		final EPackage ePackage = object.getEPackage();  
		if (ePackage != null) {
			return qualify(getIri(ePackage)+ getSeparator(ePackage)+ getMappedName(object), object);
		}
		return null;
	}	

	private String getIri(EEnum object) {
		final EPackage ePackage = object.getEPackage();  
		if (ePackage != null) {
			return qualify(getIri(ePackage)+ getSeparator(ePackage)+ getMappedName(object), object);
		}
		return null;
	}	

	private String getIri(EDataType object) {
		final String name = getMappedName(object);
		
		if (EcorePackage.Literals.ESTRING.getName().equals(name))
			return XSD+"#string";
		if (EcorePackage.Literals.EINT.getName().equals(name))
			return XSD+"#int";
		if (EcorePackage.Literals.EINTEGER_OBJECT.getName().equals(name))
			return XSD+"#int";
		if (EcorePackage.Literals.EBOOLEAN.getName().equals(name))
			return XSD+"#boolean";
		if (EcorePackage.Literals.EDOUBLE.getName().equals(name))
			return XSD+"#double";
		if (EcorePackage.Literals.EDOUBLE_OBJECT.getName().equals(name))
			return XSD+"#double";
		if (EcorePackage.Literals.EFLOAT.getName().equals(name))
			return XSD+"#float";
		if (EcorePackage.Literals.EFLOAT_OBJECT.getName().equals(name))
			return XSD+"#float";
		if (EcorePackage.Literals.EBIG_DECIMAL.getName().equals(name))
			return XSD+"#decimal";

		final EPackage ePackage = object.getEPackage();  
		if (ePackage != null) {
			return qualify(getIri(ePackage)+getSeparator(ePackage)+object.getName(), object);
		}
		return null;
	}

	private String getIri(EStructuralFeature object) {
		final EPackage ePackage = object.getEContainingClass().getEPackage();  
		return getIri(ePackage)+ getSeparator(ePackage)+ getMappedName(object);
	}	

	private String qualify(String iri, EClassifier object) {
		final String vocabularyIri = getIri(object.getEPackage());
		if (!vocabularyIri.equals(vocabulary.getIri()) && !Ecore.equals(vocabularyIri)) {
			if (!vocabulary.getOwnedImports().stream().anyMatch(i -> i.getUri().equals(vocabularyIri))) {
				oml.addVocabularyExtension(vocabulary, vocabularyIri, null);
			}
		}
		return iri;
	}

	private String getMappedName(ENamedElement object) {
		return getAnnotationValue(object, AnnotationKind.name, object.getName());
	}

	private <T extends ENamedElement> String getAnnotatedElementIri(Collection<T> coll, AnnotationKind kind) {
		final Optional<T> object = coll.stream().filter(i -> isAnnotationSet(i, kind)).findFirst();
		if (object.isPresent()) {
			return getIri(object.get());
		}
		return null;
	}
	
	private String getAnnotationValue(EModelElement object, AnnotationKind kind, String defaultValue) {
		final String value = getAnnotationValue(object, kind);
		return (value != null) ? value : defaultValue;
	}

	private String getAnnotationValue(EModelElement object, AnnotationKind kind) {
		final Optional<EAnnotation> annotation = object.getEAnnotations().stream().
			filter(i -> OML.equals(i.getSource())).findFirst();
		if (annotation.isPresent()) {
			return annotation.get().getDetails().get(kind.toString());
		}
		return null;
	}

	private boolean isAnnotationSet(EModelElement object, AnnotationKind kind) {
		return getAnnotationValue(object, kind) != null;
	}
}
