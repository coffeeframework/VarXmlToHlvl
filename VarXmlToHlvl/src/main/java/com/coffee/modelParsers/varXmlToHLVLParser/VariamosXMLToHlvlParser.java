package com.coffee.modelParsers.varXmlToHLVLParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import com.github.coffeeframework.basickhlvlpackage.DecompositionType;
import com.github.coffeeframework.basickhlvlpackage.GroupType;
import com.github.coffeeframework.basickhlvlpackage.HlvlBasicFactory;
import com.github.coffeeframework.basickhlvlpackage.IHlvlBasicFactory;
import com.github.coffeeframework.basickhlvlpackage.IHlvlParser;
import com.github.coffeeframework.utils.*;

/**
 * This is a class that is responsible for extracting 
 * information from the object of the XML element and the XML 
 * dependency object and converting that information into the HLVL code.
 * 
 * @version 0.5, 19/01/2019
 * @author Joan David Colina Echeverry
 * contracts modified on September 15th by Juan Diego Carvajal Castaño
 * bugs fixed by avillota on octobre 16th 2019
 */
public class VariamosXMLToHlvlParser implements IHlvlParser {
	
	/**
	 * tabulation 
	 */
	private static final String TAB ="\t"; 

	/**
	 * List that contains the dependencies between the elements from a xml file
	*/
	private ArrayList<Dependency> importantXmlDependecy;

	/**
	 * Map that contains the elements from a xml. These elements can be: root, general and leaf
	 */
	private HashMap<String, Element> xmlElements;

	/**
	 * Relationship with the XmlReader class that fulfills the
	 * function of loading the XML file
	 */
	private XmlReader xmlReader;

	/**
	 * Relationship with the ParsingParameters class that fulfills
	 * the function of save the pahts to load de xml fiel and write de HLVL
	 * code file
	 */
	private ParsingParameters params;

	/**
	 * Relationship with the StringBuilder class that fulfills the
	 * function of save the HLVL code
	 */
	private StringBuilder HlvlCode;
	/**
	 * Relationship with the HlvlBasicFactory class that fulfills
	 * the function of creating the HLVL code
	 */
	private IHlvlBasicFactory rules;

	/**
	 * This method is responsible for creating a VariamosXMLToHlvlParser object and
	 * initializedHlvlCode and params attributes.
	 */
	public VariamosXMLToHlvlParser(ParsingParameters params) {
		HlvlCode = new StringBuilder();
		this.params = params;
	}
	
	/**
	 * This method is responsible for creating a VariamosXMLToHlvlParser object and
	 * Initialized the HlvlCode attribute
	 */
	public VariamosXMLToHlvlParser() {
		HlvlCode = new StringBuilder();
	}

	/**
	 * This method is responsible for creating a HLVL code's file
	 */
	public void writeFile() {
		FileUtils.writeHLVLProgram(params.getOutputPath(), HlvlCode.toString());
		System.out.println("Conversion complete");
	}

	/**
	 * This method is responsible for load the XML file and inicializate the
	 * importantXmlDependency and xmlElements arraylists using an xml file whose
	 * location path is configured in the params attribute
	 * <pre>params != null and params.inputPath != null and params.outputPath != null and params.targetName != null</pre>
	 */
	public void loadArrayLists() {
		xmlReader = new XmlReader();
		rules = new HlvlBasicFactory();
		xmlReader.loadXmlFile(params.getInputPath());
		importantXmlDependecy = xmlReader.getImportantXmlDependecy();
		xmlElements = xmlReader.getXmlElements();

	}
	/**
	 * This method is responsible for load the XML file and inicializate the
	 * importantXmlDependency and xmlElements arraylists using a string that
	 * represents the content of a xml file
	 * @param xml: string that represents the content of a xml file
	 */
	public void loadArrayLists(String xml) {
		xmlReader = new XmlReader();
		rules = new HlvlBasicFactory();
		xmlReader.loadXmlString(xml);
		importantXmlDependecy = xmlReader.getImportantXmlDependecy();
		xmlElements = xmlReader.getXmlElements();
	
	}

	/**
	 * This method is responsible for creating descomposition, implies and mutex (mutual exclusion)
	 * relations in HLVL code from the importantXmlDependency attribute
	 */
	public void converterXmlDependecyToHLVLCode() {
		HlvlCode.append(rules.getRelationsLab());
		converterGroupAndCore();
		for (int i = 0; i < importantXmlDependecy.size(); i++) {

			String target = getValidName(searchForName(importantXmlDependecy.get(i).getTarget()));
			String source = getValidName(searchForName(importantXmlDependecy.get(i).getSource()));
			String caso = importantXmlDependecy.get(i).getRelType();
			switch (caso) {
			case "mandatory":
				HlvlCode.append(TAB + rules.getDecomposition(source, target, DecompositionType.Mandatory));
				break;
			case "optional":
				HlvlCode.append(TAB + rules.getDecomposition(target, source, DecompositionType.Optional));
				break;
			case "requires":
				HlvlCode.append(TAB + rules.getImplies(source, target));
				break;
			case "excludes":
				HlvlCode.append(TAB + rules.getMutex(target, source));
				break;

			}
		}
	}

	/**
	 * This method is responsible for searching in xmlElements attribute the XML
	 * Element's name having the XML Element's id
	 * 
	 * @param id: string that represent the XML Element's id.
	 * @return String that represent the XML Element's name.
	 */
	public String searchForName(String id) {
		return xmlElements.get(id).getName();
	}

	/**
	 * This method is responsible for creating the declaration for a boolean
	 * element in HLVL and adding it to the HlvlCode attribute
	 */
	public void converterXmlElementToHLVLCode() {

		for (Entry<String, Element> entry : xmlElements.entrySet()) {
			String name = getValidName(entry.getValue().getName());
			if (!name.equals("bundle")) {
				HlvlCode.append(TAB + rules.getElement(name));
			}
		}

	}

	/**
	 * This method is responsible for creating coreElements and group relations in HLVL and
	 * adding them to the HlvlCode attribute
	 */
	public void converterGroupAndCore() {

		for (Entry<String, Element> entry : xmlElements.entrySet()) {
			String name = getValidName(entry.getValue().getName());
			String caso = entry.getValue().getType();
			switch (caso) {
			case "root":
				HlvlCode.append(TAB + rules.getCommon(name));
				break;
			case "bundle":
				if (entry.getValue().getBundleType().endsWith("XOR"))
					HlvlCode.append(TAB + rules.getGroup(findRootBundle(entry.getValue()),
							findGroupsElements(entry.getValue()), GroupType.Xor));
				else
					HlvlCode.append(TAB + rules.getGroup(findRootBundle(entry.getValue()),
							findGroupsElements(entry.getValue()), GroupType.Or));
				break;
			}
		}
	}

	/**
	 * This method is responsible for searching into the xmlElements attribute to find
	 * the root element from a bundle element
	 * 
	 * @param bundle: Element that represent a XML bundle.
	 * @return String that represent the XML Element's name that is root to the given bundle.
	 */
	public String findRootBundle(Element bundle) {
		String name = "";
		String id = bundle.getId();
		for (int i = 0; i < importantXmlDependecy.size(); i++) {
			if (id.equals(importantXmlDependecy.get(i).getSource())) {
				name = searchForName(importantXmlDependecy.get(i).getTarget());
				break;
			}
		}
		name = getValidName(name);
		return name;
	}

	/**
	 * This method is responsible for searching into the xmlElements attribute the
	 * elements which are grouped by a bundle element
	 * 
	 * @param bundle: Element that represent a XML bundle.
	 * @return ArrayList that contains the names of all elements grouped by the bundle given
	 */
	public ArrayList<String> findGroupsElements(Element bundle) {

		ArrayList<String> result = new ArrayList<String>();
		String id = bundle.getId();
		for (int i = 0; i < importantXmlDependecy.size(); i++) {
			if (id.equals(importantXmlDependecy.get(i).getTarget())) {
				String name = getValidName(searchForName(importantXmlDependecy.get(i).getSource()));
				result.add(name);
			}
		}
		return result;
	}

	/**
	 * This method is responsible for ensure the correct format of an item name
	 * 
	 * @param name: name of an Element.
	 * @return String: name in the valid format
	 */
	@Override
	public String getValidName(String name) {
		return name.replaceAll(" ", "_").replaceAll("\\-", "Minus").replaceAll("\\+", "Plus").replaceAll("\\.", "dot")
				.replaceAll("/", "");
	}

	/**
	 * This method is responsible creating the HLVL code from a xml file and to persist the HLVL code 
	 * <pre>params != null and params.inputPath != null and params.outputPath != null and params.targetName != null</pre>
	 */
	@Override
	public void parse() throws Exception {
		loadArrayLists();
		HlvlCode.append(rules.getHeader(params.getTargetName()+"_generated"));
		converterXmlElementToHLVLCode();
		converterXmlDependecyToHLVLCode();
		writeFile();
	}

	@Override
	public String parse(String data) throws Exception {
		loadArrayLists(data);
		//FIXME consider to include a timestamp in the id of the model 
		HlvlCode.append(rules.getHeader("Auto_generated"));
		converterXmlElementToHLVLCode();
		converterXmlDependecyToHLVLCode();
		return  HlvlCode.toString();
	}

	public ArrayList<Dependency> getImportantXmlDependecy() {
		return importantXmlDependecy;
	}

	public void setImportantXmlDependecy(ArrayList<Dependency> importantXmlDependecy) {
		this.importantXmlDependecy = importantXmlDependecy;
	}

	public HashMap<String, Element> getXmlElements() {
		return xmlElements;
	}

	public void setXmlElements(HashMap<String, Element> xmlElements) {
		this.xmlElements = xmlElements;
	}

	public XmlReader getXmlReader() {
		return xmlReader;
	}

	public void setXmlReader(XmlReader xmlReader) {
		this.xmlReader = xmlReader;
	}

	public ParsingParameters getParams() {
		return params;
	}

	public void setParams(ParsingParameters params) {
		this.params = params;
	}

	public StringBuilder getHlvlCode() {
		return HlvlCode;
	}

	public void setHlvlCode(StringBuilder hlvlCode) {
		HlvlCode = hlvlCode;
	}

	public IHlvlBasicFactory getRules() {
		return rules;
	}

	public void setRules(IHlvlBasicFactory rules) {
		this.rules = rules;
	}
	
	
	
	
}
