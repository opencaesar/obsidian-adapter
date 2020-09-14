package io.opencaesar.ecore2oml;

import static io.opencaesar.oml.util.OmlRead.getMembers;
import static io.opencaesar.oml.util.OmlRead.getNamespace;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.exists;

import java.util.Optional;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.xtext.xbase.lib.StringExtensions;

import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.RangeRestrictionKind;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.SeparatorKind;
import io.opencaesar.oml.Term;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlWriter;

public class Ecore2Oml {
	
	private static final String XSD = "http://www.w3.org/2001/XMLSchema";
	private static final String OWL = "http://www.w3.org/2002/07/owl";
	private static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns";
	private static final String RDFS = "http://www.w3.org/2000/01/rdf-schema";
	private static final String DC = "http://purl.org/dc/elements/1.1";
	private static final String Ecore = "http://www.eclipse.org/emf/2002/Ecore";
	private static final String OML = "http://opencaesar.io/oml";
	
	private static enum AnnotationKind {
		annotationProperty,
		relationEntity,
		name,
		source,
		target,
		forward,
		reverse,
		ignore,
		duplicate,
		className,
		typeName,
		oppositeName
	};

	private final EPackage ePackage;
	private final URI outputResourceURI;
	private final OmlWriter oml;	
	
	public Ecore2Oml(EPackage ePackage, URI outputResourceURI, OmlWriter oml) {
		this.ePackage = ePackage;
		this.outputResourceURI = outputResourceURI;
		this.oml = oml;
	}
	
	public void run() {
		final Vocabulary vocabulary = toVocabulary(ePackage);
		final TreeIterator<EObject> i = ePackage.eAllContents();

		while (i.hasNext()) {
			final EObject object = i.next();
			if (object instanceof EModelElement) {
				if (isAnnotationSet((EModelElement)object, AnnotationKind.ignore)) {
					i.prune();
				} else {
					addToVocabulary(object, vocabulary);
				}
			}
		}
	}

	// Eobject

	private void addToVocabulary(final EObject eObject, final Vocabulary vocabulary) {
		if (eObject instanceof EClass) {
			addToVocabulary((EClass) eObject, vocabulary);
		} else if (eObject instanceof EEnum) {
			addToVocabulary((EEnum) eObject, vocabulary);
		} else if (eObject instanceof EDataType) {
			addToVocabulary((EDataType) eObject, vocabulary);
		} else if (eObject instanceof EAttribute) {
			addToVocabulary((EAttribute) eObject, vocabulary);
		} else if (eObject instanceof EReference) {
			addToVocabulary((EReference) eObject, vocabulary);
		}
	}		  

	// EPackage

	private Vocabulary toVocabulary(EPackage ePackage) {
		final String iri = getIri(ePackage);
		final SeparatorKind separator = getSeparator(ePackage);
		final String pefix = getPrefix(ePackage);
		final Vocabulary vocabulary = oml.createVocabulary(outputResourceURI, iri, separator, pefix);
		oml.addVocabularyExtension(vocabulary, OWL, null);
		return vocabulary;
	}

	//EClass

	private void addToVocabulary(EClass eClass, Vocabulary vocabulary) {
		Term term = null;
		if (isAnnotationSet(eClass, AnnotationKind.relationEntity)) {
			final Optional<EReference> source = eClass.getEAllReferences().stream().filter(i -> isAnnotationSet(i, AnnotationKind.source)).findFirst();
			final Optional<EReference> target = eClass.getEAllReferences().stream().filter(i -> isAnnotationSet(i, AnnotationKind.target)).findFirst();
			final String forward = getAnnotationValue(eClass, AnnotationKind.forward);
			final String reverse = getAnnotationValue(eClass, AnnotationKind.reverse);
			final RelationEntity entity = oml.addRelationEntity(
				vocabulary,
				getRealName(eClass),
				(source.isPresent()) ? getIri(source.get().getEType(), vocabulary) : null, 
				(target.isPresent()) ? getIri(target.get().getEType(), vocabulary) : null, 
				false,
				false,
				false,
				false,
				false,
				false,
				false);
			oml.addForwardRelation(
				entity,
				forward);
			if (reverse != null) {
				oml.addReverseRelation(
					entity,
					reverse);
			}
			term = entity;
		} else if (isAspect(eClass)) {
			term = oml.addAspect(vocabulary, getRealName(eClass));
		} else {
			term = oml.addConcept(vocabulary, getRealName(eClass));
		}
		if (!eClass.getName().equals(getRealName(eClass))) {
			oml.addAnnotation(vocabulary, getIri(eClass), RDFS+"#label", oml.createQuotedLiteral(vocabulary, eClass.getName(), null, null));
		}
		final Term finalTerm = term;
		eClass.getESuperTypes().forEach(superTerm -> oml.addSpecializationAxiom(vocabulary, OmlRead.getIri(finalTerm), getIri(superTerm, vocabulary)));
	}

	//EEnum

	private void addToVocabulary(EEnum eEnum, Vocabulary vocabulary) {
		Literal[] literals = eEnum.getELiterals().stream().map(i -> oml.createQuotedLiteral(vocabulary, i.getName(), null, null)).toArray(Literal[]::new);
		oml.addEnumeratedScalar(vocabulary, getRealName(eEnum), literals);
		if (!eEnum.getName().equals(getRealName(eEnum))) {
			oml.addAnnotation(vocabulary, getIri(eEnum), RDFS+"#label", oml.createQuotedLiteral(vocabulary, eEnum.getName(), null, null));
		}
	}	

	//EDataType

	private void addToVocabulary(EDataType eDataType, Vocabulary vocabulary) {
		oml.addFacetedScalar(vocabulary, getRealName(eDataType), null, null, null, null, null, null, null, null, null);
		if (!eDataType.getName().equals(getRealName(eDataType))) {
			oml.addAnnotation(vocabulary, getIri(eDataType), RDFS+"#label", oml.createQuotedLiteral(vocabulary, eDataType.getName(), null, null));
		}
	}	

	//EAttribute

	private void addToVocabulary(EAttribute eAttribute, Vocabulary vocabulary) {
		final EClass domain = eAttribute.getEContainingClass();
		final EClassifier range = eAttribute.getEType();

		// if this reference is derived
		if (eAttribute.isDerived()) {
			return;
		}

		final String propertyName = getRealName(eAttribute);
		String domainIri = getIri(domain, vocabulary);
		String rangeIri = getIri(range, vocabulary);
	
		if (isAnnotationSet(eAttribute, AnnotationKind.className)) {
			final String aspectName = getAnnotationValue(eAttribute, AnnotationKind.className);
			final String aspectIri = OmlRead.getNamespace(vocabulary)+aspectName;
			if (!exists(getMembers(vocabulary), i -> i.getName().equals(aspectName))) {
				final Aspect aspect = oml.addAspect(vocabulary, aspectName);
				oml.addAnnotation(vocabulary, OmlRead.getIri(aspect), DC+"/source", oml.createQuotedLiteral(vocabulary, "generated", null, null));
			}
			oml.addSpecializationAxiom(vocabulary, getIri(domain, vocabulary), aspectIri);
			domainIri = aspectIri;
		}
		
		if (isAnnotationSet(eAttribute, AnnotationKind.typeName)) {
			rangeIri = RDF+"#PlainLiteral";
			oml.addScalarPropertyRangeRestrictionAxiom(vocabulary, getIri(domain, vocabulary), getNamespace(vocabulary)+propertyName, getIri(range, vocabulary), RangeRestrictionKind.ALL);
		}

		if (!isAnnotationSet(eAttribute, AnnotationKind.duplicate)) {
			if (isAnnotationSet(eAttribute, AnnotationKind.annotationProperty)) {
				oml.addAnnotationProperty(
					vocabulary, 
					propertyName);		
			} else {
				oml.addScalarProperty(
					vocabulary, 
					propertyName, 
					domainIri, 
					rangeIri, 
					eAttribute.getLowerBound() == 1);
			}
			if (!eAttribute.getName().equals(propertyName)) {
				oml.addAnnotation(vocabulary, getIri(eAttribute), RDFS+"#label", oml.createQuotedLiteral(vocabulary, eAttribute.getName(), null, null));
			}
		}
	}

	//ERference
	
	private void addToVocabulary(EReference eReference, Vocabulary vocabulary) {
		final EClass source = eReference.getEContainingClass();
		final EClassifier target = eReference.getEType();
		final EReference opposite = eReference.getEOpposite();
		
		// if this reference is derived
		if (eReference.isDerived()) {
			return;
		}
		
		// if this reference is considered source or target property of a relation entity or if its target is ignored
		if (isAnnotationSet(eReference, AnnotationKind.source) || 
			isAnnotationSet(eReference, AnnotationKind.target) ||
			isAnnotationSet(target, AnnotationKind.ignore)) {
			return;
		}
		
		// if this reference is singular but has a multi-valued opposite 
		if (opposite != null &&
			opposite.getUpperBound() != 1 &&
			eReference.getUpperBound() == 1) {
			return;
		}
		
		final String entityName = StringExtensions.toFirstUpper(getRealName(eReference))+"Relation";
		String sourceIri = getIri(source, vocabulary);
		String targetIri = getIri(target, vocabulary);
		final String forwardName = getRealName(eReference);
		
		if (isAnnotationSet(eReference, AnnotationKind.className)) {
			final String aspectName = getAnnotationValue(eReference, AnnotationKind.className);
			final String aspectIri = getNamespace(vocabulary)+aspectName;
			if (!exists(getMembers(vocabulary), i -> i.getName().equals(aspectName))) {
				final Aspect aspect = oml.addAspect(vocabulary, aspectName);
				oml.addAnnotation(vocabulary, OmlRead.getIri(aspect), DC+"/source", oml.createQuotedLiteral(vocabulary, "generated", null, null));
			}
			oml.addSpecializationAxiom(vocabulary, getIri(source, vocabulary), aspectIri);
			sourceIri = aspectIri;
		}

		if (!isAnnotationSet(eReference, AnnotationKind.duplicate)) {
			final RelationEntity entity = oml.addRelationEntity(
				vocabulary,
				entityName,
				sourceIri, 
				targetIri, 
				eReference.getLowerBound() == 1,
				(opposite != null) && opposite.getLowerBound() == 1,
				false,
				false,
				false,
				false,
				false);
			oml.addForwardRelation(entity, forwardName);
			if (!eReference.getName().equals(forwardName)) {
				oml.addAnnotation(vocabulary, getIri(eReference), RDFS+"#label", oml.createQuotedLiteral(vocabulary, eReference.getName(), null, null));
			}
			if (opposite != null) {
				 String reverseName = getRealName(opposite);
				if (isAnnotationSet(eReference, AnnotationKind.oppositeName)) {
					reverseName = getAnnotationValue(eReference, AnnotationKind.oppositeName);
				}
				oml.addReverseRelation(entity, reverseName);
				if (!opposite.getName().equals(reverseName)) {
					oml.addAnnotation(vocabulary, getIri(opposite), RDFS+"#label", oml.createQuotedLiteral(vocabulary, opposite.getName(), null, null));
				}
			}
		}
	}

	//--------------------------------------
	// Utilities
	
	private boolean isAspect(EClass eClass) {
		return (eClass.isAbstract() || eClass.isInterface()) && eClass.getESuperTypes().stream().allMatch(i -> isAspect(i));
	}
	
	private String getIri(EClassifier element, Vocabulary vocabulary) {
		final String importedIri = getIri(element.getEPackage());
		if (!importedIri.equals(vocabulary.getIri()) && !Ecore.equals(importedIri)) {
			if (vocabulary.getOwnedImports().stream().filter(i -> importedIri.equals(i.getUri())).count() == 0) {
				oml.addVocabularyExtension(vocabulary, importedIri, null);
			}
		}
		return getIri(element);
	}

	private SeparatorKind getSeparator(EPackage ePackage) {
		final String nsURI = ePackage.getNsURI();
		if (nsURI.endsWith("/")) {
			return SeparatorKind.SLASH;
		} else {
			return SeparatorKind.HASH;
		}
	}	

	private String getPrefix(EPackage ePackage) {
		return ePackage.getNsPrefix();
	}	

	private String getIri(final ENamedElement eEnum) {
		if (eEnum instanceof EEnum) {
			return getIri((EEnum) eEnum);
		} else if (eEnum instanceof EClass) {
			return getIri((EClass) eEnum);
		} else if (eEnum instanceof EDataType) {
			return getIri((EDataType) eEnum);
		} else if (eEnum instanceof EStructuralFeature) {
			return getIri((EStructuralFeature) eEnum);
		} else if (eEnum instanceof EPackage) {
			return getIri((EPackage) eEnum);
		}
		return null;
	}

	private String getIri(EPackage ePackage) {
		String nsURI = ePackage.getNsURI();
		if (nsURI.endsWith("#") || nsURI.endsWith("/")) {
			nsURI = nsURI.substring(0, nsURI.length()-1);
		}
		return nsURI;
	}	

	private String getIri(EClass eClass) {
		final EPackage ePackage = eClass.getEPackage();  
		return getIri(ePackage)+ getSeparator(ePackage)+ getRealName(eClass);
	}	

	private String getIri(EEnum eEnum) {
		final EPackage ePackage = eEnum.getEPackage();  
		return getIri(ePackage)+ getSeparator(ePackage)+ getRealName(eEnum);
	}	

	private String getIri(EStructuralFeature eFeature) {
		final EPackage ePackage = eFeature.getEContainingClass().getEPackage();  
		return getIri(ePackage)+ getSeparator(ePackage)+ getRealName(eFeature);
	}	

	private String getIri(EDataType eDataType) {
		final String name = getRealName(eDataType);
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
		return getIri(eDataType.getEPackage())+getSeparator(eDataType.getEPackage())+eDataType.getName(); 	
	}
	
	private String getRealName(ENamedElement element) {
		return getAnnotationValue(element, AnnotationKind.name, element.getName());
	}
	
	private boolean isAnnotationSet(EModelElement element, AnnotationKind kind) {
		final Optional<EAnnotation> annotation = element.getEAnnotations().stream().
				filter(i -> OML.equals(i.getSource())).findFirst();
			if (annotation.isPresent()) {
				return annotation.get().getDetails().containsKey(kind.toString());
			}
			return false;
	}

	private String getAnnotationValue(EModelElement element, AnnotationKind kind) {
		final Optional<EAnnotation> annotation = element.getEAnnotations().stream().
			filter(i -> OML.equals(i.getSource())).findFirst();
		if (annotation.isPresent()) {
			return annotation.get().getDetails().get(kind.toString());
		}
		return null;
	}

	private String getAnnotationValue(EModelElement element, AnnotationKind kind, String defaultValue) {
		final String value = getAnnotationValue(element, kind);
		return (value != null) ? value : defaultValue;
	}

}
