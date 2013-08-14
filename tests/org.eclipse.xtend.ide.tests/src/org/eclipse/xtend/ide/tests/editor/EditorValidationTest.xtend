/*******************************************************************************
 * Copyright (c) 2013 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.ide.tests.editor

import com.google.inject.Inject
import org.eclipse.core.resources.IMarker
import org.eclipse.core.resources.IResource
import org.eclipse.xtend.ide.tests.AbstractXtendUITestCase
import org.eclipse.xtend.ide.tests.WorkbenchTestHelper
import org.junit.After
import org.junit.Test
import org.eclipse.xtext.ui.refactoring.ui.SyncUtil
import org.eclipse.xtext.validation.ResourceValidatorImpl
import org.eclipse.xtext.validation.IResourceValidator
import org.eclipse.xtext.validation.CheckMode

/**
 * @author Sven Efftinge - Initial contribution and API
 */
class EditorValidationTest extends AbstractXtendUITestCase {
	@Inject extension WorkbenchTestHelper helper
	@Inject extension SyncUtil
	@Inject IResourceValidator validator

	@After override void tearDown() {
		helper.tearDown
	}
	
	/**
	 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=414947
	 */
	@Test def void testLinkingOfEnum() {
		val content = '''
			import static Foo.*
				
			@SuppressWarnings("all")
			class SomeClass {
				def foo() {
					val Foo x = A
					return x
				}
			}
			
			enum Foo {
				A, B, C
			}
		'''
		val file = createFile("SomeClass.xtend",content)
		waitForBuild(null)
		assertEquals(0, file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE).length)
		
		val editor = openEditor(file)
		editor.document.set('')
		editor.waitForReconciler()
		editor.document.set(content)
		editor.waitForReconciler()
		editor.document.readOnly [
			val issues = validator.validate(it, CheckMode.NORMAL_AND_FAST, [|false])
			assertTrue(issues.toString,issues.empty)
			return null
		]
	}
	
	
}