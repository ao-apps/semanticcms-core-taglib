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

import com.aoindustries.io.NullWriter;
import com.aoindustries.io.buffer.BufferWriter;
import com.aoindustries.io.buffer.SegmentedWriter;
import com.aoindustries.servlet.http.Dispatcher;
import com.aoindustries.web.page.Node;
import com.aoindustries.web.page.Page;
import com.aoindustries.web.page.PageRef;
import com.aoindustries.web.page.servlet.CaptureLevel;
import com.aoindustries.web.page.servlet.CapturePage;
import com.aoindustries.web.page.servlet.CurrentNode;
import com.aoindustries.web.page.servlet.CurrentPage;
import com.aoindustries.web.page.servlet.PageRefResolver;
import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.SkipPageException;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class PageTag extends SimpleTagSupport {

	private final Page page = new Page();

	public void setTitle(String title) {
		page.setTitle(title);
    }

	public void setToc(String toc) {
		page.setToc(
			"auto".equalsIgnoreCase(toc)
			? null
			: Boolean.valueOf(toc)
		);
	}

	public void setTocLevels(int tocLevels) {
		page.setTocLevels(tocLevels);
	}

	@Override
    public void doTag() throws JspException, IOException {
		try {
			final PageContext pageContext = (PageContext)getJspContext();
			final HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
			final ServletContext servletContext = pageContext.getServletContext();

			// Find the path to this page
			PageRef pageRef = PageRefResolver.getCurrentPageRef(servletContext, request);
			page.setPageRef(pageRef);
			page.setSrc(pageRef);

			{
				// Pages may not be nested within any kind of node
				Node parentNode = CurrentNode.getCurrentNode(request);
				if(parentNode != null) throw new JspTagException("Pages may not be nested within other nodes: " + page.getPageRef() + " not allowed inside of " + parentNode);
				assert CurrentPage.getCurrentPage(request) == null : "When no parent node, cannot have a parent page";
			}

			// Set currentNode
			CurrentNode.setCurrentNode(request, page);
			try {
				// Set currentPage
				CurrentPage.setCurrentPage(request, page);
				try {
					// Freeze page once body done
					try {
						// Unlike elements, the page body is still invoked on captureLevel=PAGE, this
						// is done to catch childen.
						JspFragment body = getJspBody();
						if(body != null) {
							final CaptureLevel captureLevel = CaptureLevel.getCaptureLevel(request);
							if(captureLevel == CaptureLevel.BODY) {
								// Invoke page body, capturing output
								BufferWriter capturedOut = new SegmentedWriter();
								try {
									body.invoke(capturedOut);
								} finally {
									capturedOut.close();
								}
								page.setBody(capturedOut.getResult().trim());
							} else {
								// Invoke page body, discarding output
								body.invoke(NullWriter.getInstance());
							}
						}
					} finally {
						page.freeze();
					}
				} finally {
					// Restore previous currentPage
					CurrentPage.setCurrentPage(request, null);
				}
			} finally {
				// Restore previous currentNode
				CurrentNode.setCurrentNode(request, null);
			}
			CapturePage capture = CapturePage.getCaptureContext(request);
			if(capture != null) {
				// Capturing, add to capture
				capture.setCapturedPage(page);
			} else {
				// Display page directly
				// Forward to PAGE_TEMPLATE_JSP_PATH, passing PAGE_REQUEST_ATTRIBUTE request attribute
				Object oldValue = request.getAttribute(com.aoindustries.web.page.servlet.Page.PAGE_REQUEST_ATTRIBUTE);
				try {
					// Pass PAGE_REQUEST_ATTRIBUTE attribute
					request.setAttribute(com.aoindustries.web.page.servlet.Page.PAGE_REQUEST_ATTRIBUTE, page);
					Dispatcher.forward(
						servletContext,
						com.aoindustries.web.page.servlet.Page.PAGE_TEMPLATE_JSP_PATH,
						request,
						(HttpServletResponse)pageContext.getResponse()
					);
				} finally {
					// Restore old value of PAGE_REQUEST_ATTRIBUTE attribute
					request.setAttribute(com.aoindustries.web.page.servlet.Page.PAGE_REQUEST_ATTRIBUTE, oldValue);
				}
				throw new SkipPageException();
			}
		} catch(ServletException e) {
			throw new JspTagException(e);
		}
	}
}
