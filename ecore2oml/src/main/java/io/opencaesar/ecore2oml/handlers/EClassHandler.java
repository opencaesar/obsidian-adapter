package io.opencaesar.ecore2oml.handlers;

import static io.opencaesar.ecore2oml.util.Util.getIri;
import static io.opencaesar.ecore2oml.util.Util.getMappedName;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

import io.opencaesar.ecore2oml.Ecore2Oml;
import io.opencaesar.ecore2oml.options.Aspect;
import io.opencaesar.ecore2oml.options.RelationshipUtil;
import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.ecore2oml.util.Pair;
import io.opencaesar.ecore2oml.util.Util;
import io.opencaesar.oml.CardinalityRestrictionKind;
import io.opencaesar.oml.Concept;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.InverseSourceRelation;
import io.opencaesar.oml.InverseTargetRelation;
import io.opencaesar.oml.RangeRestrictionKind;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.SourceRelation;
import io.opencaesar.oml.TargetRelation;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlWriter;

public class EClassHandler implements ConversionHandler {

	private static final String ETYPE = "eType";
	private static final String DUPLICATES = "duplicates";
	private static final String REDEFINES = "redefines";
	private static final String CONCEPT_POSTFIX = "_Concept";
	private static final String RELATION_POSTFIX = "_Relation";
	static private Logger LOGGER = LogManager.getLogger(EClassHandler.class);

	@Override
	public EObject doConvert(EObject eObject, Vocabulary vocabulary, OmlWriter oml,
			Map<CollectionKind, Object> collections,Ecore2Oml visitor) {
		EClass object = (EClass) eObject;
		Pair<EReference, EReference> srcAndTarget=null;
		@SuppressWarnings("unchecked")
		Map<EClass,Pair<EReference, EReference>> relationInfo = (Map<EClass,Pair<EReference, EReference>>)collections.get(CollectionKind.RelationShips);
		boolean isRelationship = false;
		boolean isForcedAspect = visitor.context.aspectUtil.basicIsAspect(object);
		if (!isForcedAspect) {
			isRelationship = relationInfo!=null ? relationInfo.containsKey(object) : false;
		}
		
		if (isRelationship) {
			srcAndTarget =  relationInfo.get(object);
			LOGGER.debug(getIri(object, vocabulary, oml,visitor) + "Relationship: " + srcAndTarget.source.getName() + " => " + srcAndTarget.target.getName());
		}
		EAnnotation annotation = Util.getAnnotation(object, DUPLICATES);
		boolean isDuplicate = annotation == null ? false : true;
		Entity entity = null;
		if (isRelationship) {
			entity = convertEClassToRelationEntity(object,srcAndTarget, oml, vocabulary,visitor);
		} else if (Util.defaultsToAspect(object)) {
			entity = oml.addAspect(vocabulary, getMappedName(object));
		} else if (isForcedAspect){
			entity = oml.addAspect(vocabulary, getMappedName(object));
			createSubElementsOfForcedAspect(entity,object,vocabulary,oml,visitor);
		}else {
			entity = oml.addConcept(vocabulary, getMappedName(object));
		}
		for (EClass eSuperType : object.getESuperTypes()) {
			String superIri = getIri(eSuperType, vocabulary, oml,visitor);
			if (superIri != null) {
				oml.addSpecializationAxiom(vocabulary, OmlRead.getIri(entity), superIri);
			}
		}
		if (isDuplicate) {
			handleDuplicate(object, entity, annotation, oml, vocabulary,visitor);
		}
		object.getEStructuralFeatures().stream().forEach(f -> visitor.doSwitch(f));
		return entity;
	}

	private void createSubElementsOfForcedAspect(Entity entity, EClass object, Vocabulary vocabulary, OmlWriter oml, Ecore2Oml visitor) {
		Aspect aspectInfo = visitor.context.aspectUtil.getAspectInfo(object);
		List<EClass> eSuperTypes = object.getESuperTypes().stream().filter(a -> visitor.context.aspectUtil.basicIsAspect(a)).collect(Collectors.toList());
		
		if (aspectInfo.concept!=null && aspectInfo.concept.subConcept) {
			// create the sub class concept
			String name = getMappedName(object) + CONCEPT_POSTFIX;
			Concept conceptEntity = oml.addConcept(vocabulary,name);
			String superIri = OmlRead.getIri(entity) ;
			if (superIri != null) {
				oml.addSpecializationAxiom(vocabulary, OmlRead.getIri(conceptEntity), superIri);
			}
			for (EClass superType : eSuperTypes) {
				superIri = getIri(superType, vocabulary, oml,visitor);
				Aspect superAspectInfo = visitor.context.aspectUtil.getAspectInfo(superIri);
				if (superAspectInfo != null && superAspectInfo.concept !=null && superAspectInfo.concept.subConcept) {
					oml.addSpecializationAxiom(vocabulary, OmlRead.getIri(conceptEntity), superIri + CONCEPT_POSTFIX);
				}
			}
			Util.addLabelAnnotation(conceptEntity, oml, vocabulary);
		}
		if (aspectInfo.relation!=null) {
			String name = getMappedName(object) + RELATION_POSTFIX;
			RelationEntity relEntity = oml.addRelationEntity(vocabulary, name, aspectInfo.relation.from,aspectInfo.relation.to,
					false, false, false, false, false, false, false);
			String superIri = OmlRead.getIri(entity);
			if (superIri != null) {
				oml.addSpecializationAxiom(vocabulary, OmlRead.getIri(relEntity), superIri);
			}
			for (EClass superType : eSuperTypes) {
				superIri = getIri(superType, vocabulary, oml,visitor) ;
				Aspect superAspectInfo = visitor.context.aspectUtil.getAspectInfo(superIri);
				if (superAspectInfo != null && superAspectInfo.relation !=null) {
					oml.addSpecializationAxiom(vocabulary, OmlRead.getIri(relEntity), superIri + RELATION_POSTFIX);
				}
			}
			Util.addLabelAnnotation(relEntity, oml, vocabulary);
		}
	}

	private void handleDuplicate(EClass object, Entity entity, EAnnotation annotation, OmlWriter oml,
			Vocabulary vocabulary,Ecore2Oml e2o) {
		annotation.eContents().forEach(element -> {
			// elements here can be EOperation , ERef, EAttribute, we handle only ERef and EAttribute
			if (element instanceof EReference) {
				handleEReferenceDuplicate(annotation, entity, (EReference) element, oml, vocabulary,e2o);
			} else if (element instanceof EAttribute) {
				handleEAttributeDuplicate(annotation, entity, (EAttribute) element, oml, vocabulary,e2o);
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
			boolean isRef, OmlWriter oml, Vocabulary vocabulary,Ecore2Oml e2o) {
		EStructuralFeature original = (EStructuralFeature) getOriginal(element);
		int upper = element.getUpperBound();
		int lower = element.getLowerBound();
		int oUpper = original.getUpperBound();
		int oLower = original.getLowerBound();
		String entityIRI = OmlRead.getIri(entity);
		String relationOrPropIRI = getIri(original, vocabulary, oml,e2o);
		if (upper != oUpper) {
			if (element instanceof EReference) {
				oml.addRelationCardinalityRestrictionAxiom(vocabulary, entityIRI, relationOrPropIRI,
						CardinalityRestrictionKind.MAX, upper, null);
			} else {
				oml.addScalarPropertyCardinalityRestrictionAxiom(vocabulary, entityIRI, relationOrPropIRI,
						CardinalityRestrictionKind.MAX, upper, null);
			}
		}
		if (lower != oLower) {
			if (element instanceof EReference) {
				oml.addRelationCardinalityRestrictionAxiom(vocabulary, entityIRI, relationOrPropIRI,
						CardinalityRestrictionKind.MIN, lower, null);
			}else {
				oml.addScalarPropertyCardinalityRestrictionAxiom(vocabulary, entityIRI, relationOrPropIRI,
						CardinalityRestrictionKind.MIN, lower, null);
			}
		}
		Object defaultValue = element.getDefaultValue();
		Object oDefaultValue = original.getDefaultValue();
		if (defaultValue!=null && !defaultValue.equals(oDefaultValue) || (oDefaultValue!=null && !oDefaultValue.equals(defaultValue))) {
			// TODO : annotation
			LOGGER.debug(element.getName() + " New Default Value : " + defaultValue);
		}

		EClassifier oType = original.getEType();
		if (type != oType) {
			String rangeIRI = getIri(type, vocabulary, oml,e2o);
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
			Vocabulary vocabulary,Ecore2Oml e2o) {
		handleRetrictions(annotation, entity, element, element.getEType(), false, oml, vocabulary, e2o);
	}

	private void handleEReferenceDuplicate(EAnnotation annotation, Entity entity, EReference element, OmlWriter oml,
			Vocabulary vocabulary,Ecore2Oml e2o) {
		EClassifier type = element.getEType();
		EAnnotation typeAnnotation = Util.getAnnotation(annotation, element.getName());
		if (typeAnnotation != null && typeAnnotation.getDetails()!=null ) {
			String val = typeAnnotation.getDetails().get(ETYPE);
			// UGLY !!
			if (val!=null) {
				String typeName = val.substring(5);
				type = type.getEPackage().getEClassifier(typeName);
			}
		}
		handleRetrictions(annotation, entity, element, type, true, oml, vocabulary, e2o);
	}

	static private RelationEntity convertEClassToRelationEntity(EClass object, Pair<EReference, EReference> srcAndTarget, OmlWriter oml, Vocabulary vocabulary,Ecore2Oml e2o) {
		String classIRI = Util.getIri(object,vocabulary,oml,e2o); 
		final String sourceIri = getIri(srcAndTarget.source.getEType(),vocabulary,oml,e2o);
		final String targetIri = getIri(srcAndTarget.target.getEType(),vocabulary,oml, e2o);
		final RelationEntity entity = oml.addRelationEntity(vocabulary, getMappedName(object), sourceIri, targetIri,
				false, false, false, false, false, false, false);
		Util.setSemanticFlags(classIRI, entity);
		
		String forwardName = RelationshipUtil.getInstance().getForwardName(object,classIRI);
		if (!forwardName.isEmpty()) {
			oml.addForwardRelation(entity, forwardName);
			LOGGER.debug(Util.getIri(object,vocabulary,oml,e2o) + " => (forward) => " + forwardName);
		}
		String reverseName = RelationshipUtil.getInstance().getReverseName(object,classIRI);
		if (!reverseName.isEmpty()) {
			oml.addReverseRelation(entity, reverseName);
			LOGGER.debug(Util.getIri(object,vocabulary,oml,e2o) + " => (reverse) => " + reverseName);
		}
		if (object == srcAndTarget.source.getEContainingClass()) {
			String sourceName = getMappedName(srcAndTarget.source);
			if (!sourceName.isEmpty()) {
				SourceRelation sourceRelation = oml.addSourceRelation(entity, sourceName);
				Util.addTitleAnnotationIfNeeded(srcAndTarget.source, sourceRelation, oml, vocabulary);
				Util.addLabelAnnotation(srcAndTarget.source, sourceRelation, oml, vocabulary);
				LOGGER.debug(Util.getIri(object,vocabulary,oml,e2o) + " => (source) => " + sourceName);
			}
			if (srcAndTarget.source.getEOpposite() != null) {
				String inverseSourceName = getMappedName(srcAndTarget.source.getEOpposite());
				if (!inverseSourceName.isEmpty()) {
					InverseSourceRelation rel = oml.addInverseSourceRelation(entity, inverseSourceName);
					Util.addTitleAnnotationIfNeeded(srcAndTarget.source.getEOpposite(), rel, oml, vocabulary);
					Util.addLabelAnnotation(srcAndTarget.source.getEOpposite(), rel, oml, vocabulary);
					LOGGER.debug(Util.getIri(object,vocabulary,oml,e2o) + " => (inverse source) => " + inverseSourceName);
				}
			}
		}
		if (object == srcAndTarget.target.getEContainingClass()) {
			String targetName = getMappedName(srcAndTarget.target);
			if (!targetName.isEmpty()) {
				TargetRelation targetRelation = oml.addTargetRelation(entity, targetName);
				Util.addTitleAnnotationIfNeeded(srcAndTarget.target, targetRelation, oml, vocabulary);
				Util.addLabelAnnotation(srcAndTarget.target, targetRelation, oml, vocabulary);
				LOGGER.debug(Util.getIri(object,vocabulary,oml,e2o) + " => (target) => " + targetName);
			}
			if (srcAndTarget.target.getEOpposite() != null) {
				String inverseTargetName = getMappedName(srcAndTarget.target.getEOpposite());
				if (!inverseTargetName.isEmpty()) {
					InverseTargetRelation rel = oml.addInverseTargetRelation(entity, inverseTargetName);
					Util.addTitleAnnotationIfNeeded(srcAndTarget.target.getEOpposite(), rel, oml, vocabulary);
					Util.addLabelAnnotation(srcAndTarget.target.getEOpposite(), rel, oml, vocabulary);
					LOGGER.debug(Util.getIri(object,vocabulary,oml,e2o) + " => (inverse target) => " + inverseTargetName);
				}
			}
		}
		return entity;
	}
}
