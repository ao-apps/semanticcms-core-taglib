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
import static com.aoindustries.encoding.TextInXhtmlEncoder.encodeTextInXhtml;
import static com.aoindustries.encoding.TextInXhtmlEncoder.textInXhtmlEncoder;
import com.aoindustries.io.NullWriter;
import com.aoindustries.io.buffer.BufferResult;
import com.aoindustries.io.buffer.SegmentedWriter;
import com.aoindustries.net.EmptyParameters;
import com.aoindustries.net.HttpParameters;
import com.aoindustries.net.HttpParametersMap;
import com.aoindustries.net.MutableHttpParameters;
import com.aoindustries.servlet.http.LastModifiedServlet;
import com.aoindustries.servlet.jsp.LocalizedJspTagException;
import static com.aoindustries.taglib.ApplicationResources.accessor;
import com.aoindustries.taglib.ParamUtils;
import com.aoindustries.taglib.ParamsAttribute;
import com.aoindustries.taglib.UrlUtils;
import com.aoindustries.web.page.Element;
import com.aoindustries.web.page.Node;
import com.aoindustries.web.page.Page;
import com.aoindustries.web.page.PageRef;
import com.aoindustries.web.page.servlet.CaptureLevel;
import com.aoindustries.web.page.servlet.CapturePage;
import com.aoindustries.web.page.servlet.CurrentNode;
import com.aoindustries.web.page.servlet.CurrentPage;
import com.aoindustries.web.page.servlet.LinkImpl;
import com.aoindustries.web.page.servlet.PageIndex;
import com.aoindustries.web.page.servlet.PageRefResolver;
import java.io.IOException;
import java.net.URLEncoder;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.DynamicAttributes;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class LinkTag
	extends SimpleTagSupport
	implements
		DynamicAttributes,
		ParamsAttribute
{

    private MutableHttpParameters params;
	private String clazz;
	private String book;
	private String page;
	private String element;
	private String view;

    @Override
    public HttpParameters getParams() {
        return params==null ? EmptyParameters.getInstance() : params;
    }

    @Override
    public void addParam(String name, String value) {
        if(params==null) params = new HttpParametersMap();
        params.addParameter(name, value);
    }

	public String getClazz() {
		return clazz;
	}

	public void setClazz(String clazz) {
		this.clazz = clazz;
    }

	public void setBook(String book) {
		this.book = book;
    }

	public void setPage(String page) {
		this.page = page;
    }

	public void setElement(String element) {
		this.element = element;
	}

	public void setView(String view) {
		this.view = view==null || view.isEmpty() ? null : view;
	}

	@Override
	public void setDynamicAttribute(String uri, String localName, Object value) throws JspTagException {
		if(
			uri==null
			&& localName.startsWith(ParamUtils.PARAM_ATTRIBUTE_PREFIX)
		) {
			ParamUtils.setDynamicAttribute(this, uri, localName, value);
		} else {
			throw new LocalizedJspTagException(
				accessor,
				"error.unexpectedDynamicAttribute",
				localName,
				ParamUtils.PARAM_ATTRIBUTE_PREFIX+"*"
			);
		}
	}

	@Override
	public void doTag() throws JspException, IOException {
		if(page==null && element==null && view==null) throw new JspTagException("If neither element nor view provided, then page is required.");

		final PageContext pageContext = (PageContext)getJspContext();
		final HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();

		// Get the current capture state
		final CaptureLevel captureLevel = CaptureLevel.getCaptureLevel(request);
		if(captureLevel.compareTo(CaptureLevel.META) >= 0) {
			// Capture the body first for any nested parameter tags
			BufferResult capturedBody;
			if(captureLevel == CaptureLevel.BODY) {
				JspFragment body = getJspBody();
				if(body != null) {
					SegmentedWriter captureOut = new SegmentedWriter();
					try {
						body.invoke(captureOut);
					} finally {
						captureOut.close();
					}
					capturedBody = captureOut.getResult().trim();
				} else {
					capturedBody = null;
				}
			} else {
				// Invoke body for any meta data, but discard any output
				JspFragment body = getJspBody();
				if(body != null) body.invoke(NullWriter.getInstance());
				capturedBody = null;
			}
			
			final Node currentNode = CurrentNode.getCurrentNode(request);
			final Page currentPage = CurrentPage.getCurrentPage(request);
			
			final ServletContext servletContext = pageContext.getServletContext();
			// Use current page when page not set
			PageRef targetPageRef;
			if(page == null) {
				if(this.book != null) throw new JspTagException("page must be provided when book is provided.");
				if(currentPage == null) throw new JspTagException("<p:link> must be nested in <p:page> when page attribute not set.");
				targetPageRef = currentPage.getPageRef();
			} else {
				try {
					targetPageRef = PageRefResolver.getPageRef(servletContext, request, this.book, this.page);
				} catch(ServletException e) {
					throw new JspTagException(e);
				}
			}
			// Add page links
			if(currentNode != null) currentNode.addPageLink(targetPageRef);
			if(captureLevel == CaptureLevel.BODY) {
				final HttpServletResponse response = (HttpServletResponse)pageContext.getResponse();
				final String responseEncoding = response.getCharacterEncoding();

				// Capture the page
				Page targetPage;
				if(targetPageRef.getBook()==null) {
					targetPage = null;
				} else if(
					// Short-cut for element already added above within current page
					currentPage != null
					&& targetPageRef.equals(currentPage.getPageRef())
					&& (
						element==null
						|| currentPage.getElementsById().containsKey(element)
					)
				) {
					targetPage = currentPage;
				} else {
					// Capture required, even if capturing self
					try {
						targetPage = CapturePage.capturePage(
							servletContext,
							request,
							response,
							targetPageRef,
							element==null ? CaptureLevel.PAGE : CaptureLevel.META
						);
					} catch(ServletException e) {
						throw new JspTagException(e);
					}
				}

				// Find the element
				Element targetElement;
				if(element != null && targetPage != null) {
					targetElement = targetPage.getElementsById().get(element);
					if(targetElement == null) throw new JspTagException("Element not found in target page: " + element);
				} else {
					targetElement = null;
				}

				// Write a link to the page
				final JspWriter out = pageContext.getOut();

				PageIndex pageIndex = PageIndex.getCurrentPageIndex(request);
				Integer index = pageIndex==null ? null : pageIndex.getPageIndex(targetPageRef);

				out.write("<a");
				String href;
				{
					if(element == null) {
						// Link to page
						if(index != null && view == null) {
							href = '#' + PageIndex.getRefId(index, null);
						} else {
							StringBuilder url = new StringBuilder();
							targetPageRef.appendServletPath(url);
							if(view != null) {
								boolean hasQuestion = url.lastIndexOf("?") != -1;
								url
									.append(hasQuestion ? "&view=" : "?view=")
									.append(URLEncoder.encode(view, responseEncoding));
							}
							href = url.toString();
						}
					} else {
						if(index != null && view == null) {
							// Link to target in indexed page (view=all mode)
							href = '#' + PageIndex.getRefId(index, element);
						} else if(currentPage!=null && currentPage.equals(targetPage) && view == null) {
							// Link to target on same page
							href = '#' + element;
						} else {
							// Link to target on different page (or same page, different view)
							StringBuilder url = new StringBuilder();
							targetPageRef.appendServletPath(url);
							if(view != null) {
								boolean hasQuestion = url.lastIndexOf("?") != -1;
								url
									.append(hasQuestion ? "&view=" : "?view=")
									.append(URLEncoder.encode(view, responseEncoding));
							}
							url.append('#').append(element);
							href = url.toString();
						}
					}
				}
				UrlUtils.writeHref(out, getJspContext(), href, params, false, LastModifiedServlet.AddLastModifiedWhen.FALSE);
				if(clazz != null) {
					if(!clazz.isEmpty()) {
						out.write(" class=\"");
						encodeTextInXhtmlAttribute(clazz, out);
						out.write("\"");
					}
				} else {
					if(targetElement != null) {
						String linkCssClass = targetElement.getLinkCssClass();
						if(linkCssClass != null) {
							out.write(" class=\"");
							encodeTextInXhtmlAttribute(linkCssClass, out);
							out.write('"');
						}
					}
				}
				// No search index all view to avoid duplicate content penalties
				if("all".equals(view)) {
					out.write(" rel=\"nofollow\"");
				}
				out.write('>');

				if(capturedBody == null || capturedBody.getLength()==0) {
					if(targetElement != null) {
						targetElement.appendLabel(new MediaWriter(textInXhtmlEncoder, out));
					} else if(targetPage!=null) {
						encodeTextInXhtml(targetPage.getTitle(), out);
					} else {
						LinkImpl.writeBrokenPathInXhtml(targetPageRef, element, out);
					}
					if(index != null) {
						out.write("<sup>[");
						encodeTextInXhtml(Integer.toString(index+1), out);
						out.write("]</sup>");
					}
				} else {
					capturedBody.writeTo(out);
				}
				out.write("</a>");
			}
		}
	}
}
