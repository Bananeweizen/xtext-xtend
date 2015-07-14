/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.core.idea.config

import com.google.inject.Inject
import com.intellij.facet.FacetManager
import com.intellij.facet.FacetTypeRegistry
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.VirtualFile
import javax.inject.Provider
import org.eclipse.xtend.core.idea.facet.XtendFacetConfiguration
import org.eclipse.xtend.core.idea.lang.XtendLanguage
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.JpsJavaExtensionService

/**
 * @author kosyakov - Initial contribution and API
 */
class XtendSupportConfigurable extends FrameworkSupportInModuleConfigurable {

	@Inject
	Provider<XtendLibraryDescription> libraryDescriptionProvider

	override addSupport(
		Module module,
		ModifiableRootModel rootModel,
		ModifiableModelsProvider modifiableModelsProvider
	) {
		val entry = rootModel.contentEntries.head
		val mainSrc = entry.sourceFolders.filter[!testSource].head
		var VirtualFile xtendGenMain = null
		if (mainSrc != null) {
			xtendGenMain = mainSrc.file.parent.getOrCreateDir("xtend-gen")
			val properties = JpsJavaExtensionService.getInstance().createSourceRootProperties("", true);
			entry.addSourceFolder(xtendGenMain, JavaSourceRootType.SOURCE, properties)
		}
		val testSrc = entry.sourceFolders.filter[testSource].head
		var VirtualFile xtendGenTest = null
		if (testSrc != null) {
			xtendGenTest = testSrc.file.parent.getOrCreateDir("xtend-gen")
			val properties = JpsJavaExtensionService.getInstance().createSourceRootProperties("", true);
			entry.addSourceFolder(xtendGenTest, JavaSourceRootType.TEST_SOURCE, properties)
		}
		
		val facetType = FacetTypeRegistry.getInstance().findFacetType(XtendLanguage.INSTANCE.ID)
		val mnr = FacetManager.getInstance(module)
		var facet = mnr.findFacet(facetType.id, facetType.defaultFacetName) 
						?: FacetManager.getInstance(module).addFacet(facetType, facetType.defaultFacetName, null)
		val conf = facet.configuration as XtendFacetConfiguration
		val state = conf.state
		if (xtendGenMain != null)
			state.outputDirectory = xtendGenMain.canonicalPath
		if (xtendGenTest != null)
			state.testOutputDirectory = xtendGenTest.canonicalPath
		val libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable()
		var library = libraryTable.getLibraryByName(XtendLibraryDescription.XTEND_LIBRARY_NAME)
		if (library != null && !rootModel.moduleLibraryTable.libraries.contains(library)) {
			rootModel.addLibraryEntry(library)
		}
	}
	
	private def VirtualFile getOrCreateDir(VirtualFile parent, String name) {
		val existing = parent.children.findFirst[it.name==name] 
		if (existing != null) {
			return existing
		}
		return parent.createChildDirectory(null, name)
	}

	override createComponent() {
		null
	}

	override createLibraryDescription() {
		libraryDescriptionProvider.get
	}

}