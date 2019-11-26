package io.opencaesar.ecore2oml

import io.opencaesar.oml.Vocabulary
import io.opencaesar.oml.util.OmlWriter
import org.eclipse.emf.common.util.URI
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

import static extension io.opencaesar.oml.util.OmlRead.*

class EcoreToOml {

	static val XSD = "http://www.w3.org/2001/XMLSchema"

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
	val URI outputResourceURI
	val URI owlResourceURI
	
	
	new(Resource inputResource, URI outputResourceURI, URI owlResourceURI, OmlWriter oml) {
		this.inputResource = inputResource
		this.outputResourceURI = outputResourceURI
		this.owlResourceURI = owlResourceURI
		this.oml = oml
	}
	
	def run() {
		val ePackage = inputResource.contents.filter(EPackage).head
		val vocabulary = ePackage.toVocabulary()
		var i = ePackage.eAllContents
		while (i.hasNext) {
			val object = i.next
			if (object instanceof EModelElement) {
				if(object.isAnnotationSet(Annotation.ignore)) {
					i.prune
				} else {
					object.addToVocabulary(vocabulary)
				}
			}
		}
	}

	// EPackage

	protected def Vocabulary toVocabulary(EPackage ePackage) {
		var iri = ePackage.iri
		val separator = ePackage.separator
		val pefix = ePackage.prefix
		val String relativePath = owlResourceURI.deresolve(outputResourceURI, true, true, true).toString
		val vocabulary = oml.createVocabulary(iri, separator, pefix, outputResourceURI)
		oml.addVocabularyExtension(vocabulary, relativePath, null)
		vocabulary
	}

	// Eobject

	protected dispatch def void addToVocabulary(EObject eObject, Vocabulary vocabulary) {
		// no handling by default
	}

	//EClass

	protected dispatch def void addToVocabulary(EClass eClass, Vocabulary vocabulary) {
		val term = if (eClass.isAnnotationSet(Annotation.reifiedRelationship)) {
			val source = eClass.EAllReferences.findFirst[isAnnotationSet(Annotation.source)]
			val target = eClass.EAllReferences.findFirst[isAnnotationSet(Annotation.target)]
			val forward = eClass.getAnnotationValue(Annotation.forward)
			val inverse = eClass.getAnnotationValue(Annotation.inverse)
			val reifiedRelationship = oml.addRelationEntity(
				vocabulary,
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
			oml.addForwardRelation(
				reifiedRelationship,
				forward)
			if (inverse !== null) {
				oml.addInverseRelation(
					reifiedRelationship,
					inverse)
			}
			reifiedRelationship
		} else if (eClass.isAbstract) {
			oml.addAspect(vocabulary, eClass.realName)
		} else {
			oml.addConcept(vocabulary, eClass.realName)
		}
		eClass.ESuperTypes.forEach[superTerm| oml.addSpecializationAxiom(vocabulary, term.iri, superTerm.iri)]
	}

	//EEnum

	protected dispatch def void addToVocabulary(EEnum eEnum, Vocabulary vocabulary) {
		oml.addEnumeratedScalar(vocabulary, eEnum.realName, eEnum.ELiterals.map[oml.createQuotedLiteral(name, null, null)])
	}	

	//EDataType

	protected dispatch def void addToVocabulary(EDataType eDataType, Vocabulary vocabulary) {
		oml.addFacetedScalar(vocabulary, eDataType.realName, null, null, null, null, null, null, null, null, null)
	}	

	//EAttribute

	protected dispatch def void addToVocabulary(EAttribute eAttribute, Vocabulary vocabulary) {
		val domain = eAttribute.EContainingClass
		val range = eAttribute.EType
		if (eAttribute.isAnnotationSet(Annotation.annotationProperty)) {
			oml.addAnnotationProperty(
				vocabulary, 
				eAttribute.realName+"Of"+domain.realName)			
		} else {
			oml.addScalarProperty(
				vocabulary, 
				eAttribute.realName+"Of"+domain.realName, 
				domain.iri, 
				range.iri, 
				eAttribute.upperBound === 1
			)
		}
	}

	//ERference
	
	protected dispatch def void addToVocabulary(EReference eReference, Vocabulary vocabulary) {
		val source = eReference.EContainingClass
		val target = eReference.EType
		val opposite = eReference.EOpposite
		
		if (eReference.isAnnotationSet(Annotation.source) || 
			eReference.isAnnotationSet(Annotation.target) ||
			target.isAnnotationSet(Annotation.ignore)) {
			return
		}
				
		
		val reifiedRelationship = oml.addRelationEntity(
			vocabulary,
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
		oml.addForwardRelation(
			reifiedRelationship,
			eReference.realName+"Of"+source.realName)
		if (opposite !== null) {
			oml.addInverseRelation(
				reifiedRelationship,
				opposite.realName+"of"+target.realName)
		}
	}

	// Utilities

	protected dispatch def String getIri(EPackage ePackage) {
		var nsURI = ePackage.nsURI
		if (nsURI.endsWith('#') || nsURI.endsWith('/')) {
			nsURI = nsURI.substring(0, nsURI.length-1)
		}
		nsURI
	}	

	protected def String getSeparator(EPackage ePackage) {
		var nsURI = ePackage.nsURI
		if (nsURI.endsWith('#') || nsURI.endsWith('/')) {
			nsURI.substring(nsURI.length-1, nsURI.length-1)
		} else {
			"#"
		}
	}	

	protected def String getPrefix(EPackage ePackage) {
		ePackage.nsPrefix
	}	

	protected dispatch def String getIri(EClass eClass) {
		eClass.EPackage.iri + eClass.EPackage.separator + eClass.realName
	}	

	protected dispatch def String getIri(EEnum eEnum) {
		eEnum.EPackage.iri + eEnum.EPackage.separator + eEnum.realName
	}	

	protected dispatch def String getIri(EDataType eDataType) {
		switch (eDataType.realName) {
			case EcorePackage.Literals.ESTRING.name: XSD+'#string'
			case EcorePackage.Literals.EINT.name: XSD+'#int'
			case EcorePackage.Literals.EINTEGER_OBJECT.name: XSD+'#int'
			case EcorePackage.Literals.EBOOLEAN.name: XSD+'#boolean'
			case EcorePackage.Literals.EDOUBLE.name: XSD+'#double'
			case EcorePackage.Literals.EDOUBLE_OBJECT.name: XSD+'#double'
			case EcorePackage.Literals.EFLOAT.name: XSD+'#float'
			case EcorePackage.Literals.EFLOAT_OBJECT.name: XSD+'#float'
			case EcorePackage.Literals.EBIG_DECIMAL.name: XSD+'#decimal'
			default: eDataType.EPackage.iri + eDataType.EPackage.separator + eDataType.name 	
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