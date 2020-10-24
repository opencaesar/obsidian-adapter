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
		if (isAnnotationSet(object.getEReferenceType(), AnnotationKind.ignore)) {
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
		if (isAnnotationSet(eAttr.getEAttributeType(), AnnotationKind.ignore)) {
			return true;
		}
		return false;
	}

}
