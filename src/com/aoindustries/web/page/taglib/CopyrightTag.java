/*
 * ao-web-page-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2016  AO Industries, Inc.
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

import com.aoindustries.web.page.Copyright;
import com.aoindustries.web.page.Node;
import com.aoindustries.web.page.Page;
import com.aoindustries.web.page.servlet.CurrentNode;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class CopyrightTag extends SimpleTagSupport {

	private String rightsHolder;
	public void setRightsHolder(String rightsHolder) {
		this.rightsHolder = rightsHolder;
    }

	private String rights;
	public void setRights(String rights) {
		this.rights = rights;
    }

	private String dateCopyrighted;
	public void setDateCopyrighted(String dateCopyrighted) {
		this.dateCopyrighted = dateCopyrighted;
    }

	@Override
    public void doTag() throws JspTagException, IOException {
		final PageContext pageContext = (PageContext)getJspContext();
		final HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();

		final Node currentNode = CurrentNode.getCurrentNode(request);
		if(!(currentNode instanceof Page)) throw new JspTagException("<p:copyright> tag must be nested directly inside a <p:page> tag.");
		final Page currentPage = (Page)currentNode;

		currentPage.setCopyright(new Copyright(rightsHolder, rights, dateCopyrighted));
	}
}
