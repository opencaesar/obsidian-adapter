package io.opencaesar.ecore2oml.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.xtext.xbase.lib.StringExtensions;

import io.opencaesar.ecore2oml.Ecore2Oml;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlWriter;

public class RelationshipUtil {
	
	private Map<String, Relationship> relationShips = new HashMap<>();
	private Map<String, OverrideInfo> overrides = new HashMap<>();
	static private RelationshipUtil _instance = new RelationshipUtil();

	static public RelationshipUtil getInstance() {
		return _instance;
	}

	static public void init(List<Relationship> relationships) {
		synchronized (RelationshipUtil.class) {
			for (Relationship relation : relationships) {
				_instance.addRelationship(relation.root, relation);
				for (OverrideInfo override : relation.overrides) {
					_instance.overrides.put(override.iri, override);
				}
			}

		}
	}

	public boolean isRelationship(EClass toCheck, OmlWriter oml, Vocabulary voc, Ecore2Oml e2o) {
		String[] matched = new String[1];
		Relationship[] matchedInfo = new Relationship[1];
		boolean bRetVal = _isRelationship(toCheck, oml, voc, matched, matchedInfo, e2o);
		if (bRetVal) {
			String iri = Util.getIri(toCheck, voc, oml, e2o);
			if (!relationShips.containsKey(iri)) {
				addRelationship(matchedInfo[0], iri,
						new Relationship(iri, matchedInfo[0].source, matchedInfo[0].target));
			}
		}
		return bRetVal;
	}

	public boolean _isRelationship(EClass toCheck, OmlWriter oml, Vocabulary voc, String[] matchedIRI,
			Relationship[] matchedInfo, Ecore2Oml e2o) {
		boolean bRetVal = false;
		String iri = Util.getIri(toCheck, voc, oml, e2o);
		if (relationShips.containsKey(iri)) {
			matchedIRI[0] = iri;
			matchedInfo[0] = relationShips.get(iri);
			return true;
		}
		EList<EClass> superTypes = toCheck.getEAllSuperTypes();
		for (EClass superType : superTypes) {
			bRetVal |= _isRelationship(superType, oml, voc, matchedIRI, matchedInfo, e2o);
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

	public Relationship getInfo(EClass eContainingClass, OmlWriter oml, Vocabulary vocabulary, Ecore2Oml e2o) {
		return relationShips.get(Util.getIri(eContainingClass, vocabulary, oml, e2o));
	}

	public String getForwardName(EClassifier eClass, String iri) {
		Relationship info = relationShips.get(iri);
		if (info.forwardName != null) {
			return info.forwardName;
		}
		return StringExtensions.toFirstLower(Util.getMappedName(eClass)) + Constants.NAME_SEPERATOR
				+ info.forwardPostFix;
	}

	public String getReverseName(EClassifier eClass, String iri) {
		Relationship info = relationShips.get(iri);
		if (info.reverseName != null) {
			return info.reverseName;
		}
		return StringExtensions.toFirstLower(Util.getMappedName(eClass)) + Constants.NAME_SEPERATOR
				+ info.reversePostFix;
	}

}
