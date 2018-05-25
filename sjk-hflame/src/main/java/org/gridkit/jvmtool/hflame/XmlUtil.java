/**
 * Copyright 2018 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.jvmtool.hflame;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

public class XmlUtil {

	public static Document newDocument() {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			return dbf.newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Document parseFromResource(String path) throws FileNotFoundException {
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
		if (is == null) {
			throw new FileNotFoundException("Unable locate resource: " + path);
		}
		InputStreamReader reader = new InputStreamReader(is, Charset.forName("UTF8"));
		return parse(reader);
	}
	
	public static Document parse(Reader reader) {
    	try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			return db.parse(new InputSource(reader));
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	} 
	}
	
	public static List<Element> elementsOf(Element el) {
		List<Element> r = new ArrayList<Element>();
		NodeList nl = el.getChildNodes();
		for(int i = 0; i != nl.getLength(); ++i) {
			Node n = nl.item(i);
			if (n instanceof Element) {
				r.add((Element)n);
			}
		}
		return r;
	}

	public static List<Text> textOf(Element el) {
		List<Text> r = new ArrayList<Text>();
		NodeList nl = el.getChildNodes();
		for(int i = 0; i != nl.getLength(); ++i) {
			Node n = nl.item(i);
			if (n instanceof Text) {
				r.add((Text)n);
			}
		}
		return r;
	}
	
	public static String id(Element el) {
		return attr(el, "id");
	}
	
	public static String href(Element el) {
		return attr(el, "src", "href");
	}
	
	public static boolean isStyleSheet(Element el) {
		if (el.getNodeName().equalsIgnoreCase("link")) {
			return "stylesheet".equalsIgnoreCase(attr(el, "rel"));
		}
		else if (el.getNodeName().equalsIgnoreCase("style")) {
			return true;
		}
		return false;
	}
	
	public static boolean isScript(Element el) {
		return el.getNodeName().equalsIgnoreCase("script");
	}
	
	public static String attr(Element el, String... attr) {
		NamedNodeMap nnm = el.getAttributes();
		for(int i = 0; i != nnm.getLength(); ++i) {
			Node node = nnm.item(i);
			if (node instanceof Attr) {
				Attr a = (Attr) node;
				for(String at: attr) {
					if (a.getName().equalsIgnoreCase(at)) {
						return a.getValue();
					}
				}
			}
 		}
		return null;
	}
}
