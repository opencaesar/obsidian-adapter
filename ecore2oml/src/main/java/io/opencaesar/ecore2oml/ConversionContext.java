package io.opencaesar.ecore2oml;

import io.opencaesar.ecore2oml.options.AspectUtil;
import io.opencaesar.ecore2oml.options.Options;

public class ConversionContext {

	public AspectUtil aspectUtil = new AspectUtil();

	public ConversionContext() {
	}

	public void setAspectOptions(Options options) {
		aspectUtil.init(options.aspects);
	}

}