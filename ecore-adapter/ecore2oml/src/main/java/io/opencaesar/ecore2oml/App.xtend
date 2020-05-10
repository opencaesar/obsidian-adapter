package io.opencaesar.ecore2oml

import com.beust.jcommander.IParameterValidator
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.google.common.io.CharStreams
import io.opencaesar.oml.dsl.OmlStandaloneSetup
import io.opencaesar.oml.util.OmlWriter
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.emf.ecore.xcore.XcoreStandaloneSetup
import org.eclipse.xtext.resource.XtextResourceSet

class App {

	@Parameter(
		names=#["--input","-i"], 
		description="Location of Ecore input folder (Required)",
		validateWith=InputFolderPath, 
		required=true, 
		order=1)
	package String inputPath = null

	@Parameter(
		names=#["--owl","-w"], 
		description="Location of the owl.oml file (Required)",
		validateWith=OwlPath, 
		required=true, 
		order=2)
	package String owlPath = null

	@Parameter(
		names=#["--output", "-o"], 
		description="Location of the OML output folder", 
		validateWith=OutputFolderPath, 
		order=2
	)
	package String outputPath = "."

	@Parameter(
		names=#["-d", "--debug"], 
		description="Shows debug logging statements", 
		order=3
	)
	package boolean debug

	@Parameter(
		names=#["--help","-h"], 
		description="Displays summary of options", 
		help=true, 
		order=4) package boolean help

	val LOGGER = LogManager.getLogger(App)

	/*
	 * Main method
	 */
	def static void main(String ... args) {
		val app = new App
		val builder = JCommander.newBuilder().addObject(app).build()
		builder.parse(args)
		if (app.help) {
			builder.usage()
			return
		}
		if (app.debug) {
			val appender = LogManager.getRootLogger.getAppender("stdout")
			(appender as AppenderSkeleton).setThreshold(Level.DEBUG)
		}
		if (app.inputPath.endsWith('/')) {
			app.inputPath = app.inputPath.substring(0, app.inputPath.length-1)
		}
		if (app.outputPath.endsWith('/')) {
			app.outputPath = app.outputPath.substring(0, app.outputPath.length-1)
		}
		app.run()
	}

	/*
	 * Run method
	 */
	def void run() {
		LOGGER.info("=================================================================")
		LOGGER.info("                        S T A R T")
		LOGGER.info("                      Ecore to Oml "+getAppVersion)
		LOGGER.info("=================================================================")
		LOGGER.info("Input Folder= " + inputPath)
		LOGGER.info("Owl File= " + owlPath)
		LOGGER.info("Output Folder= " + outputPath)

		val inputFolder = new File(inputPath)
		val inputFiles = collectOMLFiles(inputFolder)
		
		val injector = new XcoreStandaloneSetup().createInjectorAndDoEMFRegistration();
		val inputResourceSet = injector.getInstance(XtextResourceSet);

		// load the input models and resolve their references
		for (inputFile : inputFiles) {
			val inputURI = URI.createFileURI(inputFile.absolutePath)
			val inputResource = inputResourceSet.getResource(inputURI, true)
			if (inputResource !== null) {
				EcoreUtil.resolveAll(inputResource)
				LOGGER.info("Reading: "+inputURI)
			}
		}

		// load the Oml registries here after the input have been read
		OmlStandaloneSetup.doSetup()
		val outputResourceSet = new XtextResourceSet

		// create the Oml writer
		val writer = new OmlWriter(outputResourceSet)
		
		// load the resource dependencies
		val owlResourceURI = URI.createFileURI(owlPath)
		writer.loadDependentResource(owlResourceURI)

		// start the Oml Writer
		writer.start

		// create the new resources
		val outputResourceURIs = new ArrayList<URI>
		for (inputFile : inputFiles) {
			val inputURI = URI.createFileURI(inputFile.absolutePath)
			val inputResource = inputResourceSet.getResource(inputURI, true)
			if (inputResource !== null) {
				var relativePath = inputFolder.toURI().relativize(inputFile.toURI()).getPath()
				relativePath = relativePath.substring(0, relativePath.lastIndexOf('.')+1)+'oml'
				val outputResourceURI = URI.createFileURI(outputPath+'/'+relativePath)
				new EcoreToOml(inputResource, outputResourceURI, owlResourceURI, writer).run
				outputResourceURIs.add (outputResourceURI)
			}
		}

		// finish the Oml writer
		writer.finish
		
		// save the output resources here instead of calling writer.save in order to log
		for (outputResourceURI : outputResourceURIs) {
			if (outputResourceURI.fileExtension =='oml') {
				LOGGER.info("Saving: "+outputResourceURI)
				val outputResource = outputResourceSet.getResource(outputResourceURI, false)
				outputResource.save(Collections.EMPTY_MAP)
			}
		}

		LOGGER.info("=================================================================")
		LOGGER.info("                          E N D")
		LOGGER.info("=================================================================")
	}

	// Utility methods
	
	def Collection<File> collectOMLFiles(File directory) {
		val omlFiles = new ArrayList<File>
		for (file : directory.listFiles()) {
			if (file.isFile) {
				val ext = getFileExtension(file)
				if (ext == "ecore" || ext == "xcore") {
					omlFiles.add(file)
				}
			} else if (file.isDirectory) {
				omlFiles.addAll(collectOMLFiles(file))
			}
		}
		return omlFiles
	}

	private def String getFileExtension(File file) {
        val fileName = file.getName()
        if(fileName.lastIndexOf(".") != -1)
        	return fileName.substring(fileName.lastIndexOf(".")+1)
        else 
        	return ""
    }

	/**
	 * Get application version id from properties file.
	 * @return version string from build.properties or UNKNOWN
	 */
	def String getAppVersion() {
		var version = "UNKNOWN"
		try {
			val input = Thread.currentThread().getContextClassLoader().getResourceAsStream("version.txt")
			val reader = new InputStreamReader(input)
			version = CharStreams.toString(reader);
		} catch (IOException e) {
			val errorMsg = "Could not read version.txt file." + e
			LOGGER.error(errorMsg, e)
		}
		version
	}

	static class InputFolderPath implements IParameterValidator {
		override validate(String name, String value) throws ParameterException {
			val directory = new File(value)
			if (!directory.isDirectory) {
				throw new ParameterException("Parameter " + name + " should be a valid folder path");
			}
	  	}
	}

	static class OutputFolderPath implements IParameterValidator {
		override validate(String name, String value) throws ParameterException {
			val directory = new File(value)
			if (!directory.exists) {
				directory.mkdir
			}
	  	}
	}
	
	static class OwlPath implements IParameterValidator {
		override validate(String name, String value) throws ParameterException {
			val file = new File(value)
			if (!file.name.endsWith('owl.oml')) {
				throw new ParameterException("Parameter " + name + " should be a valid owl.oml path");
			}
	  	}
	}
}
