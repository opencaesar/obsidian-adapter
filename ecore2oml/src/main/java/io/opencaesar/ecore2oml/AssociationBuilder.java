/**
 * 
 * Copyright 2022 Modelware Solutions LLC and CEA-LIST.
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
package io.opencaesar.ecore2oml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;

public class AssociationBuilder {
	
	private Resource inputResource;

	private Map<EReference, Association> associations = new HashMap<>();

	private List<Set<Association>> associationGroups = new ArrayList<>();

	public class Association {
		public Set<Association> supers = new HashSet<>();
		public Set<Association> subs = new HashSet<>();
		public List<EReference> ends = new ArrayList<>();
		public EReference forward;
		public EReference reverse;
		public String getName() {
			String name = (forward != null) ? getForwardName() + "_" : "";
			name = name + ((reverse != null) ?  getReverseName() + "_" : "");
			name += "A";
			return name;
		}
		public String getForwardName() {
			return (forward != null) ? getRelationName(forward) : "";
		}
		public String getReverseName() {
			return (reverse != null) ? getRelationName(reverse) : "";
		}
		@Override
		public String toString() {
			Iterator<EReference> i = ends.iterator();
			String name = (i.hasNext()) ? getRelationName(i.next()) + "_" : "";
			name = name + ((i.hasNext()) ?  getRelationName(i.next()) + "_" : "");
			name += "A";
			return name;
		}
	}
	
	public AssociationBuilder(Resource inputResource) {
		this.inputResource = inputResource;
	}
	
	public void build() {
		// create associations
		var i = inputResource.getAllContents();
		while (i.hasNext()) {
			EObject o = i.next();
			if (o instanceof EClass) {
				EClass c = (EClass)o;
				for(EReference e : c.getEReferences()) {
					getAssociation(e);
				}
			}
		}
		// remove redundant association super types
		for (Association a : associations.values()) {
			removeRedundantSupers(a);
		}
		// calculate disjoint association groups
		for (Association a : associations.values()) {
			putInAssociationGroup(a, null);
		}
		// determine the forward ends consistently within each group 
		for (Set<Association> g : associationGroups) {
			fixOrderOfEndsInGroup(g);
		}
	}
	
	public Association get(EReference eRef) {
		return associations.get(eRef);
	}

	private void removeRedundantSupers(Association a) {
		Map<Association, Set<Association>> superMap = new HashMap<>();
		a.supers.forEach(i -> superMap.put(i, getAllSupers(i)));
		Set<Association> redundants = new HashSet<>();
		Iterator<Association> i = a.supers.iterator();
		while (i.hasNext()) {
			Association superAss = i.next();
			for (Association s : a.supers) {
				if (superMap.get(s).contains(superAss)) {
					redundants.add(superAss);
				}
			};
		}
		a.supers.removeAll(redundants);
	}
	
	private Set<Association> getAllSupers(Association a) {
		Set<Association> allSupers = new HashSet<>();
		for (Association superAss : a.supers) {
			allSupers.add(superAss);
			allSupers.addAll(getAllSupers(superAss));
		}
		return allSupers;
	}
	
	private Association getAssociation(EReference e) {
		if (!associations.containsKey(e)) {
			Association a = new Association();
			associations.put(e, a);
			a.ends.add(e);
			for (EReference superRef : getSuperEReferences(e)) {
				Association superAss = getAssociation(superRef);
				a.supers.add(superAss);
				superAss.subs.add(a);
			}
			if (e.getEOpposite() != null) {
				EReference o = e.getEOpposite();
				associations.put(o, a);
				a.ends.add(o);
				for (EReference superRef : getSuperEReferences(o)) {
					Association superAss = getAssociation(superRef);
					a.supers.add(superAss);
					superAss.subs.add(a);
				}
			}
		}
		return associations.get(e);
	}
	
	private void putInAssociationGroup(Association a, Set<Association> group) {
		if (group == null) {
			for (Set<Association> g : associationGroups) {
				if (g.contains(a)) {
					return;
				}
			}
			group = new HashSet<>();
			associationGroups.add(group);
		}
		if (!group.contains(a)) {
			group.add(a);
			for (Association s : a.supers) {
				putInAssociationGroup(s, group);
			};
			for (Association s : a.subs) {
				putInAssociationGroup(s, group);
			};
		}
	}	
	
	private void fixOrderOfEndsInGroup(Set<Association> group) {
		// find the root if inheritance
		Set<Association> roots = group.stream()
				.filter(a -> a.supers.isEmpty())
				.collect(Collectors.toSet());
		
		// find the first clear forward from the roots
		EReference forward = roots.stream()
				.map(a -> a.ends.iterator())
				.map(i -> getPreferredForward(i.next(), (i.hasNext()? i.next() : null)))
				.filter(i -> i != null)
				.findFirst()
				.orElse(group.iterator().next().ends.iterator().next());
		
		//set the forwards consistently throughout the group
		Association a = associations.get(forward);
		a.forward = forward;
		a.reverse = forward.getEOpposite();
		a.subs.forEach(s -> setForwardSub(s, a));
	}
	
	private void setForwardSub(Association ass, Association superAss) {
		if (ass.forward == null && ass.reverse == null) {
			Iterator<EReference> i = ass.ends.iterator();
			EReference ref1 = i.next();
			EReference ref2 = ref1.getEOpposite();
			if ((ref1 != null && superAss.forward != null && getSuperEReferences(ref1).contains(superAss.forward)) ||
				(ref2 != null && superAss.reverse != null && getSuperEReferences(ref2).contains(superAss.reverse))) {
				if (ass.forward != null && ass.forward != ref1) {
					System.out.println("oh oh");
				}
				ass.forward = ref1;
				ass.reverse = ref1.getEOpposite();
			} else {
				if (ass.reverse != null && ass.reverse != ref1) {
					System.out.println("oh oh");
				}
				ass.forward = ref1.getEOpposite();
				ass.reverse = ref1;
			}
			ass.subs.forEach(s -> setForwardSub(s, ass));
			ass.supers.forEach(s -> setForwardSuper(s, ass));
		}
	}
	
	private void setForwardSuper(Association ass, Association subAss) {
		if (ass.forward == null && ass.reverse == null) {
			Iterator<EReference> i = ass.ends.iterator();
			EReference ref1 = i.next();
			EReference ref2 = ref1.getEOpposite();
			if ((ref1 != null && subAss.forward != null && getSuperEReferences(subAss.forward).contains(ref1)) ||
				(ref2 != null && subAss.reverse != null && getSuperEReferences(subAss.reverse).contains(ref2))) {
				if (ass.forward != null && ass.forward != ref1) {
					System.out.println("oh oh");
				}
				ass.forward = ref1;
				ass.reverse = ref1.getEOpposite();
			} else {
				if (ass.reverse != null && ass.reverse != ref1) {
					System.out.println("oh oh");
				}
				ass.forward = ref1.getEOpposite();
				ass.reverse = ref1;
			}
			ass.subs.forEach(s -> setForwardSub(s, ass));
			ass.supers.forEach(s -> setForwardSuper(s, ass));
		}
	}

	private EReference getPreferredForward(EReference ref1, EReference ref2) {
		EReference one = null;
		if (ref2 == null) {
			one = ref1;
		} else {
			// container wins
			if (ref1.isContainment() && !ref2.isContainment()) {
				one = ref1;
			} else if (ref2.isContainment() && !ref1.isContainment()) {
				one = ref2;
			}
			// higher multiplicity wins
			if (one == null) {
				int ref1Upper = (ref1.getUpperBound() == -1) ? Integer.MAX_VALUE : ref1.getUpperBound();
				int ref2Upper = (ref2.getUpperBound() == -1) ? Integer.MAX_VALUE : ref2.getUpperBound();
				if (ref1Upper > ref2Upper) {
					one = ref1;
				} else if (ref2Upper > ref1Upper) {
					one = ref2;
				}
			}
			// higher alphabetical wins
			//if (one == null) {
				//one = ref1.getName().compareTo(ref2.getName()) > 0 ? ref1 : ref2;
			//}
		}
		return one;
	}

	private Set<EReference> getSuperEReferences(EReference object) {
		Set<EReference> superRefs = new LinkedHashSet<EReference>() ;
		EAnnotation ann = object.getEAnnotation("subsets");
		if (ann != null) {
			for (EObject ref : ann.getReferences()) {
				superRefs.add((EReference)ref);
			}
		}
		ann = object.getEAnnotation("redefines");
		if (ann != null) {
			for (EObject ref : ann.getReferences()) {
				superRefs.add((EReference)ref);
			}
		}
		return superRefs;
	}
	
	private String getRelationName(EReference object) {
		return object.getEContainingClass().getName() + "_"  + object.getName();
	}
}
