package io.opencaesar.ecore2oml.util;

import static io.opencaesar.ecore2oml.util.NameSpaces.DC;
import static io.opencaesar.ecore2oml.util.NameSpaces.OML;
import static io.opencaesar.ecore2oml.util.NameSpaces.RDFS;
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
import io.opencaesar.ecore2oml.Ecore2Oml;
import io.opencaesar.oml.AnnotatedElement;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Member;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.SeparatorKind;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlWriter;

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
			filter(i -> OML.equals(i.getSource())).findFirst();
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
	
	public static void addGeneratedAnnotation(Member object, OmlWriter oml, Vocabulary vocabulary) {
		Literal generated = oml.createQuotedLiteral(vocabulary, "generated", null, null);
		oml.addAnnotation(vocabulary, OmlRead.getIri(object), DC+"/source", generated);
	}
	
	public static void addLabelAnnotatiopnIfNeeded(Member object, String mappedName, OmlWriter oml, Vocabulary vocabulary) {
		String originalName = object.getName();
		if (!originalName.equals(mappedName)) {
			Literal label = oml.createQuotedLiteral(vocabulary, originalName, null, null);
			oml.addAnnotation(vocabulary, OmlRead.getIri(object), RDFS+"#label", label);
		}
	}
	
	public static void addLabelAnnotationIfNeeded(ENamedElement object, Member element, OmlWriter oml, Vocabulary vocabulary) {
		if (!object.getName().equals(element.getName())) {
			Literal label = oml.createQuotedLiteral(vocabulary, object.getName(), null, null);
			oml.addAnnotation(vocabulary, OmlRead.getIri(element), DC+"#title", label);
		}
	}
	
	public static void addTitle(ENamedElement object, AnnotatedElement element, OmlWriter oml, Vocabulary vocabulary) {
		String splitted = splitCamelCase(object.getName());
		Literal label = oml.createQuotedLiteral(vocabulary, splitted, null, null);
		oml.addAnnotation(element, RDFS+"#label", label);
	}
	
	// TODO : may be user org.apache.commons.lang.StringUtils.splitByCharacterTypeCamelCase instead
	private static String splitCamelCase(String s) {
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
	
	public static String getIri(final ENamedElement object, Vocabulary vocabulary, OmlWriter oml,Ecore2Oml e2o) {
		if (object instanceof EPackage) {
			return getIri((EPackage) object);
		} else if (object instanceof EClass) {
			return getIri((EClass) object,vocabulary,oml,e2o);
		} else if (object instanceof EEnum) {
			return getIri((EEnum) object,vocabulary,oml,e2o);
		} else if (object instanceof EDataType) {
			return getIri((EDataType) object,vocabulary,oml,e2o);
		} else if (object instanceof EStructuralFeature) {
			return getIri((EStructuralFeature) object);
		}
		return null;
	}

	static public String getIri(EPackage object) {
		String nsURI = object.getNsURI();
		if (nsURI.endsWith("#") || nsURI.endsWith("/")) {
			nsURI = nsURI.substring(0, nsURI.length()-1);
		}
		return URIMapper.getInstance().getMappedIRI(nsURI);
	}	
	
	
	public static String buildIRIFromClassName(EPackage ePackage, String name) {
		return getIri(ePackage)+ getSeparator(ePackage)+name;
	}

	public static String getIri(EClass object, Vocabulary vocabulary, OmlWriter oml,Ecore2Oml e2o) {
		final EPackage ePackage = object.getEPackage();  
		if (ePackage != null) {
			return qualify(getIri(ePackage)+ getSeparator(ePackage)+ getMappedName(object), object,vocabulary,oml,e2o);
		}
		return null;
	}	

	public static String getIri(EDataType object, Vocabulary vocabulary, OmlWriter oml, Ecore2Oml e2o) {
		final EPackage ePackage = object.getEPackage();  
		if (ePackage != null) {
			return qualify(getIri(ePackage)+ getSeparator(ePackage)+ getMappedName(object), object,vocabulary,oml,e2o);
		}
		return null;
	}	

	static public String getIri(EStructuralFeature object) {
		final EPackage ePackage = object.getEContainingClass().getEPackage();  
		return getIri(ePackage)+ getSeparator(ePackage)+ getMappedName(object);
	}	

	static private String qualify(String iri, EClassifier object, Vocabulary vocabulary, OmlWriter oml,Ecore2Oml e2o) {
		final String vocabularyIri = getIri(object.getEPackage());
		if (!vocabularyIri.equals(vocabulary.getIri())) {
			if (!vocabulary.getOwnedImports().stream().anyMatch(i -> i.getUri().equals(vocabularyIri))) {
				oml.addVocabularyExtension(vocabulary, vocabularyIri, null);
				e2o.addExternalDepenedncy(vocabularyIri,object.getEPackage());
			}
		}
		return iri;
	}
	
	static public void handleNamedElementDoc(ENamedElement element, AnnotatedElement object, OmlWriter oml, Vocabulary vocabulary) {
		EAnnotation genModelAnnotation = element.getEAnnotation(GEN_MODEL);
		if (genModelAnnotation!=null) {
			String val = genModelAnnotation.getDetails().get(DOCUMENTATION);
			if (val!=null && !val.isBlank()) {
				Literal value = oml.createQuotedLiteral(vocabulary, val, null, null);
				oml.addAnnotation(object, DC+"#description", value);
			}
		}
	}
	
	static public void setSemanticFlags(String iri,RelationEntity entity) {
		setSemanticFlags(iri, entity, true);
	}

	
	static public void setSemanticFlags(String iri, RelationEntity entity, boolean value) {
		Set<SemanticFlagKind> flags = SemanticFlags.getInstance().getSemanticFlags(iri);
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
}
