package io.opencaesar.ecore2oml.handlers;

import static io.opencaesar.ecore2oml.Util.addLabelAnnotatiopnIfNeeded;
import static io.opencaesar.ecore2oml.Util.getAnnotationValue;
import static io.opencaesar.ecore2oml.Util.getIri;
import static io.opencaesar.ecore2oml.Util.getMappedName;
import static io.opencaesar.ecore2oml.Util.handleNamedElementDoc;
import static io.opencaesar.ecore2oml.Util.isAnnotationSet;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

import io.opencaesar.ecore2oml.AnnotationKind;
import io.opencaesar.ecore2oml.Util;
import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.oml.CardinalityRestrictionKind;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.RangeRestrictionKind;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlWriter;

public class EClassHandler implements ConversionHandler {

	private static final String ETYPE = "eType";
	private static final String DUPLICATES = "duplicates";
	private static final String REDEFINES = "redefines";

	static private Logger LOGGER = LogManager.getLogger(EClassHandler.class);

	@Override
	public EObject convert(EObject eObject, Vocabulary vocabulary, OmlWriter oml,
			Map<CollectionKind, Object> collections) {
		EClass object = (EClass) eObject;
		EAnnotation annotation = Util.getAnnotation(object, DUPLICATES);
		boolean isDuplicate = annotation == null ? false : true;
		final String name = getMappedName(object);

		Entity entity = null;
		if (isAnnotationSet(object, AnnotationKind.isRelationEntity)) {
			entity = convertEClassToRelationEntity(object, oml, vocabulary);
		} else if (isAspect(object)) {
			entity = oml.addAspect(vocabulary, getMappedName(object));
		} else {
			entity = oml.addConcept(vocabulary, getMappedName(object));
		}
		for (EClass eSuperType : object.getESuperTypes()) {
			String superIri = getIri(eSuperType, vocabulary, oml);
			if (superIri != null) {
				oml.addSpecializationAxiom(vocabulary, OmlRead.getIri(entity), superIri);
			}
		}
		addLabelAnnotatiopnIfNeeded(entity, name, oml, vocabulary);
		if (isDuplicate) {
			handleDuplicate(object, entity, annotation, oml, vocabulary);
		}
		handleNamedElementDoc(object, entity, oml, vocabulary);
		return entity;
	}

	private void handleDuplicate(EClass object, Entity entity, EAnnotation annotation, OmlWriter oml,
			Vocabulary vocabulary) {
		annotation.eContents().forEach(element -> {
			// elements here can be EOperation , ERef, EAttribute, we handle only ERef and
			// EAttribute
			if (element instanceof EReference) {
				handleEReferenceDuplicate(annotation, entity, (EReference) element, oml, vocabulary);
			} else if (element instanceof EAttribute) {
				handleEAttributeDuplicate(annotation, entity, (EAttribute) element, oml, vocabulary);
			}
		});
	}

	static private EObject getOriginal(EModelElement element) {
		EObject retVal = null;
		EAnnotation redefAnnotation = Util.getAnnotation(element, REDEFINES);
		if (redefAnnotation != null) {
			for (EObject eRef : redefAnnotation.getReferences()) {
				if (! (eRef.eContainer() instanceof EAnnotation) ) {
					retVal = eRef;
				}
			}
		}
		if (retVal == null) {
			LOGGER.error("could not find original of duplicate :" + element);
		}
		return retVal;
	}

	private void handleRetrictions(EAnnotation annotation, Entity entity, EStructuralFeature element, EClassifier type,
			boolean isRef, OmlWriter oml, Vocabulary vocabulary) {
		EStructuralFeature original = (EStructuralFeature) getOriginal(element);
		int upper = element.getUpperBound();
		int lower = element.getLowerBound();
		int oUpper = original.getUpperBound();
		int oLower = original.getLowerBound();
		String entityIRI = OmlRead.getIri(entity);
		String relationOrPropIRI = getIri(original, vocabulary, oml);
		if (upper != oUpper || lower != oLower) {
			// cardinality restriction
			if (element instanceof EReference) {
				oml.addRelationCardinalityRestrictionAxiom(vocabulary, entityIRI, relationOrPropIRI,
						CardinalityRestrictionKind.MAX, upper, null);
				oml.addRelationCardinalityRestrictionAxiom(vocabulary, entityIRI, relationOrPropIRI,
						CardinalityRestrictionKind.MIN, lower, null);
			}else {
				oml.addScalarPropertyCardinalityRestrictionAxiom(vocabulary, entityIRI, relationOrPropIRI,
						CardinalityRestrictionKind.MAX, upper, null);
				oml.addScalarPropertyCardinalityRestrictionAxiom(vocabulary, entityIRI, relationOrPropIRI,
						CardinalityRestrictionKind.MIN, lower, null);
				
			}

		}
		Object defaultValue = element.getDefaultValue();
		Object oDefaultValue = original.getDefaultValue();
		if (defaultValue!=null && !defaultValue.equals(oDefaultValue) || (oDefaultValue!=null && !oDefaultValue.equals(defaultValue))) {
			// TODO : annotation
			LOGGER.info(element.getName() + " New Default Value : " + defaultValue);
		}

		EClassifier oType = original.getEType();
		if (type != oType) {
			String rangeIRI = getIri(type, vocabulary, oml);
			// range restriction
			if (element instanceof EReference) {
				oml.addRelationRangeRestrictionAxiom(vocabulary, entityIRI, relationOrPropIRI, rangeIRI,
						RangeRestrictionKind.ALL);
			} else {
				oml.addScalarPropertyRangeRestrictionAxiom(vocabulary, entityIRI, relationOrPropIRI, rangeIRI,
						RangeRestrictionKind.ALL);
			}
		}

	}

	private void handleEAttributeDuplicate(EAnnotation annotation, Entity entity, EAttribute element, OmlWriter oml,
			Vocabulary vocabulary) {
		handleRetrictions(annotation, entity, element, element.getEType(), false, oml, vocabulary);
	}

	private void handleEReferenceDuplicate(EAnnotation annotation, Entity entity, EReference element, OmlWriter oml,
			Vocabulary vocabulary) {
		EClassifier type = element.getEType();
		if (element.getEOpposite() != null) {
			EAnnotation typeAnnotation = Util.getAnnotation(annotation, element.getName());
			if (typeAnnotation != null) {
				String val = typeAnnotation.getDetails().get(ETYPE);
				// UGLY !!
				String typeName = val.substring(5);
				type = type.getEPackage().getEClassifier(typeName);
			}
		}
		handleRetrictions(annotation, entity, element, type, true, oml, vocabulary);
	}

	static private RelationEntity convertEClassToRelationEntity(EClass object, OmlWriter oml, Vocabulary vocabulary) {
		final String sourceIri = getAnnotatedElementIri(object.getEAllReferences(), AnnotationKind.isRelationSource,
				oml, vocabulary);
		final String targetIri = getAnnotatedElementIri(object.getEAllReferences(), AnnotationKind.isRelationTarget,
				oml, vocabulary);
		final String forward = getAnnotationValue(object, AnnotationKind.forwardName);
		final String reverse = getAnnotationValue(object, AnnotationKind.reverseName);

		final RelationEntity entity = oml.addRelationEntity(vocabulary, getMappedName(object), sourceIri, targetIri,
				false, false, false, false, false, false, false);
		oml.addForwardRelation(entity, forward);
		if (reverse != null) {
			oml.addReverseRelation(entity, reverse);
		}

		return entity;
	}

	static private <T extends ENamedElement> String getAnnotatedElementIri(Collection<T> coll, AnnotationKind kind,
			OmlWriter oml, Vocabulary vocabulary) {
		final Optional<T> object = coll.stream().filter(i -> isAnnotationSet(i, kind)).findFirst();
		if (object.isPresent()) {
			return getIri(object.get(), vocabulary, oml);
		}
		return null;
	}

	static private boolean isAspect(EClass object) {
		return (object.eIsProxy() || object.isAbstract() || object.isInterface())
				&& object.getESuperTypes().stream().allMatch(i -> isAspect(i));
	}
}
