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
package io.opencaesar.ecore2oml.preprocessors;

import static io.opencaesar.ecore2oml.util.Util.getMappedName;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;

import io.opencaesar.ecore2oml.preprocessors.participants.ConversionParticipant;
import io.opencaesar.ecore2oml.util.FilterUtil;

public class EAttributeConversionParticipant extends ConversionParticipant {

	private Logger LOGGER = LogManager.getLogger(EAttributeConversionParticipant.class);

	@SuppressWarnings("unchecked")
	@Override
	public void handle(EObject element, Map<CollectionKind, Object> collections) {
		// EAttribute
		EAttribute eAttr = (EAttribute) element;
		final String name = getMappedName(eAttr);
		if (FilterUtil.shouldFilter(eAttr)) {
			return;
		}

		Map<String, CollisionInfo> names = (Map<String, CollisionInfo>) collections
				.get(CollectionKind.CollidingAttributes);
		if (names == null) {
			names = new HashMap<>();
			collections.put(CollectionKind.CollidingAttributes, names);
		}
		CollisionInfo info = names.get(name);
		if (info == null) {
			info = new CollisionInfo(name);
			names.put(name, info);
		}
		info.add(eAttr);
	}

	@Override
	public void postProcess(Map<CollectionKind, Object> collections) {
		LOGGER.debug("Post Processing");
		@SuppressWarnings("unchecked")
		Map<String, CollisionInfo> names = (Map<String, CollisionInfo>) collections
				.get(CollectionKind.CollidingAttributes);
		if (names == null) {
			return;
		}
		int before = names.size();
		Set<String> cleanMe = new HashSet<>();
		if (names != null) {
			Set<Entry<String, CollisionInfo>> entries = names.entrySet();
			for (Entry<String, CollisionInfo> entry : entries) {
				String name = entry.getKey();
				CollisionInfo info = entry.getValue();
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

}
