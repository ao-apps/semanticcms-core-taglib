/*
 * semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2013, 2014, 2015, 2016, 2017, 2019, 2020, 2021  AO Industries, Inc.
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

import com.aoindustries.encoding.taglib.EncodingBufferedTag;
import com.aoindustries.html.servlet.DocumentEE;
import com.aoindustries.io.NullWriter;
import com.aoindustries.io.buffer.BufferResult;
import com.aoindustries.io.buffer.BufferWriter;
import com.aoindustries.net.MutableURIParameters;
import com.aoindustries.net.URIParametersMap;
import com.aoindustries.taglib.AttributeUtils;
import com.aoindustries.taglib.ParamUtils;
import com.aoindustries.taglib.ParamsAttribute;
import com.semanticcms.core.servlet.CaptureLevel;
import com.semanticcms.core.servlet.impl.LinkImpl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.el.ValueExpression;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.SkipPageException;
import javax.servlet.jsp.tagext.DynamicAttributes;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class LinkTag
	extends SimpleTagSupport
	implements
		DynamicAttributes,
		//ClassAttribute,
		ParamsAttribute
{

	private MutableURIParameters params;
	private ValueExpression clazz;
	private ValueExpression book;
	private ValueExpression page;
	private ValueExpression element;
	private boolean allowGeneratedElement;
	private ValueExpression anchor;
	private ValueExpression view;
	private boolean small;
	private boolean absolute;
	private boolean canonical;

	@Override
	public void addParam(String name, Object value) {
		if(params == null) params = new URIParametersMap();
		params.add(name, value);
	}

	public ValueExpression getClazz() {
		return clazz;
	}

	public void setClazz(ValueExpression clazz) {
		this.clazz = clazz;
	}

	public void setBook(ValueExpression book) {
		this.book = book;
	}

	public void setPage(ValueExpression page) {
		this.page = page;
	}

	public void setElement(ValueExpression element) {
		this.element = element;
	}

	public void setAllowGeneratedElement(boolean allowGeneratedElement) {
		this.allowGeneratedElement = allowGeneratedElement;
	}

	public void setAnchor(ValueExpression anchor) {
		this.anchor = anchor;
	}

	public void setView(ValueExpression view) {
		this.view = view;
	}

	public void setSmall(boolean small) {
		this.small = small;
	}

	public void setAbsolute(boolean absolute) {
		this.absolute = absolute;
	}

	public void setCanonical(boolean canonical) {
		this.canonical = canonical;
	}

	/**
	 * Adds a {@linkplain DynamicAttributes dynamic attribute}.
	 *
	 * @return  {@code true} when added, or {@code false} when attribute not expected and has not been added.
	 *
	 * @see  ParamUtils#addDynamicAttribute(java.lang.String, java.lang.String, java.lang.Object, java.util.List, com.aoindustries.taglib.ParamsAttribute)
	 * @see  #setDynamicAttribute(java.lang.String, java.lang.String, java.lang.Object)
	 */
	protected boolean addDynamicAttribute(String uri, String localName, Object value, List<String> expectedPatterns) throws JspTagException {
		return ParamUtils.addDynamicAttribute(uri, localName, value, expectedPatterns, this);
	}

	/**
	 * Sets a {@linkplain DynamicAttributes dynamic attribute}.
	 *
	 * @deprecated  You should probably be implementing in {@link #addDynamicAttribute(java.lang.String, java.lang.String, java.lang.Object, java.util.List)}
	 *
	 * @see  #addDynamicAttribute(java.lang.String, java.lang.String, java.lang.Object, java.util.List)
	 */
	@Deprecated
	@Override
	public void setDynamicAttribute(String uri, String localName, Object value) throws JspException {
		List<String> expectedPatterns = new ArrayList<>();
		if(!addDynamicAttribute(uri, localName, value, expectedPatterns)) {
			throw AttributeUtils.newDynamicAttributeFailedException(uri, localName, value, expectedPatterns);
		}
	}

	@Override
	@SuppressWarnings("TooBroadCatch") // Should not be necessary, NetBeans bug?
	public void doTag() throws JspException, IOException {
		try {
			final PageContext pageContext = (PageContext)getJspContext();
			final HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();

			// Get the current capture state
			final CaptureLevel captureLevel = CaptureLevel.getCaptureLevel(request);
			if(captureLevel.compareTo(CaptureLevel.META) >= 0) {
				// Capture the body first for any nested parameter tags
				final BufferResult capturedBody;
				if(captureLevel == CaptureLevel.BODY) {
					JspFragment body = getJspBody();
					if(body != null) {
						BufferWriter captureOut = EncodingBufferedTag.newBufferWriter(request);
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
				final JspWriter out = pageContext.getOut();
				ServletContext servletContext = pageContext.getServletContext();
				HttpServletResponse response = (HttpServletResponse)pageContext.getResponse();
				LinkImpl.writeLinkImpl(servletContext,
					pageContext.getELContext(),
					request,
					response,
					DocumentEE.get(
						servletContext,
						request,
						response,
						out,
						false, // Do not add extra newlines to JSP
						false  // Do not add extra indentation to JSP
					),
					book,
					page,
					element,
					allowGeneratedElement,
					anchor,
					view,
					small,
					params,
					absolute,
					canonical,
					clazz,
					capturedBody == null || capturedBody.getLength() == 0
						? null
						: new LinkImpl.LinkImplBody<JspException>() {
							@Override
							public void doBody(boolean discard) throws JspException, IOException, SkipPageException {
								if(discard) throw new AssertionError("Conditions that lead to discard should have caused no capturedBody above");
								capturedBody.writeTo(out);
							}
						}
				);
			}
		} catch(ServletException e) {
			throw new JspTagException(e);
		}
	}
}
