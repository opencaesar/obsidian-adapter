package io.opencaesar.ecore2oml

import com.beust.jcommander.IParameterValidator
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import io.opencaesar.oml.dsl.OmlStandaloneSetup
import io.opencaesar.oml.util.OmlWriter
import java.io.File
import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.xcore.XcoreStandaloneSetup
import org.eclipse.xtext.resource.XtextResourceSet

class App {

	@Parameter(
		names=#["--input","-i"], 
		description="Location of Ecore input folder (Required)",
		validateWith=FolderPath, 
		required=true, 
		order=1)
	package String inputPath = null

	@Parameter(
		names=#["--output", "-o"], 
		description="Location of the OML output folder", 
		validateWith=FolderPath, 
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
		LOGGER.info("=================================================================")
		LOGGER.info("Input Folder= " + inputPath)
		LOGGER.info("Output Folder= " + outputPath)

		val inputFolder = new File(inputPath)
		val inputFiles = collectOMLFiles(inputFolder)
		
		val injector = new XcoreStandaloneSetup().createInjectorAndDoEMFRegistration();
		val inputResourceSet = injector.getInstance(XtextResourceSet);

		// start the Oml writer
		val writer = new OmlWriter
		writer.start

		for (inputFile : inputFiles) {
			val inputURI = URI.createFileURI(inputFile.absolutePath)
			val inputResource = inputResourceSet.getResource(inputURI, true)
			if (inputResource !== null) {
				LOGGER.info("Reading: "+inputURI)
				var relativePath = inputFolder.toURI().relativize(inputFile.toURI()).getPath()
				relativePath = relativePath.substring(0, relativePath.lastIndexOf('.')+1)+'oml'
				new EcoreToOml(inputResource, relativePath, writer).run
			}
		}

		// load the Oml registries here after the input have been read (since extension "oml" is used by both)
		OmlStandaloneSetup.doSetup()
		val outputResourceSet = new XtextResourceSet

		// finish the Oml writer
		writer.finish(outputResourceSet, outputPath)
		
		// save the output resources
		for (outputResource : outputResourceSet.resources) {
			if (outputResource.URI.fileExtension =='oml') {
				LOGGER.info("Saving: "+outputResource.URI)
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

	static class FolderPath implements IParameterValidator {
		override validate(String name, String value) throws ParameterException {
			val directory = new File(value)
			if (!directory.isDirectory) {
				throw new ParameterException("Parameter " + name + " should be a valid folder path");
			}
	  	}
	}
	
}
