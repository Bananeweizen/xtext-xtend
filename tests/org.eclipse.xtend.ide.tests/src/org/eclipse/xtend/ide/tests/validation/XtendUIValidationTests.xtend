package org.eclipse.xtend.ide.tests.validation

import com.google.inject.Inject
import org.eclipse.xtend.core.xtend.XtendClass
import org.eclipse.xtend.core.xtend.XtendField
import org.eclipse.xtend.core.xtend.XtendFunction
import org.eclipse.xtend.ide.tests.AbstractXtendUITestCase
import org.eclipse.xtend.ide.tests.WorkbenchTestHelper
import org.eclipse.xtext.common.types.JvmParameterizedTypeReference
import org.eclipse.xtext.junit4.validation.ValidationTestHelper
import org.junit.Ignore
import org.junit.Test

import static org.eclipse.xtend.core.xtend.XtendPackage$Literals.*
import static org.eclipse.xtext.common.types.TypesPackage$Literals.*
import static org.eclipse.xtext.xbase.XbasePackage$Literals.*
import static org.eclipse.xtext.xtype.XtypePackage$Literals.*
import static org.eclipse.xtext.xbase.validation.IssueCodes.*
import static org.eclipse.xtend.core.validation.IssueCodes.*

class XtendUIValidationTests extends AbstractXtendUITestCase {
	@Inject
	private WorkbenchTestHelper testHelper;
	@Inject
	private ValidationTestHelper helper;
	
	override tearDown() throws Exception {
		testHelper.tearDown
	}
	
	@Test
	def void testWrongPackage() {
		val xtendFile = testHelper.xtendFile("Clazz.xtend",'''
			package my.foo.pack
			class Foo {
			}
		''')
		helper.assertError(xtendFile, XTEND_FILE, WRONG_PACKAGE)
	}
	
	@Test
	def void testForbiddenImport() {
		val xtendFile = testHelper.xtendFile("Clazz.xtend",'''
		import org.eclipse.xtend.core.tests.restricted.RestrictedClass
		class Foo {
		}

		''')
		helper.assertError(xtendFile.importSection.importDeclarations.get(0), XIMPORT_DECLARATION, FORBIDDEN_REFERENCE)
	}

	@Test
	def void testDiscouragedImport() {
		val xtendFile = testHelper.xtendFile("Clazz.xtend",'''
		import org.eclipse.xtend.core.tests.internal.InternalClass
		class Foo {
		}

		''')
		helper.assertWarning(xtendFile.importSection.importDeclarations.get(0), XIMPORT_DECLARATION, DISCOURAGED_REFERENCE)
	}
	
	@Test
	def void testForbiddenImportInnerClass() {
		val xtendFile = testHelper.xtendFile("Clazz.xtend",'''
		import org.eclipse.xtend.core.tests.restricted.RestrictedClass$InnerRestrictedClass
		class Foo {
		}

		''')
		helper.assertError(xtendFile.importSection.importDeclarations.get(0), XIMPORT_DECLARATION, FORBIDDEN_REFERENCE)
	}

	@Test
	def void testDiscouragedImportInnerClass() {
		val xtendFile = testHelper.xtendFile("Clazz.xtend",'''
		import org.eclipse.xtend.core.tests.internal.InternalClass$InnerInternalClass
		class Foo {
		}

		''')
		helper.assertWarning(xtendFile.importSection.importDeclarations.get(0), XIMPORT_DECLARATION, DISCOURAGED_REFERENCE)
	}

	@Test
	def void testValidImport() {
		val xtendFile = testHelper.xtendFile("Clazz.xtend",'''
		import java.util.List
		class Foo {
			def bar(List l){}
		}

		''')
		helper.assertNoIssues(xtendFile.importSection.importDeclarations.get(0))
	}

	@Test
	def void testForbiddenConstructorCall() {
		val xtendFile = testHelper.xtendFile("Clazz.xtend",'''
		import org.eclipse.xtend.core.tests.restricted.RestrictedClass
		class Foo {
			RestrictedClass x = new RestrictedClass
		}

		''')
		helper.assertError(xtendFile.importSection.importDeclarations.get(0), XIMPORT_DECLARATION, FORBIDDEN_REFERENCE)
		val field = xtendFile.xtendTypes.filter(typeof(XtendClass)).head.members.head as XtendField
		helper.assertError(field.type, JVM_TYPE_REFERENCE, FORBIDDEN_REFERENCE)
		helper.assertError(field.initialValue, XCONSTRUCTOR_CALL, FORBIDDEN_REFERENCE)
	}
	
	@Test
	def void testDiscouragedConstructorCall() {
		val xtendFile = testHelper.xtendFile("Clazz.xtend",'''
		import org.eclipse.xtend.core.tests.internal.InternalClass
		class Foo {
			InternalClass x = new InternalClass
		}

		''')
		helper.assertWarning(xtendFile.importSection.importDeclarations.get(0), XIMPORT_DECLARATION, DISCOURAGED_REFERENCE)
		val field = xtendFile.xtendTypes.filter(typeof(XtendClass)).head.members.head as XtendField
		helper.assertWarning(field.type, JVM_TYPE_REFERENCE, DISCOURAGED_REFERENCE)
		helper.assertWarning(field.initialValue, XCONSTRUCTOR_CALL, DISCOURAGED_REFERENCE)
	}
	

	@Test
	def void testDiscouragedConstructorCallInnernClass() {
		val xtendFile = testHelper.xtendFile("Clazz.xtend",'''
		import org.eclipse.xtend.core.tests.internal.InternalClass$InnerInternalClass
		class Foo {
			InnerInternalClass x = new InnerInternalClass
		}

		''')
		helper.assertWarning(xtendFile.importSection.importDeclarations.get(0), XIMPORT_DECLARATION, DISCOURAGED_REFERENCE)
		val field = xtendFile.xtendTypes.filter(typeof(XtendClass)).head.members.head as XtendField
		helper.assertWarning(field.type, JVM_TYPE_REFERENCE, DISCOURAGED_REFERENCE)
		helper.assertWarning(field.initialValue, XCONSTRUCTOR_CALL, DISCOURAGED_REFERENCE)
	}
	
	@Test
	def void testForbiddenConstructorCallInnerClass() {
		val xtendFile = testHelper.xtendFile("Clazz.xtend",'''
		import org.eclipse.xtend.core.tests.restricted.RestrictedClass$InnerRestrictedClass
		class Foo {
			InnerRestrictedClass x = new InnerRestrictedClass
		}

		''')
		helper.assertError(xtendFile.importSection.importDeclarations.get(0), XIMPORT_DECLARATION, FORBIDDEN_REFERENCE)
		val field = xtendFile.xtendTypes.filter(typeof(XtendClass)).head.members.head as XtendField
		helper.assertError(field.type, JVM_TYPE_REFERENCE, FORBIDDEN_REFERENCE)
		helper.assertError(field.initialValue, XCONSTRUCTOR_CALL, FORBIDDEN_REFERENCE)
	}
	
	@Test
	def void testForbiddenTypeUsageInnernClass() {
		val xtendFile = testHelper.xtendFile("Clazz.xtend",'''
		class Foo {
			def bar(org.eclipse.xtend.core.tests.restricted.RestrictedClass$InnerRestrictedClass x) {}
		}

		''')
		val function = xtendFile.xtendTypes.filter(typeof(XtendClass)).head.members.head as XtendFunction
		helper.assertError(function.parameters.get(0), JVM_TYPE_REFERENCE, FORBIDDEN_REFERENCE)
	}

	@Test
	def void testDiscouragedTypeUsageInnernClass() {
		val xtendFile = testHelper.xtendFile("Clazz.xtend",'''
		class Foo {
			def bar(org.eclipse.xtend.core.tests.internal.InternalClass$InnerInternalClass x){}
		}

		''')

		val function = xtendFile.xtendTypes.filter(typeof(XtendClass)).head.members.head as XtendFunction
		helper.assertWarning(function.parameters.get(0), JVM_TYPE_REFERENCE, DISCOURAGED_REFERENCE)
	}
	
	@Test
	def void testForbiddenTypeUsage() {
		val xtendFile = testHelper.xtendFile("Clazz.xtend",'''
		class Foo {
			def bar(org.eclipse.xtend.core.tests.restricted.RestrictedClass x) {}
		}

		''')
		val function = xtendFile.xtendTypes.filter(typeof(XtendClass)).head.members.head as XtendFunction
		helper.assertError(function.parameters.get(0), JVM_TYPE_REFERENCE, FORBIDDEN_REFERENCE)
	}
	
		@Test
	def void testParameterizedTypeReference() {
		val xtendFile = testHelper.xtendFile("Clazz.xtend",'''
		class Foo {
			def bar(org.eclipse.xtend.core.tests.restricted.RestrictedClass<org.eclipse.xtend.core.tests.internal.InternalClass> x) {}
		}
		''')
		val function = xtendFile.xtendTypes.filter(typeof(XtendClass)).head.members.head as XtendFunction
		val parameter = function.parameters.get(0)
		val type = parameter.parameterType as JvmParameterizedTypeReference
		val typeParameter = type.arguments.get(0)
		helper.assertError(type, JVM_PARAMETERIZED_TYPE_REFERENCE, FORBIDDEN_REFERENCE)
		helper.assertWarning(typeParameter, JVM_TYPE_REFERENCE, DISCOURAGED_REFERENCE)
	}
	
	@Test@Ignore("Enable on demand")
	def void testPerformance_1() {
		testPerformance
	}
	
	@Test@Ignore("Enable on demand")
	def void testPerformance_2() {
		testPerformance
	}

	def void testPerformance() {
		val xtendFile = testHelper.xtendFile("Clazz.xtend",'''
		import org.eclipse.xtend.core.tests.restricted.RestrictedClass
		import org.eclipse.xtend.core.tests.internal.InternalClass
		
		class Foo {
		�FOR i : (0..200)�
			RestrictedClass x�i� = new RestrictedClass
			InternalClass y�i� = new InternalClass
			def bar(InternalClass p1�i�, RestrictedClass p2�i�){}
		�ENDFOR�
		}
		''')
		helper.validate(xtendFile)
	}
}