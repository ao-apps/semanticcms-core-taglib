/*
 * semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2013, 2014, 2015, 2016, 2017, 2019  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of semanticcms-core-taglib.
 *
 * semanticcms-core-taglib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * semanticcms-core-taglib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with semanticcms-core-taglib.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.semanticcms.core.taglib;

import com.aoindustries.io.NullWriter;
import com.aoindustries.io.buffer.BufferWriter;
import com.aoindustries.servlet.jsp.LocalizedJspTagException;
import static com.aoindustries.taglib.AttributeUtils.resolveValue;
import com.aoindustries.taglib.AutoEncodingBufferedTag;
import static com.aoindustries.util.StringUtility.nullIfEmpty;
import com.semanticcms.core.model.Element;
import com.semanticcms.core.model.ElementWriter;
import com.semanticcms.core.model.Node;
import com.semanticcms.core.model.NodeBodyWriter;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.servlet.CaptureLevel;
import com.semanticcms.core.servlet.CurrentNode;
import com.semanticcms.core.servlet.CurrentPage;
import static com.semanticcms.core.taglib.PageTag.PROPERTY_ATTRIBUTE_PREFIX;
import java.io.IOException;
import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.DynamicAttributes;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.SimpleTagSupport;

/**
 * The base tag for capturing elements.
 */
abstract public class ElementTag<E extends Element> extends SimpleTagSupport implements DynamicAttributes, ElementWriter {

	/**
	 * Set during the beginning of doTag, but only for captureLevel >= META.
	 * This is not available while tag attributes are set.
	 */
	private E element;

	private ValueExpression id;
	public void setId(ValueExpression id) {
		this.id = id;
	}

	@Override
	public void setDynamicAttribute(String uri, String localName, Object value) throws JspTagException {
		if(
			uri == null
			&& localName.startsWith(PROPERTY_ATTRIBUTE_PREFIX)
		) {
			if(value != null) {
				String propertyName = localName.substring(PROPERTY_ATTRIBUTE_PREFIX.length());
				if(!element.setProperty(propertyName, value)) {
					throw new LocalizedJspTagException(
						ApplicationResources.accessor,
						"error.duplicateDynamicElementProperty",
						localName
					);
				}
			}
		} else {
			throw new LocalizedJspTagException(
				com.aoindustries.taglib.ApplicationResources.accessor,
				"error.unexpectedDynamicAttribute",
				localName,
				PROPERTY_ATTRIBUTE_PREFIX + "*"
			);
		}
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
			E elem = createElement();
			element = elem;
			evaluateAttributes(elem, pageContext.getELContext());

			// Set currentNode
			Node parentNode = CurrentNode.getCurrentNode(request);
			CurrentNode.setCurrentNode(request, elem);
			try {
				// Find the optional parent page
				Page currentPage = CurrentPage.getCurrentPage(request);
				if(currentPage != null) currentPage.addElement(elem);

				Long elementKey;
				if(parentNode != null) elementKey = parentNode.addChildElement(elem, this);
				else elementKey = null;
				// Freeze element once body done
				try {
					doBody(elem, captureLevel);
				} finally {
					// Note: Page freezes all of its elements
					if(currentPage == null) elem.freeze();
				}
				// Write now
				if(captureLevel == CaptureLevel.BODY) {
					JspWriter out = pageContext.getOut();
					if(elementKey == null) {
						try {
							writeTo(out, new PageElementContext(pageContext));
						} catch(JspException | IOException | RuntimeException e) {
							throw e;
						} catch(Exception e) {
							throw new JspTagException(e);
						}
					} else {
						// Write an element marker instead
						// TODO: Do not write element marker for empty elements, such as passwordTable at http://localhost:8080/docs/ao/infrastructure/ao/regions/mobile-al/workstations/francis.aoindustries.com/
						NodeBodyWriter.writeElementMarker(elementKey, out);
					}
				}
			} finally {
				// Restore previous currentNode
				CurrentNode.setCurrentNode(request, parentNode);
			}
		}
	}

	/**
	 * Called to create the element from doTag.
	 * This is only called for captureLevel >= META.
	 */
	abstract protected E createElement();

	/**
	 * Gets the element, only available after created.
	 *
	 * @see  #createElement()
	 *
	 * @throws  IllegalStateException  if element not yet created
	 */
	protected E getElement() throws IllegalStateException {
		if(element == null) throw new IllegalStateException();
		return element;
	}

	/**
	 * Resolves all attributes, setting into the created element as appropriate,
	 * This is only called for captureLevel >= META.
	 * Attributes are resolved before the element is added to any parent node.
	 * Typically, deferred expressions will be evaluated here.
	 * Overriding methods must call this implementation.
	 */
	protected void evaluateAttributes(E element, ELContext elContext) throws JspTagException, IOException {
		String idStr = nullIfEmpty(resolveValue(id, String.class, elContext));
		if(idStr != null) element.setId(idStr);
	}

	/**
	 * This is only called for captureLevel >= META.
	 */
	protected void doBody(E element, CaptureLevel captureLevel) throws JspException, IOException {
		JspFragment body = getJspBody();
		if(body != null) {
			if(captureLevel == CaptureLevel.BODY) {
				final PageContext pageContext = (PageContext)getJspContext();
				// Invoke tag body, capturing output
				BufferWriter capturedOut = AutoEncodingBufferedTag.newBufferWriter(pageContext.getRequest());
				try {
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
