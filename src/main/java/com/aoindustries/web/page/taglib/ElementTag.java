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
import com.aoindustries.io.TempFileList;
import com.aoindustries.io.buffer.AutoTempFileWriter;
import com.aoindustries.io.buffer.BufferWriter;
import com.aoindustries.io.buffer.SegmentedWriter;
import com.aoindustries.servlet.filter.TempFileContext;
import com.aoindustries.web.page.servlet.CaptureLevel;
import com.aoindustries.web.page.servlet.CurrentNode;
import com.aoindustries.web.page.servlet.CurrentPage;
import com.semanticcms.core.model.Element;
import com.semanticcms.core.model.ElementWriter;
import com.semanticcms.core.model.Node;
import com.semanticcms.core.model.NodeBodyWriter;
import com.semanticcms.core.model.Page;
import java.io.IOException;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.SimpleTagSupport;

/**
 * The base tag for capturing elements.
 */
abstract public class ElementTag<E extends Element> extends SimpleTagSupport implements ElementWriter {

	protected final E element;

	protected ElementTag(E element) {
		this.element = element;
	}

	public void setId(String id) throws JspTagException {
		element.setId(id);
    }

	/**
	 * Adds this element to the current page, if part of a page.
	 * Sets this element as the current element.
	 * Then, if not capturing or capturing META or higher, calls {@link #doBody}
	 */
	@Override
    public void doTag() throws JspException, IOException {
		final PageContext pageContext = (PageContext)getJspContext();
		final ServletRequest request = pageContext.getRequest();

		// Get the current capture state
		CaptureLevel captureLevel = CaptureLevel.getCaptureLevel(request);
		if(captureLevel.compareTo(CaptureLevel.META) >= 0) {
			// Set currentNode
			Node parentNode = CurrentNode.getCurrentNode(request);
			CurrentNode.setCurrentNode(request, element);
			try {
				// Find the optional parent page
				Page currentPage = CurrentPage.getCurrentPage(request);
				if(currentPage != null) currentPage.addElement(element);

				Long elementKey;
				if(parentNode != null) elementKey = parentNode.addChildElement(element, this);
				else elementKey = null;
				// Freeze element once body done
				try {
					doBody(captureLevel);
				} finally {
					// Note: Page freezes all of its elements after setting missing ids
					if(currentPage == null || element.getId() != null) {
						element.freeze();
					}
				}
				JspWriter out = pageContext.getOut();
				if(elementKey == null) {
					// Write now
					writeTo(out, new PageElementContext(pageContext));
				} else {
					// Write an element marker instead
					// TODO: Do not write element marker for empty elements, such as passwordTable at http://localhost:8080/docs/ao/infrastructure/ao/regions/mobile-al/workstations/francis.aoindustries.com/
					NodeBodyWriter.writeElementMarker(elementKey, out);
				}
			} finally {
				// Restore previous currentNode
				CurrentNode.setCurrentNode(request, parentNode);
			}
		}
	}

	/**
	 * Only called at capture level of META and higher.
	 */
	protected void doBody(CaptureLevel captureLevel) throws JspException, IOException {
		JspFragment body = getJspBody();
		if(body != null) {
			if(captureLevel == CaptureLevel.BODY) {
				// Invoke tag body, capturing output
				BufferWriter capturedOut = new SegmentedWriter();
				try {
					final PageContext pageContext = (PageContext)getJspContext();
					// Enable temp files if temp file context active
					capturedOut = TempFileContext.wrapTempFileList(
						capturedOut,
						pageContext.getRequest(),
						// Java 1.8: AutoTempFileWriter::new
						new TempFileContext.Wrapper<BufferWriter>() {
							@Override
							public BufferWriter call(BufferWriter original, TempFileList tempFileList) {
								return new AutoTempFileWriter(original, tempFileList);
							}
						}
					);
					body.invoke(capturedOut);
				} finally {
					capturedOut.close();
				}
				element.setBody(capturedOut.getResult().trim());
			} else if(captureLevel == CaptureLevel.META) {
				// Invoke body for any meta data, but discard any output
				body.invoke(NullWriter.getInstance());
			} else {
				throw new AssertionError();
			}
		}
	}
}
