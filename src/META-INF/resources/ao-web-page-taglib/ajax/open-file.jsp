<%--
  ao-web-page-taglib - Java API for modeling web page content and relationships in a JSP environment.
  Copyright (C) 2013, 2014, 2016  AO Industries, Inc.
    support@aoindustries.com
    7262 Bull Pen Cir
    Mobile, AL 36695

  This file is part of ao-web-page-taglib.

  ao-web-page-taglib is free software: you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  ao-web-page-taglib is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with ao-web-page-taglib.  If not, see <http://www.gnu.org/licenses/>.
--%>
<%@ page language="java" buffer="512kb" autoFlush="true" pageEncoding="UTF-8" session="false" %>
<%@ page contentType="application/xml" %>
<%@include file="../taglibs.inc.jsp" %>
<%--
Opens the file provided in the path parameter.  This file
must reside within this application and be of a supported type.
This is to be called by the JavaScript function openFile.

Request parameters:
	book  The name of the book of the file to open
	path  The book-relative path of the file to open
--%>
<p:openFile book="${param.book}" path="${param.path}" />
<?xml version="1.0" encoding="UTF-8" standalone="yes" ?>
<success>true</success>
