/**
 * 
 * Copyright 2022 Modelware Solutions LLC and CEA-LIST.
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
 */package io.opencaesar.ecore2oml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.ECrossReferenceAdapter;
import org.eclipse.emf.ecore.xcore.XcoreStandaloneSetup;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.inject.Injector;

import io.opencaesar.oml.dsl.OmlStandaloneSetup;
import io.opencaesar.oml.resource.OmlJsonResourceFactory;
import io.opencaesar.oml.resource.OmlXMIResourceFactory;
import io.opencaesar.oml.util.OmlBuilder;
import io.opencaesar.oml.util.OmlCatalog;
import io.opencaesar.oml.util.OmlConstants;

public class Ecore2OmlApp {

	private static final String ECORE = "ecore";
	private static final String XCORE = "xcore";
	
	@Parameter(
		names= {"--input-folder-path","-i"}, 
		description="Location of input folder (Required)",
		validateWith=InputFolderPath.class, 
		required=true, 
		order=1)
	protected String inputFolderPath = null;

	@Parameter(
		names= {"--output-catalog-path", "-o"}, 
		description="Location of the output OML catalog XML file (Required)", 
		validateWith=OutputCatalogPath.class, 
		required=true, 
		order=2)
	protected String outputCatalogPath;

	@Parameter(
		names= {"--referenced-ecore-path", "-r"}, 
		description="Location of a referenced ecore/xcore file (Optional)", 
		validateWith= InputEcorePath.class,
		required=false, 
		order=3
	)
	protected List<String> referencedEcorePaths = new ArrayList<>();
	
	@Parameter(
		names= {"--input-file-extension","-ie"}, 
		description="Extension of input file (Optional, ecore/xcore by default)",
		required=false, 
		order=4)
	protected List<String> inputFileExtensions = new ArrayList<>();

	@Parameter(
		names= {"--output-file-extension","-oe"}, 
		description="Extension of output file (Optional, oml by default)",
		required=false, 
		order=5)
	protected String outputFileExtension = OmlConstants.OML_EXTENSION;

	@Parameter(
		names= {"--debug", "-d"}, 
		description="Shows debug logging statements", 
		order=6)
	protected boolean debug;

	@Parameter(
		names= {"--help","-h"}, 
		description="Displays summary of options", 
		help=true, 
		order=7) 
	protected boolean help;

	protected Logger LOGGER = LogManager.getLogger(Ecore2OmlApp.class);

	/*
	 * Main method
	 */
	public static void main(String ... args) throws IOException {
		final Ecore2OmlApp app = new Ecore2OmlApp();
		final JCommander builder = JCommander.newBuilder().addObject(app).build();
		builder.parse(args);
		if (app.help) {
			builder.usage();
			return;
		}
		if (app.debug) {
			final Appender appender = LogManager.getRootLogger().getAppender("stdout");
			((AppenderSkeleton)appender).setThreshold(Level.DEBUG);
		}
		if (app.inputFolderPath.endsWith(File.separator)) {
			app.inputFolderPath = app.inputFolderPath.substring(0, app.inputFolderPath.length()-1);
		}
		app.run();
	}

	/*
	 * Run method
	 */
	protected void run() throws IOException {
		LOGGER.info("=================================================================");
		LOGGER.info("                        S T A R T");
		LOGGER.info("                      Ecore to Oml "+getAppVersion());
		LOGGER.info("=================================================================");
		LOGGER.info("Input Folder Path= " + inputFolderPath);
		LOGGER.info("Output Catalog Path= " + outputCatalogPath);
		
		if (inputFileExtensions.isEmpty()) {
			inputFileExtensions.add(ECORE);
			inputFileExtensions.add(XCORE);
		}
		
		final File inputFolder = new File(inputFolderPath);
		final Collection<File> inputFiles = collectInputFiles(inputFolder, inputFileExtensions);
		
		final ResourceSet inputResourceSet = createInputResourceSet();
		
		// load the input models and resolve their references
		List<URI> inputResourceURIs = new ArrayList<>();
		for (File inputFile : inputFiles) {
			final URI inputURI = URI.createFileURI(inputFile.getAbsolutePath());
			inputResourceURIs.add(inputURI);
			LOGGER.info("Reading: "+inputURI);
			inputResourceSet.getResource(inputURI, true);
		}

		// load the Oml registries here after the input have been read
		OmlStandaloneSetup.doSetup();
		OmlXMIResourceFactory.register();
		OmlJsonResourceFactory.register();
		final ResourceSet outputResourceSet = new ResourceSetImpl();
		outputResourceSet.eAdapters().add(new ECrossReferenceAdapter());

		final OmlCatalog catalog = OmlCatalog.create(URI.createFileURI(outputCatalogPath));		

		// create the Oml builder
		final OmlBuilder builder = new OmlBuilder(outputResourceSet);
		
		// start the Oml Builder
		builder.start();

		// convert the input resources
		List<URI> outputResourceURIs = new ArrayList<>(); 
		Set<URI> unconvertedResourceURIs= new HashSet<>(inputResourceURIs); 
		while (!unconvertedResourceURIs.isEmpty()) {
			List<URI> uris = new ArrayList<URI>(unconvertedResourceURIs);
			for (URI uri : uris) {
				Resource inputResource = inputResourceSet.getResource(uri, true);
				Ecore2Oml e2o = createEcore2Oml(inputFolder, inputResource, catalog, builder);
				Set<URI> newURIs = e2o.run();
				assert (!outputResourceURIs.removeAll(newURIs));
				outputResourceURIs.addAll(newURIs);
				unconvertedResourceURIs.addAll(e2o.getImportedURIs());
			}
			unconvertedResourceURIs.removeAll(uris);
		}
		
		// finish the Oml builder
		builder.finish();
		
		// save the output resources here instead of calling builder.save in order to log
		for (URI outputResourceURI : outputResourceURIs) {
			if (outputResourceURI.fileExtension().equals("oml")) {
				LOGGER.info("Saving: "+outputResourceURI);
				final Resource outputResource = outputResourceSet.getResource(outputResourceURI, false);
				outputResource.save(Collections.EMPTY_MAP);
			}
		}

		LOGGER.info("=================================================================");
		LOGGER.info("                          E N D");
		LOGGER.info("=================================================================");
	}

	protected Ecore2Oml createEcore2Oml(File inputFolder, Resource inputResource, OmlCatalog catalog, OmlBuilder builder) {
		return new Ecore2Oml(inputFolder, inputResource, catalog, builder);
	}
	
	protected ResourceSet createInputResourceSet() {
		final Injector injector = new XcoreStandaloneSetup().createInjectorAndDoEMFRegistration();
		final XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
		
		XMLResourceFactoryImpl resourceFactory = new XMLResourceFactoryImpl();
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("*", resourceFactory);
		
		// load any referenced Ecore files
	    EPackage.Registry packageRegistry = injector.getInstance(EPackage.Registry.class);
		for (String path : referencedEcorePaths) {
			final URI inputURI = URI.createFileURI(path);
			Resource inputResource = resourceSet.getResource(inputURI, true);
			if (inputResource != null) {
				final TreeIterator<EObject> i = inputResource.getAllContents();
				while (i.hasNext()) {
					EObject content = i.next();
					if (content instanceof EPackage) {
						EPackage ePackage = (EPackage)content;
					    packageRegistry.put(ePackage.getNsURI(), ePackage);
					}
				}
			}
		}
		
		return resourceSet;
	}
	
	// Utility methods
	
	protected Collection<File> collectInputFiles(File directory, List<String> inputFileExtensions) {
		final List<File> inputFiles = new ArrayList<File>();
		for (File file : directory.listFiles()) {
			if (file.isFile()) {
				final String ext = getFileExtension(file);
				if (inputFileExtensions.contains(ext)) {
					inputFiles.add(file);
				}
			} else if (file.isDirectory()) {
				inputFiles.addAll(collectInputFiles(file, inputFileExtensions));
			}
		}
		return inputFiles;
	}

	private static String getFileExtension(File file) {
        final String fileName = file.getName();
        if(fileName.lastIndexOf(".") != -1)
        	return fileName.substring(fileName.lastIndexOf(".")+1);
        else 
        	return "";
    }

	/**
	 * Get application version id from properties file.
	 * @return version string from build.properties or UNKNOWN
	 */
	private String getAppVersion() {
    	var version = this.getClass().getPackage().getImplementationVersion();
    	return (version != null) ? version : "<SNAPSHOT>";
	}

	static public class InputFolderPath implements IParameterValidator {
		@Override
		public void validate(String name, String value) throws ParameterException {
			final File directory = new File(value);
			if (!directory.isDirectory()) {
				throw new ParameterException("Parameter " + name + " should be a valid folder path");
			}
	  	}
	}

	static public class InputEcorePath implements IParameterValidator {
		@Override
		public void validate(String name, String value) throws ParameterException {
			final File file = new File(value);
			final String ext = getFileExtension(file);
			if (!file.exists() || !(ext.equals(ECORE) || ext.equals(XCORE))) {
				throw new ParameterException("Parameter " + name + " should be a valid Ecore file path");
			}
	  	}
	}

	static public class OutputCatalogPath implements IParameterValidator {
		@Override
		public void validate(String name, String value) throws ParameterException {
			final File file = new File(value);
			if (!file.getName().endsWith("catalog.xml")) {
				throw new ParameterException("Parameter " + name + " should be a valid OML catalog path");
			}
		}
	}

}
