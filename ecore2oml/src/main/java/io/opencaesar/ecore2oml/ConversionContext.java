/**
 * 
 * Copyright 2021 Modelware Solutions and CAE-LIST.
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