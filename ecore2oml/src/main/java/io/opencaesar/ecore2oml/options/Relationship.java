package io.opencaesar.ecore2oml.options;

import java.util.List;

import org.eclipse.emf.ecore.EStructuralFeature;

import io.opencaesar.ecore2oml.ConversionContext;
import io.opencaesar.ecore2oml.util.Util;

public class Relationship {
	
	public String root;
	public  String source;
	public String target;
	public String forwardPostFix;
	public String reversePostFix;
	public String forwardName;
	public String reverseName;
	public List<OverrideInfo> overrides;
	
	public boolean isSource(EStructuralFeature toCheck, ConversionContext context) {
		return Util.getIri(toCheck, context).equals(source);
	}
	
	public boolean isTarget(EStructuralFeature toCheck, ConversionContext context) {
		return Util.getIri(toCheck, context).equals(target);
	}
	
	public Relationship(String iri, String source, String target) {
		this.root = iri;
		this.source = source;
		this.target = target;
	}
	

}
