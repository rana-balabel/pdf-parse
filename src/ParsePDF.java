/*
 * Rana Balabel
 * input file is a 246 page pdf of institutions and their MICR, routing number, and branch address
 * this class will take the text from the pdf using PDFBox. logic is used to break down the text to rows of data.
 * data is then written to a CSV for company use
 * 
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.text.WordUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import com.opencsv.CSVWriter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.String;

public class ParsePDF {
    public static void main(String[] args) throws Exception {
        // insert directory of local pdf
        File file = new File("D:\\22-10-14-MBRBNKSN.pdf");
        FileInputStream fis = new FileInputStream(file);
        PDDocument pdfDocument = PDDocument.load(fis);
        int pdfPageNumber = pdfDocument.getPages().getCount();

        // creates a new PDF text stripper object to strip pdf contents into long string
        PDFTextStripper pdfTextStripper = new PDFTextStripper();
        // set to read the entire pdf
        pdfTextStripper.setStartPage(1);
        pdfTextStripper.setEndPage(pdfPageNumber);
        String docText = pdfTextStripper.getText(pdfDocument);

        // function will return a map containing several string LISTS (List<String>)
        // each list is a collection of the SAME insitution, with its rows of data to
        // insert in the form of:
        // { NAME, MICR, ROUTING NUMBER, ADDRESS, NAME, MICR, ROUTING NUMBER, ADDRESS}
        // for however many lines of data there are
        Map<String, List<String>> returnedCleanData = separateBanks(docText, pdfPageNumber);

        // // write the final data to a CSV already existing locally
        writeFinalDataToCSV("D:\\ApparaCSVs\\ParsedPDF.csv", returnedCleanData);

        // close the open pointers
        pdfDocument.close();
        fis.close();
    }

    /*
     * function: writeFinalDataToCSV
     * parameters: filePath to the CSV to write in to in the form of a String, and
     * the data to insert in the form of a Map<String, List<String>>
     */
    public static void writeFinalDataToCSV(String filePath, Map<String, List<String>> dataToInsert) {
        File file = new File(filePath);
        try {
            // create FileWriter object with file as parameter
            FileWriter outputfile = new FileWriter(file);

            // create CSVWriter object filewriter object as parameter
            CSVWriter writer = new CSVWriter(outputfile);

            // adding header to csv
            String[] header = { "Institution Name", "MICR", "Routing Numbers", "Address", "Unit", "Street", "City",
                    "Province", "Postal Code" };
            writer.writeNext(header);

            // there are 9 fields to insert
            String[] oneRowofData = new String[9];
            // variable to track where we are in the current data object (index #)
            int indexNumber = 0;
            // note that the map keys are identified as row1, row2, row3, etc for each
            // collection of banks
            for (int i = 1; i <= dataToInsert.size(); i++) {
                indexNumber = 0; // reset the index of where we're at within the object at the start of every new
                                 // bank collection
                // the datasets come in multiples of 9 (9 columns per row). loop through the
                // number of rows to insert by dividing number of data by 9
                for (int x = 0; x < (dataToInsert.get("row" + i).size() / 9); x++) {
                    // populate the oneRowofData 9 indices with the indices in the map. traverse
                    // through the entire object until the number of rows is complete
                    for (int rowNumber = 0; rowNumber < 9; rowNumber++) {
                        oneRowofData[rowNumber] = dataToInsert.get("row" + i).get(indexNumber);
                        indexNumber++;
                    }
                    // after populating one row, write it into the CSV
                    writer.writeNext(oneRowofData);
                }
                System.out.println(" End of one collection of banks \n");
            }
            // closing writer connection
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * function: separateBanks
     * parameters: long string of document text, total number of pages in the pdf
     * returns final data to insert in the form of a map: { bank1 and all its
     * associative data, bank2 and all its associative data, etc. }
     */
    public static Map<String, List<String>> separateBanks(String text, int pdfPageNumber) {
        // split the long string based on the presence of the 3 digit code at the end of
        // each section
        String banks[] = text.split("\s\s\s\s[0-9]{3}", 0);
        // array to store all bank names
        String bankNames[] = new String[banks.length];
        int indexOfLastLine = 0;
        String informationArray[] = new String[banks.length];

        // we don't have a precise number on how many locations each bank will have, so
        // using a List<String> we can append the rows dynamically
        List<String> rowOfData = new ArrayList<>();
        Map<String, List<String>> finalMap = new HashMap();
        for (int i = 0; i < banks.length; i++) {
            // first index is only an individual bank name, save it as first index in
            // bankNames array
            bankNames[0] = banks[0];

            // clean up unneeded text from data
            banks[i] = banks[i].replace(
                    "Routing Numbers /",
                    "");
            banks[i] = banks[i].replace(
                    "Numéros d'acheminement",
                    "");
            banks[i] = banks[i].replace(
                    "Electronic Paper(MICR)",
                    "");
            banks[i] = banks[i].replace(
                    "Électronique Papier(MICR) Postal Address - Addresse postale",
                    "");
            banks[i] = banks[i].replace(
                    "- continued",
                    "");
            banks[i] = banks[i].replace(
                    "SECTION I NUMERIC LIST / LISTE NUMÉRIQUE",
                    "");

            // save the rest of bank names in this array. the last line of each section
            // contains the bank name partaining to the next section
            if (i > 0) {
                indexOfLastLine = banks[i].lastIndexOf("\n");
                // using (i-1) since the first information section appears for banks[i] when i=1
                // below separates the information from the last line in the section ( last line
                // is next bank's name)
                informationArray[i - 1] = banks[i].substring(0, indexOfLastLine);
                bankNames[i] = banks[i].substring(banks[i].lastIndexOf("\n"));
                if (informationArray[i - 1] != null) {
                    Map<String, List<String>> addressAndNumbersSeparated = separateAddressInformation(
                            informationArray[i - 1],
                            pdfPageNumber);
                    // the logic here is to loop through all the indices in the string arrays and
                    // add it to one row of data. this row of data is
                    // then passed onto a final map. both returned objects in
                    // addressAndNumbersSeparated map share the same size
                    for (int x = 0; x < addressAndNumbersSeparated.get("numbersUnseparated").size(); x++) {
                        rowOfData.add(bankNames[i - 1].trim());
                        // index 0 of returned array is MICR, index 1 of returned array is routing #
                        String[] MICRandRouting = findMICRandRoutingNumbers(
                                addressAndNumbersSeparated.get("numbersUnseparated").get(x));
                        rowOfData.add(MICRandRouting[0]);
                        rowOfData.add(MICRandRouting[1]);
                        // returns original address before any parsing
                        rowOfData.add(addressAndNumbersSeparated.get("addressUnseparated").get(x));
                        // returns parsed data in the form of an array
                        String[] parsedAddress = separateAddresses(
                                addressAndNumbersSeparated.get("addressUnseparated").get(x));
                        rowOfData.add(parsedAddress[0]); // suite
                        rowOfData.add(parsedAddress[1]); // street
                        rowOfData.add(parsedAddress[2]); // city
                        rowOfData.add(parsedAddress[3]); // province
                        rowOfData.add(parsedAddress[4]); // postal
                    }
                    finalMap.put("row" + i, rowOfData);
                    rowOfData = new ArrayList<>(); // reset arraylist after insertion
                }
            }
        }

        return finalMap;

    }

    /*
     * function: regExIndex
     * checks if a regEx pattern is present in a passed string. returns indices of
     * where the pattern occurs if found. returns -1 if not found
     */
    public static int[] regExIndex(String pattern, String text, Integer fromIndex) {
        Matcher matcher = Pattern.compile(pattern).matcher(text);
        if ((fromIndex != null && matcher.find(fromIndex)) || matcher.find()) {
            return new int[] { matcher.start(), matcher.end() };
        }

        // System.out.println(text);
        return new int[] { -1, -1 };
    }

    /*
     * checks if passed string contains a postal code
     */
    public static boolean addressContainsPostalCode(String lastTenChars) {
        int[] contains = regExIndex("[a-zA-Z][0-9]{1}[a-zA-Z]\s[0-9]{1}[a-zA-Z][0-9]{1}", lastTenChars, 0);
        if (contains[0] != -1)
            return true;
        else
            return false;
    }

    /*
     * checks if passed string is a two letter postal code
     */
    public static boolean lastCharsAreProvince(String lastTwoChars) {
        int[] contains = regExIndex("\s*[a-zA-Z][a-zA-Z]\s*", lastTwoChars, 0);
        if (contains[0] != -1)
            return true;
        else
            return false;
    }

    /*
     * checks if passed string is only half of a postal code and needs to be
     * concatenated with its other half (special edge case)
     */
    public static boolean addressContainPartPostalCode(String lastTenChars) {
        int[] contains = regExIndex("[a-zA-Z][0-9]{1}[a-zA-Z]", lastTenChars, 0);
        if (contains[0] != -1)
            return true;
        else
            return false;
    }

    /*
     * function: separateAddresses
     * main address parser function. takes in an unparsed address string and returns
     * a string array of parsed address in the form of { unit, street, city,
     * province, postal code}
     */
    public static String[] separateAddresses(String unsepAddress) {
        // convert address first to sentence case:
        final char[] delimeters = { ' ', '-' };
        unsepAddress = WordUtils.capitalizeFully(unsepAddress, delimeters);

        // catches case where someone inputted a unit as a hashtag
        if (unsepAddress.charAt(0) == '#')
            unsepAddress = unsepAddress.replaceFirst("#", "Unit ");
        String[] finalParsedAddress = new String[5];
        for (int k = 0; k < 5; k++) {
            finalParsedAddress[k] = ""; // initializes values as some parameters are optional
        }
        // passes last 10 chars of address to check if it contains a postal & province
        if (addressContainsPostalCode(unsepAddress.substring(unsepAddress.length() - 10))) {
            // separate by comma everything except the last 10 characters because we know
            // those are the province and postal code
            if (unsepAddress.length() > 10) {
                String[] splitByComma = unsepAddress.substring(0, unsepAddress.length() - 10).split("\\s*,\\s*");
                String provinceAndPostal = unsepAddress.substring(unsepAddress.length() - 10);
                String[] separated = provinceAndPostal.split("\s");
                // separated should now contain something in the form of ["ON","XXX","XXX"]
                if (separated.length > 1) {
                    // populate postal code index
                    if (separated.length >= 3) {
                        finalParsedAddress[4] = separated[1].toUpperCase() + ' ' + separated[2].toUpperCase();
                    } else {
                        finalParsedAddress[4] = separated[1].toUpperCase(); // to catch cases where the line passed
                                                                            // doesn't have a complete postal code
                    }
                }
                // store province in province index
                finalParsedAddress[3] = separated[0].toUpperCase();

                // initial boolean vals
                boolean isSuite = false;
                boolean isCity = false;
                // loop through the rest of the string passed now (excluding last 10 chars)
                for (int x = 0; x < splitByComma.length; x++) {
                    splitByComma[x] = splitByComma[x].trim();
                    if (splitByComma[x].contains("Suite")) {
                        // find suite number
                        String[] streetTemp = splitByComma[x].split("Suite");
                        if (streetTemp.length > 1) {
                            // check if any of these suite formats exist in the string
                            int[] startEndIndex = regExIndex(
                                    "[a-zA-Z]-[0-9]{1,4}|[0-9]{1,4}\s*&\s*[0-9]{1,4}|[0-9]{1,4}-[0-9]{1,4}|[a-zA-Z][0-9]{1,4}-[0-9]{1,4}|[a-zA-Z][0-9]{1,4}[a-zA-Z]|[a-zA-Z][a-zA-Z]-[0-9]{1,4}|[a-zA-Z][a-zA-Z][0-9]{1,4}|[a-zA-Z]-[0-9]{1,4}|[a-zA-Z][0-9]{1,4}|[0-9][a-zA-Z]|[a-zA-Z]|[0-9]{1,4}",
                                    streetTemp[1], 0);
                            // populate suite index if regex pattern is found
                            if (startEndIndex[0] != -1) {
                                finalParsedAddress[0] = "Suite "
                                        + streetTemp[1].substring(startEndIndex[0], startEndIndex[1]).toUpperCase();
                                // if after splitting based on the word "Suite" the first index is an empty
                                // string, then the index with the unit number contained further address
                                // information
                                if (streetTemp[0] == "") {
                                    // store remaining data in street column
                                    finalParsedAddress[1] += streetTemp[1].substring(startEndIndex[1],
                                            streetTemp[1].length())
                                            + ", ";
                                } else {
                                    // if index 0 was non empty, then there was data before the word Suite.
                                    // concatenate it as part of street as well
                                    finalParsedAddress[1] += streetTemp[0].trim()
                                            + streetTemp[1].substring(startEndIndex[1],
                                                    streetTemp[1].length()).trim()
                                            + ", ";
                                }

                                isSuite = true;
                            } else {

                                isSuite = false;
                            }
                        }
                        // below case is more complicated, since forms of the word Unit can exist as
                        // bank names. make sure the word Unit exists as a non-title
                    } else if (splitByComma[x].contains("Unit") &&
                            regExIndex("Units\s*[A-Z][a-zA-z]", splitByComma[x], 0)[0] == -1 &&
                            regExIndex("Unite\s*[A-Z][a-zA-z]", splitByComma[x], 0)[0] == -1 &&
                            regExIndex("Unit\s*[A-Z][a-zA-z]", splitByComma[x], 0)[0] == -1 &&
                            regExIndex("Unity\s*[A-Z][a-zA-z]", splitByComma[x], 0)[0] == -1

                    ) {
                        // repeat the same process but for Unit numbers
                        splitByComma[x] = splitByComma[x].replace("Units", "Unit");
                        splitByComma[x] = splitByComma[x].replace("Unite", "Unit");
                        String[] streetTemp = splitByComma[x].split("Unit");
                        if (streetTemp.length > 1) {
                            int[] startEndIndex = regExIndex(
                                    "[0-9][A-Z]-[0-9]{1,4}|[a-zA-Z]-[0-9]{1,4}|[0-9]{1,4}\s*&\s*[0-9]{1,4}|[0-9]{1,4}-[0-9]{1,4}|[a-zA-Z][0-9]{1,4}-[0-9]{1,4}|[a-zA-Z][0-9]{1,4}[a-zA-Z]|[a-zA-Z][a-zA-Z]-[0-9]{1,4}|[a-zA-Z][a-zA-Z][0-9]{1,4}|[a-zA-Z]-[0-9]{1,4}|[a-zA-Z][0-9]{1,4}|[0-9][a-zA-Z]|[A-Z]|[0-9]{1,4}",
                                    streetTemp[1], 0);// find location of
                            // suite number
                            if (startEndIndex[0] != -1) {
                                finalParsedAddress[0] = "Unit "
                                        + streetTemp[1].substring(startEndIndex[0], startEndIndex[1]).toUpperCase();
                                if (streetTemp[0] == "") {
                                    finalParsedAddress[1] += streetTemp[1].substring(startEndIndex[1],
                                            streetTemp[1].length())
                                            + ", ";
                                } else {
                                    finalParsedAddress[1] += streetTemp[0].trim()
                                            + streetTemp[1].substring(startEndIndex[1],
                                                    streetTemp[1].length()).trim()
                                            + ", ";
                                }

                                isSuite = true;
                            } else {

                                isSuite = false;
                            }
                        }
                    } else {
                        isSuite = false;
                    }
                    if (x == splitByComma.length - 1) // last index is city
                    {
                        finalParsedAddress[2] = splitByComma[x];
                        isCity = true;
                    } else {
                        isCity = false;
                    }
                    // if all else fails, then concatenate it to street
                    if (!isCity && !isSuite) {
                        finalParsedAddress[1] += splitByComma[x] + ", ";
                    }

                    isCity = false;
                    isSuite = false;
                }
                // remove tail comma from street
                if (finalParsedAddress[1].length() > 2) {
                    finalParsedAddress[1] = finalParsedAddress[1].substring(0,
                            finalParsedAddress[1].length() - 2);
                }

            } else {
                finalParsedAddress[2] = unsepAddress; // address is less than 10 characters. assume it to be just the
                                                      // city
            }
        } else { // line does not contain a postal code
            // split line by commas
            String[] findRemainingData = unsepAddress.split("\\s*,\\s*");
            for (int j = 0; j < findRemainingData.length; j++) {
                // last index is province
                if (j == findRemainingData.length - 1) {
                    finalParsedAddress[3] = findRemainingData[j].toUpperCase();
                } else if (j == findRemainingData.length - 2) // second last index is city
                {
                    finalParsedAddress[2] = findRemainingData[j];
                } else { // remaining is the street
                    finalParsedAddress[1] += findRemainingData[j] + ", ";
                }
            }
            if (finalParsedAddress[1].length() > 2) { // remove tail comma
                finalParsedAddress[1] = finalParsedAddress[1].substring(0,
                        finalParsedAddress[1].length() - 2);
            }
        }

        // clean final form of parsed address
        for (int cleaning = 0; cleaning < finalParsedAddress.length; cleaning++) {
            finalParsedAddress[cleaning] = finalParsedAddress[cleaning].trim();
            if (finalParsedAddress[cleaning] != "") {
                if (finalParsedAddress[cleaning].contains("*csm*")) {
                    finalParsedAddress[cleaning] = finalParsedAddress[cleaning].replace("*csm*", "");
                }
                if (finalParsedAddress[cleaning].charAt(0) == ',') {
                    finalParsedAddress[cleaning] = finalParsedAddress[cleaning].replaceFirst(",", "");
                }
                if (finalParsedAddress[cleaning].charAt(finalParsedAddress[cleaning].length() - 1) == '-') {
                    finalParsedAddress[cleaning] = finalParsedAddress[cleaning].substring(0,
                            finalParsedAddress[cleaning].lastIndexOf("-") - 1);
                }
                if (finalParsedAddress[cleaning].charAt(finalParsedAddress[cleaning].length() - 1) == ',') {
                    finalParsedAddress[cleaning] = finalParsedAddress[cleaning].substring(0,
                            finalParsedAddress[cleaning].lastIndexOf(",") - 1);
                }
                if (finalParsedAddress[cleaning].charAt(0) == '-') {
                    finalParsedAddress[cleaning] = finalParsedAddress[cleaning].replaceFirst("-", "");
                }
            } else
                continue; // skip iteration if the index is empty (optional parameters like suite no.,
                          // etc.)
        }
        return finalParsedAddress;
    }

    // split numbers by space to find different data
    public static String[] findMICRandRoutingNumbers(String unsepNumber) {
        String[] splitBySpace = unsepNumber.split("\s");
        return splitBySpace;
    }

    /*
     * function: separateAddressInformation()
     * takes in a large string of unseparated MICRs, routing numbers, and addresses
     * classifies them into their data types and returns a map of each bank's
     * unseparated numbers and unseparated lines of address (before further parsing)
     * ie. returns one map in the form of { addressesUnseparated,
     * numbersUnseparated} partaining to one bank
     */
    public static Map<String, List<String>> separateAddressInformation(String information, int pages) {

        // split the string to multiple lines
        information = information.trim();
        String[] lines = information.split(System.lineSeparator());

        // separate the first 19 characters as they will always be the MICRs & routing #
        int charactersToSeparate = 19;
        int linesLength = 0;
        List<String> finalAddresses = new ArrayList<String>();
        List<String> separateNumbersFromAddress = new ArrayList<String>();
        String pageNumberasString = "" + pages;
        String numbersTemp = "";
        String addressesTemp = "";

        for (int i = 0; i < lines.length; i++) {
            lines[i] = lines[i].trim();
            linesLength = lines[i].length();

            if (linesLength > pageNumberasString.length() && linesLength > 19) { // makes sure we're not including a
                // page number as a line of data
                // if address does not contain a postal code and does not end in the words
                // (Sub), then the information
                // spreads across two consecutive lines. concatenate the first line with the
                // next lines content
                if (!addressContainsPostalCode(lines[i].substring(lines[i].length() - 10)) && (i != lines.length - 1)
                        && lines[i].lastIndexOf("(Sub") == -1) {

                    lines[i] = lines[i] + " " + lines[i + 1];

                    lines[i + 1] = ""; // clear out next line

                }

                // ignore the words (Sub to) if found
                if (lines[i].contains("(Sub to")) {
                    lines[i] = lines[i].substring(0, lines[i].lastIndexOf("(Sub"));

                }

                // to make things more uniform
                lines[i] = lines[i].replace("Suites", "Suite");
                lines[i] = lines[i].replace("Suite No.", "Suite");
                // clean unwanted string
                lines[i] = lines[i].replace("*Csm*", "");
                // update line length after above concatenation and replacements
                linesLength = lines[i].length();
                // store first 19 characters of this line to add to the numbers list
                numbersTemp = lines[i].substring(0, charactersToSeparate).trim();
                // store remaining info of this line to add to the addresses list
                addressesTemp = lines[i].substring(charactersToSeparate, linesLength).trim();
                // catches cases where someone inputted a unit number as a hashtag
                finalAddresses.add(addressesTemp);
                separateNumbersFromAddress.add(numbersTemp);
            }
        }

        // create a map of the two lists and return them
        Map<String, List<String>> map = new HashMap();
        map.put("addressUnseparated", finalAddresses);
        map.put("numbersUnseparated", separateNumbersFromAddress);

        return map;
    }

}