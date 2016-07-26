/*
 * ao-web-page-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2014, 2015, 2016  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of ao-web-page-taglib.
 *
 * ao-web-page-taglib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ao-web-page-taglib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ao-web-page-taglib.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoindustries.web.page.taglib;

import com.aoindustries.encoding.MediaWriter;
import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.encodeTextInXhtmlAttribute;
import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.textInXhtmlAttributeEncoder;
import static com.aoindustries.encoding.TextInXhtmlEncoder.encodeTextInXhtml;
import static com.aoindustries.encoding.TextInXhtmlEncoder.textInXhtmlEncoder;
import com.aoindustries.net.UrlUtils;
import com.aoindustries.web.page.Element;
import com.aoindustries.web.page.Node;
import com.aoindustries.web.page.Page;
import com.aoindustries.web.page.PageRef;
import com.aoindustries.web.page.servlet.CaptureLevel;
import com.aoindustries.web.page.servlet.CapturePage;
import com.aoindustries.web.page.servlet.CurrentNode;
import com.aoindustries.web.page.servlet.PageIndex;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class FileTreeTag extends SimpleTagSupport {

	private Page root;
	public void setRoot(Page root) {
		this.root = root;
	}

	private boolean includeElements;
	public void setIncludeElements(boolean includeElements) {
		this.includeElements = includeElements;
	}

	private boolean findFiles(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		Set<Node> nodesWithFiles,
		Node node
	) throws ServletException, IOException {
		boolean hasFile = false;
		if(!node.getFiles().isEmpty()) {
			hasFile = true;
		}
		if(includeElements) {
			for(Element childElem : node.getChildElements()) {
				if(findFiles(servletContext, request, response, nodesWithFiles, childElem)) {
					hasFile = true;
				}
			}
		} else {
			// Not including elements, so any file from an element must be considered a file from the page the element is on
			assert (node instanceof Page);
			Page page = (Page)node;
			for(Element e : page.getElements()) {
				if(!e.getFiles().isEmpty()) {
					hasFile = true;
				}
			}
		}
		if(node instanceof Page) {
			for(PageRef childRef : ((Page)node).getChildPages()) {
				Page child = CapturePage.capturePage(servletContext, request, response, childRef, CaptureLevel.META);
				if(findFiles(servletContext, request, response, nodesWithFiles, child)) {
					hasFile = true;
				}
			}
		}
		if(hasFile) {
			nodesWithFiles.add(node);
		}
		return hasFile;
	}

	/**
	 * Creates the nested &lt;ul&gt; and &lt;li&gt; tags for the file tree.
	 */
	@Override
	public void doTag() throws JspException, IOException {
		try {
			final PageContext pageContext = (PageContext)getJspContext();
			final HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();

			// Get the current capture state
			final CaptureLevel captureLevel = CaptureLevel.getCaptureLevel(request);
			if(captureLevel.compareTo(CaptureLevel.META) >= 0) {
				final Node currentNode = CurrentNode.getCurrentNode(request);
				final ServletContext servletContext = pageContext.getServletContext();
				final HttpServletResponse response = (HttpServletResponse)pageContext.getResponse();
				// Filter by has files
				final Set<Node> nodesWithFiles = new HashSet<>();
				findFiles(servletContext, request, response, nodesWithFiles, root);

				final JspWriter out = captureLevel == CaptureLevel.BODY ? pageContext.getOut() : null;
				if(out != null) out.write("<ul>\n");
				writeNode(
					servletContext,
					request,
					response,
					currentNode,
					nodesWithFiles,
					PageIndex.getCurrentPageIndex(request),
					out,
					root,
					includeElements
				);
				if(out != null) out.write("</ul>\n");
			}
		} catch(ServletException e) {
			throw new JspTagException(e);
		}
	}

	private static void writeNode(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		Node currentNode,
		Set<Node> nodesWithFiles,
		PageIndex pageIndex,
		JspWriter out,
		Node node,
		boolean includeElements
	) throws JspException, IOException, ServletException {
		final Page page;
		final Element element;
		if(node instanceof Page) {
			page = (Page)node;
			element = null;
		} else if(node instanceof Element) {
			assert includeElements;
			element = (Element)node;
			page = element.getPage();
		} else {
			throw new AssertionError();
		}
		final PageRef pageRef = page.getPageRef();
		if(currentNode != null) {
			// Add page links
			currentNode.addPageLink(pageRef);
		}
		final String servletPath;
		if(out == null) {
			// Will be unused
			servletPath = null;
		} else {
			if(element == null) {
				servletPath = pageRef.getServletPath();
			} else {
				String elemId = element.getId();
				assert elemId != null;
				servletPath = pageRef.getServletPath() + '#' + elemId;
			}
		}
		if(out != null) {
			out.write("<li");
			String listItemCssClass = node.getListItemCssClass();
			if(listItemCssClass == null) listItemCssClass = "ao-web-page-list-item-none";
			out.write(" class=\"");
			encodeTextInXhtmlAttribute(listItemCssClass, out);
			out.write("\"><a href=\"");
			Integer index = pageIndex==null ? null : pageIndex.getPageIndex(pageRef);
			if(index != null) {
				out.write('#');
				PageIndex.appendIdInPage(
					index,
					element==null ? null : element.getId(),
					new MediaWriter(textInXhtmlAttributeEncoder, out)
				);
			} else {
				encodeTextInXhtmlAttribute(
					response.encodeURL(
						UrlUtils.encodeUrlPath(
							request.getContextPath() + servletPath,
							response.getCharacterEncoding()
						)
					),
					out
				);
			}
			out.write("\">");
			node.appendLabel(new MediaWriter(textInXhtmlEncoder, out));
			if(index != null) {
				out.write("<sup>[");
				encodeTextInXhtml(Integer.toString(index+1), out);
				out.write("]</sup>");
			}
			out.write("</a>");
			final Set<PageRef> files;
			if(includeElements) {
				files = node.getFiles();
			} else {
				assert node == page;
				// Gather all files referenced by the page or any of its elements
				files = new LinkedHashSet<>();
				files.addAll(page.getFiles());
				for(Element e : page.getElements()) {
					files.addAll(e.getFiles());
				}
			}
			for(PageRef file : files) {
				out.write("\n<div>");
				FileTag.writeFileLink(
					servletContext,
					request,
					response,
					out,
					null,
					file
				);
				out.write("</div>");
			}
		}
		List<Node> childNodes = NavigationTreeTag.getChildNodes(servletContext, request, response, includeElements, true, node);
		childNodes = NavigationTreeTag.filterChildren(childNodes, nodesWithFiles);
		if(!childNodes.isEmpty()) {
			if(out != null) {
				out.write('\n');
				out.write("<ul>\n");
			}
			for(Node childNode : childNodes) {
				writeNode(servletContext, request, response, currentNode, nodesWithFiles, pageIndex, out, childNode, includeElements);
			}
			if(out != null) out.write("</ul>\n");
		}
		if(out != null) out.write("</li>\n");
	}
}
