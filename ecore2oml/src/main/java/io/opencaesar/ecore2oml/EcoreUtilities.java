package io.opencaesar.ecore2oml;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;

class EcoreUtilities {

	// Ecore annotation namespaces
	private static final String EXTENDED_META_DATA_IRI = "http:///org/eclipse/emf/ecore/util/ExtendedMetaData";
	private static final String GEN_MODEL_IRI = "http://www.eclipse.org/emf/2002/GenModel";

	// Ecore metamodel namespaces
	private static final String Ecore_IRI = "http://www.eclipse.org/emf/2002/Ecore";
	
	public static String getExtendedMetadataProperty(ENamedElement object, String property) {
		final EAnnotation annotation = object.getEAnnotation(EXTENDED_META_DATA_IRI);
		return (annotation == null) ? null : annotation.getDetails().get(property);
	}

	public static String getGenModelProperty(ENamedElement object, String property) {
		EAnnotation annotation = object.getEAnnotation(GEN_MODEL_IRI);
		return (annotation == null) ? null : annotation.getDetails().get(property); 
	}

	public static String getEcoreProperty(String property) {
		return getEcoreNamespace()+property;
	}

	public static String getEcoreNamespace() {
		return Ecore_IRI+"#";
	}

	public static boolean isAbstract(EClass object) {
		return (object.isAbstract() || object.isInterface()) && 
				object.getESuperTypes().stream().allMatch(i -> isAbstract(i));
	}

	public static Set<EAttribute> getSuperEAttributes(EAttribute object) {
		Set<EAttribute> superRefs = new LinkedHashSet<EAttribute>() ;
		EAnnotation ann = object.getEAnnotation("subsets");
		if (ann != null) {
			for (EObject ref : ann.getReferences()) {
				superRefs.add((EAttribute)ref);
			}
		}
		ann = object.getEAnnotation("redefines");
		if (ann != null) {
			for (EObject ref : ann.getReferences()) {
				superRefs.add((EAttribute)ref);
			}
		}
		return superRefs;
	}

	public static Set<EReference> getSuperEReferences(EReference object) {
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

	public static Set<EStructuralFeature> getSuperEStructuralFeatures(EStructuralFeature object) {
		Set<EStructuralFeature> supers = new LinkedHashSet<EStructuralFeature>() ;
		if (object instanceof EAttribute) {
			supers.addAll(getSuperEAttributes((EAttribute)object));
		} else if (object instanceof EReference) {
			supers.addAll(getSuperEReferences((EReference)object));
		}
		return supers;
	}
	
	/**
	 * ERelation Builder
	 */
	public static class ERelationBuilder {
		
		private Map<EReference, ERelation> eRelations = new LinkedHashMap<>();

		private List<Set<ERelation>> eRelationGroups = new ArrayList<>();

		public class ERelation {
			public Set<ERelation> supers = new LinkedHashSet<>();
			public Set<ERelation> subs = new LinkedHashSet<>();
			public List<EReference> ends = new ArrayList<>();
			public EReference forward;
			public EReference reverse;
			@Override
			public String toString() {
				Iterator<EReference> i = ends.iterator();
				String name = (i.hasNext()) ? getRelationName(i.next()) + "_" : "";
				name = name + ((i.hasNext()) ?  getRelationName(i.next()) + "_" : "");
				name += "A";
				return name;
			}
		}
		
		public ERelationBuilder() {
		}
		
		public void build(Resource inputResource) {
			// create eRelations
			var i = inputResource.getAllContents();
			while (i.hasNext()) {
				EObject o = i.next();
				if (o instanceof EClass) {
					EClass c = (EClass)o;
					for(EReference e : c.getEReferences()) {
						getERelation(e);
					}
				}
			}
			// remove redundant eRelation super types
			for (ERelation a : eRelations.values()) {
				removeRedundantSupers(a);
			}
			// calculate disjoint eRelation groups
			for (ERelation a : eRelations.values()) {
				putInERelationGroup(a, null);
			}
			
			// determine the forward ends consistently within each group 
			for (Set<ERelation> g : eRelationGroups) {
				fixOrderOfEndsInGroup(g);
			}
		}
		
		public ERelation get(EReference eRef) {
			return eRelations.get(eRef);
		}

		private void removeRedundantSupers(ERelation a) {
			Map<ERelation, Set<ERelation>> superMap = new LinkedHashMap<>();
			a.supers.forEach(i -> superMap.put(i, getAllSupers(i)));
			Set<ERelation> redundants = new LinkedHashSet<>();
			Iterator<ERelation> i = a.supers.iterator();
			while (i.hasNext()) {
				ERelation superAss = i.next();
				for (ERelation s : a.supers) {
					if (superMap.get(s).contains(superAss)) {
						redundants.add(superAss);
					}
				};
			}
			a.supers.removeAll(redundants);
		}
		
		private Set<ERelation> getAllSupers(ERelation a) {
			Set<ERelation> allSupers = new LinkedHashSet<>();
			for (ERelation superAss : a.supers) {
				allSupers.add(superAss);
				allSupers.addAll(getAllSupers(superAss));
			}
			return allSupers;
		}
		
		private ERelation getERelation(EReference e) {
			if (!eRelations.containsKey(e)) {
				ERelation a = new ERelation();
				eRelations.put(e, a);
				a.ends.add(e);
				for (EReference superRef : getSuperEReferences(e)) {
					ERelation superAss = getERelation(superRef);
					a.supers.add(superAss);
					superAss.subs.add(a);
				}
				if (e.getEOpposite() != null) {
					EReference o = e.getEOpposite();
					eRelations.put(o, a);
					a.ends.add(o);
					for (EReference superRef : getSuperEReferences(o)) {
						ERelation superAss = getERelation(superRef);
						a.supers.add(superAss);
						superAss.subs.add(a);
					}
				}
			}
			return eRelations.get(e);
		}
		
		private void putInERelationGroup(ERelation a, Set<ERelation> group) {
			if (group == null) {
				for (Set<ERelation> g : eRelationGroups) {
					if (g.contains(a)) {
						return;
					}
				}
				group = new LinkedHashSet<>();
				eRelationGroups.add(group);
			}
			if (!group.contains(a)) {
				group.add(a);
				for (ERelation s : a.supers) {
					putInERelationGroup(s, group);
				};
				for (ERelation s : a.subs) {
					putInERelationGroup(s, group);
				};
			}
		}	
		
		private void fixOrderOfEndsInGroup(Set<ERelation> group) {
			// find the root of inheritance
			Set<ERelation> roots = group.stream()
					.filter(a -> a.supers.isEmpty())
					.collect(Collectors.toCollection(LinkedHashSet::new));
			
			// find the first clear forward from the roots
			EReference forward = roots.stream()
					.map(a -> a.ends.iterator())
					.map(i -> getPreferredForward(i.next(), (i.hasNext()? i.next() : null)))
					.filter(i -> i != null)
					.findFirst()
					.orElse(group.iterator().next().ends.iterator().next());
			
			//set the forwards consistently throughout the group
			ERelation a = eRelations.get(forward);
			a.forward = forward;
			a.reverse = forward.getEOpposite();
			a.subs.forEach(s -> setForwardSub(s, a));
		}
		
		private void setForwardSub(ERelation ass, ERelation superAss) {
			if (ass.forward == null && ass.reverse == null) {
				Iterator<EReference> i = ass.ends.iterator();
				EReference ref1 = i.next();
				EReference ref2 = ref1.getEOpposite();
				if ((ref1 != null && superAss.forward != null && getSuperEReferences(ref1).contains(superAss.forward)) ||
					(ref2 != null && superAss.reverse != null && getSuperEReferences(ref2).contains(superAss.reverse))) {
					ass.forward = ref1;
					ass.reverse = ref1.getEOpposite();
				} else {
					ass.forward = ref1.getEOpposite();
					ass.reverse = ref1;
				}
				ass.subs.forEach(s -> setForwardSub(s, ass));
				ass.supers.forEach(s -> setForwardSuper(s, ass));
			}
		}
		
		private void setForwardSuper(ERelation ass, ERelation subAss) {
			if (ass.forward == null && ass.reverse == null) {
				Iterator<EReference> i = ass.ends.iterator();
				EReference ref1 = i.next();
				EReference ref2 = ref1.getEOpposite();
				if ((ref1 != null && subAss.forward != null && getSuperEReferences(subAss.forward).contains(ref1)) ||
					(ref2 != null && subAss.reverse != null && getSuperEReferences(subAss.reverse).contains(ref2))) {
					ass.forward = ref1;
					ass.reverse = ref1.getEOpposite();
				} else {
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
			assert (one == null);
			return one;
		}
		
		private String getRelationName(EReference object) {
			return object.getEContainingClass().getName() + "_"  + object.getName();
		}
	}
}
