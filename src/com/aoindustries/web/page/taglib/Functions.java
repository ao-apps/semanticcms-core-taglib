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

import static com.aoindustries.servlet.filter.FunctionContext.getRequest;
import static com.aoindustries.servlet.filter.FunctionContext.getResponse;
import static com.aoindustries.servlet.filter.FunctionContext.getServletContext;
import com.aoindustries.servlet.http.ServletUtil;
import com.aoindustries.util.StringUtility;
import com.aoindustries.web.page.Book;
import com.aoindustries.web.page.Element;
import com.aoindustries.web.page.Heading;
import com.aoindustries.web.page.Page;
import com.aoindustries.web.page.PageRef;
import com.aoindustries.web.page.servlet.BooksContextListener;
import com.aoindustries.web.page.servlet.CaptureLevel;
import com.aoindustries.web.page.servlet.CapturePage;
import com.aoindustries.web.page.servlet.ContentRoot;
import com.aoindustries.web.page.servlet.PageDags;
import com.aoindustries.web.page.servlet.PageIndex;
import com.aoindustries.web.page.servlet.PageRefResolver;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspTagException;

final public class Functions {

	public static Page capturePageInBook(String book, String page, String level) throws ServletException, IOException {
		PageRef pageRef = PageRefResolver.getPageRef(
			getServletContext(),
			getRequest(),
			book,
			page
		);
		if(pageRef.getBook()==null) throw new IllegalArgumentException("Book not found: " + pageRef.getBookName());
		return CapturePage.capturePage(
			getServletContext(),
			getRequest(),
			getResponse(),
			pageRef,
			CaptureLevel.valueOf(level.toUpperCase(Locale.ENGLISH))
		);
	}

	public static Page capturePage(String page, String level) throws ServletException, IOException, JspTagException {
		return capturePageInBook(null, page, level);
	}

	public static Page captureContentRoot(String level) throws ServletException, IOException {
		ServletContext servletContext = getServletContext();
		return CapturePage.capturePage(
			servletContext,
			getRequest(),
			getResponse(),
			ContentRoot.getContentRoot(servletContext),
			CaptureLevel.valueOf(level.toUpperCase(Locale.ENGLISH))
		);
	}

	public static PageIndex getPageIndex(PageRef pageRef) throws ServletException, IOException, JspTagException {
		return PageIndex.getPageIndex(
			getServletContext(),
			getRequest(),
			getResponse(),
			pageRef
		);
	}

	public static List<Page> convertPageDagToList(Page rootPage, String level) throws ServletException, IOException {
		return PageDags.convertPageDagToList(
			getServletContext(),
			getRequest(),
			getResponse(),
			rootPage,
			CaptureLevel.valueOf(level.toUpperCase(Locale.ENGLISH))
		);
	}

	/**
	 * Gets the current capture level or <code>null</code> if not currently
	 * capturing
	 * 
	 * @see  CaptureLevel
	 */
	public static String getCaptureLevel() {
		final HttpServletRequest request = getRequest();
		final CapturePage capture = CapturePage.getCaptureContext(request);
		if(capture == null) return null;
		return CaptureLevel.getCaptureLevel(request).name().toLowerCase(Locale.ENGLISH);
	}

	public static File getFileInBook(String book, String path, boolean requireFile) throws ServletException, IOException {
		PageRef pageRef = PageRefResolver.getPageRef(
			getServletContext(),
			getRequest(),
			book,
			path
		);
		if(pageRef.getBook()==null) throw new IllegalArgumentException("Book not found: " + pageRef.getBookName());
		return pageRef.getResourceFile(true, requireFile);
	}

	public static File getFile(String path, boolean requireFile) throws ServletException, IOException {
		return getFileInBook(null, path, requireFile);
	}

	public static File getExeFileInBook(String book, String path) throws ServletException, IOException {
		File file = getFileInBook(book, path, false);
		if(
			!file.canExecute()
			&& !file.setExecutable(true)
		) {
			throw new IOException("Unable to set executable flag: " + file.getPath());
		}
		return file;
	}

	public static File getExeFile(String path) throws ServletException, IOException {
		return getExeFileInBook(null, path);
	}

	public static String encodeUrlParam(String value) throws UnsupportedEncodingException {
		return URLEncoder.encode(value, getResponse().getCharacterEncoding());
	}

	public static String getRefId(String id) throws ServletException {
		return PageIndex.getRefId(
			getServletContext(),
			getRequest(),
			id
		);
	}

	public static String getRefIdInPage(Page page, String id) {
		return PageIndex.getRefIdInPage(
			getServletContext(),
			getRequest(),
			page,
			id
		);
	}

	public static Double ceil(Double a) {
		return a==null ? null : Math.ceil(a);
	}

	public static Double floor(Double a) {
		return a==null ? null : Math.floor(a);
	}

	public static Map<String,String> parseQueryString(String queryString) throws UnsupportedEncodingException {
		if(queryString==null) return null;
		String requestEncoding = ServletUtil.getRequestEncoding(getRequest());
		List<String> pairs = StringUtility.splitString(queryString, '&');
		Map<String,String> params = new LinkedHashMap<>(pairs.size() * 4/3 + 1);
		for(String pair : pairs) {
			int equalPos = pair.indexOf('=');
			String name, value;
			if(equalPos==-1) {
				name = URLDecoder.decode(pair, requestEncoding);
				value = "";
			} else {
				name = URLDecoder.decode(pair.substring(0, equalPos), requestEncoding);
				value = URLDecoder.decode(pair.substring(equalPos + 1), requestEncoding);
			}
			if(!params.containsKey(name)) params.put(name, value);
		}
		return params;
	}

	public static String repeat(String value, int count) {
		if(value==null) return null;
		int len = value.length();
		if(len==0) return value;
		StringBuilder sb = new StringBuilder(len * count);
		for(int i=0; i<count; i++) sb.append(value);
		return sb.toString();
	}

	public static Book getBook(String pagePath) {
		if(pagePath==null) return null;
		Book book = BooksContextListener.getBook(getServletContext(), pagePath);
		if(book==null) throw new IllegalArgumentException("Book not found: " + pagePath);
		return book;
	}

	public static String encodeHexData(String data) {
		// Note: This is always UTF-8 encoded and does not depend on response encoding
		return StringUtility.convertToHex(data.getBytes(StandardCharsets.UTF_8));
	}

	public static boolean hasElement(Page page, String classname, boolean recursive) throws ServletException, IOException, ClassNotFoundException {
		return hasElementRecursive(
			getServletContext(),
			getRequest(),
			getResponse(),
			page,
			Class.forName(classname),
			recursive,
			recursive ? new HashSet<>() : null
		);
	}

	private static boolean hasElementRecursive(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		Page page,
		Class<?> clazz,
		boolean recursive,
		Set<PageRef> seenPages
	) throws ServletException, IOException {
		for(Element element : page.getElements()) {
			if(clazz.isAssignableFrom(element.getClass())) {
				return true;
			}
		}
		if(recursive) {
			seenPages.add(page.getPageRef());
			for(PageRef childRef : page.getChildPages()) {
				if(!seenPages.contains(childRef)) {
					if(
						hasElementRecursive(
							servletContext,
							request,
							response,
							CapturePage.capturePage(servletContext, request, response, childRef, CaptureLevel.META),
							clazz,
							recursive,
							seenPages
						)
					) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static boolean hasFile(Page page, boolean recursive) throws ServletException, IOException {
		return hasFileRecursive(
			getServletContext(),
			getRequest(),
			getResponse(),
			page,
			recursive,
			recursive ? new HashSet<>() : null
		);
	}

	private static boolean hasFileRecursive(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		Page page,
		boolean recursive,
		Set<PageRef> seenPages
	) throws ServletException, IOException {
		// Direct on page
		if(!page.getFiles().isEmpty()) {
			return true;
		}
		// In an element
		for(Element e : page.getElements()) {
			if(!e.getFiles().isEmpty()) {
				return true;
			}
		}
		if(recursive) {
			seenPages.add(page.getPageRef());
			for(PageRef childRef : page.getChildPages()) {
				if(!seenPages.contains(childRef)) {
					if(
						hasFileRecursive(
							servletContext,
							request,
							response,
							CapturePage.capturePage(servletContext, request, response, childRef, CaptureLevel.META),
							recursive,
							seenPages
						)
					) {
						return true;
					}
				}
			}
		}
		return false;
	}

	// <editor-fold desc="Accessing all elements by type">

	// <editor-fold defaultstate="collapsed" desc="Headings">
	/**
	 * Gets the list of all headings within the page in the order declared in the page.
	 */
	public static List<Heading> getHeadings(Page page) {
		return page.filterElements(Heading.class);
	}
//	private Map<String,Heading> headingsById;
//	public Map<String,Heading> getHeadingsById() {
//		if(elementsById == null) return Collections.emptyMap();
//		if(headingsById == null) headingsById = PragmaticMaps.filter(elementsById, Heading.class);
//		return headingsById;
//	}
	// </editor-fold>

	// </editor-fold>

	/**
	 * Make no instances.
	 */
	private Functions() {
	}
}