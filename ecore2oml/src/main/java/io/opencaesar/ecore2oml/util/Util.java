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

import static io.opencaesar.oml.util.OmlRead.getMembers;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.exists;

import java.util.Optional;
import java.util.Set;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.lib.StringExtensions;

import io.opencaesar.ecore2oml.AnnotationKind;
import io.opencaesar.ecore2oml.ConversionContext;
import io.opencaesar.ecore2oml.Ecore2Oml;
import io.opencaesar.oml.AnnotatedElement;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.SeparatorKind;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlBuilder;
import io.opencaesar.oml.util.OmlConstants;

public class Util {
	
	private static final String GEN_MODEL = "http://www.eclipse.org/emf/2002/GenModel";
	private static final Object DOCUMENTATION = "documentation"; 

	public static String getMappedName(ENamedElement object) {
		return getMappedName(object,false);
	}
	
	public static String getMappedName(ENamedElement object, boolean CAP_ALL_SEGMENTS) {
		String defaultValue =  object.getName();
		// make default value qualified in the case of eStructural Features
		if (object instanceof EStructuralFeature) {
			EStructuralFeature eFeature = (EStructuralFeature)object;
			EClass container = eFeature.getEContainingClass();
			if (container!=null) {
				defaultValue =  (!CAP_ALL_SEGMENTS ? StringExtensions.toFirstLower(container.getName()) : container.getName()) +
								Constants.NAME_SEPERATOR +
								(CAP_ALL_SEGMENTS ? StringExtensions.toFirstUpper(defaultValue) : defaultValue );
			}
		}
		return getAnnotationValue(object, AnnotationKind.name, defaultValue);
	}
	
	public static String getAnnotationValue(EModelElement object, AnnotationKind kind) {
		final Optional<EAnnotation> annotation = object.getEAnnotations().stream().
			filter(i -> OmlConstants.OML_IRI.equals(i.getSource())).findFirst();
		if (annotation.isPresent()) {
			return annotation.get().getDetails().get(kind.toString());
		}
		return null;
	}
	
	public static String getAnnotationValue(EModelElement object, AnnotationKind kind, String defaultValue) {
		final String value = getAnnotationValue(object, kind);
		return (value != null) ? value : defaultValue;
	}
	
		
	public static boolean isAnnotationSet(EModelElement object, AnnotationKind kind) {
		return getAnnotationValue(object, kind) != null;
	}
	
	public static EAnnotation getAnnotation(EModelElement object, String source) {
		final EAnnotation[] retVal = new EAnnotation[1];
		object.getEAnnotations().forEach(annotation->{
			if (source.equals(annotation.getSource())) {
				retVal[0] = annotation;
			}
		});
		return retVal[0];
	}
	
	public static void addGeneratedAnnotation(Member object, OmlBuilder oml, Vocabulary vocabulary) {
		Literal generated = oml.createQuotedLiteral(vocabulary, "generated", null, null);
		oml.addAnnotation(vocabulary, object, OmlConstants.DC_NS+"source", generated);
		addVocabularyExtensionIfNeeded(vocabulary, OmlConstants.DC_IRI, SeparatorKind.SLASH, OmlConstants.DC_PREFIX, oml);
	}
	
	public static void addLabelAnnotation(Member member, OmlBuilder oml, Vocabulary vocabulary) {
		String splitted = splitCamelCase(member.getName().replaceAll("_", ""));
		Literal label = oml.createQuotedLiteral(vocabulary, splitted, null, null);
		oml.addAnnotation(vocabulary, member, OmlConstants.RDFS_NS+"label", label);
		addVocabularyExtensionIfNeeded(vocabulary, OmlConstants.RDFS_IRI, SeparatorKind.HASH, OmlConstants.RDFS_PREFIX, oml);
	}
	
	public static void addLabelAnnotation(ENamedElement object, AnnotatedElement element, OmlBuilder oml, Vocabulary vocabulary) {
		String splitted = splitCamelCase(object.getName());
		Literal label = oml.createQuotedLiteral(vocabulary, splitted, null, null);
		if (element instanceof Vocabulary) {
			oml.addAnnotation((Vocabulary)element, OmlConstants.RDFS_NS+"label", label);
			addVocabularyExtensionIfNeeded((Vocabulary)element, OmlConstants.RDFS_IRI, SeparatorKind.HASH, OmlConstants.RDFS_PREFIX, oml);
		} else {
			oml.addAnnotation(vocabulary, element, OmlConstants.RDFS_NS+"label", label);
			addVocabularyExtensionIfNeeded(vocabulary, OmlConstants.RDFS_IRI, SeparatorKind.HASH, OmlConstants.RDFS_PREFIX, oml);
		}
	}
	
	public static void addTitleAnnotationIfNeeded(ENamedElement object, Member member, OmlBuilder oml, Vocabulary vocabulary) {
		if (!object.getName().equals(member.getName())) {
			Literal label = oml.createQuotedLiteral(vocabulary, object.getName(), null, null);
			oml.addAnnotation(vocabulary, member, OmlConstants.DC_NS+"title", label);
			addVocabularyExtensionIfNeeded(vocabulary, OmlConstants.DC_IRI, SeparatorKind.SLASH, OmlConstants.DC_PREFIX, oml);
		}
	}
	
	static public void addDescriptionAnnotation(ENamedElement element, AnnotatedElement object, OmlBuilder oml, Vocabulary vocabulary) {
		EAnnotation genModelAnnotation = element.getEAnnotation(GEN_MODEL);
		if (genModelAnnotation!=null) {
			String val = genModelAnnotation.getDetails().get(DOCUMENTATION);
			if (val!=null && !val.isBlank()) {
				Literal value = oml.createQuotedLiteral(vocabulary, val, null, null);
				oml.addAnnotation(vocabulary, object, OmlConstants.DC_NS+"description", value);
				addVocabularyExtensionIfNeeded(vocabulary, OmlConstants.DC_IRI, SeparatorKind.SLASH, OmlConstants.DC_PREFIX, oml);
			}
		}
	}
	
	// TODO : may be user org.apache.commons.lang.StringUtils.splitByCharacterTypeCamelCase instead
	private static String splitCamelCase(String s) {
		s = s.substring(0, 1).toUpperCase() + s.substring(1);
		return s.replaceAll(
		      String.format("%s|%s|%s",
		         "(?<=[A-Z])(?=[A-Z][a-z])",
		         "(?<=[^A-Z])(?=[A-Z])",
		         "(?<=[A-Za-z])(?=[^A-Za-z])"
		      ),
		      " "
		   );
		}
	
	public static String getPrefix(EPackage object) {
		return object.getNsPrefix();
	}	

	public static SeparatorKind getSeparator(EPackage object) {
		final String nsURI = object.getNsURI();
		if (nsURI.endsWith("/")) {
			return SeparatorKind.SLASH;
		} else {
			return SeparatorKind.HASH;
		}
	}
	
	public static boolean memberExists(String name, Vocabulary vocabulary) {
		return exists(getMembers(vocabulary), i -> i.getName().equals(name));		
	}
	
	public static String getIri(final ENamedElement object, Vocabulary vocabulary, OmlBuilder oml,Ecore2Oml e2o) {
		if (object instanceof EPackage) {
			return getIri((EPackage) object, e2o.context);
		} else if (object instanceof EClass) {
			return getIri((EClass) object,vocabulary,oml,e2o);
		} else if (object instanceof EEnum) {
			return getIri((EEnum) object,vocabulary,oml,e2o);
		} else if (object instanceof EDataType) {
			return getIri((EDataType) object,vocabulary,oml,e2o);
		} else if (object instanceof EStructuralFeature) {
			return getIri((EStructuralFeature) object, e2o.context);
		}
		return null;
	}

	static public String getIri(EPackage object, ConversionContext context) {
		String nsURI = object.getNsURI();
		if (nsURI.endsWith("#") || nsURI.endsWith("/")) {
			nsURI = nsURI.substring(0, nsURI.length()-1);
		}
		return context.uriMapper.getMappedIRI(nsURI);
	}	
	
	static public String getSeparatorChar(EPackage object) {
		String nsURI = object.getNsURI();
		if (nsURI.endsWith("#") || nsURI.endsWith("/")) {
			return nsURI.substring( nsURI.length()-1, nsURI.length());
		}
		return SeparatorKind.HASH.toString();
	}	
	
	public static String buildIRIFromClassName(EPackage ePackage, String name, ConversionContext context) {
		return getIri(ePackage, context)+ getSeparator(ePackage)+name;
	}

	public static String getIri(EClass object, Vocabulary vocabulary, OmlBuilder oml,Ecore2Oml e2o) {
		final EPackage ePackage = object.getEPackage();  
		if (ePackage != null) {
			return qualify(getIri(ePackage, e2o.context)+ getSeparator(ePackage)+ getMappedName(object), object,vocabulary,oml,e2o);
		}
		return null;
	}
	
	public static String getLocalEClassIri(EClass eClass, ConversionContext context) {
		final EPackage ePackage = eClass.getEPackage();  
		if (ePackage != null) {
			return getIri(ePackage, context)+ getSeparator(ePackage)+ getMappedName(eClass);
		}
		return null;
	}

	public static String getIri(EDataType object, Vocabulary vocabulary, OmlBuilder oml, Ecore2Oml e2o) {
		final EPackage ePackage = object.getEPackage();  
		if (ePackage != null) {
			return qualify(getIri(ePackage, e2o.context)+ getSeparator(ePackage)+ getMappedName(object), object,vocabulary,oml,e2o);
		}
		return null;
	}	

	static public String getIri(EStructuralFeature object, ConversionContext context) {
		final EPackage ePackage = object.getEContainingClass().getEPackage();  
		return getIri(ePackage, context)+ getSeparator(ePackage)+ getMappedName(object);
	}	

	static private String qualify(String iri, EClassifier object, Vocabulary vocabulary, OmlBuilder oml,Ecore2Oml e2o) {
		final String vocabularyIri = getIri(object.getEPackage(), e2o.context);
		final SeparatorKind vocabularySeparator = getSeparator(object.getEPackage());
		final String vocabularyPrefix = getPrefix(object.getEPackage());
		if (!vocabularyIri.equals(vocabulary.getIri())) {
			if (addVocabularyExtensionIfNeeded(vocabulary, vocabularyIri, vocabularySeparator, vocabularyPrefix, oml)) {
				e2o.addExternalDepenedncy(vocabularyIri, object.getEPackage());
			}
		}
		return iri;
	}
	
	static public boolean defaultsToAspect(EClass object) {
		return (object.eIsProxy() || object.isAbstract() || object.isInterface())
				&& object.getESuperTypes().stream().allMatch(i -> defaultsToAspect(i));
	}
	
	static public void setSemanticFlags(String iri,RelationEntity entity, ConversionContext context) {
		setSemanticFlags(iri, entity, true, context);
	}

	
	static public void setSemanticFlags(String iri, RelationEntity entity, boolean value, ConversionContext context) {
		Set<SemanticFlagKind> flags = context.semanticFlags.getSemanticFlags(iri);
		for (SemanticFlagKind flag : flags) {
			switch (flag) {
			case asymmetric:
				if (value)	entity.setAsymmetric(true);
				else entity.setSymmetric(true);
				break;
			case irreflexive:
				if(value)	entity.setIrreflexive(true);
				else	entity.setReflexive(true);
				break;
			case reflexive:
				if (value) entity.setReflexive(true);
				else entity.setIrreflexive(true);
				break;
			case symmetric:
				if (value) entity.setSymmetric(true);
				else entity.setAsymmetric(true);
				break;
			case transitive:
				entity.setTransitive(true);
				break;
			}
		}
	}

	static public boolean addVocabularyExtensionIfNeeded(Vocabulary context, String importIri, SeparatorKind separator, String importPrefix, OmlBuilder oml) {
		if (!context.getIri().equals(importIri)) {
			boolean imported = context.getOwnedImports().stream()
				.filter(i -> i.getIri().equals(importIri))
				.findFirst().isPresent();
			if (!imported) {
				oml.addVocabularyExtension(context, importIri, separator, importPrefix);
				return true;
			}
		}
		return false;
	}
}
