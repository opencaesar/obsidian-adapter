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
package io.opencaesar.ecore2oml.preprocessors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;

import io.opencaesar.ecore2oml.util.Util;

public class ERefGroups {
	AtomicInteger ID = new AtomicInteger();
	private Set<Group> groups = new HashSet<>();
	private static final String SUBSETS = "subsets";
	private static final String REDEFINES = "redefines";
	private Map<EReference, Group> refToGroup = new HashMap<>();
	private Set<EReference> toSkip = new HashSet<>();
	private Graph graph = new Graph();
	private Map<EReference, Set<EReference>> memory = new HashMap<>();
	private Logger LOGGER = LogManager.getLogger(ERefGroups.class);
	private class Graph {
		List<Node> nodes = new ArrayList<>();
		Map<EReference, Node> refToNode = new HashMap<>();
		
		public void addRef(EReference ref, EReference superRef) {
			Node superNode = getOrCreateNode(superRef);
			Node node = getOrCreateNode(ref);
			superNode.addSub(node);
		}

		private Node getOrCreateNode(EReference ref) {
			Node node = refToNode.get(ref);
			if (node==null) {
				node = new Node(ref);
				nodes.add(node);
				refToNode.put(ref, node);
			}
			return node;
		}

		public void buildGroups() {
			List<Node> roots = new ArrayList<>();
			for (Node node : nodes) {
				if (node.getParents().isEmpty()) {
					roots.add(node);
				}
			}
			
			for (Node node : roots) {
				if (!node.isVisited()) {
					List<Node> reachables = new ArrayList<>();
					collectReachables(node,reachables); 
					Group g = findGroup(reachables);
					if (g==null) {
						g = new Group(ID.addAndGet(1));
						groups.add(g);
					}
					g.addAll(reachables);
				}
			}
		}

		private Group findGroup(List<Node> reachables) {
			for (Node node : reachables) {
				Group g = getERefGroup(node.ref);
				if (g==null && node.ref.getEOpposite()!=null ) {
					g = getERefGroup(node.ref.getEOpposite());
				}
				if (g!=null) {
					return g;
				}
			}
			return null;
		}

		private void collectReachables(Node node, List<Node> reachables) {
			if (!node.isVisited()) {
				reachables.add(node);
				node.setVisited();
				for (Node sub : node.getSubs()) {
					collectReachables(sub, reachables);
				}
				for (Node parents : node.getParents()) {
					collectReachables(parents, reachables);
				}
			}
		}
	}
	
	private class Node {
		private EReference ref;
		private Set<Node> subs = new HashSet<>();
		private Set<Node> parents = new HashSet<>();
		boolean visited = false;
		
		public Node(EReference ref) {
			this.ref = ref;
		}
		
		public void addSub(Node ref) {
			subs.add(ref);
			ref.addParent(this);
		}
		
		private void addParent(Node node) {
			parents.add(node);
		}

		public boolean isVisited() {
			return visited;
		}
		
		public void setVisited() {
			visited = true;
		}
		
		public Set<Node> getSubs() {
			return subs;
		}
		
		public Set<Node> getParents() {
			return parents;
		}
		
		@Override
		public String toString() {
			return getRefLabel(ref);
		}
	}
	
	
	
	private class Group {
		private int id;
		private Set<EReference> side = new HashSet<>();
		private Set<EReference> otherSide = new HashSet<>();
		private double side1Weight = 0 ;
		private double side2Weight = 0 ;
		private Map<EReference,Double> ref_to_weight = new HashMap<>();
		
		public Group(int id) {
			this.id = id;
		}
		
		public void addAll(List<Node> nodes) {
			List<EReference> otherRefs = new ArrayList<>();
			Set<EReference> whereToAdd = null;
			Set<EReference> whereToAddOthers = null;
			for (Node node : nodes) {
				EReference other = node.ref.getEOpposite();
				if (whereToAdd==null) {
					if (side.contains(node.ref) ||
						(other!=null && otherSide.contains(other))) {
						whereToAdd = side;
						whereToAddOthers = otherSide;
					}else if (otherSide.contains(node.ref) || 
							(other!=null && side.contains(other))) {
						whereToAdd = otherSide;
						whereToAddOthers = side;
					}
					if (other!=null) otherRefs.add(other);
				}
			}
			if (whereToAdd==null) {
				whereToAdd = side;
				whereToAddOthers = otherSide;
			}
			for (Node node : nodes){
				whereToAdd.add(node.ref);
				refToGroup.put(node.ref,this);
			}
			whereToAddOthers.addAll(otherRefs);
			map(otherRefs);
		}

		private void map(List<EReference> refs) {
			for (EReference ref : refs) {
				refToGroup.put(ref,this);
			}
		}
		
		private double calcWeight(EReference ref) {
			if (ref.getEOpposite()==null) {
				return 1;
			}
			if (ref.isContainer() != ref.getEOpposite().isContainer()) {
				if (ref.getEOpposite().isContainer()) {
					return 0.5;
				}
				return 0.0;
			} else if (ref.getUpperBound() != ref.getEOpposite().getUpperBound()) {
				if (ref.getUpperBound()==-1) {
					return 0.4;
				}
				if (ref.getEOpposite().getUpperBound()==-1) {
					return 0;
				}
				if (ref.getEOpposite().getUpperBound() > ref.getUpperBound()) {
					return 0.0;
				}
				return 0.4;
			}else if (!ref.getName().equals(ref.getEOpposite().getName())) {
				if (ref.getName().compareTo(ref.getEOpposite().getName()) > 0) {
					return 0.0;
				}
				return 0.2;
			} else if (!ref.getEContainingClass().getName().equals(ref.getEOpposite().getEContainingClass().getName())) {
				if (ref.getEContainingClass().getName()
						.compareTo(ref.getEOpposite().getEContainingClass().getName()) > 0) {
					return 0.0;
				}
				return 0.1;
			} else if (ref.getEContainingClass().getEPackage().getNsURI()
					.compareTo(ref.getEOpposite().getEContainingClass().getEPackage().getNsURI()) <= 0) {
				return 0.05;
			}
			return 0;
		}
		
		private double calcCollectionWeight(Set<EReference> refs) {
			float weight = 0 ;
			for (EReference ref : refs) {
				double refweight = calcWeight(ref);
				ref_to_weight.put(ref, refweight);
				weight += refweight;
			}
			return weight;
		}		

		public void finish() {
			side1Weight = calcCollectionWeight(side);
			side2Weight = calcCollectionWeight(otherSide);
			if (side1Weight>side2Weight) {
				filterCollection(otherSide);
			}else {
				filterCollection(side);
			}
			
		}

		private void filterCollection(Set<EReference> refs) {
			for (EReference ref : refs ) {
				toSkip.add(ref);
			}
		}

		public String getWeight(EReference ref) {
			return ref_to_weight.get(ref).toString();
		}
		
	}
	
	public ERefGroups() {
		ID.set(0);
	}
	
	private String getRefLabel(EReference ref) {
		return  ref.getEContainingClass().getName() + "_" + ref.getName();
	}
	
	public void addERef(EReference ref) {
		// if it is handled already then skip
		addRefToGraph(ref);
		if (ref.getEOpposite()!=null) {
			addRefToGraph(ref.getEOpposite());
		}
	}
	
	public void add(EReference element, EReference superRef) {
		Collection<EReference> col = Collections.singletonList(superRef);
		pushTograph(element,col);
		if (element.getEOpposite()!=null) {
			addRefToGraph(element.getEOpposite());
		}
	}


	private void addRefToGraph(EReference ref) {
		if (ref!=null) {
			Set<EReference> subSets = getSubSets(ref);
			pushTograph(ref,subSets);
		}
	}
	


	private void pushTograph(EReference ref, Collection<EReference> subs) {
		if (subs.isEmpty()) {
			graph.getOrCreateNode(ref);
			if (ref.getEOpposite()!=null) {
				graph.getOrCreateNode(ref.getEOpposite());
			}
		}
		for (EReference sub : subs) {
			graph.addRef(ref, sub);
		}		
	}

	private Set<EReference> getSubSets(EReference ref) {
		// this can be memoized
		if (memory.containsKey(ref)) {
			return memory.get(ref);
		}
		EAnnotation subsets = Util.getAnnotation(ref, SUBSETS);
		Set<EReference> ret = Collections.emptySet();
		boolean emptySet = true;
		if (subsets!=null) {
			emptySet = false;
			ret = new HashSet<>();
			EList<EObject> refs = subsets.getReferences();
			for (EObject sub : refs) {
				EReference superRef = (EReference)sub;
				ret.add(superRef);
				ret.addAll(getSubSets(superRef));
			}
		}
		EAnnotation redefine = Util.getAnnotation(ref, REDEFINES);
		if (redefine!=null) {
			if (emptySet) {
				ret = new HashSet<>();
			}
			EList<EObject> refs = redefine.getReferences();
			for (EObject sub : refs) {
				EReference superRef = (EReference)sub;
				ret.add(superRef);
				ret.addAll(getSubSets(superRef));
			}	
		}
		memory.put(ref, ret);
		return ret;
	}
	
	private Group getERefGroup(EReference ref) {
		return refToGroup.get(ref);
	}
	
	public void finish() {
		graph.buildGroups();
		
		groups = groups.stream().filter(g-> g.side.size() >=1 && g.otherSide.size() >= 1).collect(Collectors.toSet());
		for (Group group : groups) {
			group.finish();
		}
		LOGGER.debug(this);
	}
	
	@Override
	public String toString() {
		StringBuilder bldr = new StringBuilder();
		for (Group g : groups) {
			bldr.append("Group: " + g.id + "\n");
			bldr.append("\tSide1: " + "(Weight: " + g.side1Weight + ")\n");
			for (EReference ref : g.side) {
				bldr.append("\t\t" +  getContainerName(ref) + ":" + ref.getName() + " weight = "+ g.getWeight(ref)+"\n");
			}
			bldr.append("\tSide2: " + "(Weight: " + g.side2Weight + ")\n");
			for (EReference ref : g.otherSide) {
				bldr.append("\t\t" +  getContainerName(ref) + ":" + ref.getName()+ " weight = "+ g.getWeight(ref) + "\n");
			}
		}
		return bldr.toString();
	}


	private String getContainerName(EReference ref) {
		EClass containingClass = ref.getEContainingClass();
		if (containingClass==null) {
			return "NULL";
		}
		return containingClass.getName();
	}

	public boolean shouldSkip(EReference object) {
		return toSkip.contains(object);
	}

}
