/**
 * 
 * Copyright 2021 Modelware Solutions and CAE-LIST.
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
package io.opencaesar.ecore2oml.handlers;

import static io.opencaesar.ecore2oml.util.Util.addGeneratedAnnotation;
import static io.opencaesar.ecore2oml.util.Util.getAnnotationValue;
import static io.opencaesar.ecore2oml.util.Util.getIri;
import static io.opencaesar.ecore2oml.util.Util.getMappedName;
import static io.opencaesar.ecore2oml.util.Util.isAnnotationSet;
import static io.opencaesar.ecore2oml.util.Util.memberExists;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;

import io.opencaesar.ecore2oml.AnnotationKind;
import io.opencaesar.ecore2oml.Ecore2Oml;
import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.ecore2oml.preprocessors.ERefGroups;
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
import io.opencaesar.oml.util.OmlBuilder;

public class EReferenceHandler implements ConversionHandler{
	
	private static final String SUBSETS = "subsets";
	
	private String getRelationShipName(EReference eRef,Map<CollectionKind, Object> collections) {
		return getMappedName(eRef,true);
	}
	
	public  EObject doConvert(EObject eObject, Vocabulary vocabulary, OmlBuilder oml,
			Map<CollectionKind, Object> collections,Ecore2Oml visitor) {
		EReference object = (EReference)eObject;
		final String name = getMappedName(object);
		final String entityName =  getRelationShipName(object, collections);
		if (FilterUtil.shouldFilter(object)) {
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
		ERefGroups refGroups = (ERefGroups)collections.get(CollectionKind.RefGroups);
		
		RefCollisionInfo collisionInfo = names!=null ? names.get(name) : null;

		if (opposite!=null && refGroups!=null && refGroups.shouldSkip(object)) {
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
			sourceIri =  collisionInfo.fromAspect.getIri();
			targetIri =  collisionInfo.toAspect.getIri();
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
			final String aspectIri = vocabulary.getNamespace() + aspectName;
			oml.addSpecializationAxiom(vocabulary, getIri(object.getEContainingClass(), vocabulary, oml,visitor), aspectIri);
			sourceIri = aspectIri;
		}
		RelationEntity entity=null;

		if (collisionInfo==null || collisionInfo.entity==null) {
			// if we will reverse then source and target should be flipped
			if (opposite==null && shouldReverse(refGroups, object)) {
				entity = oml.addRelationEntity(vocabulary, entityName, targetIri, sourceIri, isFunctional, isInverseFunctional,
						false, false, false, false, false);
			} else {
				entity = oml.addRelationEntity(vocabulary, entityName, sourceIri, targetIri, isFunctional, isInverseFunctional,
						false, false, false, false, false);
			}
			
			
			if (opposite ==null) {
				if (shouldReverse(refGroups,object)) {
					addReverse(vocabulary, oml, object, object, entity);
				}else {
					addForward(vocabulary, oml, object, name, collisionInfo, entity);
				}
			}
			else {
				addForward(vocabulary, oml, object, name, collisionInfo, entity);
				addReverse(vocabulary, oml, object, opposite, entity);
			}
		}
		
		// forward restriction happen last
		if (collisionInfo!=null) {
			oml.addRelationRangeRestrictionAxiom(vocabulary,
						getIri(object.getEContainingClass(), vocabulary, oml,visitor),
						collisionInfo.forward.getIri(),
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

	private boolean shouldReverse(ERefGroups refGroups, EReference object) {
		if (refGroups!=null) {
			return refGroups.shouldSkip(object);
		}
		return false;
	}

	private void addForward(Vocabulary vocabulary, OmlBuilder oml, EReference object, final String name,
			RefCollisionInfo collisionInfo, RelationEntity entity) {
		ForwardRelation forward = oml.addForwardRelation(entity, name);
		Util.addTitleAnnotationIfNeeded(object, forward, oml, vocabulary);
		Util.addLabelAnnotation(object,forward, oml, vocabulary);
		if (collisionInfo!=null) {
			collisionInfo.entity = entity;
			collisionInfo.forward = forward;
		}
	}

	private void addReverse(Vocabulary vocabulary, OmlBuilder oml, EReference object, final EReference opposite,
			RelationEntity entity) {
		String reverseName = getMappedName(opposite);
		if (isAnnotationSet(object, AnnotationKind.reverseName)) {
			reverseName = getAnnotationValue(object, AnnotationKind.reverseName);
		}
		ReverseRelation reverse = oml.addReverseRelation(entity, reverseName);
		Util.addTitleAnnotationIfNeeded(opposite, reverse, oml, vocabulary);
		Util.addLabelAnnotation(opposite,reverse, oml, vocabulary);
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

	private void handleSubsets(EReference object, RelationEntity entity, OmlBuilder oml, Vocabulary vocabulary,Map<CollectionKind, Object> collections) {
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
	public void postConvert(Vocabulary vocabulary, OmlBuilder oml, Map<CollectionKind, Object> collections,Ecore2Oml visitor) {
		@SuppressWarnings("unchecked")
		Set<EReference> subSets = (Set<EReference>)collections.get(CollectionKind.SubSets);
		if (subSets!=null) {
			subSets.forEach(object -> {
				String subSetRelationName = getRelationShipName(object,collections);
				String subSetIRI = Util.buildIRIFromClassName(object.getEType().getEPackage(), subSetRelationName, visitor.context);
				Collection<EReference> subs = getSubSets(object);
				subs.forEach(superRef -> {
					if (!isFiltered(superRef,collections)) {
						String superSetRelationName = getRelationShipName(superRef, collections);
						String superSetIRI = Util.buildIRIFromClassName(superRef.getEType().getEPackage(), superSetRelationName,visitor.context);
						oml.addSpecializationAxiom(vocabulary, subSetIRI, superSetIRI);
					}
				});
			});
			
		}
		
	}
	
	private Collection<EReference> getSubSets(EReference object) {
		EList<EObject> myRefs = getDirectSubSets(object);
		EList<EObject> otherRefs = getDirectSubSets(object.getEOpposite());
		return mergeExcludingEopposites(myRefs, otherRefs);
	}
	
	private Collection<EReference> mergeExcludingEopposites(EList<EObject> myRefs, EList<EObject> otherRefs) {
		Set<EReference> merged = new HashSet<>();
		myRefs.forEach(ref -> {
			EReference eRef = (EReference)ref; 
			merged.add(eRef);
			if (otherRefs!=null) otherRefs.remove(eRef.getEOpposite());
		});
		if (otherRefs!=null) {
			otherRefs.forEach(ref -> {
				EReference eRef = (EReference)ref; 
				if (!merged.contains(eRef.getEOpposite())) {
					merged.add(eRef);
				}
			});
		}
		return merged;
	}

	private EList<EObject> getDirectSubSets(EReference ref){
		if (ref == null) {
			return null;
		}
		EAnnotation subsetAnnotaion = Util.getAnnotation(ref, SUBSETS);
		if (subsetAnnotaion!=null) {
			return subsetAnnotaion.getReferences();
		}
		return null;
	}

	private boolean isFiltered(EObject object, Map<CollectionKind, Object> collections) {
		@SuppressWarnings("unchecked")
		Set<EObject> filtered = (Set<EObject>)collections.get(CollectionKind.Filtered);
		if (filtered!=null) {
			return filtered.contains(object);
		}
		return false;
	}

	private void handleCardinality(Vocabulary vocabulary, OmlBuilder oml, EReference object, String sourceIri,
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
