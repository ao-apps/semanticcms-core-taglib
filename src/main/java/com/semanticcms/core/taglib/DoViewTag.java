/*
 * semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2016, 2017, 2019, 2020, 2021  AO Industries, Inc.
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
 * along with semanticcms-core-taglib.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.semanticcms.core.taglib;

import com.aoapps.html.servlet.DocumentEE;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.servlet.View;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import org.apache.commons.lang3.NotImplementedException;

public class DoViewTag extends SimpleTagSupport {

	private View view;
	public void setView(View view) {
		this.view = view;
	}

	private Page page;
	public void setPage(Page page) {
		this.page = page;
	}

	@Override
	public void doTag() throws JspException, IOException {
		try {
			final PageContext pageContext = (PageContext)getJspContext();
			final PrintWriter out = new PrintWriter(pageContext.getOut()) {
				@Override
				public void flush() {
					// Avoid "Illegal to flush within a custom tag" from BodyContentImpl
				}
			};
			ServletContext servletContext = pageContext.getServletContext();
			HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
			HttpServletResponse response = new HttpServletResponseWrapper((HttpServletResponse)pageContext.getResponse()) {
				@Override
				public PrintWriter getWriter() {
					return out;
				}
				@Override
				public ServletOutputStream getOutputStream() {
					throw new NotImplementedException("getOutputStream not expected");
				}
			};
			view.doView(servletContext,
				request,
				response,
				new DocumentEE(
					servletContext,
					request,
					response,
					out,
					false, // Do not add extra newlines to JSP
					false  // Do not add extra indentation to JSP
				),
				page
			);
			if(out.checkError()) throw new IOException("Error on doView PrintWriter");
		} catch(ServletException e) {
			throw new JspTagException(e);
		}
	}
}
