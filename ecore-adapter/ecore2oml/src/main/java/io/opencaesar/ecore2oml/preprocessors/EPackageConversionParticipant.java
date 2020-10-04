package io.opencaesar.ecore2oml.preprocessors;

import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;

public class EPackageConversionParticipant extends ConversionParticipant {
	
	private Logger LOGGER = LogManager.getLogger(EPackageConversionParticipant.class);

	@Override
	public void handle(EObject element, Map<CollectionKind, Object> collections) {
		LOGGER.info("Hanlding : " + element);
		
	}

	@Override
	public void postProcess(Map<CollectionKind, Object> collections) {
		LOGGER.info("Post Processing");
		
	}

	

	

}
