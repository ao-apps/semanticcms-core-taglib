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

import com.aoindustries.io.FileUtils;
import com.aoindustries.lang.ProcessResult;
import com.aoindustries.web.page.DiaExport;
import com.aoindustries.web.page.servlet.OpenFile;
import com.aoindustries.web.page.servlet.PageRefResolver;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.SkipPageException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class OpenFileTag extends SimpleTagSupport {

	private static final Logger logger = Logger.getLogger(OpenFileTag.class.getName());

	private static String getJdkPath() {
		try {
			String hostname = InetAddress.getLocalHost().getCanonicalHostName();
			if(
				"francis.aoindustries.com".equals(hostname)
				|| "freedom.aoindustries.com".equals(hostname)
			) return "/opt/jdk1.8.0-i686";
		} catch(UnknownHostException e) {
			// Fall-through to default 64-bit
		}
		return "/opt/jdk1.8.0";
	}

	private String book;
	public void setBook(String book) {
		this.book = book;
	}

	private String path;
	public void setPath(String path) {
		this.path = path;
    }

	@Override
    public void doTag() throws JspException, IOException {
		final PageContext pageContext = (PageContext)getJspContext();
		final ServletContext servletContext = pageContext.getServletContext();
		final HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
		final HttpServletResponse response = (HttpServletResponse)pageContext.getResponse();
		// Only allow from localhost and when open enabled
		if(!OpenFile.isAllowed(servletContext, request)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			throw new SkipPageException();
		} else {
			String[] command;
			File resourceFile;
			try {
				resourceFile = PageRefResolver.getPageRef(servletContext, request, this.book, this.path).getResourceFile(true, true);
			} catch(ServletException e) {
				throw new JspTagException(e);
			}
			if(resourceFile.isDirectory()) {
				command = new String[] {
					// TODO: What is good windows path?
					//DiaExport.isWindows()
					//	? "C:\\Program Files (x86)\\OpenOffice 4\\program\\swriter.exe"
					"/usr/bin/konqueror",
					resourceFile.getCanonicalPath()
				};
			} else {
				// Open the file with the appropriate application based on extension
				String extension = FileUtils.getExtension(resourceFile.getName()).toLowerCase(Locale.ENGLISH);
				switch(extension) {
					case "dia" :
						command = new String[] {
							DiaExport.getDiaOpenPath(),
							resourceFile.getCanonicalPath()
						};
						break;
					case "gif" :
					case "jpg" :
					case "jpeg" :
					case "png" :
						command = new String[] {
							DiaExport.isWindows()
								? "C:\\Program Files (x86)\\OpenOffice 4\\program\\swriter.exe"
								: "/usr/bin/gwenview",
							resourceFile.getCanonicalPath()
						};
						break;
					case "doc" :
					case "odt" :
						command = new String[] {
							DiaExport.isWindows()
								? "C:\\Program Files (x86)\\OpenOffice 4\\program\\swriter.exe"
								: "/usr/bin/libreoffice",
							"--writer",
							resourceFile.getCanonicalPath()
						};
						break;
					case "csv" :
					case "ods" :
					case "sxc" :
					case "xls" :
						command = new String[] {
							DiaExport.isWindows()
								? "C:\\Program Files (x86)\\OpenOffice 4\\program\\scalc.exe"
								: "/usr/bin/libreoffice",
							"--calc",
							resourceFile.getCanonicalPath()
						};
						break;
					case "pdf" :
						command = new String[] {
							DiaExport.isWindows()
								? "C:\\Program Files (x86)\\Adobe\\Reader 11.0\\Reader\\AcroRd32.exe"
								: "/usr/bin/okular",
							resourceFile.getCanonicalPath()
						};
						break;
					//case "sh" :
					//	command = new String[] {
					//		"/usr/bin/kwrite",
					//		resourceFile.getCanonicalPath()
					//	};
					//	break;
					case "java" :
					case "jsp" :
					case "sh" :
					case "txt" :
					case "xml" :
						if(DiaExport.isWindows()) {
							command = new String[] {
								"C:\\Program Files\\NetBeans 7.4\\bin\\netbeans64.exe",
								"--open",
								resourceFile.getCanonicalPath()
							};
						} else {
							command = new String[] {
								//"/usr/bin/kwrite",
								"/opt/netbeans-8.0.2/bin/netbeans",
								"--jdkhome",
								getJdkPath(),
								"--open",
								resourceFile.getCanonicalPath()
							};
						}
						break;
					case "zip" :
						if(DiaExport.isWindows()) {
							command = new String[] {
								resourceFile.getCanonicalPath()
							};
						} else {
							command = new String[] {
								"/usr/bin/konqueror",
								resourceFile.getCanonicalPath()
							};
						}
						break;
					case "mp3" :
					case "wma" :
						command = new String[] {
							DiaExport.isWindows()
								? "C:\\Program Files\\VideoLAN\\VLC.exe"
								: "/usr/bin/vlc",
							resourceFile.getCanonicalPath()
						};
						break;
					default :
						throw new IllegalArgumentException("Unsupprted file type by extension: " + extension);
				}
			}
			// Start the process
			final Process process = Runtime.getRuntime().exec(command);
			// Result is watched in the background only
			new Thread(() -> {
				try {
					final ProcessResult result = ProcessResult.getProcessResult(process);
					int exitVal = result.getExitVal();
					if(exitVal != 0) {
						logger.log(Level.SEVERE, "Non-zero exit status from \"{0}\": {1}", new Object[]{path, exitVal});
					}
					String stdErr = result.getStderr();
					if(!stdErr.isEmpty()) {
						logger.log(Level.SEVERE, "Standard error from \"{0}\":\n{1}", new Object[]{path, stdErr});
					}
					if(logger.isLoggable(Level.INFO)) {
						String stdOut = result.getStdout();
						if(!stdOut.isEmpty()) {
							logger.log(Level.INFO, "Standard output from \"{0}\":\n{1}", new Object[]{path, stdOut});
						}
					}
				} catch(IOException e) {
					logger.log(Level.SEVERE, null, e);
				}
			}).start();
		}
	}
}
