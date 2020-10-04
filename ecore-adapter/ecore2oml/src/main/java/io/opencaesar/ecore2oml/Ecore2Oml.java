package io.opencaesar.ecore2oml;

import static io.opencaesar.ecore2oml.NameSpaces.OWL;
import static io.opencaesar.ecore2oml.Util.addLabelAnnotatiopnIfNeeded;
import static io.opencaesar.ecore2oml.Util.getIri;
import static io.opencaesar.ecore2oml.Util.getMappedName;
import static io.opencaesar.ecore2oml.Util.getPrefix;
import static io.opencaesar.ecore2oml.Util.getSeparator;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreSwitch;

import io.opencaesar.ecore2oml.handlers.ConversionHandler;
import io.opencaesar.ecore2oml.handlers.EAttributeHandler;
import io.opencaesar.ecore2oml.handlers.EClassHandler;
import io.opencaesar.ecore2oml.handlers.EDataTypeHandler;
import io.opencaesar.ecore2oml.handlers.EReferenceHandler;
import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.ecore2oml.preprocessors.ConversionPreProcessing;
import io.opencaesar.ecore2oml.preprocessors.EAttributeConversionParticipant;
import io.opencaesar.ecore2oml.preprocessors.EPackageConversionParticipant;
import io.opencaesar.ecore2oml.preprocessors.EReferencConversionParticipant;
import io.opencaesar.oml.EnumeratedScalar;
import io.opencaesar.oml.Literal;
import io.opencaesar.oml.SeparatorKind;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlWriter;

public class Ecore2Oml extends EcoreSwitch<EObject> {
	
	// pre processors regitery
	private static final Set<Class<? extends ConversionPreProcessing >> preProcessorsRegistery = ConcurrentHashMap.newKeySet();
	private static final Map<Integer, ConversionHandler> handlers = new HashMap<>();
	
	static {
		preProcessorsRegistery.add(ConversionPreProcessing.class);
	}
	
	static {
		initHandlers();
	}
	


	private Vocabulary vocabulary;
	private final EPackage ePackage;
	private final URI outputResourceURI;
	private final OmlWriter oml;
	private Set<ConversionPreProcessing> preprocessors = new HashSet<>();
	
	private Logger LOGGER = LogManager.getLogger(Ecore2Oml.class);
	
	private Map<CollectionKind,Object> collections = new HashMap<>();
	
	public Ecore2Oml(EPackage ePackage, URI outputResourceURI, OmlWriter oml) {
		this.ePackage = ePackage;
		this.outputResourceURI = outputResourceURI;
		this.oml = oml;
		initPreProcessors();
	}
	
	public void run() {
		for (ConversionPreProcessing preProcessor : preprocessors) {
			preProcessor.run(ePackage);
		}
		for (ConversionPreProcessing preProcessor : preprocessors) {
			preProcessor.finished();
		}
		doSwitch(ePackage);
		handlersPostProcess();
	}

	private void handlersPostProcess() {
		Set<Entry<Integer, ConversionHandler>> entries = handlers.entrySet();
		for (Entry<Integer, ConversionHandler> entry : entries) {
			entry.getValue().postConvert(vocabulary, oml, collections);
		}
	}

	@Override
	public EObject caseEPackage(EPackage object) {
		final String iri = getIri(object);
		final SeparatorKind separator = getSeparator(object);
		final String pefix = getPrefix(object);
		
		vocabulary = oml.createVocabulary(outputResourceURI, iri, separator, pefix);
		oml.addVocabularyExtension(vocabulary, OWL, null);
		object.getEClassifiers().stream().forEach(c -> doSwitch(c));
		
		return vocabulary;
	}

	@Override
	public EObject caseEEnum(EEnum object) {
		final String name = getMappedName(object);
		final Literal[] literals = object.getELiterals().stream().map(i -> doSwitch(i)).toArray(Literal[]::new);

		final EnumeratedScalar scalar = oml.addEnumeratedScalar(vocabulary, name, literals);
		addLabelAnnotatiopnIfNeeded(scalar, name,oml,vocabulary);
		
		return scalar;
	}
	
	@Override
	public EObject caseEEnumLiteral(EEnumLiteral object) {
		return oml.createQuotedLiteral(vocabulary, getMappedName(object), null, null);
	}

	@Override
	public EObject caseEDataType(EDataType object) {
		return handlers.get(EcorePackage.EDATA_TYPE).convert(object, vocabulary, oml, collections);
	}

	@Override
	public EObject caseEClass(EClass object) {
		EObject entity = handlers.get(EcorePackage.ECLASS).convert(object, vocabulary, oml, collections);
		object.getEStructuralFeatures().stream().forEach(f -> doSwitch(f));
		return entity;
	}


	@Override
	public EObject caseEAttribute(EAttribute object) {
		return handlers.get(EcorePackage.EATTRIBUTE).convert(object, vocabulary, oml, collections);
	}

	
	
	@Override
	public EObject caseEReference(EReference object) {
		return handlers.get(EcorePackage.EREFERENCE).convert(object, vocabulary, oml, collections);
	}

	//----------------------------------------------------------------------
	// Utilities
	//----------------------------------------------------------------------
	
	private void initPreProcessors(){
		for (Class<? extends ConversionPreProcessing> class1 : preProcessorsRegistery) {
			try {
				ConversionPreProcessing pre = class1.getDeclaredConstructor().newInstance();
				pre.setCollections(collections);
				preprocessors.add(pre);
				// TODO: read from File
				pre.addParticipant(EcorePackage.EATTRIBUTE, EAttributeConversionParticipant.class);
				pre.addParticipant(EcorePackage.EPACKAGE, EPackageConversionParticipant.class);
				pre.addParticipant(EcorePackage.EREFERENCE, EReferencConversionParticipant.class);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				LOGGER.error("Failed to create instance of ConversionPreProcessing");
			}
		}
	}
	
	private static void initHandlers() {
		// TODO : get classes from file if needed
		handlers.put(EcorePackage.EDATA_TYPE, new EDataTypeHandler());
		handlers.put(EcorePackage.EATTRIBUTE, new EAttributeHandler());
		handlers.put(EcorePackage.EREFERENCE, new EReferenceHandler());
		handlers.put(EcorePackage.ECLASS, new EClassHandler());
	}
}
