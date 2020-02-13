/*
 * semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020  AO Industries, Inc.
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

import com.aoindustries.encoding.Doctype;
import com.aoindustries.encoding.EncodingContext;
import com.aoindustries.encoding.Serialization;
import com.aoindustries.io.NullWriter;
import com.aoindustries.io.buffer.BufferResult;
import com.aoindustries.io.buffer.BufferWriter;
import com.aoindustries.io.buffer.EmptyResult;
import com.aoindustries.net.DomainName;
import com.aoindustries.net.Path;
import com.aoindustries.servlet.ServletContextCache;
import com.aoindustries.servlet.jsp.LocalizedJspTagException;
import com.aoindustries.taglib.AutoEncodingBufferedTag;
import static com.aoindustries.util.StringUtility.nullIfEmpty;
import com.aoindustries.validation.ValidationException;
import com.semanticcms.core.controller.PageRefResolver;
import com.semanticcms.core.controller.PageUtils;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.model.PageRef;
import com.semanticcms.core.model.ResourceRef;
import com.semanticcms.core.pages.local.CaptureContext;
import com.semanticcms.core.servlet.impl.PageImpl;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

public class PageTag extends SimpleTagSupport implements DynamicAttributes {

	/**
	 * The prefix for property attributes.
	 */
	public static final String PROPERTY_ATTRIBUTE_PREFIX = "property.";

	/**
	 * The application scoped attribute holding the properties cache.
	 */
	private static final String PROPERTIES_CACHE_APPLICATION_KEY = PageTag.class.getName() + ".propertiesCache";

	/**
	 * Cache properties files from URLs with unknown modified times for up to 10 seconds.
	 */
	private static final long PROPERTIES_CACHE_UNKNOWN_MODIFIED_CACHE_DURATION = 10000;

	/**
	 * The time span between URL last modified checks.
	 */
	private static final long PROPERTIES_CACHE_LAST_MODIFIED_RECHECK_INTERVAL = 1000;

	/**
	 * Constant debug flag for compile-time debugging.
	 */
	private static final boolean DEBUG = false;

	private DomainName domain;
	public void setDomain(String domain) throws ValidationException {
		this.domain = DomainName.valueOf(nullIfEmpty(domain));
	}

	private Path book;
	public void setBook(String book) throws ValidationException {
		this.book = Path.valueOf(nullIfEmpty(book));
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

	private Serialization serialization;
	public void setSerialization(String serialization) {
		if(serialization == null) {
			this.serialization = null;
		} else {
			serialization = serialization.trim();
			this.serialization = (serialization.isEmpty() || "auto".equalsIgnoreCase(serialization)) ? null : Serialization.valueOf(serialization.toUpperCase(Locale.ROOT));
		}
	}

	private Doctype doctype = EncodingContext.DEFAULT_DOCTYPE;
	public void setDoctype(String doctype) {
		if(doctype == null) {
			this.doctype = null;
		} else {
			doctype = doctype.trim();
			this.doctype = (doctype.isEmpty() || "default".equalsIgnoreCase(doctype)) ? null : Doctype.valueOf(doctype.toUpperCase(Locale.ROOT));
		}
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
					properties = new LinkedHashMap<>();
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

	private static class PropertiesCacheEntry {

		private final long lastModified;
		private final long cachedTime;
		private final Map<String,String> properties;

		private PropertiesCacheEntry(
			long lastModified,
			long cachedTime,
			Map<String,String> properties
		) {
			this.lastModified = lastModified;
			this.cachedTime = cachedTime;
			this.properties = properties;
		}
	}

	@Override
	public void doTag() throws JspException, IOException {
		try {
			PageContext pageContext = (PageContext)getJspContext();
			ServletContext servletContext = pageContext.getServletContext();
			final HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();

			CaptureContext capture = CaptureContext.getCaptureContext(request);
			if(capture == null) {
				throw new ServletException("CaptureContext not set, direct access to page instead of through Controller?");
			}

			// Resolve pageRef, if book or path set
			final ResourceRef jspSrc;
			final PageRef pageRef;
			if(domain != null && book == null) {
				throw new ServletException("book must be provided when domain is provided.");
			}
			// TODO: Use pages
			if(path == null) {
				if(book != null) throw new ServletException("path must be provided when book is provided.");
				// Use default
				PageRef currentPageRef = PageRefResolver.getCurrentPageRef(servletContext, request, true);
				jspSrc = new ResourceRef(
					currentPageRef.getBookRef(),
					currentPageRef.getPath()
				);
				final String _path = currentPageRef.getPath().toString();
				String pagePath;
				if(_path.endsWith("/index.jspx")) {
					pagePath = _path.substring(0, _path.length() - "index.jspx".length());
					assert pagePath.endsWith("/");
				} else if(_path.endsWith("/index.jsp")) {
					pagePath = _path.substring(0, _path.length() - "index.jsp".length());
					assert pagePath.endsWith("/");
				} else if(_path.endsWith(".jspx")) {
					pagePath = _path.substring(0, _path.length() - ".jspx".length());
					if(pagePath.endsWith("/")) throw new ServletException("Unexpected path for page tag: " + _path);
				} else if(_path.endsWith(".jsp")) {
					pagePath = _path.substring(0, _path.length() - ".jsp".length());
					if(pagePath.endsWith("/")) throw new ServletException("Unexpected path for page tag: " + _path);
				} else {
					throw new ServletException("Unexpected path for page tag: " + _path);
				}
				try {
					pageRef = new PageRef(
						currentPageRef.getBookRef(),
						Path.valueOf(pagePath)
					);
				} catch(ValidationException e) {
					throw new ServletException(e);
				}
			} else {
				PageRef currentPageRef = PageRefResolver.getCurrentPageRef(servletContext, request, false);
				jspSrc = new ResourceRef(
					currentPageRef.getBookRef(),
					currentPageRef.getPath()
				);
				pageRef = PageRefResolver.getPageRef(servletContext, request, domain, book, path);
			}

			//  Load properties from *.properties file, too
			String propertiesPath = null;
			if(jspSrc != null) {
				String pagePath = jspSrc.getPath().toString();
				if(pagePath.endsWith(".jspx")) {
					int basePathLen = pagePath.length() - 5;
					if(
						basePathLen > 0
						&& pagePath.charAt(basePathLen - 1) != '/'
					) {
						propertiesPath = pagePath.substring(0, basePathLen) + ".properties";
					}
				}
			}
			if(propertiesPath != null) {
				// TODO: Try real path first for more direct file-based I/O - benchmark if is any faster
				// TODO: Work through Resources API
				URL url = ServletContextCache.getCache(servletContext).getResource(
					pageRef.getBookRef().getPrefix()
					+ propertiesPath
				);
				if(url != null) {
					// if(DEBUG) System.out.println("PageTag: doTag: Got properties URL: " + url);
					Map<String,String> propsFromFile;
					{
						final ConcurrentMap<URL,PropertiesCacheEntry> propertiesCache;
						// No locking since no harm done if we create and immediately discard a cache instance between threads
						{
							@SuppressWarnings("unchecked")
							ConcurrentMap<URL,PropertiesCacheEntry> map = (ConcurrentMap)servletContext.getAttribute(PROPERTIES_CACHE_APPLICATION_KEY);
							if(map == null) {
								map = new ConcurrentHashMap<>();
								servletContext.setAttribute(PROPERTIES_CACHE_APPLICATION_KEY, map);
								if(DEBUG) System.out.println("PageTag: doTag: Created propertiesCache");
							}
							propertiesCache = map;
						}
						final long currentTime = System.currentTimeMillis();
						URLConnection urlConn = null;
						boolean urlClosed = false;
						try {
							long urlLastModified = 0;
							propsFromFile = null;
							// Check cache first
							{
								PropertiesCacheEntry cacheEntry = propertiesCache.get(url);
								if(cacheEntry != null) {
									if(cacheEntry.lastModified == 0) {
										// Using expiration time since last modified was unknown
										if(
											currentTime < (cacheEntry.cachedTime + PROPERTIES_CACHE_UNKNOWN_MODIFIED_CACHE_DURATION)
											// Time set to the past
											&& currentTime > (cacheEntry.cachedTime - PROPERTIES_CACHE_UNKNOWN_MODIFIED_CACHE_DURATION)
										) {
											//if(DEBUG) System.out.println("PageTag: doTag: Still in unknown last modified");
											propsFromFile = cacheEntry.properties;
										} else {
											if(DEBUG) System.out.println("PageTag: doTag: Time expired for cache entry with unknown last modified: currentTime = " + currentTime + ", cacheEntry.cachedTime = " + cacheEntry.cachedTime);
										}
									} else {
										// Only check last modified from URL at defined interval
										if(
											currentTime < (cacheEntry.cachedTime + PROPERTIES_CACHE_LAST_MODIFIED_RECHECK_INTERVAL)
											// Time set to the past
											&& currentTime > (cacheEntry.cachedTime - PROPERTIES_CACHE_LAST_MODIFIED_RECHECK_INTERVAL)
										) {
											//if(DEBUG) System.out.println("PageTag: doTag: Still in known last modified");
											propsFromFile = cacheEntry.properties;
										} else {
											if(DEBUG) System.out.println("PageTag: doTag: Time expired for cache entry with known last modified: currentTime = " + currentTime + ", cacheEntry.cachedTime = " + cacheEntry.cachedTime);
											if(urlConn == null) {
												urlConn = url.openConnection();
												urlLastModified = urlConn.getLastModified();
												// TODO: Use ServletContextCache.getLastModified?
												if(DEBUG) System.out.println("PageTag: doTag: Got last modified 1: " + urlLastModified);
											}
											// Use properties when last modified matches
											if(urlLastModified != 0 && cacheEntry.lastModified == urlLastModified) {
												propsFromFile = cacheEntry.properties;
												// Refresh in cache
												propertiesCache.put(
													url,
													new PropertiesCacheEntry(urlLastModified, currentTime, propsFromFile)
												);
											}
										}
									}
								} else {
									if(DEBUG) System.out.println("PageTag: doTag: URL not found in cache: " + url);
								}
							}
							if(propsFromFile == null) {
								// Load properties
								if(urlConn == null) {
									urlConn = url.openConnection();
									// TODO: Use ServletContextCache.getLastModified?
									urlLastModified = urlConn.getLastModified();
									if(DEBUG) System.out.println("PageTag: doTag: Got last modified 2: " + urlLastModified);
								}
								Properties props = new Properties();
								{
									if(DEBUG) System.out.println("PageTag: doTag: Loading properties from URL: " + url);
									try (InputStream in = urlConn.getInputStream()) {
										props.load(in);
									} finally {
										urlClosed = true;
									}
								}
								Set<String> propertyNames = props.stringPropertyNames();
								int size = propertyNames.size();
								if(size == 0) {
									if(DEBUG) System.out.println("PageTag: doTag: Got " + size + " properties, using empty map");
									propsFromFile = Collections.emptyMap();
								} else if(size == 1) {
									if(DEBUG) System.out.println("PageTag: doTag: Got " + size + " property, using singleton map");
									String propertyName = propertyNames.iterator().next();
									propsFromFile = Collections.singletonMap(
										propertyName,
										props.getProperty(propertyName)
									);
								} else {
									if(DEBUG) System.out.println("PageTag: doTag: Got " + size + " properties, using unmodifiable wrapped linked hash map");
									Map<String,String> newMap = new LinkedHashMap<>(size*4/3+1); // linked map for maximum iteration performance
									for(String propertyName : propertyNames) {
										newMap.put(
											propertyName,
											props.getProperty(propertyName)
										);
									}
									propsFromFile = Collections.unmodifiableMap(newMap);
								}
								// Store in cache
								propertiesCache.put(
									url,
									new PropertiesCacheEntry(urlLastModified, currentTime, propsFromFile)
								);
							}
						} finally {
							if(urlConn != null && !urlClosed) {
								if(DEBUG) System.out.println("PageTag: doTag: Closing connection");
								urlConn.getInputStream().close();
							}
						}
					}
					// Apply the loaded properties, not replacing any set on the page via dynamic attributes
					int numPropsFromFile = propsFromFile.size();
					if(numPropsFromFile > 0) {
						if(properties == null) {
							properties = new LinkedHashMap<>(numPropsFromFile*4/3+1);
						}
						for(Map.Entry<String,String> entry : propsFromFile.entrySet()) {
							String propertyName = entry.getKey();
							if(properties.containsKey(propertyName)) {
								throw new LocalizedJspTagException(
									ApplicationResources.accessor,
									"error.duplicatePropertiesFileProperty",
									propertyName,
									url
								);
							}
							properties.put(propertyName, entry.getValue());
						}
					}
				}
			}

			final JspFragment body = getJspBody();
			PageImpl.doPageImpl(
				servletContext,
				request,
				(HttpServletResponse)pageContext.getResponse(),
				capture,
				pageRef,
				PageUtils.toDateTime(dateCreated),
				PageUtils.toDateTime(datePublished),
				PageUtils.toDateTime(dateModified),
				PageUtils.toDateTime(dateReviewed),
				serialization,
				doctype,
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
							// TODO: Finish this
							if(jspSrc != null && jspSrc.equals(page.getPageRef())) page.setSrc(jspSrc);
							if(discard) {
								body.invoke(NullWriter.getInstance());
								return EmptyResult.getInstance();
							} else {
								// TODO: Are request-scoped temp files still correct for longer-term caches like the export cache?
								// TODO:     Caches only store PAGE and META captures, right?  Impact?
								// TODO:     Would we have a cache-scoped TempFileContext?
								BufferWriter capturedOut = AutoEncodingBufferedTag.newBufferWriter(request);
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
