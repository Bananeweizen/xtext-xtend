/*******************************************************************************
 * Copyright (c) 2012 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.ide.hover;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.xtend.core.jvmmodel.IXtendJvmAssociations;
import org.eclipse.xtend.core.typing.XtendOverridesService;
import org.eclipse.xtend.core.xtend.XtendFunction;
import org.eclipse.xtext.TerminalRule;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmExecutable;
import org.eclipse.xtext.common.types.JvmFeature;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.TypesPackage;
import org.eclipse.xtext.documentation.IEObjectDocumentationProvider;
import org.eclipse.xtext.documentation.impl.MultiLineCommentDocumentationProvider;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.ui.editor.hover.html.XtextElementLinks;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author Holger Schill - Initial contribution and API
 */
@SuppressWarnings("unchecked")
public class XtendHoverDocumentationProvider implements IEObjectDocumentationProvider {

	private static final String BLOCK_TAG_START = "<dl>"; //$NON-NLS-1$
	private static final String BLOCK_TAG_END = "</dl>"; //$NON-NLS-1$

	private static final String BlOCK_TAG_ENTRY_START = "<dd>"; //$NON-NLS-1$
	private static final String BlOCK_TAG_ENTRY_END = "</dd>"; //$NON-NLS-1$

	private static final String PARAM_NAME_START = "<b>"; //$NON-NLS-1$
	private static final String PARAM_NAME_END = "</b> "; //$NON-NLS-1$

	private String rawJavaDoc = "";

	private EObject context = null;

	@Inject
	private IXtendJvmAssociations associations;

	@Inject
	private XtendOverridesService overridesService;

	@Inject
	private IScopeProvider scopeProvider;
	
	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	private StringBuffer buffer;

	private int fLiteralContent;

	@Inject(optional = true)
	@Named(MultiLineCommentDocumentationProvider.RULE)
	String ruleName = "ML_COMMENT";

	@Inject(optional = true)
	@Named(MultiLineCommentDocumentationProvider.START_TAG)
	String startTag = "/\\*\\*?"; // regular expression

	public String getDocumentation(EObject object) {
		context = object;
		rawJavaDoc = findComment();

		Javadoc javadoc = getJavaDoc();
		if (javadoc == null)
			return "";

		TagElement deprecatedTag = null;
		TagElement start = null;
		List<TagElement> parameters = new ArrayList<TagElement>();
		TagElement returnTag = null;
		List<TagElement> exceptions = new ArrayList<TagElement>();
		List<TagElement> versions = new ArrayList<TagElement>();
		List<TagElement> authors = new ArrayList<TagElement>();
		List<TagElement> sees = new ArrayList<TagElement>();
		List<TagElement> since = new ArrayList<TagElement>();
		List<TagElement> rest = new ArrayList<TagElement>();

		List<TagElement> tags = javadoc.tags();
		for (Iterator<TagElement> iter = tags.iterator(); iter.hasNext();) {
			TagElement tag = iter.next();
			String tagName = tag.getTagName();
			if (tagName == null) {
				start = tag;

			} else if (TagElement.TAG_PARAM.equals(tagName)) {
				parameters.add(tag);
			} else if (TagElement.TAG_RETURN.equals(tagName)) {
				if (returnTag == null)
					returnTag = tag; // the Javadoc tool only shows the first return tag
			} else if (TagElement.TAG_EXCEPTION.equals(tagName) || TagElement.TAG_THROWS.equals(tagName)) {
				exceptions.add(tag);
			} else if (TagElement.TAG_SINCE.equals(tagName)) {
				since.add(tag);
			} else if (TagElement.TAG_VERSION.equals(tagName)) {
				versions.add(tag);
			} else if (TagElement.TAG_AUTHOR.equals(tagName)) {
				authors.add(tag);
			} else if (TagElement.TAG_SEE.equals(tagName)) {
				sees.add(tag);
			} else if (TagElement.TAG_DEPRECATED.equals(tagName)) {
				if (deprecatedTag == null)
					deprecatedTag = tag; // the Javadoc tool only shows the first deprecated tag
			} else {
				rest.add(tag);
			}
		}
		buffer = new StringBuffer();
		fLiteralContent = 0;
		boolean hasParameters = parameters.size() > 0;
		boolean hasReturnTag = returnTag != null;
		boolean hasExceptions = exceptions.size() > 0;
		if (deprecatedTag != null)
			handleDeprecatedTag(deprecatedTag);
		if (start != null)
			handleContentElements(start.fragments());

		if (hasParameters || hasReturnTag || hasExceptions || versions.size() > 0 || authors.size() > 0
				|| since.size() > 0 || sees.size() > 0 || rest.size() > 0 || (buffer.length() > 0)) {
			handleSuperMethodReferences(object);
			buffer.append(BLOCK_TAG_START);
			handleParameters(object, parameters);
			handleReturnTag(returnTag);
			handleExceptionTags(exceptions);
			handleBlockTags("Since:", since);
			handleBlockTags("Version:", versions);
			handleBlockTags("Author:", authors);
			handleBlockTags("See Also:", sees);
			handleBlockTags(rest);
			buffer.append(BLOCK_TAG_END);
		} else if (buffer.length() > 0) {
			handleSuperMethodReferences(object);
		}
		String result = buffer.toString();
		buffer = null;
		return result;
	}

	private void handleSuperMethodReferences(EObject context) {
		if (context instanceof XtendFunction) {
			XtendFunction function = (XtendFunction) context;
			if (function.isOverride()) {
				JvmOperation overwritten = overridesService.findOverriddenOperation(function);
				buffer.append("<div>"); //$NON-NLS-1$
				buffer.append("<b>"); //$NON-NLS-1$
				buffer.append("Overrides:"); //$NON-NLS-1$
				buffer.append("</b> "); //$NON-NLS-1$
				buffer.append(createMethodInTypeLinks(overwritten));
				buffer.append("</div>"); //$NON-NLS-1$
			}
		}
	}

	private String createMethodInTypeLinks(JvmOperation overridden) {
		String methodLink = createSimpleMemberLink(overridden);
		String typeLink = createSimpleMemberLink(overridden.getDeclaringType());
		String methodInType = MessageFormat.format("{0} in {1}", new Object[] { methodLink, typeLink });
		return methodInType;
	}

	private String createSimpleMemberLink(EObject type) {
		String label = "";
		if (type instanceof JvmDeclaredType)
			label = ((JvmDeclaredType) type).getSimpleName();
		else if (type instanceof JvmOperation) {
			JvmOperation operation = (JvmOperation) type;
			label = operation.getSimpleName();
			if (operation.getParameters().size() > 0) {
				label += "(...)";
			}
		}
		return createLinkWithLabel(EcoreUtil.getURI(type), label);
	}

	private String createLinkWithLabel(org.eclipse.emf.common.util.URI uri, String label) {
		StringBuffer buf = new StringBuffer();
		String emfURI = uri.toString();
		String uriForLink = null;
		buf.append("<a href=\"");
		try {
			uriForLink = new URI(XtextElementLinks.OPEN_LINK_SCHEME, emfURI, null).toASCIIString();
		} catch (URISyntaxException e) {
			return "";
		}
		buf.append(uriForLink);
		buf.append("\">");
		buf.append(label);
		buf.append("</a>");
		return buf.toString();
	}

	private boolean handleValueTag(TagElement node) {
		//TODO: implement me!
		return false;
	}

	private boolean handleInheritDoc(TagElement node) {
		if (!TagElement.TAG_INHERITDOC.equals(node.getTagName()))
			return false;
		//TODO: implement me!
		return true;
	}

	private boolean handleDocRoot(TagElement node) {
		if (!TagElement.TAG_DOCROOT.equals(node.getTagName()))
			return false;
		//TODO: implement me!
		return true;
	}

	private void handleLink(List<? extends ASTNode> fragments) {
		if (fragments.size() > 0) {
			org.eclipse.emf.common.util.URI elementURI = null;
			String firstFragment = fragments.get(0).toString();
			int hashIndex = firstFragment.indexOf("#");
			HoverReference reference = new HoverReference(TypesPackage.Literals.JVM_TYPE);
			IScope scope = scopeProvider.getScope(context, reference);
			if(hashIndex != -1){
				IEObjectDescription declarator = scope.getSingleElement(qualifiedNameConverter.toQualifiedName(firstFragment.substring(0,hashIndex)));
				if(declarator != null){
					EObject resolvedDeclarator = context.eResource().getResourceSet().getEObject(declarator.getEObjectURI(), true);
					if(!resolvedDeclarator.eIsProxy() && resolvedDeclarator instanceof JvmGenericType){
						JvmGenericType castedDeclarator = (JvmGenericType) resolvedDeclarator;
						String signature = firstFragment.substring(hashIndex + 1);
						int indexOfOpenBracket = signature.indexOf("(");
						int indexOfClosingBracket = signature.indexOf(")");
						String[] splitedParameter = signature.substring(indexOfOpenBracket + 1, indexOfClosingBracket).split(",");
						if(indexOfOpenBracket != -1){
							String simpleNameOfFeature = signature.substring(0, indexOfOpenBracket);
							Iterable<JvmFeature> possibleCandidates = castedDeclarator.findAllFeaturesByName(simpleNameOfFeature);
							Iterator<JvmFeature> featureIterator = possibleCandidates.iterator();
							while(elementURI == null && featureIterator.hasNext()){
								JvmFeature feature = featureIterator.next();
								boolean valid = false;
								if(feature instanceof JvmField){
									valid = true;
								} else if(feature instanceof JvmExecutable){
									JvmExecutable executable = (JvmExecutable) feature;
									EList<JvmFormalParameter> parameters = executable.getParameters();
									if(parameters.size() == splitedParameter.length){
										valid = true;
										for(int i = 0; (i < parameters.size() && valid); i++){
											org.eclipse.emf.common.util.URI parameterTypeURI = EcoreUtil.getURI(parameters.get(i).getParameterType().getType());
											IEObjectDescription expectedParameter = scopeProvider.getScope(context, reference).getSingleElement(qualifiedNameConverter.toQualifiedName(splitedParameter[i]));
											if(expectedParameter == null || !expectedParameter.getEObjectURI().equals(parameterTypeURI)){
												valid = false;
											}
										}
									}
								}
								if(valid)
									elementURI = EcoreUtil.getURI(feature);
							}
						}
					}
				}
			} else {
				IEObjectDescription singleElement = scope.getSingleElement(qualifiedNameConverter.toQualifiedName(firstFragment));
				if(singleElement != null)
					elementURI = singleElement.getEObjectURI();
			}
			String label = "";
			Iterator<? extends ASTNode> iterator = fragments.iterator();
			while (iterator.hasNext()) {
				label += iterator.next(); //$NON-NLS-1$
			}
			if (elementURI == null)
				buffer.append(label);
			else {
				buffer.append(createLinkWithLabel(elementURI, label));
			}
		}
	}

	private void handleBlockTags(List<TagElement> tags) {
		for (Iterator<TagElement> iter = tags.iterator(); iter.hasNext();) {
			TagElement tag = iter.next();
			handleBlockTagTitle(tag.getTagName());
			buffer.append(BlOCK_TAG_ENTRY_START);
			handleContentElements(tag.fragments());
			buffer.append(BlOCK_TAG_ENTRY_END);
		}
	}

	private void handleBlockTags(String title, List<TagElement> tags) {
		if (tags.isEmpty())
			return;
		handleBlockTagTitle(title);
		for (Iterator<TagElement> iter = tags.iterator(); iter.hasNext();) {
			TagElement tag = iter.next();
			buffer.append(BlOCK_TAG_ENTRY_START);
			if (TagElement.TAG_SEE.equals(tag.getTagName())) {
				handleSeeTag(tag);
			} else {
				handleContentElements(tag.fragments());
			}
			buffer.append(BlOCK_TAG_ENTRY_END);
		}
	}

	private void handleSeeTag(TagElement tag) {
		handleLink(tag.fragments());
	}

	private void handleExceptionTags(List<TagElement> tags) {
		if (tags.size() == 0)
			return;
		handleBlockTagTitle("Throws:");
		for (Iterator<TagElement> iter = tags.iterator(); iter.hasNext();) {
			TagElement tag = iter.next();
			buffer.append(BlOCK_TAG_ENTRY_START);
			handleThrowsTag(tag);
			buffer.append(BlOCK_TAG_ENTRY_END);
		}
	}

	private void handleThrowsTag(TagElement tag) {
		List<? extends ASTNode> fragments = tag.fragments();
		int size = fragments.size();
		if (size > 0) {
			handleLink(fragments.subList(0, 1));
			if (size > 1) {
				buffer.append(JavaElementLabels.CONCAT_STRING);
				handleContentElements(fragments.subList(1, size));
			}
		}
	}

	private void handleDeprecatedTag(TagElement tag) {
		buffer.append("<p><b>"); //$NON-NLS-1$
		//TODO: Messages out of properties like in JDT?
		buffer.append("Deprecarted.");
		buffer.append("</b> <i>"); //$NON-NLS-1$
		handleContentElements(tag.fragments());
		buffer.append("</i></p>"); //$NON-NLS-1$
	}

	private void handleContentElements(List<? extends ASTNode> nodes) {
		handleContentElements(nodes, false);
	}

	private void handleContentElements(List<? extends ASTNode> nodes, boolean skipLeadingWhitespace) {
		ASTNode previousNode = null;
		for (Iterator<? extends ASTNode> iter = nodes.iterator(); iter.hasNext();) {
			ASTNode child = iter.next();
			if (previousNode != null) {
				int previousEnd = previousNode.getStartPosition() + previousNode.getLength();
				int childStart = child.getStartPosition();
				if (previousEnd > childStart) {
					// should never happen, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=304826
				} else if (previousEnd != childStart) {
					// Need to preserve whitespace before a node that's not
					// directly following the previous node (e.g. on a new line)
					// due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=206518 :
					String textWithStars = rawJavaDoc.substring(previousEnd, childStart);
					String text = removeDocLineIntros(textWithStars);
					buffer.append(text);
				}
			}
			previousNode = child;
			if (child instanceof TextElement) {
				String text = ((TextElement) child).getText();
				if (skipLeadingWhitespace) {
					text = text.replaceFirst("^\\s+", ""); //$NON-NLS-1$ //$NON-NLS-2$
				}
				handleText(text);
			} else if (child instanceof TagElement) {
				handleInlineTagElement((TagElement) child);
			} else {
				// This is unexpected. Fail gracefully by just copying the source.
				int start = child.getStartPosition();
				String text = rawJavaDoc.substring(start, start + child.getLength());
				buffer.append(removeDocLineIntros(text));
			}
		}
	}

	private void handleInlineTagElement(TagElement node) {
		String name = node.getTagName();
		if (TagElement.TAG_VALUE.equals(name) && handleValueTag(node))
			return;
		boolean isLink = TagElement.TAG_LINK.equals(name);
		boolean isLinkplain = TagElement.TAG_LINKPLAIN.equals(name);
		boolean isCode = TagElement.TAG_CODE.equals(name);
		boolean isLiteral = TagElement.TAG_LITERAL.equals(name);
		if (isLiteral || isCode)
			fLiteralContent++;
		if (isLink || isCode)
			buffer.append("<code>"); //$NON-NLS-1$
		if (isLink || isLinkplain)
			handleLink(node.fragments());
		else if (isCode || isLiteral)
			handleContentElements(node.fragments(), true);
		else if (handleInheritDoc(node)) {
			// handled
		} else if (handleDocRoot(node)) {
			// handled
		} else {
			//print uninterpreted source {@tagname ...} for unknown tags
			int start = node.getStartPosition();
			String text = rawJavaDoc.substring(start, start + node.getLength());
			buffer.append(removeDocLineIntros(text));
		}
		if (isLink || isCode)
			buffer.append("</code>"); //$NON-NLS-1$
		if (isLiteral || isCode)
			fLiteralContent--;

	}

	private void handleText(String text) {
		if (fLiteralContent == 0) {
			buffer.append(text);
		} else {
			appendEscaped(buffer, text);
		}
	}

	private static void appendEscaped(StringBuffer buf, String text) {
		int nextToCopy = 0;
		int length = text.length();
		for (int i = 0; i < length; i++) {
			char ch = text.charAt(i);
			String rep = null;
			switch (ch) {
				case '&':
					rep = "&amp;"; //$NON-NLS-1$
					break;
				case '"':
					rep = "&quot;"; //$NON-NLS-1$
					break;
				case '<':
					rep = "&lt;"; //$NON-NLS-1$
					break;
				case '>':
					rep = "&gt;"; //$NON-NLS-1$
					break;
			}
			if (rep != null) {
				if (nextToCopy < i)
					buf.append(text.substring(nextToCopy, i));
				buf.append(rep);
				nextToCopy = i + 1;
			}
		}
		if (nextToCopy < length)
			buf.append(text.substring(nextToCopy));
	}

	private String removeDocLineIntros(String textWithStars) {
		String lineBreakGroup = "(\\r\\n?|\\n)"; //$NON-NLS-1$
		String noBreakSpace = "[^\r\n&&\\s]"; //$NON-NLS-1$
		return textWithStars.replaceAll(lineBreakGroup + noBreakSpace + "*\\*" /*+ noBreakSpace + '?'*/, "$1"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void handleParameters(EObject object, List<TagElement> parameters) {
		if (parameters.size() == 0)
			return;

		handleBlockTagTitle("Parameters:");

		for (Iterator<TagElement> iter = parameters.iterator(); iter.hasNext();) {
			TagElement tag = iter.next();
			buffer.append(BlOCK_TAG_ENTRY_START);
			handleParamTag(tag);
			buffer.append(BlOCK_TAG_ENTRY_END);
		}
	}

	private void handleParamTag(TagElement tag) {
		List<? extends ASTNode> fragments = tag.fragments();
		int i = 0;
		int size = fragments.size();
		if (size > 0) {
			Object first = fragments.get(0);
			buffer.append(PARAM_NAME_START);
			if (first instanceof SimpleName) {
				String name = ((SimpleName) first).getIdentifier();
				buffer.append(name);
				i++;
			} else if (first instanceof TextElement) {
				String firstText = ((TextElement) first).getText();
				if ("<".equals(firstText)) { //$NON-NLS-1$
					buffer.append("&lt;"); //$NON-NLS-1$
					i++;
					if (size > 1) {
						Object second = fragments.get(1);
						if (second instanceof SimpleName) {
							String name = ((SimpleName) second).getIdentifier();
							buffer.append(name);
							i++;
							if (size > 2) {
								Object third = fragments.get(2);
								String thirdText = ((TextElement) third).getText();
								if (">".equals(thirdText)) { //$NON-NLS-1$
									buffer.append("&gt;"); //$NON-NLS-1$
									i++;
								}
							}
						}
					}
				}
			}
			buffer.append(PARAM_NAME_END);

			handleContentElements(fragments.subList(i, fragments.size()));
		}
	}

	private void handleReturnTag(TagElement tag) {
		if (tag == null)
			return;

		handleBlockTagTitle("Returns:");
		buffer.append(BlOCK_TAG_ENTRY_START);
		if (tag != null)
			handleContentElements(tag.fragments());

		buffer.append(BlOCK_TAG_ENTRY_END);
	}

	private void handleBlockTagTitle(String title) {
		buffer.append("<dt>"); //$NON-NLS-1$
		buffer.append(title);
		buffer.append("</dt>"); //$NON-NLS-1$
	}

	protected String findComment() {
		String returnValue = null;
		ICompositeNode node = NodeModelUtils.getNode(context);
		if (node != null) {
			// get the last multi line comment before a non hidden leaf node
			for (INode abstractNode : node.getAsTreeIterable()) {
				if (abstractNode instanceof ILeafNode && !((ILeafNode) abstractNode).isHidden())
					break;
				if (abstractNode instanceof ILeafNode && abstractNode.getGrammarElement() instanceof TerminalRule
						&& ruleName.equalsIgnoreCase(((TerminalRule) abstractNode.getGrammarElement()).getName())) {
					String comment = ((ILeafNode) abstractNode).getText();
					if (comment.matches("(?s)" + startTag + ".*")) {
						returnValue = comment;
					}
				}
			}
		}
		return returnValue;
	}

	public Javadoc getJavaDoc() {

		ASTParser parser = ASTParser.newParser(ASTProvider.SHARED_AST_LEVEL);

		IJavaProject javaProject = (IJavaProject) ((XtextResourceSet) context.eResource().getResourceSet())
				.getClasspathURIContext();
		parser.setProject(javaProject);
		Map<String, String> options = javaProject.getOptions(true);
		options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED); // workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=212207
		parser.setCompilerOptions(options);

		String source = rawJavaDoc + "class C{}"; //$NON-NLS-1$
		parser.setSource(source.toCharArray());

		CompilationUnit root = (CompilationUnit) parser.createAST(null);
		if (root == null)
			return null;
		List<AbstractTypeDeclaration> types = root.types();
		if (types.size() != 1)
			return null;
		AbstractTypeDeclaration type = types.get(0);
		return type.getJavadoc();
	}

}
