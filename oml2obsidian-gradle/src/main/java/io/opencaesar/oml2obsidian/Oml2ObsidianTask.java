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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.URI;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;

import groovy.lang.Closure;
import io.opencaesar.oml.util.OmlResolve;

/**
 * A gradle task to invoke the Oml2Owl tool 
 */
public abstract class Oml2ObsidianTask extends DefaultTask {
	
	/**
	 * Creates a new Oml2ObsidianTask object
	 */
	public Oml2ObsidianTask() {
		getOutputs().upToDateWhen(task -> true); // since user could change template and class files
	}

    @SuppressWarnings("rawtypes")
	@Override
    public Task configure(Closure closure) {
        Task task = super.configure(closure);
		try {
			// calculate input files
			final URI inputCatalogUri = URI.createFileURI(getInputCatalogPath().get().getAbsolutePath());
			Collection<File> inputFiles = new ArrayList<>();
			inputFiles.addAll(OmlResolve.resolveOmlFileUris(inputCatalogUri).stream()
					.map(i -> new File(i.toFileString()))
					.collect(Collectors.toList()));
			inputFiles.add(new File(inputCatalogUri.toFileString()));
			getInputFiles().setFrom(inputFiles);
		} catch (Exception e) {
			throw new GradleException(e.getLocalizedMessage(), e);
		}
        return task;
    }

	/**
	 * Path of the input Oml catalog.
	 * 
	 * @return File Property
	 */
	@Input
    public abstract Property<File> getInputCatalogPath();

	/**
	 * IRI of the input OML ontology
	 * 
	 * @return String Property
	 */
    @Input
    public abstract Property<String> getInputOntologyIri();

	/**
	 * Relative path of the output classes folder
	 * 
	 * @return String Property
	 */
    @Input
    public abstract Property<File> getOutputClassesPath();

	/**
	 * Relative path the output templates folder
	 * 
	 * @return String Property
	 */
    @Input
    public abstract Property<File> getOutputTemplatesPath();

	/**
	 * Relative path of the metadata folder in the vault.
	 * 
	 * @return File Property
	 */
	@Optional
	@Input
    public abstract Property<File> getMetadataRelativePath();

    /**
	 * The debug flag
	 * 
	 * @return Boolean Property
	 */
    @Optional
    @Input
    public abstract Property<Boolean> getDebug();

	/**
	 * The collection of input Oml files referenced by the input Oml catalog
	 * 
	 * @return ConfigurableFileCollection
	 */
	@Incremental
	@InputFiles
	protected abstract ConfigurableFileCollection getInputFiles();

	
    /**
     * The gradle task action logic.
     * 
     * @param inputChanges The input changes
     */
    @TaskAction
    public void run(InputChanges inputChanges) {
        List<String> args = new ArrayList<>();
        if (getInputCatalogPath().isPresent()) {
		    args.add("-i");
		    args.add(getInputCatalogPath().get().getAbsolutePath());
        }
        if (getInputOntologyIri().isPresent()) {
		    args.add("-iri");
		    args.add(getInputOntologyIri().get());
        }
        if (getOutputClassesPath().isPresent()) {
        	args.add("-cls");
        	args.add(getOutputClassesPath().get().getAbsolutePath());
		}
        if (getOutputTemplatesPath().isPresent()) {
        	args.add("-tmp");
        	args.add(getOutputTemplatesPath().get().getAbsolutePath());
		}
        if (getMetadataRelativePath().isPresent()) {
		    args.add("-m");
		    args.add(getMetadataRelativePath().get().getAbsolutePath());
        }
		if (getDebug().isPresent() && getDebug().get()) {
		    args.add("-d");
	    }
	    try {
	    	if (inputChanges.isIncremental()) {
	    		final Set<File> deltas = new HashSet<>();
	        	inputChanges.getFileChanges(getInputFiles()).forEach(f -> deltas.add(f.getFile()));
	        	Oml2ObsidianApp.mainWithDeltas(deltas, args.toArray(new String[0]));
	    	} else {
	    		Oml2ObsidianApp.main(args.toArray(new String[0]));
	    	}
		} catch (Exception e) {
			throw new TaskExecutionException(this, e);
		}
   	}
}
