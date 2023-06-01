package io.opencaesar.ecore2oml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.work.Incremental;

/**
 * A gradle task to invoke the ecore to oml tool 
 */
public abstract class Ecore2OmlTask extends DefaultTask {

	/**
	 * Creates a new Ecore2OmlTask object
	 */
	public Ecore2OmlTask() {
	}

	/**
	 * Path of input folder of Ecore files
	 * 
	 * @return Directory Property
	 */
	@InputDirectory
    public abstract DirectoryProperty getInputFolderPath();

	/**
	 * Path of output OML folder
	 * 
	 * @return Directory Property
	 */
    @OutputDirectory
    public abstract DirectoryProperty getOutputFolderPath();

	/**
	 * Path of referenced Ecore files
	 * 
	 * @return String List Property
	 */
    @Optional
    @InputFiles
    @Incremental
    public abstract ListProperty<File> getReferencedEcorePaths();

    /**
	 * The file extensions for input Ecore files
	 * 
	 * @return String List Property
	 */
    @Optional
    @Input
    public abstract ListProperty<String> getInputFileExtensions();

    /**
	 * The file extension for output OML files
	 * 
	 * @return String Property
	 */
    @Optional
    @Input
    public abstract Property<String> getOutputFileExtension();

    /**
	 * The debug flag
	 * 
	 * @return Boolean Property
	 */
    @Optional
    @Input
    public abstract Property<Boolean> getDebug();
    
   /**
    * The gradle task action logic.
    */
	@TaskAction
    public void run() {
        List<String> args = new ArrayList<String>();
        if (getInputFolderPath().isPresent()) {
		    args.add("-i");
		    args.add(getInputFolderPath().get().getAsFile().getAbsolutePath());
        }
        if (getOutputFolderPath().isPresent()) {
		    args.add("-o");
		    args.add(getOutputFolderPath().get().getAsFile().getAbsolutePath());
        }
        if (getReferencedEcorePaths().isPresent()) {
        	for (var file : getReferencedEcorePaths().get()) {
    		    args.add("-r");
    		    args.add(file.getAbsolutePath());
        	}
        }
        if (getInputFileExtensions().isPresent()) {
        	for (var ext : getInputFileExtensions().get()) {
    		    args.add("-ie");
    		    args.add(ext);
        	}
        }
        if (getOutputFileExtension().isPresent()) {
		    args.add("-oe");
		    args.add(getOutputFileExtension().get());
        }
		if (getDebug().isPresent() && getDebug().get()) {
		    args.add("-d");
	    }
	    try {
	    	Ecore2OmlApp.main(args.toArray(new String[0]));
		} catch (Exception e) {
			throw new TaskExecutionException(this, e);
		}
   	}
    
}