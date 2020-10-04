package io.opencaesar.ecore2oml.preprocessors;

import static io.opencaesar.ecore2oml.Util.getMappedName;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;

import io.opencaesar.ecore2oml.FilterUtil;

public class EReferencConversionParticipant extends ConversionParticipant {

	private Logger LOGGER = LogManager.getLogger(EReferencConversionParticipant.class);

	@Override
	public void handle(EObject element, Map<CollectionKind, Object> collections) {
		EReference eRef = (EReference) element;
		final String name = getMappedName(eRef);
		if (FilterUtil.shouldFilter(eRef)) {
			return;
		}
		if (eRef.getEOpposite() == null) {
			_handleEReference(collections, eRef, name);
		} else {
			_handleEOppositeRefernces(collections, eRef, name);
		}
	}

	private void _handleEOppositeRefernces(Map<CollectionKind, Object> collections, EReference eRef, String name) {
		CollidingEOppositeData collInfo = (CollidingEOppositeData) collections
				.get(CollectionKind.CollidingEOppositeRefernces);
		if (collInfo == null) {
			collInfo = new CollidingEOppositeData();
			collections.put(CollectionKind.CollidingEOppositeRefernces, collInfo);
		}
		collInfo.addForward(eRef);
		collInfo.addReverse(eRef);
		LOGGER.debug(name);
	}

	private void _handleEReference(Map<CollectionKind, Object> collections, EReference eRef, final String name) {
		@SuppressWarnings("unchecked")
		Map<String, RefCollisionInfo> names = (Map<String, RefCollisionInfo>) collections
				.get(CollectionKind.CollidingRefernces);
		if (names == null) {
			names = new HashMap<>();
			collections.put(CollectionKind.CollidingRefernces, names);
		}
		RefCollisionInfo info = names.get(name);
		if (info == null) {
			info = new RefCollisionInfo(name);
			names.put(name, info);
		}
		info.add(eRef);
		LOGGER.debug(name);
	}

	private void _postProcessEOpposite(Map<CollectionKind, Object> collections) {
		LOGGER.info("Post Processing : EOpposite");
		CollidingEOppositeData collInfo = (CollidingEOppositeData) collections
				.get(CollectionKind.CollidingEOppositeRefernces);
		if (collInfo == null) {
			return;
		}
		int before = collInfo.size();
		Set<TwoERefKey> cleanMe = new HashSet<>();
		collInfo.finish();
		Set<Entry<TwoERefKey, Set<EReference>>> enteries = collInfo.forward.entrySet();
		for (Entry<TwoERefKey, Set<EReference>> entry : enteries) {
			TwoERefKey key = entry.getKey();
			Set<EReference> val = entry.getValue();
			if (val.size() > 1) {
				LOGGER.debug(getMappedName(key.forward) + " ==> " + val.size());
			} else {
				cleanMe.add(key);
			}
		}

		for (TwoERefKey key : cleanMe) {
			collInfo.forward.remove(key);
		}
		LOGGER.debug("Size befoe = " + before + ", Size after = " + collInfo.size());

	}

	private void _postProcessEReference(Map<CollectionKind, Object> collections) {
		LOGGER.debug("Post Processing : EREFERNCE");
		@SuppressWarnings("unchecked")
		Map<String, RefCollisionInfo> names = (Map<String, RefCollisionInfo>) collections
				.get(CollectionKind.CollidingRefernces);
		if (names == null) {
			return;
		}
		int before = names.size();
		Set<String> cleanMe = new HashSet<>();
		if (names != null) {
			Set<Entry<String, RefCollisionInfo>> entries = names.entrySet();
			for (Entry<String, RefCollisionInfo> entry : entries) {
				String name = entry.getKey();
				RefCollisionInfo info = entry.getValue();
				info.finish();
				if (info.size() > 1) {
					LOGGER.debug(name + " ==> " + info);
				} else {
					cleanMe.add(name);
				}
			}

		}
		for (String name : cleanMe) {
			names.remove(name);
		}
		LOGGER.debug("Size befoe = " + before + ", Size after = " + names.size());
	}

	@Override
	public void postProcess(Map<CollectionKind, Object> collections) {
		_postProcessEReference(collections);
		_postProcessEOpposite(collections);
	}

}
