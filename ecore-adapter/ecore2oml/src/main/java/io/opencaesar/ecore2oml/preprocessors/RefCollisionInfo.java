package io.opencaesar.ecore2oml.preprocessors;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.ecore.EReference;

import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.ForwardRelation;
import io.opencaesar.oml.RelationEntity;

public class RefCollisionInfo {
	public Set<EReference> members = new HashSet<>();
	private String name = "";
	public Aspect fromAspect;
	public Aspect toAspect;
	public RelationEntity entity;
	public ForwardRelation forward;

	public RefCollisionInfo(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public int size() {
		return members.size();
	}

	public void add(EReference attr) {
		members.add(attr);
	}

	public void setName(String name) {
		this.name = name;
	}

	public void finish() {
		// deal with type
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(" Size = ");
		builder.append(size());
		builder.append("++ Memebrs : ");
		members.forEach(e -> {
			builder.append(e.getEContainingClass().getName() + " - ");
		});
		return builder.toString();
	}
}