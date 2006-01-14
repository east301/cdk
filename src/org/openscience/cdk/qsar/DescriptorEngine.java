/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2004-2005  The Chemistry Development Kit (CDK) project
 *
 * Contact: cdk-devel@lists.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package org.openscience.cdk.qsar;

import org.openscience.cdk.dict.DictionaryDatabase;
import org.openscience.cdk.dict.Entry;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.AtomContainer;
import org.openscience.cdk.tools.LoggingTool;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
 * A class that provides access to automatic descriptor calculation and more.
 * <p/>
 * The aim of this class is to provide an easy to use interface to automatically evaluate
 * all the CDK descriptors for a given molecule. Note that at a given time this class
 * will evaluate all <i>atomic</i> or <i>molecular</i> descriptors but not both.
 * <p/>
 * The available descriptors are determined by scanning all the jar files in the users CLASSPATH
 * and selecting classes that belong to the CDK QSAR atomic or molecular descriptors package.
 * <p/>
 * An example of its usage would be
 * <pre>
 * Molecule someMolecule;
 * ...
 * DescriptorEngine descriptoEngine = new DescriptorEngine(DescriptorEngine.MOLECULAR);
 * descriptorEngine.process(someMolecule);
 * </pre>
 * The class allows the user to obtain a List of all the available descriptors in terms of their
 * Java class names as well as instances of each descriptor class.   For each descriptor, it is possible to
 * obtain its classification as described in the CDK descriptor-algorithms OWL dictionary.
 *
 * @cdk.created 2004-12-02
 * @cdk.module qsar
 * @see DescriptorSpecification
 */
public class DescriptorEngine {

    public static final int ATOMIC = 1;
    public static final int MOLECULAR = 2;

    private DictionaryDatabase dictDB = null;
    private List classNames = null;
    private List descriptors = null;
    private List speclist = null;
    private LoggingTool logger;

    /**
     * Constructor that generates a list of descriptors to calculate.
     * <p/>
     * All available descriptors are included in the list of descriptors to
     * calculate
     */
    public DescriptorEngine(int type) {
        logger = new LoggingTool(this);
        switch (type) {
            case ATOMIC:
                classNames = getDescriptorClassNameByPackage("org.openscience.cdk.qsar.descriptors.atomic");
                break;
            case MOLECULAR:
                classNames = getDescriptorClassNameByPackage("org.openscience.cdk.qsar.descriptors.molecular");
                break;
        }
        instantiateDescriptors(classNames);
        initializeSpecifications(descriptors);

        // get the dictionary for the descriptors
        dictDB = new DictionaryDatabase();
    }

    /**
     * Constructor that generates a list of descriptors to calculate.
     *
     * @param descriptorClasses A String array containing one or more of the above elements
     * @deprecated
     */
    public DescriptorEngine(String[] descriptorClasses) {
        this(DescriptorEngine.MOLECULAR);
    }

    /**
     * Calculates all available (or only those specified) descriptors for a molecule.
     * <p/>
     * The results for a given descriptor as well as associated parameters and
     * specifications are used to create a <code>DescriptorValue</code>
     * object which is then added to the molecule as a property keyed
     * on the <code>DescriptorSpecification</code> object for that descriptor
     *
     * @param molecule The molecule for which we want to calculate descriptors
     * @throws CDKException if an error occured during descriptor calculation
     */
    public void process(AtomContainer molecule) throws CDKException {

        for (int i = 0; i < descriptors.size(); i++) {
            Descriptor descriptor = (Descriptor) descriptors.get(i);
            try {
                DescriptorValue value = descriptor.calculate(molecule);
                molecule.setProperty(speclist.get(i), value);
            } catch (CDKException exception) {
                logger.error("Could not calculate descriptor value for: ", descriptor.getClass().getName());
                logger.debug(exception);
                throw new CDKException("Could not calculate descriptor value for: " + descriptor.getClass().getName(), exception);
            }
        }
    }

    /**
     * Returns the type of the descriptor as defined in the descriptor dictionary.
     * <p/>
     * The method will look for the identifier specified by the user in the QSAR descriptor
     * dictionary. If a corresponding entry is found, first child element that is called
     * "isClassifiedAs" is returned. Note that the OWL descriptor spec allows both the type of
     * descriptor (electronic, topological etc) as well as the class of descriptor (molecular, atomic)
     * to be specified in an "isClassifiedAs" element. Thus we ignore any such element that
     * indicates the descriptors class (since we know it from its package location)
     * <p/>
     * The method assumes that any descriptor entry will have only one "isClassifiedAs" entry describing
     * the descriptors type.
     * <p/>
     * The descriptor can be identified either by the name of the class implementing the descriptor
     * or else the specification reference value of the descriptor which can be obtained from an instance
     * of the descriptor class.
     *
     * @param identifier A String containing either the descriptors class name or else the descriptors
     *                   specification reference
     * @return The type of the descriptor as stored in the dictionary, null if no entry is found matching
     *         the supplied identifier
     */
    public String getDictionaryType(String identifier) {

        Entry[] dictEntries = dictDB.getDictionaryEntry("descriptor-algorithms");

        String specRef = null;

        // see if we got a descriptors java class name
        for (int i = 0; i < classNames.size(); i++) {
            String className = (String) classNames.get(i);
            if (className.equals(identifier)) {
                Descriptor descriptor = (Descriptor) descriptors.get(i);
                DescriptorSpecification descSpecification = descriptor.getSpecification();
                specRef = descSpecification.getSpecificationReference();
            }
        }

        // if we are here we have a SpecificationReference
        if (specRef == null) {
            String[] tmp = identifier.split(":");
            specRef = tmp[2];
        }


        for (int j = 0; j < dictEntries.length; j++) {
            if (dictEntries[j].getID().equals(specRef.toLowerCase())) {
                Vector metaData = dictEntries[j].getDescriptorMetadata();
                for (Iterator iter = metaData.iterator(); iter.hasNext();) {
                    String metaDataEntry = (String) iter.next();
                    String[] values = metaDataEntry.split("/");

                    if (values == null) continue;
                    if (values.length != 2) continue;
                    if (!values[0].startsWith("qsar-descriptors-metadata") ||
                            !values[1].startsWith("qsar-descriptors-metadata")) continue;

                    String[] dictRef = values[0].split(":");
                    String[] content = values[1].split(":");

                    if (dictRef[1].toLowerCase().equals("descriptortype")) return content[1];
                }
            }
        }
        return null;
    }

    /**
     * Returns the type of the decsriptor as defined in the descriptor dictionary.
     * <p/>
     * The method will look for the identifier specified by the user in the QSAR descriptor
     * dictionary. If a corresponding entry is found, the meta-data list is examined to
     * look for a dictRef attribute that contains a descriptorType value. if such an attribute is
     * found, the value of the contents attribute  is returned.
     * <p/>
     * The method assumes that any descriptor entry will have only one dictRef attribute with
     * a value of <i>  qsar-descriptors-metadata:descriptorType</i>.
     * <p/>
     * The descriptor can be identified it DescriptorSpecification object
     *
     * @param descriptorSpecification A DescriptorSpecification object
     * @return he type of the descriptor as stored in the dictionary, null if no entry is found matching
     *         the supplied identifier
     */
    public String getDictionaryType(DescriptorSpecification descriptorSpecification) {
        return getDictionaryType(descriptorSpecification.getSpecificationReference());
    }

    /**
     * Returns the class(es) of the decsriptor as defined in the descriptor dictionary.
     * <p/>
     * The method will look for the identifier specified by the user in the QSAR descriptor
     * dictionary. If a corresponding entry is found, the meta-data list is examined to
     * look for a dictRef attribute that contains a descriptorClass value. if such an attribute is
     * found, the value of the contents attribute  add to a list. Since a descriptor may be classed in
     * multiple ways (geometric and electronic for example), in general, a given descriptor will
     * have multiple classes associated with it.
     * <p/>
     * The descriptor can be identified either by the name of the class implementing the descriptor
     * or else the specification reference value of the descriptor which can be obtained from an instance
     * of the descriptor class.
     *
     * @param identifier A String containing either the descriptors class name or else the descriptors
     *                   specification reference
     * @return A List containing the names of the QSAR descriptor classes that this  descriptor was declared
     *         to belong to. If an entry for the specified identifier was not found, null is returned.
     */
    public String[] getDictionaryClass(String identifier) {


        Entry[] dictEntries = dictDB.getDictionaryEntry("qsar-descriptors");

        String specRef = null;

        // see if we got a descriptors java class name
        for (int i = 0; i < classNames.size(); i++) {
            String className = (String) classNames.get(i);
            if (className.equals(identifier)) {
                Descriptor descriptor = (Descriptor) descriptors.get(i);
                DescriptorSpecification descSpecification = descriptor.getSpecification();
                specRef = descSpecification.getSpecificationReference();
            }
        }

        // if we are here and specRef==null we have a SpecificationReference
        if (specRef == null) {
            String[] tmp = identifier.split(":");
            specRef = tmp[2];
        }

        List dictClasses = new ArrayList();

        for (int j = 0; j < dictEntries.length; j++) {
            if (dictEntries[j].getID().equals(specRef.toLowerCase())) {
                Vector metaData = dictEntries[j].getDescriptorMetadata();
                for (Iterator iter = metaData.iterator(); iter.hasNext();) {
                    String[] values = ((String) iter.next()).split("/");

                    if (values.length != 2) continue;
                    if (!values[0].startsWith("qsar-descriptors-metadata") ||
                            !values[1].startsWith("qsar-descriptors-metadata")) continue;

                    String[] dictRef = values[0].split(":");
                    String[] content = values[1].split(":");
                    if (dictRef[1].equals("descriptorClass")) dictClasses.add(content[1]);
                }
            }
        }
        if (dictClasses.size() == 0) return null;
        else
            return (String[]) dictClasses.toArray(new String[]{});
    }

    /**
     * Returns the class(es) of the decsriptor as defined in the descriptor dictionary.
     * <p/>
     * The method will look for the identifier specified by the user in the QSAR descriptor
     * dictionary. If a corresponding entry is found, the meta-data list is examined to
     * look for a dictRef attribute that contains a descriptorClass value. if such an attribute is
     * found, the value of the contents attribute  add to a list. Since a descriptor may be classed in
     * multiple ways (geometric and electronic for example), in general, a given descriptor will
     * have multiple classes associated with it.
     * <p/>
     * The descriptor can be identified by its DescriptorSpecification object.
     *
     * @param descriptorSpecification A DescriptorSpecification object
     * @return A List containing the names of the QSAR descriptor classes that this  descriptor was declared
     *         to belong to. If an entry for the specified identifier was not found, null is returned.
     */

    public String[] getDictionaryClass(DescriptorSpecification descriptorSpecification) {
        return getDictionaryClass(descriptorSpecification.getSpecificationReference());
    }

    /**
     * Returns the DescriptorSpecification objects for all available descriptors.
     *
     * @return An array of <code>DescriptorSpecification</code> objects. These are the keys
     *         with which the <code>DescriptorValue</code> objects can be obtained from a
     *         molecules property list
     */
    public List getDescriptorSpecifications() {
        return (speclist);
    }

    /**
     * Returns a list containing the names of the classes implementing the descriptors.
     *
     * @return A list of class names.
     */
    public List getDescriptorClassNames() {
        return classNames;
    }

    /**
     * Returns a List containing the instantiated descriptor classes.
     *
     * @return A List containing descriptor classes
     */
    public List getDescriptorInstances() {
        return descriptors;
    }

    /**
     * Get the all the unique dictionary classes that the descriptors belong to.
     *
     * @return An array containing the unique dictionary classes.
     */
    public String[] getAvailableDictionaryClasses() {
        List classList = new ArrayList();
        for (Iterator iter = speclist.iterator(); iter.hasNext();) {
            DescriptorSpecification spec = (DescriptorSpecification) iter.next();
            String[] tmp = getDictionaryClass(spec);
            if (tmp != null) classList.addAll(Arrays.asList(tmp));
        }
        Set uniqueClasses = new HashSet(classList);
        return (String[]) uniqueClasses.toArray(new String[]{});
    }

    /**
     * Returns a list containing the classes found in the specified descriptor package.
     * <p/>
     * The package name specified can be null or an empty string. In this case the package name
     * is automatcally set to "org.openscience.cdk.qsar.descriptors" and as a result will return
     * classes corresponding to both atomic and molecular descriptors.
     *
     * @param packageName The name of the package containing the required descriptor
     * @return A list containing the classes in the specified package
     */
    private List getDescriptorClassNameByPackage(String packageName) {

        if (packageName == null || packageName.equals("")) {
            packageName = "org.openscience.cdk.qsar.descriptors";
        }

        String classPath = System.getProperty("java.class.path");
        String[] jars = classPath.split(File.pathSeparator);
        ArrayList classlist = new ArrayList();

        for (int i = 0; i < jars.length; i++) {
            JarFile j;
            try {
                j = new JarFile(jars[i]);
                Enumeration e = j.entries();
                while (e.hasMoreElements()) {
                    JarEntry je = (JarEntry) e.nextElement();
                    if (je.toString().indexOf(".class") != -1) {
                        String tmp = je.toString().replace('/', '.').replaceAll(".class", "");
                        if (!(tmp.indexOf(packageName) != -1)) continue;
                        if (tmp.indexOf("$") != -1) continue;
                        classlist.add(tmp);
                    }
                }
            } catch (IOException e) {
                logger.error("Error opening the jar file: " + jars[i]);
                logger.debug(e);
            }
        }
        return classlist;
    }

    private void instantiateDescriptors(List descriptorClassNames) {
        if (descriptors != null) return;

        descriptors = new ArrayList();
        for (Iterator iter = descriptorClassNames.iterator(); iter.hasNext();) {
            String descriptorName = (String) iter.next();
            try {
                Descriptor descriptor = (Descriptor) this.getClass().getClassLoader().loadClass(descriptorName).newInstance();
                descriptors.add(descriptor);
                logger.info("Loaded descriptor: ", descriptorName);
            } catch (ClassNotFoundException exception) {
                logger.error("Could not find this Descriptor: ", descriptorName);
                logger.debug(exception);
            } catch (Exception exception) {
                logger.error("Could not load this Descriptor: ", descriptorName);
                logger.debug(exception);
            }
        }
    }

    private void initializeSpecifications(List descriptors) {
        speclist = new Vector();
        for (int i = 0; i < descriptors.size(); i++) {
            Descriptor descriptor = (Descriptor) descriptors.get(i);
            speclist.add(descriptor.getSpecification());
        }
    }
}

