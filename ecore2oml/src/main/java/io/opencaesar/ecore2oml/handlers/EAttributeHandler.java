package io.opencaesar.ecore2oml.handlers;

import static io.opencaesar.ecore2oml.util.Util.addGeneratedAnnotation;
import static io.opencaesar.ecore2oml.util.Util.getAnnotationValue;
import static io.opencaesar.ecore2oml.util.Util.getIri;
import static io.opencaesar.ecore2oml.util.Util.getMappedName;
import static io.opencaesar.ecore2oml.util.Util.isAnnotationSet;
import static io.opencaesar.ecore2oml.util.Util.memberExists;

import java.util.Map;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.lib.StringExtensions;

import io.opencaesar.ecore2oml.AnnotationKind;
import io.opencaesar.ecore2oml.Ecore2Oml;
import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.ecore2oml.preprocessors.CollisionInfo;
import io.opencaesar.ecore2oml.util.Constants;
import io.opencaesar.ecore2oml.util.FilterUtil;
import io.opencaesar.oml.AnnotationProperty;
import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.CardinalityRestrictionKind;
import io.opencaesar.oml.Property;
import io.opencaesar.oml.RangeRestrictionKind;
import io.opencaesar.oml.ScalarProperty;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlWriter;

public class EAttributeHandler implements ConversionHandler {

	static public ScalarProperty handleEAttributeToScalarProperty(EAttribute object, String domainIri,
			OmlWriter oml, Vocabulary vocabulary, Map<CollectionKind, Object> collections,Ecore2Oml e2o) {
		final String name = getMappedName(object);
		// check for collision
		@SuppressWarnings("unchecked")
		Map<String, CollisionInfo> names = (Map<String, CollisionInfo>) collections
				.get(CollectionKind.CollidingAttributes);
		CollisionInfo collisionInfo = names!=null ? names.get(name) : null;
		final boolean isFunctional = object.getUpperBound() == 1;
		String containerIRI = getIri(object.getEContainingClass(), vocabulary, oml,e2o);
		String attribuiteIRI =  getIri(object, vocabulary, oml,e2o);
		final String rangeIRI = getIri(object.getEAttributeType(), vocabulary, oml,e2o);
		if (collisionInfo != null) {
			// fix the rangeIRI
			String realName = Constants.BASE_PREFIX + StringExtensions.toFirstUpper(name);
			if (!memberExists(realName, vocabulary)) {
				collisionInfo.baseConcept = oml.addAspect(vocabulary, realName);
				collisionInfo.baseProperty = oml.addScalarProperty(vocabulary, name,
						OmlRead.getIri(collisionInfo.baseConcept), rangeIRI, isFunctional);
			}
			oml.addSpecializationAxiom(vocabulary, containerIRI,
					OmlRead.getIri(collisionInfo.baseConcept));
			if (!collisionInfo.sameType() && !collisionInfo.getName().equals("value")) {
				oml.addScalarPropertyRangeRestrictionAxiom(vocabulary,
						containerIRI,
						OmlRead.getIri(collisionInfo.baseProperty), rangeIRI, RangeRestrictionKind.ALL);

			}
			if (object.getUpperBound()>1) {
				oml.addScalarPropertyCardinalityRestrictionAxiom(vocabulary, containerIRI, attribuiteIRI,
						CardinalityRestrictionKind.MAX, object.getUpperBound(), null);
			}
			if (object.getLowerBound()> 0) {
				oml.addScalarPropertyCardinalityRestrictionAxiom(vocabulary, containerIRI, attribuiteIRI,
						CardinalityRestrictionKind.MIN, object.getLowerBound(), null);
			}
			return collisionInfo.baseProperty;
		}
		return oml.addScalarProperty(vocabulary, name, domainIri, rangeIRI, isFunctional);
	}

	@Override
	public EObject doConvert(EObject oObject, Vocabulary vocabulary, OmlWriter oml,
			Map<CollectionKind, Object> collections,Ecore2Oml visitor) {
		EAttribute object = (EAttribute) oObject;
		final EClass domain = object.getEContainingClass();
		
		if (FilterUtil.shouldFilter(object)) {
			return null;
		}
		
		// find the domain
		String domainIri = getIri(domain, vocabulary, oml,visitor);
		if (isAnnotationSet(object, AnnotationKind.domainName)) {
			final String aspectName = getAnnotationValue(object, AnnotationKind.domainName);
			if (!memberExists(aspectName, vocabulary)) {
				final Aspect aspect = oml.addAspect(vocabulary, aspectName);
				addGeneratedAnnotation(aspect, oml, vocabulary);
			}
			final String aspectIri = OmlRead.getNamespace(vocabulary) + aspectName;
			oml.addSpecializationAxiom(vocabulary, getIri(domain, vocabulary, oml,visitor), aspectIri);
			domainIri = aspectIri;
		}

		// create Property
		Property property = null;
		if (isAnnotationSet(object, AnnotationKind.isAnnotationProperty)) {
			property = caseEAttributeToAnnotationProperty(object, oml, vocabulary);
		} else {
			property = handleEAttributeToScalarProperty(object, domainIri, oml, vocabulary, collections,visitor);
		}
		return property;
	}

	static private AnnotationProperty caseEAttributeToAnnotationProperty(EAttribute object, OmlWriter oml,
			Vocabulary vocabulary) {
		final String name = getMappedName(object);
		return oml.addAnnotationProperty(vocabulary, name);
	}

}
