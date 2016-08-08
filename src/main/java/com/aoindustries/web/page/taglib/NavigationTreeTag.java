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

import com.aoindustries.web.page.Page;
import com.aoindustries.web.page.servlet.impl.NavigationTreeImpl;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class NavigationTreeTag extends SimpleTagSupport {

	private Page root;
	public void setRoot(Page root) {
		this.root = root;
	}

	private boolean skipRoot;
	public void setSkipRoot(boolean skipRoot) {
		this.skipRoot = skipRoot;
	}

	private boolean yuiConfig;
	public void setYuiConfig(boolean yuiConfig) {
		this.yuiConfig = yuiConfig;
	}

	private boolean includeElements;
	public void setIncludeElements(boolean includeElements) {
		this.includeElements = includeElements;
	}

	private String target;
	public void setTarget(String target) {
		this.target = target;
	}

	private String thisBook;
	public void setThisBook(String thisBook) {
		this.thisBook = thisBook;
	}

	private String thisPage;
	public void setThisPage(String thisPage) {
		this.thisPage = thisPage;
	}

	private String linksToBook;
	public void setLinksToBook(String linksToBook) {
		this.linksToBook = linksToBook;
	}

	private String linksToPage;
	public void setLinksToPage(String linksToPage) {
		this.linksToPage = linksToPage;
	}

	private int maxDepth;
	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	/**
	 * Creates the nested &lt;ul&gt; and &lt;li&gt; tags used by TreeView.
	 * The first level is expanded.
	 *
	 * {@link http://developer.yahoo.com/yui/treeview/#start}
	 */
	@Override
	public void doTag() throws JspTagException, IOException {
		try {
			final PageContext pageContext = (PageContext)getJspContext();
			NavigationTreeImpl.writeNavigationTreeImpl(
				pageContext.getServletContext(),
				(HttpServletRequest)pageContext.getRequest(),
				(HttpServletResponse)pageContext.getResponse(),
				pageContext.getOut(),
				root,
				skipRoot,
				yuiConfig,
				includeElements,
				target,
				thisBook,
				thisPage,
				linksToBook,
				linksToPage,
				maxDepth
			);
		} catch(ServletException e) {
			throw new JspTagException(e);
		}
	}
}
