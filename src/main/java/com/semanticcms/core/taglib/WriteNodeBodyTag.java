/*
 * semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2015, 2016, 2017, 2021, 2022  AO Industries, Inc.
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

import static com.aoapps.taglib.AttributeUtils.resolveValue;
import com.semanticcms.core.model.Node;
import com.semanticcms.core.model.NodeBodyWriter;
import com.semanticcms.core.pages.CaptureLevel;
import com.semanticcms.core.pages.local.CurrentCaptureLevel;
import java.io.IOException;
import javax.el.ValueExpression;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class WriteNodeBodyTag extends SimpleTagSupport {

	private ValueExpression node;
	public void setNode(ValueExpression node) {
		this.node = node;
	}

	@Override
	public void doTag() throws JspException, IOException {
		PageContext pageContext = (PageContext)getJspContext();
		// Get the current capture state
		final CaptureLevel captureLevel = CurrentCaptureLevel.getCaptureLevel(pageContext.getRequest());
		if(captureLevel == CaptureLevel.BODY) {
			// Evaluate expressions
			Node nodeObj = resolveValue(node, Node.class, pageContext.getELContext());
			// Buffering made it slower, only about half throughput:
			// BufferedWriter out = new BufferedWriter(pageContext.getOut());
			nodeObj.getBody().writeTo(
				new NodeBodyWriter(
					nodeObj,
					pageContext.getOut(),
					new PageElementContext(pageContext)
				)
			);
			//out.flush();
		}
	}
}
