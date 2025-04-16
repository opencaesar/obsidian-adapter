/**
 * 
 * Copyright 2019-2021 California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
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
 */
package io.opencaesar.oml2obsidian;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.ECrossReferenceAdapter;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import io.opencaesar.oml.AnnotationProperty;
import io.opencaesar.oml.Aspect;
import io.opencaesar.oml.Concept;
import io.opencaesar.oml.Entity;
import io.opencaesar.oml.Ontology;
import io.opencaesar.oml.Property;
import io.opencaesar.oml.RelationEntity;
import io.opencaesar.oml.SemanticProperty;
import io.opencaesar.oml.Vocabulary;
import io.opencaesar.oml.dsl.OmlStandaloneSetup;
import io.opencaesar.oml.resource.OmlJsonResourceFactory;
import io.opencaesar.oml.resource.OmlXMIResourceFactory;
import io.opencaesar.oml.util.OmlConstants;
import io.opencaesar.oml.util.OmlRead;
import io.opencaesar.oml.util.OmlResolve;
import io.opencaesar.oml.util.OmlSearch;
import io.opencaesar.oml.validate.OmlValidator;

/**
 * An application to transform Oml resources into Obsidian resources
 */
public class Oml2ObsidianApp {

	private static final List<String> BUILT_IN_ONTOLOGIES = Arrays.asList(new String[] {
			"http://www.w3.org/2001/XMLSchema#",
			"http://www.w3.org/1999/02/22-rdf-syntax-ns#",
			"http://www.w3.org/2000/01/rdf-schema#",
			"http://www.w3.org/2002/07/owl#",
			"http://www.w3.org/2003/11/swrlb#"
		});

	@Parameter(
			names = { "--input-catalog-path", "-i" }, 
			description = "Path of the input Oml catalog file (Required)", 
			validateWith = InputCatalogPath.class, 
			required = true)
	private String inputCatalogPath;

	@Parameter(
			names= { "--input-ontology-iri", "-iri" }, 
			description="IRI of the input Oml ontology (Required)",
			required=true)
	private String inputOntologyIri = null;

	@Parameter(
			names = { "--output-vault-path", "-o" }, 
			description = "Path of the output Obsidian vault (Required)", 
			validateWith = OutputVaultPath.class, 
			required = true)
	private String outputVaultPath;

	@Parameter(
			names = { "--output-classes-path", "-cls" }, 
			description = "Relative path of the output Obsidian classes folder (Required)", 
			required = true)
	private String outputClassesPath;

	@Parameter(
			names = { "--output-templates-path", "-tmp" }, 
			description = "Relative path of the output Obsidian templates folder (Required)", 
			required = true)
	private String outputTemplatesPath;

	@Parameter(
			names = { "--debug", "-d" },
			description = "Shows debug logging statements")
	private boolean debug;

	@Parameter(
			names = { "--help", "-h" },
			description = "Displays summary of options",
			help = true)
	private boolean help;

	private final Logger LOGGER = LogManager.getLogger(Oml2ObsidianApp.class);

    /**
     * Main Method
     * @param args Application arguments.
     * @throws Exception Error
     */
    public static void main(final String... args) throws Exception {
    	mainWithDeltas(null, args);
    }

    /**
     * Main Method with Deltas
     * @param deltas The set of changed files
     * @param args Application arguments.
     * @throws Exception Error
     */
    public static void mainWithDeltas(Collection<File> deltas, final String... args) throws Exception {
		final Oml2ObsidianApp app = new Oml2ObsidianApp();
		final JCommander builder = JCommander.newBuilder().addObject(app).build();
		builder.parse(args);
		if (app.help) {
			builder.usage();
			return;
		}
		if (app.debug) {
			final Appender appender = LogManager.getRootLogger().getAppender("stdout");
			((AppenderSkeleton) appender).setThreshold(Level.DEBUG);
		}
		app.run(deltas);
	}

	/**
	 * Creates a new Oml2ObsidianApp object
	 */
	public Oml2ObsidianApp() {
	}
	
	private void run(Collection<File> deltas) throws Exception {
		LOGGER.info("=================================================================");
		LOGGER.info("                        S T A R T");
		LOGGER.info("                      Oml to Obsidian "+getAppVersion());
		LOGGER.info("=================================================================");
		LOGGER.info("Input catalog path= " + inputCatalogPath);
		LOGGER.info("Input vocabulary bundle Iri= " + inputOntologyIri);
		LOGGER.info("Output metadata path= " + outputVaultPath);

		// Setup OML resource set
		OmlStandaloneSetup.doSetup();
		OmlXMIResourceFactory.register();
		OmlJsonResourceFactory.register();
		final ResourceSet inputResourceSet = new ResourceSetImpl();
		inputResourceSet.eAdapters().add(new ECrossReferenceAdapter());
		
		// load the Oml vocabulary bundle
		Set<String> inputIris = new LinkedHashSet<>(); 
		if (inputOntologyIri != null) {
			final URI inputCatalogUri = URI.createFileURI(inputCatalogPath);
			URI rootUri = resolveRootOntologyIri(inputOntologyIri, inputCatalogUri);
			LOGGER.info(("Reading: " + rootUri));
			Ontology rootOntology = OmlRead.getOntology(inputResourceSet.getResource(rootUri, true));
			inputIris.addAll(OmlRead.getImportedOntologyClosure(rootOntology, true).stream().map(i -> i.getIri()).collect(Collectors.toList()));
		}
		
		// validate resources
		StringBuffer problems = new StringBuffer();
		for (Resource resource : inputResourceSet.getResources()) {
			LOGGER.info(("Validating: " + resource.getURI().path()));
			String results = OmlValidator.validate(resource);
	        if (results.length()>0) {
	        	if (problems.length()>0)
	        		problems.append("\n\n");
	        	problems.append(results);
	        }
		}
		if (problems.length()>0) {
			throw new IllegalStateException("\n"+problems.toString());
		}
		
		// initialize class generator
		var classGenerator = new Oml2Class(inputResourceSet, outputTemplatesPath);
		var classPath = new File(outputVaultPath+"/"+outputClassesPath);
		classPath.mkdirs();
	
		// initialize template generator
		var templateGenerator = new Oml2Template(inputResourceSet);
		var templatePath = new File(outputVaultPath+"/"+outputTemplatesPath);
		templatePath.mkdirs();

		// Initialize scope
		var uniquePrefixes = new HashSet<String>();
		var scope = new HashSet<>(inputResourceSet.getResources());

		// look for some members by iri
		var ignoreProperty = (AnnotationProperty) OmlRead.getMemberByAbbreviatedIri(inputResourceSet, "obsidian:ignore"); 
		var thingAspect = (Aspect) OmlRead.getMemberByIri(inputResourceSet, "http://www.w3.org/2002/07/owl#Thing");

		// Convert resources to Obsidian 
		for (Resource resource : inputResourceSet.getResources()) {
			
			// only process vocabularies that are not built-in
			var ontology = OmlRead.getOntology(resource);
			if (ontology instanceof Vocabulary && !BUILT_IN_ONTOLOGIES.contains(ontology.getNamespace())) {
				var vocabulary = (Vocabulary) ontology;

				// make sure the vocabulary has a unique prefix
				if (uniquePrefixes.contains(vocabulary.getPrefix())) {
					throw new RuntimeException("The ontology prefix '"+ontology.getPrefix()+"' is used more than once in this vocabulary bundle");
				} else {
					uniquePrefixes.add(vocabulary.getPrefix());
				}
								
				// collect entities defined by the vocabulary
				var entities = vocabulary.getOwnedStatements().stream()
						.filter(i -> !i.isRef())
						.filter(i -> i instanceof Concept || i instanceof RelationEntity)
						.filter(i -> !OmlSearch.findIsAnnotatedBy(i, ignoreProperty, scope))
						.map(i -> (Entity)i)
						.collect(Collectors.toList());

				// collect entity's properties
				var entityToProperties = new HashMap<Entity, List<SemanticProperty>>();
				for(var entity : entities) {
					// collect all properties in the domain of the entity
					var properties = OmlSearch.findAllSuperTerms(entity, true, scope).stream()
						.map(j -> (Entity)j)
						.flatMap(j -> OmlSearch.findSemanticPropertiesWithDomain(j, scope).stream())
						.collect(Collectors.toList());
					
					// add all properties with no domains or with owl:Thing domain
					properties.addAll(OmlRead.getOntologies(inputResourceSet).stream()
						.flatMap(i -> OmlRead.getMembers(i).stream())
						.filter(i -> i instanceof SemanticProperty)
						.map(i -> (SemanticProperty)i)
						.filter(i -> {
							var domains = OmlSearch.findDomains(i, scope);
							return domains.isEmpty() || domains.contains(thingAspect);
						})
						.toList());
					
					// validate property names
			        var seen = new HashMap<String, Property>();
					for (var property: properties) {
						var name = property.getName();
						if (!seen.containsKey(name)) {
							seen.put(name, property);
						} else {
							throw new RuntimeException("Property "+seen.get(name).getAbbreviatedIri()
									+" has the same name as "+property.getAbbreviatedIri()
									+" in the context of entity "+entity.getAbbreviatedIri());
						}
					}
					
					// add entity properties
					entityToProperties.put(entity, properties);
				}

				// generate class file for each entity
				for(var entity : entities) {
					var path = classPath.getAbsolutePath()+"/"+entity.getOntology().getPrefix() + "/" + entity.getName()+".md";
					LOGGER.info("Writing: " + path);

					var content = classGenerator.generate(entity, entityToProperties.get(entity), scope);
					
					var file = new File(path);
					file.getParentFile().mkdirs();
					
			        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			            writer.write(content.toString());
			        } catch (IOException e) {
						throw new RuntimeException("Error writing to file: " + e.getMessage());
			        }
				};
				
				// generate template file for each entity
				for(var entity : entities) {
					var path = templatePath.getAbsolutePath()+"/"+entity.getOntology().getPrefix() + "/New " + entity.getName()+".md";
					var file = new File(path);
					LOGGER.info("Writing: " + path + (file.exists()? " (exists)" : ""));

					var content = templateGenerator.generate(entity, entityToProperties.get(entity), scope);
					
					if (file.exists()) {
						var markdown = Files.readString(Path.of(path), StandardCharsets.UTF_8);
						content += extractContentAfterFrontMatter(markdown);
					} else {
						file.getParentFile().mkdirs();
						content += "# Tags\n"
								+ "`= this.tags`\n";
					}
					
			        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			            writer.write(content.toString());
			        } catch (IOException e) {
						throw new RuntimeException("Error writing to file: " + e.getMessage());
			        }
				};
			}
		}

		LOGGER.info("=================================================================");
		LOGGER.info("                          E N D");
		LOGGER.info("=================================================================");
	}

    private static String extractContentAfterFrontMatter(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return markdown;
        }

        Pattern pattern = Pattern.compile("(?s)(?m)^---\\s*$.*?^---\\s*$\\R?(.*)");
        Matcher matcher = pattern.matcher(markdown);

        // If we find a match, group(1) holds all text after the second '---'
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // If no match, we assume no valid front matter was found
        return markdown;
    }

	private URI resolveRootOntologyIri(String rootOntologyIri, URI catalogUri) throws IOException {
		final URI resolved = OmlResolve.resolveOmlFileUri(catalogUri, rootOntologyIri);
		
		if (resolved.isFile()) {
			final String filename = resolved.toFileString();
			if (new File(filename).isFile()) {
				return resolved;
			}
			var fileExtensions = Arrays.asList(OmlConstants.OML_EXTENSIONS);
			for (String ext : fileExtensions) {
				if (new File(filename+'.'+ext).isFile()) {
					return URI.createFileURI(filename+'.'+ext);
				}
			}
		}
		
		return resolved;
	}

	/**
	 * Get application version id from properties file.
	 * 
	 * @return version string from build.properties or UNKNOWN
	 */
	private String getAppVersion() {
    	var version = this.getClass().getPackage().getImplementationVersion();
    	return (version != null) ? version : "<SNAPSHOT>";
	}

	/**
	 * The validator of the input catalog path 
	 */
	public static class InputCatalogPath implements IParameterValidator {
		/**
		 * Creates a new InputCatalogPath object
		 */
		public InputCatalogPath() {
		}
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			final File file = new File(value);
			if (!file.exists() || !file.getName().endsWith("catalog.xml")) {
				throw new ParameterException((("Parameter " + name) + " should be a valid Oml catalog path"));
			}
		}
	}

	/**
	 * The validator of the output vault path 
	 */
	public static class OutputVaultPath implements IParameterValidator {
		/**
		 * Creates a new OutputVaultPath object
		 */
		public OutputVaultPath() {
		}
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			final File file = new File(value);
			file.mkdirs();
			if (!file.exists()) {
				throw new ParameterException((("Parameter " + name) + " should be a valid folder path"));
			}
		}
	}
}