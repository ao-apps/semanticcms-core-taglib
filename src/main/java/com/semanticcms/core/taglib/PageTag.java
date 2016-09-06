/*
 * semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2013, 2014, 2015, 2016  AO Industries, Inc.
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

import com.aoindustries.io.NullWriter;
import com.aoindustries.io.TempFileList;
import com.aoindustries.io.buffer.AutoTempFileWriter;
import com.aoindustries.io.buffer.BufferResult;
import com.aoindustries.io.buffer.BufferWriter;
import com.aoindustries.io.buffer.EmptyResult;
import com.aoindustries.io.buffer.SegmentedWriter;
import com.aoindustries.servlet.filter.TempFileContext;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.servlet.impl.PageImpl;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.SkipPageException;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class PageTag extends SimpleTagSupport {

	private String title;
	public void setTitle(String title) {
		this.title = title;
	}

	private String shortTitle;
	public void setShortTitle(String shortTitle) {
		this.shortTitle = shortTitle;
	}

	private String description;
	public void setDescription(String description) {
		this.description = description;
	}

	private String keywords;
	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	private Boolean allowRobots;
	public void setAllowRobots(String allowRobots) {
		// Not using Boolean.valueOf to be more specific in parsing, "blarg" is not same as "false".
		if("auto".equalsIgnoreCase(allowRobots)) {
			this.allowRobots = null;
		} else if("true".equalsIgnoreCase(allowRobots)) {
			this.allowRobots = true;
		} else if("false".equalsIgnoreCase(allowRobots)) {
			this.allowRobots = false;
		} else {
			throw new IllegalArgumentException("Unexpected value for allowRobots, expect one of \"auto\", \"true\", or \"false\": " + allowRobots);
		}
	}

	private Boolean toc;
	public void setToc(String toc) {
		// Not using Boolean.valueOf to be more specific in parsing, "blarg" is not same as "false".
		if("auto".equalsIgnoreCase(toc)) {
			this.toc = null;
		} else if("true".equalsIgnoreCase(toc)) {
			this.toc = true;
		} else if("false".equalsIgnoreCase(toc)) {
			this.toc = false;
		} else {
			throw new IllegalArgumentException("Unexpected value for toc, expect one of \"auto\", \"true\", or \"false\": " + toc);
		}
	}

	private int tocLevels = Page.DEFAULT_TOC_LEVELS;
	public void setTocLevels(int tocLevels) {
		this.tocLevels = tocLevels;
	}

	private boolean allowParentMismatch;
	public void setAllowParentMismatch(boolean allowParentMismatch) {
		this.allowParentMismatch = allowParentMismatch;
	}

	private boolean allowChildMismatch;
	public void setAllowChildMismatch(boolean allowChildMismatch) {
		this.allowChildMismatch = allowChildMismatch;
	}

	@Override
	public void doTag() throws JspException, IOException {
		try {
			final PageContext pageContext = (PageContext)getJspContext();
			final HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();

			final JspFragment body = getJspBody();
			PageImpl.doPageImpl(pageContext.getServletContext(),
				request,
				(HttpServletResponse)pageContext.getResponse(),
				title,
				shortTitle,
				description,
				keywords,
				allowRobots,
				toc,
				tocLevels,
				allowParentMismatch,
				allowChildMismatch,
				body == null
					? null
					: new PageImpl.PageImplBody<JspException>() {
						@Override
						public BufferResult doBody(boolean discard, Page page) throws JspException, IOException, SkipPageException {
							// JSP pages are their own source
							page.setSrc(page.getPageRef());
							if(discard) {
								body.invoke(NullWriter.getInstance());
								return EmptyResult.getInstance();
							} else {
								// Enable temp files if temp file context active
								BufferWriter capturedOut = TempFileContext.wrapTempFileList(
									new SegmentedWriter(),
									request,
									// Java 1.8: AutoTempFileWriter::new
									new TempFileContext.Wrapper<BufferWriter>() {
										@Override
										public BufferWriter call(BufferWriter original, TempFileList tempFileList) {
											return new AutoTempFileWriter(original, tempFileList);
										}
									}
								);
								try {
									body.invoke(capturedOut);
								} finally {
									capturedOut.close();
								}
								return capturedOut.getResult();
							}
						}
					}
			);
		} catch(ServletException e) {
			throw new JspTagException(e);
		}
	}
}
