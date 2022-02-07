package io.opencaesar.ecore2oml;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.xml.resolver.Catalog;
import org.eclipse.emf.common.util.URI;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.work.Incremental;

import io.opencaesar.oml.util.OmlCatalog;

public abstract class Ecore2OmlTask extends DefaultTask {
	
    @Incremental
    @InputDirectory
    protected abstract DirectoryProperty getInputFolderPath();

	private String outputCatalogPath = null;
	
	@Input
	public String getOutputCatalogPath() { return outputCatalogPath; }

    public void setOutputCatalogPath(String s) {
    	try {
    		outputCatalogPath = s;
    		Collection<File> files = new ArrayList<>();
	    	files.add(new File(s));
    		if (new File(s).exists()) {
	    		final OmlCatalog inputCatalog = OmlCatalog.create(URI.createFileURI(s));
	    		files.addAll(collectOmlFiles(inputCatalog));
    		}
	    	getOutputFiles().from(files);
    	} catch (Exception e) {
			throw new GradleException(e.getLocalizedMessage(), e);
    	}
    }

    @OutputFiles
    protected abstract ConfigurableFileCollection getOutputFiles();

    @Optional
    @InputFile
	public abstract RegularFileProperty getOptionsFilePath();

	public boolean debug;

    @TaskAction
    public void run() {
        List<String> args = new ArrayList<String>();
        if (getInputFolderPath().isPresent()) {
		    args.add("-i");
		    args.add(getInputFolderPath().get().getAsFile().getAbsolutePath());
        }
        if (outputCatalogPath != null) {
		    args.add("-o");
		    args.add(outputCatalogPath);
        }
        if (getOptionsFilePath().isPresent()) {
		    args.add("-op");
		    args.add(getOptionsFilePath().get().getAsFile().getAbsolutePath());
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
    
	public static List<File> collectOmlFiles(OmlCatalog catalog) throws Exception {
		final List<File> files = new ArrayList<>();
		catalog.getEntries().stream().filter(e -> e.getEntryType() == Catalog.REWRITE_URI).forEach(e -> {
			String folderPath = e.getEntryArg(1);
			File path = new File(URI.createURI(folderPath).toFileString());
			files.addAll(collectOmlFiles(path));
		});
		for (String subCatalogPath : catalog.getNestedCatalogs()) {
			final OmlCatalog subCatalog = OmlCatalog.create(URI.createFileURI(subCatalogPath));
			files.addAll(collectOmlFiles(subCatalog));
		}
		return files;
	}
	
	private static List<File> collectOmlFiles(File path) {
		final List<File> files;
		if (path.isDirectory()) {
			files = Arrays.asList(path.listFiles());
		} else {
			files = Collections.singletonList(path);
		}
		final List<File> omlFiles = new ArrayList<>();
		for (File file : files) {
			if (file.isDirectory()) {
				omlFiles.addAll(collectOmlFiles(file));
			} else if (file.isFile()) {
				String ext = getFileExtension(file);
				if (Ecore2OmlApp.OML_EXTENSION.equals(ext)) {
					omlFiles.add(file);
				}
			} else { // must be a file name with no extension
				File f = new File(path.toString()+'.'+Ecore2OmlApp.OML_EXTENSION);
				if (f.exists()) {
					omlFiles.add(f);
				}
			}
		}
		return omlFiles;
	}

	private static String getFileExtension(File file) {
        String fileName = file.getName();
        if(fileName.lastIndexOf(".") != -1)
        	return fileName.substring(fileName.lastIndexOf(".")+1);
        else 
        	return "";
    }

}