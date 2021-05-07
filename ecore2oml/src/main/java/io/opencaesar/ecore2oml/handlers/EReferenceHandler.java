package io.opencaesar.ecore2oml.handlers;

import static io.opencaesar.ecore2oml.util.Util.addGeneratedAnnotation;
import static io.opencaesar.ecore2oml.util.Util.getAnnotationValue;
import static io.opencaesar.ecore2oml.util.Util.getIri;
import static io.opencaesar.ecore2oml.util.Util.getMappedName;
import static io.opencaesar.ecore2oml.util.Util.isAnnotationSet;
import static io.opencaesar.ecore2oml.util.Util.memberExists;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;

import io.opencaesar.ecore2oml.AnnotationKind;
import io.opencaesar.ecore2oml.Ecore2Oml;
import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.ecore2oml.preprocessors.CollidingEOppositeData;
import io.opencaesar.ecore2oml.preprocessors.RefCollisionInfo;
import io.opencaesar.ecore2oml.util.Constants;
import io.opencaesar.ecore2oml.util.FilterUtil;
import io.opencaesar.ecore2oml.util.Util;
import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.CardinalityRestrictionKind;
import io.opencaesar.oml.ForwardRelation;
import io.opencaesar.oml.RangeRestrictionKind;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.ReverseRelation;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlWriter;

public class EReferenceHandler implements ConversionHandler{
	
	private static final String SUBSETS = "subsets";
	
	private String getRelationShipName(EReference eRef) {
		return  getMappedName(eRef,true);
	}
	
	public  EObject doConvert(EObject eObject, Vocabulary vocabulary, OmlWriter oml,
			Map<CollectionKind, Object> collections,Ecore2Oml visitor) {
		EReference object = (EReference)eObject;
		final String name = getMappedName(object);
		final String entityName =  getRelationShipName(object);
		@SuppressWarnings("unchecked")
		Set<EReference> skipSet = (Set<EReference>)collections.get(CollectionKind.SKIP_EREFERENCES);
		if (skipSet==null) skipSet = Collections.emptySet();
		if (FilterUtil.shouldFilter(object)  || skipSet.contains(object)) {
			addFiltered(object,collections);
			return null;
		}
		
		String sourceIri = getIri(object.getEContainingClass(), vocabulary, oml,visitor);
		String targetIri = getIri(object.getEReferenceType(), vocabulary, oml,visitor);
		final EReference opposite = object.getEOpposite();
		final boolean isFunctional = object.getUpperBound() == 1;
		final boolean isInverseFunctional = (opposite != null) && opposite.getLowerBound() == 1;
		@SuppressWarnings("unchecked")
		Map<String, RefCollisionInfo> names = (Map<String, RefCollisionInfo>) collections.get(CollectionKind.CollidingRefernces);
		CollidingEOppositeData collidingEOpposite = (CollidingEOppositeData) collections.get(CollectionKind.CollidingEOppositeRefernces);
		
		RefCollisionInfo collisionInfo = names!=null ? names.get(name) : null;


		if (opposite!=null && collidingEOpposite!=null &&  collidingEOpposite.shouldSkip(object)) {
			addFiltered(object,collections);
			return null;
		}
		
		String refIRI = getIri(object, vocabulary, oml,visitor);
		String rangeIRI = getIri(object.getEType(), vocabulary, oml,visitor);
		
		if (collisionInfo!=null) {
			// create the base source, and target
			if (collisionInfo.fromAspect==null) {
				String baseNameFrom = Constants.BASE_PREFIX + entityName + Constants.FROM;
				collisionInfo.fromAspect = oml.addAspect(vocabulary, baseNameFrom);
				addGeneratedAnnotation(collisionInfo.fromAspect, oml, vocabulary);
				String baseNameTo = Constants.BASE_PREFIX + entityName + Constants.To;
				collisionInfo.toAspect = oml.addAspect(vocabulary, baseNameTo);
				addGeneratedAnnotation(collisionInfo.toAspect, oml, vocabulary);
			}
			sourceIri =  OmlRead.getIri(collisionInfo.fromAspect);
			targetIri =  OmlRead.getIri(collisionInfo.toAspect);
			oml.addSpecializationAxiom(vocabulary, getIri(object.getEContainingClass(), vocabulary, oml,visitor), sourceIri);
			oml.addSpecializationAxiom(vocabulary, getIri(object.getEReferenceType(), vocabulary, oml,visitor), targetIri);
		}
		
		
		
		// the relation entity's source
		if (isAnnotationSet(object, AnnotationKind.domainName)) {
			final String aspectName = getAnnotationValue(object, AnnotationKind.domainName);
			if (!memberExists(aspectName, vocabulary)) {
				final Aspect aspect = oml.addAspect(vocabulary, aspectName);
				addGeneratedAnnotation(aspect, oml, vocabulary);
			}
			final String aspectIri = OmlRead.getNamespace(vocabulary) + aspectName;
			oml.addSpecializationAxiom(vocabulary, getIri(object.getEContainingClass(), vocabulary, oml,visitor), aspectIri);
			sourceIri = aspectIri;
		}
		RelationEntity entity=null;

		if (collisionInfo==null || collisionInfo.entity==null) {
			entity = oml.addRelationEntity(vocabulary, entityName, sourceIri, targetIri, isFunctional, isInverseFunctional,
					false, false, false, false, false);
			
			// the forward relation
			ForwardRelation forward = oml.addForwardRelation(entity, name);
			Util.addTitleAnnotationIfNeeded(object, forward, oml, vocabulary);
			Util.addLabelAnnotation(object,forward, oml, vocabulary);
			if (collisionInfo!=null) {
				collisionInfo.entity = entity;
				collisionInfo.forward = forward;
			}
			// the reverse relation
			if (opposite != null) {
				String reverseName = getMappedName(opposite);
				if (isAnnotationSet(object, AnnotationKind.reverseName)) {
					reverseName = getAnnotationValue(object, AnnotationKind.reverseName);
				}
				ReverseRelation reverse = oml.addReverseRelation(entity, reverseName);
				Util.addTitleAnnotationIfNeeded(opposite, reverse, oml, vocabulary);
				Util.addLabelAnnotation(opposite,reverse, oml, vocabulary);
			}
		}
		
		// forward restriction happen last
		if (collisionInfo!=null) {
			oml.addRelationRangeRestrictionAxiom(vocabulary,
						getIri(object.getEContainingClass(), vocabulary, oml,visitor),
					   OmlRead.getIri(collisionInfo.forward),
					   getIri(object.getEReferenceType(), vocabulary, oml,visitor),
					   RangeRestrictionKind.ALL);
		}
		handleCardinality(vocabulary, oml, object, sourceIri, refIRI, rangeIRI);
		handleSubsets(object, entity,oml,vocabulary,collections);
		Util.setSemanticFlags(Util.getIri(object,visitor.context), entity,visitor.context);
		if (opposite!=null) {
			Util.setSemanticFlags(Util.getIri(opposite, visitor.context), entity,false,visitor.context);
		}
		return entity;
	}

	private void addFiltered(EReference object, Map<CollectionKind, Object> collections) {
		@SuppressWarnings("unchecked")
		Set<EObject> filtered = (Set<EObject>)collections.get(CollectionKind.Filtered);
		if (filtered==null) {
			filtered = new HashSet<EObject>();
			collections.put(CollectionKind.Filtered, filtered);
		}
		filtered.add(object);
	}

	private void handleSubsets(EReference object, RelationEntity entity, OmlWriter oml, Vocabulary vocabulary,Map<CollectionKind, Object> collections) {
		EAnnotation subsetAnnotaion = Util.getAnnotation(object, SUBSETS);
		if (subsetAnnotaion!=null) {
			@SuppressWarnings("unchecked")
			Set<EReference> subSets = (Set<EReference>)collections.get(CollectionKind.SubSets);
			if (subSets==null) {
				subSets = new LinkedHashSet<EReference>();
				collections.put(CollectionKind.SubSets, subSets);
			}
			subSets.add(object);
		}
	}
	
	@Override
	public void postConvert(Vocabulary vocabulary, OmlWriter oml, Map<CollectionKind, Object> collections,Ecore2Oml visitor) {
		@SuppressWarnings("unchecked")
		Set<EReference> subSets = (Set<EReference>)collections.get(CollectionKind.SubSets);
		if (subSets!=null) {
			subSets.forEach(object -> {
				EAnnotation subsetAnnotaion = Util.getAnnotation(object, SUBSETS);
				String subSetRelationName = getRelationShipName(object);
				String subSetIRI = Util.buildIRIFromClassName(object.getEType().getEPackage(), subSetRelationName, visitor.context);
				if (subsetAnnotaion!=null) {
					subsetAnnotaion.getReferences().forEach(superSet -> {
						EReference superRef = (EReference)superSet;
						if (!isFiltered(superRef,collections)) {
							String superSetRelationName = getRelationShipName(superRef);
							String superSetIRI = Util.buildIRIFromClassName(superRef.getEType().getEPackage(), superSetRelationName,visitor.context);
							oml.addSpecializationAxiom(vocabulary, subSetIRI, superSetIRI);
						}
					});
				}
			});
			
		}
		
	}
	


	private boolean isFiltered(EObject object, Map<CollectionKind, Object> collections) {
		@SuppressWarnings("unchecked")
		Set<EObject> filtered = (Set<EObject>)collections.get(CollectionKind.Filtered);
		if (filtered!=null) {
			return filtered.contains(object);
		}
		return false;
	}

	private void handleCardinality(Vocabulary vocabulary, OmlWriter oml, EReference object, String sourceIri,
			String refIRI, String rangeIRI) {
		if (object.getUpperBound()>1) {
			oml.addRelationCardinalityRestrictionAxiom(vocabulary, sourceIri,  refIRI,
					CardinalityRestrictionKind.MAX, object.getUpperBound(), null);
		}
		if (object.getLowerBound()> 0) {
			oml.addRelationCardinalityRestrictionAxiom(vocabulary, sourceIri,  refIRI,
					CardinalityRestrictionKind.MIN, object.getLowerBound(), null);
		}
	}

}
