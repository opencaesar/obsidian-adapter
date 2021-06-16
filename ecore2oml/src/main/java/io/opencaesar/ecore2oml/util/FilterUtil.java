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
package io.opencaesar.ecore2oml.util;

import static io.opencaesar.ecore2oml.util.Util.isAnnotationSet;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EReference;

import io.opencaesar.ecore2oml.AnnotationKind;

public class FilterUtil {
	
	public static boolean shouldFilter(EReference object) {
		boolean bRetVal  = false;
		if (isAnnotationSet(object, AnnotationKind.ignore)) {
			bRetVal = true;
		}
		if (object.getEType() != null && isAnnotationSet(object.getEReferenceType(), AnnotationKind.ignore)) {
			bRetVal = true;
		}
		if (isAnnotationSet(object, AnnotationKind.isRelationSource) || 
			isAnnotationSet(object, AnnotationKind.isRelationTarget)) {
			bRetVal = true;
		}
		
		return bRetVal;
	}
	
	public static boolean shouldFilter(EAttribute eAttr) {
		if (isAnnotationSet(eAttr, AnnotationKind.ignore)) {
			return true;
		}
		if (eAttr.getEType() != null && isAnnotationSet(eAttr.getEAttributeType(), AnnotationKind.ignore)) {
			return true;
		}
		return false;
	}

}
