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

import static org.gridkit.jvmtool.hflame.XmlUtil.elementsOf;
import static org.gridkit.jvmtool.hflame.XmlUtil.href;
import static org.gridkit.jvmtool.hflame.XmlUtil.id;
import static org.gridkit.jvmtool.hflame.XmlUtil.isScript;
import static org.gridkit.jvmtool.hflame.XmlUtil.isStyleSheet;
import static org.gridkit.jvmtool.hflame.XmlUtil.textOf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class FlameTemplateProcessor {

	private final Document template;
	private final Map<String, String> imports = new HashMap<String, String>();
	private final Map<String, JsonFlameDataSet> datasets = new HashMap<String, JsonFlameDataSet>();
	
	public FlameTemplateProcessor(Document template) {
		this.template = template;
		initDefaultScripts();
	}	
	
	private void initDefaultScripts() {
		// nothing
	}

	public void setDataSet(String name, JsonFlameDataSet dataSet) {
		datasets.put(name, dataSet);
	}
	
	public void generate(Writer output) throws IOException {
		Document doc = (Document) template.cloneNode(true);
		transformHead((Element) doc.getDocumentElement().getElementsByTagName("head").item(0));
		transformBody((Element) doc.getDocumentElement().getElementsByTagName("body").item(0));
		encodeDocument(doc, output);
	}
	
	private void encodeDocument(Document doc, Writer output) {
		try {

			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "utf8");
			
			StreamResult result = new StreamResult(output);
			DOMSource source = new DOMSource(doc.getDocumentElement());
			transformer.transform(source, result);
			
		} catch (TransformerConfigurationException e) {
			throw new RuntimeException(e);
		} catch (TransformerFactoryConfigurationError e) {
			throw new RuntimeException(e);
		} catch (TransformerException e) {
			throw new RuntimeException(e);
		}
	}

	private void transformHead(Element head) throws IOException {
		for(Element e: elementsOf(head)) {
			String id = id(e);
			String href = href(e);
			if (isStyleSheet(e) || isScript(e)) {
				if (id == null) {
					if (href != null && !href.startsWith("..")) {
						if (isStyleSheet(e)) {
							// inline stylesheet
							Element re = head.getOwnerDocument().createElement("style");
							head.replaceChild(re, e);
							importCss(href, re);
						}
						else {
							// inline script
							e.removeAttribute("src");
							importJs(href, e);							
						}
					}
					else {
						// remove elements with ../ source
						head.removeChild(e);
					}
				}
				else {
					// keep "identified" elements as is
				}
			}
			if (id != null && id.startsWith("debug_")) {
				head.removeChild(e);
			}
			else if (id != null && id.startsWith("importflame_")) {
				String flameName = id.substring("importflame_".length());
				e.removeAttribute("id");
				e.removeAttribute("src");
				importDataSet(flameName, e);
			}
		}		
	}
	
	private void transformBody(Element node) throws IOException {
		for(Element e: elementsOf(node)) {
			String id = id(e);
			if (id != null && id.startsWith("debug_")) {
				node.removeChild(e);
			}
			else if (id != null && id.startsWith("importflame_")) {
				String flameName = id.substring("importflame_".length());
				e.removeAttribute("id");
				e.removeAttribute("src");
				importDataSet(flameName, e);
			}
			else {
				transformBody(e);
			}			
		}				
	}

	private InputStream openResource(String res) throws IOException {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(res);
	}
	
	private void importCss(String cssName, Element e) throws IOException {
		InputStream is = openResource(cssName);
		if (is == null) {
			String res = imports.get("css:" + cssName);
			if (res != null) {
				is = openResource(res);
			}
		}
		if (is == null) {
			throw new IllegalArgumentException("Unknown CSS resource: " + cssName);
		}
		loadContent(e, is);
	}

	private void importJs(String jsName, Element e) throws IOException {
		InputStream is = openResource(jsName);
		if (is == null) {
			String res = imports.get("js:" + jsName);
			if (res != null) {
				is = openResource(res);
			}
		}
		if (is == null) {
			throw new IllegalArgumentException("Unknown script name: " + jsName);
		}
		loadContent(e, is);
	}

	private void importDataSet(String flameName, Element e) {
		JsonFlameDataSet dataSet = datasets.get(flameName);
		if (dataSet == null) {
			throw new IllegalArgumentException("Unknown data set name: " + flameName);
		}

		for(Text t: textOf(e)) {
			e.removeChild(t);
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("$(document).ready(function() {createFlameChart(\"" + flameName + "\", ");
		dataSet.exportJson(sb);
		sb.append(").initFlameChart()});\n");
		Text text = e.getOwnerDocument().createCDATASection(sb.toString());
		e.appendChild(text);
	}

	private void loadContent(Element e, InputStream is) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF8")));
		StringBuilder sb = new StringBuilder();
		while(true) {
			String line = reader.readLine();
			if (line == null) {
				break;
			}
			if (line.contains("!!THROW AWAY BELOW!!")) {
				break;
			}
			if (line.contains("debug(") && line.contains(");")) {
				// filtering out debug
				continue;
			}
			for(int i = 0; i != line.length(); ++i) {
				char ch = line.charAt(i);
				if (ch != '\r' ) {
					sb.append(ch);
				}
			}
			sb.append('\n');
		}
		reader.close();
		
		for(Text t: textOf(e)) {
			e.removeChild(t);
		}
		
		Text text = e.getOwnerDocument().createCDATASection(sb.toString());
		e.appendChild(text);
	}
}
