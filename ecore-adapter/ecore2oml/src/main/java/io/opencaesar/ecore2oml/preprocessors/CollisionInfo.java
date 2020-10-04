package io.opencaesar.ecore2oml.preprocessors;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClassifier;

import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.ScalarProperty;

public class CollisionInfo {
	public Set<EAttribute> members = new HashSet<EAttribute>();
	public Set<EClassifier> types = new HashSet<EClassifier>();
	private EClassifier baseType = null;
	public ScalarProperty baseProperty = null;
	public Aspect baseConcept = null;
	private String name = "";

	public CollisionInfo(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	public boolean sameType() {
		return types.size() == 1;
	}

	public int size() {
		return members.size();
	}

	public void add(EAttribute attr) {
		members.add(attr);
		types.add(attr.getEType());
	}

	public void setName(String name) {
		this.name = name;
	}

	public void finish() {
		// deal with type
		if (!sameType()) {
			for (EClassifier type : types) {
				if (baseType == null) {
					baseType = type;
				} 
			}
		}
	}

	

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(" Size = ");
		builder.append(size());
		builder.append(" - One Type = " + sameType() + " : ");
		types.forEach(e -> {
			builder.append(e.getName() + " - ");
		});
		builder.append("++ Memebrs : ");
		members.forEach(e -> {
			builder.append(e.getEContainingClass().getName() + " - ");
		});
		return builder.toString();
	}
}