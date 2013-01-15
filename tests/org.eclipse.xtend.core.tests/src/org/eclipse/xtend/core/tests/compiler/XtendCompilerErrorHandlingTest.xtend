package org.eclipse.xtend.core.tests.compiler

import com.google.inject.Inject
import org.eclipse.xtend.core.tests.AbstractXtendTestCase
import org.eclipse.xtext.common.types.JvmDeclaredType
import org.eclipse.xtext.util.CancelIndicator
import org.eclipse.xtext.util.OnChangeEvictingCache
import org.eclipse.xtext.validation.CheckMode
import org.eclipse.xtext.validation.IResourceValidator
import org.eclipse.xtext.xbase.compiler.ElementIssueProvider
import org.eclipse.xtext.xbase.compiler.IElementIssueProvider
import org.eclipse.xtext.xbase.compiler.JvmModelGenerator
import org.junit.Test
import org.eclipse.xtext.xbase.compiler.GeneratorConfig

class XtendCompilerErrorHandlingTest extends AbstractXtendTestCase {
	
	@Inject JvmModelGenerator generator
	
	@Inject IResourceValidator validator
	
	@Inject ElementIssueProvider$Factory issueProviderFactory
	
	@Inject GeneratorConfig generatorConfig

	@Test
	def testUnresolvedSuperclass() {
		'''
			class Foo extends Unresolved {
			}
		'''.assertCompilesTo( '''
			@SuppressWarnings("all")
			public class Foo /* implements Unresolved  */{
			}
		''')
	}
		
	@Test
	def testUnresolvedInterface() {
		'''
			class Foo implements Unresolved {
			}
		'''.assertCompilesTo( '''
			@SuppressWarnings("all")
			public class Foo /* implements Unresolved  */{
			}
		''')
	}
		
	@Test
	def testUnresolvedInterface_1() {
		'''
			class Foo implements Cloneable, Unresolved {
			}
		'''.assertCompilesTo( '''
			@SuppressWarnings("all")
			public class Foo implements Cloneable/* , Unresolved */ {
			}
		''')
	}
		
	@Test
	def testUnresolvedInterface_2() {
		'''
			class Foo implements Unresolved, Cloneable {
			}
		'''.assertCompilesTo( '''
			@SuppressWarnings("all")
			public class Foo implements /* Unresolved */Cloneable {
			}
		''')
	}
		
	@Test
	def testUnresolvedAnnotation() {
		'''
			@Unresolved
			class Foo {
			}
		'''.assertCompilesTo( '''
			/* @Unresolved */@SuppressWarnings("all")
			public class Foo {
			}
		''')
	}
		
	@Test
	def testUnresolvedAnnotation_1() {
		'''
			@Deprecated
			@Unresolved
			class Foo {
			}
		'''.assertCompilesTo( '''
			@Deprecated/* 
			@Unresolved */
			@SuppressWarnings("all")
			public class Foo {
			}
		''')
	}
		
	@Test
	def testUnresolvedFieldType() {
		'''
			class Foo {
				Unresolved bar
			}
		'''.assertCompilesTo( '''
			@SuppressWarnings("all")
			public class Foo {
			  private /* Unresolved */Object bar;
			}
		''')
	}
		
	@Test
	def testUnresolvedReturnType() {
		'''
			class Foo {
				def Unresolved bar() {
					null
				}
			}
		'''.assertCompilesTo( '''
			@SuppressWarnings("all")
			public class Foo {
			  public /* Unresolved */Object bar() {
			    throw new Error("Unresolved compilation problems:"
			      + "\nIncompatible implicit return type. Expected void but was null");
			  }
			}
		''')
	}
		
	@Test
	def testUnresolvedParameterType() {
		'''
			class Foo {
				def void bar(Unresolved p) {
				}
			}
		'''.assertCompilesTo( '''
			@SuppressWarnings("all")
			public class Foo {
			  public void bar(final /* Unresolved */Object p) {
			  }
			}
		''')
	}
		
	@Test
	def testUnresolvedException() {
		'''
			class Foo {
				def void bar() throws Unresolved {
				}
			}
		'''.assertCompilesTo( '''
			@SuppressWarnings("all")
			public class Foo {
			  public void bar()/*  throws Unresolved */ {
			  }
			}
		''')
	}
		
	@Test
	def testUnresolvedException_1() {
		'''
			class Foo {
				def void bar() throws Unresolved, RuntimeException {
				}
			}
		'''.assertCompilesTo( '''
			@SuppressWarnings("all")
			public class Foo {
			  public void bar() throws /* Unresolved */RuntimeException {
			  }
			}
		''')
	}
		
	@Test
	def testUnresolvedException_2() {
		'''
			class Foo {
				def void bar() throws RuntimeException, Unresolved {
				}
			}
		'''.assertCompilesTo( '''
			@SuppressWarnings("all")
			public class Foo {
			  public void bar() throws RuntimeException/* , Unresolved */ {
			  }
			}
		''')
	}	

	@Test
	def testUnresolvedTypeConstraint() {
		'''
			class Foo <T extends Unresolved> {}
		'''.assertCompilesTo( '''
			@SuppressWarnings("all")
			public class Foo<T/*  extends Unresolved */> {
			}
		''')
	}
	
	@Test
	def testFieldInitializerTypeError() {
		'''
			class Foo {
				val int bar = null
			}
		'''.assertCompilesTo( '''
			@SuppressWarnings("all")
			public class Foo {
			  private final int bar /* Skipped initializer because of errors */;
			}
		''')
	}

	@Test
	def testFieldInitializerLinkError() {
		'''
			class Foo {
				val bar = foo()
			}
		'''.assertCompilesTo( '''
			@SuppressWarnings("all")
			public class Foo {
			  private final /* type is 'null' */ bar /* Skipped initializer because of errors */;
			}
		''')
	}

	@Test
	def testMethodBodyTypeError() {
		'''
			class Foo {
				def int bar() {
					null
				}
			}
		'''.assertCompilesTo( '''
			@SuppressWarnings("all")
			public class Foo {
			  public int bar() {
			    throw new Error("Unresolved compilation problems:"
			      + "\nIncompatible implicit return type. Expected int or java.lang.Integer but was null");
			  }
			}
		''')
	}
	
	@Test
	def testMethodBodyLinkError() {
		'''
			class Foo {
				def int bar() {
					foo()
				}
			}
		'''.assertCompilesTo( '''
			@SuppressWarnings("all")
			public class Foo {
			  public int bar() {
			    throw new Error("Unresolved compilation problems:"
			      + "\nThe method or field foo is undefined for the type Foo"
			      + "\nIncompatible implicit return type. Expected int or java.lang.Integer but was void");
			  }
			}
		''')
	}
		
	
	def assertCompilesTo(CharSequence input, CharSequence expected) {
		val file = file(input.toString(), false)
		val resource = file.eResource
		val issues = validator.validate(resource, CheckMode::ALL, CancelIndicator::NullImpl)
		try {
			issueProviderFactory.attachData(resource, issues)
			val inferredType = resource.contents.filter(typeof(JvmDeclaredType)).head
			val javaCode = generator.generateType(inferredType, generatorConfig);
			assertEquals(expected.toString, javaCode.toString)
		} finally {
			issueProviderFactory.detachData(resource);
		}
	}
}