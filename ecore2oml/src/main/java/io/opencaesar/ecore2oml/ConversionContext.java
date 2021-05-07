package io.opencaesar.ecore2oml;

import java.io.FileNotFoundException;
import java.io.FileReader;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import io.opencaesar.ecore2oml.options.AspectUtil;
import io.opencaesar.ecore2oml.options.Options;
import io.opencaesar.ecore2oml.options.RelationshipUtil;
import io.opencaesar.ecore2oml.options.SemanticFlags;
import io.opencaesar.ecore2oml.options.URIMapper;

public class ConversionContext {

	public AspectUtil aspectUtil = new AspectUtil();
	public RelationshipUtil relationUtil = new RelationshipUtil();
	public URIMapper uriMapper = new URIMapper();
	public SemanticFlags semanticFlags = new SemanticFlags();

	public ConversionContext() {
	}

	public void setOptions(String optionsPath) throws FileNotFoundException {
		Gson gson = new Gson();
		JsonReader reader = new JsonReader(new FileReader(optionsPath));
		Options options = gson.fromJson(reader, Options.class);
		aspectUtil.init(options.aspects);
		relationUtil.init(options.relationships);
		uriMapper.init(options.uriMapping);
		semanticFlags.init(options.semanticFlags);
		
	}

}