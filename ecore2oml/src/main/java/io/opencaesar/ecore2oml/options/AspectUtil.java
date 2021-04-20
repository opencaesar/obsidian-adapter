package io.opencaesar.ecore2oml.options;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;

import io.opencaesar.ecore2oml.util.Pair;
import io.opencaesar.ecore2oml.util.Util;

public class AspectUtil {
	
	public static final String RELATION_IRI = "aspect_relaion_IRI";
	public static final String CONCEPT_IRI = "aspect_concept_IRI";
	
	
	private Map<String, Aspect> aspects = new HashMap<>();
	static private AspectUtil _instance = new AspectUtil();
	static private Set<Pair<EClass, Aspect>> toAddSupers = new HashSet<>();
	
	static public AspectUtil getInstance() {
		return _instance;
	}

	static public void init(List<Aspect> aspects) {
		synchronized (AspectUtil.class) {
			for (Aspect aspect : aspects) {
				_instance.addAspect(aspect.root, aspect);
			}
		}
	}

	private void addAspect(String root, Aspect aspect) {
		aspects.put(root, aspect);
	}
	
	public boolean basicIsAspect(EClass toCheck) {
		String iri = Util.getLocalEClassIri(toCheck);
		return aspects.containsKey(iri);
	}
	
	
	public boolean isAspect(EClass toCheck) {
		String[] matched = new String[1];
		Aspect[] matchedInfo = new Aspect[1];
		boolean bRetVal = _isAspect(toCheck, matched, matchedInfo);
		if (bRetVal) {
			String iri = Util.getLocalEClassIri(toCheck);
			if (!aspects.containsKey(iri)) {
				addAspect(iri,new Aspect(iri, matchedInfo[0].relation, matchedInfo[0].concept));
				Pair<EClass,Aspect> pair = new Pair<>();
				pair.source = toCheck;
				pair.target = matchedInfo[0];
				toAddSupers.add(pair);
			}
		}
		return bRetVal;
	}

	private void addSupersAsAspects(EClass toCheck, Aspect matchedInfo) {
		EList<EClass> supers = toCheck.getEAllSuperTypes();
		for (EClass type : supers) {
			if (!Util.defaultsToAspect(type)) {
				String iri = Util.getLocalEClassIri(type);
				if (!aspects.containsKey(iri)) {
					aspects.put(iri,new Aspect(iri, null, matchedInfo.concept));
					addSupersAsAspects(type,matchedInfo);
				}
			}
		}
		
	}

	public boolean _isAspect(EClass toCheck, String[] matchedIRI, Aspect[] matchedInfo) {
		boolean bRetVal = false;
		String iri = Util.getLocalEClassIri(toCheck);
		if (aspects.containsKey(iri)) {
			matchedIRI[0] = iri;
			matchedInfo[0] = aspects.get(iri);
			return true;
		}
		EList<EClass> superTypes = toCheck.getEAllSuperTypes();
		for (EClass superType : superTypes) {
			bRetVal |= _isAspect(superType, matchedIRI, matchedInfo);
			if (bRetVal) {
				return true;
			}
		}
		return false;
	}

	public Aspect getAspectInfo(EClass object) {
		String iri = Util.getLocalEClassIri(object);
		return getAspectInfo(iri);
	}

	public void populateSuperClasses() {
		for (Pair<EClass, Aspect> pair : toAddSupers) {
			addSupersAsAspects(pair.source, pair.target);
		}
	}

	public Aspect getAspectInfo(String iri) {
		return aspects.get(iri);
	}
}
