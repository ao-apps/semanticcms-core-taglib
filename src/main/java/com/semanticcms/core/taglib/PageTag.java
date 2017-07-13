/*
 * semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2013, 2014, 2015, 2016, 2017  AO Industries, Inc.
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
import com.aoindustries.servlet.filter.TempFileContext;
import com.aoindustries.servlet.jsp.LocalizedJspTagException;
import com.aoindustries.taglib.AutoEncodingBufferedTag;
import static com.aoindustries.util.StringUtility.nullIfEmpty;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.model.PageRef;
import com.semanticcms.core.servlet.PageRefResolver;
import com.semanticcms.core.servlet.PageUtils;
import com.semanticcms.core.servlet.impl.PageImpl;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.SkipPageException;
import javax.servlet.jsp.tagext.DynamicAttributes;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.SimpleTagSupport;

/**
 * TODO: Load page properties from a properties file with the same name as the jsp file, minus
 * ".jsp" or ".jspx", but with ".properties" added.  Also block all "*.properties" from direct access
 */
public class PageTag extends SimpleTagSupport implements DynamicAttributes {

	/**
	 * The prefix for property attributes.
	 */
	public static final String PROPERTY_ATTRIBUTE_PREFIX = "property.";

	private String book;
	public void setBook(String book) {
		this.book = nullIfEmpty(book);
	}

	private String path;
	public void setPath(String path) {
		this.path = nullIfEmpty(path);
	}

	private Object dateCreated;
	public void setDateCreated(Object dateCreated) {
		this.dateCreated = dateCreated;
	}

	private Object datePublished;
	public void setDatePublished(Object datePublished) {
		this.datePublished = datePublished;
	}

	private Object dateModified;
	public void setDateModified(Object dateModified) {
		this.dateModified = dateModified;
	}

	private Object dateReviewed;
	public void setDateReviewed(Object dateReviewed) {
		this.dateReviewed = dateReviewed;
	}

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

	private Map<String,Object> properties;

	@Override
	public void setDynamicAttribute(String uri, String localName, Object value) throws JspTagException {
		if(
			uri == null
			&& localName.startsWith(PROPERTY_ATTRIBUTE_PREFIX)
		) {
			if(value != null) {
				String propertyName = localName.substring(PROPERTY_ATTRIBUTE_PREFIX.length());
				if(properties == null) {
					properties = new LinkedHashMap<String,Object>();
				} else if(properties.containsKey(propertyName)) {
					throw new LocalizedJspTagException(
						ApplicationResources.accessor,
						"error.duplicateDynamicPageProperty",
						localName
					);
				}
				properties.put(propertyName, value);
			}
		} else {
			throw new LocalizedJspTagException(
				com.aoindustries.taglib.ApplicationResources.accessor,
				"error.unexpectedDynamicAttribute",
				localName,
				PROPERTY_ATTRIBUTE_PREFIX + "*"
			);
		}
	}

	@Override
	public void doTag() throws JspException, IOException {
		try {
			PageContext pageContext = (PageContext)getJspContext();
			ServletContext servletContext = pageContext.getServletContext();
			final HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();

			// Resolve pageRef, if book or path set
			final PageRef pageRef;
			if(path == null) {
				if(book != null) throw new ServletException("path must be provided when book is provided.");
				pageRef = null; // Use default
			} else {
				pageRef = PageRefResolver.getPageRef(servletContext, request, book, path);
			}

			// TODO: Load properties from *.properties file, too

			final JspFragment body = getJspBody();
			PageImpl.doPageImpl(
				servletContext,
				request,
				(HttpServletResponse)pageContext.getResponse(),
				pageRef,
				PageUtils.toDateTime(dateCreated),
				PageUtils.toDateTime(datePublished),
				PageUtils.toDateTime(dateModified),
				PageUtils.toDateTime(dateReviewed),
				title,
				shortTitle,
				description,
				keywords,
				allowRobots,
				toc,
				tocLevels,
				allowParentMismatch,
				allowChildMismatch,
				properties,
				body == null
					? null
					: new PageImpl.PageImplBody<JspException>() {
						@Override
						public BufferResult doBody(boolean discard, Page page) throws JspException, IOException, SkipPageException {
							// JSP pages are their own source when using default pageRef
							if(pageRef == null) page.setSrc(page.getPageRef());
							if(discard) {
								body.invoke(NullWriter.getInstance());
								return EmptyResult.getInstance();
							} else {
								// Enable temp files if temp file context active
								BufferWriter capturedOut = TempFileContext.wrapTempFileList(
									AutoEncodingBufferedTag.newBufferWriter(),
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
