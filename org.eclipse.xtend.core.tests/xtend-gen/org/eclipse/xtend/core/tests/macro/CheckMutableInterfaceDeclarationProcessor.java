/**
 * Copyright (c) 2013 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.xtend.core.tests.macro;

import java.util.Collections;
import java.util.List;
import org.eclipse.xtend.core.tests.macro.MutableAssert;
import org.eclipse.xtend.lib.macro.CodeGenerationContext;
import org.eclipse.xtend.lib.macro.CodeGenerationParticipant;
import org.eclipse.xtend.lib.macro.RegisterGlobalsContext;
import org.eclipse.xtend.lib.macro.RegisterGlobalsParticipant;
import org.eclipse.xtend.lib.macro.TransformationContext;
import org.eclipse.xtend.lib.macro.TransformationParticipant;
import org.eclipse.xtend.lib.macro.declaration.AnnotationReference;
import org.eclipse.xtend.lib.macro.declaration.InterfaceDeclaration;
import org.eclipse.xtend.lib.macro.declaration.MutableInterfaceDeclaration;
import org.eclipse.xtend.lib.macro.declaration.MutableTypeParameterDeclaration;
import org.eclipse.xtend.lib.macro.declaration.TypeReference;
import org.eclipse.xtend.lib.macro.services.AnnotationReferenceBuildContext;
import org.eclipse.xtext.validation.ComposedChecks;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure0;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

@SuppressWarnings("all")
public class CheckMutableInterfaceDeclarationProcessor implements RegisterGlobalsParticipant<InterfaceDeclaration>, TransformationParticipant<MutableInterfaceDeclaration>, CodeGenerationParticipant<MutableInterfaceDeclaration> {
  @Override
  public void doRegisterGlobals(final List<? extends InterfaceDeclaration> annotatedSourceElements, final RegisterGlobalsContext context) {
  }
  
  @Override
  public void doTransform(final List<? extends MutableInterfaceDeclaration> annotatedTargetElements, @Extension final TransformationContext context) {
    for (final MutableInterfaceDeclaration annotatedTargetElement : annotatedTargetElements) {
      {
        final Procedure1<String> _function = (String identifier) -> {
          MutableTypeParameterDeclaration _addTypeParameter = annotatedTargetElement.addTypeParameter(identifier);
          _addTypeParameter.remove();
        };
        MutableAssert.assertValidJavaIdentifier("name", _function);
        final Procedure0 _function_1 = () -> {
          annotatedTargetElement.addTypeParameter("T", ((TypeReference[]) null));
        };
        MutableAssert.<IllegalArgumentException>assertThrowable(IllegalArgumentException.class, "upperBounds cannot be null", _function_1);
        final Procedure0 _function_2 = () -> {
          annotatedTargetElement.addTypeParameter("T", new TypeReference[] { null });
        };
        MutableAssert.<IllegalArgumentException>assertThrowable(IllegalArgumentException.class, "upperBounds cannot contain null", _function_2);
        List<TypeReference> _emptyList = CollectionLiterals.<TypeReference>emptyList();
        MutableTypeParameterDeclaration _addTypeParameter = annotatedTargetElement.addTypeParameter("T", ((TypeReference[])Conversions.unwrapArray(_emptyList, TypeReference.class)));
        _addTypeParameter.remove();
        final Procedure0 _function_3 = () -> {
          annotatedTargetElement.setExtendedInterfaces(null);
        };
        MutableAssert.<IllegalArgumentException>assertThrowable(IllegalArgumentException.class, "superinterfaces cannot be null", _function_3);
        final Procedure0 _function_4 = () -> {
          annotatedTargetElement.setExtendedInterfaces(Collections.<TypeReference>unmodifiableList(CollectionLiterals.<TypeReference>newArrayList((TypeReference)null)));
        };
        MutableAssert.<IllegalArgumentException>assertThrowable(IllegalArgumentException.class, "superinterfaces cannot contain null", _function_4);
        List<TypeReference> _emptyList_1 = CollectionLiterals.<TypeReference>emptyList();
        annotatedTargetElement.setExtendedInterfaces(_emptyList_1);
        final Procedure1<AnnotationReferenceBuildContext> _function_5 = (AnnotationReferenceBuildContext it) -> {
          final Procedure0 _function_6 = () -> {
            it.set(null, null);
          };
          MutableAssert.<IllegalArgumentException>assertThrowable(IllegalArgumentException.class, "name has to be a valid java identifier", _function_6);
          final Procedure0 _function_7 = () -> {
            it.set(null, "foo");
          };
          MutableAssert.<IllegalArgumentException>assertThrowable(IllegalArgumentException.class, "name has to be a valid java identifier", _function_7);
          final Procedure0 _function_8 = () -> {
            String[] _xblockexpression = null;
            {
              final String[] array = { "foo" };
              _xblockexpression = array;
            }
            it.set(null, _xblockexpression);
          };
          MutableAssert.<IllegalArgumentException>assertThrowable(IllegalArgumentException.class, "name has to be a valid java identifier", _function_8);
          final Procedure0 _function_9 = () -> {
            it.set(null, Boolean.valueOf(true));
          };
          MutableAssert.<IllegalArgumentException>assertThrowable(IllegalArgumentException.class, "name has to be a valid java identifier", _function_9);
          final Procedure0 _function_10 = () -> {
            it.set(null, ((boolean[]) ((boolean[])Conversions.unwrapArray(Collections.<Boolean>unmodifiableList(CollectionLiterals.<Boolean>newArrayList(Boolean.valueOf(true))), boolean.class))));
          };
          MutableAssert.<IllegalArgumentException>assertThrowable(IllegalArgumentException.class, "name has to be a valid java identifier", _function_10);
          final Procedure0 _function_11 = () -> {
            it.set(null, Integer.valueOf(0));
          };
          MutableAssert.<IllegalArgumentException>assertThrowable(IllegalArgumentException.class, "name has to be a valid java identifier", _function_11);
          final Procedure0 _function_12 = () -> {
            it.set(null, ((int[]) ((int[])Conversions.unwrapArray(Collections.<Integer>unmodifiableList(CollectionLiterals.<Integer>newArrayList(Integer.valueOf(0))), int.class))));
          };
          MutableAssert.<IllegalArgumentException>assertThrowable(IllegalArgumentException.class, "name has to be a valid java identifier", _function_12);
          final Procedure0 _function_13 = () -> {
            String[] _xblockexpression = null;
            {
              final String[] array = { "foo" };
              _xblockexpression = array;
            }
            it.set("doesNotExist", _xblockexpression);
          };
          MutableAssert.<IllegalArgumentException>assertThrowable(IllegalArgumentException.class, "The annotation property \'doesNotExist\' is not declared on the annotation type \'java.lang.Deprecated\'.", _function_13);
        };
        AnnotationReference _newAnnotationReference = context.newAnnotationReference(Deprecated.class, _function_5);
        final AnnotationReference annotationReference = annotatedTargetElement.addAnnotation(_newAnnotationReference);
        annotatedTargetElement.removeAnnotation(annotationReference);
        final Procedure1<AnnotationReferenceBuildContext> _function_6 = (AnnotationReferenceBuildContext it) -> {
          String[] _xblockexpression = null;
          {
            final String[] array = { "foo" };
            _xblockexpression = array;
          }
          it.set("value", _xblockexpression);
          final Procedure0 _function_7 = () -> {
            int[] _xblockexpression_1 = null;
            {
              final int[] array = { 1 };
              _xblockexpression_1 = array;
            }
            it.set("value", _xblockexpression_1);
          };
          MutableAssert.<IllegalArgumentException>assertThrowable(IllegalArgumentException.class, "int[] is not applicable at this location. Expected java.lang.String[]", _function_7);
        };
        AnnotationReference _newAnnotationReference_1 = context.newAnnotationReference(SuppressWarnings.class, _function_6);
        final AnnotationReference otherAnnotationReference = annotatedTargetElement.addAnnotation(_newAnnotationReference_1);
        annotatedTargetElement.removeAnnotation(otherAnnotationReference);
        final Procedure1<AnnotationReferenceBuildContext> _function_7 = (AnnotationReferenceBuildContext it) -> {
          TypeReference[] _newArrayOfSize = new TypeReference[0];
          it.set("validators", _newArrayOfSize);
          TypeReference _newTypeReference = context.newTypeReference(String.class);
          it.set("validators", _newTypeReference);
          final Procedure0 _function_8 = () -> {
            String[] _newArrayOfSize_1 = new String[0];
            it.set("validators", _newArrayOfSize_1);
          };
          MutableAssert.<IllegalArgumentException>assertThrowable(IllegalArgumentException.class, "java.lang.String[] is not applicable at this location. Expected org.eclipse.xtend.lib.macro.declaration.TypeReference[]", _function_8);
          final Procedure0 _function_9 = () -> {
            Object[] _newArrayOfSize_1 = new Object[0];
            it.set("validators", _newArrayOfSize_1);
          };
          MutableAssert.<IllegalArgumentException>assertThrowable(IllegalArgumentException.class, "Cannot set annotation values of type java.lang.Object[]", _function_9);
        };
        AnnotationReference _newAnnotationReference_2 = context.newAnnotationReference(ComposedChecks.class, _function_7);
        final AnnotationReference composedChecksReference = annotatedTargetElement.addAnnotation(_newAnnotationReference_2);
        annotatedTargetElement.removeAnnotation(composedChecksReference);
      }
    }
  }
  
  @Override
  public void doGenerateCode(final List<? extends MutableInterfaceDeclaration> annotatedSourceElements, @Extension final CodeGenerationContext context) {
  }
}
