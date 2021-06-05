/*
 * semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2016, 2017, 2020, 2021  AO Industries, Inc.
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

import com.aoapps.html.servlet.DocumentEE;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.renderer.html.ComponentPosition;
import com.semanticcms.core.renderer.html.ComponentUtils;
import com.semanticcms.core.renderer.html.View;
import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class DoComponentsTag extends SimpleTagSupport {

	private View view;
	public void setView(View view) {
		this.view = view;
	}

	private Page page;
	public void setPage(Page page) {
		this.page = page;
	}

	private ComponentPosition position;
	public void setPosition(ComponentPosition position) {
		this.position = position;
	}

	private boolean reverse;
	public void setReverse(boolean reverse) {
		this.reverse = reverse;
	}

	@Override
	public void doTag() throws JspException, IOException {
		try {
			final PageContext pageContext = (PageContext)getJspContext();
			ServletContext servletContext = pageContext.getServletContext();
			HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
			HttpServletResponse response = (HttpServletResponse)pageContext.getResponse();
			ComponentUtils.doComponents(servletContext,
				request,
				response,
				new DocumentEE(
					servletContext,
					request,
					response,
					pageContext.getOut(),
					false, // Do not add extra newlines to JSP
					false  // Do not add extra indentation to JSP
				),
				view,
				page,
				position,
				reverse
			);
		} catch(ServletException e) {
			throw new JspTagException(e);
		}
	}
}
