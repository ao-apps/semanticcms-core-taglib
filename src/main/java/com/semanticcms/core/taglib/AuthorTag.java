/*
 * semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2016, 2017  AO Industries, Inc.
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

import com.semanticcms.core.model.Author;
import com.semanticcms.core.model.Node;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.model.PageRef;
import com.semanticcms.core.servlet.CurrentNode;
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

	private String domain;
	public void setDomain(String domain) {
		this.domain = domain;
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
		if(!(currentNode instanceof Page)) throw new JspTagException("<core:author> tag must be nested directly inside a <core:page> tag.");
		final Page currentPage = (Page)currentNode;

		PageRef currentPageRef = null;

		String d = domain;
		String bookName = book;
		// When domain provided, both book and page attributes must also be provided.
		if(d != null) {
			if(bookName == null) throw new JspTagException("When domain provided, both book and page attributes must also be provided.");
		}
		// When book provided, page attribute must also be provided.
		if(bookName != null) {
			if(page == null) throw new JspTagException("When book provided, page attribute must also be provided.");
		}
		if(page != null) {
			// Default to this domain if nothing set
			if(d == null) {
				currentPageRef = currentPage.getPageRef();
				d = currentPageRef.getBookRef().getDomain();
			}
			// Default to this book if nothing set
			if(bookName == null) {
				if(currentPageRef == null) currentPageRef = currentPage.getPageRef();
				bookName = currentPageRef.getBookRef().getName();
			}
		}
		// Name required when referencing an author outside this book
		if(name == null && bookName != null) {
			if(currentPageRef == null) currentPageRef = currentPage.getPageRef();
			assert d != null;
			if(
				!d.equals(currentPageRef.getBookRef().getDomain())
				|| !bookName.equals(currentPageRef.getBookRef().getName())
			) {
				throw new IllegalStateException("Author name required when author is in a different book: " + page);
			}
		}
		currentPage.addAuthor(
			new Author(name, href, d, bookName, page)
		);
	}
}
