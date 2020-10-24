package io.opencaesar.ecore2oml.handlers;

import static io.opencaesar.ecore2oml.util.Util.getMappedName;

import java.util.Map;

import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EObject;

import io.opencaesar.ecore2oml.Ecore2Oml;
import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlWriter;

public class EEnumHandler implements ConversionHandler {

	@Override
	public EObject doConvert(EObject eObject, Vocabulary vocabulary, OmlWriter oml,
			Map<CollectionKind, Object> collections,Ecore2Oml visitor) {
		EEnum object = (EEnum)eObject;
		final String name = getMappedName(object);
		final Literal[] literals = object.getELiterals().stream().map(i -> visitor.doSwitch(i)).toArray(Literal[]::new);
		return oml.addEnumeratedScalar(vocabulary, name, literals);
	}

}
