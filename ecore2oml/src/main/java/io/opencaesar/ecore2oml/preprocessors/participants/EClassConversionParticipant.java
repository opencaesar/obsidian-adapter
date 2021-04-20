package io.opencaesar.ecore2oml.preprocessors.participants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;

import io.opencaesar.ecore2oml.options.AspectUtil;
import io.opencaesar.ecore2oml.options.Relationship;
import io.opencaesar.ecore2oml.options.RelationshipUtil;
import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.ecore2oml.util.Pair;
import io.opencaesar.ecore2oml.util.Util;

public class EClassConversionParticipant extends ConversionParticipant {

	private static final String SUBSETS = "subsets";

	@Override
	public void handle(EObject element, Map<CollectionKind, Object> collections) {
		EClass object = (EClass) element;
		if (AspectUtil.getInstance().isAspect(object)) {
			addAspect(object,collections);
		} else if (RelationshipUtil.getInstance().isRelationship(object)) {
			Pair<EReference, EReference> srcAndTarget = getSourceAndTaregt(object);
			addRelation(object, srcAndTarget, collections);
		}
	}

	private void addAspect(EClass object, Map<CollectionKind, Object> collections) {
				
	}

	@SuppressWarnings("unchecked")
	private void addRelation(EClass object, Pair<EReference, EReference> srcAndTarget,
			Map<CollectionKind, Object> collections) {
		Map<EClass,Pair<EReference, EReference>> relationInfo = (Map<EClass,Pair<EReference, EReference>>)collections.get(CollectionKind.RelationShips);
		Set<EReference> skipSet = (Set<EReference>)collections.get(CollectionKind.SKIP_EREFERENCES);
		if (relationInfo==null) {
			relationInfo = new HashMap<>();
			collections.put(CollectionKind.RelationShips, relationInfo);
			skipSet = new HashSet<>();
			collections.put(CollectionKind.SKIP_EREFERENCES, skipSet);
		}
		relationInfo.put(object, srcAndTarget);		
		addFilteredRefs(skipSet, srcAndTarget.source);
		addFilteredRefs(skipSet, srcAndTarget.target);
		
	}

	private void addFilteredRefs(Set<EReference> skipSet, EReference src) {
		skipSet.add(src);
		if (src.getEOpposite()!=null) {
			skipSet.add(src.getEOpposite());
		}
	}

	@Override
	public void postProcess(Map<CollectionKind, Object> collections) {
		AspectUtil.getInstance().populateSuperClasses();
	}

	private static Pair<EReference, EReference> getSourceAndTaregt(EClass object) {
		Pair<EReference, EReference> state = new Pair<>();
		boolean isRelationShip = RelationshipUtil.getInstance().isRelationship(object);
		if (isRelationShip) {
			Relationship info = RelationshipUtil.getInstance().getInfo(Util.getLocalEClassIri(object));
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
				state = getSourceAndTaregt(eClass);
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
