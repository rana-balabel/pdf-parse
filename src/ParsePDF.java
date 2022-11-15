/*
 * Rana Balabel
 * input file is a 246 page pdf of institutions and their MICR, routing number, and branch address
 * this class will take the text from the pdf using PDFBox. logic is used to break down the text to rows of data.
 * data is then written to a CSV for company use
 * 
 * Referenced library depandencies (.jar): 
 * openCSV, pdfbox, fontbox, commons-logging
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.text.WordUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import com.opencsv.CSVWriter;
import com.opencsv.bean.RegexToBeanField;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.String;

public class ParsePDF {
    public static void main(String[] args) throws Exception {
        File file = new File("D:\\22-10-14-MBRBNKSN.pdf");
        FileInputStream fis = new FileInputStream(file);
        PDDocument pdfDocument = PDDocument.load(fis);
        int pdfPageNumber = pdfDocument.getPages().getCount();

        // creates a new PDF text stripper object to strip pdf contents into one long
        // string
        PDFTextStripper pdfTextStripper = new PDFTextStripper();
        pdfTextStripper.setStartPage(1); // comment this line if you want to read the entire document
        pdfTextStripper.setEndPage(pdfPageNumber); // comment this line if you want to read the entire document
        String docText = pdfTextStripper.getText(pdfDocument);

        Map<String, List<String>> returnedCleanData = new HashMap();
        // // function will return a map containing several List<String> objects
        // // each object is a collection of the SAME insitution, with its rows of data
        // to
        // // insert in the form of:
        // // { NAME, MICR, ROUTING NUMBER, ADDRESS, NAME, MICR, ROUTING NUMBER,
        // ADDRESS}
        // // for however many lines of data there are
        returnedCleanData = separateBanks(docText, pdfPageNumber);

        // // write the final data to a CSV already existing locally
        writeFinalDataToCSV("D:\\ApparaCSVs\\Test.csv", returnedCleanData);

        // String[] address = separateAddresses(
        // "Chateauguay, 129, boul. d'Anjou, Chateauguay");
        // for (int i = 0; i < address.length; i++) {
        // System.out.println(address[i]);
        // }
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
            String[] oneRowofData = new String[9];
            // variable to track where we are in the current data object (first index,
            // second, etc)
            int indexNumber = 0;
            // add data to csv
            // note that the object keys are identified as row1, row2, row3, etc for each
            // collection of banks
            for (int i = 1; i <= dataToInsert.size(); i++) {
                indexNumber = 0; // reset the index of where we're at within the object at the start of every new
                                 // bank collection
                // the datasets come in multiples of 9 (9 columns per row). loop through the
                // number of rows to insert by dividing number of data by 9
                for (int x = 0; x < (dataToInsert.get("row" + i).size() / 9); x++) {
                    // populate the oneRowofData 4 indices with the indices in the map. traverse
                    // through the entire object until the number of rows is complete
                    for (int rowNumber = 0; rowNumber < 9; rowNumber++) {
                        oneRowofData[rowNumber] = dataToInsert.get("row" + i).get(indexNumber);
                        indexNumber++;
                    }
                    // after populating the row, write it into the CSV
                    writer.writeNext(oneRowofData);
                }
                System.out.println(" End of one collection \n");
            }
            // closing writer connection
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, List<String>> separateBanks(String text, int pdfPageNumber) {
        String banks[] = text.split("\s\s\s\s[0-9]{3}", 0);
        String bankNames[] = new String[banks.length];
        int indexOfLastLine = 0;
        String informationArray[] = new String[banks.length];
        List<String> rowOfData = new ArrayList<>();
        Map<String, List<String>> finalMap = new HashMap();
        for (int i = 0; i < banks.length; i++) {
            // first index is already a separate bank names, save it as first index here
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

            // save the rest of bank names in this array
            if (i > 0) {
                indexOfLastLine = banks[i].lastIndexOf("\n");
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
                        // rowOfData = new ArrayList<>();
                        // index 0 of returned array is MICR, index 1 of returned array is routing
                        // number
                        rowOfData.add(findMICRandRoutingNumbers(
                                addressAndNumbersSeparated.get("numbersUnseparated").get(x))[0]);
                        rowOfData.add(findMICRandRoutingNumbers(
                                addressAndNumbersSeparated.get("numbersUnseparated").get(x))[1]);
                        rowOfData.add(addressAndNumbersSeparated.get("addressSeparated").get(x));
                        rowOfData.add(separateAddresses(addressAndNumbersSeparated.get("addressSeparated").get(x))[0]); // suite
                                                                                                                        // number
                        rowOfData.add(separateAddresses(addressAndNumbersSeparated.get("addressSeparated").get(x))[1]); // street
                                                                                                                        // address
                        rowOfData.add(separateAddresses(addressAndNumbersSeparated.get("addressSeparated").get(x))[2]); // city
                        rowOfData.add(separateAddresses(addressAndNumbersSeparated.get("addressSeparated").get(x))[3]); // province
                        rowOfData.add(separateAddresses(addressAndNumbersSeparated.get("addressSeparated").get(x))[4]); // postal
                                                                                                                        // code
                    }
                    finalMap.put("row" + i, rowOfData);
                    rowOfData = new ArrayList<>(); // reset arraylist after insertion
                }
            }
        }

        return finalMap;

    }

    public static int[] regExIndex(String pattern, String text, Integer fromIndex) {
        Matcher matcher = Pattern.compile(pattern).matcher(text);
        if ((fromIndex != null && matcher.find(fromIndex)) || matcher.find()) {
            return new int[] { matcher.start(), matcher.end() };
        }

        // System.out.println(text);
        return new int[] { -1, -1 };
    }

    public static boolean addressContainsPostalCode(String lastTenChars) {
        int[] contains = regExIndex("[a-zA-Z][0-9]{1}[a-zA-Z]\s[0-9]{1}[a-zA-Z][0-9]{1}", lastTenChars, 0);
        if (contains[0] != -1)
            return true;
        else
            return false;
    }

    public static boolean lastCharsAreProvince(String lastTwoChars) {
        int[] contains = regExIndex("\s*[a-zA-Z][a-zA-Z]\s*", lastTwoChars, 0);
        if (contains[0] != -1)
            return true;
        else
            return false;
    }

    public static boolean addressContainPartPostalCode(String lastTenChars) {
        int[] contains = regExIndex("[a-zA-Z][0-9]{1}[a-zA-Z]", lastTenChars, 0);
        if (contains[0] != -1)
            return true;
        else
            return false;
    }

    public static String[] separateAddresses(String unsepAddress) {

        // convert address first to sentence case:
        final char[] delimeters = { ' ', '-' };
        unsepAddress = WordUtils.capitalizeFully(unsepAddress, delimeters);

        // in the form of { unit, street, city, province, postal code}
        String[] finalParsedAddress = new String[5];
        for (int k = 0; k < 5; k++) {
            finalParsedAddress[k] = ""; // initial values
        }
        if (addressContainsPostalCode(unsepAddress.substring(unsepAddress.length() - 10))) {
            // separate by comma everything except the last 10 characters because we know
            // those are the province and postal code
            if (unsepAddress.length() > 10) {
                String[] splitByComma = unsepAddress.substring(0, unsepAddress.length() - 10).split("\\s*,\\s*");
                String provinceAndPostal = unsepAddress.substring(unsepAddress.length() - 10);
                String[] separated = provinceAndPostal.split("\s");
                // the two part postal code
                if (separated.length > 1) {
                    if (separated.length >= 3) {
                        finalParsedAddress[4] = separated[1].toUpperCase() + ' ' + separated[2].toUpperCase();
                    } else {
                        finalParsedAddress[4] = separated[1].toUpperCase(); // to catch cases where the line has no
                                                                            // postal
                                                                            // code
                    }
                }
                finalParsedAddress[3] = separated[0].toUpperCase();

                // initial boolean vals
                boolean isSuite = false;
                boolean isCity = false;

                // once all arrays return correctly, pass the addressSep into this function to
                // return, pass to CSV function to write into columns
                for (int x = 0; x < splitByComma.length; x++) {

                    splitByComma[x] = splitByComma[x].trim();
                    if (splitByComma[x].contains("Suite")) {
                        String[] streetTemp = splitByComma[x].split("Suite");
                        if (streetTemp.length > 1) {
                            int[] startEndIndex = regExIndex(
                                    "[a-zA-Z]-[0-9]{1,4}|[0-9]{1,4}\s*&\s*[0-9]{1,4}|[0-9]{1,4}-[0-9]{1,4}|[a-zA-Z][0-9]{1,4}-[0-9]{1,4}|[a-zA-Z][0-9]{1,4}[a-zA-Z]|[a-zA-Z][a-zA-Z]-[0-9]{1,4}|[a-zA-Z][a-zA-Z][0-9]{1,4}|[a-zA-Z]-[0-9]{1,4}|[a-zA-Z][0-9]{1,4}|[0-9][a-zA-Z]|[a-zA-Z]|[0-9]{1,4}",
                                    streetTemp[1], 0);// find location of
                            // suite number
                            finalParsedAddress[0] = "Suite "
                                    + streetTemp[1].substring(startEndIndex[0], startEndIndex[1]).toUpperCase();
                            if (streetTemp[0] == "") {
                                finalParsedAddress[1] += streetTemp[1].substring(startEndIndex[1],
                                        streetTemp[1].length())
                                        + ", ";
                            } else {
                                finalParsedAddress[1] += streetTemp[0] + ", ";
                            }
                            isSuite = true;
                        }
                    } else if (splitByComma[x].contains("Unit") &&
                            regExIndex("Units\s*[A-Z][a-zA-z]", splitByComma[x], 0)[0] == -1 &&
                            regExIndex("Unite\s*[A-Z][a-zA-z]", splitByComma[x], 0)[0] == -1 &&
                            regExIndex("Unit\s*[A-Z][a-zA-z]", splitByComma[x], 0)[0] == -1

                    ) {
                        splitByComma[x] = splitByComma[x].replace("Units", "Unit");
                        splitByComma[x] = splitByComma[x].replace("Unite", "Unit");
                        String[] streetTemp = splitByComma[x].split("Unit");
                        if (streetTemp.length > 1) {
                            int[] startEndIndex = regExIndex(
                                    "[a-zA-Z]-[0-9]{1,4}|[0-9]{1,4}\s*&\s*[0-9]{1,4}|[0-9]{1,4}-[0-9]{1,4}|[a-zA-Z][0-9]{1,4}-[0-9]{1,4}|[a-zA-Z][0-9]{1,4}[a-zA-Z]|[a-zA-Z][a-zA-Z]-[0-9]{1,4}|[a-zA-Z][a-zA-Z][0-9]{1,4}|[a-zA-Z]-[0-9]{1,4}|[a-zA-Z][0-9]{1,4}|[0-9][a-zA-Z]|[A-Z]|[0-9]{1,4}",
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
                                    finalParsedAddress[1] += streetTemp[0] + ", ";
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
                    // if all else fails, then it is a street
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

        for (int cleaning = 0; cleaning < finalParsedAddress.length; cleaning++) {
            finalParsedAddress[cleaning] = finalParsedAddress[cleaning].trim();
            if (finalParsedAddress[cleaning] != "") {
                if (finalParsedAddress[cleaning].charAt(0) == '#') {
                    finalParsedAddress[cleaning] = finalParsedAddress[cleaning].replaceFirst("#", "Unit ");
                }
                if (finalParsedAddress[cleaning].charAt(0) == ',') {
                    finalParsedAddress[cleaning] = finalParsedAddress[cleaning].replaceFirst(",", "");
                }
                if (finalParsedAddress[cleaning].charAt(finalParsedAddress[cleaning].length() - 1) == '-') {
                    finalParsedAddress[cleaning] = finalParsedAddress[cleaning].substring(0,
                            finalParsedAddress[cleaning].lastIndexOf("-") - 1);
                }
                if (finalParsedAddress[cleaning].charAt(0) == '-') {
                    finalParsedAddress[cleaning] = finalParsedAddress[cleaning].replaceFirst("-", "Unit ");
                }
            } else
                continue;
        }
        return finalParsedAddress;
    }

    public static String[] findMICRandRoutingNumbers(String unsepNumber) {
        String[] splitBySpace = unsepNumber.split("\s");
        return splitBySpace;
    }

    public static boolean stringContainsItemFromList(String inputStr, String[] items) {
        return Arrays.stream(items).anyMatch(inputStr::contains);
    }

    public static Map<String, List<String>> separateAddressInformation(String information, int pages) {
        // step1: separate each string into multiple lines:
        information = information.trim();
        String[] lines = information.split(System.lineSeparator());

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
                if (!addressContainsPostalCode(lines[i].substring(lines[i].length() - 10)) && (i != lines.length - 1)
                        && lines[i].lastIndexOf("(Sub") == -1) {

                    lines[i] = lines[i] + " " + lines[i + 1];

                    lines[i + 1] = "";

                }

                if (lines[i].contains("(Sub to")) {
                    lines[i] = lines[i].substring(0, lines[i].lastIndexOf("(Sub"));

                }

                lines[i] = lines[i].replace("Suites", "Suite");
                lines[i] = lines[i].replace("Suite No.", "Suite");
                lines[i] = lines[i].replace("*Csm*", "");
                linesLength = lines[i].length();
                numbersTemp = lines[i].substring(0, charactersToSeparate).trim();
                addressesTemp = lines[i].substring(charactersToSeparate, linesLength).trim();
                finalAddresses.add(addressesTemp);
                separateNumbersFromAddress.add(numbersTemp);
            }
        }

        // to access individual string items, use .get(index)
        Map<String, List<String>> map = new HashMap();
        map.put("addressSeparated", finalAddresses);
        map.put("numbersUnseparated", separateNumbersFromAddress);

        return map;
    }

}