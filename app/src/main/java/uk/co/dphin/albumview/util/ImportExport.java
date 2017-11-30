package uk.co.dphin.albumview.util;

import android.util.Xml;

import org.w3c.dom.*;
import org.xmlpull.v1.XmlSerializer;

import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import uk.co.dphin.albumview.models.*;

/**
 * Created by peter on 29/11/17.
 */

public class ImportExport {

    public static String exportAlbumAsXml(Album album)
    {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            Document doc=parser.newDocument();
            doc.appendChild(doc.importNode(albumToXml(album), true));

            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            return writer.toString();

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        return "";
    }

    private static Element albumToXml(Album album) throws ParserConfigurationException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = factory.newDocumentBuilder();
        Document doc=parser.newDocument();
        Element root=doc.createElement("album");

        String name = album.getName();
        Element nameEl = doc.createElement("name");
        nameEl.setTextContent(name);
        root.appendChild(nameEl);

        Date dateCreated = album.getCreated();
        if (dateCreated != null) {
            Element dateCreatedEl = doc.createElement("date");
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // TODO: Timezone
            dateCreatedEl.setTextContent(dateFormat.format(dateCreated));
            root.appendChild(dateCreatedEl);
        }

        Element slidesContainerEl = doc.createElement("slides");
        for (Slide slide : album.getSlides()) {
            Element slideEl = (Element)doc.importNode(slideToXml(slide), true);
            slidesContainerEl.appendChild(slideEl);
        }
        root.appendChild(slidesContainerEl);

        return root;
    }

    /**
     *
     * @param slide
     * @return
     */
    private static Element slideToXml(Slide slide) throws ParserConfigurationException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = factory.newDocumentBuilder();
        Document doc=parser.newDocument();
        Element root=doc.createElement("slide");

        Date date = slide.getDate();
        if (date != null) {
            Element dateEl = doc.createElement("date");
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // TODO: Timezone
            dateEl.setTextContent(dateFormat.format(date));
            root.appendChild(dateEl);
        }

        String heading = slide.getHeading();
        Element headingEl = doc.createElement("heading");
        headingEl.setTextContent(heading);
        root.appendChild(headingEl);

        String caption = slide.getCaption();
        Element captionEl = doc.createElement("caption");
        captionEl.setTextContent(caption);
        root.appendChild(captionEl);

        if (slide instanceof ImageSlide) {
            root.setAttribute("type", "image");
            ImageSlide is = (ImageSlide)slide;
            String imagePath = is.getImagePath();
            Element srcEl = doc.createElement("src");
            srcEl.setTextContent(imagePath); // TODO: rewrite path for export - option to zip up all images with new filenames
            root.appendChild(srcEl);
        }

        return root;
    }
}
