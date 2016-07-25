/*
 * ao-web-page-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2013, 2014, 2015, 2016  AO Industries, Inc.
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
import com.aoindustries.web.page.servlet.PageRefResolver;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class NavigationTreeTag extends SimpleTagSupport {

	private Page root;
	public void setRoot(Page root) {
		this.root = root;
	}

	private boolean skipRoot;
	public void setSkipRoot(boolean skipRoot) {
		this.skipRoot = skipRoot;
	}

	private boolean yuiConfig;
	public void setYuiConfig(boolean yuiConfig) {
		this.yuiConfig = yuiConfig;
	}

	private boolean includeElements;
	public void setIncludeElements(boolean includeElements) {
		this.includeElements = includeElements;
	}

	private String target;
	public void setTarget(String target) {
		this.target = target;
	}

	private String thisBook;
	public void setThisBook(String thisBook) {
		this.thisBook = thisBook;
	}

	private String thisPage;
	public void setThisPage(String thisPage) {
		this.thisPage = thisPage;
	}

	private String linksToBook;
	public void setLinksToBook(String linksToBook) {
		this.linksToBook = linksToBook;
	}

	private String linksToPage;
	public void setLinksToPage(String linksToPage) {
		this.linksToPage = linksToPage;
	}

	private int maxDepth;
	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	private boolean findLinks(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		PageRef linksTo,
		Set<Node> nodesWithLinks,
		Set<Node> nodesWithChildLinks,
		Node node
	) throws JspTagException, MalformedURLException, ServletException, IOException {
		boolean hasChildLink = false;
		if(node.getPageLinks().contains(linksTo)) {
			nodesWithLinks.add(node);
			hasChildLink = true;
		}
		if(includeElements) {
			for(Element childElem : node.getChildElements()) {
				if(findLinks(servletContext, request, response, linksTo, nodesWithLinks, nodesWithChildLinks, childElem)) {
					hasChildLink = true;
				}
			}
		} else {
			// Not including elements, so any link from an element must be considered a link from the page the element is on
			assert (node instanceof Page);
			Page page = (Page)node;
			for(Element e : page.getElements()) {
				if(e.getPageLinks().contains(linksTo)) {
					nodesWithLinks.add(node);
					hasChildLink = true;
					break;
				}
			}
		}
		if(node instanceof Page) {
			for(PageRef childRef : ((Page)node).getChildPages()) {
				Page child = CapturePage.capturePage(servletContext, request, response, childRef, CaptureLevel.META);
				if(findLinks(servletContext, request, response, linksTo, nodesWithLinks, nodesWithChildLinks, child)) {
					hasChildLink = true;
				}
			}
		}
		if(hasChildLink) {
			nodesWithChildLinks.add(node);
		}
		return hasChildLink;
	}

	public static <T> List<T> filterChildren(List<T> children, Set<T> pagesToInclude) {
		int size = children.size();
		if(size == 0) return children;
		List<T> filtered = new ArrayList<>(size);
		for(T child : children) {
			if(pagesToInclude.contains(child)) {
				filtered.add(child);
			}
		}
		return filtered;
	}

	public static List<Node> getChildNodes(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		boolean includeElements,
		boolean metaCapture,
		Node node
	) throws ServletException, IOException {
		// Both elements and pages are child nodes
		List<Element> childElements = includeElements ? node.getChildElements() : null;
		List<PageRef> childPages = (node instanceof Page) ? ((Page)node).getChildPages() : null;
		List<Node> childNodes = new ArrayList<>(
			(childElements==null ? 0 : childElements.size())
			+ (childPages==null ? 0 : childPages.size())
		);
		if(includeElements) {
			childNodes.addAll(childElements);
		}
		if(childPages != null) {
			for(PageRef childRef : childPages) {
				Page childPage = CapturePage.capturePage(servletContext, request, response, childRef, includeElements || metaCapture ? CaptureLevel.META : CaptureLevel.PAGE);
				childNodes.add(childPage);
			}
		}
		return childNodes;
	}

	/**
	 * Creates the nested &lt;ul&gt; and &lt;li&gt; tags used by TreeView.
	 * The first level is expanded.
	 *
	 * {@link http://developer.yahoo.com/yui/treeview/#start}
	 */
	@Override
	public void doTag() throws JspTagException, IOException {
		try {
			final PageContext pageContext = (PageContext)getJspContext();
			final HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();

			// Get the current capture state
			final CaptureLevel captureLevel = CaptureLevel.getCaptureLevel(request);
			if(captureLevel.compareTo(CaptureLevel.META) >= 0) {
				final Node currentNode = CurrentNode.getCurrentNode(request);
				final ServletContext servletContext = pageContext.getServletContext();
				final HttpServletResponse response = (HttpServletResponse)pageContext.getResponse();
				// Filter by link-to
				final Set<Node> nodesWithLinks;
				final Set<Node> nodesWithChildLinks;
				if(linksToPage == null) {
					if(linksToBook != null) throw new JspTagException("linksToPage must be provided when linksToBook is provided.");
					nodesWithLinks = null;
					nodesWithChildLinks = null;
				} else {
					// Find all nodes in the navigation tree that link to the linksToPage
					PageRef linksTo = PageRefResolver.getPageRef(servletContext, request, linksToBook, linksToPage);
					nodesWithLinks = new HashSet<>();
					nodesWithChildLinks = new HashSet<>();
					findLinks(servletContext, request, response, linksTo, nodesWithLinks, nodesWithChildLinks, root);
				}

				PageRef thisPageRef;
				if(thisPage == null) {
					if(thisBook != null) throw new JspTagException("thisPage must be provided when thisBook is provided.");
					thisPageRef = null;
				} else {
					thisPageRef = PageRefResolver.getPageRef(servletContext, request, thisBook, thisPage);
				}

				boolean foundThisPage = false;
				PageIndex pageIndex = PageIndex.getCurrentPageIndex(request);
				final JspWriter out = captureLevel == CaptureLevel.BODY ? pageContext.getOut() : null;
				if(skipRoot) {
					List<Node> childNodes = getChildNodes(servletContext, request, response, includeElements, false, root);
					if(nodesWithChildLinks != null) {
						childNodes = filterChildren(childNodes, nodesWithChildLinks);
					}
					if(!childNodes.isEmpty()) {
						if(out != null) out.write("<ul>\n");
						for(Node childNode : childNodes) {
							foundThisPage = writeNode(
								servletContext,
								request,
								response,
								currentNode,
								nodesWithLinks,
								nodesWithChildLinks,
								pageIndex,
								out,
								childNode,
								yuiConfig,
								includeElements,
								target,
								thisPageRef,
								foundThisPage,
								maxDepth,
								1
							);
						}
						if(out != null) out.write("</ul>\n");
					}
				} else {
					if(out != null) out.write("<ul>\n");
					/*foundThisPage =*/ writeNode(
						servletContext,
						request,
						response,
						currentNode,
						nodesWithLinks,
						nodesWithChildLinks,
						pageIndex,
						out,
						root,
						yuiConfig,
						includeElements,
						target,
						thisPageRef,
						foundThisPage,
						maxDepth,
						1
					);
					if(out != null) out.write("</ul>\n");
				}
			}
		} catch(ServletException e) {
			throw new JspTagException(e);
		}
	}

	private static boolean writeNode(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		Node currentNode,
		Set<Node> nodesWithLinks,
		Set<Node> nodesWithChildLinks,
		PageIndex pageIndex,
		JspWriter out,
		Node node,
		boolean yuiConfig,
		boolean includeElements,
		String target,
		PageRef thisPageRef,
		boolean foundThisPage,
		int maxDepth,
		int level
	) throws IOException, ServletException {
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
			if(yuiConfig) {
				out.write(" yuiConfig='{\"data\":\"");
				encodeTextInXhtmlAttribute(Functions.encodeHexData(servletPath), out);
				out.write("\"}'");
			}
			String listItemCssClass = node.getListItemCssClass();
			if(listItemCssClass == null) listItemCssClass = "list-item-none";
			out.write(" class=\"");
			encodeTextInXhtmlAttribute(listItemCssClass, out);
			if(level==1) out.write(" expanded");
			out.write("\"><a");
		}
		// Look for thisPage match
		boolean thisPageClass = false;
		if(pageRef.equals(thisPageRef) && element == null) {
			if(!foundThisPage) {
				if(out != null) out.write(" id=\"tree-this-page\"");
				foundThisPage = true;
			}
			thisPageClass = true;
		}
		// Look for linkToPage match
		boolean linksToPageClass = nodesWithLinks!=null && nodesWithLinks.contains(node);
		if(out != null && (thisPageClass || linksToPageClass)) {
			out.write(" class=\"");
			if(thisPageClass && nodesWithLinks!=null && !linksToPageClass) {
				out.write("no-link-to-this-page");
			} else if(thisPageClass) {
				out.write("tree-this-page");
			} else if(linksToPageClass) {
				out.write("links-to-page");
			} else {
				throw new AssertionError();
			}
			out.write('"');
		}
		if(out != null) {
			if(target != null) {
				out.write(" target=\"");
				encodeTextInXhtmlAttribute(target, out);
				out.write('"');
			}
			out.write(" href=\"");
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
		}
		if(maxDepth==0 || level < maxDepth) {
			List<Node> childNodes = getChildNodes(servletContext, request, response, includeElements, false, node);
			if(nodesWithChildLinks!=null) {
				childNodes = filterChildren(childNodes, nodesWithChildLinks);
			}
			if(!childNodes.isEmpty()) {
				if(out != null) {
					out.write('\n');
					out.write("<ul>\n");
				}
				for(Node childNode : childNodes) {
					foundThisPage = writeNode(
						servletContext,
						request,
						response,
						currentNode,
						nodesWithLinks,
						nodesWithChildLinks,
						pageIndex,
						out,
						childNode,
						yuiConfig,
						includeElements,
						target,
						thisPageRef,
						foundThisPage,
						maxDepth,
						level+1
					);
				}
				if(out != null) out.write("</ul>\n");
			}
		}
		if(out != null) out.write("</li>\n");
		return foundThisPage;
	}
}
