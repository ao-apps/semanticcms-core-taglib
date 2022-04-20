/*
 * semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2016, 2017, 2020, 2021, 2022  AO Industries, Inc.
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

import com.aoapps.lang.Strings;
import com.aoapps.lang.validation.ValidationException;
import com.aoapps.net.DomainName;
import com.aoapps.net.Path;
import com.semanticcms.core.model.Author;
import com.semanticcms.core.model.Node;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.model.PageRef;
import com.semanticcms.core.pages.local.CurrentNode;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class AuthorTag extends SimpleTagSupport {

  public static final String TAG_NAME = "<core:author>";

  private String name;
  public void setName(String name) {
    this.name = Strings.nullIfEmpty(name);
  }

  private String href;
  public void setHref(String href) {
    this.href = Strings.nullIfEmpty(href);
  }

  private DomainName domain;
  public void setDomain(String domain) throws ValidationException {
    this.domain = DomainName.valueOf(Strings.nullIfEmpty(domain));
  }

  private Path book;
  public void setBook(String book) throws ValidationException {
    this.book = Path.valueOf(Strings.nullIfEmpty(book));
  }

  private Path page;
  public void setPage(String page) throws ValidationException {
    this.page = Path.valueOf(Strings.nullIfEmpty(page));
  }

  @Override
  public void doTag() throws JspException, IOException {
    final PageContext pageContext = (PageContext)getJspContext();
    final HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();

    final Node currentNode = CurrentNode.getCurrentNode(request);
    if (!(currentNode instanceof Page)) {
      throw new JspTagException(TAG_NAME + " tag must be nested directly inside a " + PageTag.TAG_NAME + " tag.");
    }
    final Page currentPage = (Page)currentNode;

    PageRef currentPageRef = null;

    // When domain provided, both book and page attributes must also be provided.
    if (domain != null) {
      if (book == null) {
        throw new JspTagException("When domain provided, both book and page attributes must also be provided.");
      }
    }
    // When book provided, page attribute must also be provided.
    if (book != null) {
      if (page == null) {
        throw new JspTagException("When book provided, page attribute must also be provided.");
      }
    }
    if (page != null) {
      // Default to this domain if nothing set
      if (domain == null) {
        currentPageRef = currentPage.getPageRef();
        domain = currentPageRef.getBookRef().getDomain();
      }
      // Default to this book if nothing set
      if (book == null) {
        if (currentPageRef == null) {
          currentPageRef = currentPage.getPageRef();
        }
        book = currentPageRef.getBookRef().getPath();
      }
    }
    // Name required when referencing an author outside this book
    if (name == null && book != null) {
      if (currentPageRef == null) {
        currentPageRef = currentPage.getPageRef();
      }
      assert domain != null;
      if (
        !domain.equals(currentPageRef.getBookRef().getDomain())
        || !book.equals(currentPageRef.getBookRef().getPath())
      ) {
        throw new IllegalStateException("Author name required when author is in a different book: " + page);
      }
    }
    currentPage.addAuthor(
      new Author(name, href, domain, book, page)
    );
  }
}
