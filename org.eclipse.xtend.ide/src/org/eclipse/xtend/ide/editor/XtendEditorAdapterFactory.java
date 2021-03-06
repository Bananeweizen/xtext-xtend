/*******************************************************************************
 * Copyright (c) 2017 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.ide.editor;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.xtend.ide.view.DerivedSourceView;

/**
 * 
 * @author Karsten Thoms (karsten.thoms@itemis.de) - Initial contribution and API
 */
public class XtendEditorAdapterFactory implements IAdapterFactory {

	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof XtendEditor && IShowInTargetList.class.equals(adapterType)) {
			return new IShowInTargetList() {
				@Override
				public String[] getShowInTargetIds() {
					return new String[] { DerivedSourceView.class.getName() };
				}
			};
		}
		return null;
	}

	@Override
	public Class[] getAdapterList() {
		return new Class[] { IShowInTargetList.class };
	}

}
