package io.opencaesar.ecore2oml.handlers;

import static io.opencaesar.ecore2oml.util.Util.getIri;
import static io.opencaesar.ecore2oml.util.Util.getMappedName;

import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

import io.opencaesar.ecore2oml.Ecore2Oml;
import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.ecore2oml.util.Pair;
import io.opencaesar.ecore2oml.util.Relationship;
import io.opencaesar.ecore2oml.util.RelationshipUtil;
import io.opencaesar.ecore2oml.util.Util;
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
	private static final String SUBSETS = "subsets";

	static private Logger LOGGER = LogManager.getLogger(EClassHandler.class);

	@Override
	public EObject doConvert(EObject eObject, Vocabulary vocabulary, OmlWriter oml,
			Map<CollectionKind, Object> collections,Ecore2Oml visitor) {
		EClass object = (EClass) eObject;
		Pair<EReference, EReference> srcAndTarget=null;
		boolean isRelationship = RelationshipUtil.getInstance().isRelationship(object, oml, vocabulary,visitor);
		if (isRelationship) {
			srcAndTarget =  getSourceAndTaregt( object, oml, vocabulary,visitor);
			LOGGER.info(getIri(object, vocabulary, oml,visitor) + "Relationship: " + srcAndTarget.source.getName() + " => " + srcAndTarget.target.getName());
			/*if (srcAndTarget.source.getUpperBound()!=1 ||
				srcAndTarget.target.getUpperBound()!=1) {
				isRelationship = false;
				LOGGER.debug(getIri(object, vocabulary, oml,visitor) + " Not  Relationship");
			}*/
		}
		EAnnotation annotation = Util.getAnnotation(object, DUPLICATES);
		boolean isDuplicate = annotation == null ? false : true;
		Entity entity = null;
		if (isRelationship) {
			entity = convertEClassToRelationEntity(object,srcAndTarget, oml, vocabulary,visitor);
			visitor.addConverted(srcAndTarget.source);
			visitor.addConverted(srcAndTarget.target);
		} else if (isAspect(object)) {
			entity = oml.addAspect(vocabulary, getMappedName(object));
		} else {
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

	private void handleDuplicate(EClass object, Entity entity, EAnnotation annotation, OmlWriter oml,
			Vocabulary vocabulary,Ecore2Oml e2o) {
		annotation.eContents().forEach(element -> {
			// elements here can be EOperation , ERef, EAttribute, we handle only ERef and
			// EAttribute
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
			LOGGER.info(element.getName() + " New Default Value : " + defaultValue);
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
		if (element.getEOpposite() != null) {
			EAnnotation typeAnnotation = Util.getAnnotation(annotation, element.getName());
			if (typeAnnotation != null) {
				String val = typeAnnotation.getDetails().get(ETYPE);
				// UGLY !!
				String typeName = val.substring(5);
				type = type.getEPackage().getEClassifier(typeName);
			}
		}
		handleRetrictions(annotation, entity, element, type, true, oml, vocabulary, e2o);
	}

	static private RelationEntity convertEClassToRelationEntity(EClass object, Pair<EReference, EReference> srcAndTarget, OmlWriter oml, Vocabulary vocabulary,Ecore2Oml e2o) {
		String classIRI = Util.getIri(object,vocabulary,oml,e2o); 
		String forwardName = RelationshipUtil.getInstance().getForwardName(object,classIRI);
		final String sourceIri = getIri(srcAndTarget.source.getEType(),vocabulary,oml,e2o);
		final String targetIri = getIri(srcAndTarget.target.getEType(),vocabulary,oml, e2o);
		final RelationEntity entity = oml.addRelationEntity(vocabulary, getMappedName(object), sourceIri, targetIri,
				false, false, false, false, false, false, false);
		oml.addForwardRelation(entity, forwardName);
		if (srcAndTarget.source.getEOpposite()!=null) {
			String reverseName = RelationshipUtil.getInstance().getReverseName(object,classIRI);
			oml.addReverseRelation(entity, reverseName);
			LOGGER.debug(Util.getIri(object,vocabulary,oml,e2o) + " => " + forwardName + " - " + reverseName);
		}else {
			LOGGER.debug(Util.getIri(object,vocabulary,oml,e2o) + " => " + forwardName);
		}
		Util.setSemanticFlags(classIRI, entity);
		return entity;
	}

	
	
	private static Pair<EReference, EReference> getSourceAndTaregt(EClass object, OmlWriter oml,
			Vocabulary vocabulary,Ecore2Oml e2o) {
		Pair<EReference, EReference> state = new Pair<>();
		boolean isRelationShip = RelationshipUtil.getInstance().isRelationship(object,  oml,vocabulary,e2o);
		if (isRelationShip) {
			Relationship info = RelationshipUtil.getInstance().getInfo(object, oml, vocabulary,e2o);
			EList<EReference> refs = object.getEReferences();
			for (EReference ref : refs) {
				if (ref.getName().equals(info.source)) {
					info.source = (ref.getName());
					state.source = ref;
				}else if (ref.getName().equals(info.target)) {
					info.target = (ref.getName());
					state.target = ref;
				}else {
					// check using sub sets
					checkSubsets(ref, state, info);
				}
				if (state.source !=null && state.target!=null) {
					break;
				}
			}
		}
		if (state.source==null && state.target==null) {
			// if we could not find source and target we need to walk up the hierarchy 
			EList<EClass> supers = object.getESuperTypes();
			for (EClass eClass : supers) {
				state =  getSourceAndTaregt( eClass, oml, vocabulary,e2o);
				if (state.source !=null && state.target!=null) {
					break;
				}
			}
		}
		return state;
	}

	static private void checkSubsets(EReference object, Pair<EReference, EReference> state, Relationship info) {
		EAnnotation subsetAnnotaion = Util.getAnnotation(object, SUBSETS);
		if (subsetAnnotaion!=null) {
			subsetAnnotaion.getReferences().forEach(superSet -> {
				EReference superRef = (EReference)superSet;
				if (superRef.getName().equals(info.source)) {
					info.source = (object.getName());
					state.source = object;
					return;
				}else if (superRef.getName().equals(info.target)) {
					info.target = (object.getName());
					state.target = object;
					return;
				}
			});

		}
	}


	static private boolean isAspect(EClass object) {
		return (object.eIsProxy() || object.isAbstract() || object.isInterface())
				&& object.getESuperTypes().stream().allMatch(i -> isAspect(i));
	}
}
