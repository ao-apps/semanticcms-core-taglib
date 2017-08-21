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

import com.aoindustries.net.Path;
import com.aoindustries.util.StringUtility;
import com.aoindustries.validation.ValidationException;
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
		this.name = StringUtility.nullIfEmpty(name);
	}

	private String href;
	public void setHref(String href) {
		this.href = StringUtility.nullIfEmpty(href);
	}

	private String domain;
	public void setDomain(String domain) {
		this.domain = StringUtility.nullIfEmpty(domain);
	}

	private String book;
	public void setBook(String book) {
		this.book = StringUtility.nullIfEmpty(book);
	}

	private String page;
	public void setPage(String page) {
		this.page = StringUtility.nullIfEmpty(page);
	}

	@Override
	public void doTag() throws JspTagException, IOException {
		try {
			final PageContext pageContext = (PageContext)getJspContext();
			final HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();

			final Node currentNode = CurrentNode.getCurrentNode(request);
			if(!(currentNode instanceof Page)) throw new JspTagException("<core:author> tag must be nested directly inside a <core:page> tag.");
			final Page currentPage = (Page)currentNode;

			PageRef currentPageRef = null;

			String d = domain;
			Path bookPath = Path.valueOf(book);
			Path pagePath = Path.valueOf(page);
			// When domain provided, both book and page attributes must also be provided.
			if(d != null) {
				if(bookPath == null) throw new JspTagException("When domain provided, both book and page attributes must also be provided.");
			}
			// When book provided, page attribute must also be provided.
			if(bookPath != null) {
				if(pagePath == null) throw new JspTagException("When book provided, page attribute must also be provided.");
			}
			if(pagePath != null) {
				// Default to this domain if nothing set
				if(d == null) {
					currentPageRef = currentPage.getPageRef();
					d = currentPageRef.getBookRef().getDomain();
				}
				// Default to this book if nothing set
				if(bookPath == null) {
					if(currentPageRef == null) currentPageRef = currentPage.getPageRef();
					bookPath = currentPageRef.getBookRef().getPath();
				}
			}
			// Name required when referencing an author outside this book
			if(name == null && bookPath != null) {
				if(currentPageRef == null) currentPageRef = currentPage.getPageRef();
				assert d != null;
				if(
					!d.equals(currentPageRef.getBookRef().getDomain())
					|| !bookPath.equals(currentPageRef.getBookRef().getPath())
				) {
					throw new IllegalStateException("Author name required when author is in a different book: " + pagePath);
				}
			}
			currentPage.addAuthor(
				new Author(name, href, d, bookPath, pagePath)
			);
		} catch(ValidationException e) {
			throw new JspTagException(e);
		}
	}
}
