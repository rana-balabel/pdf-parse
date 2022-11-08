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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import com.opencsv.CSVWriter;

import java.util.*;

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
        // function will return a map containing several List<String> objects
        // each object is a collection of the SAME insitution, with its rows of data to
        // insert in the form of:
        // { NAME, MICR, ROUTING NUMBER, ADDRESS, NAME, MICR, ROUTING NUMBER, ADDRESS}
        // for however many lines of data there are
        returnedCleanData = separateBanks(docText, pdfPageNumber);

        // write the final data to a CSV already existing locally
        writeFinalDataToCSV("D:\\ApparaCSVs\\Test.csv", returnedCleanData);

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
            String[] header = { "Institution Name", "MICR", "Routing Numbers", "Address" };
            writer.writeNext(header);
            String[] oneRowofData = new String[4];
            // variable to track where we are in the current data object (first index,
            // second, etc)
            int indexNumber = 0;
            // add data to csv
            // note that the object keys are identified as row1, row2, row3, etc for each
            // collection of banks
            for (int i = 1; i <= dataToInsert.size(); i++) {
                indexNumber = 0; // reset the index of where we're at within the object at the start of every new
                                 // bank collection
                // the datasets come in multiples of 4 (4 columns per row). loop through the
                // number of rows to insert by dividing number of data by 4
                for (int x = 0; x < (dataToInsert.get("row" + i).size() / 4); x++) {
                    // populate the oneRowofData 4 indices with the indices in the map. traverse
                    // through the entire object until the number of rows is complete
                    for (int rowNumber = 0; rowNumber < 4; rowNumber++) {
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
                    }
                    finalMap.put("row" + i, rowOfData);
                    rowOfData = new ArrayList<>(); // reset arraylist after insertion
                }
            }
        }

        return finalMap;

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
        String[] specialCases = {
                "000308561 08561-003", "000304453 04453-003", "000304136 04136-003", "000102279 02279-001",
                "000108892 08892-001", "000108930 08930-001", "000116626 16626-001", "001007292 07292-010" };
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
                if (stringContainsItemFromList(lines[i], specialCases)) {
                    if (lines[i].lastIndexOf("-") == linesLength - 1) {
                        lines[i] = lines[i] + "" + lines[i + 1];
                    } else {
                        lines[i] = lines[i] + ", " + lines[i + 1];
                    }
                    lines[i + 1] = "";
                    linesLength = lines[i].length(); // update line length
                }
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
