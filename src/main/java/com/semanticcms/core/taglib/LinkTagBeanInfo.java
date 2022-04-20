/*
 * semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2015, 2016, 2017, 2018, 2019, 2020, 2021, 2022  AO Industries, Inc.
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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

public class LinkTagBeanInfo extends SimpleBeanInfo {

  private static final PropertyDescriptor[] properties;
  static {
    try {
      properties = new PropertyDescriptor[] {
        new PropertyDescriptor("class",                 LinkTag.class, "getClazz", "setClazz"),
        new PropertyDescriptor("book",                  LinkTag.class, null,       "setBook"),
        new PropertyDescriptor("page",                  LinkTag.class, null,       "setPage"),
        new PropertyDescriptor("element",               LinkTag.class, null,       "setElement"),
        new PropertyDescriptor("allowGeneratedElement", LinkTag.class, null,       "setAllowGeneratedElement"),
        new PropertyDescriptor("anchor",                LinkTag.class, null,       "setAnchor"),
        new PropertyDescriptor("view",                  LinkTag.class, null,       "setView"),
        new PropertyDescriptor("small",                 LinkTag.class, null,       "setSmall"),
        new PropertyDescriptor("absolute",              LinkTag.class, null,       "setAbsolute"),
        new PropertyDescriptor("canonical",             LinkTag.class, null,       "setCanonical")
      };
    } catch (IntrospectionException err) {
      throw new ExceptionInInitializerError(err);
    }
  }

  @Override
  @SuppressWarnings("ReturnOfCollectionOrArrayField") // Not copying array for performance
  public PropertyDescriptor[] getPropertyDescriptors () {
    return properties;
  }

  /**
   * Include base class.
   */
  @Override
  public BeanInfo[] getAdditionalBeanInfo() {
    try {
      return new BeanInfo[] {
        Introspector.getBeanInfo(LinkTag.class.getSuperclass())
      };
    } catch (IntrospectionException err) {
      throw new AssertionError(err);
    }
  }
}
