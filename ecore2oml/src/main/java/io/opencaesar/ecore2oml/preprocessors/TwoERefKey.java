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