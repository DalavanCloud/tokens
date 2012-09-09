import java.io.StringWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import junit.framework.TestCase;
import name.kazennikov.annotations.Document;
import name.kazennikov.annotations.XmlStreamAnnotationRender;

import org.junit.Test;


public class AnnotationRenderTest extends TestCase {
	XMLOutputFactory factory;
	Document d;
	
	@Override
	public void setUp() {
		factory = XMLOutputFactory.newFactory();
		d = new Document("doc", "This is a tree.");
		d.addAnnotation("foo", 0, 4);
		d.addAnnotation("bar", 0, 7);
		d.addAnnotation("bar", 10, 17);
		
	}
	
	@Test
	public void testXmlRender() throws Exception {
		StringWriter sw = new StringWriter();
		XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
		XmlStreamAnnotationRender render = new XmlStreamAnnotationRender(writer);
		render.render(d.getAllAnnotations());
		writer.close();
		String value = sw.toString();
		assertEquals("<doc><bar><foo>This</foo> is</bar> a <bar>tree.</bar></doc>", value);
	}

}
