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
package io.opencaesar.ecore2oml.preprocessors.participants;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;

import io.opencaesar.ecore2oml.ConversionContext;
import io.opencaesar.ecore2oml.options.Relationship;
import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.ecore2oml.preprocessors.ERefGroups;
import io.opencaesar.ecore2oml.util.Pair;
import io.opencaesar.ecore2oml.util.Util;

public class EClassConversionParticipant extends ConversionParticipant {

	private static final String SUBSETS = "subsets";
	private static final String DUPLICATES = "duplicates";
	
	@Override
	public void handle(EObject element, Map<CollectionKind, Object> collections) {
		EClass object = (EClass) element;
		if (!context.aspectUtil.isAspect(object, context) && context.relationUtil.isRelationship(object, context)) {
			Pair<EReference, EReference> srcAndTarget = getSourceAndTaregt(object, context);
			addRelation(object, srcAndTarget, collections);
		}
		
		handleRedefineAndDublicates(object,collections);
	}

	private void handleRedefineAndDublicates(EClass object, Map<CollectionKind, Object> collections) {
		EAnnotation annotation = Util.getAnnotation(object, DUPLICATES);
		ERefGroups refGroups = (ERefGroups)collections.get(CollectionKind.RefGroups);
		if (refGroups==null) {
			refGroups = new ERefGroups();
			collections.put(CollectionKind.RefGroups, refGroups);
		}
		
		if (annotation!=null) {
			EList<EObject> contents = annotation.eContents();
			for(EObject element : contents) {
				// elements here can be EOperation , ERef, EAttribute, we handle only ERef
				if (element instanceof EReference) {
					handleEReferenceDuplicate(object, (EReference) element,refGroups);
				} 
			}
		}
	}

	private void handleEReferenceDuplicate(EClass eClass, EReference element, ERefGroups refGroups) {
		EList<EClass> supers = eClass.getEAllSuperTypes();
		for (int index = supers.size() -1 ; index >=0 ; index-- ) {
			EClass superClass = supers.get(index);
			EReference superRef = getRefByName(superClass, element.getName());
			if (superRef!=null) {
				refGroups.add(element,superRef);
			}
		}
	}

	private EReference getRefByName(EClass superClass, String name) {
		EReference retVal  = getFetaureByNameFromDuplicate(superClass,name);
		if (retVal==null) {
			EList<EReference> refs = superClass.getEReferences();
			for (EReference ref : refs) {
				if (ref.getName().equals(name)) {
					return ref;
				}
			}
		}
		return null;
	}

	private EReference getFetaureByNameFromDuplicate(EClass superClass, String name) {
		EAnnotation annotation = Util.getAnnotation(superClass, DUPLICATES);
		if (annotation!=null) {
			EList<EObject> contents = annotation.eContents();
			for (EObject element : contents) {
				// elements here can be EOperation , ERef, EAttribute, we handle only ERef
				if (element instanceof EReference) {
					EReference ref= (EReference)element;
					if (ref.getName().equals(name)) {
						return ref;
					}
				} 
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private void addRelation(EClass object, Pair<EReference, EReference> srcAndTarget,
			Map<CollectionKind, Object> collections) {
		Map<EClass,Pair<EReference, EReference>> relationInfo = (Map<EClass,Pair<EReference, EReference>>)collections.get(CollectionKind.RelationShips);
		if (relationInfo==null) {
			relationInfo = new HashMap<>();
			collections.put(CollectionKind.RelationShips, relationInfo);
		}
		relationInfo.put(object, srcAndTarget);		
	}

	@Override
	public void postProcess(Map<CollectionKind, Object> collections) {
		context.aspectUtil.populateSuperClasses(context);
	}

	private static Pair<EReference, EReference> getSourceAndTaregt(EClass object, ConversionContext context) {
		Pair<EReference, EReference> state = new Pair<>();
		boolean isRelationShip = context.relationUtil.isRelationship(object, context);
		if (isRelationShip) {
			Relationship info =context.relationUtil.getInfo(Util.getLocalEClassIri(object,context));
			EList<EReference> refs = object.getEReferences();
			for (EReference ref : refs) {
				if (ref.getName().equals(info.source)) {
					info.source = (ref.getName());
					state.source = ref;
				} else if (ref.getName().equals(info.target)) {
					info.target = (ref.getName());
					state.target = ref;
				} else {
					// check using sub sets
					checkSubsets(ref, state, info);
				}
				if (state.source != null && state.target != null) {
					break;
				}
			}
		}
		if (state.source == null && state.target == null) {
			// if we could not find source and target we need to walk up the hierarchy
			EList<EClass> supers = object.getESuperTypes();
			for (EClass eClass : supers) {
				state = getSourceAndTaregt(eClass, context);
				if (state.source != null && state.target != null) {
					break;
				}
			}
		}
		return state;
	}

	static private void checkSubsets(EReference object, Pair<EReference, EReference> state, Relationship info) {
		EAnnotation subsetAnnotaion = Util.getAnnotation(object, SUBSETS);
		if (subsetAnnotaion != null) {
			subsetAnnotaion.getReferences().forEach(superSet -> {
				EReference superRef = (EReference) superSet;
				if (superRef.getName().equals(info.source)) {
					info.source = (object.getName());
					state.source = object;
					return;
				} else if (superRef.getName().equals(info.target)) {
					info.target = (object.getName());
					state.target = object;
					return;
				}
			});

		}
	}

}
