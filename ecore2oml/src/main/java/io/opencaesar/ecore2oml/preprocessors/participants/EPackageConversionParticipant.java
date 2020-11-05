package io.opencaesar.ecore2oml.preprocessors.participants;

import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;

import io.opencaesar.ecore2oml.preprocessors.CollectionKind;

public class EPackageConversionParticipant extends ConversionParticipant {
	
	private Logger LOGGER = LogManager.getLogger(EPackageConversionParticipant.class);

	@Override
	public void handle(EObject element, Map<CollectionKind, Object> collections) {
		LOGGER.debug("Hanlding : " + element);
		
	}

	@Override
	public void postProcess(Map<CollectionKind, Object> collections) {
		LOGGER.debug("Post Processing");
		
	}

	

	

}
