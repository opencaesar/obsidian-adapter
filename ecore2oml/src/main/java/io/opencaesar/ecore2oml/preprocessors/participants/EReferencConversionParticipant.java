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

import static io.opencaesar.ecore2oml.util.Util.getMappedName;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;

import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.ecore2oml.preprocessors.ERefGroups;
import io.opencaesar.ecore2oml.preprocessors.RefCollisionInfo;
import io.opencaesar.ecore2oml.util.FilterUtil;

public class EReferencConversionParticipant extends ConversionParticipant {

	private Logger LOGGER = LogManager.getLogger(EReferencConversionParticipant.class);

	@Override
	public void handle(EObject element, Map<CollectionKind, Object> collections) {
		EReference eRef = (EReference) element;
		final String name = getMappedName(eRef);
		if (FilterUtil.shouldFilter(eRef)) {
			return;
		}
		ERefGroups groups = (ERefGroups)collections.get(CollectionKind.RefGroups);
		if (groups==null) {
			groups = new ERefGroups();
			collections.put(CollectionKind.RefGroups, groups);
		}
		groups.addERef(eRef);
		if (eRef.getEOpposite() == null) {
			_handleEReference(collections, eRef, name);
		} 
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
		ERefGroups groups = (ERefGroups)collections.get(CollectionKind.RefGroups);
		if (groups!=null) {
			groups.finish();
		}
	}

}
