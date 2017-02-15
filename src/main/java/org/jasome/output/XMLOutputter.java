package org.jasome.output;

import org.apache.commons.lang3.tuple.Pair;
import org.jasome.calculators.Metric;
import org.jasome.parsing.*;
import org.jasome.parsing.Package;
import org.jscience.mathematics.number.LargeInteger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class XMLOutputter implements Outputter<Document> {

    private static final DecimalFormat METRIC_VALUE_FORMAT = new DecimalFormat("0.0######");

    @Override
    public Document output(Project project) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element projectElement = doc.createElement("Project");
            doc.appendChild(projectElement);
            
            addAttributes(project, projectElement);
            addMetricsForNode(doc, projectElement, project);

            Element packagesElement = doc.createElement("Packages");
            projectElement.appendChild(packagesElement);

            for (Package packageNode : sortChildren(project.getPackages())) {
                Element packageElement = doc.createElement("Package");
                packageElement.setAttribute("name", packageNode.getName());
                packagesElement.appendChild(packageElement);

                addAttributes(packageNode, packageElement);
                addMetricsForNode(doc, packageElement, packageNode);

                Element classesElement = doc.createElement("Classes");
                packageElement.appendChild(classesElement);

                for (Type classNode : sortChildren(packageNode.getTypes())) {
                    Element classElement = doc.createElement("Class");
                    classElement.setAttribute("name", classNode.getName());
                    classesElement.appendChild(classElement);

                    addAttributes(classNode, classElement);
                    addMetricsForNode(doc, classElement, classNode);

                    Element methodsElement = doc.createElement("Methods");
                    classElement.appendChild(methodsElement);

                    for (Method methodNode : sortChildren(classNode.getMethods())) {
                        Element methodElement = doc.createElement("Method");
                        methodElement.setAttribute("name", methodNode.getName());
                        methodsElement.appendChild(methodElement);

                        addAttributes(methodNode, methodElement);
                        addMetricsForNode(doc, methodElement, methodNode);
                    }
                }
            }

            return doc;

        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

    }

    private <T extends Code> List<T> sortChildren(Collection<T> children) {
        return children.stream().sorted(new Comparator<Code>() {
            @Override
            public int compare(Code o1, Code o2) {
                return o1.getName().compareTo(o2.getName());
            }

        }).collect(Collectors.toList());
    }

    private void addAttributes(Code classNode, Element classElement) {
        for (Pair<String, String> attribute : classNode.getAttributes()) {
            classElement.setAttribute(attribute.getKey(), attribute.getValue());
        }
    }

    private void addMetricsForNode(Document doc, Node parentElement, Code node) {
        Element metricsContainer = doc.createElement("Metrics");

        Set<Metric> metrics = node.getMetrics();
        List<Metric> sortedMetrics = metrics.stream().sorted((m1, m2) -> m1.getName().compareTo(m2.getName())).collect(Collectors.toList());
        for (Metric metric : sortedMetrics) {
            Element metricsElement = doc.createElement("Metric");

            metricsElement.setAttribute("name", metric.getName());
            metricsElement.setAttribute("description", metric.getDescription());

            if(metric.getValue() instanceof LargeInteger) {
                metricsElement.setAttribute("value", metric.getValue().toString());
            } else {
                metricsElement.setAttribute("value", METRIC_VALUE_FORMAT.format(metric.getValue().doubleValue()));
            }

            metricsContainer.appendChild(metricsElement);
        }
        parentElement.appendChild(metricsContainer);
    }
}
