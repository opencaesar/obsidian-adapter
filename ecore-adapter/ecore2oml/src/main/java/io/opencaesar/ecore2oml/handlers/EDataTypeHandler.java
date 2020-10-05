package io.opencaesar.ecore2oml.handlers;

import static io.opencaesar.ecore2oml.Util.addLabelAnnotatiopnIfNeeded;

import java.util.Map;

import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;

import io.opencaesar.ecore2oml.IRICONSTANTS;
import io.opencaesar.ecore2oml.Util;
import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.oml.FacetedScalar;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlWriter;

public class EDataTypeHandler implements ConversionHandler{

	@Override
	public EObject convert(EObject eObject, Vocabulary vocabulary, OmlWriter oml, Map<CollectionKind, Object> collections) {
		EDataType object = (EDataType)eObject;
		final String name = Util.getMappedName(object);
		final FacetedScalar scalar = oml.addFacetedScalar(vocabulary, name, null, null, null, null, null, null, null, null, null);
		String base = "";
		switch (name) {
			case "Boolean":
				base = "boolean";
				break;
			case "Integer":
				base = "integer";
				break;
			case "Real":
				base = "double";
				break;
			case "UnlimitedNatural":
				base = "integer";
				break;
			default:
				base = "string";
				break;
		}
		String baseIRI = IRICONSTANTS.XSD_IRI + base;
		oml.addSpecializationAxiom(vocabulary, OmlRead.getIri(scalar), baseIRI);
		addLabelAnnotatiopnIfNeeded(scalar, name,oml,vocabulary);
		return null;
	}

}
