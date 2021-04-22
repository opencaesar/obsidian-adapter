package io.opencaesar.ecore2oml;

import static io.opencaesar.ecore2oml.util.Util.getMappedName;

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
import io.opencaesar.ecore2oml.handlers.EEnumHandler;
import io.opencaesar.ecore2oml.handlers.EPackageHandler;
import io.opencaesar.ecore2oml.handlers.EReferenceHandler;
import io.opencaesar.ecore2oml.preprocessors.CollectionKind;
import io.opencaesar.ecore2oml.preprocessors.ConversionPreProcessing;
import io.opencaesar.ecore2oml.preprocessors.EAttributeConversionParticipant;
import io.opencaesar.ecore2oml.preprocessors.participants.EClassConversionParticipant;
import io.opencaesar.ecore2oml.preprocessors.participants.EPackageConversionParticipant;
import io.opencaesar.ecore2oml.preprocessors.participants.EReferencConversionParticipant;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.util.OmlWriter;

public class Ecore2Oml extends EcoreSwitch<EObject> {
	
	// pre processors regitery
	private static final Set<Class<? extends ConversionPreProcessing >> preProcessorsRegistery = ConcurrentHashMap.newKeySet();
	private static final Map<Integer, ConversionHandler> handlers = new HashMap<>();
	
	
	static {
		preProcessorsRegistery.add(ConversionPreProcessing.class);
	}
	
	private Vocabulary vocabulary;
	private final EPackage ePackage;
	private final URI outputResourceURI;
	private final OmlWriter oml;
	private Set<ConversionPreProcessing> preprocessors = new HashSet<>();
	private Map<String,EPackage> dependency = new HashMap<>();
	public ConversionContext context;
	
	private Logger LOGGER = LogManager.getLogger(Ecore2Oml.class);
	
	private Map<CollectionKind,Object> collections = new HashMap<>();
	
	public Ecore2Oml(EPackage ePackage, URI outputResourceURI, OmlWriter oml, ConversionContext conversionContext) {
		this.ePackage = ePackage;
		this.outputResourceURI = outputResourceURI;
		this.oml = oml;
		this.context = conversionContext;
		initPreProcessors();
		initHandlers();
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
	
	public void addExternalDepenedncy(String iri, EPackage ePackage) {
		dependency.put(iri,ePackage);
	}
	
	public Map<String,EPackage> getDependencies() {
		return dependency;
	}
	
	private void handlersPostProcess() {
		Set<Entry<Integer, ConversionHandler>> entries = handlers.entrySet();
		for (Entry<Integer, ConversionHandler> entry : entries) {
			entry.getValue().postConvert(vocabulary, oml, collections);
		}
	}

	@Override
	public EObject caseEPackage(EPackage object) {
		return handlers.get(EcorePackage.EPACKAGE).convert(object, null, oml, collections,this);
	}

	@Override
	public EObject caseEEnum(EEnum object) {
		return handlers.get(EcorePackage.EENUM).convert(object, vocabulary, oml, collections,this);
	}
	
	@Override
	public EObject caseEEnumLiteral(EEnumLiteral object) {
		return oml.createQuotedLiteral(vocabulary, getMappedName(object), null, null);
	}

	@Override
	public EObject caseEDataType(EDataType object) {
		return handlers.get(EcorePackage.EDATA_TYPE).convert(object, vocabulary, oml, collections,this);
	}

	@Override
	public EObject caseEClass(EClass object) {
		return handlers.get(EcorePackage.ECLASS).convert(object, vocabulary, oml, collections,this);
	}


	@Override
	public EObject caseEAttribute(EAttribute object) {
		return handlers.get(EcorePackage.EATTRIBUTE).convert(object, vocabulary, oml, collections,this);
	}

	
	
	@Override
	public EObject caseEReference(EReference object) {
		return handlers.get(EcorePackage.EREFERENCE).convert(object, vocabulary, oml, collections,this);
	}

	//----------------------------------------------------------------------
	// Utilities
	//----------------------------------------------------------------------
	
	private void initPreProcessors(){
		for (Class<? extends ConversionPreProcessing> class1 : preProcessorsRegistery) {
			try {
				ConversionPreProcessing pre = class1.getDeclaredConstructor().newInstance();
				pre.setCollections(collections);
				pre.setContext(this.context);
				preprocessors.add(pre);
				// TODO: read from File
				pre.addParticipant(EcorePackage.EATTRIBUTE, EAttributeConversionParticipant.class);
				pre.addParticipant(EcorePackage.EPACKAGE, EPackageConversionParticipant.class);
				pre.addParticipant(EcorePackage.EREFERENCE, EReferencConversionParticipant.class);
				pre.addParticipant(EcorePackage.ECLASS, EClassConversionParticipant.class);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				LOGGER.error("Failed to create instance of ConversionPreProcessing");
			}
		}
	}
	
	private  void initHandlers() {
		// TODO : get classes from file if needed
		handlers.put(EcorePackage.EPACKAGE, new EPackageHandler(outputResourceURI));
		handlers.put(EcorePackage.EENUM, new EEnumHandler());
		handlers.put(EcorePackage.EDATA_TYPE, new EDataTypeHandler());
		handlers.put(EcorePackage.EATTRIBUTE, new EAttributeHandler());
		handlers.put(EcorePackage.EREFERENCE, new EReferenceHandler());
		handlers.put(EcorePackage.ECLASS, new EClassHandler());
	}

	public void setVocabulary(Vocabulary vocabulary2) {
		assert vocabulary2==null : "Vocabulary is set already";
		this.vocabulary = vocabulary2;
		
	}
}
