/*
 * semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2016, 2017, 2020  AO Industries, Inc.
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

import com.semanticcms.core.model.Copyright;
import com.semanticcms.core.model.Node;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.pages.local.CurrentNode;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class CopyrightTag extends SimpleTagSupport {

	public static final String TAG_NAME = "<core:copyright>";

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
		if(!(currentNode instanceof Page)) throw new JspTagException(TAG_NAME + " tag must be nested directly inside a " + PageTag.TAG_NAME + " tag.");
		final Page currentPage = (Page)currentNode;

		currentPage.setCopyright(new Copyright(rightsHolder, rights, dateCopyrighted));
	}
}
