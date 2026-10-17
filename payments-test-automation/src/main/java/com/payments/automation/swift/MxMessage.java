package com.payments.automation.swift;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * ISO 20022 (MX) message wrapper with namespace-agnostic XPath.
 *
 * <p>Write simple paths without prefixes - they are converted to {@code local-name()} XPath,
 * so the same test works for pacs.008.001.08 and .001.10 or with/without AppHdr:</p>
 * <pre>
 * mx.get("GrpHdr/MsgId");
 * mx.get("CdtTrfTxInf/IntrBkSttlmAmt/@Ccy");
 * mx.get("AppHdr/Fr/FIId/FinInstnId/BICFI");
 * </pre>
 * A path starting with "/" is anchored at the root; otherwise it matches anywhere ("//").
 */
public class MxMessage {

    private final Document document;
    private final String raw;

    private MxMessage(Document document, String raw) {
        this.document = document;
        this.raw = raw;
    }

    public static MxMessage parse(String xml) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            // XXE protection
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Document doc = f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            doc.getDocumentElement().normalize();
            return new MxMessage(doc, xml);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new SwiftValidationException("Not well-formed XML: " + e.getMessage());
        }
    }

    /** e.g. "pacs.008.001.08" taken from the Document namespace. */
    public String getMessageDefinition() {
        String ns = namespaceOf("Document");
        return ns == null ? "" : ns.substring(ns.lastIndexOf(':') + 1);
    }

    /** "pacs.008", "pacs.009", "camt.053" ... */
    public String getMessageType() {
        String def = getMessageDefinition();
        return def.length() >= 8 ? def.substring(0, 8) : def;
    }

    public String namespaceOf(String localName) {
        NodeList nodes = document.getElementsByTagNameNS("*", localName);
        return nodes.getLength() == 0 ? null : nodes.item(0).getNamespaceURI();
    }

    public String get(String path) {
        Node node = node(path);
        return node == null ? null : node.getTextContent().trim();
    }

    public List<String> getAll(String path) {
        try {
            NodeList list = (NodeList) XPathFactory.newInstance().newXPath()
                    .evaluate(toXPath(path), document, XPathConstants.NODESET);
            List<String> values = new ArrayList<>();
            for (int i = 0; i < list.getLength(); i++) {
                values.add(list.item(i).getTextContent().trim());
            }
            return values;
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException("Bad path " + path, e);
        }
    }

    public boolean exists(String path) {
        return node(path) != null;
    }

    public int count(String path) {
        return getAll(path).size();
    }

    /** Changes an element / attribute value - handy for negative tests (invalid BIC, amount ...). */
    public MxMessage set(String path, String value) {
        Node node = node(path);
        if (node == null) {
            throw new IllegalArgumentException("Path not found: " + path);
        }
        node.setTextContent(value);
        return this;
    }

    public MxMessage remove(String path) {
        Node node = node(path);
        if (node != null) {
            node.getParentNode().removeChild(node);
        }
        return this;
    }

    private Node node(String path) {
        try {
            return (Node) XPathFactory.newInstance().newXPath().evaluate(toXPath(path), document, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException("Bad path " + path, e);
        }
    }

    /** "GrpHdr/MsgId" -> "//*[local-name()='GrpHdr']/*[local-name()='MsgId']"; "@Ccy" kept as attribute. */
    static String toXPath(String path) {
        if (path.contains("local-name") || path.contains("::")) {
            return path; // already a real XPath
        }
        boolean anchored = path.startsWith("/");
        String[] parts = path.replaceAll("^/+", "").split("/");
        StringBuilder xp = new StringBuilder(anchored ? "" : "/");
        for (String part : parts) {
            xp.append('/');
            if (part.startsWith("@")) {
                xp.append(part);
                continue;
            }
            String index = "";
            int bracket = part.indexOf('[');
            if (bracket > 0) {
                index = part.substring(bracket);
                part = part.substring(0, bracket);
            }
            xp.append("*[local-name()='").append(part).append("']").append(index);
        }
        return xp.toString();
    }

    /** Validates against an XSD on the classpath, e.g. "swift/xsd/pacs.008.001.08.xsd" (download from MyStandards). */
    public ValidationResult validateAgainstXsd(String classpathXsd) {
        ValidationResult r = new ValidationResult();
        URL xsd = Thread.currentThread().getContextClassLoader().getResource(classpathXsd);
        if (xsd == null) {
            r.error("XSD", "Schema not found on classpath: " + classpathXsd);
            return r;
        }
        try {
            SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(xsd)
                    .newValidator().validate(new StreamSource(new StringReader(raw)));
        } catch (SAXException | IOException e) {
            r.error("XSD", e.getMessage());
        }
        return r;
    }

    public Document getDocument() {
        return document;
    }

    public String toXml() {
        try {
            var t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StringWriter w = new StringWriter();
            t.transform(new DOMSource(document), new StreamResult(w));
            return w.toString();
        } catch (TransformerException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        return toXml();
    }
}
