import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import com.opencsv.CSVWriter;

import java.util.*;

import java.lang.String;

public class App {
    public static void main(String[] args) throws Exception {
        Map<String, List<String>> returnedCleanData = new HashMap();
        // if the file is available in local machine
        File file = new File("D:\\22-10-14-MBRBNKSN.pdf");
        FileInputStream fis = new FileInputStream(file);
        PDDocument pdfDocument = PDDocument.load(fis);
        int pdfPageNumber = pdfDocument.getPages().getCount();

        // System.out.println("Number of Pages: " + pdfPageNumber);

        PDFTextStripper pdfTextStripper = new PDFTextStripper();
        pdfTextStripper.setStartPage(1); // comment this line if you want to read the entire document
        pdfTextStripper.setEndPage(pdfPageNumber); // comment this line if you want to read the entire document
        String docText = pdfTextStripper.getText(pdfDocument);

        // System.out.println(docText);
        returnedCleanData = separateBanks(docText, pdfPageNumber);

        writeFinalDataToCSV("D:\\ApparaCSVs\\Test.csv", returnedCleanData);

        pdfDocument.close();
        fis.close();
    }

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
            System.out.println(dataToInsert.get("row1").size());
            int count = 0;
            // add data to csv
            for (int i = 1; i <= dataToInsert.size(); i++) {
                count = 0;
                for (int x = 0; x < (dataToInsert.get("row" + i).size() / 4); x++) {
                    oneRowofData[0] = (dataToInsert.get("row" + i).get(count));
                    count++;
                    oneRowofData[1] = (dataToInsert.get("row" + i).get(count));
                    count++;
                    oneRowofData[2] = (dataToInsert.get("row" + i).get(count));
                    count++;
                    oneRowofData[3] = (dataToInsert.get("row" + i).get(count));
                    count++;
                    writer.writeNext(oneRowofData);
                }
                System.out.println(" End of one row \n");
            }

            // closing writer connection
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static Map<String, List<String>> separateBanks(String text, int pdfPageNumber) {
        // regular expression needed to separate banks based on the format of XXXXXXXXX
        // 123 reg ex: [A-Z\s]+
        // System.out.println("Start of function");
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

        // for (int k = 1; k <= finalMap.size(); k++) {
        // System.out.println(finalMap.get("row" + k));
        // System.out.println(" End of one row \n");
        // }
        // idea about looping through if its a multiple of 4 (4 data sets in each row..
        // once it is a multiple of 4, insert row in CSV)
        return finalMap;

    }

    public static String[] findMICRandRoutingNumbers(String unsepNumber) {
        String[] splitBySpace = unsepNumber.split("\s");
        return splitBySpace;
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
                                                                                 // page number as a line
                // of data
                numbersTemp = lines[i].substring(0, charactersToSeparate).trim();
                addressesTemp = lines[i].substring(charactersToSeparate, linesLength).trim();
                finalAddresses.add(addressesTemp);
                separateNumbersFromAddress.add(numbersTemp);
            }
        }

        // to access individual string items, use .get(index)
        // System.out.println(finalAddresses);
        // System.out.println(separateNumbersFromAddress);
        Map<String, List<String>> map = new HashMap();
        map.put("addressSeparated", finalAddresses);
        map.put("numbersUnseparated", separateNumbersFromAddress);

        return map;
    }

}
