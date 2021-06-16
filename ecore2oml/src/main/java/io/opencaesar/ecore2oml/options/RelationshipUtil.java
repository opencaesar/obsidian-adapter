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
package io.opencaesar.ecore2oml.options;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.xtext.xbase.lib.StringExtensions;

import io.opencaesar.ecore2oml.ConversionContext;
import io.opencaesar.ecore2oml.Ecore2Oml;
import io.opencaesar.ecore2oml.util.Constants;
import io.opencaesar.ecore2oml.util.Util;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlBuilder;

public class RelationshipUtil {

	private Map<String, Relationship> relationShips = new HashMap<>();
	private Map<String, OverrideInfo> overrides = new HashMap<>();

	public void init(List<Relationship> relationships) {
		for (Relationship relation : relationships) {
			addRelationship(relation.root, relation);
			if (relation.overrides != null) {
				for (OverrideInfo override : relation.overrides) {
					overrides.put(override.iri, override);
				}
			}
		}
	}

	public boolean isRelationship(EClass toCheck, ConversionContext context) {
		String[] matched = new String[1];
		Relationship[] matchedInfo = new Relationship[1];
		boolean bRetVal = _isRelationship(toCheck, matched, matchedInfo, context);
		if (bRetVal) {
			String iri = Util.getLocalEClassIri(toCheck, context);
			if (!relationShips.containsKey(iri)) {
				addRelationship(matchedInfo[0], iri,
						new Relationship(iri, matchedInfo[0].source, matchedInfo[0].target));
			}
		}
		return bRetVal;
	}

	public boolean _isRelationship(EClass toCheck, String[] matchedIRI, Relationship[] matchedInfo, ConversionContext context) {
		boolean bRetVal = false;
		String iri = Util.getLocalEClassIri(toCheck, context);
		if (relationShips.containsKey(iri)) {
			matchedIRI[0] = iri;
			matchedInfo[0] = relationShips.get(iri);
			return true;
		}
		EList<EClass> superTypes = toCheck.getEAllSuperTypes();
		for (EClass superType : superTypes) {
			bRetVal |= _isRelationship(superType, matchedIRI, matchedInfo, context);
			if (bRetVal) {
				return true;
			}
		}
		return false;
	}

	public void addRelationship(String classifierIRI, Relationship rel) {
		assert relationShips.containsKey(classifierIRI) == false : "Already added:" + classifierIRI;
		OverrideInfo override = overrides.get(classifierIRI);
		if (override != null) {
			rel.forwardPostFix = override.forwardName;
		}
		relationShips.put(classifierIRI, rel);
	}

	private void addRelationship(Relationship relationship, String iri, Relationship relationship2) {
		assert relationShips.containsKey(iri) == false : "Already added:" + iri;
		OverrideInfo override = overrides.get(iri);
		if (override != null && override.forwardName != null) {
			relationship2.forwardName = override.forwardName;
		} else {
			relationship2.forwardPostFix = relationship.forwardPostFix;
			relationship2.forwardName = relationship.forwardName;
		}

		if (override != null && override.reverseName != null) {
			relationship2.reverseName = override.reverseName;
		} else {
			relationship2.reversePostFix = relationship.reversePostFix;
			relationship2.reverseName = relationship.reverseName;
		}

		relationShips.put(iri, relationship2);

	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Entry<String, Relationship> entry : relationShips.entrySet()) {
			builder.append("Relationship: " + entry.getKey() + " => " + entry.getValue() + "\n");
		}
		return builder.toString();
	}

	public Relationship getInfo(EClass eContainingClass, OmlBuilder oml, Vocabulary vocabulary, Ecore2Oml e2o) {
		return this.getInfo(Util.getIri(eContainingClass, vocabulary, oml, e2o));
	}

	public Relationship getInfo(String IRI) {
		return relationShips.get(IRI);
	}

	public String getForwardName(EClassifier eClass, String iri) {
		Relationship info = relationShips.get(iri);
		if (info.forwardName != null && !info.forwardName.isEmpty()) {
			return info.forwardName;
		}
		if (info.forwardPostFix != null && !info.forwardPostFix.isEmpty()) {
			return StringExtensions.toFirstLower(Util.getMappedName(eClass)) + Constants.NAME_SEPERATOR
					+ info.forwardPostFix;
		}
		return "";
	}

	public String getReverseName(EClassifier eClass, String iri) {
		Relationship info = relationShips.get(iri);
		if (info.reverseName != null && !info.reverseName.isEmpty()) {
			return info.reverseName;
		}
		if (info.reversePostFix != null && !info.reversePostFix.isEmpty()) {
			return StringExtensions.toFirstLower(Util.getMappedName(eClass)) + Constants.NAME_SEPERATOR
					+ info.reversePostFix;
		}
		return "";
	}

}
