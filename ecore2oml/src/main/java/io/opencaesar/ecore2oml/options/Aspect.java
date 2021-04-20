package io.opencaesar.ecore2oml.options;

public class Aspect {
	public String root;
	public AspectRelation relation;
	public AspectConcept concept;
			
	public Aspect(String iri, AspectRelation relation, AspectConcept concept) {
		this.root = iri;
		this.relation = relation;
		this.concept = concept;
	}

}

