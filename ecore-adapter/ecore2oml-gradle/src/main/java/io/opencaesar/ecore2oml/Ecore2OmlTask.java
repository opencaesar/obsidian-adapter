package io.opencaesar.ecore2oml;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

public class Ecore2OmlTask extends DefaultTask {
	
	public String inputFolderPath = null;

	public String outputCatalogPath;

	public boolean debug;

    @TaskAction
    public void run() {
        List<String> args = new ArrayList<String>();
        if (inputFolderPath != null) {
		    args.add("-i");
		    args.add(inputFolderPath);
        }
        if (outputCatalogPath != null) {
		    args.add("-o");
		    args.add(outputCatalogPath);
        }
	    if (debug) {
		    args.add("-d");
	    }
	    try {
	    	Ecore2OmlApp.main(args.toArray(new String[0]));
		} catch (Exception e) {
			throw new TaskExecutionException(this, e);
		}
   	}
    
}