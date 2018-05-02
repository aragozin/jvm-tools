package org.gridkit.jvmtool.hflame;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
		String id = el.getAttribute("id");
		if (id == null || id.length() == 0) {
			id = el.getAttribute("ID");
		}
		if (id == null || id.length() == 0) {
			id = el.getAttribute("Id");
		}
		return id;
	}
}
