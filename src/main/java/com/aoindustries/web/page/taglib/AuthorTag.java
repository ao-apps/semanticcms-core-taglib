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

import com.aoindustries.web.page.Author;
import com.aoindustries.web.page.Node;
import com.aoindustries.web.page.Page;
import com.aoindustries.web.page.servlet.CurrentNode;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class AuthorTag extends SimpleTagSupport {

	private String name;
	public void setName(String name) {
		this.name = name;
    }

	private String href;
	public void setHref(String href) {
		this.href = href;
    }

	private String book;
	public void setBook(String book) {
		this.book = book;
    }

	private String page;
	public void setPage(String page) {
		this.page = page;
    }

	@Override
    public void doTag() throws JspTagException, IOException {
		final PageContext pageContext = (PageContext)getJspContext();
		final HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();

		final Node currentNode = CurrentNode.getCurrentNode(request);
		if(!(currentNode instanceof Page)) throw new JspTagException("<p:author> tag must be nested directly inside a <p:page> tag.");
		final Page currentPage = (Page)currentNode;

		String bookName = book;
		// Default to this book if nothing set
		if(page != null && bookName == null) bookName = currentPage.getPageRef().getBookName();
		// Name required when referencing an author outside this book
		if(
			name == null
			&& bookName != null
			&& !bookName.equals(currentPage.getPageRef().getBookName())
		) {
			throw new IllegalStateException("Author name required when author is in a different book: " + page);
		}
		currentPage.addAuthor(
			new Author(name, href, bookName, page)
		);
	}
}
