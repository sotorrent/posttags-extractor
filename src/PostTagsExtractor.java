import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.Arrays;
import java.util.Properties;

import static java.util.function.Predicate.not;

class PostTagsExtractor {
	public static void main(String[] args) {
	    // prevents JAXP00010004: The accumulated size of entities is "50.000.001" that exceeded the "50.000.000" limitset by "FEATURE_SECURE_PROCESSING".
        System.setProperty("jdk.xml.totalEntitySizeLimit", String.valueOf(Integer.MAX_VALUE));

        System.out.println("Reading properties...");
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String inputFile = properties.getProperty("InputFile");
        String outputFile = properties.getProperty("OutputFile");
        String delimiter = properties.getProperty("Delimiter");

        System.out.println("Reading tags from " + inputFile + ", writing output to " + outputFile + "...");
        XMLInputFactory factory = XMLInputFactory.newInstance();
		try (PrintWriter printWriter = new PrintWriter(new FileWriter(outputFile))) {
            // add CSV header
            printWriter.print("PostId,Tag\n");

            XMLEventReader xml = factory.createXMLEventReader(new StreamSource(new File(inputFile)));

            while (xml.hasNext()) {
                XMLEvent event = xml.nextEvent();

                if (!event.isStartElement()) {
                    continue;
                }

                StartElement elem = event.asStartElement();

                // only consider row elements containing question metadata (only questions have tags)
                if (!elem.getName().getLocalPart().equals("row")
                        || !(elem.getAttributeByName(new QName("PostTypeId")).getValue().equals("1"))) {
                    continue;
                }
                String id = elem.getAttributeByName(new QName("Id")).getValue();

                // write data to CSV
                String[] tags = Arrays.stream(elem.getAttributeByName(new QName("Tags")).getValue()
                        .replaceAll("<", "")
                        .replaceAll(">", ";")
                        .split(";"))
                        .filter(not(String::isEmpty))
                        .toArray(String[]::new);
                for (String tag : tags) {
                    printWriter.printf("%s%s%s\n", id, delimiter, tag);
                }
            }
		} catch (IOException | XMLStreamException e) {
			e.printStackTrace();
		}

        System.out.println("Done.");
    }
}
