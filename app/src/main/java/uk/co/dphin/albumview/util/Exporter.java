package uk.co.dphin.albumview.util;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

import org.w3c.dom.*;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.*;

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

public class Exporter {

    private static final int BUFFER = 1024;

    public static void exportAlbumAsZip(Context context, Album album, File destination)
    {
        try {
            Exporter exporter = new Exporter();
            String xml = exporter.exportAlbumAsXml(album);
            Map<Slide, Exception> failedSlides = new TreeMap<>(); // TODO: Output somewhere

            // Create the zip file
            destination.createNewFile();
            Log.v("Export", "Created export file: " + destination.getPath());
            FileOutputStream fileOutput = new FileOutputStream(destination);
            ZipOutputStream output = new ZipOutputStream(new BufferedOutputStream(fileOutput));

            // Write the album XML file
            ZipEntry xmlEntry = new ZipEntry("album.xml");
            output.putNextEntry(xmlEntry);
            OutputStreamWriter outputStream = new OutputStreamWriter(output);
            PrintWriter printWriter = new PrintWriter(outputStream);
            printWriter.println(xml);
            printWriter.flush();
            outputStream.flush();
            Log.v("Export", "Saved album.xml");

            // Copy all images into the zip file
            output.putNextEntry(new ZipEntry("images/")); // If the name ends with / it's a directory
            int slideNumber = 0;
            byte data[] = new byte[BUFFER];

            for (Slide slide : album.getSlides()) {
                try {
                    slideNumber++;
                    if (slide instanceof ImageSlide) {
                        ImageSlide is = (ImageSlide) slide;
                        String originalName = is.getFileName();
                        String newName = generateNewFilename(originalName, slideNumber);
                        ZipEntry imageEntry = new ZipEntry("images/"+newName);
                        output.putNextEntry(imageEntry);

                        BufferedInputStream bis = new BufferedInputStream(context.getContentResolver().openInputStream(is.getFile().getUri()), BUFFER);
                        while (bis.read(data, 0, BUFFER) != -1) {
                            output.write(data);
                        }
                        bis.close();
                        output.flush();
                        Log.v("Export", "Stored image file "+newName);
                    }
                } catch (IOException e) {
                    failedSlides.put(slide, e);
                    Log.w("Export", "Could not copy file(s) for slide "+slideNumber+" due to a "+e.getClass().getCanonicalName());
                }
            }

            output.close();
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: close any hanging files
        }


    }

    public String exportAlbumAsXml(Album album)
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

    private Element albumToXml(Album album) throws ParserConfigurationException
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
        int slideNumber = 0;
        for (Slide slide : album.getSlides()) {
            slideNumber++;
            Element slideEl = (Element)doc.importNode(slideToXml(slide, slideNumber), true);
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
    private Element slideToXml(Slide slide, int slideNumber) throws ParserConfigurationException
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
            String imageName = is.getFileName();
            String srcPath;
            srcPath = generateNewFilename(imageName, slideNumber);
            Element srcEl = doc.createElement("src");
            srcEl.setTextContent(srcPath);
            root.appendChild(srcEl);
        }

        return root;
    }

    private static String generateNewFilename(String originalPath, int slideNumber)
    {
        return String.format(Locale.getDefault(), "slide%04d", slideNumber) +
                originalPath.substring(originalPath.lastIndexOf("."));
    }
}
