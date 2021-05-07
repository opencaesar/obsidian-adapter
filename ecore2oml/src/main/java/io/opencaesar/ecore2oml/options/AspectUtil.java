package io.opencaesar.ecore2oml.options;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;

import io.opencaesar.ecore2oml.ConversionContext;
import io.opencaesar.ecore2oml.util.Pair;
import io.opencaesar.ecore2oml.util.Util;

public class AspectUtil {
	
	private Map<String, Aspect> aspects = new HashMap<>();
	private Set<Pair<EClass, Aspect>> toAddSupers = new HashSet<>();
	
	public void init(List<Aspect> aspects) {
		for (Aspect aspect : aspects) {
			addAspect(aspect.root, aspect);
		}
	}

	private void addAspect(String root, Aspect aspect) {
		aspects.put(root, aspect);
	}
	
	public boolean basicIsAspect(EClass toCheck, ConversionContext context) {
		String iri = Util.getLocalEClassIri(toCheck, context);
		return aspects.containsKey(iri);
	}
	
	
	public boolean isAspect(EClass toCheck, ConversionContext context) {
		String[] matched = new String[1];
		Aspect[] matchedInfo = new Aspect[1];
		boolean bRetVal = _isAspect(toCheck, matched, matchedInfo, context);
		if (bRetVal) {
			String iri = Util.getLocalEClassIri(toCheck, context);
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

	private void addSupersAsAspects(EClass toCheck, Aspect matchedInfo, ConversionContext context) {
		EList<EClass> supers = toCheck.getEAllSuperTypes();
		for (EClass type : supers) {
			if (!Util.defaultsToAspect(type)) {
				String iri = Util.getLocalEClassIri(type, context);
				if (!aspects.containsKey(iri)) {
					aspects.put(iri,new Aspect(iri, null, matchedInfo.concept));
					addSupersAsAspects(type,matchedInfo, context);
				}
			}
		}
		
	}

	public boolean _isAspect(EClass toCheck, String[] matchedIRI, Aspect[] matchedInfo, ConversionContext context) {
		boolean bRetVal = false;
		String iri = Util.getLocalEClassIri(toCheck, context);
		if (aspects.containsKey(iri)) {
			matchedIRI[0] = iri;
			matchedInfo[0] = aspects.get(iri);
			return true;
		}
		EList<EClass> superTypes = toCheck.getEAllSuperTypes();
		for (EClass superType : superTypes) {
			bRetVal |= _isAspect(superType, matchedIRI, matchedInfo, context);
			if (bRetVal) {
				return true;
			}
		}
		return false;
	}

	public Aspect getAspectInfo(EClass object, ConversionContext context) {
		String iri = Util.getLocalEClassIri(object, context);
		return getAspectInfo(iri);
	}

	public void populateSuperClasses(ConversionContext context) {
		for (Pair<EClass, Aspect> pair : toAddSupers) {
			addSupersAsAspects(pair.source, pair.target, context);
		}
	}

	public Aspect getAspectInfo(String iri) {
		return aspects.get(iri);
	}
}
