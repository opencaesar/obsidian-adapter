package io.opencaesar.ecore2oml.preprocessors;

import java.util.Map;

import org.eclipse.emf.ecore.EObject;

public abstract class ConversionParticipant{
	
	abstract public void handle(EObject element, Map<CollectionKind,Object> collections);
	abstract public void postProcess(Map<CollectionKind,Object> collections);

}
