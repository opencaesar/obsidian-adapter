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
package io.opencaesar.ecore2oml.options;

import java.util.List;

import org.eclipse.emf.ecore.EStructuralFeature;

import io.opencaesar.ecore2oml.ConversionContext;
import io.opencaesar.ecore2oml.util.Util;

public class Relationship {
	
	public String root;
	public  String source;
	public String target;
	public String forwardPostFix;
	public String reversePostFix;
	public String forwardName;
	public String reverseName;
	public List<OverrideInfo> overrides;
	
	public boolean isSource(EStructuralFeature toCheck, ConversionContext context) {
		return Util.getIri(toCheck, context).equals(source);
	}
	
	public boolean isTarget(EStructuralFeature toCheck, ConversionContext context) {
		return Util.getIri(toCheck, context).equals(target);
	}
	
	public Relationship(String iri, String source, String target) {
		this.root = iri;
		this.source = source;
		this.target = target;
	}
	

}
