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
import com.semanticcms.core.model.Node;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.model.ParentRef;
import com.semanticcms.core.servlet.CurrentNode;
import com.semanticcms.core.servlet.PageRefResolver;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class ParentTag extends SimpleTagSupport {

  public static final String TAG_NAME = "<core:parent>";

  private String book;

  public void setBook(String book) {
    this.book = Strings.nullIfEmpty(book);
  }

  private String page;

  public void setPage(String page) {
    this.page = page;
  }

  private String shortTitle;

  public void setShortTitle(String shortTitle) {
    this.shortTitle = Strings.nullIfEmpty(shortTitle);
  }

  @Override
  public void doTag() throws JspException, IOException {
    final PageContext pageContext = (PageContext) getJspContext();
    final HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

    final Node currentNode = CurrentNode.getCurrentNode(request);
    if (!(currentNode instanceof Page)) {
      throw new JspTagException(TAG_NAME + " tag must be nested directly inside a " + PageTag.TAG_NAME + " tag.");
    }
    final Page currentPage = (Page) currentNode;

    try {
      currentPage.addParentRef(
          new ParentRef(
              PageRefResolver.getPageRef(
                  pageContext.getServletContext(),
                  request,
                  book,
                  page
              ),
              shortTitle
          )
      );
    } catch (ServletException e) {
      throw new JspTagException(e);
    }
  }
}
