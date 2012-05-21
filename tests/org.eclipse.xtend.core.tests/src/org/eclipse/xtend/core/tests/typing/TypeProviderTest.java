/*******************************************************************************
 * Copyright (c) 2011 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.core.tests.typing;

import org.eclipse.xtend.core.tests.AbstractXtendTestCase;
import org.eclipse.xtend.core.xtend.XtendClass;
import org.eclipse.xtend.core.xtend.XtendConstructor;
import org.eclipse.xtend.core.xtend.XtendFile;
import org.eclipse.xtend.core.xtend.XtendFunction;
import org.eclipse.xtend.core.xtend.XtendParameter;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XFeatureCall;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.eclipse.xtext.xbase.typing.ITypeProvider;
import org.junit.Test;

import com.google.inject.Inject;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public class TypeProviderTest extends AbstractXtendTestCase {

	@Inject
	private ITypeProvider typeProvider;
	
	@Override
	protected XtendFile file(String string) throws Exception {
		return file(string, true);
	}
	
	@Test public void testReturnTypeInConstructor() throws Exception {
		XtendConstructor constructor = constructor(
				"new() {\n" + 
				"	''.toString\n" + 
				"}\n");
		XBlockExpression body = (XBlockExpression) constructor.getExpression();
		assertEquals("void", typeProvider.getExpectedType(body).getIdentifier());
		assertEquals("void", typeProvider.getExpectedReturnType(body, true).getIdentifier());
		XMemberFeatureCall toString = (XMemberFeatureCall) body.getExpressions().get(0);
		assertEquals("void", typeProvider.getExpectedType(toString).getIdentifier());
		assertEquals("void", typeProvider.getExpectedReturnType(toString, true).getIdentifier());
	}
	
	@Test public void testTypeOfSuperInConstructor() throws Exception {
		XtendConstructor constructor = constructor(
				"new() {\n" + 
				"	super()\n" + 
				"}\n");
		XBlockExpression body = (XBlockExpression) constructor.getExpression();
		XFeatureCall superCall = (XFeatureCall) body.getExpressions().get(0);
		assertEquals("void", typeProvider.getExpectedType(superCall).getIdentifier());
		assertEquals("void", typeProvider.getExpectedReturnType(superCall, true).getIdentifier());
		assertEquals("void", typeProvider.getType(superCall).getIdentifier());
	}
	
	@Test public void testTypeOfThisInConstructor() throws Exception {
		XtendConstructor constructor = constructor(
				"new(int a) {\n" + 
				"	this()\n" + 
				"}\n" +
				"new() {}");
		XBlockExpression body = (XBlockExpression) constructor.getExpression();
		XFeatureCall thisCall = (XFeatureCall) body.getExpressions().get(0);
		assertEquals("void", typeProvider.getExpectedType(thisCall).getIdentifier());
		assertEquals("void", typeProvider.getExpectedReturnType(thisCall, true).getIdentifier());
		assertEquals("void", typeProvider.getType(thisCall).getIdentifier());
	}

	@Test public void testBug380063NoException() throws Exception {
		XtendClass clazz = clazz("class Foo<T> { " +
				"def foo(java.util.List<? extends T> l) { " +
				"	l.add(null)" +
				"}}");
		XtendFunction function = (XtendFunction) clazz.getMembers().get(0);
		XtendParameter xtendParameter = function.getParameters().get(0);
		XBlockExpression expr = (XBlockExpression) function.getExpression();
		XMemberFeatureCall call = (XMemberFeatureCall) expr.getExpressions().get(0);
		JvmTypeReference type = typeProvider.getType(call.getMemberCallTarget());
		assertEquals("List<? extends T>", type.getSimpleName());
		assertEquals("List<? extends T>", xtendParameter.getParameterType().getSimpleName());



	}

}
