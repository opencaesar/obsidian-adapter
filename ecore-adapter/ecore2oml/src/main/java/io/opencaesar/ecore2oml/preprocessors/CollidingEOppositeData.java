package io.opencaesar.ecore2oml.preprocessors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;

import io.opencaesar.ecore2oml.Util;

public class CollidingEOppositeData {
	Map<TwoERefKey, Set<EReference>> forward = new HashMap<>();
	private Set<EReference> toSkip = new HashSet<>();
	private Map<EReference, EReference> eRefToReplacement = new HashMap<EReference, EReference>();
	private static final String SUBSETS = "subsets";

	public CollidingEOppositeData() {

	}

	public boolean shouldSkip(EReference eRef) {
		return toSkip.contains(eRef);
	}

	public void addForward(EReference eRef) {
		TwoERefKey key = new TwoERefKey(eRef, eRef.getEOpposite());
		Set<EReference> forwardSet = forward.get(key);
		if (forwardSet == null) {
			forwardSet = new LinkedHashSet<>();
			forward.put(key, forwardSet);
		}
		forwardSet.add(eRef);
	}

	public void addReverse(EReference eRef) {
		TwoERefKey key = new TwoERefKey(eRef.getEOpposite(), eRef);
		Set<EReference> forwardSet = forward.get(key);
		if (forwardSet != null) {
			forwardSet.add(eRef);
		}
	}

	private boolean pick(EReference[] ret, EReference ref, EReference replacment) {
		if (ref.isContainer() != ref.getEOpposite().isContainer()) {
			if (ref.getEOpposite().isContainer()) {
				ret[0] = replacment;
			}
			return true;
		} else if (ref.getUpperBound() != ref.getEOpposite().getUpperBound()) {
			if (ref.getEOpposite().getUpperBound() != -1
					&& (ref.getUpperBound() == -1 || ref.getUpperBound() > ref.getEOpposite().getUpperBound())) {
				ret[0] = replacment;
			}
			return true;

		}else  if (resolveUsingSubsets(ret, ref, replacment)) {
			return true;
		}
		return false;
	}

	public EReference getMainRef(Set<EReference> refs) {
		EReference[] retVal = new EReference[1];
		retVal[0] = refs.stream().findFirst().get();
		refs.forEach(ref -> {
			// System.out.println(retVal[0].getName() + " " + retVal[0].isContainer() + " "
			// + retVal[0].getUpperBound());
			// System.out.println(ref.getName() + " " + ref.isContainer() + " " +
			// ref.getUpperBound());
			if (retVal[0] != ref) {
				if (!pick(retVal, ref, ref)) {
					if (!retVal[0].getName().equals(ref.getName())) {
						if (retVal[0].getName().compareTo(ref.getName()) > 0) {
							retVal[0] = ref;
						}
					} else if (!retVal[0].getEContainingClass().getName().equals(ref.getEContainingClass().getName())) {
						if (retVal[0].getEContainingClass().getName()
								.compareTo(ref.getEContainingClass().getName()) > 0) {
							retVal[0] = ref;
						}
					} else if (retVal[0].getEContainingClass().getEPackage().getNsURI()
							.compareTo(ref.getEContainingClass().getEPackage().getNsURI()) > 0) {
						retVal[0] = ref;
					}
				}
			}
		});
		return retVal[0];
	}

	private boolean resolveUsingSubsets(EReference[] retVal, EReference ref, EReference replacment) {
		EAnnotation subsets = Util.getAnnotation(ref, SUBSETS);
		if (subsets!=null) {
			EList<EObject> refs = subsets.getReferences();
			for (EObject superRef : refs) {
				if (pick(retVal,(EReference)superRef, replacment)) {
					return true;
				}
			}
		}
		return false;
	}

	public void finish() {
		// deal with type
		// reflect the ERef Election Rules
		forward.entrySet().forEach(entry -> {
			Set<EReference> value = entry.getValue();
			if (value.size() > 1) {
				EReference ref = getMainRef(value);
				value.remove(ref);
				assert value.size() <= 1;
				value.forEach(replacedEef -> {
					eRefToReplacement.put(replacedEef, ref);
					toSkip.add(replacedEef);
				});
			}
		});
	}

	public EReference getMappedRef(EReference replacedRef) {
		return eRefToReplacement.get(replacedRef);
	}

	public int size() {
		return forward.size();
	}
}