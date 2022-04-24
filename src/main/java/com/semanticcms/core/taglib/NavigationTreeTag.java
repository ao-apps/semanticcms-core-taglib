/*
 * semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2013, 2014, 2015, 2016, 2017, 2018, 2020, 2021, 2022  AO Industries, Inc.
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
import com.semanticcms.core.renderer.html.NavigationTreeRenderer;
import java.io.IOException;
import javax.el.ValueExpression;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class NavigationTreeTag extends SimpleTagSupport {

  private ValueExpression root;

  public void setRoot(ValueExpression root) {
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

  private ValueExpression thisDomain;

  public void setThisDomain(ValueExpression thisDomain) {
    this.thisDomain = thisDomain;
  }

  private ValueExpression thisBook;

  public void setThisBook(ValueExpression thisBook) {
    this.thisBook = thisBook;
  }

  private ValueExpression thisPage;

  public void setThisPage(ValueExpression thisPage) {
    this.thisPage = thisPage;
  }

  private ValueExpression linksToDomain;

  public void setLinksToDomain(ValueExpression linksToDomain) {
    this.linksToDomain = linksToDomain;
  }

  private ValueExpression linksToBook;

  public void setLinksToBook(ValueExpression linksToBook) {
    this.linksToBook = linksToBook;
  }

  private ValueExpression linksToPage;

  public void setLinksToPage(ValueExpression linksToPage) {
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
   * <a href="http://developer.yahoo.com/yui/treeview/#start">http://developer.yahoo.com/yui/treeview/#start</a>
   */
  @Override
  public void doTag() throws JspException, IOException {
    try {
      final PageContext pageContext = (PageContext) getJspContext();
      ServletContext servletContext = pageContext.getServletContext();
      HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
      HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();
      NavigationTreeRenderer.writeNavigationTree(servletContext,
          pageContext.getELContext(),
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
          root,
          skipRoot,
          yuiConfig,
          includeElements,
          target,
          thisDomain,
          thisBook,
          thisPage,
          linksToDomain,
          linksToBook,
          linksToPage,
          maxDepth
      );
    } catch (ServletException e) {
      throw new JspTagException(e);
    }
  }
}
