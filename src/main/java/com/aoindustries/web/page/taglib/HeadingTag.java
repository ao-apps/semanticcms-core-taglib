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

import com.aoindustries.web.page.servlet.CaptureLevel;
import com.aoindustries.web.page.servlet.PageIndex;
import com.aoindustries.web.page.servlet.impl.HeadingImpl;
import com.semanticcms.core.model.ElementContext;
import com.semanticcms.core.model.Heading;
import java.io.IOException;
import java.io.Writer;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

public class HeadingTag extends ElementTag<Heading> {

	public HeadingTag() {
		super(new Heading());
	}

    public void setLabel(String label) {
		element.setLabel(label);
	}

	private PageIndex pageIndex;
	@Override
	protected void doBody(CaptureLevel captureLevel) throws JspException, IOException {
		final PageContext pageContext = (PageContext)getJspContext();
		pageIndex = PageIndex.getCurrentPageIndex(pageContext.getRequest());
		super.doBody(captureLevel);
		HeadingImpl.doAfterBody(element);
	}

	@Override
	public void writeTo(Writer out, ElementContext context) throws IOException {
		HeadingImpl.writeHeading(out, context, element, pageIndex);
	}
}
