/*
 * semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2013, 2014, 2015, 2016  AO Industries, Inc.
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
import com.aoindustries.io.TempFileList;
import com.aoindustries.io.buffer.AutoTempFileWriter;
import com.aoindustries.io.buffer.BufferResult;
import com.aoindustries.io.buffer.BufferWriter;
import com.aoindustries.io.buffer.SegmentedWriter;
import com.aoindustries.net.EmptyParameters;
import com.aoindustries.net.HttpParameters;
import com.aoindustries.net.HttpParametersMap;
import com.aoindustries.net.MutableHttpParameters;
import com.aoindustries.servlet.filter.TempFileContext;
import com.aoindustries.servlet.jsp.LocalizedJspTagException;
import static com.aoindustries.taglib.ApplicationResources.accessor;
import com.aoindustries.taglib.ParamUtils;
import com.aoindustries.taglib.ParamsAttribute;
import com.semanticcms.core.servlet.CaptureLevel;
import com.semanticcms.core.servlet.impl.LinkImpl;
import java.io.IOException;
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
		ParamsAttribute
{

    private MutableHttpParameters params;
	private String clazz;
	private String book;
	private String page;
	private String element;
	private boolean allowGeneratedElement;
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

	public void setAllowGeneratedElement(boolean allowGeneratedElement) {
		this.allowGeneratedElement = allowGeneratedElement;
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
						BufferWriter captureOut = new SegmentedWriter();
						try {
							// Enable temp files if temp file context active
							captureOut = TempFileContext.wrapTempFileList(
								captureOut,
								request,
								// Java 1.8: AutoTempFileWriter::new
								new TempFileContext.Wrapper<BufferWriter>() {
									@Override
									public BufferWriter call(BufferWriter original, TempFileList tempFileList) {
										return new AutoTempFileWriter(original, tempFileList);
									}
								}
							);
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
				LinkImpl.writeLinkImpl(
					pageContext.getServletContext(),
					request,
					(HttpServletResponse)pageContext.getResponse(),
					out,
					book,
					page,
					element,
					allowGeneratedElement,
					view,
					params,
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
