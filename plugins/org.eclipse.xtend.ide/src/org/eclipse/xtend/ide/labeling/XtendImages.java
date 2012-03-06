/*******************************************************************************
 * Copyright (c) 2011 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.ide.labeling;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.ui.IImageHelper;
import org.eclipse.xtext.xbase.ui.labeling.XbaseImages;

import com.google.inject.Inject;

/**
 * @author Jan Koehnlein - Initial contribution and API
 * @author Holger Schill
 */
public class XtendImages extends XbaseImages{

	@Inject
	private IImageHelper imageHelper;
	
	public ImageDescriptor forFilter() {
		return JavaPluginImages.DESC_ELCL_FILTER;
	}

	public ImageDescriptor forImplementsAnnotation() {
		return JavaPluginImages.DESC_OBJ_IMPLEMENTS;
	}

	public ImageDescriptor forOverridesAnnotation() {
		return JavaPluginImages.DESC_OBJ_OVERRIDES;
	}

	public Image forPackage() {
		return getJdtImage(JavaPluginImages.DESC_OBJS_PACKDECL);
	}

	public Image forImportContainer() {
		return getJdtImage(JavaPluginImages.DESC_OBJS_IMPCONT);
	}

	public Image forImport() {
		return getJdtImage(JavaPluginImages.DESC_OBJS_IMPDECL);
	}

	public Image forDispatcherFunction(JvmVisibility visibility, boolean isStatic) {
		return forOperation(visibility, isStatic);
	}

	public Image forFile() {
		return imageHelper.getImage("xtend.gif");
	}
}
