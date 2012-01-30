/*******************************************************************************
 * Copyright (c) 2011 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.core.jvmmodel;

import java.util.List;

import org.eclipse.xtend.core.xtend.CreateExtensionInfo;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.xbase.compiler.ImportManager;
import org.eclipse.xtext.xbase.lib.Functions;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author koehnlein - Initial contribution and API
 */
public class XtendCompileStrategies {

	@Inject
	private Provider<DispatchMethodCompileStrategy> dispatchMethodProvider;

	public Functions.Function1<ImportManager, CharSequence> forDispatcher(JvmOperation dispatchOperation,
			List<JvmOperation> sortedDispatchOperations) {
		DispatchMethodCompileStrategy strategy = dispatchMethodProvider.get();
		strategy.initialize(dispatchOperation, sortedDispatchOperations);
		return strategy;
	}

	@Inject
	private Provider<CacheMethodCompileStrategy> cacheMathodProvider;

	public Functions.Function1<ImportManager, CharSequence> forCacheMethod(CreateExtensionInfo createExtensionInfo,
			JvmField cacheField, JvmOperation initializerMethod) {
		CacheMethodCompileStrategy strategy = cacheMathodProvider.get();
		strategy.init(createExtensionInfo, cacheField, initializerMethod);
		return strategy;
	}

	@Inject
	private Provider<CacheVariableCompileStrategy> cacheVarProvider;

	public Functions.Function1<ImportManager, CharSequence> forCacheVariable(JvmGenericType container) {
		CacheVariableCompileStrategy strategy = cacheVarProvider.get();
		strategy.init(container);
		return strategy;
	}
}
