/*******************************************************************************
 * Copyright (c) 2012 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.ide.hover;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.Pair;
import org.eclipse.xtext.util.Tuples;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.eclipse.xtext.xbase.impl.FeatureCallToJavaMapping;
import org.eclipse.xtext.xbase.util.XbaseSwitch;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * 
 * @author Holger Schill - Initial contribution and API
 * @since 2.3
 */
public class XtendHoverSerializer {

	private static final String STATICDELIMITER = "::";
	private static final String IT = "it";
	private static final String SEPERATOR = ", ";
	private static final String DELIMITER = ".";
	@Inject
	private FeatureCallToJavaMapping featureCallToJavaMapping;

	public String computeUnsugaredExpression(EObject object) {
		if (object instanceof XAbstractFeatureCall) {
			StringBuilder stringBuilder = new StringBuilder();
			XAbstractFeatureCall featureCall = (XAbstractFeatureCall) object;
			JvmIdentifiableElement feature = featureCall.getFeature();
			if (feature != null && !feature.eIsProxy()) {
				if (featureCall instanceof XMemberFeatureCall) {
					if (((JvmOperation) feature).isStatic()) {
						stringBuilder.append(((JvmOperation) feature).getDeclaringType().getSimpleName());
						stringBuilder.append(STATICDELIMITER);
						stringBuilder.append(feature.getSimpleName());
						stringBuilder.append(computeArguments(featureCall));
						return stringBuilder.toString();
					}
				}
				if (featureCall.getImplicitReceiver() != null || featureCall.getImplicitFirstArgument() != null) {
					XExpression receiver = featureCallToJavaMapping.getActualReceiver(featureCall);
					if (receiver instanceof XMemberFeatureCall) {
						stringBuilder.append(((XMemberFeatureCall) receiver).getFeature().getSimpleName());
						stringBuilder.append(DELIMITER);
					} else {
						if (receiver instanceof XAbstractFeatureCall) {
							JvmIdentifiableElement receiverFeature = ((XAbstractFeatureCall) receiver).getFeature();
							if (receiverFeature.getSimpleName().equals(IT)) {
								stringBuilder.append(IT);
								stringBuilder.append(DELIMITER);
							}

						}
					}
					stringBuilder.append(feature.getSimpleName());
					stringBuilder.append(computeArguments(featureCall));
					return stringBuilder.toString();
				}
			}
		}
		return "";
	}

	public String computeArguments(XAbstractFeatureCall featureCall) {
		StringBuilder stringBuilder = new StringBuilder("(");
		XExpression implicitFirstArgument = null;
		List<XExpression> arguments = Lists.newArrayList();
		boolean needsSeperator = false;
		if (featureCall instanceof XMemberFeatureCall) {
			arguments = ((XMemberFeatureCall) featureCall).getMemberCallArguments();
			implicitFirstArgument = ((XMemberFeatureCall) featureCall).getMemberCallTarget();
			needsSeperator = implicitFirstArgument != null && arguments.size() > 0;
		} else {
			implicitFirstArgument = featureCall.getImplicitFirstArgument();
			arguments = featureCallToJavaMapping.getActualArguments(featureCall);
			needsSeperator = implicitFirstArgument != null && arguments.size() > 1;
		}
		
		XbaseSwitch<String> xbaseSwitch = new XtendHoverXbaseSwitch();
		if (implicitFirstArgument != null) {
			String doSwitch = xbaseSwitch.doSwitch(implicitFirstArgument).trim();
			if (doSwitch != null)
				stringBuilder.append(doSwitch);
		}
		if (arguments.size() > 0) {
			XExpression first = arguments.get(0);
			if(needsSeperator)
				stringBuilder.append(SEPERATOR);
			
			if (first == implicitFirstArgument && arguments.size() > 1) {
				first = arguments.get(1);
			}
			XExpression last = arguments.get(arguments.size() - 1);
			if (last != implicitFirstArgument) {
				int startOffset = NodeModelUtils.getNode(first).getTotalOffset();
				int endOffset = NodeModelUtils.getNode(last).getTotalEndOffset();

				String model = ((XtextResource) featureCall.eResource()).getParseResult().getRootNode().getText();
				stringBuilder.append(model.substring(startOffset, endOffset).trim());
			}
		}

		stringBuilder.append(")");
		return stringBuilder.toString();
	}

	private static final class XtendHoverXbaseSwitch extends XbaseSwitch<String> {
		@Override
		public String caseXAbstractFeatureCall(XAbstractFeatureCall object) {
			ICompositeNode node = NodeModelUtils.getNode(object);
			if (node != null)
				return node.getText();
			else
				return object.getFeature().getSimpleName();
		}

		@Override
		public String caseXExpression(XExpression object) {
			return NodeModelUtils.getNode(object).getText();
		}
	}

	public Pair<String, String> computePreAndSuffix(EObject element) {
		ICompositeNode node = NodeModelUtils.getNode(element);
		if (node != null) {
			String model = ((XtextResource) element.eResource()).getParseResult().getRootNode().getText();
			return Tuples.create(model.substring(0, node.getTotalOffset()) + "\n",
					"\n" + model.substring(node.getTotalEndOffset()));
		} else {
			return Tuples.create("", "");
		}

	}
}