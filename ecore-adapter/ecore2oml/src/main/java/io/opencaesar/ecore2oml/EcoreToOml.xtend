package io.opencaesar.ecore2oml

import io.opencaesar.oml.Terminology
import io.opencaesar.oml.TerminologyKind
import io.opencaesar.oml.util.OmlWriter
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EDataType
import org.eclipse.emf.ecore.EEnum
import org.eclipse.emf.ecore.EModelElement
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EReference
import org.eclipse.emf.ecore.EcorePackage
import org.eclipse.emf.ecore.resource.Resource

class EcoreToOml {

	static val XSD = "http://www.w3.org/2001/XMLSchema#"
	static val OML = "http://opencaesar.io/Oml"
	
	static enum Annotation {
		annotationProperty,
		reifiedRelationship,
		name,
		source,
		target,
		forward,
		inverse,
		ignore
	} 

	val OmlWriter oml	
	val Resource inputResource 
	val String relativePath
	
	new(Resource inputResource, String relativePath, OmlWriter oml) {
		this.inputResource = inputResource
		this.relativePath = relativePath
		this.oml = oml
	}
	
	def run() {
		val ePackage = inputResource.contents.filter(EPackage).head
		val terminology = ePackage.toTerminology()
		var i = ePackage.eAllContents
		while (i.hasNext) {
			val object = i.next
			if (object instanceof EModelElement) {
				if(object.isAnnotationSet(Annotation.ignore)) {
					i.prune
				} else {
					object.addToTerminology(terminology)
				}
			}
		}
	}

	// EPackage

	protected def Terminology toTerminology(EPackage ePackage) {
		var iri = ePackage.iri
		val pefix = ePackage.nsPrefix
		var i = iri.indexOf('//')
		if (i === -1) i=0 else i=i+2
		var j = iri.lastIndexOf('/')
		if (j === -1) j=iri.length
		var namespace = iri.substring(i, j)
		val owlPath = namespace.split("/").map(m|"../").join() + "www.w3.org/2002/07/owl.oml"
		val terminology = oml.createTerminology(TerminologyKind.OPEN, iri, pefix, namespace+'/'+relativePath)
		oml.addTerminologyExtension(terminology, owlPath, "owl")
		terminology
	}

	// Eobject

	protected dispatch def void addToTerminology(EObject eObject, Terminology terminology) {
		// no handling by default
	}

	//EClass

	protected dispatch def void addToTerminology(EClass eClass, Terminology terminology) {
		val term = if (eClass.isAnnotationSet(Annotation.reifiedRelationship)) {
			val source = eClass.EAllReferences.findFirst[isAnnotationSet(Annotation.source)]
			val target = eClass.EAllReferences.findFirst[isAnnotationSet(Annotation.target)]
			val forward = eClass.getAnnotationValue(Annotation.forward)
			val inverse = eClass.getAnnotationValue(Annotation.inverse)
			val reifiedRelationship = oml.addReifiedRelationship(
				terminology,
				eClass.realName,
				source?.EType.iri, 
				target?.EType.iri, 
				false,
				false,
				false,
				false,
				false,
				false,
				false)
			oml.addForwardDirection(
				reifiedRelationship,
				forward)
			if (inverse !== null) {
				oml.addInverseDirection(
					reifiedRelationship,
					inverse)
			}
			reifiedRelationship
		} else if (eClass.isAbstract) {
			oml.addAspect(terminology, eClass.realName)
		} else {
			oml.addConcept(terminology, eClass.realName)
		}
		eClass.ESuperTypes.forEach[oml.addTermSpecializationAxiom(terminology, term, EPackage.iri+name)]
	}

	//EEnum

	protected dispatch def void addToTerminology(EEnum eEnum, Terminology terminology) {
		oml.addEnumerationScalar(terminology, eEnum.realName, eEnum.ELiterals.map[oml.createLiteralString(name, null)])
	}	

	//EDataType

	protected dispatch def void addToTerminology(EDataType eDataType, Terminology terminology) {
		oml.addScalar(terminology, eDataType.realName)
	}	

	//EAttribute

	protected dispatch def void addToTerminology(EAttribute eAttribute, Terminology terminology) {
		val domain = eAttribute.EContainingClass
		val range = eAttribute.EType
		if (eAttribute.isAnnotationSet(Annotation.annotationProperty)) {
			oml.addAnnotationProperty(
				terminology, 
				eAttribute.realName+"Of"+domain.realName)			
		} else {
			oml.addScalarProperty(
				terminology, 
				eAttribute.realName+"Of"+domain.realName, 
				domain.iri, 
				range.iri, 
				eAttribute.upperBound === 1
			)
		}
	}

	//ERference
	
	protected dispatch def void addToTerminology(EReference eReference, Terminology terminology) {
		val source = eReference.EContainingClass
		val target = eReference.EType
		val opposite = eReference.EOpposite
		
		if (eReference.isAnnotationSet(Annotation.source) || 
			eReference.isAnnotationSet(Annotation.target) ||
			target.isAnnotationSet(Annotation.ignore)) {
			return
		}
				
		
		val reifiedRelationship = oml.addReifiedRelationship(
			terminology,
			eReference.realName.toFirstUpper+"Of"+source.realName,
			source.iri, 
			target.iri, 
			eReference.upperBound === 1,
			(opposite !== null) && opposite.upperBound == 1,
			false,
			false,
			false,
			false,
			false)
		oml.addForwardDirection(
			reifiedRelationship,
			eReference.realName+"Of"+source.realName)
		if (opposite !== null) {
			oml.addInverseDirection(
				reifiedRelationship,
				opposite.realName+"of"+target.realName)
		}
	}

	// Utilities

	protected dispatch def String getIri(EPackage ePackage) {
		var nsURI = ePackage.nsURI
		if (!nsURI.endsWith('#') && !nsURI.endsWith('/')) nsURI += '#'
		nsURI
	}	

	protected dispatch def String getIri(EClass eClass) {
		eClass.EPackage.iri + eClass.realName
	}	

	protected dispatch def String getIri(EEnum eEnum) {
		eEnum.EPackage.iri + eEnum.realName
	}	

	protected dispatch def String getIri(EDataType eDataType) {
		switch (eDataType.realName) {
			case EcorePackage.Literals.ESTRING.name: XSD+'string'
			case EcorePackage.Literals.EINT.name: XSD+'int'
			case EcorePackage.Literals.EINTEGER_OBJECT.name: XSD+'int'
			case EcorePackage.Literals.EBOOLEAN.name: XSD+'boolean'
			case EcorePackage.Literals.EDOUBLE.name: XSD+'double'
			case EcorePackage.Literals.EDOUBLE_OBJECT.name: XSD+'double'
			case EcorePackage.Literals.EFLOAT.name: XSD+'float'
			case EcorePackage.Literals.EFLOAT_OBJECT.name: XSD+'float'
			case EcorePackage.Literals.EBIG_DECIMAL.name: XSD+'decimal'
			default: eDataType.EPackage.iri + eDataType.name 	
		}
	}
	
	def getRealName(ENamedElement element) {
		element.getAnnotationValue(Annotation.name,element.name)
	}
	
	def isAnnotationSet(EModelElement element, Annotation annotation) {
		getAnnotationValue(element, annotation) == "true"
	}

	def getAnnotationValue(EModelElement element, Annotation annotation, String defaultValue) {
		getAnnotationValue(element, annotation) ?: defaultValue
	}

	def getAnnotationValue(EModelElement element, Annotation annotation) {
		element.EAnnotations.findFirst[OML == source]?.details?.get(annotation.toString)
	}
}