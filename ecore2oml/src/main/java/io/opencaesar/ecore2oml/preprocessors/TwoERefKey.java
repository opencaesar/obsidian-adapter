package io.opencaesar.ecore2oml.preprocessors;

import org.eclipse.emf.ecore.EReference;

public class TwoERefKey {

	public TwoERefKey(EReference forward, EReference reverse) {
		this.forward = forward;
		this.reverse = reverse;
	}

	public EReference forward;
	public EReference reverse;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((forward == null) ? 0 : forward.hashCode());
		result = prime * result + ((reverse == null) ? 0 : reverse.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TwoERefKey other = (TwoERefKey) obj;
		if (forward == null) {
			if (other.forward != null)
				return false;
		} else if (!forward.equals(other.forward))
			return false;
		if (reverse == null) {
			if (other.reverse != null)
				return false;
		} else if (!reverse.equals(other.reverse))
			return false;
		return true;
	}

}