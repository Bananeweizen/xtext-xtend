/*******************************************************************************
 * Copyright (c) 2012 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.core.typesystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtend.core.jvmmodel.DispatchUtil;
import org.eclipse.xtend.core.jvmmodel.IXtendJvmAssociations;
import org.eclipse.xtend.core.xtend.CreateExtensionInfo;
import org.eclipse.xtend.core.xtend.XtendField;
import org.eclipse.xtend.core.xtend.XtendFunction;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.util.TypeReferences;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XFeatureCall;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.eclipse.xtext.xbase.XbaseFactory;
import org.eclipse.xtext.xbase.scoping.batch.IFeatureNames;
import org.eclipse.xtext.xbase.scoping.batch.IFeatureScopeSession;
import org.eclipse.xtext.xbase.typesystem.InferredTypeIndicator;
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationResult;
import org.eclipse.xtext.xbase.typesystem.conformance.TypeConformanceComputer;
import org.eclipse.xtext.xbase.typesystem.internal.LogicalContainerAwareReentrantTypeResolver;
import org.eclipse.xtext.xbase.typesystem.internal.OperationBodyComputationState;
import org.eclipse.xtext.xbase.typesystem.internal.ResolvedTypes;
import org.eclipse.xtext.xbase.typesystem.references.ITypeReferenceOwner;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;
import org.eclipse.xtext.xbase.typesystem.references.OwnedConverter;
import org.eclipse.xtext.xbase.typesystem.references.ParameterizedTypeReference;
import org.eclipse.xtext.xbase.typesystem.references.WildcardTypeReference;
import org.eclipse.xtext.xbase.typesystem.util.AbstractReentrantTypeReferenceProvider;
import org.eclipse.xtext.xtype.XComputedTypeReference;
import org.eclipse.xtext.xtype.impl.XComputedTypeReferenceImplCustom;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

/**
 * The customized reentrant type resolver is responsible for proper typing
 * of dispatch methods' return type and parameter types as well as adding
 * extension fields to the scope. 
 * 
 * @author Sebastian Zarnekow - Initial contribution and API
 */
@NonNullByDefault
public class DispatchAndExtensionAwareReentrantTypeResolver extends LogicalContainerAwareReentrantTypeResolver {

	public class DispatchReturnTypeReferenceProvider extends AbstractReentrantTypeReferenceProvider {
		private final JvmOperation operation;
		private final ResolvedTypes resolvedTypes;
		private final IFeatureScopeSession session;

		public DispatchReturnTypeReferenceProvider(JvmOperation operation, ResolvedTypes resolvedTypes, IFeatureScopeSession session) {
			this.operation = operation;
			this.resolvedTypes = resolvedTypes;
			this.session = session;
		}

		@Override
		@Nullable
		protected JvmTypeReference doGetTypeReference(XComputedTypeReferenceImplCustom context) {
			LightweightTypeReference expectedType = getReturnTypeOfOverriddenOperation(operation, resolvedTypes, session);
			if (expectedType != null) {
				return expectedType.toJavaCompliantTypeReference();
			}
			
			List<JvmOperation> cases = dispatchUtil.getDispatchCases(operation);
			List<LightweightTypeReference> types = Lists.newArrayListWithCapacity(cases.size());
			for(JvmOperation operation: cases) {
				LightweightTypeReference caseType = resolvedTypes.getActualType(operation);
				types.add(caseType);
			}
			TypeConformanceComputer conformanceComputer = getServices().getTypeConformanceComputer();
			LightweightTypeReference result = conformanceComputer.getCommonSuperType(types);
			if (result == null)
				return null;
			return result.toJavaCompliantTypeReference();
		}
	}
	
	public class DispatchParameterTypeReferenceProvider extends AbstractReentrantTypeReferenceProvider {
		private final JvmOperation operation;
		private final ResolvedTypes resolvedTypes;
		private final int idx;

		public DispatchParameterTypeReferenceProvider(JvmOperation operation, int idx, ResolvedTypes resolvedTypes) {
			this.idx = idx;
			this.operation = operation;
			this.resolvedTypes = resolvedTypes;
		}

		@Override
		@Nullable
		protected JvmTypeReference doGetTypeReference(XComputedTypeReferenceImplCustom context) {
			// TODO type parameters on dispatch operations
			List<JvmOperation> cases = dispatchUtil.getDispatchCases(operation);
			TypeConformanceComputer conformanceComputer = getServices().getTypeConformanceComputer();
			List<LightweightTypeReference> parameterTypes = Lists.newArrayListWithCapacity(cases.size());
			for(JvmOperation caseOperation: cases) {
				JvmFormalParameter parameter = caseOperation.getParameters().get(idx);
				LightweightTypeReference parameterType = resolvedTypes.getActualType(parameter);
				parameterTypes.add(parameterType);
			}
			LightweightTypeReference parameterType = conformanceComputer.getCommonSuperType(parameterTypes);
			if (parameterType == null) {
				throw new IllegalStateException("TODO: handle broken models properly");
			}
			return parameterType.toJavaCompliantTypeReference();
		}
	}
	
	public class InitializerParameterTypeReferenceProvider extends AbstractReentrantTypeReferenceProvider {
		private final ResolvedTypes resolvedTypes;
		private final XtendFunction createFunction;
		private final IFeatureScopeSession featureScopeSession;

		public InitializerParameterTypeReferenceProvider(XtendFunction createFunction, ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession) {
			this.createFunction = createFunction;
			this.resolvedTypes = resolvedTypes;
			this.featureScopeSession = featureScopeSession;
		}

		@Override
		@Nullable
		protected JvmTypeReference doGetTypeReference(XComputedTypeReferenceImplCustom context) {
			CreateExtensionInfo createExtensionInfo = createFunction.getCreateExtensionInfo();
			XExpression expression = createExtensionInfo.getCreateExpression();
			LightweightTypeReference actualType = resolvedTypes.getReturnType(expression);
			if (actualType == null) {
				JvmOperation operation = associations.getDirectlyInferredOperation(createFunction);
				computeTypes(resolvedTypes, featureScopeSession, operation);
				actualType = resolvedTypes.getReturnType(expression);
			}
			if (actualType == null)
				return null;
			return actualType.toJavaCompliantTypeReference();
		}
	}
	
	public class CreateCacheFieldTypeReferenceProvider extends AbstractReentrantTypeReferenceProvider {
		private final JvmOperation createOperation;
		private final ResolvedTypes resolvedTypes;

		public CreateCacheFieldTypeReferenceProvider(JvmOperation createOperation, ResolvedTypes resolvedTypes) {
			this.createOperation = createOperation;
			this.resolvedTypes = resolvedTypes;
		}

		@Override
		@Nullable
		protected JvmTypeReference doGetTypeReference(XComputedTypeReferenceImplCustom context) {
			JvmTypeReference declaredReturnType = createOperation.getReturnType();
			TypeReferences typeReferences = resolvedTypes.getServices().getTypeReferences();
			ITypeReferenceOwner owner = resolvedTypes.getReferenceOwner();
			JvmType arrayList = typeReferences.findDeclaredType(ArrayList.class, createOperation);
			ParameterizedTypeReference arrayListReference = new ParameterizedTypeReference(owner, arrayList);
			JvmType objectType = typeReferences.findDeclaredType(Object.class, createOperation);
			WildcardTypeReference wildcard = new WildcardTypeReference(owner);
			wildcard.addUpperBound(new ParameterizedTypeReference(owner, objectType));
			arrayListReference.addTypeArgument(wildcard);
			JvmType hashMap = typeReferences.findDeclaredType(HashMap.class, createOperation);
			ParameterizedTypeReference hashMapReference = new ParameterizedTypeReference(owner, hashMap);
			hashMapReference.addTypeArgument(arrayListReference);
			hashMapReference.addTypeArgument(new OwnedConverter(owner).toLightweightReference(declaredReturnType));
			return hashMapReference.toJavaCompliantTypeReference();
		}
	}
	
	@Inject
	private DispatchUtil dispatchUtil;
	
	@Inject
	private IXtendJvmAssociations associations;
	
	@Inject
	private XbaseFactory xbaseFactory;
	
	@Override
	protected void _computeTypes(ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession,
			JvmOperation operation) {
		if (dispatchUtil.isDispatcherFunction(operation)) {
			// TODO an inherited declared type should influence the expectation of the cases
			
			// no associated expression, we just resolve it to the common super type of all associated cases
			// see #createTypeProvider and #_doPrepare
			computeAnnotationTypes(resolvedTypes, featureScopeSession, operation);
		} else if (dispatchUtil.isDispatchFunction(operation) && InferredTypeIndicator.isInferred(operation.getReturnType())) {
			JvmOperation dispatcher = dispatchUtil.getDispatcherOperation(operation);
			LightweightTypeReference declaredDispatcherType = null;
			if (dispatcher != null) {
				declaredDispatcherType = getReturnTypeOfOverriddenOperation(dispatcher, resolvedTypes, featureScopeSession);
			}
			List<JvmOperation> dispatchCases = dispatchUtil.getDispatchCases(dispatcher);
			List<LightweightTypeReference> dispatchCaseResults = Lists.newArrayListWithCapacity(dispatchCases.size());
			boolean hasInferredCase = false;
			for(JvmOperation dispatchCase: dispatchCases) {
				OperationBodyComputationState state = new DispatchOperationBodyComputationState(resolvedTypes, featureScopeSession, dispatchCase, dispatcher, this);
				ITypeComputationResult dispatchCaseResult = state.computeTypes();
				if (InferredTypeIndicator.isInferred(dispatchCase.getReturnType())) {
					if (declaredDispatcherType == null) {
						dispatchCaseResults.add(dispatchCaseResult.getReturnType());
					}
					hasInferredCase = true;
				} else {
					dispatchCaseResults.add(resolvedTypes.getActualType(dispatchCase));
				}
				computeAnnotationTypes(resolvedTypes, featureScopeSession, operation);
			}
			if (hasInferredCase) {
				LightweightTypeReference commonDispatchType = declaredDispatcherType != null ? declaredDispatcherType : getServices().getTypeConformanceComputer().getCommonSuperType(dispatchCaseResults);
				if (commonDispatchType != null) {
					for(JvmOperation dispatchCase: dispatchCases) {
						JvmTypeReference returnType = dispatchCase.getReturnType();
						if (InferredTypeIndicator.isInferred(returnType)) {
							InferredTypeIndicator.resolveTo(returnType, commonDispatchType.toJavaCompliantTypeReference());
						}
					}
				}
			}
		} else {
			super._computeTypes(resolvedTypes, featureScopeSession, operation);
		}
	}
	
	@Override
	protected void computeMemberTypes(Map<JvmDeclaredType, ResolvedTypes> preparedResolvedTypes, ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession,
			JvmDeclaredType type) {
		IFeatureScopeSession childSession = addExtensionsToMemberSession(resolvedTypes, featureScopeSession, type);
		super.computeMemberTypes(preparedResolvedTypes, resolvedTypes, childSession, type);
	}
	
	@Override
	protected void prepareMembers(ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession, JvmDeclaredType type, Map<JvmDeclaredType, ResolvedTypes> resolvedTypesByType) {
		IFeatureScopeSession childSession = addExtensionsToMemberSession(resolvedTypes, featureScopeSession, type);
		super.prepareMembers(resolvedTypes, childSession, type, resolvedTypesByType);
	}
	
	/**
	 * Initializes the type inference strategy for the cache field for create extensions.
	 */
	@Override
	protected void _doPrepare(ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession, JvmField field) {
		JvmTypeReference knownType = field.getType();
		if (InferredTypeIndicator.isInferred(knownType)) {
			XComputedTypeReference castedKnownType = (XComputedTypeReference) knownType;
			EObject sourceElement = associations.getPrimarySourceElement(field);
			if (sourceElement instanceof XtendFunction) {
				XtendFunction function = (XtendFunction) sourceElement;
				if (function.getCreateExtensionInfo() != null) {
					JvmOperation operation = associations.getDirectlyInferredOperation(function);
					XComputedTypeReference fieldType = getServices().getXtypeFactory().createXComputedTypeReference();
					fieldType.setTypeProvider(new CreateCacheFieldTypeReferenceProvider(operation, resolvedTypes));
					castedKnownType.setEquivalent(fieldType);
					return;
				}
			}
		}
		super._doPrepare(resolvedTypes, featureScopeSession, field);
	}

	protected IFeatureScopeSession addExtensionsToMemberSession(ResolvedTypes resolvedTypes,
			IFeatureScopeSession featureScopeSession, JvmDeclaredType type) {
		Iterable<JvmField> fields = type.getDeclaredFields();
		IEObjectDescription thisDescription = featureScopeSession.getLocalElement(IFeatureNames.THIS);
		if (thisDescription == null) {
			throw new IllegalStateException("Cannot find feature 'THIS'");
		}
		JvmIdentifiableElement thisFeature = (JvmIdentifiableElement) thisDescription.getEObjectOrProxy();
		Map<XExpression, LightweightTypeReference> extensionProviders = null;
		for(JvmField field: fields) {
			if (isExtensionField(field)) {
				if (extensionProviders == null)
					extensionProviders = Maps.newHashMapWithExpectedSize(3);
				XMemberFeatureCall extensionProvider = createExtensionProvider(thisFeature, field);
				LightweightTypeReference fieldType = resolvedTypes.getActualType(field);
				extensionProviders.put(extensionProvider, fieldType);
			}
		}
		IFeatureScopeSession childSession = featureScopeSession;
		if (extensionProviders != null) {
			childSession = featureScopeSession.addToExtensionScope(extensionProviders);
		}
		XFeatureCall thisAccess = xbaseFactory.createXFeatureCall();
		thisAccess.setFeature(thisFeature);
		LightweightTypeReference thisType = resolvedTypes.getActualType(thisFeature);
		childSession = childSession.addToExtensionScope(Collections.<XExpression, LightweightTypeReference>singletonMap(thisAccess, thisType));
		return childSession;
	}

	protected XMemberFeatureCall createExtensionProvider(JvmIdentifiableElement thisFeature, JvmField field) {
		XMemberFeatureCall extensionProvider = xbaseFactory.createXMemberFeatureCall();
		extensionProvider.setFeature(field);
		XFeatureCall thisAccess = xbaseFactory.createXFeatureCall();
		thisAccess.setFeature(thisFeature);
		extensionProvider.setMemberCallTarget(thisAccess);
		return extensionProvider;
	}
	
	protected boolean isExtensionField(JvmField field) {
		XtendField xtendField = associations.getXtendField(field);
		return xtendField != null && xtendField.isExtension();
	}
	
	@Override
	protected void _doPrepare(ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession,
			JvmOperation operation) {
		if (dispatchUtil.isDispatcherFunction(operation)) {
			List<JvmFormalParameter> parameters = operation.getParameters();
			for(int i = 0; i < parameters.size(); i++) {
				JvmFormalParameter parameter = parameters.get(i);
				JvmTypeReference parameterType = parameter.getParameterType();
				if (InferredTypeIndicator.isInferred(parameterType)) {
					XComputedTypeReference casted = (XComputedTypeReference) parameterType;
					XComputedTypeReference computedParameterType = getServices().getXtypeFactory().createXComputedTypeReference();
					computedParameterType.setTypeProvider(new DispatchParameterTypeReferenceProvider(operation, i, resolvedTypes));
					casted.setEquivalent(computedParameterType);
				} else if (parameterType == null) {
					XComputedTypeReference computedParameterType = getServices().getXtypeFactory().createXComputedTypeReference();
					computedParameterType.setTypeProvider(new DispatchParameterTypeReferenceProvider(operation, i, resolvedTypes));
					parameter.setParameterType(computedParameterType);
				}
			}
		} else if (operation.getParameters().size() >= 1){
			EObject sourceElement = associations.getPrimarySourceElement(operation);
			if (sourceElement instanceof XtendFunction) {
				XtendFunction function = (XtendFunction) sourceElement;
				if (function.getCreateExtensionInfo() != null) {
					JvmFormalParameter firstParameter = operation.getParameters().get(0);
					JvmTypeReference parameterType = firstParameter.getParameterType();
					if (InferredTypeIndicator.isInferred(parameterType)) {
						XComputedTypeReference casted = (XComputedTypeReference) parameterType;
						XComputedTypeReference computedParameterType = getServices().getXtypeFactory().createXComputedTypeReference();
						computedParameterType.setTypeProvider(new InitializerParameterTypeReferenceProvider(function, resolvedTypes, featureScopeSession));
						casted.setEquivalent(computedParameterType);
					}
				}
			}
		}
		super._doPrepare(resolvedTypes, featureScopeSession, operation);
	}
	
	@Override
	protected AbstractReentrantTypeReferenceProvider createTypeProvider(ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession, JvmMember member,
			boolean returnType) {
		if (member instanceof JvmOperation) {
			JvmOperation operation = (JvmOperation) member;
			if (dispatchUtil.isDispatcherFunction(operation)) {
				return new DispatchReturnTypeReferenceProvider(operation, resolvedTypes, featureScopeSession);
			}
		}
		return super.createTypeProvider(resolvedTypes, featureScopeSession, member, returnType);
	}

	protected DispatchUtil getDispatchUtil() {
		return dispatchUtil;
	}
	
}
